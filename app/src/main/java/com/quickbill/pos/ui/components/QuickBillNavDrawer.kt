package com.quickbill.pos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.local.entity.UserEntity
import com.quickbill.pos.data.model.UserRole
import com.quickbill.pos.ui.navigation.Screen
import com.quickbill.pos.ui.theme.*

import androidx.compose.foundation.clickable
import com.quickbill.pos.data.repository.AppThemeMode

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
    currentThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onSelectTheme: (AppThemeMode) -> Unit = {},
    onOpenAppearanceDialog: () -> Unit = {},
    onKdsClick: () -> Unit = {},
    onChangeDeviceModeClick: () -> Unit = {},
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    onLogoutClick: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = SurfaceWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // =========================================================================
            // Drawer Header with QuickBill Branding & User Info
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmeraldPrimary)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QuickBillLogoBadge(size = 46.dp, iconSize = 26.dp)

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "QuickBill",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 20.sp
                                )
                            )
                            Text(
                                text = "Smart POS for Your Business",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // User Profile Banner
                    if (currentUser != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (currentUser.role == UserRole.ADMIN) CoralAccent else Color.White,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = currentUser.fullName.take(1).uppercase(),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (currentUser.role == UserRole.ADMIN) Color.White else EmeraldPrimary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = currentUser.fullName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "${currentUser.role.name} • @${currentUser.username}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // =========================================================================
            // Navigation Links
            // =========================================================================
            val navItems = listOf(
                DrawerItem("Dashboard", Screen.Dashboard.route, Icons.Default.Dashboard),
                DrawerItem("Billing Terminal", Screen.Billing.route, Icons.Default.PointOfSale),
                DrawerItem("Held Bills", Screen.HeldCarts.route, Icons.Default.PauseCircleFilled, if (heldCartsCount > 0) "$heldCartsCount" else null),
                DrawerItem("Products & Inventory", Screen.Products.route, Icons.Default.Inventory2),
                DrawerItem("Sales History", Screen.SalesHistory.route, Icons.AutoMirrored.Filled.ReceiptLong),
                DrawerItem("Daily Analytics", Screen.DailyReport.route, Icons.Default.Assessment)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                navItems.forEach { item ->
                    val isSelected = currentRoute == item.route

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) EmeraldPrimary else TextSecondaryLight
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) EmeraldPrimary else TextPrimaryLight
                                )
                            )
                        },
                        badge = {
                            item.badge?.let { count ->
                                Surface(
                                    shape = CircleShape,
                                    color = CoralAccent,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = count,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        selected = isSelected,
                        onClick = {
                            onCloseDrawer()
                            onNavigate(item.route)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = EmeraldContainer,
                            selectedTextColor = EmeraldPrimary,
                            unselectedTextColor = TextPrimaryLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

            // =========================================================================
            // Appearance (Dark, Light, System) Manual Changing Section
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenAppearanceDialog() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Appearance",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldContainer,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = when (currentThemeMode) {
                                AppThemeMode.SYSTEM -> "System"
                                AppThemeMode.LIGHT -> "Light"
                                AppThemeMode.DARK -> "Dark"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3-way Segmented Switcher (System, Light, Dark)
                AppearanceSegmentedRow(
                    currentThemeMode = currentThemeMode,
                    onSelectTheme = onSelectTheme
                )
            }

            // =========================================================================
            // Drawer Footer with Logout
            // =========================================================================
            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "Kitchen Display (KDS)",
                        tint = EmeraldPrimary
                    )
                },
                label = {
                    Text(
                        text = "Kitchen Display (KDS)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryLight
                        )
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onKdsClick()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = "Change Device Mode",
                        tint = EmeraldPrimary
                    )
                },
                label = {
                    Text(
                        text = "Change Device Mode",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryLight
                        )
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onChangeDeviceModeClick()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))


            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = ErrorCoral
                    )
                },
                label = {
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ErrorCoral
                        )
                    )
                },
                selected = false,
                onClick = {
                    onCloseDrawer()
                    onLogoutClick()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            Text(
                text = "QuickBill POS v1.0.0 • Offline Ready",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = TextMutedLight
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}
