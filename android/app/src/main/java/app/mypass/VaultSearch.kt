package app.mypass

import java.text.Normalizer
import java.util.Locale

data class SearchHit(
    val item: UiItem,
    val section: String,
)

object VaultSearch {
    fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.getDefault())
            .trim()
    }

    fun section(item: UiItem): String = when {
        item.credential != null -> "credential"
        item.medical != null -> "medical"
        item.pass != null -> "pass"
        else -> "document"
    }

    fun search(items: List<UiItem>, query: String): List<SearchHit> {
        val normalized = normalize(query)
        if (normalized.isBlank()) {
            return emptyList()
        }

        return items
            .asSequence()
            .filter { it.state != "trashed" && it.state != "transferred" }
            .mapNotNull { item ->
                val text = normalize(searchText(item))
                if (!text.contains(normalized)) {
                    return@mapNotNull null
                }
                SearchHit(item, section(item))
            }
            .sortedWith(
                compareBy<SearchHit> {
                    if (normalize(it.item.title).startsWith(normalized)) 0 else 1
                }.thenBy { normalize(it.item.title) },
            )
            .take(50)
            .toList()
    }

    fun recent(items: List<UiItem>, ids: List<String>): List<SearchHit> {
        return ids.mapNotNull { id ->
            items.firstOrNull { it.id == id && it.state != "trashed" }
                ?.let { SearchHit(it, section(it)) }
        }.take(12)
    }

    private fun searchText(item: UiItem): String {
        val values = mutableListOf(
            item.title,
            item.originalName,
            item.ocrText,
            item.type,
        )

        item.pass?.let { pass ->
            values += pass.issuer
            values += pass.holder
            values += pass.notes
            values += pass.fields.values
        }

        item.credential?.let { credential ->
            values += credential.categoryLabel
            values += credential.group
            values += credential.username
            values += credential.url
            values += credential.notes
            values += credential.tags
        }

        item.medical?.let { medical ->
            values += medical.holder
            values += medical.bloodType
            values += medical.notes
            values += medical.allergies
            values += medical.conditions
            values += medical.medications
            values += medical.medicationSchedule.flatMap { listOf(it.name, it.dose, it.times.joinToString(" ")) }
            values += medical.appointments.flatMap { listOf(it.title, it.location, it.notes) }
            values += medical.insurance.provider
            values += medical.insurance.plan
            values += medical.insurance.policyNumber
            values += medical.doctor.name
            values += medical.doctor.clinic
            values += medical.contacts.flatMap { listOf(it.name, it.relation, it.phone) }
        }

        return values.filter { it.isNotBlank() }.joinToString(" ")
    }
}
