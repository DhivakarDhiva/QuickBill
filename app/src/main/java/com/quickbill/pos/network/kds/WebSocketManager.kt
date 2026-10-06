package com.quickbill.pos.network.kds

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.URI
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class KdsWebSocketServer(
    port: Int,
    private val onMessageReceived: (message: String, fromSocket: WebSocket) -> Unit,
    private val onClientCountChanged: (count: Int) -> Unit,
    private val onServerError: ((Exception) -> Unit)? = null
) : WebSocketServer(InetSocketAddress("0.0.0.0", port)) {

    companion object {
        private const val TAG = "KdsWebSocketServer"
    }

    private val connectedClients = Collections.newSetFromMap(ConcurrentHashMap<WebSocket, Boolean>())
    @Volatile
    var isServerStopped: Boolean = false
        private set

    override fun onStart() {
        isServerStopped = false
        Log.i(TAG, "KDS WebSocket Server started on port $port")
    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        if (conn != null) {
            connectedClients.add(conn)
            Log.i(TAG, "POS Client connected from ${conn.remoteSocketAddress}. Total clients: ${connectedClients.size}")
            onClientCountChanged(connectedClients.size)
        }
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        if (conn != null) {
            connectedClients.remove(conn)
            Log.i(TAG, "POS Client disconnected: $reason (code $code). Remaining: ${connectedClients.size}")
            onClientCountChanged(connectedClients.size)
        }
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        if (conn != null && message != null) {
            Log.d(TAG, "Message received: $message")
            onMessageReceived(message, conn)
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        Log.e(TAG, "Server error on connection ${conn?.remoteSocketAddress}", ex)
        if (conn == null && ex != null) {
            onServerError?.invoke(ex)
        }
    }

    fun broadcastToAll(message: String) {
        broadcast(message)
    }

    fun stopServer() {
        isServerStopped = true
        try {
            connectedClients.forEach { conn ->
                try {
                    conn.close(1000, "Server stopping")
                } catch (_: Exception) {}
            }
            connectedClients.clear()
            stop(1000)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    fun getClientCount(): Int = connectedClients.size
}

class PosWebSocketClient(
    serverUri: URI,
    private val onConnected: () -> Unit,
    private val onDisconnected: (reason: String) -> Unit,
    private val onMessageReceived: (message: String) -> Unit,
    private val onErrorOccurred: (Exception) -> Unit
) : WebSocketClient(serverUri) {

    companion object {
        private const val TAG = "PosWebSocketClient"
    }

    var isDetached: Boolean = false
        private set

    fun detachAndClose() {
        isDetached = true
        try {
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing PosWebSocketClient", e)
        }
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        if (isDetached) return
        Log.i(TAG, "Connected to KDS server at $uri")
        onConnected()
    }

    override fun onMessage(message: String?) {
        if (isDetached) return
        if (message != null) {
            Log.d(TAG, "Received message from KDS: $message")
            onMessageReceived(message)
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        if (isDetached) return
        val details = "Code: $code, Reason: ${reason ?: "Unknown"}, Remote: $remote"
        Log.i(TAG, "Connection closed to KDS: $details")
        onDisconnected(details)
    }

    override fun onError(ex: Exception?) {
        if (isDetached) return
        Log.e(TAG, "WebSocket client error", ex)
        onErrorOccurred(ex ?: Exception("Unknown error"))
    }
}
