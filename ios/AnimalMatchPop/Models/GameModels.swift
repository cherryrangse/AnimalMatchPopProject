import SwiftUI

enum AnimalType: Int, CaseIterable, Identifiable, Equatable, Hashable {
    case bunny = 1
    case bear = 2
    case cat = 3
    case pig = 4
    case penguin = 5
    case frog = 6

    var id: Int { rawValue }

    var displayName: String {
        switch self {
        case .bunny: return "Bunny"
        case .bear: return "Bear"
        case .cat: return "Cat"
        case .pig: return "Pig"
        case .penguin: return "Penguin"
        case .frog: return "Frog"
        }
    }

    var color: Color {
        switch self {
        case .bunny: return Color.white
        case .bear: return Color(hex: 0x8D6E63)
        case .cat: return Color(hex: 0xFFD54F)
        case .pig: return Color(hex: 0xF06292)
        case .penguin: return Color(hex: 0x42A5F5)
        case .frog: return Color(hex: 0x81C784)
        }
    }
}

enum SpecialBlockType: Equatable, Hashable {
    case normal
    case lineBombH
    case lineBombV
    case areaBomb
    case rainbowMirror
}

enum GameMode: String, CaseIterable, Identifiable {
    case easy
    case normal
    case hard
    case stage

    var id: String { rawValue }

    var gridSize: Int {
        switch self {
        case .easy: return 6
        case .normal, .hard, .stage: return 8
        }
    }

    var title: String {
        switch self {
        case .easy: return "Easy"
        case .normal: return "Normal"
        case .hard: return "Hard"
        case .stage: return "Stage"
        }
    }

    var accentColor: Color {
        switch self {
        case .easy: return Color(hex: 0x5CC46A)
        case .normal: return Color(hex: 0xFF85A2)
        case .hard: return Color(hex: 0xFF4081)
        case .stage: return Color(hex: 0x42A5F5)
        }
    }
}

struct GridPos: Hashable, Equatable {
    let row: Int
    let col: Int

    func isAdjacent(to other: GridPos) -> Bool {
        let dr = abs(row - other.row)
        let dc = abs(col - other.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }
}

struct TileCell: Identifiable, Equatable {
    let id: Int64
    var row: Int
    var col: Int
    var animal: AnimalType
    var special: SpecialBlockType = .normal
    var isMatched: Bool = false
    var hasIce: Bool = false
}

enum StageTargetType {
    case collectAnimal
    case reachScore
    case breakIce
}

struct StageConfig: Equatable {
    let stageNumber: Int
    let maxMoves: Int
    let targetType: StageTargetType
    let targetValue: Int
    var targetAnimal: AnimalType? = nil
    var icePositions: Set<GridPos> = []
}

enum StageRepository {
    static let maxStage = 20

    static func getStage(_ stageNumber: Int) -> StageConfig {
        let stage = min(max(stageNumber, 1), maxStage)
        switch stage {
        case 1...5:
            let targets: [AnimalType] = [.bunny, .cat, .penguin, .bear, .pig]
            return StageConfig(
                stageNumber: stage,
                maxMoves: 18 - (stage - 1),
                targetType: .collectAnimal,
                targetValue: 10 + stage * 3,
                targetAnimal: targets[(stage - 1) % targets.count]
            )
        case 6...10:
            return StageConfig(
                stageNumber: stage,
                maxMoves: 16,
                targetType: .reachScore,
                targetValue: 1500 + (stage - 5) * 600
            )
        case 11...15:
            let iceCount = 4 + (stage - 11) * 2
            let iceSet = generateCenterIcePositions(count: iceCount)
            return StageConfig(
                stageNumber: stage,
                maxMoves: 16,
                targetType: .breakIce,
                targetValue: iceSet.count,
                icePositions: iceSet
            )
        default:
            let iceCount = 8 + (stage - 15) * 2
            let iceSet = generateCenterIcePositions(count: iceCount)
            return StageConfig(
                stageNumber: stage,
                maxMoves: 18,
                targetType: .breakIce,
                targetValue: iceSet.count,
                icePositions: iceSet
            )
        }
    }

    private static func generateCenterIcePositions(count: Int) -> Set<GridPos> {
        var candidates: [GridPos] = []
        for r in 2...5 {
            for c in 1...6 {
                candidates.append(GridPos(row: r, col: c))
            }
        }
        return Set(candidates.prefix(min(count, candidates.count)))
    }
}

extension Color {
    init(hex: UInt32, opacity: Double = 1.0) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: opacity)
    }

    static let bunnyPink = Color(hex: 0xFF85A2)
    static let feverPink = Color(hex: 0xFF4081)
    static let feverGold = Color(hex: 0xFFB300)
    static let penguinBlue = Color(hex: 0x42A5F5)
    static let easyGreen = Color(hex: 0x5CC46A)
    static let deepText = Color(hex: 0x3E2723)
    static let subText = Color(hex: 0x795548)
    static let pastelBg = Color(hex: 0xFFF8F2)
}
