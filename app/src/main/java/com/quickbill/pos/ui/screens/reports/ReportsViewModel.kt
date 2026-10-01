package com.quickbill.pos.ui.screens.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickbill.pos.data.model.DailyReportData
import com.quickbill.pos.data.repository.ReportRepository
import com.quickbill.pos.data.util.CsvExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportPeriod(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    THIS_MONTH("This Month")
}

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.TODAY,
    val reportData: DailyReportData? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ReportsViewModel(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReport(ReportPeriod.TODAY)
    }

    fun selectPeriod(period: ReportPeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadReport(period)
    }

    fun loadReport(period: ReportPeriod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val cal = Calendar.getInstance()
            val now = System.currentTimeMillis()

            val (start, end) = when (period) {
                ReportPeriod.TODAY -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val s = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    Pair(s, cal.timeInMillis)
                }
                ReportPeriod.YESTERDAY -> {
                    cal.timeInMillis = now
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val s = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    Pair(s, cal.timeInMillis)
                }
                ReportPeriod.LAST_7_DAYS -> {
                    cal.timeInMillis = now
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    Pair(cal.timeInMillis, now)
                }
                ReportPeriod.THIS_MONTH -> {
                    cal.timeInMillis = now
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    Pair(cal.timeInMillis, now)
                }
            }

            try {
                val data = reportRepository.getReportForRange(start, end, period.label)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    reportData = data
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load report: ${e.message}"
                )
            }
        }
    }

    fun exportToCsv(context: Context) {
        val report = _uiState.value.reportData ?: return
        viewModelScope.launch {
            try {
                val file = CsvExporter.exportDailyReportToCsv(context, report)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "QuickBill Sales Report (${report.dateLabel})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Sales Report"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Export failed: ${e.message}")
            }
        }
    }
}
