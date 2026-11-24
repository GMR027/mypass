function clean(value) {
    return String(value ?? '').trim();
}

function parseCoordinates(value) {
    const text = clean(value);
    if (!text) {
        return null;
    }

    const direct = text.match(/^\s*(-?\d{1,3}(?:\.\d+)?)\s*[,;]\s*(-?\d{1,3}(?:\.\d+)?)\s*$/);
    if (direct) {
        const lat = Number(direct[1]);
        const lng = Number(direct[2]);
        if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
            return `${lat},${lng}`;
        }
    }

    try {
        const url = new URL(text);
        const query = url.searchParams.get('q') || url.searchParams.get('query');
        const at = url.pathname.match(/@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)/);
        const candidate = query && query.match(/^\s*(-?\d{1,3}(?:\.\d+)?)\s*,\s*(-?\d{1,3}(?:\.\d+)?)\s*$/)
            ? query
            : at
                ? `${at[1]},${at[2]}`
                : null;
        if (candidate) {
            return parseCoordinates(candidate);
        }
    } catch {
        return null;
    }

    return null;
}

export function mapQuery(pass) {
    const fields = pass?.fields ?? {};
    const category = pass?.category ?? 'event';
    const coordinates = parseCoordinates(fields.coordinates);
    if (coordinates) {
        return coordinates;
    }

    const parts = category === 'flight'
        ? [fields.origin, fields.terminal, fields.airport]
        : [fields.location, fields.address, fields.terminal];

    const query = parts
        .map(clean)
        .filter(Boolean)
        .join(', ');

    return query || null;
}

export function mapUrl(query) {
    if (!query) {
        return '';
    }

    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
}

export function passMapUrl(pass) {
    return mapUrl(mapQuery(pass));
}
