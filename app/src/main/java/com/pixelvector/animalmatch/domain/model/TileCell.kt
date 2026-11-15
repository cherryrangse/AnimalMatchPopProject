package com.pixelvector.animalmatch.domain.model

import androidx.compose.runtime.Immutable

/**
 * 8x8 그리드 내 단일 타일 셀 모델 (@Immutable 지정으로 Compose 불필요 리컴포지션 방지)
 */
@Immutable
data class TileCell(
    val id: Long,
    val row: Int,
    val col: Int,
    val animal: AnimalType,
    val special: SpecialBlockType = SpecialBlockType.NORMAL,
    val isMatched: Boolean = false,
    val hasIce: Boolean = false
)

@Immutable
data class GridPos(val row: Int, val col: Int) {
    fun isAdjacentTo(other: GridPos): Boolean {
        val dr = kotlin.math.abs(row - other.row)
        val dc = kotlin.math.abs(col - other.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }
}
