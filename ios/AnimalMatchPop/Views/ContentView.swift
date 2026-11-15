import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = GameViewModel()
    @State private var showLaunchSplash = true

    var body: some View {
        ZStack {
            Image("bg_forest_meadow")
                .resizable()
                .scaledToFill()
                .ignoresSafeArea()

            Color.white.opacity(viewModel.isOnTitleScreen ? 0.16 : 0.22)
                .ignoresSafeArea()

            if viewModel.isOnTitleScreen {
                TitleStartView(viewModel: viewModel)
                    .transition(.opacity)
            } else {
                InGameMatchView(viewModel: viewModel)
                    .transition(.opacity)
            }

            FeverParticleOverlayView(isActive: !viewModel.isOnTitleScreen && viewModel.isFeverTime)
                .allowsHitTesting(false)
                .ignoresSafeArea()

            if showLaunchSplash {
                ForestLaunchSplashView()
                    .transition(.opacity)
                    .zIndex(10)
            }
        }
        .statusBarHidden(true)
        .persistentSystemOverlays(.hidden)
        .task {
            try? await Task.sleep(nanoseconds: 950_000_000)
            withAnimation(.easeOut(duration: 0.35)) {
                showLaunchSplash = false
            }
        }
    }
}

private struct ForestLaunchSplashView: View {
    @State private var pulse = false

    var body: some View {
        ZStack {
            Image("bg_forest_meadow")
                .resizable()
                .scaledToFill()
                .ignoresSafeArea()

            Color(hex: 0x0B2418, opacity: 0.35)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                Image("ic_launcher_kawaii")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 112, height: 112)
                    .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
                    .shadow(color: .black.opacity(0.28), radius: 14, y: 6)
                    .scaleEffect(pulse ? 1.04 : 0.96)
                    .animation(.easeInOut(duration: 0.7).repeatForever(autoreverses: true), value: pulse)

                Text("ANIMAL MATCH POP")
                    .font(.system(size: 28, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .shadow(color: .black.opacity(0.45), radius: 6, y: 2)

                Text("Forest Puzzle Adventure")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(Color(hex: 0xE8F8EE))
            }
        }
        .onAppear { pulse = true }
    }
}

