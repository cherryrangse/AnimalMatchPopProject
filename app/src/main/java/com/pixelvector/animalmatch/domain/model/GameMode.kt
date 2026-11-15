package com.pixelvector.animalmatch.domain.model

/**
 * 4대 게임 모드 정의 (Easy / Normal / Hard / Stage)
 */
enum class GameMode(val gridSize: Int) {
    EASY(6),   // 6x6 작은 그리드, 90초 타임어택, 4종 동물 (초보자용 - 매칭이 매우 쉬움)
    NORMAL(8), // 8x8 그리드, 60초 타임어택, 5종 동물 (캐주얼)
    HARD(8),   // 8x8 그리드, 45초 타임어택, 6종 동물 전체 + 획득 점수 1.5배 (고난도 도전)
    STAGE(8)   // 1~20단계 스테이지 클리어 모드 (제한 이동 횟수 Moves 내 목표 달성)
}
