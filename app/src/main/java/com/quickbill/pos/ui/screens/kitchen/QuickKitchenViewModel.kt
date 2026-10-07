package com.quickbill.pos.ui.screens.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickbill.pos.data.local.dao.OrderDao
import com.quickbill.pos.data.model.kds.KitchenOrder
import com.quickbill.pos.data.model.kds.KitchenOrderItem
import com.quickbill.pos.data.model.kds.OrderStatus
import com.quickbill.pos.data.model.kds.OrderType
import com.quickbill.pos.data.repository.KdsSettings
import com.quickbill.pos.data.repository.KdsSettingsRepository
import com.quickbill.pos.network.kds.ConnectionManager
import com.quickbill.pos.network.kds.NsdDiscoveryManager
import com.quickbill.pos.network.kds.OrderSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.quickbill.pos.data.model.kds.ConnectedPosTerminal

data class KitchenUiState(
    val displayedOrders: List<KitchenOrder> = emptyList(),
    val allOrders: List<KitchenOrder> = emptyList(),
    val selectedStatus: OrderStatus = OrderStatus.NEW,
    val currentNavTab: Int = 0, // 0 = Orders, 1 = History, 2 = Settings
    val selectedOrderForDetail: KitchenOrder? = null,
    val newOrdersCount: Int = 0,
    val preparingOrdersCount: Int = 0,
    val readyOrdersCount: Int = 0,
    val completedOrdersCount: Int = 0,
    val activeOrdersCount: Int = 0,
    val kitchenName: String = "Main Kitchen",
    val serverIp: String = "127.0.0.1",
    val serverPort: Int = 8080,
    val connectedClients: Int = 0,
    val connectedPosTerminals: List<ConnectedPosTerminal> = emptyList(),
    val isServerRunning: Boolean = false,
    val warningThresholdMinutes: Int = 5,
    val settings: KdsSettings = KdsSettings(),
    val isSetupComplete: Boolean = true,
    val hasShownWaitingScreen: Boolean = false
)

