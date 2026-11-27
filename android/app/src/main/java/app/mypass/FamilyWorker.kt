package app.mypass

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

object FamilyNotifier {
    private const val periodicWork = "mypass-family-periodic"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<FamilyWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            periodicWork,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

class FamilyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val token = VaultStore(context).token() ?: return Result.success()
        val store = NotificationStore(context)
        val seen = store.lastNotifiedNotificationIds()
        var notified = mutableSetOf<String>()

        runCatching { ApiClient.listNotifications(token) }
            .getOrNull()
            ?.optJSONArray("data")
            ?.let { array ->
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    val id = json.optString("id")
                    val type = json.optString("type")
                    val readAt = json.optString("read_at")
                    val unread = readAt.isBlank() || readAt == "null"

                    if (id.isBlank() || !unread || id in seen || type == "app_update") {
                        continue
                    }

                    if (type !in setOf(
                            "share_received",
                            "share_accepted",
                            "contact_request",
                            "contact_accepted",
                            "document_request",
                        )
                    ) {
                        continue
                    }

                    val data = json.optJSONObject("data") ?: org.json.JSONObject()
                    val from = data.optString("from").ifBlank {
                        context.getString(R.string.someone)
                    }
                    val (title, body, openFamily) = when (type) {
                        "share_received" -> Triple(
                            context.getString(R.string.notif_share_received, from),
                            context.getString(R.string.notification_tap_to_review),
                            true,
                        )
                        "share_accepted" -> Triple(
                            context.getString(R.string.notif_share_accepted, from),
                            context.getString(R.string.notification_tap_to_review),
                            true,
                        )
                        "contact_request" -> Triple(
                            context.getString(R.string.notif_contact_request, from),
                            context.getString(R.string.notification_tap_to_family),
                            true,
                        )
                        "contact_accepted" -> Triple(
                            context.getString(R.string.notif_contact_accepted, from),
                            context.getString(R.string.notification_tap_to_family),
                            true,
                        )
                        else -> Triple(
                            context.getString(
                                R.string.notif_document_request,
                                from,
                                data.optString("document_type").ifBlank {
                                    context.getString(R.string.document)
                                },
                            ),
                            context.getString(R.string.notification_tap_to_family),
                            true,
                        )
                    }

                    postNotification(
                        context = context,
                        channelId = CHANNEL_FAMILY,
                        channelName = context.getString(R.string.family_notification_channel),
                        notificationId = id.hashCode(),
                        title = title,
                        body = body,
                        action = if (openFamily) {
                            "app.mypass.action.FAMILY"
                        } else {
                            "app.mypass.action.NOTIFICATIONS"
                        },
                    )
                    notified += id
                }
            }

        if (notified.isNotEmpty()) {
            store.markNotificationsNotified(notified)
        }

        runCatching { ApiClient.appVersion() }
            .getOrNull()
            ?.let { json ->
                val release = AppRelease.fromJson(json)
                if (release != null &&
                    release.versionCode > BuildConfig.VERSION_CODE &&
                    store.lastUpdateNotifiedCode() != release.versionCode
                ) {
                    postNotification(
                        context = context,
                        channelId = CHANNEL_UPDATES,
                        channelName = context.getString(R.string.update_channel_name),
                        notificationId = 4000 + release.versionCode,
                        title = context.getString(R.string.notification_update_title, release.versionName),
                        body = context.getString(R.string.notification_update_body),
                        action = "app.mypass.action.NOTIFICATIONS",
                    )
                    store.markUpdateNotified(release.versionCode)
                }
            }

        return Result.success()
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        channelName: String,
        notificationId: Int,
        title: String,
        body: String,
        action: String,
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        manager.notify(notificationId, notification)
    }

    private companion object {
        const val CHANNEL_FAMILY = "mypass_family"
        const val CHANNEL_UPDATES = "mypass_updates"
    }
}
