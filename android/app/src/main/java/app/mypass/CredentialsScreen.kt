package app.mypass

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val credentialCategoryLabels = mapOf(
    "login" to R.string.credential_category_login,
    "bank" to R.string.credential_category_bank,
    "email" to R.string.credential_category_email,
    "social" to R.string.credential_category_social,
    "work" to R.string.credential_category_work,
    "wifi" to R.string.credential_category_wifi,
    "server" to R.string.credential_category_server,
    "other" to R.string.credential_category_other,
)

@Composable
private fun credentialCategoryLabel(category: String): String {
    return stringResource(credentialCategoryLabels[category] ?: R.string.credential_category_login)
}

@Composable
fun CredentialsScreen(state: UiState, viewModel: MainViewModel) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var deleteTarget by remember { mutableStateOf<UiItem?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val all = remember(state.items) {
        state.items.filter { it.credential != null && it.state != "transferred" }
    }
    val visible = remember(all, search, filter) {
        val query = search.trim()
        all.filter { item ->
            val credential = item.credential ?: return@filter false
            val matchesFilter = when (filter) {
                "favorite" -> credential.favorite
                "totp" -> credential.totpSecret != null
                else -> true
            }
            matchesFilter && (
                query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    credential.username.contains(query, ignoreCase = true) ||
                    credential.tags.any { it.contains(query, ignoreCase = true) } ||
                    credential.notes.contains(query, ignoreCase = true)
                )
        }
    }

    deleteTarget?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_credential_title),
            message = stringResource(R.string.confirm_delete_credential_body),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteItem(item)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MpTopBar(
            state = state,
            onLock = viewModel::lock,
            onSettings = viewModel::openSettings,
            onLogout = viewModel::logout,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (state.simpleMode) 560.dp else 320.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    VaultSectionTabs(
                        current = "credentials",
                        onDocuments = viewModel::backToVault,
                        onCredentials = {},
                        onMedical = viewModel::openMedical,
                    )
                    MpStatusBanner(state.status, state.error)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.credentials_title),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = stringResource(R.string.credentials_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MpColors.OnSurfaceVariant,
                            )
                        }
                        MpButton(
                            text = stringResource(R.string.add_credential),
                            onClick = viewModel::newCredential,
                            icon = Icons.Filled.Add,
                            enabled = !state.busy,
                        )
                    }
                    MpTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = stringResource(R.string.search_credentials),
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = MpColors.OnSurfaceVariant)
                        },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MpFilterChip(stringResource(R.string.all_credentials), filter == "all") { filter = "all" }
                        MpFilterChip(stringResource(R.string.favorite_credentials), filter == "favorite") { filter = "favorite" }
                        MpFilterChip(stringResource(R.string.totp_credentials), filter == "totp") { filter = "totp" }
                    }
                }
            }

            if (all.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MpEmptyState(
                        icon = Icons.Filled.Lock,
                        title = stringResource(R.string.no_credentials),
                        subtitle = stringResource(R.string.no_credentials_subtitle),
                    )
                }
            } else if (visible.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MpEmptyState(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.no_credential_results, search.trim()),
                    )
                }
            } else {
                gridItems(visible, key = { it.id }) { item ->
                    CredentialCard(
                        item = item,
                        now = now,
                        context = context,
                        onEdit = { viewModel.editCredential(item) },
                        onDelete = { deleteTarget = item },
                        onFavorite = { viewModel.toggleCredentialFavorite(item) },
                        onOpenAttachment = { viewModel.openItem(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialCard(
    item: UiItem,
    now: Long,
    context: Context,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onOpenAttachment: () -> Unit,
) {
    val credential = item.credential ?: return
    var revealPassword by remember(item.id) { mutableStateOf(false) }
    val usernameLabel = stringResource(R.string.username)
    val passwordLabel = stringResource(R.string.password)

    LaunchedEffect(revealPassword) {
        if (revealPassword) {
            delay(30_000)
            revealPassword = false
        }
    }

    MpCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MpIconTile(icon = Icons.Filled.Lock, size = 50)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = credentialCategoryLabel(credential.category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
                if (credential.tags.isNotEmpty()) {
                    Text(
                        text = credential.tags.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(if (credential.favorite) R.string.remove_favorite else R.string.add_favorite),
                    tint = if (credential.favorite) MpColors.Secondary else MpColors.OutlineVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CredentialValueRow(
                label = stringResource(R.string.username),
                value = credential.username.ifBlank { stringResource(R.string.no_username) },
                onCopy = {
                    SensitiveClipboard.copy(context, usernameLabel, credential.username)
                },
            )
            CredentialValueRow(
                label = stringResource(R.string.password),
                value = when {
                    credential.password.isBlank() -> stringResource(R.string.no_password)
                    revealPassword -> credential.password
                    else -> "••••••••••••"
                },
                monospace = true,
                onCopy = {
                    SensitiveClipboard.copy(context, passwordLabel, credential.password)
                },
                extra = {
                    TextButton(onClick = { revealPassword = !revealPassword }) {
                        Text(stringResource(if (revealPassword) R.string.hide else R.string.show))
                    }
                },
            )
            if (!credential.totpSecret.isNullOrBlank()) {
                val totp = remember(credential.totpSecret, now / 1000) {
                    Totp.parse(credential.totpSecret)?.let { config ->
                        Totp.code(config, now) to Totp.secondsLeft(config.period, now)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "TOTP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MpColors.OnSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = totp?.first ?: "••• •••",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MpColors.Primary,
                    )
                    Text(
                        text = stringResource(R.string.totp_seconds, totp?.second ?: 0),
                        style = MaterialTheme.typography.labelSmall,
                        color = MpColors.OnSurfaceVariant,
                    )
                    TextButton(onClick = {
                        totp?.first?.let {
                            SensitiveClipboard.copy(context, "TOTP", it)
                        }
                    }) {
                        Text(stringResource(R.string.copy))
                    }
                }
            }
            if (credential.url.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = credential.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(credential.url)))
                        }
                    }) {
                        Text(stringResource(R.string.open))
                    }
                }
            }
            if (item.blobId != null) {
                TextButton(onClick = onOpenAttachment) {
                    Text(stringResource(R.string.open_attachment))
                }
            }
            if (credential.notes.isNotBlank()) {
                Text(
                    text = credential.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.edit))
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.delete), color = MpColors.Error)
            }
        }
    }
}

