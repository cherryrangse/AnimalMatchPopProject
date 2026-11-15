package com.pixelvector.animalmatch.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.pixelvector.animalmatch.domain.model.AnimalType
import com.pixelvector.animalmatch.domain.model.SpecialBlockType
import kotlin.math.cos
import kotlin.math.sin

private val RainbowMirrorColors = listOf(
    Color(0xFFFF5252),
    Color(0xFFFFB300),
    Color(0xFFFFEB3B),
    Color(0xFF69F0AE),
    Color(0xFF40C4FF),
    Color(0xFFE040FB),
    Color(0xFFFF5252)
)

/**
 * 6종 동물 캐릭터 및 4종 특수 블록을 애니팡 스타일의 말랑말랑한 젤리 탄성 & 생동감 있는 표정 애니메이션으로 렌더링하는 컴포저블.
 *
 * 성능 최적화:
 * - 보드 전체가 단 1개의 공유 InfiniteTransition(sharedWaveAngle, sharedSpecialPulse)을 공유하고,
 *   State.value 읽기를 Canvas DrawScope 내부로 지연(Deferred State Read)하여 매 프레임 리컴포지션 0회 달성
 * - Path 객체를 remember로 사전 할당 후 재사용(reset)하여 프레임당 힙 메모리 할당(GC) 0회 달성
 */
