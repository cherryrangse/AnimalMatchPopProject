package com.pixelvector.animalmatch.domain.engine

import com.pixelvector.animalmatch.domain.model.AnimalType
import com.pixelvector.animalmatch.domain.model.GridPos
import com.pixelvector.animalmatch.domain.model.SpecialBlockType
import com.pixelvector.animalmatch.domain.model.TileCell

/**
 * 특수 블록 생성 정보
 */
data class SpawnedSpecial(
    val pos: GridPos,
    val animal: AnimalType,
    val specialType: SpecialBlockType
)

/**
 * 단일 연쇄(Cascade Step) 결과 모델
 */
data class CascadeStep(
    val stepIndex: Int,                        // 1부터 시작하는 연쇄 단계 (콤보 증가량)
    val clearedPositions: Set<GridPos>,        // 이번 단계에서 소거(폭발 포함)된 좌표들
    val clearedAnimalCounts: Map<AnimalType, Int> = emptyMap(), // 이번 단계에서 소거된 동물별 개수
    val brokenIceCount: Int = 0,               // 이번 단계에서 깨진 얼음(hasIce) 셀 개수
    val spawnedSpecials: List<SpawnedSpecial>, // 매치 형태로 인해 새로 생성된 특수 블록들
    val boardAfterGravityAndRefill: List<List<TileCell>>, // 낙하 및 상단 리필 완료 후 8x8 보드
    val baseScoreEarned: Int                   // 이번 단계의 기본 획득 점수 (Fever 배수 적용 전)
)

/**
 * 인접 타일 스왑(Swap) 시뮬레이션 최종 결과
 */
data class SwapResolution(
    val isValidMove: Boolean,                  // 매치 또는 레인보우 스왑 성립 여부 (false면 UI에서 Rollback 연출)
    val swappedBoard: List<List<TileCell>>,    // 두 타일이 위치 교환된 직후의 보드
    val cascadeSteps: List<CascadeStep>,       // 연쇄 단계 리스트
    val finalBoard: List<List<TileCell>>       // 모든 연쇄 종료 후(데드락 시 셔플 포함) 최종 보드
)
