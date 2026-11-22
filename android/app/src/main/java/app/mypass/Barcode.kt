package app.mypass

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

object Barcode {
    data class Decoded(
        val text: String,
        val format: String,
    )

    fun decode(bitmap: Bitmap): Decoded? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            return null
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binary = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.PDF_417,
                BarcodeFormat.AZTEC,
                BarcodeFormat.CODE_128,
                BarcodeFormat.DATA_MATRIX,
            ),
        )
        val reader = MultiFormatReader().apply { setHints(hints) }
        val result = runCatching { reader.decodeWithState(binary) }.getOrNull() ?: return null
        return Decoded(
            text = result.text.orEmpty(),
            format = when (result.barcodeFormat) {
                BarcodeFormat.PDF_417 -> "pdf417"
                BarcodeFormat.AZTEC -> "aztec"
                BarcodeFormat.CODE_128 -> "code128"
                BarcodeFormat.DATA_MATRIX -> "qr"
                else -> "qr"
            },
        )
    }

    fun generate(payload: String, format: String, size: Int = 720): Bitmap? {
        if (payload.isBlank()) {
            return null
        }

        val barcodeFormat = when (format) {
            "pdf417" -> BarcodeFormat.PDF_417
            "aztec" -> BarcodeFormat.AZTEC
            "code128" -> BarcodeFormat.CODE_128
            else -> BarcodeFormat.QR_CODE
        }

        return encode(payload, barcodeFormat, size)
            ?: if (barcodeFormat != BarcodeFormat.QR_CODE) {
                encode(payload, BarcodeFormat.QR_CODE, size)
            } else {
                null
            }
    }

    private fun encode(payload: String, barcodeFormat: BarcodeFormat, size: Int): Bitmap? {
        val width = size.coerceIn(240, 1200)
        val height = if (barcodeFormat == BarcodeFormat.CODE_128) (width / 4).coerceAtLeast(120) else width
        val hints = mutableMapOf<EncodeHintType, Any>(
            EncodeHintType.MARGIN to if (barcodeFormat == BarcodeFormat.PDF_417) 12 else 2,
        )
        if (barcodeFormat == BarcodeFormat.QR_CODE) {
            hints[EncodeHintType.ERROR_CORRECTION] = "M"
        } else if (barcodeFormat == BarcodeFormat.PDF_417) {
            hints[EncodeHintType.ERROR_CORRECTION] = 2
        }

        return runCatching {
            val matrix = MultiFormatWriter().encode(payload, barcodeFormat, width, height, hints)
            val pixels = IntArray(matrix.width * matrix.height)
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    pixels[y * matrix.width + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
            }
        }.getOrNull()
    }
}
