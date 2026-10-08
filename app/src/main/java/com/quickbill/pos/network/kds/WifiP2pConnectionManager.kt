package com.quickbill.pos.network.kds

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class P2pPeerDevice(
    val deviceName: String,
    val deviceAddress: String,
    val status: Int,
    val isGroupOwner: Boolean = false
) {
    val statusLabel: String
        get() = when (status) {
            WifiP2pDevice.CONNECTED -> "Connected"
            WifiP2pDevice.INVITED -> "Invited"
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Unknown"
        }
}

data class P2pConnectionState(
    val isConnected: Boolean = false,
    val isGroupOwner: Boolean = false,
    val groupOwnerAddress: String? = null,
    val groupNetworkName: String? = null,
    val clientCount: Int = 0
)

class WifiP2pConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "WifiP2pConnMgr"
        const val P2P_DEFAULT_PORT = 8080
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    private var channel: WifiP2pManager.Channel? = null
    private var isReceiverRegistered = false

    // State flows
    private val _isP2pEnabled = MutableStateFlow(false)
    val isP2pEnabled: StateFlow<Boolean> = _isP2pEnabled.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<P2pPeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<P2pPeerDevice>> = _discoveredPeers.asStateFlow()

    private val _connectionState = MutableStateFlow(P2pConnectionState())
    val connectionState: StateFlow<P2pConnectionState> = _connectionState.asStateFlow()

    private val _thisDevice = MutableStateFlow<P2pPeerDevice?>(null)
    val thisDevice: StateFlow<P2pPeerDevice?> = _thisDevice.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private val p2pReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val enabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    _isP2pEnabled.value = enabled
                    Log.d(TAG, "Wi-Fi P2P state changed: enabled=$enabled")
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d(TAG, "Wi-Fi P2P peers changed, requesting peers...")
                    requestPeers()
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    Log.d(TAG, "Wi-Fi P2P connection changed")
                    @Suppress("DEPRECATION")
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true) {
                        requestConnectionInfo()
                        requestGroupInfo()
                    } else {
                        Log.d(TAG, "Wi-Fi P2P disconnected")
                        _connectionState.value = P2pConnectionState(isConnected = false)
                    }
                }

                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    @Suppress("DEPRECATION")
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                    } else {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }
                    device?.let {
                        _thisDevice.value = P2pPeerDevice(
                            deviceName = it.deviceName ?: "Unknown Device",
                            deviceAddress = it.deviceAddress ?: "",
                            status = it.status,
                            isGroupOwner = it.isGroupOwner
                        )
                        Log.d(TAG, "This device changed: ${it.deviceName} (${it.deviceAddress})")
                    }
                }
            }
        }
    }

    init {
        initializeChannel()
    }

    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun initializeChannel() {
        if (manager == null) {
            Log.w(TAG, "WifiP2pManager is not supported on this device")
            return
        }
        if (channel == null) {
            try {
                channel = manager?.initialize(context, context.mainLooper) {
                    Log.w(TAG, "WifiP2p channel disconnected, reinitializing...")
                    channel = null
                    initializeChannel()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize WifiP2pManager channel", e)
            }
        }
        registerReceiver()
    }

    fun registerReceiver() {
        if (!isReceiverRegistered && channel != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(p2pReceiver, intentFilter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(p2pReceiver, intentFilter)
                }
                isReceiverRegistered = true
                Log.d(TAG, "P2P broadcast receiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register P2P receiver", e)
            }
        }
    }

    fun unregisterReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(p2pReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "P2P broadcast receiver unregistered")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister P2P receiver", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startPeerDiscovery(
        onStarted: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!hasRequiredPermissions()) {
            val err = "Missing required Wi-Fi permissions"
            _lastError.value = err
            onError(err)
            return
        }
        val mgr = manager ?: run {
            val err = "Wi-Fi Direct is not supported"
            _lastError.value = err
            onError(err)
            return
        }
        val ch = channel ?: run {
            val err = "Wi-Fi Direct channel not ready"
            _lastError.value = err
            onError(err)
            return
        }

        mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Wi-Fi P2P discoverPeers started successfully")
                _isDiscovering.value = true
                _lastError.value = null
                onStarted()
            }

            override fun onFailure(reasonCode: Int) {
                val err = "Discovery failed: ${getReasonString(reasonCode)}"
                Log.w(TAG, err)
                _isDiscovering.value = false
                _lastError.value = err
                onError(err)
            }
        })
    }

    fun stopPeerDiscovery() {
        val mgr = manager ?: return
        val ch = channel ?: return
        mgr.stopPeerDiscovery(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _isDiscovering.value = false
                Log.d(TAG, "Wi-Fi P2P discovery stopped")
            }
            override fun onFailure(reasonCode: Int) {
                _isDiscovering.value = false
                Log.w(TAG, "stopPeerDiscovery failed: ${getReasonString(reasonCode)}")
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestPeers() {
        if (!hasRequiredPermissions()) return
        val mgr = manager ?: return
        val ch = channel ?: return

        mgr.requestPeers(ch) { peerList: WifiP2pDeviceList? ->
            val peers = peerList?.deviceList?.map { device ->
                P2pPeerDevice(
                    deviceName = device.deviceName?.ifBlank { "Kitchen / POS Device" } ?: "Kitchen / POS Device",
                    deviceAddress = device.deviceAddress ?: "",
                    status = device.status,
                    isGroupOwner = device.isGroupOwner
                )
            } ?: emptyList()

            Log.d(TAG, "Discovered ${peers.size} P2P peers")
            _discoveredPeers.value = peers
        }
    }

    private fun requestConnectionInfo() {
        val mgr = manager ?: return
        val ch = channel ?: return

        mgr.requestConnectionInfo(ch) { info: WifiP2pInfo? ->
            if (info == null) return@requestConnectionInfo

            val isGroupOwner = info.isGroupOwner
            val groupOwnerIp = info.groupOwnerAddress?.hostAddress

            Log.i(TAG, "P2P connection info: isConnected=${info.groupFormed}, isGroupOwner=$isGroupOwner, groupOwnerIp=$groupOwnerIp")

            _connectionState.value = _connectionState.value.copy(
                isConnected = info.groupFormed,
                isGroupOwner = isGroupOwner,
                groupOwnerAddress = groupOwnerIp
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestGroupInfo() {
        if (!hasRequiredPermissions()) return
        val mgr = manager ?: return
        val ch = channel ?: return

        mgr.requestGroupInfo(ch) { group: WifiP2pGroup? ->
            if (group != null) {
                val groupName = group.networkName
                val clientCount = group.clientList?.size ?: 0
                Log.d(TAG, "P2P group info: networkName=$groupName, clients=$clientCount")
                _connectionState.value = _connectionState.value.copy(
                    groupNetworkName = groupName,
                    clientCount = clientCount
                )
            }
        }
    }

    /**
     * KDS Mode: Creates an autonomous Wi-Fi Direct group so that KDS acts as Group Owner
     * with fixed IP 192.168.49.1. POS devices can then connect directly without needing an external router.
     */
    @SuppressLint("MissingPermission")
    fun createAutonomousGroup(
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (!hasRequiredPermissions()) {
            val err = "Missing Wi-Fi permissions to create P2P group"
            _lastError.value = err
            onFailure(err)
            return
        }
        val mgr = manager ?: run {
            val err = "Wi-Fi Direct not available"
            _lastError.value = err
            onFailure(err)
            return
        }
        val ch = channel ?: run {
            val err = "Channel not ready"
            _lastError.value = err
            onFailure(err)
            return
        }

        mgr.createGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Wi-Fi Direct P2P Group created successfully!")
                _lastError.value = null
                requestConnectionInfo()
                requestGroupInfo()
                onSuccess()
            }

            override fun onFailure(reasonCode: Int) {
                val err = "Failed to create P2P group: ${getReasonString(reasonCode)}"
                Log.w(TAG, err)
                _lastError.value = err
                onFailure(err)
            }
        })
    }

    /**
     * POS Mode: Connect to a discovered KDS peer device.
     */
    @SuppressLint("MissingPermission")
    fun connectToPeer(
        deviceAddress: String,
        isGroupOwnerPreferred: Boolean = false,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (!hasRequiredPermissions()) {
            val err = "Missing Wi-Fi permissions to connect"
            _lastError.value = err
            onFailure(err)
            return
        }
        val mgr = manager ?: return
        val ch = channel ?: return

        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            wps.setup = WpsInfo.PBC // Push-button configuration
            groupOwnerIntent = if (isGroupOwnerPreferred) 15 else 0
        }

        mgr.connect(ch, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Initiated P2P connection to $deviceAddress")
                _lastError.value = null
                onSuccess()
            }

            override fun onFailure(reasonCode: Int) {
                val err = "Failed to connect to peer: ${getReasonString(reasonCode)}"
                Log.w(TAG, err)
                _lastError.value = err
                onFailure(err)
            }
        })
    }

    /**
     * Disconnects and removes any active group/connection.
     */
    fun removeGroup(onComplete: () -> Unit = {}) {
        val mgr = manager ?: run {
            onComplete()
            return
        }
        val ch = channel ?: run {
            onComplete()
            return
        }

        mgr.removeGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "P2P group removed")
                _connectionState.value = P2pConnectionState(isConnected = false)
                onComplete()
            }

            override fun onFailure(reasonCode: Int) {
                Log.w(TAG, "Failed to remove P2P group: ${getReasonString(reasonCode)}")
                // Still reset state
                _connectionState.value = P2pConnectionState(isConnected = false)
                onComplete()
            }
        })
    }

    private fun getReasonString(reasonCode: Int): String {
        return when (reasonCode) {
            WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
            WifiP2pManager.ERROR -> "Internal Error"
            WifiP2pManager.BUSY -> "Framework Busy"
            else -> "Unknown error ($reasonCode)"
        }
    }
}
