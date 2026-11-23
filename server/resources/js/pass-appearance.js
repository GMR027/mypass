const MAX_BACKGROUND_DATA_URL = 2_500_000;
const DATA_IMAGE_PATTERN = /^data:image\/(?:png|jpe?g|webp);base64,[a-z0-9+/=]+$/i;

const svgTexture = (markup) => `url("data:image/svg+xml,${encodeURIComponent(markup)}")`;
const rgba = (alpha) => `rgba(255,255,255,${alpha.toFixed(3)})`;
const clampOpacity = (value) => Math.min(1, Math.max(0, value));

const PASS_TEXTURES = {
    dots: {
        label: 'Puntos',
        size: [18, 18],
        build: (opacity) => `radial-gradient(circle at 2px 2px, ${rgba(0.30 * opacity)} 1.5px, transparent 1.6px)`,
    },
    lines: {
        label: 'Líneas',
        size: [16, 16],
        build: (opacity) => svgTexture(
            `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16"><path d="M-4 20L20 -4" fill="none" stroke="${rgba(0.18 * opacity)}" stroke-width="1.4"/></svg>`,
        ),
    },
    grid: {
        label: 'Cuadrícula',
        size: [24, 24],
        build: (opacity) => svgTexture(
            `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path d="M24 0H0V24" fill="none" stroke="${rgba(0.14 * opacity)}" stroke-width="1"/></svg>`,
        ),
    },
    carbon: {
        label: 'Carbón',
        size: [18, 18],
        build: (opacity) => svgTexture(
            `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 18 18"><path d="M0 0L18 18M18 0L0 18" fill="none" stroke="${rgba(0.10 * opacity)}" stroke-width="1"/></svg>`,
        ),
    },
    waves: {
        label: 'Ondas',
        size: [80, 36],
        build: (opacity) => svgTexture(
            `<svg xmlns="http://www.w3.org/2000/svg" width="80" height="36" viewBox="0 0 80 36"><path d="M0 18C12 4 28 4 40 18S68 32 80 18" fill="none" stroke="${rgba(0.18 * opacity)}" stroke-width="1.6"/></svg>`,
        ),
    },
    grain: {
        label: 'Grano',
        size: [160, 160],
        build: (opacity) => svgTexture(
            `<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160" viewBox="0 0 160 160"><filter id="n"><feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="2" stitchTiles="stitch"/><feColorMatrix type="saturate" values="0"/></filter><rect width="160" height="160" filter="url(#n)" opacity="${(0.16 * opacity).toFixed(3)}"/></svg>`,
        ),
    },
};

const TEXTURE_SIZE_OPTIONS = [
    { value: 'sm', label: 'Pequeña' },
    { value: 'md', label: 'Mediana' },
    { value: 'lg', label: 'Grande' },
];

const REPEAT_OPTIONS = [
    { value: 'repeat', label: 'Repetir' },
    { value: 'repeat-x', label: 'Horizontal' },
    { value: 'repeat-y', label: 'Vertical' },
    { value: 'no-repeat', label: 'Sin repetir' },
];

const IMAGE_SIZE_OPTIONS = [
    { value: 'cover', label: 'Cubrir' },
    { value: 'contain', label: 'Contener' },
    { value: '50%', label: '50%' },
    { value: '100%', label: '100%' },
    { value: '200%', label: '200%' },
];

const TEXTURE_SIZE_FACTORS = {
    sm: 0.72,
    md: 1,
    lg: 1.55,
};

const DEFAULT_APPEARANCE = {
    texture: 'none',
    textureSize: 'md',
    textureRepeat: 'repeat',
    textureOpacity: 100,
    image: null,
    imageSize: 'cover',
    imageRepeat: 'no-repeat',
    imageShade: 30,
};

function normalizeAppearance(source) {
    const raw = source && typeof source === 'object' ? source : {};
    const texture = raw.texture === 'none' || Object.hasOwn(PASS_TEXTURES, raw.texture)
        ? raw.texture
        : 'none';
    const textureSize = TEXTURE_SIZE_OPTIONS.some((option) => option.value === raw.textureSize)
        ? raw.textureSize
        : DEFAULT_APPEARANCE.textureSize;
    const textureRepeat = REPEAT_OPTIONS.some((option) => option.value === raw.textureRepeat)
        ? raw.textureRepeat
        : DEFAULT_APPEARANCE.textureRepeat;
    const textureOpacity = Number.isFinite(Number(raw.textureOpacity))
        ? Math.min(100, Math.max(0, Math.round(Number(raw.textureOpacity))))
        : DEFAULT_APPEARANCE.textureOpacity;
    const image = typeof raw.image === 'string'
        && raw.image.length <= MAX_BACKGROUND_DATA_URL
        && DATA_IMAGE_PATTERN.test(raw.image)
        ? raw.image
        : null;
    const imageSize = IMAGE_SIZE_OPTIONS.some((option) => option.value === raw.imageSize)
        ? raw.imageSize
        : DEFAULT_APPEARANCE.imageSize;
    const imageRepeat = REPEAT_OPTIONS.some((option) => option.value === raw.imageRepeat)
        ? raw.imageRepeat
        : DEFAULT_APPEARANCE.imageRepeat;
    const imageShade = Number.isFinite(Number(raw.imageShade))
        ? Math.min(70, Math.max(0, Math.round(Number(raw.imageShade))))
        : DEFAULT_APPEARANCE.imageShade;

    return {
        texture,
        textureSize,
        textureRepeat,
        textureOpacity,
        image,
        imageSize,
        imageRepeat,
        imageShade,
    };
}

function textureImage(texture, opacity) {
    if (!texture) {
        return 'none';
    }

    return texture.build(clampOpacity(opacity / 100));
}

function applyAppearance(node, source) {
    const appearance = normalizeAppearance(source);
    const texture = PASS_TEXTURES[appearance.texture];

    if (texture) {
        const factor = TEXTURE_SIZE_FACTORS[appearance.textureSize] || 1;
        node.style.setProperty('--pass-texture', textureImage(texture, appearance.textureOpacity));
        node.style.setProperty(
            '--pass-texture-size',
            `${Math.round(texture.size[0] * factor)}px ${Math.round(texture.size[1] * factor)}px`,
        );
        node.style.setProperty('--pass-texture-repeat', appearance.textureRepeat);
    } else {
        node.style.removeProperty('--pass-texture');
        node.style.removeProperty('--pass-texture-size');
        node.style.removeProperty('--pass-texture-repeat');
    }

    if (appearance.image) {
        const alpha = (appearance.imageShade / 100).toFixed(2);
        node.style.setProperty('--pass-image', `url("${appearance.image}")`);
        node.style.setProperty('--pass-image-size', appearance.imageSize);
        node.style.setProperty('--pass-image-repeat', appearance.imageRepeat);
        node.style.setProperty(
            '--pass-image-shade',
            `linear-gradient(rgb(0 0 0 / ${alpha}), rgb(0 0 0 / ${alpha}))`,
        );
    } else {
        node.style.removeProperty('--pass-image');
        node.style.removeProperty('--pass-image-size');
        node.style.removeProperty('--pass-image-repeat');
        node.style.removeProperty('--pass-image-shade');
    }
}

export {
    applyAppearance,
    DEFAULT_APPEARANCE,
    IMAGE_SIZE_OPTIONS,
    MAX_BACKGROUND_DATA_URL,
    normalizeAppearance,
    PASS_TEXTURES,
    REPEAT_OPTIONS,
    TEXTURE_SIZE_FACTORS,
    TEXTURE_SIZE_OPTIONS,
    textureImage,
};
