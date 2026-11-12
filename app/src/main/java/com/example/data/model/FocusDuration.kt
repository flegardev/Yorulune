package com.example.data.model

/** History stores whole minutes; an explicitly logged short session is at least one. */
fun focusMinutesToRecord(mode: String, totalSeconds: Int, seconds: Int): Int {
    val elapsed = if (mode == "stopwatch") seconds.toLong() else totalSeconds.toLong() - seconds
    return (elapsed.coerceAtLeast(60L) / 60L).coerceIn(1L, 600L).toInt()
}
