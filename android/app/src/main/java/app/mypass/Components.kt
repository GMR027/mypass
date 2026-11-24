package app.mypass

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MpButtonVariant {
    Primary,
    Secondary,
    Quiet,
    Danger,
    Destructive,
}

enum class MpBadgeTone {
    Primary,
    Warning,
    Danger,
    Neutral,
}

@Composable
fun MpLogo(
    modifier: Modifier = Modifier,
    inverse: Boolean = false,
    showName: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (inverse) Color.White else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.mypass_wallet),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit,
            )
        }
        if (showName) {
            Text(
                text = "My Pass",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
                color = if (inverse) Color.White else MpColors.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MpButtonVariant = MpButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    fillWidth: Boolean = false,
    compact: Boolean = false,
    maxLines: Int = 2,
    contentPadding: PaddingValues? = null,
) {
    val buttonModifier = modifier
        .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
        .heightIn(min = 56.dp)
    val resolvedPadding = contentPadding ?: if (compact) {
        PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    } else {
        PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    }
    val content: @Composable () -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = when (variant) {
                    MpButtonVariant.Primary -> MpColors.OnPrimary
                    MpButtonVariant.Danger -> MpColors.OnErrorContainer
                    MpButtonVariant.Destructive -> Color.White
                    else -> MpColors.Primary
                },
            )
            Spacer(modifier = Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
        } else if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }

    when (variant) {
        MpButtonVariant.Primary -> {
            val shape = MaterialTheme.shapes.medium
            val brush = if (enabled && !loading) {
                MpGradients.Brand
            } else {
                Brush.linearGradient(listOf(MpColors.Primary.copy(alpha = 0.35f), MpColors.Primary.copy(alpha = 0.28f)))
            }
            Button(
                onClick = onClick,
                modifier = buttonModifier
                    .shadow(if (enabled && !loading) 8.dp else 0.dp, shape)
                    .background(brush = brush, shape = shape),
                enabled = enabled && !loading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MpColors.OnPrimary,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color.White.copy(alpha = 0.8f),
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
                ),
                contentPadding = resolvedPadding,
            ) { content() }
        }

        MpButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(2.dp, MpColors.OnSurface.copy(alpha = 0.72f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MpColors.OnSurface,
                disabledContentColor = MpColors.OnSurface.copy(alpha = 0.4f),
            ),
            contentPadding = resolvedPadding,
        ) { content() }

        MpButtonVariant.Quiet -> FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MpColors.SurfaceLow,
                contentColor = MpColors.OnSurface,
                disabledContainerColor = MpColors.SurfaceLow.copy(alpha = 0.5f),
                disabledContentColor = MpColors.OnSurface.copy(alpha = 0.4f),
            ),
            contentPadding = resolvedPadding,
        ) { content() }

        MpButtonVariant.Danger -> FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MpColors.ErrorContainer,
                contentColor = MpColors.OnErrorContainer,
                disabledContainerColor = MpColors.ErrorContainer.copy(alpha = 0.5f),
                disabledContentColor = MpColors.OnErrorContainer.copy(alpha = 0.4f),
            ),
            contentPadding = resolvedPadding,
        ) { content() }

        MpButtonVariant.Destructive -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MpColors.Error,
                contentColor = Color.White,
                disabledContainerColor = MpColors.Error.copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.8f),
            ),
            contentPadding = resolvedPadding,
        ) { content() }
    }
}

@Composable
fun MpTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MpColors.Primary,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (enabled) color else color.copy(alpha = 0.4f),
        )
    }
}

@Composable
fun MpCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MpColors.Surface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
        shadowElevation = 2.dp,
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun MpHeroPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(MpGradients.BrandDeep),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = MpColors.Accent.copy(alpha = 0.16f),
                radius = size.minDimension * 0.42f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.04f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.minDimension * 0.6f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 1.05f),
            )
        }
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun MpIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Int = 48,
    background: Color = MpColors.PrimaryFixed,
    tint: Color = MpColors.Primary,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
fun MpIconTile(
    painter: Painter,
    modifier: Modifier = Modifier,
    size: Int = 48,
    background: Color = MpColors.PrimaryFixed,
    tint: Color = MpColors.Primary,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painter, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.5f).dp))
    }
}

@Composable
fun MpBadge(
    text: String,
    tone: MpBadgeTone = MpBadgeTone.Neutral,
    modifier: Modifier = Modifier,
    showDot: Boolean = false,
) {
    val background = when (tone) {
        MpBadgeTone.Primary -> MpColors.PrimaryFixed
        MpBadgeTone.Warning -> MpColors.SecondaryFixed
        MpBadgeTone.Danger -> MpColors.ErrorContainer
        MpBadgeTone.Neutral -> MpColors.SurfaceHighest
    }
    val foreground = when (tone) {
        MpBadgeTone.Primary -> MpColors.Primary
        MpBadgeTone.Warning -> MpColors.OnSecondaryFixed
        MpBadgeTone.Danger -> MpColors.OnErrorContainer
        MpBadgeTone.Neutral -> MpColors.OnSurfaceVariant
    }
    val badgeModifier = modifier
        .clip(CircleShape)
        .background(background)

    if (showDot) {
        Row(
            modifier = badgeModifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(foreground),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = badgeModifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun MpAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 44,
    containerColor: Color = MpColors.PrimaryFixed,
    contentColor: Color = MpColors.Primary,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    photo: Bitmap? = null,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "M",
                style = textStyle,
                color = contentColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun MpStatusBanner(
    status: String?,
    error: String?,
    modifier: Modifier = Modifier,
) {
    val message = error ?: status
    if (message == null) {
        return
    }

    val isError = error != null
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (isError) MpColors.ErrorContainer else MpColors.PrimaryFixed,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (isError) Icons.Filled.Warning else Icons.Filled.Info,
                contentDescription = null,
                tint = if (isError) MpColors.OnErrorContainer else MpColors.Primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MpColors.OnErrorContainer else MpColors.Primary,
            )
        }
    }
}

