package com.quickbill.pos.ui.components

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
    var valueText by remember { mutableStateOf(if (initialValue > 0) initialValue.toString() else "") }

    val numericValue = valueText.toDoubleOrNull() ?: 0.0
    val calculatedDiscount = when (selectedType) {
        DiscountType.PERCENTAGE -> BillingCalculator.round2(subtotal * (numericValue.coerceIn(0.0, 100.0) / 100.0))
        DiscountType.FLAT -> BillingCalculator.round2(numericValue.coerceIn(0.0, subtotal))
        DiscountType.NONE -> 0.0
    }
    val netAmount = BillingCalculator.round2(maxOf(0.0, subtotal - calculatedDiscount))

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.LocalOffer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Subtotal reference
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Eligible Amount:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "₹${String.format(java.util.Locale.US, "%.2f", subtotal)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Discount Type Selector (Percentage vs Flat)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == DiscountType.PERCENTAGE,
                        onClick = { selectedType = DiscountType.PERCENTAGE },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Percentage (%)")
                    }
                    SegmentedButton(
                        selected = selectedType == DiscountType.FLAT,
                        onClick = { selectedType = DiscountType.FLAT },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Flat Amount (₹)")
                    }
                }

                // Input Field
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                            valueText = input
                        }
                    },
                    label = {
                        Text(if (selectedType == DiscountType.PERCENTAGE) "Discount Percentage (%)" else "Discount Amount (₹)")
                    },
                    placeholder = {
                        Text(if (selectedType == DiscountType.PERCENTAGE) "e.g. 10" else "e.g. 50")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick preset chips
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
                        FilterChip(
                            selected = valueText == preset,
                            onClick = { valueText = preset },
                            label = {
                                Text(if (selectedType == DiscountType.PERCENTAGE) "$preset%" else "₹$preset")
                            }
                        )
                    }
                }

                // Summary preview card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Discount Savings:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "-₹${String.format(java.util.Locale.US, "%.2f", calculatedDiscount)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "New Net Total:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                text = "₹${String.format(java.util.Locale.US, "%.2f", netAmount)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (numericValue <= 0) {
                        onApply(DiscountType.NONE, 0.0)
                    } else {
                        onApply(selectedType, numericValue)
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply Discount")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (initialType != DiscountType.NONE && initialValue > 0) {
                    TextButton(onClick = {
                        onApply(DiscountType.NONE, 0.0)
                        onDismiss()
                    }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
