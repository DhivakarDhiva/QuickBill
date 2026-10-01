package com.quickbill.pos.ui.screens.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.model.BillStatus
import com.quickbill.pos.data.model.BillWithDetails
import com.quickbill.pos.data.repository.BillingRepository
import com.quickbill.pos.data.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days")
}

class SalesHistoryViewModel(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow<BillStatus?>(null)
    val statusFilter: StateFlow<BillStatus?> = _statusFilter.asStateFlow()

    private val _selectedBillDetails = MutableStateFlow<BillWithDetails?>(null)
    val selectedBillDetails: StateFlow<BillWithDetails?> = _selectedBillDetails.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val bills: StateFlow<List<BillEntity>> = combine(
        billingRepository.allBills,
        _searchQuery,
        _dateFilter,
        _statusFilter
    ) { allBills, query, filter, status ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        allBills.filter { bill ->
            val matchesQuery = query.isBlank() ||
                    bill.billNumber.contains(query, ignoreCase = true) ||
                    bill.customerName.contains(query, ignoreCase = true) ||
                    bill.customerPhone.contains(query, ignoreCase = true)

            val matchesStatus = status == null || bill.status == status

            val matchesDate = when (filter) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    bill.timestamp >= cal.timeInMillis
                }
                DateFilter.YESTERDAY -> {
                    cal.timeInMillis = now
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    val startYesterday = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    val endYesterday = cal.timeInMillis
                    bill.timestamp in startYesterday..endYesterday
                }
                DateFilter.LAST_7_DAYS -> {
                    cal.timeInMillis = now
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    bill.timestamp >= cal.timeInMillis
                }
            }

            matchesQuery && matchesStatus && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun onDateFilterSelected(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun onStatusFilterSelected(status: BillStatus?) {
        _statusFilter.value = status
    }

    fun viewBillDetails(billId: Long) {
        viewModelScope.launch {
            val details = billingRepository.getBillDetails(billId)
            _selectedBillDetails.value = details
        }
    }

    fun closeBillDetails() {
        _selectedBillDetails.value = null
    }

    fun refundBill(billId: Long) {
        viewModelScope.launch {
            val result = billingRepository.refundBill(billId)
            result.onSuccess {
                _message.value = "Bill marked as REFUNDED and inventory stock restored successfully!"
                // Refresh detail if open
                val updatedDetails = billingRepository.getBillDetails(billId)
                _selectedBillDetails.value = updatedDetails
            }.onFailure { error ->
                _message.value = "Refund failed: ${error.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
