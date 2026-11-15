import Foundation

struct SpawnedSpecial {
    let pos: GridPos
    let animal: AnimalType
    let specialType: SpecialBlockType
}

struct CascadeStep {
    let stepIndex: Int
    let clearedPositions: Set<GridPos>
    let clearedAnimalCounts: [AnimalType: Int]
    let brokenIceCount: Int
    let spawnedSpecials: [SpawnedSpecial]
    let boardAfterGravityAndRefill: [[TileCell]]
    let baseScoreEarned: Int
}

struct SwapResolution {
    let isValidMove: Bool
    let swappedBoard: [[TileCell]]
    let cascadeSteps: [CascadeStep]
    let finalBoard: [[TileCell]]
}

final class Match3Engine {
    static let defaultGridSize = 8
    private var tileIdCounter: Int64 = 1

    func nextTileId() -> Int64 {
        let current = tileIdCounter
        tileIdCounter += 1
        return current
    }

    func generateInitialBoard(
        animalPool: [AnimalType] = AnimalType.allCases,
        icePositions: Set<GridPos> = [],
        gridSize: Int = Match3Engine.defaultGridSize
    ) -> [[TileCell]] {
        let activePool = animalPool.count >= 3 ? animalPool : AnimalType.allCases
        for _ in 0..<20 {
            var grid: [[TileCell?]] = Array(
                repeating: Array(repeating: nil, count: gridSize),
                count: gridSize
            )
            for r in 0..<gridSize {
                for c in 0..<gridSize {
                    var forbidden = Set<AnimalType>()
                    if c >= 2,
                       let a1 = grid[r][c - 1]?.animal,
                       let a2 = grid[r][c - 2]?.animal,
                       a1 == a2 {
                        forbidden.insert(a1)
                    }
                    if r >= 2,
                       let a1 = grid[r - 1][c]?.animal,
                       let a2 = grid[r - 2][c]?.animal,
                       a1 == a2 {
                        forbidden.insert(a1)
                    }
                    let candidates = activePool.filter { !forbidden.contains($0) }
                    let picked = candidates.randomElement() ?? activePool[0]
                    let pos = GridPos(row: r, col: c)
                    grid[r][c] = TileCell(
                        id: nextTileId(),
                        row: r,
                        col: c,
                        animal: picked,
                        special: .normal,
                        hasIce: icePositions.contains(pos)
                    )
                }
            }
            let board = grid.map { row in row.compactMap { $0 } }
            if hasAnyValidMove(board) {
                return board
            }
        }
        return generateFallbackBoard(animalPool: activePool, icePositions: icePositions, gridSize: gridSize)
    }

    private func generateFallbackBoard(
        animalPool: [AnimalType],
        icePositions: Set<GridPos>,
        gridSize: Int
    ) -> [[TileCell]] {
        (0..<gridSize).map { r in
            (0..<gridSize).map { c in
                let animal = animalPool[(r * 2 + c) % animalPool.count]
                let pos = GridPos(row: r, col: c)
                return TileCell(
                    id: nextTileId(),
                    row: r,
                    col: c,
                    animal: animal,
                    hasIce: icePositions.contains(pos)
                )
            }
        }
    }

