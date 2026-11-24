const MONTHS = {
    JAN: 1, FEB: 2, MAR: 3, APR: 4, MAY: 5, JUN: 6,
    JUL: 7, AUG: 8, SEP: 9, OCT: 10, NOV: 11, DEC: 12,
};

const AIRPORT_WORDS = new Set([
    'THE', 'AND', 'FOR', 'PDF', 'PNG', 'JPG', 'WEB', 'WWW', 'COM', 'NET', 'ORG',
    'MRZ', 'VIP', 'GATE', 'SEAT', 'FLIGHT', 'BOARD', 'CLASS', 'ECON', 'BUSINESS',
    'FIRST', 'TERM', 'TERMINAL', 'DATE', 'TIME', 'FROM', 'TO', 'NAME', 'PASS',
    'TICKET', 'TRAIN', 'METRO', 'BUS', 'HOTEL', 'ROOM', 'CLUB', 'MEMBER', 'EVENT',
    'CITY', 'ZONE', 'ROW', 'STATUS', 'PAGE', 'CODE', 'TYPE', 'TOTAL', 'PRICE',
]);

const AIRLINES = [
    'IBERIA', 'AEROMEXICO', 'VUELING', 'LATAM', 'RYANAIR', 'EASYJET', 'AIR FRANCE',
    'KLM', 'LUFTHANSA', 'UNITED', 'AMERICAN AIRLINES', 'DELTA', 'EMIRATES', 'QATAR',
    'TURKISH', 'AVIANCA', 'COPA', 'VOLARIS', 'VIVA AEROBUS', 'BRITISH AIRWAYS',
    'SWISS', 'AUSTRIAN', 'FINNAIR', 'NORWEGIAN', 'TAP', 'AIR EUROPA', 'LEVEL',
    'JETBLUE', 'ALASKA', 'SOUTHWEST', 'ANA', 'JAL', 'SINGAPORE AIRLINES', 'QANTAS',
];

const CATEGORY_KEYWORDS = {
    flight: [
        'BOARDING', 'BOARDING PASS', 'FLIGHT', 'VUELO', 'GATE', 'PUERTA', 'SEAT',
        'ASIENTO', 'AIRLINE', 'AEROLINEA', 'DEPARTURE', 'SALIDA', 'ARRIVAL', 'LLEGADA',
        'TERMINAL', 'PNR', 'ETKT',
    ],
    event: [
        'EVENT', 'EVENTO', 'CONCERT', 'CONCIERTO', 'TICKET', 'ENTRADA', 'VENUE',
        'RECINTO', 'ADMISSION', 'ADMISION', 'SECTION', 'SECCION', 'ROW', 'FILA',
        'DOORS', 'PUERTAS', 'SHOW', 'FESTIVAL',
    ],
    transit: [
        'TRANSIT', 'TRANSPORTE', 'TRAIN', 'TREN', 'METRO', 'SUBWAY', 'BUS', 'AUTOBUS',
        'PLATFORM', 'ANDEN', 'CARRIAGE', 'COCHE', 'STATION', 'ESTACION', 'ROUTE', 'RUTA',
        'VALID', 'VALIDO', 'FARE', 'TARIFA',
    ],
    id: [
        'PASSPORT', 'PASAPORTE', 'IDENTITY', 'IDENTIDAD', 'DRIVER', 'LICENCIA',
        'LICENSE', 'DNI', 'CURP', 'BIRTH', 'NACIMIENTO', 'NATIONALITY', 'NACIONALIDAD',
        'EXPIRES', 'VENCE', 'HOLDER', 'TITULAR',
    ],
    membership: [
        'MEMBERSHIP', 'MEMBRESIA', 'MEMBER', 'SOCIO', 'CLUB', 'LOYALTY', 'REWARDS',
        'POINTS', 'PUNTOS', 'LEVEL', 'NIVEL', 'TIER', 'CARD', 'TARJETA', 'BENEFITS',
        'BENEFICIOS',
    ],
};

function cleanText(value) {
    return String(value ?? '')
        .replace(/\r/g, '\n')
        .replace(/[ \t]+/g, ' ')
        .replace(/\n{3,}/g, '\n\n')
        .trim();
}

function normalize(value) {
    return cleanText(value)
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toUpperCase();
}

