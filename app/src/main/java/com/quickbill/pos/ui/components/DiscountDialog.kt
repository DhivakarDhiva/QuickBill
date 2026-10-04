package com.quickbill.pos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.model.DiscountType
import com.quickbill.pos.data.util.BillingCalculator
import com.quickbill.pos.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountDialog(
    title: String = "Apply Discount",
    subtotal: Double,
    initialType: DiscountType = DiscountType.NONE,
    initialValue: Double = 0.0,
    onDismiss: () -> Unit,
    onApply: (DiscountType, Double) -> Unit
) {
    var selectedType by remember { mutableStateOf(if (initialType == DiscountType.NONE) DiscountType.PERCENTAGE else initialType) }
    var valueText by remember { mutableStateOf(if (initialValue > 0) String.format(java.util.Locale.US, "%.0f", initialValue) else "") }

    val numericValue = valueText.toDoubleOrNull() ?: 0.0

    val isPercentage = selectedType == DiscountType.PERCENTAGE
    val isFlat = selectedType == DiscountType.FLAT
    val isInvalidPercentage = isPercentage && (numericValue > 100.0 || numericValue < 0.0)
    val isInvalidFlat = isFlat && (numericValue > subtotal || numericValue < 0.0)
    val isInvalid = isInvalidPercentage || isInvalidFlat

    val validationErrorMessage = when {
        isInvalidPercentage -> "Percentage discount cannot exceed 100%"
        isInvalidFlat -> "Flat discount cannot exceed eligible amount (₹${String.format(java.util.Locale.US, "%.2f", subtotal)})"
        else -> null
    }

    val calculatedDiscount = when (selectedType) {
        DiscountType.PERCENTAGE -> BillingCalculator.round2(subtotal * (numericValue.coerceIn(0.0, 100.0) / 100.0))
        DiscountType.FLAT -> BillingCalculator.round2(numericValue.coerceIn(0.0, subtotal))
        DiscountType.NONE -> 0.0
    }
    val netAmount = BillingCalculator.round2(maxOf(0.0, subtotal - calculatedDiscount))

    QuickBillDialog(
        onDismissRequest = onDismiss,
        title = title
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Eligible Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Eligible Amount",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight)
                )
                Text(
                    text = "₹ ${String.format(java.util.Locale.US, "%.2f", subtotal)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                )
            }

            // Segmented selector: Percentage vs Flat
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceMutedLight,
                border = BorderStroke(1.dp, OutlineLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isPct = selectedType == DiscountType.PERCENTAGE
                    Surface(
                        onClick = { selectedType = DiscountType.PERCENTAGE },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPct) EmeraldPrimary else Color.Transparent,
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Percentage (%)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isPct) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isPct) Color.White else TextSecondaryLight
                                )
                            )
                        }
                    }

                    val isFlatType = selectedType == DiscountType.FLAT
                    Surface(
                        onClick = { selectedType = DiscountType.FLAT },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isFlatType) EmeraldPrimary else Color.Transparent,
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Flat Amount (₹)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isFlatType) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isFlatType) Color.White else TextSecondaryLight
                                )
                            )
                        }
                    }
                }
            }

            // Discount Input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                QuickBillTextField(
                    value = valueText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                            valueText = input
                        }
                    },
                    placeholder = if (selectedType == DiscountType.PERCENTAGE) "e.g. 10%" else "e.g. 50",
                    leadingIcon = Icons.Default.LocalOffer,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationErrorMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = ErrorCoral,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = validationErrorMessage,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ErrorCoral,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Preset Quick Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = if (selectedType == DiscountType.PERCENTAGE) {
                    listOf("5", "10", "15", "20")
                } else {
                    listOf("20", "50", "100", "200")
                }
                presets.forEach { preset ->
                    Surface(
                        onClick = { valueText = preset },
                        shape = RoundedCornerShape(8.dp),
                        color = if (valueText == preset) EmeraldContainer else SurfaceMutedLight,
                        border = BorderStroke(1.dp, if (valueText == preset) EmeraldPrimary else OutlineLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (selectedType == DiscountType.PERCENTAGE) "$preset%" else "₹$preset",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (valueText == preset) EmeraldPrimary else TextPrimaryLight
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            // Savings Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WarmBackgroundLight,
                border = BorderStroke(1.dp, OutlineLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Discount Savings:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight))
                        Text(
                            "- ₹ ${String.format(java.util.Locale.US, "%.2f", calculatedDiscount)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = CoralAccent)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("New Total:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "₹ ${String.format(java.util.Locale.US, "%.2f", netAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initialType != DiscountType.NONE && initialValue > 0) {
                    OutlinedButton(
                        onClick = {
                            onApply(DiscountType.NONE, 0.0)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorCoral),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Remove")
                    }
                }

                QuickBillButton(
                    text = "Apply Discount",
                    onClick = {
                        if (numericValue <= 0) {
                            onApply(DiscountType.NONE, 0.0)
                        } else {
                            onApply(selectedType, numericValue)
                        }
                        onDismiss()
                    },
                    enabled = !isInvalid,
                    modifier = Modifier.weight(1.5f),
                    height = 46.dp
                )
            }
        }
    }
}
