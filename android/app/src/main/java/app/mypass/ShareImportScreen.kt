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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ShareImportScreen(state: UiState, viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(R.string.share_import_title),
            onBack = viewModel::discardSharedFiles,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.share_import_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MpColors.OnSurfaceVariant,
                    )
                    MpStatusBanner(state.status, state.error)
                }
            }

            items(state.sharedFiles, key = { it.uri }) { file ->
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MpIconTile(icon = Icons.Filled.Info, size = 44)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = listOfNotNull(
                                    file.mime.takeIf { it.isNotBlank() },
                                    file.size.takeIf { it > 0 }?.let(::formatBytes),
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.OnSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MpButton(
                        text = if (state.sharedFiles.size == 1) {
                            stringResource(R.string.share_import_save_one)
                        } else {
                            stringResource(R.string.share_import_save_many, state.sharedFiles.size)
                        },
                        onClick = viewModel::saveSharedFiles,
                        enabled = !state.busy && state.sharedFiles.isNotEmpty(),
                        loading = state.busy,
                        fillWidth = true,
                    )
                    MpButton(
                        text = stringResource(R.string.cancel),
                        onClick = viewModel::discardSharedFiles,
                        variant = MpButtonVariant.Quiet,
                        enabled = !state.busy,
                        fillWidth = true,
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) {
        return "$bytes B"
    }
    val kb = bytes / 1024.0
    if (kb < 1024) {
        return String.format("%.1f KB", kb)
    }
    return String.format("%.1f MB", kb / 1024.0)
}