@Composable
private fun CredentialValueRow(
    label: String,
    value: String,
    monospace: Boolean = false,
    onCopy: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MpColors.OnSurfaceVariant,
            modifier = Modifier.weight(0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(2f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        extra?.invoke()
        TextButton(onClick = onCopy) {
            Text(stringResource(R.string.copy))
        }
    }
}

@Composable
fun CredentialEditorScreen(state: UiState, viewModel: MainViewModel) {
    val source = state.credentialDraft ?: return
    var category by remember(source) { mutableStateOf(source.category) }
    var title by remember(source) { mutableStateOf(source.title) }
    var username by remember(source) { mutableStateOf(source.username) }
    var password by remember(source) { mutableStateOf(source.password) }
    var url by remember(source) { mutableStateOf(source.url) }
    var notes by remember(source) { mutableStateOf(source.notes) }
    var tags by remember(source) { mutableStateOf(source.tags) }
    var totpSecret by remember(source) { mutableStateOf(source.totpSecret.orEmpty()) }
    var attachmentUri by remember(source) { mutableStateOf(source.attachmentUri) }
    var attachmentName by remember(source) { mutableStateOf(source.attachmentName ?: source.existingAttachmentName) }
    var clearAttachment by remember(source) { mutableStateOf(source.clearAttachment) }
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            attachmentUri = uri.toString()
            attachmentName = displayName(context, uri)
            clearAttachment = false
        }
    }

    val draft = source.copy(
        category = category,
        title = title,
        username = username,
        password = password,
        url = url,
        notes = notes,
        tags = tags,
        passwordChangedAt = if (password != source.password) {
            java.time.LocalDate.now().toString()
        } else {
            source.passwordChangedAt
        },
        totpSecret = totpSecret.takeIf { it.isNotBlank() },
        attachmentUri = attachmentUri,
        attachmentName = attachmentName,
        clearAttachment = clearAttachment,
    )
    val strength = remember(password) { PasswordGenerator.strength(password) }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(if (source.isNew) R.string.new_credential else R.string.edit_credential),
            onBack = viewModel::discardCredentialDraft,
            action = {
                TextButton(
                    onClick = { viewModel.saveCredential(draft) },
                    enabled = !state.busy,
                ) {
                    Text(stringResource(R.string.save), color = MpColors.Primary)
                }
            },
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { MpStatusBanner(state.status, state.error) }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.credential_preview), style = MaterialTheme.typography.labelMedium, color = MpColors.OnSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MpIconTile(icon = Icons.Filled.Lock, size = 50)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title.ifBlank { username.ifBlank { stringResource(R.string.new_credential) } },
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = username.ifBlank { stringResource(R.string.no_username) },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MpColors.OnSurfaceVariant,
                                )
                            }
                            if (source.favorite) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = MpColors.Secondary)
                            }
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.account), style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            credentialCategoryLabels.keys.forEach { value ->
                                MpFilterChip(
                                    text = credentialCategoryLabel(value),
                                    selected = category == value,
                                    onClick = { category = value },
                                )
                            }
                        }
                        MpTextField(title, { title = it }, stringResource(R.string.service_name))
                        MpTextField(username, { username = it }, stringResource(R.string.username), keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                        MpPasswordField(password, { password = it }, stringResource(R.string.password))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            MpButton(
                                text = stringResource(R.string.generate_password),
                                onClick = { password = PasswordGenerator.generate(24) },
                                variant = MpButtonVariant.Secondary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = stringResource(
                                    when {
                                        strength >= 80 -> R.string.password_strength_strong
                                        strength >= 55 -> R.string.password_strength_medium
                                        password.isBlank() -> R.string.password_strength_none
                                        else -> R.string.password_strength_weak
                                    },
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    strength >= 80 -> MpColors.Primary
                                    strength >= 55 -> MpColors.Secondary
                                    else -> MpColors.Error
                                },
                            )
                        }
                        MpTextField(
                            url,
                            { url = it },
                            stringResource(R.string.website_optional),
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.two_factor), style = MaterialTheme.typography.titleMedium)
                        MpTextField(
                            totpSecret,
                            { totpSecret = it },
                            stringResource(R.string.totp_secret_optional),
                            placeholder = "JBSWY3DPEHPK3PXP u otpauth://...",
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        )
                        Text(
                            text = stringResource(R.string.totp_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                        if (totpSecret.isNotBlank() && Totp.parse(totpSecret) == null) {
                            Text(
                                text = stringResource(R.string.error_invalid_totp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.Error,
                            )
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.more_data), style = MaterialTheme.typography.titleMedium)
                        MpTextField(tags, { tags = it }, stringResource(R.string.tags))
                        MpTextField(
                            notes,
                            { notes = it },
                            stringResource(R.string.notes),
                            singleLine = false,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MpButton(
                                text = stringResource(R.string.choose_file),
                                onClick = { picker.launch("*/*") },
                                variant = MpButtonVariant.Secondary,
                                modifier = Modifier.weight(1f),
                            )
                            if (attachmentName != null) {
                                TextButton(onClick = {
                                    attachmentUri = null
                                    attachmentName = null
                                    clearAttachment = true
                                }) {
                                    Text(stringResource(R.string.remove), color = MpColors.Error)
                                }
                            }
                        }
                        attachmentName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.OnSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = stringResource(R.string.attachment_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            item {
                MpButton(
                    text = stringResource(R.string.save_credential),
                    onClick = { viewModel.saveCredential(draft) },
                    icon = Icons.Filled.Lock,
                    enabled = !state.busy,
                    loading = state.busy,
                    fillWidth = true,
                )
            }
        }
    }
}

private fun displayName(context: Context, uri: Uri): String {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: uri.lastPathSegment ?: "adjunto"
}
