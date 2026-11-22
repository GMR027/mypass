package app.mypass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class PassFieldDefinition(
    val key: String,
    val label: String,
    val placeholder: String = "",
)

private val themeColors = listOf(
    "midnight" to "#0c2340",
    "violet" to "#4c1d95",
    "forest" to "#064e3b",
    "obsidian" to "#18181b",
    "burgundy" to "#7f1d1d",
    "amber" to "#9a3412",
)

@Composable
fun PassEditorScreen(state: UiState, viewModel: MainViewModel) {
    val source = state.passDraft ?: return
    var category by remember(source) { mutableStateOf(source.category) }
    var title by remember(source) { mutableStateOf(source.title) }
    var issuer by remember(source) { mutableStateOf(source.issuer) }
    var holder by remember(source) { mutableStateOf(source.holder) }
    var fields by remember(source) { mutableStateOf(source.fields) }
    var notes by remember(source) { mutableStateOf(source.notes) }
    var noteInput by remember { mutableStateOf("") }
    var codePayload by remember(source) { mutableStateOf(source.codePayload) }
    var codeFormat by remember(source) { mutableStateOf(source.codeFormat) }
    var themeColor by remember(source) { mutableStateOf(source.themeColor) }
    var attachSource by remember(source) { mutableStateOf(source.attachSource) }

    val draft = source.copy(
        category = category,
        title = title,
        issuer = issuer,
        holder = holder,
        fields = fields,
        notes = notes,
        codePayload = codePayload,
        codeFormat = codeFormat,
        themeColor = themeColor,
        attachSource = attachSource,
    )
    val definitions = fieldDefinitions(category)

    Column(modifier = Modifier.fillMaxSize()) {
        SubScreenHeader(
            title = stringResource(R.string.pass_editor_title),
            onBack = viewModel::discardPassDraft,
            action = {
                TextButton(
                    onClick = { viewModel.savePass(draft) },
                    enabled = !state.busy,
                ) {
                    Text(stringResource(R.string.save), color = MpColors.Primary)
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { MpStatusBanner(state.status, state.error) }

            item {
                PassPreviewCard(draft = draft)
            }

            item {
                Text(
                    text = stringResource(R.string.pass_review_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.pass_category),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                "flight" to stringResource(R.string.flight),
                                "event" to stringResource(R.string.event),
                                "transit" to stringResource(R.string.transit),
                                "id" to stringResource(R.string.identity),
                                "membership" to stringResource(R.string.membership),
                            ).forEach { (value, label) ->
                                MpFilterChip(
                                    text = label,
                                    selected = category == value,
                                    onClick = {
                                        category = value
                                        if (themeColor == source.themeColor) {
                                            themeColor = defaultTheme(value)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.pass_main_data),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        MpTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = stringResource(R.string.pass_title),
                            placeholder = stringResource(R.string.pass_title_placeholder),
                        )
                        MpTextField(
                            value = issuer,
                            onValueChange = { issuer = it },
                            label = stringResource(R.string.pass_issuer),
                            placeholder = stringResource(R.string.pass_issuer_placeholder),
                        )
                        MpTextField(
                            value = holder,
                            onValueChange = { holder = it },
                            label = stringResource(R.string.pass_holder),
                            placeholder = stringResource(R.string.pass_holder_placeholder),
                        )
                        MpTextField(
                            value = codePayload,
                            onValueChange = { codePayload = it },
                            label = stringResource(R.string.pass_code),
                            placeholder = stringResource(R.string.pass_code_placeholder),
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.pass_extracted_fields),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        definitions.forEach { definition ->
                            MpTextField(
                                value = fields[definition.key].orEmpty(),
                                onValueChange = { value ->
                                    fields = fields.toMutableMap().apply { put(definition.key, value) }
                                },
                                label = definition.label,
                                placeholder = definition.placeholder,
                            )
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.pass_code_format),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                "qr" to "QR",
                                "pdf417" to "PDF417",
                                "aztec" to "Aztec",
                                "code128" to "Code 128",
                            ).forEach { (value, label) ->
                                MpFilterChip(
                                    text = label,
                                    selected = codeFormat == value,
                                    onClick = { codeFormat = value },
                                )
                            }
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.pass_card_color),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            themeColors.forEach { (_, hex) ->
                                val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                    .getOrDefault(MpColors.Primary)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (themeColor.equals(hex, ignoreCase = true)) {
                                                Modifier.border(3.dp, MpColors.Primary, CircleShape)
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .clickable { themeColor = hex },
                                )
                            }
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.pass_notes),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        notes.forEachIndexed { index, note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MpColors.SurfaceLow)
                                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                IconButton(onClick = {
                                    notes = notes.toMutableList().also { it.removeAt(index) }
                                }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.delete),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                        MpTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = stringResource(R.string.pass_add_note),
                            placeholder = stringResource(R.string.pass_note_placeholder),
                        )
                        TextButton(
                            onClick = {
                                if (noteInput.isNotBlank()) {
                                    notes = notes + noteInput.trim()
                                    noteInput = ""
                                }
                            },
                            enabled = noteInput.isNotBlank(),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.add))
                        }
                    }
                }
            }

            if (source.isNew && source.sourcePath != null) {
                item {
                    MpCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Checkbox(
                                checked = attachSource,
                                onCheckedChange = { attachSource = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MpColors.Primary,
                                    uncheckedColor = MpColors.Outline,
                                ),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.pass_attach_source),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = stringResource(R.string.pass_attach_source_help),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MpColors.OnSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MpButton(
                        text = stringResource(R.string.save_pass),
                        onClick = { viewModel.savePass(draft) },
                        enabled = !state.busy,
                        loading = state.busy,
                        fillWidth = true,
                    )
                    MpButton(
                        text = stringResource(R.string.discard_pass),
                        onClick = viewModel::discardPassDraft,
                        variant = MpButtonVariant.Secondary,
                        enabled = !state.busy,
                        fillWidth = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun fieldDefinitions(category: String): List<PassFieldDefinition> {
    return when (category) {
        "flight" -> listOf(
            PassFieldDefinition("origin", stringResource(R.string.origin), "MAD"),
            PassFieldDefinition("destination", stringResource(R.string.destination), "MEX"),
            PassFieldDefinition("flight", stringResource(R.string.flight_number), "IB 6401"),
            PassFieldDefinition("date", stringResource(R.string.departure_date), "2026-11-28"),
            PassFieldDefinition("departure", stringResource(R.string.departure_time), "17:45"),
            PassFieldDefinition("boarding", stringResource(R.string.boarding_time), "17:05"),
            PassFieldDefinition("seat", stringResource(R.string.seat), "2A"),
            PassFieldDefinition("gate", stringResource(R.string.gate), "H12"),
            PassFieldDefinition("zone", stringResource(R.string.zone), "1"),
            PassFieldDefinition("pnr", stringResource(R.string.booking_code), "ABC123"),
        )
        "event" -> listOf(
            PassFieldDefinition("location", stringResource(R.string.location), stringResource(R.string.stadium_placeholder)),
            PassFieldDefinition("date", stringResource(R.string.event_date), "2026-11-28"),
            PassFieldDefinition("time", stringResource(R.string.event_time), "20:30"),
            PassFieldDefinition("gate", stringResource(R.string.access_gate), "6"),
            PassFieldDefinition("section", stringResource(R.string.section), "Golden Circle"),
            PassFieldDefinition("row", stringResource(R.string.row), "A"),
            PassFieldDefinition("seat", stringResource(R.string.seat), "12"),
        )
        "transit" -> listOf(
            PassFieldDefinition("route", stringResource(R.string.route), "Madrid → Barcelona"),
            PassFieldDefinition("date", stringResource(R.string.travel_date), "2026-11-28"),
            PassFieldDefinition("time", stringResource(R.string.departure_time), "17:45"),
            PassFieldDefinition("platform", stringResource(R.string.platform), "3"),
            PassFieldDefinition("carriage", stringResource(R.string.carriage), "5"),
            PassFieldDefinition("seat", stringResource(R.string.seat), "12B"),
        )
        "id" -> listOf(
            PassFieldDefinition("number", stringResource(R.string.document_number), "ABC123456"),
            PassFieldDefinition("birth", stringResource(R.string.birth_date), "1990-01-01"),
            PassFieldDefinition("expires", stringResource(R.string.valid_until), "2030-01-01"),
        )
        else -> listOf(
            PassFieldDefinition("number", stringResource(R.string.member_number), "MP-000123"),
            PassFieldDefinition("level", stringResource(R.string.level), stringResource(R.string.level_placeholder)),
            PassFieldDefinition("expires", stringResource(R.string.valid_until), "2030-01-01"),
        )
    }
}

private fun defaultTheme(category: String): String = when (category) {
    "event" -> "#4c1d95"
    "transit" -> "#064e3b"
    "id" -> "#18181b"
    "membership" -> "#9a3412"
    else -> "#0c2340"
}
