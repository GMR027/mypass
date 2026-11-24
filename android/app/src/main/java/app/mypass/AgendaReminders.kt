package app.mypass

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit

object AgendaReminders {
    const val TAG = "mypass-agenda"
    private const val TAG_WORK = "mypass-agenda-work"

    @Synchronized
    fun rebuild(context: Context, items: List<UiItem>) {
        val store = ReminderStore(context)
        val previous = store.all()
        previous.forEach { cancel(context, it) }

        val reminders = build(context, items, System.currentTimeMillis())
        store.saveAll(reminders)
        reminders.forEach { schedule(context, it) }
    }

    @Synchronized
    fun fire(context: Context, id: String) {
        val store = ReminderStore(context)
        val reminder = store.find(id) ?: return
        notify(context, reminder)

        val medication = reminder.medication
        if (medication != null) {
            val next = nextMedicationTime(medication, System.currentTimeMillis())
            if (next != null) {
                val updated = reminder.copy(triggerAt = next)
                store.saveAll(store.all().map { if (it.id == id) updated else it })
                schedule(context, updated)
            } else {
                store.remove(id)
            }
        } else {
            store.remove(id)
        }
    }

    @Synchronized
    fun rescheduleStored(context: Context) {
        val store = ReminderStore(context)
        val now = System.currentTimeMillis()
        val updated = mutableListOf<AgendaReminder>()

        for (reminder in store.all()) {
            if (reminder.triggerAt > now) {
                updated.add(reminder)
                schedule(context, reminder)
                continue
            }

            val medication = reminder.medication
            if (medication != null) {
                val next = nextMedicationTime(medication, now)
                if (next != null) {
                    val nextReminder = reminder.copy(triggerAt = next)
                    updated.add(nextReminder)
                    schedule(context, nextReminder)
                }
            }
        }

        store.saveAll(updated)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return manager.canScheduleExactAlarms()
    }

    private fun build(context: Context, items: List<UiItem>, now: Long): List<AgendaReminder> {
        val reminders = mutableListOf<AgendaReminder>()

        for (item in items) {
            if (item.state != "active" && item.state != "received") {
                continue
            }

            item.pass?.let { pass ->
                reminders.addAll(passReminders(context, item, pass, now))
            }

            item.medical?.let { medical ->
                for (medication in medical.medicationSchedule) {
                    if (medication.name.isBlank() || medication.times.isEmpty()) {
                        continue
                    }
                    val recurrence = MedicationReminder(
                        medicationId = medication.id.ifBlank { medication.name },
                        name = medication.name,
                        dose = medication.dose,
                        times = medication.times,
                        startDate = medication.startDate,
                        endDate = medication.endDate,
                    )
                    val trigger = nextMedicationTime(recurrence, now) ?: continue
                    reminders.add(
                        AgendaReminder(
                            id = "med-${item.id}-${recurrence.medicationId}",
                            kind = "medication",
                            itemId = item.id,
                            title = context.getString(R.string.medication_reminder_title, medication.name),
                            body = medication.dose.ifBlank { context.getString(R.string.medication_reminder_body) },
                            triggerAt = trigger,
                            medication = recurrence,
                        ),
                    )
                }

                for (appointment in medical.appointments) {
                    if (appointment.title.isBlank() || appointment.date.isBlank()) {
                        continue
                    }
                    val date = runCatching { LocalDate.parse(appointment.date.take(10)) }.getOrNull()
                        ?: continue
                    val time = runCatching { LocalTime.parse(appointment.time.take(5)) }.getOrNull()
                        ?: LocalTime.of(9, 0)
                    val start = LocalDateTime.of(date, time)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    val trigger = start - appointment.reminderMinutes * 60_000L
                    if (trigger <= now) {
                        continue
                    }
                    reminders.add(
                        AgendaReminder(
                            id = "appt-${item.id}-${appointment.id}",
                            kind = "appointment",
                            itemId = item.id,
                            title = context.getString(R.string.appointment_reminder_title, appointment.title),
                            body = listOf(appointment.date, appointment.time, appointment.location)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            triggerAt = trigger,
                        ),
                    )
                }
            }
        }

        return reminders.sortedBy { it.triggerAt }.take(120)
    }

