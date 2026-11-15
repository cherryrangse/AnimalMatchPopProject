package com.pixelvector.animalmatch.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pixelvector.animalmatch.domain.model.StageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "animal_match_pop_prefs"
)

data class PlayerProfileData(
    val highScoreEasy: Int = 0,
    val highScoreNormal: Int = 0,
    val highScoreHard: Int = 0,
    val coins: Int = 0,
    val unlockedStage: Int = 1,
    val currentStage: Int = 1,
    val isSoundEnabled: Boolean = true,
    val isAdsRemoved: Boolean = false
)

class GamePreferencesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val deviceFp = CryptoIntegrityHelper.getDeviceFingerprint(appContext)

    val playerProfileFlow: Flow<PlayerProfileData> = appContext.dataStore.data.map { prefs ->
        val highScoreEasy = CryptoIntegrityHelper.decryptAndVerifyInt(
            keyName = "high_score_easy",
            storedToken = prefs[KEY_ENC_HIGH_SCORE_EASY],
            deviceFingerprint = deviceFp,
            defaultValue = 0
        )

        val highScoreNormal = CryptoIntegrityHelper.decryptAndVerifyInt(
            keyName = "high_score",
            storedToken = prefs[KEY_ENC_HIGH_SCORE],
            deviceFingerprint = deviceFp,
            defaultValue = 0
        )

        val highScoreHard = CryptoIntegrityHelper.decryptAndVerifyInt(
            keyName = "high_score_hard",
            storedToken = prefs[KEY_ENC_HIGH_SCORE_HARD],
            deviceFingerprint = deviceFp,
            defaultValue = 0
        )

        val coins = CryptoIntegrityHelper.decryptAndVerifyInt(
            keyName = "coins",
            storedToken = prefs[KEY_ENC_COINS],
            deviceFingerprint = deviceFp,
            defaultValue = 0
        )

        val unlockedStage = (prefs[KEY_UNLOCKED_STAGE] ?: 1).coerceIn(1, StageRepository.MAX_STAGE)
        val currentStage = (prefs[KEY_CURRENT_STAGE] ?: 1).coerceIn(1, unlockedStage)

        val soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true

        val rawAdsRemoved = prefs[KEY_ADS_REMOVED] ?: false
        val adsRemovedHash = prefs[KEY_ADS_REMOVED_HASH]
        val verifiedAdsRemoved = if (
            !rawAdsRemoved ||
            CryptoIntegrityHelper.verifyChecksum("ads_removed", rawAdsRemoved.toString(), adsRemovedHash, deviceFp)
        ) {
            rawAdsRemoved
        } else {
            false
        }

        PlayerProfileData(
            highScoreEasy = highScoreEasy,
            highScoreNormal = highScoreNormal,
            highScoreHard = highScoreHard,
            coins = coins,
            unlockedStage = unlockedStage,
            currentStage = currentStage,
            isSoundEnabled = soundEnabled,
            isAdsRemoved = verifiedAdsRemoved
        )
    }

    suspend fun updateHighScoreNormal(newHighScore: Int) {
        val safeScore = newHighScore.coerceAtLeast(0)
        appContext.dataStore.edit { prefs ->
            val currentBest = CryptoIntegrityHelper.decryptAndVerifyInt(
                keyName = "high_score",
                storedToken = prefs[KEY_ENC_HIGH_SCORE],
                deviceFingerprint = deviceFp,
                defaultValue = 0
            )
            if (safeScore > currentBest) {
                prefs[KEY_ENC_HIGH_SCORE] = CryptoIntegrityHelper.encryptInt(
                    keyName = "high_score",
                    value = safeScore,
                    deviceFingerprint = deviceFp
                )
            }
        }
    }

    suspend fun updateHighScoreEasy(newHighScore: Int) {
        val safeScore = newHighScore.coerceAtLeast(0)
        appContext.dataStore.edit { prefs ->
            val currentBest = CryptoIntegrityHelper.decryptAndVerifyInt(
                keyName = "high_score_easy",
                storedToken = prefs[KEY_ENC_HIGH_SCORE_EASY],
                deviceFingerprint = deviceFp,
                defaultValue = 0
            )
            if (safeScore > currentBest) {
                prefs[KEY_ENC_HIGH_SCORE_EASY] = CryptoIntegrityHelper.encryptInt(
                    keyName = "high_score_easy",
                    value = safeScore,
                    deviceFingerprint = deviceFp
                )
            }
        }
    }

    suspend fun updateHighScoreHard(newHighScore: Int) {
        val safeScore = newHighScore.coerceAtLeast(0)
        appContext.dataStore.edit { prefs ->
            val currentBest = CryptoIntegrityHelper.decryptAndVerifyInt(
                keyName = "high_score_hard",
                storedToken = prefs[KEY_ENC_HIGH_SCORE_HARD],
                deviceFingerprint = deviceFp,
                defaultValue = 0
            )
            if (safeScore > currentBest) {
                prefs[KEY_ENC_HIGH_SCORE_HARD] = CryptoIntegrityHelper.encryptInt(
                    keyName = "high_score_hard",
                    value = safeScore,
                    deviceFingerprint = deviceFp
                )
            }
        }
    }

    suspend fun saveStageProgress(unlockedStage: Int, currentStage: Int) {
        val clampedUnlocked = unlockedStage.coerceIn(1, StageRepository.MAX_STAGE)
        val clampedCurrent = currentStage.coerceIn(1, clampedUnlocked)
        appContext.dataStore.edit { prefs ->
            val existingUnlocked = prefs[KEY_UNLOCKED_STAGE] ?: 1
            prefs[KEY_UNLOCKED_STAGE] = maxOf(existingUnlocked, clampedUnlocked)
            prefs[KEY_CURRENT_STAGE] = clampedCurrent
        }
    }

    suspend fun updateCoins(newCoins: Int) {
        val safeCoins = newCoins.coerceAtLeast(0)
        appContext.dataStore.edit { prefs ->
            prefs[KEY_ENC_COINS] = CryptoIntegrityHelper.encryptInt(
                keyName = "coins",
                value = safeCoins,
                deviceFingerprint = deviceFp
            )
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[KEY_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setAdsRemoved(removed: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[KEY_ADS_REMOVED] = removed
            prefs[KEY_ADS_REMOVED_HASH] = CryptoIntegrityHelper.computeChecksum(
                key = "ads_removed",
                value = removed.toString(),
                deviceFingerprint = deviceFp
            )
        }
    }

    companion object {
        private val KEY_ENC_HIGH_SCORE_EASY = stringPreferencesKey("enc_high_score_easy_token")
        private val KEY_ENC_HIGH_SCORE = stringPreferencesKey("enc_high_score_token")
        private val KEY_ENC_HIGH_SCORE_HARD = stringPreferencesKey("enc_high_score_hard_token")
        private val KEY_ENC_COINS = stringPreferencesKey("enc_coins_token")
        private val KEY_UNLOCKED_STAGE = intPreferencesKey("unlocked_stage")
        private val KEY_CURRENT_STAGE = intPreferencesKey("current_stage")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_ADS_REMOVED = booleanPreferencesKey("ads_removed")
        private val KEY_ADS_REMOVED_HASH = stringPreferencesKey("ads_removed_hash")
    }
}
