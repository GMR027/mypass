import { mapQuery } from './geo';

const t = (key) => (window.mypassT ? window.mypassT(key) : key);

function pad(value) {
    return String(value).padStart(2, '0');
}

function parseDateTime(pass) {
    const fields = pass?.fields ?? {};
    const rawDate = String(fields.date || fields.expires || '').slice(0, 10);
    const dateMatch = rawDate.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (!dateMatch) {
        return null;
    }

    const timeRaw = fields.departure || fields.time || fields.boarding_start || '';
    const timeMatch = String(timeRaw).match(/^(\d{1,2}):(\d{2})$/);
    const start = new Date(
        Number(dateMatch[1]),
        Number(dateMatch[2]) - 1,
        Number(dateMatch[3]),
        timeMatch ? Number(timeMatch[1]) : 9,
        timeMatch ? Number(timeMatch[2]) : 0,
        0,
        0,
    );
    if (Number.isNaN(start.getTime())) {
        return null;
    }

    const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
    return { start, end, allDay: !timeMatch };
}

function icsDate(date, allDay = false) {
    if (allDay) {
        return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}`;
    }
    return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}T${pad(date.getHours())}${pad(date.getMinutes())}00`;
}

function escapeText(value) {
    return String(value ?? '')
        .replace(/\\/g, '\\\\')
        .replace(/\n/g, '\\n')
        .replace(/,/g, '\\,')
        .replace(/;/g, '\\;');
}

function fold(line) {
    const encoder = new TextEncoder();
    const output = [];
    let current = '';

    for (const char of line) {
        if (encoder.encode(current + char).length > 73) {
            output.push(current);
            current = ` ${char}`;
        } else {
            current += char;
        }
    }
    output.push(current);
    return output.join('\r\n');
}

function alarmsFor(pass) {
    if (pass?.category === 'flight') {
        return [1440, 120];
    }
    if (pass?.category === 'transit') {
        return [120];
    }
    if (pass?.category === 'event') {
        return [1440];
    }
    return [];
}

export function passCalendar(item, pass) {
    const timing = parseDateTime(pass);
    if (!timing) {
        return null;
    }

    const fields = pass?.fields ?? {};
    const location = mapQuery(pass) || '';
    const description = [
        pass.issuer,
        pass.holder,
        fields.flight ? `${t('Vuelo')} ${fields.flight}` : '',
        fields.gate ? `${t('Puerta')} ${fields.gate}` : '',
        fields.terminal ? `${t('Terminal')} ${fields.terminal}` : '',
        fields.seat ? `${t('Asiento')} ${fields.seat}` : '',
        fields.pnr ? `${t('Reserva')} ${fields.pnr}` : '',
        ...(pass.notes ?? []),
    ].filter(Boolean).join('\n');

    const allDay = timing.allDay;
    const end = allDay
        ? new Date(timing.start.getFullYear(), timing.start.getMonth(), timing.start.getDate() + 1)
        : timing.end;
    const lines = [
        'BEGIN:VCALENDAR',
        'VERSION:2.0',
        'PRODID:-//My Pass//Boveda//ES',
        'CALSCALE:GREGORIAN',
        'METHOD:PUBLISH',
        'BEGIN:VEVENT',
        `UID:${item.id}@mypass`,
        `DTSTAMP:${new Date().toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z')}`,
        `DTSTART${allDay ? ';VALUE=DATE' : ''}:${icsDate(timing.start, allDay)}`,
        `DTEND${allDay ? ';VALUE=DATE' : ''}:${icsDate(end, allDay)}`,
        `SUMMARY:${escapeText(pass.title || t('Pase'))}`,
        location ? `LOCATION:${escapeText(location)}` : '',
        description ? `DESCRIPTION:${escapeText(description)}` : '',
        ...alarmsFor(pass).flatMap((minutes) => [
            'BEGIN:VALARM',
            `TRIGGER:-PT${minutes}M`,
            'ACTION:DISPLAY',
            `DESCRIPTION:${escapeText(pass.title || t('Pase'))}`,
            'END:VALARM',
        ]),
        'END:VEVENT',
        'END:VCALENDAR',
        '',
    ].filter((line) => line !== '');

    const filename = `${String(pass.title || 'pase')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-zA-Z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 48) || 'pase'}.ics`;

    return {
        filename,
        content: lines.map(fold).join('\r\n'),
    };
}

