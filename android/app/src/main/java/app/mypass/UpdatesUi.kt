package app.mypass

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun UpdateSettingsCard(state: UiState, viewModel: MainViewModel) {
    val release = state.updateState.release
    val busy = state.updateState is UpdateState.Checking ||
        state.updateState is UpdateState.Downloading

    MpCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MpIconTile(icon = Icons.Filled.Refresh, size = 46)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.updates_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.updates_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.updates_installed_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MpColors.OnSurfaceVariant,
            )

            UpdateStatus(state = state, release = release)

            if (state.updateState is UpdateState.Downloading) {
                val downloading = state.updateState as UpdateState.Downloading
                LinearProgressIndicator(
                    progress = { downloading.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MpColors.Primary,
                    trackColor = MpColors.PrimaryFixed,
                )
                Text(
                    text = stringResource(
                        R.string.updates_downloading_size,
                        formatBytes(downloading.downloadedBytes),
                        formatBytes(downloading.totalBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }

            if (release != null) {
                UpdateNotes(release = release)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MpButton(
                    text = stringResource(R.string.updates_check),
                    onClick = viewModel::checkForUpdatesManually,
                    variant = MpButtonVariant.Secondary,
                    enabled = !busy,
                    loading = state.updateState is UpdateState.Checking,
                    fillWidth = true,
                )
                UpdatePrimaryAction(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = if (state.lastUpdateCheck > 0L) {
                    stringResource(
                        R.string.updates_last_checked,
                        DateUtils.getRelativeTimeSpanString(
                            state.lastUpdateCheck,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                        ).toString(),
                    )
                } else {
                    stringResource(R.string.updates_never_checked)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MpColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
fun UpdateNotificationCard(state: UiState, viewModel: MainViewModel) {
    val release = state.updateState.release ?: return
    val mandatory = release.isMandatory

    MpCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MpIconTile(icon = Icons.Filled.Refresh, size = 44)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.notification_update_title, release.versionName),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.updates_dialog_body,
                            release.versionName,
                            formatBytes(release.sizeBytes),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
            }

            UpdateStatus(state = state, release = release)
            UpdateNotes(release = release)

            if (state.updateState is UpdateState.Downloading) {
                val downloading = state.updateState as UpdateState.Downloading
                LinearProgressIndicator(
                    progress = { downloading.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MpColors.Primary,
                    trackColor = MpColors.PrimaryFixed,
                )
                Text(
                    text = stringResource(
                        R.string.updates_downloading_size,
                        formatBytes(downloading.downloadedBytes),
                        formatBytes(downloading.totalBytes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UpdatePrimaryAction(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
                if (!mandatory) {
                    MpButton(
                        text = stringResource(R.string.updates_now_later),
                        onClick = {
                            viewModel.dismissUpdate()
                            viewModel.dismissUpdateDialog()
                        },
                        variant = MpButtonVariant.Quiet,
                        modifier = Modifier.weight(1f),
                        compact = true,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdatePrimaryAction(
    state: UiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    when (val update = state.updateState) {
        is UpdateState.Available -> MpButton(
            text = stringResource(R.string.updates_download),
            onClick = viewModel::downloadUpdate,
            icon = Icons.Filled.Refresh,
            modifier = modifier,
            compact = true,
            maxLines = 1,
        )

        is UpdateState.Downloading -> MpButton(
            text = stringResource(R.string.updates_downloading, update.progress),
            onClick = {},
            enabled = false,
            modifier = modifier,
            compact = true,
            maxLines = 1,
        )

        is UpdateState.Ready -> MpButton(
            text = stringResource(R.string.updates_install),
            onClick = viewModel::installUpdate,
            icon = Icons.Filled.Refresh,
            modifier = modifier,
            compact = true,
            maxLines = 1,
        )

        is UpdateState.NeedsPermission -> MpButton(
            text = stringResource(R.string.updates_open_permission),
            onClick = viewModel::openInstallPermissionSettings,
            modifier = modifier,
            compact = true,
            maxLines = 1,
        )

        is UpdateState.Failed -> MpButton(
            text = stringResource(R.string.updates_retry),
            onClick = viewModel::downloadUpdate,
            icon = Icons.Filled.Refresh,
            modifier = modifier,
            compact = true,
            maxLines = 1,
        )

        else -> Unit
    }
}

@Composable
private fun UpdateStatus(state: UiState, release: AppRelease?) {
    val message = when (val update = state.updateState) {
        is UpdateState.Idle -> null
        is UpdateState.Checking -> stringResource(R.string.updates_checking)
        is UpdateState.UpToDate -> stringResource(R.string.updates_up_to_date)
        is UpdateState.Available -> stringResource(R.string.updates_available, update.release.versionName)
        is UpdateState.Downloading -> stringResource(R.string.updates_downloading, update.progress)
        is UpdateState.Ready -> stringResource(R.string.updates_ready)
        is UpdateState.NeedsPermission -> stringResource(R.string.updates_permission_title)
        is UpdateState.Failed -> update.message
    }

    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = when (state.updateState) {
                is UpdateState.Failed -> MpColors.Error
                is UpdateState.UpToDate -> MpColors.Primary
                else -> MpColors.OnSurface
            },
        )
    }

    if (state.updateState is UpdateState.NeedsPermission) {
        Text(
            text = stringResource(R.string.updates_permission_body),
            style = MaterialTheme.typography.bodySmall,
            color = MpColors.OnSurfaceVariant,
        )
    }

    if (release?.isMandatory == true) {
        Text(
            text = stringResource(R.string.updates_mandatory),
            style = MaterialTheme.typography.bodySmall,
            color = MpColors.Error,
        )
    }
}

@Composable
private fun UpdateNotes(release: AppRelease) {
    val notes = release.notesFor(Locale.getDefault().language)
    if (notes.isEmpty()) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.updates_notes_title),
            style = MaterialTheme.typography.labelLarge,
            color = MpColors.OnSurfaceVariant,
        )
        notes.take(4).forEach { note ->
            Text(
                text = "• $note",
                style = MaterialTheme.typography.bodySmall,
                color = MpColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
fun UpdateDialog(state: UiState, viewModel: MainViewModel) {
    val release = state.updateState.release ?: return
    val mandatory = release.isMandatory

    MpDialog(
        onDismissRequest = {
            if (!mandatory) {
                viewModel.dismissUpdateDialog()
            }
        },
        header = {
            MpDialogHeader(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.updates_dialog_title),
                subtitle = stringResource(
                    R.string.updates_dialog_body,
                    release.versionName,
                    formatBytes(release.sizeBytes),
                ),
                onClose = if (mandatory) null else viewModel::dismissUpdateDialog,
            )
        },
        actions = {
            MpButton(
                text = when (state.updateState) {
                    is UpdateState.Ready -> stringResource(R.string.updates_install)
                    is UpdateState.NeedsPermission -> stringResource(R.string.updates_open_permission)
                    is UpdateState.Downloading -> stringResource(
                        R.string.updates_downloading,
                        (state.updateState as UpdateState.Downloading).progress,
                    )
                    is UpdateState.Failed -> stringResource(R.string.updates_retry)
                    else -> stringResource(R.string.updates_download)
                },
                onClick = {
                    when (state.updateState) {
                        is UpdateState.Ready -> viewModel.installUpdate()
                        is UpdateState.NeedsPermission -> viewModel.openInstallPermissionSettings()
                        is UpdateState.Failed -> viewModel.downloadUpdate()
                        is UpdateState.Downloading -> Unit
                        else -> viewModel.downloadUpdate()
                    }
                },
                enabled = state.updateState !is UpdateState.Downloading,
                icon = Icons.Filled.Refresh,
                fillWidth = true,
            )
            if (!mandatory) {
                MpButton(
                    text = stringResource(R.string.updates_now_later),
                    onClick = {
                        viewModel.dismissUpdate()
                        viewModel.dismissUpdateDialog()
                    },
                    variant = MpButtonVariant.Quiet,
                    fillWidth = true,
                )
            }
        },
    ) {
        UpdateStatus(state = state, release = release)
        UpdateNotes(release = release)

        if (state.updateState is UpdateState.Downloading) {
            val downloading = state.updateState as UpdateState.Downloading
            LinearProgressIndicator(
                progress = { downloading.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MpColors.Primary,
                trackColor = MpColors.PrimaryFixed,
            )
            Text(
                text = stringResource(
                    R.string.updates_downloading_size,
                    formatBytes(downloading.downloadedBytes),
                    formatBytes(downloading.totalBytes),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MpColors.OnSurfaceVariant,
            )
        }

        Text(
            text = stringResource(R.string.updates_restart_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MpColors.OnSurfaceVariant,
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) {
        return "—"
    }

    val megabytes = bytes / (1024.0 * 1024.0)
    return if (megabytes >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", megabytes)
    } else {
        "${bytes / 1024} KB"
    }
}
