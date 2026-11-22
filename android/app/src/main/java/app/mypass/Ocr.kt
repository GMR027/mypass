package app.mypass

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

object Ocr {
    private val languages = listOf("spa", "eng")
    private const val languageList = "spa+eng"

    private fun dataDir(context: Context): File {
        val dir = File(context.filesDir, "tesseract")
        val tessdata = File(dir, "tessdata")
        tessdata.mkdirs()

        for (language in languages) {
            val target = File(tessdata, "$language.traineddata")
            if (!target.exists() || target.length() < 1000) {
                context.assets.open("tessdata/$language.traineddata").use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }

        return dir
    }

    fun extract(context: Context, bitmap: Bitmap): String {
        val engine = TessBaseAPI()
        return try {
            if (!engine.init(dataDir(context).absolutePath, languageList)) {
                return ""
            }
            engine.setImage(bitmap)
            engine.getUTF8Text()?.trim().orEmpty()
        } finally {
            engine.recycle()
        }
    }
}
