package app.mypass

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object PassParser {
    data class Parsed(
        val category: String,
        val title: String,
        val issuer: String,
        val holder: String,
        val fields: Map<String, String>,
        val notes: List<String> = emptyList(),
        val codePayload: String? = null,
        val codeFormat: String = "qr",
        val confidence: Float = 0.35f,
    )

    private val months = mapOf(
        "JAN" to 1, "FEB" to 2, "MAR" to 3, "APR" to 4, "MAY" to 5, "JUN" to 6,
        "JUL" to 7, "AUG" to 8, "SEP" to 9, "OCT" to 10, "NOV" to 11, "DEC" to 12,
        "ENE" to 1, "ABR" to 4, "AGO" to 8, "DIC" to 12,
    )

    private val airportWords = setOf(
        "THE", "AND", "FOR", "PDF", "PNG", "JPG", "WEB", "WWW", "COM", "NET", "ORG",
        "MRZ", "VIP", "GATE", "SEAT", "FLIGHT", "BOARD", "CLASS", "ECON", "BUSINESS",
        "FIRST", "TERM", "TERMINAL", "DATE", "TIME", "FROM", "TO", "NAME", "PASS",
        "TICKET", "TRAIN", "METRO", "BUS", "HOTEL", "ROOM", "CLUB", "MEMBER", "EVENT",
        "CITY", "ZONE", "ROW", "STATUS", "PAGE", "CODE", "TYPE", "TOTAL", "PRICE",
    )

    private val airlines = listOf(
        "IBERIA", "AEROMEXICO", "VUELING", "LATAM", "RYANAIR", "EASYJET", "AIR FRANCE",
        "KLM", "LUFTHANSA", "UNITED", "AMERICAN AIRLINES", "DELTA", "EMIRATES", "QATAR",
        "TURKISH", "AVIANCA", "COPA", "VOLARIS", "VIVA AEROBUS", "BRITISH AIRWAYS",
        "SWISS", "AUSTRIAN", "FINNAIR", "NORWEGIAN", "TAP", "AIR EUROPA", "LEVEL",
        "JETBLUE", "ALASKA", "SOUTHWEST", "ANA", "JAL", "SINGAPORE AIRLINES", "QANTAS",
    )

    fun parseBcbp(payload: String, reference: LocalDate = LocalDate.now()): Parsed? {
        val raw = payload.replace("\r", "").replace("\n", "").replace("\t", "").trim()
        val index = raw.indexOf("M1")
        if (index == -1 || raw.length < index + 58) {
            return null
        }

        val data = raw.substring(index)
        val name = data.substring(2, 22).trim()
        val pnr = data.substring(23, 30).trim()
        val origin = data.substring(30, 33).trim().uppercase(Locale.US)
        val destination = data.substring(33, 36).trim().uppercase(Locale.US)
        val carrier = data.substring(36, 39).trim().uppercase(Locale.US)
        val flightNumber = data.substring(39, 44).trim().trimStart('0')
        val julian = data.substring(44, 47).trim().toIntOrNull()
        val compartment = data.substring(47, 48).trim().uppercase(Locale.US)
        val seat = data.substring(48, 52).trim()
        val sequence = data.substring(52, 57).trim()

        if (!origin.matches(Regex("^[A-Z]{3}$")) || !destination.matches(Regex("^[A-Z]{3}$"))) {
            return null
        }

        val compartmentNames = mapOf(
            "F" to "First", "A" to "First",
            "C" to "Business", "D" to "Business", "J" to "Business",
            "W" to "Premium economy",
            "Y" to "Economy", "B" to "Economy", "M" to "Economy", "K" to "Economy",
            "L" to "Economy", "Q" to "Economy", "S" to "Economy", "V" to "Economy",
        )

        val date = julian?.takeIf { it in 1..366 }?.let { day ->
            var value = runCatching { LocalDate.ofYearDay(reference.year, day) }.getOrNull()
            if (value != null && value.isBefore(reference.minusDays(180))) {
                value = runCatching { LocalDate.ofYearDay(reference.year + 1, day) }.getOrNull()
            }
            value?.toString()
        }.orEmpty()

        val fields = linkedMapOf(
            "origin" to origin,
            "destination" to destination,
            "flight" to listOf(carrier, flightNumber).filter { it.isNotBlank() }.joinToString(" "),
            "carrier" to carrier,
            "flight_number" to flightNumber,
            "passenger" to name,
            "seat" to seat,
            "zone" to (compartmentNames[compartment] ?: compartment),
            "cabin" to (compartmentNames[compartment] ?: compartment),
            "date" to date,
            "pnr" to pnr,
            "sequence" to sequence,
        ).filterValues { it.isNotBlank() }

        return Parsed(
            category = "flight",
            title = if (fields["flight"].isNullOrBlank()) {
                "$origin → $destination"
            } else {
                "$origin → $destination · ${fields["flight"]}"
            },
            issuer = carrier,
            holder = name,
            fields = fields,
            codePayload = payload.trim(),
            codeFormat = "pdf417",
            confidence = 0.98f,
        )
    }

    fun parse(
        text: String,
        categoryHint: String = "flight",
        reference: LocalDate = LocalDate.now(),
    ): Parsed {
        val category = detectCategory(text, categoryHint)
        return when (category) {
            "flight" -> parseFlight(text, reference)
            "transit" -> parseGeneric(text, "transit", reference)
            "id" -> parseGeneric(text, "id", reference)
            "membership" -> parseGeneric(text, "membership", reference)
            else -> parseGeneric(text, "event", reference)
        }.copy(category = category)
    }

    fun detectCategory(text: String, fallback: String = "flight"): String {
        val normalized = normalize(text)
        val scores = mapOf(
            "flight" to listOf(
                "BOARDING", "FLIGHT", "VUELO", "GATE", "PUERTA", "SEAT", "ASIENTO",
                "AIRLINE", "AEROLINEA", "DEPARTURE", "SALIDA", "TERMINAL", "PNR",
            ),
            "event" to listOf(
                "EVENT", "EVENTO", "CONCERT", "CONCIERTO", "ENTRADA", "VENUE", "RECINTO",
                "ADMISSION", "ADMISION", "SECTION", "SECCION", "ROW", "FILA", "FESTIVAL",
            ),
            "transit" to listOf(
                "TRANSIT", "TRANSPORTE", "TRAIN", "TREN", "METRO", "SUBWAY", "BUS",
                "PLATFORM", "ANDEN", "CARRIAGE", "COCHE", "STATION", "ESTACION",
            ),
            "id" to listOf(
                "PASSPORT", "PASAPORTE", "IDENTITY", "IDENTIDAD", "DRIVER", "LICENCIA",
                "LICENSE", "DNI", "CURP", "BIRTH", "NACIMIENTO", "NATIONALITY",
            ),
            "membership" to listOf(
                "MEMBERSHIP", "MEMBRESIA", "MEMBER", "SOCIO", "CLUB", "LOYALTY",
                "REWARDS", "POINTS", "PUNTOS", "LEVEL", "NIVEL", "TIER",
            ),
        )
        var best = fallback
        var bestScore = scores[fallback]?.count { normalized.contains(it) } ?: 0
        for ((category, words) in scores) {
            if (category == fallback) {
                continue
            }
            val score = words.count { normalized.contains(it) }
            if (score > bestScore) {
                best = category
                bestScore = score
            }
        }
        return best
    }

    private fun parseFlight(text: String, reference: LocalDate): Parsed {
        val lines = meaningfulLines(text)
        val normalized = normalize(text)
        val fields = linkedMapOf<String, String>()

        val airports = Regex("\\b[A-Z]{3}\\b")
            .findAll(normalized)
            .map { it.value }
            .filter { it !in airportWords }
            .distinct()
            .take(2)
            .toList()
        if (airports.getOrNull(0) != null) {
            fields["origin"] = airports[0]
        }
        if (airports.getOrNull(1) != null) {
            fields["destination"] = airports[1]
        }

        Regex("\\b([A-Z]{2})\\s?(\\d{2,4})\\b").find(normalized)?.let { match ->
            fields["carrier"] = match.groupValues[1]
            fields["flight_number"] = match.groupValues[2].trimStart('0')
            fields["flight"] = "${match.groupValues[1]} ${match.groupValues[2].trimStart('0')}"
        }

        val dateMatch = Regex(
            "\\b(\\d{1,2}\\s+[A-Z]{3}\\.?\\s+\\d{4}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})\\b",
        ).find(normalized)
        dateMatch?.value?.let { parseDate(it, reference) }?.let { fields["date"] = it }

        val times = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b").findAll(normalized)
            .map { "${it.groupValues[1].padStart(2, '0')}:${it.groupValues[2]}" }
            .toList()
        times.getOrNull(0)?.let { fields["departure"] = it }
        times.getOrNull(1)?.let {
            fields["boarding_start"] = it
            fields["boarding"] = it
        }

        val boardingRange = Regex(
            "\\b([01]?\\d|2[0-3]):([0-5]\\d)\\s*(?:-|–|—|TO|A)\\s*([01]?\\d|2[0-3]):([0-5]\\d)\\b",
        ).find(normalized)
        boardingRange?.let {
            fields["boarding_start"] = "${it.groupValues[1].padStart(2, '0')}:${it.groupValues[2]}"
            fields["boarding_end"] = "${it.groupValues[3].padStart(2, '0')}:${it.groupValues[4]}"
            fields["boarding"] = fields["boarding_start"].orEmpty()
        }

        labeledValue(lines, listOf("SEAT", "ASIENTO"))?.let { fields["seat"] = it }
        labeledValue(lines, listOf("GATE", "PUERTA"))?.let { fields["gate"] = it }
        labeledValue(lines, listOf("TERMINAL", "TERM"))?.let { fields["terminal"] = it }
        labeledValue(lines, listOf("ZONE", "ZONA", "GROUP", "GRUPO"))?.let { fields["zone"] = it }
        labeledValue(lines, listOf("BOARDING", "EMBARQUE", "ABORDAJE", "INICIO DE ABORDAJE"))
            ?.let { parseTime(it) }
            ?.let {
                fields["boarding_start"] = it
                fields["boarding"] = it
            }
        labeledValue(lines, listOf("BOARDING ENDS", "BOARDING CLOSES", "BOARDING END", "CIERRE DE ABORDAJE", "FIN DE ABORDAJE"))
            ?.let { parseTime(it) }
            ?.let { fields["boarding_end"] = it }
        labeledValue(lines, listOf("DEPARTURE", "SALIDA"))
            ?.let { parseTime(it) }
            ?.let { fields["departure"] = it }
        labeledValue(lines, listOf("PNR", "RESERVA", "BOOKING"))
            ?.let { fields["pnr"] = it }

        val holder = detectHolder(lines)
        holder?.let {
            fields["passenger"] = it
            fields["passenger_name"] = it
        }

        val issuer = airlines.firstOrNull { normalized.contains(it) } ?: fields["carrier"].orEmpty()
        val route = listOf(fields["origin"], fields["destination"])
            .filterNot { it.isNullOrBlank() }
            .joinToString(" → ")
        val title = listOf(route, fields["flight"].orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { meaningfulLines(text).firstOrNull()?.take(90) ?: "Pase de vuelo" }

        return Parsed(
            category = "flight",
            title = title,
            issuer = issuer,
            holder = holder.orEmpty(),
            fields = fields,
            confidence = confidence(
                fields,
                listOf("origin", "destination", "flight", "date", "passenger", "seat", "terminal", "boarding_start"),
            ),
        )
    }

    private fun parseGeneric(text: String, category: String, reference: LocalDate): Parsed {
        val lines = meaningfulLines(text)
        val normalized = normalize(text)
        val fields = linkedMapOf<String, String>()
        val holder = detectHolder(lines)

        val dateMatch = Regex(
            "\\b(\\d{1,2}\\s+[A-Z]{3}\\.?\\s+\\d{4}|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})\\b",
        ).find(normalized)
        dateMatch?.value?.let { parseDate(it, reference) }?.let { fields["date"] = it }
        parseTime(normalized)?.let { fields["time"] = it }

        labeledValue(lines, listOf("VENUE", "RECINTO", "LOCATION", "UBICACION", "LUGAR"))?.let {
            fields["location"] = it
        }
        labeledValue(lines, listOf("ADDRESS", "DIRECCION", "CALLE", "STREET"))?.let {
            fields["address"] = it
        }
        findCoordinates(text)?.let { fields["coordinates"] = it }
        labeledValue(lines, listOf("GATE", "PUERTA", "ACCESS", "ACCESO"))?.let { fields["gate"] = it }
        labeledValue(lines, listOf("SECTION", "SECCION"))?.let { fields["section"] = it }
        labeledValue(lines, listOf("ROW", "FILA"))?.let { fields["row"] = it }
        labeledValue(lines, listOf("SEAT", "ASIENTO", "PLAZA"))?.let { fields["seat"] = it }
        labeledValue(lines, listOf("PLATFORM", "ANDEN", "VIA"))?.let { fields["platform"] = it }
        labeledValue(lines, listOf("TERMINAL", "TERM", "STATION", "ESTACION"))?.let {
            fields["terminal"] = it
            if (category == "transit") {
                fields["location"] = it
            }
        }
        labeledValue(lines, listOf("CARRIAGE", "COCHE", "VAGON"))?.let { fields["carriage"] = it }
        labeledValue(lines, listOf("ROUTE", "RUTA", "LINEA", "LINE"))?.let { fields["route"] = it }
        labeledValue(lines, listOf("BOARDING", "EMBARQUE", "ABORDAJE", "INICIO DE ABORDAJE"))
            ?.let { parseTime(it) }
            ?.let { fields["boarding_start"] = it }
        labeledValue(lines, listOf("BOARDING ENDS", "BOARDING CLOSES", "BOARDING END", "CIERRE DE ABORDAJE", "FIN DE ABORDAJE"))
            ?.let { parseTime(it) }
            ?.let { fields["boarding_end"] = it }
        Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\s*(?:-|–|—|TO|A)\\s*([01]?\\d|2[0-3]):([0-5]\\d)\\b")
            .find(normalized)?.let {
                fields["boarding_start"] = "${it.groupValues[1].padStart(2, '0')}:${it.groupValues[2]}"
                fields["boarding_end"] = "${it.groupValues[3].padStart(2, '0')}:${it.groupValues[4]}"
            }
        holder?.let {
            fields[if (category == "flight") "passenger" else "holder"] = it
        }

        val number = Regex("\\b(?:NO|NUMERO|NUMBER|DOCUMENTO|DOCUMENT|ID)\\s*[:#-]?\\s*([A-Z0-9-]{5,20})\\b")
            .find(normalized)?.groupValues?.getOrNull(1)
        number?.let { fields["number"] = it }
        val expires = Regex("\\b(?:EXPIRA|EXPIRES|VENCE|VALID UNTIL|VALIDO HASTA)\\s*[:#-]?\\s*([0-9A-Z/.-]{4,})\\b")
            .find(normalized)?.groupValues?.getOrNull(1)
        expires?.let { parseDate(it, reference)?.let { date -> fields["expires"] = date } }
        labeledValue(lines, listOf("LEVEL", "NIVEL", "TIER", "CATEGORIA", "CATEGORY"))
            ?.let { fields["level"] = it }

        val title = meaningfulLines(text).firstOrNull()?.take(90) ?: "Pase"
        return Parsed(
            category = category,
            title = title,
            issuer = "",
            holder = holder.orEmpty(),
            fields = fields,
            confidence = confidence(
                fields,
                if (category == "event") {
                    listOf("date", "time", "location", "gate", "seat")
                } else {
                    listOf("date", "holder", "number", "expires")
                },
            ),
        )
    }

    fun parseDate(value: String, reference: LocalDate = LocalDate.now()): String? {
        val raw = value.trim().uppercase(Locale.US)

        Regex("\\b(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})\\b").find(raw)?.let { match ->
            return runCatching {
                LocalDate.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
            }.getOrNull()?.toString()
        }

        Regex("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2,4})\\b").find(raw)?.let { match ->
            var year = match.groupValues[3].toInt()
            if (year < 100) {
                year += if (year > 70) 1900 else 2000
            }
            val first = match.groupValues[1].toInt()
            val second = match.groupValues[2].toInt()
            val dayFirst = first > 12 || (second <= 12 && Locale.getDefault().language.startsWith("es"))
            val day = if (dayFirst) first else second
            val month = if (dayFirst) second else first
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()?.toString()
        }

        Regex("\\b(\\d{1,2})\\s+([A-Z]{3})\\.?\\s+(\\d{4})\\b").find(raw)?.let { match ->
            val month = months[match.groupValues[2]] ?: return@let
            return runCatching {
                LocalDate.of(match.groupValues[3].toInt(), month, match.groupValues[1].toInt())
            }.getOrNull()?.toString()
        }

        Regex("\\b([A-Z]{3})\\.?\\s+(\\d{1,2}),?\\s+(\\d{4})\\b").find(raw)?.let { match ->
            val month = months[match.groupValues[1]] ?: return@let
            return runCatching {
                LocalDate.of(match.groupValues[3].toInt(), month, match.groupValues[2].toInt())
            }.getOrNull()?.toString()
        }

        return runCatching {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.parse(value)?.toInstant()?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()?.toString()
        }.getOrNull()
            ?: runCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).toString() }.getOrNull()
    }

    fun parseTime(value: String): String? {
        val match = Regex("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b").find(value) ?: return null
        return "${match.groupValues[1].padStart(2, '0')}:${match.groupValues[2]}"
    }

    private fun findCoordinates(text: String): String? {
        val match = Regex("[?&]q=(-?\\d{1,3}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)").find(text)
            ?: Regex("@(-?\\d{1,3}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)").find(text)
            ?: Regex("\\b(-?\\d{1,3}\\.\\d{3,})\\s*[,;]\\s*(-?\\d{1,3}\\.\\d{3,})\\b").find(text)
            ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            return null
        }
        return "$lat,$lng"
    }

    private fun labeledValue(lines: List<String>, labels: List<String>): String? {
        val normalizedLabels = labels.map { normalize(it) }
        for ((index, line) in lines.withIndex()) {
            val normalized = normalize(line)
            val labelIndex = normalizedLabels.indexOfFirst { normalized.contains(it) }
            if (labelIndex == -1) {
                continue
            }
            val raw = line.trim()
            val separator = raw.indexOfAny(charArrayOf(':', '#', '-'))
            if (separator > 0 && separator < raw.length - 1) {
                return raw.substring(separator + 1).trim().takeIf { it.isNotBlank() }
            }
            val label = labels[labelIndex]
            val position = raw.lowercase(Locale.getDefault()).indexOf(label.lowercase(Locale.getDefault()))
            if (position >= 0) {
                val after = raw.substring(position + label.length).trim().trimStart(':', '#', '-').trim()
                if (after.isNotBlank()) {
                    return after
                }
            }
            if (index + 1 < lines.size && lines[index + 1].length <= 80) {
                return lines[index + 1].trim()
            }
        }
        return null
    }

    private fun detectHolder(lines: List<String>): String? {
        labeledValue(
            lines,
            listOf(
                "PASSENGER", "PASAJERO", "NAME OF PASSENGER", "NOMBRE DEL PASAJERO",
                "HOLDER", "TITULAR", "NAME", "NOMBRE", "SOCIO", "MEMBER",
            ),
        )?.let { return it }

        return lines.firstOrNull { it.matches(Regex("^[A-ZÁÉÍÓÚÑ ]{2,}\\s*/\\s*[A-ZÁÉÍÓÚÑ ]{2,}$")) }
    }

    private fun confidence(fields: Map<String, String>, required: List<String>): Float {
        val filled = required.count { !fields[it].isNullOrBlank() }
        return (0.35f + (filled.toFloat() / required.size) * 0.6f).coerceAtMost(0.95f)
    }

    private fun meaningfulLines(text: String): List<String> {
        return text.replace("\r", "\n")
            .split('\n')
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.length >= 2 }
    }

    private fun normalize(value: String): String {
        return value
            .replace("\r", "\n")
            .uppercase(Locale.getDefault())
            .replace(Regex("[ÁÀÄÂ]"), "A")
            .replace(Regex("[ÉÈËÊ]"), "E")
            .replace(Regex("[ÍÌÏÎ]"), "I")
            .replace(Regex("[ÓÒÖÔ]"), "O")
            .replace(Regex("[ÚÙÜÛ]"), "U")
            .replace("Ñ", "N")
    }
}