private struct TitleStartView: View {
    @ObservedObject var viewModel: GameViewModel

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                HStack(spacing: 6) {
                    Circle()
                        .fill(Color.feverGold)
                        .frame(width: 16, height: 16)
                        .overlay(
                            Circle()
                                .stroke(Color.white, lineWidth: 1.5)
                        )
                    Text("\(viewModel.coins)")
                        .font(.system(size: 14, weight: .black, design: .rounded))
                        .foregroundColor(.deepText)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(.ultraThinMaterial, in: Capsule())

                Spacer()

                Button {
                    viewModel.toggleSound()
                } label: {
                    Image(systemName: viewModel.isSoundEnabled ? "speaker.wave.2.fill" : "speaker.slash.fill")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.deepText)
                        .frame(width: 36, height: 36)
                        .background(.ultraThinMaterial, in: Circle())
                }
            }
            .padding(.horizontal, 18)
            .padding(.top, 10)

            Spacer(minLength: 4)

            VStack(spacing: 8) {
                Image("ic_launcher_kawaii")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 84, height: 84)
                    .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                    .shadow(color: .black.opacity(0.22), radius: 10, y: 4)

                Text("ANIMAL MATCH POP")
                    .font(.system(size: 26, weight: .black, design: .rounded))
                    .foregroundColor(.white)
                    .shadow(color: Color.deepText.opacity(0.65), radius: 6, y: 2)

                HStack(spacing: 8) {
                    ForEach(AnimalType.allCases) { animal in
                        AnimalVectorTileView(
                            animal: animal,
                            special: .normal,
                            isSelected: false,
                            isMatched: false,
                            tileId: Int64(animal.rawValue)
                        )
                        .frame(width: 40, height: 40)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(Color.white.opacity(0.78), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                .shadow(color: .black.opacity(0.1), radius: 6, y: 3)
            }

            VStack(spacing: 10) {
                ModeStartButton(
                    title: "EASY MODE",
                    subtitle: "6x6 Board • 90s • 4 Animals • Auto Hint",
                    badge: "BEST \(viewModel.highScoreEasy)",
                    color: .easyGreen
                ) {
                    viewModel.startFromTitle(mode: .easy)
                }

                ModeStartButton(
                    title: "NORMAL MODE",
                    subtitle: "8x8 Board • 60s • 5 Animals",
                    badge: "BEST \(viewModel.highScoreNormal)",
                    color: .bunnyPink
                ) {
                    viewModel.startFromTitle(mode: .normal)
                }

                ModeStartButton(
                    title: "HARD MODE",
                    subtitle: "8x8 Board • 45s • 6 Animals • 1.5x Score",
                    badge: "BEST \(viewModel.highScoreHard)",
                    color: .feverPink
                ) {
                    viewModel.startFromTitle(mode: .hard)
                }

                VStack(spacing: 8) {
                    HStack {
                        Button {
                            viewModel.previewStageOnTitle(viewModel.currentStage - 1)
                        } label: {
                            Image(systemName: "chevron.left.circle.fill")
                                .font(.system(size: 22))
                                .foregroundColor(viewModel.currentStage > 1 ? .penguinBlue : .gray.opacity(0.4))
                        }
                        .disabled(viewModel.currentStage <= 1)

                        Spacer()

                        VStack(spacing: 2) {
                            Text("STAGE \(viewModel.currentStage) / \(StageRepository.maxStage)")
                                .font(.system(size: 16, weight: .black, design: .rounded))
                                .foregroundColor(.deepText)
                            Text(stageSummaryText(viewModel.stageConfig))
                                .font(.system(size: 12, weight: .semibold, design: .rounded))
                                .foregroundColor(.subText)
                        }

                        Spacer()

                        Button {
                            viewModel.previewStageOnTitle(viewModel.currentStage + 1)
                        } label: {
                            Image(systemName: "chevron.right.circle.fill")
                                .font(.system(size: 22))
                                .foregroundColor(viewModel.currentStage < viewModel.unlockedStage ? .penguinBlue : .gray.opacity(0.4))
                        }
                        .disabled(viewModel.currentStage >= viewModel.unlockedStage)
                    }

                    Button {
                        viewModel.startFromTitle(mode: .stage, stageNum: viewModel.currentStage)
                    } label: {
                        Text("PLAY STAGE \(viewModel.currentStage)")
                            .font(.system(size: 15, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.penguinBlue, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }
                }
                .padding(12)
                .background(Color.white.opacity(0.92), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                .shadow(color: .black.opacity(0.12), radius: 6, y: 3)
            }
            .padding(.horizontal, 18)

            Spacer(minLength: 8)
        }
    }
}

private struct ModeStartButton: View {
    let title: String
    let subtitle: String
    let badge: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.system(size: 16, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                    Text(subtitle)
                        .font(.system(size: 11, weight: .semibold, design: .rounded))
                        .foregroundColor(.white.opacity(0.92))
                }
                Spacer()
                Text(badge)
                    .font(.system(size: 11, weight: .bold, design: .rounded))
                    .foregroundColor(color)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.white, in: Capsule())
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(color, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .shadow(color: color.opacity(0.35), radius: 6, y: 3)
        }
    }
}

private struct InGameMatchView: View {
    @ObservedObject var viewModel: GameViewModel

    var body: some View {
        VStack(spacing: 10) {
            // Mode selector bar
            HStack(spacing: 6) {
                ForEach(GameMode.allCases) { mode in
                    let isSelected = viewModel.gameMode == mode
                    Button {
                        viewModel.selectGameMode(mode)
                    } label: {
                        Text(mode.title)
                            .font(.system(size: 12, weight: .black, design: .rounded))
                            .foregroundColor(isSelected ? .white : .deepText)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 7)
                            .background(
                                isSelected ? mode.accentColor : Color.white.opacity(0.85),
                                in: RoundedRectangle(cornerRadius: 10, style: .continuous)
                            )
                    }
                }
            }
            .padding(.horizontal, 14)
            .padding(.top, 8)

            // HUD card
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("SCORE")
                        .font(.system(size: 10, weight: .bold, design: .rounded))
                        .foregroundColor(.subText)
                    Text("\(viewModel.score)")
                        .font(.system(size: 22, weight: .black, design: .rounded))
                        .foregroundColor(.deepText)
                }

                Spacer()