export function downloadCalendar(item, pass) {
    const calendar = passCalendar(item, pass);
    if (!calendar) {
        return false;
    }

    downloadText(calendar.content, calendar.filename);
    return true;
}

function downloadText(content, filename) {
    const blob = new Blob([content], { type: 'text/calendar;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    setTimeout(() => URL.revokeObjectURL(url), 60000);
}

function dateFromIso(value) {
    const match = String(value || '').slice(0, 10).match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (!match) {
        return null;
    }
    const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]), 0, 0, 0, 0);
    return Number.isNaN(date.getTime()) ? null : date;
}

function timeParts(value) {
    const match = String(value || '').slice(0, 5).match(/^(\d{1,2}):(\d{2})$/);
    return match ? [Number(match[1]), Number(match[2])] : null;
}

export function medicalCalendar(item, medical) {
    const events = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    for (const medication of medical?.medicationSchedule || []) {
        if (!medication.name || !Array.isArray(medication.times)) {
            continue;
        }
        const startDate = dateFromIso(medication.startDate) || today;
        const endDate = dateFromIso(medication.endDate);
        for (const time of medication.times) {
            const parts = timeParts(time);
            if (!parts) {
                continue;
            }
            const start = new Date(startDate);
            start.setHours(parts[0], parts[1], 0, 0);
            const uid = `${item.id}-med-${medication.id}-${time.replace(':', '')}@mypass`;
            const description = [
                medication.dose,
                medication.notes,
                t('Recordatorio de medicamento de My Pass.'),
            ].filter(Boolean).join('\n');
            events.push([
                'BEGIN:VEVENT',
                `UID:${uid}`,
                `DTSTAMP:${new Date().toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z')}`,
                `DTSTART:${icsDate(start)}`,
                'DURATION:PT10M',
                `SUMMARY:${escapeText(`${t('Medicamento')}: ${medication.name}`)}`,
                `DESCRIPTION:${escapeText(description)}`,
                `RRULE:FREQ=DAILY${endDate ? `;UNTIL=${icsDate(endDate).slice(0, 8)}T235959` : ''}`,
                'BEGIN:VALARM',
                'TRIGGER:PT0M',
                'ACTION:DISPLAY',
                `DESCRIPTION:${escapeText(medication.name)}`,
                'END:VALARM',
                'END:VEVENT',
            ]);
        }
    }

    for (const appointment of medical?.appointments || []) {
        const date = dateFromIso(appointment.date);
        if (!appointment.title || !date) {
            continue;
        }
        const parts = timeParts(appointment.time) || [9, 0];
        const start = new Date(date);
        start.setHours(parts[0], parts[1], 0, 0);
        const end = new Date(start.getTime() + 60 * 60 * 1000);
        const reminder = Number(appointment.reminderMinutes ?? 1440);
        events.push([
            'BEGIN:VEVENT',
            `UID:${item.id}-appointment-${appointment.id}@mypass`,
            `DTSTAMP:${new Date().toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z')}`,
            `DTSTART:${icsDate(start)}`,
            `DTEND:${icsDate(end)}`,
            `SUMMARY:${escapeText(appointment.title)}`,
            appointment.location ? `LOCATION:${escapeText(appointment.location)}` : '',
            appointment.notes ? `DESCRIPTION:${escapeText(appointment.notes)}` : '',
            'BEGIN:VALARM',
            `TRIGGER:-PT${Math.max(0, reminder)}M`,
            'ACTION:DISPLAY',
            `DESCRIPTION:${escapeText(appointment.title)}`,
            'END:VALARM',
            'END:VEVENT',
        ]);
    }

    const lines = events
        .flat()
        .filter((line) => line !== '');
    if (lines.length === 0) {
        return null;
    }

    const filename = `recordatorios-${String(item.metadata?.title || 'salud')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-zA-Z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 40) || 'salud'}.ics`;

    return {
        filename,
        content: [
            'BEGIN:VCALENDAR',
            'VERSION:2.0',
            'PRODID:-//My Pass//Salud//ES',
            'CALSCALE:GREGORIAN',
            'METHOD:PUBLISH',
            ...lines,
            'END:VCALENDAR',
            '',
        ].map(fold).join('\r\n'),
    };
}

export function downloadMedicalCalendar(item, medical) {
    const calendar = medicalCalendar(item, medical);
    if (!calendar) {
        return false;
    }
    downloadText(calendar.content, calendar.filename);
    return true;
}
