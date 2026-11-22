package app.mypass

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext

        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val today = LocalDate.now()
        var expired = 0
        var soon = 0

        for (item in VaultStore(context).allItems()) {
            val date = parseDate(item.expiresAt) ?: continue
            val days = ChronoUnit.DAYS.between(today, date)
            if (days < 0) {
                expired += 1
            } else if (days <= 7) {
                soon += 1
            }
        }

        if (expired == 0 && soon == 0) {
            return Result.success()
        }

        val channelId = "mypass_expiry"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Vencimiento de documentos",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Avisa cuando un documento está por vencer."
            },
        )

        val title = when {
            expired > 0 && soon > 0 -> "Documentos vencidos y por vencer"
            expired > 0 -> "Tienes documentos vencidos"
            else -> "Documentos por vencer"
        }
        val parts = buildList {
            if (expired > 0) {
                add("$expired vencido${if (expired == 1) "" else "s"}")
            }
            if (soon > 0) {
                add("$soon por vencer en los próximos 7 días")
            }
        }
        val message = parts.joinToString(" y ")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(2001, notification)
        return Result.success()
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) {
            return null
        }

        return runCatching {
            if (value.contains('T')) {
                OffsetDateTime.parse(value)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDate()
            } else {
                LocalDate.parse(value.take(10))
            }
        }.getOrNull()
    }
}
