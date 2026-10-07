package com.quickbill.pos.network.kds

import android.os.Build
import android.util.Log
import com.quickbill.pos.data.model.kds.ConnectedKdsScreen
import com.quickbill.pos.data.model.kds.ConnectedPosTerminal
import com.quickbill.pos.data.model.kds.OrderEvent
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
import java.util.concurrent.ConcurrentHashMap

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

private data class PosConnectionEntry(
    val id: String, // "host:port"
    val host: String,
    val port: Int,
    var name: String = "Kitchen Display",
    var client: PosWebSocketClient? = null,
    var status: ConnectionStatus = ConnectionStatus.CONNECTING,
    var hasConnectedOnce: Boolean = false,
    var connectedAt: Long = 0L,
    var userDisconnected: Boolean = false,
    var reconnectAttempts: Int = 0,
    var reconnectJob: Job? = null,
    var connectionWatchdogJob: Job? = null,
    var lastError: String = ""
)

class ConnectionManager {

    companion object {
        private const val TAG = "ConnectionManager"
        private const val BASE_RECONNECT_DELAY_MS = 1500L
        private const val MAX_RECONNECT_DELAY_MS = 10000L
        private const val PING_INTERVAL_MS = 15000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- POS Multi-KDS Client State ---
    private val connections = ConcurrentHashMap<String, PosConnectionEntry>()
    private var heartbeatJob: Job? = null
    private var currentHost: String = ""
    private var currentPort: Int = 8080

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedServerAddress = MutableStateFlow("")
    val connectedServerAddress: StateFlow<String> = _connectedServerAddress.asStateFlow()

    private val _connectedKdsScreens = MutableStateFlow<List<ConnectedKdsScreen>>(emptyList())
    val connectedKdsScreens: StateFlow<List<ConnectedKdsScreen>> = _connectedKdsScreens.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow("")
    val lastErrorMessage: StateFlow<String> = _lastErrorMessage.asStateFlow()

    // Stream of incoming messages received on POS client: Pair(message, originConnectionId)
    private val _clientIncomingMessages = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64)
    val clientIncomingMessages: SharedFlow<Pair<String, String>> = _clientIncomingMessages.asSharedFlow()

    // Event emitted whenever a KDS screen connects successfully (passes screen connectionId e.g. "host:port")
    private val _screenConnectedEvent = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val screenConnectedEvent: SharedFlow<String> = _screenConnectedEvent.asSharedFlow()

    // --- KDS Server State ---
    private var server: KdsWebSocketServer? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val _connectedPosTerminals = MutableStateFlow<List<ConnectedPosTerminal>>(emptyList())
    val connectedPosTerminals: StateFlow<List<ConnectedPosTerminal>> = _connectedPosTerminals.asStateFlow()

    // Stream of incoming messages received on KDS server with origin socket
    private val _serverIncomingMessages = MutableSharedFlow<Pair<String, WebSocket>>(extraBufferCapacity = 64)
    val serverIncomingMessages: SharedFlow<Pair<String, WebSocket>> = _serverIncomingMessages.asSharedFlow()

    fun getLastConnectedHost(): String = currentHost
    fun getLastConnectedPort(): Int = currentPort

