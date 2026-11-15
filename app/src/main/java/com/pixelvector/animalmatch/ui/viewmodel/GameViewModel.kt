package com.pixelvector.animalmatch.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelvector.animalmatch.data.local.GamePreferencesRepository
import com.pixelvector.animalmatch.domain.engine.Match3Engine
import com.pixelvector.animalmatch.domain.model.AnimalType
import com.pixelvector.animalmatch.domain.model.GameMode
import com.pixelvector.animalmatch.domain.model.GridPos
import com.pixelvector.animalmatch.domain.model.StageConfig
import com.pixelvector.animalmatch.domain.model.StageRepository
import com.pixelvector.animalmatch.domain.model.StageTargetType
import com.pixelvector.animalmatch.domain.model.TileCell
import com.pixelvector.animalmatch.domain.security.AntiMacroLimiter
import com.pixelvector.animalmatch.domain.security.SecureInt
import com.pixelvector.animalmatch.ui.components.SoundEffectPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class GameUiState(
    val isOnTitleScreen: Boolean = true,
    val gameMode: GameMode = GameMode.NORMAL,
    val board: List<List<TileCell>> = emptyList(),
    val score: Int = 0,
    val highScoreEasy: Int = 0,
    val highScoreNormal: Int = 0,
    val highScoreHard: Int = 0,
    val coins: Int = 0,
    val remainingSeconds: Int = 60,
    val comboCount: Int = 0,
    val feverProgress: Float = 0f,
    val isFeverTime: Boolean = false,
    // Easy 모드 전용: 3초간 입력이 없으면 매칭 가능한 두 칸을 반짝여 알려주는 힌트
    val hintPositions: Set<GridPos> = emptySet(),
    // Stage 모드 전용 상태
    val currentStage: Int = 1,
    val unlockedStage: Int = 1,
    val stageConfig: StageConfig = StageRepository.getStage(1),
    val remainingMoves: Int = 18,
    val stageProgress: Int = 0,
    val isStageCleared: Boolean = false,
    // 공통 인터랙션 상태
    val selectedPos: GridPos? = null,
    val isAnimating: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val isAdsRemoved: Boolean = false
) {
    val activeHighScore: Int
        get() = when (gameMode) {
            GameMode.EASY -> highScoreEasy
            GameMode.HARD -> highScoreHard
            else -> highScoreNormal
        }
}

