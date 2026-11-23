package app.mypass

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PassSortingTest {
    @Test
    fun upcoming_passes_are_sorted_before_undated_and_recent_ones() {
        val today = LocalDate.now()
        val undated = item("undated", date = null, updatedAt = "2026-11-22T12:00:00Z")
        val later = item("later", date = today.plusDays(3).toString(), updatedAt = "2026-11-20T12:00:00Z")
        val sooner = item("sooner", date = today.plusDays(1).toString(), updatedAt = "2026-11-21T12:00:00Z")
        val past = item("past", date = today.minusDays(2).toString(), updatedAt = "2026-11-23T12:00:00Z")

        val sorted = sortActivePassItems(listOf(undated, later, sooner, past))

        assertEquals(listOf("sooner", "later", "past", "undated"), sorted.map { it.id })
    }

    private fun item(id: String, date: String?, updatedAt: String): UiItem {
        return UiItem(
            id = id,
            type = "ticket",
            title = id,
            mime = "application/pdf",
            originalName = "$id.pdf",
            pendingUpload = false,
            blobId = null,
            localPath = null,
            expiresAt = date,
            updatedAt = updatedAt,
            pass = UiPass(
                category = "event",
                title = id,
                issuer = "",
                holder = "",
                fields = mapOf("date" to date.orEmpty()),
                notes = emptyList(),
                codePayload = null,
                codeFormat = "qr",
                codeSource = "generated",
                themeColor = "#0c2340",
                options = emptyMap(),
            ),
        )
    }
}
