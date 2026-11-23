package app.mypass

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
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
    Home,
    Vault,
    Passes,
    PassEditor,
    Family,
    Scan,
    Settings,
    Viewer,
}

data class UiPass(
    val category: String,
    val title: String,
    val issuer: String,
    val holder: String,
    val fields: Map<String, String>,
    val notes: List<String>,
    val codePayload: String?,
    val codeFormat: String,
    val codeSource: String,
    val themeColor: String,
    val options: Map<String, Boolean>,
    val logo: String? = null,
)

data class PassDraft(
    val itemId: String? = null,
    val version: Int? = null,
    val category: String = "flight",
    val title: String = "",
    val issuer: String = "",
    val holder: String = "",
    val fields: Map<String, String> = emptyMap(),
    val notes: List<String> = emptyList(),
    val codePayload: String = "",
    val codeFormat: String = "qr",
    val codeSource: String = "generated",
    val themeColor: String = "#0c2340",
    val logo: String? = null,
    val options: Map<String, Boolean> = mapOf(
        "presentation" to true,
        "share" to true,
        "reminder" to true,
    ),
    val ocrText: String = "",
    val sourcePath: String? = null,
    val sourceMime: String? = null,
    val sourceName: String? = null,
    val attachSource: Boolean = false,
    val isNew: Boolean = true,
)

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
    val isOutgoing: Boolean = false,
)

data class UiShareRequest(
    val id: String,
    val incoming: Boolean,
    val person: String,
    val documentType: String,
    val purpose: String,
)

data class UiNotification(
    val id: String,
    val type: String,
    val from: String,
    val documentType: String,
    val read: Boolean,
    val createdAt: String?,
)