function textLines(text) {
    return cleanText(text)
        .split('\n')
        .map((line) => line.trim())
        .filter(Boolean);
}

function parseDate(value, reference = new Date()) {
    if (!value) {
        return null;
    }

    const raw = String(value).trim().toUpperCase();
    let match = raw.match(/\b(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})\b/);
    if (match) {
        return `${match[1]}-${match[2].padStart(2, '0')}-${match[3].padStart(2, '0')}`;
    }

    match = raw.match(/\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})\b/);
    if (match) {
        let year = Number(match[3]);
        if (year < 100) {
            year += year > 70 ? 1900 : 2000;
        }
        const day = Number(match[1]);
        const month = Number(match[2]);
        if (day >= 1 && day <= 31 && month >= 1 && month <= 12) {
            return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        }
    }

    match = raw.match(/\b(\d{1,2})\s+([A-Z]{3})\.?\s+(\d{4})\b/);
    if (match && MONTHS[match[2]]) {
        return `${match[3]}-${String(MONTHS[match[2]]).padStart(2, '0')}-${match[1].padStart(2, '0')}`;
    }

    match = raw.match(/\b([A-Z]{3})\.?\s+(\d{1,2}),?\s+(\d{4})\b/);
    if (match && MONTHS[match[1]]) {
        return `${match[3]}-${String(MONTHS[match[1]]).padStart(2, '0')}-${match[2].padStart(2, '0')}`;
    }

    const fallback = new Date(value);
    if (!Number.isNaN(fallback.getTime())) {
        return fallback.toISOString().slice(0, 10);
    }

    return null;
}

function parseTime(value) {
    if (!value) {
        return null;
    }

    const match = String(value).match(/\b([01]?\d|2[0-3]):([0-5]\d)\b/);
    if (!match) {
        return null;
    }

    return `${match[1].padStart(2, '0')}:${match[2]}`;
}

function parseJulianDate(value, reference = new Date()) {
    const julian = Number(value);
    if (!Number.isFinite(julian) || julian < 1 || julian > 366) {
        return null;
    }

    const date = new Date(reference.getFullYear(), 0, julian);
    if (Number.isNaN(date.getTime())) {
        return null;
    }

    return date.toISOString().slice(0, 10);
}

function parseBcbp(payload, reference = new Date()) {
    if (!payload) {
        return null;
    }

    const raw = String(payload).replace(/[\r\n\t]/g, '').trim();
    const index = raw.indexOf('M1');
    if (index === -1 || raw.length < index + 58) {
        return null;
    }

    const data = raw.slice(index);
    const name = data.slice(2, 22).trim();
    const pnr = data.slice(23, 30).trim();
    const origin = data.slice(30, 33).trim().toUpperCase();
    const destination = data.slice(33, 36).trim().toUpperCase();
    const carrier = data.slice(36, 39).trim().toUpperCase();
    const flightNumber = data.slice(39, 44).trim().replace(/^0+/, '');
    const julian = data.slice(44, 47).trim();
    const compartment = data.slice(47, 48).trim().toUpperCase();
    const seat = data.slice(48, 52).trim();
    const sequence = data.slice(52, 57).trim();

    if (!/^[A-Z]{3}$/.test(origin) || !/^[A-Z]{3}$/.test(destination)) {
        return null;
    }

    const compartmentNames = {
        F: 'First',
        A: 'First',
        C: 'Business',
        D: 'Business',
        J: 'Business',
        W: 'Premium economy',
        Y: 'Economy',
        B: 'Economy',
        M: 'Economy',
        K: 'Economy',
        L: 'Economy',
        Q: 'Economy',
        S: 'Economy',
        V: 'Economy',
    };

    const fields = {
        origin,
        destination,
        flight: [carrier, flightNumber].filter(Boolean).join(' '),
        carrier,
        flight_number: flightNumber,
        passenger: name,
        passenger_name: name,
        seat,
        zone: compartmentNames[compartment] || compartment,
        cabin: compartmentNames[compartment] || compartment,
        date: parseJulianDate(julian, reference),
        pnr,
        sequence,
    };

    return {
        category: 'flight',
        title: fields.flight ? `${origin} → ${destination} · ${fields.flight}` : `${origin} → ${destination}`,
        issuer: carrier,
        holder: name,
        fields,
        notes: [],
        code: {
            payload: String(payload).trim(),
            format: 'pdf417',
            source: 'decoded',
        },
        confidence: 0.98,
        detectedFields: countFilled(fields),
    };
}

