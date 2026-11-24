package app.mypass

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AgendaReminder(
    val id: String,
    val kind: String,
    val itemId: String,
    val title: String,
    val body: String,
    val triggerAt: Long,
    val medication: MedicationReminder? = null,
)

data class MedicationReminder(
    val medicationId: String,
    val name: String,
    val dose: String,
    val times: List<String>,
    val startDate: String,
    val endDate: String,
)

class ReminderStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("mypass_agenda", Context.MODE_PRIVATE)

    fun all(): List<AgendaReminder> {
        val raw = prefs.getString(KEY_REMINDERS, null) ?: return emptyList()
        return runCatching {
            val json = JSONObject(decrypt(raw))
            val array = json.optJSONArray("reminders") ?: JSONArray()
            buildList {
                for (index in 0 until array.length()) {
                    val entry = array.optJSONObject(index) ?: continue
                    val medicationJson = entry.optJSONObject("medication")
                    val times = mutableListOf<String>()
                    medicationJson?.optJSONArray("times")?.let { values ->
                        for (timeIndex in 0 until values.length()) {
                            values.optString(timeIndex).takeIf { it.isNotBlank() }?.let(times::add)
                        }
                    }
                    add(
                        AgendaReminder(
                            id = entry.optString("id"),
                            kind = entry.optString("kind"),
                            itemId = entry.optString("item_id"),
                            title = entry.optString("title"),
                            body = entry.optString("body"),
                            triggerAt = entry.optLong("trigger_at"),
                            medication = medicationJson?.let {
                                MedicationReminder(
                                    medicationId = it.optString("medication_id"),
                                    name = it.optString("name"),
                                    dose = it.optString("dose"),
                                    times = times,
                                    startDate = it.optString("start_date"),
                                    endDate = it.optString("end_date"),
                                )
                            },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveAll(reminders: List<AgendaReminder>) {
        val array = JSONArray()
        for (reminder in reminders) {
            val entry = JSONObject()
                .put("id", reminder.id)
                .put("kind", reminder.kind)
                .put("item_id", reminder.itemId)
                .put("title", reminder.title)
                .put("body", reminder.body)
                .put("trigger_at", reminder.triggerAt)
            reminder.medication?.let { medication ->
                entry.put(
                    "medication",
                    JSONObject()
                        .put("medication_id", medication.medicationId)
                        .put("name", medication.name)
                        .put("dose", medication.dose)
                        .put("times", JSONArray(medication.times))
                        .put("start_date", medication.startDate)
                        .put("end_date", medication.endDate),
                )
            }
            array.put(entry)
        }
        val payload = JSONObject().put("reminders", array).toString()
        prefs.edit().putString(KEY_REMINDERS, encrypt(payload)).apply()
    }

    fun find(id: String): AgendaReminder? = all().firstOrNull { it.id == id }

    fun remove(id: String) {
        saveAll(all().filter { it.id != id })
    }

    fun clear() {
        prefs.edit().remove(KEY_REMINDERS).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val KEY_ALIAS = "mypass_agenda_key"
        private const val KEY_REMINDERS = "reminders"
    }
}
