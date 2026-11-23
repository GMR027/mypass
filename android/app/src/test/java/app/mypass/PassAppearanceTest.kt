package app.mypass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PassAppearanceTest {
    @Test
    fun appearance_defaults_are_safe_for_older_passes() {
        val appearance = normalizePassAppearance(
            texture = null,
            textureSize = null,
            textureRepeat = null,
            textureOpacity = null,
            image = null,
            imageSize = null,
            imageRepeat = null,
            imageShade = null,
        )

        assertEquals(PASS_TEXTURE_NONE, appearance.texture)
        assertEquals("md", appearance.textureSize)
        assertEquals("repeat", appearance.textureRepeat)
        assertEquals(100, appearance.textureOpacity)
        assertNull(appearance.image)
        assertEquals("cover", appearance.imageSize)
        assertEquals("no-repeat", appearance.imageRepeat)
        assertEquals(30, appearance.imageShade)
    }

    @Test
    fun appearance_keeps_supported_values() {
        val image = "data:image/webp;base64,AAAA"
        val appearance = normalizePassAppearance(
            texture = "waves",
            textureSize = "lg",
            textureRepeat = "repeat-x",
            textureOpacity = 45,
            image = image,
            imageSize = "50%",
            imageRepeat = "repeat-y",
            imageShade = 55,
        )

        assertEquals("waves", appearance.texture)
        assertEquals("lg", appearance.textureSize)
        assertEquals("repeat-x", appearance.textureRepeat)
        assertEquals(45, appearance.textureOpacity)
        assertEquals(image, appearance.image)
        assertEquals("50%", appearance.imageSize)
        assertEquals("repeat-y", appearance.imageRepeat)
        assertEquals(55, appearance.imageShade)
    }

    @Test
    fun appearance_rejects_unknown_and_unsafe_values() {
        val appearance = normalizePassAppearance(
            texture = "javascript",
            textureSize = "huge",
            textureRepeat = "sideways",
            textureOpacity = 500,
            image = "https://example.com/background.png",
            imageSize = "fit",
            imageRepeat = "sometimes",
            imageShade = 500,
        )

        assertEquals(PASS_TEXTURE_NONE, appearance.texture)
        assertEquals("md", appearance.textureSize)
        assertEquals("repeat", appearance.textureRepeat)
        assertEquals(100, appearance.textureOpacity)
        assertNull(appearance.image)
        assertEquals("cover", appearance.imageSize)
        assertEquals("no-repeat", appearance.imageRepeat)
        assertEquals(70, appearance.imageShade)
    }

    @Test
    fun appearance_accepts_jpeg_and_png_images_but_rejects_svg() {
        val jpeg = normalizePassAppearance(
            texture = "dots",
            textureSize = "sm",
            textureRepeat = "repeat",
            textureOpacity = 100,
            image = "data:image/jpeg;base64,AAAA",
            imageSize = "contain",
            imageRepeat = "no-repeat",
            imageShade = 20,
        )
        val svg = normalizePassAppearance(
            texture = "dots",
            textureSize = "sm",
            textureRepeat = "repeat",
            textureOpacity = 100,
            image = "data:image/svg+xml;base64,AAAA",
            imageSize = "contain",
            imageRepeat = "no-repeat",
            imageShade = 20,
        )

        assertEquals("data:image/jpeg;base64,AAAA", jpeg.image)
        assertNull(svg.image)
    }
}
