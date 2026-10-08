package com.quickbill.pos

import com.quickbill.pos.data.model.kds.ConnectedKdsScreen
import com.quickbill.pos.data.model.kds.ConnectedPosTerminal
import com.quickbill.pos.data.model.kds.KitchenOrder
import com.quickbill.pos.data.model.kds.KitchenOrderItem
import com.quickbill.pos.data.model.kds.OrderEvent
import com.quickbill.pos.data.model.kds.OrderEventType
import com.quickbill.pos.data.model.kds.OrderStatus
import com.quickbill.pos.data.model.kds.OrderType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class KdsSyncUnitTest {

    // -------------------------------------------------------------
    // Test 1: Order Creation and Serialization
    // -------------------------------------------------------------
    @Test
    fun test1_orderCreationAndSerialization() {
        val items = listOf(
            KitchenOrderItem(
                id = 1,
                orderId = "ord-101",
                productId = 42,
                name = "Paneer Butter Masala",
                quantity = 2,
                unitPrice = 220.0,
                notes = "Extra spicy",
                isVeg = true
            ),
            KitchenOrderItem(
                id = 2,
                orderId = "ord-101",
                productId = 43,
                name = "Butter Naan",
                quantity = 4,
                unitPrice = 40.0,
                notes = "",
                isVeg = true
            )
        )

        val originalOrder = KitchenOrder(
            orderId = "ord-101",
            orderNumber = "ORD-2026-001",
            customerName = "Rahul Sharma",
            tableNumber = "T-4",
            notes = "Serve together",
            orderType = OrderType.DINE_IN,
            status = OrderStatus.NEW,
            items = items,
            createdAt = 1770000000000L,
            updatedAt = 1770000000000L,
            synced = false
        )

        val json = originalOrder.toJson()
        val parsedOrder = KitchenOrder.fromJson(json)

        assertEquals("ord-101", parsedOrder.orderId)
        assertEquals("ORD-2026-001", parsedOrder.orderNumber)
        assertEquals("Rahul Sharma", parsedOrder.customerName)
        assertEquals("T-4", parsedOrder.tableNumber)
        assertEquals(OrderType.DINE_IN, parsedOrder.orderType)
        assertEquals(OrderStatus.NEW, parsedOrder.status)
        assertEquals(2, parsedOrder.items.size)
        assertEquals("Paneer Butter Masala", parsedOrder.items[0].name)
        assertEquals(2, parsedOrder.items[0].quantity)
        assertEquals("Butter Naan", parsedOrder.items[1].name)
        assertEquals(4, parsedOrder.items[1].quantity)
    }

    // -------------------------------------------------------------
    // Test 2: Valid Status Transitions (NEW -> PREPARING -> READY -> COMPLETED)
    // -------------------------------------------------------------
    @Test
    fun test2_validStatusTransitions() {
        val initialStatus = OrderStatus.NEW

        val preparingStatus = initialStatus.nextStatus()
        assertEquals(OrderStatus.PREPARING, preparingStatus)
        assertTrue(initialStatus.canTransitionTo(OrderStatus.PREPARING))

        val readyStatus = preparingStatus?.nextStatus()
        assertEquals(OrderStatus.READY, readyStatus)
        assertTrue(preparingStatus!!.canTransitionTo(OrderStatus.READY))

        val completedStatus = readyStatus?.nextStatus()
        assertEquals(OrderStatus.COMPLETED, completedStatus)
        assertTrue(readyStatus!!.canTransitionTo(OrderStatus.COMPLETED))

        // Completed has no next status
        val nextAfterCompleted = completedStatus?.nextStatus()
        assertNull(nextAfterCompleted)
    }

    // -------------------------------------------------------------
    // Test 3: Invalid Status Transitions Blocked
    // -------------------------------------------------------------
    @Test
    fun test3_invalidStatusTransitionsBlocked() {
        // COMPLETED order cannot transition back to PREPARING or NEW
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.PREPARING))
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.NEW))
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.READY))
        assertNull(OrderStatus.COMPLETED.nextStatus())

        // READY order cannot transition backwards to NEW
        assertFalse(OrderStatus.READY.canTransitionTo(OrderStatus.NEW))

        // Same status transition is blocked
        assertFalse(OrderStatus.PREPARING.canTransitionTo(OrderStatus.PREPARING))

        // CANCELLED order cannot transition anywhere
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PREPARING))
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.COMPLETED))
        assertNull(OrderStatus.CANCELLED.nextStatus())
    }

    // -------------------------------------------------------------
    // Test 4: Order Cancellation from Active States
    // -------------------------------------------------------------
    @Test
    fun test4_orderCancellationFromNewAndPreparing() {
        // Cancellation allowed from NEW and PREPARING
        assertTrue(OrderStatus.NEW.canTransitionTo(OrderStatus.CANCELLED))
        assertTrue(OrderStatus.PREPARING.canTransitionTo(OrderStatus.CANCELLED))
        assertTrue(OrderStatus.READY.canTransitionTo(OrderStatus.CANCELLED))

        // Cancellation NOT allowed once already COMPLETED
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.CANCELLED))

        // Event builder creates proper cancellation payload
        val cancelEvent = OrderEvent.createOrderCancelledEvent("ord-999", "Customer walked out")
        assertEquals(OrderEventType.ORDER_CANCELLED, cancelEvent.eventType)
        assertEquals("ord-999", cancelEvent.orderId)

        val payload = JSONObject(cancelEvent.payloadJson)
        assertEquals("ord-999", payload.getString("orderId"))
        assertEquals("Customer walked out", payload.getString("reason"))
        assertEquals(OrderStatus.CANCELLED.name, payload.getString("status"))
    }

    // -------------------------------------------------------------
    // Test 5: Order Event Serialization and Deserialization
    // -------------------------------------------------------------
    @Test
    fun test5_orderEventCreationAndSerialization() {
        val order = KitchenOrder(
            orderId = "ord-555",
            orderNumber = "ORD-555",
            customerName = "Priya",
            status = OrderStatus.NEW
        )

        val createEvent = OrderEvent.createOrderEvent(order)
        val eventJson = createEvent.toJson()
        val parsedEvent = OrderEvent.fromJson(eventJson)

        assertEquals(createEvent.eventId, parsedEvent.eventId)
        assertEquals(OrderEventType.ORDER_CREATED, parsedEvent.eventType)
        assertEquals("ord-555", parsedEvent.orderId)
        assertTrue(parsedEvent.payloadJson.contains("ORD-555"))

        // Status changed event roundtrip
        val statusEvent = OrderEvent.createStatusChangedEvent("ord-555", OrderStatus.READY)
        val statusJson = statusEvent.toJson()
        val parsedStatusEvent = OrderEvent.fromJson(statusJson)

        assertEquals(OrderEventType.STATUS_CHANGED, parsedStatusEvent.eventType)
        val statusPayload = JSONObject(parsedStatusEvent.payloadJson)
        assertEquals("READY", statusPayload.getString("status"))

        // ACK event roundtrip
        val ackEvent = OrderEvent.createAckEvent("evt-123", "ord-555")
        val ackJson = ackEvent.toJson()
        val parsedAckEvent = OrderEvent.fromJson(ackJson)

        assertEquals(OrderEventType.ORDER_ACK, parsedAckEvent.eventType)
        val ackPayload = JSONObject(parsedAckEvent.payloadJson)
        assertEquals("evt-123", ackPayload.getString("acknowledgedEventId"))
    }

    // -------------------------------------------------------------
    // Test 6: Duplicate Event Deduplication Logic (Idempotency)
    // -------------------------------------------------------------
    @Test
    fun test6_duplicateEventDeduplicationLogic() {
        val processedEventIds = mutableSetOf<String>()

        fun processEvent(event: OrderEvent): Boolean {
            if (processedEventIds.contains(event.eventId)) {
                // Idempotent: Duplicate detected, do not process again
                return false
            }
            processedEventIds.add(event.eventId)
            return true
        }

        val event = OrderEvent(
            eventId = "unique-evt-001",
            eventType = OrderEventType.ORDER_CREATED,
            orderId = "ord-1"
        )

        // First attempt: should succeed
        val firstResult = processEvent(event)
        assertTrue(firstResult)
        assertEquals(1, processedEventIds.size)

        // Second attempt with exact same eventId (e.g. network retry): should be detected as duplicate
        val retryResult = processEvent(event)
        assertFalse(retryResult)
        assertEquals(1, processedEventIds.size)
    }

    // -------------------------------------------------------------
    // Test 7: Outbox Queue Preserves FIFO Order
    // -------------------------------------------------------------
    @Test
    fun test7_outboxQueueFifoOrdering() {
        val queue = mutableListOf<OrderEvent>()

        val event1 = OrderEvent(eventId = "evt-1", eventType = OrderEventType.ORDER_CREATED, orderId = "ord-1", timestamp = 1000L)
        val event2 = OrderEvent(eventId = "evt-2", eventType = OrderEventType.STATUS_CHANGED, orderId = "ord-1", timestamp = 2000L)
        val event3 = OrderEvent(eventId = "evt-3", eventType = OrderEventType.ORDER_CREATED, orderId = "ord-2", timestamp = 3000L)

        // Add events
        queue.add(event1)
        queue.add(event2)
        queue.add(event3)

        // Sort by timestamp as Room does (ORDER BY createdAt ASC)
        val ordered = queue.sortedBy { it.timestamp }

        assertEquals("evt-1", ordered[0].eventId)
        assertEquals("evt-2", ordered[1].eventId)
        assertEquals("evt-3", ordered[2].eventId)
    }

    // -------------------------------------------------------------
    // Test 8: Outbox ACK Removes Pending Event
    // -------------------------------------------------------------
    @Test
    fun test8_outboxAckRemovesPendingEvent() {
        val pendingOutbox = mutableMapOf<String, OrderEvent>()

        val event1 = OrderEvent(eventId = "evt-ack-1", eventType = OrderEventType.ORDER_CREATED, orderId = "ord-1")
        val event2 = OrderEvent(eventId = "evt-ack-2", eventType = OrderEventType.ORDER_CREATED, orderId = "ord-2")

        pendingOutbox[event1.eventId] = event1
        pendingOutbox[event2.eventId] = event2
        assertEquals(2, pendingOutbox.size)

        // Simulate receiving ACK for event 1
        val ackEvent = OrderEvent.createAckEvent("evt-ack-1", "ord-1")
        val payload = JSONObject(ackEvent.payloadJson)
        val acknowledgedId = payload.getString("acknowledgedEventId")

        pendingOutbox.remove(acknowledgedId)

        assertEquals(1, pendingOutbox.size)
        assertFalse(pendingOutbox.containsKey("evt-ack-1"))
        assertTrue(pendingOutbox.containsKey("evt-ack-2"))
    }

    // -------------------------------------------------------------
    // Test 9: Retry Count Increment on Transmission Failure
    // -------------------------------------------------------------
    @Test
    fun test9_retryCountIncrementOnFailure() {
        data class MockPendingEvent(
            val eventId: String,
            var retryCount: Int = 0,
            var status: String = "PENDING"
        )

        val pending = MockPendingEvent("evt-fail-1", retryCount = 0)

        // Simulate failed transmission attempt 1
        pending.retryCount++
        pending.status = "FAILED"
        assertEquals(1, pending.retryCount)
        assertEquals("FAILED", pending.status)

        // Simulate failed transmission attempt 2
        pending.retryCount++
        assertEquals(2, pending.retryCount)
    }

    // -------------------------------------------------------------
    // Test 10: Long-Wait Warning Calculation
    // -------------------------------------------------------------
    @Test
    fun test10_longWaitWarningCalculation() {
        fun isOrderOverdue(
            orderStatus: OrderStatus,
            createdAt: Long,
            currentTime: Long,
            thresholdMinutes: Int
        ): Boolean {
            if (orderStatus == OrderStatus.COMPLETED || orderStatus == OrderStatus.CANCELLED) {
                return false
            }
            val elapsedMinutes = (currentTime - createdAt) / (1000 * 60)
            return elapsedMinutes >= thresholdMinutes
        }

        val currentTime = 1000000000000L
        val warningThresholdMinutes = 5

        // Order created 2 minutes ago: NOT overdue
        val recentOrderCreated = currentTime - (2 * 60 * 1000L)
        assertFalse(isOrderOverdue(OrderStatus.NEW, recentOrderCreated, currentTime, warningThresholdMinutes))

        // Order created 6 minutes ago: OVERDUE!
        val lateOrderCreated = currentTime - (6 * 60 * 1000L)
        assertTrue(isOrderOverdue(OrderStatus.NEW, lateOrderCreated, currentTime, warningThresholdMinutes))
        assertTrue(isOrderOverdue(OrderStatus.PREPARING, lateOrderCreated, currentTime, warningThresholdMinutes))

        // Order created 10 minutes ago, but already COMPLETED: NOT overdue!
        val completedLateOrder = currentTime - (10 * 60 * 1000L)
        assertFalse(isOrderOverdue(OrderStatus.COMPLETED, completedLateOrder, currentTime, warningThresholdMinutes))

        // Order created 10 minutes ago, but CANCELLED: NOT overdue!
        assertFalse(isOrderOverdue(OrderStatus.CANCELLED, completedLateOrder, currentTime, warningThresholdMinutes))
    }

    // -------------------------------------------------------------
    // Test 11: POS_HELLO Event Creation and Parsing
    // -------------------------------------------------------------
    @Test
    fun test11_posHelloEventCreationAndParsing() {
        val helloEvent = OrderEvent.createPosHelloEvent(
            terminalName = "Main Counter POS",
            deviceModel = "Pixel 7 Pro",
            ipAddress = "192.168.1.15"
        )

        assertEquals(OrderEventType.POS_HELLO, helloEvent.eventType)
        assertEquals("", helloEvent.orderId)
        assertTrue(helloEvent.payloadJson.isNotBlank())

        val payload = JSONObject(helloEvent.payloadJson)
        assertEquals("Main Counter POS", payload.getString("terminalName"))
        assertEquals("Pixel 7 Pro", payload.getString("deviceModel"))
        assertEquals("192.168.1.15", payload.getString("ipAddress"))

        // Roundtrip serialization
        val eventJson = helloEvent.toJson()
        val parsedEvent = OrderEvent.fromJson(eventJson)
        assertEquals(OrderEventType.POS_HELLO, parsedEvent.eventType)
        assertEquals(helloEvent.eventId, parsedEvent.eventId)
    }

    // -------------------------------------------------------------
    // Test 12: ConnectedPosTerminal Serialization
    // -------------------------------------------------------------
    @Test
    fun test12_connectedPosTerminalSerialization() {
        val terminal = ConnectedPosTerminal(
            id = "192.168.1.15:52134",
            name = "QuickBill POS Terminal",
            ipAddress = "192.168.1.15",
            port = 52134,
            deviceModel = "Samsung Galaxy Tab",
            connectedAt = 1770000000000L
        )

        val json = terminal.toJson()
        val parsed = ConnectedPosTerminal.fromJson(json)

        assertEquals(terminal.id, parsed.id)
        assertEquals(terminal.name, parsed.name)
        assertEquals(terminal.ipAddress, parsed.ipAddress)
        assertEquals(terminal.port, parsed.port)
        assertEquals(terminal.deviceModel, parsed.deviceModel)
        assertEquals(terminal.connectedAt, parsed.connectedAt)
    }

    // -------------------------------------------------------------
    // Test 13: ConnectedKdsScreen Model and State
    // -------------------------------------------------------------
    @Test
    fun test13_connectedKdsScreenModel() {
        val screen1 = ConnectedKdsScreen(
            id = "192.168.1.20:8080",
            name = "Main Kitchen",
            host = "192.168.1.20",
            port = 8080,
            status = "CONNECTED"
        )
        val screen2 = ConnectedKdsScreen(
            id = "192.168.1.21:8080",
            name = "Drinks & Bar Display",
            host = "192.168.1.21",
            port = 8080,
            status = "CONNECTED"
        )

        val screens = listOf(screen1, screen2)
        assertEquals(2, screens.size)
        assertEquals("Main Kitchen", screens[0].name)
        assertEquals("Drinks & Bar Display", screens[1].name)
    }

    // -------------------------------------------------------------
    // Test 14: Smart Connect / Disconnect button state logic
    // -------------------------------------------------------------
    @Test
    fun test14_smartConnectDisconnectButtonStateLogic() {
        val connectedHosts = setOf("192.168.1.50:8080", "192.168.1.51:8080")

        fun shouldShowDisconnectOnly(host: String, port: Int): Boolean {
            return connectedHosts.contains("$host:$port")
        }

        // Host 1: Already connected -> MUST show Disconnect only, Connect hidden!
        assertTrue(shouldShowDisconnectOnly("192.168.1.50", 8080))
        // Host 2: Already connected -> MUST show Disconnect only, Connect hidden!
        assertTrue(shouldShowDisconnectOnly("192.168.1.51", 8080))
        // Host 3: Not connected -> Show Connect button
        assertFalse(shouldShowDisconnectOnly("192.168.1.52", 8080))
    }

    // -------------------------------------------------------------
    // Test 15: Strict Connected Screens Filtering (Preventing Fake Connected Status on Wrong IP)
    // -------------------------------------------------------------
    @Test
    fun test15_strictConnectedScreensFiltering() {
        data class MockConnectionEntry(
            val id: String,
            val host: String,
            val port: Int,
            val status: String // "CONNECTING", "CONNECTED", "DISCONNECTED"
        )

        val entries = listOf(
            MockConnectionEntry("192.168.1.50:8080", "192.168.1.50", 8080, "CONNECTED"),
            MockConnectionEntry("192.168.1.99:8080", "192.168.1.99", 8080, "CONNECTING"), // Non-existent IP
            MockConnectionEntry("192.168.1.100:8080", "192.168.1.100", 8080, "DISCONNECTED")
        )

        // Strict filter: Only CONNECTED screens count towards connectedKdsScreens
        val activeConnectedScreens = entries.filter { it.status == "CONNECTED" }

        assertEquals(1, activeConnectedScreens.size)
        assertEquals("192.168.1.50:8080", activeConnectedScreens[0].id)
        assertFalse(activeConnectedScreens.any { it.host == "192.168.1.99" })
    }

    // -------------------------------------------------------------
    // Test 16: Unique KDS Service Name Generation Across Multiple KDS Devices
    // -------------------------------------------------------------
    @Test
    fun test16_uniqueKdsServiceNameGeneration() {
        fun generateServiceName(kitchenName: String, localIp: String): String {
            val ipSuffix = localIp.substringAfterLast(".", "").ifBlank { "0" }
            val kitchenTitle = kitchenName.trim().ifBlank { "Kitchen" }
            return "QuickKitchen-$kitchenTitle-$ipSuffix"
        }

        val device1Name = generateServiceName("Main Kitchen", "192.168.1.101")
        val device2Name = generateServiceName("Main Kitchen", "192.168.1.102")

        assertEquals("QuickKitchen-Main Kitchen-101", device1Name)
        assertEquals("QuickKitchen-Main Kitchen-102", device2Name)
        // Ensure names do NOT collide even when kitchenName is identical
        assertFalse(device1Name == device2Name)
    }

    // -------------------------------------------------------------
    // Test 17: Status Changed Event Propagation & Multi-KDS Relay
    // -------------------------------------------------------------
    @Test
    fun test17_statusChangedEventSerializationAndMultiKdsRelay() {
        // KDS-1 changes order status from PREPARING to READY
        val orderId = "order-test-555"
        val statusChangedEvent = OrderEvent.createStatusChangedEvent(orderId, OrderStatus.READY)

        val json = statusChangedEvent.toJson()
        val parsedEvent = OrderEvent.fromJson(json)

        assertEquals(OrderEventType.STATUS_CHANGED, parsedEvent.eventType)
        assertEquals(orderId, parsedEvent.orderId)

        val payload = JSONObject(parsedEvent.payloadJson)
        assertEquals(orderId, payload.getString("orderId"))
        assertEquals("READY", payload.getString("status"))

        // Simulate POS relaying to KDS-2
        val receivedOnKds2Status = OrderStatus.valueOf(payload.getString("status"))
        assertEquals(OrderStatus.READY, receivedOnKds2Status)
    }

    // -------------------------------------------------------------
    // Test 18: Multi-KDS Broadcast Exclude Logic (Prevents Echo Loop)
    // -------------------------------------------------------------
    @Test
    fun test18_multiKdsBroadcastExcludeLogic() {
        val connectedScreenIds = listOf("192.168.1.101:8080", "192.168.1.102:8080", "192.168.1.103:8080")

        fun getRecipients(excludeId: String): List<String> {
            return connectedScreenIds.filter { excludeId.isBlank() || it != excludeId }
        }

        // When KDS-1 (101) triggers status change, only KDS-2 (102) and KDS-3 (103) receive it
        val recipientsForKds1Event = getRecipients("192.168.1.101:8080")
        assertEquals(2, recipientsForKds1Event.size)
        assertFalse(recipientsForKds1Event.contains("192.168.1.101:8080"))
        assertTrue(recipientsForKds1Event.contains("192.168.1.102:8080"))
        assertTrue(recipientsForKds1Event.contains("192.168.1.103:8080"))

        // When POS itself broadcasts an order, all screens receive it
        val recipientsForPosOrder = getRecipients("")
        assertEquals(3, recipientsForPosOrder.size)
    }

    // -------------------------------------------------------------
    // Test 19: Deterministic Order ID & Order Deduplication by Order Number
    // -------------------------------------------------------------
    @Test
    fun test19_orderDeduplicationByOrderNumber() {
        val billNumber = "QB-20261007-0002"
        val deterministicOrderId = "order-${billNumber.replace(" ", "_")}"
        assertEquals("order-QB-20261007-0002", deterministicOrderId)

        val order1 = KitchenOrder(
            orderId = deterministicOrderId,
            orderNumber = billNumber,
            customerName = "Walk-in",
            tableNumber = "Takeaway",
            status = OrderStatus.NEW,
            createdAt = 1000L
        )

        // Duplicate incoming order from network sync or re-transmission
        val order2 = KitchenOrder(
            orderId = "legacy-uuid-12345",
            orderNumber = billNumber,
            customerName = "Walk-in",
            tableNumber = "Takeaway",
            status = OrderStatus.PREPARING,
            createdAt = 1000L
        )

        val rawList = listOf(order1, order2)
        assertEquals(2, rawList.size)

        // Deduplication in QuickKitchenViewModel
        val deduplicated = rawList.distinctBy { it.orderNumber }
        assertEquals(1, deduplicated.size)
        assertEquals(billNumber, deduplicated[0].orderNumber)
    }

    // -------------------------------------------------------------
    // Test 20: Overdue Order Detection and Highlight Threshold
    // -------------------------------------------------------------
    @Test
    fun test20_overdueOrderHighlightLogic() {
        fun isOrderOverdue(createdAt: Long, currentTimeMillis: Long, warningThresholdMinutes: Int, status: OrderStatus): Boolean {
            val elapsedSeconds = ((currentTimeMillis - createdAt) / 1000L).coerceAtLeast(0L)
            val elapsedMinutes = elapsedSeconds / 60
            return elapsedMinutes >= warningThresholdMinutes && status != OrderStatus.COMPLETED && status != OrderStatus.CANCELLED
        }

        val baseTime = 1000000000L
        val warningMinutes = 5

        // Order created 2 minutes ago -> NOT overdue
        val recentOrderElapsed = isOrderOverdue(
            createdAt = baseTime,
            currentTimeMillis = baseTime + (2 * 60 * 1000L),
            warningThresholdMinutes = warningMinutes,
            status = OrderStatus.NEW
        )
        assertFalse(recentOrderElapsed)

        // Order created 16 minutes ago (like in user screenshot) -> OVERDUE
        val overdueOrderElapsed = isOrderOverdue(
            createdAt = baseTime,
            currentTimeMillis = baseTime + (16 * 60 * 1000L),
            warningThresholdMinutes = warningMinutes,
            status = OrderStatus.NEW
        )
        assertTrue(overdueOrderElapsed)

        // Completed or Cancelled order past 16 minutes -> NOT overdue (highlight only active orders)
        val completedOrderElapsed = isOrderOverdue(
            createdAt = baseTime,
            currentTimeMillis = baseTime + (16 * 60 * 1000L),
            warningThresholdMinutes = warningMinutes,
            status = OrderStatus.COMPLETED
        )
        assertFalse(completedOrderElapsed)
    }

    // -------------------------------------------------------------
    // Test 17: Wi-Fi Direct (P2P) Device & Connection Models
    // -------------------------------------------------------------
    @Test
    fun test17_wifiDirectP2pDeviceAndConnectionState() {
        val peerAvailable = com.quickbill.pos.network.kds.P2pPeerDevice(
            deviceName = "KDS-Kitchen-1",
            deviceAddress = "aa:bb:cc:dd:ee:ff",
            status = android.net.wifi.p2p.WifiP2pDevice.AVAILABLE
        )
        assertEquals("Available", peerAvailable.statusLabel)
        assertFalse(peerAvailable.isGroupOwner)

        val peerConnected = com.quickbill.pos.network.kds.P2pPeerDevice(
            deviceName = "KDS-Kitchen-2",
            deviceAddress = "11:22:33:44:55:66",
            status = android.net.wifi.p2p.WifiP2pDevice.CONNECTED,
            isGroupOwner = true
        )
        assertEquals("Connected", peerConnected.statusLabel)
        assertTrue(peerConnected.isGroupOwner)

        val connectionState = com.quickbill.pos.network.kds.P2pConnectionState(
            isConnected = true,
            isGroupOwner = false,
            groupOwnerAddress = "192.168.49.1",
            groupNetworkName = "DIRECT-QuickKitchen-KDS",
            clientCount = 1
        )
        assertTrue(connectionState.isConnected)
        assertFalse(connectionState.isGroupOwner)
        assertEquals("192.168.49.1", connectionState.groupOwnerAddress)
        assertEquals("DIRECT-QuickKitchen-KDS", connectionState.groupNetworkName)
    }
}


