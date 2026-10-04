package com.quickbill.pos.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickbill.pos.data.local.dao.TopSellingItemResult
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.data.model.BillStatus
import com.quickbill.pos.data.repository.AuthRepository
import com.quickbill.pos.data.repository.BillingRepository
import com.quickbill.pos.data.repository.ProductRepository
import com.quickbill.pos.data.repository.ReportRepository
import com.quickbill.pos.data.util.BillingCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val totalSales: Double = 0.0,
    val billsCount: Int = 0,
    val itemsSoldCount: Int = 0,
    val lowStockCount: Int = 0,
    val highlightedSales: Double = 0.0,
    val displayedBillsCount: Int = 0,
    val displayedItemsCount: Int = 0,
    val selectedSlotIndex: Int? = null,
    val selectedSlotLabel: String? = null,
    val chartPoints: List<HourlySalePoint> = emptyList(),
    val topSellingItems: List<TopSellingItemResult> = emptyList(),
    val isLoading: Boolean = false
)

data class HourlySalePoint(
    val label: String,
    val timeRange: String,
    val amount: Double,
    val billsCount: Int,
    val itemsCount: Int
)

class DashboardViewModel(
    private val reportRepository: ReportRepository,
    private val productRepository: ProductRepository,
    private val billingRepository: BillingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
    val lowStockProducts: StateFlow<List<ProductEntity>> = productRepository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Continuously collect low stock products so lowStockCount is always real-time
        viewModelScope.launch {
            productRepository.lowStockProducts.collect { lowStockList ->
                _uiState.update { it.copy(lowStockCount = lowStockList.size) }
            }
        }
        loadDashboardData()
    }

    fun selectSlot(index: Int?) {
        _uiState.update { currentState ->
            if (index == null || currentState.selectedSlotIndex == index) {
                // Deselect: return to full day's totals
                currentState.copy(
                    selectedSlotIndex = null,
                    selectedSlotLabel = null,
                    highlightedSales = currentState.totalSales,
                    displayedBillsCount = currentState.billsCount,
                    displayedItemsCount = currentState.itemsSoldCount
                )
            } else {
                val point = currentState.chartPoints.getOrNull(index)
                if (point != null) {
                    currentState.copy(
                        selectedSlotIndex = index,
                        selectedSlotLabel = point.timeRange,
                        highlightedSales = point.amount,
                        displayedBillsCount = point.billsCount,
                        displayedItemsCount = point.itemsCount
                    )
                } else currentState
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Fetch real today's report
            val dailyReport = reportRepository.getDailyReport()
            val todayBills = reportRepository.getTodayBills()

            // 2. Calculate real low stock count directly from repository flow
            val lowStockCount = try {
                productRepository.lowStockProducts.first().size
            } catch (_: Exception) {
                _uiState.value.lowStockCount
            }

            // 3. Compute real hourly slots for today
            // Slots: 6AM (6-9), 9AM (9-12), 12PM (12-15), 3PM (15-18), 6PM (18-21), 9PM (21-24)
            val slotLabels = listOf(
                Pair("6 AM", "6:00 AM - 9:00 AM"),
                Pair("9 AM", "9:00 AM - 12:00 PM"),
                Pair("12 PM", "12:00 PM - 3:00 PM"),
                Pair("3 PM", "3:00 PM - 6:00 PM"),
                Pair("6 PM", "6:00 PM - 9:00 PM"),
                Pair("9 PM", "9:00 PM - 11:59 PM")
            )

            val slotSales = DoubleArray(6)
            val slotBills = IntArray(6)
            val slotItems = IntArray(6)

            val cal = Calendar.getInstance()
            for (bill in todayBills) {
                if (bill.status == BillStatus.COMPLETED) {
                    cal.timeInMillis = bill.timestamp
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val slotIdx = when (hour) {
                        in 0..8 -> 0    // Early morning & up to 9 AM
                        in 9..11 -> 1   // 9 AM to 12 PM
                        in 12..14 -> 2  // 12 PM to 3 PM
                        in 15..17 -> 3  // 3 PM to 6 PM
                        in 18..20 -> 4  // 6 PM to 9 PM
                        else -> 5       // 9 PM onwards
                    }
                    slotSales[slotIdx] += bill.grandTotal
                    slotBills[slotIdx] += 1
                    val items = reportRepository.getItemsForBill(bill.id)
                    slotItems[slotIdx] += items.sumOf { it.quantity }
                }
            }

            val points = slotLabels.mapIndexed { i, pair ->
                HourlySalePoint(
                    label = pair.first,
                    timeRange = pair.second,
                    amount = BillingCalculator.round2(slotSales[i]),
                    billsCount = slotBills[i],
                    itemsCount = slotItems[i]
                )
            }

            // Real accurate data directly from Room DB
            val realTotalSales = dailyReport.totalSales
            val realBillsCount = dailyReport.completedBillsCount
            val realItemsSold = dailyReport.totalItemsSold

            // Preserve current slot selection if valid, or reset to total
            val prevSlotIdx = _uiState.value.selectedSlotIndex
            val selectedPoint = prevSlotIdx?.let { points.getOrNull(it) }

            val highlightedSales = selectedPoint?.amount ?: realTotalSales
            val displayedBills = selectedPoint?.billsCount ?: realBillsCount
            val displayedItems = selectedPoint?.itemsCount ?: realItemsSold
            val selectedLabel = selectedPoint?.timeRange

            _uiState.update { current ->
                current.copy(
                    totalSales = realTotalSales,
                    billsCount = realBillsCount,
                    itemsSoldCount = realItemsSold,
                    lowStockCount = lowStockCount,
                    highlightedSales = highlightedSales,
                    displayedBillsCount = displayedBills,
                    displayedItemsCount = displayedItems,
                    selectedSlotIndex = prevSlotIdx,
                    selectedSlotLabel = selectedLabel,
                    chartPoints = points,
                    topSellingItems = dailyReport.topSellingItems,
                    isLoading = false
                )
            }
        }
    }
}
