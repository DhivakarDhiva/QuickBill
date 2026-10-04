package com.quickbill.pos.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.pos.data.model.UserRole
import com.quickbill.pos.data.repository.AppThemeMode
import com.quickbill.pos.ui.components.AppearanceDialog
import com.quickbill.pos.ui.components.QuickBillButton
import com.quickbill.pos.ui.components.QuickBillLogoBadge
import com.quickbill.pos.ui.components.QuickBillTextField
import com.quickbill.pos.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeUsers by viewModel.activeUsers.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

            // =========================================================================
            // Top Hero Banner (Rich Coffee / Retail Shop Theme with Brand Badge)
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 130.dp else 270.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF231812),
                                Color(0xFF3D271E),
                                Color(0xFF1E130D)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Subtle decorative radial glow
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 120.dp else 200.dp)
                        .clip(CircleShape)
                        .background(Color(0x15FF8A50))
                )

                // Quick Theme / Appearance Switcher Button
                val currentThemeMode = LocalAppThemeMode.current
                val onThemeChange = LocalOnThemeChange.current
                var showAppearanceDialog by remember { mutableStateOf(false) }

                Surface(
                    onClick = { showAppearanceDialog = true },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (currentThemeMode) {
                                AppThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = "Change Appearance",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (showAppearanceDialog) {
                    AppearanceDialog(
                        currentThemeMode = currentThemeMode,
                        onSelectTheme = { mode ->
                            onThemeChange(mode)
                        },
                        onDismiss = { showAppearanceDialog = false }
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(if (isLandscape) 8.dp else 16.dp)
                ) {
                    QuickBillLogoBadge(
                        size = if (isLandscape) 46.dp else 72.dp,
                        iconSize = if (isLandscape) 26.dp else 40.dp
                    )

                    Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 14.dp))

                    Text(
                        text = "QuickBill",
                        style = (if (isLandscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium).copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )

                    if (!isLandscape) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Smart POS for Your Business",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.75f),
                                letterSpacing = 0.2.sp
                            )
                        )
                    }
                }
            }

            // =========================================================================
            // Main Login Card (White Surface with Rounded Top Corners)
            // =========================================================================
            Surface(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .offset(y = (-20).dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = SurfaceWhite,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = if (isLandscape) 16.dp else 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                    )

                    Text(
                        text = "Login to continue",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondaryLight
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Username Input
                    QuickBillTextField(
                        value = uiState.username,
                        onValueChange = { viewModel.onUsernameChange(it) },
                        placeholder = "Username",
                        leadingIcon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password / PIN Input
                    QuickBillTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        placeholder = "Password / 4-Digit PIN",
                        leadingIcon = Icons.Default.Lock,
                        trailingIcon = {
                            IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                Icon(
                                    imageVector = if (uiState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Password",
                                    tint = TextSecondaryLight
                                )
                            }
                        },
                        visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { viewModel.attemptLogin() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Role Selector: [Admin] [Cashier]
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceMutedLight,
                        border = BorderStroke(1.dp, OutlineLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Admin Tab
                            val isAdmin = uiState.selectedRole == UserRole.ADMIN
                            Surface(
                                onClick = { viewModel.onRoleSelected(UserRole.ADMIN) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAdmin) EmeraldPrimary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isAdmin) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = "Admin",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isAdmin) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isAdmin) Color.White else TextSecondaryLight
                                        )
                                    )
                                }
                            }

                            // Cashier Tab
                            val isCashier = uiState.selectedRole == UserRole.CASHIER
                            Surface(
                                onClick = { viewModel.onRoleSelected(UserRole.CASHIER) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCashier) EmeraldPrimary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isCashier) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = "Cashier",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = if (isCashier) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCashier) Color.White else TextSecondaryLight
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Error Message
                    AnimatedVisibility(visible = uiState.error != null) {
                        Surface(
                            color = ErrorCoralContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            Text(
                                text = uiState.error.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = OnErrorCoral,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Login CTA Button
                    QuickBillButton(
                        text = "Login",
                        onClick = { viewModel.attemptLogin() },
                        isLoading = uiState.isLoading,
                        containerColor = EmeraldPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick Cashier Switch for Testing / Speed
                    if (activeUsers.isNotEmpty()) {
                        HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Quick Demo Profiles",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMutedLight,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            activeUsers.forEach { user ->
                                Surface(
                                    onClick = { viewModel.quickSelectUser(user) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = SurfaceMutedLight,
                                    border = BorderStroke(1.dp, OutlineLight),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = user.fullName.split(" ").first(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryLight
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = user.role.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                color = if (user.role == UserRole.ADMIN) CoralAccent else EmeraldPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
