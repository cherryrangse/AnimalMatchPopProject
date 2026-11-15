package com.pixelvector.animalmatch.domain.engine

import com.pixelvector.animalmatch.domain.model.AnimalType
import com.pixelvector.animalmatch.domain.model.GridPos
import com.pixelvector.animalmatch.domain.model.SpecialBlockType
import com.pixelvector.animalmatch.domain.model.TileCell
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Animal Match Pop - 8x8 ?뺥넻 Match-3 肄붿뼱 ?붿쭊 (?쒖닔 Kotlin, Android SDK 臾댁쓽議?
 */
class Match3Engine(
    private val random: Random = Random.Default
) {
    private val tileIdGenerator = AtomicLong(1L)

    fun nextTileId(): Long = tileIdGenerator.getAndIncrement()

    /**
     * 珥덇린 3留ㅼ튂媛 ?녾퀬 理쒖냼 1媛??댁긽???좏슚 ?ㅼ솑??媛?ν븳 8x8 蹂대뱶 ?앹꽦
     */
    fun generateInitialBoard(
        animalPool: List<AnimalType> = AnimalType.entries,
        icePositions: Set<GridPos> = emptySet(),
        gridSize: Int = GRID_SIZE
    ): List<List<TileCell>> {
        val activePool = if (animalPool.size >= 3) animalPool else AnimalType.entries
        var attempts = 0
        while (attempts < 20) {
            attempts++
            val grid = Array(gridSize) { arrayOfNulls<TileCell>(gridSize) }
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val forbidden = mutableSetOf<AnimalType>()
                    if (c >= 2 && grid[r][c - 1]?.animal == grid[r][c - 2]?.animal) {
                        grid[r][c - 1]?.animal?.let { forbidden.add(it) }
                    }
                    if (r >= 2 && grid[r - 1][c]?.animal == grid[r - 2][c]?.animal) {
                        grid[r - 1][c]?.animal?.let { forbidden.add(it) }
                    }
                    val candidates = activePool.filterNot { it in forbidden }
                    val picked = candidates[random.nextInt(candidates.size)]
                    val pos = GridPos(r, c)
                    grid[r][c] = TileCell(
                        id = nextTileId(),
                        row = r,
                        col = c,
                        animal = picked,
                        special = SpecialBlockType.NORMAL,
                        hasIce = pos in icePositions
                    )
                }
            }
            val board = grid.map { row -> row.filterNotNull() }
            if (hasAnyValidMove(board)) {
                return board
            }
        }
        return generateFallbackBoard(activePool, icePositions, gridSize)
    }

    private fun generateFallbackBoard(
        animalPool: List<AnimalType>,
        icePositions: Set<GridPos>,
        gridSize: Int
    ): List<List<TileCell>> {
        return Array(gridSize) { r ->
            Array(gridSize) { c ->
                val animal = animalPool[(r * 2 + c) % animalPool.size]
                val pos = GridPos(r, c)
                TileCell(
                    id = nextTileId(),
                    row = r,
                    col = c,
                    animal = animal,
                    hasIce = pos in icePositions
                )
            }.toList()
        }.toList()
    }

    
    /**
     * 특수 블록 단독 터치 격발(Tap-to-Detonate) 처리
     */
    fun detonateSpecial(
        currentBoard: List<List<TileCell>>,
        targetPos: GridPos,
        animalPool: List<AnimalType> = AnimalType.entries
    ): SwapResolution {
        val n = currentBoard.size
        if (!isInside(targetPos, n)) {
            return SwapResolution(false, currentBoard, emptyList(), currentBoard)
        }
        val targetCell = currentBoard[targetPos.row][targetPos.col]
        if (targetCell.special == SpecialBlockType.NORMAL) {
            return SwapResolution(false, currentBoard, emptyList(), currentBoard)
        }

        val initialTargets = if (targetCell.special == SpecialBlockType.RAINBOW_MIRROR) {
            val allSame = mutableSetOf(targetPos)
            for (r in 0 until n) {
                for (c in 0 until n) {
                    if (currentBoard[r][c].animal == targetCell.animal) {
                        allSame.add(GridPos(r, c))
                    }
                }
            }
            allSame
        } else {
            setOf(targetPos)
        }

        val allCleared = expandSpecialExplosions(currentBoard, initialTargets)
        val animalCounts = countClearedAnimals(currentBoard, allCleared)
        val brokenIce = allCleared.count { currentBoard[it.row][it.col].hasIce }

        val afterFirstRefill = applyGravityAndRefill(
            board = currentBoard,
            clearedPositions = allCleared,
            spawnedSpecials = emptyList(),
            animalPool = animalPool
        )
        val firstScore = calculateStepScore(
            clearedCount = allCleared.size,
            spawnedCount = 0,
            stepIndex = 1
        )

        val steps = mutableListOf<CascadeStep>()
        steps.add(
            CascadeStep(
                stepIndex = 1,
                clearedPositions = allCleared,
                clearedAnimalCounts = animalCounts,
                brokenIceCount = brokenIce,
                spawnedSpecials = emptyList(),
                boardAfterGravityAndRefill = afterFirstRefill,
                baseScoreEarned = firstScore
            )
        )

        val subsequentSteps = resolveCascades(
            initialBoard = afterFirstRefill,
            swapOriginA = null,
            swapOriginB = null,
            startingStepIndex = 2,
            animalPool = animalPool
        )
        steps.addAll(subsequentSteps)

        var finalBoard = steps.lastOrNull()?.boardAfterGravityAndRefill ?: currentBoard
        if (!hasAnyValidMove(finalBoard)) {
            val remainingIce = extractIcePositions(finalBoard)
            finalBoard = generateInitialBoard(animalPool, remainingIce, n)
        }

        return SwapResolution(
            isValidMove = true,
            swappedBoard = currentBoard,
            cascadeSteps = steps,
            finalBoard = finalBoard
        )
    }

