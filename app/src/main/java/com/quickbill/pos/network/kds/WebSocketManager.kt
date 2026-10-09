/*
 * QuickBill + QuickKitchen
 *
 * Author: Dhivakar
 * Role: Android Developer
 *
 * Copyright (c) 2026 Dhivakar
 *
 * This file is part of the QuickBill + QuickKitchen project.
 * The original implementation and modifications in this file were
 * created by Dhivakar for the project/assignment.
 *
 * QuickBill-QuickKitchen-Author: Dhivakar
 *
 * Do not remove or alter this attribution notice.
 */

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

import com.quickbill.pos.data.model.kds.ConnectedPosTerminal
import org.json.JSONObject

class KdsWebSocketServer(
    port: Int,
    private val onMessageReceived: (message: String, fromSocket: WebSocket) -> Unit,
    private val onClientCountChanged: (count: Int) -> Unit,
    private val onTerminalsChanged: ((List<ConnectedPosTerminal>) -> Unit)? = null,
    private val onServerError: ((Exception) -> Unit)? = null
) : WebSocketServer(InetSocketAddress("0.0.0.0", port)) {

    companion object {
        private const val TAG = "KdsWebSocketServer"
        private const val CONNECTION_LOST_TIMEOUT_SECONDS = 4
    }

    init {
        // Aggressive heartbeat: detect severed P2P/Wi-Fi connection within 4 seconds
        connectionLostTimeout = CONNECTION_LOST_TIMEOUT_SECONDS
        isTcpNoDelay = true
    }

    private val connectedClients = Collections.newSetFromMap(ConcurrentHashMap<WebSocket, Boolean>())
    private val clientTerminalMap = ConcurrentHashMap<WebSocket, ConnectedPosTerminal>()

    @Volatile
    var isServerStopped: Boolean = false
        private set

    override fun onStart() {
        isServerStopped = false
        Log.i(TAG, "KDS WebSocket Server started on port $port")
    }

    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        if (conn != null) {
            val remoteAddr = conn.remoteSocketAddress
            val ip = remoteAddr?.address?.hostAddress ?: "Unknown"
            val clientPort = remoteAddr?.port ?: 0

            // Prune any existing stale socket with the exact same remote IP address
            val staleSockets = connectedClients.filter { existingConn ->
                existingConn != conn && existingConn.remoteSocketAddress?.address?.hostAddress == ip
            }
            for (stale in staleSockets) {
                Log.i(TAG, "Replacing previous connection from $ip")
                connectedClients.remove(stale)
                clientTerminalMap.remove(stale)
                try {
                    stale.close(1000, "Superseded by new connection from same host")
                } catch (_: Exception) {}
            }

            connectedClients.add(conn)
            val initialTerminal = ConnectedPosTerminal(
                id = "$ip:$clientPort",
                name = "POS Terminal",
                ipAddress = ip,
                port = clientPort,
                deviceModel = "",
                connectedAt = System.currentTimeMillis()
            )
            clientTerminalMap[conn] = initialTerminal
            val uniqueList = getDeduplicatedTerminals()
            Log.i(TAG, "POS Client connected from $ip:$clientPort. Unique clients: ${uniqueList.size}")
            onClientCountChanged(uniqueList.size)
            onTerminalsChanged?.invoke(uniqueList)
        }
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        if (conn != null) {
            connectedClients.remove(conn)
            clientTerminalMap.remove(conn)
            val uniqueList = getDeduplicatedTerminals()
            Log.i(TAG, "POS Client disconnected: $reason (code $code). Unique remaining: ${uniqueList.size}")
            onClientCountChanged(uniqueList.size)
            onTerminalsChanged?.invoke(uniqueList)
        }
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        if (conn != null && message != null) {
            Log.d(TAG, "Message received: $message")
            // Check if this message is a POS_HELLO handshake from the client
            try {
                val json = JSONObject(message)
                if (json.optString("eventType") == "POS_HELLO") {
                    val payloadStr = json.optString("payloadJson", "{}")
                    val payload = JSONObject(payloadStr)
                    val termName = payload.optString("terminalName", "POS Terminal")
                    val devModel = payload.optString("deviceModel", "")
                    val reportedIp = payload.optString("ipAddress", "").ifBlank {
                        conn.remoteSocketAddress?.address?.hostAddress ?: "Unknown"
                    }

                    // Deduplicate by deviceModel: if another socket belongs to the same hardware model, supersede it!
                    if (devModel.isNotBlank()) {
                        val staleEntries = clientTerminalMap.entries.filter { (existingConn, term) ->
                            existingConn != conn && term.deviceModel == devModel
                        }
                        for ((staleConn, _) in staleEntries) {
                            Log.i(TAG, "Superseding duplicate socket for device model $devModel")
                            connectedClients.remove(staleConn)
                            clientTerminalMap.remove(staleConn)
                            try {
                                staleConn.close(1000, "Superseded by re-registration of $devModel")
                            } catch (_: Exception) {}
                        }
                    }

                    val existing = clientTerminalMap[conn]
                    val updated = (existing ?: ConnectedPosTerminal(
                        id = "${conn.remoteSocketAddress?.address?.hostAddress}:${conn.remoteSocketAddress?.port}",
                        name = termName,
                        ipAddress = reportedIp,
                        port = conn.remoteSocketAddress?.port ?: 0
                    )).copy(
                        name = termName,
                        deviceModel = devModel,
                        ipAddress = reportedIp
                    )
                    clientTerminalMap[conn] = updated
                    Log.i(TAG, "Registered POS Terminal: ${updated.name} (${updated.deviceModel}) at ${updated.ipAddress}")
                    val uniqueList = getDeduplicatedTerminals()
                    onClientCountChanged(uniqueList.size)
                    onTerminalsChanged?.invoke(uniqueList)
                }
            } catch (_: Exception) {}

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
            clientTerminalMap.clear()
            onTerminalsChanged?.invoke(emptyList())
            stop(1000)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    /**
     * Deduplicates connected terminals by deviceModel (or IP address if model is blank)
     * so that the UI never displays duplicate entries for the same physical device.
     */
    fun getDeduplicatedTerminals(): List<ConnectedPosTerminal> {
        val uniqueMap = mutableMapOf<String, ConnectedPosTerminal>()
        clientTerminalMap.values.forEach { terminal ->
            val key = if (terminal.deviceModel.isNotBlank()) {
                terminal.deviceModel
            } else {
                terminal.ipAddress
            }
            val existing = uniqueMap[key]
            if (existing == null || terminal.connectedAt >= existing.connectedAt) {
                uniqueMap[key] = terminal
            }
        }
        return uniqueMap.values.toList()
    }

    /**
     * Closes and clears all client connections originating from the specified subnet (e.g. "192.168.49.").
     * Called when Wi-Fi Direct is stopped or disconnected.
     */
    fun closeClientsOnSubnet(subnetPrefix: String = "192.168.49.") {
        val toClose = connectedClients.filter { conn ->
            val ip = conn.remoteSocketAddress?.address?.hostAddress ?: ""
            ip.startsWith(subnetPrefix)
        }
        for (conn in toClose) {
            Log.i(TAG, "Pruning P2P client socket on subnet $subnetPrefix: ${conn.remoteSocketAddress}")
            connectedClients.remove(conn)
            clientTerminalMap.remove(conn)
            try {
                conn.close(1000, "P2P connection severed")
            } catch (_: Exception) {}
        }
        if (toClose.isNotEmpty()) {
            val uniqueList = getDeduplicatedTerminals()
            onClientCountChanged(uniqueList.size)
            onTerminalsChanged?.invoke(uniqueList)
        }
    }
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
        private const val CONNECTION_LOST_TIMEOUT_SECONDS = 4
    }

    init {
        // Fast connection lost detection: detect severed P2P within 4 seconds
        connectionLostTimeout = CONNECTION_LOST_TIMEOUT_SECONDS
        isTcpNoDelay = true
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
