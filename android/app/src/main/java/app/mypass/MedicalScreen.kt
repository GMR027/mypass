package app.mypass

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MedicalScreen(state: UiState, viewModel: MainViewModel) {
    var search by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<UiItem?>(null) }
    var shareTarget by remember { mutableStateOf<UiItem?>(null) }
    var manageSharesTarget by remember { mutableStateOf<UiItem?>(null) }
    var presenting by remember { mutableStateOf<UiItem?>(null) }
    var selectedMedicalId by remember { mutableStateOf<String?>(null) }
    var revealMedicalId by remember { mutableStateOf<String?>(null) }
    val acceptedContacts = state.contacts.filter { it.status == "accepted" }

    val all = remember(state.items) {
        state.items.filter { it.medical != null && it.state != "transferred" }
    }
    val visible = remember(all, search) {
        val query = search.trim()
        all.filter { item ->
            val medical = item.medical ?: return@filter false
            query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                medical.holder.contains(query, ignoreCase = true) ||
                medical.insurance.provider.contains(query, ignoreCase = true) ||
                medical.insurance.policyNumber.contains(query, ignoreCase = true) ||
                medical.allergies.any { it.contains(query, ignoreCase = true) } ||
                medical.conditions.any { it.contains(query, ignoreCase = true) }
        }
    }

    LaunchedEffect(visible.map { it.id }) {
        if (selectedMedicalId != null && visible.none { it.id == selectedMedicalId }) {
            selectedMedicalId = null
        }
    }
    val selectedMedicalIndex = visible.indexOfFirst { it.id == selectedMedicalId }
        .takeIf { it >= 0 }

    deleteTarget?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_medical_title),
            message = stringResource(R.string.confirm_delete_medical_body),
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

    presenting?.let { item ->
        MedicalPresentationDialog(item = item, onDismiss = { presenting = null })
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
            columns = GridCells.Adaptive(minSize = if (state.simpleMode) 560.dp else 340.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    VaultSectionTabs(
                        current = "medical",
                        onDocuments = viewModel::backToVault,
                        onCredentials = viewModel::openCredentials,
                        onMedical = {},
                    )
                    MpStatusBanner(state.status, state.error)
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val compactHeader = maxWidth < 520.dp
                        if (compactHeader) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.medical_title),
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                    Text(
                                        text = stringResource(R.string.medical_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MpColors.OnSurfaceVariant,
                                    )
                                }
                                MpButton(
                                    text = stringResource(R.string.add_medical_card),
                                    onClick = viewModel::newMedicalCard,
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
                                        text = stringResource(R.string.medical_title),
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                    Text(
                                        text = stringResource(R.string.medical_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MpColors.OnSurfaceVariant,
                                    )
                                }
                                MpButton(
                                    text = stringResource(R.string.add_medical_card),
                                    onClick = viewModel::newMedicalCard,
                                    icon = Icons.Filled.Add,
                                    enabled = !state.busy,
                                )
                            }
                        }
                    }
                    MpTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = stringResource(R.string.search_medical),
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = MpColors.OnSurfaceVariant)
                        },
                    )
                }
            }

            if (all.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MpEmptyState(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.no_medical_cards),
                        subtitle = stringResource(R.string.no_medical_cards_subtitle),
                    )
                }
            } else if (visible.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MpEmptyState(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.no_medical_results, search.trim()),
                    )
                }
            } else {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MpCascadeStack(selectedIndex = selectedMedicalIndex) {
                        visible.forEachIndexed { index, item ->
                            if (index == selectedMedicalIndex) {
                                MpRevealOnSelection(
                                    selectionKey = if (revealMedicalId == item.id) item.id else null,
                                    onRevealed = { revealMedicalId = null },
                                ) {
                                    MedicalFlipCard(
                                        item = item,
                                        shared = state.isItemShared(item.id),
                                        canShare = acceptedContacts.isNotEmpty(),
                                        onPresent = { presenting = item },
                                        onEdit = { viewModel.editMedicalCard(item) },
                                        onShare = { shareTarget = item },
                                        onManageShares = { manageSharesTarget = item },
                                        onOpenAttachment = { viewModel.openItem(item) },
                                        onDelete = { deleteTarget = item },
                                    )
                                }
                            } else {
                                MedicalCompactCard(
                                    item = item,
                                    shared = state.isItemShared(item.id),
                                    onClick = {
                                        selectedMedicalId = item.id
                                        revealMedicalId = item.id
                                    },
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
private fun MedicalCompactCard(
    item: UiItem,
    shared: Boolean = false,
    onClick: () -> Unit,
) {
    val medical = item.medical ?: return
    MpCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MedicalHeader(item = item, medical = medical, shared = shared)
            Text(
                text = listOf(
                    stringResource(R.string.blood_type) + ": " + medical.bloodType.ifBlank { "—" },
                    stringResource(R.string.organ_donor) + ": " + stringResource(if (medical.organDonor) R.string.yes else R.string.no),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MpColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MedicalFlipCard(
    item: UiItem,
    shared: Boolean = false,
    canShare: Boolean,
    onPresent: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onManageShares: () -> Unit,
    onOpenAttachment: () -> Unit,
    onDelete: () -> Unit,
) {
    val medical = item.medical ?: return

    MpFlipCard(
        itemKey = item.id,
        shape = MaterialTheme.shapes.large,
        containerColor = MpColors.Surface,
        front = { onFlip ->
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MedicalHeader(item = item, medical = medical, shared = shared)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MedicalStat(
                        label = stringResource(R.string.blood_type),
                        value = medical.bloodType.ifBlank { "—" },
                        modifier = Modifier.weight(1f),
                    )
                    MedicalStat(
                        label = stringResource(R.string.organ_donor),
                        value = stringResource(if (medical.organDonor) R.string.yes else R.string.no),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MedicalHeader(item = item, medical = medical, shared = shared)
                MedicalDetails(
                    item = item,
                    medical = medical,
                    canShare = canShare,
                    shared = shared,
                    onPresent = onPresent,
                    onEdit = onEdit,
                    onShare = onShare,
                    onManageShares = onManageShares,
                    onOpenAttachment = onOpenAttachment,
                    onDelete = onDelete,
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
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicalHeader(item: UiItem, medical: UiMedicalCard, shared: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MpIconTile(icon = Icons.Filled.Info, size = 50)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = medical.holder.ifBlank { stringResource(R.string.no_holder) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MpBadge(
                text = stringResource(R.string.medical),
                tone = MpBadgeTone.Primary,
            )
            if (shared) {
                MpBadge(text = stringResource(R.string.shared), tone = MpBadgeTone.Primary)
            }
        }
    }
}

@Composable
private fun MedicalDetails(
    item: UiItem,
    medical: UiMedicalCard,
    canShare: Boolean,
    shared: Boolean,
    onPresent: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onManageShares: () -> Unit,
    onOpenAttachment: () -> Unit,
    onDelete: () -> Unit,
) {
    val contact = medical.contacts.firstOrNull { it.name.isNotBlank() || it.phone.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MedicalStat(
                label = stringResource(R.string.blood_type),
                value = medical.bloodType.ifBlank { "—" },
                modifier = Modifier.weight(1f),
            )
            MedicalStat(
                label = stringResource(R.string.organ_donor),
                value = stringResource(if (medical.organDonor) R.string.yes else R.string.no),
                modifier = Modifier.weight(1f),
            )
        }

        if (medical.allergies.isNotEmpty()) {
            MedicalStat(
                label = stringResource(R.string.allergies),
                value = medical.allergies.joinToString(", "),
            )
        }
        if (medical.conditions.isNotEmpty() || medical.medications.isNotEmpty()) {
            MedicalStat(
                label = stringResource(R.string.conditions_medications),
                value = listOf(
                    medical.conditions.joinToString(", "),
                    medical.medications.joinToString(", "),
                ).filter { it.isNotBlank() }.joinToString(" · "),
            )
        }
        val scheduledMeds = medical.medicationSchedule.filter { it.name.isNotBlank() }
        if (scheduledMeds.isNotEmpty()) {
            MedicalStat(
                label = stringResource(R.string.medications_schedule_title),
                value = scheduledMeds.joinToString(" · ") { medication ->
                    listOf(
                        medication.name,
                        medication.dose,
                        medication.times.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "(", postfix = ")"),
                    ).filter { !it.isNullOrBlank() }.joinToString(" ")
                },
            )
        }
        val nextAppointment = medical.appointments
            .filter { it.title.isNotBlank() && it.date.isNotBlank() }
            .minByOrNull { "${it.date} ${it.time}" }
        if (nextAppointment != null) {
            MedicalStat(
                label = stringResource(R.string.next_appointment),
                value = listOf(
                    nextAppointment.title,
                    nextAppointment.date,
                    nextAppointment.time,
                    nextAppointment.location,
                ).filter { it.isNotBlank() }.joinToString(" · "),
            )
        }
        if (contact != null) {
            MedicalStat(
                label = stringResource(R.string.emergency_contact),
                value = listOf(contact.name, contact.relation, contact.phone)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
            )
        }
        if (medical.insurance.provider.isNotBlank() || medical.insurance.policyNumber.isNotBlank()) {
            MedicalStat(
                label = stringResource(R.string.medical_insurance),
                value = listOf(medical.insurance.provider, medical.insurance.policyNumber)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
            )
        }
        if (item.expiresAt != null) {
            MedicalStat(
                label = stringResource(R.string.valid_until),
                value = item.expiresAt.take(10),
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MedicalAction(stringResource(R.string.present_card), onPresent, primary = true)
            MedicalAction(stringResource(R.string.edit), onEdit)
            if (canShare) {
                MedicalAction(stringResource(R.string.share), onShare)
            }
            if (shared) {
                MedicalAction(stringResource(R.string.manage_shares), onManageShares)
            }
            if (item.blobId != null) {
                MedicalAction(stringResource(R.string.attachment), onOpenAttachment)
            }
            MpButton(
                text = stringResource(R.string.delete),
                onClick = onDelete,
                variant = MpButtonVariant.Danger,
                compact = true,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MedicalStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MpColors.SurfaceLow, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MpColors.OnSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MpColors.OnSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MedicalAction(text: String, onClick: () -> Unit, primary: Boolean = false) {
    MpButton(
        text = text,
        onClick = onClick,
        variant = if (primary) MpButtonVariant.Primary else MpButtonVariant.Secondary,
        compact = true,
        maxLines = 1,
    )
}

private fun parseMedicationTimes(value: String): List<String> {
    return value.split(',', ';', ' ')
        .map { it.trim().take(5) }
        .filter { it.matches(Regex("^\\d{1,2}:\\d{2}$")) }
        .map { it.padStart(5, '0') }
        .distinct()
}

@Composable
private fun MedicationRow(
    medication: UiMedication,
    onChange: (UiMedication) -> Unit,
    onRemove: () -> Unit,
) {
    var timesText by remember(medication.id) {
        mutableStateOf(medication.times.joinToString(", "))
    }

    MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.medication),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.remove), color = MpColors.Error)
                }
            }
            MpTextField(
                value = medication.name,
                onValueChange = { onChange(medication.copy(name = it)) },
                label = stringResource(R.string.medication),
                placeholder = stringResource(R.string.medication_placeholder),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MpTextField(
                    value = medication.dose,
                    onValueChange = { onChange(medication.copy(dose = it)) },
                    label = stringResource(R.string.dose),
                    placeholder = stringResource(R.string.dose_placeholder),
                    modifier = Modifier.weight(1f),
                )
                MpTextField(
                    value = timesText,
                    onValueChange = {
                        timesText = it
                        onChange(medication.copy(times = parseMedicationTimes(it)))
                    },
                    label = stringResource(R.string.medication_times),
                    placeholder = "08:00, 20:00",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MpDateField(
                    value = medication.startDate,
                    onValueChange = { onChange(medication.copy(startDate = it)) },
                    label = stringResource(R.string.medication_start),
                    modifier = Modifier.weight(1f),
                )
                MpDateField(
                    value = medication.endDate,
                    onValueChange = { onChange(medication.copy(endDate = it)) },
                    label = stringResource(R.string.medication_end),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AppointmentRow(
    appointment: UiAppointment,
    onChange: (UiAppointment) -> Unit,
    onRemove: () -> Unit,
) {
    MpCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.appointment),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRemove) {
                    Text(stringResource(R.string.remove), color = MpColors.Error)
                }
            }
            MpTextField(
                value = appointment.title,
                onValueChange = { onChange(appointment.copy(title = it)) },
                label = stringResource(R.string.appointment_title),
                placeholder = stringResource(R.string.appointment_placeholder),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MpDateField(
                    value = appointment.date,
                    onValueChange = { onChange(appointment.copy(date = it)) },
                    label = stringResource(R.string.date),
                    modifier = Modifier.weight(1f),
                )
                MpTimeField(
                    value = appointment.time,
                    onValueChange = { onChange(appointment.copy(time = it)) },
                    label = stringResource(R.string.time),
                    modifier = Modifier.weight(1f),
                )
            }
            MpTextField(
                value = appointment.location,
                onValueChange = { onChange(appointment.copy(location = it)) },
                label = stringResource(R.string.appointment_location),
                placeholder = stringResource(R.string.appointment_location_placeholder),
            )
            MpTextField(
                value = appointment.notes,
                onValueChange = { onChange(appointment.copy(notes = it)) },
                label = stringResource(R.string.notes),
            )
            Text(
                text = stringResource(R.string.appointment_reminder),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    1440 to stringResource(R.string.reminder_one_day),
                    120 to stringResource(R.string.reminder_two_hours),
                    30 to stringResource(R.string.reminder_thirty_minutes),
                ).forEach { (minutes, label) ->
                    MpFilterChip(
                        text = label,
                        selected = appointment.reminderMinutes == minutes,
                        onClick = { onChange(appointment.copy(reminderMinutes = minutes)) },
                    )
                }
            }
        }
    }
}

@Composable
fun MedicalEditorScreen(state: UiState, viewModel: MainViewModel) {
    val source = state.medicalDraft ?: return
    var title by remember(source) { mutableStateOf(source.title) }
    var holder by remember(source) { mutableStateOf(source.holder) }
    var birthDate by remember(source) { mutableStateOf(source.birthDate) }
    var bloodType by remember(source) { mutableStateOf(source.bloodType) }
    var allergies by remember(source) { mutableStateOf(source.allergies) }
    var conditions by remember(source) { mutableStateOf(source.conditions) }
    var medicationSchedule by remember(source) { mutableStateOf(source.medicationSchedule) }
    var appointments by remember(source) { mutableStateOf(source.appointments) }
    var contactName by remember(source) { mutableStateOf(source.contactName) }
    var contactRelation by remember(source) { mutableStateOf(source.contactRelation) }
    var contactPhone by remember(source) { mutableStateOf(source.contactPhone) }
    var insuranceProvider by remember(source) { mutableStateOf(source.insuranceProvider) }
    var insurancePlan by remember(source) { mutableStateOf(source.insurancePlan) }
    var insurancePolicy by remember(source) { mutableStateOf(source.insurancePolicy) }
    var insuranceMember by remember(source) { mutableStateOf(source.insuranceMember) }
    var insurancePhone by remember(source) { mutableStateOf(source.insurancePhone) }
    var doctorName by remember(source) { mutableStateOf(source.doctorName) }
    var doctorPhone by remember(source) { mutableStateOf(source.doctorPhone) }
    var doctorClinic by remember(source) { mutableStateOf(source.doctorClinic) }
    var organDonor by remember(source) { mutableStateOf(source.organDonor) }
    var notes by remember(source) { mutableStateOf(source.notes) }
    var expiresAt by remember(source) { mutableStateOf(source.expiresAt) }
    var attachmentUri by remember(source) { mutableStateOf(source.attachmentUri) }
    var attachmentName by remember(source) { mutableStateOf(source.attachmentName ?: source.existingAttachmentName) }
    var clearAttachment by remember(source) { mutableStateOf(source.clearAttachment) }
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            attachmentUri = uri.toString()
            attachmentName = medicalDisplayName(context, uri)
            clearAttachment = false
        }
    }

    val draft = source.copy(
        title = title,
        holder = holder,
        birthDate = birthDate,
        bloodType = bloodType,
        allergies = allergies,
        conditions = conditions,
        medications = medicationSchedule.map { it.name }.joinToString(", "),
        medicationSchedule = medicationSchedule,
        appointments = appointments,
        contactName = contactName,
        contactRelation = contactRelation,
        contactPhone = contactPhone,
        insuranceProvider = insuranceProvider,
        insurancePlan = insurancePlan,
        insurancePolicy = insurancePolicy,
        insuranceMember = insuranceMember,
        insurancePhone = insurancePhone,
        doctorName = doctorName,
        doctorPhone = doctorPhone,
        doctorClinic = doctorClinic,
        organDonor = organDonor,
        notes = notes,
        expiresAt = expiresAt,
        attachmentUri = attachmentUri,
        attachmentName = attachmentName,
        clearAttachment = clearAttachment,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(if (source.isNew) R.string.new_medical_card else R.string.edit_medical_card),
            onBack = viewModel::discardMedicalDraft,
            action = {
                TextButton(
                    onClick = { viewModel.saveMedicalCard(draft) },
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
                MedicalEditorCard(title, holder, bloodType, allergies, contactName, contactPhone)
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.holder_data), style = MaterialTheme.typography.titleMedium)
                        MpTextField(title, { title = it }, stringResource(R.string.card_name))
                        MpTextField(holder, { holder = it }, stringResource(R.string.holder))
                        MpTextField(birthDate, { birthDate = it }, stringResource(R.string.birth_date), placeholder = "AAAA-MM-DD")
                        Text(stringResource(R.string.blood_type), style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-").forEach { value ->
                                MpFilterChip(value, bloodType == value) { bloodType = value }
                            }
                        }
                        MpSwitchRow(
                            title = stringResource(R.string.organ_donor),
                            subtitle = stringResource(R.string.organ_donor_hint),
                            checked = organDonor,
                            onCheckedChange = { organDonor = it },
                        )
                        MpTextField(allergies, { allergies = it }, stringResource(R.string.allergies_hint))
                        MpTextField(conditions, { conditions = it }, stringResource(R.string.conditions_hint))
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.medications_schedule_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.medications_schedule_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                        medicationSchedule.forEachIndexed { index, medication ->
                            MedicationRow(
                                medication = medication,
                                onChange = { updated ->
                                    medicationSchedule = medicationSchedule.toMutableList().also {
                                        it[index] = updated
                                    }
                                },
                                onRemove = {
                                    medicationSchedule = medicationSchedule.filterIndexed { itemIndex, _ ->
                                        itemIndex != index
                                    }
                                },
                            )
                        }
                        MpButton(
                            text = stringResource(R.string.add_medication),
                            onClick = {
                                medicationSchedule = medicationSchedule + UiMedication(
                                    id = java.util.UUID.randomUUID().toString(),
                                )
                            },
                            variant = MpButtonVariant.Secondary,
                            fillWidth = true,
                            icon = Icons.Filled.Add,
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.appointments_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.appointments_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                        appointments.forEachIndexed { index, appointment ->
                            AppointmentRow(
                                appointment = appointment,
                                onChange = { updated ->
                                    appointments = appointments.toMutableList().also {
                                        it[index] = updated
                                    }
                                },
                                onRemove = {
                                    appointments = appointments.filterIndexed { itemIndex, _ ->
                                        itemIndex != index
                                    }
                                },
                            )
                        }
                        MpButton(
                            text = stringResource(R.string.add_appointment),
                            onClick = {
                                appointments = appointments + UiAppointment(
                                    id = java.util.UUID.randomUUID().toString(),
                                )
                            },
                            variant = MpButtonVariant.Secondary,
                            fillWidth = true,
                            icon = Icons.Filled.Add,
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.emergency_contact), style = MaterialTheme.typography.titleMedium)
                        MpTextField(contactName, { contactName = it }, stringResource(R.string.name))
                        MpTextField(contactRelation, { contactRelation = it }, stringResource(R.string.relationship))
                        MpTextField(
                            contactPhone,
                            { contactPhone = it },
                            stringResource(R.string.phone),
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.medical_insurance), style = MaterialTheme.typography.titleMedium)
                        MpTextField(insuranceProvider, { insuranceProvider = it }, stringResource(R.string.insurance_provider))
                        MpTextField(insurancePlan, { insurancePlan = it }, stringResource(R.string.insurance_plan))
                        MpTextField(insurancePolicy, { insurancePolicy = it }, stringResource(R.string.policy_number))
                        MpTextField(insuranceMember, { insuranceMember = it }, stringResource(R.string.insurance_member_number))
                        MpTextField(
                            insurancePhone,
                            { insurancePhone = it },
                            stringResource(R.string.insurance_phone),
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.doctor), style = MaterialTheme.typography.titleMedium)
                        MpTextField(doctorName, { doctorName = it }, stringResource(R.string.name))
                        MpTextField(
                            doctorPhone,
                            { doctorPhone = it },
                            stringResource(R.string.phone),
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                        )
                        MpTextField(doctorClinic, { doctorClinic = it }, stringResource(R.string.clinic))
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.more_data), style = MaterialTheme.typography.titleMedium)
                        MpTextField(expiresAt, { expiresAt = it }, stringResource(R.string.valid_until), placeholder = "AAAA-MM-DD")
                        MpTextField(notes, { notes = it }, stringResource(R.string.emergency_notes), singleLine = false)
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
                            text = stringResource(R.string.medical_attachment_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            item {
                MpButton(
                    text = stringResource(R.string.save_medical_card),
                    onClick = { viewModel.saveMedicalCard(draft) },
                    icon = Icons.Filled.Info,
                    enabled = !state.busy,
                    loading = state.busy,
                    fillWidth = true,
                )
            }
        }
    }
}

@Composable
private fun MedicalEditorCard(
    title: String,
    holder: String,
    bloodType: String,
    allergies: String,
    contactName: String,
    contactPhone: String,
) {
    MpCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MpIconTile(icon = Icons.Filled.Info, size = 50)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { stringResource(R.string.medical_card_preview) },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = holder.ifBlank { stringResource(R.string.no_holder) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MedicalStat(stringResource(R.string.blood_type), bloodType.ifBlank { "—" }, Modifier.weight(1f))
                MedicalStat(
                    stringResource(R.string.allergies),
                    allergies.ifBlank { stringResource(R.string.none) },
                    Modifier.weight(1f),
                )
            }
            if (contactName.isNotBlank() || contactPhone.isNotBlank()) {
                MedicalStat(
                    stringResource(R.string.emergency_contact),
                    listOf(contactName, contactPhone).filter { it.isNotBlank() }.joinToString(" · "),
                )
            }
        }
    }
}