/**
     * ??醫뚰몴???몄젒 ????ㅼ솑 ?쒕룄 諛??곗뇙(Cascade) ?먯젙 ?섑뻾
     */
    fun attemptSwap(
        currentBoard: List<List<TileCell>>,
        from: GridPos,
        to: GridPos,
        animalPool: List<AnimalType> = AnimalType.entries
    ): SwapResolution {
        val n = currentBoard.size
        if (!isInside(from, n) || !isInside(to, n) || !from.isAdjacentTo(to)) {
            return SwapResolution(
                isValidMove = false,
                swappedBoard = currentBoard,
                cascadeSteps = emptyList(),
                finalBoard = currentBoard
            )
        }

        val swapped = swapCells(currentBoard, from, to)
        val cellA = swapped[from.row][from.col]
        val cellB = swapped[to.row][to.col]

        // 1. RAINBOW_MIRROR ?ㅼ솑 ?뱀닔 洹쒖튃 ?뺤씤
        val isRainbowSwap =
            cellA.special == SpecialBlockType.RAINBOW_MIRROR ||
                cellB.special == SpecialBlockType.RAINBOW_MIRROR

        if (isRainbowSwap) {
            val steps = resolveRainbowSwapAndCascades(swapped, from, to, animalPool)
            var finalBoard = steps.lastOrNull()?.boardAfterGravityAndRefill ?: swapped
            if (!hasAnyValidMove(finalBoard)) {
                val remainingIce = extractIcePositions(finalBoard)
                finalBoard = generateInitialBoard(animalPool, remainingIce, n)
            }
            return SwapResolution(
                isValidMove = true,
                swappedBoard = swapped,
                cascadeSteps = steps,
                finalBoard = finalBoard
            )
        }

        // 2. ?쇰컲 留ㅼ튂 ?먯젙
        val initialClusters = findConnectedMatchClusters(swapped)
        if (initialClusters.isEmpty()) {
            return SwapResolution(
                isValidMove = false,
                swappedBoard = swapped,
                cascadeSteps = emptyList(),
                finalBoard = currentBoard
            )
        }

        val cascadeSteps = resolveCascades(
            initialBoard = swapped,
            swapOriginA = from,
            swapOriginB = to,
            startingStepIndex = 1,
            animalPool = animalPool
        )
        var finalBoard = cascadeSteps.lastOrNull()?.boardAfterGravityAndRefill ?: swapped
        if (!hasAnyValidMove(finalBoard)) {
            val remainingIce = extractIcePositions(finalBoard)
            finalBoard = generateInitialBoard(animalPool, remainingIce, n)
        }

        return SwapResolution(
            isValidMove = true,
            swappedBoard = swapped,
            cascadeSteps = cascadeSteps,
            finalBoard = finalBoard
        )
    }

    private fun extractIcePositions(board: List<List<TileCell>>): Set<GridPos> {
        val set = mutableSetOf<GridPos>()
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                if (board[r][c].hasIce) {
                    set.add(GridPos(r, c))
                }
            }
        }
        return set
    }

    private fun resolveRainbowSwapAndCascades(
        swappedBoard: List<List<TileCell>>,
        posA: GridPos,
        posB: GridPos,
        animalPool: List<AnimalType>
    ): List<CascadeStep> {
        val steps = mutableListOf<CascadeStep>()
        val cellA = swappedBoard[posA.row][posA.col]
        val cellB = swappedBoard[posB.row][posB.col]

        val initialTargets = mutableSetOf(posA, posB)
        if (cellA.special == SpecialBlockType.RAINBOW_MIRROR && cellB.special == SpecialBlockType.RAINBOW_MIRROR) {
            for (r in 0 until swappedBoard.size) {
                for (c in 0 until swappedBoard.size) {
                    initialTargets.add(GridPos(r, c))
                }
            }
        } else {
            val targetAnimal = if (cellA.special == SpecialBlockType.RAINBOW_MIRROR) {
                cellB.animal
            } else {
                cellA.animal
            }
            for (r in 0 until swappedBoard.size) {
                for (c in 0 until swappedBoard.size) {
                    if (swappedBoard[r][c].animal == targetAnimal) {
                        initialTargets.add(GridPos(r, c))
                    }
                }
            }
        }

        val allCleared = expandSpecialExplosions(swappedBoard, initialTargets)
        val animalCounts = countClearedAnimals(swappedBoard, allCleared)
        val brokenIce = allCleared.count { swappedBoard[it.row][it.col].hasIce }

        val afterFirstRefill = applyGravityAndRefill(
            board = swappedBoard,
            clearedPositions = allCleared,
            spawnedSpecials = emptyList(),
            animalPool = animalPool
        )
        val firstScore = calculateStepScore(
            clearedCount = allCleared.size,
            spawnedCount = 0,
            stepIndex = 1
        )
        steps.add(
            CascadeStep(
                stepIndex = 1,
                clearedPositions = allCleared,
                clearedAnimalCounts = animalCounts,
                brokenIceCount = brokenIce,
                spawnedSpecials = emptyList(),
                boardAfterGravityAndRefill = afterFirstRefill,
                baseScoreEarned = firstScore
            )
        )

        val subsequentSteps = resolveCascades(
            initialBoard = afterFirstRefill,
            swapOriginA = null,
            swapOriginB = null,
            startingStepIndex = 2,
            animalPool = animalPool
        )
        steps.addAll(subsequentSteps)
        return steps
    }

    fun resolveCascades(
        initialBoard: List<List<TileCell>>,
        swapOriginA: GridPos? = null,
        swapOriginB: GridPos? = null,
        startingStepIndex: Int = 1,
        animalPool: List<AnimalType> = AnimalType.entries
    ): List<CascadeStep> {
        val steps = mutableListOf<CascadeStep>()
        var workingBoard = initialBoard
        var stepIdx = startingStepIndex

        while (true) {
            val clusters = findConnectedMatchClusters(workingBoard)
            if (clusters.isEmpty()) break

            val baseMatchedPositions = mutableSetOf<GridPos>()
            val spawnedSpecials = mutableListOf<SpawnedSpecial>()

            for (cluster in clusters) {
                baseMatchedPositions.addAll(cluster.positions)

                val specialType = cluster.determineSpecialType()
                if (specialType != SpecialBlockType.NORMAL) {
                    val anchorPos = if (stepIdx == 1 && swapOriginA != null && swapOriginB != null) {
                        when {
                            swapOriginB in cluster.positions -> swapOriginB
                            swapOriginA in cluster.positions -> swapOriginA
                            else -> cluster.anchorPosition()
                        }
                    } else {
                        cluster.anchorPosition()
                    }
                    spawnedSpecials.add(
                        SpawnedSpecial(
                            pos = anchorPos,
                            animal = cluster.animal,
                            specialType = specialType
                        )
                    )
                }
            }

            val allClearedPositions = expandSpecialExplosions(workingBoard, baseMatchedPositions)
            val animalCounts = countClearedAnimals(workingBoard, allClearedPositions)
            val brokenIce = allClearedPositions.count { workingBoard[it.row][it.col].hasIce }

            val nextBoard = applyGravityAndRefill(
                board = workingBoard,
                clearedPositions = allClearedPositions,
                spawnedSpecials = spawnedSpecials,
                animalPool = animalPool
            )

            val stepScore = calculateStepScore(
                clearedCount = allClearedPositions.size,
                spawnedCount = spawnedSpecials.size,
                stepIndex = stepIdx
            )

            steps.add(
                CascadeStep(
                    stepIndex = stepIdx,
                    clearedPositions = allClearedPositions,
                    clearedAnimalCounts = animalCounts,
                    brokenIceCount = brokenIce,
                    spawnedSpecials = spawnedSpecials,
                    boardAfterGravityAndRefill = nextBoard,
                    baseScoreEarned = stepScore
                )
            )

            workingBoard = nextBoard
            stepIdx++

            if (stepIdx > 30) break
        }

        return steps
    }

    private fun countClearedAnimals(
        board: List<List<TileCell>>,
        clearedPositions: Set<GridPos>
    ): Map<AnimalType, Int> {
        val counts = mutableMapOf<AnimalType, Int>()
        for (pos in clearedPositions) {
            val animal = board[pos.row][pos.col].animal
            counts[animal] = (counts[animal] ?: 0) + 1
        }
        return counts
    }

    fun expandSpecialExplosions(
        board: List<List<TileCell>>,
        initialCleared: Set<GridPos>
    ): Set<GridPos> {
        val result = initialCleared.toMutableSet()
        val queue = ArrayDeque<GridPos>()
        val triggeredSpecials = mutableSetOf<GridPos>()

        initialCleared.forEach { pos ->
            if (board[pos.row][pos.col].special != SpecialBlockType.NORMAL) {
                queue.add(pos)
            }
        }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!triggeredSpecials.add(current)) continue

            val cell = board[current.row][current.col]
            val affected = mutableListOf<GridPos>()

            when (cell.special) {
                SpecialBlockType.LINE_BOMB_H -> {
                    for (c in 0 until board.size) {
                        affected.add(GridPos(current.row, c))
                    }
                }
                SpecialBlockType.LINE_BOMB_V -> {
                    for (r in 0 until board.size) {
                        affected.add(GridPos(r, current.col))
                    }
                }
                SpecialBlockType.AREA_BOMB -> {
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val nr = current.row + dr
                            val nc = current.col + dc
                            val np = GridPos(nr, nc)
                            if (isInside(np, board.size)) {
                                affected.add(np)
                            }
                        }
                    }
                }
                SpecialBlockType.RAINBOW_MIRROR -> {
                    for (r in 0 until board.size) {
                        for (c in 0 until board.size) {
                            if (board[r][c].animal == cell.animal) {
                                affected.add(GridPos(r, c))
                            }
                        }
                    }
                }
                SpecialBlockType.NORMAL -> Unit
            }

            for (pos in affected) {
                if (result.add(pos)) {
                    if (board[pos.row][pos.col].special != SpecialBlockType.NORMAL) {
                        queue.add(pos)
                    }
                }
            }
        }

        return result
    }

    /**
     * ?뚭굅???먮━瑜?鍮꾩슦怨??쇱쓬 ?곹깭 hasIce??? 醫뚰몴??怨좎젙?섏뼱 ?뚭굅??醫뚰몴留?false濡??꾪솚),
     * 媛??대쭏???꾩쓽 ??쇰뱾???꾨옒濡??먯뿰 ?숉븯?쒗궓 ???곷떒 鍮덉옄由ъ뿉 ??????앹꽦
     */
    fun applyGravityAndRefill(
        board: List<List<TileCell>>,
        clearedPositions: Set<GridPos>,
        spawnedSpecials: List<SpawnedSpecial>,
        animalPool: List<AnimalType> = AnimalType.entries
    ): List<List<TileCell>> {
        val activePool = if (animalPool.size >= 3) animalPool else AnimalType.entries
        val spawnedByPos = spawnedSpecials.associateBy { it.pos }
        val newGrid = Array(board.size) { arrayOfNulls<TileCell>(board.size) }

        for (c in 0 until board.size) {
            val survivingColumn = mutableListOf<TileCell>()

            for (r in (board.size - 1) downTo 0) {
                val pos = GridPos(r, c)
                val spawned = spawnedByPos[pos]
                if (spawned != null) {
                    survivingColumn.add(
                        TileCell(
                            id = nextTileId(),
                            row = r,
                            col = c,
                            animal = spawned.animal,
                            special = spawned.specialType
                        )
                    )
                } else if (pos !in clearedPositions) {
                    survivingColumn.add(board[r][c])
                }
            }

            for (idx in 0 until board.size) {
                val targetRow = board.size - 1 - idx
                val targetPos = GridPos(targetRow, c)
                // ?쇱쓬(hasIce)? 蹂대뱶 諛붾떏 ????щ’ ?띿꽦?대?濡??대떦 醫뚰몴媛 ?대쾲 留ㅼ튂?먯꽌 ?뚭굅?섏? ?딆븯?ㅻ㈃ ?좎?
                val iceRemains = board[targetRow][c].hasIce && (targetPos !in clearedPositions)

                val cell = if (idx < survivingColumn.size) {
                    survivingColumn[idx].copy(
                        row = targetRow,
                        col = c,
                        isMatched = false,
                        hasIce = iceRemains
                    )
                } else {
                    val randomAnimal = activePool[random.nextInt(activePool.size)]
                    TileCell(
                        id = nextTileId(),
                        row = targetRow,
                        col = c,
                        animal = randomAnimal,
                        special = SpecialBlockType.NORMAL,
                        hasIce = iceRemains
                    )
                }
                newGrid[targetRow][c] = cell
            }
        }

        return newGrid.map { row -> row.filterNotNull() }
    }

    internal fun findConnectedMatchClusters(board: List<List<TileCell>>): List<MatchCluster> {
        val horizontalRuns = mutableListOf<MatchRun>()
        val verticalRuns = mutableListOf<MatchRun>()

        for (r in 0 until board.size) {
            var c = 0
            while (c < board.size) {
                val animal = board[r][c].animal
                var end = c + 1
                while (end < board.size && board[r][end].animal == animal) {
                    end++
                }
                val length = end - c
                if (length >= 3) {
                    val cells = (c until end).map { col -> GridPos(r, col) }.toSet()
                    horizontalRuns.add(MatchRun(animal = animal, positions = cells, isHorizontal = true))
                }
                c = end
            }
        }

        for (c in 0 until board.size) {
            var r = 0
            while (r < board.size) {
                val animal = board[r][c].animal
                var end = r + 1
                while (end < board.size && board[end][c].animal == animal) {
                    end++
                }
                val length = end - r
                if (length >= 3) {
                    val cells = (r until end).map { row -> GridPos(row, c) }.toSet()
                    verticalRuns.add(MatchRun(animal = animal, positions = cells, isHorizontal = false))
                }
                r = end
            }
        }

        val allRuns = horizontalRuns + verticalRuns
        if (allRuns.isEmpty()) return emptyList()

        val visited = BooleanArray(allRuns.size)
        val clusters = mutableListOf<MatchCluster>()

        for (i in allRuns.indices) {
            if (visited[i]) continue
            visited[i] = true
            val seed = allRuns[i]
            val mergedRuns = mutableListOf(seed)
            val mergedPositions = seed.positions.toMutableSet()

            var changed = true
            while (changed) {
                changed = false
                for (j in allRuns.indices) {
                    if (visited[j]) continue
                    val candidate = allRuns[j]
                    if (candidate.animal == seed.animal &&
                        candidate.positions.any { it in mergedPositions }
                    ) {
                        visited[j] = true
                        mergedRuns.add(candidate)
                        mergedPositions.addAll(candidate.positions)
                        changed = true
                    }
                }
            }

            clusters.add(
                MatchCluster(
                    animal = seed.animal,
                    positions = mergedPositions,
                    runs = mergedRuns
                )
            )
        }

        return clusters
    }

    /**
     * ?댁? 紐⑤뱶 ?뚰듃?? 留ㅼ튂媛 ?깅┰?섎뒗 泥?踰덉㎏ ?좏슚 ?ㅼ솑 ?띿쓣 諛섑솚 (?놁쑝硫?null)
     */
    fun findHintMove(board: List<List<TileCell>>): Pair<GridPos, GridPos>? {
        val n = board.size
        for (r in 0 until n) {
            for (c in 0 until n) {
                val current = GridPos(r, c)
                if (c + 1 < n) {
                    val right = GridPos(r, c + 1)
                    if (findConnectedMatchClusters(swapCells(board, current, right)).isNotEmpty()) {
                        return current to right
                    }
                }
                if (r + 1 < n) {
                    val down = GridPos(r + 1, c)
                    if (findConnectedMatchClusters(swapCells(board, current, down)).isNotEmpty()) {
                        return current to down
                    }
                }
            }
        }
        return null
    }

    fun hasAnyValidMove(board: List<List<TileCell>>): Boolean {
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                if (board[r][c].special == SpecialBlockType.RAINBOW_MIRROR) return true
                val current = GridPos(r, c)
                if (c + 1 < board.size) {
                    val right = GridPos(r, c + 1)
                    val swapped = swapCells(board, current, right)
                    if (findConnectedMatchClusters(swapped).isNotEmpty()) return true
                }
                if (r + 1 < board.size) {
                    val down = GridPos(r + 1, c)
                    val swapped = swapCells(board, current, down)
                    if (findConnectedMatchClusters(swapped).isNotEmpty()) return true
                }
            }
        }
        return false
    }

    fun swapCells(
        board: List<List<TileCell>>,
        posA: GridPos,
        posB: GridPos
    ): List<List<TileCell>> {
        val mutable = board.map { it.toMutableList() }.toMutableList()
        val cellA = mutable[posA.row][posA.col]
        val cellB = mutable[posB.row][posB.col]

        // ????숇Ъ/?뱀닔釉붾줉留??꾩튂 援먰솚?섍퀬 ?쇱쓬(hasIce)? 蹂대뱶 醫뚰몴??怨좎젙
        mutable[posA.row][posA.col] = cellB.copy(
            row = posA.row,
            col = posA.col,
            hasIce = cellA.hasIce
        )
        mutable[posB.row][posB.col] = cellA.copy(
            row = posB.row,
            col = posB.col,
            hasIce = cellB.hasIce
        )
        return mutable.map { it.toList() }
    }

    private fun calculateStepScore(
        clearedCount: Int,
        spawnedCount: Int,
        stepIndex: Int
    ): Int {
        val base = (clearedCount * 50) + (spawnedCount * 150)
        return base * stepIndex
    }

    private fun isInside(pos: GridPos, n: Int = GRID_SIZE): Boolean =
        pos.row in 0 until n && pos.col in 0 until n

    internal data class MatchRun(
        val animal: AnimalType,
        val positions: Set<GridPos>,
        val isHorizontal: Boolean
    ) {
        val length: Int get() = positions.size
    }

    internal data class MatchCluster(
        val animal: AnimalType,
        val positions: Set<GridPos>,
        val runs: List<MatchRun>
    ) {
        fun determineSpecialType(): SpecialBlockType {
            val maxStraightRun = runs.maxOfOrNull { it.length } ?: 0
            if (maxStraightRun >= 5) {
                return SpecialBlockType.RAINBOW_MIRROR
            }

            val hasHorizontal = runs.any { it.isHorizontal }
            val hasVertical = runs.any { !it.isHorizontal }
            if (hasHorizontal && hasVertical && positions.size >= 5) {
                return SpecialBlockType.AREA_BOMB
            }

            val fourRun = runs.firstOrNull { it.length == 4 }
            if (fourRun != null) {
                return if (fourRun.isHorizontal) {
                    SpecialBlockType.LINE_BOMB_H
                } else {
                    SpecialBlockType.LINE_BOMB_V
                }
            }

            return SpecialBlockType.NORMAL
        }

        fun anchorPosition(): GridPos {
            if (runs.size >= 2) {
                val hPositions = runs.filter { it.isHorizontal }.flatMap { it.positions }.toSet()
                val vPositions = runs.filter { !it.isHorizontal }.flatMap { it.positions }.toSet()
                val intersection = hPositions.intersect(vPositions).firstOrNull()
                if (intersection != null) return intersection
            }
            val sorted = positions.sortedWith(compareBy<GridPos> { it.row }.thenBy { it.col })
            return sorted[sorted.size / 2]
        }
    }

    companion object {
        const val GRID_SIZE = 8
    }
}
