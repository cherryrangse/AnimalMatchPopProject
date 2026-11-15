package com.pixelvector.animalmatch.domain.model

import androidx.compose.runtime.Immutable

/**
 * 스테이지 목표 타입
 */
enum class StageTargetType {
    COLLECT_ANIMAL,   // 특정 동물 타일 N개 수집 (소거)
    REACH_SCORE,      // 목표 점수 달성
    BREAK_ICE         // 얼음 타일(isIce) N개 깨기
}

/**
 * 스테이지 1~20단계 개별 설정 모델
 */
@Immutable
data class StageConfig(
    val stageNumber: Int,
    val maxMoves: Int,
    val targetType: StageTargetType,
    val targetValue: Int,
    val targetAnimal: AnimalType? = null,
    val icePositions: Set<GridPos> = emptySet()
)

object StageRepository {
    const val MAX_STAGE = 20

    fun getStage(stageNumber: Int): StageConfig {
        val stage = stageNumber.coerceIn(1, MAX_STAGE)
        return when (stage) {
            in 1..5 -> {
                val targetAnimals = listOf(
                    AnimalType.BUNNY,
                    AnimalType.CAT,
                    AnimalType.PENGUIN,
                    AnimalType.BEAR,
                    AnimalType.PIG
                )
                StageConfig(
                    stageNumber = stage,
                    maxMoves = 18 - (stage - 1),
                    targetType = StageTargetType.COLLECT_ANIMAL,
                    targetValue = 10 + stage * 3,
                    targetAnimal = targetAnimals[(stage - 1) % targetAnimals.size]
                )
            }
            in 6..10 -> {
                StageConfig(
                    stageNumber = stage,
                    maxMoves = 16,
                    targetType = StageTargetType.REACH_SCORE,
                    targetValue = 1500 + (stage - 5) * 600
                )
            }
            in 11..15 -> {
                val iceCount = 4 + (stage - 11) * 2
                val iceSet = generateCenterIcePositions(iceCount)
                StageConfig(
                    stageNumber = stage,
                    maxMoves = 16,
                    targetType = StageTargetType.BREAK_ICE,
                    targetValue = iceSet.size,
                    icePositions = iceSet
                )
            }
            else -> {
                val iceCount = 8 + (stage - 15) * 2
                val iceSet = generateCenterIcePositions(iceCount)
                StageConfig(
                    stageNumber = stage,
                    maxMoves = 18,
                    targetType = StageTargetType.BREAK_ICE,
                    targetValue = iceSet.size,
                    icePositions = iceSet
                )
            }
        }
    }

    private fun generateCenterIcePositions(count: Int): Set<GridPos> {
        val candidates = mutableListOf<GridPos>()
        for (r in 2..5) {
            for (c in 1..6) {
                candidates.add(GridPos(r, c))
            }
        }
        return candidates.take(count.coerceAtMost(candidates.size)).toSet()
    }
}