    // ==========================================
    // KDS Server Methods
    // ==========================================
    fun startKdsServer(port: Int = 8080) {
        val currentServer = server
        if (_isServerRunning.value && currentServer != null && !currentServer.isServerStopped) {
            if (_serverPort.value == port) {
                Log.i(TAG, "KDS WebSocket Server already running on port $port")
                return
            }
            stopKdsServer()
        }

        try {
            _serverPort.value = port
            val newServer = KdsWebSocketServer(
                port = port,
                onMessageReceived = { message, socket ->
                    scope.launch {
                        _serverIncomingMessages.emit(Pair(message, socket))
                    }
                },
                onClientCountChanged = { count ->
                    _connectedClientsCount.value = count
                },
                onTerminalsChanged = { terminals ->
                    _connectedPosTerminals.value = terminals
                },
                onServerError = { ex ->
                    Log.e(TAG, "KDS WebSocket Server socket error on port $port: ${ex.message}", ex)
                    _isServerRunning.value = false
                    if (ex is java.net.BindException) {
                        Log.w(TAG, "Port $port still busy (TIME_WAIT). Retrying server bind in 500ms...")
                        scope.launch {
                            delay(500)
                            if (server != null && !_isServerRunning.value) {
                                startKdsServer(port)
                            }
                        }
                    }
                }
            ).apply {
                isReuseAddr = true
                start()
            }
            server = newServer
            _isServerRunning.value = true
            Log.i(TAG, "Started KDS WebSocket Server on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start KDS WebSocket Server on port $port", e)
            _isServerRunning.value = false
        }
    }