    func detonateSpecial(
        currentBoard: [[TileCell]],
        targetPos: GridPos,
        animalPool: [AnimalType] = AnimalType.allCases
    ) -> SwapResolution {
        let n = currentBoard.count
        guard isInside(targetPos, n: n) else {
            return SwapResolution(isValidMove: false, swappedBoard: currentBoard, cascadeSteps: [], finalBoard: currentBoard)
        }
        let targetCell = currentBoard[targetPos.row][targetPos.col]
        guard targetCell.special != .normal else {
            return SwapResolution(isValidMove: false, swappedBoard: currentBoard, cascadeSteps: [], finalBoard: currentBoard)
        }

        var initialTargets = Set<GridPos>()
        if targetCell.special == .rainbowMirror {
            initialTargets.insert(targetPos)
            for r in 0..<n {
                for c in 0..<n where currentBoard[r][c].animal == targetCell.animal {
                    initialTargets.insert(GridPos(row: r, col: c))
                }
            }
        } else {
            initialTargets.insert(targetPos)
        }

        let allCleared = expandSpecialExplosions(board: currentBoard, initialCleared: initialTargets)
        let animalCounts = countClearedAnimals(board: currentBoard, clearedPositions: allCleared)
        let brokenIce = allCleared.filter { currentBoard[$0.row][$0.col].hasIce }.count

        let afterFirstRefill = applyGravityAndRefill(
            board: currentBoard,
            clearedPositions: allCleared,
            spawnedSpecials: [],
            animalPool: animalPool
        )
        let firstScore = calculateStepScore(clearedCount: allCleared.count, spawnedCount: 0, stepIndex: 1)

        var steps: [CascadeStep] = [
            CascadeStep(
                stepIndex: 1,
                clearedPositions: allCleared,
                clearedAnimalCounts: animalCounts,
                brokenIceCount: brokenIce,
                spawnedSpecials: [],
                boardAfterGravityAndRefill: afterFirstRefill,
                baseScoreEarned: firstScore
            )
        ]

        let subsequent = resolveCascades(
            initialBoard: afterFirstRefill,
            swapOriginA: nil,
            swapOriginB: nil,
            startingStepIndex: 2,
            animalPool: animalPool
        )
        steps.append(contentsOf: subsequent)

        var finalBoard = steps.last?.boardAfterGravityAndRefill ?? currentBoard
        if !hasAnyValidMove(finalBoard) {
            let remainingIce = extractIcePositions(finalBoard)
            finalBoard = generateInitialBoard(animalPool: animalPool, icePositions: remainingIce, gridSize: n)
        }

        return SwapResolution(isValidMove: true, swappedBoard: currentBoard, cascadeSteps: steps, finalBoard: finalBoard)
    }

    func attemptSwap(
        currentBoard: [[TileCell]],
        from: GridPos,
        to: GridPos,
        animalPool: [AnimalType] = AnimalType.allCases
    ) -> SwapResolution {
        let n = currentBoard.count
        guard isInside(from, n: n), isInside(to, n: n), from.isAdjacent(to: to) else {
            return SwapResolution(isValidMove: false, swappedBoard: currentBoard, cascadeSteps: [], finalBoard: currentBoard)
        }

        let swapped = swapCells(currentBoard, posA: from, posB: to)
        let cellA = swapped[from.row][from.col]
        let cellB = swapped[to.row][to.col]

        if cellA.special == .rainbowMirror || cellB.special == .rainbowMirror {
            let steps = resolveRainbowSwapAndCascades(swappedBoard: swapped, posA: from, posB: to, animalPool: animalPool)
            var finalBoard = steps.last?.boardAfterGravityAndRefill ?? swapped
            if !hasAnyValidMove(finalBoard) {
                finalBoard = generateInitialBoard(animalPool: animalPool, icePositions: extractIcePositions(finalBoard), gridSize: n)
            }
            return SwapResolution(isValidMove: true, swappedBoard: swapped, cascadeSteps: steps, finalBoard: finalBoard)
        }

        let initialClusters = findConnectedMatchClusters(swapped)
        if initialClusters.isEmpty {
            return SwapResolution(isValidMove: false, swappedBoard: swapped, cascadeSteps: [], finalBoard: currentBoard)
        }

        let steps = resolveCascades(
            initialBoard: swapped,
            swapOriginA: from,
            swapOriginB: to,
            startingStepIndex: 1,
            animalPool: animalPool
        )
        var finalBoard = steps.last?.boardAfterGravityAndRefill ?? swapped
        if !hasAnyValidMove(finalBoard) {
            finalBoard = generateInitialBoard(animalPool: animalPool, icePositions: extractIcePositions(finalBoard), gridSize: n)
        }

        return SwapResolution(isValidMove: true, swappedBoard: swapped, cascadeSteps: steps, finalBoard: finalBoard)
    }

    private func extractIcePositions(_ board: [[TileCell]]) -> Set<GridPos> {
        var set = Set<GridPos>()
        for r in 0..<board.count {
            for c in 0..<board[r].count where board[r][c].hasIce {
                set.insert(GridPos(row: r, col: c))
            }
        }
        return set
    }