class QuickKitchenViewModel(
    private val orderDao: OrderDao,
    private val orderSyncManager: OrderSyncManager,
    private val connectionManager: ConnectionManager,
    private val discoveryManager: NsdDiscoveryManager,
    private val settingsRepository: KdsSettingsRepository
) : ViewModel() {

    private val _selectedStatus = MutableStateFlow(OrderStatus.NEW)
    private val _currentNavTab = MutableStateFlow(0)
    private val _selectedOrderForDetail = MutableStateFlow<KitchenOrder?>(null)
    private val _isSetupComplete = MutableStateFlow(true)
    private val _hasShownWaitingScreen = MutableStateFlow(false)

    private val _ordersFlow = orderDao.getAllOrdersFlow()
    private val _itemsFlow = orderDao.getAllOrderItemsFlow()

    val uiState: StateFlow<KitchenUiState> = combine(
        _ordersFlow,
        _itemsFlow,
        _selectedStatus,
        _currentNavTab,
        _selectedOrderForDetail,
        settingsRepository.settings,
        connectionManager.connectedClientsCount,
        connectionManager.isServerRunning,
        _isSetupComplete,
        _hasShownWaitingScreen,
        connectionManager.connectedPosTerminals
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val orderEntities = args[0] as List<com.quickbill.pos.data.local.entity.OrderEntity>
        @Suppress("UNCHECKED_CAST")
        val itemEntities = args[1] as List<com.quickbill.pos.data.local.entity.OrderItemEntity>
        val status = args[2] as OrderStatus
        val navTab = args[3] as Int
        val detailOrder = args[4] as KitchenOrder?
        val settings = args[5] as KdsSettings
        val clients = args[6] as Int
        val isRunning = args[7] as Boolean
        val setupComplete = args[8] as Boolean
        val waitingShown = args[9] as Boolean
        @Suppress("UNCHECKED_CAST")
        val terminals = args[10] as List<ConnectedPosTerminal>

        val itemsByOrderId = itemEntities.groupBy { it.orderId }

        val allKitchenOrders = orderEntities.map { entity ->
            val orderItems = itemsByOrderId[entity.orderId]?.map {
                KitchenOrderItem(
                    id = it.id,
                    orderId = it.orderId,
                    productId = it.productId,
                    name = it.name,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    notes = it.notes,
                    isVeg = it.isVeg
                )
            } ?: emptyList()

            KitchenOrder(
                orderId = entity.orderId,
                orderNumber = entity.orderNumber,
                customerName = entity.customerName,
                tableNumber = entity.tableNumber,
                notes = entity.notes,
                orderType = entity.orderType,
                status = entity.status,
                items = orderItems,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                synced = entity.synced
            )
        }.distinctBy { it.orderNumber }

        val newCount = allKitchenOrders.count { it.status == OrderStatus.NEW }
        val preparingCount = allKitchenOrders.count { it.status == OrderStatus.PREPARING }
        val readyCount = allKitchenOrders.count { it.status == OrderStatus.READY }
        val completedCount = allKitchenOrders.count { it.status == OrderStatus.COMPLETED }
        val activeCount = newCount + preparingCount + readyCount

        val displayed = allKitchenOrders.filter { it.status == status }

        // Updated selected detail order if status changed
        val currentDetail = detailOrder?.let { d ->
            allKitchenOrders.find { it.orderId == d.orderId } ?: d
        }

        KitchenUiState(
            displayedOrders = displayed,
            allOrders = allKitchenOrders,
            selectedStatus = status,
            currentNavTab = navTab,
            selectedOrderForDetail = currentDetail,
            newOrdersCount = newCount,
            preparingOrdersCount = preparingCount,
            readyOrdersCount = readyCount,
            completedOrdersCount = completedCount,
            activeOrdersCount = activeCount,
            kitchenName = settings.kitchenName,
            serverIp = discoveryManager.getLocalIpAddress(),
            serverPort = settings.serverPort,
            connectedClients = clients,
            connectedPosTerminals = terminals,
            isServerRunning = isRunning,
            warningThresholdMinutes = settings.warningThresholdMinutes,
            settings = settings,
            isSetupComplete = setupComplete,
            hasShownWaitingScreen = waitingShown
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        KitchenUiState(serverIp = discoveryManager.getLocalIpAddress())
    )

    init {
        startKdsServices()
    }

    fun startKdsServices() {
        val settings = settingsRepository.settings.value
        connectionManager.startKdsServer(settings.serverPort)
        val localIp = discoveryManager.getLocalIpAddress()
        val ipSuffix = localIp.substringAfterLast(".", "").ifBlank { (100..999).random().toString() }
        val kitchenTitle = settings.kitchenName.trim().ifBlank { "Kitchen" }
        val advertisedName = "QuickKitchen-$kitchenTitle-$ipSuffix"
        discoveryManager.startAdvertising(serviceName = advertisedName, port = settings.serverPort)
    }

    fun stopKdsServices() {
        connectionManager.stopKdsServer()
        discoveryManager.stopAdvertising()
    }

    fun selectStatus(status: OrderStatus) {
        _selectedStatus.value = status
    }

    fun selectNavTab(tab: Int) {
        _currentNavTab.value = tab
        _selectedOrderForDetail.value = null
    }

    fun openOrderDetail(order: KitchenOrder) {
        _selectedOrderForDetail.value = order
    }

    fun closeOrderDetail() {
        _selectedOrderForDetail.value = null
    }

    fun markSetupComplete(kitchenName: String) {
        val current = settingsRepository.settings.value
        settingsRepository.updateSettings(current.copy(kitchenName = kitchenName))
        _isSetupComplete.value = true
    }

    fun markWaitingScreenShown() {
        _hasShownWaitingScreen.value = true
    }

    fun updateSettings(settings: KdsSettings) {
        val current = settingsRepository.settings.value
        settingsRepository.updateSettings(settings)
        if (current.serverPort != settings.serverPort || current.kitchenName != settings.kitchenName) {
            connectionManager.stopKdsServer()
            discoveryManager.stopAdvertising()
            connectionManager.startKdsServer(settings.serverPort)
            val localIp = discoveryManager.getLocalIpAddress()
            val ipSuffix = localIp.substringAfterLast(".", "").ifBlank { (100..999).random().toString() }
            val kitchenTitle = settings.kitchenName.trim().ifBlank { "Kitchen" }
            val advertisedName = "QuickKitchen-$kitchenTitle-$ipSuffix"
            discoveryManager.startAdvertising(serviceName = advertisedName, port = settings.serverPort)
        }
    }

    // Status transitions
    fun advanceOrderStatus(order: KitchenOrder) {
        val next = order.status.nextStatus() ?: return
        viewModelScope.launch {
            orderSyncManager.updateOrderStatusOnKds(order.orderId, next)
            // If currently viewing order detail, update or close if completed
            if (next == OrderStatus.COMPLETED) {
                _selectedOrderForDetail.value = null
            }
        }
    }

    fun setOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            orderSyncManager.updateOrderStatusOnKds(orderId, newStatus)
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            orderSyncManager.updateOrderStatusOnKds(orderId, OrderStatus.CANCELLED)
            _selectedOrderForDetail.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopKdsServices()
    }
}
