package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.icons.AppIcons
import com.example.ui.theme.LocalSwitchboardColors
import com.example.util.BackupUtil

@Composable
fun SavedBoardsDialog(
    savedBoards: List<String>,
    currentBoardId: String,
    onSelectBoard: (String) -> Unit,
    onAddBoard: (String) -> Unit,
    onRemoveBoard: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val extraColors = LocalSwitchboardColors.current
    var newBoardInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    val invalidIdMessage = stringResource(R.string.saved_boards_invalid_id)

    val effectiveBoards = if (savedBoards.isEmpty()) listOf(currentBoardId) else savedBoards

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.saved_boards_title),
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
                Text(
                    text = stringResource(R.string.saved_boards_shared_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = extraColors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Add new board input + button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newBoardInput,
                        onValueChange = {
                            newBoardInput = it
                            validationError = null
                        },
                        label = { Text("New Board ID") },
                        singleLine = true,
                        isError = validationError != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_board_id_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = extraColors.brandAccent,
                            unfocusedBorderColor = extraColors.cardBorder,
                            focusedTextColor = extraColors.textPrimary,
                            unfocusedTextColor = extraColors.textPrimary
                        )
                    )

                    Button(
                        onClick = {
                            val trimmed = newBoardInput.trim()
                            if (!BackupUtil.isValidBoardId(trimmed)) {
                                validationError = invalidIdMessage
                            } else {
                                onAddBoard(trimmed)
                                newBoardInput = ""
                                validationError = null
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extraColors.brandAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("add_saved_board_button")
                    ) {
                        Icon(AppIcons.Add, contentDescription = "Add board", modifier = Modifier.size(18.dp))
                    }
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Saved boards list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    effectiveBoards.forEach { boardId ->
                        val isSelected = boardId == currentBoardId
                        val canRemove = effectiveBoards.size > 1

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) extraColors.brandAccent.copy(alpha = 0.14f)
                                    else extraColors.offCard
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) extraColors.brandAccent else extraColors.cardBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectBoard(boardId) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = AppIcons.Check,
                                        contentDescription = "Selected",
                                        tint = extraColors.brandAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = boardId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) extraColors.brandAccent else extraColors.textPrimary
                                )
                            }

                            IconButton(
                                onClick = { if (canRemove) onRemoveBoard(boardId) },
                                enabled = canRemove,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Delete,
                                    contentDescription = "Remove $boardId",
                                    tint = if (canRemove) MaterialTheme.colorScheme.error
                                    else extraColors.textSecondary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = extraColors.brandAccent)
            }
        }
    )
}
