package app.mypass

import android.app.LocaleManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val lockRunnable = Runnable { viewModel.lock() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        enableEdgeToEdge()
        Reminders.schedule(this)
        Reminders.checkNow(this)
        setContent {
            val state by viewModel.state.collectAsState()
            val baseDensity = LocalDensity.current

            MaterialTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = baseDensity.density,
                        fontScale = baseDensity.fontScale * if (state.simpleMode) 1.25f else 1f,
                    ),
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        BackHandler(
                            enabled = state.screen == Screen.Viewer ||
                                state.screen == Screen.Family ||
                                state.screen == Screen.Scan ||
                                state.screen == Screen.Settings,
                        ) {
                            when (state.screen) {
                                Screen.Viewer -> viewModel.closeViewer()
                                else -> viewModel.backToVault()
                            }
                        }
                        MyPassApp(state = state, viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        handler.removeCallbacks(lockRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.postDelayed(lockRunnable, 60_000)
    }
}

@Composable
private fun MyPassApp(state: UiState, viewModel: MainViewModel) {
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.addDocument(uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "mypass", style = MaterialTheme.typography.headlineMedium)
        if (state.status != null) {
            Text(text = state.status, style = MaterialTheme.typography.bodyMedium)
        }
        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.busy) {
            CircularProgressIndicator()
        }
        Spacer(modifier = Modifier.height(12.dp))

        when (state.screen) {
            Screen.Loading -> Text(stringResource(R.string.loading))
            Screen.Login -> LoginScreen(viewModel)
            Screen.Pair -> PairScreen(viewModel)
            Screen.PinSetup -> PinSetupScreen(viewModel)
            Screen.TwoFactor -> TwoFactorScreen(state = state, viewModel = viewModel)
            Screen.Locked -> LockedScreen(viewModel)
            Screen.Vault -> VaultScreen(
                state = state,
                viewModel = viewModel,
                onAdd = { filePicker.launch(arrayOf("application/pdf", "image/*")) },
            )
            Screen.Family -> FamilyScreen(state = state, viewModel = viewModel)
            Screen.Scan -> ScannerScreen(viewModel = viewModel)
            Screen.Settings -> SettingsScreen(state = state, viewModel = viewModel)
            Screen.Viewer -> ViewerScreen(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun LoginScreen(viewModel: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.sign_in))
        Text(stringResource(R.string.use_same_account))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.login(email, password) },
            enabled = !viewModel.state.value.busy,
        ) {
            Text(stringResource(R.string.sign_in))
        }
    }
}

@Composable
private fun TwoFactorScreen(state: UiState, viewModel: MainViewModel) {
    var code by remember(state.twoFactorToken) { mutableStateOf("") }
    var method by remember(state.twoFactorToken) {
        mutableStateOf(
            when {
                "totp" in state.twoFactorMethods -> "totp"
                "email" in state.twoFactorMethods -> "email"
                else -> "recovery"
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.two_factor), style = MaterialTheme.typography.headlineSmall)
        Text(
            when (method) {
                "recovery" -> stringResource(R.string.two_factor_recovery_hint)
                "email" -> stringResource(R.string.two_factor_email_hint, state.email)
                else -> stringResource(R.string.two_factor_totp_hint)
            },
        )

        OutlinedTextField(
            value = code,
            onValueChange = { text ->
                code = if (method == "recovery") {
                    text.take(24)
                } else {
                    text.filter { it.isDigit() }.take(6)
                }
            },
            label = {
                Text(
                    stringResource(
                        if (method == "recovery") R.string.recovery_code else R.string.code,
                    ),
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (method == "recovery") KeyboardType.Text else KeyboardType.Number,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { viewModel.verifyTwoFactor(code, method) },
            enabled = !state.busy && code.isNotBlank(),
        ) {
            Text(stringResource(R.string.verify))
        }

        if (method == "email") {
            OutlinedButton(
                onClick = viewModel::sendTwoFactorEmail,
                enabled = !state.busy,
            ) {
                Text(stringResource(R.string.send_email_code))
            }
        }

        if (method != "email" && "email" in state.twoFactorMethods) {
            TextButton(onClick = { method = "email"; code = "" }) {
                Text(stringResource(R.string.use_email_code))
            }
        }

        if (method != "recovery" && "recovery" in state.twoFactorMethods) {
            TextButton(onClick = { method = "recovery"; code = "" }) {
                Text(stringResource(R.string.use_recovery_code))
            }
        }

        if (method != "totp" && "totp" in state.twoFactorMethods) {
            TextButton(onClick = { method = "totp"; code = "" }) {
                Text(stringResource(R.string.use_auth_app))
            }
        }

        OutlinedButton(onClick = viewModel::cancelTwoFactor, enabled = !state.busy) {
            Text(stringResource(R.string.back))
        }
    }
}

@Composable
private fun PairScreen(viewModel: MainViewModel) {    var code by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.connect_computer))
        Text(stringResource(R.string.pairing_hint))
        OutlinedTextField(
            value = code,
            onValueChange = { value -> code = value.filter(Char::isDigit).take(6) },
            label = { Text(stringResource(R.string.six_digit_code)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.joinPairing(code) },
            enabled = !viewModel.state.value.busy && code.length == 6,
        ) {
            Text(stringResource(R.string.connect_phone))
        }
        OutlinedButton(onClick = viewModel::logout) {
            Text(stringResource(R.string.use_another_account))
        }
    }
}

