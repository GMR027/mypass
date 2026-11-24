package app.mypass

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.OffsetDateTime

enum class NotificationKind {
    Update,
    Expired,
    Expiring,
    Activity,
}

data class NotificationEntry(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val detail: String,
    val read: Boolean,
    val itemId: String? = null,
    val itemType: String? = null,
    val activityId: String? = null,
    val activityType: String? = null,
    val versionCode: Int? = null,
    val createdAt: String? = null,
)

@Composable
fun notificationEntries(state: UiState): List<NotificationEntry> {
    val context = LocalContext.current
    val today = LocalDate.now()
    val entries = mutableListOf<NotificationEntry>()

    state.updateState.release
        ?.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
        ?.let { release ->
            val acknowledged = state.dismissedUpdateCode == release.versionCode ||
                state.updateState is UpdateState.Downloading ||
                state.updateState is UpdateState.Ready ||
                state.updateState is UpdateState.NeedsPermission
            val detail = when {
                release.isMandatory -> context.getString(R.string.updates_mandatory)
                state.updateState is UpdateState.Ready ||
                    state.updateState is UpdateState.NeedsPermission -> context.getString(R.string.notification_update_ready)
                state.updateState is UpdateState.Downloading -> {
                    val downloading = state.updateState as UpdateState.Downloading
                    context.getString(R.string.notification_update_downloading, downloading.progress)
                }
                release.notesFor(context.resources.configuration.locales[0].language).isNotEmpty() ->
                    release.notesFor(context.resources.configuration.locales[0].language).first()
                else -> context.getString(R.string.notification_update_body)
            }
            entries += NotificationEntry(
                id = "update:${release.versionCode}",
                kind = NotificationKind.Update,
                title = context.getString(R.string.notification_update_title, release.versionName),
                detail = detail,
                read = acknowledged && !release.isMandatory,
                versionCode = release.versionCode,
            )
        }

    state.items.forEach { item ->
        val days = expiryDays(item.expiresAt, today) ?: return@forEach
        if (days > 7) {
            return@forEach
        }

        val id = expiryNotificationId(item.id, item.expiresAt ?: return@forEach)
        val detail = when {
            days < -1 -> context.getString(R.string.notification_expired_days_ago, -days)
            days == -1L -> context.getString(R.string.notification_expired_yesterday)
            days == 0L -> context.getString(R.string.notification_expires_today)
            days == 1L -> context.getString(R.string.notification_expires_tomorrow)
            else -> context.getString(R.string.notification_expires_in_days, days)
        }
        entries += NotificationEntry(
            id = id,
            kind = if (days < 0) NotificationKind.Expired else NotificationKind.Expiring,
            title = item.title.ifBlank { context.getString(R.string.document) },
            detail = detail,
            read = id in state.expiryReadIds,
            itemId = item.id,
            itemType = item.type,
            createdAt = item.expiresAt,
        )
    }

    state.notifications.forEach { notification ->
        entries += NotificationEntry(
            id = "activity:${notification.id}",
            kind = NotificationKind.Activity,
            title = activityMessage(context, notification),
            detail = relativeDate(notification.createdAt).orEmpty(),
            read = notification.read,
            activityId = notification.id,
            activityType = notification.type,
            createdAt = notification.createdAt,
        )
    }

    return entries.sortedWith(
        compareBy<NotificationEntry> { notificationPriority(it.kind) }
            .thenBy { it.read }
            .thenByDescending { it.createdAt.orEmpty() },
    )
}

private fun notificationPriority(kind: NotificationKind): Int {
    return when (kind) {
        NotificationKind.Update -> 0
        NotificationKind.Expired -> 1
        NotificationKind.Expiring -> 2
        NotificationKind.Activity -> 3
    }
}

private fun activityMessage(context: android.content.Context, notification: UiNotification): String {
    val name = notification.from.ifBlank { context.getString(R.string.someone) }
    val document = notification.documentType.ifBlank { context.getString(R.string.document) }

    return when (notification.type) {
        "contact_request" -> context.getString(R.string.notif_contact_request, name)
        "contact_accepted" -> context.getString(R.string.notif_contact_accepted, name)
        "share_received" -> context.getString(R.string.notif_share_received, name)
        "share_accepted" -> context.getString(R.string.notif_share_accepted, name)
        "document_request" -> context.getString(R.string.notif_document_request, name, document)
        "vault_recovery_enabled" -> context.getString(R.string.notif_recovery_enabled)
        "vault_recovery_disabled" -> context.getString(R.string.notif_recovery_disabled)
        "vault_recovery_used" -> context.getString(R.string.notif_recovery_used)
        else -> context.getString(R.string.notif_default)
    }
}

private fun relativeDate(value: String?): String? {
    if (value.isNullOrBlank()) {
        return null
    }

    val millis = runCatching {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    }.getOrNull() ?: return null

    return DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}

