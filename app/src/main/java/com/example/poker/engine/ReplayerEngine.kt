package com.example.poker.engine

import com.example.poker.model.HandStep
import com.example.poker.model.PokerHand
import com.example.poker.model.Street
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReplayerEngine(
    private val scope: CoroutineScope
) {
    private val _currentHand = MutableStateFlow<PokerHand?>(null)
    val currentHand: StateFlow<PokerHand?> = _currentHand.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeedMs = MutableStateFlow(1200L)
    val playbackSpeedMs: StateFlow<Long> = _playbackSpeedMs.asStateFlow()

    private var playbackJob: Job? = null

    fun loadHand(hand: PokerHand) {
        pause()
        _currentHand.value = hand
        _currentStepIndex.value = 0
    }

    val currentStep: HandStep?
        get() {
            val hand = _currentHand.value ?: return null
            val idx = _currentStepIndex.value
            return if (idx in hand.steps.indices) hand.steps[idx] else hand.steps.lastOrNull()
        }

    fun stepForward(): Boolean {
        val hand = _currentHand.value ?: return false
        val nextIdx = _currentStepIndex.value + 1
        if (nextIdx < hand.steps.size) {
            _currentStepIndex.value = nextIdx
            return true
        }
        pause()
        return false
    }

    fun stepBackward(): Boolean {
        val prevIdx = _currentStepIndex.value - 1
        if (prevIdx >= 0) {
            _currentStepIndex.value = prevIdx
            return true
        }
        return false
    }

    fun jumpToStart() {
        pause()
        _currentStepIndex.value = 0
    }

    fun jumpToEnd() {
        pause()
        val hand = _currentHand.value ?: return
        _currentStepIndex.value = maxOf(0, hand.steps.size - 1)
    }

    fun jumpToStreet(targetStreet: Street) {
        pause()
        val hand = _currentHand.value ?: return
        val targetIdx = hand.steps.indexOfFirst { it.street == targetStreet }
        if (targetIdx >= 0) {
            _currentStepIndex.value = targetIdx
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        val hand = _currentHand.value ?: return
        if (_currentStepIndex.value >= hand.steps.size - 1) {
            _currentStepIndex.value = 0
        }
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (_isPlaying.value) {
                delay(_playbackSpeedMs.value)
                val moved = stepForward()
                if (!moved) break
            }
            _isPlaying.value = false
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun setPlaybackSpeed(speedMs: Long) {
        _playbackSpeedMs.value = speedMs.coerceIn(300L, 3000L)
    }
}
