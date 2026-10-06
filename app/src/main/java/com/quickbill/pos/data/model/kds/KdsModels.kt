package com.quickbill.pos.data.model.kds

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class DeviceMode(val displayName: String, val description: String) {
    POS("POS Mode (Checkout & Counter)", "Take orders, manage products, accept payments, and send orders to Kitchen Display"),
    KDS("KDS Mode (Kitchen Display System)", "Display incoming orders in real-time, track cooking stages, and notify counter")
}

enum class OrderStatus(val displayName: String) {
    NEW("New"),
    PREPARING("Preparing"),
    READY("Ready"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    fun nextStatus(): OrderStatus? {
        return when (this) {
            NEW -> PREPARING
            PREPARING -> READY
            READY -> COMPLETED
            COMPLETED, CANCELLED -> null
        }
    }

    fun canTransitionTo(next: OrderStatus): Boolean {
        if (this == next) return false
        if (this == COMPLETED || this == CANCELLED) return false
        if (next == CANCELLED) return true // can cancel any non-completed order
        return when (this) {
            NEW -> next == PREPARING || next == READY || next == COMPLETED
            PREPARING -> next == READY || next == COMPLETED
            READY -> next == COMPLETED
            else -> false
        }
    }
}

enum class OrderEventType {
    ORDER_CREATED,
    ORDER_MODIFIED,
    ORDER_CANCELLED,
    STATUS_CHANGED,
    ORDER_ACK,
    POS_HELLO,
    PING,
    PONG
}

enum class OrderType(val displayName: String) {
    DINE_IN("Dine-In"),
    TAKEAWAY("Takeaway"),
    DELIVERY("Delivery")
}

data class KitchenOrderItem(
    val id: Long = 0,
    val orderId: String,
    val productId: Long,
    val name: String,
    val quantity: Int,
    val unitPrice: Double = 0.0,
    val notes: String = "",
    val isVeg: Boolean = true
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("orderId", orderId)
            put("productId", productId)
            put("name", name)
            put("quantity", quantity)
            put("unitPrice", unitPrice)
            put("notes", notes)
            put("isVeg", isVeg)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): KitchenOrderItem {
            return KitchenOrderItem(
                id = json.optLong("id", 0),
                orderId = json.optString("orderId", ""),
                productId = json.optLong("productId", 0),
                name = json.optString("name", "Unknown Item"),
                quantity = json.optInt("quantity", 1),
                unitPrice = json.optDouble("unitPrice", 0.0),
                notes = json.optString("notes", ""),
                isVeg = json.optBoolean("isVeg", true)
            )
        }
    }
}

data class KitchenOrder(
    val orderId: String = UUID.randomUUID().toString(),
    val orderNumber: String,
    val customerName: String = "",
    val tableNumber: String = "",
    val notes: String = "",
    val orderType: OrderType = OrderType.DINE_IN,
    val status: OrderStatus = OrderStatus.NEW,
    val items: List<KitchenOrderItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    fun toJson(): JSONObject {
        val itemsArray = JSONArray()
        items.forEach { itemsArray.put(it.toJson()) }

        return JSONObject().apply {
            put("orderId", orderId)
            put("orderNumber", orderNumber)
            put("customerName", customerName)
            put("tableNumber", tableNumber)
            put("notes", notes)
            put("orderType", orderType.name)
            put("status", status.name)
            put("items", itemsArray)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
            put("synced", synced)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): KitchenOrder {
            val orderId = json.optString("orderId", UUID.randomUUID().toString())
            val itemsList = mutableListOf<KitchenOrderItem>()
            val itemsArray = json.optJSONArray("items")
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(i)
                    itemsList.add(KitchenOrderItem.fromJson(itemObj))
                }
            }

            return KitchenOrder(
                orderId = orderId,
                orderNumber = json.optString("orderNumber", "ORD-000"),
                customerName = json.optString("customerName", ""),
                tableNumber = json.optString("tableNumber", ""),
                notes = json.optString("notes", ""),
                orderType = try {
                    OrderType.valueOf(json.optString("orderType", OrderType.DINE_IN.name))
                } catch (_: Exception) {
                    OrderType.DINE_IN
                },
                status = try {
                    OrderStatus.valueOf(json.optString("status", OrderStatus.NEW.name))
                } catch (_: Exception) {
                    OrderStatus.NEW
                },
                items = itemsList,
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                synced = json.optBoolean("synced", false)
            )
        }
    }
}

