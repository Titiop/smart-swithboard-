package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BiometricLockScreen
import com.example.ui.LoginScreen
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.UiSnackbarEvent
import com.example.ui.theme.SmartSwitchboardTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            viewModel.onAppStart()
        }

        setContent {
            val appPreferences by viewModel.appPreferences.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
            val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
            val displayUserEmail by viewModel.displayUserEmail.collectAsStateWithLifecycle()
            val isLoggingIn by viewModel.isLoggingIn.collectAsStateWithLifecycle()
            val loginError by viewModel.loginError.collectAsStateWithLifecycle()
            val switches by viewModel.switches.collectAsStateWithLifecycle()
            val switchVisibility by viewModel.switchVisibility.collectAsStateWithLifecycle()
            val savedBoards by viewModel.savedBoards.collectAsStateWithLifecycle()
            val scenes by viewModel.scenes.collectAsStateWithLifecycle()
            val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
            val boardStatus by viewModel.boardStatus.collectAsStateWithLifecycle()
            val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
            val firebaseConfig by viewModel.firebaseConfig.collectAsStateWithLifecycle()
            val schedules by viewModel.schedules.collectAsStateWithLifecycle()
            val timers by viewModel.timers.collectAsStateWithLifecycle()
            val serverTimeOffsetMs by viewModel.serverTimeOffsetMs.collectAsStateWithLifecycle()
            val autoOffMap by viewModel.autoOffMap.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(appPreferences.hideRecentPreview) {
                if (appPreferences.hideRecentPreview) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.snackbarEvents.collect { event ->
                    when (event) {
                        is UiSnackbarEvent.ShowMessage -> {
                            snackbarHostState.showSnackbar(event.message)
                        }
                    }
                }
            }

            SmartSwitchboardTheme(
                themeMode = appPreferences.themeMode,
                dynamicColor = appPreferences.dynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!isLoggedIn) {
                        LoginScreen(
                            isLoading = isLoggingIn,
                            errorMessage = loginError,
                            firebaseConfig = firebaseConfig,
                            onLoginClick = { email, pass ->
                                viewModel.login(email, pass)
                            },
                            onEnterDemoMode = {
                                viewModel.enterDemoMode()
                            },
                            onSaveFirebaseConfig = { apiKey, projectId, dbUrl ->
                                viewModel.updateFirebaseConfig(apiKey, projectId, dbUrl)
                            }
                        )
                    } else if (isAppLocked) {
                        BiometricLockScreen(
                            onUnlocked = { viewModel.unlockApp() }
                        )
                    } else {
                        MainScreen(
                            switches = switches,
                            scenes = scenes,
                            appPreferences = appPreferences,
                            isConnected = isConnected,
                            boardStatus = boardStatus,
                            userEmail = displayUserEmail,
                            snackbarHostState = snackbarHostState,
                            onToggleSwitch = { relay ->
                                viewModel.toggleSwitch(relay)
                            },
                            onSetAllSwitches = { turnOn ->
                                viewModel.setAllSwitches(turnOn)
                            },
                            onApplyScene = { scene ->
                                viewModel.applyScene(scene)
                            },
                            onUpdateSwitchMeta = { id, name, iconKey ->
                                viewModel.updateSwitchMeta(id, name, iconKey)
                            },
                            onSaveScene = { scene ->
                                viewModel.saveScene(scene)
                            },
                            onDeleteScene = { sceneId ->
                                viewModel.deleteScene(sceneId)
                            },
                            onMoveScene = { from, to ->
                                viewModel.moveScene(from, to)
                            },
                            onThemeChange = { mode ->
                                viewModel.updateThemeMode(mode)
                            },
                            onDynamicColorChange = { enabled ->
                                viewModel.updateDynamicColor(enabled)
                            },
                            onHapticChange = { enabled ->
                                viewModel.updateHapticFeedback(enabled)
                            },
                            onBiometricChange = { enabled ->
                                viewModel.updateBiometricLock(enabled)
                            },
                            onBoardIdChange = { newId ->
                                viewModel.updateBoardId(newId)
                            },
                            firebaseConfig = firebaseConfig,
                            onSaveFirebaseConfig = { k, p, d ->
                                viewModel.updateFirebaseConfig(k, p, d)
                            },
                            onLogout = {
                                viewModel.logout()
                            },
                            switchVisibility = switchVisibility,
                            onUpdateSwitchVisibility = { id, visible ->
                                viewModel.updateSwitchVisibility(id, visible)
                            },
                            savedBoards = savedBoards,
                            onAddSavedBoard = { boardId ->
                                viewModel.addSavedBoard(boardId)
                            },
                            onRemoveSavedBoard = { boardId ->
                                viewModel.removeSavedBoard(boardId)
                            },
                            isDemoMode = isDemoMode,
                            onFetchBoardInfo = { boardId ->
                                viewModel.fetchBoardInfo(boardId)
                            },
                            onExportBackup = {
                                viewModel.exportBackupJson()
                            },
                            onRestoreBackup = { payload ->
                                viewModel.restoreBackup(payload)
                            },
                            onShowMessage = { msg ->
                                viewModel.showMessage(msg)
                            },
                            schedules = schedules,
                            timers = timers,
                            serverTimeOffsetMs = serverTimeOffsetMs,
                            onSchedulesSheetOpenChange = { open ->
                                viewModel.setSchedulesSheetOpen(open)
                            },
                            onSaveSchedule = { item, isNew ->
                                viewModel.saveSchedule(item, isNew)
                            },
                            onToggleScheduleEnabled = { item, enabled ->
                                viewModel.toggleScheduleEnabled(item, enabled)
                            },
                            onDeleteSchedule = { id ->
                                viewModel.deleteSchedule(id)
                            },
                            onStartRelayTimer = { relayId, action, minutes ->
                                viewModel.startRelayTimer(relayId, action, minutes)
                            },
                            onCancelRelayTimer = { relayId ->
                                viewModel.cancelRelayTimer(relayId)
                            },
                            onFetchAutoOffMinutes = { relayId ->
                                viewModel.fetchAutoOffMinutes(relayId)
                            },
                            onSaveAutoOffMinutes = { relayId, minutes ->
                                viewModel.saveAutoOffMinutes(relayId, minutes)
                            },
                            onHideRecentPreviewChange = { enabled ->
                                viewModel.updateHideRecentPreview(enabled)
                            },
                            onRequireExternalUnlockChange = { enabled ->
                                viewModel.updateRequireExternalUnlock(enabled)
                            },
                            onSetSchedulesEnabled = { ids, enabled ->
                                viewModel.setSchedulesEnabled(ids, enabled)
                            },
                            onDeleteSchedules = { ids ->
                                viewModel.deleteSchedules(ids)
                            },
                            onRunScheduleNow = { schedule ->
                                viewModel.runScheduleNow(schedule)
                            },
                            autoOffMap = autoOffMap,
                            onAutoOffTabVisibleChange = { visible ->
                                viewModel.setAutoOffTabVisible(visible)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
}
