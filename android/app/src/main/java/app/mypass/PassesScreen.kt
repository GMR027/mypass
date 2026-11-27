package app.mypass

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.util.Base64
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    val passes = sortActivePassItems(
        state.items.filter { it.pass != null && !isExpired(it) && it.state != "transferred" },
    )
    val nextPass = passes.firstOrNull()
    val otherPasses = passes.drop(1).take(5)
    val expiring = state.items.count { item ->
        if (item.pass != null) {
            return@count false
        }
        val date = item.expiresAt?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        date != null && !date.isBefore(LocalDate.now()) &&
            !date.isAfter(LocalDate.now().plusDays(30))
    }
    val firstName = state.name.substringBefore(" ").ifBlank {
        state.email.substringBefore("@")
    }.ifBlank { stringResource(R.string.app_name) }
    val notificationItems = notificationEntries(state)
    var presentingItem by remember { mutableStateOf<UiItem?>(null) }
    val todayLabel = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE d MMM", Locale.getDefault()))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    presentingItem?.let { item ->
        PassPresentationDialog(item = item, onDismiss = { presentingItem = null })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MpTopBar(
            state = state,
            onLock = viewModel::lock,
            onSettings = viewModel::openSettings,
            onLogout = viewModel::logout,
            onNotifications = viewModel::openNotifications,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = todayLabel.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MpColors.AccentDeep,
                        letterSpacing = 1.1.sp,
                    )
                    Text(
                        text = stringResource(R.string.home_greeting, firstName),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = stringResource(R.string.home_wallet_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
            }

            item { MpStatusBanner(state.status, state.error) }

            item {
                MpCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openSearch() },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MpIconTile(icon = Icons.Filled.Search, size = 44)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.search_vault),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.search_vault_placeholder),
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.OnSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (nextPass != null) {
                item {
                    NextPassHero(
                        item = nextPass,
                        passCount = passes.size,
                        shared = state.isItemShared(nextPass.id),
                        onPresent = { presentingItem = nextPass },
                        onOpenPasses = viewModel::openPasses,
                    )
                }

                if (otherPasses.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.home_my_passes),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = viewModel::openPasses) {
                                Text(stringResource(R.string.home_view_all))
                            }
                        }
                    }
                    items(otherPasses, key = { "home-${it.id}" }) { item ->
                        PassCompactCard(item = item, shared = state.isItemShared(item.id), onClick = { viewModel.openPasses() })
                    }
                }
            } else {
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
            }

            item {
                QuickActionDock(
                    onVault = viewModel::backToVault,
                    onPasses = viewModel::openPasses,
                    onScan = { viewModel.openScanner(true) },
                    onFamily = { viewModel.openFamily() },
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeVaultShortcut(
                        title = stringResource(R.string.credentials),
                        subtitle = stringResource(R.string.credentials_shortcut),
                        icon = Icons.Filled.Lock,
                        onClick = viewModel::openCredentials,
                    )
                    HomeVaultShortcut(
                        title = stringResource(R.string.medical),
                        subtitle = stringResource(R.string.medical_shortcut),
                        icon = Icons.Filled.Info,
                        onClick = viewModel::openMedical,
                    )
                }
            }

            item { VaultStatusCard(state) }

            if (expiring > 0) {
                item { ExpiryBanner(count = expiring) }
            }

            if (notificationItems.isNotEmpty()) {
                item {
                    NotificationsCard(
                        entries = notificationItems,
                        onOpenEntry = viewModel::openNotificationEntry,
                        onViewAll = viewModel::openNotifications,
                        onReadAll = viewModel::markAllNotificationsRead,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeVaultShortcut(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MpCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MpIconTile(icon = icon, size = 48)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NextPassHero(
    item: UiItem,
    passCount: Int,
    shared: Boolean,
    onPresent: () -> Unit,
    onOpenPasses: () -> Unit,
) {
    val pass = item.pass ?: return
    val background = Modifier.passCardBackground(pass)
    val dateLabel = passDateLabel(item)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 12.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(background),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.55f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.96f, size.height * 0.02f),
                )
            }
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MpColors.Accent),
                            )
                            Text(
                                text = stringResource(R.string.home_next_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f)) {
                        Text(
                            text = categoryLabel(pass.category),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.86f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (shared) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = stringResource(R.string.shared),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                    Text(
                        text = if (passCount == 1) stringResource(R.string.home_pass_count_one, passCount) else stringResource(R.string.home_pass_count, passCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.58f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = pass.issuer.ifBlank { categoryLabel(pass.category) },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.66f),
                    )
                    Text(
                        text = pass.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pass.holder.ifBlank { item.title }.takeIf { it != pass.title }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (dateLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = onPresent,
                        shape = CircleShape,
                        color = Color.White,
                        contentColor = MpColors.Primary,
                        shadowElevation = 2.dp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_qr_code),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.home_present_now),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Surface(
                        onClick = onOpenPasses,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                        contentColor = Color.White,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = stringResource(R.string.home_view_all),
                            modifier = Modifier.padding(14.dp).size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionDock(
    onVault: () -> Unit,
    onPasses: () -> Unit,
    onScan: () -> Unit,
    onFamily: () -> Unit,
) {
    MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.home_quick_actions),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionItem(
                    title = stringResource(R.string.vault),
                    icon = Icons.Filled.Lock,
                    modifier = Modifier.weight(1f),
                    onClick = onVault,
                )
                QuickActionItem(
                    title = stringResource(R.string.passes),
                    icon = Icons.Filled.DateRange,
                    modifier = Modifier.weight(1f),
                    onClick = onPasses,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionItem(
                    title = stringResource(R.string.scan),
                    icon = Icons.Filled.Add,
                    modifier = Modifier.weight(1f),
                    onClick = onScan,
                    accent = true,
                )
                QuickActionItem(
                    title = stringResource(R.string.family),
                    icon = Icons.Filled.Person,
                    modifier = Modifier.weight(1f),
                    onClick = onFamily,
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (accent) MpColors.AccentSoft else MpColors.SurfaceLow,
        border = BorderStroke(1.dp, if (accent) MpColors.Accent.copy(alpha = 0.35f) else MpColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (accent) MpColors.Accent else MpColors.PrimaryFixed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (accent) Color.White else MpColors.Primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (accent) MpColors.AccentDeep else MpColors.OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PassesScreen(state: UiState, viewModel: MainViewModel) {
    var filter by remember { mutableStateOf("all") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var presentingItem by remember { mutableStateOf<UiItem?>(null) }
    var shareItem by remember { mutableStateOf<UiItem?>(null) }
    var deleteItem by remember { mutableStateOf<UiItem?>(null) }
    var manageSharesItem by remember { mutableStateOf<UiItem?>(null) }
    var showNewPassDialog by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var revealRequest by remember { mutableStateOf(0) }
    val revealSelectedPass: () -> Unit = { revealRequest += 1 }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.createPassFromUri(uri)
        }
    }

    val allPasses = state.items.filter { it.pass != null }
    val active = sortActivePassItems(allPasses.filterNot { isExpired(it) || it.state == "transferred" })
    val archived = allPasses.filter { isExpired(it) || it.state == "transferred" }
    val filtered = when (filter) {
        "all" -> active
        "shared" -> active.filter { state.isItemShared(it.id) }
        else -> active.filter { it.pass?.category == filter }
    }
    val selected = filtered.firstOrNull { it.id == selectedId }
        ?: filtered.firstOrNull()
        ?: active.firstOrNull()
    val acceptedContacts = state.contacts.filter { it.status == "accepted" }
    val sharedCount = active.count { state.isItemShared(it.id) }
    val selectedDetailItemIndex = 3

    LaunchedEffect(sharedCount) {
        if (filter == "shared" && sharedCount == 0) {
            filter = "all"
        }
    }

    LaunchedEffect(state.focusItemId) {
        state.focusItemId?.let { id ->
            filter = "all"
            selectedId = id
            viewModel.clearFocusItem()
            revealSelectedPass()
        }
    }

    LaunchedEffect(revealRequest) {
        if (revealRequest == 0) {
            return@LaunchedEffect
        }
        val current = selected ?: return@LaunchedEffect
        if (filter != "all" && current.pass?.category != filter) {
            return@LaunchedEffect
        }
        withFrameNanos { }
        withFrameNanos { }
        listState.animateScrollToItem(selectedDetailItemIndex)
    }

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

    manageSharesItem?.let { item ->
        ManageSharesDialog(
            itemId = item.id,
            itemTitle = item.title,
            shares = state.outgoingShares,
            canShare = acceptedContacts.isNotEmpty(),
            onDismiss = { manageSharesItem = null },
            onShare = {
                shareItem = item
                manageSharesItem = null
            },
            onRevoke = viewModel::revokeShare,
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

    if (showNewPassDialog) {
        NewPassDialog(
            onDismiss = { showNewPassDialog = false },
            onSelect = { category ->
                showNewPassDialog = false
                viewModel.newPass(category)
            },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    Box {
                        MpIconButton(
                            icon = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_pass),
                            onClick = { showAddMenu = true },
                            container = MpColors.Primary,
                            tint = MpColors.OnPrimary,
                        )
                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.create_pass_from_scratch)) },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                onClick = {
                                    showAddMenu = false
                                    showNewPassDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.scan)) },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                onClick = {
                                    showAddMenu = false
                                    viewModel.openScanner(true)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.import_short)) },
                                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                                onClick = {
                                    showAddMenu = false
                                    importLauncher.launch(arrayOf("application/pdf", "image/*"))
                                },
                            )
                        }
                    }
                }
            }

            item { MpStatusBanner(state.status, state.error) }

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
                    val filterLabel = when (filter) {
                        "shared" -> stringResource(R.string.shared_filter, sharedCount)
                        "flight" -> stringResource(R.string.flight)
                        "event" -> stringResource(R.string.event)
                        "transit" -> stringResource(R.string.transit)
                        "id" -> stringResource(R.string.identity)
                        "membership" -> stringResource(R.string.membership)
                        else -> stringResource(R.string.all)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.pass_count_visible, filtered.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Box {
                            MpButton(
                                text = filterLabel,
                                onClick = { showFilterMenu = true },
                                variant = MpButtonVariant.Secondary,
                                compact = true,
                                maxLines = 1,
                            )
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                            ) {
                                val options = buildList {
                                    add("all" to stringResource(R.string.all))
                                    if (sharedCount > 0) {
                                        add("shared" to stringResource(R.string.shared_filter, sharedCount))
                                    }
                                    add("flight" to stringResource(R.string.flight))
                                    add("event" to stringResource(R.string.event))
                                    add("transit" to stringResource(R.string.transit))
                                    add("id" to stringResource(R.string.identity))
                                    add("membership" to stringResource(R.string.membership))
                                }
                                options.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            filter = value
                                            showFilterMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                if (selected != null && (
                        filter == "all" ||
                            (filter == "shared" && state.isItemShared(selected.id)) ||
                            selected.pass?.category == filter
                        )
                ) {
                    item {
                        PassDetailCard(
                            item = selected,
                            isNext = selected.id == active.firstOrNull()?.id,
                            hasContacts = acceptedContacts.isNotEmpty(),
                            shared = state.isItemShared(selected.id),
                            onPresent = { presentingItem = selected },
                            onShare = { shareItem = selected },
                            onManageShares = { manageSharesItem = selected },
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
                    item {
                        MpCascadeStack {
                            others.forEach { item ->
                                PassCompactCard(
                                    item = item,
                                    headerOnly = true,
                                    shared = state.isItemShared(item.id),
                                    onClick = {
                                        selectedId = item.id
                                        viewModel.recordRecentItem(item.id)
                                        revealSelectedPass()
                                    },
                                )
                            }
                        }
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
                            shared = state.isItemShared(item.id),
                            onClick = {
                                selectedId = item.id
                                viewModel.recordRecentItem(item.id)
                                if (filter == "all" || item.pass?.category == filter) {
                                    revealSelectedPass()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewPassDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val categories = listOf(
        Triple("flight", R.string.flight, Icons.Filled.Send),
        Triple("event", R.string.event, Icons.Filled.DateRange),
        Triple("transit", R.string.transit, Icons.Filled.Place),
        Triple("id", R.string.identity, Icons.Filled.Person),
        Triple("membership", R.string.membership, Icons.Filled.Star),
    )

    MpDialog(
        onDismissRequest = onDismiss,
        header = {
            MpDialogHeader(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.create_pass_from_scratch),
                subtitle = stringResource(R.string.choose_pass_type),
                onClose = onDismiss,
            )
        },
        actions = {
            MpDialogActions(
                primaryLabel = stringResource(R.string.cancel),
                onPrimary = onDismiss,
                primaryVariant = MpButtonVariant.Secondary,
            )
        },
    ) {
        categories.forEach { (category, label, icon) ->
            MpOptionCard(
                title = stringResource(label),
                onClick = { onSelect(category) },
                leading = {
                    MpIconTile(
                        icon = icon,
                        size = 44,
                    )
                },
                trailing = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MpColors.Primary,
                        modifier = Modifier.size(22.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun PassDetailCard(
    item: UiItem,
    isNext: Boolean,
    hasContacts: Boolean,
    shared: Boolean,
    onPresent: () -> Unit,
    onShare: () -> Unit,
    onManageShares: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val pass = item.pass ?: return
    val payload = remember(item.id, pass.codePayload) { passPayload(item, pass) }
    val bitmap = remember(payload, pass.codeFormat) {
        Barcode.generate(payload, pass.codeFormat, 720)
    }
    val background = Modifier.passCardBackground(pass)

    MpFlipCard(
        itemKey = item.id,
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.Transparent,
        border = null,
        shadowElevation = 10.dp,
        front = { onFlip ->
            PassDetailFront(
                item = item,
                pass = pass,
                isNext = isNext,
                shared = shared,
                background = background,
                onFlip = onFlip,
                onManageShares = onManageShares,
            )
        },
        back = { onFlipBack ->
            PassDetailBack(
                item = item,
                pass = pass,
                bitmap = bitmap,
                background = background,
                hasContacts = hasContacts,
                shared = shared,
                onFlip = onFlipBack,
                onPresent = onPresent,
                onShare = onShare,
                onManageShares = onManageShares,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        },
    )
}

@Composable
private fun PassDetailFront(
    item: UiItem,
    pass: UiPass,
    isNext: Boolean,
    shared: Boolean,
    background: Modifier,
    onFlip: () -> Unit,
    onManageShares: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(background)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PassCardHeader(
            pass = pass,
            badge = if (isNext) {
                "${categoryLabel(pass.category)} · ${stringResource(R.string.home_next_badge)}"
            } else {
                categoryLabel(pass.category)
            },
            shared = shared,
        )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.flip_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
            )
            if (shared) {
                MpIconButton(
                    icon = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.manage_shares),
                    onClick = onManageShares,
                    tint = Color.White,
                    container = Color.White.copy(alpha = 0.14f),
                    shape = CircleShape,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            MpFlipButton(
                text = stringResource(R.string.view_back),
                onClick = onFlip,
                dark = true,
            )
        }
    }
}

@Composable
private fun PassDetailBack(
    item: UiItem,
    pass: UiPass,
    bitmap: Bitmap?,
    background: Modifier,
    hasContacts: Boolean,
    shared: Boolean,
    onFlip: () -> Unit,
    onPresent: () -> Unit,
    onShare: () -> Unit,
    onManageShares: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val details = passFieldEntries(pass)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(background)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.back_of_pass),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.62f),
            letterSpacing = 1.sp,
        )
        PassCardHeader(
            pass = pass,
            badge = categoryLabel(pass.category),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.pass_code),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                MpIconTile(painter = painterResource(R.drawable.ic_qr_code), size = 64)
                Text(
                    text = stringResource(R.string.pass_code_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.ScanInk,
                )
            }
            Text(
                text = if (pass.codeSource == "decoded") {
                    stringResource(R.string.pass_read_code)
                } else {
                    stringResource(R.string.pass_generated_code)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MpColors.ScanInk,
            )
        }

        if (details.isNotEmpty()) {
            Text(
                text = stringResource(R.string.all_data),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
                letterSpacing = 1.sp,
            )
            details.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { (label, value) ->
                        PassBackDetail(
                            label = label,
                            value = value,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (pass.notes.isNotEmpty()) {
            Text(
                text = stringResource(R.string.notes),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
                letterSpacing = 1.sp,
            )
            Text(
                text = pass.notes.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f),
            )
        }

        PassDarkActionButton(
            text = stringResource(R.string.present_code),
            onClick = onPresent,
            enabled = pass.options["presentation"] != false,
            primary = true,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_qr_code),
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
            },
        )

        if (PassLocation.query(pass) != null) {
            PassDarkActionButton(
                text = stringResource(R.string.how_to_get_there),
                onClick = { PassLocation.open(context, pass) },
                leadingIcon = {
                    Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(19.dp))
                },
            )
        }

        if (PassCalendar.canAdd(pass)) {
            PassDarkActionButton(
                text = stringResource(R.string.add_to_calendar),
                onClick = {
                    if (!PassCalendar.add(context, item, pass)) {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.calendar_not_available),
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                leadingIcon = {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(19.dp))
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PassDarkActionButton(
                text = stringResource(R.string.share),
                onClick = onShare,
                enabled = hasContacts && pass.options["share"] != false,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
            PassDarkActionButton(
                text = stringResource(R.string.edit),
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
        }

        if (shared) {
            PassDarkActionButton(
                text = stringResource(R.string.manage_shares),
                onClick = onManageShares,
                leadingIcon = {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
        }

        if (!hasContacts) {
            Text(
                text = stringResource(R.string.share_needs_contact),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.62f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.id.take(8).uppercase(Locale.US),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            Text(
                text = stringResource(R.string.protected_by_mypass),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        TextButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = Color(0xFFFFB4AB),
                modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.delete), color = Color(0xFFFFB4AB))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            MpFlipButton(
                text = stringResource(R.string.back_to_front),
                onClick = onFlip,
                dark = true,
            )
        }
    }
}

@Composable
private fun PassBackDetail(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.Black.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PassDarkActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val foreground = if (primary) Color(0xFF0C2340) else Color.White
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.alpha(if (enabled) 1f else 0.42f),
        shape = RoundedCornerShape(12.dp),
        color = if (primary) Color.White else Color.White.copy(alpha = 0.12f),
        border = if (primary) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides foreground,
                ) {
                    leadingIcon()
                }
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun passFieldEntries(pass: UiPass): List<Pair<String, String>> {
    return pass.fields.entries
        .filter { (_, value) -> value.isNotBlank() }
        .map { (key, value) -> passFieldLabel(key) to formatPassFieldValue(key, value) }
}

@Composable
private fun passFieldLabel(key: String): String {
    val resource = PassFieldLabels[key]
    if (resource != null) {
        return stringResource(resource)
    }

    return key
        .replace('_', ' ')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

private fun formatPassFieldValue(key: String, value: String): String {
    if (key == "date" || key == "expires" || key == "birth") {
        return runCatching {
            LocalDate.parse(value.take(10))
                .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
        }.getOrDefault(value)
    }

    return value
}

private val PassFieldLabels: Map<String, Int> = mapOf(
    "origin" to R.string.origin,
    "destination" to R.string.destination,
    "flight" to R.string.flight,
    "date" to R.string.date,
    "departure" to R.string.departure,
    "boarding" to R.string.boarding_start,
    "boarding_start" to R.string.boarding_start,
    "boarding_end" to R.string.boarding_end,
    "gate" to R.string.gate,
    "terminal" to R.string.terminal,
    "seat" to R.string.seat,
    "zone" to R.string.zone,
    "pnr" to R.string.booking_code,
    "location" to R.string.location,
    "address" to R.string.address,
    "coordinates" to R.string.coordinates,
    "time" to R.string.time,
    "section" to R.string.section,
    "row" to R.string.row,
    "route" to R.string.route,
    "platform" to R.string.platform,
    "carriage" to R.string.carriage,
    "number" to R.string.number,
    "level" to R.string.level,
    "expires" to R.string.expires,
    "birth" to R.string.birth_date,
    "holder" to R.string.holder,
    "passenger" to R.string.passenger,
    "carrier" to R.string.carrier,
    "code_value" to R.string.code,
)

@Composable
internal fun PassCompactCard(
    item: UiItem,
    archived: Boolean = false,
    headerOnly: Boolean = false,
    shared: Boolean = false,
    onClick: () -> Unit,
) {
    val pass = item.pass ?: return
    val cardBackground = Modifier.passCardBackground(pass)
    val background = if (archived) {
        Modifier.background(Brush.linearGradient(listOf(MpColors.SurfaceHighest, MpColors.SurfaceHighest)))
    } else {
        cardBackground
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
                .then(background)
                .padding(horizontal = 16.dp, vertical = if (headerOnly) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (headerOnly) 4.dp else 10.dp),
        ) {
            PassCardHeader(
                pass = pass,
                badge = if (archived) {
                    if (item.state == "transferred") {
                        stringResource(R.string.transferred)
                    } else {
                        stringResource(R.string.expired_pass)
                    }
                } else {
                    categoryLabel(pass.category)
                },
                dark = !archived,
                shared = shared,
            )
            if (headerOnly) {
                val summary = passSummaryText(item, pass)
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                PassCardBody(pass = pass, compact = true, dark = !archived)
            }
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
        appearance = draft.appearance,
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
                    .passCardBackground(pass)
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
                    color = MpColors.ScanInk,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassCardHeader(pass: UiPass, badge: String, dark: Boolean = true, shared: Boolean = false) {
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
            FlowRow(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (dark) Color.White.copy(alpha = 0.16f) else MpColors.SurfaceHighest,
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (dark) Color.White else MpColors.OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (shared) {
                    MpBadge(
                        text = stringResource(R.string.shared),
                        tone = MpBadgeTone.Primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PassCardBody(pass: UiPass, compact: Boolean = false, dark: Boolean = true) {
    val fields = pass.fields
    val boardingStart = fields["boarding_start"] ?: fields["boarding"].orEmpty()
    val boardingEnd = fields["boarding_end"].orEmpty()
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
                    text = boardingStart,
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
                PassMeta(stringResource(R.string.terminal), fields["terminal"] ?: "—", dark)
                PassMeta(stringResource(R.string.seat), fields["seat"] ?: "—", dark)
                PassMeta(stringResource(R.string.zone), fields["zone"] ?: "—", dark)
            }
            if (boardingStart.isNotBlank() || boardingEnd.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PassMeta(stringResource(R.string.boarding_start), boardingStart.ifBlank { "—" }, dark)
                    PassMeta(stringResource(R.string.boarding_end), boardingEnd.ifBlank { "—" }, dark)
                }
            }
        }
    } else if (pass.category == "transit") {
        Text(
            text = listOf(fields["route"], fields["date"], fields["time"])
                .filterNot { it.isNullOrBlank() }
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = if (dark) Color.White.copy(alpha = 0.84f) else MpColors.OnSurfaceVariant,
        )
        if (!compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(Color.Black.copy(alpha = if (dark) 0.16f else 0.04f), RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PassMeta(stringResource(R.string.terminal), fields["terminal"] ?: "—", dark)
                PassMeta(stringResource(R.string.platform), fields["platform"] ?: "—", dark)
                PassMeta(stringResource(R.string.carriage), fields["carriage"] ?: "—", dark)
                PassMeta(stringResource(R.string.seat), fields["seat"] ?: "—", dark)
            }
            if (boardingStart.isNotBlank() || boardingEnd.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PassMeta(stringResource(R.string.boarding_start), boardingStart.ifBlank { "—" }, dark)
                    PassMeta(stringResource(R.string.boarding_end), boardingEnd.ifBlank { "—" }, dark)
                }
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

    MpFullScreenWindow(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.presentation),
                    style = MaterialTheme.typography.labelMedium,
                    color = MpColors.Primary,
                )
                Text(
                    text = pass.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(pass.issuer, pass.holder).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }
            MpIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.close),
                onClick = onDismiss,
                container = MpColors.SurfaceLow,
                tint = MpColors.OnSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MpColors.OutlineVariant.copy(alpha = 0.45f)),
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.pass_code),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MpIconTile(painter = painterResource(R.drawable.ic_qr_code), size = 80)
                        Text(
                            text = stringResource(R.string.pass_code_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MpColors.ScanInk,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.presentation_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MpColors.ScanInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            MpBadge(
                text = stringResource(R.string.brightness_max),
                tone = MpBadgeTone.Primary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        MpButton(
            text = stringResource(R.string.close_presentation),
            onClick = onDismiss,
            fillWidth = true,
        )
    }
}

internal fun isExpired(item: UiItem): Boolean {
    val date = item.expiresAt?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() } ?: return false
    return date.isBefore(LocalDate.now())
}

internal fun passRelevantDate(item: UiItem): LocalDate? {
    val fields = item.pass?.fields.orEmpty()
    for (value in listOf(fields["date"], fields["expires"], item.expiresAt)) {
        val date = value?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        if (date != null) {
            return date
        }
    }
    return null
}

internal fun sortActivePassItems(items: List<UiItem>): List<UiItem> {
    val today = LocalDate.now()
    return items.sortedWith(Comparator { a, b ->
        val dateA = passRelevantDate(a)
        val dateB = passRelevantDate(b)
        val upcomingA = dateA != null && !dateA.isBefore(today)
        val upcomingB = dateB != null && !dateB.isBefore(today)

        when {
            upcomingA && upcomingB -> dateA!!.compareTo(dateB!!)
            upcomingA != upcomingB -> if (upcomingA) -1 else 1
            dateA != null && dateB != null -> dateB.compareTo(dateA)
            else -> (b.updatedAt ?: "").compareTo(a.updatedAt ?: "")
        }
    })
}

private fun passDateLabel(item: UiItem): String? {
    val date = passRelevantDate(item) ?: return null
    val locale = Locale.getDefault()
    return date.format(DateTimeFormatter.ofPattern("d MMM", locale))
}

private fun passSummaryText(item: UiItem, pass: UiPass): String {
    val fields = pass.fields
    val date = passDateLabel(item).orEmpty()

    return when (pass.category) {
        "flight" -> listOfNotNull(
            listOfNotNull(fields["origin"], fields["destination"])
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" → "),
            date.takeIf { it.isNotBlank() },
            fields["gate"],
            fields["terminal"],
        ).joinToString(" · ")

        "event" -> listOfNotNull(
            fields["location"],
            date.takeIf { it.isNotBlank() },
            fields["gate"],
        ).joinToString(" · ")

        "transit" -> listOfNotNull(
            fields["route"],
            date.takeIf { it.isNotBlank() },
            fields["platform"],
        ).joinToString(" · ")

        else -> listOfNotNull(
            pass.holder.takeIf { it.isNotBlank() },
            fields["number"],
            fields["level"],
            date.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
    }
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

internal fun passGradient(pass: UiPass): Brush {
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

internal fun decodeDataUrl(value: String): Bitmap? {
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
