package app.mypass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FamilyScreen(state: UiState, viewModel: MainViewModel) {
    var blockContact by remember { mutableStateOf<UiContact?>(null) }
    var showAddContact by remember { mutableStateOf(false) }
    var showNewRequest by remember { mutableStateOf(false) }
    var manageShares by remember { mutableStateOf<Pair<String, String>?>(null) }
    var search by remember { mutableStateOf("") }
    var sentFilter by remember { mutableStateOf("all") }
    val acceptedContacts = state.contacts.filter { it.status == "accepted" }
    val incomingShares = state.incomingShares.filter { !it.isOutgoing && it.status == "pending" }
    val outgoingShares = state.outgoingShares
    val incomingRequests = state.shareRequests.filter { it.incoming }
    val outgoingRequests = state.shareRequests.filter { !it.incoming }
    val currentTab = state.familyTab.ifBlank { "contacts" }
    val query = search.trim()

    if (blockContact != null) {
        ConfirmDialog(
            title = stringResource(R.string.confirm_block_contact_title),
            message = stringResource(R.string.confirm_block_contact_body),
            confirmLabel = stringResource(R.string.block_contact),
            onConfirm = {
                viewModel.blockContact(blockContact!!.id)
                blockContact = null
            },
            onDismiss = { blockContact = null },
            icon = Icons.Filled.Warning,
        )
    }

    if (showAddContact) {
        AddContactDialog(
            onDismiss = { showAddContact = false },
            onAdd = { email ->
                viewModel.addContact(email)
                showAddContact = false
            },
        )
    }

    if (showNewRequest) {
        NewShareRequestDialog(
            contacts = acceptedContacts,
            onDismiss = { showNewRequest = false },
            onSend = { contactId, documentType, purpose ->
                viewModel.createShareRequest(contactId, documentType, purpose)
                showNewRequest = false
            },
        )
    }

    manageShares?.let { (itemId, title) ->
        ManageSharesDialog(
            itemId = itemId,
            itemTitle = title,
            shares = outgoingShares,
            canShare = false,
            onDismiss = { manageShares = null },
            onShare = { manageShares = null },
            onRevoke = { viewModel.revokeShare(it) },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(R.string.family),
            onBack = viewModel::backToHome,
            action = {
                MpIconButton(
                    icon = Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.lock),
                    onClick = viewModel::lock,
                    container = MpColors.SurfaceLow,
                )
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.family_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MpColors.OnSurfaceVariant,
                    )
                    MpStatusBanner(state.familyStatus, state.error, modifier = Modifier.padding(top = 4.dp))
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FamilyTabTile(
                            label = stringResource(R.string.contacts),
                            count = acceptedContacts.size,
                            icon = Icons.Filled.Person,
                            selected = currentTab == "contacts",
                            onClick = { viewModel.setFamilyTab("contacts") },
                            modifier = Modifier.weight(1f),
                        )
                        FamilyTabTile(
                            label = stringResource(R.string.received),
                            count = incomingShares.size,
                            icon = Icons.Filled.Email,
                            selected = currentTab == "received",
                            onClick = { viewModel.setFamilyTab("received") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FamilyTabTile(
                            label = stringResource(R.string.outgoing_shares),
                            count = outgoingShares.count { it.status == "pending" || it.status == "accepted" },
                            icon = Icons.Filled.Send,
                            selected = currentTab == "sent",
                            onClick = { viewModel.setFamilyTab("sent") },
                            modifier = Modifier.weight(1f),
                        )
                        FamilyTabTile(
                            label = stringResource(R.string.requests),
                            count = incomingRequests.size + outgoingRequests.size,
                            icon = Icons.Filled.List,
                            selected = currentTab == "requests",
                            onClick = { viewModel.setFamilyTab("requests") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                MpTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = when (currentTab) {
                        "received" -> stringResource(R.string.search_received)
                        "sent" -> stringResource(R.string.search_sent)
                        "requests" -> stringResource(R.string.search_requests)
                        else -> stringResource(R.string.search_contacts)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when (currentTab) {
                "received" -> {
                    item {
                        SectionHeaderWithAction(
                            title = stringResource(R.string.shared_with_me),
                            count = incomingShares.size,
                            actionLabel = null,
                            onAction = null,
                        )
                    }
                    val filtered = incomingShares.filter { share ->
                        query.isBlank() ||
                            share.sender.contains(query, ignoreCase = true) ||
                            share.itemType.contains(query, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        item {
                            MpEmptyState(
                                icon = Icons.Filled.Email,
                                title = if (incomingShares.isEmpty()) {
                                    stringResource(R.string.no_shares)
                                } else {
                                    stringResource(R.string.no_search_results, query)
                                },
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { share ->
                            ShareCard(
                                share = share,
                                onAccept = { viewModel.acceptShare(share.id) },
                                onDecline = { viewModel.declineShare(share.id) },
                            )
                        }
                    }
                }

                "sent" -> {
                    item {
                        SectionHeaderWithAction(
                            title = stringResource(R.string.outgoing_shares),
                            count = outgoingShares.size,
                            actionLabel = null,
                            onAction = null,
                        )
                    }
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            MpCompactFilterChip(stringResource(R.string.all), sentFilter == "all") { sentFilter = "all" }
                            MpCompactFilterChip(stringResource(R.string.pending), sentFilter == "pending") { sentFilter = "pending" }
                            MpCompactFilterChip(stringResource(R.string.share_status_accepted), sentFilter == "accepted") { sentFilter = "accepted" }
                            MpCompactFilterChip(stringResource(R.string.share_status_declined), sentFilter == "declined") { sentFilter = "declined" }
                        }
                    }
                    val filtered = outgoingShares.filter { share ->
                        val matchesStatus = sentFilter == "all" || share.status == sentFilter
                        val matchesQuery = query.isBlank() ||
                            share.sender.contains(query, ignoreCase = true) ||
                            (state.items.firstOrNull { it.id == share.itemId }?.title
                                ?.contains(query, ignoreCase = true) == true)
                        matchesStatus && matchesQuery
                    }
                    if (filtered.isEmpty()) {
                        item {
                            MpEmptyState(
                                icon = Icons.Filled.Send,
                                title = when {
                                    outgoingShares.isEmpty() -> stringResource(R.string.no_outgoing_shares)
                                    query.isNotBlank() -> stringResource(R.string.no_search_results, query)
                                    else -> stringResource(R.string.no_sent_filter_results)
                                },
                                subtitle = if (outgoingShares.isEmpty()) {
                                    stringResource(R.string.no_outgoing_shares_help)
                                } else {
                                    null
                                },
                            )
                        }
                    } else {
                        items(filtered, key = { "out-${it.id}" }) { share ->
                            val itemTitle = state.items.firstOrNull { it.id == share.itemId }?.title
                            OutgoingShareCard(
                                share = share,
                                itemTitle = itemTitle,
                                onCancel = { viewModel.revokeShare(share.id) },
                                onManage = if (share.itemId != null) {
                                    {
                                        manageShares = share.itemId to (itemTitle ?: share.sender)
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }

                "requests" -> {
                    item {
                        SectionHeaderWithAction(
                            title = stringResource(R.string.requests),
                            count = incomingRequests.size + outgoingRequests.size,
                            actionLabel = stringResource(R.string.new_request),
                            onAction = {
                                if (acceptedContacts.isEmpty()) {
                                    viewModel.setFamilyTab("contacts")
                                } else {
                                    showNewRequest = true
                                }
                            },
                        )
                    }
                    if (incomingRequests.isEmpty() && outgoingRequests.isEmpty()) {
                        item {
                            MpEmptyState(
                                icon = Icons.Filled.List,
                                title = stringResource(R.string.no_requests),
                            )
                        }
                    } else {
                        if (incomingRequests.isNotEmpty()) {
                            item { FamilySubheading(stringResource(R.string.request_incoming_badge)) }
                            items(incomingRequests, key = { "req-in-${it.id}" }) { request ->
                                ShareRequestCard(
                                    request = request,
                                    onDecline = { viewModel.declineShareRequest(request.id) },
                                    onCancel = {},
                                )
                            }
                        }
                        if (outgoingRequests.isNotEmpty()) {
                            item { FamilySubheading(stringResource(R.string.request_outgoing_badge)) }
                            items(outgoingRequests, key = { "req-out-${it.id}" }) { request ->
                                ShareRequestCard(
                                    request = request,
                                    onDecline = {},
                                    onCancel = { viewModel.cancelShareRequest(request.id) },
                                )
                            }
                        }
                    }
                }

                else -> {
                    item {
                        SectionHeaderWithAction(
                            title = stringResource(R.string.contacts),
                            count = state.contacts.size,
                            actionLabel = stringResource(R.string.add),
                            onAction = { showAddContact = true },
                        )
                    }
                    val filtered = state.contacts.filter { contact ->
                        query.isBlank() ||
                            contact.name.contains(query, ignoreCase = true) ||
                            contact.email.contains(query, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        item {
                            MpEmptyState(
                                icon = Icons.Filled.Person,
                                title = if (state.contacts.isEmpty()) {
                                    stringResource(R.string.no_contacts)
                                } else {
                                    stringResource(R.string.no_search_results, query)
                                },
                            )
                        }
                    } else {
                        items(filtered, key = { it.id }) { contact ->
                            ContactCard(
                                contact = contact,
                                onAccept = { viewModel.acceptContact(contact.id) },
                                onBlock = { blockContact = contact },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyTabTile(
    label: String,
    count: Int,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = if (selected) MpColors.Primary else MpColors.Surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MpColors.Primary else MpColors.Border,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MpColors.OnPrimary else MpColors.Primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MpColors.OnPrimary else MpColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MpColors.OnPrimary.copy(alpha = 0.8f) else MpColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionHeaderWithAction(
    title: String,
    count: Int,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (count > 0) {
            MpBadge(text = count.toString(), tone = MpBadgeTone.Neutral)
        }
        if (actionLabel != null && onAction != null) {
            MpButton(
                text = actionLabel,
                onClick = onAction,
                variant = MpButtonVariant.Secondary,
                compact = true,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FamilySubheading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MpColors.OnSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var email by remember { mutableStateOf("") }

    MpDialog(
        onDismissRequest = onDismiss,
        header = {
            MpDialogHeader(
                icon = Icons.Filled.Person,
                title = stringResource(R.string.add_contact),
                subtitle = stringResource(R.string.add_contact_hint),
                onClose = onDismiss,
            )
        },
        actions = {
            MpButton(
                text = stringResource(R.string.add_contact),
                onClick = { onAdd(email.trim()) },
                enabled = email.isNotBlank(),
                icon = Icons.Filled.Add,
                fillWidth = true,
            )
            MpButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                variant = MpButtonVariant.Secondary,
                fillWidth = true,
            )
        },
    ) {
        MpTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.family_email),
            keyboardType = KeyboardType.Email,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewShareRequestDialog(
    contacts: List<UiContact>,
    onDismiss: () -> Unit,
    onSend: (String, String, String) -> Unit,
) {
    var contactId by remember { mutableStateOf(contacts.firstOrNull()?.id.orEmpty()) }
    var type by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }

    MpDialog(
        onDismissRequest = onDismiss,
        header = {
            MpDialogHeader(
                icon = Icons.Filled.Email,
                title = stringResource(R.string.new_request),
                subtitle = stringResource(R.string.request_from),
                onClose = onDismiss,
            )
        },
        actions = {
            MpButton(
                text = stringResource(R.string.request),
                onClick = { onSend(contactId, type.trim(), purpose.trim()) },
                enabled = contactId.isNotBlank() && type.isNotBlank() && purpose.isNotBlank(),
                icon = Icons.Filled.Send,
                fillWidth = true,
            )
            MpButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                variant = MpButtonVariant.Secondary,
                fillWidth = true,
            )
        },
    ) {
        if (contacts.isEmpty()) {
            MpCallout(text = stringResource(R.string.request_needs_contact), tone = MpCalloutTone.Info)
        } else {
            Text(stringResource(R.string.contact), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                contacts.forEach { contact ->
                    MpFilterChip(
                        text = contact.name,
                        selected = contactId == contact.id,
                        onClick = { contactId = contact.id },
                    )
                }
            }
            MpTextField(
                value = type,
                onValueChange = { type = it },
                label = stringResource(R.string.request_document),
            )
            MpTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = stringResource(R.string.request_purpose),
            )
        }
    }
}
