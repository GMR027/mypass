import {
    copySensitive,
    formatDate,
    generatePassword,
    generateTotp,
    newUuid,
    parseTotp,
    passwordStrength,
    safeExternalUrl,
    splitList,
    totpSecondsLeft,
} from './vault-secrets';
import { bannerDataUrl, isImageDataUrl, logoDataUrl } from './media';
import { centerCardInView, createCompactCard, createFlipCard, setDeckFlipped, syncDeckHeight } from './card-deck';

const t = (key) => (window.mypassT ? window.mypassT(key) : key);
const tf = (key, values) => (window.mypassTF ? window.mypassTF(key, values) : key);
const locale = () => (window.mypassLocale === 'en' ? 'en-US' : 'es-MX');

const CATEGORY_ICONS = {
    login: 'key',
    bank: 'account_balance',
    email: 'mail',
    social: 'public',
    work: 'work',
    wifi: 'wifi',
    server: 'dns',
    other: 'vpn_key',
};

const CATEGORY_LABELS = {
    login: 'Acceso general',
    bank: 'Banco y finanzas',
    email: 'Correo electrónico',
    social: 'Red social',
    work: 'Trabajo',
    wifi: 'Wi-Fi',
    server: 'Servidor o API',
    other: 'Otra',
};

const CUSTOM_TYPE_ICONS = [
    { key: 'lock', symbol: 'lock', label: 'Candado' },
    { key: 'mail', symbol: 'mail', label: 'Correo' },
    { key: 'person', symbol: 'person', label: 'Persona' },
    { key: 'phone', symbol: 'phone', label: 'Teléfono' },
    { key: 'shopping', symbol: 'shopping_cart', label: 'Compras' },
    { key: 'star', symbol: 'star', label: 'Favorito' },
    { key: 'play', symbol: 'play_arrow', label: 'Streaming' },
    { key: 'build', symbol: 'build', label: 'Herramientas' },
    { key: 'place', symbol: 'place', label: 'Lugar' },
    { key: 'home', symbol: 'home', label: 'Hogar' },
    { key: 'settings', symbol: 'settings', label: 'Ajustes' },
    { key: 'search', symbol: 'search', label: 'Búsqueda' },
];

const CUSTOM_TYPE_ICON_SYMBOLS = Object.fromEntries(
    CUSTOM_TYPE_ICONS.map((entry) => [entry.key, entry.symbol]),
);

const BANNER_COLORS = [
    '#1B4332',
    '#0C2340',
    '#4C1D95',
    '#7F1D1D',
    '#9A3412',
    '#0E7490',
    '#374151',
    '#B45309',
];

function stringValue(value) {
    return typeof value === 'string' && value.trim() ? value.trim() : '';
}

function validColor(value) {
    return /^#[0-9a-f]{6}$/i.test(String(value || '')) ? String(value) : '';
}

function customTypeKey(label) {
    return label.trim().toLowerCase();
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
    return el('span', ['material-symbols-outlined', className].filter(Boolean).join(' '), name);
}

function button(label, className, iconName = null) {
    const node = el('button', className);
    node.type = 'button';
    if (iconName) {
        node.appendChild(icon(iconName, 'text-[18px]'));
    }
    node.appendChild(document.createTextNode(label));
    return node;
}

function today() {
    return new Date().toISOString().slice(0, 10);
}

