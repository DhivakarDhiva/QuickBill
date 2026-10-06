package com.quickbill.pos.network.kds

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.quickbill.pos.data.local.dao.OrderDao
import com.quickbill.pos.data.local.dao.ReceivedEventDao
import com.quickbill.pos.data.local.entity.ReceivedEventEntity
import com.quickbill.pos.data.model.kds.KitchenOrder
import com.quickbill.pos.data.model.kds.OrderEvent
import com.quickbill.pos.data.model.kds.OrderEventType
import com.quickbill.pos.data.model.kds.OrderStatus
import com.quickbill.pos.data.repository.KdsSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class OrderSyncManager(
    private val context: Context,
    private val orderDao: OrderDao,
    private val receivedEventDao: ReceivedEventDao,
    val outboxManager: OutboxManager,
    val connectionManager: ConnectionManager,
    val discoveryManager: NsdDiscoveryManager,
    private val kdsSettingsRepository: KdsSettingsRepository
) {
    companion object {
        private const val TAG = "OrderSyncManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Event notifications for UI toast / sound
    private val _newOrderNotificationFlow = MutableSharedFlow<KitchenOrder>(extraBufferCapacity = 16)
    val newOrderNotificationFlow: SharedFlow<KitchenOrder> = _newOrderNotificationFlow.asSharedFlow()

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator initialization failed", e)
        }

        // POS Mode: Listen for incoming messages from KDS
        scope.launch {
            connectionManager.clientIncomingMessages.collectLatest { rawJson ->
                handleClientMessage(rawJson)
            }
        }

        // KDS Mode: Listen for incoming messages from POS terminals
        scope.launch {
            connectionManager.serverIncomingMessages.collectLatest { (rawJson, clientSocket) ->
                handleServerMessage(rawJson, clientSocket)
            }
        }

        // POS Mode: When connection status becomes CONNECTED, automatically drain outbox!
        scope.launch {
            connectionManager.connectionStatus.collectLatest { status ->
                if (status == ConnectionStatus.CONNECTED) {
                    Log.i(TAG, "Connection established. Draining outbox...")
                    drainPendingOutbox()
                }
            }
        }
    }

    // ==========================================
    // POS Mode Operations
    // ==========================================

    suspend fun sendOrder(order: KitchenOrder) {
        // 1. Save locally in room
        orderDao.saveFullKitchenOrder(order)

        // 2. Wrap in event
        val event = OrderEvent.createOrderEvent(order)

        // 3. Enqueue to persistent outbox
        outboxManager.enqueueEvent(event)

        // 4. If connected, attempt immediate drain
        if (connectionManager.connectionStatus.value == ConnectionStatus.CONNECTED) {
            drainPendingOutbox()
        }
    }

    suspend fun sendOrderCancellation(orderId: String, reason: String = "") {
        orderDao.updateOrderStatus(orderId, OrderStatus.CANCELLED)
        val event = OrderEvent.createOrderCancelledEvent(orderId, reason)
        outboxManager.enqueueEvent(event)

        if (connectionManager.connectionStatus.value == ConnectionStatus.CONNECTED) {
            drainPendingOutbox()
        }
    }

    fun drainPendingOutbox() {
        scope.launch {
            outboxManager.drainOutbox { payloadJson ->
                connectionManager.sendFromClient(payloadJson)
            }
        }
    }

    private suspend fun handleClientMessage(rawJson: String) {
        try {
            val json = JSONObject(rawJson)
            val event = OrderEvent.fromJson(json)

            when (event.eventType) {
                OrderEventType.ORDER_ACK -> {
                    val payload = JSONObject(event.payloadJson)
                    val acknowledgedEventId = payload.optString("acknowledgedEventId", event.eventId)
                    val orderId = payload.optString("orderId", event.orderId)

                    Log.i(TAG, "Received ACK for event $acknowledgedEventId, order $orderId")
                    outboxManager.onEventAcked(acknowledgedEventId)

                    val existing = orderDao.getOrderById(orderId)
                    if (existing != null) {
                        orderDao.updateOrder(existing.copy(synced = true))
                    }
                }

                OrderEventType.STATUS_CHANGED -> {
                    val payload = JSONObject(event.payloadJson)
                    val orderId = payload.getString("orderId")
                    val statusStr = payload.getString("status")
                    val newStatus = try {
                        OrderStatus.valueOf(statusStr)
                    } catch (_: Exception) {
                        OrderStatus.NEW
                    }

                    Log.i(TAG, "POS received STATUS_CHANGED for $orderId: $newStatus")
                    orderDao.updateOrderStatus(orderId, newStatus)
                }

                else -> {
                    Log.d(TAG, "Unhandled event type on POS client: ${event.eventType}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message on POS client", e)
        }
    }

    // ==========================================
    // KDS Mode Operations
    // ==========================================

    private suspend fun handleServerMessage(rawJson: String, originSocket: org.java_websocket.WebSocket) {
        try {
            val json = JSONObject(rawJson)
            val event = OrderEvent.fromJson(json)

            when (event.eventType) {
                OrderEventType.ORDER_CREATED -> {
                    // Idempotency check: Check if eventId has already been processed
                    val alreadyReceived = receivedEventDao.isEventReceived(event.eventId)
                    if (alreadyReceived) {
                        Log.w(TAG, "Duplicate event ${event.eventId} detected. Sending ACK without re-inserting.")
                        val ack = OrderEvent.createAckEvent(event.eventId, event.orderId)
                        try {
                            originSocket.send(ack.toJson().toString())
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send duplicate ACK", e)
                        }
                        return
                    }

                    // Record as received
                    receivedEventDao.insertReceivedEvent(
                        ReceivedEventEntity(
                            eventId = event.eventId,
                            eventType = event.eventType.name,
                            orderId = event.orderId
                        )
                    )

                    // Parse kitchen order
                    val orderPayload = JSONObject(event.payloadJson)
                    val order = KitchenOrder.fromJson(orderPayload).copy(synced = true)

                    // Save order in local database
                    orderDao.saveFullKitchenOrder(order)
                    Log.i(TAG, "New Kitchen Order received and saved: #${order.orderNumber} (${order.orderId})")

                    // Respond with ACK to POS
                    val ack = OrderEvent.createAckEvent(event.eventId, order.orderId)
                    try {
                        originSocket.send(ack.toJson().toString())
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send ACK to POS", e)
                    }

                    // Alert sound and vibration
                    triggerAlerts()
                    _newOrderNotificationFlow.emit(order)
                }

                OrderEventType.ORDER_CANCELLED -> {
                    val payload = JSONObject(event.payloadJson)
                    val orderId = payload.optString("orderId", event.orderId)
                    orderDao.updateOrderStatus(orderId, OrderStatus.CANCELLED)

                    val ack = OrderEvent.createAckEvent(event.eventId, orderId)
                    try {
                        originSocket.send(ack.toJson().toString())
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send ACK for cancellation", e)
                    }
                }

                OrderEventType.PING -> {
                    val pong = OrderEvent(eventType = OrderEventType.PONG, orderId = "")
                    try {
                        originSocket.send(pong.toJson().toString())
                    } catch (_: Exception) {}
                }

                else -> {
                    Log.d(TAG, "Server received unhandled event: ${event.eventType}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling server message", e)
        }
    }

    suspend fun updateOrderStatusOnKds(orderId: String, newStatus: OrderStatus) {
        // 1. Update Room DB
        orderDao.updateOrderStatus(orderId, newStatus)

        // 2. Broadcast status change to all connected POS clients
        val event = OrderEvent.createStatusChangedEvent(orderId, newStatus)
        val sent = connectionManager.broadcastFromServer(event.toJson().toString())
        Log.i(TAG, "Broadcasted status update for $orderId -> $newStatus: $sent")
    }

    private fun triggerAlerts() {
        val settings = kdsSettingsRepository.settings.value
        if (settings.soundAlertEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            } catch (e: Exception) {
                Log.w(TAG, "Sound alert failed", e)
            }
        }

        if (settings.vibrateAlertEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(400)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Vibration alert failed", e)
            }
        }
    }
}
