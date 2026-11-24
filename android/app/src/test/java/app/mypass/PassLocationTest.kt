package app.mypass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PassLocationTest {
    @Test
    fun buildsQueryForEventAndTransit() {
        assertEquals(
            "Estadio Azteca, Calzada de Tlalpan 3465",
            PassLocation.query(pass("event", mapOf("location" to "Estadio Azteca", "address" to "Calzada de Tlalpan 3465"))),
        )
        assertEquals(
            "Estación Central, Andén 3",
            PassLocation.query(pass("transit", mapOf("location" to "Estación Central", "terminal" to "Andén 3"))),
        )
    }

    @Test
    fun prefersCoordinatesAndHandlesFlightCodes() {
        assertEquals(
            "19.3029,-99.1505",
            PassLocation.query(pass("event", mapOf("coordinates" to "19.3029, -99.1505", "location" to "Estadio"))),
        )
        assertEquals("MAD, T2", PassLocation.query(pass("flight", mapOf("origin" to "MAD", "terminal" to "T2"))))
        assertNull(PassLocation.query(pass("membership", emptyMap())))
    }

    private fun pass(category: String, fields: Map<String, String>): UiPass {
        return UiPass(
            category = category,
            title = "Pase",
            issuer = "",
            holder = "",
            fields = fields,
            notes = emptyList(),
            codePayload = null,
            codeFormat = "qr",
            codeSource = "generated",
            themeColor = "#0c2340",
            options = emptyMap(),
        )
    }
}
