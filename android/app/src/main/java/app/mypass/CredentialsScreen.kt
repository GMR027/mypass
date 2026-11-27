package app.mypass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
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

private fun credentialTypeIcon(credential: UiCredential) = when {
    credential.category == "custom" -> credential.categoryIcon.ifBlank { "lock" }
    credential.category == "email" -> "mail"
    credential.category == "social" -> "person"
    credential.category == "work" -> "build"
    credential.category == "wifi" -> "home"
    credential.category == "server" -> "settings"
    credential.category == "bank" -> "shopping"
    else -> "lock"
}

private fun credentialIconVector(key: String) = when (key) {
    "mail" -> Icons.Filled.Email
    "person" -> Icons.Filled.Person
    "phone" -> Icons.Filled.Phone
    "shopping" -> Icons.Filled.ShoppingCart
    "star" -> Icons.Filled.Star
    "play" -> Icons.Filled.PlayArrow
    "build" -> Icons.Filled.Build
    "place" -> Icons.Filled.Place
    "home" -> Icons.Filled.Home
    "settings" -> Icons.Filled.Settings
    "search" -> Icons.Filled.Search
    else -> Icons.Filled.Lock
}

@Composable
private fun credentialTypeLabel(credential: UiCredential): String {
    return if (credential.category == "custom") {
        credential.categoryLabel.ifBlank { stringResource(R.string.credential_category_custom) }
    } else {
        credentialCategoryLabel(credential.category)
    }
}

private val customCredentialIcons = listOf(
    "lock", "mail", "person", "phone", "shopping", "star",
    "play", "build", "place", "home", "settings", "search",
)

private val credentialBannerColors = listOf(
    "#1B4332", "#0C2340", "#4C1D95", "#7F1D1D",
    "#9A3412", "#0E7490", "#374151", "#B45309",
)

