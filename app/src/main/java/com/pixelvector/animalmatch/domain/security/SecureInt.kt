package com.pixelvector.animalmatch.domain.security

/**
 * 런타임 메모리에서 평문 Int 노출을 방지하는 XOR 난독화 래퍼 클래스.
 * 점수(Score), 코인(Coin) 등 주요 재화/점수 변조(GameGuardian 등)를 방어합니다.
 */
class SecureInt(initialValue: Int = 0) {
    private var mask: Int = generateMask()
    private var encryptedValue: Int = initialValue xor mask

    private fun generateMask(): Int {
        val nano = System.nanoTime()
        val time = System.currentTimeMillis()
        val mixed = ((nano xor (nano ushr 32)) xor time xor 0x5A5A5A5AL).toInt()
        return if (mixed == 0) 0x5A5A5A5A else mixed
    }

    @Synchronized
    fun get(): Int = encryptedValue xor mask

    @Synchronized
    fun set(value: Int) {
        // 값 갱신 시마다 마스크를 회전시켜 고정 메모리 주소 추적까지 차단
        mask = generateMask()
        encryptedValue = value xor mask
    }

    @Synchronized
    fun add(delta: Int): Int {
        val updated = (get() + delta).coerceAtLeast(0)
        set(updated)
        return updated
    }
}
