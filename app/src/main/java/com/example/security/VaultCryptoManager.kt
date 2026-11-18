package com.example.security

import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultCryptoManager {
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128

    private val random = SecureRandom()

    fun generateSaltBase64(): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun generateRecoveryCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val groups = (0 until 3).map {
            (0 until 4).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        }
        return "VRN-${groups.joinToString("-")}"
    }

    fun hashPin(pin: String, saltBase64: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Base64.decode(saltBase64, Base64.NO_WRAP))
        val hashed = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashed, Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, expectedHash: String, saltBase64: String): Boolean {
        if (expectedHash.isBlank() || saltBase64.isBlank()) return false
        return MessageDigest.isEqual(
            hashPin(pin, saltBase64).toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun sha256Preview(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }

    fun encryptJsonPayload(plainJson: String, passphrase: String): String {
        require(passphrase.length >= 4) { "Passphrase must be at least 4 characters." }
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(GCM_IV_LENGTH).also { random.nextBytes(it) }
        val secretKey = deriveAesKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))

        return JSONObject().apply {
            put("format", "varnika-encrypted-backup-v1")
            put("alg", "AES-256-GCM")
            put("kdf", "PBKDF2WithHmacSHA256")
            put("iterations", PBKDF2_ITERATIONS)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }.toString(2)
    }

    fun decryptJsonPayload(envelopeJson: String, passphrase: String): String {
        val envelope = JSONObject(envelopeJson)
        val salt = Base64.decode(envelope.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
        val ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP)
        val secretKey = deriveAesKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, Charsets.UTF_8)
    }

    private fun deriveAesKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