    fun stopKdsServer() {
        val oldServer = server
        server = null
        _isServerRunning.value = false
        _connectedClientsCount.value = 0
        _connectedPosTerminals.value = emptyList()
        if (oldServer != null) {
            try {
                oldServer.stopServer()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping KDS WebSocket Server", e)
            }
        }
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
    // POS Multi-Client Helper Methods
    // ==========================================

    private fun parseHostAndPort(rawHost: String, rawPort: Int): Pair<String, Int> {
        var host = rawHost.trim()
        if (host.startsWith("ws://", ignoreCase = true)) host = host.substring(5)
        else if (host.startsWith("wss://", ignoreCase = true)) host = host.substring(6)
        else if (host.startsWith("http://", ignoreCase = true)) host = host.substring(7)
        else if (host.startsWith("https://", ignoreCase = true)) host = host.substring(8)
        host = host.removePrefix("/")

        var port = rawPort
        if (host.contains(":") && !host.startsWith("[") && host.indexOf(":") == host.lastIndexOf(":")) {
            val parts = host.split(":")
            if (parts.size == 2 && parts[1].toIntOrNull() != null) {
                host = parts[0]
                port = parts[1].toInt()
            }
        }
        return Pair(host, if (port > 0) port else 8080)
    }

    private fun updateClientStateFlows() {
        val entries = connections.values.toList()
        val connectedList = entries.filter { it.status == ConnectionStatus.CONNECTED }

        // STRICT FILTER: Only expose actually CONNECTED screens to UI and outbox sync!
        _connectedKdsScreens.value = connectedList.map { entry ->
            ConnectedKdsScreen(
                id = entry.id,
                name = entry.name,
                host = entry.host,
                port = entry.port,
                status = entry.status.name,
                connectedAt = entry.connectedAt
            )
        }

        if (connectedList.isNotEmpty()) {
            _connectionStatus.value = ConnectionStatus.CONNECTED
            _connectedServerAddress.value = if (connectedList.size == 1) {
                "${connectedList[0].host}:${connectedList[0].port}"
            } else {
                "${connectedList.size} Screens Connected"
            }
        } else {
            val anyConnecting = entries.any {
                it.status == ConnectionStatus.CONNECTING || it.status == ConnectionStatus.RECONNECTING
            }
            if (anyConnecting) {
                _connectionStatus.value = ConnectionStatus.CONNECTING
            } else {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }
            _connectedServerAddress.value = ""
        }
    }

    fun isKdsConnected(rawHost: String, rawPort: Int = 8080): Boolean {
        val (host, port) = parseHostAndPort(rawHost, rawPort)
        val key = "$host:$port"
        return connections[key]?.status == ConnectionStatus.CONNECTED
    }

    fun isKdsConnecting(rawHost: String, rawPort: Int = 8080): Boolean {
        val (host, port) = parseHostAndPort(rawHost, rawPort)
        val key = "$host:$port"
        val status = connections[key]?.status
        return status == ConnectionStatus.CONNECTING || status == ConnectionStatus.RECONNECTING
    }

    // ==========================================
    // POS Multi-KDS Connection Methods
    // ==========================================
    fun connectToKds(rawHost: String, rawPort: Int = 8080, name: String = "Kitchen Display") {
        val (host, port) = parseHostAndPort(rawHost, rawPort)
        if (host.isBlank()) return

        currentHost = host
        currentPort = port
        val key = "$host:$port"

        val existing = connections[key]
        if (existing != null && existing.status == ConnectionStatus.CONNECTED) {
            Log.i(TAG, "Already connected to KDS at $key")
            return
        }

        val entry = existing?.apply {
            this.name = name
            this.userDisconnected = false
            this.reconnectAttempts = 0
            this.status = ConnectionStatus.CONNECTING
        } ?: PosConnectionEntry(
            id = key,
            host = host,
            port = port,
            name = name,
            status = ConnectionStatus.CONNECTING
        )

        connections[key] = entry
        _lastErrorMessage.value = ""
        updateClientStateFlows()

        initiateEntryConnection(entry)
    }

    fun disconnectFromKds(rawHost: String, rawPort: Int = 8080) {
        val (host, port) = parseHostAndPort(rawHost, rawPort)
        val key = "$host:$port"
        val entry = connections[key] ?: return

        entry.userDisconnected = true
        entry.reconnectJob?.cancel()
        entry.reconnectJob = null
        entry.connectionWatchdogJob?.cancel()
        entry.connectionWatchdogJob = null
        try {
            entry.client?.detachAndClose()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing client for $key", e)
        }
        entry.client = null
        connections.remove(key)
        updateClientStateFlows()
        Log.i(TAG, "Disconnected from KDS screen at $key")
    }

    fun reconnect() {
        if (connections.isNotEmpty()) {
            connections.values.forEach { entry ->
                entry.userDisconnected = false
                entry.reconnectAttempts = 0
                entry.reconnectJob?.cancel()
                entry.reconnectJob = null
                initiateEntryConnection(entry)
            }
        } else if (currentHost.isNotBlank()) {
            connectToKds(currentHost, currentPort)
        }
    }

    private fun initiateEntryConnection(entry: PosConnectionEntry) {
        if (entry.userDisconnected) return

        val formattedHost = if (entry.host.contains(":") && !entry.host.startsWith("[")) {
            "[${entry.host}]"
        } else {
            entry.host
        }
        val uriStr = "ws://$formattedHost:${entry.port}"
        val serverUri = try {
            URI(uriStr)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid URI: $uriStr", e)
            entry.lastError = "Invalid server address: $uriStr"
            entry.status = ConnectionStatus.DISCONNECTED
            _lastErrorMessage.value = "Invalid server address: $uriStr"
            connections.remove(entry.id)
            updateClientStateFlows()
            return
        }

        entry.status = if (entry.reconnectAttempts > 0) ConnectionStatus.RECONNECTING else ConnectionStatus.CONNECTING
        updateClientStateFlows()

        Log.i(TAG, "Connecting to KDS (${entry.name}) at $serverUri (attempt ${entry.reconnectAttempts})...")

        // 4.5-second connection watchdog: if initial connection doesn't succeed in time, fail cleanly
        entry.connectionWatchdogJob?.cancel()
        entry.connectionWatchdogJob = scope.launch {
            delay(4500L)
            if (!entry.hasConnectedOnce && entry.status != ConnectionStatus.CONNECTED && !entry.userDisconnected) {
                Log.w(TAG, "Connection attempt to ${entry.id} timed out after 4500ms")
                try {
                    entry.client?.detachAndClose()
                } catch (_: Exception) {}
                entry.client = null
                entry.reconnectJob?.cancel()
                connections.remove(entry.id)
                _lastErrorMessage.value = "Cannot reach ${entry.name} at ${entry.host}:${entry.port}. Device unreachable or IP does not exist."
                updateClientStateFlows()
            }
        }

        val newClient = PosWebSocketClient(
            serverUri = serverUri,
            onConnected = {
                entry.connectionWatchdogJob?.cancel()
                entry.connectionWatchdogJob = null
                entry.hasConnectedOnce = true
                entry.reconnectAttempts = 0
                entry.status = ConnectionStatus.CONNECTED
                entry.connectedAt = System.currentTimeMillis()
                entry.lastError = ""
                _lastErrorMessage.value = ""
                updateClientStateFlows()
                Log.i(TAG, "Successfully connected to KDS screen ${entry.name} at ${entry.id}")

                // Send POS_HELLO event handshake to register POS identity with KDS
                val helloEvent = OrderEvent.createPosHelloEvent(
                    terminalName = "QuickBill POS",
                    deviceModel = Build.MODEL ?: "Android POS",
                    ipAddress = ""
                )
                try {
                    entry.client?.send(helloEvent.toJson().toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send POS_HELLO to ${entry.id}", e)
                }

                // Notify listeners that this screen is newly connected so initial sync can take place
                scope.launch {
                    _screenConnectedEvent.emit(entry.id)
                }

                startHeartbeat()
            },
            onDisconnected = { details ->
                entry.connectionWatchdogJob?.cancel()
                entry.connectionWatchdogJob = null
                val userFriendlyError = when {
                    details.contains("ETIMEDOUT", ignoreCase = true) || details.contains("timed out", ignoreCase = true) ->
                        "Connection timed out to ${entry.name} (${entry.host}). IP may not exist."
                    details.contains("ECONNREFUSED", ignoreCase = true) || details.contains("refused", ignoreCase = true) ->
                        "Connection refused on port ${entry.port}. Verify QuickKitchen KDS is running."
                    details.contains("ECONNABORTED", ignoreCase = true) ->
                        "Connection aborted by network for ${entry.name}."
                    details.contains("Cleartext", ignoreCase = true) ->
                        "Cleartext traffic blocked by network security policy."
                    details.contains("No route", ignoreCase = true) || details.contains("unreachable", ignoreCase = true) ->
                        "Host unreachable (${entry.host}). IP does not exist on this network."
                    else -> details
                }
                entry.lastError = userFriendlyError
                _lastErrorMessage.value = userFriendlyError
                Log.w(TAG, "Disconnected from KDS ${entry.name}: $userFriendlyError")

                if (!entry.hasConnectedOnce) {
                    // Initial connection attempt never succeeded - do not enter reconnect loop!
                    try {
                        entry.client?.detachAndClose()
                    } catch (_: Exception) {}
                    entry.client = null
                    connections.remove(entry.id)
                    updateClientStateFlows()
                } else {
                    entry.status = ConnectionStatus.DISCONNECTED
                    updateClientStateFlows()
                    if (!entry.userDisconnected) {
                        scheduleEntryReconnect(entry)
                    }
                }
            },
            onMessageReceived = { msg ->
                scope.launch {
                    _clientIncomingMessages.emit(Pair(msg, entry.id))
                }
            },
            onErrorOccurred = { ex ->
                Log.e(TAG, "Client socket error for ${entry.id}: ${ex.message}")
                if (!entry.hasConnectedOnce) {
                    entry.connectionWatchdogJob?.cancel()
                    entry.connectionWatchdogJob = null
                    try {
                        entry.client?.detachAndClose()
                    } catch (_: Exception) {}
                    entry.client = null
                    connections.remove(entry.id)
                    val msg = ex.message ?: ""
                    val err = when {
                        msg.contains("refused", ignoreCase = true) -> "Connection refused on port ${entry.port}. Verify QuickKitchen KDS is running."
                        msg.contains("timed out", ignoreCase = true) || msg.contains("ETIMEDOUT", ignoreCase = true) -> "Connection timed out to ${entry.host}. IP does not exist."
                        msg.contains("unreachable", ignoreCase = true) || msg.contains("No route", ignoreCase = true) -> "Host unreachable (${entry.host}). IP does not exist."
                        else -> "Failed to connect to ${entry.host}:${entry.port}"
                    }
                    _lastErrorMessage.value = err
                    updateClientStateFlows()
                }
            }
        ).apply {
            setConnectionLostTimeout(15)
        }

        entry.client = newClient

        try {
            newClient.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Client connect exception for ${entry.id}", e)
            entry.connectionWatchdogJob?.cancel()
            entry.connectionWatchdogJob = null
            entry.lastError = e.message ?: "Failed to initiate connection"
            _lastErrorMessage.value = "Failed to connect to ${entry.host}:${entry.port}: ${e.message}"
            if (!entry.hasConnectedOnce) {
                connections.remove(entry.id)
                updateClientStateFlows()
            } else {
                scheduleEntryReconnect(entry)
            }
        }
    }

    private fun scheduleEntryReconnect(entry: PosConnectionEntry) {
        if (entry.userDisconnected) return

        entry.status = ConnectionStatus.RECONNECTING
        entry.reconnectJob?.cancel()

        entry.reconnectAttempts++
        val delayMs = (BASE_RECONNECT_DELAY_MS * (1 shl (entry.reconnectAttempts - 1).coerceAtMost(3)))
            .coerceAtMost(MAX_RECONNECT_DELAY_MS)

        Log.i(TAG, "Scheduling reconnect attempt ${entry.reconnectAttempts} in ${delayMs}ms to ${entry.id}")

        entry.reconnectJob = scope.launch {
            delay(delayMs)
            if (!entry.userDisconnected && isActive) {
                try {
                    entry.client?.detachAndClose()
                } catch (_: Exception) {}
                entry.client = null
                initiateEntryConnection(entry)
            }
        }
    }

    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (isActive && connections.values.any { it.status == ConnectionStatus.CONNECTED }) {
                delay(PING_INTERVAL_MS)
                connections.values.forEach { entry ->
                    val activeClient = entry.client
                    if (activeClient != null && activeClient.isOpen && !activeClient.isDetached) {
                        try {
                            activeClient.sendPing()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to send ping to ${entry.id}", e)
                        }
                    }
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun disconnectClient() {
        connections.values.forEach { entry ->
            entry.userDisconnected = true
            entry.reconnectJob?.cancel()
            entry.reconnectJob = null
            entry.connectionWatchdogJob?.cancel()
            entry.connectionWatchdogJob = null
            try {
                entry.client?.detachAndClose()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing client for ${entry.id}", e)
            }
            entry.client = null
        }
        connections.clear()
        stopHeartbeat()
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _connectedServerAddress.value = ""
        _connectedKdsScreens.value = emptyList()
    }

    /**
     * Broadcasts a message from POS to all connected KDS screens,
     * optionally excluding a specific screen (e.g. the origin of the event).
     * Returns true if sent to at least one KDS screen.
     */
    fun sendFromClient(message: String, excludeId: String = ""): Boolean {
        var deliveredToAny = false
        for (entry in connections.values) {
            if (excludeId.isNotBlank() && entry.id == excludeId) continue
            val activeClient = entry.client
            if (activeClient != null && activeClient.isOpen && !activeClient.isDetached) {
                try {
                    activeClient.send(message)
                    deliveredToAny = true
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending from client to ${entry.id}", e)
                }
            }
        }
        return deliveredToAny
    }

    /**
     * Sends a message directly to a specific connected KDS screen.
     */
    fun sendToClient(id: String, message: String): Boolean {
        val entry = connections[id] ?: return false
        val activeClient = entry.client
        if (activeClient != null && activeClient.isOpen && !activeClient.isDetached) {
            return try {
                activeClient.send(message)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to client $id", e)
                false
            }
        }
        return false
    }
}
