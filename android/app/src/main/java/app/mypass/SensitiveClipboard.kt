package app.mypass

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import java.security.SecureRandom

object SensitiveClipboard {
    private val random = SecureRandom()

    fun copy(context: Context, label: String, value: String) {
        if (value.isBlank()) {
            return
        }

        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(label, value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)

        Handler(Looper.getMainLooper()).postDelayed({
            runCatching {
                val current = clipboard.primaryClip
                val text = current?.getItemAt(0)?.coerceToText(context)?.toString()
                if (text == value) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        }, 45_000)
    }
}

object PasswordGenerator {
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?"
    private val random = SecureRandom()

    fun generate(length: Int = 24): String {
        val safeLength = length.coerceIn(8, 128)
        val sets = listOf(LOWER, UPPER, DIGITS, SYMBOLS)
        val all = sets.joinToString("")
        val chars = MutableList(safeLength) { all[random.nextInt(all.length)] }
        sets.forEachIndexed { index, set ->
            chars[index] = set[random.nextInt(set.length)]
        }
        for (index in chars.lastIndex downTo 1) {
            val swap = random.nextInt(index + 1)
            val value = chars[index]
            chars[index] = chars[swap]
            chars[swap] = value
        }
        return chars.joinToString("")
    }

    fun strength(password: String): Int {
        if (password.isBlank()) {
            return 0
        }
        var score = (password.length * 2.5).toInt().coerceAtMost(45)
        score += listOf(
            password.any(Char::isLowerCase),
            password.any(Char::isUpperCase),
            password.any(Char::isDigit),
            password.any { !it.isLetterOrDigit() },
        ).count { it } * 10
        if (password.length >= 20) {
            score += 10
        }
        if (password.length >= 28) {
            score += 10
        }
        return score.coerceIn(0, 100)
    }
}
