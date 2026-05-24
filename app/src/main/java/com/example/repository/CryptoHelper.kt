package com.example.repository

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Lightweight cryptography helper using the native Android KeyStore
 * with AES/GCM/NoPadding for secure, hardware-backed encryption.
 *
 * Replaces the deprecated androidx.security:security-crypto library.
 */
object CryptoHelper {
    private const val TAG = "CryptoHelper"
    private const val PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "TennisBearerKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize KeyStore key", e)
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
            (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve secret key", e)
            null
        }
    }

    /**
     * Encrypt a plaintext string. Returns "iv:ciphertext" in Base64, or null on failure.
     */
    fun encrypt(plainText: String): String? {
        return try {
            val key = getSecretKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, key)
            }
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(cipherText, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            null
        }
    }

    /**
     * Decrypt a "iv:ciphertext" Base64 string back to plaintext. Returns null on failure.
     */
    fun decrypt(encryptedText: String): String? {
        return try {
            val parts = encryptedText.split(":")
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
            val key = getSecretKey() ?: return null

            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            }
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            null
        }
    }
}
