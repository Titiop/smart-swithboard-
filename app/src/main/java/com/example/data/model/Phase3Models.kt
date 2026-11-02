package com.example.data.model

data class ScheduleItem(
    val id: String,
    val relay: Int,       // 1..4
    val action: Boolean,  // true = Turn ON, false = Turn OFF
    val minute: Int,      // minutes since local midnight, 0..1439
    val days: Int,        // bitmask: bit0=Mon, bit1=Tue, ..., bit6=Sun (127 = every day, 31 = weekdays)
    val enabled: Boolean,
    val label: String = ""
)

data class RelayTimerInfo(
    val relayId: Int,     // 1..4
    val action: Boolean,  // state to apply when time arrives
    val atMs: Long        // SERVER epoch milliseconds when it must run
)