                if viewModel.gameMode == .stage {
                    VStack(spacing: 2) {
                        Text("STAGE \(viewModel.currentStage)")
                            .font(.system(size: 11, weight: .black, design: .rounded))
                            .foregroundColor(.penguinBlue)
                        Text("\(viewModel.stageProgress)/\(viewModel.stageConfig.targetValue)")
                            .font(.system(size: 16, weight: .black, design: .rounded))
                            .foregroundColor(.deepText)
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 2) {
                        Text("MOVES")
                            .font(.system(size: 10, weight: .bold, design: .rounded))
                            .foregroundColor(.subText)
                        Text("\(viewModel.remainingMoves)")
                            .font(.system(size: 22, weight: .black, design: .rounded))
                            .foregroundColor(viewModel.remainingMoves <= 4 ? .feverPink : .deepText)
                    }
                } else {
                    if viewModel.comboCount > 1 {
                        Text("\(viewModel.comboCount) COMBO!")
                            .font(.system(size: 13, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color.feverPink, in: Capsule())
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 2) {
                        Text("TIME")
                            .font(.system(size: 10, weight: .bold, design: .rounded))
                            .foregroundColor(.subText)
                        Text("\(viewModel.remainingSeconds)s")
                            .font(.system(size: 22, weight: .black, design: .rounded))
                            .foregroundColor(viewModel.remainingSeconds <= 10 ? .feverPink : .deepText)
                    }
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color.white.opacity(0.9), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .padding(.horizontal, 14)

            // Fever progress bar
            VStack(spacing: 4) {
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color.white.opacity(0.55))
                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: viewModel.isFeverTime ? [.feverPink, .feverGold] : [.bunnyPink, .feverGold],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: max(8, geo.size.width * min(max(viewModel.feverProgress, 0), 1)))
                            .animation(.easeOut(duration: 0.25), value: viewModel.feverProgress)
                    }
                }
                .frame(height: 10)

                HStack {
                    Text(viewModel.isFeverTime ? "FEVER TIME! 2X SCORE" : "FEVER")
                        .font(.system(size: 11, weight: .black, design: .rounded))
                        .foregroundColor(viewModel.isFeverTime ? .feverPink : .white)
                        .shadow(color: .black.opacity(0.35), radius: 2, y: 1)
                    Spacer()
                    if viewModel.gameMode == .easy {
                        Text("Tap bomb to pop • Auto-hint ON")
                            .font(.system(size: 10, weight: .bold, design: .rounded))
                            .foregroundColor(.white)
                            .shadow(color: .black.opacity(0.35), radius: 2, y: 1)
                    }
                }
            }
            .padding(.horizontal, 16)

            Spacer(minLength: 2)

            // Match-3 Board
            ZStack {
                MatchBoardGrid(viewModel: viewModel)

                if viewModel.isPaused || viewModel.isGameOver || viewModel.isStageCleared {
                    BoardStateOverlay(viewModel: viewModel)
                }
            }
            .padding(.horizontal, 10)

            Spacer(minLength: 2)

            // Bottom control bar
            HStack(spacing: 10) {
                Button {
                    viewModel.returnToTitle()
                } label: {
                    Label("Home", systemImage: "house.fill")
                        .font(.system(size: 13, weight: .bold, design: .rounded))
                        .foregroundColor(.deepText)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.white.opacity(0.9), in: Capsule())
                }

                Button {
                    viewModel.toggleSound()
                } label: {
                    Image(systemName: viewModel.isSoundEnabled ? "speaker.wave.2.fill" : "speaker.slash.fill")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.deepText)
                        .frame(width: 36, height: 36)
                        .background(Color.white.opacity(0.9), in: Circle())
                }

                Button {
                    viewModel.togglePause()
                } label: {
                    Image(systemName: viewModel.isPaused ? "play.fill" : "pause.fill")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.deepText)
                        .frame(width: 36, height: 36)
                        .background(Color.white.opacity(0.9), in: Circle())
                }

                Spacer()

                Text("BEST \(viewModel.activeHighScore)")
                    .font(.system(size: 13, weight: .black, design: .rounded))
                    .foregroundColor(.deepText)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color.white.opacity(0.9), in: Capsule())
            }
            .padding(.horizontal, 14)
            .padding(.bottom, 10)
        }
    }
}

private struct MatchBoardGrid: View {
    @ObservedObject var viewModel: GameViewModel

