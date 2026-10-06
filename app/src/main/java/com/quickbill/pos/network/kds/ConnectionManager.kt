package com.quickbill.pos.network.kds

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.java_websocket.WebSocket
import java.net.URI

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

class ConnectionManager {

    companion object {
        private const val TAG = "ConnectionManager"
        private const val BASE_RECONNECT_DELAY_MS = 1500L
        private const val MAX_RECONNECT_DELAY_MS = 10000L
        private const val PING_INTERVAL_MS = 15000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- POS Client State ---
    private var client: PosWebSocketClient? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var currentHost: String = ""
    private var currentPort: Int = 8080
    private var userDisconnected: Boolean = false
    private var reconnectAttempts: Int = 0

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedServerAddress = MutableStateFlow("")
    val connectedServerAddress: StateFlow<String> = _connectedServerAddress.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow("")
    val lastErrorMessage: StateFlow<String> = _lastErrorMessage.asStateFlow()

    // Stream of incoming messages received on POS client
    private val _clientIncomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val clientIncomingMessages: SharedFlow<String> = _clientIncomingMessages.asSharedFlow()

    // --- KDS Server State ---
    private var server: KdsWebSocketServer? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    // Stream of incoming messages received on KDS server with origin socket
    private val _serverIncomingMessages = MutableSharedFlow<Pair<String, WebSocket>>(extraBufferCapacity = 64)
    val serverIncomingMessages: SharedFlow<Pair<String, WebSocket>> = _serverIncomingMessages.asSharedFlow()

    fun getLastConnectedHost(): String = currentHost
    fun getLastConnectedPort(): Int = currentPort

    // ==========================================
    // KDS Server Methods
    // ==========================================
    fun startKdsServer(port: Int = 8080) {
        if (_isServerRunning.value && server != null) {
            if (_serverPort.value == port) return
            stopKdsServer()
        }

        try {
            _serverPort.value = port
            server = KdsWebSocketServer(
                port = port,
                onMessageReceived = { message, socket ->
                    scope.launch {
                        _serverIncomingMessages.emit(Pair(message, socket))
                    }
                },
                onClientCountChanged = { count ->
                    _connectedClientsCount.value = count
                }
            ).apply {
                isReuseAddr = true
                start()
            }
            _isServerRunning.value = true
            Log.i(TAG, "Started KDS WebSocket Server on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start KDS WebSocket Server on port $port", e)
            _isServerRunning.value = false
        }
    }

    fun stopKdsServer() {
        try {
            server?.stopServer()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping KDS WebSocket Server", e)
        }
        server = null
        _isServerRunning.value = false
        _connectedClientsCount.value = 0
    }

    fun broadcastFromServer(message: String): Boolean {
        return try {
            server?.broadcastToAll(message)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting message from server", e)
            false
        }
    }

    // ==========================================
    // POS Client Methods
    // ==========================================
    fun connectToKds(rawHost: String, rawPort: Int = 8080) {
        var host = rawHost.trim()
        if (host.isBlank()) return

        // Strip scheme if user typed ws:// or http://
        if (host.startsWith("ws://", ignoreCase = true)) host = host.substring(5)
        else if (host.startsWith("wss://", ignoreCase = true)) host = host.substring(6)
        else if (host.startsWith("http://", ignoreCase = true)) host = host.substring(7)
        else if (host.startsWith("https://", ignoreCase = true)) host = host.substring(8)

        // Strip leading slash
        host = host.removePrefix("/")

        var port = rawPort
        // Check if host contains embedded port like 192.168.1.4:8080 (and not IPv6 with multiple colons)
        if (host.contains(":") && !host.startsWith("[") && host.indexOf(":") == host.lastIndexOf(":")) {
            val parts = host.split(":")
            if (parts.size == 2 && parts[1].toIntOrNull() != null) {
                host = parts[0]
                port = parts[1].toInt()
            }
        }

        userDisconnected = false
        currentHost = host
        currentPort = if (port > 0) port else 8080
        reconnectAttempts = 0
        _lastErrorMessage.value = ""

        disconnectClientInternal()
        initiateClientConnection()
    }

    fun reconnect() {
        if (currentHost.isNotBlank()) {
            userDisconnected = false
            reconnectAttempts = 0
            _lastErrorMessage.value = ""
            reconnectJob?.cancel()
            reconnectJob = null
            disconnectClientInternal()
            initiateClientConnection()
        }
    }

    private fun initiateClientConnection() {
        if (userDisconnected) return

        val formattedHost = if (currentHost.contains(":") && !currentHost.startsWith("[")) {
            "[$currentHost]"
        } else {
            currentHost
        }
        val uriStr = "ws://$formattedHost:$currentPort"
        val serverUri = try {
            URI(uriStr)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URI: $uriStr", e)
            _lastErrorMessage.value = "Invalid server address: $uriStr"
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            return
        }

        _connectionStatus.value = if (reconnectAttempts > 0) ConnectionStatus.RECONNECTING else ConnectionStatus.CONNECTING

        Log.i(TAG, "Connecting to KDS at $serverUri (attempt $reconnectAttempts)...")

        val newClient = PosWebSocketClient(
            serverUri = serverUri,
            onConnected = {
                reconnectAttempts = 0
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _connectedServerAddress.value = "$currentHost:$currentPort"
                _lastErrorMessage.value = ""
                Log.i(TAG, "Successfully connected to KDS at $currentHost:$currentPort")
                startHeartbeat()
            },
            onDisconnected = { details ->
                stopHeartbeat()
                _connectedServerAddress.value = ""
                
                val userFriendlyError = when {
                    details.contains("ETIMEDOUT", ignoreCase = true) ->
                        "Connection timed out. Verify both devices are on the same Wi-Fi and the IP ($currentHost) is reachable."
                    details.contains("ECONNREFUSED", ignoreCase = true) ->
                        "Connection refused on port $currentPort. Make sure QuickKitchen KDS is running on the target device."
                    details.contains("ECONNABORTED", ignoreCase = true) ->
                        "Connection aborted by network. Verify Wi-Fi network routing."
                    details.contains("Cleartext", ignoreCase = true) ->
                        "Cleartext traffic blocked by network security policy."
                    else -> details
                }
                _lastErrorMessage.value = userFriendlyError
                Log.w(TAG, "Disconnected from KDS: $userFriendlyError")

                if (!userDisconnected) {
                    scheduleReconnect()
                } else {
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                }
            },
            onMessageReceived = { msg ->
                scope.launch {
                    _clientIncomingMessages.emit(msg)
                }
            },
            onErrorOccurred = { ex ->
                stopHeartbeat()
                Log.e(TAG, "Client socket error: ${ex.message}")
            }
        ).apply {
            setConnectionLostTimeout(15)
        }

        client = newClient

        try {
            newClient.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Client connect exception", e)
            _lastErrorMessage.value = e.message ?: "Failed to initiate connection"
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (userDisconnected) return
        if (currentHost.isBlank()) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            return
        }

        _connectionStatus.value = ConnectionStatus.RECONNECTING
        reconnectJob?.cancel()

        reconnectAttempts++
        val delayMs = (BASE_RECONNECT_DELAY_MS * (1 shl (reconnectAttempts - 1).coerceAtMost(3)))
            .coerceAtMost(MAX_RECONNECT_DELAY_MS)

        Log.i(TAG, "Scheduling reconnect attempt $reconnectAttempts in ${delayMs}ms to $currentHost:$currentPort")

        reconnectJob = scope.launch {
            delay(delayMs)
            if (!userDisconnected && isActive) {
                disconnectClientInternal()
                initiateClientConnection()
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _connectionStatus.value == ConnectionStatus.CONNECTED) {
                delay(PING_INTERVAL_MS)
                try {
                    client?.sendPing()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send ping", e)
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun disconnectClient() {
        userDisconnected = true
        disconnectClientInternal()
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _connectedServerAddress.value = ""
    }

    private fun disconnectClientInternal() {
        reconnectJob?.cancel()
        reconnectJob = null
        stopHeartbeat()

        try {
            client?.detachAndClose()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing client", e)
        }
        client = null
    }

    fun sendFromClient(message: String): Boolean {
        val activeClient = client
        return if (activeClient != null && activeClient.isOpen && !activeClient.isDetached) {
            try {
                activeClient.send(message)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error sending from client", e)
                false
            }
        } else {
            false
        }
    }
}
