package com.pixelvector.animalmatch.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Base64
import java.security.MessageDigest

/**
 * DataStore에 저장되는 최고 점수, 코인 등 민감 데이터를 XOR 암호화 및 SHA-256 해시 체크섬으로 보호합니다.
 */
object CryptoIntegrityHelper {
    private const val SECRET_SALT = "PixelVector_AnimalMatchPop_2026_SecureSalt_#772"

    @SuppressLint("HardwareIds")
    fun getDeviceFingerprint(context: Context): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
        return androidId?.takeIf { it.isNotBlank() } ?: "pixelvector_default_fp"
    }

    private fun deriveKeyBytes(deviceFingerprint: String): ByteArray {
        val seed = "$deviceFingerprint|$SECRET_SALT"
        return MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))
    }

    fun encryptInt(keyName: String, value: Int, deviceFingerprint: String): String {
        val rawPayload = "$keyName:$value"
        val keyBytes = deriveKeyBytes(deviceFingerprint)
        val inputBytes = rawPayload.toByteArray(Charsets.UTF_8)
        val xored = ByteArray(inputBytes.size) { i ->
            (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        val cipherBase64 = Base64.encodeToString(xored, Base64.NO_WRAP)
        val checksum = computeChecksum(keyName, value.toString(), deviceFingerprint)
        return "$cipherBase64.$checksum"
    }

    fun decryptAndVerifyInt(
        keyName: String,
        storedToken: String?,
        deviceFingerprint: String,
        defaultValue: Int = 0
    ): Int {
        if (storedToken.isNullOrBlank()) return defaultValue
        val parts = storedToken.split(".")
        if (parts.size != 2) return defaultValue

        val cipherBase64 = parts[0]
        val expectedChecksum = parts[1]

        return runCatching {
            val xored = Base64.decode(cipherBase64, Base64.NO_WRAP)
            val keyBytes = deriveKeyBytes(deviceFingerprint)
            val plainBytes = ByteArray(xored.size) { i ->
                (xored[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            }
            val decoded = String(plainBytes, Charsets.UTF_8)
            val prefix = "$keyName:"
            if (!decoded.startsWith(prefix)) return defaultValue
            val numberStr = decoded.removePrefix(prefix)
            val parsedValue = numberStr.toInt()

            if (verifyChecksum(keyName, parsedValue.toString(), expectedChecksum, deviceFingerprint)) {
                parsedValue
            } else {
                defaultValue
            }
        }.getOrDefault(defaultValue)
    }

    fun computeChecksum(key: String, value: String, deviceFingerprint: String): String {
        val payload = "$key|$value|$deviceFingerprint|$SECRET_SALT"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyChecksum(
        key: String,
        value: String,
        expectedChecksum: String?,
        deviceFingerprint: String
    ): Boolean {
        if (expectedChecksum.isNullOrBlank()) return false
        val actual = computeChecksum(key, value, deviceFingerprint)
        return actual.equals(expectedChecksum, ignoreCase = true)
    }
}
