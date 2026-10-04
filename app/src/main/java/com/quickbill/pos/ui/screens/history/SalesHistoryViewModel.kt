package com.quickbill.pos.ui.screens.history

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickbill.pos.data.local.entity.BillEntity
import com.quickbill.pos.data.model.BillStatus
import com.quickbill.pos.data.model.BillWithDetails
import com.quickbill.pos.data.model.PaymentMode
import com.quickbill.pos.data.repository.AuthRepository
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
    LAST_7_DAYS("Last 7 Days"),
    CUSTOM_RANGE("Date Range")
}

class SalesHistoryViewModel(
    private val billingRepository: BillingRepository,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long?, Long?>>(Pair(null, null))
    val customDateRange: StateFlow<Pair<Long?, Long?>> = _customDateRange.asStateFlow()

    private val _paymentFilter = MutableStateFlow<PaymentMode?>(null)
    val paymentFilter: StateFlow<PaymentMode?> = _paymentFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow<BillStatus?>(null)
    val statusFilter: StateFlow<BillStatus?> = _statusFilter.asStateFlow()

    private val _selectedBillDetails = MutableStateFlow<BillWithDetails?>(null)
    val selectedBillDetails: StateFlow<BillWithDetails?> = _selectedBillDetails.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    data class DateFilterParams(
        val filter: DateFilter = DateFilter.ALL,
        val customRange: Pair<Long?, Long?> = Pair(null, null)
    )

    private val _dateFilterParams = combine(_dateFilter, _customDateRange) { f, r ->
        DateFilterParams(f, r)
    }

    val bills: StateFlow<List<BillEntity>> = combine(
        billingRepository.allBills,
        _searchQuery,
        _dateFilterParams,
        _paymentFilter,
        _statusFilter
    ) { allBills, query, dateParams, paymentMode, status ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val filter = dateParams.filter
        val customRange = dateParams.customRange

        allBills.filter { bill ->
            val matchesQuery = query.isBlank() ||
                    bill.billNumber.contains(query, ignoreCase = true) ||
                    bill.customerName.contains(query, ignoreCase = true) ||
                    bill.customerPhone.contains(query, ignoreCase = true)

            val matchesStatus = status == null || bill.status == status

            val matchesPayment = paymentMode == null || bill.paymentMode == paymentMode

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
                    cal.set(Calendar.MILLISECOND, 0)
                    val startYesterday = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val endYesterday = cal.timeInMillis
                    bill.timestamp in startYesterday..endYesterday
                }
                DateFilter.LAST_7_DAYS -> {
                    cal.timeInMillis = now
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    bill.timestamp >= cal.timeInMillis
                }
                DateFilter.CUSTOM_RANGE -> {
                    val (start, end) = customRange
                    val matchesStart = if (start != null) {
                        val sCal = Calendar.getInstance().apply {
                            timeInMillis = start
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        bill.timestamp >= sCal.timeInMillis
                    } else true

                    val matchesEnd = if (end != null) {
                        val eCal = Calendar.getInstance().apply {
                            timeInMillis = end
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        bill.timestamp <= eCal.timeInMillis
                    } else true

                    matchesStart && matchesEnd
                }
            }

            matchesQuery && matchesStatus && matchesPayment && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun onDateFilterSelected(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun setCustomDateRange(startDateMillis: Long?, endDateMillis: Long?) {
        _customDateRange.value = Pair(startDateMillis, endDateMillis)
        _dateFilter.value = DateFilter.CUSTOM_RANGE
    }

    fun onPaymentFilterSelected(paymentMode: PaymentMode?) {
        _paymentFilter.value = paymentMode
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

    fun exportToCsv(context: Context) {
        val currentBills = bills.value
        viewModelScope.launch {
            try {
                val file = CsvExporter.exportBillsToCsv(context, currentBills)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "QuickBill Sales Ledger")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Sales CSV"))
            } catch (e: Exception) {
                _message.value = "CSV export failed: ${e.message}"
            }
        }
    }

    fun exportToExcel(context: Context) {
        val currentBills = bills.value
        viewModelScope.launch {
            try {
                val file = CsvExporter.exportBillsToExcel(context, currentBills)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.ms-excel"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "QuickBill Excel Transactions Ledger")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open or Share Excel Ledger"))
            } catch (e: Exception) {
                _message.value = "Excel export failed: ${e.message}"
            }
        }
    }

    fun clearAllHistory(isAdmin: Boolean? = null) {
        val userIsAdmin = isAdmin ?: (authRepository?.isAdmin() == true)
        if (!userIsAdmin) {
            _message.value = "Permission denied: Only Admin can clear sales history."
            return
        }
        viewModelScope.launch {
            billingRepository.clearAllTransactions()
            _message.value = "All sales history records have been cleared"
        }
    }
}
