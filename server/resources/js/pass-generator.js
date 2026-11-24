import {
    THEME_COLORS,
    categoryLabel,
    createPassCardElement,
    passPayload,
    renderCodeInto,
} from './passes';
import {
    MAX_BACKGROUND_DATA_URL,
    normalizeAppearance,
    PASS_TEXTURES,
    TEXTURE_SIZE_FACTORS,
    textureImage,
} from './pass-appearance';
import { parseBcbp, parsePassText } from './pass-parser';

const t = (key) => (window.mypassT ? window.mypassT(key) : key);
const tf = (key, values) => (window.mypassTF ? window.mypassTF(key, values) : key);
const locale = () => (window.mypassLocale === 'en' ? 'en-US' : 'es-MX');

const MAX_FILE_SIZE = 25 * 1024 * 1024;
const MAX_IMAGE_EDGE = 2400;
const MAX_LOGO_EDGE = 256;
const MAX_BACKGROUND_FILE_SIZE = 15 * 1024 * 1024;
const MAX_BACKGROUND_EDGE = 1600;

let ocrWorkerPromise = null;
let ocrLogger = null;
let zxingScriptPromise = null;
let zxingModulePromise = null;

function newUuid() {
    if (globalThis.crypto?.randomUUID) {
        return globalThis.crypto.randomUUID();
    }

    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
        const random = Math.floor(Math.random() * 16);
        const value = char === 'x' ? random : (random & 0x3) | 0x8;
        return value.toString(16);
    });
}

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

function fieldSchemas() {
    return {
        flight: [
            { key: 'origin', label: t('Código origen'), placeholder: 'MAD' },
            { key: 'destination', label: t('Código destino'), placeholder: 'MEX' },
            { key: 'flight', label: t('Número de vuelo'), placeholder: 'IB 6401' },
            { key: 'date', label: t('Fecha de salida'), type: 'date' },
            { key: 'departure', label: t('Hora de salida'), type: 'time' },
            { key: 'boarding_start', label: t('Inicio de abordaje'), type: 'time' },
            { key: 'boarding_end', label: t('Cierre de abordaje'), type: 'time' },
            { key: 'gate', label: t('Puerta de embarque'), placeholder: 'H12' },
            { key: 'terminal', label: t('Terminal'), placeholder: 'T2' },
            { key: 'seat', label: t('Asiento'), placeholder: '2A' },
            { key: 'zone', label: t('Zona / grupo'), placeholder: '1' },
            { key: 'pnr', label: t('Código de reserva'), placeholder: 'ABC123' },
        ],
        event: [
            { key: 'location', label: t('Recinto / lugar'), placeholder: t('Estadio, teatro…') },
            { key: 'address', label: t('Dirección (opcional)'), placeholder: t('Calle, número, ciudad') },
            { key: 'coordinates', label: t('Ubicación exacta (opcional)'), placeholder: t('19.3029, -99.1505 o enlace de Maps') },
            { key: 'date', label: t('Fecha del evento'), type: 'date' },
            { key: 'time', label: t('Hora del evento'), type: 'time' },
            { key: 'gate', label: t('Puerta de acceso'), placeholder: '6' },
            { key: 'section', label: t('Sección'), placeholder: 'Golden Circle' },
            { key: 'row', label: t('Fila'), placeholder: 'A' },
            { key: 'seat', label: t('Asiento'), placeholder: '12' },
        ],
        transit: [
            { key: 'route', label: t('Ruta / línea'), placeholder: t('Madrid → Barcelona') },
            { key: 'location', label: t('Estación / parada'), placeholder: t('Estación Central, andén 3') },
            { key: 'address', label: t('Dirección (opcional)'), placeholder: t('Calle, número, ciudad') },
            { key: 'coordinates', label: t('Ubicación exacta (opcional)'), placeholder: t('19.3029, -99.1505 o enlace de Maps') },
            { key: 'date', label: t('Fecha de viaje'), type: 'date' },
            { key: 'time', label: t('Hora de salida'), type: 'time' },
            { key: 'boarding_start', label: t('Inicio de abordaje'), type: 'time' },
            { key: 'boarding_end', label: t('Cierre de abordaje'), type: 'time' },
            { key: 'terminal', label: t('Terminal'), placeholder: t('Terminal de autobuses, estación…') },
            { key: 'platform', label: t('Andén / vía'), placeholder: '3' },
            { key: 'carriage', label: t('Coche / vagón'), placeholder: '5' },
            { key: 'seat', label: t('Asiento / plaza'), placeholder: '12B' },
        ],
        id: [
            { key: 'number', label: t('Número del documento'), placeholder: 'ABC123456' },
            { key: 'birth', label: t('Fecha de nacimiento'), type: 'date' },
            { key: 'expires', label: t('Válido hasta'), type: 'date' },
        ],
        membership: [
            { key: 'number', label: t('Número de socio'), placeholder: 'MP-000123' },
            { key: 'level', label: t('Nivel / categoría'), placeholder: t('Oro, Platino…') },
            { key: 'expires', label: t('Válido hasta'), type: 'date' },
        ],
    };
}

function commonFields() {
    return [
        { key: 'title', label: t('Título del pase'), placeholder: t('Ej. Vuelo IB 6401'), wide: true },
        { key: 'issuer', label: t('Entidad emisora'), placeholder: t('Aerolínea, club, transporte…') },
        { key: 'holder', label: t('Titular'), placeholder: t('Nombre completo') },
        { key: 'code_value', label: t('Código o localizador (opcional)'), placeholder: t('El código que leerá el escáner') },
    ];
}

function categoryTheme(category) {
    return {
        flight: 'midnight',
        event: 'violet',
        transit: 'forest',
        id: 'obsidian',
        membership: 'amber',
    }[category] || 'midnight';
}

function drawImage(bitmap, maxEdge = MAX_IMAGE_EDGE) {
    const scale = Math.min(1, maxEdge / Math.max(bitmap.width, bitmap.height));
    const width = Math.max(1, Math.round(bitmap.width * scale));
    const height = Math.max(1, Math.round(bitmap.height * scale));
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, width, height);
    context.drawImage(bitmap, 0, 0, width, height);
    return canvas;
}

async function getOcrWorker() {
    if (!ocrWorkerPromise) {
        ocrWorkerPromise = import('tesseract.js').then(async ({ createWorker }) => {
            return createWorker(['spa', 'eng'], 1, {
                workerPath: '/vendor/tesseract/worker.min.js',
                corePath: '/vendor/tesseract',
                langPath: '/vendor/tesseract/lang',
                gzip: false,
                logger: (message) => ocrLogger?.(message),
            });
        }).catch((error) => {
            ocrWorkerPromise = null;
            throw error;
        });
    }

    return ocrWorkerPromise;
}

