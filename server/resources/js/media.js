const MAX_LOGO_SOURCE_BYTES = 3 * 1024 * 1024;
const MAX_BANNER_SOURCE_BYTES = 8 * 1024 * 1024;
const MAX_LOGO_EDGE = 256;
const MAX_BANNER_EDGE = 1200;
const MAX_BANNER_DATA_URL = 500_000;

export function isImageDataUrl(value) {
    return typeof value === 'string' && /^data:image\/(png|jpe?g|webp);base64,/i.test(value);
}

async function bitmapFromFile(file) {
    return createImageBitmap(file);
}

function canvasDataUrl(bitmap, maxEdge, type, quality) {
    const scale = Math.min(1, maxEdge / Math.max(bitmap.width, bitmap.height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(bitmap.width * scale));
    canvas.height = Math.max(1, Math.round(bitmap.height * scale));
    const context = canvas.getContext('2d');

    if (!context) {
        throw new Error('No se pudo procesar la imagen.');
    }

    context.clearRect(0, 0, canvas.width, canvas.height);
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);

    return canvas.toDataURL(type, quality);
}

export async function logoDataUrl(file) {
    if (!file?.type?.startsWith('image/')) {
        throw new Error('El archivo debe ser una imagen.');
    }
    if (file.size > MAX_LOGO_SOURCE_BYTES) {
        throw new Error('La imagen supera el tamaño permitido.');
    }

    const bitmap = await bitmapFromFile(file);
    try {
        return canvasDataUrl(bitmap, MAX_LOGO_EDGE, 'image/png');
    } finally {
        bitmap.close?.();
    }
}

export async function bannerDataUrl(file) {
    if (!file?.type?.startsWith('image/')) {
        throw new Error('El archivo debe ser una imagen.');
    }
    if (file.size > MAX_BANNER_SOURCE_BYTES) {
        throw new Error('La imagen supera el tamaño permitido.');
    }

    const bitmap = await bitmapFromFile(file);
    try {
        for (const [edge, quality] of [[MAX_BANNER_EDGE, 0.82], [1000, 0.72], [800, 0.62]]) {
            let dataUrl = canvasDataUrl(bitmap, edge, 'image/webp', quality);
            if (!dataUrl.startsWith('data:image/webp')) {
                dataUrl = canvasDataUrl(bitmap, edge, 'image/jpeg', quality);
            }
            if (dataUrl.length <= MAX_BANNER_DATA_URL) {
                return dataUrl;
            }
        }
    } finally {
        bitmap.close?.();
    }

    throw new Error('La imagen del banner es demasiado grande.');
}
