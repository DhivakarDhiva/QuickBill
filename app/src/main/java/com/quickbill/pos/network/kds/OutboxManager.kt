package com.quickbill.pos.network.kds

import android.util.Log
import com.quickbill.pos.data.local.dao.PendingEventDao
import com.quickbill.pos.data.local.entity.PendingEventEntity
import com.quickbill.pos.data.model.kds.OrderEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OutboxManager(
    private val pendingEventDao: PendingEventDao
) {
    companion object {
        private const val TAG = "OutboxManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val drainMutex = Mutex()

    val pendingCountFlow: Flow<Int> = pendingEventDao.getPendingCountFlow()
    val allPendingEventsFlow: Flow<List<PendingEventEntity>> = pendingEventDao.getAllPendingEventsFlow()

    suspend fun enqueueEvent(event: OrderEvent) {
        val entity = PendingEventEntity(
            eventId = event.eventId,
            eventType = event.eventType,
            orderId = event.orderId,
            payloadJson = event.toJson().toString(),
            createdAt = event.timestamp,
            retryCount = 0,
            status = "PENDING"
        )
        pendingEventDao.insertPendingEvent(entity)
        Log.i(TAG, "Enqueued pending event ${event.eventId} (${event.eventType}) for order ${event.orderId}")
    }

    suspend fun onEventAcked(eventId: String) {
        pendingEventDao.deletePendingEvent(eventId)
        Log.i(TAG, "Removed ACKed event from outbox: $eventId")
    }

    suspend fun drainOutbox(sendAction: suspend (String) -> Boolean): Int {
        return drainMutex.withLock {
            val pendingList = pendingEventDao.getAllPendingEvents()
            if (pendingList.isEmpty()) return@withLock 0

            Log.i(TAG, "Draining outbox (${pendingList.size} events)...")
            var sentCount = 0

            for (entity in pendingList) {
                val success = try {
                    sendAction(entity.payloadJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send pending event ${entity.eventId}", e)
                    false
                }

                if (success) {
                    sentCount++
                    // We keep it until ACK is received or remove if immediate delivery is assumed.
                    // To follow strict at-least-once with ACK, we can increment or wait for ACK.
                    // For responsiveness, if send returned true, we can mark as SENDING or wait for ACK.
                } else {
                    pendingEventDao.incrementRetryCount(entity.eventId, "FAILED")
                    // If network fails mid-stream, stop loop to preserve in-order delivery
                    break
                }
            }
            sentCount
        }
    }

    suspend fun getPendingCount(): Int {
        return pendingEventDao.getPendingCount()
    }

    suspend fun clearOutbox() {
        pendingEventDao.clearAllPendingEvents()
    }
}
