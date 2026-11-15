package com.pixelvector.animalmatch.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelvector.animalmatch.R
import com.pixelvector.animalmatch.domain.model.AnimalType
import com.pixelvector.animalmatch.domain.model.GameMode
import com.pixelvector.animalmatch.domain.model.GridPos
import com.pixelvector.animalmatch.domain.model.SpecialBlockType
import com.pixelvector.animalmatch.domain.model.StageRepository
import com.pixelvector.animalmatch.domain.model.StageTargetType
import com.pixelvector.animalmatch.domain.model.TileCell
import com.pixelvector.animalmatch.ui.components.AnimalVectorTile
import com.pixelvector.animalmatch.ui.components.BannerAdView
import com.pixelvector.animalmatch.ui.components.FeverParticleOverlay
import com.pixelvector.animalmatch.ui.theme.BunnyPink
import com.pixelvector.animalmatch.ui.theme.DeepText
import com.pixelvector.animalmatch.ui.theme.FeverGold
import com.pixelvector.animalmatch.ui.theme.FeverPink
import com.pixelvector.animalmatch.ui.theme.PastelBackground
import com.pixelvector.animalmatch.ui.theme.PenguinBlue
import com.pixelvector.animalmatch.ui.theme.SubText
import com.pixelvector.animalmatch.ui.viewmodel.GameUiState
import com.pixelvector.animalmatch.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val EasyGreen = Color(0xFF5CC46A)

