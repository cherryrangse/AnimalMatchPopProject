package com.pixelvector.animalmatch.domain.security

import kotlin.math.abs

/**
 * 비정상 초고속 터치/스와이프 및 기계적 고정 주기 반복 입력(매크로)을 차단하는 레이트 리미터.
 */
class AntiMacroLimiter(
    private val minIntervalMillis: Long = 120L,
    private val identicalIntervalToleranceMillis: Long = 2L,
    private val identicalIntervalThreshold: Int = 5
) {
    private var lastInputTime: Long = 0L
    private var lastInterval: Long = 0L
    private var identicalIntervalCount: Int = 0

    @Synchronized
    fun isAllowed(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (lastInputTime == 0L) {
            lastInputTime = currentTimeMillis
            return true
        }

        val interval = currentTimeMillis - lastInputTime
        lastInputTime = currentTimeMillis

        // 1. 최소 간격 미만 초고속 입력 차단
        if (interval < minIntervalMillis) {
            return false
        }

        // 2. 허용 오차 이내의 기계적 고정 주기 반복 매크로 감지
        if (lastInterval > 0L && abs(interval - lastInterval) <= identicalIntervalToleranceMillis) {
            identicalIntervalCount++
            if (identicalIntervalCount >= identicalIntervalThreshold) {
                return false
            }
        } else {
            identicalIntervalCount = 0
        }

        lastInterval = interval
        return true
    }

    @Synchronized
    fun reset() {
        lastInputTime = 0L
        lastInterval = 0L
        identicalIntervalCount = 0
    }
}
