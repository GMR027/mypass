package app.mypass

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun BackupScreen(state: UiState, viewModel: MainViewModel) {
    var exportUri by remember { mutableStateOf<Uri?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var dialogMode by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            exportUri = uri
            dialogMode = "export"
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            importUri = uri
            dialogMode = "import"
        }
    }

    when (dialogMode) {
        "export" -> BackupPassphraseDialog(
            mode = "export",
            onDismiss = { dialogMode = null },
            onConfirm = { passphrase ->
                exportUri?.let { viewModel.exportBackup(passphrase, it) }
                dialogMode = null
            },
        )

        "import" -> BackupPassphraseDialog(
            mode = "import",
            onDismiss = { dialogMode = null },
            onConfirm = { passphrase ->
                importUri?.let { viewModel.importBackup(passphrase, it) }
                dialogMode = null
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(R.string.backup_title),
            onBack = viewModel::openSettings,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { MpStatusBanner(state.status, state.error) }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.backup_create_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.backup_create_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                        MpButton(
                            text = stringResource(R.string.backup_create_action),
                            onClick = {
                                val name = "mypass-backup-" + LocalDate.now()
                                    .format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".mypassbackup"
                                exportLauncher.launch(name)
                            },
                            enabled = !state.busy,
                            fillWidth = true,
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.backup_restore_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.backup_restore_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                        MpButton(
                            text = stringResource(R.string.backup_restore_action),
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                            variant = MpButtonVariant.Secondary,
                            enabled = !state.busy,
                            fillWidth = true,
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.backup_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BackupPassphraseDialog(
    mode: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val exporting = mode == "export"
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = passphrase.length >= 12 && (!exporting || passphrase == confirmation)
    val mismatch = exporting && confirmation.isNotBlank() && passphrase != confirmation

    MpDialog(
        onDismissRequest = onDismiss,
        header = {
            MpDialogHeader(
                icon = Icons.Filled.Lock,
                title = stringResource(if (exporting) R.string.backup_passphrase_title else R.string.backup_restore_passphrase_title),
                subtitle = stringResource(
                    if (exporting) R.string.backup_passphrase_help else R.string.backup_restore_passphrase_help,
                ),
                onClose = onDismiss,
            )
        },
        actions = {
            MpDialogActions(
                primaryLabel = stringResource(if (exporting) R.string.backup_create_action else R.string.backup_restore_action),
                onPrimary = { onConfirm(passphrase) },
                primaryEnabled = valid,
                secondaryLabel = stringResource(R.string.cancel),
                onSecondary = onDismiss,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MpPasswordField(
                value = passphrase,
                onValueChange = { passphrase = it.take(128) },
                label = stringResource(R.string.backup_passphrase),
                keyboardType = KeyboardType.Password,
            )
            if (exporting) {
                MpPasswordField(
                    value = confirmation,
                    onValueChange = { confirmation = it.take(128) },
                    label = stringResource(R.string.backup_passphrase_confirm),
                    keyboardType = KeyboardType.Password,
                )
                Text(
                    text = stringResource(R.string.backup_passphrase_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.Secondary,
                )
            }
            if (mismatch) {
                Text(
                    text = stringResource(R.string.backup_passphrase_mismatch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.Error,
                )
            }
        }
    }
}
