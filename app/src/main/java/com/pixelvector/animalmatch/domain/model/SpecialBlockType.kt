package com.pixelvector.animalmatch.domain.model

/**
 * 타일 상태 및 특수 블록 정의 (MASTER_SPEC_V2.md 2.3)
 */
enum class SpecialBlockType {
    NORMAL,          // 기본 3매치 대상
    LINE_BOMB_H,     // 4개 가로 매치 시 생성: 가로 라인 전체 소거
    LINE_BOMB_V,     // 4개 세로 매치 시 생성: 세로 라인 전체 소거
    AREA_BOMB,       // T자 또는 L자 5개 매치 시 생성: 주변 3x3 범위 폭발
    RAINBOW_MIRROR   // 일렬 5개 매치 시 생성: 인접 동물과 스왑 시 해당 동물 화면 전체 소거
}
