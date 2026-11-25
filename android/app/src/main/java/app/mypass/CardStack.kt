package app.mypass

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apila varios composables con un traslape controlado. El elemento seleccionado
 * se separa de la pila para que su contenido completo quede visible.
 */
@Composable
fun MpCascadeStack(
    modifier: Modifier = Modifier,
    overlap: Dp = 14.dp,
    selectedIndex: Int? = null,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = content,
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        var nextTop = 0
        val positions = placeables.mapIndexed { index, placeable ->
            val top = nextTop
            nextTop += placeable.height
            if (index < placeables.lastIndex) {
                nextTop += if (index == selectedIndex || index + 1 == selectedIndex) {
                    10.dp.roundToPx()
                } else {
                    -overlap.roundToPx()
                }
            }
            top
        }

        layout(constraints.maxWidth, nextTop.coerceAtLeast(0)) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(0, positions[index])
            }
        }
    }
}

/**
 * Contenedor con giro 3D entre una cara frontal y una trasera.
 */
@Composable
fun MpFlipCard(
    itemKey: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MpColors.Surface,
    border: BorderStroke? = BorderStroke(1.dp, MpColors.OutlineVariant.copy(alpha = 0.55f)),
    shadowElevation: Dp = 2.dp,
    front: @Composable (onFlip: () -> Unit) -> Unit,
    back: @Composable (onFlipBack: () -> Unit) -> Unit,
) {
    var flipped by remember(itemKey) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 620),
        label = "card-flip",
    )
    val density = LocalDensity.current.density

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        border = border,
        shadowElevation = shadowElevation,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .graphicsLayer {
                    rotationY = if (rotation <= 90f) rotation else rotation - 180f
                    cameraDistance = 18f * density
                },
        ) {
            if (rotation <= 90f) {
                front { flipped = true }
            } else {
                back { flipped = false }
            }
        }
    }
}

@Composable
fun MpFlipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    val foreground = if (dark) Color.White else MpColors.Primary
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (dark) Color.White.copy(alpha = 0.14f) else MpColors.PrimaryFixed.copy(alpha = 0.55f),
        border = BorderStroke(
            1.dp,
            if (dark) Color.White.copy(alpha = 0.22f) else MpColors.Primary.copy(alpha = 0.18f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = foreground,
                maxLines = 1,
            )
        }
    }
}
