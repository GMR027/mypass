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

    return {
        category: CATEGORY_ICONS[raw.category] ? raw.category : 'login',
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

export class CredentialsApp {
    constructor(options) {
        this.options = options;
        this.items = [];
        this.filter = 'all';
        this.search = '';
        this.editing = null;
        this.baseMetadata = {};
        this.existingBlobId = null;
        this.clearAttachment = false;
        this.fileLabel = '';
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
        this.usernameInput = document.getElementById('credential-username');
        this.passwordInput = document.getElementById('credential-password');
        this.passwordToggle = document.getElementById('credential-password-toggle');
        this.passwordStrength = document.getElementById('credential-password-strength');
        this.generateButton = document.getElementById('credential-generate');
        this.urlInput = document.getElementById('credential-url');
        this.totpInput = document.getElementById('credential-totp');
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
        this.totpConfigs.clear();
        this.revealTimers.forEach((timer) => clearTimeout(timer));
        this.revealTimers.clear();
        this.close();
        this.render();
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
        this.modalTitle.textContent = t('Nueva credencial');
        this.form.reset();
        this.idInput.value = '';
        this.versionInput.value = '';
        this.blobInput.value = '';
        this.passwordInput.type = 'password';
        this.passwordToggle?.setAttribute('aria-pressed', 'false');
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
        this.baseMetadata = item.metadata ? { ...item.metadata } : {};
        this.existingBlobId = item.blob_id || null;
        this.clearAttachment = false;
        this.fileLabel = item.blob_id ? (item.metadata?.original_name || '') : '';
        this.modalTitle.textContent = t('Editar credencial');
        this.form.reset();
        this.idInput.value = item.id;
        this.versionInput.value = String(item.version || '');
        this.blobInput.value = item.blob_id || '';
        this.titleInput.value = item.metadata?.title || '';
        this.categoryInput.value = parsed.category;
        this.usernameInput.value = parsed.username;
        this.passwordInput.value = parsed.password;
        this.passwordInput.type = 'password';
        this.passwordToggle?.setAttribute('aria-pressed', 'false');
        this.urlInput.value = parsed.url;
        this.totpInput.value = parsed.totp ? totpToUri(parsed.totp) : '';
        this.tagsInput.value = parsed.tags.join(', ');
        this.notesInput.value = parsed.notes;
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
        this.form?.reset();
        this.hideError();
        this.renderFileState();
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

        const metadata = {
            ...this.baseMetadata,
            title,
            type: 'credential',
            expires_at: this.baseMetadata.expires_at || null,
            credential: {
                ...(this.baseMetadata.credential || {}),
                category: this.categoryInput.value,
                username: this.usernameInput.value.trim(),
                password,
                url: this.urlInput.value.trim(),
                notes: this.notesInput.value.trim(),
                tags: splitList(this.tagsInput.value),
                favorite: previous?.favorite === true,
                password_changed_at: passwordChangedAt,
                totp: totp || null,
            },
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
        const action = event.target.closest('[data-credential-action]');
        if (!action) {
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
        const filtered = this.items
            .filter((item) => item.state !== 'transferred')
            .filter((item) => {
                const parsed = credentialFromItem(item);
                if (this.filter === 'favorite' && !parsed.favorite) {
                    return false;
                }
                if (this.filter === 'totp' && !parsed.totp) {
                    return false;
                }
                if (!query) {
                    return true;
                }
                return [
                    item.metadata?.title,
                    parsed.username,
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

        const all = this.items.filter((item) => item.state !== 'transferred');
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

        for (const item of filtered) {
            this.list.appendChild(this.renderCard(item));
        }

        this.updateTotpCodes();
    }

    renderCard(item) {
        const parsed = credentialFromItem(item);
        const card = el('li', 'mp-credential-card mp-fade-in');
        card.dataset.credentialId = item.id;

        const head = el('div', 'mp-credential-card__head');
        const identity = el('div', 'mp-credential-card__identity');
        const tile = el('span', 'mp-credential-card__tile');
        tile.appendChild(icon(CATEGORY_ICONS[parsed.category] || 'key', 'text-[24px]'));
        const names = el('div', 'min-w-0');
        names.appendChild(el('p', 'mp-credential-card__title', item.metadata?.title || t('Credencial')));
        names.appendChild(el('p', 'mp-credential-card__subtitle', t(CATEGORY_LABELS[parsed.category] || 'Acceso general')));
        identity.append(tile, names);
        head.appendChild(identity);

        const favorite = el('button', 'mp-btn mp-btn-quiet mp-btn-icon shrink-0');
        favorite.type = 'button';
        favorite.dataset.credentialAction = 'favorite';
        favorite.setAttribute('aria-pressed', parsed.favorite ? 'true' : 'false');
        favorite.setAttribute('aria-label', parsed.favorite ? t('Quitar de favoritas') : t('Marcar como favorita'));
        favorite.appendChild(icon(parsed.favorite ? 'star' : 'star_border', parsed.favorite ? 'text-[22px] text-secondary' : 'text-[22px]'));
        head.appendChild(favorite);
        card.appendChild(head);

        if (parsed.tags.length > 0) {
            const tags = el('div', 'mt-3 flex flex-wrap gap-1.5');
            for (const tag of parsed.tags.slice(0, 6)) {
                tags.appendChild(el('span', 'mp-badge mp-badge-neutral', tag));
            }
            card.appendChild(tags);
        }

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
        card.appendChild(body);

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
        card.appendChild(actions);

        if (item.state === 'transferred') {
            card.appendChild(el('p', 'mt-3 text-sm font-semibold text-on-surface-variant', t('Transferida a otra cuenta')));
        }

        return card;
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