    private fun passReminders(context: Context, item: UiItem, pass: UiPass, now: Long): List<AgendaReminder> {
        if (pass.options["reminder"] == false) {
            return emptyList()
        }
        val date = runCatching { LocalDate.parse(pass.fields["date"].orEmpty().take(10)) }.getOrNull()
            ?: return emptyList()
        val time = runCatching {
            LocalTime.parse(
                (pass.fields["departure"] ?: pass.fields["time"] ?: pass.fields["boarding_start"])
                    .orEmpty()
                    .take(5),
            )
        }.getOrNull() ?: LocalTime.of(9, 0)
        val start = LocalDateTime.of(date, time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val result = mutableListOf<AgendaReminder>()
        fun add(kind: String, minutes: Long, title: String, body: String) {
            val trigger = start - TimeUnit.MINUTES.toMillis(minutes)
            if (trigger > now) {
                result.add(
                    AgendaReminder(
                        id = "pass-${item.id}-$kind",
                        kind = kind,
                        itemId = item.id,
                        title = title,
                        body = body,
                        triggerAt = trigger,
                    ),
                )
            }
        }

        when (pass.category) {
            "flight" -> {
                add(
                    "checkin",
                    24 * 60,
                    context.getString(R.string.checkin_reminder_title, pass.title),
                    context.getString(R.string.checkin_reminder_body),
                )
                add("soon", 120, context.getString(R.string.flight_soon_title), pass.title)
            }
            "transit" -> add("soon", 120, context.getString(R.string.transit_soon_title), pass.title)
            else -> add("soon", 24 * 60, context.getString(R.string.pass_tomorrow_title), pass.title)
        }

        return result
    }

    private fun nextMedicationTime(medication: MedicationReminder, now: Long): Long? {
        val zone = ZoneId.systemDefault()
        val nowDate = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val nowTime = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalTime()
        val startDate = runCatching { LocalDate.parse(medication.startDate.take(10)) }.getOrNull()
        val endDate = runCatching { LocalDate.parse(medication.endDate.take(10)) }.getOrNull()
        val times = medication.times.mapNotNull { time ->
            runCatching { LocalTime.parse(time.take(5)) }.getOrNull()
        }.sorted()
        if (times.isEmpty()) {
            return null
        }

        var date = maxOf(nowDate, startDate ?: nowDate)
        repeat(14) {
            if (endDate != null && date.isAfter(endDate)) {
                return null
            }
            for (time in times) {
                val candidate = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
                if (candidate > now + 30_000) {
                    return candidate
                }
            }
            date = date.plusDays(1)
        }
        return null
    }

    private fun schedule(context: Context, reminder: AgendaReminder) {
        if (reminder.triggerAt <= System.currentTimeMillis()) {
            return
        }

        if (canScheduleExactAlarms(context)) {
            scheduleAlarm(context, reminder)
        } else {
            scheduleWork(context, reminder)
        }
    }

    private fun scheduleAlarm(context: Context, reminder: AgendaReminder) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, reminder.id)
        val scheduled = runCatching {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pending)
        }.isSuccess
        if (!scheduled) {
            scheduleWork(context, reminder)
        }
    }

    private fun scheduleWork(context: Context, reminder: AgendaReminder) {
        val delay = (reminder.triggerAt - System.currentTimeMillis()).coerceAtLeast(1_000L)
        val request = OneTimeWorkRequestBuilder<AgendaReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString("reminder_id", reminder.id).build())
            .addTag(TAG_WORK)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "agenda-${reminder.id}",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun cancel(context: Context, reminder: AgendaReminder) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pendingIntent(context, reminder.id))
        WorkManager.getInstance(context).cancelUniqueWork("agenda-${reminder.id}")
    }

    fun cancelAll(context: Context) {
        ReminderStore(context).all().forEach { cancel(context, it) }
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG_WORK)
        ReminderStore(context).clear()
    }

    private fun pendingIntent(context: Context, id: String): PendingIntent {
        val intent = Intent(context, AgendaAlarmReceiver::class.java)
            .putExtra("reminder_id", id)
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notify(context: Context, reminder: AgendaReminder) {
        if (
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = when (reminder.kind) {
            "medication" -> "mypass_medications"
            "appointment" -> "mypass_appointments"
            else -> "mypass_passes"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                when (reminder.kind) {
                    "medication" -> context.getString(R.string.reminder_channel_medications)
                    "appointment" -> context.getString(R.string.reminder_channel_appointments)
                    else -> context.getString(R.string.reminder_channel_passes)
                },
                if (reminder.kind == "medication") {
                    NotificationManager.IMPORTANCE_HIGH
                } else {
                    NotificationManager.IMPORTANCE_DEFAULT
                },
            ),
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "app.mypass.action.OPEN_ITEM"
            putExtra("item_id", reminder.itemId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val public = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_generic_title))
            .setContentText(context.getString(R.string.reminder_generic_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(reminder.title)
            .setContentText(reminder.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(reminder.id.hashCode(), notification)
    }
}