    var body: some View {
        let size = viewModel.board.count
        GeometryReader { geo in
            let side = min(geo.size.width, geo.size.height)
            let cellSize = size > 0 ? side / CGFloat(size) : 40
            let allCells = viewModel.board.flatMap { $0 }

            ZStack(alignment: .topLeading) {
                // Slot highlight / ice / gesture grid
                VStack(spacing: 0) {
                    ForEach(0..<size, id: \.self) { r in
                        HStack(spacing: 0) {
                            ForEach(0..<size, id: \.self) { c in
                                let pos = GridPos(row: r, col: c)
                                let isSelected = viewModel.selectedPos == pos
                                let isHint = viewModel.hintPositions.contains(pos)
                                let hasIce = r < viewModel.board.count && c < viewModel.board[r].count && viewModel.board[r][c].hasIce

                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .fill(
                                        hasIce
                                        ? Color(hex: 0xB3E5FC, opacity: 0.45)
                                        : (isSelected
                                           ? Color.feverGold.opacity(0.28)
                                           : (isHint ? Color.easyGreen.opacity(0.26) : Color.clear))
                                    )
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                                            .stroke(
                                                isSelected
                                                ? Color.feverGold
                                                : (isHint ? Color.easyGreen : (hasIce ? Color.white.opacity(0.75) : Color.clear)),
                                                lineWidth: isSelected || isHint ? 2.5 : 1.5
                                            )
                                    )
                                    .frame(width: cellSize, height: cellSize)
                                    .contentShape(Rectangle())
                                    .onTapGesture {
                                        viewModel.onTileTapped(pos)
                                    }
                                    .gesture(
                                        DragGesture(minimumDistance: 12)
                                            .onEnded { value in
                                                let dx = value.translation.width
                                                let dy = value.translation.height
                                                var target = pos
                                                if abs(dx) > abs(dy) {
                                                    target = GridPos(row: r, col: c + (dx > 0 ? 1 : -1))
                                                } else {
                                                    target = GridPos(row: r + (dy > 0 ? 1 : -1), col: c)
                                                }
                                                if target.row >= 0 && target.row < size && target.col >= 0 && target.col < size {
                                                    viewModel.onSwapRequested(from: pos, to: target)
                                                }
                                            }
                                    )
                            }
                        }
                    }
                }

                // Animated tile layer keyed by cell.id
                ForEach(allCells) { cell in
                    let pos = GridPos(row: cell.row, col: cell.col)
                    AnimalVectorTileView(
                        animal: cell.animal,
                        special: cell.special,
                        isSelected: viewModel.selectedPos == pos,
                        isMatched: cell.isMatched,
                        tileId: cell.id
                    )
                    .frame(width: cellSize - 4, height: cellSize - 4)
                    .offset(
                        x: CGFloat(cell.col) * cellSize + 2,
                        y: CGFloat(cell.row) * cellSize + 2
                    )
                    .allowsHitTesting(false)
                    .transition(.move(edge: .top).combined(with: .opacity))
                }
            }
            .frame(width: side, height: side)
            .background(Color.white.opacity(0.28), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(Color.white.opacity(0.55), lineWidth: 2)
            )
            .position(x: geo.size.width / 2, y: geo.size.height / 2)
        }
        .aspectRatio(1, contentMode: .fit)
    }
}

private struct BoardStateOverlay: View {
    @ObservedObject var viewModel: GameViewModel

    var body: some View {
        ZStack {
            Color.black.opacity(0.42)
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

            VStack(spacing: 14) {
                if viewModel.isPaused {
                    Text("PAUSED")
                        .font(.system(size: 26, weight: .black, design: .rounded))
                        .foregroundColor(.deepText)

                    Button {
                        viewModel.togglePause()
                    } label: {
                        Text("RESUME")
                            .font(.system(size: 16, weight: .black, design: .rounded))
                            .foregroundColor(.white)
                            .frame(width: 170)
                            .padding(.vertical, 10)
                            .background(Color.easyGreen, in: Capsule())
                    }
                } else if viewModel.isStageCleared {
                    Text("STAGE CLEAR!")
                        .font(.system(size: 24, weight: .black, design: .rounded))
                        .foregroundColor(.penguinBlue)
                    Text("Score: \(viewModel.score)")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundColor(.deepText)

                    if viewModel.currentStage < StageRepository.maxStage {
                        Button {
                            viewModel.nextStage()
                        } label: {
                            Text("NEXT STAGE")
                                .font(.system(size: 16, weight: .black, design: .rounded))
                                .foregroundColor(.white)
                                .frame(width: 170)
                                .padding(.vertical, 10)
                                .background(Color.penguinBlue, in: Capsule())
                        }
                    }
                } else if viewModel.isGameOver {
                    Text("TIME'S UP!")
                        .font(.system(size: 24, weight: .black, design: .rounded))
                        .foregroundColor(.feverPink)
                    Text("Score: \(viewModel.score)")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                        .foregroundColor(.deepText)
                }

                Button {
                    viewModel.startNewGame()
                } label: {
                    Text("PLAY AGAIN")
                        .font(.system(size: 15, weight: .black, design: .rounded))
                        .foregroundColor(.white)
                        .frame(width: 170)
                        .padding(.vertical, 10)
                        .background(Color.bunnyPink, in: Capsule())
                }

                Button {
                    viewModel.returnToTitle()
                } label: {
                    Text("TITLE LOBBY")
                        .font(.system(size: 14, weight: .bold, design: .rounded))
                        .foregroundColor(.subText)
                }
            }
            .padding(24)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .shadow(color: .black.opacity(0.25), radius: 14, y: 6)
        }
    }
}

private func stageSummaryText(_ config: StageConfig) -> String {
    switch config.targetType {
    case .collectAnimal:
        let name = config.targetAnimal?.displayName ?? "Animal"
        return "Collect \(config.targetValue) \(name) • \(config.maxMoves) Moves"
    case .reachScore:
        return "Reach \(config.targetValue) pts • \(config.maxMoves) Moves"
    case .breakIce:
        return "Break \(config.targetValue) Ice • \(config.maxMoves) Moves"
    }
}