@Composable
fun AnimalMatchScreen(
    viewModel: GameViewModel,
    canRequestAds: Boolean = true,
    onRemoveAdsClick: () -> Unit = {},
    onRestorePurchasesClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 앱 최초 실행 시 중앙 아이콘이 숲속 배경 위에서 통통 튀며 활성화되는 스플래시 인트로 상태
    var showLaunchSplash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (showLaunchSplash) {
            delay(950L)
            showLaunchSplash = false
        }
    }

    // 보드 전체 64개 타일이 공유하는 단일 InfiniteTransition (128개 개별 코루틴 -> 2개로 최적화)
    val sharedTransition = rememberInfiniteTransition(label = "boardSharedTransition")
    val sharedWaveAngle = sharedTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sharedWaveAngle"
    )
    val sharedSpecialPulse = sharedTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sharedSpecialPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PastelBackground)
    ) {
        // 0. 동화풍 파스텔 숲속 초원 배경 이미지 (앱 실행 스플래시, 타이틀, 인게임 모두 일관되게 채움)
        Image(
            painter = painterResource(id = R.drawable.bg_forest_meadow),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Fever Time 발동 시 은은한 핑크빛 마법 필터 + 배경 파티클 오버레이
        if (uiState.isFeverTime && !uiState.isOnTitleScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x28FF4081))
            )
        }
        FeverParticleOverlay(isFeverTime = uiState.isFeverTime || uiState.isOnTitleScreen)

        if (showLaunchSplash) {
            ForestLaunchSplashOverlay()
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 최상단 AdMob Adaptive Banner + Safety Divider (MASTER_SPEC_V2.md 3)
                if (!uiState.isAdsRemoved) {
                    BannerAdView(
                        isAdsRemoved = false,
                        canRequestAds = canRequestAds
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.6f))
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 2. 타이틀(시작 화면) <-> 인게임 화면 부드러운 전환
                Crossfade(
                    targetState = uiState.isOnTitleScreen,
                    animationSpec = tween(durationMillis = 300),
                    label = "screenCrossfade",
                    modifier = Modifier.weight(1f)
                ) { onTitle ->
                    if (onTitle) {
                        TitleStartScreen(
                            uiState = uiState,
                            sharedWaveAngle = sharedWaveAngle,
                            sharedSpecialPulse = sharedSpecialPulse,
                            onStartMode = { mode -> viewModel.startFromTitle(mode) },
                            onStartStage = { stage -> viewModel.startFromTitle(GameMode.STAGE, stage) },
                            onPreviewStage = viewModel::previewStageOnTitle,
                            onToggleSound = viewModel::toggleSound,
                            onRemoveAdsClick = onRemoveAdsClick,
                            onRestorePurchasesClick = onRestorePurchasesClick
                        )
                    } else {
                        InGameMatchContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            sharedWaveAngle = sharedWaveAngle,
                            sharedSpecialPulse = sharedSpecialPulse,
                            onRemoveAdsClick = onRemoveAdsClick,
                            onRestorePurchasesClick = onRestorePurchasesClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * 앱 실행 시 흰색 배경 대신 숲속 배경 이미지 위에서 중앙 아이콘이 통통 튀며 등장하는 스플래시 인트로
 */
@Composable
private fun ForestLaunchSplashOverlay() {
    val iconScale = remember { Animatable(0.75f) }
    LaunchedEffect(Unit) {
        iconScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale.value
                scaleY = iconScale.value
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_kawaii),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(172.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FeverPink,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun TitleStartScreen(
    uiState: GameUiState,
    sharedWaveAngle: State<Float>,
    sharedSpecialPulse: State<Float>,
    onStartMode: (GameMode) -> Unit,
    onStartStage: (Int) -> Unit,
    onPreviewStage: (Int) -> Unit,
    onToggleSound: () -> Unit,
    onRemoveAdsClick: () -> Unit,
    onRestorePurchasesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 상단 코인 & 사운드 미니 바
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.90f))
                    .border(1.5.dp, FeverGold, RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "🪙 ${stringResource(R.string.label_coins)}: %,d".format(uiState.coins),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepText
                )
            }

            OutlinedButton(
                onClick = onToggleSound,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .background(Color.White.copy(alpha = 0.88f), RoundedCornerShape(18.dp))
            ) {
                Text(
                    text = if (uiState.isSoundEnabled) {
                        stringResource(R.string.btn_sound_on)
                    } else {
                        stringResource(R.string.btn_sound_off)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepText
                )
            }
        }

        // 중앙 히어로 로고 & 말랑말랑 살아 움직이는 동물 친구들 쇼케이스
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_kawaii),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(84.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.93f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FeverPink,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.title_subtitle),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SubText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6종 동물 아이콘들이 젤리처럼 숨쉬며 인사하는 라이브 배너
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.72f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                AnimalType.entries.forEachIndexed { idx, animal ->
                    AnimalVectorTile(
                        animal = animal,
                        special = SpecialBlockType.NORMAL,
                        isSelected = false,
                        isMatched = false,
                        tileId = (idx + 1).toLong(),
                        sharedWaveAngle = sharedWaveAngle,
                        sharedSpecialPulse = sharedSpecialPulse,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        // 4가지 도전 모드 선택 카드 (Easy / Normal / Hard / Stage)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 0. 이지 모드 카드 (6x6 작은 보드 + 힌트)
            TitleModeCard(
                title = stringResource(R.string.mode_easy),
                description = stringResource(R.string.mode_easy_desc),
                badgeText = "${stringResource(R.string.label_high_score)}: %,d".format(uiState.highScoreEasy),
                accentColor = EasyGreen,
                representativeAnimal = AnimalType.FROG,
                tileId = 404L,
                sharedWaveAngle = sharedWaveAngle,
                sharedSpecialPulse = sharedSpecialPulse,
                onStartClick = { onStartMode(GameMode.EASY) }
            )

            // 1. 노멀 모드 카드
            TitleModeCard(
                title = stringResource(R.string.mode_normal),
                description = stringResource(R.string.mode_normal_desc),
                badgeText = "${stringResource(R.string.label_high_score)}: %,d".format(uiState.highScoreNormal),
                accentColor = BunnyPink,
                representativeAnimal = AnimalType.BUNNY,
                tileId = 101L,
                sharedWaveAngle = sharedWaveAngle,
                sharedSpecialPulse = sharedSpecialPulse,
                onStartClick = { onStartMode(GameMode.NORMAL) }
            )

            // 2. 하드 모드 카드
            TitleModeCard(
                title = stringResource(R.string.mode_hard),
                description = stringResource(R.string.mode_hard_desc),
                badgeText = "${stringResource(R.string.label_high_score)}: %,d".format(uiState.highScoreHard),
                accentColor = FeverPink,
                representativeAnimal = AnimalType.CAT,
                tileId = 202L,
                sharedWaveAngle = sharedWaveAngle,
                sharedSpecialPulse = sharedSpecialPulse,
                onStartClick = { onStartMode(GameMode.HARD) }
            )

            // 3. 스테이지 모드 카드 (스테이지 선택 화살표 포함)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartStage(uiState.currentStage) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, PenguinBlue.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AnimalVectorTile(
                            animal = AnimalType.PENGUIN,
                            special = SpecialBlockType.NORMAL,
                            isSelected = true,
                            isMatched = false,
                            tileId = 303L,
                            sharedWaveAngle = sharedWaveAngle,
                            sharedSpecialPulse = sharedSpecialPulse,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.mode_stage),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepText
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PenguinBlue.copy(alpha = 0.14f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "◀",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.currentStage > 1) DeepText else Color.LightGray,
                                        modifier = Modifier
                                            .clickable(enabled = uiState.currentStage > 1) {
                                                onPreviewStage(uiState.currentStage - 1)
                                            }
                                            .padding(horizontal = 4.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.stage_title, uiState.currentStage),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PenguinBlue
                                    )
                                    val canNext = uiState.currentStage < uiState.unlockedStage
                                    Text(
                                        text = "▶",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canNext) DeepText else Color.LightGray,
                                        modifier = Modifier
                                            .clickable(enabled = canNext) {
                                                onPreviewStage(uiState.currentStage + 1)
                                            }
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.mode_stage_desc),
                                fontSize = 11.sp,
                                color = SubText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Button(
                        onClick = { onStartStage(uiState.currentStage) },
                        colors = ButtonDefaults.buttonColors(containerColor = PenguinBlue),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_start_game),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 하단 광고 제거 / 구매 복원 버튼
        if (!uiState.isAdsRemoved) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRemoveAdsClick,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = stringResource(R.string.btn_remove_ads),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepText
                    )
                }
                OutlinedButton(
                    onClick = onRestorePurchasesClick,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = stringResource(R.string.btn_restore),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepText
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun TitleModeCard(
    title: String,
    description: String,
    badgeText: String,
    accentColor: Color,
    representativeAnimal: AnimalType,
    tileId: Long,
    sharedWaveAngle: State<Float>,
    sharedSpecialPulse: State<Float>,
    onStartClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, accentColor.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AnimalVectorTile(
                    animal = representativeAnimal,
                    special = SpecialBlockType.NORMAL,
                    isSelected = true,
                    isMatched = false,
                    tileId = tileId,
                    sharedWaveAngle = sharedWaveAngle,
                    sharedSpecialPulse = sharedSpecialPulse,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepText
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = SubText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = onStartClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_start_game),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun InGameMatchContent(
    uiState: GameUiState,
    viewModel: GameViewModel,
    sharedWaveAngle: State<Float>,
    sharedSpecialPulse: State<Float>,
    onRemoveAdsClick: () -> Unit,
    onRestorePurchasesClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 3대 게임 모드 탭 선택 바 (Normal / Hard / Stage)
        ModeSelectorBar(
            uiState = uiState,
            onSelectMode = viewModel::selectGameMode,
            onSelectStage = viewModel::selectStage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // 2. Top HUD (모드별 Score / Time 또는 Moves & Goal 표시)
        TopHudSection(
            uiState = uiState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // 3. Fever Bar
        FeverBarSection(
            feverProgress = uiState.feverProgress,
            isFeverTime = uiState.isFeverTime,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. 8x8 Match-3 Board (스프링 물리 스왑 & 중력 낙하 애니메이션 적용)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            MatchBoardCanvasGrid(
                board = uiState.board,
                selectedPos = uiState.selectedPos,
                hintPositions = uiState.hintPositions,
                isFeverTime = uiState.isFeverTime,
                sharedWaveAngle = sharedWaveAngle,
                sharedSpecialPulse = sharedSpecialPulse,
                onTileClick = viewModel::onTileTapped,
                onTileSwap = viewModel::onSwapRequested
            )

            // 일시정지 / 스테이지 클리어 / 게임 종료 오버레이 모달
            this@Column.AnimatedVisibility(
                visible = uiState.isPaused || uiState.isGameOver || uiState.isStageCleared,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BoardStateOverlay(
                    uiState = uiState,
                    onResume = viewModel::togglePause,
                    onRestart = { viewModel.startNewGame() },
                    onNextStage = viewModel::nextStage,
                    onHomeClick = viewModel::returnToTitle,
                    onRemoveAdsClick = onRemoveAdsClick,
                    onRestorePurchasesClick = onRestorePurchasesClick
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.82f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.hint_swap),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = DeepText
            )
        }

        // 5. Bottom Bar: Home / Sound Toggle / Pause / HighScore
        BottomBarSection(
            uiState = uiState,
            onHomeClick = viewModel::returnToTitle,
            onToggleSound = viewModel::toggleSound,
            onTogglePause = viewModel::togglePause,
            onRestart = { viewModel.startNewGame() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ModeSelectorBar(
    uiState: GameUiState,
    onSelectMode: (GameMode) -> Unit,
    onSelectStage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val modes = listOf(
            GameMode.EASY to stringResource(R.string.mode_easy),
            GameMode.NORMAL to stringResource(R.string.mode_normal),
            GameMode.HARD to stringResource(R.string.mode_hard),
            GameMode.STAGE to stringResource(R.string.mode_stage)
        )

        modes.forEach { (mode, label) ->
            val selected = uiState.gameMode == mode
            val activeColor = when (mode) {
                GameMode.EASY -> EasyGreen
                GameMode.NORMAL -> BunnyPink
                GameMode.HARD -> FeverPink
                GameMode.STAGE -> PenguinBlue
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (selected) activeColor else Color.White.copy(alpha = 0.85f))
                    .border(
                        width = 1.5.dp,
                        color = if (selected) Color.White else activeColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(17.dp)
                    )
                    .clickable { onSelectMode(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selected) Color.White else DeepText
                )
            }
        }

        if (uiState.gameMode == GameMode.STAGE) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "◀",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.currentStage > 1) DeepText else Color.LightGray,
                    modifier = Modifier
                        .clickable(enabled = uiState.currentStage > 1) {
                            onSelectStage(uiState.currentStage - 1)
                        }
                        .padding(horizontal = 4.dp)
                )
                Text(
                    text = "S${uiState.currentStage}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PenguinBlue,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                val canGoNext = uiState.currentStage < uiState.unlockedStage
                Text(
                    text = "▶",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canGoNext) DeepText else Color.LightGray,
                    modifier = Modifier
                        .clickable(enabled = canGoNext) {
                            onSelectStage(uiState.currentStage + 1)
                        }
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TopHudSection(
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.gameMode == GameMode.STAGE) {
                val cfg = uiState.stageConfig
                val goalDesc = when (cfg.targetType) {
                    StageTargetType.COLLECT_ANIMAL -> {
                        val animalName = cfg.targetAnimal?.displayName ?: "Animal"
                        "$animalName ${uiState.stageProgress}/${cfg.targetValue}"
                    }
                    StageTargetType.REACH_SCORE -> {
                        "%,d/%,d".format(uiState.stageProgress, cfg.targetValue)
                    }
                    StageTargetType.BREAK_ICE -> {
                        "❄ ${uiState.stageProgress}/${cfg.targetValue}"
                    }
                }
                HudMetricItem(
                    label = stringResource(R.string.stage_title, uiState.currentStage),
                    value = "%,d".format(uiState.score)
                )
                HudMetricItem(
                    label = stringResource(R.string.label_moves),
                    value = "${uiState.remainingMoves}",
                    highlight = uiState.remainingMoves <= 3
                )
                HudMetricItem(
                    label = stringResource(R.string.label_goal),
                    value = goalDesc,
                    highlight = true
                )
            } else {
                HudMetricItem(
                    label = stringResource(R.string.label_score),
                    value = "%,d".format(uiState.score)
                )
                HudMetricItem(
                    label = stringResource(R.string.label_time),
                    value = "${uiState.remainingSeconds}s",
                    highlight = uiState.remainingSeconds <= 10
                )
                HudMetricItem(
                    label = stringResource(R.string.label_combo),
                    value = "x${uiState.comboCount}",
                    highlight = uiState.isFeverTime
                )
            }
        }
    }
}

@Composable
private fun HudMetricItem(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = SubText, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (highlight) FeverPink else DeepText
        )
    }
}

@Composable
private fun FeverBarSection(
    feverProgress: Float,
    isFeverTime: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isFeverTime) {
                        "${stringResource(R.string.label_fever)} (SCORE x2)"
                    } else {
                        stringResource(R.string.label_fever_gauge)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFeverTime) FeverPink else DeepText
                )
                val blocksFilled = (feverProgress.coerceIn(0f, 1f) * 10).toInt()
                val barText = "■".repeat(blocksFilled) + "□".repeat(10 - blocksFilled)
                Text(
                    text = barText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFeverTime) FeverPink else FeverGold
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { feverProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isFeverTime) FeverPink else FeverGold,
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