@Composable
private fun PinSetupScreen(viewModel: MainViewModel) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.create_pin))
        Text(stringResource(R.string.pin_help))
        OutlinedTextField(
            value = pin,
            onValueChange = { value -> pin = value.filter(Char::isDigit).take(12) },
            label = { Text(stringResource(R.string.pin_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = confirmation,
            onValueChange = { value -> confirmation = value.filter(Char::isDigit).take(12) },
            label = { Text(stringResource(R.string.repeat_pin)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.setupPin(pin, confirmation) },
            enabled = !viewModel.state.value.busy,
        ) {
            Text(stringResource(R.string.save_pin))
        }
    }
}

@Composable
private fun LockedScreen(viewModel: MainViewModel) {
    var pin by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.open_vault))
        OutlinedTextField(
            value = pin,
            onValueChange = { value -> pin = value.filter(Char::isDigit).take(12) },
            label = { Text(stringResource(R.string.pin)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.unlock(pin) },
            enabled = !viewModel.state.value.busy,
        ) {
            Text(stringResource(R.string.unlock))
        }
        OutlinedButton(onClick = viewModel::logout) {
            Text(stringResource(R.string.sign_out))
        }
    }
}

@Composable
private fun VaultScreen(
    state: UiState,
    viewModel: MainViewModel,
    onAdd: () -> Unit,
) {
    var shareItem by remember { mutableStateOf<UiItem?>(null) }
    var expiryItem by remember { mutableStateOf<UiItem?>(null) }
    var search by remember { mutableStateOf("") }
    val acceptedContacts = state.contacts.filter { it.status == "accepted" }
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    LaunchedEffect(Unit) {
        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val visibleItems = remember(state.items, search) {
        val query = search.trim()
        if (query.isBlank()) {
            state.items
        } else {
            state.items.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                    item.originalName.contains(query, ignoreCase = true) ||
                    item.ocrText.contains(query, ignoreCase = true)
            }
        }
    }

    if (shareItem != null) {
        ShareDialog(
            item = shareItem!!,
            contacts = acceptedContacts,
            onDismiss = { shareItem = null },
            onSend = { contactId, mode ->
                viewModel.sendShare(shareItem!!, contactId, mode)
                shareItem = null
            },
        )
    }

    if (expiryItem != null) {
        ExpiryDialog(
            item = expiryItem!!,
            onDismiss = { expiryItem = null },
            onSave = { date ->
                viewModel.setExpiry(expiryItem!!, date)
                expiryItem = null
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onAdd, enabled = !state.busy) {
                    Text(stringResource(R.string.add_document))
                }
                OutlinedButton(onClick = viewModel::openScanner, enabled = !state.busy) {
                    Text(stringResource(R.string.scan))
                }
                OutlinedButton(onClick = viewModel::sync, enabled = !state.busy) {
                    Text(stringResource(R.string.sync))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = viewModel::openFamily) {
                    Text(stringResource(R.string.family))
                }
                OutlinedButton(onClick = viewModel::openSettings) {
                    Text(stringResource(R.string.settings))
                }
                OutlinedButton(onClick = viewModel::lock) {
                    Text(stringResource(R.string.lock))
                }
            }
        }

        if (state.items.isEmpty()) {
            Text(stringResource(R.string.no_documents))
        } else {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(stringResource(R.string.search_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        if (state.items.isNotEmpty() && visibleItems.isEmpty()) {
            Text(stringResource(R.string.no_search_results, search.trim()))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visibleItems, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.type)
                        if (item.ocrText.isNotBlank()) {
                            Text(
                                item.ocrText.lineSequence().first().take(120),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        val expiry = expiryLabel(item.expiresAt)
                        if (expiry != null) {
                            val expired = item.expiresAt?.let { value ->
                                runCatching {
                                    LocalDate.parse(value.take(10)).isBefore(LocalDate.now())
                                }.getOrDefault(false)
                            } ?: false
                            Text(
                                expiry,
                                color = if (expired) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color.Unspecified
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (item.pendingUpload) {
                            Text(stringResource(R.string.pending_upload))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.openItem(item) }) {
                                Text(stringResource(R.string.open))
                            }
                            if (acceptedContacts.isNotEmpty()) {
                                OutlinedButton(onClick = { shareItem = item }) {
                                    Text(stringResource(R.string.send))
                                }
                            }
                            OutlinedButton(onClick = { expiryItem = item }) {
                                Text(
                                    stringResource(
                                        if (item.expiresAt == null) R.string.expiry else R.string.change_expiry,
                                    ),
                                )
                            }
                            OutlinedButton(onClick = { viewModel.deleteItem(item) }) {
                                Text(stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareDialog(
    item: UiItem,
    contacts: List<UiContact>,
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit,
) {
    var contactId by remember { mutableStateOf(contacts.firstOrNull()?.id.orEmpty()) }
    var mode by remember { mutableStateOf("copy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.send_item, item.title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.contact))
                for (contact in contacts) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = contactId == contact.id,
                            onClick = { contactId = contact.id },
                        )
                        Text(stringResource(R.string.contact_name_email, contact.name, contact.email))
                    }
                }

                Text(stringResource(R.string.share_mode))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == "copy", onClick = { mode = "copy" })
                    Text(stringResource(R.string.send_copy))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == "transfer", onClick = { mode = "transfer" })
                    Text(stringResource(R.string.transfer))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (contactId.isNotBlank()) onSend(contactId, mode) },
                enabled = contactId.isNotBlank(),
            ) {
                Text(stringResource(R.string.send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun ExpiryDialog(
    item: UiItem,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var value by remember(item.id) {
        mutableStateOf(item.expiresAt?.take(10).orEmpty())
    }
    val valid = value.isBlank() || runCatching { LocalDate.parse(value) }.isSuccess

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.expiry_title, item.title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.expiry_help))
                OutlinedTextField(
                    value = value,
                    onValueChange = { text ->
                        value = text.filter { it.isDigit() || it == '-' }.take(10)
                    },
                    label = { Text(stringResource(R.string.expiry_date_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(value.takeIf { it.isNotBlank() }) },
                enabled = valid,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun expiryLabel(value: String?): String? {
    if (value.isNullOrBlank()) {
        return null
    }

    val date = runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
        ?: return stringResource(R.string.expires_on, value.take(10))
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)

    return when {
        days < 0 -> stringResource(R.string.expired_days_ago, -days)
        days == 0L -> stringResource(R.string.expiry_today)
        days == 1L -> stringResource(R.string.expiry_tomorrow)
        else -> stringResource(R.string.expiry_in_days, days)
    }
}

@Composable
private fun FamilyScreen(state: UiState, viewModel: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var requestContactId by remember { mutableStateOf("") }
    var requestType by remember { mutableStateOf("") }
    var requestPurpose by remember { mutableStateOf("") }
    val acceptedContacts = state.contacts.filter { it.status == "accepted" }
    val effectiveContactId = requestContactId.ifBlank { acceptedContacts.firstOrNull()?.id.orEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::backToVault) {
                Text(stringResource(R.string.back))
            }
            Text(stringResource(R.string.family), style = MaterialTheme.typography.headlineSmall)
        }

        if (state.familyStatus != null) {
            Text(state.familyStatus)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.family_email)) },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { viewModel.addContact(email); email = "" }) {
                Text(stringResource(R.string.add))
            }
        }

        Text(stringResource(R.string.contacts), style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.contacts, key = { it.id }) { contact ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(contact.name, style = MaterialTheme.typography.titleMedium)
                        Text(contact.email)
                        Text(contact.status)
                        if (contact.status == "pending") {
                            OutlinedButton(onClick = { viewModel.acceptContact(contact.id) }) {
                                Text(stringResource(R.string.accept_contact))
                            }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.document_requests), style = MaterialTheme.typography.titleMedium)
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (acceptedContacts.isEmpty()) {
                            Text(stringResource(R.string.request_needs_contact))
                        } else {
                            Text(stringResource(R.string.request_from))
                            for (contact in acceptedContacts) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = effectiveContactId == contact.id,
                                        onClick = { requestContactId = contact.id },
                                    )
                                    Text(stringResource(R.string.contact_name_email, contact.name, contact.email))
                                }
                            }
                            OutlinedTextField(
                                value = requestType,
                                onValueChange = { requestType = it },
                                label = { Text(stringResource(R.string.request_document)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = requestPurpose,
                                onValueChange = { requestPurpose = it },
                                label = { Text(stringResource(R.string.request_purpose)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    viewModel.createShareRequest(
                                        effectiveContactId,
                                        requestType,
                                        requestPurpose,
                                    )
                                    requestType = ""
                                    requestPurpose = ""
                                },
                                enabled = effectiveContactId.isNotBlank() &&
                                    requestType.isNotBlank() && requestPurpose.isNotBlank(),
                            ) {
                                Text(stringResource(R.string.request))
                            }
                        }
                    }
                }
            }

            items(state.shareRequests, key = { it.id }) { request ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (request.incoming) {
                            Text(stringResource(R.string.request_incoming, request.person, request.documentType))
                            Text(request.purpose)
                            Text(
                                stringResource(R.string.request_send_hint),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            OutlinedButton(onClick = { viewModel.declineShareRequest(request.id) }) {
                                Text(stringResource(R.string.decline))
                            }
                        } else {
                            Text(stringResource(R.string.request_outgoing, request.documentType, request.person))
                            Text(request.purpose)
                            OutlinedButton(onClick = { viewModel.cancelShareRequest(request.id) }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.shared_with_me), style = MaterialTheme.typography.titleMedium)
            }

            items(state.incomingShares, key = { it.id }) { share ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            stringResource(
                                if (share.itemType == "ticket") {
                                    R.string.share_received_ticket
                                } else {
                                    R.string.share_received_document
                                },
                                share.sender,
                            ),
                        )
                        Text(
                            stringResource(
                                if (share.mode == "transfer") R.string.transfer_label else R.string.copy_label,
                            ),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.acceptShare(share.id) }) {
                                Text(stringResource(R.string.accept))
                            }
                            OutlinedButton(onClick = { viewModel.declineShare(share.id) }) {
                                Text(stringResource(R.string.decline))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::backToVault) {
                Text(stringResource(R.string.back))
            }
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.simple_mode),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(stringResource(R.string.simple_mode_help))
                }
                Switch(
                    checked = state.simpleMode,
                    onCheckedChange = viewModel::setSimpleMode,
                )
            }
        }

        Text(
            stringResource(R.string.language),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { setAppLocale(context, "es-MX") }) {
                Text(stringResource(R.string.language_spanish))
            }
            OutlinedButton(onClick = { setAppLocale(context, "en") }) {
                Text(stringResource(R.string.language_english))
            }
        }

        Text(stringResource(R.string.security_web))
        Text(stringResource(R.string.version, "0.1.0"))
    }
}