@Composable
fun AnimalVectorTile(
    animal: AnimalType,
    special: SpecialBlockType,
    isSelected: Boolean,
    isMatched: Boolean,
    modifier: Modifier = Modifier,
    tileId: Long = 0L,
    sharedWaveAngle: State<Float>? = null,
    sharedSpecialPulse: State<Float>? = null
) {
    // 1. 선택/매치 시 탱글탱글한 스프링 탄성 스케일 (State 객체를 DrawScope 안에서만 읽음)
    val baseScaleState = animateFloatAsState(
        targetValue = when {
            isMatched -> 1.25f
            isSelected -> 1.14f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tileBaseScale"
    )

    // 2. 부모 보드에서 전달된 공유 타이머가 없을 때만 로컬 타이머 생성
    val waveAngleState: State<Float> = sharedWaveAngle ?: run {
        val transition = rememberInfiniteTransition(label = "fallbackWave")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 6.2831855f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "waveAngle"
        )
    }

    val specialPulseState: State<Float> = sharedSpecialPulse ?: run {
        val transition = rememberInfiniteTransition(label = "fallbackPulse")
        transition.animateFloat(
            initialValue = 0.93f,
            targetValue = 1.09f,
            animationSpec = infiniteRepeatable(
                animation = tween(520, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "specialPulse"
        )
    }

    // 3. GC 부하 방지를 위해 단일 Path 인스턴스를 재사용
    val reusablePath = remember { Path() }

    Canvas(modifier = modifier) {
        val waveAngle = waveAngleState.value
        val baseScale = baseScaleState.value
        val pulseFactor = if (special != SpecialBlockType.NORMAL) specialPulseState.value else 1f

        val phase = waveAngle + (tileId % 17L) * 0.55f
        val sinVal = sin(phase)
        val cosVal = cos(phase)

        val squashIntensity = if (isSelected) 0.065f else 0.032f
        val jellyScaleX = 1f + sinVal * squashIntensity
        val jellyScaleY = 1f - sinVal * squashIntensity

        val tiltDegrees = when {
            isMatched -> sin(phase * 3f) * 10f
            isSelected -> sin(phase * 2.5f) * 7.5f
            else -> cosVal * 2.2f
        }

        val isHappyOrBlink = isMatched || isSelected || (sin(phase + (tileId % 5L) * 1.1f) > 0.92f)
        val earTwitch = sin(phase * 2f) * 0.06f

        val finalScaleX = baseScale * jellyScaleX * pulseFactor
        val finalScaleY = baseScale * jellyScaleY * pulseFactor
        val bobOffsetY = sinVal * (size.height * 0.024f)

        translate(left = 0f, top = bobOffsetY) {
            rotate(degrees = tiltDegrees) {
                scale(scaleX = finalScaleX, scaleY = finalScaleY) {
                    if (special == SpecialBlockType.RAINBOW_MIRROR) {
                        drawRainbowMirrorBall(waveAngle, reusablePath)
                    } else {
                        drawAnimalFace(
                            animal = animal,
                            isHappyOrBlink = isHappyOrBlink,
                            earTwitch = earTwitch,
                            reusablePath = reusablePath
                        )
                        drawSpecialOverlay(special)
                    }

                    if (isMatched) {
                        drawBurstSparkles()
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawAnimalFace(
    animal: AnimalType,
    isHappyOrBlink: Boolean,
    earTwitch: Float,
    reusablePath: Path
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val r = minOf(w, h) * 0.42f
    val baseColor = if (animal == AnimalType.BUNNY) Color(0xFFFFFFFF) else Color(animal.colorHex)
    val darkEye = Color(0xFF2D1E18)
    val blush = if (animal == AnimalType.BUNNY) Color(0xFFFF80AB).copy(alpha = 0.45f) else Color(0xFFFF5252).copy(alpha = 0.38f)

    // 1. 동물별 귀(Ears) 렌더링
    val earOffsetY = r * earTwitch
    when (animal) {
        AnimalType.BUNNY -> {
            val earW = r * 0.42f
            val earH = r * 0.98f
            // 순백의 귀 본체 + 부드러운 외곽선
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx - r * 0.65f, cy - r * 1.35f + earOffsetY),
                size = Size(earW, earH),
                cornerRadius = CornerRadius(earW / 2f, earW / 2f)
            )
            drawRoundRect(
                color = Color(0xFFE0E0E0),
                topLeft = Offset(cx - r * 0.65f, cy - r * 1.35f + earOffsetY),
                size = Size(earW, earH),
                cornerRadius = CornerRadius(earW / 2f, earW / 2f),
                style = Stroke(width = 1.8f)
            )
            // 귀 안쪽 베이비 핑크
            drawRoundRect(
                color = Color(0xFFFFB2C9),
                topLeft = Offset(cx - r * 0.55f, cy - r * 1.20f + earOffsetY),
                size = Size(earW * 0.5f, earH * 0.65f),
                cornerRadius = CornerRadius(earW / 3f, earW / 3f)
            )

            // 오른쪽 귀
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx + r * 0.23f, cy - r * 1.35f - earOffsetY),
                size = Size(earW, earH),
                cornerRadius = CornerRadius(earW / 2f, earW / 2f)
            )
            drawRoundRect(
                color = Color(0xFFE0E0E0),
                topLeft = Offset(cx + r * 0.23f, cy - r * 1.35f - earOffsetY),
                size = Size(earW, earH),
                cornerRadius = CornerRadius(earW / 2f, earW / 2f),
                style = Stroke(width = 1.8f)
            )
            drawRoundRect(
                color = Color(0xFFFFB2C9),
                topLeft = Offset(cx + r * 0.33f, cy - r * 1.20f - earOffsetY),
                size = Size(earW * 0.5f, earH * 0.65f),
                cornerRadius = CornerRadius(earW / 3f, earW / 3f)
            )
        }
        AnimalType.BEAR -> {
            drawCircle(color = baseColor, radius = r * 0.38f, center = Offset(cx - r * 0.68f, cy - r * 0.68f + earOffsetY))
            drawCircle(color = Color(0xFFD7CCC8), radius = r * 0.20f, center = Offset(cx - r * 0.68f, cy - r * 0.68f + earOffsetY))
            drawCircle(color = baseColor, radius = r * 0.38f, center = Offset(cx + r * 0.68f, cy - r * 0.68f - earOffsetY))
            drawCircle(color = Color(0xFFD7CCC8), radius = r * 0.20f, center = Offset(cx + r * 0.68f, cy - r * 0.68f - earOffsetY))
        }
        AnimalType.CAT -> {
            reusablePath.reset()
            reusablePath.moveTo(cx - r * 0.85f, cy - r * 0.25f)
            reusablePath.lineTo(cx - r * 0.75f, cy - r * 1.05f + earOffsetY)
            reusablePath.lineTo(cx - r * 0.15f, cy - r * 0.70f)
            reusablePath.close()
            drawPath(reusablePath, color = baseColor)

            reusablePath.reset()
            reusablePath.moveTo(cx + r * 0.85f, cy - r * 0.25f)
            reusablePath.lineTo(cx + r * 0.75f, cy - r * 1.05f - earOffsetY)
            reusablePath.lineTo(cx + r * 0.15f, cy - r * 0.70f)
            reusablePath.close()
            drawPath(reusablePath, color = baseColor)
        }
        AnimalType.PIG -> {
            drawCircle(color = Color(0xFFEC407A), radius = r * 0.32f, center = Offset(cx - r * 0.72f, cy - r * 0.62f + earOffsetY))
            drawCircle(color = Color(0xFFEC407A), radius = r * 0.32f, center = Offset(cx + r * 0.72f, cy - r * 0.62f - earOffsetY))
        }
        AnimalType.PENGUIN -> {
            drawCircle(color = baseColor, radius = r * 0.22f, center = Offset(cx, cy - r * 0.95f + earOffsetY))
        }
        AnimalType.FROG -> {
            drawCircle(color = baseColor, radius = r * 0.38f, center = Offset(cx - r * 0.52f, cy - r * 0.72f))
            drawCircle(color = baseColor, radius = r * 0.38f, center = Offset(cx + r * 0.52f, cy - r * 0.72f))
        }
    }

    // 2. 얼굴 본체
    drawCircle(
        color = Color.Black.copy(alpha = 0.20f),
        radius = r * 1.04f,
        center = Offset(cx, cy + r * 0.13f)
    )
    drawCircle(
        color = baseColor,
        radius = r,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.88f),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = 2.4f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.30f),
        radius = r * 0.80f,
        center = Offset(cx - r * 0.08f, cy - r * 0.12f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = r * 0.16f,
        center = Offset(cx - r * 0.45f, cy - r * 0.48f)
    )

    // 3. 동물별 얼굴 디테일
    when (animal) {
        AnimalType.PENGUIN -> {
            drawCircle(color = Color.White, radius = r * 0.72f, center = Offset(cx, cy + r * 0.12f))
            reusablePath.reset()
            reusablePath.moveTo(cx - r * 0.25f, cy + r * 0.05f)
            reusablePath.lineTo(cx + r * 0.25f, cy + r * 0.05f)
            reusablePath.lineTo(cx, cy + r * 0.35f)
            reusablePath.close()
            drawPath(reusablePath, color = Color(0xFFFFB300))
        }
        AnimalType.PIG -> {
            drawRoundRect(
                color = Color(0xFFEC407A),
                topLeft = Offset(cx - r * 0.36f, cy - r * 0.02f),
                size = Size(r * 0.72f, r * 0.48f),
                cornerRadius = CornerRadius(r * 0.24f, r * 0.24f)
            )
            drawCircle(color = darkEye, radius = r * 0.08f, center = Offset(cx - r * 0.14f, cy + r * 0.22f))
            drawCircle(color = darkEye, radius = r * 0.08f, center = Offset(cx + r * 0.14f, cy + r * 0.22f))
        }
        AnimalType.BEAR -> {
            drawCircle(color = Color(0xFFEFEBE9), radius = r * 0.36f, center = Offset(cx, cy + r * 0.20f))
            drawCircle(color = darkEye, radius = r * 0.13f, center = Offset(cx, cy + r * 0.12f))
        }
        AnimalType.CAT -> {
            drawCircle(color = Color(0xFFFF80AB), radius = r * 0.12f, center = Offset(cx, cy + r * 0.12f))
            drawLine(
                color = darkEye.copy(alpha = 0.6f),
                start = Offset(cx - r * 0.85f, cy + r * 0.05f),
                end = Offset(cx - r * 0.45f, cy + r * 0.12f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = darkEye.copy(alpha = 0.6f),
                start = Offset(cx + r * 0.85f, cy + r * 0.05f),
                end = Offset(cx + r * 0.45f, cy + r * 0.12f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
        AnimalType.BUNNY -> {
            drawCircle(color = Color(0xFFFF4081), radius = r * 0.10f, center = Offset(cx, cy + r * 0.12f))
            // 눈토끼의 귀여운 핑크 입 (Y자 토끼 입술)
            drawLine(
                color = Color(0xFFFF4081),
                start = Offset(cx, cy + r * 0.14f),
                end = Offset(cx, cy + r * 0.22f),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFF4081),
                start = Offset(cx, cy + r * 0.22f),
                end = Offset(cx - r * 0.12f, cy + r * 0.26f),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFF4081),
                start = Offset(cx, cy + r * 0.22f),
                end = Offset(cx + r * 0.12f, cy + r * 0.26f),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
        }
        AnimalType.FROG -> {
            drawCircle(color = Color.White, radius = r * 0.22f, center = Offset(cx - r * 0.52f, cy - r * 0.72f))
            drawCircle(color = Color.White, radius = r * 0.22f, center = Offset(cx + r * 0.52f, cy - r * 0.72f))
            if (isHappyOrBlink) {
                drawLine(
                    color = darkEye,
                    start = Offset(cx - r * 0.62f, cy - r * 0.72f),
                    end = Offset(cx - r * 0.42f, cy - r * 0.72f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = darkEye,
                    start = Offset(cx + r * 0.42f, cy - r * 0.72f),
                    end = Offset(cx + r * 0.62f, cy - r * 0.72f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            } else {
                drawCircle(color = darkEye, radius = r * 0.12f, center = Offset(cx - r * 0.52f, cy - r * 0.72f))
                drawCircle(color = darkEye, radius = r * 0.12f, center = Offset(cx + r * 0.52f, cy - r * 0.72f))
            }
            drawLine(
                color = darkEye,
                start = Offset(cx - r * 0.35f, cy + r * 0.18f),
                end = Offset(cx + r * 0.35f, cy + r * 0.18f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }

    // 4. 공통 눈 & 볼터치 (개구리 제외)
    if (animal != AnimalType.FROG) {
        if (isHappyOrBlink) {
            reusablePath.reset()
            reusablePath.moveTo(cx - r * 0.48f, cy - r * 0.08f)
            reusablePath.quadraticTo(cx - r * 0.36f, cy - r * 0.24f, cx - r * 0.24f, cy - r * 0.08f)
            drawPath(reusablePath, color = darkEye, style = Stroke(width = 3.2f, cap = StrokeCap.Round))

            reusablePath.reset()
            reusablePath.moveTo(cx + r * 0.24f, cy - r * 0.08f)
            reusablePath.quadraticTo(cx + r * 0.36f, cy - r * 0.24f, cx + r * 0.48f, cy - r * 0.08f)
            drawPath(reusablePath, color = darkEye, style = Stroke(width = 3.2f, cap = StrokeCap.Round))
        } else {
            drawCircle(color = darkEye, radius = r * 0.135f, center = Offset(cx - r * 0.36f, cy - r * 0.12f))
            drawCircle(color = darkEye, radius = r * 0.135f, center = Offset(cx + r * 0.36f, cy - r * 0.12f))
            drawCircle(color = Color.White, radius = r * 0.055f, center = Offset(cx - r * 0.40f, cy - r * 0.16f))
            drawCircle(color = Color.White, radius = r * 0.055f, center = Offset(cx + r * 0.32f, cy - r * 0.16f))
        }
    }

    drawCircle(color = blush, radius = r * 0.17f, center = Offset(cx - r * 0.52f, cy + r * 0.10f))
    drawCircle(color = blush, radius = r * 0.17f, center = Offset(cx + r * 0.52f, cy + r * 0.10f))
}

private fun DrawScope.drawSpecialOverlay(special: SpecialBlockType) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    when (special) {
        SpecialBlockType.LINE_BOMB_H -> {
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(w * 0.08f, cy),
                end = Offset(w * 0.92f, cy),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFFFFEA00),
                radius = minOf(w, h) * 0.44f,
                center = Offset(cx, cy),
                style = Stroke(width = 3.5f)
            )
        }
        SpecialBlockType.LINE_BOMB_V -> {
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(cx, h * 0.08f),
                end = Offset(cx, h * 0.92f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = minOf(w, h) * 0.44f,
                center = Offset(cx, cy),
                style = Stroke(width = 3.5f)
            )
        }
        SpecialBlockType.AREA_BOMB -> {
            drawCircle(
                color = Color(0xFFFF1744),
                radius = minOf(w, h) * 0.45f,
                center = Offset(cx, cy),
                style = Stroke(width = 4.5f)
            )
            drawCircle(
                color = Color(0xFFFFEA00),
                radius = minOf(w, h) * 0.12f,
                center = Offset(cx, h * 0.12f)
            )
        }
        SpecialBlockType.NORMAL, SpecialBlockType.RAINBOW_MIRROR -> Unit
    }
}

private fun DrawScope.drawRainbowMirrorBall(waveAngle: Float, reusablePath: Path) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val r = minOf(w, h) * 0.42f

    rotate(degrees = (waveAngle / 6.2831855f) * 360f) {
        drawCircle(
            brush = Brush.sweepGradient(colors = RainbowMirrorColors, center = Offset(cx, cy)),
            radius = r,
            center = Offset(cx, cy)
        )
    }
    drawCircle(
        color = Color.White,
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = 3.5f)
    )
    reusablePath.reset()
    reusablePath.moveTo(cx, cy - r * 0.65f)
    reusablePath.lineTo(cx + r * 0.20f, cy - r * 0.20f)
    reusablePath.lineTo(cx + r * 0.65f, cy)
    reusablePath.lineTo(cx + r * 0.20f, cy + r * 0.20f)
    reusablePath.lineTo(cx, cy + r * 0.65f)
    reusablePath.lineTo(cx - r * 0.20f, cy + r * 0.20f)
    reusablePath.lineTo(cx - r * 0.65f, cy)
    reusablePath.lineTo(cx - r * 0.20f, cy - r * 0.20f)
    reusablePath.close()
    drawPath(reusablePath, color = Color.White)
}

private fun DrawScope.drawBurstSparkles() {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val r = minOf(w, h) * 0.47f

    drawCircle(
        color = Color(0xFFFFF59D).copy(alpha = 0.85f),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = 5.5f)
    )
    drawCircle(color = Color.White, radius = r * 0.14f, center = Offset(cx - r * 0.85f, cy - r * 0.85f))
    drawCircle(color = Color.White, radius = r * 0.14f, center = Offset(cx + r * 0.85f, cy - r * 0.85f))
    drawCircle(color = Color.White, radius = r * 0.14f, center = Offset(cx - r * 0.85f, cy + r * 0.85f))
    drawCircle(color = Color.White, radius = r * 0.14f, center = Offset(cx + r * 0.85f, cy + r * 0.85f))
}
