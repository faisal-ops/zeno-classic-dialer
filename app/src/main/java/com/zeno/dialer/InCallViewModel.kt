package com.zeno.dialer

import android.telecom.Call
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InCallViewModel : ViewModel() {

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var lastObservedState = -1

    /** Call whenever the call state changes to drive the timer. */
    fun onStateChanged(state: Int) {
        if (state == lastObservedState) return
        lastObservedState = state

        when (state) {
            Call.STATE_ACTIVE -> startTimer()
            Call.STATE_DISCONNECTED,
            Call.STATE_DISCONNECTING -> stopTimer()
        }
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        _elapsedSeconds.value = 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _elapsedSeconds.update { it + 1 }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

fun Int.toCallDuration(): String {
    val m = this / 60
    val s = this % 60
    return "%02d:%02d".format(m, s)
}