@Composable
private fun MedicalPresentationDialog(item: UiItem, onDismiss: () -> Unit) {
    val medical = item.medical ?: return
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = activity?.window
        val previousBrightness = window?.attributes?.screenBrightness
        view.keepScreenOn = true
        window?.let {
            val attributes = it.attributes
            attributes.screenBrightness = 1f
            it.attributes = attributes
            it.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            view.keepScreenOn = false
            window?.let {
                val attributes = it.attributes
                attributes.screenBrightness = previousBrightness ?: -1f
                it.attributes = attributes
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    MpFullScreenWindow(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.medical_card),
                    style = MaterialTheme.typography.labelMedium,
                    color = MpColors.Primary,
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = medical.holder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MpColors.OnSurfaceVariant,
                )
            }
            MpIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.close),
                onClick = onDismiss,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PresentationValue(stringResource(R.string.blood_type), medical.bloodType.ifBlank { "—" })
            PresentationValue(
                stringResource(R.string.allergies),
                medical.allergies.joinToString(", ").ifBlank { stringResource(R.string.none) },
            )
            PresentationValue(
                stringResource(R.string.conditions_medications),
                listOf(
                    medical.conditions.joinToString(", "),
                    medical.medications.joinToString(", "),
                ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { stringResource(R.string.none) },
            )
            val contact = medical.contacts.firstOrNull { it.name.isNotBlank() || it.phone.isNotBlank() }
            PresentationValue(
                stringResource(R.string.emergency_contact),
                contact?.let { listOf(it.name, it.relation, it.phone).filter(String::isNotBlank).joinToString(" · ") }
                    ?: stringResource(R.string.none),
            )
            PresentationValue(
                stringResource(R.string.medical_insurance),
                listOf(medical.insurance.provider, medical.insurance.policyNumber)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { stringResource(R.string.none) },
            )
            PresentationValue(
                stringResource(R.string.doctor),
                listOf(medical.doctor.name, medical.doctor.clinic, medical.doctor.phone)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { stringResource(R.string.none) },
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.medical_presentation_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MpColors.OnSurfaceVariant,
        )
    }
}

@Composable
private fun PresentationValue(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MpColors.SurfaceLow,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MpColors.OnSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MpColors.OnSurface,
            )
        }
    }
}

private fun medicalDisplayName(context: Context, uri: Uri): String {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: uri.lastPathSegment ?: "adjunto"
}
