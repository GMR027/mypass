package app.mypass

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val minSupportedVersionCode: Int,
    val publishedAt: String?,
    val notes: Map<String, List<String>>,
) {
    val isMandatory: Boolean
        get() = BuildConfig.VERSION_CODE < minSupportedVersionCode

    fun notesFor(language: String): List<String> {
        val preferred = notes[language].orEmpty()
        return if (preferred.isNotEmpty()) preferred else notes["es"].orEmpty().ifEmpty { notes["en"].orEmpty() }
    }

    fun toStoredJson(): String {
        val notesJson = JSONObject()
        notes.forEach { (language, items) ->
            notesJson.put(language, org.json.JSONArray(items))
        }

        return JSONObject()
            .put("versionCode", versionCode)
            .put("versionName", versionName)
            .put("apkUrl", apkUrl)
            .put("sha256", sha256)
            .put("sizeBytes", sizeBytes)
            .put("minSupportedVersionCode", minSupportedVersionCode)
            .put("publishedAt", publishedAt ?: JSONObject.NULL)
            .put("notes", notesJson)
            .toString()
    }

    companion object {
        private val SHA256_PATTERN = Regex("^[a-fA-F0-9]{64}$")

        fun fromJson(json: JSONObject): AppRelease? {
            val versionCode = json.optInt("versionCode", 0)
            val versionName = json.optString("versionName").trim()
            val apkUrl = json.optString("apkUrl").trim()
            val sha256 = json.optString("sha256").trim()
            val sizeBytes = json.optLong("sizeBytes", 0L)
            val minSupportedVersionCode = json.optInt("minSupportedVersionCode", 0)

            if (versionCode <= 0 || versionName.isBlank()) {
                return null
            }
            if (!apkUrl.startsWith("https://") &&
                !apkUrl.startsWith("http://127.0.0.1") &&
                !apkUrl.startsWith("http://localhost")
            ) {
                return null
            }
            if (!SHA256_PATTERN.matches(sha256) || sizeBytes <= 0L) {
                return null
            }

            val notes = mutableMapOf<String, List<String>>()
            json.optJSONObject("notes")?.let { notesJson ->
                notesJson.keys().forEach { language ->
                    val items = notesJson.optJSONArray(language) ?: return@forEach
                    notes[language] = buildList {
                        for (index in 0 until items.length()) {
                            val item = items.optString(index).trim()
                            if (item.isNotEmpty()) {
                                add(item)
                            }
                        }
                    }
                }
            }

            return AppRelease(
                versionCode = versionCode,
                versionName = versionName,
                apkUrl = apkUrl,
                sha256 = sha256,
                sizeBytes = sizeBytes,
                minSupportedVersionCode = minSupportedVersionCode,
                publishedAt = json.optString("publishedAt").takeIf { it.isNotBlank() },
                notes = notes,
            )
        }
    }
}

sealed interface UpdateState {
    val release: AppRelease?

    data object Idle : UpdateState {
        override val release: AppRelease? = null
    }

    data object Checking : UpdateState {
        override val release: AppRelease? = null
    }

    data object UpToDate : UpdateState {
        override val release: AppRelease? = null
    }

    data class Available(override val release: AppRelease) : UpdateState

    data class Downloading(
        override val release: AppRelease,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : UpdateState {
        val progress: Int
            get() = if (totalBytes > 0L) {
                ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
            } else {
                0
            }
    }

    data class Ready(
        override val release: AppRelease,
        val path: String,
    ) : UpdateState

    data class NeedsPermission(
        override val release: AppRelease,
        val path: String,
    ) : UpdateState

    data class Failed(
        override val release: AppRelease?,
        val message: String,
    ) : UpdateState
}

class UpdateStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("mypass_updates", Context.MODE_PRIVATE)

    fun lastCheck(): Long = prefs.getLong(KEY_LAST_CHECK, 0L)

