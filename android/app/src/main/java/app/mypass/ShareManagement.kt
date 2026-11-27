package app.mypass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val activeShareStatuses = setOf("pending", "accepted")

fun UiState.activeSharesFor(itemId: String): List<UiShare> {
    return outgoingShares
        .filter { it.itemId == itemId && it.status in activeShareStatuses }
        .sortedByDescending { it.createdAt.orEmpty() }
}

fun UiState.hasActiveShares(itemId: String): Boolean = activeSharesFor(itemId).isNotEmpty()

fun UiState.activeShareCount(itemId: String): Int = activeSharesFor(itemId).size

@Composable
fun ManageSharesDialog(
    itemId: String,
    itemTitle: String,
    shares: List<UiShare>,
    canShare: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onRevoke: (String) -> Unit,
) {
    val itemShares = shares
        .filter { it.itemId == itemId }
        .sortedWith(compareBy<UiShare> { it.status !in activeShareStatuses }.thenByDescending { it.createdAt.orEmpty() })
    val activeCount = itemShares.count { it.status in activeShareStatuses }

    MpDialog(
        onDismissRequest = onDismiss,
        header = {
            MpDialogHeader(
                icon = Icons.Filled.Send,
                title = stringResource(R.string.manage_shares_title),
                subtitle = itemTitle,
                onClose = onDismiss,
            )
        },
        actions = {
            if (canShare) {
                MpButton(
                    text = stringResource(R.string.share_with_another),
                    onClick = onShare,
                    icon = Icons.Filled.Send,
                    fillWidth = true,
                )
            }
            MpButton(
                text = stringResource(R.string.close),
                onClick = onDismiss,
                variant = MpButtonVariant.Secondary,
                fillWidth = true,
            )
        },
    ) {
        if (itemShares.isEmpty()) {
            MpCallout(
                text = stringResource(R.string.item_not_shared),
                tone = MpCalloutTone.Info,
            )
        } else {
            Text(
                text = if (activeCount > 0) {
                    stringResource(R.string.item_shared_count, activeCount)
                } else {
                    stringResource(R.string.share_history)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MpColors.OnSurfaceVariant,
            )

            itemShares.forEach { share ->
                ShareRecordCard(
                    share = share,
                    onRevoke = { onRevoke(share.id) },
                )
            }
        }
    }
}

@Composable
private fun ShareRecordCard(share: UiShare, onRevoke: () -> Unit) {
    val statusLabel = shareStatusLabel(share.status)
    val tone = when (share.status) {
        "accepted" -> MpBadgeTone.Primary
        "pending" -> MpBadgeTone.Warning
        "declined", "expired" -> MpBadgeTone.Danger
        else -> MpBadgeTone.Neutral
    }

    MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MpIconTile(
                    icon = if (share.status == "accepted") Icons.Filled.CheckCircle else Icons.Filled.Send,
                    size = 40,
                    background = if (share.status == "accepted") MpColors.PrimaryFixed else MpColors.SurfaceHigh,
                    tint = if (share.status == "accepted") MpColors.Primary else MpColors.OnSurfaceVariant,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = share.sender.ifBlank { stringResource(R.string.share_recipient_unknown) },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MpBadge(text = statusLabel, tone = tone)
                        MpBadge(
                            text = stringResource(if (share.mode == "transfer") R.string.transfer_label else R.string.copy_label),
                            tone = if (share.mode == "transfer") MpBadgeTone.Warning else MpBadgeTone.Neutral,
                        )
                    }
                }
            }

            share.createdAt?.take(10)?.takeIf { it.isNotBlank() }?.let { date ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MpColors.OnSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.share_date, date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
            }

            when (share.status) {
                "pending" -> {
                    MpButton(
                        text = stringResource(R.string.cancel_send),
                        onClick = onRevoke,
                        variant = MpButtonVariant.Danger,
                        compact = true,
                        maxLines = 1,
                        fillWidth = true,
                    )
                }

                "accepted" -> Text(
                    text = stringResource(R.string.share_accepted_no_revoke),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ShareStatusLabel(status: String): String = shareStatusLabel(status)

@Composable
private fun shareStatusLabel(status: String): String {
    return when (status) {
        "accepted" -> stringResource(R.string.share_status_accepted)
        "declined" -> stringResource(R.string.share_status_declined)
        "revoked" -> stringResource(R.string.share_status_revoked)
        "expired" -> stringResource(R.string.share_status_expired)
        else -> stringResource(R.string.share_status_pending)
    }
}
