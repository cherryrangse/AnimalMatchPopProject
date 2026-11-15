package com.pixelvector.animalmatch.domain.engine

import com.pixelvector.animalmatch.domain.model.AnimalType
import com.pixelvector.animalmatch.domain.model.GridPos
import com.pixelvector.animalmatch.domain.model.SpecialBlockType
import com.pixelvector.animalmatch.domain.model.TileCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class Match3EngineTest {

    private val engine = Match3Engine(Random(42))

    /**
     * 초기 매치가 전혀 없는 베이스라인 8x8 보드 생성 헬퍼
     */
    private fun createCleanBaselineBoard(): MutableList<MutableList<TileCell>> {
        val animals = AnimalType.entries
        var id = 1L
        return MutableList(8) { r ->
            MutableList(8) { c ->
                // (r * 2 + c) % 6 패턴은 가로/세로 연속 2개조차 나오지 않는 무결성 패턴
                val animal = animals[(r * 2 + c) % animals.size]
                TileCell(
                    id = id++,
                    row = r,
                    col = c,
                    animal = animal,
                    special = SpecialBlockType.NORMAL
                )
            }
        }
    }

    @Test
    fun `initial board has 8x8 cells, zero pre-existing matches, and at least one valid move`() {
        val board = engine.generateInitialBoard()
        assertEquals(8, board.size)
        board.forEach { row -> assertEquals(8, row.size) }

        val preExistingMatches = engine.findConnectedMatchClusters(board)
        assertTrue("초기 보드에는 즉시 터지는 3매치가 없어야 함", preExistingMatches.isEmpty())
        assertTrue("초기 보드에는 최소 1개 이상의 유효 스왑이 보장되어야 함", engine.hasAnyValidMove(board))
    }

    @Test
    fun `invalid swap with no match returns isValidMove false and rolls back to original board`() {
        val board = createCleanBaselineBoard().map { it.toList() }
        val resolution = engine.attemptSwap(board, GridPos(0, 0), GridPos(0, 1))

        assertFalse("매치가 성립하지 않는 스왑은 무효 처리되어야 함", resolution.isValidMove)
        assertTrue(resolution.cascadeSteps.isEmpty())
        assertEquals("롤백 후 최종 보드는 원본 보드와 동일해야 함", board, resolution.finalBoard)
    }

    @Test
    fun `horizontal 4-match spawns LINE_BOMB_H at swapped position`() {
        val grid = createCleanBaselineBoard()
        // row 3: (3,0), (3,1), (3,3)에 BUNNY 배치 후 (2,2)의 BUNNY를 (3,2)와 스왑 -> 가로 4매치
        grid[3][0] = grid[3][0].copy(animal = AnimalType.BUNNY)
        grid[3][1] = grid[3][1].copy(animal = AnimalType.BUNNY)
        grid[3][2] = grid[3][2].copy(animal = AnimalType.FROG)
        grid[3][3] = grid[3][3].copy(animal = AnimalType.BUNNY)
        grid[2][2] = grid[2][2].copy(animal = AnimalType.BUNNY)

        val resolution = engine.attemptSwap(grid.map { it.toList() }, GridPos(2, 2), GridPos(3, 2))
        assertTrue(resolution.isValidMove)
        assertTrue(resolution.cascadeSteps.isNotEmpty())

        val firstStep = resolution.cascadeSteps.first()
        val lineBombH = firstStep.spawnedSpecials.find { it.specialType == SpecialBlockType.LINE_BOMB_H }
        assertTrue("가로 4매치 시 LINE_BOMB_H 특수 블록이 생성되어야 함", lineBombH != null)
        assertEquals(GridPos(3, 2), lineBombH?.pos)
    }

    @Test
    fun `vertical 4-match spawns LINE_BOMB_V`() {
        val grid = createCleanBaselineBoard()
        // col 4: (0,4)와 (5,4)가 PENGUIN이 아니도록 명시 설정 후 (1,4)~(4,4) 세로 4매치 구성
        grid[0][4] = grid[0][4].copy(animal = AnimalType.BUNNY)
        grid[5][4] = grid[5][4].copy(animal = AnimalType.BUNNY)
        grid[1][4] = grid[1][4].copy(animal = AnimalType.PENGUIN)
        grid[2][4] = grid[2][4].copy(animal = AnimalType.PENGUIN)
        grid[3][4] = grid[3][4].copy(animal = AnimalType.CAT)
        grid[4][4] = grid[4][4].copy(animal = AnimalType.PENGUIN)
        grid[3][5] = grid[3][5].copy(animal = AnimalType.PENGUIN)

        val resolution = engine.attemptSwap(grid.map { it.toList() }, GridPos(3, 5), GridPos(3, 4))
        assertTrue(resolution.isValidMove)

        val firstStep = resolution.cascadeSteps.first()
        val lineBombV = firstStep.spawnedSpecials.find { it.specialType == SpecialBlockType.LINE_BOMB_V }
        assertTrue("세로 4매치 시 LINE_BOMB_V 특수 블록이 생성되어야 함", lineBombV != null)
    }

    @Test
    fun `T or L shape 5-match spawns AREA_BOMB`() {
        val grid = createCleanBaselineBoard()
        // L자 매치 구성: 가로 (5,2), (5,3), (5,4) + 세로 (3,2), (4,2), (5,2) -> (5,2)를 스왑으로 완성
        grid[5][3] = grid[5][3].copy(animal = AnimalType.BEAR)
        grid[5][4] = grid[5][4].copy(animal = AnimalType.BEAR)
        grid[3][2] = grid[3][2].copy(animal = AnimalType.BEAR)
        grid[4][2] = grid[4][2].copy(animal = AnimalType.BEAR)
        grid[5][2] = grid[5][2].copy(animal = AnimalType.CAT)
        grid[5][1] = grid[5][1].copy(animal = AnimalType.BEAR)

        val resolution = engine.attemptSwap(grid.map { it.toList() }, GridPos(5, 1), GridPos(5, 2))
        assertTrue(resolution.isValidMove)

        val firstStep = resolution.cascadeSteps.first()
        val areaBomb = firstStep.spawnedSpecials.find { it.specialType == SpecialBlockType.AREA_BOMB }
        assertTrue("T/L자 5매치 시 AREA_BOMB 특수 블록이 생성되어야 함", areaBomb != null)
    }

    @Test
    fun `straight 5-match spawns RAINBOW_MIRROR and swapping it clears all matching animals`() {
        val grid = createCleanBaselineBoard()
        // 가로 일렬 5개 매치: (6,1), (6,2), (6,4), (6,5) + (5,3)을 (6,3)으로 스왑
        grid[6][1] = grid[6][1].copy(animal = AnimalType.PIG)
        grid[6][2] = grid[6][2].copy(animal = AnimalType.PIG)
        grid[6][3] = grid[6][3].copy(animal = AnimalType.FROG)
        grid[6][4] = grid[6][4].copy(animal = AnimalType.PIG)
        grid[6][5] = grid[6][5].copy(animal = AnimalType.PIG)
        grid[5][3] = grid[5][3].copy(animal = AnimalType.PIG)

        val resolution = engine.attemptSwap(grid.map { it.toList() }, GridPos(5, 3), GridPos(6, 3))
        assertTrue(resolution.isValidMove)

        val firstStep = resolution.cascadeSteps.first()
        val rainbow = firstStep.spawnedSpecials.find { it.specialType == SpecialBlockType.RAINBOW_MIRROR }
        assertTrue("일렬 5매치 시 RAINBOW_MIRROR 특수 블록이 생성되어야 함", rainbow != null)

        // RAINBOW_MIRROR와 인접 타일 스왑 시 해당 동물 전체 소거 검증
        val boardWithRainbow = createCleanBaselineBoard()
        boardWithRainbow[4][4] = boardWithRainbow[4][4].copy(
            animal = AnimalType.PIG,
            special = SpecialBlockType.RAINBOW_MIRROR
        )
        val targetAnimal = boardWithRainbow[4][5].animal
        val totalTargetCount = boardWithRainbow.flatten().count { it.animal == targetAnimal }

        val rainbowSwapRes = engine.attemptSwap(
            boardWithRainbow.map { it.toList() },
            GridPos(4, 4),
            GridPos(4, 5)
        )
        assertTrue(rainbowSwapRes.isValidMove)
        val clearedInStep1 = rainbowSwapRes.cascadeSteps.first().clearedPositions
        assertTrue(
            "레인보우 미러와 스왑한 동물의 모든 타일($totalTargetCount 개)이 소거 대상에 포함되어야 함",
            clearedInStep1.size >= totalTargetCount + 1
        )
    }

    @Test
    fun `special block explosions expand to full row, column, and 3x3 area`() {
        val grid = createCleanBaselineBoard()
        grid[2][3] = grid[2][3].copy(special = SpecialBlockType.LINE_BOMB_H)
        grid[4][4] = grid[4][4].copy(special = SpecialBlockType.AREA_BOMB)

        val expandedH = engine.expandSpecialExplosions(
            board = grid.map { it.toList() },
            initialCleared = setOf(GridPos(2, 3))
        )
        for (c in 0 until 8) {
            assertTrue("LINE_BOMB_H 폭발 시 2행 전체가 소거되어야 함", GridPos(2, c) in expandedH)
        }

        val expandedArea = engine.expandSpecialExplosions(
            board = grid.map { it.toList() },
            initialCleared = setOf(GridPos(4, 4))
        )
        assertEquals("AREA_BOMB 폭발 시 3x3(9칸)이 소거되어야 함", 9, expandedArea.size)
    }

    /**
     * Easy 모드: 6x6 보드 + 동물 4종으로 생성 시 크기/초기 무매치/유효 이동/힌트가 보장되는지 검증
     */
    @Test
    fun `easy mode 6x6 board is valid and has a hint`() {
        val pool = AnimalType.entries.take(4)
        repeat(20) {
            val board = engine.generateInitialBoard(pool, emptySet(), gridSize = 6)
            assertEquals(6, board.size)
            assertTrue(board.all { it.size == 6 })
            assertTrue(engine.findConnectedMatchClusters(board).isEmpty())
            assertTrue(engine.hasAnyValidMove(board))

            val hint = engine.findHintMove(board)
            assertTrue(hint != null)
            val resolution = engine.attemptSwap(board, hint!!.first, hint.second, pool)
            assertTrue(resolution.isValidMove)
            assertEquals(6, resolution.finalBoard.size)
            assertTrue(resolution.finalBoard.all { row -> row.size == 6 })
        }
    }
}