function countFilled(object) {
    return Object.values(object).filter((value) => value !== null && value !== undefined && value !== '').length;
}

function findLabeledValue(lines, labels) {
    const normalizedLabels = labels.map((label) => normalize(label));
    for (let index = 0; index < lines.length; index += 1) {
        const line = normalize(lines[index]);
        const labelIndex = normalizedLabels.findIndex((label) => line.includes(label));
        if (labelIndex === -1) {
            continue;
        }

        const rawLine = lines[index];
        const separator = rawLine.match(/[:#-]\s*(.+)$/);
        if (separator && separator[1].trim()) {
            return separator[1].trim();
        }

        const afterLabel = rawLine.slice(rawLine.toLowerCase().indexOf(labels[labelIndex].toLowerCase()) + labels[labelIndex].length).trim();
        if (afterLabel) {
            return afterLabel.replace(/^[:#-]\s*/, '').trim();
        }

        if (lines[index + 1] && lines[index + 1].length <= 80) {
            return lines[index + 1].trim();
        }
    }

    return null;
}

function findFirstMatch(lines, patterns) {
    for (const line of lines) {
        for (const pattern of patterns) {
            const match = line.match(pattern);
            if (match) {
                return match;
            }
        }
    }

    return null;
}

function detectCategory(text) {
    const normalized = normalize(text);
    let best = 'event';
    let bestScore = 0;

    for (const [category, keywords] of Object.entries(CATEGORY_KEYWORDS)) {
        const score = keywords.reduce((total, keyword) => total + (normalized.includes(keyword) ? 1 : 0), 0);
        if (score > bestScore) {
            best = category;
            bestScore = score;
        }
    }

    return bestScore === 0 ? 'event' : best;
}

function detectAirline(text) {
    const normalized = normalize(text);
    return AIRLINES.find((airline) => normalized.includes(airline)) || null;
}

function detectAirports(text) {
    const normalized = normalize(text);
    const candidates = [];
    const matches = normalized.matchAll(/\b([A-Z]{3})\b/g);
    for (const match of matches) {
        const code = match[1];
        if (AIRPORT_WORDS.has(code) || candidates.includes(code)) {
            continue;
        }
        candidates.push(code);
        if (candidates.length === 4) {
            break;
        }
    }

    return candidates;
}

function detectPersonName(lines) {
    const labeled = findLabeledValue(lines, [
        'PASSENGER', 'PASAJERO', 'NAME OF PASSENGER', 'NOMBRE DEL PASAJERO',
        'HOLDER', 'TITULAR', 'NAME', 'NOMBRE', 'SOCIO', 'MEMBER',
    ]);
    if (labeled) {
        return labeled.replace(/\s{2,}/g, ' ').trim();
    }

    for (const line of lines) {
        const surname = line.match(/\b([A-ZÁÉÍÓÚÑ]{2,})\s*\/\s*([A-ZÁÉÍÓÚÑ ]{2,})\b/);
        if (surname) {
            return `${surname[1]}/${surname[2]}`.trim();
        }
    }

    return null;
}

function detectCode(text) {
    const normalized = normalize(text);
    const flight = normalized.match(/\b([A-Z]{2})\s?(\d{2,4})\b/);
    if (flight) {
        return {
            payload: `${flight[1]}${flight[2]}`,
            format: 'qr',
            source: 'generated',
        };
    }

    const pnr = normalized.match(/\b(?:PNR|RESERVA|BOOKING|CONFIRMATION)\s*[:#-]?\s*([A-Z0-9]{5,8})\b/);
    if (pnr) {
        return {
            payload: pnr[1],
            format: 'qr',
            source: 'generated',
        };
    }

    return null;
}

function parseFlightText(text, reference = new Date()) {
    const lines = textLines(text);
    const normalized = normalize(text);
    const fields = {};
    const airports = detectAirports(text);
    const flight = normalized.match(/\b([A-Z]{2})\s?(\d{2,4})\b/);
    const dateMatch = normalized.match(/\b(\d{1,2}\s+[A-Z]{3}\.?\s+\d{4}|\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}|\d{4}[-/.]\d{1,2}[-/.]\d{1,2})\b/);
    const timeMatches = [...normalized.matchAll(/\b([01]?\d|2[0-3]):([0-5]\d)\b/g)];
    const seat = findLabeledValue(lines, ['SEAT', 'ASIENTO']) || (normalized.match(/\b([0-9]{1,3}[A-F])\b/) || [])[1];
    const gate = findLabeledValue(lines, ['GATE', 'PUERTA']) || (normalized.match(/\b(?:GATE|PUERTA)\s*[:#-]?\s*([A-Z]?\d{1,3})\b/) || [])[1];
    const terminal = findLabeledValue(lines, ['TERMINAL', 'TERM']);
    const zone = findLabeledValue(lines, ['ZONE', 'ZONA', 'GROUP', 'GRUPO']);
    const boarding = findLabeledValue(lines, ['BOARDING', 'EMBARQUE', 'ABORDAJE', 'INICIO DE ABORDAJE']);
    const boardingEnd = findLabeledValue(lines, ['BOARDING ENDS', 'BOARDING CLOSES', 'BOARDING END', 'CIERRE DE ABORDAJE', 'FIN DE ABORDAJE', 'CIERRE EMBARQUE']);
    const boardingRange = normalized.match(/\b([01]?\d|2[0-3]):([0-5]\d)\s*(?:-|–|—|TO|A)\s*([01]?\d|2[0-3]):([0-5]\d)\b/);
    const departure = findLabeledValue(lines, ['DEPARTURE', 'SALIDA', 'DEPARTS']);

    if (airports[0]) {
        fields.origin = airports[0];
    }
    if (airports[1]) {
        fields.destination = airports[1];
    }
    if (flight) {
        fields.flight = `${flight[1]} ${flight[2]}`;
        fields.carrier = flight[1];
        fields.flight_number = flight[2];
    }
    if (dateMatch) {
        fields.date = parseDate(dateMatch[1], reference);
    }
    if (timeMatches[0]) {
        fields.departure = `${timeMatches[0][1].padStart(2, '0')}:${timeMatches[0][2]}`;
    }
    if (timeMatches[1]) {
        fields.boarding_start = `${timeMatches[1][1].padStart(2, '0')}:${timeMatches[1][2]}`;
        fields.boarding = fields.boarding_start;
    }
    if (boardingRange) {
        fields.boarding_start = `${boardingRange[1].padStart(2, '0')}:${boardingRange[2]}`;
        fields.boarding_end = `${boardingRange[3].padStart(2, '0')}:${boardingRange[4]}`;
        fields.boarding = fields.boarding_start;
    }
    if (boarding) {
        const parsed = parseTime(boarding);
        if (parsed) {
            fields.boarding_start = parsed;
            fields.boarding = parsed;
        }
    }
    if (boardingEnd) {
        const parsed = parseTime(boardingEnd);
        if (parsed) {
            fields.boarding_end = parsed;
        }
    }
    if (departure) {
        const parsed = parseTime(departure);
        if (parsed) {
            fields.departure = parsed;
        }
    }
    if (seat) {
        fields.seat = seat.trim();
    }
    if (gate) {
        fields.gate = gate.trim();
    }
    if (terminal) {
        fields.terminal = terminal.trim();
    }
    if (zone) {
        fields.zone = zone.trim();
    }

    const passenger = detectPersonName(lines);
    if (passenger) {
        fields.passenger = passenger;
        fields.passenger_name = passenger;
    }

    const pnr = normalized.match(/\b(?:PNR|RESERVA|BOOKING)\s*[:#-]?\s*([A-Z0-9]{5,8})\b/);
    if (pnr) {
        fields.pnr = pnr[1];
    }

    const issuer = detectAirline(text);
    const title = fields.flight
        ? `${fields.origin || ''}${fields.origin && fields.destination ? ' → ' : ''}${fields.destination || ''} · ${fields.flight}`.trim()
        : [fields.origin, fields.destination].filter(Boolean).join(' → ');

    return {
        category: 'flight',
        title: title || firstMeaningfulLine(lines),
        issuer: issuer || fields.carrier || null,
        holder: fields.passenger || null,
        fields,
        notes: [],
        code: detectCode(text),
        confidence: confidenceFor(fields, ['origin', 'destination', 'flight', 'date', 'passenger', 'seat', 'terminal', 'boarding_start']),
        detectedFields: countFilled(fields),
    };
}

function parseEventText(text, reference = new Date()) {
    const lines = textLines(text);
    const normalized = normalize(text);
    const fields = {};
    const dateMatch = normalized.match(/\b(\d{1,2}\s+[A-Z]{3}\.?\s+\d{4}|\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}|\d{4}[-/.]\d{1,2}[-/.]\d{1,2})\b/);
    const time = normalized.match(/\b([01]?\d|2[0-3]):([0-5]\d)\b/);
    const gate = findLabeledValue(lines, ['GATE', 'PUERTA', 'ACCESS', 'ACCESO']);
    const section = findLabeledValue(lines, ['SECTION', 'SECCION', 'AREA', 'ZONA']);
    const row = findLabeledValue(lines, ['ROW', 'FILA']);
    const seat = findLabeledValue(lines, ['SEAT', 'ASIENTO']);
    const venue = findLabeledValue(lines, ['VENUE', 'RECINTO', 'LOCATION', 'UBICACION', 'PLACE', 'LUGAR']);
    const holder = detectPersonName(lines);

    if (dateMatch) {
        fields.date = parseDate(dateMatch[1], reference);
    }
    if (time) {
        fields.time = `${time[1].padStart(2, '0')}:${time[2]}`;
    }
    if (gate) {
        fields.gate = gate.trim();
    }
    if (section) {
        fields.section = section.trim();
    }
    if (row) {
        fields.row = row.trim();
    }
    if (seat) {
        fields.seat = seat.trim();
    }
    if (venue) {
        fields.location = venue.trim();
    }
    if (holder) {
        fields.holder = holder;
    }

    const title = firstMeaningfulLine(lines);
    return {
        category: 'event',
        title,
        issuer: venue || null,
        holder: holder || null,
        fields,
        notes: [],
        code: detectCode(text),
        confidence: confidenceFor(fields, ['date', 'time', 'location', 'gate', 'seat']),
        detectedFields: countFilled(fields),
    };
}

function parseTransitText(text, reference = new Date()) {
    const lines = textLines(text);
    const normalized = normalize(text);
    const fields = {};
    const dateMatch = normalized.match(/\b(\d{1,2}\s+[A-Z]{3}\.?\s+\d{4}|\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}|\d{4}[-/.]\d{1,2}[-/.]\d{1,2})\b/);
    const time = normalized.match(/\b([01]?\d|2[0-3]):([0-5]\d)\b/);
    const platform = findLabeledValue(lines, ['PLATFORM', 'ANDEN', 'VIA', 'TRACK']);
    const terminal = findLabeledValue(lines, ['TERMINAL', 'TERM', 'ESTACION', 'STATION']);
    const carriage = findLabeledValue(lines, ['CARRIAGE', 'COCHE', 'VAGON', 'COACH']);
    const seat = findLabeledValue(lines, ['SEAT', 'ASIENTO', 'PLAZA']);
    const route = findLabeledValue(lines, ['ROUTE', 'RUTA', 'LINE', 'LINEA', 'DESTINATION', 'DESTINO']);
    const boarding = findLabeledValue(lines, ['BOARDING', 'EMBARQUE', 'ABORDAJE', 'INICIO DE ABORDAJE']);
    const boardingEnd = findLabeledValue(lines, ['BOARDING ENDS', 'BOARDING CLOSES', 'BOARDING END', 'CIERRE DE ABORDAJE', 'FIN DE ABORDAJE']);
    const boardingRange = normalized.match(/\b([01]?\d|2[0-3]):([0-5]\d)\s*(?:-|–|—|TO|A)\s*([01]?\d|2[0-3]):([0-5]\d)\b/);
    const holder = detectPersonName(lines);

    if (dateMatch) {
        fields.date = parseDate(dateMatch[1], reference);
    }
    if (time) {
        fields.time = `${time[1].padStart(2, '0')}:${time[2]}`;
    }
    if (boardingRange) {
        fields.boarding_start = `${boardingRange[1].padStart(2, '0')}:${boardingRange[2]}`;
        fields.boarding_end = `${boardingRange[3].padStart(2, '0')}:${boardingRange[4]}`;
    }
    if (boarding) {
        const parsed = parseTime(boarding);
        if (parsed) {
            fields.boarding_start = parsed;
        }
    }
    if (boardingEnd) {
        const parsed = parseTime(boardingEnd);
        if (parsed) {
            fields.boarding_end = parsed;
        }
    }
    if (terminal) {
        fields.terminal = terminal.trim();
    }
    if (platform) {
        fields.platform = platform.trim();
    }
    if (carriage) {
        fields.carriage = carriage.trim();
    }
    if (seat) {
        fields.seat = seat.trim();
    }
    if (route) {
        fields.route = route.trim();
    }
    if (holder) {
        fields.holder = holder;
    }

    return {
        category: 'transit',
        title: firstMeaningfulLine(lines),
        issuer: null,
        holder,
        fields,
        notes: [],
        code: detectCode(text),
        confidence: confidenceFor(fields, ['date', 'time', 'route', 'platform', 'seat', 'terminal', 'boarding_start']),
        detectedFields: countFilled(fields),
    };
}

function parseIdentityText(text, reference = new Date(), category = 'id') {
    const lines = textLines(text);
    const normalized = normalize(text);
    const fields = {};
    const holder = detectPersonName(lines);
    const number = normalized.match(/\b(?:NO|NUMERO|NUMBER|DOCUMENTO|DOCUMENT|ID)\s*[:#-]?\s*([A-Z0-9-]{5,20})\b/);
    const expires = normalized.match(/\b(?:EXPIRA|EXPIRES|VENCE|VALID UNTIL|VALIDO HASTA)\s*[:#-]?\s*([0-9A-Z/.-]{4,})\b/);
    const birth = normalized.match(/\b(?:BIRTH|NACIMIENTO|DOB)\s*[:#-]?\s*([0-9A-Z/.-]{4,})\b/);
    const level = findLabeledValue(lines, ['LEVEL', 'NIVEL', 'TIER', 'CATEGORIA', 'CATEGORY']);

    if (holder) {
        fields.holder = holder;
    }
    if (number) {
        fields.number = number[1];
    }
    if (expires) {
        fields.expires = parseDate(expires[1], reference) || expires[1];
    }
    if (birth) {
        fields.birth = parseDate(birth[1], reference) || birth[1];
    }
    if (level) {
        fields.level = level.trim();
    }

    return {
        category,
        title: firstMeaningfulLine(lines),
        issuer: null,
        holder,
        fields,
        notes: [],
        code: detectCode(text),
        confidence: confidenceFor(fields, ['holder', 'number', 'expires', 'level']),
        detectedFields: countFilled(fields),
    };
}

function firstMeaningfulLine(lines) {
    const ignored = /^(mypass|page|pagina|www\.|https?:|tel[:. ]|fax[:. ])/i;
    return lines.find((line) => line.length >= 3 && !ignored.test(line))?.slice(0, 90) || 'Pase';
}

function confidenceFor(fields, required) {
    const filled = required.filter((key) => fields[key] !== null && fields[key] !== undefined && fields[key] !== '');
    return Math.min(0.95, 0.35 + (filled.length / required.length) * 0.6);
}

function parsePassText(text, options = {}) {
    const reference = options.reference || new Date();
    const category = options.category || detectCategory(text);
    let parsed;

    if (category === 'flight') {
        parsed = parseFlightText(text, reference);
    } else if (category === 'event') {
        parsed = parseEventText(text, reference);
    } else if (category === 'transit') {
        parsed = parseTransitText(text, reference);
    } else if (category === 'membership') {
        parsed = parseIdentityText(text, reference, 'membership');
    } else {
        parsed = parseIdentityText(text, reference, 'id');
    }

    return {
        ...parsed,
        category,
        rawText: cleanText(text),
    };
}

export {
    cleanText,
    confidenceFor,
    countFilled,
    detectCategory,
    normalize,
    parseBcbp,
    parseDate,
    parseEventText,
    parseFlightText,
    parseIdentityText,
    parsePassText,
    parseTime,
    parseTransitText,
};
