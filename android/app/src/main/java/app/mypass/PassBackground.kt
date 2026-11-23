package app.mypass

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
internal fun Modifier.passCardBackground(pass: UiPass): Modifier {
    val appearance = pass.appearance
    val bitmap = remember(appearance.image) {
        appearance.image?.let(::decodeDataUrl)
    }
    val imageBitmap = remember(bitmap) {
        bitmap?.asImageBitmap()
    }
    val baseBrush = passGradient(pass)

    return drawWithCache {
        onDrawBehind {
            drawRect(brush = baseBrush)
            drawPassTexture(appearance)
            if (bitmap != null && imageBitmap != null) {
                drawPassImage(bitmap, imageBitmap, appearance)
            }
        }
    }
}

internal fun DrawScope.drawPassTexture(appearance: PassAppearance) {
    if (appearance.texture == PASS_TEXTURE_NONE) {
        return
    }

    val spec = PASS_TEXTURE_SPECS.firstOrNull { it.id == appearance.texture } ?: return
    val factor = PASS_TEXTURE_SIZES[appearance.textureSize] ?: 1f
    val textureAlpha = (appearance.textureOpacity / 100f).coerceIn(0f, 1f)
    val tileWidth = (spec.widthDp * factor).dp.toPx()
    val tileHeight = (spec.heightDp * factor).dp.toPx()
    if (tileWidth < 1f || tileHeight < 1f || textureAlpha <= 0f) {
        return
    }

    val drawPattern = {
        drawTexturePattern(spec.id, tileWidth, tileHeight, factor, textureAlpha)
    }

    when (appearance.textureRepeat) {
        "repeat-x" -> {
            val top = ((size.height - tileHeight) / 2f).coerceAtLeast(0f)
            clipRect(0f, top, size.width, (top + tileHeight).coerceAtMost(size.height)) {
                drawPattern()
            }
        }

        "repeat-y" -> {
            val left = ((size.width - tileWidth) / 2f).coerceAtLeast(0f)
            clipRect(left, 0f, (left + tileWidth).coerceAtMost(size.width), size.height) {
                drawPattern()
            }
        }

        "no-repeat" -> {
            val left = ((size.width - tileWidth) / 2f).coerceAtLeast(0f)
            val top = ((size.height - tileHeight) / 2f).coerceAtLeast(0f)
            clipRect(
                left,
                top,
                (left + tileWidth).coerceAtMost(size.width),
                (top + tileHeight).coerceAtMost(size.height),
            ) {
                drawPattern()
            }
        }

        else -> drawPattern()
    }
}

private fun DrawScope.drawTexturePattern(
    texture: String,
    tileWidth: Float,
    tileHeight: Float,
    factor: Float,
    alpha: Float,
) {
    when (texture) {
        "dots" -> drawDots(tileWidth, tileHeight, factor, alpha)
        "lines" -> drawLines(tileWidth, factor, alpha)
        "grid" -> drawGrid(tileWidth, tileHeight, alpha)
        "carbon" -> drawCarbon(tileWidth, factor, alpha)
        "waves" -> drawWaves(tileWidth, tileHeight, factor, alpha)
        "grain" -> drawGrain(tileWidth, tileHeight, alpha)
    }
}

private fun DrawScope.drawDots(tileWidth: Float, tileHeight: Float, factor: Float, alpha: Float) {
    val radius = 1.5.dp.toPx() * factor.coerceIn(0.7f, 1.8f)
    var x = -tileWidth
    while (x < size.width + tileWidth) {
        var y = -tileHeight
        while (y < size.height + tileHeight) {
            drawCircle(
                color = Color.White.copy(alpha = 0.30f * alpha),
                radius = radius,
                center = Offset(x + tileWidth / 2f, y + tileHeight / 2f),
            )
            y += tileHeight
        }
        x += tileWidth
    }
}

private fun DrawScope.drawLines(tileWidth: Float, factor: Float, alpha: Float) {
    val stroke = 1.4.dp.toPx() * factor.coerceIn(0.7f, 1.8f)
    var x = -size.height
    while (x < size.width) {
        drawLine(
            color = Color.White.copy(alpha = 0.18f * alpha),
            start = Offset(x, 0f),
            end = Offset(x + size.height, size.height),
            strokeWidth = stroke,
        )
        x += tileWidth
    }
}

private fun DrawScope.drawGrid(tileWidth: Float, tileHeight: Float, alpha: Float) {
    val stroke = 1.dp.toPx()
    var x = -tileWidth
    while (x < size.width + tileWidth) {
        drawLine(
            color = Color.White.copy(alpha = 0.14f * alpha),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = stroke,
        )
        x += tileWidth
    }
    var y = -tileHeight
    while (y < size.height + tileHeight) {
        drawLine(
            color = Color.White.copy(alpha = 0.14f * alpha),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = stroke,
        )
        y += tileHeight
    }
}

private fun DrawScope.drawCarbon(tileWidth: Float, factor: Float, alpha: Float) {
    val stroke = 1.dp.toPx() * factor.coerceIn(0.7f, 1.8f)
    val color = Color.White.copy(alpha = 0.10f * alpha)
    var x = -size.height
    while (x < size.width) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x + size.height, size.height),
            strokeWidth = stroke,
        )
        x += tileWidth
    }
    x = 0f
    while (x < size.width + size.height) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x - size.height, size.height),
            strokeWidth = stroke,
        )
        x += tileWidth
    }
}

