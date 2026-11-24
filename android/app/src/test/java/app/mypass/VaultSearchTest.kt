package app.mypass

import org.junit.Assert.assertEquals
import org.junit.Test

class VaultSearchTest {
    @Test
    fun findsItemsIgnoringCaseAndAccents() {
        val medical = item("medical", "Carnet de José", medical = UiMedicalCard(holder = "José Pérez"))
        val credential = item("credential", "Banco", credential = UiCredential(
            category = "login",
            categoryLabel = "",
            categoryIcon = "",
            group = "",
            logo = null,
            appearanceMode = "none",
            appearanceColor = "#1B4332",
            appearanceImage = null,
            username = "jose@example.com",
            password = "secret",
            url = "",
            notes = "",
            tags = emptyList(),
            favorite = false,
            passwordChangedAt = null,
            totpSecret = null,
            totpAlgorithm = "SHA1",
            totpDigits = 6,
            totpPeriod = 30,
        ))

        val hits = VaultSearch.search(listOf(medical, credential), "jose")

        assertEquals(listOf("credential", "medical"), hits.map { it.item.id })
    }

    @Test
    fun ignoresTrashedItemsAndRanksTitleMatchesFirst() {
        val trashed = item("trashed", "Vuelo viejo").copy(state = "trashed")
        val other = item("other", "Documento con vuelo dentro", original = "vuelo.pdf")
        val title = item("title", "Vuelo Madrid")

        val hits = VaultSearch.search(listOf(trashed, other, title), "vuelo")

        assertEquals(listOf("title", "other"), hits.map { it.item.id })
    }

    private fun item(
        id: String,
        title: String,
        original: String = "$id.pdf",
        credential: UiCredential? = null,
        medical: UiMedicalCard? = null,
    ): UiItem {
        return UiItem(
            id = id,
            type = if (credential != null) "credential" else if (medical != null) "medical_card" else "document",
            title = title,
            mime = "application/pdf",
            originalName = original,
            pendingUpload = false,
            blobId = null,
            localPath = null,
            expiresAt = null,
            credential = credential,
            medical = medical,
        )
    }
}
