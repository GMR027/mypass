package app.mypass

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class LocalItem(
    val id: String,
    val type: String,
    val state: String,
    val encryptedMetadata: String,
    val blobId: String?,
    val version: Int,
    val expiresAt: String?,
    val localPath: String?,
    val pendingUpload: Boolean,
    val updatedAt: String,
)

class VaultStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("mypass_secure", Context.MODE_PRIVATE)
    private val db = VaultDatabase(appContext)
    private val tokenStore = TokenStore(appContext)

    val vaultDir: File = File(appContext.filesDir, "vault").apply { mkdirs() }

    fun hasVault(): Boolean = prefs.contains(KEY_WRAPPED_MASTER_KEY)

    fun savePinWrapped(
        wrapped: String,
        salt: String,
        opsLimit: Long,
        memLimit: Long,
    ) {
        prefs.edit()
            .putString(KEY_WRAPPED_MASTER_KEY, wrapped)
            .putString(KEY_PIN_SALT, salt)
            .putLong(KEY_OPS_LIMIT, opsLimit)
            .putLong(KEY_MEM_LIMIT, memLimit)
            .apply()
    }

    fun wrappedMasterKey(): String? = prefs.getString(KEY_WRAPPED_MASTER_KEY, null)
    fun pinSalt(): String? = prefs.getString(KEY_PIN_SALT, null)
    fun opsLimit(): Long = prefs.getLong(KEY_OPS_LIMIT, 0L)
    fun memLimit(): Long = prefs.getLong(KEY_MEM_LIMIT, 0L)

    fun biometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
            && prefs.contains(KEY_BIOMETRIC_CIPHERTEXT)
            && prefs.contains(KEY_BIOMETRIC_IV)
    }

    fun saveBiometricWrapped(ciphertext: String, iv: String) {
        prefs.edit()
            .putString(KEY_BIOMETRIC_CIPHERTEXT, ciphertext)
            .putString(KEY_BIOMETRIC_IV, iv)
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .apply()
    }

    fun biometricCiphertext(): String? = prefs.getString(KEY_BIOMETRIC_CIPHERTEXT, null)
    fun biometricIv(): String? = prefs.getString(KEY_BIOMETRIC_IV, null)

    fun clearBiometric() {
        prefs.edit()
            .remove(KEY_BIOMETRIC_CIPHERTEXT)
            .remove(KEY_BIOMETRIC_IV)
            .remove(KEY_BIOMETRIC_ENABLED)
            .apply()
        BiometricUnlock.deleteKey()
    }

    fun pinFailedAttempts(): Int = prefs.getInt(KEY_PIN_FAILED_ATTEMPTS, 0)

    fun pinLockUntil(): Long = prefs.getLong(KEY_PIN_LOCK_UNTIL, 0L)

    fun registerPinFailure(now: Long): Long {
        val attempts = pinFailedAttempts() + 1
        val lockSeconds = when {
            attempts < 5 -> 0L
            attempts == 5 -> 30L
            attempts == 6 -> 60L
            attempts == 7 -> 120L
            attempts == 8 -> 300L
            else -> 900L
        }
        val lockUntil = if (lockSeconds > 0) now + lockSeconds * 1_000 else 0L

        prefs.edit()
            .putInt(KEY_PIN_FAILED_ATTEMPTS, attempts)
            .putLong(KEY_PIN_LOCK_UNTIL, lockUntil)
            .apply()

        return lockUntil
    }

    fun clearPinFailures() {
        prefs.edit()
            .remove(KEY_PIN_FAILED_ATTEMPTS)
            .remove(KEY_PIN_LOCK_UNTIL)
            .apply()
    }

    fun deviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) {
            return existing
        }

        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    fun saveToken(token: String) = tokenStore.save(token)
    fun token(): String? = tokenStore.load()
    fun clearToken() = tokenStore.clear()

    fun saveServerDeviceId(id: String) {
        prefs.edit().putString(KEY_SERVER_DEVICE_ID, id).apply()
    }

    fun serverDeviceId(): String? = prefs.getString(KEY_SERVER_DEVICE_ID, null)

    fun simpleMode(): Boolean = prefs.getBoolean(KEY_SIMPLE_MODE, false)

    fun setSimpleMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMPLE_MODE, enabled).apply()
    }

    fun saveEmail(email: String) {
        prefs.edit().putString(KEY_EMAIL, email).apply()
    }

    fun email(): String = prefs.getString(KEY_EMAIL, "").orEmpty()

    fun saveName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun name(): String = prefs.getString(KEY_NAME, "").orEmpty()

    fun saveAvatarUrl(url: String) {
        prefs.edit().putString(KEY_AVATAR_URL, url).apply()
    }

    fun avatarUrl(): String = prefs.getString(KEY_AVATAR_URL, "").orEmpty()

    fun newItemId(): String = UUID.randomUUID().toString()

    fun vaultFile(id: String): File = File(vaultDir, "$id.mypd")

    fun writeVaultFile(id: String, bytes: ByteArray): File {
        val file = vaultFile(id)
        file.writeBytes(bytes)
        return file
    }

    fun upsert(item: LocalItem) {
        val values = ContentValues().apply {
            put("id", item.id)
            put("type", item.type)
            put("state", item.state)
            put("encrypted_metadata", item.encryptedMetadata)
            put("blob_id", item.blobId)
            put("version", item.version)
            put("expires_at", item.expiresAt)
            put("local_path", item.localPath)
            put("pending_upload", if (item.pendingUpload) 1 else 0)
            put("updated_at", item.updatedAt)
        }
        db.writableDatabase.insertWithOnConflict("items", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun allItems(): List<LocalItem> {
        val items = mutableListOf<LocalItem>()
        db.readableDatabase.rawQuery(
            "SELECT id, type, state, encrypted_metadata, blob_id, version, expires_at, local_path, pending_upload, updated_at " +
                "FROM items WHERE state != 'deleted' ORDER BY updated_at DESC",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                items.add(
                    LocalItem(
                        id = cursor.getString(0),
                        type = cursor.getString(1),
                        state = cursor.getString(2),
                        encryptedMetadata = cursor.getString(3),
                        blobId = cursor.getString(4),
                        version = cursor.getInt(5),
                        expiresAt = cursor.getString(6),
                        localPath = cursor.getString(7),
                        pendingUpload = cursor.getInt(8) == 1,
                        updatedAt = cursor.getString(9),
                    ),
                )
            }
        }
        return items
    }

    fun findItem(id: String): LocalItem? {
        db.readableDatabase.rawQuery(
            "SELECT id, type, state, encrypted_metadata, blob_id, version, expires_at, local_path, pending_upload, updated_at " +
                "FROM items WHERE id = ? LIMIT 1",
            arrayOf(id),
        ).use { cursor ->
            if (!cursor.moveToNext()) {
                return null
            }

            return LocalItem(
                id = cursor.getString(0),
                type = cursor.getString(1),
                state = cursor.getString(2),
                encryptedMetadata = cursor.getString(3),
                blobId = cursor.getString(4),
                version = cursor.getInt(5),
                expiresAt = cursor.getString(6),
                localPath = cursor.getString(7),
                pendingUpload = cursor.getInt(8) == 1,
                updatedAt = cursor.getString(9),
            )
        }
    }

    fun deleteItem(id: String) {
        val item = findItem(id)
        item?.localPath?.let { runCatching { File(it).delete() } }
        db.writableDatabase.delete("items", "id = ?", arrayOf(id))
    }

    fun pendingItems(): List<LocalItem> = allItems().filter { it.pendingUpload }

    fun markUploaded(id: String, blobId: String?) {
        val item = findItem(id) ?: return
        upsert(item.copy(blobId = blobId, pendingUpload = false, updatedAt = now()))
    }

    fun markSyncedFromServer(
        id: String,
        type: String,
        encryptedMetadata: String,
        blobId: String?,
        version: Int,
        expiresAt: String?,
        localPath: String?,
    ) {
        val existing = findItem(id)
        if (existing?.pendingUpload == true) {
            return
        }

        upsert(
            LocalItem(
                id = id,
                type = type,
                state = "active",
                encryptedMetadata = encryptedMetadata,
                blobId = blobId,
                version = version,
                expiresAt = expiresAt,
                localPath = localPath ?: existing?.localPath,
                pendingUpload = false,
                updatedAt = now(),
            ),
        )
    }

    fun clearAll() {
        db.writableDatabase.delete("items", null, null)
        vaultDir.listFiles()?.forEach { it.delete() }
        prefs.edit().clear().apply()
        tokenStore.clear()
        BiometricUnlock.deleteKey()
    }

    companion object {
        const val KEY_WRAPPED_MASTER_KEY = "wrapped_master_key"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_OPS_LIMIT = "ops_limit"
        const val KEY_MEM_LIMIT = "mem_limit"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_SERVER_DEVICE_ID = "server_device_id"
        const val KEY_SIMPLE_MODE = "simple_mode"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_BIOMETRIC_CIPHERTEXT = "biometric_ciphertext"
        const val KEY_BIOMETRIC_IV = "biometric_iv"
        const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        const val KEY_PIN_FAILED_ATTEMPTS = "pin_failed_attempts"
        const val KEY_PIN_LOCK_UNTIL = "pin_lock_until"

        fun now(): String = java.time.Instant.now().toString()
    }
}

private class VaultDatabase(context: Context) : SQLiteOpenHelper(context, "mypass.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE items (
                id TEXT PRIMARY KEY,
                type TEXT NOT NULL,
                state TEXT NOT NULL,
                encrypted_metadata TEXT NOT NULL,
                blob_id TEXT,
                version INTEGER NOT NULL,
                expires_at TEXT,
                local_path TEXT,
                pending_upload INTEGER NOT NULL DEFAULT 0,
                updated_at TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX items_updated_at ON items(updated_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS items")
        onCreate(db)
    }
}

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("mypass_token", Context.MODE_PRIVATE)

    fun save(token: String) {
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            val combined = cipher.iv + ciphertext
            prefs.edit().putString(KEY_TOKEN, Base64.encodeToString(combined, Base64.NO_WRAP)).apply()
        }
    }

    fun load(): String? {
        return runCatching {
            val value = prefs.getString(KEY_TOKEN, null) ?: return null
            val combined = Base64.decode(value, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
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
        private const val KEY_ALIAS = "mypass_token_key"
        private const val KEY_TOKEN = "token"
    }
}
