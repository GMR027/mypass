package app.mypass

import org.json.JSONObject

internal const val PASS_TEXTURE_NONE = "none"
internal const val PASS_TEXTURE_SIZE_DEFAULT = "md"
internal const val PASS_TEXTURE_REPEAT_DEFAULT = "repeat"
internal const val PASS_TEXTURE_OPACITY_DEFAULT = 100
internal const val PASS_IMAGE_SIZE_DEFAULT = "cover"
internal const val PASS_IMAGE_REPEAT_DEFAULT = "no-repeat"
internal const val PASS_IMAGE_SHADE_DEFAULT = 30
internal const val PASS_IMAGE_SHADE_MAX = 70
internal const val MAX_BACKGROUND_DATA_URL = 2_500_000

internal data class PassTextureSpec(
    val id: String,
    val labelRes: Int,
    val widthDp: Float,
    val heightDp: Float,
)

internal val PASS_TEXTURE_SPECS = listOf(
    PassTextureSpec("dots", R.string.pass_texture_dots, 18f, 18f),
    PassTextureSpec("lines", R.string.pass_texture_lines, 16f, 16f),
    PassTextureSpec("grid", R.string.pass_texture_grid, 24f, 24f),
    PassTextureSpec("carbon", R.string.pass_texture_carbon, 18f, 18f),
    PassTextureSpec("waves", R.string.pass_texture_waves, 80f, 36f),
    PassTextureSpec("grain", R.string.pass_texture_grain, 160f, 160f),
)

internal val PASS_TEXTURE_SIZES = linkedMapOf(
    "sm" to 0.72f,
    "md" to 1f,
    "lg" to 1.55f,
)

internal val PASS_REPEAT_VALUES = setOf("repeat", "repeat-x", "repeat-y", "no-repeat")
internal val PASS_IMAGE_SIZES = setOf("cover", "contain", "50%", "100%", "200%")

private val BACKGROUND_IMAGE_PATTERN = Regex(
    "^data:image/(?:png|jpe?g|webp);base64,[A-Za-z0-9+/=]+$",
    RegexOption.IGNORE_CASE,
)

data class PassAppearance(
    val texture: String = PASS_TEXTURE_NONE,
    val textureSize: String = PASS_TEXTURE_SIZE_DEFAULT,
    val textureRepeat: String = PASS_TEXTURE_REPEAT_DEFAULT,
    val textureOpacity: Int = PASS_TEXTURE_OPACITY_DEFAULT,
    val image: String? = null,
    val imageSize: String = PASS_IMAGE_SIZE_DEFAULT,
    val imageRepeat: String = PASS_IMAGE_REPEAT_DEFAULT,
    val imageShade: Int = PASS_IMAGE_SHADE_DEFAULT,
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("texture", texture)
            .put("textureSize", textureSize)
            .put("textureRepeat", textureRepeat)
            .put("textureOpacity", textureOpacity)
            .put("image", image ?: JSONObject.NULL)
            .put("imageSize", imageSize)
            .put("imageRepeat", imageRepeat)
            .put("imageShade", imageShade)
    }

    companion object {
        fun fromJson(json: JSONObject?): PassAppearance {
            return normalizePassAppearance(
                texture = json?.optString("texture"),
                textureSize = json?.optString("textureSize"),
                textureRepeat = json?.optString("textureRepeat"),
                textureOpacity = json?.optInt("textureOpacity", PASS_TEXTURE_OPACITY_DEFAULT),
                image = json?.optString("image"),
                imageSize = json?.optString("imageSize"),
                imageRepeat = json?.optString("imageRepeat"),
                imageShade = json?.optInt("imageShade", PASS_IMAGE_SHADE_DEFAULT),
            )
        }
    }
}

internal fun normalizePassAppearance(
    texture: String?,
    textureSize: String?,
    textureRepeat: String?,
    textureOpacity: Int?,
    image: String?,
    imageSize: String?,
    imageRepeat: String?,
    imageShade: Int?,
): PassAppearance {
    val safeTexture = texture
        ?.lowercase()
        ?.takeIf { value -> value == PASS_TEXTURE_NONE || PASS_TEXTURE_SPECS.any { it.id == value } }
        ?: PASS_TEXTURE_NONE
    val safeTextureSize = textureSize
        ?.lowercase()
        ?.takeIf { PASS_TEXTURE_SIZES.containsKey(it) }
        ?: PASS_TEXTURE_SIZE_DEFAULT
    val safeTextureRepeat = textureRepeat
        ?.lowercase()
        ?.takeIf { it in PASS_REPEAT_VALUES }
        ?: PASS_TEXTURE_REPEAT_DEFAULT
    val safeTextureOpacity = (textureOpacity ?: PASS_TEXTURE_OPACITY_DEFAULT).coerceIn(0, 100)
    val safeImage = image
        ?.takeIf { it.length <= MAX_BACKGROUND_DATA_URL && BACKGROUND_IMAGE_PATTERN.matches(it) }
    val safeImageSize = imageSize
        ?.lowercase()
        ?.takeIf { it in PASS_IMAGE_SIZES }
        ?: PASS_IMAGE_SIZE_DEFAULT
    val safeImageRepeat = imageRepeat
        ?.lowercase()
        ?.takeIf { it in PASS_REPEAT_VALUES }
        ?: PASS_IMAGE_REPEAT_DEFAULT
    val safeShade = (imageShade ?: PASS_IMAGE_SHADE_DEFAULT).coerceIn(0, PASS_IMAGE_SHADE_MAX)

    return PassAppearance(
        texture = safeTexture,
        textureSize = safeTextureSize,
        textureRepeat = safeTextureRepeat,
        textureOpacity = safeTextureOpacity,
        image = safeImage,
        imageSize = safeImageSize,
        imageRepeat = safeImageRepeat,
        imageShade = safeShade,
    )
}
