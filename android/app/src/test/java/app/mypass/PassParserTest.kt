package app.mypass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PassParserTest {
    @Test
    fun bcbp_parses_the_standard_fixed_width_payload() {
        val name = "FERNANDEZ/GUILLERMO".padEnd(20, ' ')
        val pnr = "ABC123".padEnd(7, ' ')
        val seat = "2A".padEnd(4, ' ')
        val payload = "M1${name}E${pnr}MADMEX${"IB".padEnd(3, ' ')}06401332Y${seat}001471"

        val parsed = PassParser.parseBcbp(payload, LocalDate.of(2026, 11, 22))

        assertNotNull(parsed)
        assertEquals("flight", parsed?.category)
        assertEquals("MAD", parsed?.fields?.get("origin"))
        assertEquals("MEX", parsed?.fields?.get("destination"))
        assertEquals("IB 6401", parsed?.fields?.get("flight"))
        assertEquals("2A", parsed?.fields?.get("seat"))
        assertEquals("2026-11-28", parsed?.fields?.get("date"))
        assertEquals("ABC123", parsed?.fields?.get("pnr"))
        assertEquals("FERNANDEZ/GUILLERMO", parsed?.holder)
    }

    @Test
    fun flight_text_extracts_route_flight_and_passenger() {
        val text = """
            IBERIA
            BOARDING PASS
            PASSENGER / PASAJERO: FERNANDEZ/GUILLERMO MR
            FLIGHT / VUELO: IB 6401
            FROM / ORIGEN: MAD Madrid Barajas T4S
            TO / DESTINO: MEX Ciudad de Mexico T1
            DATE / FECHA: 28 NOV 2026
            DEPARTURE / SALIDA: 17:45
            BOARDING / EMBARQUE: 17:05
            GATE / PUERTA: H12
            SEAT / ASIENTO: 2A
            ZONE / ZONA: 1
        """.trimIndent()

        val parsed = PassParser.parse(text, reference = LocalDate.of(2026, 11, 22))

        assertEquals("flight", parsed.category)
        assertEquals("MAD", parsed.fields["origin"])
        assertEquals("MEX", parsed.fields["destination"])
        assertEquals("IB 6401", parsed.fields["flight"])
        assertEquals("2A", parsed.fields["seat"])
        assertEquals("H12", parsed.fields["gate"])
        assertEquals("2026-11-28", parsed.fields["date"])
        assertTrue(parsed.holder.contains("FERNANDEZ"))
        assertTrue(parsed.confidence > 0.8f)
    }

    @Test
    fun date_parser_handles_spanish_and_iso_dates() {
        assertEquals("2026-11-28", PassParser.parseDate("28 NOV 2026"))
        assertEquals("2026-11-28", PassParser.parseDate("2026-11-28"))
        assertEquals("2026-11-28", PassParser.parseDate("28/11/2026"))
    }
}
