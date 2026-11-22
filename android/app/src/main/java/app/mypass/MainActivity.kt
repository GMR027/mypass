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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private val lockRunnable = Runnable { viewModel.lock() }

    // Development-only switch used for visual verification on the tablet.
    // The release build must keep this false.
    private val allowScreenCapture = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!allowScreenCapture) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        enableEdgeToEdge()
        Reminders.schedule(this)
        Reminders.checkNow(this)
        setContent {
            val state by viewModel.state.collectAsState()
            val baseDensity = LocalDensity.current

            MyPassTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = baseDensity.density,
                        fontScale = baseDensity.fontScale * if (state.simpleMode) 1.2f else 1f,
                    ),
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MpColors.Background) {
                        BackHandler(
                            enabled = state.screen == Screen.Viewer ||
                                state.screen == Screen.Family ||
                                state.screen == Screen.Scan ||
                                state.screen == Screen.Settings ||
                                state.screen == Screen.Passes ||
                                state.screen == Screen.PassEditor,
                        ) {
                            when (state.screen) {
                                Screen.Viewer -> viewModel.closeViewer()
                                Screen.Scan -> viewModel.backFromScan()
                                Screen.PassEditor -> viewModel.discardPassDraft()
                                else -> viewModel.backToHome()
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

    val showBottomBar = state.screen in setOf(Screen.Home, Screen.Vault, Screen.Passes, Screen.Settings)

    LaunchedEffect(state.status, state.screen) {
        if (state.status != null && state.screen in setOf(Screen.Home, Screen.Vault, Screen.Passes, Screen.Settings)) {
            delay(3500)
            viewModel.clearStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MpColors.Background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (state.screen) {
                Screen.Loading -> LoadingScreen()
                Screen.Login -> LoginScreen(state = state, viewModel = viewModel)
                Screen.Pair -> PairScreen(state = state, viewModel = viewModel)
                Screen.PinSetup -> PinSetupScreen(state = state, viewModel = viewModel)
                Screen.TwoFactor -> TwoFactorScreen(state = state, viewModel = viewModel)
                Screen.Locked -> LockedScreen(state = state, viewModel = viewModel)
                Screen.Home -> HomeScreen(state = state, viewModel = viewModel)
                Screen.Vault -> VaultScreen(
                    state = state,
                    viewModel = viewModel,
                    onAdd = { filePicker.launch(arrayOf("application/pdf", "image/*")) },
                )
                Screen.Passes -> PassesScreen(state = state, viewModel = viewModel)
                Screen.PassEditor -> PassEditorScreen(state = state, viewModel = viewModel)
                Screen.Family -> FamilyScreen(state = state, viewModel = viewModel)
                Screen.Scan -> ScannerScreen(state = state, viewModel = viewModel)
                Screen.Settings -> SettingsScreen(state = state, viewModel = viewModel)
                Screen.Viewer -> ViewerScreen(state = state, viewModel = viewModel)
            }
        }
        if (showBottomBar) {
            MpBottomBar(screen = state.screen, viewModel = viewModel)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            MpLogo()
            CircularProgressIndicator(color = MpColors.Primary)
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MpColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AuthShell(content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MpColors.Background),
    ) {
        val wide = maxWidth >= 900.dp

        if (wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(top = 24.dp),
                    color = MpColors.Primary,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        MpLogo(inverse = true)
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(
                                text = stringResource(R.string.auth_brand_title),
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                            )
                            Text(
                                text = stringResource(R.string.auth_brand_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MpColors.PrimaryFixed,
                            )
                            AuthFeature(Icons.Filled.Lock, stringResource(R.string.auth_feature_encryption), stringResource(R.string.auth_feature_encryption_help))
                            AuthFeature(Icons.Filled.Phone, stringResource(R.string.auth_feature_devices), stringResource(R.string.auth_feature_devices_help))
                            AuthFeature(Icons.Filled.AccountCircle, stringResource(R.string.auth_feature_private), stringResource(R.string.auth_feature_private_help))
                        }
                        Text(
                            text = stringResource(R.string.auth_brand_footer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.PrimaryFixed.copy(alpha = 0.8f),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(modifier = Modifier.widthIn(max = 560.dp)) {
                        content()
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MpLogo()
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.widthIn(max = 560.dp)) {
                    content()
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AuthFeature(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MpColors.PrimaryFixed)
        }
    }
}

@Composable
private fun AuthCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    state: UiState,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MpIconTile(icon = icon, size = 64)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MpColors.OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MpStatusBanner(state.status, state.error)
                content()
            }
        }
    }
}

@Composable
private fun LoginScreen(state: UiState, viewModel: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthShell {
        AuthCard(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.sign_in),
            subtitle = stringResource(R.string.use_same_account),
            state = state,
        ) {
            MpTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.email),
                placeholder = "correo@ejemplo.com",
                keyboardType = KeyboardType.Email,
            )
            MpPasswordField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.password),
            )
            MpButton(
                text = stringResource(R.string.sign_in),
                onClick = { viewModel.login(email, password) },
                enabled = !state.busy,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MpColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.auth_login_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }
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

    AuthShell {
        AuthCard(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.two_factor),
            subtitle = when (method) {
                "recovery" -> stringResource(R.string.two_factor_recovery_hint)
                "email" -> stringResource(R.string.two_factor_email_hint, state.email)
                else -> stringResource(R.string.two_factor_totp_hint)
            },
            state = state,
        ) {
            if (state.twoFactorMethods.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if ("totp" in state.twoFactorMethods) {
                        MpFilterChip("App", method == "totp") { method = "totp"; code = "" }
                    }
                    if ("email" in state.twoFactorMethods) {
                        MpFilterChip("Correo", method == "email") { method = "email"; code = "" }
                    }
                    if ("recovery" in state.twoFactorMethods) {
                        MpFilterChip("Recuperación", method == "recovery") { method = "recovery"; code = "" }
                    }
                }
            }

            MpTextField(
                value = code,
                onValueChange = { text ->
                    code = if (method == "recovery") {
                        text.take(24)
                    } else {
                        text.filter { it.isDigit() }.take(6)
                    }
                },
                label = stringResource(if (method == "recovery") R.string.recovery_code else R.string.code),
                keyboardType = if (method == "recovery") KeyboardType.Text else KeyboardType.Number,
            )

            MpButton(
                text = stringResource(R.string.verify),
                onClick = { viewModel.verifyTwoFactor(code, method) },
                enabled = !state.busy && code.isNotBlank(),
                loading = state.busy,
                modifier = Modifier.fillMaxWidth(),
            )

            if (method == "email") {
                MpButton(
                    text = stringResource(R.string.send_email_code),
                    onClick = viewModel::sendTwoFactorEmail,
                    variant = MpButtonVariant.Secondary,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            MpTextButton(
                text = stringResource(R.string.back),
                onClick = viewModel::cancelTwoFactor,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun PairScreen(state: UiState, viewModel: MainViewModel) {
    var code by remember { mutableStateOf("") }

    AuthShell {
        AuthCard(
            icon = Icons.Filled.Phone,
            title = stringResource(R.string.connect_computer),
            subtitle = stringResource(R.string.pairing_hint),
            state = state,
        ) {
            MpTextField(
                value = code,
                onValueChange = { value -> code = value.filter(Char::isDigit).take(6) },
                label = stringResource(R.string.six_digit_code),
                keyboardType = KeyboardType.Number,
            )
            MpButton(
                text = stringResource(R.string.connect_phone),
                onClick = { viewModel.joinPairing(code) },
                enabled = !state.busy && code.length == 6,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            MpTextButton(
                text = stringResource(R.string.use_another_account),
                onClick = viewModel::logout,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun PinSetupScreen(state: UiState, viewModel: MainViewModel) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    AuthShell {
        AuthCard(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.create_pin),
            subtitle = stringResource(R.string.pin_help),
            state = state,
        ) {
            MpPasswordField(
                value = pin,
                onValueChange = { value -> pin = value.filter(Char::isDigit).take(12) },
                label = stringResource(R.string.pin_hint),
                keyboardType = KeyboardType.NumberPassword,
            )
            MpPasswordField(
                value = confirmation,
                onValueChange = { value -> confirmation = value.filter(Char::isDigit).take(12) },
                label = stringResource(R.string.repeat_pin),
                keyboardType = KeyboardType.NumberPassword,
            )
            MpButton(
                text = stringResource(R.string.save_pin),
                onClick = { viewModel.setupPin(pin, confirmation) },
                enabled = !state.busy,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LockedScreen(state: UiState, viewModel: MainViewModel) {
    var pin by remember { mutableStateOf("") }

    AuthShell {
        AuthCard(
            icon = Icons.Filled.Lock,
            title = stringResource(R.string.open_vault),
            subtitle = stringResource(R.string.locked_help),
            state = state,
        ) {
            MpPasswordField(
                value = pin,
                onValueChange = { value -> pin = value.filter(Char::isDigit).take(12) },
                label = stringResource(R.string.pin),
                keyboardType = KeyboardType.NumberPassword,
            )
            MpButton(
                text = stringResource(R.string.unlock),
                onClick = { viewModel.unlock(pin) },
                enabled = !state.busy,
                loading = state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            MpTextButton(
                text = stringResource(R.string.sign_out),
                onClick = viewModel::logout,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
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
    var deleteItem by remember { mutableStateOf<UiItem?>(null) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
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

    val vaultItems = remember(state.items) { state.items.filter { it.pass == null } }
    val visibleItems = remember(vaultItems, search, filter) {
        val query = search.trim()
        vaultItems.filter { item ->
            (filter == "all" || item.type == filter) &&
                (
                    query.isBlank() ||
                        item.title.contains(query, ignoreCase = true) ||
                        item.originalName.contains(query, ignoreCase = true) ||
                        item.ocrText.contains(query, ignoreCase = true)
                    )
        }
    }

    val expiring = remember(vaultItems) {
        vaultItems.filter { item ->
            val date = item.expiresAt?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            date != null && ChronoUnit.DAYS.between(LocalDate.now(), date) <= 30
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

    if (deleteItem != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_body),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteItem(deleteItem!!)
                deleteItem = null
            },
            onDismiss = { deleteItem = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MpTopBar(state = state, onLock = viewModel::lock)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (state.simpleMode) 560.dp else 300.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    VaultStatusCard(state)
                    MpStatusBanner(state.status, state.error)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MpButton(
                            text = stringResource(R.string.add),
                            onClick = onAdd,
                            icon = Icons.Filled.Add,
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                        )
                        MpButton(
                            text = stringResource(R.string.scan),
                            onClick = viewModel::openScanner,
                            variant = MpButtonVariant.Secondary,
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                        )
                        MpButton(
                            text = stringResource(R.string.sync),
                            onClick = viewModel::sync,
                            variant = MpButtonVariant.Quiet,
                            icon = Icons.Filled.Refresh,
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (vaultItems.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MpTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = stringResource(R.string.search_documents),
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MpColors.OnSurfaceVariant,
                            )
                        },
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MpFilterChip(stringResource(R.string.all_documents), filter == "all") { filter = "all" }
                        MpFilterChip(stringResource(R.string.document), filter == "document") { filter = "document" }
                        MpFilterChip(stringResource(R.string.ticket), filter == "ticket") { filter = "ticket" }
                        MpFilterChip(stringResource(R.string.card), filter == "card") { filter = "card" }
                        MpFilterChip(stringResource(R.string.other), filter == "other") { filter = "other" }
                    }
                }

                if (expiring.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ExpiryBanner(count = expiring.size)
                    }
                }
            }

            if (vaultItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MpEmptyState(
                        icon = Icons.Filled.List,
                        title = stringResource(R.string.no_documents),
                        subtitle = stringResource(R.string.no_documents_subtitle),
                    )
                }
            } else if (visibleItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MpEmptyState(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.no_search_results, search.trim().ifBlank { stringResource(R.string.all_documents) }),
                    )
                }
            } else {
                gridItems(visibleItems, key = { it.id }) { item ->
                    DocumentCard(
                        item = item,
                        hasContacts = acceptedContacts.isNotEmpty(),
                        onOpen = { viewModel.openItem(item) },
                        onShare = { shareItem = item },
                        onExpiry = { expiryItem = item },
                        onDelete = { deleteItem = item },
                    )
                }
            }
        }
    }
}

@Composable
internal fun VaultStatusCard(state: UiState) {
    MpCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MpIconTile(icon = Icons.Filled.Lock, size = 56)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.vault), style = MaterialTheme.typography.headlineSmall)
                    MpBadge(stringResource(R.string.vault_open_badge), MpBadgeTone.Primary)
                }
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.vault_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ExpiryBanner(count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MpColors.SecondaryFixed,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MpColors.OnSecondaryFixed)
            Column {
                Text(
                    text = stringResource(R.string.expiry_soon_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MpColors.OnSecondaryFixed,
                )
                Text(
                    text = if (count == 1) {
                        stringResource(R.string.expiry_soon_count_one, count)
                    } else {
                        stringResource(R.string.expiry_soon_count, count)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSecondaryFixed,
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(
    item: UiItem,
    hasContacts: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onExpiry: () -> Unit,
    onDelete: () -> Unit,
) {
    val expiry = expiryLabel(item.expiresAt)
    MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ItemTypeIcon(type = item.type)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = typeLabel(item.type),
                        style = MaterialTheme.typography.labelSmall,
                        color = MpColors.Primary,
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        if (item.pendingUpload) {
                            MpBadge(stringResource(R.string.pending_upload), MpBadgeTone.Warning)
                        }
                        if (expiry != null) {
                            val expired = item.expiresAt?.let { value ->
                                runCatching { LocalDate.parse(value.take(10)).isBefore(LocalDate.now()) }
                                    .getOrDefault(false)
                            } ?: false
                            MpBadge(
                                text = expiry,
                                tone = if (expired) MpBadgeTone.Danger else MpBadgeTone.Warning,
                            )
                        }
                    }
                }
            }

            if (item.ocrText.isNotBlank()) {
                Text(
                    text = item.ocrText.lineSequence().first().take(120),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            MpDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onOpen) {
                    Text(stringResource(R.string.open), style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.weight(1f))
                if (hasContacts) {
                    MpIconButton(Icons.Filled.Send, stringResource(R.string.send), onShare, tint = MpColors.Primary)
                }
                MpIconButton(Icons.Filled.DateRange, stringResource(R.string.expiry), onExpiry, tint = MpColors.Primary)
                MpIconButton(Icons.Filled.Delete, stringResource(R.string.delete), onDelete, tint = MpColors.Error)
            }
        }
    }
}

@Composable
private fun ItemTypeIcon(type: String, size: Int = 46) {
    val painter = when (type) {
        "ticket" -> painterResource(R.drawable.ic_ticket)
        "card" -> painterResource(R.drawable.ic_card)
        else -> painterResource(R.drawable.ic_document)
    }
    MpIconTile(painter = painter, size = size)
}

@Composable
private fun typeLabel(type: String): String = when (type) {
    "ticket" -> stringResource(R.string.ticket)
    "card" -> stringResource(R.string.card)
    "other" -> stringResource(R.string.other)
    else -> stringResource(R.string.document)
}

@Composable
private fun FamilyScreen(state: UiState, viewModel: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var requestContactId by remember { mutableStateOf("") }
    var requestType by remember { mutableStateOf("") }
    var requestPurpose by remember { mutableStateOf("") }
    val acceptedContacts = state.contacts.filter { it.status == "accepted" }
    val effectiveContactId = requestContactId.ifBlank { acceptedContacts.firstOrNull()?.id.orEmpty() }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(R.string.family),
            onBack = viewModel::backToHome,
            action = {
                MpIconButton(
                    icon = Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.lock),
                    onClick = viewModel::lock,
                    container = MpColors.SurfaceLow,
                )
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.family), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.family_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MpColors.OnSurfaceVariant,
                    )
                    MpStatusBanner(state.familyStatus, state.error, modifier = Modifier.padding(top = 4.dp))
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MpIconTile(icon = Icons.Filled.Person, size = 46)
                            Column {
                                Text(stringResource(R.string.add_contact), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(R.string.add_contact_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MpColors.OnSurfaceVariant,
                                )
                            }
                        }
                        MpTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.family_email),
                            keyboardType = KeyboardType.Email,
                        )
                        MpButton(
                            text = stringResource(R.string.add_contact),
                            onClick = { viewModel.addContact(email); email = "" },
                            icon = Icons.Filled.Add,
                            enabled = email.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.contacts), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    MpBadge(
                        text = if (acceptedContacts.size == 1) {
                            stringResource(R.string.contacts_count_one, acceptedContacts.size)
                        } else {
                            stringResource(R.string.contacts_count, acceptedContacts.size)
                        },
                        tone = MpBadgeTone.Neutral,
                    )
                }
            }

            if (state.contacts.isEmpty()) {
                item {
                    MpEmptyState(
                        icon = Icons.Filled.Person,
                        title = stringResource(R.string.no_contacts),
                    )
                }
            } else {
                lazyItems(state.contacts, key = { it.id }) { contact ->
                    ContactCard(contact = contact, onAccept = { viewModel.acceptContact(contact.id) })
                }
            }

            item {
                Text(stringResource(R.string.shared_with_me), style = MaterialTheme.typography.titleLarge)
            }

            if (state.incomingShares.isEmpty()) {
                item {
                    MpEmptyState(icon = Icons.Filled.Email, title = stringResource(R.string.no_shares))
                }
            } else {
                lazyItems(state.incomingShares, key = { it.id }) { share ->
                    ShareCard(
                        share = share,
                        onAccept = { viewModel.acceptShare(share.id) },
                        onDecline = { viewModel.declineShare(share.id) },
                    )
                }
            }

            item {
                Text(stringResource(R.string.document_requests), style = MaterialTheme.typography.titleLarge)
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (acceptedContacts.isEmpty()) {
                            Text(
                                stringResource(R.string.request_needs_contact),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MpColors.OnSurfaceVariant,
                            )
                        } else {
                            Text(stringResource(R.string.request_from), style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                for (contact in acceptedContacts) {
                                    MpFilterChip(
                                        text = contact.name,
                                        selected = effectiveContactId == contact.id,
                                        onClick = { requestContactId = contact.id },
                                    )
                                }
                            }
                            MpTextField(
                                value = requestType,
                                onValueChange = { requestType = it },
                                label = stringResource(R.string.request_document),
                            )
                            MpTextField(
                                value = requestPurpose,
                                onValueChange = { requestPurpose = it },
                                label = stringResource(R.string.request_purpose),
                            )
                            MpButton(
                                text = stringResource(R.string.request),
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
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            if (state.shareRequests.isEmpty()) {
                item {
                    MpEmptyState(icon = Icons.Filled.List, title = stringResource(R.string.no_requests))
                }
            } else {
                lazyItems(state.shareRequests, key = { it.id }) { request ->
                    ShareRequestCard(
                        request = request,
                        onDecline = { viewModel.declineShareRequest(request.id) },
                        onCancel = { viewModel.cancelShareRequest(request.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactCard(contact: UiContact, onAccept: () -> Unit) {
    MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MpAvatar(contact.name, size = 46)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MpBadge(
                        text = when (contact.status) {
                            "accepted" -> stringResource(R.string.accepted)
                            "pending" -> stringResource(R.string.pending)
                            else -> contact.status
                        },
                        tone = if (contact.status == "accepted") MpBadgeTone.Primary else MpBadgeTone.Warning,
                    )
                }
                Text(
                    text = contact.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (contact.status == "pending") {
                MpButton(
                    text = stringResource(R.string.accept_contact),
                    onClick = onAccept,
                    variant = MpButtonVariant.Secondary,
                    modifier = Modifier.widthIn(max = 160.dp),
                )
            }
        }
    }
}

@Composable
private fun ShareCard(share: UiShare, onAccept: () -> Unit, onDecline: () -> Unit) {
    MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ItemTypeIcon(type = share.itemType, size = 44)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (share.itemType == "ticket") R.string.share_received_ticket else R.string.share_received_document,
                            share.sender,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    MpBadge(
                        text = stringResource(if (share.mode == "transfer") R.string.transfer_label else R.string.copy_label),
                        tone = if (share.mode == "transfer") MpBadgeTone.Warning else MpBadgeTone.Neutral,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MpButton(
                    text = stringResource(R.string.accept),
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                )
                MpButton(
                    text = stringResource(R.string.decline),
                    onClick = onDecline,
                    variant = MpButtonVariant.Danger,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ShareRequestCard(request: UiShareRequest, onDecline: () -> Unit, onCancel: () -> Unit) {
    MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MpBadge(
                text = stringResource(if (request.incoming) R.string.request_incoming_badge else R.string.request_outgoing_badge),
                tone = if (request.incoming) MpBadgeTone.Primary else MpBadgeTone.Neutral,
            )
            Text(
                text = if (request.incoming) {
                    stringResource(R.string.request_incoming, request.person, request.documentType)
                } else {
                    stringResource(R.string.request_outgoing, request.documentType, request.person)
                },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = request.purpose,
                style = MaterialTheme.typography.bodySmall,
                color = MpColors.OnSurfaceVariant,
            )
            if (request.incoming) {
                Text(
                    text = stringResource(R.string.request_send_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
                MpButton(
                    text = stringResource(R.string.decline),
                    onClick = onDecline,
                    variant = MpButtonVariant.Danger,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                MpButton(
                    text = stringResource(R.string.cancel),
                    onClick = onCancel,
                    variant = MpButtonVariant.Danger,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(state: UiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentLanguage = LocalConfiguration.current.locales[0]?.language.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        MpTopBar(state = state, onLock = viewModel::lock)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.profile_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MpIconTile(icon = Icons.Filled.Person, size = 46)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.family), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.family_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.OnSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        MpButton(
                            text = stringResource(R.string.open_family),
                            onClick = viewModel::openFamily,
                            variant = MpButtonVariant.Quiet,
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    MpSwitchRow(
                        title = stringResource(R.string.simple_mode),
                        subtitle = stringResource(R.string.simple_mode_help),
                        checked = state.simpleMode,
                        onCheckedChange = viewModel::setSimpleMode,
                        icon = Icons.Filled.Person,
                    )
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MpFilterChip(
                                text = stringResource(R.string.language_spanish),
                                selected = currentLanguage.startsWith("es"),
                                onClick = { setAppLocale(context, "es-MX") },
                            )
                            MpFilterChip(
                                text = stringResource(R.string.language_english),
                                selected = currentLanguage == "en",
                                onClick = { setAppLocale(context, "en") },
                            )
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MpIconTile(icon = Icons.Filled.Lock, size = 46)
                        Column {
                            Text(stringResource(R.string.security_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.security_web),
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.OnSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.account_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = state.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        MpButton(
                            text = stringResource(R.string.sign_out),
                            onClick = viewModel::logout,
                            variant = MpButtonVariant.Danger,
                            icon = Icons.Filled.ExitToApp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.version, "0.1.0"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
internal fun MpTopBar(state: UiState, onLock: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MpLogo()
        Spacer(modifier = Modifier.weight(1f))
        MpIconButton(
            icon = Icons.Filled.Lock,
            contentDescription = stringResource(R.string.lock),
            onClick = onLock,
            container = MpColors.SurfaceLow,
        )
        Spacer(modifier = Modifier.width(10.dp))
        MpAvatar(name = state.email.ifBlank { "M" }, size = 40)
    }
}

@Composable
fun SubScreenHeader(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MpIconButton(Icons.Filled.ArrowBack, stringResource(R.string.back), onBack)
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun MpBottomBar(screen: Screen, viewModel: MainViewModel) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            selected = screen == Screen.Home,
            onClick = { if (screen != Screen.Home) viewModel.backToHome() },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.home)) },
            colors = bottomBarColors(),
        )
        NavigationBarItem(
            selected = screen == Screen.Vault,
            onClick = { if (screen != Screen.Vault) viewModel.backToVault() },
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            label = { Text(stringResource(R.string.vault)) },
            colors = bottomBarColors(),
        )
        NavigationBarItem(
            selected = screen == Screen.Passes,
            onClick = { if (screen != Screen.Passes) viewModel.openPasses() },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_ticket),
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(R.string.passes)) },
            colors = bottomBarColors(),
        )
        NavigationBarItem(
            selected = screen == Screen.Settings,
            onClick = { if (screen != Screen.Settings) viewModel.openSettings() },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text(stringResource(R.string.profile)) },
            colors = bottomBarColors(),
        )
    }
}

@Composable
private fun bottomBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MpColors.Primary,
    selectedTextColor = MpColors.Primary,
    indicatorColor = MpColors.PrimaryFixed,
    unselectedIconColor = MpColors.OnSurfaceVariant,
    unselectedTextColor = MpColors.OnSurfaceVariant,
)

@Composable
internal fun MpFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MpColors.Primary,
            selectedLabelColor = MpColors.OnPrimary,
            containerColor = Color.White,
            labelColor = MpColors.OnSurfaceVariant,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MpColors.OutlineVariant,
            selectedBorderColor = MpColors.Primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp,
        ),
    )
}

@Composable
internal fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = Color.White,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = MpColors.OnSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MpColors.Error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MpColors.Primary)
            }
        },
    )
}

@Composable
internal fun ShareDialog(
    item: UiItem,
    contacts: List<UiContact>,
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit,
) {
    var contactId by remember { mutableStateOf(contacts.firstOrNull()?.id.orEmpty()) }
    var mode by remember { mutableStateOf("copy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = Color.White,
        title = {
            Column {
                Text(stringResource(R.string.send_item, item.title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.share_dialog_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.contact), style = MaterialTheme.typography.titleSmall)
                for (contact in contacts) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(
                            selected = contactId == contact.id,
                            onClick = { contactId = contact.id },
                        )
                        Text(
                            text = stringResource(R.string.contact_name_email, contact.name, contact.email),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Text(stringResource(R.string.share_mode), style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(
                        selected = mode == "copy",
                        onClick = { mode = "copy" },
                    )
                    Text(stringResource(R.string.send_copy), style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.RadioButton(
                        selected = mode == "transfer",
                        onClick = { mode = "transfer" },
                    )
                    Text(stringResource(R.string.transfer), style = MaterialTheme.typography.bodyMedium)
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MpColors.SecondaryFixed,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MpColors.OnSecondaryFixed, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(R.string.transfer_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSecondaryFixed,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (contactId.isNotBlank()) onSend(contactId, mode) },
                enabled = contactId.isNotBlank(),
            ) {
                Text(stringResource(R.string.send), color = MpColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MpColors.OnSurfaceVariant)
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
        shape = MaterialTheme.shapes.large,
        containerColor = Color.White,
        title = { Text(stringResource(R.string.expiry_title, item.title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.expiry_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MpColors.OnSurfaceVariant,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { text ->
                        value = text.filter { it.isDigit() || it == '-' }.take(10)
                    },
                    label = { Text(stringResource(R.string.expiry_date_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MpColors.Primary,
                        unfocusedBorderColor = MpColors.OutlineVariant,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(value.takeIf { it.isNotBlank() }) },
                enabled = valid,
            ) {
                Text(stringResource(R.string.save), color = MpColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MpColors.OnSurfaceVariant)
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

private fun setAppLocale(context: android.content.Context, languageTag: String) {
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        val manager = context.getSystemService(LocaleManager::class.java)
        manager.applicationLocales = android.os.LocaleList.forLanguageTags(languageTag)
    }
}

@Composable
private fun ViewerScreen(state: UiState, viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        SubScreenHeader(
            title = state.viewerName ?: stringResource(R.string.document),
            onBack = viewModel::closeViewer,
        )
        MpStatusBanner(state.status, state.error)
        Spacer(modifier = Modifier.height(8.dp))

        val path = state.viewerPath
        val mime = state.viewerMime.orEmpty()

        MpCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(8.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (path == null) {
                    Text(stringResource(R.string.open_failed), color = MpColors.OnSurfaceVariant)
                } else if (mime.contains("pdf")) {
                    PdfViewer(path)
                } else if (mime.startsWith("image/")) {
                    val bitmap = remember(path) { BitmapFactory.decodeFile(path) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = state.viewerName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(stringResource(R.string.image_failed), color = MpColors.OnSurfaceVariant)
                    }
                } else {
                    Text(stringResource(R.string.preview_unavailable), color = MpColors.OnSurfaceVariant)
                }
            }
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

        onDispose { bitmap = null }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.page_number, pageIndex + 1),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MpButton(
                text = stringResource(R.string.previous),
                onClick = { if (pageIndex > 0) pageIndex -= 1 },
                variant = MpButtonVariant.Secondary,
                enabled = pageIndex > 0,
            )
            Text(
                text = stringResource(R.string.page_of, pageIndex + 1, pageCount),
                style = MaterialTheme.typography.bodyMedium,
            )
            MpButton(
                text = stringResource(R.string.next),
                onClick = { if (pageIndex < pageCount - 1) pageIndex += 1 },
                variant = MpButtonVariant.Secondary,
                enabled = pageIndex < pageCount - 1,
            )
        }
    }
}