data class UiItem(
    val id: String,
    val type: String,
    val state: String = "active",
    val title: String,
    val mime: String,
    val originalName: String,
    val pendingUpload: Boolean,
    val blobId: String?,
    val localPath: String?,
    val expiresAt: String?,
    val updatedAt: String? = null,
    val ocrText: String = "",
    val pass: UiPass? = null,
    val metadataJson: JSONObject? = null,
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
    val notifications: List<UiNotification> = emptyList(),
    val familyStatus: String? = null,
    val twoFactorToken: String? = null,
    val twoFactorMethods: List<String> = emptyList(),
    val simpleMode: Boolean = false,
    val biometricEnabled: Boolean = false,
    val viewerPath: String? = null,
    val viewerMime: String? = null,
    val viewerName: String? = null,
    val passDraft: PassDraft? = null,
    val scanForPass: Boolean = false,
    val scanSourcePath: String? = null,
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
            biometricEnabled = store.biometricEnabled(),
            email = store.email(),
        )
    }

    private fun text(id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearStatus() {
        _state.value = _state.value.copy(status = null)
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

                    if (store.hasVault() && store.email().isNotBlank() && store.email() != email.trim()) {
                        store.clearAll()
                    }
                    store.saveToken(token)
                    store.saveEmail(email.trim())
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            screen = if (store.hasVault()) Screen.Locked else Screen.Pair,
                            email = email.trim(),
                            busy = false,
                            status = null,
                            twoFactorToken = null,
                            twoFactorMethods = emptyList(),
                            biometricEnabled = store.biometricEnabled(),
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

                    val accountEmail = body.optJSONObject("user")?.optString("email")
                        ?.takeIf { it.isNotBlank() }
                        ?: _state.value.email
                    if (store.hasVault() && store.email().isNotBlank() && store.email() != accountEmail) {
                        store.clearAll()
                    }
                    if (accountEmail.isNotBlank()) {
                        store.saveEmail(accountEmail)
                    }
                    store.saveToken(accessToken)
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            screen = if (store.hasVault()) Screen.Locked else Screen.Pair,
                            email = accountEmail,
                            busy = false,
                            status = null,
                            error = null,
                            twoFactorToken = null,
                            twoFactorMethods = emptyList(),
                            biometricEnabled = store.biometricEnabled(),
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
                        screen = Screen.Home,
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

        val now = System.currentTimeMillis()
        val lockUntil = store.pinLockUntil()
        if (lockUntil > now) {
            val seconds = ((lockUntil - now) / 1_000).coerceAtLeast(1)
            _state.value = _state.value.copy(error = text(R.string.error_pin_locked, seconds))
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
                store.clearPinFailures()
                masterKey = key
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.Home,
                        busy = false,
                        status = null,
                        error = null,
                    )
                }
                sync()
            }.onFailure {
                val failureLockUntil = store.registerPinFailure(System.currentTimeMillis())
                withContext(Dispatchers.Main) {
                    if (failureLockUntil > System.currentTimeMillis()) {
                        val seconds = ((failureLockUntil - System.currentTimeMillis()) / 1_000).coerceAtLeast(1)
                        _state.value = _state.value.copy(
                            busy = false,
                            status = null,
                            error = text(R.string.error_pin_locked, seconds),
                        )
                    } else {
                        _state.value = _state.value.copy(
                            busy = false,
                            status = null,
                            error = text(R.string.error_wrong_pin),
                        )
                    }
                }
            }
        }
    }

    fun biometricIv(): String? = store.biometricIv()

    fun unlockWithBiometric(cipher: Cipher) {
        val encrypted = store.biometricCiphertext()
        if (encrypted == null) {
            _state.value = _state.value.copy(
                biometricEnabled = false,
                error = text(R.string.error_biometric_unavailable),
            )
            return
        }

        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_opening_vault))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
            }.onSuccess { key ->
                store.clearPinFailures()
                masterKey = key
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.Home,
                        busy = false,
                        status = null,
                        error = null,
                    )
                }
                sync()
            }.onFailure {
                store.clearBiometric()
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        biometricEnabled = false,
                        error = text(R.string.error_biometric_failed),
                    )
                }
            }
        }
    }

    fun enableBiometric(cipher: Cipher) {
        val key = masterKey
        if (key == null) {
            _state.value = _state.value.copy(error = text(R.string.error_unlock_first))
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val encrypted = cipher.doFinal(key)
                val ciphertext = Base64.encodeToString(encrypted, Base64.NO_WRAP)
                val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
                store.saveBiometricWrapped(ciphertext, iv)

                val token = store.token()
                val deviceId = store.serverDeviceId()
                if (token != null && deviceId != null) {
                    runCatching {
                        ApiClient.updateDevice(
                            token = token,
                            id = deviceId,
                            biometricWrappedMasterKey = ciphertext,
                        )
                    }
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        biometricEnabled = true,
                        status = text(R.string.status_biometric_enabled),
                        error = null,
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        error = error.message ?: text(R.string.error_biometric_failed),
                    )
                }
            }
        }
    }

    fun disableBiometric() {
        store.clearBiometric()
        _state.value = _state.value.copy(
            biometricEnabled = false,
            status = text(R.string.status_biometric_disabled),
            error = null,
        )
    }

    fun changePin(oldPin: String, newPin: String, confirmation: String) {
        if (newPin.length < 6 || !newPin.all(Char::isDigit)) {
            _state.value = _state.value.copy(error = text(R.string.error_pin_length))
            return
        }

        if (newPin != confirmation) {
            _state.value = _state.value.copy(error = text(R.string.error_pins_differ))
            return
        }

        val wrapped = store.wrappedMasterKey()
        val salt = store.pinSalt()
        if (wrapped == null || salt == null) {
            _state.value = _state.value.copy(error = text(R.string.error_no_local_vault))
            return
        }

        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_saving_pin))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val oldKek = Crypto.argon2id(oldPin, Base64.decode(salt, Base64.NO_WRAP))
                val master = Crypto.open(oldKek, Base64.decode(wrapped, Base64.NO_WRAP))
                Crypto.memzero(oldKek)

                val newSalt = Crypto.randomBytes(16)
                val newKek = Crypto.argon2id(newPin, newSalt)
                val newWrapped = Crypto.seal(newKek, master)
                Crypto.memzero(newKek)
                Crypto.memzero(master)

                val wrappedBase64 = Base64.encodeToString(newWrapped, Base64.NO_WRAP)
                val saltBase64 = Base64.encodeToString(newSalt, Base64.NO_WRAP)
                store.savePinWrapped(wrappedBase64, saltBase64, Crypto.PIN_OPS, Crypto.PIN_MEM)
                store.clearPinFailures()

                val token = store.token()
                val deviceId = store.serverDeviceId()
                if (token != null && deviceId != null) {
                    runCatching {
                        ApiClient.updateDevice(
                            token = token,
                            id = deviceId,
                            pinSalt = saltBase64,
                            pinParams = JSONObject()
                                .put("ops_limit", Crypto.PIN_OPS)
                                .put("mem_limit", Crypto.PIN_MEM)
                                .put("algorithm", "argon2id13"),
                        )
                    }
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = text(R.string.status_pin_changed),
                        error = null,
                    )
                }
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
        _state.value.passDraft?.let { cleanupDraftSource(it) }
        closeViewer()
        _state.value = _state.value.copy(
            screen = if (store.hasVault()) Screen.Locked else Screen.Login,
            items = emptyList(),
            contacts = emptyList(),
            incomingShares = emptyList(),
            shareRequests = emptyList(),
            notifications = emptyList(),
            familyStatus = null,
            passDraft = null,
            scanForPass = false,
            scanSourcePath = null,
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
                loadNotifications(token)
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
        val sharesResponse = ApiClient.listShares(token)
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
        for (direction in listOf("incoming" to false, "outgoing" to true)) {
            val array = sharesResponse.optJSONArray(direction.first) ?: JSONArray()
            for (index in 0 until array.length()) {
                val share = array.getJSONObject(index)
                if (share.optString("status") != "pending") {
                    continue
                }
                incoming.add(
                    UiShare(
                        id = share.getString("id"),
                        sender = if (direction.second) {
                            share.optString("recipient")
                        } else {
                            share.optString("sender")
                        },
                        itemType = share.optString("item_type"),
                        mode = share.optString("mode"),
                        expiresAt = share.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
                        isOutgoing = direction.second,
                    ),
                )
            }
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
                familyStatus = null,
            )
        }
    }

    private fun loadNotifications(token: String) {
        val response = ApiClient.listNotifications(token)
        val array = response.optJSONArray("data") ?: JSONArray()
        val notifications = mutableListOf<UiNotification>()

        for (index in 0 until array.length()) {
            val json = array.getJSONObject(index)
            val data = json.optJSONObject("data") ?: JSONObject()
            notifications.add(
                UiNotification(
                    id = json.getString("id"),
                    type = json.optString("type"),
                    from = data.optString("from"),
                    documentType = data.optString("document_type"),
                    read = !json.isNull("read_at") &&
                        json.optString("read_at").isNotBlank() &&
                        json.optString("read_at") != "null",
                    createdAt = json.optString("created_at").takeIf { it.isNotBlank() && it != "null" },
                ),
            )
        }

        viewModelScope.launch(Dispatchers.Main) {
            _state.value = _state.value.copy(notifications = notifications)
        }
    }

    fun markNotificationRead(id: String) {
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.markNotificationRead(token, id)
                loadNotifications(token)
            }
        }
    }

    fun markAllNotificationsRead() {
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.markAllNotificationsRead(token)
                loadNotifications(token)
            }
        }
    }

    fun backToVault() {
        _state.value = _state.value.copy(
            screen = Screen.Vault,
            error = null,
            familyStatus = null,
            passDraft = null,
        )
    }

    fun backToHome() {
        _state.value = _state.value.copy(
            screen = Screen.Home,
            error = null,
            familyStatus = null,
            passDraft = null,
        )
    }

    fun openPasses() {
        _state.value = _state.value.copy(
            screen = Screen.Passes,
            error = null,
            status = null,
            passDraft = null,
        )
    }

    fun openFamily() {
        _state.value = _state.value.copy(screen = Screen.Family, error = null, familyStatus = null)
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { loadFamily(token) }
        }
    }

    fun openScanner(forPass: Boolean = false) {
        _state.value = _state.value.copy(
            screen = Screen.Scan,
            error = null,
            status = null,
            scanForPass = forPass,
        )
    }

    fun backFromScan() {
        val target = if (_state.value.scanForPass) Screen.Passes else Screen.Vault
        _state.value = _state.value.copy(screen = target, error = null, status = null)
    }

    fun openSettings() {
        _state.value = _state.value.copy(screen = Screen.Settings, error = null, status = null)
    }

    fun createPassFromScan(photo: File) {
        if (masterKey == null) {
            return
        }

        _state.value = _state.value.copy(
            busy = true,
            error = null,
            status = text(R.string.status_reading_pass),
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bitmap = decodeSampledBitmap(photo, 2400)
                analyzeBitmap(
                    bitmap = bitmap,
                    sourcePath = photo.absolutePath,
                    sourceMime = "image/jpeg",
                    sourceName = "escaneo-${System.currentTimeMillis()}.jpg",
                )
            }.onSuccess { draft ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.PassEditor,
                        passDraft = draft,
                        busy = false,
                        status = null,
                        error = null,
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_read_pass),
                    )
                }
            }
        }
    }

    fun createPassFromUri(uri: Uri) {
        if (masterKey == null) {
            return
        }

        val resolver = getApplication<Application>().contentResolver
        _state.value = _state.value.copy(
            busy = true,
            error = null,
            status = text(R.string.status_reading_pass),
        )

        viewModelScope.launch(Dispatchers.IO) {
            var cacheFile: File? = null
            runCatching {
                val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: "pase"
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error(text(R.string.error_read_file))

                if (bytes.size > 100 * 1024 * 1024) {
                    error(text(R.string.error_file_too_large))
                }

                val extension = when {
                    mime.contains("pdf") -> "pdf"
                    mime.contains("png") -> "png"
                    mime.contains("webp") -> "webp"
                    else -> "jpg"
                }
                cacheFile = File(
                    getApplication<Application>().cacheDir,
                    "pass-source-${System.currentTimeMillis()}.$extension",
                ).apply { writeBytes(bytes) }

                val bitmap = if (mime.contains("pdf")) {
                    resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                        renderPdfFirstPage(descriptor)
                    } ?: error(text(R.string.error_read_file))
                } else {
                    decodeSampledByteArray(bytes, 2400)
                        ?: error(text(R.string.error_read_file))
                }

                analyzeBitmap(
                    bitmap = bitmap,
                    sourcePath = cacheFile?.absolutePath,
                    sourceMime = mime,
                    sourceName = name,
                )
            }.onSuccess { draft ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.PassEditor,
                        passDraft = draft,
                        busy = false,
                        status = null,
                        error = null,
                    )
                }
            }.onFailure { error ->
                cacheFile?.delete()
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_read_pass),
                    )
                }
            }
        }
    }

    fun newPass(category: String) {
        _state.value = _state.value.copy(
            screen = Screen.PassEditor,
            passDraft = PassDraft(
                category = category,
                themeColor = passThemeColor(category, null),
                isNew = true,
            ),
            error = null,
            status = null,
        )
    }

    fun editPass(item: UiItem) {
        val pass = item.pass ?: return
        val local = store.findItem(item.id)
        _state.value = _state.value.copy(
            screen = Screen.PassEditor,
            passDraft = PassDraft(
                itemId = item.id,
                version = local?.version ?: 1,
                category = pass.category,
                title = pass.title,
                issuer = pass.issuer,
                holder = pass.holder,
                fields = pass.fields,
                notes = pass.notes,
                codePayload = pass.codePayload.orEmpty(),
                codeFormat = pass.codeFormat,
                codeSource = pass.codeSource,
                themeColor = pass.themeColor,
                logo = pass.logo,
                options = pass.options,
                ocrText = item.ocrText,
                isNew = false,
            ),
            error = null,
            status = null,
        )
    }

    fun discardPassDraft() {
        _state.value.passDraft?.let { cleanupDraftSource(it) }
        _state.value = _state.value.copy(
            screen = Screen.Passes,
            passDraft = null,
            busy = false,
            status = null,
            error = null,
        )
    }

    fun savePass(draft: PassDraft) {
        val key = masterKey ?: return
        val token = store.token()
        val id = draft.itemId ?: store.newItemId()
        val type = if (draft.category == "id" || draft.category == "membership") "card" else "ticket"
        val expiresAt = if (draft.options["reminder"] != false) {
            draft.fields["expires"] ?: draft.fields["date"]
        } else {
            null
        }

        _state.value = _state.value.copy(
            busy = true,
            error = null,
            status = text(R.string.status_saving_pass),
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val title = draft.title.ifBlank { fallbackPassTitle(draft) }
                val metadata = JSONObject()
                    .put("title", title)
                    .put("type", type)
                    .put("expires_at", expiresAt ?: JSONObject.NULL)
                    .put("pass", buildPassJson(id, draft.copy(title = title)))
                if (draft.ocrText.isNotBlank()) {
                    metadata.put("ocr_text", draft.ocrText)
                }

                val existing = store.findItem(id)
                if (!draft.isNew && existing != null) {
                    runCatching {
                        JSONObject(String(Crypto.open(key, Base64.decode(existing.encryptedMetadata, Base64.NO_WRAP))))
                    }.getOrNull()?.let { previous ->
                        for (field in listOf("original_name", "mime", "size", "ocr_text")) {
                            if (!metadata.has(field) && previous.has(field)) {
                                metadata.put(field, previous.get(field))
                            }
                        }
                    }
                }

                var localPath = existing?.localPath
                if (draft.isNew && draft.attachSource) {
                    draft.sourcePath?.let { path ->
                        val source = File(path)
                        if (source.exists()) {
                            val bytes = source.readBytes()
                            val encryptedFile = Crypto.encryptFile(key, bytes)
                            localPath = store.writeVaultFile(id, encryptedFile).absolutePath
                            metadata
                                .put("original_name", draft.sourceName ?: source.name)
                                .put("mime", draft.sourceMime ?: "application/octet-stream")
                                .put("size", bytes.size)
                        }
                    }
                }

                val encryptedMetadata = Base64.encodeToString(
                    Crypto.seal(key, metadata.toString().toByteArray(Charsets.UTF_8)),
                    Base64.NO_WRAP,
                )

                if (draft.isNew) {
                    store.upsert(
                        LocalItem(
                            id = id,
                            type = type,
                            state = "active",
                            encryptedMetadata = encryptedMetadata,
                            blobId = null,
                            version = 1,
                            expiresAt = expiresAt,
                            localPath = localPath,
                            pendingUpload = true,
                            updatedAt = VaultStore.now(),
                        ),
                    )
                } else {
                    val local = existing ?: error(text(R.string.error_document_not_found))
                    var version = local.version
                    if (!local.pendingUpload) {
                        val activeToken = token ?: error(text(R.string.error_sign_in_again))
                        ApiClient.updateItem(activeToken, id, encryptedMetadata, expiresAt, local.version)
                        version += 1
                    }
                    store.upsert(
                        local.copy(
                            type = type,
                            encryptedMetadata = encryptedMetadata,
                            expiresAt = expiresAt,
                            version = version,
                            updatedAt = VaultStore.now(),
                        ),
                    )
                }

                cleanupDraftSource(draft)
                refreshItems(key)
                Reminders.checkNow(getApplication())
                if (draft.isNew) {
                    sync()
                }

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.Passes,
                        passDraft = null,
                        busy = false,
                        status = text(R.string.status_pass_saved),
                        error = null,
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_save_pass),
                    )
                }
            }
        }
    }

    private fun analyzeBitmap(
        bitmap: Bitmap,
        sourcePath: String?,
        sourceMime: String?,
        sourceName: String?,
    ): PassDraft {
        val ocrText = runCatching { Ocr.extract(getApplication(), bitmap) }.getOrDefault("")
        val barcode = Barcode.decode(bitmap)
        val bcbp = barcode?.text?.let { PassParser.parseBcbp(it) }
        val parsed = PassParser.parse(ocrText.ifBlank { barcode?.text.orEmpty() })
        val category = bcbp?.category ?: parsed.category
        val fields = linkedMapOf<String, String>().apply {
            putAll(parsed.fields)
            bcbp?.fields?.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    put(key, value)
                }
            }
        }
        val title = bcbp?.title ?: parsed.title
        val issuer = bcbp?.issuer ?: parsed.issuer
        val holder = bcbp?.holder ?: parsed.holder

        return PassDraft(
            category = category,
            title = title,
            issuer = issuer,
            holder = holder,
            fields = fields,
            notes = parsed.notes,
            codePayload = barcode?.text ?: bcbp?.codePayload.orEmpty(),
            codeFormat = barcode?.format ?: "qr",
            codeSource = if (barcode != null) "decoded" else "generated",
            themeColor = passThemeColor(category, null),
            options = mapOf(
                "presentation" to true,
                "share" to true,
                "reminder" to true,
            ),
            ocrText = ocrText,
            sourcePath = sourcePath,
            sourceMime = sourceMime,
            sourceName = sourceName,
            attachSource = false,
            isNew = true,
        )
    }

    private fun buildPassJson(id: String, draft: PassDraft): JSONObject {
        val allowedFields = when (draft.category) {
            "flight" -> setOf(
                "origin", "destination", "flight", "carrier", "flight_number", "passenger",
                "passenger_name", "seat", "gate", "zone", "cabin", "date", "departure",
                "boarding", "pnr", "sequence",
            )
            "event" -> setOf("location", "date", "time", "gate", "section", "row", "seat")
            "transit" -> setOf("route", "date", "time", "platform", "carriage", "seat")
            "id" -> setOf("number", "birth", "expires")
            else -> setOf("number", "level", "expires")
        }
        val fields = JSONObject()
        draft.fields
            .filterKeys { it in allowedFields }
            .filterValues { it.isNotBlank() }
            .forEach { (key, value) -> fields.put(key, value) }
        if (draft.holder.isNotBlank()) {
            fields.put(if (draft.category == "flight") "passenger" else "holder", draft.holder)
        }

        val notes = JSONArray()
        draft.notes.filter { it.isNotBlank() }.forEach(notes::put)

        val title = draft.title.ifBlank { fallbackPassTitle(draft) }
        val payload = draft.codePayload.ifBlank {
            JSONObject()
                .put("v", 1)
                .put("app", "mypass")
                .put("id", id)
                .put("c", draft.category)
                .put("title", title)
                .put("issuer", draft.issuer)
                .put("holder", draft.holder)
                .put("fields", fields)
                .toString()
        }

        return JSONObject()
            .put("category", draft.category)
            .put("title", title)
            .put("issuer", draft.issuer)
            .put("holder", draft.holder)
            .put("fields", fields)
            .put("notes", notes)
            .put(
                "code",
                JSONObject()
                    .put("payload", payload)
                    .put("format", draft.codeFormat)
                    .put("source", draft.codeSource),
            )
            .put("themeColor", draft.themeColor)
            .put("logo", draft.logo ?: JSONObject.NULL)
            .put(
                "options",
                JSONObject()
                    .put("presentation", draft.options["presentation"] != false)
                    .put("share", draft.options["share"] != false)
                    .put("reminder", draft.options["reminder"] != false),
            )
    }

    private fun fallbackPassTitle(draft: PassDraft): String {
        return when (draft.category) {
            "flight" -> {
                val route = listOf(draft.fields["origin"], draft.fields["destination"])
                    .filterNot { it.isNullOrBlank() }
                    .joinToString(" → ")
                listOf(route, draft.fields["flight"].orEmpty())
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { text(R.string.new_flight_pass) }
            }
            "event" -> draft.fields["location"] ?: text(R.string.new_event_pass)
            "transit" -> draft.fields["route"] ?: text(R.string.new_transit_pass)
            "membership" -> draft.issuer.ifBlank { text(R.string.new_membership) }
            else -> draft.issuer.ifBlank { text(R.string.new_identity_pass) }
        }
    }

    private fun decodeSampledByteArray(bytes: ByteArray, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxEdge || bounds.outHeight / sampleSize > maxEdge) {
            sampleSize *= 2
        }

        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    }

    private fun renderPdfFirstPage(descriptor: ParcelFileDescriptor): Bitmap {
        val renderer = PdfRenderer(descriptor)
        try {
            if (renderer.pageCount == 0) {
                error(text(R.string.error_read_file))
            }
            renderer.openPage(0).use { page ->
                val maxEdge = 2400f
                val scale = (maxEdge / maxOf(page.width, page.height).toFloat()).coerceIn(1f, 2.5f)
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        } finally {
            renderer.close()
        }
    }

    private fun cleanupDraftSource(draft: PassDraft) {
        val path = draft.sourcePath ?: return
        if (path.contains(getApplication<Application>().cacheDir.absolutePath)) {
            runCatching { File(path).delete() }
        }
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

    fun blockContact(id: String) {
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.blockContact(token, id)
                loadFamily(token)
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = error.message ?: text(R.string.error_block_contact))
                }
            }
        }
    }

    fun revokeShare(id: String) {
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.revokeShare(token, id)
                loadFamily(token)
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(error = error.message ?: text(R.string.error_revoke_share))
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
                val metadata = item.metadataJson?.let { JSONObject(it.toString()) } ?: JSONObject()
                metadata
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

            val title = metadata?.optString("title")?.takeIf { it.isNotBlank() }
                ?: text(R.string.document_unreadable)
            val passJson = metadata?.optJSONObject("pass")
            val pass = when {
                passJson != null -> parseUiPass(passJson, title, item.type)
                item.type == "ticket" || item.type == "card" -> fallbackUiPass(title, item.type)
                else -> null
            }

            UiItem(
                id = item.id,
                type = item.type,
                state = item.state,
                title = title,
                mime = metadata?.optString("mime") ?: "",
                originalName = metadata?.optString("original_name") ?: "documento",
                pendingUpload = item.pendingUpload,
                blobId = item.blobId,
                localPath = item.localPath,
                expiresAt = item.expiresAt,
                updatedAt = item.updatedAt,
                ocrText = metadata?.optString("ocr_text")?.takeIf { it.isNotBlank() && it != "null" } ?: "",
                pass = pass,
                metadataJson = metadata,
            )
        }

        viewModelScope.launch(Dispatchers.Main) {
            _state.value = _state.value.copy(items = items, status = null, error = null)
        }
    }

    private fun parseUiPass(json: JSONObject, fallbackTitle: String, type: String): UiPass {
        val fields = mutableMapOf<String, String>()
        json.optJSONObject("fields")?.let { objectFields ->
            for (key in objectFields.keys()) {
                val value = objectFields.optString(key)
                if (value.isNotBlank() && value != "null") {
                    fields[key] = value
                }
            }
        }

        val notes = mutableListOf<String>()
        json.optJSONArray("notes")?.let { array ->
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(notes::add)
            }
        }

        val options = mutableMapOf<String, Boolean>()
        json.optJSONObject("options")?.let { objectOptions ->
            for (key in objectOptions.keys()) {
                options[key] = objectOptions.optBoolean(key, true)
            }
        }

        val code = json.optJSONObject("code")
        val category = json.optString("category").takeIf { it.isNotBlank() }
            ?: if (type == "card") "membership" else "flight"

        return UiPass(
            category = category,
            title = json.optString("title").takeIf { it.isNotBlank() } ?: fallbackTitle,
            issuer = json.optString("issuer").takeIf { it.isNotBlank() && it != "null" } ?: "",
            holder = json.optString("holder").takeIf { it.isNotBlank() && it != "null" }
                ?: fields["passenger"].orEmpty(),
            fields = fields,
            notes = notes,
            codePayload = code?.optString("payload")?.takeIf { it.isNotBlank() && it != "null" },
            codeFormat = code?.optString("format")?.takeIf { it.isNotBlank() && it != "null" } ?: "qr",
            codeSource = code?.optString("source")?.takeIf { it.isNotBlank() && it != "null" } ?: "generated",
            themeColor = json.optString("themeColor").takeIf { it.isNotBlank() && it != "null" }
                ?: passThemeColor(category, json.optString("theme")),
            options = options,
            logo = json.optString("logo").takeIf { it.isNotBlank() && it != "null" },
        )
    }

    private fun fallbackUiPass(title: String, type: String): UiPass {
        val normalized = title.lowercase(Locale.getDefault())
        val category = when {
            type == "card" -> "membership"
            normalized.contains("vuelo") || normalized.contains("flight") ||
                normalized.contains("boarding") || normalized.contains("iberia") -> "flight"
            else -> "event"
        }

        return UiPass(
            category = category,
            title = title,
            issuer = "",
            holder = "",
            fields = emptyMap(),
            notes = emptyList(),
            codePayload = null,
            codeFormat = "qr",
            codeSource = "generated",
            themeColor = passThemeColor(category, null),
            options = emptyMap(),
            logo = null,
        )
    }

    private fun passThemeColor(category: String, theme: String?): String = when (theme) {
        "midnight" -> "#0c2340"
        "violet" -> "#4c1d95"
        "forest" -> "#064e3b"
        "obsidian" -> "#18181b"
        "burgundy" -> "#7f1d1d"
        "amber" -> "#9a3412"
        else -> when (category) {
            "flight" -> "#0c2340"
            "event" -> "#4c1d95"
            "transit" -> "#064e3b"
            "id" -> "#18181b"
            else -> "#9a3412"
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
        if (_state.value.scanForPass) {
            createPassFromScan(photo)
            return
        }

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
