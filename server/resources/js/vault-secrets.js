const BASE32_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';

export function newUuid() {
    if (globalThis.crypto?.randomUUID) {
        return globalThis.crypto.randomUUID();
    }

    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
        const random = Math.floor(Math.random() * 16);
        const value = char === 'x' ? random : (random & 0x3) | 0x8;
        return value.toString(16);
    });
}

function randomInt(max) {
    if (max <= 0) {
        return 0;
    }

    const limit = Math.floor(0x100000000 / max) * max;
    const buffer = new Uint32Array(1);

    do {
        crypto.getRandomValues(buffer);
    } while (buffer[0] >= limit);

    return buffer[0] % max;
}

export function generatePassword(options = {}) {
    const length = Math.min(128, Math.max(8, Number(options.length) || 20));
    const upper = options.upper !== false;
    const lower = options.lower !== false;
    const digits = options.digits !== false;
    const symbols = options.symbols !== false;
    const avoidAmbiguous = options.avoidAmbiguous === true;

    let sets = [];

    if (lower) {
        sets.push(avoidAmbiguous ? 'abcdefghijkmnopqrstuvwxyz' : 'abcdefghijklmnopqrstuvwxyz');
    }
    if (upper) {
        sets.push(avoidAmbiguous ? 'ABCDEFGHJKLMNPQRSTUVWXYZ' : 'ABCDEFGHIJKLMNOPQRSTUVWXYZ');
    }
    if (digits) {
        sets.push(avoidAmbiguous ? '23456789' : '0123456789');
    }
    if (symbols) {
        sets.push('!@#$%^&*()-_=+[]{};:,.?');
    }

    if (sets.length === 0) {
        sets = ['abcdefghijklmnopqrstuvwxyz'];
    }

    const all = sets.join('');
    const password = [];

    for (const set of sets) {
        password.push(set[randomInt(set.length)]);
    }

    while (password.length < length) {
        password.push(all[randomInt(all.length)]);
    }

    for (let index = password.length - 1; index > 0; index -= 1) {
        const swap = randomInt(index + 1);
        [password[index], password[swap]] = [password[swap], password[index]];
    }

    return password.slice(0, length).join('');
}

export function passwordStrength(password) {
    const value = String(password || '');
    if (!value) {
        return { score: 0, label: 'Sin contraseña', tone: 'neutral' };
    }

    let score = Math.min(45, value.length * 2.5);
    const groups = [
        /[a-z]/,
        /[A-Z]/,
        /[0-9]/,
        /[^A-Za-z0-9]/,
    ].filter((pattern) => pattern.test(value)).length;
    score += groups * 10;

    if (value.length >= 20) {
        score += 10;
    }
    if (value.length >= 28) {
        score += 10;
    }

    score = Math.min(100, Math.round(score));

    if (score >= 80) {
        return { score, label: 'Fuerte', tone: 'strong' };
    }
    if (score >= 55) {
        return { score, label: 'Aceptable', tone: 'medium' };
    }

    return { score, label: 'Débil', tone: 'weak' };
}

function base32Decode(value) {
    const normalized = String(value || '')
        .replace(/[\s-]+/g, '')
        .replace(/=+$/g, '')
        .toUpperCase();
    let bits = 0;
    let buffer = 0;
    const bytes = [];

    for (const char of normalized) {
        const index = BASE32_ALPHABET.indexOf(char);
        if (index === -1) {
            throw new Error('El secreto TOTP no es Base32 válido.');
        }
        buffer = (buffer << 5) | index;
        bits += 5;
        if (bits >= 8) {
            bits -= 8;
            bytes.push((buffer >>> bits) & 0xff);
        }
    }

    return new Uint8Array(bytes);
}

