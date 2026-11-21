package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class LocalAccount(
    val id: String,
    val firstName: String,
    val lastName: String,
    val contact: String,
    val normalizedContact: String,
    val userId: String,
    val createdAt: Long
) {
    val fullName: String
        get() = "$firstName $lastName".trim()

    val isEmailContact: Boolean
        get() = contact.contains("@")
}

sealed interface AccountAuthResult {
    data class Success(val account: LocalAccount) : AccountAuthResult
    data object DuplicateAccount : AccountAuthResult
    data object InvalidCredentials : AccountAuthResult
    data object WeakPassword : AccountAuthResult
    data object InvalidInput : AccountAuthResult
}

class LocalAccountStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "yourplatform_accounts"
        private const val KEY_ACCOUNTS_JSON = "accounts_json"
        private const val KEY_ACTIVE_ACCOUNT_ID = "active_account_id"
        private const val PBKDF2_ITERATIONS = 15_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16
        private const val CAPTCHA_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val secureRandom = SecureRandom()

        fun normalizeContact(raw: String): String {
            val trimmed = raw.trim()
            return if (trimmed.contains("@")) {
                trimmed.lowercase(Locale.US)
            } else {
                trimmed.replace(Regex("[\\s\\-()]"), "")
            }
        }

        fun isValidContact(raw: String): Boolean {
            val normalized = normalizeContact(raw)
            if (normalized.isBlank()) return false
            return if (normalized.contains("@")) {
                Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(normalized)
            } else {
                Regex("^\\+?[0-9]{6,16}$").matches(normalized)
            }
        }

        fun generateVerificationCode(): String {
            val num = secureRandom.nextInt(900_000) + 100_000
            return num.toString()
        }

        fun generateCaptchaCode(length: Int = 5): String {
            return buildString(length) {
                repeat(length) {
                    append(CAPTCHA_ALPHABET[secureRandom.nextInt(CAPTCHA_ALPHABET.length)])
                }
            }
        }
    }

    fun registerAccount(
        firstName: String,
        lastName: String,
        contact: String,
        rawPassword: String
    ): AccountAuthResult {
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()
        val cleanContact = contact.trim()
        if (cleanFirst.isBlank() || cleanLast.isBlank() || !isValidContact(cleanContact)) {
            return AccountAuthResult.InvalidInput
        }
        if (rawPassword.length < 6) {
            return AccountAuthResult.WeakPassword
        }

        val normalized = normalizeContact(cleanContact)
        val existingArray = loadAccountsArray()
        for (i in 0 until existingArray.length()) {
            val obj = existingArray.optJSONObject(i) ?: continue
            if (obj.optString("normalizedContact") == normalized) {
                return AccountAuthResult.DuplicateAccount
            }
        }

        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val (alg, hashBytes) = hashPassword(rawPassword.toCharArray(), salt)

        val baseId = "${cleanFirst}_${cleanLast}"
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "user" }
        val userId = "${baseId}_${secureRandom.nextInt(9000) + 1000}"

        val account = LocalAccount(
            id = "local_${UUID.randomUUID()}",
            firstName = cleanFirst,
            lastName = cleanLast,
            contact = cleanContact,
            normalizedContact = normalized,
            userId = userId,
            createdAt = System.currentTimeMillis()
        )

        val accountJson = JSONObject().apply {
            put("id", account.id)
            put("firstName", account.firstName)
            put("lastName", account.lastName)
            put("contact", account.contact)
            put("normalizedContact", account.normalizedContact)
            put("userId", account.userId)
            put("createdAt", account.createdAt)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("passwordHash", Base64.encodeToString(hashBytes, Base64.NO_WRAP))
            put("hashAlg", alg)
        }
        existingArray.put(accountJson)

        prefs.edit()
            .putString(KEY_ACCOUNTS_JSON, existingArray.toString())
            .putString(KEY_ACTIVE_ACCOUNT_ID, account.id)
            .apply()

        return AccountAuthResult.Success(account)
    }

    fun authenticate(contact: String, rawPassword: String): AccountAuthResult {
        val normalized = normalizeContact(contact)
        if (normalized.isBlank() || rawPassword.isEmpty()) {
            return AccountAuthResult.InvalidInput
        }

        val existingArray = loadAccountsArray()
        for (i in 0 until existingArray.length()) {
            val obj = existingArray.optJSONObject(i) ?: continue
            if (obj.optString("normalizedContact") == normalized) {
                val saltB64 = obj.optString("salt", "")
                val hashB64 = obj.optString("passwordHash", "")
                val alg = obj.optString("hashAlg", "PBKDF2WithHmacSHA256")
                if (saltB64.isBlank() || hashB64.isBlank()) {
                    return AccountAuthResult.InvalidCredentials
                }
                val salt = Base64.decode(saltB64, Base64.NO_WRAP)
                val expectedHash = Base64.decode(hashB64, Base64.NO_WRAP)
                val candidateHash = computeHashWithAlg(rawPassword.toCharArray(), salt, alg)
                if (MessageDigest.isEqual(expectedHash, candidateHash)) {
                    val account = obj.toLocalAccount()
                    prefs.edit().putString(KEY_ACTIVE_ACCOUNT_ID, account.id).apply()
                    return AccountAuthResult.Success(account)
                }
                return AccountAuthResult.InvalidCredentials
            }
        }
        return AccountAuthResult.InvalidCredentials
    }

    fun getActiveAccount(): LocalAccount? {
        val activeId = prefs.getString(KEY_ACTIVE_ACCOUNT_ID, null)?.takeIf { it.isNotBlank() }
            ?: return null
        val existingArray = loadAccountsArray()
        for (i in 0 until existingArray.length()) {
            val obj = existingArray.optJSONObject(i) ?: continue
            if (obj.optString("id") == activeId) {
                return obj.toLocalAccount()
            }
        }
        return null
    }

    fun logoutLocalAccount() {
        prefs.edit().remove(KEY_ACTIVE_ACCOUNT_ID).apply()
    }

    private fun loadAccountsArray(): JSONArray {
        val raw = prefs.getString(KEY_ACCOUNTS_JSON, null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun JSONObject.toLocalAccount(): LocalAccount {
        return LocalAccount(
            id = getString("id"),
            firstName = optString("firstName", ""),
            lastName = optString("lastName", ""),
            contact = optString("contact", ""),
            normalizedContact = optString("normalizedContact", ""),
            userId = optString("userId", ""),
            createdAt = optLong("createdAt", 0L)
        )
    }

    private fun hashPassword(password: CharArray, salt: ByteArray): Pair<String, ByteArray> {
        return try {
            val bytes = computeHashWithAlg(password, salt, "PBKDF2WithHmacSHA256")
            "PBKDF2WithHmacSHA256" to bytes
        } catch (_: NoSuchAlgorithmException) {
            val bytes = computeHashWithAlg(password, salt, "PBKDF2WithHmacSHA1")
            "PBKDF2WithHmacSHA1" to bytes
        }
    }

    private fun computeHashWithAlg(password: CharArray, salt: ByteArray, algorithm: String): ByteArray {
        val factory = SecretKeyFactory.getInstance(algorithm)
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
