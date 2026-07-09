package com.finix.paxdevicereaderapplication.domain

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A simple, thread-safe, timestamped log buffer surfaced to the UI as a [StateFlow] of text.
 *
 * The timestamp source is injectable, so the formatting can be unit-tested deterministically.
 */
class TransactionLogger(
    private val now: () -> Date = ::Date,
) {
    private val formatter = ThreadLocal.withInitial { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text

    /** Appends [message] as a new timestamped line. */
    fun log(message: String) {
        val timestamp = formatter.get().format(now())
        _text.update { current -> "$current[$timestamp] $message\n" }
        Log.d("TransactionLogger", message)
    }

    fun clear() {
        _text.value = ""
    }
}