data class OrderEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: OrderEventType,
    val orderId: String,
    val payloadJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("eventId", eventId)
            put("eventType", eventType.name)
            put("orderId", orderId)
            put("payloadJson", payloadJson)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun createOrderEvent(order: KitchenOrder): OrderEvent {
            return OrderEvent(
                eventType = OrderEventType.ORDER_CREATED,
                orderId = order.orderId,
                payloadJson = order.toJson().toString()
            )
        }

        fun createStatusChangedEvent(orderId: String, newStatus: OrderStatus): OrderEvent {
            val payload = JSONObject().apply {
                put("orderId", orderId)
                put("status", newStatus.name)
                put("timestamp", System.currentTimeMillis())
            }
            return OrderEvent(
                eventType = OrderEventType.STATUS_CHANGED,
                orderId = orderId,
                payloadJson = payload.toString()
            )
        }

        fun createOrderCancelledEvent(orderId: String, reason: String = ""): OrderEvent {
            val payload = JSONObject().apply {
                put("orderId", orderId)
                put("reason", reason)
                put("status", OrderStatus.CANCELLED.name)
            }
            return OrderEvent(
                eventType = OrderEventType.ORDER_CANCELLED,
                orderId = orderId,
                payloadJson = payload.toString()
            )
        }

        fun createAckEvent(eventId: String, orderId: String): OrderEvent {
            val payload = JSONObject().apply {
                put("acknowledgedEventId", eventId)
                put("orderId", orderId)
                put("status", "ACK")
            }
            return OrderEvent(
                eventType = OrderEventType.ORDER_ACK,
                orderId = orderId,
                payloadJson = payload.toString()
            )
        }

        fun createPosHelloEvent(terminalName: String, deviceModel: String, ipAddress: String): OrderEvent {
            val payload = JSONObject().apply {
                put("terminalName", terminalName)
                put("deviceModel", deviceModel)
                put("ipAddress", ipAddress)
                put("timestamp", System.currentTimeMillis())
            }
            return OrderEvent(
                eventType = OrderEventType.POS_HELLO,
                orderId = "",
                payloadJson = payload.toString()
            )
        }

        fun fromJson(json: JSONObject): OrderEvent {
            return OrderEvent(
                eventId = json.optString("eventId", UUID.randomUUID().toString()),
                eventType = try {
                    OrderEventType.valueOf(json.optString("eventType", OrderEventType.PING.name))
                } catch (_: Exception) {
                    OrderEventType.PING
                },
                orderId = json.optString("orderId", ""),
                payloadJson = json.optString("payloadJson", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

data class ConnectedKdsScreen(
    val id: String, // "host:port"
    val name: String,
    val host: String,
    val port: Int,
    val status: String = "CONNECTED",
    val connectedAt: Long = System.currentTimeMillis()
)

data class ConnectedPosTerminal(
    val id: String, // "ip:port" or unique socket identifier
    val name: String,
    val ipAddress: String,
    val port: Int,
    val deviceModel: String = "",
    val connectedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("ipAddress", ipAddress)
            put("port", port)
            put("deviceModel", deviceModel)
            put("connectedAt", connectedAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ConnectedPosTerminal {
            return ConnectedPosTerminal(
                id = json.optString("id", ""),
                name = json.optString("name", "POS Terminal"),
                ipAddress = json.optString("ipAddress", "127.0.0.1"),
                port = json.optInt("port", 0),
                deviceModel = json.optString("deviceModel", ""),
                connectedAt = json.optLong("connectedAt", System.currentTimeMillis())
            )
        }
    }
}

