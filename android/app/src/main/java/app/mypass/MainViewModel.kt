package app.mypass

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
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

private const val AVATAR_CACHE_FILE = "profile-avatar"
private const val MAX_AVATAR_SOURCE_BYTES = 20 * 1024 * 1024
private const val MAX_AVATAR_UPLOAD_BYTES = 4 * 1024 * 1024
private const val MAX_AVATAR_EDGE_PX = 512
private const val UPDATE_CHECK_INTERVAL_MS = 15L * 60L * 1000L

enum class Screen {
    Loading,
    Login,
    Pair,
    PinSetup,
    TwoFactor,
    RecoverVault,
    Locked,
    Home,
    Vault,
    Passes,
    PassEditor,
    Credentials,
    CredentialEditor,
    Medical,
    MedicalEditor,
    Family,
    Trash,
    Search,
    ShareImport,
    Backup,
    Scan,
    Settings,
    Notifications,
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
    val appearance: PassAppearance = PassAppearance(),
)

data class UiCredential(
    val category: String,
    val categoryLabel: String,
    val categoryIcon: String,
    val group: String,
    val logo: String?,
    val appearanceMode: String,
    val appearanceColor: String,
    val appearanceImage: String?,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val tags: List<String>,
    val favorite: Boolean,
    val passwordChangedAt: String?,
    val totpSecret: String?,
    val totpAlgorithm: String,
    val totpDigits: Int,
    val totpPeriod: Int,
)

data class CredentialDraft(
    val itemId: String? = null,
    val version: Int? = null,
    val category: String = "login",
    val categoryLabel: String = "",
    val categoryIcon: String = "",
    val group: String = "",
    val logo: String? = null,
    val appearanceMode: String = "none",
    val appearanceColor: String = "#1B4332",
    val appearanceImage: String? = null,
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",
    val tags: String = "",
    val favorite: Boolean = false,
    val passwordChangedAt: String? = null,
    val totpSecret: String? = null,
    val totpAlgorithm: String = "SHA1",
    val totpDigits: Int = 6,
    val totpPeriod: Int = 30,
    val existingBlobId: String? = null,
    val existingAttachmentName: String? = null,
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val clearAttachment: Boolean = false,
    val isNew: Boolean = true,
)

data class UiMedicalContact(
    val name: String = "",
    val relation: String = "",
    val phone: String = "",
)

data class UiMedicalInsurance(
    val provider: String = "",
    val plan: String = "",
    val policyNumber: String = "",
    val memberId: String = "",
    val phone: String = "",
)

data class UiMedicalDoctor(
    val name: String = "",
    val phone: String = "",
    val clinic: String = "",
)

data class UiMedication(
    val id: String = "",
    val name: String = "",
    val dose: String = "",
    val times: List<String> = emptyList(),
    val startDate: String = "",
    val endDate: String = "",
    val notes: String = "",
)

data class UiAppointment(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val notes: String = "",
    val reminderMinutes: Int = 1440,
)

data class UiMedicalCard(
    val holder: String = "",
    val birthDate: String = "",
    val bloodType: String = "",
    val allergies: List<String> = emptyList(),
    val conditions: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val medicationSchedule: List<UiMedication> = emptyList(),
    val appointments: List<UiAppointment> = emptyList(),
    val contacts: List<UiMedicalContact> = emptyList(),
    val insurance: UiMedicalInsurance = UiMedicalInsurance(),
    val doctor: UiMedicalDoctor = UiMedicalDoctor(),
    val organDonor: Boolean = false,
    val notes: String = "",
)

