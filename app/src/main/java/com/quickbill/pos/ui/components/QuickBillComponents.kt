/*
 * QuickBill + QuickKitchen
 *
 * Author: Dhivakar
 * Role: Android Developer
 *
 * Copyright (c) 2026 Dhivakar
 *
 * This file is part of the QuickBill + QuickKitchen project.
 * The original implementation and modifications in this file were
 * created by Dhivakar for the project/assignment.
 *
 * QuickBill-QuickKitchen-Author: Dhivakar
 *
 * Do not remove or alter this attribution notice.
 */

package com.quickbill.pos.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quickbill.pos.data.local.entity.ProductEntity
import com.quickbill.pos.ui.theme.*
import java.util.Locale

// =========================================================================
// QuickBill Brand Logo Badge
// Matches the reference image: Rounded badge with Shopping Bag + Price Tag
// =========================================================================

@Composable
fun QuickBillLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    iconSize: Dp = 36.dp
) {
    Surface(
        modifier = modifier
            .size(size)
            .shadow(6.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFAF5),
                            Color(0xFFFFF0E6)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalMall,
                contentDescription = "QuickBill Logo",
                tint = CoralAccent,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

// =========================================================================
// QuickBill Primary Filled Button (Deep Emerald Green Pill)
// =========================================================================

@Composable
fun QuickBillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    containerColor: Color = EmeraldPrimary,
    contentColor: Color = Color.White,
    height: Dp = QuickBillDimens.buttonHeight
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(if (enabled) 4.dp else 0.dp, ButtonShape, ambientColor = containerColor.copy(alpha = 0.3f)),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// QuickBill Outlined Button
// =========================================================================

@Composable
fun QuickBillOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tintColor: Color = EmeraldPrimary,
    borderColor: Color = EmeraldPrimary.copy(alpha = 0.4f),
    height: Dp = QuickBillDimens.buttonHeight
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = ButtonShape,
        border = BorderStroke(1.5.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = tintColor,
            disabledContentColor = tintColor.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = tintColor
                )
            )
        }
    }
}

// =========================================================================
// QuickBill Custom Text Field
// =========================================================================