/**
 * 8x8 보드 그리드 (고성능 레이아웃 오프셋 지연 읽기 적용)
 */
@Composable
private fun MatchBoardCanvasGrid(
    board: List<List<TileCell>>,
    selectedPos: GridPos?,
    hintPositions: Set<GridPos>,
    isFeverTime: Boolean,
    sharedWaveAngle: State<Float>,
    sharedSpecialPulse: State<Float>,
    onTileClick: (GridPos) -> Unit,
    onTileSwap: (GridPos, GridPos) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isFeverTime) {
                    Modifier.border(2.5.dp, FeverPink, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
            .padding(4.dp)
    ) {
        val density = LocalDensity.current
        val n = board.size.coerceAtLeast(1)
        val boardWidthPx = with(density) { maxWidth.toPx() }
        val boardHeightPx = with(density) { maxHeight.toPx() }
        val cellWidthDp = maxWidth / n.toFloat()
        val cellHeightDp = maxHeight / n.toFloat()
        val stepXPx = boardWidthPx / n
        val stepYPx = boardHeightPx / n

        // 1. 고정 좌표 슬롯 레이어 (얼음 표시, 선택 하이라이트, 드래그/탭 제스처 처리)
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            board.forEachIndexed { r, rowList ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    rowList.forEachIndexed { c, cell ->
                        val pos = GridPos(r, c)
                        val isSelected = selectedPos == pos
                        var dragX by remember(pos) { mutableFloatStateOf(0f) }
                        var dragY by remember(pos) { mutableFloatStateOf(0f) }
                        var swappedInGesture by remember(pos) { mutableStateOf(false) }

                        val isHint = pos in hintPositions
                        val slotBg = when {
                            cell.isMatched -> Color(0xAAFFF59D)
                            isSelected -> Color(0x88FFE0EB)
                            isHint -> Color(0x99FFF176)
                            cell.hasIce -> Color(0x99B3E5FC)
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(1.5.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(slotBg)
                                .then(
                                    when {
                                        isSelected -> Modifier.border(2.dp, FeverPink, RoundedCornerShape(12.dp))
                                        isHint -> Modifier.border(2.5.dp, FeverGold, RoundedCornerShape(12.dp))
                                        cell.hasIce -> Modifier.border(2.dp, Color(0xFF00B0FF), RoundedCornerShape(12.dp))
                                        else -> Modifier
                                    }
                                )
                                .pointerInput(pos) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragX = 0f
                                            dragY = 0f
                                            swappedInGesture = false
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (!swappedInGesture) {
                                                dragX += dragAmount.x
                                                dragY += dragAmount.y
                                                val threshold = 22f
                                                if (abs(dragX) > threshold || abs(dragY) > threshold) {
                                                    val targetPos = if (abs(dragX) > abs(dragY)) {
                                                        if (dragX > 0) GridPos(r, c + 1) else GridPos(r, c - 1)
                                                    } else {
                                                        if (dragY > 0) GridPos(r + 1, c) else GridPos(r - 1, c)
                                                    }
                                                    if (targetPos.row in 0 until n && targetPos.col in 0 until n) {
                                                        swappedInGesture = true
                                                        onTileSwap(pos, targetPos)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                                .clickable { onTileClick(pos) }
                        )
                    }
                }
            }
        }

        // 2. 타일 고유 ID(cell.id) 기반 플로팅 동물 레이어
        val allCells = remember(board) { board.flatten() }
        allCells.forEach { cell ->
            key(cell.id) {
                val isSelected = selectedPos == GridPos(cell.row, cell.col)
                val animRow = remember {
                    Animatable(initialValue = (cell.row - 1.35f).coerceAtLeast(-1.5f))
                }
                val animCol = remember {
                    Animatable(initialValue = cell.col.toFloat())
                }

                LaunchedEffect(cell.row, cell.col) {
                    launch {
                        animRow.animateTo(
                            targetValue = cell.row.toFloat(),
                            animationSpec = spring(
                                dampingRatio = 0.64f,
                                stiffness = 390f
                            )
                        )
                    }
                    launch {
                        animCol.animateTo(
                            targetValue = cell.col.toFloat(),
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = 460f
                            )
                        )
                    }
                }

                // offset 람다 내부에서 animCol.value / animRow.value를 읽어 리컴포지션 없이 Layout 배치 단계만 수행
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (animCol.value * stepXPx).roundToInt(),
                                y = (animRow.value * stepYPx).roundToInt()
                            )
                        }
                        .size(width = cellWidthDp, height = cellHeightDp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimalVectorTile(
                        animal = cell.animal,
                        special = cell.special,
                        isSelected = isSelected,
                        isMatched = cell.isMatched,
                        tileId = cell.id,
                        sharedWaveAngle = sharedWaveAngle,
                        sharedSpecialPulse = sharedSpecialPulse,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardStateOverlay(
    uiState: GameUiState,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onNextStage: () -> Unit,
    onHomeClick: () -> Unit,
    onRemoveAdsClick: () -> Unit,
    onRestorePurchasesClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val titleText = when {
                uiState.isStageCleared -> stringResource(R.string.msg_stage_cleared)
                uiState.isGameOver && uiState.gameMode == GameMode.STAGE -> stringResource(R.string.msg_stage_failed)
                uiState.isGameOver -> stringResource(R.string.msg_game_over)
                else -> stringResource(R.string.msg_paused)
            }

            Text(
                text = titleText,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (uiState.isStageCleared) PenguinBlue else DeepText
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${stringResource(R.string.label_score)}: %,d".format(uiState.score),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BunnyPink
            )
            Text(
                text = "${stringResource(R.string.label_high_score)}: %,d".format(uiState.activeHighScore),
                fontSize = 14.sp,
                color = SubText
            )
            Spacer(modifier = Modifier.height(20.dp))

            val primaryBtnAction = when {
                uiState.isStageCleared && uiState.currentStage < StageRepository.MAX_STAGE -> onNextStage
                uiState.isStageCleared || uiState.isGameOver -> onRestart
                else -> onResume
            }
            val primaryBtnText = when {
                uiState.isStageCleared && uiState.currentStage < StageRepository.MAX_STAGE -> stringResource(R.string.btn_next_stage)
                uiState.isStageCleared || uiState.isGameOver -> stringResource(R.string.btn_restart)
                else -> stringResource(R.string.btn_resume)
            }

            Button(
                onClick = primaryBtnAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isStageCleared) PenguinBlue else BunnyPink
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = primaryBtnText,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onHomeClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🏠 ${stringResource(R.string.btn_home)}",
                    fontWeight = FontWeight.Bold,
                    color = DeepText
                )
            }

            if (!uiState.isAdsRemoved) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRemoveAdsClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.btn_remove_ads), fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onRestorePurchasesClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.btn_restore), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarSection(
    uiState: GameUiState,
    onHomeClick: () -> Unit,
    onToggleSound: () -> Unit,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onHomeClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🏠 ${stringResource(R.string.btn_home)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepText
                    )
                }
                OutlinedButton(
                    onClick = onToggleSound,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (uiState.isSoundEnabled) {
                            stringResource(R.string.btn_sound_on)
                        } else {
                            stringResource(R.string.btn_sound_off)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepText
                    )
                }
            }

            Button(
                onClick = if (uiState.isGameOver || uiState.isStageCleared) onRestart else onTogglePause,
                colors = ButtonDefaults.buttonColors(containerColor = BunnyPink),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when {
                        uiState.isGameOver || uiState.isStageCleared -> stringResource(R.string.btn_restart)
                        uiState.isPaused -> stringResource(R.string.btn_resume)
                        else -> stringResource(R.string.btn_pause)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.label_high_score),
                    fontSize = 10.sp,
                    color = SubText
                )
                Text(
                    text = "%,d".format(uiState.activeHighScore),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepText
                )
            }
        }
    }
}
