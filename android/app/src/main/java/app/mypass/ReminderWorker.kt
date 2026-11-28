package app.mypass

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

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
            if (item.state == "trashed" || item.type == "insurance") {
                continue
            }
            val days = expiryDays(item.expiresAt, today) ?: continue
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
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
            },
        )

        val title = when {
            expired > 0 && soon > 0 -> context.getString(R.string.reminder_title_both)
            expired > 0 -> context.getString(R.string.reminder_title_expired)
            else -> context.getString(R.string.reminder_title_soon)
        }
        val message = when {
            expired > 0 && soon > 0 -> context.getString(R.string.reminder_body_both, expired, soon)
            expired > 0 -> context.getString(R.string.reminder_body_expired, expired)
            else -> context.getString(R.string.reminder_body_soon, soon)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.mypass_wallet)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.open_vault), pendingIntent)
            .build()

        manager.notify(2001, notification)
        return Result.success()
    }
}