private fun setAppLocale(context: android.content.Context, languageTag: String) {
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        val manager = context.getSystemService(LocaleManager::class.java)
        manager.applicationLocales = android.os.LocaleList.forLanguageTags(languageTag)
    }
}

@Composable
private fun ViewerScreen(state: UiState, viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::closeViewer) {
                Text(stringResource(R.string.back))
            }
            Text(
                text = state.viewerName ?: stringResource(R.string.document),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }

        val path = state.viewerPath
        val mime = state.viewerMime.orEmpty()

        if (path == null) {
            Text(stringResource(R.string.open_failed))
        } else if (mime.contains("pdf")) {
            PdfViewer(path)
        } else if (mime.startsWith("image/")) {
            val bitmap = remember(path) {
                BitmapFactory.decodeFile(path)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = state.viewerName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(stringResource(R.string.image_failed))
            }
        } else {
            Text(stringResource(R.string.preview_unavailable))
        }
    }
}

@Composable
private fun PdfViewer(path: String) {
    var pageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(path, pageIndex) {
        val file = File(path)
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        pageCount = renderer.pageCount

        val page = renderer.openPage(pageIndex.coerceIn(0, renderer.pageCount - 1))
        val rendered = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        rendered.eraseColor(android.graphics.Color.WHITE)
        page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        descriptor.close()
        bitmap = rendered

        onDispose {
            bitmap = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.page_number, pageIndex + 1),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { if (pageIndex > 0) pageIndex -= 1 },
                enabled = pageIndex > 0,
            ) {
                Text(stringResource(R.string.previous))
            }
            Text(stringResource(R.string.page_of, pageIndex + 1, pageCount), modifier = Modifier.align(Alignment.CenterVertically))
            OutlinedButton(
                onClick = { if (pageIndex < pageCount - 1) pageIndex += 1 },
                enabled = pageIndex < pageCount - 1,
            ) {
                Text(stringResource(R.string.next))
            }
        }
    }
}
