package app.mypass

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private data class InsuranceStatus(
    val label: String,
    val tone: MpBadgeTone,
    val expired: Boolean,
)

private data class ClaimStatus(
    val label: String,
    val tone: MpBadgeTone,
)

@Composable
fun InsuranceScreen(state: UiState, viewModel: MainViewModel) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var deleteTarget by remember { mutableStateOf<UiItem?>(null) }
    var shareTarget by remember { mutableStateOf<UiItem?>(null) }
    var manageSharesTarget by remember { mutableStateOf<UiItem?>(null) }
    var presenting by remember { mutableStateOf<UiItem?>(null) }
    var emergencyOpen by remember { mutableStateOf(false) }
    var claimsTarget by remember { mutableStateOf<UiItem?>(null) }
    var claims by remember { mutableStateOf<List<UiInsuranceClaim>>(emptyList()) }
    var claimFormOpen by remember { mutableStateOf(false) }
    var claimEditing by remember { mutableStateOf<UiInsuranceClaim?>(null) }
    val acceptedContacts = state.contacts.filter { it.status == "accepted" }

    val all = remember(state.items) {
        state.items.filter { it.insurance != null && it.state != "transferred" }
    }
    val visible = remember(all, search, filter) {
        val query = search.trim()
        all.filter { item ->
            val insurance = item.insurance ?: return@filter false
            (filter == "all" || insurance.category == filter) &&
                (
                    query.isBlank() ||
                        item.title.contains(query, ignoreCase = true) ||
                        insurance.provider.contains(query, ignoreCase = true) ||
                        insurance.product.contains(query, ignoreCase = true) ||
                        insurance.policyNumber.contains(query, ignoreCase = true) ||
                        insurance.holder.contains(query, ignoreCase = true) ||
                        insurance.emergencyPhone.contains(query, ignoreCase = true) ||
                        insurance.assistancePhone.contains(query, ignoreCase = true) ||
                        insurance.subject.any {
                            it.label.contains(query, ignoreCase = true) ||
                                it.value.contains(query, ignoreCase = true)
                        } ||
                        insurance.claims.any {
                            it.claimNumber.contains(query, ignoreCase = true) ||
                                it.description.contains(query, ignoreCase = true) ||
                                it.adjusterName.contains(query, ignoreCase = true)
                        }
                    )
        }
    }

    fun openClaims(item: UiItem) {
        claimsTarget = item
        claims = item.insurance?.claims.orEmpty()
        claimFormOpen = false
        claimEditing = null
    }

    deleteTarget?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_insurance_title),
            message = stringResource(R.string.confirm_delete_insurance_body),
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
        InsurancePresentationDialog(
            item = item,
            onDismiss = { presenting = null },
            onShare = if (acceptedContacts.isNotEmpty()) {
                {
                    presenting = null
                    shareTarget = item
                }
            } else {
                null
            },
            onOpenPolicy = { viewModel.openItem(item) },
        )
    }

    if (emergencyOpen) {
        InsuranceEmergencyDialog(
            items = all,
            onDismiss = { emergencyOpen = false },
            onOpenPolicy = {
                emergencyOpen = false
                presenting = it
            },
        )
    }

    claimsTarget?.let { item ->
        if (claimFormOpen) {
            InsuranceClaimDialog(
                claim = claimEditing,
                onDismiss = {
                    claimFormOpen = false
                    claimEditing = null
                },
                onSave = { saved ->
                    val next = if (claimEditing == null) {
                        claims + saved
                    } else {
                        claims.map { if (it.id == saved.id) saved else it }
                    }
                    claims = next
                    viewModel.saveInsuranceClaims(item, next)
                    claimFormOpen = false
                    claimEditing = null
                },
            )
        } else {
            InsuranceClaimsDialog(
                item = item,
                claims = claims,
                onDismiss = { claimsTarget = null },
                onAdd = {
                    claimEditing = null
                    claimFormOpen = true
                },
                onEdit = {
                    claimEditing = it
                    claimFormOpen = true
                },
                onDelete = { claim ->
                    val next = claims.filter { it.id != claim.id }
                    claims = next
                    viewModel.saveInsuranceClaims(item, next)
                },
            )
        }
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
                        current = "insurance",
                        onDocuments = viewModel::backToVault,
                        onCredentials = viewModel::openCredentials,
                        onMedical = viewModel::openMedical,
                        onInsurance = {},
                    )
                    MpStatusBanner(state.status, state.error)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.insurance_title),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = stringResource(R.string.insurance_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MpColors.OnSurfaceVariant,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MpButton(
                            text = stringResource(R.string.insurance_emergency),
                            onClick = { emergencyOpen = true },
                            variant = MpButtonVariant.Secondary,
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            modifier = Modifier.weight(1f),
                            compact = true,
                            maxLines = 1,
                        )
                        MpButton(
                            text = stringResource(R.string.add_insurance),
                            onClick = viewModel::newInsurance,
                            icon = Icons.Filled.Add,
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                            compact = true,
                            maxLines = 1,
                        )
                    }
                    MpTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = stringResource(R.string.search_insurance),
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MpColors.OnSurfaceVariant,
                            )
                        },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MpFilterChip(stringResource(R.string.all), filter == "all") { filter = "all" }
                        insuranceCategoryValues.forEach { value ->
                            MpFilterChip(insuranceCategoryLabel(value), filter == value) { filter = value }
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MpEmptyState(
                        icon = Icons.Filled.Lock,
                        title = if (all.isEmpty()) {
                            stringResource(R.string.no_insurance)
                        } else {
                            stringResource(R.string.no_insurance_results, search.ifBlank { "—" })
                        },
                        subtitle = stringResource(R.string.no_insurance_subtitle),
                    )
                }
            } else {
                gridItems(visible, key = { it.id }) { item ->
                    InsuranceCard(
                        item = item,
                        shared = state.isItemShared(item.id),
                        hasContacts = acceptedContacts.isNotEmpty(),
                        onPresent = { presenting = item },
                        onClaims = { openClaims(item) },
                        onEdit = { viewModel.editInsurance(item) },
                        onOpenPolicy = { viewModel.openItem(item) },
                        onShare = { shareTarget = item },
                        onManageShares = { manageSharesTarget = item },
                        onDelete = { deleteTarget = item },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsuranceCard(
    item: UiItem,
    shared: Boolean,
    hasContacts: Boolean,
    onPresent: () -> Unit,
    onClaims: () -> Unit,
    onEdit: () -> Unit,
    onOpenPolicy: () -> Unit,
    onShare: () -> Unit,
    onManageShares: () -> Unit,
    onDelete: () -> Unit,
) {
    val insurance = item.insurance ?: return
    val status = insuranceStatus(item)
    val activeClaim = activeInsuranceClaim(insurance)
    MpCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MpIconTile(
                    icon = insuranceCategoryIcon(insurance.category),
                    size = 48,
                    background = MpColors.SecondaryFixed,
                    tint = MpColors.OnSecondaryFixed,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOf(insurance.provider, insurance.product)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { stringResource(R.string.insurance_fallback_title) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MpColors.OnSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        MpBadge(insuranceCategoryLabel(insurance.category), MpBadgeTone.Neutral)
                        MpBadge(status.label, status.tone)
                        if (shared) {
                            MpBadge(stringResource(R.string.shared), MpBadgeTone.Primary)
                        }
                    }
                }
            }

            if (insurance.policyNumber.isNotBlank()) {
                Text(
                    text = stringResource(R.string.policy_number_value, insurance.policyNumber),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MpColors.Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MpColors.PrimaryFixed.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (insurance.holder.isNotBlank()) {
                    InsuranceInfoRow(stringResource(R.string.holder), insurance.holder)
                }
                if (insurance.validUntil.isNotBlank()) {
                    InsuranceInfoRow(stringResource(R.string.valid_until), formatInsuranceDate(insurance.validUntil))
                }
                if (insurance.emergencyPhone.isNotBlank()) {
                    InsuranceInfoRow(stringResource(R.string.insurance_claims_phone), insurance.emergencyPhone)
                }
                InsuranceInfoRow(
                    stringResource(R.string.insurance_claims),
                    if (insurance.claims.size == 1) {
                        stringResource(R.string.insurance_claim_count_one)
                    } else {
                        stringResource(R.string.insurance_claim_count, insurance.claims.size)
                    },
                )
                activeClaim?.let {
                    InsuranceInfoRow(
                        stringResource(R.string.insurance_active_claim),
                        listOf(it.claimNumber, it.type).filter { value -> value.isNotBlank() }.joinToString(" · ")
                            .ifBlank { claimStatus(it.status).label },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MpButton(
                    text = stringResource(R.string.insurance_claim_mode),
                    onClick = onPresent,
                    icon = Icons.Filled.Warning,
                    compact = true,
                    maxLines = 1,
                )
                MpButton(
                    text = stringResource(R.string.insurance_claims),
                    onClick = onClaims,
                    variant = MpButtonVariant.Quiet,
                    icon = Icons.Filled.Info,
                    compact = true,
                    maxLines = 1,
                )
                MpButton(
                    text = stringResource(R.string.edit),
                    onClick = onEdit,
                    variant = MpButtonVariant.Quiet,
                    icon = Icons.Filled.Edit,
                    compact = true,
                    maxLines = 1,
                )
                if (item.blobId != null) {
                    MpButton(
                        text = stringResource(R.string.insurance_policy),
                        onClick = onOpenPolicy,
                        variant = MpButtonVariant.Quiet,
                        icon = Icons.Filled.DateRange,
                        compact = true,
                        maxLines = 1,
                    )
                }
                if (shared) {
                    MpButton(
                        text = stringResource(R.string.manage_shares),
                        onClick = onManageShares,
                        variant = MpButtonVariant.Quiet,
                        icon = Icons.Filled.Share,
                        compact = true,
                        maxLines = 1,
                    )
                } else if (hasContacts) {
                    MpButton(
                        text = stringResource(R.string.share),
                        onClick = onShare,
                        variant = MpButtonVariant.Quiet,
                        icon = Icons.Filled.Share,
                        compact = true,
                        maxLines = 1,
                    )
                }
                MpButton(
                    text = stringResource(R.string.delete),
                    onClick = onDelete,
                    variant = MpButtonVariant.Quiet,
                    icon = Icons.Filled.Delete,
                    compact = true,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun InsuranceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MpColors.OnSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MpColors.OnSurface,
            modifier = Modifier.weight(1.4f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InsuranceClaimsDialog(
    item: UiItem,
    claims: List<UiInsuranceClaim>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (UiInsuranceClaim) -> Unit,
    onDelete: (UiInsuranceClaim) -> Unit,
) {
    val insurance = item.insurance ?: return
    var deleteTarget by remember { mutableStateOf<UiInsuranceClaim?>(null) }

    deleteTarget?.let { claim ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_claim_title),
            message = stringResource(R.string.confirm_delete_claim_body),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                onDelete(claim)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }

    MpDialog(
        onDismissRequest = onDismiss,
        maxWidth = 760.dp,
        header = {
            MpDialogHeader(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.insurance_claims),
                subtitle = listOf(item.title, insurance.provider, insurance.policyNumber)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                onClose = onDismiss,
            )
        },
        actions = {
            MpDialogActions(
                primaryLabel = stringResource(R.string.insurance_add_claim),
                onPrimary = onAdd,
                primaryIcon = Icons.Filled.Add,
                secondaryLabel = stringResource(R.string.close),
                onSecondary = onDismiss,
            )
        },
    ) {
        if (claims.isEmpty()) {
            MpEmptyState(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.insurance_no_claims),
                subtitle = stringResource(R.string.insurance_no_claims_help),
            )
        } else {
            claims.sortedByDescending { it.date }.forEach { claim ->
                val status = claimStatus(claim.status)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MpColors.SurfaceLow.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = claim.claimNumber.ifBlank { stringResource(R.string.insurance_claim) },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = listOf(
                                        formatInsuranceDate(claim.date),
                                        claim.type,
                                        claim.amount,
                                    ).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MpColors.OnSurfaceVariant,
                                )
                            }
                            MpBadge(status.label, status.tone)
                        }
                        if (claim.description.isNotBlank()) {
                            Text(
                                text = claim.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MpColors.OnSurface,
                            )
                        }
                        if (claim.adjusterName.isNotBlank() || claim.adjusterPhone.isNotBlank()) {
                            Text(
                                text = listOf(
                                    claim.adjusterName,
                                    claim.adjusterPhone,
                                ).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.OnSurfaceVariant,
                            )
                        }
                        if (claim.notes.isNotBlank()) {
                            Text(
                                text = claim.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.OnSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MpButton(
                                text = stringResource(R.string.edit),
                                onClick = { onEdit(claim) },
                                variant = MpButtonVariant.Quiet,
                                compact = true,
                                maxLines = 1,
                            )
                            MpButton(
                                text = stringResource(R.string.delete),
                                onClick = { deleteTarget = claim },
                                variant = MpButtonVariant.Danger,
                                compact = true,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsuranceClaimDialog(
    claim: UiInsuranceClaim?,
    onDismiss: () -> Unit,
    onSave: (UiInsuranceClaim) -> Unit,
) {
    var date by remember(claim) { mutableStateOf(claim?.date.orEmpty().ifBlank { java.time.LocalDate.now().toString() }) }
    var number by remember(claim) { mutableStateOf(claim?.claimNumber.orEmpty()) }
    var type by remember(claim) { mutableStateOf(claim?.type.orEmpty()) }
    var description by remember(claim) { mutableStateOf(claim?.description.orEmpty()) }
    var status by remember(claim) { mutableStateOf(claim?.status ?: "open") }
    var adjusterName by remember(claim) { mutableStateOf(claim?.adjusterName.orEmpty()) }
    var adjusterPhone by remember(claim) { mutableStateOf(claim?.adjusterPhone.orEmpty()) }
    var amount by remember(claim) { mutableStateOf(claim?.amount.orEmpty()) }
    var notes by remember(claim) { mutableStateOf(claim?.notes.orEmpty()) }

    MpDialog(
        onDismissRequest = onDismiss,
        maxWidth = 680.dp,
        header = {
            MpDialogHeader(
                icon = Icons.Filled.Info,
                title = stringResource(
                    if (claim == null) R.string.insurance_new_claim else R.string.insurance_edit_claim,
                ),
                subtitle = stringResource(R.string.insurance_claim_help),
                onClose = onDismiss,
            )
        },
        actions = {
            MpDialogActions(
                primaryLabel = stringResource(R.string.insurance_save_claim),
                onPrimary = {
                    onSave(
                        UiInsuranceClaim(
                            id = claim?.id ?: java.util.UUID.randomUUID().toString(),
                            date = date,
                            claimNumber = number.trim(),
                            type = type.trim(),
                            description = description.trim(),
                            status = status,
                            adjusterName = adjusterName.trim(),
                            adjusterPhone = adjusterPhone.trim(),
                            amount = amount.trim(),
                            notes = notes.trim(),
                        ),
                    )
                },
                primaryIcon = Icons.Filled.CheckCircle,
                primaryEnabled = number.isNotBlank() || type.isNotBlank() || description.isNotBlank(),
                secondaryLabel = stringResource(R.string.cancel),
                onSecondary = onDismiss,
            )
        },
    ) {
        MpTextField(value = number, onValueChange = { number = it }, label = stringResource(R.string.insurance_report_number))
        MpTextField(value = type, onValueChange = { type = it }, label = stringResource(R.string.insurance_claim_reason))
        MpDateField(value = date, onValueChange = { date = it }, label = stringResource(R.string.insurance_claim_date))
        MpTextField(
            value = description,
            onValueChange = { description = it },
            label = stringResource(R.string.description),
            singleLine = false,
        )
        Text(
            text = stringResource(R.string.insurance_claim_status),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            claimStatusValues.forEach { value ->
                MpFilterChip(claimStatus(value).label, status == value) { status = value }
            }
        }
        MpTextField(value = adjusterName, onValueChange = { adjusterName = it }, label = stringResource(R.string.insurance_adjuster))
        MpTextField(
            value = adjusterPhone,
            onValueChange = { adjusterPhone = it },
            label = stringResource(R.string.insurance_adjuster_phone),
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
        )
        MpTextField(value = amount, onValueChange = { amount = it }, label = stringResource(R.string.insurance_estimated_amount))
        MpTextField(value = notes, onValueChange = { notes = it }, label = stringResource(R.string.notes))
    }
}

@Composable
private fun InsurancePresentationDialog(
    item: UiItem,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)?,
    onOpenPolicy: () -> Unit,
) {
    val insurance = item.insurance ?: return
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val status = insuranceStatus(item)
    val claim = activeInsuranceClaim(insurance)

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
                    text = "${insuranceCategoryLabel(insurance.category)} · ${stringResource(R.string.insurance_claim_mode)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MpColors.Primary,
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = listOf(insurance.provider, insurance.product)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
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

        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (status.expired) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MpColors.SecondaryFixed,
                    ) {
                        Text(
                            text = stringResource(R.string.insurance_expired_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MpColors.OnSecondaryFixed,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (insurance.emergencyPhone.isNotBlank()) {
                        MpButton(
                            text = "${stringResource(R.string.insurance_claims_phone)} · ${insurance.emergencyPhone}",
                            onClick = { dialPhone(context, insurance.emergencyPhone) },
                            icon = Icons.Filled.Phone,
                            modifier = Modifier.weight(1f),
                            fillWidth = true,
                        )
                    }
                    if (insurance.assistancePhone.isNotBlank()) {
                        MpButton(
                            text = "${stringResource(R.string.insurance_assistance)} · ${insurance.assistancePhone}",
                            onClick = { dialPhone(context, insurance.assistancePhone) },
                            variant = MpButtonVariant.Secondary,
                            icon = Icons.Filled.Phone,
                            modifier = Modifier.weight(1f),
                            fillWidth = true,
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MpColors.PrimaryFixed.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MpColors.Primary.copy(alpha = 0.2f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.insurance_policy_number_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MpColors.OnSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = insurance.policyNumber.ifBlank { "—" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MpColors.Primary,
                                modifier = Modifier.weight(1f),
                            )
                            MpButton(
                                text = stringResource(R.string.copy),
                                onClick = {
                                    SensitiveClipboard.copy(
                                        context,
                                        context.getString(R.string.insurance_policy_number_label),
                                        insurance.policyNumber,
                                    )
                                },
                                variant = MpButtonVariant.Secondary,
                                compact = true,
                                maxLines = 1,
                                enabled = insurance.policyNumber.isNotBlank(),
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InsuranceDetailBox(stringResource(R.string.holder), insurance.holder)
                    InsuranceDetailBox(stringResource(R.string.insurance_status), status.label)
                    InsuranceDetailBox(
                        stringResource(R.string.validity),
                        listOf(
                            formatInsuranceDate(insurance.validFrom),
                            formatInsuranceDate(insurance.validUntil),
                        ).filter { it.isNotBlank() }.joinToString(" — "),
                    )
                    if (insurance.memberId.isNotBlank()) {
                        InsuranceDetailBox(stringResource(R.string.insurance_member_number), insurance.memberId)
                    }
                    if (insurance.deductible.isNotBlank()) {
                        InsuranceDetailBox(stringResource(R.string.insurance_deductible), insurance.deductible)
                    }
                    InsuranceDetailBox(
                        stringResource(R.string.insurance_reminder_label),
                        stringResource(R.string.insurance_reminder_days_value, insurance.reminderDays),
                    )
                    if (insurance.insuredSum.isNotBlank()) {
                        InsuranceDetailBox(stringResource(R.string.insurance_insured_sum), insurance.insuredSum)
                    }
                    insurance.subject.filter { it.value.isNotBlank() }.forEach {
                        InsuranceDetailBox(it.label.ifBlank { stringResource(R.string.insurance_detail) }, it.value)
                    }
                    if (insurance.coverages.isNotEmpty()) {
                        InsuranceDetailBox(
                            stringResource(R.string.insurance_coverages),
                            insurance.coverages.joinToString(" · ") {
                                listOf(it.name, it.amount, it.notes)
                                    .filter { value -> value.isNotBlank() }
                                    .joinToString(" ")
                            },
                        )
                    }
                }
            }

            claim?.let {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MpColors.SecondaryFixed.copy(alpha = 0.8f),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.insurance_active_claim),
                                style = MaterialTheme.typography.labelMedium,
                                color = MpColors.OnSecondaryFixed,
                            )
                            Text(
                                text = listOf(
                                    it.claimNumber,
                                    it.type,
                                    claimStatus(it.status).label,
                                ).filter { value -> value.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.titleMedium,
                                color = MpColors.OnSecondaryFixed,
                            )
                            if (it.adjusterPhone.isNotBlank()) {
                                MpButton(
                                    text = listOf(it.adjusterName, it.adjusterPhone)
                                        .filter { value -> value.isNotBlank() }
                                        .joinToString(" · "),
                                    onClick = { dialPhone(context, it.adjusterPhone) },
                                    variant = MpButtonVariant.Secondary,
                                    icon = Icons.Filled.Phone,
                                    compact = true,
                                    fillWidth = true,
                                )
                            }
                        }
                    }
                }
            }

            if (insurance.notes.isNotBlank()) {
                item {
                    InsuranceDetailBox(
                        stringResource(R.string.insurance_what_to_do),
                        insurance.notes,
                    )
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (item.blobId != null) {
                        MpButton(
                            text = stringResource(R.string.insurance_open_policy),
                            onClick = onOpenPolicy,
                            variant = MpButtonVariant.Secondary,
                            compact = true,
                            maxLines = 1,
                        )
                    }
                    MpButton(
                        text = stringResource(R.string.insurance_copy_details),
                        onClick = {
                            SensitiveClipboard.copy(
                                context,
                                item.title,
                                insuranceSummary(context, item),
                            )
                        },
                        variant = MpButtonVariant.Secondary,
                        compact = true,
                        maxLines = 1,
                    )
                    if (onShare != null) {
                        MpButton(
                            text = stringResource(R.string.share),
                            onClick = onShare,
                            variant = MpButtonVariant.Secondary,
                            compact = true,
                            maxLines = 1,
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.insurance_presentation_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InsuranceEmergencyDialog(
    items: List<UiItem>,
    onDismiss: () -> Unit,
    onOpenPolicy: (UiItem) -> Unit,
) {
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
                    text = stringResource(R.string.insurance_emergency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.insurance_emergency_help),
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
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (items.isEmpty()) {
                item {
                    MpEmptyState(
                        icon = Icons.Filled.Lock,
                        title = stringResource(R.string.insurance_no_phones),
                    )
                }
            }
            items.sortedWith(
                compareBy<UiItem> { insuranceCategoryOrder(it.insurance?.category) }
                    .thenBy { it.title.lowercase() },
            ).forEach { policyItem ->
                val insurance = policyItem.insurance ?: return@forEach
                item {
                    val status = insuranceStatus(policyItem)
                    MpCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${insuranceCategoryLabel(insurance.category)} · ${status.label}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MpColors.Primary,
                                    )
                                    Text(
                                        text = policyItem.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = listOf(insurance.provider, insurance.policyNumber)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MpColors.OnSurfaceVariant,
                                    )
                                }
                                MpButton(
                                    text = stringResource(R.string.insurance_view_policy),
                                    onClick = { onOpenPolicy(policyItem) },
                                    variant = MpButtonVariant.Secondary,
                                    compact = true,
                                    maxLines = 1,
                                )
                            }
                            if (insurance.emergencyPhone.isNotBlank()) {
                                MpButton(
                                    text = "${stringResource(R.string.insurance_claims_phone)} · ${insurance.emergencyPhone}",
                                    onClick = { dialPhone(context, insurance.emergencyPhone) },
                                    icon = Icons.Filled.Phone,
                                    fillWidth = true,
                                    maxLines = 1,
                                )
                            }
                            if (insurance.assistancePhone.isNotBlank()) {
                                MpButton(
                                    text = "${stringResource(R.string.insurance_assistance)} · ${insurance.assistancePhone}",
                                    onClick = { dialPhone(context, insurance.assistancePhone) },
                                    variant = MpButtonVariant.Secondary,
                                    icon = Icons.Filled.Phone,
                                    fillWidth = true,
                                    maxLines = 1,
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
private fun InsuranceDetailBox(label: String, value: String) {
    if (value.isBlank()) {
        return
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MpColors.SurfaceLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MpColors.OnSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun insuranceStatus(item: UiItem): InsuranceStatus {
    val insurance = item.insurance ?: return InsuranceStatus(
        stringResource(R.string.insurance_status_active),
        MpBadgeTone.Primary,
        false,
    )
    val expired = insurance.status == "expired" ||
        isExpired(item) ||
        runCatching { java.time.LocalDate.parse(insurance.validUntil.take(10)) }.getOrNull()
            ?.isBefore(java.time.LocalDate.now()) == true
    return when {
        insurance.status == "cancelled" -> InsuranceStatus(
            stringResource(R.string.insurance_status_cancelled),
            MpBadgeTone.Neutral,
            true,
        )
        expired -> InsuranceStatus(
            stringResource(R.string.insurance_status_expired),
            MpBadgeTone.Danger,
            true,
        )
        insurance.status == "pending" -> InsuranceStatus(
            stringResource(R.string.insurance_status_pending),
            MpBadgeTone.Warning,
            false,
        )
        else -> InsuranceStatus(
            stringResource(R.string.insurance_status_active),
            MpBadgeTone.Primary,
            false,
        )
    }
}

@Composable
internal fun insuranceCategoryLabel(category: String): String = stringResource(
    when (category) {
        "health" -> R.string.insurance_category_health
        "major_medical" -> R.string.insurance_category_major_medical
        "auto" -> R.string.insurance_category_auto
        "motorcycle" -> R.string.insurance_category_motorcycle
        "home" -> R.string.insurance_category_home
        "life" -> R.string.insurance_category_life
        "travel" -> R.string.insurance_category_travel
        "pet" -> R.string.insurance_category_pet
        "business" -> R.string.insurance_category_business
        else -> R.string.insurance_category_other
    },
)

@Composable
internal fun insuranceStatusLabel(status: String): String = stringResource(
    when (status) {
        "pending" -> R.string.insurance_status_pending
        "expired" -> R.string.insurance_status_expired
        "cancelled" -> R.string.insurance_status_cancelled
        else -> R.string.insurance_status_active
    },
)

@Composable
internal fun paymentFrequencyLabel(value: String): String = stringResource(
    when (value) {
        "monthly" -> R.string.insurance_frequency_monthly
        "quarterly" -> R.string.insurance_frequency_quarterly
        "semiannual" -> R.string.insurance_frequency_semiannual
        "annual" -> R.string.insurance_frequency_annual
        "single" -> R.string.insurance_frequency_single
        else -> R.string.insurance_frequency_none
    },
)

private fun insuranceCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.CheckCircle

private fun insuranceCategoryOrder(category: String?): Int = when (category) {
    "auto" -> 0
    "motorcycle" -> 1
    "major_medical" -> 2
    "health" -> 3
    "home" -> 4
    "travel" -> 5
    "life" -> 6
    "pet" -> 7
    "business" -> 8
    else -> 9
}

@Composable
private fun claimStatus(status: String): ClaimStatus = when (status) {
    "review" -> ClaimStatus(stringResource(R.string.insurance_claim_review), MpBadgeTone.Primary)
    "approved" -> ClaimStatus(stringResource(R.string.insurance_claim_approved), MpBadgeTone.Primary)
    "rejected" -> ClaimStatus(stringResource(R.string.insurance_claim_rejected), MpBadgeTone.Danger)
    "closed" -> ClaimStatus(stringResource(R.string.insurance_claim_closed), MpBadgeTone.Neutral)
    else -> ClaimStatus(stringResource(R.string.insurance_claim_open), MpBadgeTone.Warning)
}

private fun activeInsuranceClaim(insurance: UiInsurance): UiInsuranceClaim? {
    return insurance.claims
        .filter { it.status == "open" || it.status == "review" }
        .sortedByDescending { it.date }
        .firstOrNull()
}

private fun formatInsuranceDate(value: String): String {
    if (value.isBlank()) {
        return ""
    }
    return runCatching {
        java.time.LocalDate.parse(value.take(10))
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.getDefault()))
    }.getOrDefault(value)
}

private fun dialPhone(context: android.content.Context, phone: String) {
    val sanitized = phone.replace(Regex("[^0-9+*#]"), "")
    if (sanitized.isBlank()) {
        return
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")))
    }
}

private fun insuranceSummary(context: android.content.Context, item: UiItem): String {
    val insurance = item.insurance ?: return item.title
    return listOf(
        item.title,
        insurance.provider,
        insurance.product,
        insurance.policyNumber.takeIf { it.isNotBlank() }
            ?.let { "${context.getString(R.string.insurance_policy_number_label)}: $it" },
        insurance.holder.takeIf { it.isNotBlank() }
            ?.let { "${context.getString(R.string.holder)}: $it" },
        insurance.validUntil.takeIf { it.isNotBlank() }
            ?.let { "${context.getString(R.string.validity)}: ${formatInsuranceDate(it)}" },
        insurance.emergencyPhone.takeIf { it.isNotBlank() }
            ?.let { "${context.getString(R.string.insurance_claims_phone)}: $it" },
        insurance.assistancePhone.takeIf { it.isNotBlank() }
            ?.let { "${context.getString(R.string.insurance_assistance)}: $it" },
        *insurance.subject.map { "${it.label}: ${it.value}" }.toTypedArray(),
    ).filterNotNull().filter { it.isNotBlank() }.joinToString("\n")
}

internal val insuranceCategoryValues = listOf(
    "auto",
    "motorcycle",
    "major_medical",
    "health",
    "home",
    "travel",
    "life",
    "pet",
    "business",
    "other",
)

internal val insuranceStatusValues = listOf(
    "active",
    "pending",
    "expired",
    "cancelled",
)

internal val paymentFrequencyValues = listOf(
    "",
    "monthly",
    "quarterly",
    "semiannual",
    "annual",
    "single",
)

private val claimStatusValues = listOf(
    "open",
    "review",
    "approved",
    "rejected",
    "closed",
)
