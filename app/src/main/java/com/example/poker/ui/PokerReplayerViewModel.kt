package com.example.poker.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poker.billing.RuStoreBillingManager
import com.example.poker.data.ReplayerPreferences
import com.example.poker.model.HandFilter
import com.example.poker.model.HandStepState
import com.example.poker.model.PokerHand
import com.example.poker.model.PositionRole
import com.example.poker.parser.GGPokerParser
import com.example.poker.parser.RedStarParser
import com.example.poker.state.HandReplayerEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class ReplayerUiState(
    val isLoading: Boolean = false,
    val fileName: String? = null,
    val hands: List<PokerHand> = emptyList(),
    val currentHandIndex: Int = 0,
    val currentStepIndex: Int = 0,
    val currentStepState: HandStepState? = null,
    val isPlaying: Boolean = false,
    val useBbUnits: Boolean = true,
    val errorMessage: String? = null,
    val warnings: List<String> = emptyList(),
    val isHandListVisible: Boolean = false,
    val activeFilter: HandFilter = HandFilter.ALL,
    val selectedPositions: Set<PositionRole> = emptySet(),
    val isFullAccessPurchased: Boolean = false,
    val isRuStoreInstalled: Boolean = false,
    val isPaywallVisible: Boolean = false,
    val isPrivacyPolicyVisible: Boolean = false,
    val isPurchasing: Boolean = false,
    val purchaseStatusMessage: String? = null
) {
    val currentHand: PokerHand?
        get() = hands.getOrNull(currentHandIndex)

    val isFilterActive: Boolean
        get() = activeFilter != HandFilter.ALL || selectedPositions.isNotEmpty()

    val filteredHandIndices: List<Int>
        get() = hands.indices.filter { idx ->
            val hand = hands[idx]
            val matchesType = activeFilter.matches(hand)
            val matchesPos = if (selectedPositions.isEmpty()) {
                true
            } else {
                val heroPos = hand.players.firstOrNull { it.isHero }?.position
                heroPos != null && heroPos in selectedPositions
            }
            matchesType && matchesPos
        }

    val filteredCurrentIndex: Int
        get() = filteredHandIndices.indexOf(currentHandIndex)

    val totalFilteredHands: Int
        get() = filteredHandIndices.size
}

class PokerReplayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ReplayerPreferences(application)
    private val isRuStoreInstalled = RuStoreBillingManager.isRuStoreInstalled(application)
    private val _uiState = MutableStateFlow(
        ReplayerUiState(
            useBbUnits = prefs.useBbUnits,
            isRuStoreInstalled = isRuStoreInstalled
        )
    )
    val uiState: StateFlow<ReplayerUiState> = _uiState.asStateFlow()

    private var autoPlayJob: Job? = null
    // Cache computed step states per hand to make scrubbing instantaneous
    private val computedStepsCache = mutableMapOf<Int, List<HandStepState>>()

    init {
        RuStoreBillingManager.init(application)
        val cachedPurchased = prefs.isFullAccessPurchased
        _uiState.update { 
            it.copy(
                isFullAccessPurchased = cachedPurchased,
                isRuStoreInstalled = isRuStoreInstalled
            ) 
        }
        RuStoreBillingManager.checkPurchases(application) { isPurchased ->
            _uiState.update { it.copy(isFullAccessPurchased = isPurchased) }
        }
        // Try restoring last session or load demo
        restoreOrLoadInitial()
    }

    private val internalImportedFile: File
        get() = File(getApplication<Application>().filesDir, "active_tournament.txt")

    private fun restoreOrLoadInitial() {
        viewModelScope.launch {
            val lastSource = prefs.lastSourceUri
            val localFile = internalImportedFile

            // 1. If user previously had an imported tournament and the local internal copy exists
            if (localFile.exists() && localFile.length() > 0 && lastSource != "sample_tournament") {
                try {
                    val content = localFile.readText()
                    processParsedText(
                        text = content,
                        fileName = prefs.lastFileName ?: "Загруженный турнир",
                        sourceUri = "internal_cache",
                        restoreState = true
                    )
                    return@launch
                } catch (_: Exception) {
                    // Ignore and fallback
                }
            }

            // 2. Backward compatibility: if an older version stored a raw content:// URI
            if (!lastSource.isNullOrBlank() && lastSource != "sample_tournament" && lastSource != "internal_cache") {
                try {
                    val uri = Uri.parse(lastSource)
                    val contentResolver = getApplication<Application>().contentResolver
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        localFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (localFile.exists() && localFile.length() > 0) {
                        val content = localFile.readText()
                        processParsedText(
                            text = content,
                            fileName = prefs.lastFileName ?: "История турнира",
                            sourceUri = "internal_cache",
                            restoreState = true
                        )
                        return@launch
                    }
                } catch (_: Exception) {
                    // Catch Permission Denial or any security exception safely, preventing startup crash/error
                }
            }

            // 3. Bundled demo tournament
            loadSampleTournament(restoreState = true)
        }
    }

    fun loadSampleTournament(restoreState: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val assetManager = getApplication<Application>().assets
                val inputStream = assetManager.open("sample_tournament.txt")
                val content = inputStream.bufferedReader().use { it.readText() }
                processParsedText(
                    text = content,
                    fileName = "Демо-турнир (Bounty Hunters)",
                    sourceUri = "sample_tournament",
                    restoreState = restoreState
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Не удалось загрузить демо-турнир: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadFromUri(uri: Uri, displayName: String, restoreState: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val localFile = internalImportedFile

                // Copy input stream directly into internal private app storage
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    localFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IllegalArgumentException("Не удалось открыть выбранный файл")

                val content = localFile.readText()

                processParsedText(
                    text = content,
                    fileName = displayName,
                    sourceUri = "internal_cache",
                    restoreState = restoreState
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка при чтении файла: ${e.message ?: "Файл не найден или недоступен"}"
                    )
                }
            }
        }
    }

    private fun processParsedText(
        text: String,
        fileName: String,
        sourceUri: String,
        restoreState: Boolean
    ) {
        val parseResult = if (RedStarParser.isRedStarFormat(text)) {
            RedStarParser.parseHandHistory(text)
        } else {
            val ggResult = GGPokerParser.parseHandHistory(text)
            if (ggResult.hands.isEmpty() && text.contains("<game")) {
                RedStarParser.parseHandHistory(text)
            } else {
                ggResult
            }
        }

        if (parseResult.hands.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "В файле не найдено корректных раздач (поддерживаются GGPoker и RedStar / iPoker)."
                )
            }
            return
        }

        computedStepsCache.clear()

        val targetHandIndex = if (restoreState) {
            prefs.lastHandIndex.coerceIn(0, parseResult.hands.lastIndex)
        } else {
            0
        }

        val hand = parseResult.hands[targetHandIndex]
        val steps = getOrComputeSteps(targetHandIndex, hand)

        val targetStepIndex = if (restoreState) {
            prefs.lastStepIndex.coerceIn(0, steps.lastIndex)
        } else {
            0
        }

        prefs.lastSourceUri = sourceUri
        prefs.lastFileName = fileName
        prefs.saveReplayerState(targetHandIndex, targetStepIndex, hand.handId)

        _uiState.update {
            it.copy(
                isLoading = false,
                fileName = fileName,
                hands = parseResult.hands,
                currentHandIndex = targetHandIndex,
                currentStepIndex = targetStepIndex,
                currentStepState = steps.getOrNull(targetStepIndex) ?: steps.firstOrNull(),
                warnings = parseResult.warnings,
                errorMessage = null,
                activeFilter = HandFilter.ALL,
                selectedPositions = emptySet()
            )
        }
    }

    private fun getOrComputeSteps(handIndex: Int, hand: PokerHand): List<HandStepState> {
        return computedStepsCache.getOrPut(handIndex) {
            HandReplayerEngine.computeSteps(hand)
        }
    }

    fun stepForward() {
        val currentHand = _uiState.value.currentHand ?: return
        val steps = getOrComputeSteps(_uiState.value.currentHandIndex, currentHand)
        val nextStep = (_uiState.value.currentStepIndex + 1).coerceAtMost(steps.lastIndex)
        if (nextStep != _uiState.value.currentStepIndex) {
            applyStep(nextStep, steps)
        } else if (_uiState.value.isPlaying) {
            // Reached end of hand in autoplay
            pauseAutoPlay()
        }
    }

    fun stepBackward() {
        val currentHand = _uiState.value.currentHand ?: return
        val steps = getOrComputeSteps(_uiState.value.currentHandIndex, currentHand)
        val prevStep = (_uiState.value.currentStepIndex - 1).coerceAtLeast(0)
        if (prevStep != _uiState.value.currentStepIndex) {
            applyStep(prevStep, steps)
        }
    }

    fun jumpToStep(step: Int) {
        val currentHand = _uiState.value.currentHand ?: return
        val steps = getOrComputeSteps(_uiState.value.currentHandIndex, currentHand)
        val target = step.coerceIn(0, steps.lastIndex)
        applyStep(target, steps)
    }

    private fun applyStep(stepIndex: Int, steps: List<HandStepState>) {
        val hand = _uiState.value.currentHand
        prefs.saveReplayerState(_uiState.value.currentHandIndex, stepIndex, hand?.handId)
        _uiState.update {
            it.copy(
                currentStepIndex = stepIndex,
                currentStepState = steps.getOrNull(stepIndex)
            )
        }
    }

    fun setFilter(filter: HandFilter) {
        if (_uiState.value.activeFilter == filter) return
        _uiState.update { it.copy(activeFilter = filter) }

        // If the current hand doesn't match the new filter, switch to the first matching hand
        val filtered = _uiState.value.filteredHandIndices
        if (filtered.isNotEmpty() && _uiState.value.currentHandIndex !in filtered) {
            selectHand(filtered.first())
        }
    }

    fun togglePositionFilter(position: PositionRole) {
        val current = _uiState.value.selectedPositions
        val newPositions = if (position in current) {
            current - position
        } else {
            current + position
        }
        _uiState.update { it.copy(selectedPositions = newPositions) }
        val filtered = _uiState.value.filteredHandIndices
        if (filtered.isNotEmpty() && _uiState.value.currentHandIndex !in filtered) {
            selectHand(filtered.first())
        }
    }

    fun selectAllPositions(positions: Set<PositionRole>) {
        _uiState.update { it.copy(selectedPositions = positions) }
        val filtered = _uiState.value.filteredHandIndices
        if (filtered.isNotEmpty() && _uiState.value.currentHandIndex !in filtered) {
            selectHand(filtered.first())
        }
    }

    fun clearPositionFilter() {
        if (_uiState.value.selectedPositions.isEmpty()) return
        _uiState.update { it.copy(selectedPositions = emptySet()) }
    }

    fun clearFilter() {
        _uiState.update { it.copy(activeFilter = HandFilter.ALL, selectedPositions = emptySet()) }
    }

    fun nextHand() {
        val filtered = _uiState.value.filteredHandIndices
        if (filtered.isEmpty()) return
        val currentPos = _uiState.value.filteredCurrentIndex
        val targetIndex = if (currentPos in 0 until filtered.lastIndex) {
            filtered[currentPos + 1]
        } else if (currentPos == -1) {
            filtered.firstOrNull { it > _uiState.value.currentHandIndex } ?: filtered.first()
        } else {
            return
        }

        if (targetIndex >= FREE_HAND_LIMIT && !_uiState.value.isFullAccessPurchased) {
            pauseAutoPlay()
            showPaywall()
            return
        }

        selectHand(targetIndex)
    }

    fun prevHand() {
        val filtered = _uiState.value.filteredHandIndices
        if (filtered.isEmpty()) return
        val currentPos = _uiState.value.filteredCurrentIndex
        if (currentPos > 0) {
            selectHand(filtered[currentPos - 1])
        } else if (currentPos == -1) {
            val prevOriginal = filtered.lastOrNull { it < _uiState.value.currentHandIndex } ?: filtered.last()
            selectHand(prevOriginal)
        }
    }

    fun selectHand(index: Int) {
        pauseAutoPlay()
        val hands = _uiState.value.hands
        if (index !in hands.indices) return

        // 30 free hands paywall check (index 0..29 are free, index >= 30 is locked)
        if (index >= FREE_HAND_LIMIT && !_uiState.value.isFullAccessPurchased) {
            showPaywall()
            return
        }

        val hand = hands[index]
        val steps = getOrComputeSteps(index, hand)
        val step0 = steps.firstOrNull()

        prefs.saveReplayerState(index, 0, hand.handId)

        _uiState.update {
            it.copy(
                currentHandIndex = index,
                currentStepIndex = 0,
                currentStepState = step0
            )
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pauseAutoPlay()
        } else {
            startAutoPlay()
        }
    }

    private fun startAutoPlay() {
        autoPlayJob?.cancel()
        _uiState.update { it.copy(isPlaying = true) }
        autoPlayJob = viewModelScope.launch {
            val speed = prefs.autoPlaySpeedMs
            while (isActive) {
                delay(speed)
                val currentState = _uiState.value
                val hand = currentState.currentHand ?: break
                val steps = getOrComputeSteps(currentState.currentHandIndex, hand)

                if (currentState.currentStepIndex < steps.lastIndex) {
                    val nextStep = currentState.currentStepIndex + 1
                    applyStep(nextStep, steps)
                } else {
                    // Hand finished - check if can advance or if paywall reached
                    val nextHandCandidate = currentState.currentHandIndex + 1
                    if (nextHandCandidate >= FREE_HAND_LIMIT && !currentState.isFullAccessPurchased) {
                        _uiState.update { it.copy(isPlaying = false) }
                        showPaywall()
                        break
                    } else {
                        _uiState.update { it.copy(isPlaying = false) }
                        break
                    }
                }
            }
        }
    }

    private fun pauseAutoPlay() {
        autoPlayJob?.cancel()
        autoPlayJob = null
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun showPaywall() {
        _uiState.update { it.copy(isPaywallVisible = true, purchaseStatusMessage = null) }
    }

    fun hidePaywall() {
        _uiState.update { it.copy(isPaywallVisible = false, purchaseStatusMessage = null) }
    }

    fun showPrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyVisible = true) }
    }

    fun hidePrivacyPolicy() {
        _uiState.update { it.copy(isPrivacyPolicyVisible = false) }
    }

    fun purchaseFullAccess(activity: Activity) {
        _uiState.update { it.copy(isPurchasing = true, purchaseStatusMessage = null) }
        RuStoreBillingManager.purchase(activity) { isSuccess, error ->
            _uiState.update {
                it.copy(
                    isPurchasing = false,
                    isFullAccessPurchased = isSuccess || it.isFullAccessPurchased,
                    purchaseStatusMessage = if (!isSuccess && error != null) error else null,
                    isPaywallVisible = if (isSuccess) false else it.isPaywallVisible
                )
            }
        }
    }

    fun restorePurchases(context: Context) {
        _uiState.update { it.copy(isPurchasing = true, purchaseStatusMessage = null) }
        RuStoreBillingManager.restorePurchases(context) { isSuccess, message ->
            _uiState.update {
                it.copy(
                    isPurchasing = false,
                    isFullAccessPurchased = isSuccess || it.isFullAccessPurchased,
                    purchaseStatusMessage = message,
                    isPaywallVisible = if (isSuccess) false else it.isPaywallVisible
                )
            }
        }
    }

    fun toggleUseBbUnits() {
        val newUnit = !_uiState.value.useBbUnits
        prefs.useBbUnits = newUnit
        _uiState.update { it.copy(useBbUnits = newUnit) }
    }

    fun setHandListVisible(visible: Boolean) {
        _uiState.update { it.copy(isHandListVisible = visible) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun unlockTestFullAccess() {
        prefs.isFullAccessPurchased = true
        _uiState.update {
            it.copy(
                isFullAccessPurchased = true,
                isPaywallVisible = false,
                purchaseStatusMessage = "Полный доступ активирован (тестовый режим)."
            )
        }
    }

    fun resetTestAccess() {
        prefs.isFullAccessPurchased = false
        _uiState.update {
            it.copy(
                isFullAccessPurchased = false,
                purchaseStatusMessage = "Сброшено к бесплатному режиму (30 раздач)."
            )
        }
    }

    companion object {
        const val FREE_HAND_LIMIT = 30
    }
}
