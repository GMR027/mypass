import { applyAppearance, normalizeAppearance } from './pass-appearance';
import { centerCardInView } from './card-deck';
import { passMapUrl } from './geo';
import { downloadCalendar, passCalendar } from './calendar';

const t = (key) => (window.mypassT ? window.mypassT(key) : key);
const tf = (key, values) => (window.mypassTF ? window.mypassTF(key, values) : key);
const locale = () => (window.mypassLocale === 'en' ? 'en-US' : 'es-MX');

const CATEGORY_META = {
    flight: { icon: 'flight_takeoff' },
    event: { icon: 'confirmation_number' },
    transit: { icon: 'directions_subway' },
    id: { icon: 'badge' },
    membership: { icon: 'loyalty' },
};

const THEME_COLORS = {
    midnight: '#0c2340',
    violet: '#4c1d95',
    forest: '#064e3b',
    obsidian: '#18181b',
    burgundy: '#7f1d1d',
    amber: '#9a3412',
};

function el(tag, className = '', text = null) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    if (text !== null && text !== undefined) {
        node.textContent = text;
    }
    return node;
}

function icon(name, className = '') {
    const node = el('span', ['material-symbols-outlined', className].filter(Boolean).join(' '), name);
    node.setAttribute('aria-hidden', 'true');
    return node;
}

function categoryLabel(category) {
    return {
        flight: t('Vuelo'),
        event: t('Evento'),
        transit: t('Transporte'),
        id: t('Identificación'),
        membership: t('Membresía'),
    }[category] ?? t('Pase');
}

function categoryIcon(category) {
    return CATEGORY_META[category]?.icon ?? 'confirmation_number';
}

function formatDate(value, options = {}) {
    if (!value) {
        return '';
    }

    if (value instanceof Date && !Number.isNaN(value.getTime())) {
        return value.toLocaleDateString(locale(), {
            day: 'numeric',
            month: 'short',
            year: 'numeric',
            ...options,
        });
    }

    const [year, month, day] = String(value).slice(0, 10).split('-').map(Number);
    if (!year || !month || !day) {
        return String(value);
    }

    return new Date(year, month - 1, day).toLocaleDateString(locale(), {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        ...options,
    });
}

function formatTime(value) {
    if (!value) {
        return '';
    }

    const match = String(value).match(/\b([01]?\d|2[0-3]):([0-5]\d)\b/);
    return match ? `${match[1].padStart(2, '0')}:${match[2]}` : String(value);
}

function boardingStart(fields) {
    return fields?.boarding_start || fields?.boarding || '';
}

function boardingWindow(fields) {
    const start = formatTime(boardingStart(fields));
    const end = formatTime(fields?.boarding_end || '');

    if (start && end) {
        return `${start} – ${end}`;
    }

    if (start) {
        return start;
    }

    if (end) {
        return tf('Cierre :time', { time: end });
    }

    return '';
}

function boardingStrip(fields) {
    const window = boardingWindow(fields);
    if (!window) {
        return null;
    }

    const strip = el('div', 'mp-pass-card__boarding mt-2');
    strip.appendChild(icon('schedule', 'text-[16px]'));
    strip.appendChild(el('span', null, tf('Abordaje :window', { window })));

    return strip;
}

function isExpired(item) {
    if (!item?.expires_at) {
        return false;
    }

    const date = new Date(item.expires_at);
    return !Number.isNaN(date.getTime()) && date.getTime() < Date.now();
}

