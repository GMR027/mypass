package app.mypass

import android.view.Window
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

enum class MpDialogTone {
    Primary,
    Warning,
    Danger,
    Neutral,
}

enum class MpCalloutTone {
    Info,
    Warning,
    Error,
    Success,
}

private fun toneBackground(tone: MpDialogTone): Color = when (tone) {
    MpDialogTone.Primary -> MpColors.PrimaryFixed
    MpDialogTone.Warning -> MpColors.SecondaryFixed
    MpDialogTone.Danger -> MpColors.ErrorContainer
    MpDialogTone.Neutral -> MpColors.SurfaceHighest
}

private fun toneForeground(tone: MpDialogTone): Color = when (tone) {
    MpDialogTone.Primary -> MpColors.Primary
    MpDialogTone.Warning -> MpColors.OnSecondaryFixed
    MpDialogTone.Danger -> MpColors.OnErrorContainer
    MpDialogTone.Neutral -> MpColors.OnSurfaceVariant
}

/**
 * Ventana modal común de My Pass.
 *
 * Mantiene la cabecera y las acciones fijas, y deja el cuerpo desplazable.
 * El ancho máximo y el alto se adaptan a tablet y móvil.
 */
@Composable
fun MpDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 640.dp,
    header: @Composable (() -> Unit)? = null,
    actions: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val view = LocalView.current

        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val previousDim = window?.attributes?.dimAmount ?: -1f
            window?.setDimAmount(0f)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            onDispose {
                window?.setDimAmount(previousDim)
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onDismissRequest() }
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val maxPanelHeight = maxHeight * 0.92f

            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = maxWidth)
                    .heightIn(max = maxPanelHeight)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { },
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MpColors.OutlineVariant.copy(alpha = 0.45f)),
                shadowElevation = 24.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (header != null) {
                        Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
                            header()
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        MpDivider()
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        content = content,
                    )

                    if (actions != null) {
                        MpDivider()
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            content = actions,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MpDialogHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    painter: Painter? = null,
    tone: MpDialogTone = MpDialogTone.Primary,
    onClose: (() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            icon != null -> MpIconTile(
                icon = icon,
                size = 56,
                background = toneBackground(tone),
                tint = toneForeground(tone),
            )

            painter != null -> MpIconTile(
                painter = painter,
                size = 56,
                background = toneBackground(tone),
                tint = toneForeground(tone),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MpColors.OnSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                badge?.invoke()
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MpColors.OnSurfaceVariant,
                )
            }
        }

        if (onClose != null) {
            MpIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.close),
                onClick = onClose,
                container = MpColors.SurfaceLow,
                tint = MpColors.OnSurfaceVariant,
            )
        }
    }
}

@Composable
fun MpDialogActions(
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryVariant: MpButtonVariant = MpButtonVariant.Primary,
    primaryEnabled: Boolean = true,
    primaryLoading: Boolean = false,
    primaryIcon: ImageVector? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    secondaryEnabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MpButton(
            text = primaryLabel,
            onClick = onPrimary,
            variant = primaryVariant,
            enabled = primaryEnabled,
            loading = primaryLoading,
            icon = primaryIcon,
            fillWidth = true,
        )
        if (secondaryLabel != null && onSecondary != null) {
            MpButton(
                text = secondaryLabel,
                onClick = onSecondary,
                variant = MpButtonVariant.Secondary,
                enabled = secondaryEnabled,
                fillWidth = true,
            )
        }
    }
}

@Composable
fun MpDialogSection(
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MpColors.OnSurfaceVariant,
                )
            }
        }
        content()
    }
}

@Composable
fun MpSelectableCard(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    val borderColor = if (selected) MpColors.Primary else MpColors.OutlineVariant.copy(alpha = 0.65f)
    val background = if (selected) MpColors.PrimaryFixed.copy(alpha = 0.35f) else Color.White

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MpRadioIndicator(selected = selected)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    badge?.invoke()
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun MpOptionCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = Color.White,
        border = BorderStroke(1.dp, MpColors.OutlineVariant.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            leading()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MpColors.OnSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun MpRadioIndicator(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .border(
                width = 2.dp,
                color = if (selected) MpColors.Primary else MpColors.Outline,
                shape = CircleShape,
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MpColors.Primary, CircleShape),
            )
        }
    }
}

@Composable
fun MpCallout(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    tone: MpCalloutTone = MpCalloutTone.Info,
    icon: ImageVector? = null,
) {
    val background = when (tone) {
        MpCalloutTone.Info -> MpColors.SurfaceLow
        MpCalloutTone.Warning -> MpColors.SecondaryFixed
        MpCalloutTone.Error -> MpColors.ErrorContainer
        MpCalloutTone.Success -> MpColors.PrimaryFixed
    }
    val foreground = when (tone) {
        MpCalloutTone.Info -> MpColors.Primary
        MpCalloutTone.Warning -> MpColors.OnSecondaryFixed
        MpCalloutTone.Error -> MpColors.OnErrorContainer
        MpCalloutTone.Success -> MpColors.Primary
    }
    val border = when (tone) {
        MpCalloutTone.Info -> MpColors.Primary.copy(alpha = 0.18f)
        MpCalloutTone.Warning -> MpColors.Secondary.copy(alpha = 0.28f)
        MpCalloutTone.Error -> MpColors.Error.copy(alpha = 0.24f)
        MpCalloutTone.Success -> MpColors.Primary.copy(alpha = 0.18f)
    }
    val resolvedIcon = icon ?: when (tone) {
        MpCalloutTone.Info -> Icons.Filled.Info
        MpCalloutTone.Warning -> Icons.Filled.Warning
        MpCalloutTone.Error -> Icons.Filled.Warning
        MpCalloutTone.Success -> Icons.Filled.CheckCircle
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = resolvedIcon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(22.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = foreground,
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = foreground,
                )
            }
        }
    }
}

@Composable
fun MpSummaryCard(
    label: String,
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    painter: Painter? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MpColors.SurfaceLow,
        border = BorderStroke(1.dp, MpColors.OutlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                icon != null -> MpIconTile(
                    icon = icon,
                    size = 48,
                    background = Color.White,
                    tint = MpColors.Primary,
                )

                painter != null -> MpIconTile(
                    painter = painter,
                    size = 48,
                    background = Color.White,
                    tint = MpColors.Primary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MpColors.Primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            badge?.invoke()
        }
    }
}

@Composable
fun MpFullScreenWindow(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                content = content,
            )
        }
    }
}
