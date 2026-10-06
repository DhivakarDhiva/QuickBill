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
        private const val BASE_RECONNECT_DELAY_MS = 2000L
        private const val MAX_RECONNECT_DELAY_MS = 16000L
        private const val PING_INTERVAL_MS = 15000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- POS Client State ---
    private var client: PosWebSocketClient? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var currentHost: String = ""
    private var currentPort: Int = 8887
    private var userDisconnected: Boolean = false
    private var reconnectAttempts: Int = 0

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedServerAddress = MutableStateFlow("")
    val connectedServerAddress: StateFlow<String> = _connectedServerAddress.asStateFlow()

    // Stream of incoming messages received on POS client
    private val _clientIncomingMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val clientIncomingMessages: SharedFlow<String> = _clientIncomingMessages.asSharedFlow()

    // --- KDS Server State ---
    private var server: KdsWebSocketServer? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverPort = MutableStateFlow(8887)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    // Stream of incoming messages received on KDS server with origin socket
    private val _serverIncomingMessages = MutableSharedFlow<Pair<String, WebSocket>>(extraBufferCapacity = 64)
    val serverIncomingMessages: SharedFlow<Pair<String, WebSocket>> = _serverIncomingMessages.asSharedFlow()

    // ==========================================
    // KDS Server Methods
    // ==========================================
    fun startKdsServer(port: Int = 8887) {
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
            server?.stop()
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
    fun connectToKds(host: String, port: Int = 8887) {
        if (host.isBlank()) return
        userDisconnected = false
        currentHost = host
        currentPort = port
        reconnectAttempts = 0

        disconnectClientInternal()
        initiateClientConnection()
    }

    private fun initiateClientConnection() {
        if (userDisconnected) return

        val uriStr = "ws://$currentHost:$currentPort"
        val serverUri = try {
            URI(uriStr)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URI: $uriStr", e)
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            return
        }

        _connectionStatus.value = if (reconnectAttempts > 0) ConnectionStatus.RECONNECTING else ConnectionStatus.CONNECTING

        client = PosWebSocketClient(
            serverUri = serverUri,
            onConnected = {
                reconnectAttempts = 0
                _connectionStatus.value = ConnectionStatus.CONNECTED
                _connectedServerAddress.value = "$currentHost:$currentPort"
                startHeartbeat()
            },
            onDisconnected = { _ ->
                stopHeartbeat()
                _connectedServerAddress.value = ""
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
            onErrorOccurred = { _ ->
                stopHeartbeat()
                if (!userDisconnected) {
                    scheduleReconnect()
                } else {
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                }
            }
        )

        try {
            client?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Client connect exception", e)
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (userDisconnected) return
        _connectionStatus.value = ConnectionStatus.RECONNECTING
        reconnectJob?.cancel()

        reconnectAttempts++
        val delayMs = (BASE_RECONNECT_DELAY_MS * (1 shl (reconnectAttempts - 1).coerceAtMost(3)))
            .coerceAtMost(MAX_RECONNECT_DELAY_MS)

        Log.i(TAG, "Scheduling reconnect attempt $reconnectAttempts in ${delayMs}ms to $currentHost:$currentPort")

        reconnectJob = scope.launch {
            delay(delayMs)
            if (!userDisconnected && isActive) {
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
            client?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing client", e)
        }
        client = null
    }

    fun sendFromClient(message: String): Boolean {
        val activeClient = client
        return if (activeClient != null && activeClient.isOpen) {
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