@Composable
fun QuickBillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedLight)) } },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp)) } },
            trailingIcon = trailingIcon,
            isError = isError,
            singleLine = singleLine,
            readOnly = readOnly,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = InputShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                disabledContainerColor = SurfaceMutedLight,
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = OutlineLight,
                errorBorderColor = ErrorCoral,
                focusedTextColor = TextPrimaryLight,
                unfocusedTextColor = TextPrimaryLight
            )
        )

        AnimatedVisibility(visible = isError && errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(color = ErrorCoral),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

// =========================================================================
// QuickBill Search Bar with Optional Scan Button
// =========================================================================

@Composable
fun QuickBillSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search by name, SKU or barcode...",
    onScanClick: (() -> Unit)? = null,
    onFilterClick: (() -> Unit)? = null,
    isFilterActive: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(QuickBillDimens.inputHeight),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, OutlineLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondaryLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMutedLight),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSecondaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (onScanClick != null) {
            Surface(
                onClick = onScanClick,
                shape = RoundedCornerShape(14.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, OutlineLight),
                modifier = Modifier.size(QuickBillDimens.inputHeight)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Barcode",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (onFilterClick != null) {
            Surface(
                onClick = onFilterClick,
                shape = RoundedCornerShape(14.dp),
                color = if (isFilterActive) EmeraldContainer else SurfaceWhite,
                border = BorderStroke(1.dp, if (isFilterActive) EmeraldPrimary else OutlineLight),
                modifier = Modifier.size(QuickBillDimens.inputHeight)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (isFilterActive) EmeraldPrimary else TextSecondaryLight,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// QuickBill Card Container
// =========================================================================

@Composable
fun QuickBillCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceWhite,
    borderColor: Color = OutlineLight,
    shape: RoundedCornerShape = CardShape,
    elevation: Dp = QuickBillDimens.cardElevation,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, shape, ambientColor = Color(0x0A000000))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// =========================================================================
// QuickBill Chip (Category & Status Pills)
// Matches mockup: Deep emerald active pill, warm gray inactive pill
// =========================================================================

@Composable
fun QuickBillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    badgeCount: Int? = null
) {
    Surface(
        onClick = onClick,
        shape = ChipShape,
        color = if (selected) EmeraldPrimary else SurfaceWhite,
        border = BorderStroke(
            1.dp,
            if (selected) EmeraldPrimary else OutlineLight
        ),
        shadowElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else TextSecondaryLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else TextPrimaryLight,
                    fontSize = 12.5.sp
                )
            )
            if (badgeCount != null && badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = if (selected) Color.White.copy(alpha = 0.25f) else EmeraldContainer
                ) {
                    Text(
                        text = "$badgeCount",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else EmeraldPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// QuickBill KPI Card (Dashboard Metrics)
// Matches screen 2 mockup: Clean, compact 2x2 cards with trend badges
// =========================================================================

@Composable
fun QuickBillKpiCard(
    title: String,
    value: String,
    trend: String? = null,
    trendPositive: Boolean = true,
    icon: ImageVector,
    iconColor: Color = EmeraldPrimary,
    iconBgColor: Color = EmeraldContainer,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, OutlineLight),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconBgColor,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (trend != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (trendPositive) SuccessGreenContainer else CoralContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (trendPositive) Icons.Default.ArrowDropUp else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (trendPositive) SuccessGreen else CoralAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = trend,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (trendPositive) SuccessGreen else CoralAccent
                                    )
                                )
                            }
                        }
                    }

                    if (onClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = iconColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Tap to open",
                                    tint = iconColor,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondaryLight,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = TextPrimaryLight
                )
            )

            if (onClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Tap for details",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iconColor
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// QuickBill Product Visual Icon / Avatar Generator
// Returns a rich culinary/retail visual representation with warm pastel background
// =========================================================================

@Composable
fun ProductThumbnail(
    productName: String,
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val isDark = isSystemInDarkTheme()
    val (icon, bgGradient) = when {
        productName.contains("Coffee", ignoreCase = true) ->
            Icons.Default.Coffee to if (isDark) listOf(Color(0xFF3E2723), Color(0xFF2C1B17)) else listOf(Color(0xFFFDE8D7), Color(0xFFF7D2B6))
        productName.contains("Tea", ignoreCase = true) ->
            Icons.Default.EmojiFoodBeverage to if (isDark) listOf(Color(0xFF1B4332), Color(0xFF143024)) else listOf(Color(0xFFE4F5E6), Color(0xFFC7EBD0))
        productName.contains("Burger", ignoreCase = true) ->
            Icons.Default.LunchDining to if (isDark) listOf(Color(0xFF43281C), Color(0xFF331E15)) else listOf(Color(0xFFFFE6D5), Color(0xFFFFD2BA))
        productName.contains("Fries", ignoreCase = true) || productName.contains("Crisps", ignoreCase = true) ->
            Icons.Default.Fastfood to if (isDark) listOf(Color(0xFF42351A), Color(0xFF332914)) else listOf(Color(0xFFFFF0D0), Color(0xFFFFE2A3))
        productName.contains("Sandwich", ignoreCase = true) || productName.contains("Bread", ignoreCase = true) ->
            Icons.Default.BakeryDining to if (isDark) listOf(Color(0xFF3E2F23), Color(0xFF2D2219)) else listOf(Color(0xFFF8E7D4), Color(0xFFEED1B4))
        productName.contains("Cake", ignoreCase = true) || productName.contains("Chocolate", ignoreCase = true) ->
            Icons.Default.Cake to if (isDark) listOf(Color(0xFF44242A), Color(0xFF321A1F)) else listOf(Color(0xFFF5E4E4), Color(0xFFE8C6C6))
        category.contains("Dairy", ignoreCase = true) || productName.contains("Milk", ignoreCase = true) || productName.contains("Butter", ignoreCase = true) ->
            Icons.Default.Egg to if (isDark) listOf(Color(0xFF1E3A5F), Color(0xFF152943)) else listOf(Color(0xFFE8F2FF), Color(0xFFCFE3FE))
        category.contains("Groceries", ignoreCase = true) || productName.contains("Rice", ignoreCase = true) || productName.contains("Atta", ignoreCase = true) ->
            Icons.Default.Kitchen to if (isDark) listOf(Color(0xFF3D3425), Color(0xFF2B251A)) else listOf(Color(0xFFF9F0E0), Color(0xFFEFE0C2))
        else ->
            Icons.Default.ShoppingBag to if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A)) else listOf(Color(0xFFE9F3ED), Color(0xFFD4E8DC))
    }

    val iconTint = if (isDark) {
        when {
            productName.contains("Coffee", ignoreCase = true) -> Color(0xFFFFCC99)
            productName.contains("Tea", ignoreCase = true) -> Color(0xFF86EFAC)
            productName.contains("Burger", ignoreCase = true) -> Color(0xFFFFAB78)
            productName.contains("Fries", ignoreCase = true) || productName.contains("Crisps", ignoreCase = true) -> Color(0xFFFDE047)
            productName.contains("Sandwich", ignoreCase = true) || productName.contains("Bread", ignoreCase = true) -> Color(0xFFFED7AA)
            productName.contains("Cake", ignoreCase = true) || productName.contains("Chocolate", ignoreCase = true) -> Color(0xFFF9A8D4)
            category.contains("Dairy", ignoreCase = true) || productName.contains("Milk", ignoreCase = true) -> Color(0xFF93C5FD)
            else -> DarkEmeraldPrimary
        }
    } else {
        Color(0xFF4A3423).copy(alpha = 0.85f)
    }

    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        border = if (isDark) BorderStroke(1.dp, OutlineDark) else null,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(bgGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = productName,
                tint = iconTint,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

// =========================================================================
// QuickBill Product Card (Billing Grid)
// Matches screen 5: Soft warm card with thumbnail, name, and bold price
// =========================================================================

@Composable
fun QuickBillProductCard(
    product: ProductEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cartQuantity: Int = 0,
    onIncrement: (() -> Unit)? = null,
    onDecrement: (() -> Unit)? = null
) {
    val isOutOfStock = product.stockQuantity <= 0
    val isLowStock = !isOutOfStock && product.stockQuantity <= product.minStockAlert

    Surface(
        onClick = onClick,
        enabled = !isOutOfStock,
        shape = ProductCardShape,
        color = SurfaceWhite,
        border = BorderStroke(
            1.dp,
            if (isOutOfStock) OutlineLight.copy(alpha = 0.5f) else if (cartQuantity > 0) EmeraldPrimary else OutlineLight
        ),
        shadowElevation = if (isOutOfStock) 0.dp else 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Visual Product Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isOutOfStock) SurfaceMutedLight else WarmBackgroundLight
                    ),
                contentAlignment = Alignment.Center
            ) {
                ProductThumbnail(
                    productName = product.name,
                    category = product.category,
                    size = 72.dp
                )

                // Stock badges overlay
                if (isOutOfStock) {
                    Surface(
                        color = ErrorCoral.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "Out of Stock",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isLowStock) {
                    Surface(
                        color = WarningAmber.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "LOW STOCK (${product.stockQuantity})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 8.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOutOfStock) TextMutedLight else TextPrimaryLight
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Price & Tax
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${String.format(Locale.US, "%.0f", product.price)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isOutOfStock) TextMutedLight else TextPrimaryLight
                    )
                )

                if (product.taxRate > 0) {
                    Text(
                        text = "${product.taxRate.toInt()}% Tax",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMutedLight,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Direct Cart Item Stepper on the card
            if (cartQuantity > 0 && onIncrement != null && onDecrement != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldContainer,
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = "$cartQuantity in cart",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )

                        IconButton(
                            onClick = onIncrement,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else if (!isOutOfStock) {
                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceMutedLight,
                    modifier = Modifier.fillMaxWidth().height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelSmall.copy(
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


// =========================================================================
// QuickBill Bottom Navigation Bar
// Matches screen 2: Home, Products, Sales, Reports + Terminal indicator
// =========================================================================

data class QuickBillNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun QuickBillBottomBar(
    currentRoute: String,
    items: List<QuickBillNavItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color(0x30000000),
                    ambientColor = Color(0x15000000)
                ),
            shape = RoundedCornerShape(32.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, OutlineLight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    BottomNavItemButton(
                        item = item,
                        isSelected = currentRoute == item.route,
                        onClick = { onItemClick(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItemButton(
    item: QuickBillNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 200,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "bottomNavIconScale"
    )

    val pillBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) EmeraldContainer else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 200,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "bottomNavPillBgColor"
    )

    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) EmeraldPrimary else TextSecondaryLight,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 200,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "bottomNavContentColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(pillBgColor)
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    fontSize = 10.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =========================================================================
// QuickBill Landscape Navigation Rail
// Used automatically in landscape orientation on phones/tablets for zero vertical obstruction
// =========================================================================

@Composable
fun QuickBillNavRail(
    currentRoute: String,
    items: List<QuickBillNavItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(68.dp)
            .fillMaxHeight(),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, OutlineLight),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                NavRailItemButton(
                    item = item,
                    isSelected = currentRoute == item.route,
                    onClick = { onItemClick(item.route) }
                )
            }
        }
    }
}

@Composable
private fun NavRailItemButton(
    item: QuickBillNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1.0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 200,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "navRailIconScale"
    )

    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) EmeraldPrimary else TextSecondaryLight,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 200,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "navRailContentColor"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) EmeraldContainer else Color.Transparent,
        modifier = Modifier.size(52.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                ),
                maxLines = 1
            )
        }
    }
}

