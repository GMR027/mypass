package app.mypass

import android.content.Context
import android.content.Intent
import android.net.Uri

object PassLocation {
    private fun clean(value: String?): String = value.orEmpty().trim()

    private fun coordinates(value: String?): String? {
        val text = clean(value)
        if (text.isBlank()) {
            return null
        }

        val direct = Regex("^\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*[,;]\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*$")
            .matchEntire(text)
        val fromUrl = Regex("[?&]q=(-?\\d{1,3}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)")
            .find(text)
            ?: Regex("@(-?\\d{1,3}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)")
                .find(text)
        val match = direct ?: fromUrl ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            return null
        }
        return "$lat,$lng"
    }

    fun query(pass: UiPass): String? {
        coordinates(pass.fields["coordinates"])?.let { return it }

        val parts = if (pass.category == "flight") {
            listOf(pass.fields["origin"], pass.fields["terminal"], pass.fields["airport"])
        } else {
            listOf(pass.fields["location"], pass.fields["address"], pass.fields["terminal"])
        }

        return parts.map(::clean).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }
    }

    fun open(context: Context, pass: UiPass) {
        val query = query(pass) ?: return
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val fallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"),
        )

        runCatching {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_maps)))
        }.onFailure {
            runCatching { context.startActivity(fallback) }
        }
    }
}