    private func resolveRainbowSwapAndCascades(
        swappedBoard: [[TileCell]],
        posA: GridPos,
        posB: GridPos,
        animalPool: [AnimalType]
    ) -> [CascadeStep] {
        let n = swappedBoard.count
        let cellA = swappedBoard[posA.row][posA.col]
        let cellB = swappedBoard[posB.row][posB.col]

        var initialTargets: Set<GridPos> = [posA, posB]
        if cellA.special == .rainbowMirror && cellB.special == .rainbowMirror {
            for r in 0..<n {
                for c in 0..<n {
                    initialTargets.insert(GridPos(row: r, col: c))
                }
            }
        } else {
            let targetAnimal = cellA.special == .rainbowMirror ? cellB.animal : cellA.animal
            for r in 0..<n {
                for c in 0..<n where swappedBoard[r][c].animal == targetAnimal {
                    initialTargets.insert(GridPos(row: r, col: c))
                }
            }
        }

        let allCleared = expandSpecialExplosions(board: swappedBoard, initialCleared: initialTargets)
        let animalCounts = countClearedAnimals(board: swappedBoard, clearedPositions: allCleared)
        let brokenIce = allCleared.filter { swappedBoard[$0.row][$0.col].hasIce }.count

        let afterFirstRefill = applyGravityAndRefill(
            board: swappedBoard,
            clearedPositions: allCleared,
            spawnedSpecials: [],
            animalPool: animalPool
        )
        let firstScore = calculateStepScore(clearedCount: allCleared.count, spawnedCount: 0, stepIndex: 1)

        var steps: [CascadeStep] = [
            CascadeStep(
                stepIndex: 1,
                clearedPositions: allCleared,
                clearedAnimalCounts: animalCounts,
                brokenIceCount: brokenIce,
                spawnedSpecials: [],
                boardAfterGravityAndRefill: afterFirstRefill,
                baseScoreEarned: firstScore
            )
        ]

        steps.append(contentsOf: resolveCascades(
            initialBoard: afterFirstRefill,
            swapOriginA: nil,
            swapOriginB: nil,
            startingStepIndex: 2,
            animalPool: animalPool
        ))
        return steps
    }

    func resolveCascades(
        initialBoard: [[TileCell]],
        swapOriginA: GridPos? = nil,
        swapOriginB: GridPos? = nil,
        startingStepIndex: Int = 1,
        animalPool: [AnimalType] = AnimalType.allCases
    ) -> [CascadeStep] {
        var steps: [CascadeStep] = []
        var workingBoard = initialBoard
        var stepIdx = startingStepIndex

        while true {
            let clusters = findConnectedMatchClusters(workingBoard)
            if clusters.isEmpty { break }

            var baseMatched = Set<GridPos>()
            var spawnedSpecials: [SpawnedSpecial] = []

            for cluster in clusters {
                baseMatched.formUnion(cluster.positions)
                let specialType = cluster.determineSpecialType()
                if specialType != .normal {
                    let anchor: GridPos
                    if stepIdx == 1, let a = swapOriginA, let b = swapOriginB {
                        if cluster.positions.contains(b) {
                            anchor = b
                        } else if cluster.positions.contains(a) {
                            anchor = a
                        } else {
                            anchor = cluster.anchorPosition()
                        }
                    } else {
                        anchor = cluster.anchorPosition()
                    }
                    spawnedSpecials.append(SpawnedSpecial(pos: anchor, animal: cluster.animal, specialType: specialType))
                }
            }

            let allCleared = expandSpecialExplosions(board: workingBoard, initialCleared: baseMatched)
            let animalCounts = countClearedAnimals(board: workingBoard, clearedPositions: allCleared)
            let brokenIce = allCleared.filter { workingBoard[$0.row][$0.col].hasIce }.count

            let nextBoard = applyGravityAndRefill(
                board: workingBoard,
                clearedPositions: allCleared,
                spawnedSpecials: spawnedSpecials,
                animalPool: animalPool
            )
            let stepScore = calculateStepScore(clearedCount: allCleared.count, spawnedCount: spawnedSpecials.count, stepIndex: stepIdx)

            steps.append(
                CascadeStep(
                    stepIndex: stepIdx,
                    clearedPositions: allCleared,
                    clearedAnimalCounts: animalCounts,
                    brokenIceCount: brokenIce,
                    spawnedSpecials: spawnedSpecials,
                    boardAfterGravityAndRefill: nextBoard,
                    baseScoreEarned: stepScore
                )
            )
            workingBoard = nextBoard
            stepIdx += 1
            if stepIdx > 30 { break }
        }

        return steps
    }

    private func countClearedAnimals(board: [[TileCell]], clearedPositions: Set<GridPos>) -> [AnimalType: Int] {
        var counts: [AnimalType: Int] = [:]
        for pos in clearedPositions {
            let animal = board[pos.row][pos.col].animal
            counts[animal, default: 0] += 1
        }
        return counts
    }

