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

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
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
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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
            WifiP2pDevice.INVITED -> "Connecting..."
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Available"
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
        const val DEFAULT_GROUP_OWNER_IP = "192.168.49.1"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val manager: WifiP2pManager? by lazy {
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    }

    private val wifiManager: WifiManager? by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }

    private val locationManager: LocationManager? by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    private var channel: WifiP2pManager.Channel? = null
    private var isReceiverRegistered = false

    // Background jobs for discovery keep-alive and polling
    private var peerPollingJob: Job? = null
    private var keepAliveJob: Job? = null
    private var infoPollingJob: Job? = null

    // State flows
    private val _isP2pEnabled = MutableStateFlow(false)
    val isP2pEnabled: StateFlow<Boolean> = _isP2pEnabled.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _isGroupCreating = MutableStateFlow(false)
    val isGroupCreating: StateFlow<Boolean> = _isGroupCreating.asStateFlow()

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
                    Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION: enabled=$enabled")
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION: requesting peers...")
                    requestPeers()
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION")
                    @Suppress("DEPRECATION")
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    val isConnected = networkInfo?.isConnected == true
                    Log.d(TAG, "Connection changed: networkInfo.isConnected=$isConnected")
                    if (isConnected) {
                        requestConnectionInfo()
                        requestGroupInfo()
                    } else {
                        // Group or client link dropped - query group info to sync client count or disbandment
                        requestGroupInfo()
                        if (!_connectionState.value.isGroupOwner) {
                            _connectionState.value = P2pConnectionState(isConnected = false)
                        }
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
                            deviceName = it.deviceName?.ifBlank { "This Device" } ?: "This Device",
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
        // Pre-initialize P2P enabled from WifiManager if available
        _isP2pEnabled.value = isWifiEnabled()
        initializeChannel()
    }

    fun isWifiEnabled(): Boolean {
        return try {
            wifiManager?.isWifiEnabled == true
        } catch (e: Exception) {
            true
        }
    }

    fun isLocationEnabled(): Boolean {
        // Android 13+ with neverForLocation does not require Location Services to be toggled on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        val lm = locationManager ?: return true
        return try {
            LocationManagerCompat.isLocationEnabled(lm)
        } catch (e: Exception) {
            true
        }
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    fun hasRequiredPermissions(): Boolean {
        val perms = getRequiredPermissions()
        return perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks all conditions needed for P2P operation and returns an error description if invalid.
     */
    fun checkPreconditions(): String? {
        if (!isWifiEnabled()) {
            return "Wi-Fi is turned off. Please turn ON Wi-Fi in settings."
        }
        if (!hasRequiredPermissions()) {
            return "Required Wi-Fi permissions not granted."
        }
        if (!isLocationEnabled()) {
            return "Location is turned off. Android requires Location to be ON for device discovery."
        }
        return null
    }

    fun initializeChannel() {
        if (manager == null) {
            Log.w(TAG, "WifiP2pManager is not supported on this device")
            return
        }
        if (channel == null) {
            try {
                channel = manager?.initialize(context, context.mainLooper) {
                    Log.w(TAG, "WifiP2p channel lost/disconnected, reinitializing...")
                    channel = null
                    scope.launch {
                        delay(500)
                        initializeChannel()
                    }
                }
                Log.d(TAG, "WifiP2p channel initialized successfully")
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
                    context.registerReceiver(p2pReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(p2pReceiver, intentFilter)
                }
                isReceiverRegistered = true
                Log.d(TAG, "P2P broadcast receiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register P2P receiver with RECEIVER_NOT_EXPORTED, trying fallback", e)
                try {
                    context.registerReceiver(p2pReceiver, intentFilter)
                    isReceiverRegistered = true
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed fallback P2P receiver registration", e2)
                }
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

    // =========================================================================
    // POS Peer Discovery
    // =========================================================================

    @SuppressLint("MissingPermission")
    fun startPeerDiscovery(
        onStarted: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val preErr = checkPreconditions()
        if (preErr != null) {
            _lastError.value = preErr
            onError(preErr)
            return
        }
        val mgr = manager ?: run {
            val err = "Wi-Fi Direct not supported on this device"
            _lastError.value = err
            onError(err)
            return
        }
        val ch = channel ?: run {
            initializeChannel()
            val err = "Reinitializing Wi-Fi channel. Please tap Scan again in a moment."
            _lastError.value = err
            onError(err)
            return
        }

        _isDiscovering.value = true
        _lastError.value = null

        // First stop any ongoing discovery to clear BUSY state
        mgr.stopPeerDiscovery(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                executeDiscoverPeers(mgr, ch, onStarted, onError)
            }

            override fun onFailure(reasonCode: Int) {
                // Proceed anyway even if stop failed
                executeDiscoverPeers(mgr, ch, onStarted, onError)
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun executeDiscoverPeers(
        mgr: WifiP2pManager,
        ch: WifiP2pManager.Channel,
        onStarted: () -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            delay(150)
            mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.i(TAG, "discoverPeers started successfully")
                    _isDiscovering.value = true
                    _lastError.value = null
                    // Immediately request peers to populate cache
                    requestPeers()
                    startPeriodicPeerPolling()
                    onStarted()
                }

                override fun onFailure(reasonCode: Int) {
                    Log.w(TAG, "discoverPeers failed: ${getReasonString(reasonCode)} ($reasonCode)")
                    if (reasonCode == WifiP2pManager.BUSY) {
                        // Retry once after 500ms
                        scope.launch {
                            delay(500)
                            mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                                override fun onSuccess() {
                                    _isDiscovering.value = true
                                    _lastError.value = null
                                    requestPeers()
                                    startPeriodicPeerPolling()
                                    onStarted()
                                }

                                override fun onFailure(code: Int) {
                                    _isDiscovering.value = false
                                    val err = "Scan failed: ${getReasonString(code)}"
                                    _lastError.value = err
                                    onError(err)
                                }
                            })
                        }
                    } else {
                        _isDiscovering.value = false
                        val err = "Scan failed: ${getReasonString(reasonCode)}"
                        _lastError.value = err
                        onError(err)
                    }
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    private fun startPeriodicPeerPolling() {
        peerPollingJob?.cancel()
        peerPollingJob = scope.launch {
            var cycles = 0
            while (isActive && _isDiscovering.value) {
                delay(2000)
                requestPeers()
                cycles++
                // Android discoverPeers stops after ~60-120s, refresh discovery every 20 seconds
                if (cycles % 10 == 0) {
                    val mgr = manager
                    val ch = channel
                    if (mgr != null && ch != null && hasRequiredPermissions()) {
                        mgr.discoverPeers(ch, null)
                    }
                }
            }
        }
    }

    fun stopPeerDiscovery() {
        peerPollingJob?.cancel()
        peerPollingJob = null
        _isDiscovering.value = false
        val mgr = manager ?: return
        val ch = channel ?: return
        mgr.stopPeerDiscovery(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "stopPeerDiscovery succeeded")
            }
            override fun onFailure(reasonCode: Int) {
                Log.d(TAG, "stopPeerDiscovery: ${getReasonString(reasonCode)}")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun requestPeers() {
        if (!hasRequiredPermissions()) return
        val mgr = manager ?: return
        val ch = channel ?: return

        try {
            mgr.requestPeers(ch) { peerList: WifiP2pDeviceList? ->
                val peers = peerList?.deviceList?.map { device ->
                    P2pPeerDevice(
                        deviceName = device.deviceName?.ifBlank { "Kitchen / POS Device" } ?: "Kitchen / POS Device",
                        deviceAddress = device.deviceAddress ?: "",
                        status = device.status,
                        isGroupOwner = device.isGroupOwner
                    )
                } ?: emptyList()

                Log.d(TAG, "requestPeers returned ${peers.size} devices")
                _discoveredPeers.value = peers
            }
        } catch (e: Exception) {
            Log.w(TAG, "requestPeers exception", e)
        }
    }

    // =========================================================================
    // KDS Autonomous Group Owner & Keep-Alive Discovery
    // =========================================================================

    /**
     * KDS Mode: Creates an autonomous Wi-Fi Direct group so KDS acts as Group Owner
     * with standard IP 192.168.49.1.
     * Before creating, clears any existing group to prevent BUSY (2) errors.
     */
    @SuppressLint("MissingPermission")
    fun createAutonomousGroup(
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val preErr = checkPreconditions()
        if (preErr != null) {
            _lastError.value = preErr
            onFailure(preErr)
            return
        }
        val mgr = manager ?: run {
            val err = "Wi-Fi Direct not supported on this device"
            _lastError.value = err
            onFailure(err)
            return
        }
        val ch = channel ?: run {
            initializeChannel()
            val err = "Initializing Wi-Fi channel. Please tap Start P2P again."
            _lastError.value = err
            onFailure(err)
            return
        }

        _isGroupCreating.value = true
        _lastError.value = null
        stopKeepAliveDiscovery()

        // Direct call to createGroup - do not flood the framework with pre-commands
        callCreateGroup(mgr, ch, attempt = 1, onSuccess, onFailure)
    }

    @SuppressLint("MissingPermission")
    private fun callCreateGroup(
        mgr: WifiP2pManager,
        ch: WifiP2pManager.Channel,
        attempt: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        mgr.createGroup(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Autonomous P2P Group created successfully!")
                _isGroupCreating.value = false
                _lastError.value = null
                _connectionState.value = P2pConnectionState(
                    isConnected = true,
                    isGroupOwner = true,
                    groupOwnerAddress = DEFAULT_GROUP_OWNER_IP,
                    groupNetworkName = "QuickKitchen-KDS"
                )
                pollConnectionInfo()
                onSuccess()
            }

            override fun onFailure(reasonCode: Int) {
                Log.w(TAG, "createGroup attempt $attempt returned $reasonCode")
                if (reasonCode == WifiP2pManager.BUSY && attempt < 3) {
                    // Framework is busy. Clean up any stale group and retry after 1.5s
                    mgr.removeGroup(ch, null)
                    scope.launch {
                        delay(1500)
                        callCreateGroup(mgr, ch, attempt + 1, onSuccess, onFailure)
                    }
                } else {
                    _isGroupCreating.value = false
                    val err = when (reasonCode) {
                        WifiP2pManager.BUSY -> "Wi-Fi Direct framework is busy. Note: If both devices are on the same Wi-Fi router, P2P is not needed."
                        WifiP2pManager.ERROR -> "Wi-Fi chip error. Turn Wi-Fi OFF & ON to reset."
                        else -> "Failed to start P2P group: ${getReasonString(reasonCode)}"
                    }
                    _lastError.value = err
                    onFailure(err)
                }
            }
        })
    }

    /**
     * Runs lightweight discovery keep-alive on KDS so its Wi-Fi radio stays active and
     * responds to probe requests from scanning POS terminals.
     */
    @SuppressLint("MissingPermission")
    fun startKeepAliveDiscovery() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive) {
                if (hasRequiredPermissions() && isWifiEnabled()) {
                    val mgr = manager
                    val ch = channel
                    if (mgr != null && ch != null) {
                        mgr.discoverPeers(ch, null)
                    }
                }
                delay(30000) // Keep listening every 30s
            }
        }
    }

    fun stopKeepAliveDiscovery() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }

    private fun pollConnectionInfo() {
        infoPollingJob?.cancel()
        infoPollingJob = scope.launch {
            // Poll at 500ms, 1500ms, and 3000ms
            val delays = listOf(500L, 1000L, 1500L, 2000L)
            for (d in delays) {
                delay(d)
                requestConnectionInfo()
                requestGroupInfo()
            }
        }
    }

    fun requestConnectionInfo() {
        val mgr = manager ?: return
        val ch = channel ?: return

        mgr.requestConnectionInfo(ch) { info: WifiP2pInfo? ->
            if (info == null) return@requestConnectionInfo

            val isConnected = info.groupFormed
            val isGroupOwner = info.isGroupOwner
            val rawOwnerIp = info.groupOwnerAddress?.hostAddress
            val resolvedIp = rawOwnerIp ?: if (isGroupOwner) DEFAULT_GROUP_OWNER_IP else null

            Log.i(TAG, "P2P connection info: formed=$isConnected, owner=$isGroupOwner, ip=$resolvedIp")

            if (isConnected) {
                _connectionState.value = _connectionState.value.copy(
                    isConnected = true,
                    isGroupOwner = isGroupOwner,
                    groupOwnerAddress = resolvedIp
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestGroupInfo() {
        if (!hasRequiredPermissions()) return
        val mgr = manager ?: return
        val ch = channel ?: return

        try {
            mgr.requestGroupInfo(ch) { group: WifiP2pGroup? ->
                if (group != null) {
                    val groupName = group.networkName
                    val clientCount = group.clientList?.size ?: 0
                    Log.d(TAG, "P2P group info: name=$groupName, clients=$clientCount")
                    _connectionState.value = _connectionState.value.copy(
                        groupNetworkName = groupName,
                        clientCount = clientCount
                    )
                } else {
                    Log.d(TAG, "P2P group is null (disbanded or stopped)")
                    _connectionState.value = P2pConnectionState(isConnected = false)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "requestGroupInfo exception", e)
        }
    }

    // =========================================================================
    // Connect & Disconnect Operations
    // =========================================================================

    @SuppressLint("MissingPermission")
    fun connectToPeer(
        deviceAddress: String,
        isGroupOwnerPreferred: Boolean = false,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val preErr = checkPreconditions()
        if (preErr != null) {
            _lastError.value = preErr
            onFailure(preErr)
            return
        }
        val mgr = manager ?: return
        val ch = channel ?: return

        val config = WifiP2pConfig().apply {
            this.deviceAddress = deviceAddress
            wps.setup = WpsInfo.PBC
            groupOwnerIntent = if (isGroupOwnerPreferred) 15 else 0
        }

        mgr.connect(ch, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(TAG, "Initiated P2P connection to $deviceAddress")
                _lastError.value = null
                pollConnectionInfo()
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

    fun removeGroup(onComplete: () -> Unit = {}) {
        stopKeepAliveDiscovery()
        peerPollingJob?.cancel()
        peerPollingJob = null
        infoPollingJob?.cancel()
        infoPollingJob = null

        _isGroupCreating.value = false
        _connectionState.value = P2pConnectionState(isConnected = false)
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
                Log.w(TAG, "removeGroup failed: ${getReasonString(reasonCode)}")
                _connectionState.value = P2pConnectionState(isConnected = false)
                onComplete()
            }
        })
    }

    private fun getReasonString(reasonCode: Int): String {
        return when (reasonCode) {
            WifiP2pManager.P2P_UNSUPPORTED -> "Wi-Fi Direct is not supported on this device"
            WifiP2pManager.ERROR -> "Wi-Fi chip error. Turn Wi-Fi OFF & ON to reset."
            WifiP2pManager.BUSY -> "Wi-Fi Direct radio is busy. Toggle Wi-Fi OFF & ON in Quick Settings."
            else -> "Error ($reasonCode)"
        }
    }
}