export function parseTotp(value) {
    const text = String(value || '').trim();
    if (!text) {
        return null;
    }

    if (!text.toLowerCase().startsWith('otpauth://')) {
        const secret = text.replace(/[\s-]+/g, '').toUpperCase();
        if (!/^[A-Z2-7]+=*$/.test(secret)) {
            return null;
        }
        return {
            secret,
            algorithm: 'SHA1',
            digits: 6,
            period: 30,
        };
    }

    try {
        const url = new URL(text);
        if (url.hostname.toLowerCase() !== 'totp') {
            return null;
        }
        const secret = (url.searchParams.get('secret') || '').replace(/[\s-]+/g, '').toUpperCase();
        if (!secret || !/^[A-Z2-7]+=*$/.test(secret)) {
            return null;
        }
        const algorithm = (url.searchParams.get('algorithm') || 'SHA1').toUpperCase();
        const digits = Math.min(10, Math.max(4, Number.parseInt(url.searchParams.get('digits') || '6', 10) || 6));
        const period = Math.min(120, Math.max(10, Number.parseInt(url.searchParams.get('period') || '30', 10) || 30));

        return {
            secret,
            algorithm: ['SHA1', 'SHA256', 'SHA512'].includes(algorithm) ? algorithm : 'SHA1',
            digits,
            period,
        };
    } catch {
        return null;
    }
}

export function totpSecondsLeft(period = 30) {
    const seconds = Math.floor(Date.now() / 1000);
    return period - (seconds % period);
}

export async function generateTotp(value, timestamp = Date.now()) {
    const config = typeof value === 'object' && value ? value : parseTotp(value);
    if (!config?.secret) {
        return null;
    }

    const algorithm = ['SHA1', 'SHA256', 'SHA512'].includes(config.algorithm)
        ? config.algorithm
        : 'SHA1';
    const webCryptoHash = {
        SHA1: 'SHA-1',
        SHA256: 'SHA-256',
        SHA512: 'SHA-512',
    }[algorithm] || 'SHA-1';
    const digits = Math.min(10, Math.max(4, Number(config.digits) || 6));
    const period = Math.min(120, Math.max(10, Number(config.period) || 30));
    const counter = Math.floor(timestamp / 1000 / period);
    const counterBuffer = new ArrayBuffer(8);
    const view = new DataView(counterBuffer);
    view.setUint32(0, Math.floor(counter / 0x100000000));
    view.setUint32(4, counter >>> 0);

    const keyBytes = base32Decode(config.secret);
    const key = await crypto.subtle.importKey(
        'raw',
        keyBytes,
        { name: 'HMAC', hash: webCryptoHash },
        false,
        ['sign'],
    );
    const signature = new Uint8Array(await crypto.subtle.sign('HMAC', key, counterBuffer));
    const offset = signature[signature.length - 1] & 0x0f;
    const binary = ((signature[offset] & 0x7f) << 24)
        | ((signature[offset + 1] & 0xff) << 16)
        | ((signature[offset + 2] & 0xff) << 8)
        | (signature[offset + 3] & 0xff);
    const code = String(binary % (10 ** digits)).padStart(digits, '0');

    return {
        code,
        digits,
        period,
        secondsLeft: period - (Math.floor(timestamp / 1000) % period),
    };
}

export async function copySensitive(text, toast, options = {}) {
    const value = String(text ?? '');
    if (!value) {
        return false;
    }

    try {
        await navigator.clipboard.writeText(value);
        toast?.(options.message || 'Copiado. Se borrará del portapapeles en unos segundos.');
        const delay = options.clearAfter ?? 45_000;
        setTimeout(async () => {
            try {
                const current = await navigator.clipboard.readText();
                if (current === value) {
                    await navigator.clipboard.writeText('');
                }
            } catch {
                // La lectura del portapapeles puede estar bloqueada; no se puede verificar.
            }
        }, delay);
        return true;
    } catch {
        toast?.('No se pudo copiar al portapapeles.');
        return false;
    }
}

export function splitList(value) {
    return String(value || '')
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);
}

export function formatDate(value, locale = 'es-MX') {
    if (!value) {
        return '';
    }

    const text = String(value).slice(0, 10);
    const [year, month, day] = text.split('-').map(Number);
    if (!year || !month || !day) {
        return String(value);
    }

    return new Date(year, month - 1, day).toLocaleDateString(locale, {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
    });
}

export function isExpired(value) {
    if (!value) {
        return false;
    }

    const date = new Date(`${String(value).slice(0, 10)}T23:59:59`);
    return !Number.isNaN(date.getTime()) && date.getTime() < Date.now();
}

export function safeExternalUrl(value) {
    const text = String(value || '').trim();
    if (!text) {
        return null;
    }

    try {
        const url = new URL(text);
        return ['http:', 'https:'].includes(url.protocol) ? url.toString() : null;
    } catch {
        return null;
    }
}
