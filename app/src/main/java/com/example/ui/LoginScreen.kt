package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FirebaseProjectConfig
import com.example.ui.icons.AppIcons
import com.example.ui.theme.LocalSwitchboardColors

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    firebaseConfig: FirebaseProjectConfig,
    onLoginClick: (email: String, pass: String) -> Unit,
    onEnterDemoMode: () -> Unit,
    onSaveFirebaseConfig: (apiKey: String, projectId: String, databaseUrl: String) -> Unit
) {
    val extraColors = LocalSwitchboardColors.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var localValidationMsg by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val displayError = localValidationMsg ?: errorMessage

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(extraColors.brandAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(extraColors.brandAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Power,
                            contentDescription = "Smart Switchboard",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Smart Switchboard",
                    style = MaterialTheme.typography.headlineLarge,
                    color = extraColors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Control your 4-relay ESP8266 board live",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extraColors.textSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localValidationMsg = null
                    },
                    label = { Text("Email address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.brandAccent,
                        unfocusedBorderColor = extraColors.cardBorder,
                        focusedTextColor = extraColors.textPrimary,
                        unfocusedTextColor = extraColors.textPrimary,
                        focusedLabelColor = extraColors.brandAccent,
                        unfocusedLabelColor = extraColors.textSecondary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localValidationMsg = null
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (email.isBlank() || password.isBlank()) {
                                localValidationMsg = "Please enter both email and password"
                            } else if (!isLoading) {
                                onLoginClick(email, password)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) AppIcons.VisibilityOff else AppIcons.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = extraColors.textSecondary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.brandAccent,
                        unfocusedBorderColor = extraColors.cardBorder,
                        focusedTextColor = extraColors.textPrimary,
                        unfocusedTextColor = extraColors.textPrimary,
                        focusedLabelColor = extraColors.brandAccent,
                        unfocusedLabelColor = extraColors.textSecondary
                    )
                )

                // Error message and API key helper
                AnimatedVisibility(
                    visible = displayError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (displayError != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = displayError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            if (displayError.contains("API key", ignoreCase = true) || displayError.contains("Configure", ignoreCase = true)) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showConfigDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = extraColors.brandAccent,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("Enter API Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = onEnterDemoMode,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = extraColors.offCard,
                                            contentColor = extraColors.textPrimary
                                        ),
                                        modifier = Modifier.weight(1f).height(38.dp)
                                    ) {
                                        Text("Try Demo Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login button: always clickable when not loading, validates on click
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (email.isBlank() || password.isBlank()) {
                            localValidationMsg = "Please enter both email and password"
                        } else {
                            localValidationMsg = null
                            onLoginClick(email, password)
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extraColors.brandAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = extraColors.brandAccent.copy(alpha = 0.6f)
                    )
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Connecting...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = "Login",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Action Buttons: Demo Mode & Configure Firebase
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEnterDemoMode,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("demo_mode_button")
                    ) {
                        Icon(AppIcons.Power, contentDescription = null, modifier = Modifier.size(16.dp), tint = extraColors.brandAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Demo Mode",
                            style = MaterialTheme.typography.labelMedium,
                            color = extraColors.textPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = { showConfigDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("open_firebase_config_button")
                    ) {
                        Icon(AppIcons.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = extraColors.textSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (firebaseConfig.isCustomConfigured) "Firebase Config" else "Set API Key",
                            style = MaterialTheme.typography.labelMedium,
                            color = extraColors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Requires email & password registered in your Firebase Console",
                    style = MaterialTheme.typography.labelSmall,
                    color = extraColors.textSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showConfigDialog) {
        FirebaseConfigDialog(
            currentConfig = firebaseConfig,
            onDismiss = { showConfigDialog = false },
            onSave = onSaveFirebaseConfig
        )
    }
}