function parseTotpUri(value) {
    const config = parseTotp(value);
    if (!config) {
        return null;
    }

    const text = String(value || '').trim();
    if (!text.toLowerCase().startsWith('otpauth://')) {
        return { config, issuer: '', account: '' };
    }

    try {
        const url = new URL(text);
        const label = decodeURIComponent(url.pathname.replace(/^\//, ''));
        const labelIssuer = label.includes(':') ? label.split(':')[0].trim() : label.trim();
        const labelAccount = label.includes(':') ? label.split(':').slice(1).join(':').trim() : '';
        const issuer = (url.searchParams.get('issuer') || '').trim() || labelIssuer;
        return {
            config,
            issuer,
            account: labelAccount || (issuer ? '' : label.trim()),
        };
    } catch {
        return { config, issuer: '', account: '' };
    }
}

async function decodeQrImage(file) {
    const { default: jsQR } = await import('jsqr');
    const bitmap = await createImageBitmap(file);
    const scale = Math.min(1, 1200 / Math.max(bitmap.width, bitmap.height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(bitmap.width * scale));
    canvas.height = Math.max(1, Math.round(bitmap.height * scale));
    const context = canvas.getContext('2d', { willReadFrequently: true });
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    bitmap.close?.();
    const image = context.getImageData(0, 0, canvas.width, canvas.height);
    const result = jsQR(image.data, image.width, image.height, { inversionAttempts: 'attemptBoth' });
    return result?.data || null;
}

function totpToUri(config) {
    if (!config?.secret) {
        return '';
    }

    const params = new URLSearchParams({
        secret: config.secret,
        algorithm: config.algorithm || 'SHA1',
        digits: String(config.digits || 6),
        period: String(config.period || 30),
    });

    return `otpauth://totp/My%20Pass?${params.toString()}`;
}

export function isCredentialItem(item) {
    return item?.type === 'credential' || Boolean(item?.metadata?.credential);
}

export function credentialFromItem(item) {
    const raw = item?.metadata?.credential && typeof item.metadata.credential === 'object'
        ? item.metadata.credential
        : {};
    const totp = raw.totp && typeof raw.totp === 'object'
        ? raw.totp
        : parseTotp(raw.totp_secret || '');
    const builtIn = CATEGORY_ICONS[raw.category] ? raw.category : null;
    const customLabel = stringValue(raw.category_label);
    const customIcon = stringValue(raw.category_icon);
    const group = stringValue(raw.group);
    const logo = isImageDataUrl(raw.logo) ? raw.logo : '';
    const appearance = raw.appearance && typeof raw.appearance === 'object' ? raw.appearance : {};
    const appearanceMode = ['banner', 'background'].includes(appearance.mode)
        ? appearance.mode
        : 'none';
    const appearanceColor = validColor(appearance.color);
    const appearanceImage = isImageDataUrl(appearance.image) ? appearance.image : '';

    return {
        category: builtIn || (customLabel ? 'custom' : 'login'),
        categoryLabel: customLabel,
        categoryIcon: customIcon,
        group,
        logo,
        appearance: {
            mode: appearanceMode,
            color: appearanceColor,
            image: appearanceImage,
        },
        username: typeof raw.username === 'string' ? raw.username : '',
        password: typeof raw.password === 'string' ? raw.password : '',
        url: typeof raw.url === 'string' ? raw.url : '',
        notes: typeof raw.notes === 'string' ? raw.notes : '',
        tags: Array.isArray(raw.tags) ? raw.tags.filter(Boolean).map(String) : [],
        favorite: raw.favorite === true,
        passwordChangedAt: typeof raw.password_changed_at === 'string' ? raw.password_changed_at : '',
        totp,
        customFields: Array.isArray(raw.custom_fields) ? raw.custom_fields : [],
    };
}

function typeLabelFor(parsed) {
    if (parsed.category === 'custom') {
        return parsed.categoryLabel || t('Tipo personalizado');
    }

    return t(CATEGORY_LABELS[parsed.category] || 'Acceso general');
}

function typeIconSymbol(parsed) {
    if (parsed.category === 'custom') {
        return CUSTOM_TYPE_ICON_SYMBOLS[parsed.categoryIcon] || 'key';
    }

    return CATEGORY_ICONS[parsed.category] || 'key';
}

export class CredentialsApp {
    constructor(options) {
        this.options = options;
        this.items = [];
        this.filter = 'all';
        this.search = '';
        this.groupFilter = 'all';
        this.selectedCredentialId = null;
        this.flippedCredentialId = null;
        this.editing = null;
        this.baseMetadata = {};
        this.existingBlobId = null;
        this.clearAttachment = false;
        this.fileLabel = '';
        this.customTypes = new Map();
        this.customTypeIcon = 'key';
        this.previousCategory = 'login';
        this.logo = null;
        this.bannerMode = 'none';
        this.bannerColor = BANNER_COLORS[0];
        this.bannerImage = null;
        this.totpConfigs = new Map();
        this.revealTimers = new Map();
        this.totpTimer = null;
        this.totpBusy = false;
        this.bind();
    }

    bind() {
        this.panel = document.getElementById('panel-credentials');
        if (!this.panel) {
            return;
        }

        this.list = document.getElementById('credential-list');
        this.empty = document.getElementById('credentials-empty');
        this.emptyTitle = document.getElementById('credentials-empty-title');
        this.emptyCopy = document.getElementById('credentials-empty-copy');
        this.searchInput = document.getElementById('credential-search');
        this.addButton = document.getElementById('credential-add-open');
        this.modal = document.getElementById('credential-modal');
        this.form = document.getElementById('credential-form');
        this.modalTitle = document.getElementById('credential-modal-title');
        this.error = document.getElementById('credential-error');
        this.idInput = document.getElementById('credential-item-id');
        this.versionInput = document.getElementById('credential-item-version');
        this.blobInput = document.getElementById('credential-blob-id');
        this.titleInput = document.getElementById('credential-title');
        this.categoryInput = document.getElementById('credential-category');
        this.groupInput = document.getElementById('credential-group');
        this.groupOptions = document.getElementById('credential-group-options');
        this.groupFilters = document.getElementById('credential-group-filters');
        this.customTypePanel = document.getElementById('credential-custom-type');
        this.customTypeLabel = document.getElementById('credential-custom-label');
        this.customTypeIcons = document.getElementById('credential-custom-icons');
        this.customTypeSave = document.getElementById('credential-custom-save');
        this.customTypeCancel = document.getElementById('credential-custom-cancel');
        this.logoInput = document.getElementById('credential-logo');
        this.logoPreviewBox = document.getElementById('credential-logo-preview-box');
        this.logoPreview = document.getElementById('credential-logo-preview');
        this.logoClear = document.getElementById('credential-logo-clear');
        this.bannerModeInput = document.getElementById('credential-appearance-mode');
        this.bannerColorInput = document.getElementById('credential-banner-color');
        this.bannerPresets = document.getElementById('credential-banner-presets');
        this.bannerImageInput = document.getElementById('credential-banner-image');
        this.bannerPreviewBox = document.getElementById('credential-banner-preview-box');
        this.bannerPreview = document.getElementById('credential-banner-preview');
        this.bannerClear = document.getElementById('credential-banner-clear');
        this.usernameInput = document.getElementById('credential-username');
        this.passwordInput = document.getElementById('credential-password');
        this.passwordToggle = document.getElementById('credential-password-toggle');
        this.passwordStrength = document.getElementById('credential-password-strength');
        this.generateButton = document.getElementById('credential-generate');
        this.urlInput = document.getElementById('credential-url');
        this.totpInput = document.getElementById('credential-totp');
        this.totpScanButton = document.getElementById('credential-totp-scan');
        this.totpQrInput = document.getElementById('credential-totp-qr');
        this.totpScanStatus = document.getElementById('credential-totp-scan-status');
        this.tagsInput = document.getElementById('credential-tags');
        this.notesInput = document.getElementById('credential-notes');
        this.fileInput = document.getElementById('credential-file');
        this.fileCurrent = document.getElementById('credential-file-current');
        this.fileName = document.getElementById('credential-file-name');
        this.fileClear = document.getElementById('credential-file-clear');
        this.saveButton = document.getElementById('credential-save');
        this.closeButton = document.getElementById('credential-close');
        this.cancelButton = document.getElementById('credential-cancel');

        this.addButton?.addEventListener('click', () => this.openNew());
        this.closeButton?.addEventListener('click', () => this.close());
        this.cancelButton?.addEventListener('click', () => this.close());
        this.modal?.addEventListener('close', () => this.resetForm());

        this.searchInput?.addEventListener('input', () => {
            this.search = this.searchInput.value;
            this.render();
        });

        this.panel.querySelectorAll('[data-credential-filter]').forEach((filterButton) => {
            filterButton.addEventListener('click', () => {
                this.filter = filterButton.dataset.credentialFilter;
                this.render();
            });
        });

        this.groupFilters?.addEventListener('click', (event) => {
            const button = event.target.closest('[data-credential-group-filter]');
            if (!button) {
                return;
            }
            this.groupFilter = button.dataset.credentialGroupFilter;
            this.render();
        });

        this.categoryInput?.addEventListener('change', () => {
            if (this.categoryInput.value === '__new__') {
                this.openCustomTypePanel();
            } else {
                this.previousCategory = this.categoryInput.value;
                this.closeCustomTypePanel();
            }
        });

        this.customTypeSave?.addEventListener('click', () => this.createCustomType());
        this.customTypeCancel?.addEventListener('click', () => {
            this.closeCustomTypePanel();
            this.categoryInput.value = this.previousCategory;
        });

        this.logoInput?.addEventListener('change', async () => {
            const file = this.logoInput.files?.[0];
            if (!file) {
                return;
            }
            try {
                this.logo = await logoDataUrl(file);
                this.renderAppearance();
            } catch (error) {
                this.showError(t(error.message || 'No se pudo procesar la imagen.'));
            }
        });
        this.logoClear?.addEventListener('click', () => {
            this.logo = null;
            this.logoInput.value = '';
            this.renderAppearance();
        });

        this.bannerModeInput?.addEventListener('change', () => {
            this.bannerMode = this.bannerModeInput.value;
            this.renderAppearance();
        });
        this.bannerColorInput?.addEventListener('input', () => {
            this.bannerColor = this.bannerColorInput.value;
            if (this.bannerMode === 'none') {
                this.bannerMode = 'banner';
                this.bannerModeInput.value = 'banner';
            }
            this.renderAppearance();
        });
        this.bannerPresets?.addEventListener('click', (event) => {
            const button = event.target.closest('[data-banner-color]');
            if (!button) {
                return;
            }
            this.bannerColor = button.dataset.bannerColor;
            if (this.bannerMode === 'none') {
                this.bannerMode = 'banner';
                this.bannerModeInput.value = 'banner';
            }
            this.renderAppearance();
        });
        this.bannerImageInput?.addEventListener('change', async () => {
            const file = this.bannerImageInput.files?.[0];
            if (!file) {
                return;
            }
            try {
                this.bannerImage = await bannerDataUrl(file);
                if (this.bannerMode === 'none') {
                    this.bannerMode = 'banner';
                    this.bannerModeInput.value = 'banner';
                }
                this.renderAppearance();
            } catch (error) {
                this.showError(t(error.message || 'No se pudo procesar la imagen.'));
            }
        });
        this.bannerClear?.addEventListener('click', () => {
            this.bannerImage = null;
            this.bannerImageInput.value = '';
            this.renderAppearance();
        });

        this.form?.addEventListener('submit', (event) => this.save(event));
        this.generateButton?.addEventListener('click', () => {
            const password = generatePassword({ length: 24 });
            this.passwordInput.value = password;
            this.passwordInput.type = 'text';
            this.passwordToggle?.setAttribute('aria-pressed', 'true');
            this.updatePasswordStrength();
            this.passwordInput.focus();
            this.passwordInput.select();
        });

        this.totpScanButton?.addEventListener('click', () => {
            this.totpQrInput?.click();
        });
        this.totpQrInput?.addEventListener('change', () => this.scanTotpQr());

        this.passwordInput?.addEventListener('input', () => this.updatePasswordStrength());
        this.passwordToggle?.addEventListener('click', () => {
            const show = this.passwordInput.type === 'password';
            this.passwordInput.type = show ? 'text' : 'password';
            this.passwordToggle.setAttribute('aria-pressed', show ? 'true' : 'false');
            const iconNode = this.passwordToggle.querySelector('.material-symbols-outlined');
            if (iconNode) {
                iconNode.textContent = show ? 'visibility_off' : 'visibility';
            }
        });

        this.fileInput?.addEventListener('change', () => {
            if (this.fileInput.files?.length) {
                this.clearAttachment = false;
            }
            this.renderFileState();
        });

        this.fileClear?.addEventListener('click', () => {
            this.clearAttachment = true;
            this.existingBlobId = null;
            this.fileLabel = '';
            if (this.fileInput) {
                this.fileInput.value = '';
            }
            this.renderFileState();
        });

        this.list?.addEventListener('click', (event) => this.handleListClick(event));
        this.startTotpTimer();
    }

    setItems(items) {
        this.items = (Array.isArray(items) ? items : []).filter(isCredentialItem);
        this.render();
    }

    reset() {
        this.items = [];
        this.search = '';
        this.filter = 'all';
        this.groupFilter = 'all';
        this.selectedCredentialId = null;
        this.flippedCredentialId = null;
        this.customTypes = new Map();
        this.totpConfigs.clear();
        this.revealTimers.forEach((timer) => clearTimeout(timer));
        this.revealTimers.clear();
        this.close();
        this.render();
    }

    refreshCategoryOptions() {
        if (!this.categoryInput) {
            return;
        }

        const derived = new Map();
        for (const item of this.items) {
            const parsed = credentialFromItem(item);
            if (parsed.category === 'custom' && parsed.categoryLabel) {
                derived.set(customTypeKey(parsed.categoryLabel), {
                    label: parsed.categoryLabel,
                    icon: parsed.categoryIcon || 'key',
                });
            }
        }
        for (const [key, type] of this.customTypes) {
            if (!derived.has(key)) {
                derived.set(key, type);
            }
        }
        this.customTypes = derived;

        this.categoryInput.querySelectorAll('option[data-custom-type]').forEach((option) => option.remove());
        const createOption = this.categoryInput.querySelector('option[value="__new__"]');
        for (const [key, type] of this.customTypes) {
            const option = document.createElement('option');
            option.value = `custom:${key}`;
            option.textContent = type.label;
            option.dataset.customType = 'true';
            this.categoryInput.insertBefore(option, createOption);
        }
    }

    refreshGroupOptions() {
        if (!this.groupOptions) {
            return;
        }

        const groups = new Set();
        for (const item of this.items) {
            const group = credentialFromItem(item).group;
            if (group) {
                groups.add(group);
            }
        }
        this.groupOptions.replaceChildren();
        for (const group of [...groups].sort((a, b) => a.localeCompare(b, locale()))) {
            const option = document.createElement('option');
            option.value = group;
            this.groupOptions.appendChild(option);
        }
    }

    renderCustomIcons() {
        if (!this.customTypeIcons) {
            return;
        }

        this.customTypeIcons.replaceChildren();
        for (const entry of CUSTOM_TYPE_ICONS) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'flex h-11 w-11 items-center justify-center rounded-xl border transition-colors';
            button.dataset.customIcon = entry.key;
            button.setAttribute('aria-label', t(entry.label));
            button.setAttribute('aria-pressed', this.customTypeIcon === entry.key ? 'true' : 'false');
            button.classList.toggle('border-primary', this.customTypeIcon === entry.key);
            button.classList.toggle('bg-primary-fixed', this.customTypeIcon === entry.key);
            button.classList.toggle('text-primary', this.customTypeIcon === entry.key);
            button.classList.toggle('border-outline-variant/60', this.customTypeIcon !== entry.key);
            button.appendChild(icon(entry.symbol, 'text-[22px]'));
            button.addEventListener('click', () => {
                this.customTypeIcon = entry.key;
                this.renderCustomIcons();
            });
            this.customTypeIcons.appendChild(button);
        }
    }

    openCustomTypePanel() {
        if (!this.customTypePanel) {
            return;
        }

        this.customTypeLabel.value = '';
        this.customTypeIcon = 'key';
        this.renderCustomIcons();
        this.customTypePanel.hidden = false;
        this.customTypeLabel.focus();
    }

    closeCustomTypePanel() {
        if (this.customTypePanel) {
            this.customTypePanel.hidden = true;
        }
    }

    createCustomType() {
        const label = (this.customTypeLabel?.value || '').trim();
        if (!label) {
            this.showError(t('Escribe un nombre para el tipo de cuenta.'));
            this.customTypeLabel?.focus();
            return;
        }

        const key = customTypeKey(label);
        this.customTypes.set(key, { label, icon: this.customTypeIcon });
        this.refreshCategoryOptions();
        this.categoryInput.value = `custom:${key}`;
        this.closeCustomTypePanel();
        this.hideError();
    }

    renderAppearance() {
        if (this.bannerColorInput) {
            this.bannerColorInput.value = validColor(this.bannerColor) || BANNER_COLORS[0];
        }
        if (this.bannerModeInput) {
            this.bannerModeInput.value = this.bannerMode;
        }

        this.bannerPresets?.querySelectorAll('[data-banner-color]').forEach((button) => {
            const selected = button.dataset.bannerColor.toLowerCase() === this.bannerColor.toLowerCase();
            button.setAttribute('aria-pressed', selected ? 'true' : 'false');
            button.classList.toggle('ring-2', selected);
            button.classList.toggle('ring-primary', selected);
            button.classList.toggle('ring-offset-2', selected);
        });

        if (this.logoPreviewBox && this.logoPreview) {
            this.logoPreviewBox.hidden = !this.logo;
            if (this.logo) {
                this.logoPreview.src = this.logo;
            } else {
                this.logoPreview.removeAttribute('src');
            }
        }

        if (this.bannerPreviewBox && this.bannerPreview) {
            this.bannerPreviewBox.hidden = !this.bannerImage;
            if (this.bannerImage) {
                this.bannerPreview.src = this.bannerImage;
            } else {
                this.bannerPreview.removeAttribute('src');
            }
        }
    }

    renderBannerPresets() {
        if (!this.bannerPresets) {
            return;
        }

        this.bannerPresets.replaceChildren();
        for (const color of BANNER_COLORS) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'h-9 w-9 rounded-full border border-black/10 shadow-sm transition-transform hover:scale-110';
            button.style.backgroundColor = color;
            button.dataset.bannerColor = color;
            button.setAttribute('aria-label', tf('Usar color :color', { color }));
            button.addEventListener('click', () => {
                this.bannerColor = color;
                if (this.bannerMode === 'none') {
                    this.bannerMode = 'banner';
                    this.bannerModeInput.value = 'banner';
                }
                this.renderAppearance();
            });
            this.bannerPresets.appendChild(button);
        }
        this.renderAppearance();
    }

    openNew() {
        if (!this.modal) {
            return;
        }

        this.editing = null;
        this.baseMetadata = {};
        this.existingBlobId = null;
        this.clearAttachment = false;
        this.fileLabel = '';
        this.logo = null;
        this.bannerMode = 'none';
        this.bannerColor = BANNER_COLORS[0];
        this.bannerImage = null;
        this.modalTitle.textContent = t('Nueva credencial');
        this.form.reset();
        this.idInput.value = '';
        this.versionInput.value = '';
        this.blobInput.value = '';
        this.passwordInput.type = 'password';
        this.passwordToggle?.setAttribute('aria-pressed', 'false');
        this.groupInput.value = '';
        this.categoryInput.value = 'login';
        this.previousCategory = 'login';
        this.closeCustomTypePanel();
        this.refreshCategoryOptions();
        this.refreshGroupOptions();
        this.renderBannerPresets();
        this.renderAppearance();
        this.updatePasswordStrength();
        this.renderFileState();
        this.hideError();
        this.modal.showModal();
        this.titleInput?.focus();
    }

    openEdit(item) {
        if (!this.modal) {
            return;
        }

        const parsed = credentialFromItem(item);
        this.editing = item;
        window.dispatchEvent(new CustomEvent('mypass:item-open', { detail: { id: item.id } }));
        this.baseMetadata = item.metadata ? { ...item.metadata } : {};
        this.existingBlobId = item.blob_id || null;
        this.clearAttachment = false;
        this.fileLabel = item.blob_id ? (item.metadata?.original_name || '') : '';
        this.logo = parsed.logo || null;
        this.bannerMode = parsed.appearance.mode;
        this.bannerColor = parsed.appearance.color || BANNER_COLORS[0];
        this.bannerImage = parsed.appearance.image || null;
        this.modalTitle.textContent = t('Editar credencial');
        this.form.reset();
        this.idInput.value = item.id;
        this.versionInput.value = String(item.version || '');
        this.blobInput.value = item.blob_id || '';
        this.titleInput.value = item.metadata?.title || '';
        this.refreshCategoryOptions();
        if (parsed.category === 'custom' && parsed.categoryLabel) {
            this.categoryInput.value = `custom:${customTypeKey(parsed.categoryLabel)}`;
        } else {
            this.categoryInput.value = parsed.category;
        }
        this.previousCategory = this.categoryInput.value;
        this.usernameInput.value = parsed.username;
        this.passwordInput.value = parsed.password;
        this.passwordInput.type = 'password';
        this.passwordToggle?.setAttribute('aria-pressed', 'false');
        this.urlInput.value = parsed.url;
        this.totpInput.value = parsed.totp ? totpToUri(parsed.totp) : '';
        this.tagsInput.value = parsed.tags.join(', ');
        this.notesInput.value = parsed.notes;
        this.groupInput.value = parsed.group;
        this.closeCustomTypePanel();
        this.refreshGroupOptions();
        this.renderBannerPresets();
        this.renderAppearance();
        this.updatePasswordStrength();
        this.renderFileState();
        this.hideError();
        this.modal.showModal();
        this.titleInput?.focus();
    }

    close() {
        if (this.modal?.open) {
            this.modal.close();
        }
    }

    resetForm() {
        this.editing = null;
        this.baseMetadata = {};
        this.existingBlobId = null;
        this.clearAttachment = false;
        this.fileLabel = '';
        this.logo = null;
        this.bannerMode = 'none';
        this.bannerColor = BANNER_COLORS[0];
        this.bannerImage = null;
        this.closeCustomTypePanel();
        this.form?.reset();
        this.hideError();
        if (this.totpScanStatus) {
            this.totpScanStatus.textContent = '';
        }
        this.renderFileState();
        this.renderAppearance();
    }

    renderFileState() {
        if (!this.fileCurrent) {
            return;
        }

        const file = this.fileInput?.files?.[0];
        const label = file?.name || (!this.clearAttachment ? this.fileLabel : '');
        this.fileCurrent.hidden = !label;
        if (this.fileName) {
            this.fileName.textContent = label;
        }
    }

    updatePasswordStrength() {
        if (!this.passwordStrength) {
            return;
        }

        const strength = passwordStrength(this.passwordInput.value);
        const tones = {
            strong: 'text-primary',
            medium: 'text-secondary',
            weak: 'text-error',
            neutral: 'text-on-surface-variant',
        };
        this.passwordStrength.className = `mp-hint mt-1 ${tones[strength.tone] || tones.neutral}`;
        this.passwordStrength.textContent = this.passwordInput.value
            ? tf('Seguridad: :label', { label: t(strength.label) })
            : '';
    }

    async scanTotpQr() {
        const file = this.totpQrInput?.files?.[0];
        if (!file) {
            return;
        }

        if (this.totpScanStatus) {
            this.totpScanStatus.textContent = t('Leyendo el código QR…');
        }

        try {
            const payload = await decodeQrImage(file);
            if (!payload) {
                throw new Error(t('No se encontró un código QR en la imagen.'));
            }
            const parsed = parseTotpUri(payload);
            if (!parsed) {
                throw new Error(t('Ese QR no contiene una configuración TOTP válida.'));
            }

            this.totpInput.value = payload;
            if (!this.titleInput.value.trim() && parsed.issuer) {
                this.titleInput.value = parsed.issuer;
            }
            if (!this.usernameInput.value.trim() && parsed.account) {
                this.usernameInput.value = parsed.account;
            }
            if (this.totpScanStatus) {
                this.totpScanStatus.textContent = t('Código TOTP detectado.');
            }
            this.hideError();
        } catch (error) {
            if (this.totpScanStatus) {
                this.totpScanStatus.textContent = '';
            }
            this.showError(error.message || t('No se pudo leer el código QR.'));
        } finally {
            if (this.totpQrInput) {
                this.totpQrInput.value = '';
            }
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

    async save(event) {
        event.preventDefault();
        this.hideError();

        const title = this.titleInput.value.trim();
        if (!title) {
            this.showError(t('Escribe un nombre para la cuenta.'));
            this.titleInput.focus();
            return;
        }

        const totpText = this.totpInput.value.trim();
        const totp = totpText ? parseTotp(totpText) : null;
        if (totpText && !totp) {
            this.showError(t('El secreto TOTP no es válido. Pega el secreto Base32 o la URL otpauth.'));
            this.totpInput.focus();
            return;
        }

        const previous = this.editing ? credentialFromItem(this.editing) : null;
        const password = this.passwordInput.value;
        const passwordChangedAt = previous?.password === password
            ? (previous.passwordChangedAt || (password ? today() : ''))
            : (password ? today() : '');

        const file = this.fileInput?.files?.[0] || null;
        if (file && file.size > 25 * 1024 * 1024) {
            this.showError(t('El archivo supera los 25 MB.'));
            return;
        }

        const selectedCategory = this.categoryInput.value;
        if (selectedCategory === '__new__') {
            this.showError(t('Escribe el nombre del tipo y pulsa “Usar este tipo”.'));
            return;
        }

        let category = selectedCategory;
        let categoryLabel = '';
        let categoryIcon = '';
        if (selectedCategory.startsWith('custom:')) {
            const customType = this.customTypes.get(selectedCategory.slice(7));
            if (!customType) {
                this.showError(t('Elige un tipo de cuenta válido.'));
                return;
            }
            category = 'custom';
            categoryLabel = customType.label;
            categoryIcon = customType.icon || 'key';
        }

        const credential = {
            ...(this.baseMetadata.credential || {}),
            category,
            username: this.usernameInput.value.trim(),
            password,
            url: this.urlInput.value.trim(),
            notes: this.notesInput.value.trim(),
            tags: splitList(this.tagsInput.value),
            favorite: previous?.favorite === true,
            password_changed_at: passwordChangedAt,
            totp: totp || null,
        };

        if (category === 'custom') {
            credential.category_label = categoryLabel;
            credential.category_icon = categoryIcon;
        } else {
            delete credential.category_label;
            delete credential.category_icon;
        }

        const group = (this.groupInput?.value || '').trim();
        if (group) {
            credential.group = group;
        } else {
            delete credential.group;
        }

        if (this.logo) {
            credential.logo = this.logo;
        } else {
            delete credential.logo;
        }

        if (this.bannerMode !== 'none' && (this.bannerColor || this.bannerImage)) {
            credential.appearance = {
                mode: this.bannerMode,
                color: this.bannerColor || '',
                image: this.bannerImage || '',
            };
        } else {
            delete credential.appearance;
        }

        const metadata = {
            ...this.baseMetadata,
            title,
            type: 'credential',
            expires_at: this.baseMetadata.expires_at || null,
            credential,
        };

        if (file) {
            metadata.original_name = file.name;
            metadata.mime = file.type || 'application/octet-stream';
            metadata.size = file.size;
        } else if (this.clearAttachment) {
            delete metadata.original_name;
            delete metadata.mime;
            delete metadata.size;
        }

        this.saveButton.disabled = true;
        const original = this.saveButton.textContent;
        this.saveButton.textContent = t('Guardando…');

        try {
            await this.options.saveItem({
                itemId: this.editing?.id || null,
                newId: this.editing ? null : newUuid(),
                version: this.editing?.version || null,
                type: 'credential',
                metadata,
                file,
                blobId: file ? this.existingBlobId : (this.clearAttachment ? null : this.existingBlobId),
            });
            this.close();
            this.options.toast?.(t('Credencial guardada.'));
        } catch (error) {
            this.showError(error.message || t('No se pudo guardar la credencial.'));
        } finally {
            this.saveButton.disabled = false;
            this.saveButton.textContent = original;
        }
    }

    async toggleFavorite(item) {
        const parsed = credentialFromItem(item);
        const metadata = {
            ...(item.metadata || {}),
            title: item.metadata?.title || parsed.username || t('Credencial'),
            type: 'credential',
            credential: {
                ...(item.metadata?.credential || {}),
                favorite: !parsed.favorite,
            },
        };

        try {
            await this.options.saveItem({
                itemId: item.id,
                version: item.version,
                type: 'credential',
                metadata,
                blobId: item.blob_id || null,
            });
        } catch (error) {
            this.options.toast?.(error.message || t('No se pudo actualizar la credencial.'));
        }
    }

    handleListClick(event) {
        const targetCard = event.target.closest('[data-credential-id]');
        const action = event.target.closest('[data-credential-action]');

        if (!action) {
            if (targetCard?.classList.contains('mp-deck-card--compact')) {
                const targetItem = this.items.find((candidate) => candidate.id === targetCard.dataset.credentialId);
                if (targetItem) {
                    this.selectedCredentialId = targetItem.id;
                    this.flippedCredentialId = null;
                    this.render();
                    window.requestAnimationFrame(() => {
                        centerCardInView(this.list?.querySelector(`[data-credential-id="${CSS.escape(targetItem.id)}"]`));
                    });
                }
            }
            return;
        }

        const card = action.closest('[data-credential-id]');
        const item = this.items.find((candidate) => candidate.id === card?.dataset.credentialId);
        if (!item) {
            return;
        }

        const parsed = credentialFromItem(item);
        const actionName = action.dataset.credentialAction;

        if (actionName === 'favorite') {
            this.toggleFavorite(item);
        } else if (actionName === 'copy-username') {
            copySensitive(parsed.username, this.options.toast);
        } else if (actionName === 'copy-password') {
            copySensitive(parsed.password, this.options.toast);
        } else if (actionName === 'copy-totp') {
            const code = card.querySelector('[data-totp-code]')?.textContent?.replace(/\s+/g, '') || '';
            copySensitive(code, this.options.toast);
        } else if (actionName === 'reveal-password') {
            this.revealPassword(item, card, action);
        } else if (actionName === 'open-url') {
            const url = safeExternalUrl(parsed.url);
            if (url) {
                window.open(url, '_blank', 'noopener,noreferrer');
            } else {
                this.options.toast?.(t('La dirección web no es válida.'));
            }
        } else if (actionName === 'edit') {
            this.openEdit(item);
        } else if (actionName === 'download') {
            this.options.downloadItem?.(item, action);
        } else if (actionName === 'delete') {
            this.options.deleteItem?.(item, action);
        }
    }

    revealPassword(item, card, trigger) {
        const value = card.querySelector('[data-credential-password]');
        if (!value) {
            return;
        }

        const parsed = credentialFromItem(item);
        const visible = trigger.getAttribute('aria-pressed') === 'true';
        if (visible) {
            value.textContent = '••••••••••••';
            trigger.setAttribute('aria-pressed', 'false');
            trigger.setAttribute('aria-label', t('Mostrar contraseña'));
            const iconNode = trigger.querySelector('.material-symbols-outlined');
            if (iconNode) {
                iconNode.textContent = 'visibility';
            }
            clearTimeout(this.revealTimers.get(item.id));
            this.revealTimers.delete(item.id);
            return;
        }

        value.textContent = parsed.password || t('Sin contraseña');
        trigger.setAttribute('aria-pressed', 'true');
        trigger.setAttribute('aria-label', t('Ocultar contraseña'));
        const iconNode = trigger.querySelector('.material-symbols-outlined');
        if (iconNode) {
            iconNode.textContent = 'visibility_off';
        }
        clearTimeout(this.revealTimers.get(item.id));
        this.revealTimers.set(item.id, setTimeout(() => {
            this.revealTimers.delete(item.id);
            if (!card.isConnected) {
                return;
            }
            value.textContent = '••••••••••••';
            trigger.setAttribute('aria-pressed', 'false');
            trigger.setAttribute('aria-label', t('Mostrar contraseña'));
            const timerIcon = trigger.querySelector('.material-symbols-outlined');
            if (timerIcon) {
                timerIcon.textContent = 'visibility';
            }
        }, 30_000));
    }

    render() {
        if (!this.list) {
            return;
        }

        const query = this.search.trim().toLowerCase();
        const all = this.items.filter((item) => item.state !== 'transferred');
        const groups = [...new Set(all.map((item) => credentialFromItem(item).group).filter(Boolean))]
            .sort((a, b) => a.localeCompare(b, locale()));

        if (this.groupFilter !== 'all' && this.groupFilter !== '__none__' && !groups.includes(this.groupFilter)) {
            this.groupFilter = 'all';
        }

        this.refreshGroupOptions();
        this.renderGroupFilters(all, groups);

        const filtered = all
            .filter((item) => {
                const parsed = credentialFromItem(item);
                if (this.filter === 'favorite' && !parsed.favorite) {
                    return false;
                }
                if (this.filter === 'totp' && !parsed.totp) {
                    return false;
                }
                if (this.groupFilter === '__none__' && parsed.group) {
                    return false;
                }
                if (this.groupFilter !== 'all' && this.groupFilter !== '__none__' && parsed.group !== this.groupFilter) {
                    return false;
                }
                if (!query) {
                    return true;
                }
                return [
                    item.metadata?.title,
                    parsed.username,
                    parsed.group,
                    typeLabelFor(parsed),
                    ...parsed.tags,
                    parsed.notes,
                    ...(parsed.customFields || []).map((field) => field?.label),
                ].filter(Boolean).some((value) => String(value).toLowerCase().includes(query));
            })
            .sort((a, b) => {
                const favoriteA = credentialFromItem(a).favorite ? 1 : 0;
                const favoriteB = credentialFromItem(b).favorite ? 1 : 0;
                if (favoriteA !== favoriteB) {
                    return favoriteB - favoriteA;
                }
                return String(a.metadata?.title || '').localeCompare(String(b.metadata?.title || ''), locale());
            });

        if (this.selectedCredentialId && !filtered.some((item) => item.id === this.selectedCredentialId)) {
            this.selectedCredentialId = null;
            this.flippedCredentialId = null;
        }
        if (this.flippedCredentialId && this.flippedCredentialId !== this.selectedCredentialId) {
            this.flippedCredentialId = null;
        }

        const counts = {
            all: all.length,
            favorite: all.filter((item) => credentialFromItem(item).favorite).length,
            totp: all.filter((item) => credentialFromItem(item).totp).length,
        };
        this.panel.querySelectorAll('[data-credential-filter]').forEach((filterButton) => {
            filterButton.setAttribute('aria-pressed', filterButton.dataset.credentialFilter === this.filter ? 'true' : 'false');
        });
        this.panel.querySelectorAll('[data-credential-count]').forEach((counter) => {
            counter.textContent = counts[counter.dataset.credentialCount] ?? 0;
        });

        this.empty.hidden = filtered.length > 0;
        if (all.length === 0) {
            this.emptyTitle.textContent = t('Aún no tienes credenciales.');
            this.emptyCopy.textContent = t('Agrega tu primera cuenta y guarda su contraseña cifrada.');
        } else if (filtered.length === 0) {
            this.emptyTitle.textContent = t('No hay credenciales que coincidan.');
            this.emptyCopy.textContent = t('Prueba con otra búsqueda o cambia el filtro.');
        }

        this.list.replaceChildren();
        this.totpConfigs.clear();

        const grouped = this.filter === 'all' && this.groupFilter === 'all' && !query;
        const appendDeck = (groupItems) => {
            const deck = el('div', 'mp-deck');
            groupItems.forEach((item, index) => {
                const selected = item.id === this.selectedCredentialId;
                const card = selected ? this.renderFlipCard(item) : this.renderCompactCard(item);
                card.style.setProperty('--deck-index', String(index));
                deck.appendChild(card);
                if (selected) {
                    window.requestAnimationFrame(() => syncDeckHeight(card));
                }
            });
            return deck;
        };

        if (grouped) {
            const buckets = new Map();
            for (const item of filtered) {
                const group = credentialFromItem(item).group;
                const key = group || '';
                if (!buckets.has(key)) {
                    buckets.set(key, []);
                }
                buckets.get(key).push(item);
            }

            const ordered = [...buckets.keys()]
                .filter(Boolean)
                .sort((a, b) => a.localeCompare(b, locale()));
            if (buckets.has('')) {
                ordered.push('');
            }

            for (const group of ordered) {
                const heading = el(
                    'div',
                    'mp-credential-group-heading',
                    group
                        ? `${group} · ${buckets.get(group).length}`
                        : `${t('Sin grupo')} · ${buckets.get('').length}`,
                );
                this.list.appendChild(heading);
                const wrapper = el('div', 'mp-deck-group');
                wrapper.appendChild(appendDeck(buckets.get(group)));
                this.list.appendChild(wrapper);
            }
        } else {
            const wrapper = el('div', 'mp-deck-group');
            wrapper.appendChild(appendDeck(filtered));
            this.list.appendChild(wrapper);
        }

        this.updateTotpCodes();
    }

    renderGroupFilters(all, groups) {
        if (!this.groupFilters) {
            return;
        }

        const ungrouped = all.filter((item) => !credentialFromItem(item).group).length;
        this.groupFilters.replaceChildren();
        this.groupFilters.hidden = groups.length === 0 && ungrouped === 0;

        const makeButton = (value, label, count) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'mp-filter min-h-10 shrink-0 px-4 py-1.5 text-[14px]';
            button.dataset.credentialGroupFilter = value;
            button.setAttribute('aria-pressed', this.groupFilter === value ? 'true' : 'false');
            button.appendChild(document.createTextNode(label));
            const counter = el('span', 'ml-1.5 text-[12px] font-bold opacity-80', String(count));
            button.appendChild(counter);
            this.groupFilters.appendChild(button);
        };

        makeButton('all', t('Todos los grupos'), all.length);
        for (const group of groups) {
            makeButton(group, group, all.filter((item) => credentialFromItem(item).group === group).length);
        }
        if (ungrouped > 0) {
            makeButton('__none__', t('Sin grupo'), ungrouped);
        }
    }

    renderCompactCard(item) {
        const parsed = credentialFromItem(item);
        const head = this.renderCredentialHead(item, parsed, false);
        const rest = [];
        const tags = this.renderCredentialTags(parsed);
        if (tags) {
            rest.push(tags);
        }
        if (parsed.username) {
            rest.push(el('p', 'mt-3 truncate text-[15px] text-on-surface-variant', parsed.username));
        }

        const content = this.credentialSurface(parsed, head, rest, true);
        const card = createCompactCard({
            dataset: { credentialId: item.id },
            content,
        });
        return card;
    }

    renderFlipCard(item) {
        const parsed = credentialFromItem(item);
        const frontHead = this.renderCredentialHead(item, parsed, true);
        const frontRest = [];
        const frontTags = this.renderCredentialTags(parsed);
        if (frontTags) {
            frontRest.push(frontTags);
        }
        if (parsed.username) {
            frontRest.push(el('p', 'mt-3 truncate text-[15px] text-on-surface-variant', parsed.username));
        }
        frontRest.push(this.flipRow(t('Ver reverso'), 'flip'));

        const backHead = this.renderCredentialHead(item, parsed, true);
        const backRest = [];
        const backTags = this.renderCredentialTags(parsed);
        if (backTags) {
            backRest.push(backTags);
        }
        backRest.push(this.renderCredentialBody(item, parsed));
        backRest.push(this.renderCredentialActions(item, parsed));
        backRest.push(this.flipRow(t('Volver al frente'), 'undo'));

        const front = this.credentialSurface(parsed, frontHead, frontRest);
        const back = this.credentialSurface(parsed, backHead, backRest);
        const card = createFlipCard({
            dataset: { credentialId: item.id },
            front,
            back,
            flipped: this.flippedCredentialId === item.id,
        });
        card.classList.add('is-selected');

        card.querySelectorAll('[data-deck-flip]').forEach((button) => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                const next = !card.classList.contains('is-flipped');
                setDeckFlipped(card, next);
                this.flippedCredentialId = next ? item.id : null;
            });
        });

        return card;
    }

    credentialSurface(parsed, head, rest, compact = false) {
        const hasAppearance = parsed.appearance.mode !== 'none'
            && (parsed.appearance.color || parsed.appearance.image);
        const card = el(
            'div',
            [
                'mp-credential-card',
                compact ? 'mp-credential-card--compact' : '',
                hasAppearance ? `has-${parsed.appearance.mode}` : '',
            ].filter(Boolean).join(' '),
        );

        if (hasAppearance) {
            card.style.setProperty('--credential-appearance-color', parsed.appearance.color || BANNER_COLORS[0]);
            if (parsed.appearance.image) {
                card.style.setProperty('--credential-appearance-image', `url("${parsed.appearance.image}")`);
            }

            if (parsed.appearance.mode === 'banner') {
                const banner = el('div', 'mp-credential-card__banner');
                banner.appendChild(head);
                card.appendChild(banner);
                rest.forEach((node) => card.appendChild(node));
                return card;
            }

            const content = el('div', 'mp-credential-card__content');
            content.appendChild(head);
            rest.forEach((node) => content.appendChild(node));
            card.appendChild(content);
            return card;
        }

        card.appendChild(head);
        rest.forEach((node) => card.appendChild(node));
        return card;
    }

    renderCredentialHead(item, parsed, withFavorite) {
        const head = el('div', 'mp-credential-card__head');
        const identity = el('div', 'mp-credential-card__identity');
        const tile = el('span', 'mp-credential-card__tile');
        if (parsed.logo) {
            const logo = document.createElement('img');
            logo.src = parsed.logo;
            logo.alt = '';
            logo.className = 'h-8 w-8 object-contain';
            tile.appendChild(logo);
        } else {
            tile.appendChild(icon(typeIconSymbol(parsed), 'text-[24px]'));
        }
        const names = el('div', 'min-w-0');
        names.appendChild(el('p', 'mp-credential-card__title', item.metadata?.title || t('Credencial')));
        names.appendChild(el('p', 'mp-credential-card__subtitle', typeLabelFor(parsed)));
        if (window.mypassSharedItemIds?.has(item.id)) {
            names.appendChild(el('span', 'mp-badge mp-badge-primary mt-1', t('Compartido')));
        }
        identity.append(tile, names);
        head.appendChild(identity);

        if (withFavorite) {
            const favorite = el('button', 'mp-btn mp-btn-quiet mp-btn-icon shrink-0');
            favorite.type = 'button';
            favorite.dataset.credentialAction = 'favorite';
            favorite.setAttribute('aria-pressed', parsed.favorite ? 'true' : 'false');
            favorite.setAttribute('aria-label', parsed.favorite ? t('Quitar de favoritas') : t('Marcar como favorita'));
            favorite.appendChild(icon(parsed.favorite ? 'star' : 'star_border', parsed.favorite ? 'text-[22px] text-secondary' : 'text-[22px]'));
            head.appendChild(favorite);
        } else if (parsed.favorite) {
            const star = el('span', 'shrink-0 text-secondary');
            star.appendChild(icon('star', 'text-[22px]'));
            head.appendChild(star);
        }

        return head;
    }

    renderCredentialTags(parsed) {
        if (!parsed.group && parsed.tags.length === 0) {
            return null;
        }

        const tags = el('div', 'mt-3 flex flex-wrap gap-1.5');
        if (parsed.group) {
            const groupBadge = el('span', 'mp-badge mp-badge-primary');
            groupBadge.appendChild(icon('folder', 'text-[15px]'));
            groupBadge.appendChild(document.createTextNode(parsed.group));
            tags.appendChild(groupBadge);
        }
        for (const tag of parsed.tags.slice(0, 6)) {
            tags.appendChild(el('span', 'mp-badge mp-badge-neutral', tag));
        }
        return tags;
    }

    renderCredentialBody(item, parsed) {
        const body = el('div', 'mp-credential-card__body');
        body.appendChild(this.renderRow(t('Usuario'), parsed.username || t('Sin usuario'), 'copy-username', parsed.username, false));
        body.appendChild(this.renderPasswordRow(item, parsed));
        if (parsed.totp) {
            body.appendChild(this.renderTotpRow(item, parsed.totp));
        }
        if (parsed.url) {
            body.appendChild(this.renderRow(t('Sitio'), parsed.url, 'open-url', null, false, parsed.url));
        }
        if (item.metadata?.original_name) {
            body.appendChild(this.renderRow(t('Archivo'), item.metadata.original_name, 'download', null, false));
        }
        if (parsed.notes) {
            const notes = el('div', 'mp-credential-row items-start');
            notes.appendChild(el('span', 'mp-credential-row__label', t('Notas')));
            notes.appendChild(el('p', 'mp-credential-row__value whitespace-pre-wrap font-sans text-on-surface-variant', parsed.notes));
            body.appendChild(notes);
        }
        return body;
    }

    renderCredentialActions(item, parsed) {
        const actions = el('div', 'mp-credential-actions');
        actions.appendChild(this.actionButton(t('Copiar usuario'), 'copy-username', 'content_copy', parsed.username));
        actions.appendChild(this.actionButton(t('Copiar contraseña'), 'copy-password', 'lock', parsed.password));
        if (parsed.totp) {
            actions.appendChild(this.actionButton(t('Copiar TOTP'), 'copy-totp', 'timer', true));
        }
        if (parsed.url) {
            actions.appendChild(this.actionButton(t('Abrir sitio'), 'open-url', 'open_in_new', true));
        }
        actions.appendChild(this.actionButton(t('Editar'), 'edit', 'edit', true));
        if (item.blob_id) {
            actions.appendChild(this.actionButton(t('Descargar adjunto'), 'download', 'download', true));
        }
        actions.appendChild(this.actionButton(t('Eliminar'), 'delete', 'delete', true, 'mp-btn-danger'));
        return actions;
    }

    flipRow(label, iconName) {
        const row = el('div', 'mt-4 flex justify-end');
        const button = el('button', 'mp-deck-flip');
        button.type = 'button';
        button.dataset.deckFlip = 'true';
        button.appendChild(icon(iconName, 'text-[18px]'));
        button.appendChild(document.createTextNode(label));
        row.appendChild(button);
        return row;
    }

    renderRow(label, value, actionName, copyValue, secret, display = null) {
        const row = el('div', 'mp-credential-row');
        row.appendChild(el('span', 'mp-credential-row__label', label));
        const text = el('span', 'mp-credential-row__value', display || value);
        if (secret) {
            text.classList.add('mp-credential-secret');
        }
        row.appendChild(text);

        if (actionName === 'copy-username' && copyValue) {
            row.appendChild(this.inlineButton(t('Copiar'), actionName, 'content_copy'));
        } else if (actionName === 'open-url') {
            row.appendChild(this.inlineButton(t('Abrir'), actionName, 'open_in_new'));
        } else if (actionName === 'download') {
            row.appendChild(this.inlineButton(t('Descargar'), actionName, 'download'));
        }

        return row;
    }

    renderPasswordRow(item, parsed) {
        const row = el('div', 'mp-credential-row');
        row.appendChild(el('span', 'mp-credential-row__label', t('Contraseña')));
        const value = el('span', 'mp-credential-row__value mp-credential-secret', parsed.password ? '••••••••••••' : t('Sin contraseña'));
        value.dataset.credentialPassword = 'true';
        row.appendChild(value);

        const reveal = el('button', 'mp-btn mp-btn-quiet mp-btn-sm');
        reveal.type = 'button';
        reveal.dataset.credentialAction = 'reveal-password';
        reveal.setAttribute('aria-pressed', 'false');
        reveal.setAttribute('aria-label', t('Mostrar contraseña'));
        reveal.appendChild(icon('visibility', 'text-[18px]'));
        reveal.appendChild(document.createTextNode(t('Mostrar')));
        row.appendChild(reveal);

        if (parsed.password) {
            row.appendChild(this.inlineButton(t('Copiar'), 'copy-password', 'content_copy'));
        }

        return row;
    }

    renderTotpRow(item, config) {
        const row = el('div', 'mp-credential-row');
        row.appendChild(el('span', 'mp-credential-row__label', 'TOTP'));
        const wrap = el('div', 'flex min-w-0 flex-1 flex-wrap items-center gap-3');
        const code = el('span', 'mp-totp-code', '••• •••');
        code.dataset.totpCode = 'true';
        const meta = el('span', 'mp-totp-meta', '');
        meta.dataset.totpMeta = 'true';
        wrap.append(code, meta);
        row.appendChild(wrap);
        row.appendChild(this.inlineButton(t('Copiar'), 'copy-totp', 'content_copy'));
        this.totpConfigs.set(item.id, config);
        return row;
    }

    inlineButton(label, actionName, iconName) {
        const node = button(label, 'mp-btn mp-btn-quiet mp-btn-sm');
        node.dataset.credentialAction = actionName;
        if (iconName) {
            node.replaceChildren(icon(iconName, 'text-[18px]'), document.createTextNode(label));
        }
        return node;
    }

    actionButton(label, actionName, iconName, enabled = true, extraClass = 'mp-btn-secondary') {
        const node = button(label, `mp-btn ${extraClass} mp-btn-sm`);
        node.dataset.credentialAction = actionName;
        node.disabled = !enabled;
        if (iconName !== true) {
            node.replaceChildren(icon(iconName, 'text-[18px]'), document.createTextNode(label));
        }
        return node;
    }

    startTotpTimer() {
        if (this.totpTimer) {
            clearInterval(this.totpTimer);
        }
        this.totpTimer = setInterval(() => this.updateTotpCodes(), 1000);
    }

    async updateTotpCodes() {
        if (this.totpBusy) {
            return;
        }
        this.totpBusy = true;

        try {
            const cards = this.list?.querySelectorAll('[data-credential-id]') || [];
            for (const card of cards) {
                const config = this.totpConfigs.get(card.dataset.credentialId);
                const output = card.querySelector('[data-totp-code]');
                const meta = card.querySelector('[data-totp-meta]');
                if (!config || !output) {
                    continue;
                }
                try {
                    const result = await generateTotp(config);
                    if (result) {
                        output.textContent = result.code;
                        if (meta) {
                            meta.textContent = tf('Renueva en :seconds s', {
                                seconds: totpSecondsLeft(result.period),
                            });
                        }
                    } else if (meta) {
                        meta.textContent = t('TOTP no válido');
                    }
                } catch {
                    output.textContent = '••• •••';
                    if (meta) {
                        meta.textContent = t('TOTP no válido');
                    }
                }
            }
        } finally {
            this.totpBusy = false;
        }
    }
}
