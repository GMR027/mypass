package app.mypass

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal fun parseExpiryDate(value: String?): LocalDate? {
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

internal fun expiryDays(value: String?, today: LocalDate = LocalDate.now()): Long? {
    val date = parseExpiryDate(value) ?: return null

    return ChronoUnit.DAYS.between(today, date)
}

internal fun expiryNotificationId(itemId: String, expiresAt: String): String {
    return "expiry:$itemId:$expiresAt"
}
