package app.mypass

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun InsuranceEditorScreen(state: UiState, viewModel: MainViewModel) {
    val source = state.insuranceDraft ?: return

    var title by remember(source) { mutableStateOf(source.title) }
    var category by remember(source) { mutableStateOf(source.category) }
    var provider by remember(source) { mutableStateOf(source.provider) }
    var product by remember(source) { mutableStateOf(source.product) }
    var policyNumber by remember(source) { mutableStateOf(source.policyNumber) }
    var memberId by remember(source) { mutableStateOf(source.memberId) }
    var holder by remember(source) { mutableStateOf(source.holder) }
    var validFrom by remember(source) { mutableStateOf(source.validFrom) }
    var validUntil by remember(source) { mutableStateOf(source.validUntil) }
    var status by remember(source) { mutableStateOf(source.status) }
    var emergencyPhone by remember(source) { mutableStateOf(source.emergencyPhone) }
    var assistancePhone by remember(source) { mutableStateOf(source.assistancePhone) }
    var agentName by remember(source) { mutableStateOf(source.agentName) }
    var agentPhone by remember(source) { mutableStateOf(source.agentPhone) }
    var email by remember(source) { mutableStateOf(source.email) }
    var website by remember(source) { mutableStateOf(source.website) }
    var premium by remember(source) { mutableStateOf(source.premium) }
    var currency by remember(source) { mutableStateOf(source.currency) }
    var paymentFrequency by remember(source) { mutableStateOf(source.paymentFrequency) }
    var nextPaymentDate by remember(source) { mutableStateOf(source.nextPaymentDate) }
    var insuredSum by remember(source) { mutableStateOf(source.insuredSum) }
    var deductible by remember(source) { mutableStateOf(source.deductible) }
    var subject by remember(source) { mutableStateOf(source.subject) }
    var coverages by remember(source) { mutableStateOf(source.coverages) }
    var notes by remember(source) { mutableStateOf(source.notes) }
    var reminderDays by remember(source) { mutableStateOf(source.reminderDays) }
    var attachmentUri by remember(source) { mutableStateOf(source.attachmentUri) }
    var attachmentName by remember(source) { mutableStateOf(source.attachmentName ?: source.existingAttachmentName) }
    var clearAttachment by remember(source) { mutableStateOf(source.clearAttachment) }
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            attachmentUri = uri.toString()
            attachmentName = uri.lastPathSegment?.substringAfterLast('/')
                ?: context.getString(R.string.insurance_attachment)
            clearAttachment = false
        }
    }

    val draft = InsuranceDraft(
        itemId = source.itemId,
        version = source.version,
        title = title,
        category = category,
        provider = provider,
        product = product,
        policyNumber = policyNumber,
        memberId = memberId,
        holder = holder,
        validFrom = validFrom,
        validUntil = validUntil,
        status = status,
        emergencyPhone = emergencyPhone,
        assistancePhone = assistancePhone,
        agentName = agentName,
        agentPhone = agentPhone,
        email = email,
        website = website,
        premium = premium,
        currency = currency,
        paymentFrequency = paymentFrequency,
        nextPaymentDate = nextPaymentDate,
        insuredSum = insuredSum,
        deductible = deductible,
        subject = subject,
        coverages = coverages,
        notes = notes,
        claims = source.claims,
        reminderDays = reminderDays,
        existingBlobId = source.existingBlobId,
        existingAttachmentName = source.existingAttachmentName,
        attachmentUri = attachmentUri,
        attachmentName = attachmentName,
        clearAttachment = clearAttachment,
        isNew = source.isNew,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(if (source.isNew) R.string.new_insurance else R.string.edit_insurance),
            onBack = viewModel::discardInsuranceDraft,
            action = {
                TextButton(
                    onClick = { viewModel.saveInsurance(draft) },
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
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.insurance_identity),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        MpTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = stringResource(R.string.insurance_name),
                            placeholder = stringResource(R.string.insurance_name_hint),
                        )
                        Text(
                            text = stringResource(R.string.insurance_type),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            insuranceCategoryValues.forEach { value ->
                                MpFilterChip(insuranceCategoryLabel(value), category == value) {
                                    category = value
                                    if (subject.all { it.value.isBlank() }) {
                                        subject = viewModel.insuranceTemplate(value)
                                    }
                                }
                            }
                        }
                        MpTextField(
                            value = provider,
                            onValueChange = { provider = it },
                            label = stringResource(R.string.insurance_provider),
                        )
                        MpTextField(
                            value = product,
                            onValueChange = { product = it },
                            label = stringResource(R.string.insurance_product),
                        )
                        MpTextField(
                            value = policyNumber,
                            onValueChange = { policyNumber = it },
                            label = stringResource(R.string.policy_number),
                        )
                        MpTextField(
                            value = memberId,
                            onValueChange = { memberId = it },
                            label = stringResource(R.string.insurance_member_number),
                        )
                        MpTextField(
                            value = holder,
                            onValueChange = { holder = it },
                            label = stringResource(R.string.holder),
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.insurance_validity_and_status),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            insuranceStatusValues.forEach { value ->
                                MpFilterChip(insuranceStatusLabel(value), status == value) { status = value }
                            }
                        }
                        MpDateField(value = validFrom, onValueChange = { validFrom = it }, label = stringResource(R.string.valid_from))
                        MpDateField(value = validUntil, onValueChange = { validUntil = it }, label = stringResource(R.string.valid_until))
                        MpTextField(
                            value = emergencyPhone,
                            onValueChange = { emergencyPhone = it },
                            label = stringResource(R.string.insurance_claims_phone),
                            keyboardType = KeyboardType.Phone,
                        )
                        MpTextField(
                            value = assistancePhone,
                            onValueChange = { assistancePhone = it },
                            label = stringResource(R.string.insurance_assistance_phone),
                            keyboardType = KeyboardType.Phone,
                        )
                        MpTextField(
                            value = agentName,
                            onValueChange = { agentName = it },
                            label = stringResource(R.string.insurance_agent),
                        )
                        MpTextField(
                            value = agentPhone,
                            onValueChange = { agentPhone = it },
                            label = stringResource(R.string.insurance_agent_phone),
                            keyboardType = KeyboardType.Phone,
                        )
                        MpTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.insurance_email),
                            keyboardType = KeyboardType.Email,
                        )
                        MpTextField(
                            value = website,
                            onValueChange = { website = it },
                            label = stringResource(R.string.insurance_website),
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.insurance_insured_data),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(R.string.insurance_insured_data_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MpColors.OnSurfaceVariant,
                                )
                            }
                            MpButton(
                                text = stringResource(R.string.insurance_add_detail),
                                onClick = {
                                    subject = subject + UiInsuranceSubject(
                                        id = java.util.UUID.randomUUID().toString(),
                                    )
                                },
                                variant = MpButtonVariant.Secondary,
                                compact = true,
                                maxLines = 1,
                            )
                        }
                        subject.forEachIndexed { index, entry ->
                            InsuranceSubjectRow(
                                entry = entry,
                                onChange = { updated ->
                                    subject = subject.toMutableList().also { it[index] = updated }
                                },
                                onRemove = {
                                    subject = subject.filterIndexed { itemIndex, _ -> itemIndex != index }
                                },
                            )
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.insurance_coverages),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(R.string.insurance_coverages_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MpColors.OnSurfaceVariant,
                                )
                            }
                            MpButton(
                                text = stringResource(R.string.insurance_add_coverage),
                                onClick = {
                                    coverages = coverages + UiInsuranceCoverage(
                                        id = java.util.UUID.randomUUID().toString(),
                                    )
                                },
                                variant = MpButtonVariant.Secondary,
                                compact = true,
                                maxLines = 1,
                            )
                        }
                        coverages.forEachIndexed { index, coverage ->
                            InsuranceCoverageRow(
                                coverage = coverage,
                                onChange = { updated ->
                                    coverages = coverages.toMutableList().also { it[index] = updated }
                                },
                                onRemove = {
                                    coverages = coverages.filterIndexed { itemIndex, _ -> itemIndex != index }
                                },
                            )
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.insurance_payment),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MpTextField(
                                value = premium,
                                onValueChange = { premium = it },
                                label = stringResource(R.string.insurance_premium),
                                modifier = Modifier.weight(1.4f),
                                keyboardType = KeyboardType.Decimal,
                            )
                            MpTextField(
                                value = currency,
                                onValueChange = { currency = it },
                                label = stringResource(R.string.insurance_currency),
                                modifier = Modifier.weight(0.8f),
                            )
                        }
                        Text(
                            text = stringResource(R.string.insurance_frequency),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            paymentFrequencyValues.forEach { value ->
                                MpFilterChip(paymentFrequencyLabel(value), paymentFrequency == value) {
                                    paymentFrequency = value
                                }
                            }
                        }
                        MpDateField(
                            value = nextPaymentDate,
                            onValueChange = { nextPaymentDate = it },
                            label = stringResource(R.string.insurance_next_payment),
                        )
                        MpTextField(
                            value = insuredSum,
                            onValueChange = { insuredSum = it },
                            label = stringResource(R.string.insurance_insured_sum),
                        )
                        MpTextField(
                            value = deductible,
                            onValueChange = { deductible = it },
                            label = stringResource(R.string.insurance_deductible),
                        )
                        Text(
                            text = stringResource(R.string.insurance_reminder_days),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            insuranceReminderDays.forEach { days ->
                                MpFilterChip(
                                    stringResource(R.string.insurance_reminder_days_value, days),
                                    reminderDays == days,
                                ) { reminderDays = days }
                            }
                        }
                        Text(
                            text = stringResource(R.string.insurance_reminder_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.insurance_notes_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        MpTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = stringResource(R.string.insurance_notes_hint),
                            singleLine = false,
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.insurance_attachment),
                            style = MaterialTheme.typography.titleMedium,
                        )
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
                            text = stringResource(R.string.insurance_attachment_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                    }
                }
            }

            item {
                MpButton(
                    text = stringResource(R.string.save_insurance),
                    onClick = { viewModel.saveInsurance(draft) },
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
private fun InsuranceSubjectRow(
    entry: UiInsuranceSubject,
    onChange: (UiInsuranceSubject) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MpColors.SurfaceLow.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MpTextField(
                    value = entry.label,
                    onValueChange = { onChange(entry.copy(label = it)) },
                    label = stringResource(R.string.insurance_detail_label),
                    modifier = Modifier.weight(1f),
                )
                MpTextField(
                    value = entry.value,
                    onValueChange = { onChange(entry.copy(value = it)) },
                    label = stringResource(R.string.insurance_detail_value),
                    modifier = Modifier.weight(1.2f),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                MpButton(
                    text = stringResource(R.string.remove),
                    onClick = onRemove,
                    variant = MpButtonVariant.Quiet,
                    compact = true,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun InsuranceCoverageRow(
    coverage: UiInsuranceCoverage,
    onChange: (UiInsuranceCoverage) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MpColors.SurfaceLow.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MpTextField(
                value = coverage.name,
                onValueChange = { onChange(coverage.copy(name = it)) },
                label = stringResource(R.string.insurance_coverage),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MpTextField(
                    value = coverage.amount,
                    onValueChange = { onChange(coverage.copy(amount = it)) },
                    label = stringResource(R.string.insurance_amount_limit),
                    modifier = Modifier.weight(1f),
                )
                MpTextField(
                    value = coverage.notes,
                    onValueChange = { onChange(coverage.copy(notes = it)) },
                    label = stringResource(R.string.notes),
                    modifier = Modifier.weight(1.2f),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                MpButton(
                    text = stringResource(R.string.remove),
                    onClick = onRemove,
                    variant = MpButtonVariant.Quiet,
                    compact = true,
                    maxLines = 1,
                )
            }
        }
    }
}

private val insuranceReminderDays = listOf(1, 7, 15, 30)