// =========================================================================
// QuickBill Financial Price Row
// =========================================================================

@Composable
fun QuickBillPriceRow(
    label: String,
    amount: Double,
    isNegative: Boolean = false,
    isTotal: Boolean = false,
    valueColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTotal) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimaryLight)
            } else {
                MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight)
            }
        )

        val formatted = String.format(Locale.US, "%.2f", amount)
        val displayText = if (isNegative) "- ₹$formatted" else "₹$formatted"

        Text(
            text = displayText,
            style = if (isTotal) {
                MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = valueColor ?: EmeraldPrimary
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor ?: if (isNegative) CoralAccent else TextPrimaryLight
                )
            }
        )
    }
}

// =========================================================================
// QuickBill Empty State
// =========================================================================

@Composable
fun QuickBillEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.SearchOff,
    description: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = SurfaceMutedLight,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextSecondaryLight,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                ),
                textAlign = TextAlign.Center
            )

            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryLight
                    ),
                    textAlign = TextAlign.Center
                )
            }

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                QuickBillButton(
                    text = actionText,
                    onClick = onActionClick,
                    modifier = Modifier.width(180.dp),
                    height = 42.dp
                )
            }
        }
    }
}

// =========================================================================
// QuickBill Modern Dialog Container
// =========================================================================

@Composable
fun QuickBillDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(if (isLandscape) 0.65f else 0.92f)
                .wrapContentHeight()
                .shadow(QuickBillDimens.dialogElevation, DialogShape),
            shape = DialogShape,
            color = SurfaceWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isLandscape) 320.dp else 650.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondaryLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                content()
            }
        }
    }
}
