import Foundation
import SwiftUI

@MainActor
final class GameViewModel: ObservableObject {
    static let easyDurationSeconds = 90
    static let normalDurationSeconds = 60
    static let hardDurationSeconds = 45
    static let feverComboThreshold = 5
    static let easyHintDelayNs: UInt64 = 3_000_000_000

    @Published var isOnTitleScreen: Bool = true
    @Published var gameMode: GameMode = .normal
    @Published var board: [[TileCell]] = []
    @Published var score: Int = 0
    @Published var highScoreEasy: Int = 0
    @Published var highScoreNormal: Int = 0
    @Published var highScoreHard: Int = 0
    @Published var coins: Int = 0
    @Published var remainingSeconds: Int = 60
    @Published var comboCount: Int = 0
    @Published var feverProgress: Double = 0.0
    @Published var isFeverTime: Bool = false
    @Published var hintPositions: Set<GridPos> = []

    // Stage mode
    @Published var currentStage: Int = 1
    @Published var unlockedStage: Int = 1
    @Published var stageConfig: StageConfig = StageRepository.getStage(1)
    @Published var remainingMoves: Int = 18
    @Published var stageProgress: Int = 0
    @Published var isStageCleared: Bool = false

    // Interaction
    @Published var selectedPos: GridPos? = nil
    @Published var isAnimating: Bool = false
    @Published var isSoundEnabled: Bool = true
    @Published var isPaused: Bool = false
    @Published var isGameOver: Bool = false

    var activeHighScore: Int {
        switch gameMode {
        case .easy: return highScoreEasy
        case .hard: return highScoreHard
        case .normal, .stage: return highScoreNormal
        }
    }

    private let engine = Match3Engine()
    private let defaults = UserDefaults.standard
    private var gameTimerTask: Task<Void, Never>?
    private var feverTimerTask: Task<Void, Never>?
    private var hintTask: Task<Void, Never>?

    init() {
        loadSavedProfile()
        board = engine.generateInitialBoard(animalPool: getAnimalPool(for: .normal), gridSize: GameMode.normal.gridSize)
        SoundManager.shared.initializeIfNeeded()
        if isSoundEnabled {
            SoundManager.shared.startBGM(enabled: true)
        }
    }

    private func loadSavedProfile() {
        highScoreEasy = defaults.integer(forKey: "highScoreEasy")
        highScoreNormal = defaults.integer(forKey: "highScoreNormal")
        highScoreHard = defaults.integer(forKey: "highScoreHard")
        coins = defaults.integer(forKey: "coins")
        let savedUnlocked = defaults.integer(forKey: "unlockedStage")
        unlockedStage = min(max(savedUnlocked, 1), StageRepository.maxStage)
        let savedCurrent = defaults.integer(forKey: "currentStage")
        currentStage = min(max(savedCurrent, 1), unlockedStage)
        stageConfig = StageRepository.getStage(currentStage)
        if defaults.object(forKey: "isSoundEnabled") != nil {
            isSoundEnabled = defaults.bool(forKey: "isSoundEnabled")
        }
    }

    private func persistProfile() {
        defaults.set(highScoreEasy, forKey: "highScoreEasy")
        defaults.set(highScoreNormal, forKey: "highScoreNormal")
        defaults.set(highScoreHard, forKey: "highScoreHard")
        defaults.set(coins, forKey: "coins")
        defaults.set(unlockedStage, forKey: "unlockedStage")
        defaults.set(currentStage, forKey: "currentStage")
        defaults.set(isSoundEnabled, forKey: "isSoundEnabled")
    }

    func startFromTitle(mode: GameMode, stageNum: Int? = nil) {
        isOnTitleScreen = false
        startNewGame(mode: mode, stageNum: stageNum ?? currentStage)
    }

    func previewStageOnTitle(_ stageNumber: Int) {
        let clamped = min(max(stageNumber, 1), unlockedStage)
        currentStage = clamped
        stageConfig = StageRepository.getStage(clamped)
    }

    func returnToTitle() {
        gameTimerTask?.cancel()
        feverTimerTask?.cancel()
        hintTask?.cancel()
        persistProfile()
        isOnTitleScreen = true
        isPaused = false
        isFeverTime = false
        selectedPos = nil
        hintPositions = []
    }

    func selectGameMode(_ mode: GameMode) {
        if gameMode == mode && !isGameOver { return }
        startNewGame(mode: mode)
    }

    func selectStage(_ stageNumber: Int) {
        let clamped = min(max(stageNumber, 1), unlockedStage)
        currentStage = clamped
        persistProfile()
        startNewGame(mode: .stage, stageNum: clamped)
    }

    func nextStage() {
        let next = min(currentStage + 1, StageRepository.maxStage)
        unlockedStage = max(unlockedStage, next)
        currentStage = next
        persistProfile()
        startNewGame(mode: .stage, stageNum: next)
    }

