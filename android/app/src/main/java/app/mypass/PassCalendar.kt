package app.mypass

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

object PassCalendar {
    private data class Timing(
        val startMillis: Long,
        val endMillis: Long,
        val allDay: Boolean,
    )

    private fun timing(pass: UiPass): Timing? {
        val rawDate = pass.fields["date"].orEmpty().take(10)
        val date = runCatching { LocalDate.parse(rawDate) }.getOrNull() ?: return null
        val rawTime = pass.fields["departure"]
            ?: pass.fields["time"]
            ?: pass.fields["boarding_start"]
        val time = rawTime?.let { value ->
            runCatching { LocalTime.parse(value.take(5)) }.getOrNull()
        }
        val zone = ZoneId.systemDefault()

        if (time == null) {
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return Timing(start, end, true)
        }

        val startDateTime = LocalDateTime.of(date, time)
        val start = startDateTime.atZone(zone).toInstant().toEpochMilli()
        val end = startDateTime.plusHours(2).atZone(zone).toInstant().toEpochMilli()
        return Timing(start, end, false)
    }

    fun canAdd(pass: UiPass): Boolean = timing(pass) != null

    fun add(context: Context, item: UiItem, pass: UiPass): Boolean {
        val timing = timing(pass) ?: return false
        val location = PassLocation.query(pass)
        val details = buildList {
            pass.issuer.takeIf { it.isNotBlank() }?.let(::add)
            pass.holder.takeIf { it.isNotBlank() }?.let(::add)
            pass.fields["flight"]?.takeIf { it.isNotBlank() }?.let { add(context.getString(R.string.flight) + " " + it) }
            pass.fields["gate"]?.takeIf { it.isNotBlank() }?.let { add(context.getString(R.string.gate) + " " + it) }
            pass.fields["terminal"]?.takeIf { it.isNotBlank() }?.let { add(context.getString(R.string.terminal) + " " + it) }
            pass.fields["seat"]?.takeIf { it.isNotBlank() }?.let { add(context.getString(R.string.seat) + " " + it) }
            addAll(pass.notes.take(4))
        }.joinToString("\n")

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, pass.title.ifBlank { context.getString(R.string.pass) })
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, timing.startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, timing.endMillis)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, timing.allDay)
            if (location != null) {
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            }
            if (details.isNotBlank()) {
                putExtra(CalendarContract.Events.DESCRIPTION, details)
            }
            putExtra(CalendarContract.Events.UID_2445, "${item.id}@mypass")
        }

        return runCatching {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.add_to_calendar)))
            true
        }.getOrDefault(false)
    }

    fun filename(pass: UiPass): String {
        val base = pass.title.ifBlank { "pase" }
            .replace(Regex("[^A-Za-z0-9]+"), "-")
            .trim('-')
            .take(48)
            .ifBlank { "pase" }
        return "$base.ics"
    }

    fun timezoneLabel(): String = ZoneId.systemDefault().id.lowercase(Locale.ROOT)
}
