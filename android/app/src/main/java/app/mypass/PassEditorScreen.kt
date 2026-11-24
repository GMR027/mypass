package app.mypass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class PassFieldDefinition(
    val key: String,
    val label: String,
    val placeholder: String = "",
    val isDate: Boolean = false,
    val isTime: Boolean = false,
)

private val themeColors = listOf(
    "midnight" to "#0c2340",
    "violet" to "#4c1d95",
    "forest" to "#064e3b",
    "obsidian" to "#18181b",
    "burgundy" to "#7f1d1d",
    "amber" to "#9a3412",
)

private const val MAX_BACKGROUND_FILE_BYTES = 15 * 1024 * 1024
private const val MAX_BACKGROUND_EDGE_PX = 1600

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
    var logo by remember(source) { mutableStateOf(source.logo) }
    var appearance by remember(source) { mutableStateOf(source.appearance) }
    var backgroundError by remember(source) { mutableStateOf<Int?>(null) }
    var options by remember(source) { mutableStateOf(source.options) }
    var attachSource by remember(source) { mutableStateOf(source.attachSource) }
    val context = LocalContext.current
    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@runCatching
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@runCatching
                val maxEdge = 256
                val scale = minOf(1f, maxEdge.toFloat() / maxOf(bitmap.width, bitmap.height))
                val scaled = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1),
                        true,
                    )
                } else {
                    bitmap
                }
                val output = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.PNG, 100, output)
                logo = "data:image/png;base64," +
                    Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                if (scaled !== bitmap) {
                    scaled.recycle()
                }
                bitmap.recycle()
            }
        }
    }

    val backgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val dataUrl = encodeBackgroundImage(context, uri)
            if (dataUrl == null) {
                backgroundError = R.string.pass_background_invalid
            } else {
                appearance = appearance.copy(image = dataUrl)
                backgroundError = null
            }
        }
    }

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
        logo = logo,
        appearance = appearance,
        options = options,
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
                            text = stringResource(R.string.pass_logo),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.pass_logo_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val bitmap = logo?.let(::decodeDataUrl)
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MpColors.SurfaceLow)
                                        .padding(8.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MpColors.SurfaceLow),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = MpColors.Primary)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MpButton(
                                    text = stringResource(R.string.choose_logo),
                                    onClick = { logoPicker.launch("image/*") },
                                    variant = MpButtonVariant.Secondary,
                                    fillWidth = true,
                                )
                                if (logo != null) {
                                    MpButton(
                                        text = stringResource(R.string.remove_logo),
                                        onClick = { logo = null },
                                        variant = MpButtonVariant.Quiet,
                                        fillWidth = true,
                                    )
                                }
                            }
                        }
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
                            val update = { value: String ->
                                fields = fields.toMutableMap().apply { put(definition.key, value) }
                            }

                            if (definition.isDate) {
                                MpDateField(
                                    value = fields[definition.key].orEmpty(),
                                    onValueChange = update,
                                    label = definition.label,
                                    defaultDate = if (definition.key == "birth") {
                                        LocalDate.now().minusYears(30)
                                    } else {
                                        null
                                    },
                                )
                            } else if (definition.isTime) {
                                MpTimeField(
                                    value = fields[definition.key].orEmpty(),
                                    onValueChange = update,
                                    label = definition.label,
                                )
                            } else {
                                MpTextField(
                                    value = fields[definition.key].orEmpty(),
                                    onValueChange = update,
                                    label = definition.label,
                                    placeholder = definition.placeholder,
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
                        MpTextField(
                            value = themeColor,
                            onValueChange = { value ->
                                themeColor = value.take(7)
                            },
                            label = stringResource(R.string.pass_custom_color),
                            placeholder = "#0c2340",
                        )
                        if (!Regex("^#[0-9a-fA-F]{6}$").matches(themeColor)) {
                            Text(
                                text = stringResource(R.string.pass_color_invalid),
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.Error,
                            )
                        }
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.pass_background),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.pass_background_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )

                        Text(
                            text = stringResource(R.string.pass_texture),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        val previewColor = runCatching {
                            Color(android.graphics.Color.parseColor(themeColor))
                        }.getOrDefault(MpColors.Primary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TextureSwatch(
                                texture = null,
                                baseColor = previewColor,
                                selected = appearance.texture == PASS_TEXTURE_NONE,
                                onClick = { appearance = appearance.copy(texture = PASS_TEXTURE_NONE) },
                            )
                            PASS_TEXTURE_SPECS.forEach { spec ->
                                TextureSwatch(
                                    texture = spec,
                                    baseColor = previewColor,
                                    selected = appearance.texture == spec.id,
                                    onClick = { appearance = appearance.copy(texture = spec.id) },
                                )
                            }
                        }

                        if (appearance.texture != PASS_TEXTURE_NONE) {
                            Text(
                                text = stringResource(R.string.pass_texture_size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MpColors.OnSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(
                                    "sm" to R.string.pass_texture_size_small,
                                    "md" to R.string.pass_texture_size_medium,
                                    "lg" to R.string.pass_texture_size_large,
                                ).forEach { (value, label) ->
                                    MpFilterChip(
                                        text = stringResource(label),
                                        selected = appearance.textureSize == value,
                                        onClick = { appearance = appearance.copy(textureSize = value) },
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.pass_repeat),
                                style = MaterialTheme.typography.labelMedium,
                                color = MpColors.OnSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                repeatOptions().forEach { (value, label) ->
                                    MpFilterChip(
                                        text = stringResource(label),
                                        selected = appearance.textureRepeat == value,
                                        onClick = { appearance = appearance.copy(textureRepeat = value) },
                                    )
                                }
                            }
                            Text(
                                text = "${stringResource(R.string.pass_texture_opacity)} · ${appearance.textureOpacity}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MpColors.OnSurfaceVariant,
                            )
                            Slider(
                                value = appearance.textureOpacity.toFloat(),
                                onValueChange = {
                                    appearance = appearance.copy(textureOpacity = it.roundToInt())
                                },
                                valueRange = 0f..100f,
                                steps = 19,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MpColors.OutlineVariant.copy(alpha = 0.6f)),
                        )

                        Text(
                            text = stringResource(R.string.pass_background_image),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = stringResource(R.string.pass_background_image_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MpColors.OnSurfaceVariant,
                        )
                        val backgroundBitmap = remember(appearance.image) {
                            appearance.image?.let(::decodeDataUrl)
                        }
                        val backgroundImage = remember(backgroundBitmap) {
                            backgroundBitmap?.asImageBitmap()
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (backgroundImage != null) {
                                Image(
                                    bitmap = backgroundImage,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MpColors.SurfaceLow),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MpColors.SurfaceLow),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = MpColors.Primary,
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                MpButton(
                                    text = stringResource(R.string.pass_choose_image),
                                    onClick = { backgroundPicker.launch("image/*") },
                                    variant = MpButtonVariant.Secondary,
                                    fillWidth = true,
                                )
                                if (backgroundImage != null) {
                                    MpButton(
                                        text = stringResource(R.string.pass_remove_image),
                                        onClick = {
                                            appearance = appearance.copy(image = null)
                                            backgroundError = null
                                        },
                                        variant = MpButtonVariant.Quiet,
                                        fillWidth = true,
                                    )
                                }
                            }
                        }

                        if (backgroundImage != null) {
                            Text(
                                text = stringResource(R.string.pass_image_size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MpColors.OnSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(
                                    "cover" to R.string.pass_image_size_cover,
                                    "contain" to R.string.pass_image_size_contain,
                                    "50%" to R.string.pass_image_size_half,
                                    "100%" to R.string.pass_image_size_full,
                                    "200%" to R.string.pass_image_size_double,
                                ).forEach { (value, label) ->
                                    MpFilterChip(
                                        text = stringResource(label),
                                        selected = appearance.imageSize == value,
                                        onClick = { appearance = appearance.copy(imageSize = value) },
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.pass_repeat),
                                style = MaterialTheme.typography.labelMedium,
                                color = MpColors.OnSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                repeatOptions().forEach { (value, label) ->
                                    MpFilterChip(
                                        text = stringResource(label),
                                        selected = appearance.imageRepeat == value,
                                        onClick = { appearance = appearance.copy(imageRepeat = value) },
                                    )
                                }
                            }
                            Text(
                                text = "${stringResource(R.string.pass_darken_image)} · ${appearance.imageShade}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MpColors.OnSurfaceVariant,
                            )
                            Slider(
                                value = appearance.imageShade.toFloat(),
                                onValueChange = {
                                    appearance = appearance.copy(imageShade = it.roundToInt())
                                },
                                valueRange = 0f..PASS_IMAGE_SHADE_MAX.toFloat(),
                                steps = 13,
                            )
                        }

                        backgroundError?.let { messageRes ->
                            Text(
                                text = stringResource(messageRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MpColors.Error,
                            )
                        }

                        MpButton(
                            text = stringResource(R.string.pass_background_reset),
                            onClick = {
                                appearance = PassAppearance()
                                backgroundError = null
                            },
                            variant = MpButtonVariant.Quiet,
                            fillWidth = true,
                        )
                    }
                }
            }

            item {
                MpCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.pass_options),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        OptionRow(
                            title = stringResource(R.string.pass_option_presentation),
                            checked = options["presentation"] != false,
                            onCheckedChange = { options = options + ("presentation" to it) },
                        )
                        OptionRow(
                            title = stringResource(R.string.pass_option_share),
                            checked = options["share"] != false,
                            onCheckedChange = { options = options + ("share" to it) },
                        )
                        OptionRow(
                            title = stringResource(R.string.pass_option_reminder),
                            checked = options["reminder"] != false,
                            onCheckedChange = { options = options + ("reminder" to it) },
                        )
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
private fun OptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MpColors.Primary,
                uncheckedColor = MpColors.Outline,
            ),
        )
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun fieldDefinitions(category: String): List<PassFieldDefinition> {
    return when (category) {
        "flight" -> listOf(
            PassFieldDefinition("origin", stringResource(R.string.origin), "MAD"),
            PassFieldDefinition("destination", stringResource(R.string.destination), "MEX"),
            PassFieldDefinition("flight", stringResource(R.string.flight_number), "IB 6401"),
            PassFieldDefinition("date", stringResource(R.string.departure_date), "2026-11-28", isDate = true),
            PassFieldDefinition("departure", stringResource(R.string.departure_time), "17:45", isTime = true),
            PassFieldDefinition("boarding_start", stringResource(R.string.boarding_start), "17:05", isTime = true),
            PassFieldDefinition("boarding_end", stringResource(R.string.boarding_end), "17:25", isTime = true),
            PassFieldDefinition("gate", stringResource(R.string.gate), "H12"),
            PassFieldDefinition("terminal", stringResource(R.string.terminal), "T2"),
            PassFieldDefinition("seat", stringResource(R.string.seat), "2A"),
            PassFieldDefinition("zone", stringResource(R.string.zone), "1"),
            PassFieldDefinition("pnr", stringResource(R.string.booking_code), "ABC123"),
        )
        "event" -> listOf(
            PassFieldDefinition("location", stringResource(R.string.location), stringResource(R.string.stadium_placeholder)),
            PassFieldDefinition("address", stringResource(R.string.address_optional), stringResource(R.string.address_placeholder)),
            PassFieldDefinition("coordinates", stringResource(R.string.exact_location_optional), stringResource(R.string.coordinates_placeholder)),
            PassFieldDefinition("date", stringResource(R.string.event_date), "2026-11-28", isDate = true),
            PassFieldDefinition("time", stringResource(R.string.event_time), "20:30"),
            PassFieldDefinition("gate", stringResource(R.string.access_gate), "6"),
            PassFieldDefinition("section", stringResource(R.string.section), "Golden Circle"),
            PassFieldDefinition("row", stringResource(R.string.row), "A"),
            PassFieldDefinition("seat", stringResource(R.string.seat), "12"),
        )
        "transit" -> listOf(
            PassFieldDefinition("route", stringResource(R.string.route), "Madrid → Barcelona"),
            PassFieldDefinition("location", stringResource(R.string.station_stop), stringResource(R.string.station_placeholder)),
            PassFieldDefinition("address", stringResource(R.string.address_optional), stringResource(R.string.address_placeholder)),
            PassFieldDefinition("coordinates", stringResource(R.string.exact_location_optional), stringResource(R.string.coordinates_placeholder)),
            PassFieldDefinition("date", stringResource(R.string.travel_date), "2026-11-28", isDate = true),
            PassFieldDefinition("time", stringResource(R.string.departure_time), "17:45", isTime = true),
            PassFieldDefinition("boarding_start", stringResource(R.string.boarding_start), "17:05", isTime = true),
            PassFieldDefinition("boarding_end", stringResource(R.string.boarding_end), "17:25", isTime = true),
            PassFieldDefinition("terminal", stringResource(R.string.terminal), "Terminal 1"),
            PassFieldDefinition("platform", stringResource(R.string.platform), "3"),
            PassFieldDefinition("carriage", stringResource(R.string.carriage), "5"),
            PassFieldDefinition("seat", stringResource(R.string.seat), "12B"),
        )
        "id" -> listOf(
            PassFieldDefinition("number", stringResource(R.string.document_number), "ABC123456"),
            PassFieldDefinition("birth", stringResource(R.string.birth_date), "1990-01-01", isDate = true),
            PassFieldDefinition("expires", stringResource(R.string.valid_until), "2030-01-01", isDate = true),
        )
        else -> listOf(
            PassFieldDefinition("number", stringResource(R.string.member_number), "MP-000123"),
            PassFieldDefinition("level", stringResource(R.string.level), stringResource(R.string.level_placeholder)),
            PassFieldDefinition("expires", stringResource(R.string.valid_until), "2030-01-01", isDate = true),
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

@Composable
private fun TextureSwatch(
    texture: PassTextureSpec?,
    baseColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(baseColor)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MpColors.Primary else MpColors.OutlineVariant,
                    shape = RoundedCornerShape(16.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (texture != null) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawPassTexture(
                        PassAppearance(
                            texture = texture.id,
                            textureSize = "sm",
                            textureRepeat = "repeat",
                        ),
                    )
                }
            } else {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp),
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MpColors.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
        Text(
            text = if (texture != null) {
                stringResource(texture.labelRes)
            } else {
                stringResource(R.string.pass_texture_none)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MpColors.Primary else MpColors.OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun repeatOptions(): List<Pair<String, Int>> {
    return listOf(
        "repeat" to R.string.pass_repeat_all,
        "repeat-x" to R.string.pass_repeat_horizontal,
        "repeat-y" to R.string.pass_repeat_vertical,
        "no-repeat" to R.string.pass_repeat_none,
    )
}

private fun encodeBackgroundImage(context: Context, uri: Uri): String? {
    val bytes = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull() ?: return null

    if (bytes.size > MAX_BACKGROUND_FILE_BYTES) {
        return null
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    var sampleSize = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_BACKGROUND_EDGE_PX) {
        sampleSize *= 2
    }

    val decoded = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    ) ?: return null

    try {
        for ((maxEdge, quality) in listOf(1600 to 82, 1000 to 74)) {
            val scale = min(1f, maxEdge.toFloat() / max(decoded.width, decoded.height))
            val width = max(1, (decoded.width * scale).toInt())
            val height = max(1, (decoded.height * scale).toInt())
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(decoded, width, height, true)
            } else {
                decoded
            }
            val output = ByteArrayOutputStream()
            val compressed = scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, output)
            if (scaled !== decoded) {
                scaled.recycle()
            }
            if (!compressed) {
                continue
            }
            val dataUrl = "data:image/webp;base64," +
                Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            if (dataUrl.length <= MAX_BACKGROUND_DATA_URL) {
                return dataUrl
            }
        }
        return null
    } finally {
        decoded.recycle()
    }
}