    func startNewGame(mode: GameMode? = nil, stageNum: Int? = nil) {
        let targetMode = mode ?? gameMode
        let targetStage = stageNum ?? currentStage

        gameTimerTask?.cancel()
        feverTimerTask?.cancel()
        hintTask?.cancel()

        let pool = getAnimalPool(for: targetMode)
        let cfg = StageRepository.getStage(targetStage)
        let ice: Set<GridPos> = targetMode == .stage ? cfg.icePositions : []
        let newBoard = engine.generateInitialBoard(animalPool: pool, icePositions: ice, gridSize: targetMode.gridSize)

        let duration: Int
        switch targetMode {
        case .easy: duration = Self.easyDurationSeconds
        case .normal: duration = Self.normalDurationSeconds
        case .hard: duration = Self.hardDurationSeconds
        case .stage: duration = 0
        }

        isOnTitleScreen = false
        gameMode = targetMode
        board = newBoard
        score = 0
        remainingSeconds = duration
        comboCount = 0
        feverProgress = 0
        isFeverTime = false
        hintPositions = []
        currentStage = cfg.stageNumber
        stageConfig = cfg
        remainingMoves = cfg.maxMoves
        stageProgress = 0
        isStageCleared = false
        selectedPos = nil
        isAnimating = false
        isPaused = false
        isGameOver = false

        if targetMode != .stage {
            startGameTimer()
        }
        restartHintTimer()
    }

    private func getAnimalPool(for mode: GameMode) -> [AnimalType] {
        switch mode {
        case .easy: return Array(AnimalType.allCases.prefix(4))
        case .normal: return Array(AnimalType.allCases.prefix(5))
        case .hard, .stage: return AnimalType.allCases
        }
    }

    private func startGameTimer() {
        gameTimerTask?.cancel()
        gameTimerTask = Task {
            while !Task.isCancelled && remainingSeconds > 0 && !isGameOver && gameMode != .stage {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                if Task.isCancelled { break }
                if !isPaused && !isGameOver {
                    let next = max(remainingSeconds - 1, 0)
                    remainingSeconds = next
                    if next == 0 {
                        feverTimerTask?.cancel()
                        isGameOver = true
                        isFeverTime = false
                        selectedPos = nil
                        persistProfile()
                    }
                }
            }
        }
    }

    private func restartHintTimer() {
        hintTask?.cancel()
        if !hintPositions.isEmpty {
            hintPositions = []
        }
        guard gameMode == .easy else { return }
        hintTask = Task {
            try? await Task.sleep(nanoseconds: Self.easyHintDelayNs)
            if Task.isCancelled { return }
            if gameMode == .easy && !isAnimating && !isPaused && !isGameOver && !isOnTitleScreen {
                if let hint = engine.findHintMove(board) {
                    hintPositions = [hint.0, hint.1]
                }
            }
        }
    }

    func onTileTapped(_ pos: GridPos) {
        guard !isPaused, !isGameOver, !isStageCleared, !isAnimating else { return }
        restartHintTimer()

        if pos.row < board.count, pos.col < board[pos.row].count {
            let cell = board[pos.row][pos.col]
            if cell.special != .normal {
                detonateSpecialBlock(at: pos, cell: cell)
                return
            }
        }

        if let current = selectedPos {
            if current == pos {
                selectedPos = nil
            } else if current.isAdjacent(to: pos) {
                onSwapRequested(from: current, to: pos)
            } else {
                selectedPos = pos
            }
        } else {
            selectedPos = pos
        }
    }

    private func detonateSpecialBlock(at pos: GridPos, cell: TileCell) {
        switch cell.special {
        case .lineBombH, .lineBombV:
            SoundManager.shared.playLaser(enabled: isSoundEnabled)
        case .areaBomb:
            SoundManager.shared.playExplosion(enabled: isSoundEnabled)
        case .rainbowMirror:
            SoundManager.shared.playMagicChime(enabled: isSoundEnabled)
        case .normal:
            break
        }

        let pool = getAnimalPool(for: gameMode)
        let resolution = engine.detonateSpecial(currentBoard: board, targetPos: pos, animalPool: pool)
        guard resolution.isValidMove else { return }

        let nextMoves = gameMode == .stage ? max(remainingMoves - 1, 0) : remainingMoves
        processCascadeResolution(resolution, nextMoves: nextMoves)
    }

    func onSwapRequested(from: GridPos, to: GridPos) {
        guard !isPaused, !isGameOver, !isStageCleared, !isAnimating else { return }
        guard from.isAdjacent(to: to) else { return }
        restartHintTimer()

        let originBoard = board
        let pool = getAnimalPool(for: gameMode)
        let resolution = engine.attemptSwap(currentBoard: originBoard, from: from, to: to, animalPool: pool)

        if !resolution.isValidMove {
            SoundManager.shared.playInvalidRollback(enabled: isSoundEnabled)
            Task {
                withAnimation(.spring(response: 0.25, dampingFraction: 0.72)) {
                    board = resolution.swappedBoard
                }
                selectedPos = nil
                isAnimating = true
                try? await Task.sleep(nanoseconds: 220_000_000)
                withAnimation(.spring(response: 0.25, dampingFraction: 0.72)) {
                    board = originBoard
                }
                comboCount = 0
                if !isFeverTime { feverProgress = 0 }
                isAnimating = false
                restartHintTimer()
            }
            return
        }

        let nextMoves = gameMode == .stage ? max(remainingMoves - 1, 0) : remainingMoves
        processCascadeResolution(resolution, nextMoves: nextMoves)
    }