@Composable
fun CredentialsScreen(state: UiState, viewModel: MainViewModel) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var groupFilter by remember { mutableStateOf("all") }
    var selectedCredentialId by remember { mutableStateOf<String?>(null) }
    var revealCredentialId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<UiItem?>(null) }
    var shareTarget by remember { mutableStateOf<UiItem?>(null) }
    var manageSharesTarget by remember { mutableStateOf<UiItem?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val acceptedContacts = state.contacts.filter { it.status == "accepted" }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    val all = remember(state.items) {
        state.items.filter { it.credential != null && it.state != "transferred" }
    }
    val groups = remember(all) {
        all.mapNotNull { it.credential?.group?.takeIf(String::isNotBlank) }.distinct().sorted()
    }
    val visible = remember(all, search, filter, groupFilter) {
        val query = search.trim()
        all.filter { item ->
            val credential = item.credential ?: return@filter false
            val matchesFilter = when (filter) {
                "favorite" -> credential.favorite
                "totp" -> credential.totpSecret != null
                else -> true
            }
            val matchesGroup = when (groupFilter) {
                "all" -> true
                "none" -> credential.group.isBlank()
                else -> credential.group == groupFilter
            }
            matchesFilter && matchesGroup && (
                query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    credential.username.contains(query, ignoreCase = true) ||
                    credential.group.contains(query, ignoreCase = true) ||
                    credential.categoryLabel.contains(query, ignoreCase = true) ||
                    credential.tags.any { it.contains(query, ignoreCase = true) } ||
                    credential.notes.contains(query, ignoreCase = true)
                )
        }
    }
    val visibleGroups = remember(visible, filter, groupFilter, search) {
        if (filter == "all" && groupFilter == "all" && search.isBlank()) {
            val buckets = visible.groupBy { it.credential?.group.orEmpty() }
            buckets.keys.sortedWith(
                compareBy<String> { it.isBlank() }.thenBy { it.lowercase() },
            ).map { group -> group to buckets.getValue(group) }
        } else {
            listOf("" to visible)
        }
    }

    LaunchedEffect(visible.map { it.id }) {
        if (selectedCredentialId != null && visible.none { it.id == selectedCredentialId }) {
            selectedCredentialId = null
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

    shareTarget?.let { item ->
        ShareDialog(
            item = item,
            contacts = acceptedContacts,
            onDismiss = { shareTarget = null },
            onSend = { contactId, mode ->
                viewModel.sendShare(item, contactId, mode)
                shareTarget = null
            },
        )
    }

    manageSharesTarget?.let { item ->
        ManageSharesDialog(
            itemId = item.id,
            itemTitle = item.title,
            shares = state.outgoingShares,
            canShare = acceptedContacts.isNotEmpty(),
            onDismiss = { manageSharesTarget = null },
            onShare = {
                shareTarget = item
                manageSharesTarget = null
            },
            onRevoke = viewModel::revokeShare,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MpTopBar(
            state = state,
            onLock = viewModel::lock,
            onSettings = viewModel::openSettings,
            onLogout = viewModel::logout,
            onNotifications = viewModel::openNotifications,
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (state.simpleMode) 560.dp else 320.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
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
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val compact = maxWidth < 520.dp
                        if (compact) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column {
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
                                    fillWidth = true,
                                )
                            }
                        } else {
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
                        }
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
                    if (groups.isNotEmpty() || all.any { it.credential?.group.isNullOrBlank() }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MpFilterChip(stringResource(R.string.all_groups), groupFilter == "all") {
                                groupFilter = "all"
                            }
                            groups.forEach { group ->
                                MpFilterChip(group, groupFilter == group) {
                                    groupFilter = group
                                }
                            }
                            if (all.any { it.credential?.group.isNullOrBlank() }) {
                                MpFilterChip(stringResource(R.string.ungrouped), groupFilter == "none") {
                                    groupFilter = "none"
                                }
                            }
                        }
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
                visibleGroups.forEach { (group, groupItems) ->
                    if (groupItems.isNotEmpty() && filter == "all" && groupFilter == "all" && search.isBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "group:$group") {
                            Text(
                                text = if (group.isBlank()) stringResource(R.string.ungrouped) else group,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MpColors.OnSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    val selectedIndex = groupItems.indexOfFirst { it.id == selectedCredentialId }
                        .takeIf { it >= 0 }
                    item(span = { GridItemSpan(maxLineSpan) }, key = "stack:$group") {
                        MpCascadeStack(selectedIndex = selectedIndex) {
                            groupItems.forEachIndexed { index, item ->
                                if (index == selectedIndex) {
                                    MpRevealOnSelection(
                                        selectionKey = if (revealCredentialId == item.id) item.id else null,
                                        onRevealed = { revealCredentialId = null },
                                    ) {
                                        CredentialFlipCard(
                                            item = item,
                                            now = now,
                                            context = context,
                                            shared = state.isItemShared(item.id),
                                            onEdit = { viewModel.editCredential(item) },
                                            onDelete = { deleteTarget = item },
                                            onFavorite = { viewModel.toggleCredentialFavorite(item) },
                                            onOpenAttachment = { viewModel.openItem(item) },
                                            onShare = { shareTarget = item },
                                            onManageShares = { manageSharesTarget = item },
                                        )
                                    }
                                } else {
                                    CredentialCard(
                                        item = item,
                                        now = now,
                                        context = context,
                                        headerOnly = true,
                                        shared = state.isItemShared(item.id),
                                        onSelect = {
                                            selectedCredentialId = item.id
                                            revealCredentialId = item.id
                                        },
                                        onEdit = { viewModel.editCredential(item) },
                                        onDelete = { deleteTarget = item },
                                        onFavorite = { viewModel.toggleCredentialFavorite(item) },
                                        onOpenAttachment = { viewModel.openItem(item) },
                                        onShare = { shareTarget = item },
                                        onManageShares = { manageSharesTarget = item },
                                    )
                                }
                            }
                        }
                    }
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
    headerOnly: Boolean = false,
    shared: Boolean = false,
    onSelect: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onOpenAttachment: () -> Unit,
    onShare: () -> Unit,
    onManageShares: () -> Unit,
) {
    val credential = item.credential ?: return
    var revealPassword by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(revealPassword) {
        if (revealPassword) {
            delay(30_000)
            revealPassword = false
        }
    }

    val bannerBitmap = remember(credential.appearanceImage) {
        credential.appearanceImage?.let(::decodeDataUrl)
    }
    val bannerColor = runCatching {
        Color(android.graphics.Color.parseColor(credential.appearanceColor))
    }.getOrDefault(MpColors.Primary)
    val hasAppearance = credential.appearanceMode != "none"
        && (credential.appearanceColor.isNotBlank() || bannerBitmap != null)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onSelect != null) Modifier.clickable(onClick = onSelect) else Modifier),
    ) {
        val compact = maxWidth < 420.dp
        val padding = if (compact) 16.dp else 20.dp
        val body: @Composable () -> Unit = {
            CredentialBody(
                item = item,
                credential = credential,
                now = now,
                context = context,
                revealPassword = revealPassword,
                compact = compact,
                onToggleReveal = { revealPassword = !revealPassword },
                onEdit = onEdit,
                onDelete = onDelete,
                onOpenAttachment = onOpenAttachment,
                onShare = onShare,
                onManageShares = onManageShares,
            )
        }

        if (!hasAppearance) {
            MpCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(padding),
            ) {
                Column {
                    CredentialHead(item, credential, dark = false, compact = compact, onFavorite = onFavorite, shared = shared)
                    if (!headerOnly) {
                        body()
                    }
                }
            }
        } else if (credential.appearanceMode == "banner") {
            MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bannerColor),
                    ) {
                        bannerBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.24f), Color.Black.copy(alpha = 0.5f)),
                                    ),
                                ),
                        )
                        Column(modifier = Modifier.padding(padding)) {
                            CredentialHead(item, credential, dark = true, compact = compact, onFavorite = onFavorite, shared = shared)
                        }
                    }
                    if (!headerOnly) {
                        Column(modifier = Modifier.padding(padding)) {
                            body()
                        }
                    }
                }
            }
        } else {
            MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bannerColor),
                ) {
                    bannerBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Column(modifier = Modifier.padding(if (compact) 12.dp else 16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(18.dp))
                                .padding(padding),
                        ) {
                            CredentialHead(item, credential, dark = false, compact = compact, onFavorite = onFavorite, shared = shared)
                            if (!headerOnly) {
                                body()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialFlipCard(
    item: UiItem,
    now: Long,
    context: Context,
    shared: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onOpenAttachment: () -> Unit,
    onShare: () -> Unit,
    onManageShares: () -> Unit,
) {
    val credential = item.credential ?: return
    var revealPassword by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(revealPassword) {
        if (revealPassword) {
            delay(30_000)
            revealPassword = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 420.dp

        MpFlipCard(
            itemKey = item.id,
            shape = MaterialTheme.shapes.large,
            containerColor = MpColors.Surface,
            front = { onFlip ->
                Column {
                    CredentialFlipHeader(
                        item = item,
                        credential = credential,
                        compact = compact,
                        onFavorite = onFavorite,
                        shared = shared,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        MpFlipButton(
                            text = stringResource(R.string.view_back),
                            onClick = onFlip,
                        )
                    }
                }
            },
            back = { onFlipBack ->
                Column {
                    CredentialFlipHeader(
                        item = item,
                        credential = credential,
                        compact = compact,
                        onFavorite = onFavorite,
                        shared = shared,
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CredentialBody(
                            item = item,
                            credential = credential,
                            now = now,
                            context = context,
                            revealPassword = revealPassword,
                            compact = compact,
                            onToggleReveal = { revealPassword = !revealPassword },
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onOpenAttachment = onOpenAttachment,
                            onShare = onShare,
                            onManageShares = onManageShares,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            MpFlipButton(
                                text = stringResource(R.string.back_to_front),
                                onClick = onFlipBack,
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun CredentialFlipHeader(
    item: UiItem,
    credential: UiCredential,
    compact: Boolean,
    onFavorite: () -> Unit,
    shared: Boolean = false,
) {
    val bannerBitmap = remember(credential.appearanceImage) {
        credential.appearanceImage?.let(::decodeDataUrl)
    }
    val bannerColor = runCatching {
        Color(android.graphics.Color.parseColor(credential.appearanceColor))
    }.getOrDefault(MpColors.Primary)
    val hasAppearance = credential.appearanceMode != "none"
        && (credential.appearanceColor.isNotBlank() || bannerBitmap != null)

    if (!hasAppearance) {
        Column(modifier = Modifier.padding(18.dp)) {
            CredentialHead(
                item = item,
                credential = credential,
                dark = false,
                compact = compact,
                onFavorite = onFavorite,
                shared = shared,
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bannerColor),
    ) {
        bannerBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.24f), Color.Black.copy(alpha = 0.5f)),
                    ),
                ),
        )
        Column(modifier = Modifier.padding(18.dp)) {
            CredentialHead(
                item = item,
                credential = credential,
                dark = true,
                compact = compact,
                onFavorite = onFavorite,
                shared = shared,
            )
        }
    }
}

@Composable
private fun CredentialHead(
    item: UiItem,
    credential: UiCredential,
    dark: Boolean,
    compact: Boolean,
    onFavorite: () -> Unit,
    shared: Boolean = false,
) {
    val logoBitmap = remember(credential.logo) { credential.logo?.let(::decodeDataUrl) }
    val tileSize = if (compact) 44 else 50
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
    ) {
        if (logoBitmap != null) {
            Image(
                bitmap = logoBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(tileSize.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (dark) Color.White.copy(alpha = 0.16f) else MpColors.SurfaceLow)
                    .padding(if (compact) 6.dp else 7.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            MpIconTile(
                icon = credentialIconVector(credentialTypeIcon(credential)),
                size = tileSize,
                background = if (dark) Color.White.copy(alpha = 0.16f) else MpColors.PrimaryFixed,
                tint = if (dark) Color.White else MpColors.Primary,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (dark) Color.White else MpColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = credentialTypeLabel(credential),
                style = MaterialTheme.typography.bodySmall,
                color = if (dark) Color.White.copy(alpha = 0.78f) else MpColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (shared) {
                MpBadge(text = stringResource(R.string.shared), tone = MpBadgeTone.Primary)
            }
            if (credential.group.isNotBlank()) {
                Text(
                    text = credential.group,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dark) Color.White.copy(alpha = 0.8f) else MpColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (credential.tags.isNotEmpty()) {
                Text(
                    text = credential.tags.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dark) Color.White.copy(alpha = 0.7f) else MpColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onFavorite) {
            Icon(
                Icons.Filled.Star,
                contentDescription = stringResource(if (credential.favorite) R.string.remove_favorite else R.string.add_favorite),
                tint = when {
                    dark -> Color.White
                    credential.favorite -> MpColors.Secondary
                    else -> MpColors.OutlineVariant
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CredentialBody(
    item: UiItem,
    credential: UiCredential,
    now: Long,
    context: Context,
    revealPassword: Boolean,
    compact: Boolean,
    onToggleReveal: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenAttachment: () -> Unit,
    onShare: () -> Unit,
    onManageShares: () -> Unit,
) {
    val usernameLabel = stringResource(R.string.username)
    val passwordLabel = stringResource(R.string.password)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
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
                TextButton(onClick = onToggleReveal) {
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
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "TOTP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MpColors.OnSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = totp?.first ?: "••• •••",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MpColors.Primary,
                        )
                        Text(
                            text = stringResource(R.string.totp_seconds, totp?.second ?: 0),
                            style = MaterialTheme.typography.labelSmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = {
                            totp?.first?.let {
                                SensitiveClipboard.copy(context, "TOTP", it)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Text(stringResource(R.string.copy))
                    }
                }
            } else {
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
        }
        if (credential.url.isNotBlank()) {
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = credential.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(credential.url)))
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Text(stringResource(R.string.open))
                    }
                }
            } else {
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

    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        TextButton(onClick = onEdit) {
            Text(stringResource(R.string.edit))
        }
        TextButton(onClick = onShare) {
            Text(stringResource(R.string.share))
        }
        TextButton(onClick = onManageShares) {
            Text(stringResource(R.string.manage_shares))
        }
        TextButton(onClick = onDelete) {
            Text(stringResource(R.string.delete), color = MpColors.Error)
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MpColors.OnSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    extra?.invoke()
                    TextButton(
                        onClick = onCopy,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    ) {
                        Text(stringResource(R.string.copy))
                    }
                }
            }
        } else {
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CredentialEditorScreen(state: UiState, viewModel: MainViewModel) {
    val source = state.credentialDraft ?: return
    var category by remember(source) { mutableStateOf(source.category) }
    var categoryLabel by remember(source) { mutableStateOf(source.categoryLabel) }
    var categoryIcon by remember(source) { mutableStateOf(source.categoryIcon) }
    var group by remember(source) { mutableStateOf(source.group) }
    var logo by remember(source) { mutableStateOf(source.logo) }
    var appearanceMode by remember(source) { mutableStateOf(source.appearanceMode) }
    var appearanceColor by remember(source) { mutableStateOf(source.appearanceColor) }
    var appearanceImage by remember(source) { mutableStateOf(source.appearanceImage) }
    var showCustomTypeDialog by remember { mutableStateOf(false) }
    var customTypeName by remember { mutableStateOf("") }
    var customTypeIcon by remember { mutableStateOf("lock") }
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

    val customTypes = remember(state.items) {
        state.items.mapNotNull { item ->
            val credential = item.credential ?: return@mapNotNull null
            if (credential.category != "custom" || credential.categoryLabel.isBlank()) {
                return@mapNotNull null
            }
            credential.categoryLabel to (credential.categoryIcon.ifBlank { "lock" })
        }.distinctBy { it.first.lowercase() }
    }
    val existingGroups = remember(state.items) {
        state.items.mapNotNull { it.credential?.group?.takeIf(String::isNotBlank) }
            .distinct()
            .sorted()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            attachmentUri = uri.toString()
            attachmentName = displayName(context, uri)
            clearAttachment = false
        }
    }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            encodeCredentialLogo(context, uri)?.let { logo = it }
        }
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            encodeCredentialBanner(context, uri)?.let {
                appearanceImage = it
                if (appearanceMode == "none") {
                    appearanceMode = "banner"
                }
            }
        }
    }

    val draft = source.copy(
        category = category,
        categoryLabel = categoryLabel,
        categoryIcon = categoryIcon,
        group = group,
        logo = logo,
        appearanceMode = appearanceMode,
        appearanceColor = appearanceColor,
        appearanceImage = appearanceImage,
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

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
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
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MpStatusBanner(state.status, state.error) }

            item {
                val previewLogo = remember(logo) { logo?.let(::decodeDataUrl) }
                val previewBanner = remember(appearanceImage) { appearanceImage?.let(::decodeDataUrl) }
                val previewColor = runCatching {
                    Color(android.graphics.Color.parseColor(appearanceColor))
                }.getOrDefault(MpColors.Primary)
                val previewIcon = when {
                    category == "custom" -> categoryIcon.ifBlank { "lock" }
                    category == "email" -> "mail"
                    category == "social" -> "person"
                    category == "work" -> "build"
                    category == "wifi" -> "home"
                    category == "server" -> "settings"
                    category == "bank" -> "shopping"
                    else -> "lock"
                }
                MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Column {
                        if (appearanceMode != "none" && (appearanceColor.isNotBlank() || previewBanner != null)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .background(previewColor),
                            ) {
                                previewBanner?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.matchParentSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (previewLogo != null) {
                                Image(
                                    bitmap = previewLogo.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MpColors.SurfaceLow)
                                        .padding(7.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                MpIconTile(icon = credentialIconVector(previewIcon), size = 50)
                            }
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
                                if (group.isNotBlank()) {
                                    Text(
                                        text = group,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MpColors.Primary,
                                    )
                                }
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
                                    selected = category == value && categoryLabel.isBlank(),
                                    onClick = {
                                        category = value
                                        categoryLabel = ""
                                        categoryIcon = ""
                                    },
                                )
                            }
                            customTypes.forEach { (label, icon) ->
                                MpFilterChip(
                                    text = label,
                                    selected = category == "custom" && categoryLabel.equals(label, ignoreCase = true),
                                    onClick = {
                                        category = "custom"
                                        categoryLabel = label
                                        categoryIcon = icon
                                    },
                                )
                            }
                            if (category == "custom" && categoryLabel.isNotBlank()
                                && customTypes.none { it.first.equals(categoryLabel, ignoreCase = true) }
                            ) {
                                MpFilterChip(
                                    text = categoryLabel,
                                    selected = true,
                                    onClick = {},
                                )
                            }
                            MpFilterChip(
                                text = "+ " + stringResource(R.string.create_account_type),
                                selected = false,
                                onClick = {
                                    customTypeName = ""
                                    customTypeIcon = "lock"
                                    showCustomTypeDialog = true
                                },
                            )
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
                                text = stringResource(R.string.generate),
                                onClick = { password = PasswordGenerator.generate(24) },
                                variant = MpButtonVariant.Secondary,
                                modifier = Modifier.weight(1f),
                                compact = true,
                                maxLines = 1,
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
                        MpTextField(
                            group,
                            { group = it },
                            stringResource(R.string.credential_group),
                        )
                        if (existingGroups.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                existingGroups.forEach { existing ->
                                    MpFilterChip(
                                        text = existing,
                                        selected = group == existing,
                                        onClick = { group = existing },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                val previewLogo = remember(logo) { logo?.let(::decodeDataUrl) }
                val previewBanner = remember(appearanceImage) { appearanceImage?.let(::decodeDataUrl) }
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.credential_appearance), style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                "none" to stringResource(R.string.appearance_none),
                                "banner" to stringResource(R.string.appearance_banner),
                                "background" to stringResource(R.string.appearance_background),
                            ).forEach { (value, label) ->
                                MpFilterChip(
                                    text = label,
                                    selected = appearanceMode == value,
                                    onClick = { appearanceMode = value },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (previewLogo != null) {
                                Image(
                                    bitmap = previewLogo.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MpColors.SurfaceLow)
                                        .padding(7.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                MpIconTile(icon = credentialIconVector(categoryIcon.ifBlank { "lock" }), size = 52)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MpButton(
                                    text = stringResource(R.string.choose_logo),
                                    onClick = { logoPicker.launch("image/*") },
                                    variant = MpButtonVariant.Secondary,
                                    fillWidth = true,
                                )
                                if (logo != null) {
                                    MpButton(
                                        text = stringResource(R.string.remove_logo),
                                        onClick = { logo = null },
                                        variant = MpButtonVariant.Quiet,
                                        fillWidth = true,
                                    )
                                }
                            }
                        }
                        Text(stringResource(R.string.credential_banner_color), style = MaterialTheme.typography.labelMedium, color = MpColors.OnSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            credentialBannerColors.forEach { color ->
                                val parsed = runCatching { Color(android.graphics.Color.parseColor(color)) }
                                    .getOrDefault(MpColors.Primary)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(parsed)
                                        .then(
                                            if (appearanceColor.equals(color, ignoreCase = true)) {
                                                Modifier.border(3.dp, MpColors.Primary, CircleShape)
                                            } else {
                                                Modifier.border(1.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
                                            },
                                        )
                                        .clickable {
                                            appearanceColor = color
                                            if (appearanceMode == "none") {
                                                appearanceMode = "banner"
                                            }
                                        },
                                )
                            }
                        }
                        MpTextField(
                            appearanceColor,
                            { value ->
                                appearanceColor = value.take(7)
                                if (appearanceMode == "none" && value.startsWith("#")) {
                                    appearanceMode = "banner"
                                }
                            },
                            stringResource(R.string.custom_color),
                            placeholder = "#1B4332",
                        )
                        Text(stringResource(R.string.credential_banner_image), style = MaterialTheme.typography.labelMedium, color = MpColors.OnSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (previewBanner != null) {
                                Image(
                                    bitmap = previewBanner.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(width = 112.dp, height = 64.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MpButton(
                                    text = stringResource(R.string.choose_banner_image),
                                    onClick = { bannerPicker.launch("image/*") },
                                    variant = MpButtonVariant.Secondary,
                                    fillWidth = true,
                                )
                                if (appearanceImage != null) {
                                    MpButton(
                                        text = stringResource(R.string.remove_image),
                                        onClick = { appearanceImage = null },
                                        variant = MpButtonVariant.Quiet,
                                        fillWidth = true,
                                    )
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.credential_banner_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
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
                        MpButton(
                            text = stringResource(R.string.scan_totp_qr),
                            onClick = viewModel::openTotpScanner,
                            variant = MpButtonVariant.Secondary,
                            fillWidth = true,
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
                                compact = true,
                                maxLines = 1,
                            )
                            if (attachmentName != null) {
                                MpButton(
                                    text = stringResource(R.string.remove),
                                    onClick = {
                                        attachmentUri = null
                                        attachmentName = null
                                        clearAttachment = true
                                    },
                                    variant = MpButtonVariant.Quiet,
                                    compact = true,
                                    maxLines = 1,
                                )
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

        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MpColors.Surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MpButton(
                text = stringResource(R.string.save_credential),
                onClick = { viewModel.saveCredential(draft) },
                icon = Icons.Filled.Lock,
                enabled = !state.busy,
                loading = state.busy,
                fillWidth = true,
            )
        }

        if (showCustomTypeDialog) {
            AlertDialog(
                onDismissRequest = { showCustomTypeDialog = false },
                title = { Text(stringResource(R.string.create_account_type)) },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MpTextField(
                            value = customTypeName,
                            onValueChange = { customTypeName = it.take(40) },
                            label = stringResource(R.string.custom_type_name),
                        )
                        Text(
                            text = stringResource(R.string.icon),
                            style = MaterialTheme.typography.labelMedium,
                            color = MpColors.OnSurfaceVariant,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            customCredentialIcons.forEach { iconKey ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (customTypeIcon == iconKey) MpColors.PrimaryFixed else MpColors.SurfaceLow,
                                        )
                                        .border(
                                            1.dp,
                                            if (customTypeIcon == iconKey) MpColors.Primary else MpColors.OutlineVariant,
                                            RoundedCornerShape(12.dp),
                                        )
                                        .clickable { customTypeIcon = iconKey },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = credentialIconVector(iconKey),
                                        contentDescription = null,
                                        tint = MpColors.Primary,
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val label = customTypeName.trim()
                            if (label.isNotBlank()) {
                                category = "custom"
                                categoryLabel = label
                                categoryIcon = customTypeIcon
                                showCustomTypeDialog = false
                            }
                        },
                        enabled = customTypeName.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.use_this_type))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomTypeDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

private fun displayName(context: Context, uri: Uri): String {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: uri.lastPathSegment ?: "adjunto"
}

private const val MAX_CREDENTIAL_IMAGE_BYTES = 8 * 1024 * 1024
private const val MAX_CREDENTIAL_BANNER_DATA_URL = 500_000

private fun encodeCredentialLogo(context: Context, uri: Uri): String? {
    val bytes = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull() ?: return null
    if (bytes.size > 3 * 1024 * 1024) {
        return null
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    var sampleSize = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= 256) {
        sampleSize *= 2
    }

    val decoded = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: return null

    return try {
        val scale = min(1f, 256f / max(decoded.width, decoded.height))
        val width = max(1, (decoded.width * scale).toInt())
        val height = max(1, (decoded.height * scale).toInt())
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(decoded, width, height, true) else decoded
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, output)
        if (scaled !== decoded) {
            scaled.recycle()
        }
        "data:image/png;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    } finally {
        decoded.recycle()
    }
}

private fun encodeCredentialBanner(context: Context, uri: Uri): String? {
    val bytes = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull() ?: return null
    if (bytes.size > MAX_CREDENTIAL_IMAGE_BYTES) {
        return null
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    var sampleSize = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= 1200) {
        sampleSize *= 2
    }

    val decoded = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: return null

    return try {
        for ((edge, quality) in listOf(1200 to 82, 1000 to 72, 800 to 62)) {
            val scale = min(1f, edge.toFloat() / max(decoded.width, decoded.height))
            val width = max(1, (decoded.width * scale).toInt())
            val height = max(1, (decoded.height * scale).toInt())
            val scaled = if (scale < 1f) Bitmap.createScaledBitmap(decoded, width, height, true) else decoded
            val output = ByteArrayOutputStream()
            val compressed = scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, output)
            if (scaled !== decoded) {
                scaled.recycle()
            }
            if (!compressed) {
                continue
            }
            val dataUrl = "data:image/webp;base64," +
                Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            if (dataUrl.length <= MAX_CREDENTIAL_BANNER_DATA_URL) {
                return dataUrl
            }
        }
        null
    } finally {
        decoded.recycle()
    }
}
