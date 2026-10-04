package com.quickbill.pos.ui.screens.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.local.entity.HeldCartEntity
import com.quickbill.pos.ui.components.QuickBillChip
import com.quickbill.pos.ui.components.QuickBillEmptyState
import com.quickbill.pos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HeldCartsScreen(
    viewModel: BillingViewModel,
    onCartResumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heldCarts by viewModel.heldCarts.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active, 1: Completed

    var cartToDelete by remember { mutableStateOf<HeldCartEntity?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "Held Bills",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                )
                Text(
                    text = "Resume parked customer orders to continue checkout",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryLight)
                )
            }

            // Tabs: [Active (N)] [Completed] matching Screen 9 mockup
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickBillChip(
                    text = "Active",
                    badgeCount = heldCarts.size,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                QuickBillChip(
                    text = "Completed",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }

            if (selectedTab == 1) {
                // Completed orders explanation
                QuickBillEmptyState(
                    icon = Icons.Default.CheckCircleOutline,
                    title = "No recent completed parked orders",
                    description = "Completed orders are moved directly into Sales History",
                    modifier = Modifier.weight(1f)
                )
            } else if (heldCarts.isEmpty()) {
                QuickBillEmptyState(
                    icon = Icons.Default.PauseCircleFilled,
                    title = "No Active Held Bills",
                    description = "When billing customers, tap 'Hold' in the cart to park it here temporarily.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(heldCarts, key = { it.id }) { heldCart ->
                        HeldBillCard(
                            heldCart = heldCart,
                            onResume = {
                                viewModel.resumeCart(heldCart)
                                onCartResumed()
                            },
                            onDelete = { cartToDelete = heldCart }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    cartToDelete?.let { cart ->
        AlertDialog(
            onDismissRequest = { cartToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorCoral) },
            title = { Text("Delete Held Bill?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to discard order '${cart.note}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCart() // or delete directly
                        cartToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCoral)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { cartToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Compact held bill card matching Screen 9 Mockup:
// HB-00012
// 3 items · ₹320
// 01 Oct · 02:15 PM
// [Resume] (light emerald pill)
@Composable
private fun HeldBillCard(
    heldCart: HeldCartEntity,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()).format(Date(heldCart.createdAt))
    val codeStr = "HB-${String.format(Locale.US, "%05d", heldCart.id)}"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, OutlineLight),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Bill Code
                Text(
                    text = codeStr,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Item count & Total
                Text(
                    text = "${heldCart.itemCount} items • ₹ ${String.format(Locale.US, "%.2f", heldCart.totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryLight
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Note & Date
                Text(
                    text = "$dateStr ${if (heldCart.note.isNotBlank() && heldCart.note != "Held Order") "• " + heldCart.note else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMutedLight,
                        fontSize = 11.sp
                    )
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resume Button (Light Emerald Pill matching mockup 9)
                Surface(
                    onClick = onResume,
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldContainer,
                    modifier = Modifier.height(38.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Resume",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}
