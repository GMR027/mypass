package app.mypass

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.RSAKeyGenParameterSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min

enum class Screen {
    Loading,
    Login,
    Pair,
    PinSetup,
    TwoFactor,
    Locked,
    Vault,
    Family,
    Scan,
    Settings,
    Viewer,
}

data class UiContact(
    val id: String,
    val name: String,
    val email: String,
    val status: String,
)

data class UiShare(
    val id: String,
    val sender: String,
    val itemType: String,
    val mode: String,
    val expiresAt: String?,
)

data class UiShareRequest(
    val id: String,
    val incoming: Boolean,
    val person: String,
    val documentType: String,
    val purpose: String,
)

data class UiItem(
    val id: String,
    val type: String,
    val title: String,
    val mime: String,
    val originalName: String,
    val pendingUpload: Boolean,
    val blobId: String?,
    val localPath: String?,
    val expiresAt: String?,
    val ocrText: String = "",
)

data class UiState(
    val screen: Screen = Screen.Loading,
    val email: String = "",
    val error: String? = null,
    val status: String? = null,
    val busy: Boolean = false,
    val items: List<UiItem> = emptyList(),
    val contacts: List<UiContact> = emptyList(),
    val incomingShares: List<UiShare> = emptyList(),
    val shareRequests: List<UiShareRequest> = emptyList(),
    val familyStatus: String? = null,
    val twoFactorToken: String? = null,
    val twoFactorMethods: List<String> = emptyList(),
    val simpleMode: Boolean = false,
    val viewerPath: String? = null,
    val viewerMime: String? = null,
    val viewerName: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = VaultStore(application)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var masterKey: ByteArray? = null
    private var pendingMasterKey: ByteArray? = null
    private var pairingPrivateKey: PrivateKey? = null
    private var pairingPublicKey: String? = null
    private var pollJob: Job? = null
    private var viewerFile: File? = null
    private var identityPublicKey: ByteArray? = null
    private var identityPrivateKey: ByteArray? = null

    init {
        _state.value = UiState(
            screen = when {
                store.hasVault() -> Screen.Locked
                store.token() != null -> Screen.Pair
                else -> Screen.Login
            },
            simpleMode = store.simpleMode(),
        )
    }

    private fun text(id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun showError(message: String) {
        _state.value = _state.value.copy(error = message)
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = text(R.string.error_enter_credentials))
            return
        }

        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_signing_in))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ApiClient.login(email.trim(), password) }
                .onSuccess { body ->
                    val challengeToken = body.optString("challenge_token").takeIf { it.isNotBlank() }
                    if (body.optBoolean("two_factor_required") && challengeToken != null) {
                        val methods = mutableListOf<String>()
                        body.optJSONArray("methods")?.let { array ->
                            for (index in 0 until array.length()) {
                                methods.add(array.getString(index))
                            }
                        }
                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                screen = Screen.TwoFactor,
                                email = email.trim(),
                                busy = false,
                                error = null,
                                status = body.optString("message").takeIf { it.isNotBlank() },
                                twoFactorToken = challengeToken,
                                twoFactorMethods = methods,
                            )
                        }
                        return@onSuccess
                    }

                    val token = body.optString("token")
                    if (token.isBlank()) {
                        error(text(R.string.error_no_session))
                    }

                    store.saveToken(token)
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            screen = if (store.hasVault()) Screen.Locked else Screen.Pair,
                            email = email.trim(),
                            busy = false,
                            status = null,
                            twoFactorToken = null,
                            twoFactorMethods = emptyList(),
                        )
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            busy = false,
                            status = null,
                            error = error.message ?: text(R.string.error_login),
                        )
                    }
                }
        }
    }

    fun verifyTwoFactor(code: String, method: String) {
        val token = _state.value.twoFactorToken
        if (token == null) {
            _state.value = _state.value.copy(error = text(R.string.error_sign_in_again))
            return
        }

        if (code.isBlank()) {
            _state.value = _state.value.copy(error = text(R.string.error_enter_code))
            return
        }

        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_verifying))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ApiClient.verifyTwoFactor(token, code.trim(), method) }
                .onSuccess { body ->
                    val accessToken = body.optString("token")
                    if (accessToken.isBlank()) {
                        error(text(R.string.error_no_session))
                    }

                    store.saveToken(accessToken)
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            screen = if (store.hasVault()) Screen.Locked else Screen.Pair,
                            busy = false,
                            status = null,
                            error = null,
                            twoFactorToken = null,
                            twoFactorMethods = emptyList(),
                        )
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            busy = false,
                            status = null,
                            error = error.message ?: text(R.string.error_verify_code),
                        )
                    }
                }
        }
    }

    fun sendTwoFactorEmail() {
        val token = _state.value.twoFactorToken ?: return

        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_sending_code))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ApiClient.sendTwoFactorEmail(token) }
                .onSuccess { body ->
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            busy = false,
                            status = body.optString("message").takeIf { it.isNotBlank() }
                                ?: text(R.string.status_code_sent),
                        )
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            busy = false,
                            status = null,
                            error = error.message ?: text(R.string.error_send_code),
                        )
                    }
                }
        }
    }

    fun cancelTwoFactor() {
        _state.value = _state.value.copy(
            screen = Screen.Login,
            busy = false,
            status = null,
            error = null,
            twoFactorToken = null,
            twoFactorMethods = emptyList(),
        )
    }

    fun joinPairing(code: String) {
        val token = store.token()
        if (token == null) {
            _state.value = _state.value.copy(error = text(R.string.error_sign_in_again))
            return
        }

        if (!Regex("^\\d{6}$").matches(code.trim())) {
            _state.value = _state.value.copy(error = text(R.string.error_enter_six_digits))
            return
        }

        pollJob?.cancel()
        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_connecting))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val generator = KeyPairGenerator.getInstance("RSA")
                generator.initialize(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
                val keyPair = generator.generateKeyPair()
                pairingPrivateKey = keyPair.private
                pairingPublicKey = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

                ApiClient.joinPairing(token, code.trim(), pairingPublicKey!!, "Xiaomi Pad 6")
            }.onSuccess { sessionId ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = text(R.string.status_waiting_authorization),
                    )
                }
                pollPairing(token, sessionId)
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_connect),
                    )
                }
            }
        }
    }

    private fun pollPairing(token: String, sessionId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            var attempts = 0

            while (isActive && attempts < 150) {
                attempts += 1
                runCatching { ApiClient.pollPairing(token, sessionId) }
                    .onSuccess { data ->
                        when (data.optString("status")) {
                            "approved" -> {
                                val encrypted = data.optString("encrypted_master_key")
                                val privateKey = pairingPrivateKey
                                if (encrypted.isBlank() || privateKey == null) {
                                    withContext(Dispatchers.Main) {
                                        _state.value = _state.value.copy(
                                            status = null,
                                            error = text(R.string.error_computer_no_key),
                                        )
                                    }
                                    return@launch
                                }

                                val key = decryptPairingKey(encrypted, privateKey)
                                pendingMasterKey = key
                                withContext(Dispatchers.Main) {
                                    _state.value = _state.value.copy(
                                        screen = Screen.PinSetup,
                                        status = text(R.string.status_phone_authorized),
                                    )
                                }
                                return@launch
                            }

                            "expired" -> {
                                withContext(Dispatchers.Main) {
                                    _state.value = _state.value.copy(
                                        status = null,
                                        error = text(R.string.error_code_expired),
                                    )
                                }
                                return@launch
                            }
                        }
                    }
                    .onFailure { error ->
                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                status = null,
                                error = error.message ?: text(R.string.error_check_connection),
                            )
                        }
                        return@launch
                    }

                delay(2_000)
            }

            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    status = null,
                    error = text(R.string.error_pairing_timeout),
                )
            }
        }
    }

    fun setupPin(pin: String, confirmation: String) {
        val key = pendingMasterKey
        if (key == null) {
            _state.value = _state.value.copy(error = text(R.string.error_no_pending_vault))
            return
        }

        if (pin.length < 6 || !pin.all(Char::isDigit)) {
            _state.value = _state.value.copy(error = text(R.string.error_pin_length))
            return
        }

        if (pin != confirmation) {
            _state.value = _state.value.copy(error = text(R.string.error_pins_differ))
            return
        }

        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_saving_pin))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val salt = Crypto.randomBytes(16)
                val kek = Crypto.argon2id(pin, salt)
                val wrapped = Crypto.seal(kek, key)
                val wrappedBase64 = Base64.encodeToString(wrapped, Base64.NO_WRAP)
                val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
                store.savePinWrapped(wrappedBase64, saltBase64, Crypto.PIN_OPS, Crypto.PIN_MEM)
                Crypto.memzero(kek)
                pendingMasterKey = null
                masterKey = key

                val token = store.token()
                val publicKey = pairingPublicKey
                if (token != null && publicKey != null) {
                    runCatching {
                        val serverDeviceId = ApiClient.registerDevice(
                            token,
                            publicKey,
                            wrappedBase64,
                            saltBase64,
                            Crypto.PIN_OPS,
                            Crypto.PIN_MEM,
                        )
                        store.saveServerDeviceId(serverDeviceId)
                    }
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.Vault,
                        busy = false,
                        status = text(R.string.status_vault_ready),
                    )
                }
                sync()
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_save_pin),
                    )
                }
            }
        }
    }

    fun unlock(pin: String) {
        val wrapped = store.wrappedMasterKey()
        val salt = store.pinSalt()
        if (wrapped == null || salt == null) {
            _state.value = _state.value.copy(error = text(R.string.error_no_local_vault))
            return
        }

        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_opening_vault))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val kek = Crypto.argon2id(pin, Base64.decode(salt, Base64.NO_WRAP))
                val key = Crypto.open(kek, Base64.decode(wrapped, Base64.NO_WRAP))
                Crypto.memzero(kek)
                key
            }.onSuccess { key ->
                masterKey = key
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.Vault,
                        busy = false,
                        status = null,
                    )
                }
                sync()
            }.onFailure {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = text(R.string.error_wrong_pin),
                    )
                }
            }
        }
    }

    fun lock() {
        if (
            _state.value.screen == Screen.Login ||
            _state.value.screen == Screen.Pair ||
            _state.value.screen == Screen.PinSetup ||
            _state.value.screen == Screen.TwoFactor ||
            _state.value.screen == Screen.Loading
        ) {
            return
        }

        pollJob?.cancel()
        masterKey?.let(Crypto::memzero)
        identityPrivateKey?.let(Crypto::memzero)
        masterKey = null
        identityPrivateKey = null
        identityPublicKey = null
        closeViewer()
        _state.value = _state.value.copy(
            screen = if (store.hasVault()) Screen.Locked else Screen.Login,
            items = emptyList(),
            contacts = emptyList(),
            incomingShares = emptyList(),
            shareRequests = emptyList(),
            familyStatus = null,
            status = null,
            error = null,
        )
    }

    fun logout() {
        lock()
        store.clearToken()
        _state.value = _state.value.copy(screen = Screen.Login)
    }

    fun sync() {
        val key = masterKey ?: return
        val token = store.token() ?: return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                checkDeviceAllowed(token)
                loadIdentityKeys(key, token)
                uploadPending(key, token)
                fetchServer(key, token)
                refreshItems(key)
                loadFamily(token)
                Reminders.checkNow(getApplication())
            }.onFailure { error ->
                if (error.message == "Celular bloqueado.") {
                    return@onFailure
                }

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        status = null,
                        error = text(R.string.error_sync_connection, error.message ?: ""),
                    )
                }
            }
        }
    }

    private suspend fun checkDeviceAllowed(token: String) {
        val deviceId = store.serverDeviceId() ?: return
        val device = ApiClient.getDevice(token, deviceId)
        if (device.optBoolean("revoked", false) || !device.isNull("revoked_at")) {
            withContext(Dispatchers.Main) {
                store.clearAll()
                masterKey?.let(Crypto::memzero)
                masterKey = null
                _state.value = _state.value.copy(
                    screen = Screen.Login,
                    items = emptyList(),
                    status = null,
                    error = text(R.string.error_device_blocked),
                )
            }
            error("Celular bloqueado.")
        }
    }

    private fun loadIdentityKeys(key: ByteArray, token: String) {
        val keys = ApiClient.getKeys(token) ?: return

        keys.optString("identity_private_keys_encrypted")
            .takeIf { it.isNotBlank() && it != "null" }
            ?.let { encrypted ->
                runCatching {
                    val raw = Crypto.open(key, Base64.decode(encrypted, Base64.NO_WRAP))
                    val json = JSONObject(String(raw, Charsets.UTF_8))
                    identityPrivateKey = Base64.decode(json.getString("encryption"), Base64.NO_WRAP)
                }
            }

        keys.optString("identity_public_encryption_key")
            .takeIf { it.isNotBlank() && it != "null" }
            ?.let { publicKey ->
                identityPublicKey = Base64.decode(publicKey, Base64.NO_WRAP)
            }
    }

    private fun loadFamily(token: String) {
        val contacts = ApiClient.listContacts(token)
        val shares = ApiClient.listShares(token).optJSONArray("incoming") ?: JSONArray()
        val requests = ApiClient.listShareRequests(token)

        val contactList = mutableListOf<UiContact>()
        for (index in 0 until contacts.length()) {
            val contact = contacts.getJSONObject(index)
            contactList.add(
                UiContact(
                    id = contact.getString("id"),
                    name = contact.getString("name"),
                    email = contact.getString("email"),
                    status = contact.getString("status"),
                ),
            )
        }

        val incoming = mutableListOf<UiShare>()
        for (index in 0 until shares.length()) {
            val share = shares.getJSONObject(index)
            if (share.optString("status") != "pending") {
                continue
            }
            incoming.add(
                UiShare(
                    id = share.getString("id"),
                    sender = share.optString("sender"),
                    itemType = share.optString("item_type"),
                    mode = share.optString("mode"),
                    expiresAt = share.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
                ),
            )
        }

        val requestList = mutableListOf<UiShareRequest>()
        for (direction in listOf("incoming" to true, "outgoing" to false)) {
            val array = requests.optJSONArray(direction.first) ?: JSONArray()
            for (index in 0 until array.length()) {
                val shareRequest = array.getJSONObject(index)
                if (shareRequest.optString("status") != "pending") {
                    continue
                }
                requestList.add(
                    UiShareRequest(
                        id = shareRequest.getString("id"),
                        incoming = direction.second,
                        person = if (direction.second) {
                            shareRequest.optString("requester")
                        } else {
                            shareRequest.optString("target")
                        },
                        documentType = shareRequest.optString("document_type"),
                        purpose = shareRequest.optString("purpose"),
                    ),
                )
            }
        }

        viewModelScope.launch(Dispatchers.Main) {
            _state.value = _state.value.copy(
                contacts = contactList,
                incomingShares = incoming,
                shareRequests = requestList,
                familyStatus = text(R.string.family_status_contacts, contactList.count { it.status == "accepted" }),
            )
        }
    }

    fun backToVault() {
        _state.value = _state.value.copy(
            screen = Screen.Vault,
            error = null,
            familyStatus = null,
        )
    }

    fun openFamily() {
        _state.value = _state.value.copy(screen = Screen.Family, error = null, familyStatus = null)
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { loadFamily(token) }
        }
    }

    fun openScanner() {
        _state.value = _state.value.copy(screen = Screen.Scan, error = null, status = null)
    }

    fun openSettings() {
        _state.value = _state.value.copy(screen = Screen.Settings, error = null, status = null)
    }

    fun setSimpleMode(enabled: Boolean) {
        store.setSimpleMode(enabled)
        _state.value = _state.value.copy(simpleMode = enabled)
    }

    fun addContact(email: String) {
        val token = store.token() ?: return
        val normalized = email.trim()
        if (normalized.isBlank()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.addContact(token, normalized)
                loadFamily(token)
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = error.message ?: text(R.string.error_add_contact))
                }
            }
        }
    }

    fun acceptContact(id: String) {
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.acceptContact(token, id)
                loadFamily(token)
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = error.message ?: text(R.string.error_accept_contact))
                }
            }
        }
    }

    fun acceptShare(id: String) {
        val key = masterKey ?: return
        val token = store.token() ?: return
        val publicKey = identityPublicKey
        val privateKey = identityPrivateKey

        if (publicKey == null || privateKey == null) {
            _state.value = _state.value.copy(error = text(R.string.error_no_identity_keys))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val share = ApiClient.getShare(token, id)
                val packageBytes = Crypto.boxSealOpen(
                    Base64.decode(share.getString("encrypted_package"), Base64.NO_WRAP),
                    publicKey,
                    privateKey,
                )
                val packageJson = JSONObject(String(packageBytes, Charsets.UTF_8))
                val metadata = packageJson.getJSONObject("metadata")
                val fileBase64 = packageJson.optString("file").takeIf { it.isNotBlank() && it != "null" }

                val itemId = store.newItemId()
                var blobId: String? = null
                var localPath: String? = null

                if (fileBase64 != null) {
                    val fileBytes = Base64.decode(fileBase64, Base64.NO_WRAP)
                    if (fileBytes.size > 25 * 1024 * 1024) {
                        error(text(R.string.error_shared_file_too_large))
                    }
                    val encryptedFile = Crypto.encryptFile(key, fileBytes)
                    localPath = store.writeVaultFile(itemId, encryptedFile).absolutePath
                    blobId = ApiClient.uploadBlob(token, encryptedFile)
                }

                val encryptedMetadata = Base64.encodeToString(
                    Crypto.seal(key, metadata.toString().toByteArray(Charsets.UTF_8)),
                    Base64.NO_WRAP,
                )

                ApiClient.createItem(
                    token,
                    itemId,
                    metadata.optString("type", "document"),
                    encryptedMetadata,
                    blobId,
                    metadata.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
                )
                ApiClient.acceptShare(token, id, itemId)

                store.upsert(
                    LocalItem(
                        id = itemId,
                        type = metadata.optString("type", "document"),
                        state = "active",
                        encryptedMetadata = encryptedMetadata,
                        blobId = blobId,
                        version = 1,
                        expiresAt = null,
                        localPath = localPath,
                        pendingUpload = false,
                        updatedAt = VaultStore.now(),
                    ),
                )

                refreshItems(key)
                loadFamily(token)
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = error.message ?: text(R.string.error_accept_share))
                }
            }
        }
    }

    fun declineShare(id: String) {
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.declineShare(token, id)
                loadFamily(token)
            }
        }
    }

    fun createShareRequest(contactId: String, documentType: String, purpose: String) {
        val token = store.token() ?: return
        if (contactId.isBlank() || documentType.isBlank() || purpose.isBlank()) {
            _state.value = _state.value.copy(error = text(R.string.error_request_fields))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.createShareRequest(token, contactId, documentType.trim(), purpose.trim())
                loadFamily(token)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        familyStatus = text(R.string.status_request_sent),
                        error = null,
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = error.message ?: text(R.string.error_create_request))
                }
            }
        }
    }

    fun declineShareRequest(id: String) {
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.declineShareRequest(token, id)
                loadFamily(token)
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = error.message ?: text(R.string.error_decline_request))
                }
            }
        }
    }

    fun cancelShareRequest(id: String) {
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.cancelShareRequest(token, id)
                loadFamily(token)
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = error.message ?: text(R.string.error_cancel_request))
                }
            }
        }
    }

    fun sendShare(item: UiItem, contactId: String, mode: String) {
        val key = masterKey ?: return
        val token = store.token() ?: return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val publicKey = Base64.decode(ApiClient.contactPublicKey(token, contactId), Base64.NO_WRAP)
                val metadata = JSONObject()
                    .put("title", item.title)
                    .put("type", item.type)
                    .put("original_name", item.originalName)
                    .put("mime", item.mime)
                    .put("ocr_text", item.ocrText)
                    .put("expires_at", item.expiresAt ?: JSONObject.NULL)

                var fileBase64: String? = null
                val localPath = item.localPath
                if (localPath != null) {
                    val encrypted = File(localPath).readBytes()
                    val plaintext = Crypto.decryptFile(key, encrypted)
                    if (plaintext.size > 25 * 1024 * 1024) {
                        error(text(R.string.error_share_file_too_large))
                    }
                    fileBase64 = Base64.encodeToString(plaintext, Base64.NO_WRAP)
                }

                val packageJson = JSONObject()
                    .put("metadata", metadata)
                    .put("file", fileBase64 ?: JSONObject.NULL)
                val sealed = Crypto.boxSeal(packageJson.toString().toByteArray(Charsets.UTF_8), publicKey)

                ApiClient.createShare(
                    token,
                    contactId,
                    item.id,
                    mode,
                    Base64.encodeToString(sealed, Base64.NO_WRAP),
                    item.expiresAt,
                )

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        status = text(R.string.status_share_sent),
                    )
                }
                refreshItems(key)
                loadFamily(token)
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = error.message ?: text(R.string.error_send_share))
                }
            }
        }
    }

    private fun uploadPending(key: ByteArray, token: String) {
        for (item in store.pendingItems()) {
            val bytes = item.localPath?.let { runCatching { File(it).readBytes() }.getOrNull() }
            val blobId = if (bytes != null) ApiClient.uploadBlob(token, bytes) else null
            ApiClient.createItem(
                token,
                item.id,
                item.type,
                item.encryptedMetadata,
                blobId,
                item.expiresAt,
            )
            store.markUploaded(item.id, blobId)
        }
    }

    private fun fetchServer(key: ByteArray, token: String) {
        val serverItems = ApiClient.listItems(token)
        for (index in 0 until serverItems.length()) {
            val json = serverItems.getJSONObject(index)
            val id = json.getString("id")
            val existing = store.findItem(id)
            if (existing?.pendingUpload == true) {
                continue
            }

            val encryptedMetadata = json.getString("encrypted_metadata")
            val blobId = json.optString("blob_id").takeIf { it.isNotBlank() && it != "null" }
            val metadata = runCatching {
                JSONObject(String(Crypto.open(key, Base64.decode(encryptedMetadata, Base64.NO_WRAP))))
            }.getOrNull()

            var localPath = existing?.localPath
            if (blobId != null && localPath == null) {
                val bytes = ApiClient.downloadBlob(token, blobId)
                localPath = store.writeVaultFile(id, bytes).absolutePath
            }

            store.markSyncedFromServer(
                id = id,
                type = json.getString("type"),
                encryptedMetadata = encryptedMetadata,
                blobId = blobId,
                version = json.optInt("version", 1),
                expiresAt = json.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
                localPath = localPath,
            )

            if (metadata == null) {
                continue
            }
        }
    }

    private fun refreshItems(key: ByteArray) {
        val items = store.allItems().map { item ->
            val metadata = runCatching {
                JSONObject(String(Crypto.open(key, Base64.decode(item.encryptedMetadata, Base64.NO_WRAP))))
            }.getOrNull()

            UiItem(
                id = item.id,
                type = item.type,
                title = metadata?.optString("title")?.takeIf { it.isNotBlank() } ?: text(R.string.document_unreadable),
                mime = metadata?.optString("mime") ?: "",
                originalName = metadata?.optString("original_name") ?: "documento",
                pendingUpload = item.pendingUpload,
                blobId = item.blobId,
                localPath = item.localPath,
                expiresAt = item.expiresAt,
                ocrText = metadata?.optString("ocr_text")?.takeIf { it.isNotBlank() && it != "null" } ?: "",
            )
        }

        viewModelScope.launch(Dispatchers.Main) {
            _state.value = _state.value.copy(items = items, status = null, error = null)
        }
    }

    fun addDocument(uri: Uri) {
        val key = masterKey ?: return
        val resolver = getApplication<Application>().contentResolver
        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_saving_document))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "documento"
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error(text(R.string.error_read_file))

                if (bytes.size > 100 * 1024 * 1024) {
                    error(text(R.string.error_file_too_large))
                }

                val id = store.newItemId()
                val title = name.substringBeforeLast('.').ifBlank { text(R.string.document) }
                val metadata = JSONObject()
                    .put("title", title)
                    .put("type", "document")
                    .put("original_name", name)
                    .put("mime", mime)
                    .put("size", bytes.size)
                    .put("expires_at", JSONObject.NULL)

                val encryptedMetadata = Base64.encodeToString(
                    Crypto.seal(key, metadata.toString().toByteArray(Charsets.UTF_8)),
                    Base64.NO_WRAP,
                )
                val encryptedFile = Crypto.encryptFile(key, bytes)
                val file = store.writeVaultFile(id, encryptedFile)

                store.upsert(
                    LocalItem(
                        id = id,
                        type = "document",
                        state = "active",
                        encryptedMetadata = encryptedMetadata,
                        blobId = null,
                        version = 1,
                        expiresAt = null,
                        localPath = file.absolutePath,
                        pendingUpload = true,
                        updatedAt = VaultStore.now(),
                    ),
                )

                refreshItems(key)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(busy = false, status = text(R.string.status_document_saved))
                }
                sync()
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_save_document),
                    )
                }
            }
        }
    }

    fun addScan(photo: File) {
        val key = masterKey ?: return
        _state.value = _state.value.copy(
            screen = Screen.Vault,
            busy = true,
            error = null,
            status = text(R.string.status_processing_scan),
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bitmap = decodeSampledBitmap(photo, 2400)
                val pdfBytes = createPdf(bitmap)
                val ocrText = runCatching { Ocr.extract(getApplication(), bitmap) }.getOrDefault("")
                val now = Date()
                val fallbackTitle = text(R.string.scan_title_prefix, SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(now))
                val title = ocrText.lineSequence()
                    .map { it.trim().replace(Regex("\\s+"), " ") }
                    .firstOrNull { it.length in 4..80 }
                    ?.take(80)
                    ?: fallbackTitle
                val name = "escaneo-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(now) + ".pdf"

                val id = store.newItemId()
                val metadata = JSONObject()
                    .put("title", title)
                    .put("type", "document")
                    .put("original_name", name)
                    .put("mime", "application/pdf")
                    .put("size", pdfBytes.size)
                    .put("ocr_text", ocrText)
                    .put("expires_at", JSONObject.NULL)

                val encryptedMetadata = Base64.encodeToString(
                    Crypto.seal(key, metadata.toString().toByteArray(Charsets.UTF_8)),
                    Base64.NO_WRAP,
                )
                val encryptedFile = Crypto.encryptFile(key, pdfBytes)
                val file = store.writeVaultFile(id, encryptedFile)

                store.upsert(
                    LocalItem(
                        id = id,
                        type = "document",
                        state = "active",
                        encryptedMetadata = encryptedMetadata,
                        blobId = null,
                        version = 1,
                        expiresAt = null,
                        localPath = file.absolutePath,
                        pendingUpload = true,
                        updatedAt = VaultStore.now(),
                    ),
                )

                refreshItems(key)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = text(R.string.status_scan_saved),
                        error = null,
                    )
                }
                sync()
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_process_scan),
                    )
                }
            }

            photo.delete()
        }
    }

    private fun decodeSampledBitmap(file: File, maxDimension: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            error(text(R.string.error_read_photo))
        }

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: error(text(R.string.error_read_photo))

        val orientation = runCatching {
            android.media.ExifInterface(file.absolutePath).getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(android.media.ExifInterface.ORIENTATION_NORMAL)

        val rotation = when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        if (rotation == 0f) {
            return decoded
        }

        val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }

    private fun createPdf(bitmap: Bitmap): ByteArray {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val margin = 24f
        val scale = min(
            (pageInfo.pageWidth - margin * 2) / bitmap.width.toFloat(),
            (pageInfo.pageHeight - margin * 2) / bitmap.height.toFloat(),
        )
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        val left = (pageInfo.pageWidth - width) / 2
        val top = (pageInfo.pageHeight - height) / 2
        page.canvas.drawBitmap(
            bitmap,
            null,
            RectF(left, top, left + width, top + height),
            Paint(Paint.ANTI_ALIAS_FLAG),
        )
        document.finishPage(page)

        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }

    fun deleteItem(item: UiItem) {
        val key = masterKey ?: return
        val token = store.token()

        viewModelScope.launch(Dispatchers.IO) {
            store.deleteItem(item.id)
            if (token != null && !item.pendingUpload) {
                runCatching { ApiClient.deleteItem(token, item.id) }
            }
            refreshItems(key)
        }
    }

    fun setExpiry(item: UiItem, expiresAt: String?) {
        val key = masterKey ?: return
        val token = store.token() ?: return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val local = store.findItem(item.id) ?: error(text(R.string.error_document_not_found))
                val metadata = JSONObject(
                    String(Crypto.open(key, Base64.decode(local.encryptedMetadata, Base64.NO_WRAP))),
                )
                metadata.put("expires_at", expiresAt ?: JSONObject.NULL)
                val encryptedMetadata = Base64.encodeToString(
                    Crypto.seal(key, metadata.toString().toByteArray(Charsets.UTF_8)),
                    Base64.NO_WRAP,
                )

                var version = local.version
                if (!local.pendingUpload) {
                    ApiClient.updateItem(token, item.id, encryptedMetadata, expiresAt, local.version)
                    version += 1
                }

                store.upsert(
                    local.copy(
                        encryptedMetadata = encryptedMetadata,
                        expiresAt = expiresAt,
                        version = version,
                        updatedAt = VaultStore.now(),
                    ),
                )

                refreshItems(key)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        status = if (expiresAt == null) text(R.string.status_expiry_removed) else text(R.string.status_expiry_saved),
                        error = null,
                    )
                }
                Reminders.checkNow(getApplication())
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        error = error.message ?: text(R.string.error_save_expiry),
                    )
                }
            }
        }
    }

    fun openItem(item: UiItem) {
        val key = masterKey ?: return
        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_opening_document))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                var path = item.localPath
                if (path == null && item.blobId != null) {
                    val token = store.token() ?: error(text(R.string.error_no_session_download))
                    val bytes = ApiClient.downloadBlob(token, item.blobId)
                    path = store.writeVaultFile(item.id, bytes).absolutePath
                }
                val encrypted = File(path ?: error(text(R.string.error_document_not_downloaded))).readBytes()
                val plaintext = Crypto.decryptFile(key, encrypted)
                val extension = when {
                    item.mime.contains("pdf") -> "pdf"
                    item.mime.contains("png") -> "png"
                    item.mime.contains("jpeg") || item.mime.contains("jpg") -> "jpg"
                    else -> "bin"
                }
                val temp = File(getApplication<Application>().cacheDir, "viewer-${item.id}.$extension")
                temp.writeBytes(plaintext)
                viewerFile = temp

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.Viewer,
                        busy = false,
                        status = null,
                        viewerPath = temp.absolutePath,
                        viewerMime = item.mime,
                        viewerName = item.originalName,
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_open_document),
                    )
                }
            }
        }
    }

    fun closeViewer() {
        viewerFile?.delete()
        viewerFile = null
        _state.value = _state.value.copy(
            screen = Screen.Vault,
            viewerPath = null,
            viewerMime = null,
            viewerName = null,
        )
    }

    private fun decryptPairingKey(encrypted: String, privateKey: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            privateKey,
            OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT,
            ),
        )
        return cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
    }
}
