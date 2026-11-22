package app.mypass

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.util.Base64
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.Image
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(state: UiState, viewModel: MainViewModel) {
    val passes = state.items.filter { it.pass != null && !isExpired(it) }
    val expiring = state.items.count { item ->
        if (item.pass != null) {
            return@count false
        }
        val date = item.expiresAt?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        date != null && !date.isBefore(LocalDate.now()) &&
            !date.isAfter(LocalDate.now().plusDays(30))
    }
    val firstName = state.email.substringBefore("@").ifBlank { stringResource(R.string.app_name) }

    Column(modifier = Modifier.fillMaxSize()) {
        MpTopBar(state = state, onLock = viewModel::lock)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.home_greeting, firstName),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
            }

            item { MpStatusBanner(state.status, state.error) }
            item { VaultStatusCard(state) }

            item {
                Text(
                    text = stringResource(R.string.home_quick_actions),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction(
                            title = stringResource(R.string.vault),
                            subtitle = stringResource(R.string.home_open_vault),
                            icon = Icons.Filled.Lock,
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::backToVault,
                        )
                        QuickAction(
                            title = stringResource(R.string.passes),
                            subtitle = stringResource(R.string.home_open_passes),
                            icon = Icons.Filled.DateRange,
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::openPasses,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickAction(
                            title = stringResource(R.string.scan_pass),
                            subtitle = stringResource(R.string.home_scan_pass),
                            icon = Icons.Filled.Add,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.openScanner(true) },
                        )
                        QuickAction(
                            title = stringResource(R.string.family),
                            subtitle = stringResource(R.string.home_open_family),
                            icon = Icons.Filled.Person,
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::openFamily,
                        )
                    }
                }
            }

            if (expiring > 0) {
                item { ExpiryBanner(count = expiring) }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_recent_passes),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (passes.isNotEmpty()) {
                        TextButton(onClick = viewModel::openPasses) {
                            Text(stringResource(R.string.home_view_all))
                        }
                    }
                }
            }

            if (passes.isEmpty()) {
                item {
                    MpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            MpIconTile(icon = Icons.Filled.DateRange, size = 56)
                            Text(
                                text = stringResource(R.string.no_passes_title),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = stringResource(R.string.no_passes_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            MpButton(
                                text = stringResource(R.string.create_first_pass),
                                onClick = { viewModel.openScanner(true) },
                                icon = Icons.Filled.Add,
                                fillWidth = true,
                            )
                        }
                    }
                }
            } else {
                items(passes.take(3), key = { it.id }) { item ->
                    PassCompactCard(item = item, onClick = viewModel::openPasses)
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    MpCard(
        modifier = modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MpIconTile(icon = icon, size = 42)
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MpColors.OnSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PassesScreen(state: UiState, viewModel: MainViewModel) {
    var filter by remember { mutableStateOf("all") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var presentingItem by remember { mutableStateOf<UiItem?>(null) }
    var shareItem by remember { mutableStateOf<UiItem?>(null) }
    var deleteItem by remember { mutableStateOf<UiItem?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.createPassFromUri(uri)
        }
    }

    val allPasses = state.items.filter { it.pass != null }
    val active = allPasses.filterNot { isExpired(it) }
    val archived = allPasses.filter { isExpired(it) }
    val filtered = if (filter == "all") active else active.filter { it.pass?.category == filter }
    val selected = allPasses.firstOrNull { it.id == selectedId } ?: active.firstOrNull()
    val acceptedContacts = state.contacts.filter { it.status == "accepted" }

    LaunchedEffect(allPasses.size, filter, selectedId) {
        val current = allPasses.firstOrNull { it.id == selectedId }
        val matchesFilter = filter == "all" || current?.pass?.category == filter
        if (current == null || !matchesFilter) {
            selectedId = filtered.firstOrNull()?.id ?: active.firstOrNull()?.id
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

    if (deleteItem != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_pass_body),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteItem(deleteItem!!)
                deleteItem = null
            },
            onDismiss = { deleteItem = null },
        )
    }

    presentingItem?.let { item ->
        PassPresentationDialog(item = item, onDismiss = { presentingItem = null })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MpTopBar(state = state, onLock = viewModel::lock)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.passes_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(R.string.passes_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
            }

            item { MpStatusBanner(state.status, state.error) }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MpButton(
                        text = stringResource(R.string.scan_pass),
                        onClick = { viewModel.openScanner(true) },
                        icon = Icons.Filled.Add,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f),
                    )
                    MpButton(
                        text = stringResource(R.string.import_file),
                        onClick = {
                            importLauncher.launch(arrayOf("application/pdf", "image/*"))
                        },
                        variant = MpButtonVariant.Secondary,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (allPasses.isEmpty()) {
                item {
                    MpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            MpIconTile(painter = painterResource(R.drawable.ic_ticket), size = 64)
                            Text(
                                text = stringResource(R.string.no_passes_title),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = stringResource(R.string.no_passes_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MpColors.OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            MpButton(
                                text = stringResource(R.string.create_first_pass),
                                onClick = { viewModel.openScanner(true) },
                                icon = Icons.Filled.Add,
                                fillWidth = true,
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MpFilterChip(stringResource(R.string.all), filter == "all") { filter = "all" }
                        MpFilterChip(stringResource(R.string.flight), filter == "flight") { filter = "flight" }
                        MpFilterChip(stringResource(R.string.event), filter == "event") { filter = "event" }
                        MpFilterChip(stringResource(R.string.transit), filter == "transit") { filter = "transit" }
                        MpFilterChip(stringResource(R.string.identity), filter == "id") { filter = "id" }
                        MpFilterChip(stringResource(R.string.membership), filter == "membership") { filter = "membership" }
                    }
                }

                if (selected != null && (filter == "all" || selected.pass?.category == filter)) {
                    item {
                        PassDetailCard(
                            item = selected,
                            hasContacts = acceptedContacts.isNotEmpty(),
                            onPresent = { presentingItem = selected },
                            onShare = { shareItem = selected },
                            onEdit = { viewModel.editPass(selected) },
                            onDelete = { deleteItem = selected },
                        )
                    }
                }

                val others = filtered.filter { it.id != selected?.id }
                if (others.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.other_passes),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(others, key = { it.id }) { item ->
                        PassCompactCard(
                            item = item,
                            onClick = { selectedId = item.id },
                        )
                    }
                } else if (filtered.isEmpty()) {
                    item {
                        MpEmptyState(
                            icon = Icons.Filled.Info,
                            title = stringResource(R.string.no_passes_filter),
                        )
                    }
                }

                if (archived.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.archived_passes),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(archived, key = { "archive-${it.id}" }) { item ->
                        PassCompactCard(
                            item = item,
                            archived = true,
                            onClick = {
                                selectedId = item.id
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PassDetailCard(
    item: UiItem,
    hasContacts: Boolean,
    onPresent: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val pass = item.pass ?: return
    val payload = remember(item.id, pass.codePayload) { passPayload(item, pass) }
    val bitmap = remember(payload, pass.codeFormat) {
        Barcode.generate(payload, pass.codeFormat, 720)
    }
    val gradient = passGradient(pass)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = 10.dp,
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PassCardHeader(pass = pass, badge = categoryLabel(pass.category))
                PassCardBody(pass = pass)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.holderOrTitle(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.id.take(8).uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.62f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.pass_code),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    MpIconTile(painter = painterResource(R.drawable.ic_qr_code), size = 72)
                    Text(
                        text = stringResource(R.string.pass_code_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
                Text(
                    text = if (pass.codeSource == "decoded") {
                        stringResource(R.string.pass_read_code)
                    } else {
                        stringResource(R.string.pass_generated_code)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MpColors.Outline,
                )
                MpButton(
                    text = stringResource(R.string.present_code),
                    onClick = onPresent,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_qr_code),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    fillWidth = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MpButton(
                        text = stringResource(R.string.share),
                        onClick = onShare,
                        variant = MpButtonVariant.Quiet,
                        enabled = hasContacts && item.pass?.options?.get("share") != false,
                        modifier = Modifier.weight(1f),
                    )
                    MpButton(
                        text = stringResource(R.string.edit_pass),
                        onClick = onEdit,
                        variant = MpButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (!hasContacts) {
                    Text(
                        text = stringResource(R.string.share_needs_contact),
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MpColors.Error)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.delete), color = MpColors.Error)
                }
            }
        }
    }
}

@Composable
internal fun PassCompactCard(
    item: UiItem,
    archived: Boolean = false,
    onClick: () -> Unit,
) {
    val pass = item.pass ?: return
    val gradient = passGradient(pass).let { brush ->
        if (archived) {
            Brush.linearGradient(listOf(MpColors.SurfaceHighest, MpColors.SurfaceHighest))
        } else {
            brush
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        shadowElevation = if (archived) 0.dp else 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PassCardHeader(
                pass = pass,
                badge = if (archived) stringResource(R.string.expired_pass) else categoryLabel(pass.category),
                dark = !archived,
            )
            PassCardBody(pass = pass, compact = true, dark = !archived)
            if (archived) {
                Text(
                    text = item.expiresAt?.take(10) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun PassPreviewCard(draft: PassDraft) {
    val pass = UiPass(
        category = draft.category,
        title = draft.title.ifBlank { fallbackDraftTitle(draft) },
        issuer = draft.issuer,
        holder = draft.holder,
        fields = draft.fields,
        notes = draft.notes,
        codePayload = draft.codePayload.ifBlank { null },
        codeFormat = draft.codeFormat,
        codeSource = draft.codeSource,
        themeColor = draft.themeColor,
        options = emptyMap(),
        logo = draft.logo,
    )
    val payload = draft.codePayload.ifBlank {
        val fields = JSONObject()
        draft.fields.forEach { (key, value) ->
            if (value.isNotBlank()) {
                fields.put(key, value)
            }
        }
        JSONObject()
            .put("v", 1)
            .put("app", "mypass")
            .put("c", draft.category)
            .put("title", pass.title)
            .put("fields", fields)
            .toString()
    }
    val bitmap = remember(payload, draft.codeFormat) {
        Barcode.generate(payload, draft.codeFormat, 560)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp,
    ) {
        Column {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(passGradient(pass))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PassCardHeader(pass = pass, badge = categoryLabel(pass.category))
                PassCardBody(pass = pass)
                Text(
                    text = stringResource(R.string.preview),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    MpIconTile(painter = painterResource(R.drawable.ic_qr_code), size = 56)
                }
                Text(
                    text = codeFormatLabel(draft.codeFormat),
                    style = MaterialTheme.typography.labelSmall,
                    color = MpColors.Outline,
                )
            }
        }
    }
}

@Composable
private fun PassCardHeader(pass: UiPass, badge: String, dark: Boolean = true) {
    val logo = remember(pass.logo) { pass.logo?.let(::decodeDataUrl) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (logo != null) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = if (dark) 0.16f else 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = logo.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        } else {
            MpIconTile(
                painter = painterResource(passCategoryDrawable(pass.category)),
                size = 42,
                background = Color.White.copy(alpha = if (dark) 0.16f else 0.5f),
                tint = if (dark) Color.White else MpColors.Primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pass.issuer.ifBlank { categoryLabel(pass.category) },
                style = MaterialTheme.typography.labelSmall,
                color = if (dark) Color.White.copy(alpha = 0.72f) else MpColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pass.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (dark) Color.White else MpColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Surface(
            shape = CircleShape,
            color = if (dark) Color.White.copy(alpha = 0.16f) else MpColors.SurfaceHighest,
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = if (dark) Color.White else MpColors.OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun PassCardBody(pass: UiPass, compact: Boolean = false, dark: Boolean = true) {
    val fields = pass.fields
    if (pass.category == "flight") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fields["origin"] ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) Color.White else MpColors.OnSurface,
                )
                Text(
                    text = fields["departure"].orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dark) Color.White.copy(alpha = 0.7f) else MpColors.OnSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = fields["flight"].orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dark) Color.White.copy(alpha = 0.7f) else MpColors.OnSurfaceVariant,
                )
                Text(
                    text = "✈",
                    color = if (dark) Color.White else MpColors.Primary,
                    fontSize = 20.sp,
                )
                Text(
                    text = fields["date"].orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dark) Color.White.copy(alpha = 0.6f) else MpColors.OnSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    text = fields["destination"] ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) Color.White else MpColors.OnSurface,
                )
                Text(
                    text = fields["boarding"].orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dark) Color.White.copy(alpha = 0.7f) else MpColors.OnSurfaceVariant,
                )
            }
        }
        if (!compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = if (dark) 0.16f else 0.04f), RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PassMeta(stringResource(R.string.gate), fields["gate"] ?: "—", dark)
                PassMeta(stringResource(R.string.seat), fields["seat"] ?: "—", dark)
                PassMeta(stringResource(R.string.zone), fields["zone"] ?: "—", dark)
                PassMeta(stringResource(R.string.boarding), fields["boarding"] ?: "—", dark)
            }
        }
    } else if (pass.category == "event") {
        Text(
            text = listOf(fields["location"], fields["date"], fields["time"])
                .filterNot { it.isNullOrBlank() }
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = if (dark) Color.White.copy(alpha = 0.84f) else MpColors.OnSurfaceVariant,
        )
        if (!compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = if (dark) 0.16f else 0.04f), RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PassMeta(stringResource(R.string.section), fields["section"] ?: "—", dark)
                PassMeta(stringResource(R.string.row), fields["row"] ?: "—", dark)
                PassMeta(stringResource(R.string.seat), fields["seat"] ?: "—", dark)
            }
        }
    } else {
        Text(
            text = listOf(fields["route"], fields["location"], fields["number"], fields["level"])
                .filterNot { it.isNullOrBlank() }
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = if (dark) Color.White.copy(alpha = 0.84f) else MpColors.OnSurfaceVariant,
        )
        val expiry = fields["expires"] ?: pass.fields["date"]
        if (!expiry.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.valid_until_date, expiry),
                style = MaterialTheme.typography.labelSmall,
                color = if (dark) Color.White.copy(alpha = 0.66f) else MpColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PassMeta(label: String, value: String, dark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = if (dark) Color.White.copy(alpha = 0.6f) else MpColors.OnSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (dark) Color.White else MpColors.OnSurface,
        )
    }
}

@Composable
private fun PassPresentationDialog(item: UiItem, onDismiss: () -> Unit) {
    val pass = item.pass ?: return
    val payload = remember(item.id, pass.codePayload) { passPayload(item, pass) }
    val bitmap = remember(payload, pass.codeFormat) {
        Barcode.generate(payload, pass.codeFormat, 1000)
    }
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current

    DisposableEffect(Unit) {
        val activityWindow = activity?.window
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        val previousBrightness = dialogWindow?.attributes?.screenBrightness
            ?: activityWindow?.attributes?.screenBrightness
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        for (window in listOfNotNull(activityWindow, dialogWindow)) {
            val attributes = window.attributes
            attributes.screenBrightness = 1f
            window.attributes = attributes
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
            for (window in listOfNotNull(activityWindow, dialogWindow)) {
                val attributes = window.attributes
                attributes.screenBrightness = previousBrightness ?: -1f
                window.attributes = attributes
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.presentation),
                            style = MaterialTheme.typography.labelMedium,
                            color = MpColors.Primary,
                        )
                        Text(
                            text = pass.title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOf(pass.issuer, pass.holder).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.pass_code),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MpIconTile(painter = painterResource(R.drawable.ic_qr_code), size = 80)
                            Text(
                                text = stringResource(R.string.pass_code_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MpColors.OnSurfaceVariant,
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.presentation_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                MpButton(
                    text = stringResource(R.string.close_presentation),
                    onClick = onDismiss,
                    fillWidth = true,
                )
            }
        }
    }
}

internal fun isExpired(item: UiItem): Boolean {
    val date = item.expiresAt?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } ?: return false
    return date.isBefore(LocalDate.now())
}

private fun passPayload(item: UiItem, pass: UiPass): String {
    if (!pass.codePayload.isNullOrBlank()) {
        return pass.codePayload
    }
    val fields = JSONObject()
    pass.fields.forEach { (key, value) ->
        if (value.isNotBlank()) {
            fields.put(key, value)
        }
    }
    return JSONObject()
        .put("v", 1)
        .put("app", "mypass")
        .put("id", item.id)
        .put("c", pass.category)
        .put("title", pass.title)
        .put("issuer", pass.issuer)
        .put("holder", pass.holder)
        .put("fields", fields)
        .toString()
}

@Composable
private fun categoryLabel(category: String): String = when (category) {
    "flight" -> stringResource(R.string.flight)
    "event" -> stringResource(R.string.event)
    "transit" -> stringResource(R.string.transit)
    "id" -> stringResource(R.string.identity)
    else -> stringResource(R.string.membership)
}

private fun passCategoryDrawable(category: String): Int = when (category) {
    "flight", "event", "transit" -> R.drawable.ic_ticket
    "id", "membership" -> R.drawable.ic_card
    else -> R.drawable.ic_document
}

private fun passGradient(pass: UiPass): Brush {
    val base = runCatching { Color(AndroidColor.parseColor(pass.themeColor)) }
        .getOrElse {
            when (pass.category) {
                "event" -> Color(0xFF4C1D95)
                "transit" -> Color(0xFF064E3B)
                "id" -> Color(0xFF18181B)
                "membership" -> Color(0xFF9A3412)
                else -> Color(0xFF0C2340)
            }
        }
    val dark = Color(
        red = base.red * 0.55f,
        green = base.green * 0.55f,
        blue = base.blue * 0.55f,
        alpha = 1f,
    )
    return Brush.linearGradient(listOf(base, dark))
}

private fun decodeDataUrl(value: String): Bitmap? {
    return runCatching {
        val payload = value.substringAfter(',', value)
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun codeFormatLabel(format: String): String = when (format) {
    "pdf417" -> "PDF417"
    "aztec" -> "Aztec"
    "code128" -> "Code 128"
    else -> "QR"
}

@Composable
private fun fallbackDraftTitle(draft: PassDraft): String {
    return when (draft.category) {
        "flight" -> listOf(
            listOf(draft.fields["origin"], draft.fields["destination"])
                .filterNot { it.isNullOrBlank() }
                .joinToString(" → "),
            draft.fields["flight"].orEmpty(),
        ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank {
            stringResource(R.string.new_flight_pass)
        }
        "event" -> draft.fields["location"] ?: stringResource(R.string.new_event_pass)
        "transit" -> draft.fields["route"] ?: stringResource(R.string.new_transit_pass)
        "membership" -> draft.issuer.ifBlank { stringResource(R.string.new_membership) }
        else -> draft.issuer.ifBlank { stringResource(R.string.new_identity_pass) }
    }
}

private fun UiItem.holderOrTitle(): String {
    return pass?.holder?.takeIf { it.isNotBlank() }
        ?: pass?.fields?.get("holder")?.takeIf { it.isNotBlank() }
        ?: pass?.fields?.get("passenger")?.takeIf { it.isNotBlank() }
        ?: title
}
