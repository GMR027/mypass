package app.mypass

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

class ApiException(val statusCode: Int, message: String) : Exception(message)

object ApiClient {
    private val BASE_URL = BuildConfig.API_BASE_URL.trimEnd('/')

    fun login(email: String, password: String): JSONObject {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("device_name", "Android")
        val response = request("POST", "/auth/login", null, body.toString().toByteArray(), "application/json")
        return JSONObject(response)
    }

    fun me(token: String): JSONObject {
        val response = request("GET", "/me", token, null, null)
        return JSONObject(response).getJSONObject("user")
    }

    fun uploadAvatar(token: String, bytes: ByteArray): JSONObject {
        val boundary = "mypass-${UUID.randomUUID()}"
        val output = ByteArrayOutputStream()

        fun write(value: String) = output.write(value.toByteArray(Charsets.UTF_8))

        write("--$boundary\r\n")
        write("Content-Disposition: form-data; name=\"avatar\"; filename=\"avatar.webp\"\r\n")
        write("Content-Type: image/webp\r\n\r\n")
        output.write(bytes)
        write("\r\n--$boundary--\r\n")

        val response = request(
            "POST",
            "/me/avatar",
            token,
            output.toByteArray(),
            "multipart/form-data; boundary=$boundary",
        )
        return JSONObject(response)
    }

    fun deleteAvatar(token: String) {
        request("DELETE", "/me/avatar", token, null, null)
    }

