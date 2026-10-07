package com.quickbill.pos.network.kds

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.ArrayDeque
import java.util.Collections

data class DiscoveredKdsService(
    val serviceName: String,
    val hostIp: String,
    val port: Int,
    val discoveredAt: Long = System.currentTimeMillis()
)

class NsdDiscoveryManager(private val context: Context) {
    companion object {
        private const val TAG = "NsdDiscoveryManager"
        const val SERVICE_TYPE = "_quickbill._tcp."
        const val DEFAULT_SERVICE_NAME = "QuickKitchen-KDS"
        const val DEFAULT_PORT = 8080
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var currentAdvertisedName: String = ""
    private var currentAdvertisedPort: Int = 0

    // Sequential service resolution queue to prevent Android NSD FAILURE_ALREADY_ACTIVE
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var isResolving = false

    private val _discoveredServices = MutableStateFlow<List<DiscoveredKdsService>>(emptyList())
    val discoveredServices: StateFlow<List<DiscoveredKdsService>> = _discoveredServices.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                multicastLock = wifiManager?.createMulticastLock("QuickBillMulticastLock")?.apply {
                    setReferenceCounted(true)
                }
            }
            if (multicastLock?.isHeld != true) {
                multicastLock?.acquire()
                Log.d(TAG, "MulticastLock acquired")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire MulticastLock", e)
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Log.d(TAG, "MulticastLock released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release MulticastLock", e)
        }
    }

    // KDS Mode: Register service on local network
    fun startAdvertising(serviceName: String = DEFAULT_SERVICE_NAME, port: Int = DEFAULT_PORT) {
        if (nsdManager == null) {
            Log.e(TAG, "NsdManager not available")
            return
        }

        // If already actively advertising with exact same parameters, nothing to do
        if (_isAdvertising.value && currentAdvertisedName == serviceName && currentAdvertisedPort == port && registrationListener != null) {
            Log.i(TAG, "Already advertising $serviceName on port $port")
            return
        }

        if (_isAdvertising.value || registrationListener != null) {
            stopAdvertising()
        }

        currentAdvertisedName = serviceName
        currentAdvertisedPort = port
        acquireMulticastLock()

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredInfo: NsdServiceInfo) {
                Log.i(TAG, "Service registered: ${registeredInfo.serviceName} on port ${registeredInfo.port}")
                _isAdvertising.value = true
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Registration failed: errorCode $errorCode")
                _isAdvertising.value = false
                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                    Log.w(TAG, "NSD Registration FAILURE_ALREADY_ACTIVE. Retrying registration in 1000ms...")
                    scope.launch {
                        delay(1000)
                        if (currentAdvertisedName.isNotBlank()) {
                            startAdvertising(currentAdvertisedName, currentAdvertisedPort)
                        }
                    }
                }
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.i(TAG, "Service unregistered")
                _isAdvertising.value = false
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Unregistration failed: errorCode $errorCode")
                _isAdvertising.value = false
            }
        }
        registrationListener = listener

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting NSD advertising", e)
            _isAdvertising.value = false
        }
    }

    fun stopAdvertising() {
        val listener = registrationListener
        registrationListener = null
        currentAdvertisedName = ""
        currentAdvertisedPort = 0
        if (listener != null) {
            try {
                nsdManager?.unregisterService(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering service", e)
            }
        }
        _isAdvertising.value = false
        if (!_isDiscovering.value) {
            releaseMulticastLock()
        }
    }

    private var subnetScanJob: Job? = null

    // POS Mode: Discover KDS services
    fun startDiscovery() {
        if (nsdManager == null) {
            Log.e(TAG, "NsdManager not available")
            return
        }
        if (_isDiscovering.value && discoveryListener != null) {
            Log.i(TAG, "NSD discovery already running, triggering fast subnet re-scan")
            startSubnetScan()
            return
        }

        if (_isDiscovering.value || discoveryListener != null) {
            stopDiscovery()
        }

        acquireMulticastLock()

        synchronized(resolveQueue) {
            resolveQueue.clear()
            isResolving = false
        }

        // Start high-speed background Wi-Fi subnet scanner in parallel with NSD
        startSubnetScan()

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.i(TAG, "NSD Discovery started for $regType")
                _isDiscovering.value = true
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.i(TAG, "Service found: ${service.serviceName}, type: ${service.serviceType}")
                val typeMatch = service.serviceType.contains("quickbill", ignoreCase = true) ||
                        service.serviceType.contains("_tcp", ignoreCase = true)
                val nameMatch = service.serviceName.contains("Kitchen", ignoreCase = true) ||
                        service.serviceName.contains("KDS", ignoreCase = true) ||
                        service.serviceName.contains("QuickBill", ignoreCase = true)
                if (typeMatch || nameMatch) {
                    enqueueServiceResolution(service)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.i(TAG, "Service lost: ${service.serviceName}")
                _discoveredServices.value = _discoveredServices.value.filter {
                    it.serviceName != service.serviceName
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "NSD Discovery stopped")
                _isDiscovering.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $errorCode")
                _isDiscovering.value = false
                if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                    Log.w(TAG, "NSD Discovery FAILURE_ALREADY_ACTIVE. Retrying discovery in 1000ms...")
                    scope.launch {
                        delay(1000)
                        startDiscovery()
                    }
                }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $errorCode")
            }
        }
        discoveryListener = listener

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting NSD discovery", e)
            _isDiscovering.value = false
        }
    }

    private fun enqueueServiceResolution(service: NsdServiceInfo) {
        synchronized(resolveQueue) {
            if (!resolveQueue.any { it.serviceName == service.serviceName }) {
                resolveQueue.add(service)
            }
            processNextResolve()
        }
    }

    private fun processNextResolve() {
        synchronized(resolveQueue) {
            if (isResolving || resolveQueue.isEmpty()) return
            val nextService = resolveQueue.removeFirst()
            isResolving = true
            resolveServiceInternal(nextService)
        }
    }

    private fun resolveServiceInternal(service: NsdServiceInfo) {
        var isCompleted = false

        // 3.5 second watchdog: if Android resolveService hangs or fails silently, advance queue
        val watchdogJob = scope.launch {
            delay(3500L)
            synchronized(resolveQueue) {
                if (!isCompleted && isResolving) {
                    Log.w(TAG, "Resolve watchdog timeout for ${service.serviceName}. Advancing queue...")
                    isCompleted = true
                    isResolving = false
                    processNextResolve()
                }
            }
        }

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                synchronized(resolveQueue) {
                    if (!isCompleted) {
                        isCompleted = true
                        watchdogJob.cancel()
                        isResolving = false
                        processNextResolve()
                    }
                }
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val rawHost = serviceInfo.host?.hostAddress ?: ""
                val cleanHost = rawHost.removePrefix("/").substringBefore("%").trim()
                val resolvedPort = if (serviceInfo.port > 0) serviceInfo.port else DEFAULT_PORT
                Log.i(TAG, "Service resolved: ${serviceInfo.serviceName} at $cleanHost:$resolvedPort (raw=$rawHost)")

                if (cleanHost.isNotBlank()) {
                    val discovered = DiscoveredKdsService(
                        serviceName = serviceInfo.serviceName,
                        hostIp = cleanHost,
                        port = resolvedPort
                    )
                    val current = _discoveredServices.value.filter { it.hostIp != cleanHost || it.port != resolvedPort }
                    _discoveredServices.value = current + discovered
                }

                synchronized(resolveQueue) {
                    if (!isCompleted) {
                        isCompleted = true
                        watchdogJob.cancel()
                        isResolving = false
                        processNextResolve()
                    }
                }
            }
        }

        try {
            nsdManager?.resolveService(service, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling resolveService", e)
            synchronized(resolveQueue) {
                if (!isCompleted) {
                    isCompleted = true
                    watchdogJob.cancel()
                    isResolving = false
                    processNextResolve()
                }
            }
        }
    }

    fun stopDiscovery() {
        val listener = discoveryListener
        discoveryListener = null
        subnetScanJob?.cancel()
        subnetScanJob = null
        if (listener != null) {
            try {
                nsdManager?.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery", e)
            }
        }
        synchronized(resolveQueue) {
            resolveQueue.clear()
            isResolving = false
        }
        _isDiscovering.value = false
        if (!_isAdvertising.value) {
            releaseMulticastLock()
        }
    }

    // High-speed parallel Wi-Fi subnet port scanner (guarantees discovering KDS even when multicast/mDNS is blocked by router)
    fun startSubnetScan(port: Int = DEFAULT_PORT) {
        subnetScanJob?.cancel()
        subnetScanJob = scope.launch(Dispatchers.IO) {
            val localIp = getLocalIpAddress()
            if (localIp == "127.0.0.1" || !localIp.contains(".")) return@launch

            val subnetPrefix = localIp.substringBeforeLast(".")
            val localSuffix = localIp.substringAfterLast(".").toIntOrNull() ?: -1

            Log.i(TAG, "Starting fast Wi-Fi subnet scan on $subnetPrefix.* for KDS port $port...")

            val allIps = (1..254).filter { it != localSuffix }.map { "$subnetPrefix.$it" }
            allIps.chunked(32).forEach { chunk ->
                if (!isActive) return@launch
                val jobs = chunk.map { targetIp ->
                    launch {
                        try {
                            val socket = java.net.Socket()
                            socket.connect(java.net.InetSocketAddress(targetIp, port), 250)
                            socket.close()

                            Log.i(TAG, "Subnet scan discovered active KDS at $targetIp:$port")
                            val discovered = DiscoveredKdsService(
                                serviceName = "Kitchen Display (${targetIp.substringAfterLast('.')})",
                                hostIp = targetIp,
                                port = port
                            )
                            val current = _discoveredServices.value.filter { it.hostIp != targetIp || it.port != port }
                            _discoveredServices.value = current + discovered
                        } catch (_: Exception) {}
                    }
                }
                jobs.forEach { it.join() }
            }
            Log.i(TAG, "Subnet scan completed. Total discovered: ${_discoveredServices.value.size}")
        }
    }

    // Utility: Find local device IP on Wi-Fi/LAN network (prioritize Wi-Fi over cellular)
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

            // Pass 1: Prioritize Wi-Fi or Ethernet interfaces (e.g. wlan0, eth0, en0)
            for (intf in interfaces) {
                val name = intf.name.lowercase()
                if (name.startsWith("wlan") || name.startsWith("eth") || name.startsWith("en") || name.contains("wifi")) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress
                            if (!host.isNullOrBlank() && host != "127.0.0.1") {
                                return host
                            }
                        }
                    }
                }
            }

            // Pass 2: Any non-loopback, non-cellular IPv4 address (ignore cellular/dummy/p2p)
            for (intf in interfaces) {
                val name = intf.name.lowercase()
                if (name.startsWith("rmnet") || name.startsWith("dummy") || name.startsWith("tun") || name.startsWith("p2p") || name.startsWith("ccmni")) {
                    continue
                }
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && host != "127.0.0.1") {
                            return host
                        }
                    }
                }
            }

            // Pass 3: Fallback to any non-loopback IPv4
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && host != "127.0.0.1") {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address", e)
        }
        return "127.0.0.1"
    }
}