@Composable
fun NotificationEntryRow(
    entry: NotificationEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NotificationIcon(kind = entry.kind)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (entry.read) MpColors.OnSurfaceVariant else MpColors.OnSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.detail.isNotBlank()) {
                Text(
                    text = entry.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry.kind == NotificationKind.Expired) {
                        MpColors.Error
                    } else {
                        MpColors.OnSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!entry.read) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .size(8.dp)
                    .background(MpColors.Primary, CircleShape),
            )
        }
    }
}

@Composable
private fun NotificationIcon(kind: NotificationKind) {
    val icon: ImageVector = when (kind) {
        NotificationKind.Update -> Icons.Filled.Refresh
        NotificationKind.Expired -> Icons.Filled.Warning
        NotificationKind.Expiring -> Icons.Filled.DateRange
        NotificationKind.Activity -> Icons.Filled.Person
    }
    val background = when (kind) {
        NotificationKind.Update -> MpColors.PrimaryFixed
        NotificationKind.Expired -> MpColors.ErrorContainer
        NotificationKind.Expiring -> MpColors.SecondaryFixed
        NotificationKind.Activity -> MpColors.SurfaceHighest
    }
    val tint = when (kind) {
        NotificationKind.Update -> MpColors.Primary
        NotificationKind.Expired -> MpColors.OnErrorContainer
        NotificationKind.Expiring -> MpColors.OnSecondaryFixed
        NotificationKind.Activity -> MpColors.OnSurfaceVariant
    }

    MpIconTile(
        icon = icon,
        size = 40,
        background = background,
        tint = tint,
    )
}

@Composable
fun NotificationsCard(
    entries: List<NotificationEntry>,
    onOpenEntry: (NotificationEntry) -> Unit,
    onViewAll: () -> Unit,
    onReadAll: () -> Unit,
) {
    if (entries.isEmpty()) {
        return
    }

    val unread = entries.count { !it.read }

    MpCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.notifications_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (unread > 0) {
                    MpBadge(text = unread.toString(), tone = MpBadgeTone.Primary)
                }
            }

            entries.take(3).forEach { entry ->
                NotificationEntryRow(entry = entry, onClick = { onOpenEntry(entry) })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MpTextButton(
                    text = stringResource(R.string.notifications_view_all),
                    onClick = onViewAll,
                )
                if (unread > 0) {
                    MpTextButton(
                        text = stringResource(R.string.mark_all_read),
                        onClick = onReadAll,
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(state: UiState, viewModel: MainViewModel) {
    val entries = notificationEntries(state)
    val expiryEntries = entries.filter {
        it.kind == NotificationKind.Expired || it.kind == NotificationKind.Expiring
    }
    val activityEntries = entries.filter { it.kind == NotificationKind.Activity }
    val hasUpdate = state.updateState.release != null
    val unread = entries.count { !it.read }

    LaunchedEffect(Unit) {
        viewModel.onNotificationsOpened()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(R.string.notifications_title),
            onBack = viewModel::closeNotifications,
            action = {
                MpIconButton(
                    icon = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.notifications_refresh),
                    onClick = viewModel::refreshNotificationsAndUpdates,
                )
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (hasUpdate) {
                item {
                    Text(
                        text = stringResource(R.string.notifications_section_update),
                        style = MaterialTheme.typography.labelLarge,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
                item {
                    UpdateNotificationCard(state = state, viewModel = viewModel)
                }
            }

            if (expiryEntries.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.notifications_section_expirations),
                        style = MaterialTheme.typography.labelLarge,
                        color = MpColors.OnSurfaceVariant,
                        modifier = Modifier.padding(top = if (hasUpdate) 8.dp else 0.dp),
                    )
                }
                items(expiryEntries, key = { it.id }) { entry ->
                    MpCard(modifier = Modifier.fillMaxWidth()) {
                        NotificationEntryRow(
                            entry = entry,
                            onClick = { viewModel.openNotificationEntry(entry) },
                        )
                    }
                }
            }

            if (activityEntries.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.notifications_section_activity),
                        style = MaterialTheme.typography.labelLarge,
                        color = MpColors.OnSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(activityEntries, key = { it.id }) { entry ->
                    MpCard(modifier = Modifier.fillMaxWidth()) {
                        NotificationEntryRow(
                            entry = entry,
                            onClick = { viewModel.openNotificationEntry(entry) },
                        )
                    }
                }
            }

            if (!hasUpdate && entries.isEmpty()) {
                item {
                    MpEmptyState(
                        icon = Icons.Filled.Notifications,
                        title = stringResource(R.string.notifications_empty_title),
                        subtitle = stringResource(R.string.notifications_empty_body),
                    )
                }
            }

            if (unread > 0) {
                item {
                    MpTextButton(
                        text = stringResource(R.string.mark_all_read),
                        onClick = viewModel::markAllNotificationsRead,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