    func expandSpecialExplosions(board: [[TileCell]], initialCleared: Set<GridPos>) -> Set<GridPos> {
        let n = board.count
        var result = initialCleared
        var queue: [GridPos] = initialCleared.filter { board[$0.row][$0.col].special != .normal }
        var triggered = Set<GridPos>()

        while !queue.isEmpty {
            let current = queue.removeFirst()
            if !triggered.insert(current).inserted { continue }

            let cell = board[current.row][current.col]
            var affected: [GridPos] = []

            switch cell.special {
            case .lineBombH:
                for c in 0..<n { affected.append(GridPos(row: current.row, col: c)) }
            case .lineBombV:
                for r in 0..<n { affected.append(GridPos(row: r, col: current.col)) }
            case .areaBomb:
                for dr in -1...1 {
                    for dc in -1...1 {
                        let np = GridPos(row: current.row + dr, col: current.col + dc)
                        if isInside(np, n: n) { affected.append(np) }
                    }
                }
            case .rainbowMirror:
                for r in 0..<n {
                    for c in 0..<n where board[r][c].animal == cell.animal {
                        affected.append(GridPos(row: r, col: c))
                    }
                }
            case .normal:
                break
            }

            for pos in affected where result.insert(pos).inserted {
                if board[pos.row][pos.col].special != .normal {
                    queue.append(pos)
                }
            }
        }
        return result
    }

    func applyGravityAndRefill(
        board: [[TileCell]],
        clearedPositions: Set<GridPos>,
        spawnedSpecials: [SpawnedSpecial],
        animalPool: [AnimalType] = AnimalType.allCases
    ) -> [[TileCell]] {
        let n = board.count
        let activePool = animalPool.count >= 3 ? animalPool : AnimalType.allCases
        let spawnedByPos = Dictionary(uniqueKeysWithValues: spawnedSpecials.map { ($0.pos, $0) })
        var newGrid: [[TileCell?]] = Array(repeating: Array(repeating: nil, count: n), count: n)

        for c in 0..<n {
            var surviving: [TileCell] = []
            for r in stride(from: n - 1, through: 0, by: -1) {
                let pos = GridPos(row: r, col: c)
                if let spawned = spawnedByPos[pos] {
                    surviving.append(
                        TileCell(id: nextTileId(), row: r, col: c, animal: spawned.animal, special: spawned.specialType)
                    )
                } else if !clearedPositions.contains(pos) {
                    surviving.append(board[r][c])
                }
            }

            for idx in 0..<n {
                let targetRow = n - 1 - idx
                let targetPos = GridPos(row: targetRow, col: c)
                let iceRemains = board[targetRow][c].hasIce && !clearedPositions.contains(targetPos)

                if idx < surviving.count {
                    var cell = surviving[idx]
                    cell.row = targetRow
                    cell.col = c
                    cell.isMatched = false
                    cell.hasIce = iceRemains
                    newGrid[targetRow][c] = cell
                } else {
                    let picked = activePool.randomElement() ?? activePool[0]
                    newGrid[targetRow][c] = TileCell(
                        id: nextTileId(),
                        row: targetRow,
                        col: c,
                        animal: picked,
                        special: .normal,
                        hasIce: iceRemains
                    )
                }
            }
        }
        return newGrid.map { $0.compactMap { $0 } }
    }

    private struct MatchRun {
        let animal: AnimalType
        let positions: Set<GridPos>
        let isHorizontal: Bool
        var length: Int { positions.count }
    }

    private struct MatchCluster {
        let animal: AnimalType
        let positions: Set<GridPos>
        let runs: [MatchRun]

        func determineSpecialType() -> SpecialBlockType {
            let maxStraight = runs.map(\.length).max() ?? 0
            if maxStraight >= 5 { return .rainbowMirror }
            let hasH = runs.contains { $0.isHorizontal }
            let hasV = runs.contains { !$0.isHorizontal }
            if hasH && hasV && positions.count >= 5 { return .areaBomb }
            if let fourRun = runs.first(where: { $0.length == 4 }) {
                return fourRun.isHorizontal ? .lineBombH : .lineBombV
            }
            return .normal
        }

        func anchorPosition() -> GridPos {
            if runs.count >= 2 {
                let hPos = Set(runs.filter { $0.isHorizontal }.flatMap { $0.positions })
                let vPos = Set(runs.filter { !$0.isHorizontal }.flatMap { $0.positions })
                if let intersection = hPos.intersection(vPos).first {
                    return intersection
                }
            }
            let sorted = positions.sorted { ($0.row, $0.col) < ($1.row, $1.col) }
            return sorted[sorted.count / 2]
        }
    }

