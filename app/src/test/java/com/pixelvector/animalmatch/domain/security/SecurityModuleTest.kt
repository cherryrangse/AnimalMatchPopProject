package com.pixelvector.animalmatch.domain.security

import com.pixelvector.animalmatch.domain.model.GridPos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityModuleTest {

    @Test
    fun `SecureInt obfuscates and updates score and coins accurately`() {
        val secureScore = SecureInt(100)
        assertEquals(100, secureScore.get())

        val afterAdd = secureScore.add(250)
        assertEquals(350, afterAdd)
        assertEquals(350, secureScore.get())

        secureScore.set(1200)
        assertEquals(1200, secureScore.get())

        // 음수 방어 확인
        secureScore.add(-5000)
        assertEquals(0, secureScore.get())
    }

    @Test
    fun `AntiMacroLimiter blocks rapid clicks and fixed-interval macro patterns`() {
        val limiter = AntiMacroLimiter(
            minIntervalMillis = 120L,
            identicalIntervalToleranceMillis = 2L,
            identicalIntervalThreshold = 4
        )

        var now = 1000L
        assertTrue("첫 입력은 허용되어야 함", limiter.isAllowed(now))

        // 50ms 만의 비정상 초고속 입력 -> 차단
        now += 50L
        assertFalse("최소 간격(120ms) 미만 초고속 터치 차단", limiter.isAllowed(now))

        // 정상 간격(200ms)으로 고정 주기 반복 시 매크로 감지
        limiter.reset()
        var macroTime = 2000L
        assertTrue(limiter.isAllowed(macroTime)) // 1st
        macroTime += 250L
        assertTrue(limiter.isAllowed(macroTime)) // interval 250ms (baseline)
        macroTime += 250L
        assertTrue(limiter.isAllowed(macroTime)) // identical #1
        macroTime += 251L
        assertTrue(limiter.isAllowed(macroTime)) // identical #2
        macroTime += 249L
        assertTrue(limiter.isAllowed(macroTime)) // identical #3
        macroTime += 250L
        assertFalse("고정 주기 매크로 임계치 도달 시 차단", limiter.isAllowed(macroTime)) // identical #4 -> blocked
    }

    @Test
    fun `GridPos adjacency check works for orthogonal neighbors only`() {
        val center = GridPos(3, 3)
        assertTrue(center.isAdjacentTo(GridPos(2, 3)))
        assertTrue(center.isAdjacentTo(GridPos(4, 3)))
        assertTrue(center.isAdjacentTo(GridPos(3, 2)))
        assertTrue(center.isAdjacentTo(GridPos(3, 4)))

        assertFalse("대각선은 인접 스왑 불가", center.isAdjacentTo(GridPos(2, 2)))
        assertFalse("동일 좌표는 인접 아님", center.isAdjacentTo(GridPos(3, 3)))
        assertFalse("2칸 이상 떨어진 셀은 스왑 불가", center.isAdjacentTo(GridPos(3, 5)))
    }
}
