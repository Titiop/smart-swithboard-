package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseRepository
import com.example.data.PreferencesRepository
import com.example.data.model.AppPreferences
import com.example.data.model.BoardStatus
import com.example.data.model.FirebaseProjectConfig
import com.example.data.model.SceneItem
import com.example.data.model.SwitchRelay
import com.example.ui.theme.ThemeMode
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiSnackbarEvent {
    data class ShowMessage(val message: String) : UiSnackbarEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val prefsRepo = PreferencesRepository(application)
    val firebaseRepo = FirebaseRepository()

    // Preferences
    val appPreferences: StateFlow<AppPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())

    val switchNames: StateFlow<Map<Int, String>> = prefsRepo.switchNamesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val switchIcons: StateFlow<Map<Int, String>> = prefsRepo.switchIconsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val scenes: StateFlow<List<SceneItem>> = prefsRepo.scenesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val firebaseConfig: StateFlow<FirebaseProjectConfig> = prefsRepo.firebaseConfigFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, FirebaseProjectConfig())

    // Auth & Demo Mode
    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseRepo.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = combine(_currentUser, _isDemoMode) { user, demo ->
        user != null || demo
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val displayUserEmail: StateFlow<String?> = combine(_currentUser, _isDemoMode) { user, demo ->
        when {
            demo -> "demo@smartswitchboard.local (Demo Mode)"
            user != null -> user.email
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Connectivity & Board Status
    val isConnected: StateFlow<Boolean> = combine(firebaseRepo.isConnected, _isDemoMode) { connected, demo ->
        if (demo) true else connected
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val boardStatus: StateFlow<BoardStatus> = combine(firebaseRepo.boardStatus, _isDemoMode) { status, demo ->
        if (demo) BoardStatus(hasLastSeen = true, isBoardOnline = true, statusText = "Demo Board Online")
        else status
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BoardStatus(false, false, ""))

    // Demo relays state
    private val _demoRelayStates = MutableStateFlow<Map<String, Boolean>>(
        mapOf("relay1" to true, "relay2" to false, "relay3" to true, "relay4" to false)
    )

    // Optimistic pending updates (set of relayKeys currently awaiting server acknowledgment)
    private val _pendingRelays = MutableStateFlow<Set<String>>(emptySet())
    val pendingRelays: StateFlow<Set<String>> = _pendingRelays.asStateFlow()

    // Transient optimistic overrides in case network is in-flight
    private val _optimisticOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    // Relay states accounting for demo mode
    private val effectiveRelayStates: Flow<Map<String, Boolean>> = combine(
        firebaseRepo.relayStates,
        _demoRelayStates,
        _isDemoMode
    ) { serverStates, demoStates, isDemo ->
        if (isDemo) demoStates else serverStates
    }

    // Combined switches state
    val switches: StateFlow<List<SwitchRelay>> = combine(
        effectiveRelayStates,
        _optimisticOverrides,
        _pendingRelays,
        switchNames,
        switchIcons
    ) { activeStates, overrides, pending, names, icons ->
        (1..4).map { id ->
            val key = "relay$id"
            val effectiveState = overrides[key] ?: activeStates[key] ?: false
            val name = names[id] ?: "Switch $id"
            val icon = icons[id] ?: when (id) {
                1 -> "bulb"
                2 -> "fan"
                3 -> "plug"
                else -> "tv"
            }
            SwitchRelay(
                id = id,
                relayKey = key,
                name = name,
                iconKey = icon,
                isOn = effectiveState,
                isPending = pending.contains(key)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Snackbar events
    private val _snackbarEvents = MutableSharedFlow<UiSnackbarEvent>()
    val snackbarEvents: SharedFlow<UiSnackbarEvent> = _snackbarEvents.asSharedFlow()

    // Biometric lock management
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L
    private var lastToggleTimeMap = mutableMapOf<String, Long>()

    // Active listening job
    private var isVisible = false

    init {
        // Collect auth state changes
        viewModelScope.launch {
            firebaseRepo.authStateFlow.collect { user ->
                if (!_isDemoMode.value) {
                    _currentUser.value = user
                    if (user != null && isVisible) {
                        val boardId = appPreferences.value.boardId
                        firebaseRepo.startListening(boardId)
                    } else if (user == null) {
                        firebaseRepo.stopListening()
                    }
                }
            }
        }

        // React to board ID preference change
        viewModelScope.launch {
            appPreferences.collect { prefs ->
                if (isVisible && _currentUser.value != null && !_isDemoMode.value) {
                    firebaseRepo.startListening(prefs.boardId)
                }
                if (prefs.biometricLockEnabled && backgroundTimestamp == 0L) {
                    _isAppLocked.value = true
                }
            }
        }
    }

    fun onAppStart() {
        if (appPreferences.value.biometricLockEnabled) {
            _isAppLocked.value = true
        }
    }

    fun onResume() {
        isVisible = true
        val now = System.currentTimeMillis()
        if (backgroundTimestamp > 0L && appPreferences.value.biometricLockEnabled) {
            if (now - backgroundTimestamp >= 30_000L) {
                _isAppLocked.value = true
            }
        }
        backgroundTimestamp = 0L

        if (_currentUser.value != null && !_isDemoMode.value) {
            firebaseRepo.startListening(appPreferences.value.boardId)
        }
    }

    fun onPause() {
        isVisible = false
        backgroundTimestamp = System.currentTimeMillis()
        firebaseRepo.stopListening()
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun enterDemoMode() {
        _isDemoMode.value = true
        _currentUser.value = null
        _loginError.value = null
        _isLoggingIn.value = false
        viewModelScope.launch {
            _snackbarEvents.emit(UiSnackbarEvent.ShowMessage("Entered Demo Mode. All switches and scenes are active!"))
        }
    }

    fun login(email: String, pass: String) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            _loginError.value = "Please enter both email and password"
            return
        }

        val config = firebaseConfig.value
        if (!config.isCustomConfigured) {
            // Instant feedback if user hasn't configured real Firebase credentials yet
            _loginError.value = "Firebase API Key is not configured yet. Tap 'Configure Firebase' to enter your Web API Key, or tap 'Demo Mode' below to test immediately!"
            return
        }

        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginError.value = null

            val result = firebaseRepo.login(cleanEmail, cleanPass)
            _isLoggingIn.value = false

            result.onSuccess { user ->
                _currentUser.value = user
                _isDemoMode.value = false
                _loginError.value = null
                if (isVisible) {
                    firebaseRepo.startListening(appPreferences.value.boardId)
                }
            }.onFailure { error ->
                val msg = error.message ?: ""
                _loginError.value = when {
                    error is FirebaseAuthInvalidUserException -> "Account not found for this email in Firebase"
                    error is FirebaseAuthInvalidCredentialsException -> "Incorrect password for this email"
                    error is FirebaseNetworkException -> "No internet connection. Please check your network."
                    msg.contains("API key", ignoreCase = true) || msg.contains("API_KEY", ignoreCase = true) ->
                        "Firebase API key not valid. Tap 'Configure Firebase' below to enter your Firebase Web API Key and Project ID."
                    else -> error.localizedMessage ?: "Login failed. Please verify credentials."
                }
            }
        }
    }

    fun updateFirebaseConfig(apiKey: String, projectId: String, databaseUrl: String) {
        viewModelScope.launch {
            prefsRepo.saveFirebaseConfig(apiKey, projectId, databaseUrl)
            com.example.SwitchboardApp.configureFirebase(getApplication(), apiKey, projectId, databaseUrl)
            _loginError.value = null
            _snackbarEvents.emit(UiSnackbarEvent.ShowMessage("Firebase credentials saved! You can now log in."))
        }
    }

    fun logout() {
        firebaseRepo.logout()
        _currentUser.value = null
        _isDemoMode.value = false
        _optimisticOverrides.value = emptyMap()
        _pendingRelays.value = emptySet()
    }

    fun toggleSwitch(relay: SwitchRelay) {
        val now = System.currentTimeMillis()
        val lastTime = lastToggleTimeMap[relay.relayKey] ?: 0L
        if (now - lastTime < 300L) return
        lastToggleTimeMap[relay.relayKey] = now

        val targetState = !relay.isOn
        val key = relay.relayKey

        if (_isDemoMode.value) {
            _demoRelayStates.value = _demoRelayStates.value + (key to targetState)
            com.example.widget.SwitchboardWidgetProvider.updateAllWidgets(getApplication())
            return
        }

        if (!isConnected.value) {
            viewModelScope.launch {
                _snackbarEvents.emit(UiSnackbarEvent.ShowMessage("Cannot toggle: No internet connection"))
            }
            return
        }

        val boardId = appPreferences.value.boardId

        _optimisticOverrides.value = _optimisticOverrides.value + (key to targetState)
        _pendingRelays.value = _pendingRelays.value + key

        viewModelScope.launch {
            val result = firebaseRepo.setRelayValue(boardId, key, targetState)
            _pendingRelays.value = _pendingRelays.value - key

            result.onFailure { error ->
                _optimisticOverrides.value = _optimisticOverrides.value - key
                _snackbarEvents.emit(
                    UiSnackbarEvent.ShowMessage("Failed to update ${relay.name}: ${error.localizedMessage ?: "Unknown error"}")
                )
            }.onSuccess {
                _optimisticOverrides.value = _optimisticOverrides.value - key
                com.example.widget.SwitchboardWidgetProvider.updateAllWidgets(getApplication())
            }
        }
    }

    fun setAllSwitches(turnOn: Boolean) {
        if (_isDemoMode.value) {
            _demoRelayStates.value = mapOf(
                "relay1" to turnOn,
                "relay2" to turnOn,
                "relay3" to turnOn,
                "relay4" to turnOn
            )
            com.example.widget.SwitchboardWidgetProvider.updateAllWidgets(getApplication())
            return
        }

        if (!isConnected.value) {
            viewModelScope.launch {
                _snackbarEvents.emit(UiSnackbarEvent.ShowMessage("Cannot change switches: No internet connection"))
            }
            return
        }

        val boardId = appPreferences.value.boardId
        val updates = mapOf(
            "relay1" to turnOn,
            "relay2" to turnOn,
            "relay3" to turnOn,
            "relay4" to turnOn
        )

        _optimisticOverrides.value = _optimisticOverrides.value + updates
        _pendingRelays.value = _pendingRelays.value + updates.keys

        viewModelScope.launch {
            val result = firebaseRepo.updateRelaysAtomic(boardId, updates)
            _pendingRelays.value = _pendingRelays.value - updates.keys

            result.onFailure { error ->
                _optimisticOverrides.value = _optimisticOverrides.value - updates.keys
                _snackbarEvents.emit(UiSnackbarEvent.ShowMessage("Action failed: ${error.localizedMessage}"))
            }.onSuccess {
                _optimisticOverrides.value = _optimisticOverrides.value - updates.keys
                com.example.widget.SwitchboardWidgetProvider.updateAllWidgets(getApplication())
            }
        }
    }

    fun applyScene(scene: SceneItem) {
        val updates = mutableMapOf<String, Boolean>()
        scene.relay1?.let { updates["relay1"] = it }
        scene.relay2?.let { updates["relay2"] = it }
        scene.relay3?.let { updates["relay3"] = it }
        scene.relay4?.let { updates["relay4"] = it }

        if (updates.isEmpty()) return

        if (_isDemoMode.value) {
            _demoRelayStates.value = _demoRelayStates.value + updates
            com.example.widget.SwitchboardWidgetProvider.updateAllWidgets(getApplication())
            return
        }

        if (!isConnected.value) {
            viewModelScope.launch {
                _snackbarEvents.emit(UiSnackbarEvent.ShowMessage("Cannot apply scene: No internet connection"))
            }
            return
        }

        val boardId = appPreferences.value.boardId

        _optimisticOverrides.value = _optimisticOverrides.value + updates
        _pendingRelays.value = _pendingRelays.value + updates.keys

        viewModelScope.launch {
            val result = firebaseRepo.updateRelaysAtomic(boardId, updates)
            _pendingRelays.value = _pendingRelays.value - updates.keys

            result.onFailure { error ->
                _optimisticOverrides.value = _optimisticOverrides.value - updates.keys
                _snackbarEvents.emit(UiSnackbarEvent.ShowMessage("Scene '${scene.name}' failed: ${error.localizedMessage}"))
            }.onSuccess {
                _optimisticOverrides.value = _optimisticOverrides.value - updates.keys
                com.example.widget.SwitchboardWidgetProvider.updateAllWidgets(getApplication())
            }
        }
    }

    fun updateSwitchMeta(id: Int, name: String, iconKey: String) {
        viewModelScope.launch {
            prefsRepo.updateSwitchMeta(id, name, iconKey)
        }
    }

    fun saveScene(scene: SceneItem) {
        viewModelScope.launch {
            val currentList = scenes.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == scene.id }
            if (index >= 0) {
                currentList[index] = scene
            } else {
                currentList.add(scene)
            }
            prefsRepo.saveScenes(currentList)
        }
    }

    fun deleteScene(sceneId: String) {
        viewModelScope.launch {
            val currentList = scenes.value.filter { it.id != sceneId }
            prefsRepo.saveScenes(currentList)
        }
    }

    fun moveScene(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val currentList = scenes.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            viewModelScope.launch {
                prefsRepo.saveScenes(currentList)
            }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.updateThemeMode(mode) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateDynamicColor(enabled) }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateHapticFeedback(enabled) }
    }

    fun updateBiometricLock(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateBiometricLock(enabled) }
    }

    fun updateBoardId(newId: String) {
        viewModelScope.launch {
            prefsRepo.updateBoardId(newId)
            if (isVisible && _currentUser.value != null && !_isDemoMode.value) {
                firebaseRepo.startListening(newId.trim().ifEmpty { "switchboard1" })
            }
        }
    }
}
