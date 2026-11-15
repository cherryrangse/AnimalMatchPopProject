package com.pixelvector.animalmatch.domain.model

/**
 * 6종 동물 캐릭터 정의 (MASTER_SPEC_V2.md 2.2)
 */
enum class AnimalType(val id: Int, val colorHex: Long, val displayName: String) {
    BUNNY(1, 0xFFFFFFFF, "Bunny"),
    BEAR(2, 0xFF8D6E63, "Bear"),
    CAT(3, 0xFFFFD54F, "Cat"),
    PIG(4, 0xFFF06292, "Pig"),
    PENGUIN(5, 0xFF42A5F5, "Penguin"),
    FROG(6, 0xFF81C784, "Frog");

    companion object {
        fun random(): AnimalType = entries.random()
    }
}
