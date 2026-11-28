package app.mypass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TrashScreen(state: UiState, viewModel: MainViewModel) {
    var deleteTarget by remember { mutableStateOf<UiItem?>(null) }

    deleteTarget?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.trash_delete_title),
            message = stringResource(R.string.trash_delete_body),
            confirmLabel = stringResource(R.string.trash_delete_forever),
            onConfirm = {
                viewModel.permanentlyDeleteItem(item)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(R.string.trash_title),
            onBack = viewModel::openSettings,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.trash_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MpColors.OnSurfaceVariant,
                    )
                    MpStatusBanner(state.status, state.error)
                }
            }

            if (state.trashItems.isEmpty()) {
                item {
                    MpEmptyState(
                        icon = Icons.Filled.Delete,
                        title = stringResource(R.string.trash_empty_title),
                        subtitle = stringResource(R.string.trash_empty_body),
                    )
                }
            } else {
                items(state.trashItems, key = { it.id }) { item ->
                    MpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                MpIconTile(
                                    icon = when {
                                        item.pass != null -> Icons.Filled.DateRange
                                        item.credential != null -> Icons.Filled.Lock
                                        item.medical != null -> Icons.Filled.Person
                                        item.insurance != null -> Icons.Filled.CheckCircle
                                        else -> Icons.Filled.Info
                                    },
                                    size = 46,
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = trashTypeLabel(item),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MpColors.OnSurfaceVariant,
                                    )
                                    Text(
                                        text = item.deletedAt?.let(::trashDateLabel).orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MpColors.OnSurfaceVariant,
                                    )
                                    if (item.pendingAction != null) {
                                        Text(
                                            text = stringResource(R.string.trash_waiting_sync),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MpColors.Secondary,
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                MpButton(
                                    text = stringResource(R.string.trash_restore),
                                    onClick = { viewModel.restoreItem(item) },
                                    variant = MpButtonVariant.Primary,
                                    modifier = Modifier.weight(1f),
                                )
                                MpButton(
                                    text = stringResource(R.string.trash_delete_forever),
                                    onClick = { deleteTarget = item },
                                    variant = MpButtonVariant.Danger,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun trashTypeLabel(item: UiItem): String = when {
    item.pass != null -> stringResource(R.string.trash_type_pass)
    item.credential != null -> stringResource(R.string.trash_type_credential)
    item.medical != null -> stringResource(R.string.trash_type_medical)
    item.insurance != null -> stringResource(R.string.trash_type_insurance)
    else -> stringResource(R.string.trash_type_document)
}

private fun trashDateLabel(value: String): String {
    val date = runCatching {
        Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDate()
    }.getOrNull() ?: return ""
    return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
}
