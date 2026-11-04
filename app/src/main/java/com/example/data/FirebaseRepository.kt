package com.example.data

import com.example.data.model.BoardStatus
import com.example.data.model.RelayTimerInfo
import com.example.data.model.ScheduleItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.TimeZone
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class FirebaseRepository {

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }

    private val database: FirebaseDatabase?
        get() = try {
            FirebaseDatabase.getInstance().also { db ->
                // Disk persistence is false by default; catch if already referenced
                runCatching { db.setPersistenceEnabled(false) }
            }
        } catch (_: Exception) {
            null
        }

    private var activeBoardRef: DatabaseReference? = null
    private var activeBoardListener: ValueEventListener? = null

    private var timersRef: DatabaseReference? = null
    private var timersListener: ValueEventListener? = null

    private var schedulesRef: DatabaseReference? = null
    private var schedulesListener: ValueEventListener? = null

    private var autoOffRef: DatabaseReference? = null
    private var autoOffListener: ValueEventListener? = null

    private var connectedRef: DatabaseReference? = null
    private var connectedListener: ValueEventListener? = null

    private var offsetRef: DatabaseReference? = null
    private var offsetListener: ValueEventListener? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _serverTimeOffsetMs = MutableStateFlow(0L)
    val serverTimeOffsetMs: StateFlow<Long> = _serverTimeOffsetMs.asStateFlow()

    private val _timers = MutableStateFlow<Map<Int, RelayTimerInfo>>(emptyMap())
    val timers: StateFlow<Map<Int, RelayTimerInfo>> = _timers.asStateFlow()

    private val _schedules = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val schedules: StateFlow<List<ScheduleItem>> = _schedules.asStateFlow()

    private val _autoOffMap = MutableStateFlow<Map<Int, Int>>(
        mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0)
    )
    val autoOffMap: StateFlow<Map<Int, Int>> = _autoOffMap.asStateFlow()

    private val _relayStates = MutableStateFlow<Map<String, Boolean>>(
        mapOf("relay1" to false, "relay2" to false, "relay3" to false, "relay4" to false)
    )
    val relayStates: StateFlow<Map<String, Boolean>> = _relayStates.asStateFlow()

    private val _boardStatus = MutableStateFlow(
        BoardStatus(hasLastSeen = false, isBoardOnline = false, statusText = "")
    )
    val boardStatus: StateFlow<BoardStatus> = _boardStatus.asStateFlow()

    private var cachedLastSeen: Long? = null

    // Observe current authenticated user safely
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val currentAuth = auth
        if (currentAuth == null) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        currentAuth.addAuthStateListener(listener)
        awaitClose { currentAuth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser?
        get() = try {
            auth?.currentUser
        } catch (_: Exception) {
            null
        }

    suspend fun login(email: String, pass: String): Result<FirebaseUser> {
        val currentAuth = auth ?: return Result.failure(
            IllegalStateException("Firebase Auth not initialized. Please ensure your Firebase API key is configured.")
        )
        return try {
            kotlinx.coroutines.withTimeout(15000L) {
                val result = currentAuth.signInWithEmailAndPassword(email.trim(), pass).await()
                val user = result.user ?: throw IllegalStateException("User authentication failed")
                Result.success(user)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(IllegalStateException("Connection timed out. Please check your internet or Firebase Web API Key."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        stopListening()
        try {
            auth?.signOut()
        } catch (_: Exception) {}
    }

    /**
     * Start observing Firebase connectivity and board data while app is visible.
     */
    fun startListening(boardId: String) {
        val db = database ?: return
        startConnectionListening()

        // Remove previous listener if board changed
        stopBoardListening()

        try {
            val ref = db.getReference(boardId)
            activeBoardRef = ref

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val r1 = snapshot.child("relay1").getValue(Boolean::class.java) ?: false
                    val r2 = snapshot.child("relay2").getValue(Boolean::class.java) ?: false
                    val r3 = snapshot.child("relay3").getValue(Boolean::class.java) ?: false
                    val r4 = snapshot.child("relay4").getValue(Boolean::class.java) ?: false

                    _relayStates.value = mapOf(
                        "relay1" to r1,
                        "relay2" to r2,
                        "relay3" to r3,
                        "relay4" to r4
                    )

                    val rawLastSeen = snapshot.child("lastSeen").value
                    val lastSeen: Long? = when (rawLastSeen) {
                        is Number -> rawLastSeen.toLong()
                        is String -> rawLastSeen.toLongOrNull()
                        else -> null
                    }
                    cachedLastSeen = lastSeen
                    updateBoardStatus()
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            ref.addValueEventListener(listener)
            activeBoardListener = listener

            // Attach one listener on /{boardId}_config/timers while the main screen is visible
            val tRef = db.getReference("${boardId}_config").child("timers")
            timersRef = tRef
            val tListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = mutableMapOf<Int, RelayTimerInfo>()
                    for (id in 1..4) {
                        val node = snapshot.child("relay$id")
                        if (node.exists()) {
                            val action = node.child("action").getValue(Boolean::class.java)
                            val rawAtMs = node.child("atMs").value
                            val atMs = when (rawAtMs) {
                                is Number -> rawAtMs.toLong()
                                is String -> rawAtMs.toLongOrNull()
                                else -> null
                            }
                            if (action != null && atMs != null && atMs > 0L) {
                                map[id] = RelayTimerInfo(relayId = id, action = action, atMs = atMs)
                            }
                        }
                    }
                    _timers.value = map
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            tRef.addValueEventListener(tListener)
            timersListener = tListener
        } catch (_: Exception) {}
    }

    /**
     * Remove listeners when app goes to background to keep battery near zero.
     */
    fun stopListening() {
        stopBoardListening()
        stopSchedulesListening()
        stopAutoOffListening()
        stopConnectionListening()
    }

    private fun stopBoardListening() {
        activeBoardListener?.let { listener ->
            activeBoardRef?.removeEventListener(listener)
        }
        activeBoardListener = null
        activeBoardRef = null

        timersListener?.let { listener ->
            timersRef?.removeEventListener(listener)
        }
        timersListener = null
        timersRef = null
    }

    /**
     * Attach ONE ValueEventListener on /{boardId}_config/schedules only while the Schedules screen is open.
     */
    fun startSchedulesListening(boardId: String) {
        val db = database ?: return
        stopSchedulesListening()
        try {
            val ref = db.getReference("${boardId}_config").child("schedules")
            schedulesRef = ref
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ScheduleItem>()
                    for (child in snapshot.children) {
                        val id = child.key ?: continue
                        val relayRaw = child.child("relay").value
                        val relay = when (relayRaw) {
                            is Number -> relayRaw.toInt()
                            is String -> relayRaw.toIntOrNull()
                            else -> null
                        } ?: continue
                        if (relay !in 1..4) continue

                        val action = child.child("action").getValue(Boolean::class.java) ?: false
                        val minRaw = child.child("minute").value
                        val minute = when (minRaw) {
                            is Number -> minRaw.toInt()
                            is String -> minRaw.toIntOrNull()
                            else -> null
                        } ?: continue
                        if (minute !in 0..1439) continue

                        val daysRaw = child.child("days").value
                        val days = when (daysRaw) {
                            is Number -> daysRaw.toInt()
                            is String -> daysRaw.toIntOrNull()
                            else -> 127
                        } ?: 127

                        val enabled = child.child("enabled").getValue(Boolean::class.java) ?: true
                        val label = (child.child("label").value?.toString() ?: "").take(30)

                        list.add(
                            ScheduleItem(
                                id = id,
                                relay = relay,
                                action = action,
                                minute = minute,
                                days = days.coerceIn(1, 127),
                                enabled = enabled,
                                label = label
                            )
                        )
                    }
                    _schedules.value = list.sortedBy { it.minute }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            schedulesListener = listener
        } catch (_: Exception) {}
    }

    fun stopSchedulesListening() {
        schedulesListener?.let { listener ->
            schedulesRef?.removeEventListener(listener)
        }
        schedulesListener = null
        schedulesRef = null
    }

    /**
     * Attach ONE ValueEventListener on /{boardId}_config/autoOff only while the Auto-off tab is visible.
     */
    fun startAutoOffListening(boardId: String) {
        val db = database ?: return
        stopAutoOffListening()
        try {
            val ref = db.getReference("${boardId}_config").child("autoOff")
            autoOffRef = ref
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = mutableMapOf<Int, Int>()
                    for (id in 1..4) {
                        val raw = snapshot.child("relay$id").value
                        val mins = when (raw) {
                            is Number -> raw.toInt().coerceAtLeast(0)
                            is String -> (raw.toIntOrNull() ?: 0).coerceAtLeast(0)
                            else -> 0
                        }
                        map[id] = mins
                    }
                    _autoOffMap.value = map
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            autoOffListener = listener
        } catch (_: Exception) {}
    }

    fun stopAutoOffListening() {
        autoOffListener?.let { listener ->
            autoOffRef?.removeEventListener(listener)
        }
        autoOffListener = null
        autoOffRef = null
    }

    private fun startConnectionListening() {
        val db = database ?: return
        if (connectedListener != null) return

        try {
            connectedRef = db.getReference(".info/connected")
            connectedListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    _isConnected.value = connected
                    updateBoardStatus()
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            connectedRef?.addValueEventListener(connectedListener!!)

            offsetRef = db.getReference(".info/serverTimeOffset")
            offsetListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val offset = (snapshot.value as? Number)?.toLong() ?: 0L
                    _serverTimeOffsetMs.value = offset
                    updateBoardStatus()
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            offsetRef?.addValueEventListener(offsetListener!!)
        } catch (_: Exception) {}
    }

    private fun stopConnectionListening() {
        connectedListener?.let { listener ->
            connectedRef?.removeEventListener(listener)
        }
        connectedListener = null
        connectedRef = null

        offsetListener?.let { listener ->
            offsetRef?.removeEventListener(listener)
        }
        offsetListener = null
        offsetRef = null
    }

    fun updateBoardStatus() {
        val lastSeen = cachedLastSeen
        if (lastSeen == null || lastSeen <= 0) {
            _boardStatus.value = BoardStatus(hasLastSeen = false, isBoardOnline = false, statusText = "")
            return
        }

        val estimatedServerTime = System.currentTimeMillis() + _serverTimeOffsetMs.value
        val ageMs = estimatedServerTime - lastSeen

        val isOnline = ageMs in 0..90_000
        val statusText = if (isOnline) {
            "Board online"
        } else {
            val minutesAgo = ((ageMs / 60_000).coerceAtLeast(1)).toInt()
            if (minutesAgo <= 1) "Board offline - last seen 1 min ago"
            else "Board offline - last seen $minutesAgo min ago"
        }

        _boardStatus.value = BoardStatus(
            hasLastSeen = true,
            isBoardOnline = isOnline,
            statusText = statusText
        )
    }

    /**
     * Set a single relay value.
     */
    suspend fun setRelayValue(boardId: String, relayKey: String, value: Boolean): Result<Unit> {
        val db = database ?: return Result.failure(IllegalStateException("Database not connected"))
        if (!_isConnected.value) {
            return Result.failure(IllegalStateException("No internet connection - cannot send command"))
        }
        return try {
            db.getReference(boardId).child(relayKey).setValue(value).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomically update multiple relays at once using updateChildren.
     */
    suspend fun updateRelaysAtomic(boardId: String, updates: Map<String, Boolean>): Result<Unit> {
        val db = database ?: return Result.failure(IllegalStateException("Database not connected"))
        if (!_isConnected.value) {
            return Result.failure(IllegalStateException("No internet connection - cannot send commands"))
        }
        return try {
            val childUpdates = HashMap<String, Any>()
            for ((key, value) in updates) {
                childUpdates[key] = value
            }
            db.getReference(boardId).updateChildren(childUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * One-shot fetch for Widget and Quick Settings tiles.
     */
    suspend fun fetchSingleRelay(boardId: String, relayKey: String): Boolean? {
        val db = database ?: return null
        return try {
            val snapshot = db.getReference(boardId).child(relayKey).get().await()
            snapshot.getValue(Boolean::class.java)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchAllRelays(boardId: String): Map<String, Boolean>? {
        val db = database ?: return null
        return try {
            val snapshot = db.getReference(boardId).get().await()
            mapOf(
                "relay1" to (snapshot.child("relay1").getValue(Boolean::class.java) ?: false),
                "relay2" to (snapshot.child("relay2").getValue(Boolean::class.java) ?: false),
                "relay3" to (snapshot.child("relay3").getValue(Boolean::class.java) ?: false),
                "relay4" to (snapshot.child("relay4").getValue(Boolean::class.java) ?: false)
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * One-shot read of /{boardId}_config/device for Board Info dialog.
     */
    suspend fun fetchBoardDeviceInfo(boardId: String): com.example.ui.BoardDeviceInfo? {
        val db = database ?: return null
        return try {
            withTimeout(8000L) {
                val snapshot = db.getReference("${boardId}_config").child("device").get().await()
                if (!snapshot.exists()) return@withTimeout null

                val fw = snapshot.child("fw").value?.toString()
                val rssiRaw = snapshot.child("rssi").value
                val rssi = when (rssiRaw) {
                    is Number -> rssiRaw.toInt()
                    is String -> rssiRaw.toIntOrNull()
                    else -> null
                }
                val ip = snapshot.child("ip").value?.toString()
                val uptimeRaw = snapshot.child("uptimeSec").value
                val uptimeSec = when (uptimeRaw) {
                    is Number -> uptimeRaw.toLong()
                    is String -> uptimeRaw.toLongOrNull()
                    else -> null
                }

                if (fw == null && rssi == null && ip == null && uptimeSec == null) {
                    null
                } else {
                    com.example.ui.BoardDeviceInfo(
                        fw = fw,
                        rssi = rssi,
                        ip = ip,
                        uptimeSec = uptimeSec
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Save or update a schedule under /{boardId}_config/schedules/{id} and write tzOffsetMin.
     */
    suspend fun saveSchedule(boardId: String, item: ScheduleItem, isNew: Boolean): Result<Unit> {
        val db = database ?: return Result.failure(IllegalStateException("Database not connected"))
        if (!_isConnected.value) {
            return Result.failure(IllegalStateException("No internet connection - cannot save schedule"))
        }
        return try {
            withTimeout(8000L) {
                val configRef = db.getReference("${boardId}_config")
                val schedulesNode = configRef.child("schedules")
                val targetRef = if (isNew || item.id.isBlank()) {
                    schedulesNode.push()
                } else {
                    schedulesNode.child(item.id)
                }
                val tzOffsetMin = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
                val schedulePayload = mapOf(
                    "relay" to item.relay.coerceIn(1, 4),
                    "action" to item.action,
                    "minute" to item.minute.coerceIn(0, 1439),
                    "days" to item.days.coerceIn(1, 127),
                    "enabled" to item.enabled,
                    "label" to item.label.trim().take(30)
                )
                configRef.child("tzOffsetMin").setValue(tzOffsetMin).await()
                targetRef.setValue(schedulePayload).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a schedule under /{boardId}_config/schedules/{scheduleId}.
     */
    suspend fun deleteSchedule(boardId: String, scheduleId: String): Result<Unit> {
        val db = database ?: return Result.failure(IllegalStateException("Database not connected"))
        if (!_isConnected.value) {
            return Result.failure(IllegalStateException("No internet connection - cannot delete schedule"))
        }
        return try {
            withTimeout(8000L) {
                val configRef = db.getReference("${boardId}_config")
                val tzOffsetMin = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
                configRef.child("tzOffsetMin").setValue(tzOffsetMin).await()
                configRef.child("schedules").child(scheduleId).removeValue().await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bulk enable/disable schedules under /{boardId}_config/schedules as ONE atomic updateChildren call.
     */
    suspend fun setSchedulesEnabled(boardId: String, ids: List<String>, enabled: Boolean): Result<Unit> {
        val cleanIds = ids.filter { it.isNotBlank() }
        if (cleanIds.isEmpty()) return Result.success(Unit)
        val db = database ?: return Result.failure(IllegalStateException("Database not connected"))
        if (!_isConnected.value) {
            return Result.failure(IllegalStateException("No internet connection - cannot update schedules"))
        }
        return try {
            withTimeout(8000L) {
                val configRef = db.getReference("${boardId}_config")
                val tzOffsetMin = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
                val childUpdates = HashMap<String, Any?>()
                childUpdates["tzOffsetMin"] = tzOffsetMin
                for (id in cleanIds) {
                    childUpdates["schedules/$id/enabled"] = enabled
                }
                configRef.updateChildren(childUpdates).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bulk delete schedules under /{boardId}_config/schedules as ONE atomic updateChildren call.
     */
    suspend fun deleteSchedules(boardId: String, ids: List<String>): Result<Unit> {
        val cleanIds = ids.filter { it.isNotBlank() }
        if (cleanIds.isEmpty()) return Result.success(Unit)
        val db = database ?: return Result.failure(IllegalStateException("Database not connected"))
        if (!_isConnected.value) {
            return Result.failure(IllegalStateException("No internet connection - cannot delete schedules"))
        }
        return try {
            withTimeout(8000L) {
                val configRef = db.getReference("${boardId}_config")
                val tzOffsetMin = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
                val childUpdates = HashMap<String, Any?>()
                childUpdates["tzOffsetMin"] = tzOffsetMin
                for (id in cleanIds) {
                    childUpdates["schedules/$id"] = null
                }
                configRef.updateChildren(childUpdates).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start a countdown timer for relay{N} under /{boardId}_config/timers/relay{N}.
     * Uses serverTimeOffsetMs so atMs is in SERVER epoch milliseconds.
     */
    suspend fun setRelayTimer(boardId: String, relayId: Int, action: Boolean, minutes: Int): Result<Unit> {
        val db = database ?: return Result.failure(IllegalStateException("Database not connected"))
        if (!_isConnected.value) {
            return Result.failure(IllegalStateException("No internet connection - cannot start timer"))
        }
        val validMins = minutes.coerceIn(1, 1440)
        val serverNow = System.currentTimeMillis() + _serverTimeOffsetMs.value
        val atMs = serverNow + validMins * 60_000L
        val previousTimers = _timers.value
        _timers.value = previousTimers + (relayId to RelayTimerInfo(relayId = relayId, action = action, atMs = atMs))
        return try {
            withTimeout(8000L) {
                val payload = mapOf(
                    "action" to action,
                    "atMs" to atMs
                )
                db.getReference("${boardId}_config")
                    .child("timers")
                    .child("relay$relayId")
                    .setValue(payload)
                    .await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            _timers.value = previousTimers
            Result.failure(e)
        }
    }

    /**
     * Cancel an active countdown timer by removing /{boardId}_config/timers/relay{N}.
     */
    suspend fun cancelRelayTimer(boardId: String, relayId: Int): Result<Unit> {
        val db = database ?: return Result.failure(IllegalStateException("Database not connected"))
        if (!_isConnected.value) {
            return Result.failure(IllegalStateException("No internet connection - cannot cancel timer"))
        }
        val previousTimers = _timers.value
        _timers.value = previousTimers - relayId
        return try {
            withTimeout(8000L) {
                db.getReference("${boardId}_config")
                    .child("timers")
                    .child("relay$relayId")
                    .removeValue()
                    .await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            _timers.value = previousTimers
            Result.failure(e)
        }
    }

    /**
     * Read /{boardId}_config/autoOff/relay{N} with a single get() when EditSwitchSheet opens.
     */
    suspend fun fetchAutoOffMinutes(boardId: String, relayId: Int): Int {
        val db = database ?: return 0
        return try {
            withTimeout(8000L) {
                val snapshot = db.getReference("${boardId}_config")
                    .child("autoOff")
                    .child("relay$relayId")
                    .get()
                    .await()
                val raw = snapshot.value
                val mins = when (raw) {
                    is Number -> raw.toInt().coerceAtLeast(0)
                    is String -> (raw.toIntOrNull() ?: 0).coerceAtLeast(0)
                    else -> 0
                }
                _autoOffMap.value = _autoOffMap.value + (relayId to mins)
                mins
            }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Write /{boardId}_config/autoOff/relay{N} (Int minutes, 0 for Never).
     */
    suspend fun setAutoOffMinutes(boardId: String, relayId: Int, minutes: Int): Result<Unit> {
        val db = database ?: return Result.failure(IllegalStateException("Database not connected"))
        if (!_isConnected.value) {
            return Result.failure(IllegalStateException("No internet connection - cannot update auto-off"))
        }
        val validMins = minutes.coerceAtLeast(0)
        val previousMap = _autoOffMap.value
        _autoOffMap.value = previousMap + (relayId to validMins)
        return try {
            withTimeout(8000L) {
                db.getReference("${boardId}_config")
                    .child("autoOff")
                    .child("relay$relayId")
                    .setValue(validMins)
                    .await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            _autoOffMap.value = previousMap
            Result.failure(e)
        }
    }
}