data class MedicalDraft(
    val itemId: String? = null,
    val version: Int? = null,
    val title: String = "",
    val holder: String = "",
    val birthDate: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val conditions: String = "",
    val medications: String = "",
    val medicationSchedule: List<UiMedication> = emptyList(),
    val appointments: List<UiAppointment> = emptyList(),
    val contactName: String = "",
    val contactRelation: String = "",
    val contactPhone: String = "",
    val extraContacts: List<UiMedicalContact> = emptyList(),
    val insuranceProvider: String = "",
    val insurancePlan: String = "",
    val insurancePolicy: String = "",
    val insuranceMember: String = "",
    val insurancePhone: String = "",
    val doctorName: String = "",
    val doctorPhone: String = "",
    val doctorClinic: String = "",
    val organDonor: Boolean = false,
    val notes: String = "",
    val expiresAt: String = "",
    val existingBlobId: String? = null,
    val existingAttachmentName: String? = null,
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val clearAttachment: Boolean = false,
    val isNew: Boolean = true,
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
    val appearance: PassAppearance = PassAppearance(),
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

data class SharedFile(
    val uri: String,
    val name: String,
    val mime: String,
    val size: Long,
)

data class UiContact(
    val id: String,
    val name: String,
    val email: String,
    val status: String,
    val incoming: Boolean = false,
    val direction: String = "unknown",
)

data class UiShare(
    val id: String,
    val sender: String,
    val itemType: String,
    val mode: String,
    val expiresAt: String?,
    val isOutgoing: Boolean = false,
    val status: String = "pending",
    val itemId: String? = null,
    val createdAt: String? = null,
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
    val shareId: String? = null,
    val itemId: String? = null,
    val contactId: String? = null,
    val requestId: String? = null,
    val versionCode: Int? = null,
    val versionName: String? = null,
)

data class FamilyPrompt(
    val key: String,
    val type: String,
    val id: String,
    val title: String,
    val message: String,
    val detail: String,
    val acceptLabel: String,
    val declineLabel: String,
    val notificationId: String? = null,
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
    val deletedAt: String? = null,
    val pendingAction: String? = null,
    val ocrText: String = "",
    val pass: UiPass? = null,
    val credential: UiCredential? = null,
    val medical: UiMedicalCard? = null,
    val metadataJson: JSONObject? = null,
)

data class UiState(
    val screen: Screen = Screen.Loading,
    val email: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val avatarPath: String? = null,
    val avatarVersion: Long = 0L,
    val error: String? = null,
    val status: String? = null,
    val busy: Boolean = false,
    val items: List<UiItem> = emptyList(),
    val trashItems: List<UiItem> = emptyList(),
    val recentItems: List<UiItem> = emptyList(),
    val focusItemId: String? = null,
    val pendingShortcut: String? = null,
    val pendingOpenItemId: String? = null,
    val sharedFiles: List<SharedFile> = emptyList(),
    val contacts: List<UiContact> = emptyList(),
    val incomingShares: List<UiShare> = emptyList(),
    val outgoingShares: List<UiShare> = emptyList(),
    val shareRequests: List<UiShareRequest> = emptyList(),
    val pendingPrompt: FamilyPrompt? = null,
    val pendingPromptTotal: Int = 0,
    val familyTab: String = "contacts",
    val notifications: List<UiNotification> = emptyList(),
    val familyStatus: String? = null,
    val twoFactorToken: String? = null,
    val twoFactorMethods: List<String> = emptyList(),
    val simpleMode: Boolean = false,
    val easyMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val ageGroup: String = "unknown",
    val birthDate: String = "",
    val recoveryEmail: String = "",
    val recoveryEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val viewerPath: String? = null,
    val viewerMime: String? = null,
    val viewerName: String? = null,
    val passDraft: PassDraft? = null,
    val credentialDraft: CredentialDraft? = null,
    val medicalDraft: MedicalDraft? = null,
    val scanForPass: Boolean = false,
    val scanForTotp: Boolean = false,
    val scanSourcePath: String? = null,
    val updateState: UpdateState = UpdateState.Idle,
    val dismissedUpdateCode: Int = 0,
    val lastUpdateCheck: Long = 0L,
    val showUpdateDialog: Boolean = false,
    val expiryReadIds: Set<String> = emptySet(),
    val notificationsReturnScreen: Screen = Screen.Home,
    val notificationRefresh: Long = 0L,
)

fun UiState.isItemShared(itemId: String): Boolean {
    return outgoingShares.any {
        it.itemId == itemId && (it.status == "pending" || it.status == "accepted")
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app: Application = application
    private val store = VaultStore(application)
    private val updateStore = UpdateStore(application)
    private val notificationStore = NotificationStore(application)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var masterKey: ByteArray? = null
    private var pendingMasterKey: ByteArray? = null
    private var pairingPrivateKey: PrivateKey? = null
    private var pairingPublicKey: String? = null
    private var pollJob: Job? = null
    private var updateJob: Job? = null
    private var viewerFile: File? = null
    private var identityPublicKey: ByteArray? = null
    private var identityPrivateKey: ByteArray? = null

    init {
        val cachedAvatar = avatarCacheFile().takeIf { it.exists() }
        val pendingRelease = updateStore.pendingRelease()
        val pendingPath = updateStore.pendingPath()?.let(::File)
        val initialUpdate = when {
            pendingRelease == null || pendingPath == null || !pendingPath.isFile -> {
                if (pendingRelease != null) {
                    updateStore.clearPending()
                }
                UpdateState.Idle
            }
            pendingRelease.versionCode <= BuildConfig.VERSION_CODE -> {
                pendingPath?.delete()
                updateStore.clearPending()
                UpdateState.Idle
            }
            AppUpdater.canInstall(app) -> {
                UpdateState.Ready(pendingRelease, pendingPath.absolutePath)
            }
            else -> {
                UpdateState.NeedsPermission(pendingRelease, pendingPath.absolutePath)
            }
        }

        _state.value = UiState(
            screen = when {
                store.hasVault() -> Screen.Locked
                store.token() != null -> Screen.Pair
                else -> Screen.Login
            },
            simpleMode = store.simpleMode(),
            easyMode = store.easyMode(),
            themeMode = store.themeMode(),
            ageGroup = store.ageGroup(),
            birthDate = store.birthDate(),
            recoveryEnabled = false,
            biometricEnabled = store.biometricEnabled(),
            email = store.email(),
            name = store.name(),
            avatarUrl = store.avatarUrl(),
            avatarPath = cachedAvatar?.absolutePath,
            avatarVersion = cachedAvatar?.lastModified() ?: 0L,
            updateState = initialUpdate,
            dismissedUpdateCode = updateStore.dismissedVersion(),
            lastUpdateCheck = updateStore.lastCheck(),
            expiryReadIds = notificationStore.readExpiryIds(),
        )

        refreshRecoveryStatus()
    }

    fun refreshRecoveryStatus() {
        val token = store.token() ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val status = runCatching { ApiClient.vaultRecoveryStatus(token) }.getOrNull() ?: return@launch
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    recoveryEnabled = status.optBoolean("enabled", false),
                    recoveryEmail = status.optString("masked_email").orEmpty(),
                )
            }
        }
    }

    private fun avatarCacheFile(): File = File(getApplication<Application>().filesDir, AVATAR_CACHE_FILE)

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

                    val account = body.optJSONObject("user")
                    val accountName = account
                        ?.optString("name")
                        ?.trim()
                        .orEmpty()
                    val avatarUrl = account
                        ?.optString("avatar_url")
                        ?.takeIf { it.isNotBlank() && it != "null" }
                        .orEmpty()
                    if (store.hasVault() && store.email().isNotBlank() && store.email() != email.trim()) {
                        store.clearAll()
                        AgendaReminders.cancelAll(getApplication())
                    }
                    store.saveToken(token)
                    store.saveEmail(email.trim())
                    store.saveName(accountName)
                    store.saveAvatarUrl(avatarUrl)
                    applyAccountPreferences(account)
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            screen = if (store.hasVault()) Screen.Locked else Screen.Pair,
                            email = email.trim(),
                            name = accountName,
                            avatarUrl = avatarUrl,
                            busy = false,
                            status = null,
                            twoFactorToken = null,
                            twoFactorMethods = emptyList(),
                            simpleMode = store.simpleMode(),
                            easyMode = store.easyMode(),
                            ageGroup = store.ageGroup(),
                            birthDate = store.birthDate(),
                            biometricEnabled = store.biometricEnabled(),
                        )
                    }
                    refreshRecoveryStatus()
                    refreshAvatar(avatarUrl)
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

                    val account = body.optJSONObject("user")
                    val accountEmail = account?.optString("email")
                        ?.takeIf { it.isNotBlank() }
                        ?: _state.value.email
                    val accountName = account
                        ?.optString("name")
                        ?.trim()
                        .orEmpty()
                    val avatarUrl = account
                        ?.optString("avatar_url")
                        ?.takeIf { it.isNotBlank() && it != "null" }
                        .orEmpty()
                    if (store.hasVault() && store.email().isNotBlank() && store.email() != accountEmail) {
                        store.clearAll()
                        AgendaReminders.cancelAll(getApplication())
                    }
                    if (accountEmail.isNotBlank()) {
                        store.saveEmail(accountEmail)
                    }
                    store.saveName(accountName)
                    store.saveAvatarUrl(avatarUrl)
                    store.saveToken(accessToken)
                    applyAccountPreferences(account)
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            screen = if (store.hasVault()) Screen.Locked else Screen.Pair,
                            email = accountEmail,
                            name = accountName,
                            avatarUrl = avatarUrl,
                            busy = false,
                            status = null,
                            error = null,
                            simpleMode = store.simpleMode(),
                            easyMode = store.easyMode(),
                            ageGroup = store.ageGroup(),
                            birthDate = store.birthDate(),
                            twoFactorToken = null,
                            twoFactorMethods = emptyList(),
                            biometricEnabled = store.biometricEnabled(),
                        )
                    }
                    refreshRecoveryStatus()
                    refreshAvatar(avatarUrl)
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
                if (token != null) {
                    val existingDeviceId = store.serverDeviceId()

                    if (existingDeviceId != null) {
                        runCatching {
                            ApiClient.updateDevice(
                                token,
                                existingDeviceId,
                                pinSalt = saltBase64,
                                pinParams = JSONObject()
                                    .put("ops_limit", Crypto.PIN_OPS)
                                    .put("mem_limit", Crypto.PIN_MEM)
                                    .put("algorithm", "argon2id13"),
                                wrappedMasterKey = wrappedBase64,
                            )
                        }
                    } else {
                        val publicKey = pairingPublicKey
                        if (publicKey != null) {
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
            credentialDraft = null,
            medicalDraft = null,
            scanForPass = false,
            scanForTotp = false,
            scanSourcePath = null,
            status = null,
            error = null,
        )
    }

    fun logout() {
        lock()
        store.clearToken()
        AgendaReminders.cancelAll(getApplication())
        _state.value = _state.value.copy(screen = Screen.Login)
    }

    fun sync() {
        val key = masterKey ?: return
        val token = store.token() ?: return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                checkDeviceAllowed(token)
                loadAccountProfile(token)
                refreshRecoveryStatus()
                loadIdentityKeys(key, token)
                processPendingActions(token)
                uploadPending(key, token)
                fetchServer(key, token)
                refreshItems(key)
                loadFamily(token)
                loadNotifications(token)
                Reminders.checkNow(getApplication())
                FamilyNotifier.schedule(getApplication())
                withContext(Dispatchers.Main) {
                    checkForUpdates(manual = false)
                }
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

    private fun applyAccountPreferences(account: org.json.JSONObject?) {
        if (account == null) {
            return
        }

        val birthDate = account.optString("birth_date").takeIf { it.isNotBlank() && it != "null" }.orEmpty()
        val ageGroup = account.optString("age_group").takeIf { it.isNotBlank() }.orEmpty()
        val simpleMode = account.optBoolean("simple_mode", store.simpleMode())
        val easyMode = account.optBoolean("easy_mode", store.easyMode())

        store.setBirthDate(birthDate)
        if (ageGroup.isNotBlank()) {
            store.setAgeGroup(ageGroup)
        }
        store.setSimpleMode(simpleMode)
        store.setEasyMode(easyMode)
    }

    private suspend fun loadAccountProfile(token: String) {
        val user = runCatching { ApiClient.me(token) }.getOrNull() ?: return
        val name = user.optString("name").trim()
        val avatarUrl = user.optString("avatar_url")
            .takeIf { it.isNotBlank() && it != "null" }
            .orEmpty()

        applyAccountPreferences(user)
        withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(
                simpleMode = store.simpleMode(),
                easyMode = store.easyMode(),
                ageGroup = store.ageGroup(),
                birthDate = store.birthDate(),
            )
        }

        if (name.isNotBlank() && name != _state.value.name) {
            store.saveName(name)
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(name = name)
            }
        }

        if (avatarUrl.isBlank() && avatarCacheFile().exists()) {
            refreshAvatar("")
        } else if (avatarUrl != store.avatarUrl() || avatarUrl != _state.value.avatarUrl) {
            store.saveAvatarUrl(avatarUrl)
            refreshAvatar(avatarUrl)
        } else if (avatarUrl.isNotBlank() && !avatarCacheFile().exists()) {
            refreshAvatar(avatarUrl)
        }
    }

    private suspend fun refreshAvatar(url: String) {
        val cacheFile = avatarCacheFile()

        if (url.isBlank()) {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            store.saveAvatarUrl("")
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    avatarUrl = "",
                    avatarPath = null,
                    avatarVersion = System.currentTimeMillis(),
                )
            }
            return
        }

        if (url == _state.value.avatarUrl && cacheFile.exists()) {
            return
        }

        val bytes = runCatching { ApiClient.downloadUrl(url) }.getOrNull() ?: return
        if (bytes.isEmpty()) {
            return
        }
        runCatching { cacheFile.writeBytes(bytes) }.onFailure { return }

        val validImage = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(cacheFile.absolutePath, bounds)
            bounds.outWidth > 0 && bounds.outHeight > 0
        }.getOrDefault(false)

        if (!validImage) {
            cacheFile.delete()
            return
        }

        store.saveAvatarUrl(url)
        withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(
                avatarUrl = url,
                avatarPath = cacheFile.absolutePath,
                avatarVersion = System.currentTimeMillis(),
            )
        }
    }

    fun uploadAvatar(uri: Uri) {
        val token = store.token()
        if (token == null) {
            showError(text(R.string.error_sign_in_again))
            return
        }
        if (_state.value.busy) {
            return
        }

        _state.value = _state.value.copy(
            busy = true,
            error = null,
            status = text(R.string.status_avatar_uploading),
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bytes = prepareAvatar(uri)
                val response = ApiClient.uploadAvatar(token, bytes)
                val url = response.optString("avatar_url")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?: error(text(R.string.error_avatar_upload))

                val cacheFile = avatarCacheFile()
                cacheFile.writeBytes(bytes)
                store.saveAvatarUrl(url)

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        avatarUrl = url,
                        avatarPath = cacheFile.absolutePath,
                        avatarVersion = System.currentTimeMillis(),
                        busy = false,
                        status = text(R.string.status_avatar_updated),
                        error = null,
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_avatar_upload),
                    )
                }
            }
        }
    }

    fun deleteAvatar() {
        val token = store.token()
        if (token == null) {
            showError(text(R.string.error_sign_in_again))
            return
        }
        if (_state.value.busy) {
            return
        }

        _state.value = _state.value.copy(
            busy = true,
            error = null,
            status = text(R.string.status_avatar_removing),
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ApiClient.deleteAvatar(token) }
                .onSuccess {
                    clearAvatarCache()
                    store.saveAvatarUrl("")
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            avatarUrl = "",
                            avatarPath = null,
                            avatarVersion = System.currentTimeMillis(),
                            busy = false,
                            status = text(R.string.status_avatar_removed),
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            busy = false,
                            status = null,
                            error = error.message ?: text(R.string.error_avatar_remove),
                        )
                    }
                }
        }
    }

    private fun clearAvatarCache() {
        avatarCacheFile().delete()
    }

    private fun prepareAvatar(uri: Uri): ByteArray {
        val resolver = getApplication<Application>().contentResolver
        val source = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error(text(R.string.error_avatar_file))

        if (source.size > MAX_AVATAR_SOURCE_BYTES) {
            error(text(R.string.error_avatar_file))
        }

        val bitmap = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(source))) { decoder, info, _ ->
                val longestEdge = maxOf(info.size.width, info.size.height)
                if (longestEdge > MAX_AVATAR_EDGE_PX) {
                    val scale = MAX_AVATAR_EDGE_PX.toFloat() / longestEdge
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.getOrNull() ?: error(text(R.string.error_avatar_file))

        val output = ByteArrayOutputStream()
        val compressed = runCatching {
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 86, output)
        }.getOrDefault(false)
        bitmap.recycle()

        if (!compressed) {
            error(text(R.string.error_avatar_file))
        }

        val bytes = output.toByteArray()
        if (bytes.isEmpty() || bytes.size > MAX_AVATAR_UPLOAD_BYTES) {
            error(text(R.string.error_avatar_file))
        }

        return bytes
    }

    private suspend fun checkDeviceAllowed(token: String) {
        val deviceId = store.serverDeviceId() ?: return
        val device = ApiClient.getDevice(token, deviceId)
        if (device.optBoolean("revoked", false) || !device.isNull("revoked_at")) {
            withContext(Dispatchers.Main) {
                store.clearAll()
                AgendaReminders.cancelAll(getApplication())
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
                    incoming = contact.optBoolean("incoming", false),
                    direction = contact.optString("direction", "unknown"),
                ),
            )
        }

        val incoming = mutableListOf<UiShare>()
        val outgoing = mutableListOf<UiShare>()

        for (direction in listOf("incoming" to false, "outgoing" to true)) {
            val array = sharesResponse.optJSONArray(direction.first) ?: JSONArray()
            for (index in 0 until array.length()) {
                val share = array.getJSONObject(index)
                val status = share.optString("status", "pending")

                if (!direction.second && status != "pending") {
                    continue
                }

                val parsed = UiShare(
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
                    status = status,
                    itemId = share.optString("item_id").takeIf { it.isNotBlank() && it != "null" },
                    createdAt = share.optString("created_at").takeIf { it.isNotBlank() && it != "null" },
                )

                if (direction.second) {
                    outgoing.add(parsed)
                } else {
                    incoming.add(parsed)
                }
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
                outgoingShares = outgoing.sortedByDescending { it.createdAt.orEmpty() },
                shareRequests = requestList,
            )
            queueFamilyPrompts()
        }
    }

    private fun loadNotifications(token: String) {
        val response = ApiClient.listNotifications(token)
        val array = response.optJSONArray("data") ?: JSONArray()
        val notifications = mutableListOf<UiNotification>()

        for (index in 0 until array.length()) {
            val json = array.getJSONObject(index)
            val type = json.optString("type")

            // La actualización de la app se controla con el manifiesto APK y
            // tiene su propia sección en la campana. El aviso del servidor es
            // para la web y no debe duplicarse aquí.
            if (type == "app_update") {
                continue
            }

            val data = json.optJSONObject("data") ?: JSONObject()
            notifications.add(
                UiNotification(
                    id = json.getString("id"),
                    type = type,
                    from = data.optString("from"),
                    documentType = data.optString("document_type"),
                    read = !json.isNull("read_at") &&
                        json.optString("read_at").isNotBlank() &&
                        json.optString("read_at") != "null",
                    createdAt = json.optString("created_at").takeIf { it.isNotBlank() && it != "null" },
                    shareId = data.optString("share_id").takeIf { it.isNotBlank() && it != "null" },
                    itemId = data.optString("item_id").takeIf { it.isNotBlank() && it != "null" },
                    contactId = data.optString("contact_id").takeIf { it.isNotBlank() && it != "null" },
                    requestId = data.optString("request_id").takeIf { it.isNotBlank() && it != "null" },
                    versionCode = data.optInt("version_code", 0).takeIf { it > 0 },
                    versionName = data.optString("version_name").takeIf { it.isNotBlank() && it != "null" },
                ),
            )
        }

        viewModelScope.launch(Dispatchers.Main) {
            _state.value = _state.value.copy(notifications = notifications)
            queueFamilyPrompts()
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
        markExpiryNotificationsSeen()
        if (_state.value.updateState.release != null) {
            dismissUpdate()
        }
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ApiClient.markAllNotificationsRead(token)
                loadNotifications(token)
            }
        }
    }

    fun openNotifications() {
        val current = _state.value.screen
        val returnScreen = if (current == Screen.Notifications) {
            _state.value.notificationsReturnScreen
        } else if (current in setOf(
                Screen.Home,
                Screen.Vault,
                Screen.Passes,
                Screen.Credentials,
                Screen.Medical,
                Screen.Settings,
            )
        ) {
            current
        } else {
            Screen.Home
        }

        _state.value = _state.value.copy(
            screen = Screen.Notifications,
            notificationsReturnScreen = returnScreen,
            error = null,
            status = null,
        )
    }

    fun closeNotifications() {
        _state.value = _state.value.copy(
            screen = _state.value.notificationsReturnScreen,
            error = null,
            status = null,
        )
    }

    fun onNotificationsOpened() {
        refreshNotificationsAndUpdates()
    }

    fun refreshNotificationsAndUpdates() {
        markExpiryNotificationsSeen()
        refreshNotifications()
        refreshFamily()
        checkForUpdates(manual = true)
    }

    private fun refreshFamily() {
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                loadFamily(token)
            }
        }
    }

    fun markExpiryNotificationsSeen() {
        val ids = _state.value.items.mapNotNull { item ->
            val expiresAt = item.expiresAt ?: return@mapNotNull null
            val days = expiryDays(expiresAt) ?: return@mapNotNull null
            if (days <= 7) expiryNotificationId(item.id, expiresAt) else null
        }.toSet()

        if (ids.isEmpty()) {
            return
        }

        notificationStore.markExpirySeen(ids)
        _state.value = _state.value.copy(expiryReadIds = notificationStore.readExpiryIds())
    }

    fun openNotificationEntry(entry: NotificationEntry) {
        when (entry.kind) {
            NotificationKind.Update -> openUpdateDialog()

            NotificationKind.Expired,
            NotificationKind.Expiring,
            -> {
                markExpiryEntrySeen(entry.id)
                val item = _state.value.items.firstOrNull { it.id == entry.itemId } ?: return
                when {
                    item.credential != null -> openCredentials()
                    item.medical != null -> openMedical()
                    item.pass != null -> openPasses()
                    else -> openItem(item)
                }
            }

            NotificationKind.Activity -> {
                entry.activityId?.let(::markNotificationRead)
                when (entry.activityType) {
                    "contact_request",
                    "contact_accepted",
                    -> openFamily("contacts")

                    "share_received" -> openFamily("received")
                    "share_accepted" -> openFamily("sent")
                    "document_request" -> openFamily("requests")

                    else -> Unit
                }
            }
        }
    }

    fun checkForUpdatesManually() {
        checkForUpdates(manual = true)
    }

    fun onAppResumed() {
        val current = _state.value.updateState
        if (current is UpdateState.NeedsPermission && AppUpdater.canInstall(app)) {
            _state.value = _state.value.copy(
                updateState = UpdateState.Ready(current.release, current.path),
            )
        }

        _state.value = _state.value.copy(notificationRefresh = System.currentTimeMillis())

        if (_state.value.screen !in setOf(
                Screen.Loading,
                Screen.Login,
                Screen.Pair,
                Screen.PinSetup,
                Screen.TwoFactor,
                Screen.RecoverVault,
                Screen.Locked,
            )
        ) {
            val token = store.token()
            if (token != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    runCatching {
                        loadFamily(token)
                        loadNotifications(token)
                    }
                }
            }
        }

        checkForUpdates(manual = false)
    }

    private fun checkForUpdates(manual: Boolean) {
        if (updateJob?.isActive == true) {
            return
        }

        val now = System.currentTimeMillis()
        val previous = _state.value.updateState
        if (previous is UpdateState.Downloading) {
            return
        }
        if (!manual &&
            previous !is UpdateState.Ready &&
            previous !is UpdateState.NeedsPermission &&
            updateStore.lastCheck() > 0L &&
            now - updateStore.lastCheck() < UPDATE_CHECK_INTERVAL_MS
        ) {
            return
        }

        _state.value = _state.value.copy(
            updateState = if (previous is UpdateState.Ready || previous is UpdateState.NeedsPermission) {
                previous
            } else {
                UpdateState.Checking
            },
        )

        updateJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching { ApiClient.appVersion() }
                .onSuccess { raw ->
                    val release = AppRelease.fromJson(raw)
                    if (release == null) {
                        error(text(R.string.updates_check_failed, text(R.string.error_check_connection)))
                    }

                    val checkedAt = System.currentTimeMillis()
                    updateStore.setLastCheck(checkedAt)
                    val pending = updateStore.pendingRelease()
                    val pendingPath = updateStore.pendingPath()?.let(::File)
                    val pendingMatches = pending?.versionCode == release.versionCode &&
                        pendingPath != null &&
                        pendingPath.isFile

                    val nextState = when {
                        release.versionCode <= BuildConfig.VERSION_CODE -> {
                            pendingPath?.delete()
                            updateStore.clearPending()
                            UpdateState.UpToDate
                        }

                        pendingMatches && AppUpdater.canInstall(app) -> {
                            UpdateState.Ready(release, pendingPath!!.absolutePath)
                        }

                        pendingMatches -> {
                            UpdateState.NeedsPermission(release, pendingPath!!.absolutePath)
                        }

                        else -> {
                            updateStore.clearPending()
                            UpdateState.Available(release)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            updateState = nextState,
                            lastUpdateCheck = checkedAt,
                        )
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            updateState = if (manual || previous is UpdateState.Available) {
                                UpdateState.Failed(
                                    previous.release,
                                    text(
                                        R.string.updates_check_failed,
                                        error.message ?: text(R.string.error_check_connection),
                                    ),
                                )
                            } else {
                                previous
                            },
                        )
                    }
                }
        }
    }

    fun downloadUpdate() {
        val current = _state.value.updateState
        val release = current.release ?: return
        if (current is UpdateState.Downloading) {
            return
        }

        updateStore.dismiss(release.versionCode)
        _state.value = _state.value.copy(
            dismissedUpdateCode = release.versionCode,
            updateState = UpdateState.Downloading(
                release = release,
                downloadedBytes = 0L,
                totalBytes = release.sizeBytes,
            ),
        )

        viewModelScope.launch(Dispatchers.IO) {
            var lastProgressAt = 0L

            runCatching {
                val file = AppUpdater.download(app, release) { downloaded, total ->
                    val timestamp = System.currentTimeMillis()
                    if (timestamp - lastProgressAt >= 150L || (total > 0L && downloaded >= total)) {
                        lastProgressAt = timestamp
                        viewModelScope.launch(Dispatchers.Main) {
                            val live = _state.value.updateState
                            if (live is UpdateState.Downloading &&
                                live.release.versionCode == release.versionCode
                            ) {
                                _state.value = _state.value.copy(
                                    updateState = live.copy(
                                        downloadedBytes = downloaded,
                                        totalBytes = if (total > 0L) total else release.sizeBytes,
                                    ),
                                )
                            }
                        }
                    }
                }

                if (!AppUpdater.signatureMatches(app, file, release.versionCode)) {
                    file.delete()
                    error(text(R.string.updates_signature_error))
                }

                file
            }.onSuccess { file ->
                updateStore.savePending(release, file.absolutePath)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        updateState = if (AppUpdater.canInstall(app)) {
                            UpdateState.Ready(release, file.absolutePath)
                        } else {
                            UpdateState.NeedsPermission(release, file.absolutePath)
                        },
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        updateState = UpdateState.Failed(
                            release,
                            text(
                                R.string.updates_failed,
                                error.message ?: text(R.string.error_check_connection),
                            ),
                        ),
                    )
                }
            }
        }
    }

    fun installUpdate() {
        val current = _state.value.updateState
        val release = current.release ?: return
        val path = when (current) {
            is UpdateState.Ready -> current.path
            is UpdateState.NeedsPermission -> current.path
            else -> return
        }

        if (!AppUpdater.canInstall(app)) {
            _state.value = _state.value.copy(
                updateState = UpdateState.NeedsPermission(release, path),
                showUpdateDialog = false,
            )
            return
        }

        runCatching { AppUpdater.install(app, File(path)) }
            .onSuccess { launched ->
                if (launched) {
                    _state.value = _state.value.copy(
                        status = text(R.string.updates_installing),
                        showUpdateDialog = false,
                    )
                } else {
                    _state.value = _state.value.copy(
                        updateState = UpdateState.Failed(
                            release,
                            text(R.string.updates_failed, text(R.string.error_check_connection)),
                        ),
                        showUpdateDialog = false,
                    )
                }
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    updateState = UpdateState.Failed(
                        release,
                        text(
                            R.string.updates_failed,
                            error.message ?: text(R.string.error_check_connection),
                        ),
                    ),
                    showUpdateDialog = false,
                )
            }
    }

    fun openInstallPermissionSettings() {
        AppUpdater.openInstallPermissionSettings(app)
    }

    fun dismissUpdate() {
        val release = _state.value.updateState.release ?: return
        updateStore.dismiss(release.versionCode)
        _state.value = _state.value.copy(dismissedUpdateCode = release.versionCode)
    }

    fun openUpdateDialog() {
        if (_state.value.updateState.release != null) {
            _state.value = _state.value.copy(showUpdateDialog = true)
        }
    }

    fun dismissUpdateDialog() {
        _state.value = _state.value.copy(showUpdateDialog = false)
    }

    private fun markExpiryEntrySeen(id: String) {
        notificationStore.markExpirySeen(setOf(id))
        _state.value = _state.value.copy(expiryReadIds = notificationStore.readExpiryIds())
    }

    private fun refreshNotifications() {
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { loadNotifications(token) }
        }
    }

    fun backToVault() {
        _state.value = _state.value.copy(
            screen = Screen.Vault,
            error = null,
            familyStatus = null,
            passDraft = null,
            credentialDraft = null,
            medicalDraft = null,
        )
    }

    fun backToHome() {
        _state.value = _state.value.copy(
            screen = Screen.Home,
            error = null,
            familyStatus = null,
            passDraft = null,
            credentialDraft = null,
            medicalDraft = null,
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

    fun openCredentials() {
        _state.value = _state.value.copy(
            screen = Screen.Credentials,
            credentialDraft = null,
            error = null,
            status = null,
        )
    }

    fun openMedical() {
        _state.value = _state.value.copy(
            screen = Screen.Medical,
            medicalDraft = null,
            error = null,
            status = null,
        )
    }

    fun newCredential() {
        _state.value = _state.value.copy(
            screen = Screen.CredentialEditor,
            credentialDraft = CredentialDraft(),
            error = null,
            status = null,
        )
    }

    fun editCredential(item: UiItem) {
        val credential = item.credential ?: return
        recordRecentItem(item.id)
        val local = store.findItem(item.id)
        _state.value = _state.value.copy(
            screen = Screen.CredentialEditor,
            credentialDraft = CredentialDraft(
                itemId = item.id,
                version = local?.version ?: 1,
                category = credential.category,
                categoryLabel = credential.categoryLabel,
                categoryIcon = credential.categoryIcon,
                group = credential.group,
                logo = credential.logo,
                appearanceMode = credential.appearanceMode,
                appearanceColor = credential.appearanceColor,
                appearanceImage = credential.appearanceImage,
                title = item.title,
                username = credential.username,
                password = credential.password,
                url = credential.url,
                notes = credential.notes,
                tags = credential.tags.joinToString(", "),
                favorite = credential.favorite,
                passwordChangedAt = credential.passwordChangedAt,
                totpSecret = credential.totpSecret,
                totpAlgorithm = credential.totpAlgorithm,
                totpDigits = credential.totpDigits,
                totpPeriod = credential.totpPeriod,
                existingBlobId = item.blobId,
                existingAttachmentName = item.blobId?.let {
                    item.metadataJson?.optString("original_name")
                        ?.takeIf { name -> name.isNotBlank() && name != "null" }
                },
                isNew = false,
            ),
            error = null,
            status = null,
        )
    }

    fun discardCredentialDraft() {
        _state.value = _state.value.copy(
            screen = Screen.Credentials,
            credentialDraft = null,
            busy = false,
            error = null,
            status = null,
        )
    }

    fun newMedicalCard() {
        _state.value = _state.value.copy(
            screen = Screen.MedicalEditor,
            medicalDraft = MedicalDraft(),
            error = null,
            status = null,
        )
    }

    fun editMedicalCard(item: UiItem) {
        val medical = item.medical ?: return
        recordRecentItem(item.id)
        val local = store.findItem(item.id)
        val contact = medical.contacts.firstOrNull() ?: UiMedicalContact()
        _state.value = _state.value.copy(
            screen = Screen.MedicalEditor,
            medicalDraft = MedicalDraft(
                itemId = item.id,
                version = local?.version ?: 1,
                title = item.title,
                holder = medical.holder,
                birthDate = medical.birthDate,
                bloodType = medical.bloodType,
                allergies = medical.allergies.joinToString(", "),
                conditions = medical.conditions.joinToString(", "),
                medications = medical.medications.joinToString(", "),
                medicationSchedule = medical.medicationSchedule,
                appointments = medical.appointments,
                contactName = contact.name,
                contactRelation = contact.relation,
                contactPhone = contact.phone,
                extraContacts = medical.contacts.drop(1),
                insuranceProvider = medical.insurance.provider,
                insurancePlan = medical.insurance.plan,
                insurancePolicy = medical.insurance.policyNumber,
                insuranceMember = medical.insurance.memberId,
                insurancePhone = medical.insurance.phone,
                doctorName = medical.doctor.name,
                doctorPhone = medical.doctor.phone,
                doctorClinic = medical.doctor.clinic,
                organDonor = medical.organDonor,
                notes = medical.notes,
                expiresAt = item.expiresAt?.take(10).orEmpty(),
                existingBlobId = item.blobId,
                existingAttachmentName = item.blobId?.let {
                    item.metadataJson?.optString("original_name")
                        ?.takeIf { name -> name.isNotBlank() && name != "null" }
                },
                isNew = false,
            ),
            error = null,
            status = null,
        )
    }

    fun discardMedicalDraft() {
        _state.value = _state.value.copy(
            screen = Screen.Medical,
            medicalDraft = null,
            busy = false,
            error = null,
            status = null,
        )
    }

    fun openFamily(tab: String = "contacts") {
        _state.value = _state.value.copy(
            screen = Screen.Family,
            familyTab = tab,
            error = null,
            familyStatus = null,
        )
        val token = store.token() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                loadFamily(token)
                loadNotifications(token)
            }
        }
    }

    fun setFamilyTab(tab: String) {
        _state.value = _state.value.copy(familyTab = tab)
    }

    private fun queueFamilyPrompts() {
        val current = _state.value
        val seen = notificationStore.promptedFamilyKeys()
        val prompts = mutableListOf<FamilyPrompt>()

        current.incomingShares
            .filter { it.status == "pending" && !it.isOutgoing }
            .forEach { share ->
                val notification = current.notifications.firstOrNull {
                    !it.read && it.type == "share_received" && it.shareId == share.id
                }
                prompts += FamilyPrompt(
                    key = "share:${share.id}",
                    type = "share",
                    id = share.id,
                    title = text(R.string.prompt_share_title),
                    message = text(R.string.prompt_share_message, share.sender),
                    detail = share.expiresAt?.let { text(R.string.prompt_expires, it.take(10)) }
                        ?: text(R.string.prompt_share_detail),
                    acceptLabel = text(R.string.accept_and_save),
                    declineLabel = text(R.string.decline),
                    notificationId = notification?.id,
                )
            }

        current.contacts
            .filter { it.status == "pending" && it.incoming }
            .forEach { contact ->
                val notification = current.notifications.firstOrNull {
                    !it.read && it.type == "contact_request" && it.contactId == contact.id
                }
                prompts += FamilyPrompt(
                    key = "contact:${contact.id}",
                    type = "contact",
                    id = contact.id,
                    title = text(R.string.prompt_contact_title),
                    message = text(R.string.prompt_contact_message, contact.name),
                    detail = contact.email,
                    acceptLabel = text(R.string.accept_contact),
                    declineLabel = text(R.string.block),
                    notificationId = notification?.id,
                )
            }

        current.shareRequests
            .filter { it.incoming }
            .forEach { request ->
                val notification = current.notifications.firstOrNull {
                    !it.read && it.type == "document_request" && it.requestId == request.id
                }
                prompts += FamilyPrompt(
                    key = "request:${request.id}",
                    type = "request",
                    id = request.id,
                    title = text(R.string.prompt_request_title),
                    message = text(R.string.prompt_request_message, request.person, request.documentType),
                    detail = request.purpose.ifBlank { text(R.string.prompt_request_detail) },
                    acceptLabel = text(R.string.prompt_view_request),
                    declineLabel = text(R.string.decline),
                    notificationId = notification?.id,
                )
            }

        val pending = prompts.filterNot { seen.contains(it.key) }
        _state.value = current.copy(
            pendingPrompt = pending.firstOrNull(),
            pendingPromptTotal = pending.size,
        )
    }

    fun dismissFamilyPrompt() {
        val prompt = _state.value.pendingPrompt ?: return
        notificationStore.markFamilyPrompted(prompt.key)
        _state.value = _state.value.copy(pendingPrompt = null, pendingPromptTotal = 0)
        queueFamilyPrompts()
    }

    fun acceptFamilyPrompt() {
        val prompt = _state.value.pendingPrompt ?: return
        notificationStore.markFamilyPrompted(prompt.key)
        prompt.notificationId?.let(::markNotificationRead)
        _state.value = _state.value.copy(pendingPrompt = null, pendingPromptTotal = 0)

        when (prompt.type) {
            "share" -> acceptShare(prompt.id)
            "contact" -> acceptContact(prompt.id)
            "request" -> {
                _state.value = _state.value.copy(
                    screen = Screen.Family,
                    familyTab = "requests",
                    familyStatus = text(R.string.prompt_request_open),
                )
                val token = store.token()
                if (token != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        runCatching { loadFamily(token) }
                    }
                }
            }
        }

        queueFamilyPrompts()
    }

    fun declineFamilyPrompt() {
        val prompt = _state.value.pendingPrompt ?: return
        notificationStore.markFamilyPrompted(prompt.key)
        prompt.notificationId?.let(::markNotificationRead)
        _state.value = _state.value.copy(pendingPrompt = null, pendingPromptTotal = 0)

        when (prompt.type) {
            "share" -> declineShare(prompt.id)
            "contact" -> blockContact(prompt.id)
            "request" -> declineShareRequest(prompt.id)
        }

        queueFamilyPrompts()
    }

    fun openScanner(forPass: Boolean = false) {
        _state.value = _state.value.copy(
            screen = Screen.Scan,
            error = null,
            status = null,
            scanForPass = forPass,
            scanForTotp = false,
        )
    }

    fun openTotpScanner() {
        _state.value = _state.value.copy(
            screen = Screen.Scan,
            error = null,
            status = null,
            scanForPass = false,
            scanForTotp = true,
        )
    }

    fun backFromScan() {
        val target = when {
            _state.value.scanForTotp -> if (_state.value.credentialDraft == null) Screen.Credentials else Screen.CredentialEditor
            _state.value.scanForPass -> Screen.Passes
            else -> Screen.Vault
        }
        _state.value = _state.value.copy(
            screen = target,
            error = null,
            status = null,
            scanForPass = false,
            scanForTotp = false,
        )
    }

    fun openSettings() {
        _state.value = _state.value.copy(screen = Screen.Settings, error = null, status = null)
    }

    fun openTrash() {
        _state.value = _state.value.copy(screen = Screen.Trash, error = null, status = null)
    }

    fun addSharedUris(uris: List<Uri>) {
        if (uris.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            val files = uris.distinctBy { it.toString() }.mapNotNull { uri ->
                runCatching {
                    val name = resolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }?.takeIf { it.isNotBlank() }
                        ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                        ?: "documento"

                    val mime = resolver.getType(uri) ?: "application/octet-stream"
                    val size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                    SharedFile(uri.toString(), name, mime, size)
                }.getOrNull()
            }

            if (files.isEmpty()) {
                return@launch
            }

            withContext(Dispatchers.Main) {
                val merged = (_state.value.sharedFiles + files).distinctBy { it.uri }
                _state.value = _state.value.copy(
                    sharedFiles = merged,
                    screen = if (masterKey != null) Screen.ShareImport else _state.value.screen,
                    error = null,
                    status = null,
                )
            }
        }
    }

    fun openShareImport() {
        if (_state.value.sharedFiles.isEmpty() || masterKey == null) {
            return
        }
        _state.value = _state.value.copy(
            screen = Screen.ShareImport,
            error = null,
            status = null,
        )
    }

    fun discardSharedFiles() {
        _state.value = _state.value.copy(
            sharedFiles = emptyList(),
            screen = Screen.Home,
            error = null,
            status = null,
        )
    }

    fun exportBackup(passphrase: String, uri: Uri) {
        val key = masterKey ?: return
        val token = store.token()
        if (passphrase.length < 12) {
            _state.value = _state.value.copy(error = text(R.string.backup_error_short))
            return
        }

        _state.value = _state.value.copy(
            busy = true,
            error = null,
            status = text(R.string.backup_exporting),
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val entries = linkedMapOf<String, ByteArray>()
                val manifestItems = JSONArray()
                val items = _state.value.items.filter { it.state == "active" || it.state == "received" }

                for (item in items) {
                    val local = store.findItem(item.id) ?: continue
                    val metadata = item.metadataJson ?: continue
                    var localPath = local.localPath

                    if (localPath == null && local.blobId != null && token != null) {
                        val bytes = ApiClient.downloadBlob(token, local.blobId)
                        localPath = store.writeVaultFile(item.id, bytes).absolutePath
                    }

                    val manifestItem = JSONObject()
                        .put("type", item.type)
                        .put("expires_at", item.expiresAt ?: JSONObject.NULL)
                        .put("metadata", metadata)

                    if (localPath != null) {
                        val plaintext = Crypto.decryptFile(key, File(localPath).readBytes())
                        val entryName = "files/${item.id}.bin"
                        entries[entryName] = plaintext
                        manifestItem.put(
                            "file",
                            JSONObject()
                                .put("entry", entryName)
                                .put("name", item.originalName)
                                .put("mime", item.mime)
                                .put("size", plaintext.size),
                        )
                    }
                    manifestItems.put(manifestItem)
                }

                val manifest = JSONObject()
                    .put("format", 1)
                    .put("app", "My Pass")
                    .put("app_version", BuildConfig.VERSION_NAME)
                    .put("created_at", java.time.Instant.now().toString())
                    .put("items", manifestItems)
                entries["manifest.json"] = manifest.toString().toByteArray(Charsets.UTF_8)

                val container = BackupContainer.encrypt(passphrase, BackupContainer.zip(entries))
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(container)
                } ?: error(text(R.string.backup_error_write))

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = text(R.string.backup_exported, items.size),
                        error = null,
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.backup_error_export),
                    )
                }
            }
        }
    }

    fun importBackup(passphrase: String, uri: Uri) {
        val key = masterKey ?: return
        if (passphrase.isBlank()) {
            _state.value = _state.value.copy(error = text(R.string.backup_error_empty))
            return
        }

        _state.value = _state.value.copy(
            busy = true,
            error = null,
            status = text(R.string.backup_importing),
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val resolver = getApplication<Application>().contentResolver
                val container = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error(text(R.string.backup_error_read))
                check(container.size <= 1024 * 1024 * 1024) { text(R.string.backup_error_too_large) }

                val entries = BackupContainer.unzip(BackupContainer.decrypt(passphrase, container))
                val manifestBytes = entries["manifest.json"] ?: error(text(R.string.backup_error_manifest))
                val manifest = JSONObject(String(manifestBytes, Charsets.UTF_8))
                val items = manifest.optJSONArray("items") ?: JSONArray()
                var imported = 0

                for (index in 0 until items.length()) {
                    val entry = items.optJSONObject(index) ?: continue
                    val metadata = entry.optJSONObject("metadata") ?: continue
                    val id = store.newItemId()
                    val fileInfo = entry.optJSONObject("file")
                    var localPath: String? = null

                    if (fileInfo != null) {
                        val fileName = fileInfo.optString("entry")
                        val plaintext = entries[fileName] ?: continue
                        val encrypted = Crypto.encryptFile(key, plaintext)
                        localPath = store.writeVaultFile(id, encrypted).absolutePath
                        metadata
                            .put("original_name", fileInfo.optString("name", "documento"))
                            .put("mime", fileInfo.optString("mime", "application/octet-stream"))
                            .put("size", plaintext.size)
                    }

                    val encryptedMetadata = Base64.encodeToString(
                        Crypto.seal(key, metadata.toString().toByteArray(Charsets.UTF_8)),
                        Base64.NO_WRAP,
                    )
                    val expiresAt = entry.optString("expires_at")
                        .takeIf { it.isNotBlank() && it != "null" }
                    store.upsert(
                        LocalItem(
                            id = id,
                            type = entry.optString("type", "document"),
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
                    imported += 1
                }

                refreshItems(key)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.Vault,
                        busy = false,
                        status = text(R.string.backup_imported, imported),
                        error = null,
                    )
                }
                if (imported > 0) {
                    sync()
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.backup_error_import),
                    )
                }
            }
        }
    }

    fun saveSharedFiles() {
        val key = masterKey ?: return
        val files = _state.value.sharedFiles
        if (files.isEmpty() || _state.value.busy) {
            return
        }

        _state.value = _state.value.copy(
            busy = true,
            error = null,
            status = text(R.string.status_saving_shared_files),
        )

        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            var saved = 0
            var failed = 0

            for (shared in files) {
                runCatching {
                    val uri = Uri.parse(shared.uri)
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error(text(R.string.error_read_file))
                    if (bytes.size > 100 * 1024 * 1024) {
                        error(text(R.string.error_file_too_large))
                    }

                    val id = store.newItemId()
                    val title = shared.name.substringBeforeLast('.').ifBlank { text(R.string.document) }
                    val metadata = JSONObject()
                        .put("title", title)
                        .put("type", "document")
                        .put("original_name", shared.name)
                        .put("mime", shared.mime)
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
                    saved += 1
                }.onFailure {
                    failed += 1
                }
            }

            refreshItems(key)
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    busy = false,
                    sharedFiles = emptyList(),
                    screen = Screen.Vault,
                    status = when {
                        failed == 0 -> text(R.string.status_shared_files_saved, saved)
                        saved == 0 -> null
                        else -> text(R.string.status_shared_files_partial, saved, failed)
                    },
                    error = if (saved == 0) text(R.string.error_save_document) else null,
                )
            }
            if (saved > 0) {
                sync()
            }
        }
    }

    fun openBackup() {
        _state.value = _state.value.copy(screen = Screen.Backup, error = null, status = null)
    }

    fun openSearch() {
        _state.value = _state.value.copy(screen = Screen.Search, error = null, status = null)
    }

    fun queueShortcut(action: String?) {
        val supported = setOf(
            "app.mypass.action.SEARCH",
            "app.mypass.action.SCAN",
            "app.mypass.action.NEW_PASS",
            "app.mypass.action.CREDENTIALS",
            "app.mypass.action.NEXT_PASS",
            "app.mypass.action.NOTIFICATIONS",
            "app.mypass.action.FAMILY",
        )
        if (action == null || action !in supported) {
            return
        }
        _state.value = _state.value.copy(pendingShortcut = action)
    }

    fun queueOpenItem(id: String?) {
        if (id.isNullOrBlank()) {
            return
        }
        _state.value = _state.value.copy(pendingOpenItemId = id)
    }

    fun consumeOpenItem() {
        val id = _state.value.pendingOpenItemId ?: return
        val item = _state.value.items.firstOrNull { it.id == id } ?: return
        _state.value = _state.value.copy(pendingOpenItemId = null)
        when {
            item.pass != null -> openPassItem(item.id)
            item.credential != null -> editCredential(item)
            item.medical != null -> editMedicalCard(item)
            else -> openItem(item)
        }
    }

    fun consumeShortcut() {
        val action = _state.value.pendingShortcut ?: return
        _state.value = _state.value.copy(pendingShortcut = null)

        when (action) {
            "app.mypass.action.SEARCH" -> openSearch()
            "app.mypass.action.SCAN" -> openScanner(false)
            "app.mypass.action.NEW_PASS" -> newPass("flight")
            "app.mypass.action.CREDENTIALS" -> openCredentials()
            "app.mypass.action.NEXT_PASS" -> openNextPass()
            "app.mypass.action.NOTIFICATIONS" -> openNotifications()
            "app.mypass.action.FAMILY" -> openFamily("contacts")
        }
    }

    private fun openNextPass() {
        val next = sortActivePassItems(
            _state.value.items.filter { it.pass != null && !isExpired(it) && it.state != "transferred" },
        ).firstOrNull()
        if (next != null) {
            openPassItem(next.id)
        } else {
            openPasses()
        }
    }

    fun closeSearch() {
        _state.value = _state.value.copy(screen = Screen.Home, error = null, status = null)
    }

    fun recordRecentItem(id: String) {
        store.recordRecentItem(id)
        val recent = store.recentItemIds()
            .mapNotNull { itemId -> _state.value.items.firstOrNull { it.id == itemId } }
            .take(12)
        _state.value = _state.value.copy(recentItems = recent)
    }

    fun openPassItem(id: String) {
        recordRecentItem(id)
        _state.value = _state.value.copy(
            screen = Screen.Passes,
            focusItemId = id,
            error = null,
            status = null,
        )
    }

    fun openCredentialItem(id: String) {
        val item = _state.value.items.firstOrNull { it.id == id } ?: return
        editCredential(item)
    }

    fun openMedicalItem(id: String) {
        val item = _state.value.items.firstOrNull { it.id == id } ?: return
        editMedicalCard(item)
    }

    fun clearFocusItem() {
        if (_state.value.focusItemId != null) {
            _state.value = _state.value.copy(focusItemId = null)
        }
    }

    fun clearRecentItems() {
        store.clearRecentItems()
        _state.value = _state.value.copy(recentItems = emptyList())
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
        val normalizedFields = pass.fields.toMutableMap().apply {
            if (!containsKey("boarding_start") && containsKey("boarding")) {
                put("boarding_start", getValue("boarding"))
            }
        }
        _state.value = _state.value.copy(
            screen = Screen.PassEditor,
            passDraft = PassDraft(
                itemId = item.id,
                version = local?.version ?: 1,
                category = pass.category,
                title = pass.title,
                issuer = pass.issuer,
                holder = pass.holder,
                fields = normalizedFields,
                notes = pass.notes,
                codePayload = pass.codePayload.orEmpty(),
                codeFormat = pass.codeFormat,
                codeSource = pass.codeSource,
                themeColor = pass.themeColor,
                logo = pass.logo,
                appearance = pass.appearance,
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

    fun saveCredential(draft: CredentialDraft) {
        val key = masterKey ?: return
        val token = store.token()
        val id = draft.itemId ?: store.newItemId()

        _state.value = _state.value.copy(
            busy = true,
            error = null,
            status = text(R.string.status_saving_credential),
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val title = draft.title.ifBlank {
                    draft.username.ifBlank { text(R.string.credential_fallback_title) }
                }
                val tags = commaList(draft.tags)
                val totp = Totp.parse(draft.totpSecret.orEmpty())
                if (draft.totpSecret.orEmpty().isNotBlank() && totp == null) {
                    error(text(R.string.error_invalid_totp))
                }

                val existing = store.findItem(id)
                val previousMetadata = existing?.let { local ->
                    runCatching {
                        JSONObject(String(Crypto.open(key, Base64.decode(local.encryptedMetadata, Base64.NO_WRAP))))
                    }.getOrNull()
                }
                val metadata = previousMetadata?.let { JSONObject(it.toString()) } ?: JSONObject()
                metadata
                    .put("title", title)
                    .put("type", "credential")
                    .put("expires_at", JSONObject.NULL)
                val credentialJson = metadata.optJSONObject("credential") ?: JSONObject()
                credentialJson
                    .put("category", draft.category)
                    .put("username", draft.username.trim())
                    .put("password", draft.password)
                    .put("url", draft.url.trim())
                    .put("notes", draft.notes.trim())
                    .put("tags", JSONArray(tags))
                    .put("favorite", draft.favorite)
                    .put(
                        "password_changed_at",
                        if (draft.password.isNotBlank()) {
                            draft.passwordChangedAt ?: java.time.LocalDate.now().toString()
                        } else {
                            JSONObject.NULL
                        },
                    )
                    .put("totp", totp?.toJson() ?: JSONObject.NULL)

                if (draft.category == "custom") {
                    credentialJson
                        .put("category_label", draft.categoryLabel.trim())
                        .put("category_icon", draft.categoryIcon.ifBlank { "lock" })
                } else {
                    credentialJson.remove("category_label")
                    credentialJson.remove("category_icon")
                }

                if (draft.group.isBlank()) {
                    credentialJson.remove("group")
                } else {
                    credentialJson.put("group", draft.group.trim())
                }

                if (draft.logo.isNullOrBlank()) {
                    credentialJson.remove("logo")
                } else {
                    credentialJson.put("logo", draft.logo)
                }

                if (draft.appearanceMode == "none" || (draft.appearanceColor.isBlank() && draft.appearanceImage.isNullOrBlank())) {
                    credentialJson.remove("appearance")
                } else {
                    credentialJson.put(
                        "appearance",
                        JSONObject()
                            .put("mode", draft.appearanceMode)
                            .put("color", draft.appearanceColor)
                            .put("image", draft.appearanceImage ?: ""),
                    )
                }

                metadata.put("credential", credentialJson)

                var localPath = existing?.localPath
                var blobId = existing?.blobId
                var changedAttachment = false

                if (draft.clearAttachment) {
                    localPath?.let { runCatching { File(it).delete() } }
                    localPath = null
                    blobId = null
                    changedAttachment = true
                    metadata.remove("original_name")
                    metadata.remove("mime")
                    metadata.remove("size")
                }

                draft.attachmentUri?.let { uri ->
                    val attachment = readAttachment(Uri.parse(uri))
                    if (attachment.bytes.size > 25 * 1024 * 1024) {
                        error(text(R.string.error_file_too_large))
                    }
                    val encryptedFile = Crypto.encryptFile(key, attachment.bytes)
                    localPath = store.writeVaultFile(id, encryptedFile).absolutePath
                    changedAttachment = true
                    metadata
                        .put("original_name", attachment.name)
                        .put("mime", attachment.mime)
                        .put("size", attachment.bytes.size)

                    if (!draft.isNew && existing?.pendingUpload != true) {
                        val activeToken = token ?: error(text(R.string.error_sign_in_again))
                        blobId = ApiClient.uploadBlob(activeToken, encryptedFile)
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
                            type = "credential",
                            state = "active",
                            encryptedMetadata = encryptedMetadata,
                            blobId = null,
                            version = 1,
                            expiresAt = null,
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
                        ApiClient.updateItem(
                            token = activeToken,
                            id = id,
                            encryptedMetadata = encryptedMetadata,
                            expiresAt = null,
                            version = local.version,
                            blobId = blobId,
                            updateBlob = changedAttachment,
                        )
                        version += 1
                    }
                    store.upsert(
                        local.copy(
                            type = "credential",
                            encryptedMetadata = encryptedMetadata,
                            blobId = blobId,
                            expiresAt = null,
                            localPath = localPath,
                            version = version,
                            updatedAt = VaultStore.now(),
                        ),
                    )
                }

                refreshItems(key)
                if (draft.isNew) {
                    sync()
                }

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.Credentials,
                        credentialDraft = null,
                        busy = false,
                        status = text(R.string.status_credential_saved),
                        error = null,
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_save_credential),
                    )
                }
            }
        }
    }

    fun toggleCredentialFavorite(item: UiItem) {
        val credential = item.credential ?: return
        val key = masterKey ?: return
        val token = store.token() ?: return
        val local = store.findItem(item.id) ?: return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val metadata = local.metadata(key)
                val credentialJson = metadata.optJSONObject("credential") ?: JSONObject()
                credentialJson.put("favorite", !credential.favorite)
                metadata.put("credential", credentialJson)
                val encryptedMetadata = Base64.encodeToString(
                    Crypto.seal(key, metadata.toString().toByteArray(Charsets.UTF_8)),
                    Base64.NO_WRAP,
                )
                var version = local.version
                if (!local.pendingUpload) {
                    ApiClient.updateItem(token, item.id, encryptedMetadata, local.expiresAt, local.version)
                    version += 1
                }
                store.upsert(
                    local.copy(
                        encryptedMetadata = encryptedMetadata,
                        version = version,
                        updatedAt = VaultStore.now(),
                    ),
                )
                refreshItems(key)
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        error = error.message ?: text(R.string.error_save_credential),
                    )
                }
            }
        }
    }

    fun saveMedicalCard(draft: MedicalDraft) {
        val key = masterKey ?: return
        val token = store.token()
        val id = draft.itemId ?: store.newItemId()
        val expiresAt = draft.expiresAt.takeIf { it.isNotBlank() }

        _state.value = _state.value.copy(
            busy = true,
            error = null,
            status = text(R.string.status_saving_medical),
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val title = draft.title.ifBlank {
                    draft.holder.ifBlank { text(R.string.medical_fallback_title) }
                }
                val contacts = JSONArray()
                val firstContact = JSONObject()
                    .put("name", draft.contactName.trim())
                    .put("relation", draft.contactRelation.trim())
                    .put("phone", draft.contactPhone.trim())
                if (draft.contactName.isNotBlank() || draft.contactPhone.isNotBlank()) {
                    contacts.put(firstContact)
                }
                draft.extraContacts.forEach { contact ->
                    if (contact.name.isNotBlank() || contact.phone.isNotBlank()) {
                        contacts.put(
                            JSONObject()
                                .put("name", contact.name)
                                .put("relation", contact.relation)
                                .put("phone", contact.phone),
                        )
                    }
                }

                val existing = store.findItem(id)
                val previousMetadata = existing?.let { local ->
                    runCatching {
                        JSONObject(String(Crypto.open(key, Base64.decode(local.encryptedMetadata, Base64.NO_WRAP))))
                    }.getOrNull()
                }
                val metadata = previousMetadata?.let { JSONObject(it.toString()) } ?: JSONObject()
                metadata
                    .put("title", title)
                    .put("type", "medical_card")
                    .put("expires_at", expiresAt ?: JSONObject.NULL)
                val scheduledMedications = draft.medicationSchedule.filter { it.name.isNotBlank() }
                val medicationNames = if (scheduledMedications.isNotEmpty()) {
                    scheduledMedications.map { it.name }
                } else {
                    commaList(draft.medications)
                }
                val medicationArray = JSONArray()
                scheduledMedications.forEach { medication ->
                    val times = JSONArray()
                    medication.times
                        .filter { it.matches(Regex("^\\d{1,2}:\\d{2}$")) }
                        .distinct()
                        .forEach(times::put)
                    medicationArray.put(
                        JSONObject()
                            .put("id", medication.id.ifBlank { java.util.UUID.randomUUID().toString() })
                            .put("name", medication.name.trim())
                            .put("dose", medication.dose.trim())
                            .put("times", times)
                            .put("start_date", medication.startDate.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                            .put("end_date", medication.endDate.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                            .put("notes", medication.notes.trim()),
                    )
                }
                val appointmentArray = JSONArray()
                draft.appointments
                    .filter { it.title.isNotBlank() }
                    .forEach { appointment ->
                        appointmentArray.put(
                            JSONObject()
                                .put("id", appointment.id.ifBlank { java.util.UUID.randomUUID().toString() })
                                .put("title", appointment.title.trim())
                                .put("date", appointment.date.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                                .put("time", appointment.time.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                                .put("location", appointment.location.trim())
                                .put("notes", appointment.notes.trim())
                                .put("reminder_minutes", appointment.reminderMinutes),
                        )
                    }

                val medicalJson = metadata.optJSONObject("medical") ?: JSONObject()
                medicalJson
                    .put("holder", draft.holder.trim())
                    .put("birth_date", draft.birthDate)
                    .put("blood_type", draft.bloodType)
                    .put("allergies", JSONArray(commaList(draft.allergies)))
                    .put("conditions", JSONArray(commaList(draft.conditions)))
                    .put("medications", JSONArray(medicationNames))
                    .put("medication_schedule", medicationArray)
                    .put("appointments", appointmentArray)
                    .put("emergency_contacts", contacts)
                    .put(
                        "insurance",
                        JSONObject()
                            .put("provider", draft.insuranceProvider.trim())
                            .put("plan", draft.insurancePlan.trim())
                            .put("policy_number", draft.insurancePolicy.trim())
                            .put("member_id", draft.insuranceMember.trim())
                            .put("phone", draft.insurancePhone.trim()),
                    )
                    .put(
                        "doctor",
                        JSONObject()
                            .put("name", draft.doctorName.trim())
                            .put("phone", draft.doctorPhone.trim())
                            .put("clinic", draft.doctorClinic.trim()),
                    )
                    .put("organ_donor", draft.organDonor)
                    .put("notes", draft.notes.trim())
                metadata.put("medical", medicalJson)

                var localPath = existing?.localPath
                var blobId = existing?.blobId
                var changedAttachment = false

                if (draft.clearAttachment) {
                    localPath?.let { runCatching { File(it).delete() } }
                    localPath = null
                    blobId = null
                    changedAttachment = true
                    metadata.remove("original_name")
                    metadata.remove("mime")
                    metadata.remove("size")
                }

                draft.attachmentUri?.let { uri ->
                    val attachment = readAttachment(Uri.parse(uri))
                    if (attachment.bytes.size > 25 * 1024 * 1024) {
                        error(text(R.string.error_file_too_large))
                    }
                    val encryptedFile = Crypto.encryptFile(key, attachment.bytes)
                    localPath = store.writeVaultFile(id, encryptedFile).absolutePath
                    changedAttachment = true
                    metadata
                        .put("original_name", attachment.name)
                        .put("mime", attachment.mime)
                        .put("size", attachment.bytes.size)
                    if (!draft.isNew && existing?.pendingUpload != true) {
                        val activeToken = token ?: error(text(R.string.error_sign_in_again))
                        blobId = ApiClient.uploadBlob(activeToken, encryptedFile)
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
                            type = "medical_card",
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
                        ApiClient.updateItem(
                            token = activeToken,
                            id = id,
                            encryptedMetadata = encryptedMetadata,
                            expiresAt = expiresAt,
                            version = local.version,
                            blobId = blobId,
                            updateBlob = changedAttachment,
                        )
                        version += 1
                    }
                    store.upsert(
                        local.copy(
                            type = "medical_card",
                            encryptedMetadata = encryptedMetadata,
                            blobId = blobId,
                            expiresAt = expiresAt,
                            localPath = localPath,
                            version = version,
                            updatedAt = VaultStore.now(),
                        ),
                    )
                }

                refreshItems(key)
                Reminders.checkNow(getApplication())
                if (draft.isNew) {
                    sync()
                }

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.Medical,
                        medicalDraft = null,
                        busy = false,
                        status = text(R.string.status_medical_saved),
                        error = null,
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_save_medical),
                    )
                }
            }
        }
    }

    private data class PickedAttachment(
        val bytes: ByteArray,
        val name: String,
        val mime: String,
    )

    private fun readAttachment(uri: Uri): PickedAttachment {
        val resolver = getApplication<Application>().contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "adjunto"
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error(text(R.string.error_read_file))
        return PickedAttachment(bytes = bytes, name = name, mime = mime)
    }

    private fun commaList(value: String): List<String> {
        return value.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun LocalItem.metadata(key: ByteArray): JSONObject {
        return JSONObject(String(Crypto.open(key, Base64.decode(encryptedMetadata, Base64.NO_WRAP))))
    }

    private fun TotpConfig.toJson(): JSONObject {
        return JSONObject()
            .put("secret", secret)
            .put("algorithm", algorithm)
            .put("digits", digits)
            .put("period", period)
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
            .put("appearance", draft.appearance.toJson())
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
        syncPreferences(simpleMode = enabled)
    }

    fun setEasyMode(enabled: Boolean) {
        store.setEasyMode(enabled)
        _state.value = _state.value.copy(easyMode = enabled)
        syncPreferences(easyMode = enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        store.setThemeMode(mode)
        _state.value = _state.value.copy(themeMode = mode)
    }

    fun setBirthDate(value: String) {
        if (value.isBlank()) {
            return
        }

        val token = store.token() ?: return
        val previous = store.birthDate()
        _state.value = _state.value.copy(
            busy = true,
            status = text(R.string.status_saving_birth_date),
            error = null,
            birthDate = value,
        )

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ApiClient.updateProfile(token, value) }
                .onSuccess { data ->
                    store.setBirthDate(data.optString("birth_date"))
                    store.setAgeGroup(data.optString("age_group", "unknown"))
                    store.setSimpleMode(data.optBoolean("simple_mode", store.simpleMode()))
                    store.setEasyMode(data.optBoolean("easy_mode", store.easyMode()))
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            busy = false,
                            status = text(R.string.status_birth_date_saved),
                            birthDate = store.birthDate(),
                            ageGroup = store.ageGroup(),
                            simpleMode = store.simpleMode(),
                            easyMode = store.easyMode(),
                        )
                    }
                }
                .onFailure { error ->
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            busy = false,
                            status = null,
                            birthDate = previous,
                            error = error.message ?: text(R.string.error_save_birth_date),
                        )
                    }
                }
        }
    }

    private fun syncPreferences(simpleMode: Boolean? = null, easyMode: Boolean? = null) {
        val token = store.token() ?: return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ApiClient.updatePreferences(token, simpleMode, easyMode) }
        }
    }

    fun enableEmailRecovery() {
        val token = store.token()
        val key = masterKey

        if (token == null || key == null) {
            _state.value = _state.value.copy(error = text(R.string.error_unlock_to_enable_recovery))
            return
        }

        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_enabling_recovery))

        viewModelScope.launch(Dispatchers.IO) {
            var secret: ByteArray? = null

            runCatching {
                secret = Crypto.randomBytes(32)
                val wrapped = Crypto.seal(secret!!, key)
                ApiClient.enableVaultRecovery(
                    token,
                    Base64.encodeToString(secret, Base64.NO_WRAP),
                    Base64.encodeToString(wrapped, Base64.NO_WRAP),
                )
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = text(R.string.status_recovery_enabled),
                        recoveryEnabled = true,
                        recoveryEmail = store.email(),
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_enable_recovery),
                    )
                }
            }

            secret?.let(Crypto::memzero)
        }
    }

    fun startVaultRecovery() {
        val token = store.token()
        if (token == null) {
            _state.value = _state.value.copy(error = text(R.string.error_sign_in_again))
            return
        }

        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_preparing_recovery))

        viewModelScope.launch(Dispatchers.IO) {
            val status = runCatching { ApiClient.vaultRecoveryStatus(token) }.getOrNull()

            if (status == null || !status.optBoolean("enabled", false)) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = text(R.string.error_recovery_not_enabled),
                    )
                }
                return@launch
            }

            runCatching {
                val generator = KeyPairGenerator.getInstance("RSA")
                generator.initialize(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
                val keyPair = generator.generateKeyPair()
                pairingPrivateKey = keyPair.private
                pairingPublicKey = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
                ApiClient.requestVaultRecovery(token)
            }.onSuccess { body ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.RecoverVault,
                        busy = false,
                        status = body.optString("message").takeIf { it.isNotBlank() },
                        recoveryEmail = status.optString("masked_email"),
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_send_recovery_code),
                    )
                }
            }
        }
    }

    fun verifyVaultRecovery(code: String) {
        val token = store.token()
        if (token == null) {
            _state.value = _state.value.copy(error = text(R.string.error_sign_in_again))
            return
        }

        if (code.isBlank()) {
            _state.value = _state.value.copy(error = text(R.string.error_enter_code))
            return
        }

        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_checking_code))

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val data = ApiClient.verifyVaultRecovery(token, code.trim())
                val secret = Base64.decode(data.getString("secret"), Base64.NO_WRAP)
                val wrapped = Base64.decode(data.getString("wrapped_master_key"), Base64.NO_WRAP)

                try {
                    Crypto.open(secret, wrapped)
                } finally {
                    Crypto.memzero(secret)
                }
            }.onSuccess { key ->
                pendingMasterKey = key
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        screen = Screen.PinSetup,
                        busy = false,
                        status = text(R.string.status_recovery_ready),
                        recoveryEmail = "",
                    )
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        busy = false,
                        status = null,
                        error = error.message ?: text(R.string.error_recovery_code),
                    )
                }
            }
        }
    }

    fun cancelVaultRecovery() {
        _state.value = _state.value.copy(
            screen = if (store.hasVault()) Screen.Locked else Screen.Pair,
            busy = false,
            status = null,
            error = null,
            recoveryEmail = "",
        )
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
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        status = text(R.string.status_contact_request_sent, normalized),
                        familyStatus = text(R.string.status_contact_request_sent, normalized),
                        error = null,
                    )
                }
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
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        status = text(R.string.status_contact_blocked),
                        familyStatus = text(R.string.status_contact_blocked),
                        error = null,
                    )
                }
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
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        status = text(R.string.status_share_revoked),
                        error = null,
                    )
                }
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
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        status = text(R.string.status_contact_accepted),
                        familyStatus = text(R.string.status_contact_accepted),
                        error = null,
                    )
                }
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
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        status = text(R.string.status_share_saved),
                        error = null,
                    )
                }
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
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(status = text(R.string.status_share_declined), error = null)
                }
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

    private fun processPendingActions(token: String) {
        for (item in store.pendingActions()) {
            when (item.pendingAction) {
                "delete" -> runCatching {
                    ApiClient.deleteItem(token, item.id)
                }.onSuccess {
                    store.clearPendingAction(item.id)
                }.onFailure { error ->
                    if (error is ApiException && error.statusCode == 404) {
                        store.clearPendingAction(item.id)
                    }
                }

                "restore" -> runCatching {
                    ApiClient.restoreItem(token, item.id)
                }.onSuccess {
                    store.clearPendingAction(item.id)
                    store.restoreItem(item.id, pending = false)
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
        val serverIds = mutableSetOf<String>()
        val activeItems = ApiClient.listItems(token)
        val trashedItems = ApiClient.listTrashedItems(token)

        for (serverItems in listOf(activeItems, trashedItems)) {
            for (index in 0 until serverItems.length()) {
                val json = serverItems.getJSONObject(index)
                val id = json.getString("id")
                serverIds.add(id)
                val existing = store.findItem(id)
                if (existing?.pendingUpload == true || existing?.pendingAction != null) {
                    continue
                }

                val encryptedMetadata = json.getString("encrypted_metadata")
                val blobId = json.optString("blob_id").takeIf { it.isNotBlank() && it != "null" }
                val state = json.optString("state").takeIf { it.isNotBlank() && it != "null" } ?: "active"
                val deletedAt = json.optString("deleted_at").takeIf { it.isNotBlank() && it != "null" }

                var localPath = existing?.localPath
                if (state != "trashed" && blobId != null && localPath == null) {
                    val bytes = ApiClient.downloadBlob(token, blobId)
                    localPath = store.writeVaultFile(id, bytes).absolutePath
                }

                store.markSyncedFromServer(
                    id = id,
                    type = json.getString("type"),
                    state = state,
                    encryptedMetadata = encryptedMetadata,
                    blobId = blobId,
                    version = json.optInt("version", 1),
                    expiresAt = json.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
                    deletedAt = deletedAt,
                    localPath = localPath,
                )
            }
        }

        store.removeMissingFromServer(serverIds)
    }

    private fun refreshItems(key: ByteArray) {
        val mapped = store.allItems().map { item ->
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

            val credential = metadata?.optJSONObject("credential")?.let(::parseUiCredential)
            val medical = metadata?.optJSONObject("medical")?.let(::parseUiMedical)

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
                deletedAt = item.deletedAt,
                pendingAction = item.pendingAction,
                ocrText = metadata?.optString("ocr_text")?.takeIf { it.isNotBlank() && it != "null" } ?: "",
                pass = pass,
                credential = credential,
                medical = medical,
                metadataJson = metadata,
            )
        }

        val items = mapped.filter { it.state != "trashed" }
        val trashItems = mapped
            .filter { it.state == "trashed" }
            .sortedByDescending { it.deletedAt ?: it.updatedAt.orEmpty() }
        val recentItems = store.recentItemIds()
            .mapNotNull { id -> items.firstOrNull { it.id == id } }
            .take(12)

        viewModelScope.launch(Dispatchers.Main) {
            _state.value = _state.value.copy(
                items = items,
                trashItems = trashItems,
                recentItems = recentItems,
                status = null,
                error = null,
            )
            updateNextPassShortcut(items)
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { AgendaReminders.rebuild(getApplication(), items) }
        }
    }

    private fun updateNextPassShortcut(items: List<UiItem>) {
        val next = sortActivePassItems(
            items.filter { it.pass != null && !isExpired(it) && it.state != "transferred" },
        ).firstOrNull()

        if (next == null) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(getApplication())
            return
        }

        val shortcut = ShortcutInfoCompat.Builder(getApplication(), "next_pass")
            .setShortLabel(text(R.string.shortcut_next_pass_short))
            .setLongLabel(text(R.string.shortcut_next_pass_long))
            .setIcon(IconCompat.createWithResource(getApplication(), R.drawable.ic_ticket))
            .setIntent(
                Intent(getApplication(), MainActivity::class.java)
                    .setAction("app.mypass.action.NEXT_PASS"),
            )
            .setRank(0)
            .build()

        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(getApplication(), listOf(shortcut))
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
            appearance = PassAppearance.fromJson(json.optJSONObject("appearance")),
        )
    }

    private fun parseUiCredential(json: JSONObject): UiCredential {
        val tags = mutableListOf<String>()
        json.optJSONArray("tags")?.let { array ->
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(tags::add)
            }
        }

        val totp = json.optJSONObject("totp")
        val secret = totp?.optString("secret")?.takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("totp_secret").takeIf { it.isNotBlank() && it != "null" }
        val appearance = json.optJSONObject("appearance")
        val rawCategory = json.optString("category").takeIf { it.isNotBlank() } ?: "login"
        val categoryLabel = json.optString("category_label").takeIf { it.isNotBlank() && it != "null" }.orEmpty()
        val category = if (rawCategory == "custom" || categoryLabel.isNotBlank()) "custom" else rawCategory
        val appearanceMode = appearance?.optString("mode")?.takeIf { it in setOf("banner", "background") } ?: "none"
        val appearanceColor = appearance?.optString("color")
            ?.takeIf { Regex("^#[0-9a-fA-F]{6}$").matches(it) }
            ?: "#1B4332"
        val appearanceImage = appearance?.optString("image")
            ?.takeIf { it.startsWith("data:image/") && it != "null" }

        return UiCredential(
            category = category,
            categoryLabel = if (category == "custom") categoryLabel else "",
            categoryIcon = if (category == "custom") {
                json.optString("category_icon").takeIf { it.isNotBlank() && it != "null" }.orEmpty()
            } else {
                ""
            },
            group = json.optString("group").takeIf { it != "null" }.orEmpty(),
            logo = json.optString("logo").takeIf { it.startsWith("data:image/") && it != "null" },
            appearanceMode = appearanceMode,
            appearanceColor = appearanceColor,
            appearanceImage = appearanceImage,
            username = json.optString("username").takeIf { it != "null" }.orEmpty(),
            password = json.optString("password").takeIf { it != "null" }.orEmpty(),
            url = json.optString("url").takeIf { it != "null" }.orEmpty(),
            notes = json.optString("notes").takeIf { it != "null" }.orEmpty(),
            tags = tags,
            favorite = json.optBoolean("favorite", false),
            passwordChangedAt = json.optString("password_changed_at")
                .takeIf { it.isNotBlank() && it != "null" },
            totpSecret = secret,
            totpAlgorithm = totp?.optString("algorithm")?.takeIf { it.isNotBlank() } ?: "SHA1",
            totpDigits = totp?.optInt("digits", 6)?.coerceIn(4, 10) ?: 6,
            totpPeriod = totp?.optInt("period", 30)?.coerceIn(10, 120) ?: 30,
        )
    }

    private fun parseStringList(value: String?): List<String> {
        return value.orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun parseMedicalList(json: JSONObject, key: String): List<String> {
        val array = json.optJSONArray(key)
        if (array != null) {
            val values = mutableListOf<String>()
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() && it != "null" }?.let(values::add)
            }
            return values
        }

        return parseStringList(json.optString(key).takeIf { it != "null" })
    }

    private fun parseUiMedical(json: JSONObject): UiMedicalCard {
        val contacts = mutableListOf<UiMedicalContact>()
        json.optJSONArray("emergency_contacts")?.let { array ->
            for (index in 0 until array.length()) {
                val contact = array.optJSONObject(index) ?: continue
                contacts.add(
                    UiMedicalContact(
                        name = contact.optString("name").takeIf { it != "null" }.orEmpty(),
                        relation = contact.optString("relation").takeIf { it != "null" }.orEmpty(),
                        phone = contact.optString("phone").takeIf { it != "null" }.orEmpty(),
                    ),
                )
            }
        }

        val insurance = json.optJSONObject("insurance")
        val doctor = json.optJSONObject("doctor")
        val legacyMedications = parseMedicalList(json, "medications")
        val medicationSchedule = mutableListOf<UiMedication>()
        json.optJSONArray("medication_schedule")?.let { array ->
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                val times = mutableListOf<String>()
                entry.optJSONArray("times")?.let { values ->
                    for (timeIndex in 0 until values.length()) {
                        values.optString(timeIndex).takeIf { it.isNotBlank() }?.let(times::add)
                    }
                }
                medicationSchedule.add(
                    UiMedication(
                        id = entry.optString("id").takeIf { it.isNotBlank() && it != "null" }
                            ?: java.util.UUID.randomUUID().toString(),
                        name = entry.optString("name").takeIf { it != "null" }.orEmpty(),
                        dose = entry.optString("dose").takeIf { it != "null" }.orEmpty(),
                        times = times,
                        startDate = entry.optString("start_date").takeIf { it != "null" }.orEmpty(),
                        endDate = entry.optString("end_date").takeIf { it != "null" }.orEmpty(),
                        notes = entry.optString("notes").takeIf { it != "null" }.orEmpty(),
                    ),
                )
            }
        }
        if (medicationSchedule.isEmpty()) {
            legacyMedications.forEach { name ->
                if (name.isNotBlank()) {
                    medicationSchedule.add(
                        UiMedication(name = name, id = java.util.UUID.randomUUID().toString()),
                    )
                }
            }
        }

        val appointments = mutableListOf<UiAppointment>()
        json.optJSONArray("appointments")?.let { array ->
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                appointments.add(
                    UiAppointment(
                        id = entry.optString("id").takeIf { it.isNotBlank() && it != "null" }
                            ?: java.util.UUID.randomUUID().toString(),
                        title = entry.optString("title").takeIf { it != "null" }.orEmpty(),
                        date = entry.optString("date").takeIf { it != "null" }.orEmpty(),
                        time = entry.optString("time").takeIf { it != "null" }.orEmpty(),
                        location = entry.optString("location").takeIf { it != "null" }.orEmpty(),
                        notes = entry.optString("notes").takeIf { it != "null" }.orEmpty(),
                        reminderMinutes = entry.optInt("reminder_minutes", 1440),
                    ),
                )
            }
        }

        return UiMedicalCard(
            holder = json.optString("holder").takeIf { it != "null" }.orEmpty(),
            birthDate = json.optString("birth_date").takeIf { it != "null" }.orEmpty(),
            bloodType = json.optString("blood_type").takeIf { it != "null" }.orEmpty(),
            allergies = parseMedicalList(json, "allergies"),
            conditions = parseMedicalList(json, "conditions"),
            medications = legacyMedications,
            medicationSchedule = medicationSchedule,
            appointments = appointments,
            contacts = contacts,
            insurance = UiMedicalInsurance(
                provider = insurance?.optString("provider").orEmpty(),
                plan = insurance?.optString("plan").orEmpty(),
                policyNumber = insurance?.optString("policy_number").orEmpty(),
                memberId = insurance?.optString("member_id").orEmpty(),
                phone = insurance?.optString("phone").orEmpty(),
            ),
            doctor = UiMedicalDoctor(
                name = doctor?.optString("name").orEmpty(),
                phone = doctor?.optString("phone").orEmpty(),
                clinic = doctor?.optString("clinic").orEmpty(),
            ),
            organDonor = json.optBoolean("organ_donor", false),
            notes = json.optString("notes").takeIf { it != "null" }.orEmpty(),
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

    private fun createCredentialFromTotpScan(photo: File) {
        _state.value = _state.value.copy(busy = true, error = null, status = text(R.string.status_scanning_totp))

        viewModelScope.launch(Dispatchers.IO) {
            val parsed = runCatching {
                val bitmap = decodeSampledBitmap(photo, 1800)
                val decoded = Barcode.decode(bitmap) ?: error(text(R.string.error_totp_qr_not_found))
                val uri = Totp.parseUri(decoded.text) ?: error(text(R.string.error_totp_qr_invalid))
                uri
            }

            photo.delete()
            withContext(Dispatchers.Main) {
                parsed.onSuccess { uri ->
                    val current = _state.value.credentialDraft ?: CredentialDraft()
                    val title = current.title.ifBlank {
                        uri.issuer.ifBlank { uri.account.substringBefore("@").ifBlank { "TOTP" } }
                    }
                    _state.value = _state.value.copy(
                        screen = Screen.CredentialEditor,
                        credentialDraft = current.copy(
                            title = title,
                            username = current.username.ifBlank { uri.account },
                            totpSecret = uri.config.secret,
                            totpAlgorithm = uri.config.algorithm,
                            totpDigits = uri.config.digits,
                            totpPeriod = uri.config.period,
                        ),
                        busy = false,
                        scanForTotp = false,
                        scanForPass = false,
                        status = text(R.string.status_totp_detected),
                        error = null,
                    )
                }.onFailure { error ->
                    _state.value = _state.value.copy(
                        screen = if (_state.value.credentialDraft == null) Screen.Credentials else Screen.CredentialEditor,
                        busy = false,
                        scanForTotp = false,
                        scanForPass = false,
                        status = null,
                        error = error.message ?: text(R.string.error_totp_qr_invalid),
                    )
                }
            }
        }
    }

    fun addScan(photo: File) {
        val key = masterKey ?: return
        if (_state.value.scanForTotp) {
            createCredentialFromTotpScan(photo)
            return
        }
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
            val needsServer = !item.pendingUpload
            store.trashItem(item.id, pending = needsServer)

            if (needsServer && token != null) {
                runCatching { ApiClient.deleteItem(token, item.id) }
                    .onSuccess { store.clearPendingAction(item.id) }
            }

            refreshItems(key)
            Reminders.checkNow(getApplication())
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    status = text(R.string.status_moved_to_trash),
                    error = null,
                )
            }
        }
    }

    fun restoreItem(item: UiItem) {
        val key = masterKey ?: return
        val token = store.token()

        viewModelScope.launch(Dispatchers.IO) {
            val needsServer = !item.pendingUpload
            store.restoreItem(item.id, pending = needsServer)

            if (needsServer && token != null) {
                runCatching { ApiClient.restoreItem(token, item.id) }
                    .onSuccess { store.clearPendingAction(item.id) }
            }

            refreshItems(key)
            Reminders.checkNow(getApplication())
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    status = text(R.string.status_restored),
                    error = null,
                )
            }
        }
    }

    fun permanentlyDeleteItem(item: UiItem) {
        val key = masterKey ?: return
        val token = store.token()

        viewModelScope.launch(Dispatchers.IO) {
            if (!item.pendingUpload) {
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(error = text(R.string.error_delete_forever))
                    }
                    return@launch
                }

                val result = runCatching { ApiClient.forceDeleteItem(token, item.id) }
                if (result.isFailure) {
                    val error = result.exceptionOrNull()
                    if (error !is ApiException || error.statusCode != 404) {
                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(
                                error = error?.message ?: text(R.string.error_delete_forever),
                            )
                        }
                        return@launch
                    }
                }
            }

            store.deleteItem(item.id)
            refreshItems(key)
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    status = text(R.string.status_deleted_forever),
                    error = null,
                )
            }
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
        recordRecentItem(item.id)
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
