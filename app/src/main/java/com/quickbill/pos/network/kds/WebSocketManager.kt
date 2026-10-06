package com.quickbill.pos.network.kds

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val onClientCountChanged: (count: Int) -> Unit
) : WebSocketServer(InetSocketAddress(port)) {

    companion object {
        private const val TAG = "KdsWebSocketServer"
    }

    private val connectedClients = Collections.newSetFromMap(ConcurrentHashMap<WebSocket, Boolean>())

    override fun onStart() {
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
    }

    fun broadcastToAll(message: String) {
        broadcast(message)
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

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.i(TAG, "Connected to KDS server at $uri")
        onConnected()
    }

    override fun onMessage(message: String?) {
        if (message != null) {
            Log.d(TAG, "Received message from KDS: $message")
            onMessageReceived(message)
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        val details = "Code: $code, Reason: ${reason ?: "Unknown"}, Remote: $remote"
        Log.i(TAG, "Connection closed to KDS: $details")
        onDisconnected(details)
    }

    override fun onError(ex: Exception?) {
        Log.e(TAG, "WebSocket client error", ex)
        onErrorOccurred(ex ?: Exception("Unknown error"))
    }
}
