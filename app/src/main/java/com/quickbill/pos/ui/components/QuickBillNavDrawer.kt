package com.quickbill.pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.ui.navigation.Screen
import com.quickbill.pos.ui.theme.PrimaryGreen
import com.quickbill.pos.ui.theme.PrimaryGreenLight

data class DrawerItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val badge: String? = null
)

@Composable
fun QuickBillNavDrawerContent(
    currentRoute: String,
    currentUser: UserEntity?,
    heldCartsCount: Int,
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        // Drawer Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = PrimaryGreen,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "QuickBill POS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = "Modern Retail Billing",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current Cashier Info
                if (currentUser != null) {
                    Text(
                        text = "Signed in as",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = "${currentUser.fullName} (${currentUser.role.name})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val items = listOf(
            DrawerItem("Billing Terminal", Screen.Billing.route, Icons.Default.ShoppingCart),
            DrawerItem("Parked Orders", Screen.HeldCarts.route, Icons.Default.PauseCircleFilled, if (heldCartsCount > 0) "$heldCartsCount" else null),
            DrawerItem("Product Catalog", Screen.Products.route, Icons.Default.Inventory2),
            DrawerItem("Sales History & Returns", Screen.SalesHistory.route, Icons.Default.ReceiptLong),
            DrawerItem("Daily Analytics", Screen.DailyReport.route, Icons.Default.Assessment)
        )

        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                badge = {
                    item.badge?.let { count ->
                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                            Text(count)
                        }
                    }
                },
                selected = isSelected,
                onClick = {
                    onCloseDrawer()
                    onNavigate(item.route)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = PrimaryGreen.copy(alpha = 0.12f),
                    selectedTextColor = PrimaryGreen,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "QuickBill POS v1.0.0 • Offline Ready",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}