class GameViewModel(
    private val preferencesRepository: GamePreferencesRepository,
    private val match3Engine: Match3Engine = Match3Engine()
) : ViewModel() {

    private val secureScore = SecureInt(0)
    private val secureCoins = SecureInt(0)
    private val antiMacroLimiter = AntiMacroLimiter()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameTimerJob: Job? = null
    private var feverTimerJob: Job? = null
    private var hintJob: Job? = null

    init {
        observePreferences()
        prepareBoardOnly()
    }

    private fun prepareBoardOnly() {
        val initialBoard = match3Engine.generateInitialBoard(getAnimalPoolForMode(GameMode.NORMAL))
        _uiState.update { state ->
            state.copy(
                isOnTitleScreen = true,
                board = initialBoard
            )
        }
    }

    fun startFromTitle(mode: GameMode, stageNum: Int = _uiState.value.currentStage) {
        _uiState.update { it.copy(isOnTitleScreen = false, gameMode = mode) }
        startNewGame(mode, stageNum)
    }

    fun previewStageOnTitle(stageNumber: Int) {
        val clamped = stageNumber.coerceIn(1, _uiState.value.unlockedStage)
        _uiState.update {
            it.copy(
                currentStage = clamped,
                stageConfig = StageRepository.getStage(clamped)
            )
        }
    }

    fun returnToTitle() {
        gameTimerJob?.cancel()
        feverTimerJob?.cancel()
        hintJob?.cancel()
        persistFinalRecords()
        _uiState.update {
            it.copy(
                isOnTitleScreen = true,
                isPaused = false,
                isFeverTime = false,
                selectedPos = null,
                hintPositions = emptySet()
            )
        }
    }

    /**
     * Easy 모드에서 입력이 3초간 없으면 매칭 가능한 스왑 위치를 힌트로 표시
     */
    private fun restartHintTimer() {
        hintJob?.cancel()
        if (_uiState.value.hintPositions.isNotEmpty()) {
            _uiState.update { it.copy(hintPositions = emptySet()) }
        }
        if (_uiState.value.gameMode != GameMode.EASY) return
        hintJob = viewModelScope.launch {
            delay(EASY_HINT_DELAY_MS)
            val s = _uiState.value
            if (s.gameMode == GameMode.EASY && !s.isAnimating && !s.isPaused &&
                !s.isGameOver && !s.isOnTitleScreen
            ) {
                val hint = match3Engine.findHintMove(s.board)
                if (hint != null) {
                    _uiState.update { it.copy(hintPositions = setOf(hint.first, hint.second)) }
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.playerProfileFlow.collect { profile ->
                secureCoins.set(profile.coins)
                _uiState.update { state ->
                    state.copy(
                        highScoreEasy = maxOf(state.highScoreEasy, profile.highScoreEasy),
                        highScoreNormal = maxOf(state.highScoreNormal, profile.highScoreNormal),
                        highScoreHard = maxOf(state.highScoreHard, profile.highScoreHard),
                        unlockedStage = maxOf(state.unlockedStage, profile.unlockedStage),
                        currentStage = if (state.gameMode == GameMode.STAGE) state.currentStage else profile.currentStage,
                        coins = secureCoins.get(),
                        isSoundEnabled = profile.isSoundEnabled,
                        isAdsRemoved = profile.isAdsRemoved
                    )
                }
            }
        }
    }

    fun selectGameMode(mode: GameMode) {
        if (_uiState.value.gameMode == mode && !_uiState.value.isGameOver) return
        _uiState.update { it.copy(gameMode = mode) }
        startNewGame(mode)
    }

    fun selectStage(stageNumber: Int) {
        val clamped = stageNumber.coerceIn(1, _uiState.value.unlockedStage)
        _uiState.update {
            it.copy(
                gameMode = GameMode.STAGE,
                currentStage = clamped
            )
        }
        viewModelScope.launch {
            preferencesRepository.saveStageProgress(_uiState.value.unlockedStage, clamped)
        }
        startNewGame(GameMode.STAGE, clamped)
    }

    fun nextStage() {
        val next = (_uiState.value.currentStage + 1).coerceAtMost(StageRepository.MAX_STAGE)
        val unlocked = maxOf(_uiState.value.unlockedStage, next)
        _uiState.update {
            it.copy(
                gameMode = GameMode.STAGE,
                currentStage = next,
                unlockedStage = unlocked
            )
        }
        viewModelScope.launch {
            preferencesRepository.saveStageProgress(unlocked, next)
        }
        startNewGame(GameMode.STAGE, next)
    }

    fun startNewGame(
        mode: GameMode = _uiState.value.gameMode,
        stageNum: Int = _uiState.value.currentStage
    ) {
        gameTimerJob?.cancel()
        feverTimerJob?.cancel()
        secureScore.set(0)
        antiMacroLimiter.reset()

        val activePool = getAnimalPoolForMode(mode)
        val stageCfg = StageRepository.getStage(stageNum)
        val icePositions = if (mode == GameMode.STAGE) stageCfg.icePositions else emptySet()
        val initialBoard = match3Engine.generateInitialBoard(activePool, icePositions, mode.gridSize)

        val initialDuration = when (mode) {
            GameMode.EASY -> EASY_DURATION_SECONDS
            GameMode.NORMAL -> NORMAL_DURATION_SECONDS
            GameMode.HARD -> HARD_DURATION_SECONDS
            GameMode.STAGE -> 0
        }

        _uiState.update { state ->
            state.copy(
                isOnTitleScreen = false,
                gameMode = mode,
                board = initialBoard,
                score = secureScore.get(),
                remainingSeconds = initialDuration,
                comboCount = 0,
                feverProgress = 0f,
                isFeverTime = false,
                currentStage = stageCfg.stageNumber,
                stageConfig = stageCfg,
                remainingMoves = stageCfg.maxMoves,
                stageProgress = 0,
                isStageCleared = false,
                selectedPos = null,
                isAnimating = false,
                isPaused = false,
                isGameOver = false
            )
        }

        if (mode != GameMode.STAGE) {
            startGameTimer()
        }
        restartHintTimer()
    }

    private fun getAnimalPoolForMode(mode: GameMode): List<AnimalType> {
        return when (mode) {
            // Easy 모드는 6x6 작은 보드 + 4종 동물만 등장하여 매칭/연쇄가 매우 쉽게 터짐
            GameMode.EASY -> AnimalType.entries.take(4)
            // Normal 모드는 5종 동물만 등장하여 연쇄가 쉽게 터지는 캐주얼 밸런스
            GameMode.NORMAL -> AnimalType.entries.take(5)
            // Hard 및 Stage 모드는 6종 동물 전체 등장
            GameMode.HARD, GameMode.STAGE -> AnimalType.entries
        }
    }

    private fun startGameTimer() {
        gameTimerJob?.cancel()
        gameTimerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 &&
                !_uiState.value.isGameOver &&
                _uiState.value.gameMode != GameMode.STAGE
            ) {
                delay(1000L)
                val current = _uiState.value
                if (!current.isPaused && !current.isGameOver) {
                    val nextSec = (current.remainingSeconds - 1).coerceAtLeast(0)
                    if (nextSec == 0) {
                        feverTimerJob?.cancel()
                        _uiState.update {
                            it.copy(
                                remainingSeconds = 0,
                                isGameOver = true,
                                isFeverTime = false,
                                selectedPos = null
                            )
                        }
                        persistFinalRecords()
                    } else {
                        _uiState.update { it.copy(remainingSeconds = nextSec) }
                    }
                }
            }
        }
    }

    fun onTileTapped(pos: GridPos) {
        val state = _uiState.value
        if (state.isPaused || state.isGameOver || state.isStageCleared || state.isAnimating) return
        restartHintTimer()

        // 1. 단독 터치 시: 만약 해당 타일이 특수 폭탄(레이저, 폭탄, 미러볼)이라면 즉시 격발!
        val cell = state.board.getOrNull(pos.row)?.getOrNull(pos.col)
        if (cell != null && cell.special != com.pixelvector.animalmatch.domain.model.SpecialBlockType.NORMAL) {
            detonateSpecialBlock(pos)
            return
        }

        val currentSelected = state.selectedPos
        if (currentSelected == null) {
            _uiState.update { it.copy(selectedPos = pos) }
        } else if (currentSelected == pos) {
            _uiState.update { it.copy(selectedPos = null) }
        } else if (currentSelected.isAdjacentTo(pos)) {
            onSwapRequested(currentSelected, pos)
        } else {
            _uiState.update { it.copy(selectedPos = pos) }
        }
    }

    /**
     * 특수 폭탄 타일 단독 터치 격발 로직
     */
    private fun detonateSpecialBlock(pos: GridPos) {
        val state = _uiState.value
        if (state.isPaused || state.isGameOver || state.isStageCleared || state.isAnimating) return
        if (!antiMacroLimiter.isAllowed()) return
        restartHintTimer()

        val cell = state.board[pos.row][pos.col]
        // 특수 블록 종류에 따른 전용 사운드 재생
        when (cell.special) {
            com.pixelvector.animalmatch.domain.model.SpecialBlockType.LINE_BOMB_H,
            com.pixelvector.animalmatch.domain.model.SpecialBlockType.LINE_BOMB_V -> {
                SoundEffectPlayer.playLaser(state.isSoundEnabled)
            }
            com.pixelvector.animalmatch.domain.model.SpecialBlockType.AREA_BOMB -> {
                SoundEffectPlayer.playExplosion(state.isSoundEnabled)
            }
            com.pixelvector.animalmatch.domain.model.SpecialBlockType.RAINBOW_MIRROR -> {
                SoundEffectPlayer.playMagicChime(state.isSoundEnabled)
            }
            else -> {}
        }

        val mode = state.gameMode
        val activePool = getAnimalPoolForMode(mode)
        val resolution = match3Engine.detonateSpecial(state.board, pos, activePool)
        if (!resolution.isValidMove) return

        val nextMoves = if (mode == GameMode.STAGE) {
            (state.remainingMoves - 1).coerceAtLeast(0)
        } else {
            state.remainingMoves
        }

        processCascadeResolution(resolution, nextMoves)
    }

    fun onSwapRequested(from: GridPos, to: GridPos) {
        val state = _uiState.value
        if (state.isPaused || state.isGameOver || state.isStageCleared || state.isAnimating) return
        if (!from.isAdjacentTo(to)) return
        if (!antiMacroLimiter.isAllowed()) return
        restartHintTimer()

        val mode = state.gameMode
        val activePool = getAnimalPoolForMode(mode)
        val originBoard = state.board
        val resolution = match3Engine.attemptSwap(originBoard, from, to, activePool)

        if (!resolution.isValidMove) {
            SoundEffectPlayer.playInvalidRollback(_uiState.value.isSoundEnabled)
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        board = resolution.swappedBoard,
                        selectedPos = null,
                        isAnimating = true
                    )
                }
                delay(220L)
                _uiState.update {
                    it.copy(
                        board = originBoard,
                        comboCount = 0,
                        feverProgress = if (it.isFeverTime) it.feverProgress else 0f,
                        isAnimating = false
                    )
                }
                restartHintTimer()
            }
            return
        }

        // 유효 스왑인 경우 Stage 모드라면 이동 횟수(Moves) 1 차감
        val nextMoves = if (mode == GameMode.STAGE) {
            (state.remainingMoves - 1).coerceAtLeast(0)
        } else {
            state.remainingMoves
        }

        processCascadeResolution(resolution, nextMoves)
    }

    private fun processCascadeResolution(
        resolution: com.pixelvector.animalmatch.domain.engine.SwapResolution,
        nextMoves: Int
    ) {
        val mode = _uiState.value.gameMode
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    board = resolution.swappedBoard,
                    remainingMoves = nextMoves,
                    selectedPos = null,
                    isAnimating = true
                )
            }
            delay(190L)

            for (step in resolution.cascadeSteps) {
                SoundEffectPlayer.playMatchPop(_uiState.value.isSoundEnabled, _uiState.value.comboCount + 1)
                val highlightedBoard = _uiState.value.board.map { row ->
                    row.map { cell ->
                        if (GridPos(cell.row, cell.col) in step.clearedPositions) {
                            cell.copy(isMatched = true)
                        } else {
                            cell
                        }
                    }
                }
                _uiState.update { it.copy(board = highlightedBoard) }
                delay(170L)

                val currentCombo = _uiState.value.comboCount + 1
                val feverMult = if (_uiState.value.isFeverTime) 2 else 1
                val hardMult = if (mode == GameMode.HARD) 1.5f else 1.0f
                val earnedScore = (step.baseScoreEarned * feverMult * hardMult).toInt()
                val updatedScore = secureScore.add(earnedScore)

                val updatedEasyBest = if (mode == GameMode.EASY) {
                    maxOf(_uiState.value.highScoreEasy, updatedScore)
                } else {
                    _uiState.value.highScoreEasy
                }
                val updatedNormalBest = if (mode == GameMode.NORMAL || mode == GameMode.STAGE) {
                    maxOf(_uiState.value.highScoreNormal, updatedScore)
                } else {
                    _uiState.value.highScoreNormal
                }
                val updatedHardBest = if (mode == GameMode.HARD) {
                    maxOf(_uiState.value.highScoreHard, updatedScore)
                } else {
                    _uiState.value.highScoreHard
                }

                val updatedCoins = secureCoins.add(maxOf(1, step.clearedPositions.size / 3))

                // Stage 목표 진행도 계산
                val cfg = _uiState.value.stageConfig
                val updatedStageProgress = when (cfg.targetType) {
                    StageTargetType.COLLECT_ANIMAL -> {
                        val targetAnimal = cfg.targetAnimal
                        val added = if (targetAnimal != null) {
                            step.clearedAnimalCounts[targetAnimal] ?: 0
                        } else {
                            0
                        }
                        (_uiState.value.stageProgress + added).coerceAtMost(cfg.targetValue)
                    }
                    StageTargetType.REACH_SCORE -> {
                        updatedScore.coerceAtMost(cfg.targetValue)
                    }
                    StageTargetType.BREAK_ICE -> {
                        (_uiState.value.stageProgress + step.brokenIceCount).coerceAtMost(cfg.targetValue)
                    }
                }

                val shouldTriggerFever = !_uiState.value.isFeverTime && currentCombo >= FEVER_COMBO_THRESHOLD
                val nextFeverProgress = when {
                    _uiState.value.isFeverTime -> _uiState.value.feverProgress
                    shouldTriggerFever -> 1f
                    else -> (currentCombo.toFloat() / FEVER_COMBO_THRESHOLD).coerceIn(0f, 1f)
                }

                _uiState.update {
                    it.copy(
                        board = step.boardAfterGravityAndRefill,
                        score = updatedScore,
                        highScoreEasy = updatedEasyBest,
                        highScoreNormal = updatedNormalBest,
                        highScoreHard = updatedHardBest,
                        coins = updatedCoins,
                        comboCount = currentCombo,
                        feverProgress = nextFeverProgress,
                        stageProgress = updatedStageProgress
                    )
                }

                if (shouldTriggerFever) {
                    triggerFeverMode()
                }

                delay(220L)
            }

            // 연쇄 종료 후 Stage 클리어 / 실패 판정
            val latest = _uiState.value
            val stageClearedNow = mode == GameMode.STAGE &&
                latest.stageProgress >= latest.stageConfig.targetValue
            val stageFailedNow = mode == GameMode.STAGE &&
                !stageClearedNow &&
                latest.remainingMoves <= 0

            val newUnlockedStage = if (stageClearedNow) {
                maxOf(latest.unlockedStage, (latest.currentStage + 1).coerceAtMost(StageRepository.MAX_STAGE))
            } else {
                latest.unlockedStage
            }

            _uiState.update {
                it.copy(
                    board = resolution.finalBoard,
                    isAnimating = false,
                    isStageCleared = stageClearedNow,
                    isGameOver = if (mode == GameMode.STAGE) stageFailedNow else it.isGameOver,
                    unlockedStage = newUnlockedStage
                )
            }
            restartHintTimer()

            if (stageClearedNow) {
                SoundEffectPlayer.playFeverStart(_uiState.value.isSoundEnabled)
                viewModelScope.launch {
                    preferencesRepository.saveStageProgress(newUnlockedStage, latest.currentStage)
                }
            }
            persistFinalRecords()
        }
    }

    private fun triggerFeverMode() {
        feverTimerJob?.cancel()
        SoundEffectPlayer.playFeverStart(_uiState.value.isSoundEnabled)
        _uiState.update { it.copy(isFeverTime = true, feverProgress = 1f) }

        feverTimerJob = viewModelScope.launch {
            val totalTicks = 50
            var remainingTicks = totalTicks
            while (remainingTicks > 0 && !_uiState.value.isGameOver && !_uiState.value.isStageCleared) {
                delay(100L)
                if (!_uiState.value.isPaused) {
                    remainingTicks--
                    val progress = remainingTicks.toFloat() / totalTicks
                    _uiState.update { it.copy(feverProgress = progress) }
                }
            }
            _uiState.update {
                it.copy(
                    isFeverTime = false,
                    feverProgress = 0f
                )
            }
        }
    }

    private fun persistFinalRecords() {
        val state = _uiState.value
        val currentCoins = secureCoins.get()
        viewModelScope.launch {
            preferencesRepository.updateHighScoreEasy(state.highScoreEasy)
            preferencesRepository.updateHighScoreNormal(state.highScoreNormal)
            preferencesRepository.updateHighScoreHard(state.highScoreHard)
            preferencesRepository.updateCoins(currentCoins)
        }
    }

    fun toggleSound() {
        val next = !_uiState.value.isSoundEnabled
        _uiState.update { it.copy(isSoundEnabled = next) }
        if (next) {
            SoundEffectPlayer.startBgm(true)
        } else {
            SoundEffectPlayer.stopBgm()
        }
        viewModelScope.launch {
            preferencesRepository.setSoundEnabled(next)
        }
    }

    fun togglePause() {
        if (_uiState.value.isGameOver || _uiState.value.isStageCleared) return
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun markAdsRemoved() {
        _uiState.update { it.copy(isAdsRemoved = true) }
        viewModelScope.launch {
            preferencesRepository.setAdsRemoved(true)
        }
    }

    companion object {
        const val GRID_SIZE = 8
        const val EASY_DURATION_SECONDS = 90
        const val NORMAL_DURATION_SECONDS = 60
        const val HARD_DURATION_SECONDS = 45
        const val FEVER_COMBO_THRESHOLD = 5
        const val EASY_HINT_DELAY_MS = 3000L

        fun provideFactory(repository: GamePreferencesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameViewModel(repository) as T
                }
            }
    }
}