function mapWasmFormat(format) {
    if (format === 'PDF417') {
        return 'pdf417';
    }
    if (format === 'Aztec') {
        return 'aztec';
    }
    if (format === 'Code128') {
        return 'code128';
    }
    return 'qr';
}

async function loadZXingScript() {
    if (window.ZXingWASM) {
        return window.ZXingWASM;
    }

    if (!zxingScriptPromise) {
        zxingScriptPromise = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = '/vendor/zxing/zxing_full.iife.js';
            script.async = true;
            script.onload = () => (window.ZXingWASM ? resolve(window.ZXingWASM) : reject(new Error('ZXing no disponible')));
            script.onerror = () => reject(new Error('No se pudo cargar ZXing'));
            document.head.appendChild(script);
        }).catch((error) => {
            zxingScriptPromise = null;
            throw error;
        });
    }

    return zxingScriptPromise;
}

async function readWithZXing(input) {
    const zxing = await loadZXingScript();

    if (!zxingModulePromise) {
        zxingModulePromise = Promise.resolve(zxing.prepareZXingModule({
            overrides: {
                locateFile: (path) => (path.endsWith('.wasm') ? '/vendor/zxing/zxing_full.wasm' : path),
            },
        })).catch((error) => {
            zxingModulePromise = null;
            throw error;
        });
    }

    await zxingModulePromise;
    const results = await zxing.readBarcodes(input);

    return results.find((result) => result.text && result.format !== 'None') || null;
}

class PassGenerator {
    constructor(options) {
        this.options = options;
        this.state = this.emptyState();
        this.ocrBusy = false;
        this.previewTimer = null;
        this.cameraStream = null;
        this.fieldElements = new Map();
        this.bind();
    }

    emptyState() {
        return {
            category: 'flight',
            title: '',
            issuer: '',
            holder: '',
            fields: {},
            notes: [],
            theme: 'midnight',
            themeColor: THEME_COLORS.midnight,
            themeTouched: false,
            codeType: 'qr',
            decodedPayload: null,
            codeSource: 'generated',
            logo: null,
            appearance: normalizeAppearance(null),
            file: null,
            fileName: '',
            baseMetadata: {},
            itemId: null,
            version: null,
            blobId: null,
            options: {
                presentation: true,
                share: true,
                reminder: true,
                attach: false,
            },
            categoryTouched: false,
            confidence: 0,
            pdfPage: 1,
            isNew: true,
        };
    }

