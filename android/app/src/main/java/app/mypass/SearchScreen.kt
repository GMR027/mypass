package app.mypass

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun SearchScreen(state: UiState, viewModel: MainViewModel) {
    var query by remember { mutableStateOf("") }
    val hits = remember(query, state.items) {
        if (query.isBlank()) {
            VaultSearch.recent(state.items, state.recentItems.map { it.id })
        } else {
            VaultSearch.search(state.items, query)
        }
    }
    val showingRecent = query.isBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(R.string.search_vault),
            onBack = viewModel::closeSearch,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                MpTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(R.string.search_vault),
                    placeholder = stringResource(R.string.search_vault_placeholder),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            showingRecent -> stringResource(R.string.search_recent)
                            hits.isEmpty() -> stringResource(R.string.search_no_results)
                            hits.size == 1 -> stringResource(R.string.search_one_result)
                            else -> stringResource(R.string.search_results, hits.size)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MpColors.OnSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (showingRecent && state.recentItems.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearRecentItems) {
                            Text(stringResource(R.string.search_clear_recent))
                        }
                    }
                }
            }

            if (hits.isEmpty()) {
                item {
                    MpEmptyState(
                        icon = Icons.Filled.Search,
                        title = if (showingRecent) {
                            stringResource(R.string.search_empty_recent)
                        } else {
                            stringResource(R.string.search_no_results)
                        },
                        subtitle = if (showingRecent) {
                            stringResource(R.string.search_empty_recent_help)
                        } else {
                            stringResource(R.string.search_try_other)
                        },
                    )
                }
            } else {
                items(hits, key = { "${it.section}-${it.item.id}" }) { hit ->
                    SearchResultRow(
                        hit = hit,
                        onClick = {
                            when (hit.section) {
                                "pass" -> viewModel.openPassItem(hit.item.id)
                                "credential" -> viewModel.openCredentialItem(hit.item.id)
                                "medical" -> viewModel.openMedicalItem(hit.item.id)
                                "insurance" -> viewModel.openInsuranceItem(hit.item.id)
                                else -> viewModel.openItem(hit.item)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(hit: SearchHit, onClick: () -> Unit) {
    MpCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MpIconTile(
                icon = when (hit.section) {
                    "pass" -> Icons.Filled.DateRange
                    "credential" -> Icons.Filled.Lock
                    "medical" -> Icons.Filled.Person
                    "insurance" -> Icons.Filled.CheckCircle
                    else -> Icons.Filled.Info
                },
                size = 44,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = sectionLabel(hit.section),
                    style = MaterialTheme.typography.labelSmall,
                    color = MpColors.Primary,
                )
                Text(
                    text = hit.item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = searchSubtitle(hit)
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun sectionLabel(section: String): String = stringResource(
    when (section) {
        "pass" -> R.string.trash_type_pass
        "credential" -> R.string.trash_type_credential
        "medical" -> R.string.trash_type_medical
        "insurance" -> R.string.trash_type_insurance
        else -> R.string.trash_type_document
    },
)

private fun searchSubtitle(hit: SearchHit): String {
    val item = hit.item
    return when (hit.section) {
        "pass" -> listOfNotNull(
            item.pass?.issuer?.takeIf { it.isNotBlank() },
            item.pass?.holder?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        "credential" -> listOfNotNull(
            item.credential?.username?.takeIf { it.isNotBlank() },
            item.credential?.url?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        "medical" -> listOfNotNull(
            item.medical?.holder?.takeIf { it.isNotBlank() },
            item.medical?.insurance?.provider?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        "insurance" -> listOfNotNull(
            item.insurance?.provider?.takeIf { it.isNotBlank() },
            item.insurance?.policyNumber?.takeIf { it.isNotBlank() }?.let { "#$it" },
            item.insurance?.holder?.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
        else -> listOfNotNull(
            item.originalName.takeIf { it.isNotBlank() && it != "documento" },
        ).joinToString(" · ")
    }
}