    private func findConnectedMatchClusters(_ board: [[TileCell]]) -> [MatchCluster] {
        let n = board.count
        var runs: [MatchRun] = []

        for r in 0..<n {
            var c = 0
            while c < n {
                let animal = board[r][c].animal
                var end = c + 1
                while end < n && board[r][end].animal == animal { end += 1 }
                if end - c >= 3 {
                    let cells = Set((c..<end).map { GridPos(row: r, col: $0) })
                    runs.append(MatchRun(animal: animal, positions: cells, isHorizontal: true))
                }
                c = end
            }
        }

        for c in 0..<n {
            var r = 0
            while r < n {
                let animal = board[r][c].animal
                var end = r + 1
                while end < n && board[end][c].animal == animal { end += 1 }
                if end - r >= 3 {
                    let cells = Set((r..<end).map { GridPos(row: $0, col: c) })
                    runs.append(MatchRun(animal: animal, positions: cells, isHorizontal: false))
                }
                r = end
            }
        }

        guard !runs.isEmpty else { return [] }
        var visited = Array(repeating: false, count: runs.count)
        var clusters: [MatchCluster] = []

        for i in 0..<runs.count where !visited[i] {
            visited[i] = true
            let seed = runs[i]
            var mergedRuns = [seed]
            var mergedPositions = seed.positions

            var changed = true
            while changed {
                changed = false
                for j in 0..<runs.count where !visited[j] {
                    let candidate = runs[j]
                    if candidate.animal == seed.animal && !candidate.positions.isDisjoint(with: mergedPositions) {
                        visited[j] = true
                        mergedRuns.append(candidate)
                        mergedPositions.formUnion(candidate.positions)
                        changed = true
                    }
                }
            }
            clusters.append(MatchCluster(animal: seed.animal, positions: mergedPositions, runs: mergedRuns))
        }
        return clusters
    }

    func findHintMove(_ board: [[TileCell]]) -> (GridPos, GridPos)? {
        let n = board.count
        for r in 0..<n {
            for c in 0..<n {
                let current = GridPos(row: r, col: c)
                if c + 1 < n {
                    let right = GridPos(row: r, col: c + 1)
                    if !findConnectedMatchClusters(swapCells(board, posA: current, posB: right)).isEmpty {
                        return (current, right)
                    }
                }
                if r + 1 < n {
                    let down = GridPos(row: r + 1, col: c)
                    if !findConnectedMatchClusters(swapCells(board, posA: current, posB: down)).isEmpty {
                        return (current, down)
                    }
                }
            }
        }
        return nil
    }

    func hasAnyValidMove(_ board: [[TileCell]]) -> Bool {
        let n = board.count
        for r in 0..<n {
            for c in 0..<n {
                if board[r][c].special == .rainbowMirror { return true }
                let current = GridPos(row: r, col: c)
                if c + 1 < n {
                    let right = GridPos(row: r, col: c + 1)
                    if !findConnectedMatchClusters(swapCells(board, posA: current, posB: right)).isEmpty { return true }
                }
                if r + 1 < n {
                    let down = GridPos(row: r + 1, col: c)
                    if !findConnectedMatchClusters(swapCells(board, posA: current, posB: down)).isEmpty { return true }
                }
            }
        }
        return false
    }

    func swapCells(_ board: [[TileCell]], posA: GridPos, posB: GridPos) -> [[TileCell]] {
        var mutable = board
        let cellA = mutable[posA.row][posA.col]
        let cellB = mutable[posB.row][posB.col]

        var nextA = cellB
        nextA.row = posA.row
        nextA.col = posA.col
        nextA.hasIce = cellA.hasIce

        var nextB = cellA
        nextB.row = posB.row
        nextB.col = posB.col
        nextB.hasIce = cellB.hasIce

        mutable[posA.row][posA.col] = nextA
        mutable[posB.row][posB.col] = nextB
        return mutable
    }

    private func calculateStepScore(clearedCount: Int, spawnedCount: Int, stepIndex: Int) -> Int {
        ((clearedCount * 50) + (spawnedCount * 150)) * stepIndex
    }

    private func isInside(_ pos: GridPos, n: Int) -> Bool {
        pos.row >= 0 && pos.row < n && pos.col >= 0 && pos.col < n
    }
}