    bind() {
        this.root = document.getElementById('pass-generator-app');
        if (!this.root) {
            return;
        }

        this.dropzone = document.getElementById('generator-dropzone');
        this.fileInput = document.getElementById('generator-file');
        this.fileInfo = document.getElementById('generator-file-info');
        this.fileName = document.getElementById('generator-file-name');
        this.pdfPageWrap = document.getElementById('generator-pdf-page-wrap');
        this.pdfPageInput = document.getElementById('generator-pdf-page');
        this.progressWrap = document.getElementById('generator-ocr-progress-wrap');
        this.progressBar = document.getElementById('generator-ocr-progress');
        this.progressPercent = document.getElementById('generator-ocr-percent');
        this.ocrStatus = document.getElementById('generator-ocr-status');
        this.confidence = document.getElementById('generator-confidence');
        this.reextract = document.getElementById('generator-reextract');
        this.categories = document.getElementById('generator-categories');
        this.themes = document.getElementById('generator-themes');
        this.colorInput = document.getElementById('generator-color');
        this.colorValue = document.getElementById('generator-color-value');
        this.codeType = document.getElementById('generator-code-type');
        this.logoInput = document.getElementById('generator-logo');
        this.logoPreview = document.getElementById('generator-logo-preview');
        this.logoClear = document.getElementById('generator-logo-clear');
        this.textures = document.getElementById('generator-textures');
        this.textureControls = document.getElementById('generator-texture-controls');
        this.textureSize = document.getElementById('generator-texture-size');
        this.textureRepeat = document.getElementById('generator-texture-repeat');
        this.textureOpacity = document.getElementById('generator-texture-opacity');
        this.textureOpacityValue = document.getElementById('generator-texture-opacity-value');
        this.backgroundInput = document.getElementById('generator-background-image');
        this.backgroundPreview = document.getElementById('generator-background-preview');
        this.backgroundClear = document.getElementById('generator-background-clear');
        this.backgroundControls = document.getElementById('generator-background-controls');
        this.backgroundSize = document.getElementById('generator-background-size');
        this.backgroundRepeat = document.getElementById('generator-background-repeat');
        this.backgroundShade = document.getElementById('generator-background-shade');
        this.backgroundShadeValue = document.getElementById('generator-background-shade-value');
        this.appearanceReset = document.getElementById('generator-appearance-reset');
        this.fields = document.getElementById('generator-fields');
        this.noteList = document.getElementById('generator-note-list');
        this.noteInput = document.getElementById('generator-note-input');
        this.preview = document.getElementById('generator-preview');
        this.status = document.getElementById('generator-status');
        this.error = document.getElementById('generator-error');
        this.cameraButton = document.getElementById('generator-camera');
        this.cameraModal = document.getElementById('generator-camera-modal');
        this.cameraVideo = document.getElementById('generator-camera-video');
        this.cameraError = document.getElementById('generator-camera-error');
        this.cameraCapture = document.getElementById('generator-camera-capture');
        this.saveButton = document.getElementById('generator-save');
        this.saveTop = document.getElementById('generator-save-top');
        this.shareButton = document.getElementById('generator-share');
        this.copyButton = document.getElementById('generator-copy');
        this.templatesToggle = document.getElementById('generator-templates-toggle');
        this.templatesMenu = document.getElementById('generator-templates-menu');

        this.categories?.addEventListener('click', (event) => {
            const button = event.target.closest('[data-generator-category]');
            if (!button) {
                return;
            }
            this.setCategory(button.dataset.generatorCategory, true);
        });

        this.themes?.addEventListener('click', (event) => {
            const button = event.target.closest('[data-pass-theme]');
            if (!button) {
                return;
            }
            this.state.theme = button.dataset.passTheme;
            this.state.themeColor = THEME_COLORS[button.dataset.passTheme] || this.state.themeColor;
            this.state.themeTouched = true;
            if (this.colorInput) {
                this.colorInput.value = this.state.themeColor;
            }
            this.renderThemes();
            this.renderPreview();
        });

        this.colorInput?.addEventListener('input', () => {
            this.state.theme = 'custom';
            this.state.themeColor = this.colorInput.value;
            this.state.themeTouched = true;
            this.renderThemes();
            this.renderPreview();
        });

        this.codeType?.addEventListener('change', () => {
            this.state.codeType = this.codeType.value;
            this.renderPreview();
        });

        this.fields?.addEventListener('input', (event) => {
            const input = event.target.closest('[data-field]');
            if (!input) {
                return;
            }
            this.updateField(input.dataset.field, input.value);
            this.renderPreview();
        });

        this.noteInput?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                this.addNote();
            }
        });
        document.getElementById('generator-note-add')?.addEventListener('click', () => this.addNote());

        this.fileInput?.addEventListener('change', () => {
            const file = this.fileInput.files?.[0];
            if (file) {
                this.handleFile(file);
            }
        });

        for (const eventName of ['dragover', 'dragenter']) {
            this.dropzone?.addEventListener(eventName, (event) => {
                event.preventDefault();
                this.dropzone.classList.add('border-primary', 'bg-surface-container');
            });
        }
        for (const eventName of ['dragleave', 'drop']) {
            this.dropzone?.addEventListener(eventName, (event) => {
                event.preventDefault();
                this.dropzone.classList.remove('border-primary', 'bg-surface-container');
            });
        }
        this.dropzone?.addEventListener('drop', (event) => {
            const file = event.dataTransfer?.files?.[0];
            if (file) {
                this.handleFile(file);
            }
        });

        document.getElementById('generator-file-clear')?.addEventListener('click', () => this.clearFile());
        this.reextract?.addEventListener('click', () => this.extractCurrentFile());

        this.logoInput?.addEventListener('change', () => {
            const file = this.logoInput.files?.[0];
            if (file) {
                this.handleLogo(file);
            }
        });
        this.logoClear?.addEventListener('click', () => {
            this.state.logo = null;
            this.renderLogo();
        });

        this.textures?.addEventListener('click', (event) => {
            const button = event.target.closest('[data-pass-texture]');
            if (!button) {
                return;
            }
            this.state.appearance.texture = button.dataset.passTexture;
            this.renderAppearance();
            this.renderPreview();
        });

        this.textureSize?.addEventListener('change', () => {
            this.state.appearance.textureSize = this.textureSize.value;
            this.renderPreview();
        });

        this.textureRepeat?.addEventListener('change', () => {
            this.state.appearance.textureRepeat = this.textureRepeat.value;
            this.renderPreview();
        });

        this.textureOpacity?.addEventListener('input', () => {
            this.state.appearance.textureOpacity = Number(this.textureOpacity.value);
            if (this.textureOpacityValue) {
                this.textureOpacityValue.textContent = `${this.state.appearance.textureOpacity}%`;
            }
            this.renderPreview();
        });

        this.backgroundInput?.addEventListener('change', () => {
            const file = this.backgroundInput.files?.[0];
            if (file) {
                this.handleBackgroundImage(file);
            }
        });

        this.backgroundClear?.addEventListener('click', () => {
            this.state.appearance.image = null;
            this.renderAppearance();
            this.renderPreview();
        });

        this.backgroundSize?.addEventListener('change', () => {
            this.state.appearance.imageSize = this.backgroundSize.value;
            this.renderPreview();
        });

        this.backgroundRepeat?.addEventListener('change', () => {
            this.state.appearance.imageRepeat = this.backgroundRepeat.value;
            this.renderPreview();
        });

        this.backgroundShade?.addEventListener('input', () => {
            this.state.appearance.imageShade = Number(this.backgroundShade.value);
            if (this.backgroundShadeValue) {
                this.backgroundShadeValue.textContent = `${this.state.appearance.imageShade}%`;
            }
            this.renderPreview();
        });

        this.appearanceReset?.addEventListener('click', () => {
            this.state.appearance = normalizeAppearance(null);
            this.renderAppearance();
            this.renderPreview();
        });

        document.querySelectorAll('[data-generator-template]').forEach((button) => {
            button.addEventListener('click', () => {
                this.closeTemplates();
                this.setCategory(button.dataset.generatorTemplate, true);
            });
        });
        this.templatesToggle?.addEventListener('click', () => {
            if (!this.templatesMenu) {
                return;
            }
            const open = this.templatesMenu.hidden;
            this.templatesMenu.hidden = !open;
            this.templatesToggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        });
        document.addEventListener('click', (event) => {
            if (!this.templatesMenu || this.templatesMenu.hidden) {
                return;
            }
            if (!event.target.closest('#generator-templates-menu') && !event.target.closest('#generator-templates-toggle')) {
                this.closeTemplates();
            }
        });

        this.pdfPageInput?.addEventListener('change', () => {
            const value = Number.parseInt(this.pdfPageInput.value, 10);
            this.state.pdfPage = Number.isFinite(value) && value > 0 ? value : 1;
            if (this.state.file && (this.state.file.type === 'application/pdf' || /\.pdf$/i.test(this.state.file.name))) {
                this.extractCurrentFile();
            }
        });

        this.cameraButton?.addEventListener('click', () => this.openCamera());
        document.getElementById('generator-camera-close')?.addEventListener('click', () => this.closeCamera());
        document.getElementById('generator-camera-cancel')?.addEventListener('click', () => this.closeCamera());
        this.cameraCapture?.addEventListener('click', () => this.captureCamera());
        this.cameraModal?.addEventListener('close', () => this.stopCamera());

        this.saveButton?.addEventListener('click', () => this.save());
        this.saveTop?.addEventListener('click', () => this.save());
        this.copyButton?.addEventListener('click', () => this.copyCode());
        this.shareButton?.addEventListener('click', () => this.share());
        document.getElementById('generator-back')?.addEventListener('click', () => this.options.switchTab('pases'));

        for (const [id, key] of [
            ['generator-opt-presentation', 'presentation'],
            ['generator-opt-share', 'share'],
            ['generator-opt-reminder', 'reminder'],
            ['generator-opt-attach', 'attach'],
        ]) {
            document.getElementById(id)?.addEventListener('change', (event) => {
                this.state.options[key] = event.currentTarget.checked;
                this.renderPreview();
            });
        }
    }

    open(category = 'flight', item = null, pass = null) {
        if (!this.root) {
            return;
        }

        this.state = this.emptyState();
        this.state.category = pass?.category || category || 'flight';
        this.state.categoryTouched = Boolean(item);
        this.state.itemId = item?.id || null;
        this.state.isNew = !item;
        this.state.version = item?.version || null;
        this.state.blobId = item?.blob_id || null;
        this.state.baseMetadata = item?.metadata ? { ...item.metadata } : {};

        if (pass) {
            this.state.title = pass.title || '';
            this.state.issuer = pass.issuer || '';
            this.state.holder = pass.holder || pass.fields?.holder || pass.fields?.passenger || '';
            this.state.fields = { ...(pass.fields || {}) };
            if (!this.state.fields.boarding_start && this.state.fields.boarding) {
                this.state.fields.boarding_start = this.state.fields.boarding;
            }
            this.state.notes = Array.isArray(pass.notes) ? [...pass.notes] : [];
            this.state.theme = pass.theme || categoryTheme(this.state.category);
            this.state.themeColor = pass.themeColor || THEME_COLORS[this.state.theme] || THEME_COLORS.midnight;
            this.state.themeTouched = Boolean(pass.theme || pass.themeColor);
            this.state.codeType = pass.code?.format || 'qr';
            this.state.decodedPayload = pass.code?.source === 'decoded' ? pass.code.payload : null;
            this.state.codeSource = pass.code?.source || 'generated';
            this.state.logo = pass.logo || null;
            this.state.appearance = normalizeAppearance(pass.appearance);
            this.state.options = {
                presentation: pass.options?.presentation !== false,
                share: pass.options?.share !== false,
                reminder: pass.options?.reminder !== false,
                attach: false,
            };
            this.state.confidence = pass.confidence || 0;
        } else {
            this.state.theme = categoryTheme(this.state.category);
            this.state.themeColor = THEME_COLORS[this.state.theme] || THEME_COLORS.midnight;
        }

        this.options.switchTab('generador');
        this.setStatus('');
        this.hideError();
        this.renderAll();
        this.root.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    reset() {
        this.state = this.emptyState();
        this.clearFile();
        this.renderAll();
        this.setStatus('');
        this.hideError();
    }

    setCategory(category, touched = false) {
        this.state.category = category;
        if (touched) {
            this.state.categoryTouched = true;
        }
        if (!this.state.themeTouched) {
            this.state.theme = categoryTheme(category);
            this.state.themeColor = THEME_COLORS[this.state.theme] || this.state.themeColor;
        }
        this.renderAll();
    }

    updateField(key, value) {
        if (key === 'title') {
            this.state.title = value;
        } else if (key === 'issuer') {
            this.state.issuer = value;
        } else if (key === 'holder') {
            this.state.holder = value;
        } else {
            this.state.fields[key] = value;
        }
    }

    fieldValue(key) {
        if (key === 'title') {
            return this.state.title;
        }
        if (key === 'issuer') {
            return this.state.issuer;
        }
        if (key === 'holder') {
            return this.state.holder;
        }
        return this.state.fields[key] || '';
    }

    renderAll() {
        this.renderCategories();
        this.renderThemes();
        this.renderAppearance();
        this.renderFields();
        this.renderNotes();
        this.renderLogo();
        this.renderOptions();
        this.renderCodeType();
        this.renderPreview();
        this.renderFileState();
    }

    renderCategories() {
        this.categories?.querySelectorAll('[data-generator-category]').forEach((button) => {
            const active = button.dataset.generatorCategory === this.state.category;
            button.classList.toggle('border-primary', active);
            button.classList.toggle('bg-primary-fixed', active);
            button.classList.toggle('text-primary', active);
            button.setAttribute('aria-pressed', active ? 'true' : 'false');
        });
    }

    renderThemes() {
        this.themes?.querySelectorAll('[data-pass-theme]').forEach((button) => {
            const active = button.dataset.passTheme === this.state.theme;
            button.classList.toggle('ring-primary', active);
            button.querySelector('span')?.style.setProperty('opacity', active ? '1' : '0');
            button.setAttribute('aria-pressed', active ? 'true' : 'false');
        });
        if (this.colorInput) {
            this.colorInput.value = this.state.themeColor || THEME_COLORS.midnight;
        }
        if (this.colorValue) {
            this.colorValue.textContent = (this.state.themeColor || THEME_COLORS.midnight).toUpperCase();
        }
        this.textures?.style.setProperty('--texture-swatch-color', this.state.themeColor);
    }

    renderAppearance() {
        this.renderTextures();

        const appearance = this.state.appearance;
        if (this.textureControls) {
            this.textureControls.hidden = appearance.texture === 'none';
        }
        if (this.textureSize) {
            this.textureSize.value = appearance.textureSize;
        }
        if (this.textureRepeat) {
            this.textureRepeat.value = appearance.textureRepeat;
        }
        if (this.textureOpacity) {
            this.textureOpacity.value = String(appearance.textureOpacity);
        }
        if (this.textureOpacityValue) {
            this.textureOpacityValue.textContent = `${appearance.textureOpacity}%`;
        }

        const hasImage = Boolean(appearance.image);
        if (this.backgroundControls) {
            this.backgroundControls.hidden = !hasImage;
        }
        if (this.backgroundClear) {
            this.backgroundClear.hidden = !hasImage;
        }
        if (this.backgroundPreview) {
            if (hasImage) {
                this.backgroundPreview.src = appearance.image;
                this.backgroundPreview.hidden = false;
            } else {
                this.backgroundPreview.removeAttribute('src');
                this.backgroundPreview.hidden = true;
            }
        }
        if (this.backgroundSize) {
            this.backgroundSize.value = appearance.imageSize;
        }
        if (this.backgroundRepeat) {
            this.backgroundRepeat.value = appearance.imageRepeat;
        }
        if (this.backgroundShade) {
            this.backgroundShade.value = String(appearance.imageShade);
        }
        if (this.backgroundShadeValue) {
            this.backgroundShadeValue.textContent = `${appearance.imageShade}%`;
        }
    }

    renderTextures() {
        if (!this.textures) {
            return;
        }

        this.textures.replaceChildren();
        this.textures.style.setProperty('--texture-swatch-color', this.state.themeColor);

        const options = [
            { id: 'none', label: t('Sin textura'), texture: null, size: null },
            ...Object.entries(PASS_TEXTURES).map(([id, texture]) => ({
                id,
                label: t(texture.label),
                texture,
                size: texture.size,
            })),
        ];

        for (const option of options) {
            const active = option.id === this.state.appearance.texture;
            const button = el('button', 'mp-texture-option');
            button.type = 'button';
            button.dataset.passTexture = option.id;
            button.classList.toggle('is-active', active);
            button.setAttribute('aria-pressed', active ? 'true' : 'false');

            const preview = el('span', 'mp-texture-option__preview');
            preview.setAttribute('aria-hidden', 'true');
            if (option.texture) {
                const factor = Math.min(1, TEXTURE_SIZE_FACTORS.sm);
                preview.style.backgroundImage = textureImage(option.texture, 100);
                preview.style.backgroundSize = `${Math.max(8, Math.round(option.size[0] * factor))}px ${Math.max(8, Math.round(option.size[1] * factor))}px`;
                preview.style.backgroundRepeat = 'repeat';
            } else {
                preview.classList.add('mp-texture-option__preview--none');
                preview.appendChild(icon('format_color_reset', 'text-[19px]'));
            }

            button.append(preview, el('span', 'mp-texture-option__label', option.label));
            if (active) {
                const check = el('span', 'mp-texture-option__check');
                check.appendChild(icon('check', 'text-[13px]'));
                button.appendChild(check);
            }
            this.textures.appendChild(button);
        }
    }

    renderFields() {
        if (!this.fields) {
            return;
        }

        this.fields.replaceChildren();
        this.fieldElements.clear();
        const schema = [...commonFields(), ...(fieldSchemas()[this.state.category] || [])];

        for (const field of schema) {
            const wrapper = el('div', field.wide ? 'sm:col-span-2 mp-generator-field' : 'mp-generator-field');
            const id = `generator-field-${field.key}`;
            const label = el('label', null, field.label);
            label.setAttribute('for', id);
            const input = document.createElement('input');
            input.id = id;
            input.className = field.type === 'date' || field.type === 'time'
                ? 'mp-input mp-input-sm mp-date-input'
                : 'mp-input mp-input-sm';
            input.dataset.field = field.key;
            input.type = field.type || 'text';
            if (field.placeholder) {
                input.placeholder = field.placeholder;
            }
            if (field.type === 'date' || field.type === 'time') {
                input.addEventListener('click', () => {
                    try {
                        input.showPicker?.();
                    } catch {
                        // The native picker still opens with the field controls.
                    }
                });
            }
            input.value = this.fieldValue(field.key);
            wrapper.append(label, input);
            this.fields.appendChild(wrapper);
            this.fieldElements.set(field.key, input);
        }
    }

    renderNotes() {
        if (!this.noteList) {
            return;
        }
        this.noteList.replaceChildren();
        this.state.notes.forEach((note, index) => {
            const chip = el('span', 'mp-note-chip', note);
            const remove = el('button');
            remove.type = 'button';
            remove.setAttribute('aria-label', tf('Quitar :note', { note }));
            remove.appendChild(icon('close', 'text-[15px]'));
            remove.addEventListener('click', () => {
                this.state.notes.splice(index, 1);
                this.renderNotes();
                this.renderPreview();
            });
            chip.appendChild(remove);
            this.noteList.appendChild(chip);
        });
    }

    renderLogo() {
        if (!this.logoPreview || !this.logoClear) {
            return;
        }
        if (this.state.logo) {
            this.logoPreview.src = this.state.logo;
            this.logoPreview.hidden = false;
            this.logoClear.hidden = false;
        } else {
            this.logoPreview.removeAttribute('src');
            this.logoPreview.hidden = true;
            this.logoClear.hidden = true;
        }
    }

    renderOptions() {
        for (const [id, key] of [
            ['generator-opt-presentation', 'presentation'],
            ['generator-opt-share', 'share'],
            ['generator-opt-reminder', 'reminder'],
            ['generator-opt-attach', 'attach'],
        ]) {
            const input = document.getElementById(id);
            if (input) {
                input.checked = Boolean(this.state.options[key]);
            }
        }
    }

    renderCodeType() {
        if (this.codeType) {
            this.codeType.value = this.state.codeType;
        }
    }

    renderFileState() {
        if (!this.fileInfo) {
            return;
        }
        const hasFile = Boolean(this.state.file);
        this.fileInfo.hidden = !hasFile;
        this.fileInfo.classList.toggle('hidden', !hasFile);
        this.fileInfo.classList.toggle('flex', hasFile);
        if (this.fileName) {
            this.fileName.textContent = this.state.fileName;
        }
        if (this.reextract) {
            this.reextract.disabled = !hasFile || this.ocrBusy;
        }
        const isPdf = hasFile && (
            this.state.file.type === 'application/pdf' ||
            /\.pdf$/i.test(this.state.file.name)
        );
        if (this.pdfPageWrap) {
            this.pdfPageWrap.hidden = !isPdf;
        }
        if (this.pdfPageInput) {
            this.pdfPageInput.value = String(this.state.pdfPage || 1);
        }
    }

    renderPreview() {
        if (!this.preview) {
            return;
        }
        clearTimeout(this.previewTimer);
        const pass = this.buildPassObject();
        const item = {
            id: this.state.itemId || 'preview-00000000',
            type: this.itemType(),
            version: this.state.version,
            expires_at: this.expiresAt(),
            metadata: {
                title: pass.title,
                type: this.itemType(),
                pass,
            },
        };
        const card = createPassCardElement(item, { interactive: false, preview: true });
        const codeBlock = el('div', 'mp-pass-card__preview-code');
        const codeWrap = el('div', 'flex min-h-[150px] items-center justify-center');
        codeBlock.appendChild(codeWrap);
        codeBlock.appendChild(el('p', 'mt-2 text-center font-mono text-[11px] text-on-surface-variant', this.codeDescription(pass)));
        card.appendChild(codeBlock);
        this.preview.replaceChildren(card);
        const payload = passPayload(item, pass);
        this.previewTimer = setTimeout(() => {
            renderCodeInto(codeWrap, payload, this.state.codeType, { scale: 3, includeText: false });
        }, 180);
    }

    codeDescription(pass) {
        const format = {
            qr: 'QR',
            pdf417: 'PDF417',
            aztec: 'Aztec',
            code128: 'Code 128',
        }[this.state.codeType] || 'QR';
        return this.state.decodedPayload
            ? tf(':format · leído del boleto original', { format })
            : tf(':format · generado por My Pass', { format });
    }

    buildPassObject() {
        const schema = fieldSchemas()[this.state.category] || [];
        const allowed = new Set([
            ...schema.map((field) => field.key),
            'title',
            'issuer',
            'holder',
            'code_value',
        ]);
        const fields = {};
        for (const [key, value] of Object.entries(this.state.fields)) {
            if (allowed.has(key) && value !== null && value !== undefined && value !== '') {
                fields[key] = value;
            }
        }

        if (this.state.holder) {
            fields[this.state.category === 'flight' ? 'passenger' : 'holder'] = this.state.holder;
        }

        const title = this.effectiveTitle();
        const base = {
            category: this.state.category,
            title,
            issuer: this.state.issuer || fields.carrier || '',
            holder: this.state.holder || fields.passenger || fields.holder || '',
            fields,
            notes: [...this.state.notes],
            theme: this.state.theme,
            themeColor: this.state.themeColor,
            logo: this.state.logo,
            appearance: normalizeAppearance(this.state.appearance),
            options: { ...this.state.options },
            confidence: this.state.confidence,
        };

        let payload = this.state.decodedPayload || this.state.fields.code_value || '';
        if (!payload) {
            payload = passPayload({ id: this.state.itemId || 'preview-00000000' }, { ...base, code: null });
        }

        return {
            ...base,
            code: {
                payload,
                format: this.state.codeType,
                source: this.state.decodedPayload ? 'decoded' : 'generated',
            },
        };
    }

    effectiveTitle() {
        if (this.state.title.trim()) {
            return this.state.title.trim();
        }

        const fields = this.state.fields;
        if (this.state.category === 'flight') {
            const route = [fields.origin, fields.destination].filter(Boolean).join(' → ');
            return [route, fields.flight].filter(Boolean).join(' · ') || t('Nuevo pase de vuelo');
        }
        if (this.state.category === 'event') {
            return fields.location || t('Nuevo pase de evento');
        }
        if (this.state.category === 'transit') {
            return fields.route || t('Nuevo pase de transporte');
        }
        if (this.state.category === 'membership') {
            return this.state.issuer || t('Nueva membresía');
        }
        return this.state.issuer || t('Nuevo documento de identidad');
    }

    itemType() {
        return this.state.category === 'id' || this.state.category === 'membership' ? 'card' : 'ticket';
    }

    expiresAt() {
        if (!this.state.options.reminder) {
            return null;
        }
        return this.state.fields.expires || this.state.fields.date || null;
    }

    addNote() {
        const value = this.noteInput?.value.trim();
        if (!value) {
            return;
        }
        this.state.notes.push(value);
        if (this.noteInput) {
            this.noteInput.value = '';
        }
        this.renderNotes();
        this.renderPreview();
    }

    closeTemplates() {
        if (this.templatesMenu) {
            this.templatesMenu.hidden = true;
        }
        this.templatesToggle?.setAttribute('aria-expanded', 'false');
    }

    setStatus(message) {
        if (this.status) {
            this.status.textContent = message;
        }
    }

    showError(message) {
        if (!this.error) {
            return;
        }
        this.error.textContent = message;
        this.error.hidden = false;
    }

    hideError() {
        if (this.error) {
            this.error.hidden = true;
        }
    }

    setBusy(busy) {
        this.ocrBusy = busy;
        for (const button of [this.saveButton, this.saveTop]) {
            if (button) {
                button.disabled = busy;
            }
        }
        if (this.reextract) {
            this.reextract.disabled = busy || !this.state.file;
        }
    }

    setProgress(percent, message) {
        if (this.progressWrap) {
            this.progressWrap.hidden = false;
        }
        if (this.progressBar) {
            this.progressBar.style.width = `${Math.max(0, Math.min(100, percent))}%`;
        }
        if (this.progressPercent) {
            this.progressPercent.textContent = `${Math.round(percent)}%`;
        }
        if (this.ocrStatus && message) {
            this.ocrStatus.textContent = message;
        }
    }

    hideProgress() {
        if (this.progressWrap) {
            this.progressWrap.hidden = true;
        }
        if (this.progressBar) {
            this.progressBar.style.width = '0%';
        }
    }

    async openCamera() {
        if (!navigator.mediaDevices?.getUserMedia) {
            this.showError(t('Este navegador no permite usar la cámara.'));
            return;
        }

        this.hideError();
        if (this.cameraError) {
            this.cameraError.hidden = true;
        }
        this.cameraModal?.showModal();

        try {
            this.cameraStream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: { ideal: 'environment' } },
                audio: false,
            });
            if (this.cameraVideo) {
                this.cameraVideo.srcObject = this.cameraStream;
            }
        } catch {
            this.stopCamera();
            if (this.cameraError) {
                this.cameraError.textContent = t('No se pudo abrir la cámara. Revisa el permiso del navegador.');
                this.cameraError.hidden = false;
            }
        }
    }

    stopCamera() {
        for (const track of this.cameraStream?.getTracks() ?? []) {
            track.stop();
        }
        this.cameraStream = null;
        if (this.cameraVideo) {
            this.cameraVideo.srcObject = null;
        }
    }

    closeCamera() {
        this.stopCamera();
        if (this.cameraModal?.open) {
            this.cameraModal.close();
        }
    }

    async captureCamera() {
        const video = this.cameraVideo;
        if (!video || !video.videoWidth || !video.videoHeight) {
            if (this.cameraError) {
                this.cameraError.textContent = t('La cámara todavía no está lista.');
                this.cameraError.hidden = false;
            }
            return;
        }

        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        canvas.getContext('2d').drawImage(video, 0, 0);
        const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.92));

        if (!blob) {
            if (this.cameraError) {
                this.cameraError.textContent = t('No se pudo capturar la foto.');
                this.cameraError.hidden = false;
            }
            return;
        }

        this.closeCamera();
        this.handleFile(new File([blob], `camara-${Date.now()}.jpg`, { type: 'image/jpeg' }));
    }

    handleFile(file) {
        this.hideError();
        this.setStatus('');

        if (file.size > MAX_FILE_SIZE) {
            this.showError(t('El archivo supera los 25 MB.'));
            return;
        }

        const isPdf = file.type === 'application/pdf' || /\.pdf$/i.test(file.name);
        const isImage = file.type.startsWith('image/') || /\.(jpe?g|png|webp|gif)$/i.test(file.name);
        if (!isPdf && !isImage) {
            this.showError(t('Usa un PDF, JPG, PNG o WEBP.'));
            return;
        }

        this.state.file = file;
        this.state.fileName = file.name;
        this.renderFileState();
        this.extractCurrentFile();
    }

    clearFile() {
        this.state.file = null;
        this.state.fileName = '';
        this.state.pdfPage = 1;
        if (this.fileInput) {
            this.fileInput.value = '';
        }
        this.renderFileState();
        this.hideProgress();
        if (this.confidence) {
            this.confidence.textContent = '';
        }
    }

    async extractCurrentFile() {
        if (!this.state.file || this.ocrBusy) {
            return;
        }

        this.hideError();
        this.setBusy(true);
        this.setStatus('');
        this.setProgress(2, t('Preparando archivo…'));

        try {
            let text = '';
            let barcode = null;
            const isPdf = this.state.file.type === 'application/pdf' || /\.pdf$/i.test(this.state.file.name);

            if (isPdf) {
                const result = await this.analyzePdf(this.state.file, this.state.pdfPage);
                text = result.text;
                barcode = result.barcode;
            } else {
                const result = await this.analyzeImage(this.state.file);
                text = result.text;
                barcode = result.barcode;
            }

            this.applyAnalysis(text, barcode);
        } catch (error) {
            this.hideProgress();
            this.showError(error.message || t('No se pudo leer el archivo. Puedes escribir los datos a mano.'));
        } finally {
            this.setBusy(false);
            this.renderFileState();
        }
    }

    async analyzeImage(file) {
        const bitmap = await createImageBitmap(file);
        const canvas = drawImage(bitmap);
        bitmap.close?.();
        this.setProgress(8, t('Buscando código de barras…'));
        const barcode = await this.readBarcode(canvas, file);
        const text = await this.recognize(canvas);
        return { text, barcode };
    }

    async analyzePdf(file, selectedPage = 1) {
        const pdfjs = await import('pdfjs-dist');
        const workerModule = await import('pdfjs-dist/build/pdf.worker.min.mjs?url');
        pdfjs.GlobalWorkerOptions.workerSrc = workerModule.default;

        const loadingTask = pdfjs.getDocument({ data: await file.arrayBuffer() });
        const doc = await loadingTask.promise;
        const pages = Math.min(doc.numPages, 2);
        const ocrPageNumber = Math.min(Math.max(selectedPage, 1), doc.numPages);
        let text = '';
        let firstCanvas = null;

        for (let pageNumber = 1; pageNumber <= pages; pageNumber += 1) {
            const page = await doc.getPage(pageNumber);
            const content = await page.getTextContent();
            const pageText = content.items
                .map((item) => `${item.str ?? ''}${item.hasEOL ? '\n' : ' '}`)
                .join('')
                .replace(/\s+\n/g, '\n')
                .trim();
            text += `${pageText}\n`;

            if (!firstCanvas && pageNumber === ocrPageNumber) {
                const viewport = page.getViewport({ scale: 2.2 });
                firstCanvas = document.createElement('canvas');
                firstCanvas.width = Math.min(2400, Math.round(viewport.width));
                firstCanvas.height = Math.round(viewport.height * (firstCanvas.width / viewport.width));
                const context = firstCanvas.getContext('2d');
                context.fillStyle = '#ffffff';
                context.fillRect(0, 0, firstCanvas.width, firstCanvas.height);
                await page.render({
                    canvasContext: context,
                    canvas: firstCanvas,
                    viewport: page.getViewport({ scale: firstCanvas.width / viewport.width }),
                }).promise;
            }
        }

        if (!firstCanvas) {
            const page = await doc.getPage(ocrPageNumber);
            const viewport = page.getViewport({ scale: 2.2 });
            firstCanvas = document.createElement('canvas');
            firstCanvas.width = Math.min(2400, Math.round(viewport.width));
            firstCanvas.height = Math.round(viewport.height * (firstCanvas.width / viewport.width));
            const context = firstCanvas.getContext('2d');
            context.fillStyle = '#ffffff';
            context.fillRect(0, 0, firstCanvas.width, firstCanvas.height);
            await page.render({
                canvasContext: context,
                canvas: firstCanvas,
                viewport: page.getViewport({ scale: firstCanvas.width / viewport.width }),
            }).promise;
        }

        await loadingTask.destroy();

        let barcode = null;
        if (firstCanvas) {
            this.setProgress(10, t('Buscando código de barras…'));
            barcode = await this.readBarcode(firstCanvas);
        }

        text = text.trim();
        if (text.length >= 40) {
            this.setProgress(100, t('Texto extraído del PDF.'));
            return { text, barcode };
        }

        const ocrText = await this.recognize(firstCanvas);
        return { text: ocrText, barcode };
    }

    async readBarcode(canvas, file = null) {
        try {
            const context = canvas.getContext('2d', { willReadFrequently: true });
            const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
            let result = await readWithZXing(imageData);
            if (!result?.text && file) {
                result = await readWithZXing(file);
            }
            if (result?.text) {
                return this.barcodeResult(result.text, mapWasmFormat(result.format));
            }
        } catch {
            // Si ZXing no puede cargarse, el lector QR ligero sigue disponible.
        }

        const payloadFromQr = await this.readQr(canvas);
        if (payloadFromQr) {
            return this.barcodeResult(payloadFromQr, 'qr');
        }

        return null;
    }

    async readQr(canvas) {
        try {
            const context = canvas.getContext('2d', { willReadFrequently: true });
            const image = context.getImageData(0, 0, canvas.width, canvas.height);
            const { default: jsQR } = await import('jsqr');
            const result = jsQR(image.data, image.width, image.height, {
                inversionAttempts: 'attemptBoth',
            });
            return result?.data || null;
        } catch {
            return null;
        }
    }

    barcodeResult(payload, format) {
        const bcbp = parseBcbp(payload);
        if (bcbp) {
            return { ...bcbp, code: { ...bcbp.code, format } };
        }

        return {
            category: null,
            title: null,
            issuer: null,
            holder: null,
            fields: {},
            notes: [],
            confidence: 0,
            code: { payload, format, source: 'decoded' },
        };
    }

    async recognize(canvas) {
        this.setProgress(15, t('Cargando OCR…'));
        ocrLogger = (message) => {
            if (message.status === 'recognizing text') {
                this.setProgress(20 + (message.progress || 0) * 75, t('Leyendo texto…'));
            } else if (message.status === 'loading language traineddata') {
                this.setProgress(10 + (message.progress || 0) * 10, t('Cargando idioma…'));
            } else if (message.status === 'initializing api') {
                this.setProgress(18, t('Iniciando OCR…'));
            }
        };

        try {
            const worker = await getOcrWorker();
            const { data } = await worker.recognize(canvas);
            this.setProgress(100, t('Lectura completada.'));
            return data.text || '';
        } finally {
            ocrLogger = null;
        }
    }

    applyAnalysis(text, barcode) {
        const parsed = text ? parsePassText(text, { category: this.state.category }) : null;
        if (barcode?.category && !this.state.categoryTouched) {
            this.state.category = barcode.category;
        } else if (parsed?.category && !this.state.categoryTouched) {
            this.state.category = parsed.category;
        }

        const barcodeFields = barcode?.fields || {};
        const parsedFields = parsed?.fields || {};
        this.state.fields = { ...parsedFields, ...barcodeFields };
        this.state.holder = barcode?.holder || parsed?.holder || this.state.fields.passenger || this.state.fields.holder || '';
        this.state.issuer = barcode?.issuer || parsed?.issuer || this.state.issuer;
        this.state.title = barcode?.title || parsed?.title || this.state.title;
        this.state.confidence = barcode ? 0.98 : (parsed?.confidence || 0);

        if (barcode?.code?.payload) {
            this.state.decodedPayload = barcode.code.payload;
            this.state.codeType = barcode.code.format || this.state.codeType;
            this.state.codeSource = 'decoded';
        }

        if (!this.state.themeTouched) {
            this.state.theme = categoryTheme(this.state.category);
            this.state.themeColor = THEME_COLORS[this.state.theme] || this.state.themeColor;
        }

        this.renderAll();
        this.hideProgress();
        if (this.confidence) {
            this.confidence.textContent = this.state.confidence > 0
                ? tf('Confianza de lectura: :percent%', { percent: Math.round(this.state.confidence * 100) })
                : '';
        }
        this.setStatus(t('Datos detectados. Revísalos antes de guardar.'));
        this.options.toast(t('Lectura completada. Revisa los campos.'));
    }

    async handleBackgroundImage(file) {
        this.hideError();

        if (!file.type.startsWith('image/')) {
            this.showError(t('Usa una imagen JPG, PNG o WEBP.'));
            return;
        }
        if (file.size > MAX_BACKGROUND_FILE_SIZE) {
            this.showError(t('La imagen de fondo supera los 15 MB.'));
            return;
        }

        try {
            const bitmap = await createImageBitmap(file);
            const dataUrl = await this.encodeBackgroundImage(bitmap);
            bitmap.close?.();
            this.state.appearance.image = dataUrl;
            this.renderAppearance();
            this.renderPreview();
        } catch (error) {
            this.showError(error.message || t('No se pudo leer la imagen de fondo.'));
        } finally {
            if (this.backgroundInput) {
                this.backgroundInput.value = '';
            }
        }
    }

    async encodeBackgroundImage(bitmap) {
        const attempts = [
            { edge: MAX_BACKGROUND_EDGE, quality: 0.82 },
            { edge: 1100, quality: 0.74 },
        ];

        for (const attempt of attempts) {
            const scale = Math.min(1, attempt.edge / Math.max(bitmap.width, bitmap.height));
            const width = Math.max(1, Math.round(bitmap.width * scale));
            const height = Math.max(1, Math.round(bitmap.height * scale));
            const canvas = document.createElement('canvas');
            canvas.width = width;
            canvas.height = height;
            const context = canvas.getContext('2d');
            context.clearRect(0, 0, width, height);
            context.drawImage(bitmap, 0, 0, width, height);

            let dataUrl = canvas.toDataURL('image/webp', attempt.quality);
            if (!dataUrl.startsWith('data:image/webp')) {
                dataUrl = canvas.toDataURL('image/jpeg', attempt.quality);
            }
            if (dataUrl.length <= MAX_BACKGROUND_DATA_URL) {
                return dataUrl;
            }
        }

        throw new Error(t('La imagen es demasiado grande. Elige una más pequeña.'));
    }

    async handleLogo(file) {
        if (!file.type.startsWith('image/')) {
            this.showError(t('El logo debe ser una imagen.'));
            return;
        }
        if (file.size > 3 * 1024 * 1024) {
            this.showError(t('El logo supera los 3 MB.'));
            return;
        }

        try {
            const bitmap = await createImageBitmap(file);
            const scale = Math.min(1, MAX_LOGO_EDGE / Math.max(bitmap.width, bitmap.height));
            const canvas = document.createElement('canvas');
            canvas.width = Math.max(1, Math.round(bitmap.width * scale));
            canvas.height = Math.max(1, Math.round(bitmap.height * scale));
            const context = canvas.getContext('2d');
            context.clearRect(0, 0, canvas.width, canvas.height);
            context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
            bitmap.close?.();
            this.state.logo = canvas.toDataURL('image/png');
            this.renderLogo();
            this.renderPreview();
        } catch {
            this.showError(t('No se pudo leer el logo.'));
        }
    }

    async save() {
        if (this.ocrBusy) {
            return;
        }

        this.hideError();
        this.setStatus(t('Guardando pase…'));
        for (const button of [this.saveButton, this.saveTop]) {
            if (button) {
                button.disabled = true;
            }
        }

        try {
            if (this.state.isNew && !this.state.itemId) {
                this.state.itemId = newUuid();
            }

            const pass = this.buildPassObject();
            const expiresAt = this.expiresAt();
            const attach = this.state.options.attach && this.state.file;
            const metadata = {
                ...this.state.baseMetadata,
                title: pass.title,
                type: this.itemType(),
                expires_at: expiresAt,
                pass,
            };
            if (attach) {
                metadata.original_name = this.state.file.name;
                metadata.mime = this.state.file.type || 'application/octet-stream';
                metadata.size = this.state.file.size;
            }

            const saved = await this.options.savePass({
                itemId: this.state.isNew ? null : this.state.itemId,
                newId: this.state.isNew ? this.state.itemId : null,
                version: this.state.version,
                type: this.itemType(),
                metadata,
                file: attach ? this.state.file : null,
                blobId: this.state.blobId,
            });

            this.state.itemId = saved.id;
            this.state.isNew = false;
            this.state.version = saved.version;
            this.state.blobId = saved.blob_id || null;
            if (attach) {
                this.state.baseMetadata = {
                    ...this.state.baseMetadata,
                    original_name: this.state.file.name,
                    mime: this.state.file.type || 'application/octet-stream',
                    size: this.state.file.size,
                };
            }
            this.setStatus(t('Pase guardado en tu bóveda.'));
            if (this.shareButton) {
                this.shareButton.disabled = !this.state.options.share || !this.options.canShare();
            }
            this.options.toast(t('Pase guardado.'));
        } catch (error) {
            this.showError(error.message || t('No se pudo guardar el pase.'));
            this.setStatus('');
        } finally {
            for (const button of [this.saveButton, this.saveTop]) {
                if (button) {
                    button.disabled = false;
                }
            }
        }
    }

    async copyCode() {
        const pass = this.buildPassObject();
        const payload = passPayload({ id: this.state.itemId || 'preview' }, pass);
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

    share() {
        if (!this.state.itemId) {
            this.showError(t('Guarda el pase antes de compartirlo.'));
            return;
        }
        const item = this.options.getItems().find((candidate) => candidate.id === this.state.itemId);
        if (!item) {
            this.showError(t('No se encontró el pase guardado.'));
            return;
        }
        this.options.openShare(item);
    }
}

export { PassGenerator };