function parsePassDate(value) {
    if (!value) {
        return null;
    }

    const text = String(value).trim();
    const iso = text.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (iso) {
        return new Date(Number(iso[1]), Number(iso[2]) - 1, Number(iso[3]));
    }

    const parsed = new Date(text);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function passDateFor(entry) {
    const fields = entry?.pass?.fields || {};
    return parsePassDate(fields.date)
        || parsePassDate(fields.expires)
        || parsePassDate(entry?.item?.expires_at);
}

function startOfToday() {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
}

function isUpcoming(entry) {
    const date = passDateFor(entry);
    return Boolean(date) && date.getTime() >= startOfToday();
}

function updatedTimestamp(entry) {
    const value = entry?.item?.updated_at;
    if (!value) {
        return 0;
    }

    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? 0 : parsed.getTime();
}

function sortActivePasses(entries) {
    const today = startOfToday();

    return [...entries].sort((a, b) => {
        const dateA = passDateFor(a);
        const dateB = passDateFor(b);
        const upcomingA = Boolean(dateA) && dateA.getTime() >= today;
        const upcomingB = Boolean(dateB) && dateB.getTime() >= today;

        if (upcomingA && upcomingB) {
            return dateA.getTime() - dateB.getTime();
        }

        if (upcomingA !== upcomingB) {
            return upcomingA ? -1 : 1;
        }

        if (dateA && dateB && dateA.getTime() !== dateB.getTime()) {
            return dateB.getTime() - dateA.getTime();
        }

        return updatedTimestamp(b) - updatedTimestamp(a);
    });
}

function passFromItem(item) {
    const raw = item.metadata?.pass && typeof item.metadata.pass === 'object' ? item.metadata.pass : null;
    const title = item.metadata?.title || '';
    const fallbackCategory = item.type === 'card'
        ? 'membership'
        : /vuelo|flight|boarding|iberia|aerom[eé]xico|vueling|latam/i.test(title)
            ? 'flight'
            : 'event';
    const category = raw?.category ?? fallbackCategory;
    const fields = raw?.fields && typeof raw.fields === 'object' ? raw.fields : {};

    return {
        ...raw,
        category,
        fields,
        title: raw?.title || item.metadata?.title || t('Pase sin título'),
        issuer: raw?.issuer || fields.carrier || '',
        holder: raw?.holder || fields.passenger || fields.holder || '',
        notes: Array.isArray(raw?.notes) ? raw.notes.filter(Boolean) : [],
        code: raw?.code || null,
        themeColor: raw?.themeColor || (raw?.theme ? THEME_COLORS[raw.theme] : null),
        appearance: normalizeAppearance(raw?.appearance),
        options: raw?.options || {},
        item,
    };
}

function isPassItem(item) {
    return Boolean(item.metadata?.pass) || item.type === 'ticket' || item.type === 'card';
}

function passPayload(item, pass) {
    if (pass?.code?.payload) {
        return String(pass.code.payload);
    }

    const fields = {};
    for (const [key, value] of Object.entries(pass?.fields || {})) {
        if (value !== null && value !== undefined && value !== '') {
            fields[key] = value;
        }
    }

    return JSON.stringify({
        v: 1,
        app: 'mypass',
        id: item.id,
        c: pass.category,
        title: pass.title,
        issuer: pass.issuer,
        holder: pass.holder,
        fields,
    });
}

function subtitleFor(pass) {
    const fields = pass.fields || {};
    if (pass.category === 'flight') {
        const bits = [];
        if (fields.passenger) {
            bits.push(tf('Pasajero: :name', { name: fields.passenger }));
        }
        if (fields.seat) {
            bits.push(tf('Asiento :seat', { seat: fields.seat }));
        }
        if (fields.gate) {
            bits.push(tf('Puerta :gate', { gate: fields.gate }));
        }
        if (fields.terminal) {
            bits.push(tf('Terminal :terminal', { terminal: fields.terminal }));
        }
        const window = boardingWindow(fields);
        if (window) {
            bits.push(tf('Abordaje :window', { window }));
        }
        return bits.join(' · ');
    }

    if (pass.category === 'event') {
        return [fields.location, fields.date ? formatDate(fields.date) : '', fields.time ? formatTime(fields.time) : ''].filter(Boolean).join(' · ');
    }

    if (pass.category === 'transit') {
        const bits = [fields.route, fields.date ? formatDate(fields.date) : '', fields.time ? formatTime(fields.time) : ''].filter(Boolean);
        if (fields.terminal) {
            bits.push(tf('Terminal :terminal', { terminal: fields.terminal }));
        }
        const window = boardingWindow(fields);
        if (window) {
            bits.push(tf('Abordaje :window', { window }));
        }
        return bits.join(' · ');
    }

    return [pass.holder, fields.number, fields.expires ? tf('Vigente hasta :date', { date: formatDate(fields.expires) }) : ''].filter(Boolean).join(' · ');
}

function badgeFor(pass) {
    if (pass.badge) {
        return pass.badge;
    }
    if (pass.category === 'flight' && pass.fields?.date) {
        return formatDate(pass.fields.date, { year: undefined });
    }
    if (pass.category === 'event' && pass.fields?.date) {
        return formatDate(pass.fields.date, { year: undefined });
    }
    if (pass.status) {
        return pass.status;
    }
    return categoryLabel(pass.category);
}

function themeColor(pass) {
    return pass.themeColor || THEME_COLORS[pass.theme] || '#0c2340';
}

function stackSummary(pass) {
    const fields = pass.fields || {};

    if (pass.category === 'flight') {
        return [
            fields.origin && fields.destination ? `${fields.origin} → ${fields.destination}` : '',
            fields.date ? formatDate(fields.date) : '',
            fields.gate ? tf('Puerta :gate', { gate: fields.gate }) : '',
            fields.terminal ? tf('Terminal :terminal', { terminal: fields.terminal }) : '',
        ].filter(Boolean).join(' · ');
    }

    if (pass.category === 'transit') {
        return [
            fields.route,
            fields.date ? formatDate(fields.date) : '',
            fields.terminal ? tf('Terminal :terminal', { terminal: fields.terminal }) : '',
            fields.platform ? tf('Andén :platform', { platform: fields.platform }) : '',
        ].filter(Boolean).join(' · ');
    }

    if (pass.category === 'event') {
        return [
            fields.location,
            fields.date ? formatDate(fields.date) : '',
            fields.gate ? tf('Puerta :gate', { gate: fields.gate }) : '',
        ].filter(Boolean).join(' · ');
    }

    return [pass.holder, fields.number, fields.expires ? formatDate(fields.expires) : ''].filter(Boolean).join(' · ');
}

const FIELD_LABELS = {
    origin: 'Origen',
    destination: 'Destino',
    flight: 'Vuelo',
    date: 'Fecha',
    departure: 'Salida',
    boarding: 'Abordaje',
    boarding_start: 'Inicio de abordaje',
    boarding_end: 'Cierre de abordaje',
    gate: 'Puerta',
    terminal: 'Terminal',
    seat: 'Asiento',
    zone: 'Zona',
    pnr: 'Código de reserva',
    location: 'Ubicación',
    address: 'Dirección',
    coordinates: 'Coordenadas',
    time: 'Hora',
    section: 'Sección',
    row: 'Fila',
    route: 'Ruta',
    platform: 'Andén',
    carriage: 'Coche',
    number: 'Número',
    level: 'Nivel',
    expires: 'Vigente hasta',
    birth: 'Nacimiento',
    holder: 'Titular',
    passenger: 'Pasajero',
    carrier: 'Aerolínea',
    code_value: 'Código',
};

const DATE_FIELDS = new Set(['date', 'expires', 'birth']);
const TIME_FIELDS = new Set(['time', 'departure', 'boarding', 'boarding_start', 'boarding_end']);

function fieldLabel(key) {
    return FIELD_LABELS[key] ? t(FIELD_LABELS[key]) : key.replaceAll('_', ' ');
}

function fieldValue(key, value) {
    if (DATE_FIELDS.has(key)) {
        return formatDate(value);
    }

    if (TIME_FIELDS.has(key)) {
        return formatTime(value);
    }

    return String(value);
}

function passDetailEntries(pass) {
    return Object.entries(pass.fields || {})
        .filter(([, value]) => value !== null && value !== undefined && value !== '')
        .map(([key, value]) => [fieldLabel(key), fieldValue(key, value)]);
}

function codeFormatLabel(pass) {
    return {
        qr: 'QR',
        pdf417: 'PDF417',
        aztec: 'Aztec',
        code128: 'Code 128',
    }[pass.code?.format || 'qr'] || 'QR';
}

function passHeader(pass, badge = null) {
    const header = el('div', 'flex items-start justify-between gap-3');
    const identity = el('div', 'flex min-w-0 items-center gap-2.5');
    const tile = el('span', 'flex h-9 w-9 shrink-0 items-center justify-center overflow-hidden rounded-lg bg-white/15');

    if (pass.logo) {
        const logo = document.createElement('img');
        logo.src = pass.logo;
        logo.alt = '';
        logo.className = 'h-6 w-6 object-contain';
        tile.appendChild(logo);
    } else {
        tile.appendChild(icon(categoryIcon(pass.category), 'text-[20px]'));
    }

    const names = el('div', 'min-w-0');
    names.appendChild(el('p', 'mp-pass-card__eyebrow truncate', pass.issuer || categoryLabel(pass.category)));
    names.appendChild(el('p', 'mp-pass-card__title truncate', pass.title));
    identity.append(tile, names);
    header.append(identity, el('span', 'mp-pass-card__chip shrink-0', badge || badgeFor(pass)));

    return header;
}

function appendPassContent(body, pass, item, options = {}) {
    const { preview = false } = options;

    if (pass.category === 'flight') {
        const route = el('div', 'mp-pass-card__route mt-3');
        const left = el('div');
        left.appendChild(el('p', 'text-[11px] font-semibold uppercase tracking-wider opacity-70', pass.fields?.origin || t('Origen')));
        left.appendChild(el('p', 'mp-pass-card__airport', pass.fields?.origin || '—'));
        const middle = el('div', 'flex flex-1 flex-col items-center px-2 text-center');
        middle.appendChild(el('p', 'text-[10px] opacity-60', pass.fields?.flight || ''));
        middle.appendChild(el('span', 'material-symbols-outlined text-[18px] opacity-80', 'flight'));
        middle.appendChild(el('p', 'text-[10px] opacity-60', pass.fields?.date ? formatDate(pass.fields.date) : ''));
        const right = el('div', 'text-right');
        right.appendChild(el('p', 'text-[11px] font-semibold uppercase tracking-wider opacity-70', pass.fields?.destination || t('Destino')));
        right.appendChild(el('p', 'mp-pass-card__airport', pass.fields?.destination || '—'));
        route.append(left, middle, right);
        body.appendChild(route);

        const meta = el('div', 'mp-pass-card__meta mt-3');
        for (const [label, value] of [
            [t('Puerta'), pass.fields?.gate || '—'],
            [t('Terminal'), pass.fields?.terminal || '—'],
            [t('Asiento'), pass.fields?.seat || '—'],
            [t('Zona'), pass.fields?.zone || '—'],
        ]) {
            const cell = el('div');
            cell.appendChild(el('span', null, label));
            cell.appendChild(el('strong', null, value));
            meta.appendChild(cell);
        }
        body.appendChild(meta);

        const boarding = boardingStrip(pass.fields);
        if (boarding) {
            body.appendChild(boarding);
        }

        const footer = el('div', 'mp-pass-card__footer');
        footer.appendChild(el('span', null, pass.fields?.passenger || pass.holder || t('Titular')));
        footer.appendChild(el('span', 'font-mono', preview ? t('Vista previa') : item.id.slice(0, 8).toUpperCase()));
        body.appendChild(footer);
    } else if (pass.category === 'event') {
        const details = el('div', 'mt-3 flex items-end justify-between gap-3');
        const left = el('div');
        left.appendChild(el('p', 'text-[12px] opacity-70', pass.fields?.location || pass.issuer || ''));
        left.appendChild(el('p', 'text-[17px] font-bold', [pass.fields?.date ? formatDate(pass.fields.date) : '', pass.fields?.time ? formatTime(pass.fields.time) : ''].filter(Boolean).join(' · ') || t('Fecha por confirmar')));
        const right = el('div', 'text-right');
        right.appendChild(el('p', 'text-[10px] uppercase tracking-wider opacity-60', t('Acceso')));
        right.appendChild(el('p', 'text-[17px] font-bold', pass.fields?.gate || pass.fields?.section || '—'));
        details.append(left, right);
        body.appendChild(details);

        const meta = el('div', 'mt-3 grid grid-cols-3 gap-1 rounded-xl bg-black/20 p-2 text-center');
        for (const [label, value] of [
            [t('Sección'), pass.fields?.section || '—'],
            [t('Fila'), pass.fields?.row || '—'],
            [t('Asiento'), pass.fields?.seat || '—'],
        ]) {
            const cell = el('div');
            cell.appendChild(el('span', 'block text-[10px] uppercase opacity-60', label));
            cell.appendChild(el('strong', 'block text-[12px]', value));
            meta.appendChild(cell);
        }
        body.appendChild(meta);
    } else if (pass.category === 'transit') {
        const details = el('div', 'mt-3 flex items-end justify-between gap-3');
        const left = el('div');
        left.appendChild(el('p', 'text-[11px] uppercase tracking-wider opacity-60', t('Ruta')));
        left.appendChild(el('p', 'text-[17px] font-bold', pass.fields?.route || pass.title));
        const right = el('div', 'text-right');
        right.appendChild(el('p', 'text-[10px] uppercase tracking-wider opacity-60', t('Vigencia')));
        right.appendChild(el('p', 'text-[14px] font-bold', pass.fields?.date ? formatDate(pass.fields.date) : pass.fields?.expires ? formatDate(pass.fields.expires) : '—'));
        details.append(left, right);
        body.appendChild(details);

        if (pass.fields?.terminal || pass.fields?.platform || pass.fields?.carriage || pass.fields?.seat) {
            const meta = el('div', 'mt-3 grid grid-cols-2 gap-1 rounded-xl bg-black/20 p-2 text-center');
            for (const [label, value] of [
                [t('Terminal'), pass.fields?.terminal || '—'],
                [t('Andén'), pass.fields?.platform || '—'],
                [t('Coche'), pass.fields?.carriage || '—'],
                [t('Asiento'), pass.fields?.seat || '—'],
            ]) {
                const cell = el('div');
                cell.appendChild(el('span', 'block text-[10px] uppercase opacity-60', label));
                cell.appendChild(el('strong', 'block text-[12px]', value));
                meta.appendChild(cell);
            }
            body.appendChild(meta);
        }

        const boarding = boardingStrip(pass.fields);
        if (boarding) {
            body.appendChild(boarding);
        }
    } else {
        const holder = el('div', 'mt-3 flex items-center justify-between gap-3');
        const left = el('div');
        left.appendChild(el('p', 'text-[11px] uppercase tracking-wider opacity-60', t('Titular')));
        left.appendChild(el('p', 'text-[16px] font-bold', pass.holder || pass.fields?.holder || '—'));
        const right = el('div', 'flex h-12 w-12 items-center justify-center rounded-full bg-white/15 text-[17px] font-bold');
        right.textContent = (pass.holder || pass.title || '?').slice(0, 2).toUpperCase();
        holder.append(left, right);
        body.appendChild(holder);

        if (pass.fields?.number || pass.fields?.level || pass.fields?.expires) {
            const footer = el('div', 'mp-pass-card__footer');
            if (pass.fields?.number) {
                footer.appendChild(el('span', 'font-mono', pass.fields.number));
            }
            if (pass.fields?.level) {
                footer.appendChild(el('span', null, pass.fields.level));
            }
            if (pass.fields?.expires) {
                footer.appendChild(el('span', null, tf('Vigente hasta :date', { date: formatDate(pass.fields.expires) })));
            }
            body.appendChild(footer);
        }
    }
}

function flipButton(direction) {
    const button = el('button', 'mp-pass-flip__toggle');
    button.type = 'button';
    button.dataset.flipToggle = direction;
    button.appendChild(icon(direction === 'back' ? 'flip' : 'undo', 'text-[18px]'));
    button.appendChild(document.createTextNode(direction === 'back' ? t('Ver reverso') : t('Volver al frente')));

    return button;
}

function passActionButton(action, iconName, label, primary = false) {
    const button = el('button', [
        'mp-pass-back__action',
        primary ? 'mp-pass-back__action--primary' : '',
    ].filter(Boolean).join(' '));
    button.type = 'button';
    button.dataset.passAction = action;
    button.appendChild(icon(iconName, 'text-[18px]'));
    button.appendChild(document.createTextNode(label));

    return button;
}

function buildPassFrontFace(pass, item, options) {
    const face = el('div', 'mp-pass-flip__face mp-pass-flip__face--front');
    const body = el('div', 'mp-pass-card__body');
    body.appendChild(passHeader(pass, options.badge));
    appendPassContent(body, pass, item, options);

    const flipRow = el('div', 'mt-3 flex items-center justify-between gap-3 border-t border-white/15 pt-3');
    flipRow.appendChild(el('p', 'text-[11px] leading-snug opacity-70', t('Voltea para ver el código y todos los datos.')));
    flipRow.appendChild(flipButton('back'));
    body.appendChild(flipRow);
    face.appendChild(body);

    return face;
}

function buildPassBackFace(pass, item, options) {
    const face = el('div', 'mp-pass-flip__face mp-pass-flip__face--back');
    const body = el('div', 'mp-pass-card__body');

    const header = el('div', 'flex items-start justify-between gap-3');
    const names = el('div', 'min-w-0');
    names.appendChild(el('p', 'mp-pass-card__eyebrow truncate', t('Reverso del pase')));
    names.appendChild(el('p', 'mp-pass-card__title truncate', pass.title));
    if (pass.issuer) {
        names.appendChild(el('p', 'mt-0.5 truncate text-[11px] opacity-65', pass.issuer));
    }
    header.append(names, el('span', 'mp-pass-card__chip shrink-0', badgeFor(pass)));
    body.appendChild(header);

    const code = el('div', 'mp-pass-back__code mp-scan-sheet');
    const canvasWrap = el('div', 'mp-code-canvas');
    canvasWrap.dataset.cardCode = 'true';
    code.appendChild(canvasWrap);
    code.appendChild(el('p', 'mp-pass-back__caption', codeFormatLabel(pass)));
    body.appendChild(code);

    const details = passDetailEntries(pass);
    if (details.length > 0) {
        body.appendChild(el('p', 'mp-pass-back__section-title', t('Todos los datos')));
        const grid = el('div', 'mp-pass-back__details');
        for (const [label, value] of details) {
            const cell = el('div', 'mp-pass-back__detail');
            cell.appendChild(el('span', null, label));
            cell.appendChild(el('strong', null, value));
            grid.appendChild(cell);
        }
        body.appendChild(grid);
    }

    if (pass.notes.length > 0) {
        body.appendChild(el('p', 'mp-pass-back__section-title', t('Notas')));
        const notes = el('div', 'mp-pass-back__notes');
        for (const note of pass.notes.slice(0, 6)) {
            notes.appendChild(el('span', 'mp-pass-back__note', note));
        }
        body.appendChild(notes);
    }

    const actions = el('div', 'mp-pass-back__actions');
    actions.appendChild(passActionButton('present', 'qr_code_2', t('Presentar código'), true));
    const share = passActionButton('share', 'share', t('Compartir'));
    share.disabled = !options.shareAllowed;
    if (share.disabled) {
        share.title = t('Agrega un contacto de confianza para compartir.');
    }
    actions.appendChild(share);
    actions.appendChild(passActionButton('edit', 'edit', t('Editar pase')));
    if (options.mapUrl) {
        actions.appendChild(passActionButton('map', 'map', t('Cómo llegar')));
    }
    if (options.calendarAvailable) {
        actions.appendChild(passActionButton('calendar', 'calendar_month', t('Agregar al calendario')));
    }
    body.appendChild(actions);

    const footer = el('div', 'mp-pass-back__footer');
    footer.appendChild(el('span', null, item.id.slice(0, 8).toUpperCase()));
    footer.appendChild(el('span', null, t('Protegido por My Pass')));
    body.appendChild(footer);

    const flipRow = el('div', 'mp-pass-back__flip-row');
    flipRow.appendChild(flipButton('front'));
    body.appendChild(flipRow);
    face.appendChild(body);

    return face;
}

function syncFlipFaceAccessibility(card, flipped) {
    const front = card.querySelector('.mp-pass-flip__face--front');
    const back = card.querySelector('.mp-pass-flip__face--back');
    if (!front || !back) {
        return;
    }

    const setHidden = (face, hidden) => {
        face.setAttribute('aria-hidden', hidden ? 'true' : 'false');
        if (hidden) {
            face.setAttribute('inert', '');
        } else {
            face.removeAttribute('inert');
        }
    };

    setHidden(front, flipped);
    setHidden(back, !flipped);
}

function createPassCardElement(item, options = {}) {
    const {
        interactive = true,
        selected = false,
        compact = false,
        preview = false,
        badge = null,
        highlight = false,
        headerOnly = false,
        flippable = false,
        flipped = false,
        shareAllowed = true,
    } = options;
    const pass = passFromItem(item);
    const isFlip = flippable && !preview;
    const node = document.createElement(isFlip || !interactive ? 'article' : 'button');

    if (!isFlip && interactive) {
        node.type = 'button';
    }

    node.className = [
        'mp-pass-card',
        selected ? 'is-selected' : '',
        compact ? 'mp-pass-card--compact' : '',
        highlight ? 'mp-pass-card--highlight' : '',
        headerOnly ? 'mp-pass-card--stack-header' : '',
        isFlip ? 'mp-pass-card--flip' : '',
    ].filter(Boolean).join(' ');
    node.dataset.category = pass.category;
    node.dataset.passId = item.id;
    node.style.setProperty('--pass-color', themeColor(pass));
    applyAppearance(node, pass.appearance);

    if (interactive && !isFlip) {
        node.setAttribute('aria-pressed', selected ? 'true' : 'false');
    }

    if (isFlip) {
        const flip = el('div', 'mp-pass-flip');
        const inner = el('div', 'mp-pass-flip__inner');
        const mapUrl = passMapUrl(pass);
        const faceOptions = {
            ...options,
            badge: badge || badgeFor(pass),
            shareAllowed,
            mapUrl,
            calendarAvailable: Boolean(passCalendar(item, pass)),
        };
        inner.append(
            buildPassFrontFace(pass, item, faceOptions),
            buildPassBackFace(pass, item, faceOptions),
        );
        flip.appendChild(inner);
        node.appendChild(flip);
        node.classList.toggle('is-flipped', Boolean(flipped));
        syncFlipFaceAccessibility(node, Boolean(flipped));

        return node;
    }

    const body = el('div', 'mp-pass-card__body');
    body.appendChild(passHeader(pass, badge));

    if (headerOnly) {
        const summary = stackSummary(pass);
        if (summary) {
            body.appendChild(el('p', 'mp-pass-card__stack-summary mt-1.5 truncate', summary));
        }
        node.appendChild(body);
        return node;
    }

    appendPassContent(body, pass, item, { preview });
    node.appendChild(body);

    return node;
}

async function renderCodeInto(container, payload, format = 'qr', options = {}) {
    if (!container) {
        return null;
    }

    container.replaceChildren();

    if (!payload) {
        container.appendChild(el('p', 'px-3 py-8 text-center text-sm text-on-surface-variant', t('Este pase no tiene un código guardado.')));
        return null;
    }

    container.appendChild(el('p', 'animate-pulse px-3 py-8 text-center text-sm text-on-surface-variant', t('Generando código…')));

    const bcid = {
        qr: 'qrcode',
        pdf417: 'pdf417',
        aztec: 'azteccode',
        code128: 'code128',
    }[format] || 'qrcode';
    const size = options.size || (options.scale ? Math.round(options.scale * 80) : 320);

    try {
        const canvas = document.createElement('canvas');

        if (bcid === 'qrcode') {
            const { default: QRCode } = await import('qrcode');
            await QRCode.toCanvas(canvas, String(payload), {
                width: size,
                margin: 2,
                errorCorrectionLevel: 'M',
                color: { dark: '#000000', light: '#ffffff' },
            });
        } else {
            const module = await import('bwip-js');
            const bwip = module.default ?? module;
            const settings = {
                bcid,
                text: String(payload),
                scale: options.scale ?? 3,
                backgroundcolor: 'FFFFFF',
                paddingwidth: 2,
                paddingheight: 2,
            };

            if (bcid === 'code128') {
                settings.height = 14;
                settings.includetext = options.includeText ?? true;
                settings.textsize = 8;
            }

            bwip.toCanvas(canvas, settings);
        }

        canvas.className = 'mx-auto h-auto max-w-full';
        canvas.setAttribute('role', 'img');
        canvas.setAttribute('aria-label', t('Código del pase'));
        container.replaceChildren(canvas);
        return { format: bcid, canvas };
    } catch (error) {
        if (bcid !== 'qrcode' && options.fallback !== false) {
            return renderCodeInto(container, payload, 'qr', { ...options, fallback: false });
        }

        container.replaceChildren();
        container.appendChild(el('p', 'px-3 py-8 text-center text-sm text-on-surface-variant', t('No se pudo dibujar el código.')));
        return null;
    }
}

class WalletApp {
    constructor(options) {
        this.options = options;
        this.items = [];
        this.filter = 'all';
        this.view = localStorage.getItem('mypass.passView') === 'grid' ? 'grid' : 'stack';
        this.selectedId = null;
        this.flippedId = null;
        this.wakeLock = null;
        this.passList = [];
        this.bind();
    }

    bind() {
        this.panel = document.getElementById('panel-passes');
        this.stack = document.getElementById('pass-stack');
        this.empty = document.getElementById('passes-empty');
        this.workspace = document.getElementById('passes-workspace');
        this.filters = document.getElementById('pass-filters');
        this.search = document.getElementById('pass-search');
        this.activeCount = document.getElementById('pass-active-count');
        this.archiveToggle = document.getElementById('pass-archive-toggle');
        this.archive = document.getElementById('pass-archive');
        this.archiveCount = document.getElementById('pass-archive-count');
        this.archiveLabel = document.getElementById('pass-archive-label');
        this.archiveChevron = document.getElementById('pass-archive-chevron');
        this.inspector = document.getElementById('pass-inspector');
        this.codeCanvas = document.getElementById('pass-code-canvas');
        this.codeFormatBadge = document.getElementById('pass-code-format-badge');
        this.detailTag = document.getElementById('pass-detail-tag');
        this.detailTitle = document.getElementById('pass-detail-title');
        this.detailSubtitle = document.getElementById('pass-detail-subtitle');
        this.detailNotes = document.getElementById('pass-detail-notes');
        this.codeCaption = document.getElementById('pass-code-caption');
        this.codeHash = document.getElementById('pass-code-hash');
        this.itemId = document.getElementById('pass-item-id');
        this.presentButton = document.getElementById('pass-present');
        this.shareButton = document.getElementById('pass-share');
        this.mapButton = document.getElementById('pass-map');
        this.calendarButton = document.getElementById('pass-calendar');
        this.copyButton = document.getElementById('pass-copy');
        this.downloadButton = document.getElementById('pass-download');
        this.printButton = document.getElementById('pass-print');
        this.editButton = document.getElementById('pass-edit');
        this.quickPresent = document.getElementById('pass-quick-present');
        this.nextCard = document.getElementById('pass-next');
        this.nextTitle = document.getElementById('pass-next-title');
        this.nextMeta = document.getElementById('pass-next-meta');
        this.nextBadges = document.getElementById('pass-next-badges');
        this.nextPresent = document.getElementById('pass-next-present');
        this.addToggle = document.getElementById('pass-add-toggle');
        this.addMenu = document.getElementById('pass-add-menu');
        this.viewStack = document.getElementById('pass-view-stack');
        this.viewGrid = document.getElementById('pass-view-grid');
        this.presentation = document.getElementById('pass-presentation');
        this.presentationTitle = document.getElementById('pass-presentation-title');
        this.presentationMeta = document.getElementById('pass-presentation-meta');
        this.presentationCode = document.getElementById('pass-presentation-code');
        this.presentationHint = document.getElementById('pass-presentation-hint');

        if (!this.stack) {
            return;
        }

        this.filters?.addEventListener('click', (event) => {
            const button = event.target.closest('[data-pass-filter]');
            if (!button) {
                return;
            }
            this.filter = button.dataset.passFilter;
            const filtered = this.filteredActive();
            if (!filtered.some((pass) => pass.item.id === this.selectedId)) {
                this.selectedId = filtered[0]?.item.id ?? null;
            }
            this.refresh();
        });

        this.search?.addEventListener('input', () => {
            const filtered = this.filteredActive();
            if (!filtered.some((entry) => entry.item.id === this.selectedId)) {
                this.selectedId = filtered[0]?.item.id ?? null;
            }
            this.refresh();
        });

        this.viewStack?.addEventListener('click', () => this.setView('stack'));
        this.viewGrid?.addEventListener('click', () => this.setView('grid'));

        document.querySelectorAll('[data-pass-new]').forEach((button) => {
            button.addEventListener('click', () => {
                this.closeAddMenu();
                this.options.openGenerator(button.dataset.passNew);
            });
        });

        document.getElementById('pass-open-generator')?.addEventListener('click', () => {
            this.closeAddMenu();
            this.options.openGenerator('flight');
        });
        document.getElementById('passes-empty-create')?.addEventListener('click', () => this.options.openGenerator('flight'));

        this.addToggle?.addEventListener('click', () => {
            const open = this.addMenu?.hidden;
            if (open) {
                this.openAddMenu();
            } else {
                this.closeAddMenu();
            }
        });

        document.addEventListener('click', (event) => {
            if (!this.addMenu || this.addMenu.hidden) {
                return;
            }
            if (!event.target.closest('#pass-add-menu') && !event.target.closest('#pass-add-toggle')) {
                this.closeAddMenu();
            }
        });

        this.quickPresent?.addEventListener('click', () => this.presentSelected());
        this.nextPresent?.addEventListener('click', () => this.presentNext());
        this.presentButton?.addEventListener('click', () => this.presentSelected());
        this.mapButton?.addEventListener('click', () => this.openMap());
        this.calendarButton?.addEventListener('click', () => this.addToCalendar());
        this.copyButton?.addEventListener('click', () => this.copySelected());
        this.downloadButton?.addEventListener('click', () => this.downloadSelected());
        this.printButton?.addEventListener('click', () => this.printSelected());
        this.shareButton?.addEventListener('click', () => this.shareSelected());
        this.editButton?.addEventListener('click', () => this.editSelected());
        this.archiveToggle?.addEventListener('click', () => this.toggleArchive());

        document.getElementById('pass-presentation-close')?.addEventListener('click', () => this.closePresentation());
        document.getElementById('pass-presentation-done')?.addEventListener('click', () => this.closePresentation());
        this.presentation?.addEventListener('close', () => this.releasePresentation());

        document.addEventListener('keydown', (event) => {
            if (event.code !== 'Space' || this.panel?.hidden || this.presentation?.open) {
                return;
            }
            const target = event.target;
            if (target instanceof HTMLElement && ['INPUT', 'TEXTAREA', 'SELECT', 'BUTTON', 'A'].includes(target.tagName)) {
                return;
            }
            event.preventDefault();
            this.presentSelected();
        });

        window.addEventListener('resize', () => {
            clearTimeout(this.flipResizeTimer);
            this.flipResizeTimer = setTimeout(() => this.syncAllFlipHeights(), 120);
        });
        document.fonts?.ready?.then(() => this.syncAllFlipHeights()).catch(() => {});

        this.setView(this.view, false);
    }

    openAddMenu() {
        if (!this.addMenu) {
            return;
        }
        this.addMenu.hidden = false;
        this.addToggle?.setAttribute('aria-expanded', 'true');
    }

    closeAddMenu() {
        if (!this.addMenu) {
            return;
        }
        this.addMenu.hidden = true;
        this.addToggle?.setAttribute('aria-expanded', 'false');
    }

    setItems(items) {
        this.items = Array.isArray(items) ? items : [];
        const previousId = this.selectedId;
        if (!this.selectedId || !this.allPasses().some((pass) => pass.item.id === this.selectedId)) {
            this.selectedId = this.filteredActive()[0]?.item.id ?? this.allPasses()[0]?.item.id ?? null;
        }
        if (this.selectedId !== previousId) {
            this.flippedId = null;
        }
        this.refresh();
    }

    allPasses() {
        return this.items.filter(isPassItem).map((item) => ({
            item,
            pass: passFromItem(item),
            archived: isExpired(item),
        }));
    }

    activePasses() {
        return sortActivePasses(
            this.allPasses().filter((entry) => !entry.archived && entry.item.state !== 'transferred'),
        );
    }

    archivedPasses() {
        return this.allPasses().filter((entry) => entry.archived || entry.item.state === 'transferred');
    }

    filteredActive() {
        const active = this.activePasses();
        const query = (this.search?.value ?? '').trim().toLowerCase();
        const filtered = this.filter === 'all'
            ? active
            : active.filter((entry) => entry.pass.category === this.filter);

        if (!query) {
            return filtered;
        }

        return filtered.filter((entry) => {
            const pass = entry.pass;
            const haystack = [
                pass.title,
                pass.issuer,
                pass.holder,
                ...Object.values(pass.fields || {}),
            ].filter(Boolean).join(' ').toLowerCase();

            return haystack.includes(query);
        });
    }

    entries() {
        return this.allPasses();
    }

    selectedEntry() {
        return this.allPasses().find((entry) => entry.item.id === this.selectedId) || null;
    }

    nextPassEntry() {
        return this.activePasses()[0] || null;
    }

    presentNext() {
        const entry = this.nextPassEntry();
        if (!entry) {
            this.options.toast(t('Primero elige un pase.'));
            return;
        }

        this.selectedId = entry.item.id;
        this.refresh();
        this.presentSelected();
    }

    refreshNextPass() {
        if (!this.nextCard) {
            return;
        }

        const entry = this.nextPassEntry();
        this.nextCard.hidden = !entry;

        if (!entry || !this.nextTitle || !this.nextMeta || !this.nextBadges) {
            return;
        }

        const { pass, item } = entry;
        const date = passDateFor(entry);
        this.nextTitle.textContent = pass.title;
        this.nextMeta.textContent = [
            pass.issuer || categoryLabel(pass.category),
            date
                ? formatDate(date)
                : (item.expires_at ? formatDate(item.expires_at) : t('Sin fecha')),
        ].filter(Boolean).join(' · ');

        this.nextBadges.replaceChildren();
        this.nextBadges.appendChild(el('span', 'rounded-full bg-white/10 px-2.5 py-1 text-[11px] font-bold text-white/80', categoryLabel(pass.category)));
        if (isUpcoming(entry)) {
            this.nextBadges.appendChild(el('span', 'rounded-full bg-accent px-2.5 py-1 text-[11px] font-bold text-accent-deep', t('Próximo')));
        }
    }

    refresh() {
        if (!this.stack) {
            return;
        }

        if (this.flippedId && this.flippedId !== this.selectedId) {
            this.flippedId = null;
        }

        const active = this.activePasses();
        const filtered = this.filteredActive();
        const archive = this.archivedPasses();

        this.empty.hidden = active.length > 0;
        this.workspace.hidden = active.length === 0;
        this.archiveToggle?.classList.toggle('hidden', archive.length === 0);

        this.activeCount.textContent = active.length === 1
            ? t('1 pase activo')
            : tf(':count pases activos', { count: active.length });

        const counts = {
            all: active.length,
            flight: active.filter((entry) => entry.pass.category === 'flight').length,
            event: active.filter((entry) => entry.pass.category === 'event').length,
            transit: active.filter((entry) => entry.pass.category === 'transit').length,
            id: active.filter((entry) => entry.pass.category === 'id').length,
            membership: active.filter((entry) => entry.pass.category === 'membership').length,
        };

        document.querySelectorAll('[data-pass-filter]').forEach((button) => {
            button.setAttribute('aria-pressed', button.dataset.passFilter === this.filter ? 'true' : 'false');
        });
        document.querySelectorAll('[data-pass-count]').forEach((span) => {
            span.textContent = counts[span.dataset.passCount] ?? 0;
        });

        this.renderStack(filtered);
        this.renderInspector();
        this.renderArchive(archive);
        this.refreshNextPass();
    }

    renderStack(entries) {
        this.stack.replaceChildren();
        this.stack.dataset.view = this.view;

        if (entries.length === 0) {
            const searching = (this.search?.value ?? '').trim().length > 0;
            const emptyFilter = el('div', 'mp-empty');
            emptyFilter.appendChild(icon('filter_alt_off', 'text-[30px] text-outline'));
            emptyFilter.appendChild(el(
                'p',
                'mt-2 font-semibold',
                searching ? t('No hay pases que coincidan.') : t('No hay pases en esta categoría.'),
            ));
            emptyFilter.appendChild(el(
                'p',
                'mp-hint mt-1',
                searching ? t('Prueba con otra búsqueda.') : t('Prueba con otro filtro o crea un pase nuevo.'),
            ));
            this.stack.appendChild(emptyFilter);
            return;
        }

        entries.forEach((entry, index) => {
            const selected = entry.item.id === this.selectedId;
            const next = this.nextPassEntry();
            const flipped = selected && this.flippedId === entry.item.id;
            const shareAllowed = entry.pass.options?.share !== false && this.options.canShare();
            const card = createPassCardElement(entry.item, {
                selected,
                badge: next?.item.id === entry.item.id ? t('Próximo') : null,
                highlight: next?.item.id === entry.item.id,
                headerOnly: this.view === 'stack' && !selected,
                flippable: selected,
                flipped,
                shareAllowed,
            });
            card.style.setProperty('--stack-index', String(index));

            if (!selected) {
                card.addEventListener('click', () => this.select(entry.item.id));
                card.addEventListener('keydown', (event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        this.select(entry.item.id);
                    }
                });
            } else {
                this.bindFlipCardActions(card, entry.item.id, shareAllowed);

                const codeWrap = card.querySelector('[data-card-code]');
                if (codeWrap) {
                    renderCodeInto(codeWrap, passPayload(entry.item, entry.pass), entry.pass.code?.format || 'qr', {
                        scale: 3,
                        includeText: false,
                    }).then(() => this.syncFlipHeight(card));
                }
            }

            this.stack.appendChild(card);

            if (selected) {
                window.requestAnimationFrame(() => this.syncFlipHeight(card));
            }
        });
    }

    renderInspector() {
        const entry = this.selectedEntry();
        if (!entry) {
            this.detailTag.textContent = t('Selecciona un pase');
            this.detailTitle.textContent = t('Tus pases aparecerán aquí');
            this.detailSubtitle.textContent = t('Elige una tarjeta de la pila para ver su código y su reverso.');
            this.detailNotes.replaceChildren();
            this.codeCanvas.replaceChildren();
            this.codeCaption.textContent = '';
            this.codeHash.textContent = '';
            this.itemId.textContent = '';
            this.codeFormatBadge.textContent = t('QR');
            this.presentButton.disabled = true;
            this.copyButton.disabled = true;
            this.downloadButton.disabled = true;
            this.printButton.disabled = true;
            this.shareButton.disabled = true;
            this.editButton.disabled = true;
            if (this.mapButton) {
                this.mapButton.hidden = true;
            }
            if (this.calendarButton) {
                this.calendarButton.hidden = true;
            }
            return;
        }

        const { item, pass } = entry;
        this.detailTag.textContent = categoryLabel(pass.category);
        this.detailTitle.textContent = pass.title;
        this.detailSubtitle.textContent = subtitleFor(pass) || t('Pase guardado en tu bóveda.');
        this.detailNotes.replaceChildren();
        for (const note of pass.notes.slice(0, 6)) {
            this.detailNotes.appendChild(el('span', 'mp-badge mp-badge-neutral', note));
        }

        const payload = passPayload(item, pass);
        const format = pass.code?.format || 'qr';
        this.codeFormatBadge.textContent = {
            qr: 'QR',
            pdf417: 'PDF417',
            aztec: 'Aztec',
            code128: 'Code 128',
        }[format] || 'QR';
        this.codeCaption.textContent = payload.length > 74 ? `${payload.slice(0, 74)}…` : payload;
        this.codeHash.textContent = pass.code?.source === 'decoded'
            ? t('Código leído del boleto original')
            : t('Código generado por My Pass');
        this.itemId.textContent = item.id.slice(0, 8).toUpperCase();

        const shareAllowed = pass.options?.share !== false && this.options.canShare();
        this.presentButton.disabled = false;
        this.copyButton.disabled = false;
        this.downloadButton.disabled = false;
        this.printButton.disabled = false;
        this.editButton.disabled = false;
        this.shareButton.disabled = !shareAllowed;
        this.shareButton.title = shareAllowed ? '' : t('Agrega un contacto de confianza para compartir.');

        if (this.mapButton) {
            const url = passMapUrl(pass);
            this.mapButton.hidden = !url;
            this.mapButton.disabled = !url;
        }
        if (this.calendarButton) {
            this.calendarButton.hidden = !passCalendar(item, pass);
        }

        renderCodeInto(this.codeCanvas, payload, format, { scale: 4 });
    }

    renderArchive(entries) {
        if (!this.archive) {
            return;
        }

        this.archiveCount.textContent = entries.length === 1
            ? t('1 pase vencido')
            : tf(':count pases vencidos', { count: entries.length });
        this.archive.replaceChildren();

        for (const entry of entries) {
            const card = el('article', 'mp-card flex flex-col justify-between p-4 opacity-80 transition-opacity hover:opacity-100');
            const header = el('div', 'flex items-center justify-between gap-2 text-[12px] text-on-surface-variant');
            header.appendChild(el('span', 'font-semibold', categoryLabel(entry.pass.category)));
            header.appendChild(el('span', 'rounded-full bg-surface-container px-2 py-0.5', entry.item.state === 'transferred' ? t('Transferido') : t('Vencido')));
            card.appendChild(header);
            card.appendChild(el('h4', 'mt-2 font-semibold text-on-surface', entry.pass.title));
            card.appendChild(el('p', 'text-sm text-on-surface-variant', [entry.pass.issuer, entry.item.expires_at ? formatDate(entry.item.expires_at) : ''].filter(Boolean).join(' · ')));
            const button = el('button', 'mt-3 inline-flex min-h-10 items-center gap-1 self-start text-sm font-semibold text-primary hover:underline');
            button.type = 'button';
            button.appendChild(icon('open_in_new', 'text-[18px]'));
            button.appendChild(document.createTextNode(t('Ver detalles')));
            button.addEventListener('click', () => {
                this.selectedId = entry.item.id;
                this.refresh();
                if (window.innerWidth < 1024) {
                    this.inspector?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            });
            card.appendChild(button);
            this.archive.appendChild(card);
        }
    }

    select(id) {
        if (this.selectedId !== id) {
            this.flippedId = null;
        }
        this.selectedId = id;
        this.refresh();

        const card = this.stack?.querySelector(`[data-pass-id="${CSS.escape(id)}"]`) ?? null;
        if (card) {
            window.requestAnimationFrame(() => {
                centerCardInView(this.stack?.querySelector(`[data-pass-id="${CSS.escape(id)}"]`));
            });
        }

        window.dispatchEvent(new CustomEvent('mypass:item-open', { detail: { id } }));

        return Boolean(card);
    }

    toggleFlip(id) {
        const card = this.stack?.querySelector(`[data-pass-id="${id}"]`);
        if (!card || !card.classList.contains('mp-pass-card--flip')) {
            return;
        }

        const flipped = !card.classList.contains('is-flipped');
        card.classList.toggle('is-flipped', flipped);
        this.flippedId = flipped ? id : null;
        syncFlipFaceAccessibility(card, flipped);
        this.syncFlipHeight(card);

        const target = card.querySelector(
            flipped
                ? '.mp-pass-flip__face--back [data-flip-toggle]'
                : '.mp-pass-flip__face--front [data-flip-toggle]',
        );
        window.setTimeout(() => target?.focus({ preventScroll: true }), 60);
    }

    syncFlipHeight(card) {
        if (!card?.classList.contains('mp-pass-card--flip')) {
            return;
        }

        const flipped = card.classList.contains('is-flipped');
        const face = card.querySelector(
            flipped ? '.mp-pass-flip__face--back' : '.mp-pass-flip__face--front',
        );
        if (!face) {
            return;
        }

        const height = Math.ceil(face.scrollHeight);
        if (height > 0) {
            card.style.setProperty('--flip-height', `${height}px`);
        }
    }

    syncAllFlipHeights() {
        this.stack?.querySelectorAll('.mp-pass-card--flip').forEach((card) => this.syncFlipHeight(card));
    }

    bindFlipCardActions(card, itemId, shareAllowed) {
        card.querySelectorAll('[data-flip-toggle]').forEach((button) => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                this.toggleFlip(itemId);
            });
        });

        card.querySelectorAll('[data-pass-action]').forEach((button) => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                if (button.disabled) {
                    return;
                }

                switch (button.dataset.passAction) {
                    case 'present':
                        this.presentSelected();
                        break;
                    case 'share':
                        if (shareAllowed) {
                            this.shareSelected();
                        }
                        break;
                    case 'edit':
                        this.editSelected();
                        break;
                    case 'map':
                        this.openMap();
                        break;
                    case 'calendar':
                        this.addToCalendar();
                        break;
                }
            });
        });
    }

    setView(view, persist = true) {
        this.view = view;
        if (persist) {
            localStorage.setItem('mypass.passView', view);
        }
        if (this.stack) {
            this.stack.dataset.view = view;
        }
        this.viewStack?.setAttribute('aria-pressed', view === 'stack' ? 'true' : 'false');
        this.viewGrid?.setAttribute('aria-pressed', view === 'grid' ? 'true' : 'false');
        this.viewStack?.classList.toggle('bg-paper', view === 'stack');
        this.viewStack?.classList.toggle('shadow-sm', view === 'stack');
        this.viewGrid?.classList.toggle('bg-paper', view === 'grid');
        this.viewGrid?.classList.toggle('shadow-sm', view === 'grid');
        if (this.activePasses().length > 0) {
            this.refresh();
        }
    }

    toggleArchive() {
        const open = this.archive?.hidden ?? true;
        if (!this.archive) {
            return;
        }
        this.archive.hidden = !open;
        this.archiveToggle?.setAttribute('aria-expanded', open ? 'true' : 'false');
        if (this.archiveLabel) {
            this.archiveLabel.textContent = open ? t('Ocultar') : t('Mostrar');
        }
        this.archiveChevron?.classList.toggle('rotate-180', open);
    }

    async presentSelected() {
        const entry = this.selectedEntry();
        if (!entry) {
            this.options.toast(t('Primero elige un pase.'));
            return;
        }

        const { item, pass } = entry;
        this.presentationTitle.textContent = pass.title;
        this.presentationMeta.textContent = subtitleFor(pass);
        this.presentationCode.replaceChildren();

        try {
            const result = await renderCodeInto(this.presentationCode, passPayload(item, pass), pass.code?.format || 'qr', {
                scale: 6,
                includeText: false,
            });
            if (result?.canvas) {
                result.canvas.style.maxHeight = 'min(48dvh, 380px)';
                result.canvas.style.width = 'auto';
            }
        } finally {
            if (this.presentation && !this.presentation.open) {
                this.presentation.showModal();
                this.requestPresentationSupport();
                this.presentation.requestFullscreen?.().catch(() => {});
            }
        }
    }

    async requestPresentationSupport() {
        this.presentationHint.textContent = t('Sube el brillo de la pantalla para facilitar la lectura.');
        try {
            if ('wakeLock' in navigator) {
                this.wakeLock = await navigator.wakeLock.request('screen');
                this.presentationHint.textContent = t('Pantalla activa mientras presentas el código.');
            }
        } catch {
            this.wakeLock = null;
        }
    }

    releasePresentation() {
        this.wakeLock?.release?.().catch(() => {});
        this.wakeLock = null;
        if (document.fullscreenElement) {
            document.exitFullscreen?.().catch(() => {});
        }
    }

    closePresentation() {
        this.presentation?.close();
        this.releasePresentation();
    }

    openMap() {
        const entry = this.selectedEntry();
        if (!entry) {
            return;
        }
        const url = passMapUrl(entry.pass);
        if (url) {
            window.open(url, '_blank', 'noopener,noreferrer');
        }
    }

    addToCalendar() {
        const entry = this.selectedEntry();
        if (!entry) {
            return;
        }
        if (!downloadCalendar(entry.item, entry.pass)) {
            this.options.toast?.(t('Este pase no tiene una fecha para el calendario.'));
        }
    }

    async copySelected() {
        const entry = this.selectedEntry();
        if (!entry) {
            return;
        }
        const payload = passPayload(entry.item, entry.pass);
        try {
            await navigator.clipboard.writeText(payload);
            this.options.toast(t('Código copiado.'));
            setTimeout(async () => {
                try {
                    const current = await navigator.clipboard.readText();
                    if (current === payload) {
                        await navigator.clipboard.writeText('');
                    }
                } catch {
                    // Clipboard access can be denied; the code was still copied.
                }
            }, 60_000);
        } catch {
            this.options.toast(t('No se pudo copiar el código.'));
        }
    }

    downloadSelected() {
        const entry = this.selectedEntry();
        const canvas = this.codeCanvas?.querySelector('canvas');
        if (!entry || !canvas) {
            this.options.toast(t('No se pudo generar la imagen del código.'));
            return;
        }

        const link = document.createElement('a');
        link.href = canvas.toDataURL('image/png');
        link.download = `my-pass-${entry.pass.title.replace(/[^a-z0-9]+/gi, '-').toLowerCase() || 'pase'}.png`;
        link.click();
        this.options.toast(t('Código descargado como imagen.'));
    }

    printSelected() {
        const entry = this.selectedEntry();
        if (!entry) {
            return;
        }

        const canvas = this.codeCanvas?.querySelector('canvas');
        const popup = window.open('', '_blank');
        if (!popup) {
            this.options.toast(t('El navegador bloqueó la ventana de impresión.'));
            return;
        }
        popup.opener = null;

        const doc = popup.document;
        doc.title = entry.pass.title;
        const style = doc.createElement('style');
        style.textContent = `
            body { font-family: system-ui, sans-serif; color: #13202b; padding: 32px; }
            h1 { font-size: 26px; margin: 0 0 6px; }
            p { margin: 4px 0; color: #475569; }
            .code { margin: 24px auto; max-width: 360px; }
            .code img { width: 100%; display: block; }
            dl { display: grid; grid-template-columns: max-content 1fr; gap: 6px 18px; margin-top: 20px; }
            dt { font-weight: 700; }
            dd { margin: 0; }
            .footer { margin-top: 28px; font-size: 12px; color: #64748b; }
        `;
        doc.head.appendChild(style);

        const title = doc.createElement('h1');
        title.textContent = entry.pass.title;
        doc.body.appendChild(title);
        const subtitle = doc.createElement('p');
        subtitle.textContent = [entry.pass.issuer, entry.pass.holder].filter(Boolean).join(' · ');
        doc.body.appendChild(subtitle);

        if (canvas) {
            const code = doc.createElement('div');
            code.className = 'code';
            const image = doc.createElement('img');
            image.src = canvas.toDataURL('image/png');
            image.alt = t('Código del pase');
            code.appendChild(image);
            doc.body.appendChild(code);
        }

        const fields = entry.pass.fields || {};
        const details = doc.createElement('dl');
        for (const [label, value] of Object.entries(fields)) {
            if (!value) {
                continue;
            }
            const term = doc.createElement('dt');
            term.textContent = label;
            const description = doc.createElement('dd');
            description.textContent = value;
            details.append(term, description);
        }
        if (details.children.length > 0) {
            doc.body.appendChild(details);
        }

        const footer = doc.createElement('p');
        footer.className = 'footer';
        footer.textContent = t('Generado por My Pass · Guarda este pase en un lugar seguro.');
        doc.body.appendChild(footer);

        popup.focus();
        popup.setTimeout(() => popup.print(), 300);
    }

    shareSelected() {
        const entry = this.selectedEntry();
        if (!entry || !this.options.canShare()) {
            return;
        }
        this.options.openShare(entry.item);
    }

    editSelected() {
        const entry = this.selectedEntry();
        if (!entry) {
            return;
        }
        this.options.openGenerator(entry.pass.category, entry.item, entry.pass);
    }

    reset() {
        this.items = [];
        this.selectedId = null;
        this.flippedId = null;
        this.filter = 'all';
        if (this.search) {
            this.search.value = '';
        }
        this.stack?.replaceChildren();
        this.archive?.replaceChildren();
        this.closeAddMenu();
        this.closePresentation();
    }
}

export {
    WalletApp,
    categoryIcon,
    categoryLabel,
    createPassCardElement,
    formatDate,
    formatTime,
    isPassItem,
    passFromItem,
    passPayload,
    renderCodeInto,
    THEME_COLORS,
};