    fun setLastCheck(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK, timestamp).apply()
    }

    fun dismissedVersion(): Int = prefs.getInt(KEY_DISMISSED_VERSION, 0)

    fun dismiss(versionCode: Int) {
        prefs.edit().putInt(KEY_DISMISSED_VERSION, versionCode).apply()
    }

    fun savePending(release: AppRelease, path: String) {
        prefs.edit()
            .putString(KEY_PENDING_RELEASE, release.toStoredJson())
            .putString(KEY_PENDING_PATH, path)
            .apply()
    }

    fun pendingRelease(): AppRelease? {
        val raw = prefs.getString(KEY_PENDING_RELEASE, null) ?: return null
        return runCatching { AppRelease.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun pendingPath(): String? = prefs.getString(KEY_PENDING_PATH, null)

    fun clearPending() {
        prefs.edit()
            .remove(KEY_PENDING_RELEASE)
            .remove(KEY_PENDING_PATH)
            .apply()
    }

    private companion object {
        const val KEY_LAST_CHECK = "last_check"
        const val KEY_DISMISSED_VERSION = "dismissed_version"
        const val KEY_PENDING_RELEASE = "pending_release"
        const val KEY_PENDING_PATH = "pending_path"
    }
}

class NotificationStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("mypass_notifications", Context.MODE_PRIVATE)

    fun readExpiryIds(): Set<String> {
        return prefs.getStringSet(KEY_EXPIRY_READ, emptySet()).orEmpty().toSet()
    }

    fun markExpirySeen(ids: Set<String>) {
        if (ids.isEmpty()) {
            return
        }

        val updated = readExpiryIds() + ids
        prefs.edit().putStringSet(KEY_EXPIRY_READ, updated).apply()
    }

    fun promptedFamilyKeys(): Set<String> {
        return prefs.getStringSet(KEY_FAMILY_PROMPTED, emptySet()).orEmpty().toSet()
    }

    fun markFamilyPrompted(key: String) {
        prefs.edit()
            .putStringSet(KEY_FAMILY_PROMPTED, promptedFamilyKeys() + key)
            .apply()
    }

    fun lastNotifiedNotificationIds(): Set<String> {
        return prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()).orEmpty().toSet()
    }

    fun markNotificationsNotified(ids: Set<String>) {
        if (ids.isEmpty()) {
            return
        }

        prefs.edit()
            .putStringSet(KEY_NOTIFIED_IDS, lastNotifiedNotificationIds() + ids)
            .apply()
    }

    fun lastUpdateNotifiedCode(): Int = prefs.getInt(KEY_UPDATE_NOTIFIED, 0)

    fun markUpdateNotified(versionCode: Int) {
        prefs.edit().putInt(KEY_UPDATE_NOTIFIED, versionCode).apply()
    }

    private companion object {
        const val KEY_EXPIRY_READ = "expiry_read"
        const val KEY_FAMILY_PROMPTED = "family_prompted"
        const val KEY_NOTIFIED_IDS = "notified_ids"
        const val KEY_UPDATE_NOTIFIED = "update_notified"
    }
}

object AppUpdater {
    private const val MAX_APK_BYTES = 220L * 1024L * 1024L

    fun download(
        context: Context,
        release: AppRelease,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): File {
        val directory = File(context.filesDir, "updates").apply { mkdirs() }
        val target = File(directory, "mypass-${release.versionCode}.apk")
        val partial = File(directory, "${target.name}.part")
        partial.delete()
        target.delete()

        val connection = URL(release.apkUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("Accept", "application/vnd.android.package-archive")

        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("El servidor respondió HTTP $status.")
            }

            val declaredSize = connection.contentLengthLong
            if (declaredSize > MAX_APK_BYTES) {
                throw IOException("El APK es demasiado grande.")
            }

            val total = if (declaredSize > 0L) declaredSize else release.sizeBytes
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L

            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) {
                            break
                        }
                        downloaded += read
                        if (downloaded > MAX_APK_BYTES) {
                            throw IOException("El APK es demasiado grande.")
                        }
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        onProgress(downloaded, total)
                    }
                }
            }

            val computed = digest.digest().joinToString("") { "%02x".format(it) }
            if (!computed.equals(release.sha256, ignoreCase = true)) {
                throw IOException("La verificación de seguridad del APK falló.")
            }
            if (release.sizeBytes > 0L && downloaded != release.sizeBytes) {
                throw IOException("El tamaño del APK no coincide con el publicado.")
            }
            if (!partial.renameTo(target)) {
                throw IOException("No se pudo guardar el APK descargado.")
            }

            directory.listFiles()
                ?.filter { it != target }
                ?.forEach { it.delete() }

            return target
        } catch (error: Exception) {
            partial.delete()
            target.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun signatureMatches(context: Context, file: File, expectedVersionCode: Int): Boolean {
        return runCatching {
            val packageManager = context.packageManager
            @Suppress("DEPRECATION")
            val archive = packageManager.getPackageArchiveInfo(
                file.absolutePath,
                PackageManager.GET_SIGNING_CERTIFICATES,
            ) ?: return false
            @Suppress("DEPRECATION")
            val installed = packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )

            if (archive.packageName != context.packageName) {
                return false
            }
            if (archive.longVersionCode != expectedVersionCode.toLong()) {
                return false
            }

            val archiveSignatures = archive.signingInfo
                ?.apkContentsSigners
                ?.map { signatureHash(it.toByteArray()) }
                ?.toSet()
                .orEmpty()
            val installedSignatures = installed.signingInfo
                ?.apkContentsSigners
                ?.map { signatureHash(it.toByteArray()) }
                ?.toSet()
                .orEmpty()

            if (archiveSignatures.isEmpty() || installedSignatures.isEmpty()) {
                return true
            }

            archiveSignatures == installedSignatures
        }.getOrDefault(false)
    }

    fun canInstall(context: Context): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(intent) }
            .onFailure {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_SECURITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
    }

    fun install(context: Context, file: File): Boolean {
        if (!canInstall(context)) {
            return false
        }

        abandonStaleSessions(context)

        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                clipData = ClipData.newRawUri("mypass.apk", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun abandonStaleSessions(context: Context) {
        runCatching {
            val installer = context.packageManager.packageInstaller
            installer.mySessions.forEach { session ->
                runCatching { installer.abandonSession(session.sessionId) }
            }
        }
    }

    private fun signatureHash(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}