    private func processCascadeResolution(_ resolution: SwapResolution, nextMoves: Int) {
        let mode = gameMode
        Task {
            withAnimation(.spring(response: 0.24, dampingFraction: 0.72)) {
                board = resolution.swappedBoard
            }
            remainingMoves = nextMoves
            selectedPos = nil
            isAnimating = true
            try? await Task.sleep(nanoseconds: 190_000_000)

            for step in resolution.cascadeSteps {
                SoundManager.shared.playMatchPop(enabled: isSoundEnabled, combo: comboCount + 1)

                var highlighted = board
                for r in 0..<highlighted.count {
                    for c in 0..<highlighted[r].count {
                        if step.clearedPositions.contains(GridPos(row: r, col: c)) {
                            highlighted[r][c].isMatched = true
                        }
                    }
                }
                withAnimation(.easeOut(duration: 0.14)) {
                    board = highlighted
                }
                try? await Task.sleep(nanoseconds: 170_000_000)

                let nextCombo = comboCount + 1
                let feverMult = isFeverTime ? 2.0 : 1.0
                let hardMult = mode == .hard ? 1.5 : 1.0
                let earned = Int(Double(step.baseScoreEarned) * feverMult * hardMult)
                let updatedScore = score + earned

                score = updatedScore
                if mode == .easy {
                    highScoreEasy = max(highScoreEasy, updatedScore)
                } else if mode == .hard {
                    highScoreHard = max(highScoreHard, updatedScore)
                } else {
                    highScoreNormal = max(highScoreNormal, updatedScore)
                }

                coins += max(1, step.clearedPositions.count / 3)
                comboCount = nextCombo

                switch stageConfig.targetType {
                case .collectAnimal:
                    if let target = stageConfig.targetAnimal {
                        let added = step.clearedAnimalCounts[target] ?? 0
                        stageProgress = min(stageProgress + added, stageConfig.targetValue)
                    }
                case .reachScore:
                    stageProgress = min(updatedScore, stageConfig.targetValue)
                case .breakIce:
                    stageProgress = min(stageProgress + step.brokenIceCount, stageConfig.targetValue)
                }

                let shouldTriggerFever = !isFeverTime && nextCombo >= Self.feverComboThreshold
                if shouldTriggerFever {
                    feverProgress = 1.0
                } else if !isFeverTime {
                    feverProgress = min(max(Double(nextCombo) / Double(Self.feverComboThreshold), 0.0), 1.0)
                }

                withAnimation(.spring(response: 0.34, dampingFraction: 0.64)) {
                    board = step.boardAfterGravityAndRefill
                }

                if shouldTriggerFever {
                    triggerFeverMode()
                }

                try? await Task.sleep(nanoseconds: 220_000_000)
            }

            let clearedNow = mode == .stage && stageProgress >= stageConfig.targetValue
            let failedNow = mode == .stage && !clearedNow && remainingMoves <= 0

            if clearedNow {
                unlockedStage = max(unlockedStage, min(currentStage + 1, StageRepository.maxStage))
                SoundManager.shared.playFeverStart(enabled: isSoundEnabled)
            }

            withAnimation(.spring(response: 0.3, dampingFraction: 0.72)) {
                board = resolution.finalBoard
            }
            isAnimating = false
            isStageCleared = clearedNow
            if mode == .stage {
                isGameOver = failedNow
            }
            restartHintTimer()
            persistProfile()
        }
    }

    private func triggerFeverMode() {
        feverTimerTask?.cancel()
        SoundManager.shared.playFeverStart(enabled: isSoundEnabled)
        isFeverTime = true
        feverProgress = 1.0

        feverTimerTask = Task {
            let totalTicks = 50
            var remaining = totalTicks
            while !Task.isCancelled && remaining > 0 && !isGameOver && !isStageCleared {
                try? await Task.sleep(nanoseconds: 100_000_000)
                if Task.isCancelled { break }
                if !isPaused {
                    remaining -= 1
                    feverProgress = Double(remaining) / Double(totalTicks)
                }
            }
            isFeverTime = false
            feverProgress = 0.0
        }
    }

    func toggleSound() {
        isSoundEnabled.toggle()
        if isSoundEnabled {
            SoundManager.shared.startBGM(enabled: true)
        } else {
            SoundManager.shared.stopBGM()
        }
        persistProfile()
    }

    func togglePause() {
        guard !isGameOver && !isStageCleared else { return }
        isPaused.toggle()
    }
}
