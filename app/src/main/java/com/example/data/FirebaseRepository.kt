package com.example.data

import com.example.data.model.BoardStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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

    private var connectedRef: DatabaseReference? = null
    private var connectedListener: ValueEventListener? = null

    private var offsetRef: DatabaseReference? = null
    private var offsetListener: ValueEventListener? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _serverTimeOffsetMs = MutableStateFlow(0L)

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
        } catch (_: Exception) {}
    }

    /**
     * Remove listeners when app goes to background to keep battery near zero.
     */
    fun stopListening() {
        stopBoardListening()
        stopConnectionListening()
    }

    private fun stopBoardListening() {
        activeBoardListener?.let { listener ->
            activeBoardRef?.removeEventListener(listener)
        }
        activeBoardListener = null
        activeBoardRef = null
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
}
