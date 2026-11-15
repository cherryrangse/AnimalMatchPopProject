package com.pixelvector.animalmatch.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.pixelvector.animalmatch.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * 숲속 퍼즐 테마 BGM(스튜디오 마스터링 고음질 음원)과
 * 콤보별 팝핑, 폭발, 레이저, 마법 차임 효과음을 통합 관리하는 오디오 엔진
 */
object SoundEffectPlayer {

    private const val SAMPLE_RATE = 44100
    private var soundPool: SoundPool? = null
    private var mediaPlayer: MediaPlayer? = null
    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // SoundPool 사운드 ID
    private var popSoundIds = IntArray(8)
    private var explosionSoundId = 0
    private var laserSoundId = 0
    private var magicChimeSoundId = 0
    private var feverFanfareSoundId = 0

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        appContext = context.applicationContext

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()

        scope.launch {
            generateAndLoadAudioClips()
        }
    }

    private fun generateAndLoadAudioClips() {
        val sp = soundPool ?: return

        // 1. 도레미파솔라시도 워터드롭 팝핑
        val baseFreqs = floatArrayOf(523.25f, 587.33f, 659.25f, 698.46f, 783.99f, 880.00f, 987.77f, 1046.50f)
        for (i in baseFreqs.indices) {
            val samples = generatePopTone(baseFreqs[i])
            popSoundIds[i] = loadPcmToSoundPool(sp, samples)
        }

        // 2. 묵직하고 시원한 Boom 폭탄 폭발음
        val explosionSamples = generateExplosionTone()
        explosionSoundId = loadPcmToSoundPool(sp, explosionSamples)

        // 3. 레이저 빔 관통 사운드
        val laserSamples = generateLaserTone()
        laserSoundId = loadPcmToSoundPool(sp, laserSamples)

        // 4. 무지개 미러볼 마법 별빛 아르페지오
        val magicSamples = generateMagicChimeTone()
        magicChimeSoundId = loadPcmToSoundPool(sp, magicSamples)

        // 5. 피버 타임 팡파르
        val fanfareSamples = generateFanfareTone()
        feverFanfareSoundId = loadPcmToSoundPool(sp, fanfareSamples)
    }

    private fun loadPcmToSoundPool(sp: SoundPool, samples: ShortArray): Int {
        return try {
            val wavBytes = pcmToWav(samples, SAMPLE_RATE)
            val tempFile = java.io.File.createTempFile("snd_", ".wav")
            tempFile.deleteOnExit()
            tempFile.writeBytes(wavBytes)
            sp.load(tempFile.absolutePath, 1)
        } catch (e: Exception) {
            0
        }
    }

    private fun generatePopTone(freq: Float): ShortArray {
        val durationMs = 140
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / numSamples
            val curFreq = freq * (1f + 0.6f * (1f - progress))
            val wave = sin(2.0 * PI * curFreq * t).toFloat()
            val envelope = (1f - progress) * (1f - progress)
            buffer[i] = (wave * envelope * Short.MAX_VALUE * 0.85f).toInt().toShort()
        }
        return buffer
    }

    private fun generateExplosionTone(): ShortArray {
        val durationMs = 420
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        val rng = java.util.Random(123)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / numSamples
            val lowFreq = 140f * (1f - progress * 0.75f)
            val boom = sin(2.0 * PI * lowFreq * t).toFloat()
            val noise = (rng.nextFloat() * 2f - 1f) * (1f - progress * 0.7f)
            val mixed = (boom * 0.8f + noise * 0.2f) * (1f - progress)
            buffer[i] = (mixed * Short.MAX_VALUE * 0.95f).toInt().toShort()
        }
        return buffer
    }

    private fun generateLaserTone(): ShortArray {
        val durationMs = 280
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / numSamples
            val curFreq = 1600f * (1f - progress * 0.8f)
            val wave = sin(2.0 * PI * curFreq * t).toFloat()
            val env = (1f - progress)
            buffer[i] = (wave * env * Short.MAX_VALUE * 0.75f).toInt().toShort()
        }
        return buffer
    }

    private fun generateMagicChimeTone(): ShortArray {
        val durationMs = 550
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        val freqs = floatArrayOf(880f, 1108f, 1318f, 1760f)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / numSamples
            var wave = 0f
            for (idx in freqs.indices) {
                val delayOffset = idx * 0.08f
                if (t >= delayOffset) {
                    val localT = t - delayOffset
                    wave += sin(2.0 * PI * freqs[idx] * localT).toFloat() * 0.25f
                }
            }
            val env = (1f - progress)
            buffer[i] = (wave * env * Short.MAX_VALUE * 0.8f).toInt().toShort()
        }
        return buffer
    }

    private fun generateFanfareTone(): ShortArray {
        val durationMs = 600
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        val chord = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / numSamples
            var sum = 0f
            for (f in chord) {
                sum += sin(2.0 * PI * f * t).toFloat()
            }
            val env = (1f - progress)
            buffer[i] = ((sum / chord.size) * env * Short.MAX_VALUE * 0.85f).toInt().toShort()
        }
        return buffer
    }

    private fun pcmToWav(pcmData: ShortArray, sampleRate: Int): ByteArray {
        val byteData = ByteArray(pcmData.size * 2)
        for (i in pcmData.indices) {
            byteData[i * 2] = (pcmData[i].toInt() and 0xff).toByte()
            byteData[i * 2 + 1] = ((pcmData[i].toInt() shr 8) and 0xff).toByte()
        }
        val totalAudioLen = byteData.size
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * 2 * channels

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2; header[33] = 0
        header[34] = 16; header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        return header + byteData
    }

    // --- 공개 효과음 재생 API ---

    fun playMatchPop(enabled: Boolean, combo: Int = 1) {
        if (!enabled) return
        val sp = soundPool ?: return
        val clampedIdx = (combo - 1).coerceIn(0, popSoundIds.size - 1)
        val sId = popSoundIds[clampedIdx]
        if (sId != 0) {
            sp.play(sId, 0.95f, 0.95f, 1, 0, 1.0f)
        }
    }

    fun playExplosion(enabled: Boolean) {
        if (!enabled) return
        val sp = soundPool ?: return
        if (explosionSoundId != 0) {
            sp.play(explosionSoundId, 1.0f, 1.0f, 2, 0, 1.0f)
        }
    }

    fun playLaser(enabled: Boolean) {
        if (!enabled) return
        val sp = soundPool ?: return
        if (laserSoundId != 0) {
            sp.play(laserSoundId, 0.95f, 0.95f, 2, 0, 1.0f)
        }
    }

    fun playMagicChime(enabled: Boolean) {
        if (!enabled) return
        val sp = soundPool ?: return
        if (magicChimeSoundId != 0) {
            sp.play(magicChimeSoundId, 1.0f, 1.0f, 2, 0, 1.0f)
        }
    }

    fun playInvalidRollback(enabled: Boolean) {
        if (!enabled) return
        val sp = soundPool ?: return
        val sId = popSoundIds[0]
        if (sId != 0) {
            sp.play(sId, 0.6f, 0.6f, 1, 0, 0.65f)
        }
    }

    fun playFeverStart(enabled: Boolean) {
        if (!enabled) return
        val sp = soundPool ?: return
        if (feverFanfareSoundId != 0) {
            sp.play(feverFanfareSoundId, 1.0f, 1.0f, 3, 0, 1.0f)
        }
    }

    // --- 고음질 BGM (MediaPlayer 기반 무한 루프 재생) ---

    fun startBgm(enabled: Boolean) {
        if (!enabled) {
            stopBgm()
            return
        }

        val ctx = appContext ?: return
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(ctx, R.raw.bgm_forest_theme)?.apply {
                    isLooping = true
                    setVolume(0.48f, 0.48f) // 인게임 효과음과 가장 어울리는 편안한 볼륨 밸런스
                }
            }
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopBgm() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {}
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null

        soundPool?.release()
        soundPool = null
        isInitialized = false
    }
}