    fun downloadUrl(url: String, maxBytes: Int = 5 * 1024 * 1024): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("Accept", "image/*")

        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw ApiException(status, "No se pudo descargar la foto de perfil.")
            }

            if (connection.contentLengthLong > maxBytes) {
                throw ApiException(413, "La foto de perfil es demasiado grande.")
            }

            return connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0

                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) {
                        break
                    }
                    total += read
                    if (total > maxBytes) {
                        throw ApiException(413, "La foto de perfil es demasiado grande.")
                    }
                    output.write(buffer, 0, read)
                }

                output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    fun verifyTwoFactor(challengeToken: String, code: String, method: String): JSONObject {
        val body = JSONObject()
            .put("challenge_token", challengeToken)
            .put("code", code)
            .put("method", method)
        val response = request("POST", "/auth/two-factor", null, body.toString().toByteArray(), "application/json")
        return JSONObject(response)
    }

    fun sendTwoFactorEmail(challengeToken: String): JSONObject {
        val body = JSONObject().put("challenge_token", challengeToken)
        val response = request("POST", "/auth/two-factor/email", null, body.toString().toByteArray(), "application/json")
        return JSONObject(response)
    }

    fun joinPairing(token: String, code: String, publicKey: String, deviceName: String): String {
        val body = JSONObject()
            .put("code", code)
            .put("public_key", publicKey)
            .put("device_name", deviceName)
        val response = request("POST", "/pairing/join", token, body.toString().toByteArray(), "application/json")
        return JSONObject(response).getJSONObject("data").getString("id")
    }

    fun pollPairing(token: String, sessionId: String): JSONObject {
        val response = request("GET", "/pairing/$sessionId", token, null, null)
        return JSONObject(response).getJSONObject("data")
    }

    fun listItems(token: String): JSONArray {
        val response = request("GET", "/items", token, null, null)
        return JSONObject(response).getJSONArray("data")
    }

    fun listNotifications(token: String): JSONObject {
        val response = request("GET", "/notifications", token, null, null)
        return JSONObject(response)
    }

    fun markNotificationRead(token: String, id: String) {
        request("POST", "/notifications/$id/read", token, null, null)
    }

    fun markAllNotificationsRead(token: String) {
        request("POST", "/notifications/read-all", token, null, null)
    }

    fun createItem(
        token: String,
        id: String,
        type: String,
        encryptedMetadata: String,
        blobId: String?,
        expiresAt: String?,
    ) {
        val body = JSONObject()
            .put("id", id)
            .put("type", type)
            .put("encrypted_metadata", encryptedMetadata)
        if (blobId != null) {
            body.put("blob_id", blobId)
        }
        if (expiresAt != null) {
            body.put("expires_at", expiresAt)
        }
        request("POST", "/items", token, body.toString().toByteArray(), "application/json")
    }

    fun uploadBlob(token: String, bytes: ByteArray): String {
        val boundary = "mypass-${UUID.randomUUID()}"
        val output = ByteArrayOutputStream()

        fun write(value: String) = output.write(value.toByteArray(Charsets.UTF_8))

        write("--$boundary\r\n")
        write("Content-Disposition: form-data; name=\"file\"; filename=\"blob.bin\"\r\n")
        write("Content-Type: application/octet-stream\r\n\r\n")
        output.write(bytes)
        write("\r\n--$boundary--\r\n")

        val response = request(
            "POST",
            "/blobs",
            token,
            output.toByteArray(),
            "multipart/form-data; boundary=$boundary",
        )
        return JSONObject(response).getJSONObject("data").getString("id")
    }

    fun downloadBlob(token: String, id: String): ByteArray {
        val connection = openConnection("GET", "/blobs/$id", token)
        val status = connection.responseCode

        if (status !in 200..299) {
            val error = connection.errorStream?.readBytes()?.toString(Charsets.UTF_8)
            throw ApiException(status, error ?: "No se pudo descargar el archivo.")
        }

        return connection.inputStream.use { it.readBytes() }
    }

    fun deleteItem(token: String, id: String) {
        request("DELETE", "/items/$id", token, null, null)
    }

    fun getKeys(token: String): JSONObject? {
        val response = request("GET", "/keys", token, null, null)
        return JSONObject(response).optJSONObject("data")
    }

    fun listContacts(token: String): JSONArray {
        val response = request("GET", "/contacts", token, null, null)
        return JSONObject(response).getJSONArray("data")
    }

    fun addContact(token: String, email: String) {
        val body = JSONObject().put("email", email)
        request("POST", "/contacts", token, body.toString().toByteArray(), "application/json")
    }

    fun acceptContact(token: String, id: String) {
        request("POST", "/contacts/$id/accept", token, null, null)
    }

    fun blockContact(token: String, id: String) {
        request("POST", "/contacts/$id/block", token, null, null)
    }

    fun contactPublicKey(token: String, id: String): String {
        val response = request("GET", "/contacts/$id/public-key", token, null, null)
        return JSONObject(response).getJSONObject("data").getString("public_key")
    }

    fun listShares(token: String): JSONObject {
        val response = request("GET", "/shares", token, null, null)
        return JSONObject(response)
    }

    fun getShare(token: String, id: String): JSONObject {
        val response = request("GET", "/shares/$id", token, null, null)
        return JSONObject(response).getJSONObject("data")
    }

    fun acceptShare(token: String, id: String, recipientItemId: String) {
        val body = JSONObject().put("recipient_item_id", recipientItemId)
        request("POST", "/shares/$id/accept", token, body.toString().toByteArray(), "application/json")
    }

    fun declineShare(token: String, id: String) {
        request("POST", "/shares/$id/decline", token, null, null)
    }

    fun revokeShare(token: String, id: String) {
        request("POST", "/shares/$id/revoke", token, null, null)
    }

    fun listShareRequests(token: String): JSONObject {
        val response = request("GET", "/share-requests", token, null, null)
        return JSONObject(response)
    }

    fun createShareRequest(token: String, contactId: String, documentType: String, purpose: String) {
        val body = JSONObject()
            .put("contact_id", contactId)
            .put("document_type", documentType)
            .put("purpose", purpose)
        request("POST", "/share-requests", token, body.toString().toByteArray(), "application/json")
    }

    fun declineShareRequest(token: String, id: String) {
        request("POST", "/share-requests/$id/decline", token, null, null)
    }

    fun cancelShareRequest(token: String, id: String) {
        request("POST", "/share-requests/$id/cancel", token, null, null)
    }

    fun createShare(
        token: String,
        contactId: String,
        itemId: String,
        mode: String,
        encryptedPackage: String,
        expiresAt: String?,
    ) {
        val body = JSONObject()
            .put("contact_id", contactId)
            .put("item_id", itemId)
            .put("mode", mode)
            .put("encrypted_package", encryptedPackage)
        if (expiresAt != null) {
            body.put("expires_at", expiresAt)
        }
        request("POST", "/shares", token, body.toString().toByteArray(), "application/json")
    }

    fun updateItem(
        token: String,
        id: String,
        encryptedMetadata: String,
        expiresAt: String?,
        version: Int,
        blobId: String? = null,
        updateBlob: Boolean = false,
    ) {
        val body = JSONObject()
            .put("encrypted_metadata", encryptedMetadata)
            .put("expires_at", expiresAt ?: JSONObject.NULL)
            .put("version", version)
        if (updateBlob) {
            body.put("blob_id", blobId ?: JSONObject.NULL)
        }
        request("PUT", "/items/$id", token, body.toString().toByteArray(), "application/json")
    }

    fun getDevice(token: String, id: String): JSONObject {
        val response = request("GET", "/devices/$id", token, null, null)
        return JSONObject(response).getJSONObject("data")
    }

    fun updateDevice(
        token: String,
        id: String,
        pinSalt: String? = null,
        pinParams: JSONObject? = null,
        biometricWrappedMasterKey: String? = null,
    ) {
        val body = JSONObject()
        if (pinSalt != null) {
            body.put("pin_salt", pinSalt)
        }
        if (pinParams != null) {
            body.put("pin_params", pinParams)
        }
        if (biometricWrappedMasterKey != null) {
            body.put("biometric_wrapped_master_key", biometricWrappedMasterKey)
        }
        request("PATCH", "/devices/$id", token, body.toString().toByteArray(), "application/json")
    }

    fun registerDevice(
        token: String,
        publicKey: String,
        wrappedMasterKey: String,
        pinSalt: String,
        opsLimit: Long,
        memLimit: Long,
    ): String {
        val params = JSONObject()
            .put("ops_limit", opsLimit)
            .put("mem_limit", memLimit)
            .put("algorithm", "argon2id13")
        val body = JSONObject()
            .put("name", "Tablet Android")
            .put("platform", "android")
            .put("public_key", publicKey)
            .put("wrapped_master_key", wrappedMasterKey)
            .put("pin_salt", pinSalt)
            .put("pin_params", params)
            .put("app_version", "0.3.0")
        val response = request("POST", "/devices", token, body.toString().toByteArray(), "application/json")
        return JSONObject(response).getJSONObject("data").getString("id")
    }

    fun toBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun request(
        method: String,
        path: String,
        token: String?,
        body: ByteArray?,
        contentType: String?,
    ): String {
        val connection = openConnection(method, path, token)

        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", contentType ?: "application/octet-stream")
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.readBytes()?.toString(Charsets.UTF_8) ?: ""

        if (status !in 200..299) {
            val message = runCatching {
                JSONObject(response).optString("message", "Error del servidor.")
            }.getOrDefault("Error del servidor.")
            throw ApiException(status, message)
        }

        return response
    }

    private fun openConnection(method: String, path: String, token: String?): HttpURLConnection {
        val connection = URL(BASE_URL + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty(
            "Accept-Language",
            if (Locale.getDefault().language == "en") "en" else "es-MX",
        )
        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        return connection
    }
}