private fun DrawScope.drawWaves(tileWidth: Float, tileHeight: Float, factor: Float, alpha: Float) {
    val stroke = Stroke(width = 1.5.dp.toPx() * factor.coerceIn(0.7f, 1.8f))
    var y = -tileHeight
    while (y < size.height + tileHeight) {
        val path = Path()
        var x = -tileWidth
        path.moveTo(x, y + tileHeight / 2f)
        while (x < size.width + tileWidth) {
            path.quadraticBezierTo(
                x + tileWidth * 0.25f,
                y,
                x + tileWidth * 0.5f,
                y + tileHeight / 2f,
            )
            path.quadraticBezierTo(
                x + tileWidth * 0.75f,
                y + tileHeight,
                x + tileWidth,
                y + tileHeight / 2f,
            )
            x += tileWidth
        }
        drawPath(path, color = Color.White.copy(alpha = 0.18f * alpha), style = stroke)
        y += tileHeight
    }
}

private fun DrawScope.drawGrain(tileWidth: Float, tileHeight: Float, alpha: Float) {
    val random = Random(73)
    val tiles = max(1f, size.width * size.height / (tileWidth * tileHeight))
    val count = (tiles * 7f).toInt().coerceIn(30, 700)
    repeat(count) {
        val x = random.nextFloat() * size.width
        val y = random.nextFloat() * size.height
        val radius = (0.45f + random.nextFloat() * 0.9f).dp.toPx()
        drawCircle(
            color = Color.White.copy(alpha = (0.08f + random.nextFloat() * 0.12f) * alpha),
            radius = radius,
            center = Offset(x, y),
        )
    }
}

private fun DrawScope.drawPassImage(
    bitmap: Bitmap,
    imageBitmap: ImageBitmap,
    appearance: PassAppearance,
) {
    if (bitmap.width <= 0 || bitmap.height <= 0) {
        return
    }

    val drawWidth: Float
    val drawHeight: Float
    when (appearance.imageSize) {
        "contain" -> {
            val scale = min(size.width / bitmap.width, size.height / bitmap.height)
            drawWidth = bitmap.width * scale
            drawHeight = bitmap.height * scale
        }

        "50%" -> {
            drawWidth = size.width * 0.5f
            drawHeight = drawWidth * bitmap.height / bitmap.width
        }

        "100%" -> {
            drawWidth = size.width
            drawHeight = drawWidth * bitmap.height / bitmap.width
        }

        "200%" -> {
            drawWidth = size.width * 2f
            drawHeight = drawWidth * bitmap.height / bitmap.width
        }

        else -> {
            val scale = max(size.width / bitmap.width, size.height / bitmap.height)
            drawWidth = bitmap.width * scale
            drawHeight = bitmap.height * scale
        }
    }

    if (drawWidth <= 0f || drawHeight <= 0f) {
        return
    }

    val left = (size.width - drawWidth) / 2f
    val top = (size.height - drawHeight) / 2f
    val shade = Color.Black.copy(alpha = (appearance.imageShade / 100f).coerceIn(0f, 0.7f))

    when (appearance.imageRepeat) {
        "repeat" -> {
            drawRepeatedImage(bitmap, drawWidth, drawHeight, left, top, repeatX = true, repeatY = true)
            drawRect(shade)
        }

        "repeat-x" -> {
            val bandTop = top.coerceAtLeast(0f)
            val bandBottom = (top + drawHeight).coerceAtMost(size.height)
            clipRect(0f, bandTop, size.width, bandBottom) {
                drawRepeatedImage(bitmap, drawWidth, drawHeight, left, top, repeatX = true, repeatY = false)
                drawRect(shade)
            }
        }

        "repeat-y" -> {
            val bandLeft = left.coerceAtLeast(0f)
            val bandRight = (left + drawWidth).coerceAtMost(size.width)
            clipRect(bandLeft, 0f, bandRight, size.height) {
                drawRepeatedImage(bitmap, drawWidth, drawHeight, left, top, repeatX = false, repeatY = true)
                drawRect(shade)
            }
        }

        else -> {
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt()),
            )
            drawRect(
                color = shade,
                topLeft = Offset(left, top),
                size = Size(drawWidth, drawHeight),
            )
        }
    }
}

private fun DrawScope.drawRepeatedImage(
    bitmap: Bitmap,
    drawWidth: Float,
    drawHeight: Float,
    left: Float,
    top: Float,
    repeatX: Boolean,
    repeatY: Boolean,
) {
    val shader = BitmapShader(
        bitmap,
        if (repeatX) Shader.TileMode.REPEAT else Shader.TileMode.CLAMP,
        if (repeatY) Shader.TileMode.REPEAT else Shader.TileMode.CLAMP,
    )
    val matrix = Matrix().apply {
        setScale(drawWidth / bitmap.width, drawHeight / bitmap.height)
        postTranslate(left, top)
    }
    shader.setLocalMatrix(matrix)
    drawRect(brush = ShaderBrush(shader))
}