@Composable
fun MpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = MaterialTheme.shapes.medium,
        textStyle = MaterialTheme.typography.bodyLarge,
        trailingIcon = trailingContent,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MpColors.Primary,
            unfocusedBorderColor = MpColors.OutlineVariant,
            disabledBorderColor = MpColors.OutlineVariant.copy(alpha = 0.5f),
            focusedLabelColor = MpColors.Primary,
            unfocusedLabelColor = MpColors.OnSurfaceVariant,
            cursorColor = MpColors.Primary,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = MpColors.SurfaceLow.copy(alpha = 0.5f),
        ),
    )
}

@Composable
fun MpDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    defaultDate: LocalDate? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    val context = LocalContext.current
    val parsed = remember(value) {
        runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
    }
    val display = parsed?.format(
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()),
    ) ?: value
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(showPicker) {
        if (!showPicker) {
            return@LaunchedEffect
        }

        val initial = parsed ?: defaultDate ?: LocalDate.now()
        val dialog = DatePickerDialog(
            context,
            { _, year, month, day ->
                showPicker = false
                onValueChange(LocalDate.of(year, month + 1, day).toString())
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth,
        )
        dialog.setTitle(label)
        minDate?.let {
            dialog.datePicker.minDate = it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        maxDate?.let {
            dialog.datePicker.maxDate = it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        dialog.setOnDismissListener { showPicker = false }
        dialog.show()
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            MpTextField(
                value = display,
                onValueChange = {},
                label = label,
                enabled = enabled,
                readOnly = true,
                trailingContent = {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = stringResource(R.string.choose_date),
                        tint = MpColors.Primary,
                    )
                },
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = enabled) { showPicker = true }
                    .semantics { contentDescription = label },
            )
        }

        if (value.isNotBlank()) {
            TextButton(
                onClick = { onValueChange("") },
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            ) {
                Text(stringResource(R.string.clear_date), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun MpTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    defaultTime: LocalTime? = null,
) {
    val context = LocalContext.current
    val parsed = remember(value) {
        runCatching { LocalTime.parse(value.take(5)) }.getOrNull()
    }
    val display = parsed?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: value
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(showPicker) {
        if (!showPicker) {
            return@LaunchedEffect
        }

        val initial = parsed ?: defaultTime ?: LocalTime.now()
        val dialog = TimePickerDialog(
            context,
            { _, hour, minute ->
                showPicker = false
                onValueChange(String.format(Locale.US, "%02d:%02d", hour, minute))
            },
            initial.hour,
            initial.minute,
            DateFormat.is24HourFormat(context),
        )
        dialog.setTitle(label)
        dialog.setOnDismissListener { showPicker = false }
        dialog.show()
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            MpTextField(
                value = display,
                onValueChange = {},
                label = label,
                enabled = enabled,
                readOnly = true,
                trailingContent = {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.choose_time),
                        tint = MpColors.Primary,
                    )
                },
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = enabled) { showPicker = true }
                    .semantics { contentDescription = label },
            )
        }

        if (value.isNotBlank()) {
            TextButton(
                onClick = { onValueChange("") },
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            ) {
                Text(stringResource(R.string.clear_time), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun MpPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Password,
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }

    MpTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        keyboardType = keyboardType,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        enabled = enabled,
        trailingContent = {
            TextButton(
                onClick = { visible = !visible },
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text(
                    text = stringResource(if (visible) R.string.hide_password else R.string.show_password),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

@Composable
fun MpSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            MpIconTile(icon = icon, size = 44)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MpColors.OnSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MpColors.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MpColors.OutlineVariant,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
fun MpSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MpColors.OnSurfaceVariant,
                )
            }
        }
        action?.invoke()
    }
}

@Composable
fun MpIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MpColors.OnSurfaceVariant,
    container: Color = Color.Transparent,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(container),
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun MpEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MpColors.SurfaceLow,
        border = BorderStroke(1.dp, MpColors.OutlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MpIconTile(icon = icon, size = 56, background = MpColors.SurfaceHighest, tint = MpColors.Outline)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MpColors.OnSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun MpMonoText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 2.sp,
        color = MpColors.OnSurface,
    )
}

@Composable
fun MpDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MpColors.OutlineVariant.copy(alpha = 0.5f)),
    )
}
