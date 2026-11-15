package com.pixelvector.animalmatch.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ParticleSpec(
    val xRatio: Float,
    val baseYRatio: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val color: Color
)

/**
 * Fever 모드 발동 시 보드 주변에 반짝이는 버블/별빛 파티클을 렌더링하는 오버레이
 */
@Composable
fun FeverParticleOverlay(
    isFeverTime: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isFeverTime) return

    val particles = remember {
        val palette = listOf(
            Color(0xFFFF85A2),
            Color(0xFFFFD54F),
            Color(0xFF42A5F5),
            Color(0xFF81C784),
            Color(0xFFFF4081)
        )
        val rng = Random(2026)
        List(28) {
            ParticleSpec(
                xRatio = rng.nextFloat(),
                baseYRatio = rng.nextFloat(),
                radius = rng.nextFloat() * 8f + 5f,
                speed = rng.nextFloat() * 0.8f + 0.5f,
                phase = rng.nextFloat() * 6.28f,
                color = palette[it % palette.size]
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "feverParticles")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "feverParticleProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val currentYRatio = (p.baseYRatio - progress * p.speed).mod(1f)
            val waveX = sin(progress * 6.28f + p.phase) * 14f
            val x = (p.xRatio * w + waveX).coerceIn(0f, w)
            val y = currentYRatio * h
            val alpha = (0.35f + 0.45f * cos(progress * 6.28f + p.phase)).coerceIn(0.15f, 0.85f)

            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.radius,
                center = Offset(x, y)
            )
        }
    }
}
