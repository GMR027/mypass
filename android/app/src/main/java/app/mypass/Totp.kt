package app.mypass

import java.net.URLDecoder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class TotpConfig(
    val secret: String,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
)

data class TotpUri(
    val config: TotpConfig,
    val issuer: String = "",
    val account: String = "",
)

object Totp {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun parse(value: String): TotpConfig? = parseUri(value)?.config

    fun parseUri(value: String): TotpUri? {
        val text = value.trim()
        if (text.isBlank()) {
            return null
        }

        if (!text.startsWith("otpauth://", ignoreCase = true)) {
            val secret = normalize(text) ?: return null
            return TotpUri(TotpConfig(secret = secret))
        }

        return runCatching {
            val uri = java.net.URI(text)
            if (!uri.host.equals("totp", ignoreCase = true)) {
                return null
            }
            val params = uri.rawQuery.orEmpty()
                .split('&')
                .filter { it.contains('=') }
                .associate {
                    val key = URLDecoder.decode(it.substringBefore('='), "UTF-8")
                    val value = URLDecoder.decode(it.substringAfter('='), "UTF-8")
                    key to value
                }
            val secret = normalize(params["secret"].orEmpty()) ?: return null
            val label = runCatching {
                URLDecoder.decode(uri.path.orEmpty().removePrefix("/"), "UTF-8")
            }.getOrDefault("")
            val labelIssuer = label.substringBefore(":", "").trim()
            val labelAccount = if (label.contains(':')) label.substringAfter(':').trim() else label.trim()
            val issuer = params["issuer"].orEmpty().trim().ifBlank { labelIssuer }
            val account = labelAccount.ifBlank {
                if (issuer.isBlank()) label.trim() else ""
            }
            TotpUri(
                config = TotpConfig(
                    secret = secret,
                    algorithm = params["algorithm"].orEmpty().uppercase()
                        .takeIf { it in setOf("SHA1", "SHA256", "SHA512") } ?: "SHA1",
                    digits = params["digits"]?.toIntOrNull()?.coerceIn(4, 10) ?: 6,
                    period = params["period"]?.toIntOrNull()?.coerceIn(10, 120) ?: 30,
                ),
                issuer = issuer,
                account = account,
            )
        }.getOrNull()
    }

    fun normalize(value: String): String? {
        val secret = value.replace(Regex("[\\s-]+"), "").trimEnd('=').uppercase()
        if (secret.isEmpty() || secret.any { it !in ALPHABET }) {
            return null
        }
        return secret
    }

    fun code(config: TotpConfig, timestamp: Long = System.currentTimeMillis()): String? {
        val key = decodeBase32(config.secret) ?: return null
        val counter = timestamp / 1000 / config.period.coerceAtLeast(1)
        val counterBytes = ByteArray(8)
        var value = counter
        for (index in 7 downTo 0) {
            counterBytes[index] = (value and 0xff).toByte()
            value = value ushr 8
        }

        val algorithm = when (config.algorithm.uppercase()) {
            "SHA256" -> "HmacSHA256"
            "SHA512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(key, algorithm))
        val hash = mac.doFinal(counterBytes)
        val offset = hash.last().toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
            ((hash[offset + 1].toInt() and 0xff) shl 16) or
            ((hash[offset + 2].toInt() and 0xff) shl 8) or
            (hash[offset + 3].toInt() and 0xff)
        var modulo = 1
        repeat(config.digits) { modulo *= 10 }
        return (binary % modulo).toString().padStart(config.digits, '0')
    }

    fun secondsLeft(period: Int, timestamp: Long = System.currentTimeMillis()): Int {
        val safePeriod = period.coerceAtLeast(1)
        return safePeriod - ((timestamp / 1000) % safePeriod).toInt()
    }

    fun codeFromInput(value: String, timestamp: Long = System.currentTimeMillis()): String? {
        return parse(value)?.let { code(it, timestamp) }
    }

    private fun decodeBase32(value: String): ByteArray? {
        var bits = 0
        var buffer = 0
        val bytes = mutableListOf<Byte>()

        for (char in value) {
            val index = ALPHABET.indexOf(char)
            if (index < 0) {
                return null
            }
            buffer = (buffer shl 5) or index
            bits += 5
            if (bits >= 8) {
                bits -= 8
                bytes.add(((buffer ushr bits) and 0xff).toByte())
            }
        }

        return bytes.toByteArray()
    }

    fun sha256(value: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(value)
}
