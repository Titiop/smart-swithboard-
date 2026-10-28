package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FirebaseProjectConfig
import com.example.ui.theme.LocalSwitchboardColors

@Composable
fun FirebaseConfigDialog(
    currentConfig: FirebaseProjectConfig,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, projectId: String, databaseUrl: String) -> Unit
) {
    val extraColors = LocalSwitchboardColors.current
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var projectId by remember { mutableStateOf(currentConfig.projectId) }
    var databaseUrl by remember { mutableStateOf(currentConfig.databaseUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Connect Firebase Project",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = extraColors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Quick Guide Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(extraColors.brandAccent.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Where to find these in Firebase Console:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.brandAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Open console.firebase.google.com -> Project Settings (gear icon) -> General.\n2. Copy 'Web API Key' (starts with AIzaSy) & 'Project ID'.\n3. Database URL is in Realtime Database section.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extraColors.textPrimary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Web API Key
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Web API Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_api_key_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.brandAccent,
                        unfocusedBorderColor = extraColors.cardBorder,
                        focusedTextColor = extraColors.textPrimary,
                        unfocusedTextColor = extraColors.textPrimary,
                        focusedLabelColor = extraColors.brandAccent,
                        unfocusedLabelColor = extraColors.textSecondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Project ID
                OutlinedTextField(
                    value = projectId,
                    onValueChange = {
                        projectId = it
                        if (databaseUrl.isBlank() && it.isNotBlank()) {
                            databaseUrl = "https://${it.trim()}-default-rtdb.firebaseio.com"
                        }
                    },
                    label = { Text("Project ID") },
                    placeholder = { Text("e.g. my-switchboard-12345") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_project_id_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.brandAccent,
                        unfocusedBorderColor = extraColors.cardBorder,
                        focusedTextColor = extraColors.textPrimary,
                        unfocusedTextColor = extraColors.textPrimary,
                        focusedLabelColor = extraColors.brandAccent,
                        unfocusedLabelColor = extraColors.textSecondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Realtime Database URL
                OutlinedTextField(
                    value = databaseUrl,
                    onValueChange = { databaseUrl = it },
                    label = { Text("Realtime Database URL") },
                    placeholder = { Text("https://...-default-rtdb.firebaseio.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_database_url_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extraColors.brandAccent,
                        unfocusedBorderColor = extraColors.cardBorder,
                        focusedTextColor = extraColors.textPrimary,
                        unfocusedTextColor = extraColors.textPrimary,
                        focusedLabelColor = extraColors.brandAccent,
                        unfocusedLabelColor = extraColors.textSecondary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiKey.trim(), projectId.trim(), databaseUrl.trim())
                    onDismiss()
                },
                enabled = apiKey.isNotBlank() && projectId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = extraColors.brandAccent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("save_firebase_config_button")
            ) {
                Text("Save & Connect", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = extraColors.textSecondary)
            }
        }
    )
}
