import sodium from 'libsodium-wrappers-sumo';
import { unzipSync, zipSync } from 'fflate';
import { WalletApp } from './passes';
import { PassGenerator } from './pass-generator';
import { CredentialsApp, credentialFromItem, isCredentialItem } from './credentials';
import { MedicalApp, medicalFromItem, isMedicalItem } from './medical';
import { createCompactCard, createFlipCard, setDeckFlipped, syncDeckHeight } from './card-deck';

const t = (key) => window.mypassT ? window.mypassT(key) : key;
const tf = (key, values) => window.mypassTF ? window.mypassTF(key, values) : key;
const dateLocale = () => (window.mypassLocale === 'en' ? 'en-US' : 'es-MX');

const app = document.getElementById('vault-app');
const loading = document.getElementById('vault-loading');
const vaultError = document.getElementById('vault-error');
const noKeys = document.getElementById('vault-no-keys');
const locked = document.getElementById('vault-locked');
const unlocked = document.getElementById('vault-unlocked');
const unlockForm = document.getElementById('unlock-form');
const unlockError = document.getElementById('unlock-error');
const passphraseInput = document.getElementById('vault-passphrase');
const recoveryFile = document.getElementById('recovery-file');
const recoveryDropZone = document.getElementById('recovery-drop-zone');
const recoveryFileStatus = document.getElementById('recovery-file-status');
const recoveryFileError = document.getElementById('recovery-file-error');
const emergencyForm = document.getElementById('emergency-key-form');
const emergencyKeyInput = document.getElementById('emergency-key');
const emergencyStatus = document.getElementById('emergency-key-status');
const emergencyError = document.getElementById('emergency-key-error');
const rememberDevice = document.getElementById('remember-device');
const trustedDeviceCard = document.getElementById('trusted-device-card');
const trustedUnlockButton = document.getElementById('trusted-unlock');
const trustedForgetButton = document.getElementById('trusted-forget');
const emailRecoveryCard = document.getElementById('email-recovery-card');
const emailRecoverySend = document.getElementById('email-recovery-send');
const emailRecoveryCode = document.getElementById('email-recovery-code');
const emailRecoveryVerify = document.getElementById('email-recovery-verify');
const emailRecoveryStatus = document.getElementById('email-recovery-status');
const emailRecoveryError = document.getElementById('email-recovery-error');
const lockButton = document.getElementById('vault-lock');
const addForm = document.getElementById('add-form');
const addError = document.getElementById('add-error');
const addStatus = document.getElementById('add-status');
const addSubmit = document.getElementById('add-submit');
const itemList = document.getElementById('item-list');
const docSearch = document.getElementById('doc-search');
const itemFileName = document.getElementById('item-file-name');
const expiryBanner = document.getElementById('expiry-banner');
const expiryList = document.getElementById('expiry-list');
const itemsEmpty = document.getElementById('items-empty');
const pairingCreate = document.getElementById('pairing-create');
const pairingCodeBox = document.getElementById('pairing-code-box');
const pairingCode = document.getElementById('pairing-code');
const pairingStatus = document.getElementById('pairing-status');
const pairingError = document.getElementById('pairing-error');
const deviceList = document.getElementById('device-list');
const deviceStatus = document.getElementById('device-status');
const deviceError = document.getElementById('device-error');
const contactForm = document.getElementById('contact-form');
const contactEmail = document.getElementById('contact-email');
const contactList = document.getElementById('contact-list');
const contactStatus = document.getElementById('contact-status');
const contactError = document.getElementById('contact-error');
const shareInbox = document.getElementById('share-inbox');
const shareStatus = document.getElementById('share-status');
const shareError = document.getElementById('share-error');
const shareModal = document.getElementById('share-modal');
const shareModalItem = document.getElementById('share-modal-item');
const shareContact = document.getElementById('share-contact');
const shareMode = document.getElementById('share-mode');
const shareExpires = document.getElementById('share-expires');
const shareSend = document.getElementById('share-send');
const shareCancel = document.getElementById('share-cancel');
const shareSendError = document.getElementById('share-send-error');
const shareModalClose = document.getElementById('share-modal-close');
const requestForm = document.getElementById('request-form');
const requestContact = document.getElementById('request-contact');
const requestType = document.getElementById('request-type');
const requestPurpose = document.getElementById('request-purpose');
const requestList = document.getElementById('request-list');
const requestStatus = document.getElementById('request-status');
const requestError = document.getElementById('request-error');
const contactsEmpty = document.getElementById('contacts-empty');
const sharesEmpty = document.getElementById('shares-empty');
const requestsEmpty = document.getElementById('requests-empty');
const itemsEmptyTitle = document.getElementById('items-empty-title');
const itemsEmptyCopy = document.getElementById('items-empty-copy');
const addDocumentModal = document.getElementById('add-document-modal');
const addDocumentOpen = document.getElementById('add-document-open');
const editItemModal = document.getElementById('edit-item-modal');
const editItemForm = document.getElementById('edit-item-form');
const editItemName = document.getElementById('edit-item-name');
const editItemExpires = document.getElementById('edit-item-expires');
const editItemError = document.getElementById('edit-item-error');
const editItemSubmit = document.getElementById('edit-item-submit');
const editItemClose = document.getElementById('edit-item-close');
const editItemCancel = document.getElementById('edit-item-cancel');
const addDocumentClose = document.getElementById('add-document-close');
const addDocumentCancel = document.getElementById('add-document-cancel');
const manageAccessModal = document.getElementById('manage-access-modal');
const manageAccessTitle = document.getElementById('manage-access-title');
const manageAccessList = document.getElementById('manage-access-list');
const manageAccessClose = document.getElementById('manage-access-close');
const manageAccessDone = document.getElementById('manage-access-done');
const confirmModal = document.getElementById('confirm-modal');
const confirmTitle = document.getElementById('confirm-title');
const confirmMessage = document.getElementById('confirm-message');
const confirmAccept = document.getElementById('confirm-accept');
const confirmCancel = document.getElementById('confirm-cancel');
const toast = document.getElementById('vault-toast');
const pairingCopy = document.getElementById('pairing-copy');
const sharePending = document.getElementById('share-pending');
const sharePendingText = document.getElementById('share-pending-text');
const backupOpen = document.getElementById('backup-open');
const backupModal = document.getElementById('backup-modal');
const backupClose = document.getElementById('backup-close');
const backupPassphrase = document.getElementById('backup-passphrase');
const backupPassphraseConfirm = document.getElementById('backup-passphrase-confirm');
const backupExportButton = document.getElementById('backup-export');
const backupFile = document.getElementById('backup-file');
const backupRestorePassphrase = document.getElementById('backup-restore-passphrase');
const backupImportButton = document.getElementById('backup-import');
const backupStatus = document.getElementById('backup-status');
const backupError = document.getElementById('backup-error');
const searchOpen = document.getElementById('vault-search-open');
const searchModal = document.getElementById('vault-search-modal');
const searchClose = document.getElementById('vault-search-close');
const searchInput = document.getElementById('vault-search-input');
const searchStatus = document.getElementById('vault-search-status');
const searchResults = document.getElementById('vault-search-results');
const trashOpen = document.getElementById('trash-open');
const trashModal = document.getElementById('trash-modal');
const trashClose = document.getElementById('trash-close');
const trashList = document.getElementById('trash-list');
const trashEmpty = document.getElementById('trash-empty');
const trashError = document.getElementById('trash-error');

const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
const keysUrl = app?.dataset.keysUrl;
const itemsUrl = app?.dataset.itemsUrl;
const blobsUrl = app?.dataset.blobsUrl;
const blobBase = app?.dataset.blobBase;
const pairingUrl = app?.dataset.pairingUrl;
const pairingBase = app?.dataset.pairingBase;
const devicesUrl = app?.dataset.devicesUrl;
const deviceBase = app?.dataset.deviceBase;
const contactsUrl = app?.dataset.contactsUrl;
const sharesUrl = app?.dataset.sharesUrl;
const shareBase = app?.dataset.shareBase;
const requestsUrl = app?.dataset.requestsUrl;
const requestBase = app?.dataset.requestBase;
const recoveryRequestUrl = app?.dataset.recoveryRequestUrl;
const recoveryVerifyUrl = app?.dataset.recoveryVerifyUrl;
const easyMode = app?.dataset.easyMode === '1';
const emailRecoveryEnabled = app?.dataset.emailRecoveryEnabled === '1';

const TRUSTED_DB = 'mypass-trusted-device';
const TRUSTED_STORE = 'keys';

const MAGIC = new Uint8Array([0x4d, 0x59, 0x50, 0x44]); // MYPD
const VERSION = 1;
const CHUNK_SIZE = 1024 * 1024;
const HEADER_SIZE = 4 + 1 + 4 + 8 + 16;

let masterKey = null;
let wallet = null;
let passGenerator = null;
let credentials = null;
let medical = null;
let items = [];
let keysCache = null;
let pairingTimer = null;
let pairingSessionId = null;
let pairingBusy = false;
let identityPublicKey = null;
let identityPrivateKey = null;
let acceptedContacts = [];
let sharingItem = null;
let editingItem = null;
let outgoingShares = [];
let currentFilter = 'all';
let currentSearch = '';
let selectedDocumentId = null;
let flippedDocumentId = null;
let searchIndex = [];
let pendingSharedFiles = [];
let shareRequestInFlight = false;
let toastTimer = null;
const recentKey = 'mypass.recentItems.v1';
let lastActivityAt = Date.now();
let hiddenAt = null;
const autoLockKey = 'mypass.autoLockMinutes';

const toB64 = (bytes) => sodium.to_base64(bytes, sodium.base64_variants.ORIGINAL);
const fromB64 = (value) => sodium.from_base64(value, sodium.base64_variants.ORIGINAL);
const toJson = (value) => sodium.from_string(JSON.stringify(value));

function showError(element, message) {
    element.textContent = message;
    element.hidden = false;
}

function hideError(element) {
    element.hidden = true;
}

function el(tag, className = '', text = null) {
    const node = document.createElement(tag);
    if (className) {
        node.className = className;
    }
    if (text !== null) {
        node.textContent = text;
    }

    return node;
}

function icon(name, className = '') {
    const node = el('span', ['material-symbols-outlined', className].filter(Boolean).join(' '), name);
    node.setAttribute('aria-hidden', 'true');

    return node;
}

function showToast(message, action = null) {
    if (!toast) {
        return;
    }

    toast.replaceChildren(el('span', 'min-w-0 flex-1', message));
    if (action?.label && typeof action.onClick === 'function') {
        const button = el('button', 'mp-toast__action', action.label);
        button.type = 'button';
        button.addEventListener('click', () => {
            toast.hidden = true;
            clearTimeout(toastTimer);
            action.onClick();
        });
        toast.appendChild(button);
    }

    toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        toast.hidden = true;
    }, action ? 9000 : 3500);
}

function setButtonText(button, text) {
    if (!button) {
        return;
    }

    const iconNode = button.querySelector('.material-symbols-outlined');
    button.replaceChildren();
    if (iconNode) {
        button.appendChild(iconNode);
    }
    button.appendChild(document.createTextNode(text));
}

function openDialog(dialog) {
    if (!dialog || dialog.open) {
        return;
    }

    dialog.showModal();
}

function closeDialog(dialog) {
    if (dialog?.open) {
        dialog.close();
    }
}

let confirmResolver = null;

function resolveConfirm(value) {
    closeDialog(confirmModal);
    confirmResolver?.(value);
    confirmResolver = null;
}

function confirmAction({ title = t('Confirmar acción'), message, acceptLabel = t('Continuar') }) {
    if (!confirmModal) {
        return Promise.resolve(window.confirm(message));
    }

    confirmTitle.textContent = title;
    confirmMessage.textContent = message;
    confirmAccept.textContent = acceptLabel;
    openDialog(confirmModal);

    return new Promise((resolve) => {
        confirmResolver = resolve;
    });
}

function setupConfirmDialog() {
    confirmAccept?.addEventListener('click', () => resolveConfirm(true));
    confirmCancel?.addEventListener('click', () => resolveConfirm(false));
    confirmModal?.addEventListener('cancel', (event) => {
        event.preventDefault();
        resolveConfirm(false);
    });
}

function seal(message, key) {
    const nonce = sodium.randombytes_buf(sodium.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES);
    const ciphertext = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
        message,
        new Uint8Array(0),
        null,
        nonce,
        key,
    );

    const combined = new Uint8Array(nonce.length + ciphertext.length);
    combined.set(nonce, 0);
    combined.set(ciphertext, nonce.length);

    return combined;
}

function open(combined, key) {
    const bytes = typeof combined === 'string' ? fromB64(combined) : combined;
    const nonceLength = sodium.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES;
    const nonce = bytes.slice(0, nonceLength);
    const ciphertext = bytes.slice(nonceLength);

    return sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
        null,
        ciphertext,
        new Uint8Array(0),
        nonce,
        key,
    );
}

function trustedId() {
    return `user:${app?.dataset.email ?? ''}`;
}

function openTrustedDb() {
    return new Promise((resolve, reject) => {
        const request = window.indexedDB.open(TRUSTED_DB, 1);
        request.onupgradeneeded = () => {
            if (!request.result.objectStoreNames.contains(TRUSTED_STORE)) {
                request.result.createObjectStore(TRUSTED_STORE, { keyPath: 'id' });
            }
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

function trustedTransaction(mode) {
    return openTrustedDb().then((db) => ({
        db,
        store: db.transaction(TRUSTED_STORE, mode).objectStore(TRUSTED_STORE),
    }));
}

async function readTrustedRecord() {
    const { db, store } = await trustedTransaction('readonly');

    return new Promise((resolve, reject) => {
        const request = store.get(trustedId());
        request.onsuccess = () => {
            db.close();
            resolve(request.result ?? null);
        };
        request.onerror = () => {
            db.close();
            reject(request.error);
        };
    });
}

async function writeTrustedRecord(record) {
    const { db, store } = await trustedTransaction('readwrite');

    return new Promise((resolve, reject) => {
        const request = store.put(record);
        request.onsuccess = () => {
            db.close();
            resolve();
        };
        request.onerror = () => {
            db.close();
            reject(request.error);
        };
    });
}

async function deleteTrustedRecord() {
    const { db, store } = await trustedTransaction('readwrite');

    return new Promise((resolve, reject) => {
        const request = store.delete(trustedId());
        request.onsuccess = () => {
            db.close();
            resolve();
        };
        request.onerror = () => {
            db.close();
            reject(request.error);
        };
    });
}

function trustedCryptoAvailable() {
    return Boolean(window.indexedDB && window.crypto?.subtle && window.isSecureContext);
}

async function saveTrustedDevice(key) {
    if (!trustedCryptoAvailable()) {
        return;
    }

    try {
        const cryptoKey = await window.crypto.subtle.generateKey(
            { name: 'AES-GCM', length: 256 },
            false,
            ['encrypt', 'decrypt'],
        );
        const iv = window.crypto.getRandomValues(new Uint8Array(12));
        const data = await window.crypto.subtle.encrypt(
            { name: 'AES-GCM', iv },
            cryptoKey,
            key,
        );

        await writeTrustedRecord({
            id: trustedId(),
            key: cryptoKey,
            iv,
            data,
            created_at: new Date().toISOString(),
        });
    } catch (error) {
        console.warn('No se pudo recordar este equipo', error);
    }
}

async function openTrustedDevice() {
    if (!trustedCryptoAvailable()) {
        return null;
    }

    try {
        const record = await readTrustedRecord();
        if (!record) {
            return null;
        }

        const data = await window.crypto.subtle.decrypt(
            { name: 'AES-GCM', iv: new Uint8Array(record.iv) },
            record.key,
            record.data,
        );

        return new Uint8Array(data);
    } catch (error) {
        console.warn('El acceso recordado ya no es válido', error);
        await deleteTrustedRecord().catch(() => {});
        return null;
    }
}

async function refreshTrustedDeviceCard() {
    if (!trustedDeviceCard) {
        return;
    }

    const record = trustedCryptoAvailable() ? await readTrustedRecord().catch(() => null) : null;
    trustedDeviceCard.hidden = !record;

    if (rememberDevice) {
        rememberDevice.closest('label')?.classList.toggle('hidden', !trustedCryptoAvailable());
    }
}

async function unlockWithTrustedDevice() {
    hideError(unlockError);
    const button = trustedUnlockButton;
    button && (button.disabled = true);

    try {
        await sodium.ready;
        const candidate = await openTrustedDevice();

        if (!candidate) {
            await refreshTrustedDeviceCard();
            throw new Error(t('Este equipo ya no tiene acceso recordado. Escribe tu llave de la bóveda.'));
        }

        const keys = await fetchKeys();
        await finishUnlock(keys, candidate);
    } catch (error) {
        showError(unlockError, error.message || t('No se pudo abrir la bóveda con este equipo.'));
    } finally {
        if (button) {
            button.disabled = false;
        }
    }
}

function encryptJson(value) {
    return toB64(seal(toJson(value), masterKey));
}

function decryptJson(value) {
    return JSON.parse(sodium.to_string(open(value, masterKey)));
}

function writeUint32(view, offset, value) {
    view.setUint32(offset, value, true);
}

function readUint32(view, offset) {
    return view.getUint32(offset, true);
}

function makeNonce(prefix, counter) {
    const nonce = new Uint8Array(sodium.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES);
    nonce.set(prefix, 0);
    nonce[16] = counter & 0xff;
    nonce[17] = (counter >>> 8) & 0xff;
    nonce[18] = (counter >>> 16) & 0xff;
    nonce[19] = (counter >>> 24) & 0xff;

    return nonce;
}

function encryptFile(bytes) {
    return encryptFileWithKey(bytes, masterKey);
}

function encryptFileWithKey(bytes, key) {
    const prefix = sodium.randombytes_buf(16);
    const header = new Uint8Array(HEADER_SIZE);
    const view = new DataView(header.buffer);
    header.set(MAGIC, 0);
    header[4] = VERSION;
    writeUint32(view, 5, CHUNK_SIZE);
    view.setBigUint64(9, BigInt(bytes.length), true);
    header.set(prefix, 17);

    const parts = [header];
    let counter = 0;

    for (let offset = 0; offset < bytes.length; offset += CHUNK_SIZE) {
        const chunk = bytes.slice(offset, Math.min(offset + CHUNK_SIZE, bytes.length));
        const nonce = makeNonce(prefix, counter);
        const ciphertext = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
            chunk,
            header,
            null,
            nonce,
            key,
        );
        const length = new Uint8Array(4);
        writeUint32(new DataView(length.buffer), 0, ciphertext.length);
        parts.push(length, ciphertext);
        counter += 1;
    }

    const total = parts.reduce((sum, part) => sum + part.length, 0);
    const output = new Uint8Array(total);
    let offset = 0;
    for (const part of parts) {
        output.set(part, offset);
        offset += part.length;
    }

    return output;
}

function decryptFile(bytes) {
    return decryptFileWithKey(bytes, masterKey);
}

function decryptFileWithKey(bytes, key) {
    if (bytes.length < HEADER_SIZE) {
        throw new Error(t('El archivo está incompleto.'));
    }

    const header = bytes.slice(0, HEADER_SIZE);
    const magic = header.slice(0, 4);
    if (!magic.every((value, index) => value === MAGIC[index])) {
        throw new Error(t('El archivo no tiene el formato de My Pass.'));
    }
    if (header[4] !== VERSION) {
        throw new Error(t('La versión del archivo no es compatible.'));
    }

    const view = new DataView(header.buffer);
    const chunkSize = readUint32(view, 5);
    const totalSize = Number(view.getBigUint64(9, true));
    const prefix = header.slice(17, 33);

    if (chunkSize < 1 || chunkSize > 16 * 1024 * 1024) {
        throw new Error(t('El tamaño de bloque no es válido.'));
    }

    const parts = [];
    let offset = HEADER_SIZE;
    let counter = 0;
    let written = 0;

    while (offset < bytes.length) {
        if (offset + 4 > bytes.length) {
            throw new Error(t('El archivo está truncado.'));
        }

        const lengthView = new DataView(bytes.buffer, bytes.byteOffset + offset, 4);
        const length = lengthView.getUint32(0, true);
        offset += 4;

        if (length < 16 || offset + length > bytes.length) {
            throw new Error(t('El archivo está dañado.'));
        }

        const ciphertext = bytes.slice(offset, offset + length);
        offset += length;
        const nonce = makeNonce(prefix, counter);
        const chunk = sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
            null,
            ciphertext,
            header,
            nonce,
            key,
        );
        parts.push(chunk);
        written += chunk.length;
        counter += 1;

        if (written > totalSize) {
            throw new Error(t('El archivo excede el tamaño esperado.'));
        }
    }

    if (written !== totalSize) {
        throw new Error(t('El archivo está incompleto.'));
    }

    const output = new Uint8Array(written);
    let cursor = 0;
    for (const part of parts) {
        output.set(part, cursor);
        cursor += part.length;
    }

    return output;
}

const BACKUP_MAGIC = new Uint8Array([0x4d, 0x59, 0x50, 0x42]); // MYPB
const BACKUP_HEADER_SIZE = 30;
const BACKUP_VERSION = 1;
const BACKUP_KDF_ARGON2ID = 1;
const MAX_BACKUP_BYTES = 1024 * 1024 * 1024;

function concatBytes(parts) {
    const total = parts.reduce((sum, part) => sum + part.length, 0);
    const output = new Uint8Array(total);
    let offset = 0;
    for (const part of parts) {
        output.set(part, offset);
        offset += part.length;
    }
    return output;
}

function encryptBackup(passphrase, plaintext) {
    const salt = sodium.randombytes_buf(16);
    const ops = sodium.crypto_pwhash_OPSLIMIT_INTERACTIVE;
    const mem = sodium.crypto_pwhash_MEMLIMIT_INTERACTIVE;
    const key = sodium.crypto_pwhash(
        32,
        sodium.from_string(passphrase),
        salt,
        ops,
        mem,
        sodium.crypto_pwhash_ALG_ARGON2ID13,
    );

    try {
        const header = new Uint8Array(BACKUP_HEADER_SIZE);
        const view = new DataView(header.buffer);
        header.set(BACKUP_MAGIC, 0);
        header[4] = BACKUP_VERSION;
        header[5] = BACKUP_KDF_ARGON2ID;
        writeUint32(view, 6, ops);
        writeUint32(view, 10, mem);
        header.set(salt, 14);
        return concatBytes([header, encryptFileWithKey(plaintext, key)]);
    } finally {
        sodium.memzero(key);
    }
}

function decryptBackup(passphrase, container) {
    if (container.length <= BACKUP_HEADER_SIZE) {
        throw new Error(t('La copia de seguridad está incompleta.'));
    }
    const header = container.slice(0, BACKUP_HEADER_SIZE);
    if (!header.slice(0, 4).every((value, index) => value === BACKUP_MAGIC[index])) {
        throw new Error(t('El archivo no es una copia de My Pass.'));
    }
    if (header[4] !== BACKUP_VERSION || header[5] !== BACKUP_KDF_ARGON2ID) {
        throw new Error(t('La versión de la copia no es compatible.'));
    }

    const view = new DataView(header.buffer);
    const ops = readUint32(view, 6);
    const mem = readUint32(view, 10);
    if (ops < 1 || ops > 20 || mem < 8 * 1024 * 1024 || mem > 1024 * 1024 * 1024) {
        throw new Error(t('Los parámetros de la copia no son válidos.'));
    }
    const salt = header.slice(14, 30);
    const key = sodium.crypto_pwhash(
        32,
        sodium.from_string(passphrase),
        salt,
        ops,
        mem,
        sodium.crypto_pwhash_ALG_ARGON2ID13,
    );

    try {
        return decryptFileWithKey(container.slice(BACKUP_HEADER_SIZE), key);
    } finally {
        sodium.memzero(key);
    }
}

function downloadBytes(bytes, filename, type = 'application/octet-stream') {
    const blob = new Blob([bytes], { type });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    setTimeout(() => URL.revokeObjectURL(url), 60000);
}

async function exportBackup(passphrase) {
    const entries = {};
    const manifestItems = [];
    const source = items.filter((item) => item.state === 'active' || item.state === 'received');

    for (const item of source) {
        const manifestItem = {
            type: item.type,
            expires_at: item.expires_at || null,
            metadata: item.metadata || {},
        };

        if (item.blob_id) {
            const response = await fetch(`${blobBase}/${item.blob_id}`, {
                credentials: 'same-origin',
            });
            if (!response.ok) {
                throw new Error(t('No se pudo descargar uno de los archivos.'));
            }
            const plaintext = decryptFile(new Uint8Array(await response.arrayBuffer()));
            const entryName = `files/${item.id}.bin`;
            entries[entryName] = plaintext;
            manifestItem.file = {
                entry: entryName,
                name: item.metadata?.original_name || t('Documento'),
                mime: item.metadata?.mime || 'application/octet-stream',
                size: plaintext.length,
            };
        }
        manifestItems.push(manifestItem);
    }

    const manifest = {
        format: 1,
        app: 'My Pass',
        created_at: new Date().toISOString(),
        items: manifestItems,
    };
    entries['manifest.json'] = new TextEncoder().encode(JSON.stringify(manifest));
    const zip = zipSync(entries, { level: 0 });
    const container = encryptBackup(passphrase, zip);
    const today = new Date().toISOString().slice(0, 10).replaceAll('-', '');
    downloadBytes(container, `mypass-backup-${today}.mypassbackup`);
    return manifestItems.length;
}

async function importBackup(file, passphrase) {
    if (file.size > MAX_BACKUP_BYTES) {
        throw new Error(t('La copia es demasiado grande para este dispositivo.'));
    }

    const container = new Uint8Array(await file.arrayBuffer());
    const zip = decryptBackup(passphrase, container);
    const entries = unzipSync(zip);
    const manifestBytes = entries['manifest.json'];
    if (!manifestBytes) {
        throw new Error(t('La copia no tiene un manifiesto válido.'));
    }

    const manifest = JSON.parse(new TextDecoder().decode(manifestBytes));
    const sourceItems = Array.isArray(manifest.items) ? manifest.items : [];
    let imported = 0;
    let failed = 0;

    for (const entry of sourceItems) {
        try {
            const metadata = entry?.metadata && typeof entry.metadata === 'object' ? entry.metadata : {};
            let blobId = null;
            const fileInfo = entry?.file;
            if (fileInfo?.entry) {
                const plaintext = entries[fileInfo.entry];
                if (!plaintext) {
                    throw new Error(t('Falta un archivo de la copia.'));
                }
                const uploaded = await uploadEncryptedBytes(encryptFile(plaintext));
                blobId = uploaded.id;
                metadata.original_name = fileInfo.name || metadata.original_name || t('Documento');
                metadata.mime = fileInfo.mime || metadata.mime || 'application/octet-stream';
                metadata.size = plaintext.length;
            }

            const response = await fetch(itemsUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'application/json',
                    'X-CSRF-TOKEN': csrfToken,
                },
                credentials: 'same-origin',
                body: JSON.stringify({
                    type: typeof entry.type === 'string' ? entry.type : 'document',
                    encrypted_metadata: encryptJson(metadata),
                    blob_id: blobId,
                    expires_at: entry.expires_at || null,
                }),
            });
            if (!response.ok) {
                throw new Error(t('No se pudo guardar uno de los elementos.'));
            }
            imported += 1;
        } catch {
            failed += 1;
        }
    }

    await loadItems();
    return { imported, failed };
}

async function fetchKeys() {
    if (keysCache) {
        return keysCache;
    }

    const response = await fetch(keysUrl, {
        headers: { Accept: 'application/json' },
        credentials: 'same-origin',
    });

    if (!response.ok) {
        throw new Error(t('No se pudo consultar la bóveda.'));
    }

    keysCache = (await response.json()).data;

    return keysCache;
}

async function finishUnlock(keys, candidateKey) {
    if (keys.identity_private_keys_encrypted) {
        const raw = open(keys.identity_private_keys_encrypted, candidateKey);
        const privateKeys = JSON.parse(sodium.to_string(raw));
        identityPrivateKey = fromB64(privateKeys.encryption);
    }

    if (keys.identity_public_encryption_key) {
        identityPublicKey = fromB64(keys.identity_public_encryption_key);
    }

    if (masterKey) {
        sodium.memzero(masterKey);
    }

    masterKey = candidateKey;

    if (rememberDevice?.checked) {
        await saveTrustedDevice(candidateKey);
    }

    lastActivityAt = Date.now();
    passphraseInput.value = '';
    hideError(unlockError);
    hideError(recoveryFileError);
    hideError(emergencyError);
    recoveryFileStatus.textContent = '';
    emergencyStatus.textContent = '';
    locked.hidden = true;
    unlocked.hidden = false;
    setVaultStatus(true);
    const startingTab = initialTab();
    switchTab(startingTab);
    await loadItems();
    await loadContacts();
    await loadShares();
    await loadRequests();
    await loadDevices();
    if (startingTab === 'generador') {
        passGenerator?.open('flight');
    }
    await confirmAndSaveSharedFiles();
}

async function unlock(event) {
    event.preventDefault();
    hideError(unlockError);

    const passphrase = passphraseInput.value;
    const button = unlockForm.querySelector('button[type="submit"]');
    button.disabled = true;
    setButtonText(button, t('Abriendo…'));
    let kek = null;

    try {
        await sodium.ready;
        const keys = await fetchKeys();

        if (!keys?.pc_wrapped_master_key || !keys.pc_kdf_salt || !keys.pc_kdf_params) {
            throw new Error(t('La bóveda no tiene una llave de PC configurada.'));
        }

        const params = keys.pc_kdf_params;
        kek = sodium.crypto_pwhash(
            32,
            sodium.from_string(passphrase),
            fromB64(keys.pc_kdf_salt),
            params.ops_limit,
            params.mem_limit,
            sodium.crypto_pwhash_ALG_ARGON2ID13,
        );

        let candidate;
        try {
            candidate = open(keys.pc_wrapped_master_key, kek);
        } catch {
            throw new Error(t('La llave de la bóveda no es correcta.'));
        }

        await finishUnlock(keys, candidate);
    } catch (error) {
        showError(unlockError, error.message || t('No se pudo abrir la bóveda.'));
    } finally {
        if (kek) {
            sodium.memzero(kek);
        }
        button.disabled = false;
        setButtonText(button, t('Desbloquear'));
    }
}

async function unlockFromFile(file) {
    hideError(recoveryFileError);
    recoveryFileStatus.textContent = t('Leyendo archivo…');

    let recoveryKek = null;

    try {
        if (!file.name.toLowerCase().endsWith('.mypss')) {
            throw new Error(t('El archivo debe tener extensión .mypss.'));
        }

        if (file.size > 1024 * 1024) {
            throw new Error(t('El archivo de recuperación es demasiado grande.'));
        }

        const kit = JSON.parse(await file.text());

        if (kit.format !== 'mypss' || kit.type !== 'recovery-key') {
            throw new Error(t('El archivo no es un kit de recuperación de My Pass.'));
        }

        const digits = String(kit.key ?? '').replace(/\D/g, '');
        if (![16, 32].includes(digits.length)) {
            throw new Error(t('La clave del archivo debe tener 16 o 32 dígitos.'));
        }

        const keys = await fetchKeys();
        if (!keys?.recovery_wrapped_master_key || !keys.recovery_nonce) {
            throw new Error(t('Esta bóveda no tiene una llave de emergencia configurada.'));
        }

        const kdf = kit.kdf ?? {
            salt: keys.recovery_kdf_salt,
            ops_limit: keys.recovery_kdf_params?.ops_limit,
            mem_limit: keys.recovery_kdf_params?.mem_limit,
        };

        if (!kdf.salt || !kdf.ops_limit || !kdf.mem_limit) {
            throw new Error(t('El archivo no contiene los parámetros de la llave.'));
        }

        recoveryKek = sodium.crypto_pwhash(
            32,
            sodium.from_string(digits),
            fromB64(kdf.salt),
            kdf.ops_limit,
            kdf.mem_limit,
            sodium.crypto_pwhash_ALG_ARGON2ID13,
        );

        let candidate;
        try {
            candidate = sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
                null,
                fromB64(keys.recovery_wrapped_master_key),
                new Uint8Array(0),
                fromB64(keys.recovery_nonce),
                recoveryKek,
            );
        } catch {
            throw new Error(t('La clave del archivo no corresponde a esta bóveda.'));
        }

        await finishUnlock(keys, candidate);
        recoveryFileStatus.textContent = t('Bóveda abierta con la llave de emergencia.');
        recoveryFile.value = '';
    } catch (error) {
        recoveryFileStatus.textContent = '';
        showError(recoveryFileError, error.message || t('No se pudo abrir el archivo de recuperación.'));
    } finally {
        if (recoveryKek) {
            sodium.memzero(recoveryKek);
        }
    }
}

async function handleRecoveryFile(file) {
    if (!file) {
        return;
    }

    await sodium.ready;
    await unlockFromFile(file);
}

async function unlockFromEmergencyKey(event) {
    event.preventDefault();
    hideError(emergencyError);
    emergencyStatus.textContent = t('Comprobando la llave…');

    const digits = emergencyKeyInput.value.replace(/\D/g, '');
    const button = emergencyForm.querySelector('button[type="submit"]');

    if (![16, 32].includes(digits.length)) {
        emergencyStatus.textContent = '';
        showError(emergencyError, t('La llave de emergencia debe tener 16 o 32 dígitos.'));
        return;
    }

    button.disabled = true;
    let recoveryKek = null;

    try {
        await sodium.ready;
        const keys = await fetchKeys();

        if (!keys?.recovery_wrapped_master_key || !keys.recovery_nonce) {
            throw new Error(t('Esta bóveda no tiene una llave de emergencia configurada.'));
        }

        const params = keys.recovery_kdf_params ?? {};
        if (!keys.recovery_kdf_salt || !params.ops_limit || !params.mem_limit) {
            throw new Error(t('La bóveda no tiene los parámetros de la llave de emergencia.'));
        }

        recoveryKek = sodium.crypto_pwhash(
            32,
            sodium.from_string(digits),
            fromB64(keys.recovery_kdf_salt),
            params.ops_limit,
            params.mem_limit,
            sodium.crypto_pwhash_ALG_ARGON2ID13,
        );

        let candidate;
        try {
            candidate = sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
                null,
                fromB64(keys.recovery_wrapped_master_key),
                new Uint8Array(0),
                fromB64(keys.recovery_nonce),
                recoveryKek,
            );
        } catch {
            throw new Error(t('La llave de emergencia no corresponde a esta bóveda.'));
        }

        emergencyStatus.textContent = t('Bóveda abierta con la llave de emergencia.');
        emergencyKeyInput.value = '';
        await finishUnlock(keys, candidate);
    } catch (error) {
        emergencyStatus.textContent = '';
        showError(emergencyError, error.message || t('No se pudo abrir la bóveda con esa llave.'));
    } finally {
        if (recoveryKek) {
            sodium.memzero(recoveryKek);
        }
        button.disabled = false;
    }
}

async function postJson(url, payload = {}) {
    const response = await fetch(url, {
        method: 'POST',
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': csrfToken,
        },
        credentials: 'same-origin',
        body: JSON.stringify(payload),
    });
    const body = await response.json().catch(() => ({}));

    if (!response.ok) {
        throw new Error(body.message || t('No se pudo completar la operación.'));
    }

    return body;
}

async function sendEmailRecoveryCode() {
    hideError(emailRecoveryError);
    emailRecoveryStatus.textContent = t('Enviando código…');
    emailRecoverySend.disabled = true;

    try {
        if (!emailRecoveryEnabled) {
            throw new Error(t('La recuperación por correo no está activada.'));
        }

        const body = await postJson(recoveryRequestUrl);
        emailRecoveryStatus.textContent = body.message;
        emailRecoveryCode?.focus();
    } catch (error) {
        emailRecoveryStatus.textContent = '';
        showError(emailRecoveryError, error.message || t('No se pudo enviar el código.'));
    } finally {
        emailRecoverySend.disabled = false;
    }
}

async function verifyEmailRecoveryCode() {
    hideError(emailRecoveryError);
    const digits = (emailRecoveryCode?.value ?? '').replace(/\D/g, '');

    if (digits.length !== 6) {
        showError(emailRecoveryError, t('Escribe el código de 6 dígitos que llegó a tu correo.'));
        return;
    }

    emailRecoveryStatus.textContent = t('Comprobando el código…');
    emailRecoveryVerify.disabled = true;
    let secret = null;
    let candidate = null;

    try {
        await sodium.ready;
        const body = await postJson(recoveryVerifyUrl, { code: digits });
        const payload = body.data ?? {};
        secret = fromB64(payload.secret);
        candidate = open(fromB64(payload.wrapped_master_key), secret);

        const keys = await fetchKeys();
        await finishUnlock(keys, candidate);
        candidate = null;
        emailRecoveryCode.value = '';
        emailRecoveryStatus.textContent = t('Bóveda abierta con tu correo.');
    } catch (error) {
        emailRecoveryStatus.textContent = '';
        showError(emailRecoveryError, error.message || t('No se pudo abrir la bóveda con el código.'));
    } finally {
        if (secret) {
            sodium.memzero(secret);
        }
        if (candidate) {
            sodium.memzero(candidate);
        }
        emailRecoveryVerify.disabled = false;
    }
}

function setupRecoveryDropZone() {
    if (!recoveryDropZone || !recoveryFile) {
        return;
    }

    recoveryFile.addEventListener('change', () => {
        handleRecoveryFile(recoveryFile.files?.[0]);
    });

    recoveryDropZone.addEventListener('dragover', (event) => {
        event.preventDefault();
        recoveryDropZone.classList.add('border-primary', 'bg-surface-container-high');
    });

    recoveryDropZone.addEventListener('dragleave', () => {
        recoveryDropZone.classList.remove('border-primary', 'bg-surface-container-high');
    });

    recoveryDropZone.addEventListener('drop', (event) => {
        event.preventDefault();
        recoveryDropZone.classList.remove('border-primary', 'bg-surface-container-high');
        handleRecoveryFile(event.dataTransfer?.files?.[0]);
    });
}

function base64ToBytes(value) {
    return Uint8Array.from(atob(value), (char) => char.charCodeAt(0));
}

function bytesToBase64(bytes) {
    let binary = '';
    for (const byte of bytes) {
        binary += String.fromCharCode(byte);
    }

    return btoa(binary);
}

function stopPairing() {
    if (pairingTimer) {
        clearInterval(pairingTimer);
        pairingTimer = null;
    }
    pairingSessionId = null;
    pairingBusy = false;
}

async function approvePairing(sessionId, publicKey) {
    if (!masterKey) {
        throw new Error(t('La bóveda está bloqueada.'));
    }

    const key = await crypto.subtle.importKey(
        'spki',
        base64ToBytes(publicKey),
        { name: 'RSA-OAEP', hash: 'SHA-256' },
        false,
        ['encrypt'],
    );
    const encrypted = await crypto.subtle.encrypt({ name: 'RSA-OAEP' }, key, masterKey);

    const response = await fetch(`${pairingBase}/${sessionId}/approve`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
            'X-CSRF-TOKEN': csrfToken,
        },
        credentials: 'same-origin',
        body: JSON.stringify({
            encrypted_master_key: bytesToBase64(new Uint8Array(encrypted)),
        }),
    });

    if (!response.ok) {
        throw new Error(t('No se pudo autorizar el celular.'));
    }

    pairingStatus.textContent = t('Celular conectado. Ya puede abrir la bóveda con su PIN.');
    stopPairing();
    setTimeout(loadDevices, 5000);
}

async function pollPairing() {
    if (!pairingSessionId || pairingBusy) {
        return;
    }

    pairingBusy = true;

    try {
        const response = await fetch(`${pairingBase}/${pairingSessionId}`, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
        });

        if (!response.ok) {
            throw new Error(t('No se pudo consultar el estado del celular.'));
        }

        const data = (await response.json()).data;

        if (data.status === 'joined' && data.public_key) {
            pairingStatus.textContent = t('Celular detectado. Autorizando…');
            await approvePairing(pairingSessionId, data.public_key);
            return;
        }

        if (data.status === 'approved') {
            pairingStatus.textContent = t('Celular conectado.');
            stopPairing();
            return;
        }

        if (data.status === 'expired') {
            throw new Error(t('El código venció. Genera uno nuevo.'));
        }

        pairingStatus.textContent = t('Esperando a que escribas el código en el celular…');
    } catch (error) {
        stopPairing();
        showError(pairingError, error.message || t('No se pudo conectar el celular.'));
        pairingStatus.textContent = '';
    } finally {
        pairingBusy = false;
    }
}

async function loadDevices() {
    if (!deviceList) {
        return;
    }

    deviceList.replaceChildren();
    hideError(deviceError);

    try {
        const response = await fetch(devicesUrl, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudieron cargar los celulares.'));
        }

        const devices = (await response.json()).data ?? [];
        deviceStatus.textContent = devices.length === 0 ? t('No hay celulares conectados.') : '';

        for (const device of devices) {
            const item = el('li', 'mp-list-card flex flex-col justify-between gap-3 sm:flex-row sm:items-center');

            const info = el('div', 'flex items-start gap-3');
            const tile = el('span', 'flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-surface-container-high text-primary');
            tile.appendChild(icon(device.platform === 'android' ? 'smartphone' : 'devices', 'text-[24px]'));

            const text = el('div');
            const titleRow = el('div', 'flex flex-wrap items-center gap-2');
            titleRow.appendChild(el('p', 'font-semibold text-on-surface', device.name));
            titleRow.appendChild(el(
                'span',
                device.revoked_at ? 'mp-badge mp-badge-danger' : 'mp-badge mp-badge-primary',
                device.revoked_at ? t('Bloqueado') : t('Activo'),
            ));
            text.appendChild(titleRow);

            const details = [
                device.platform,
                device.last_seen_at
                    ? tf('Última conexión: :date', { date: new Date(device.last_seen_at).toLocaleString(dateLocale()) })
                    : null,
            ].filter(Boolean).join(' · ');
            text.appendChild(el('p', 'mt-1 text-sm text-on-surface-variant', details));

            info.append(tile, text);
            item.appendChild(info);

            if (!device.revoked_at) {
                const revoke = el('button', 'mp-btn mp-btn-danger mp-btn-sm self-start sm:self-center', t('Bloquear y borrar datos'));
                revoke.type = 'button';
                revoke.addEventListener('click', () => revokeDevice(device.id, revoke));
                item.appendChild(revoke);
            }

            deviceList.appendChild(item);
        }
    } catch (error) {
        showError(deviceError, error.message || t('No se pudieron cargar los celulares.'));
    }
}

async function revokeDevice(id, button) {
    const confirmed = await confirmAction({
        title: t('¿Bloquear este celular?'),
        message: t('En su siguiente conexión borrará la copia local.'),
        acceptLabel: t('Bloquear y borrar datos'),
    });

    if (!confirmed) {
        return;
    }

    button.disabled = true;
    try {
        const response = await fetch(`${deviceBase}/${id}/revoke`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo bloquear el celular.'));
        }
        await loadDevices();
        showToast(t('Celular bloqueado.'));
    } catch (error) {
        showError(deviceError, error.message || t('No se pudo bloquear el celular.'));
        button.disabled = false;
    }
}

let contacts = [];

async function loadContacts() {
    if (!contactList) {
        return;
    }

    hideError(contactError);
    contactList.replaceChildren();

    try {
        const response = await fetch(contactsUrl, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudieron cargar los contactos.'));
        }

        contacts = (await response.json()).data ?? [];
        acceptedContacts = contacts.filter((contact) => contact.status === 'accepted');
        wallet?.refresh();
        contactStatus.textContent = contacts.length === 0
            ? ''
            : tf(':count contacto(s) de confianza.', { count: acceptedContacts.length });
        contactsEmpty.hidden = contacts.length > 0;

        for (const contact of contacts) {
            renderContactCard(contact);
        }

        shareContact.replaceChildren();
        requestContact.replaceChildren();
        for (const contact of acceptedContacts) {
            const option = document.createElement('option');
            option.value = contact.id;
            option.textContent = tf(':name (:email)', { name: contact.name, email: contact.email });
            shareContact.appendChild(option);
            requestContact.appendChild(option.cloneNode(true));
        }
        if (acceptedContacts.length === 0) {
            for (const select of [shareContact, requestContact]) {
                const option = document.createElement('option');
                option.value = '';
                option.textContent = t('No hay contactos aceptados');
                select.appendChild(option);
            }
        }

        renderItems();
    } catch (error) {
        showError(contactError, error.message || t('No se pudieron cargar los contactos.'));
    }
}

function renderContactCard(contact) {
    const item = el('li', 'mp-list-card flex flex-col justify-between gap-4 md:flex-row md:items-center');
    const info = el('div', 'flex items-start gap-3');

    const avatar = el('span', 'flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-primary-fixed text-lg font-bold uppercase text-primary', (contact.name || contact.email).slice(0, 1));
    const body = el('div');
    const titleRow = el('div', 'flex flex-wrap items-center gap-2');
    titleRow.appendChild(el('p', 'font-semibold text-on-surface', contact.name));
    titleRow.appendChild(el('span', 'text-outline', '·'));
    titleRow.appendChild(el('span', 'text-sm text-on-surface-variant mp-break', contact.email));
    titleRow.appendChild(el(
        'span',
        contact.status === 'accepted' ? 'mp-badge mp-badge-primary' : contact.status === 'pending' ? 'mp-badge mp-badge-warning' : 'mp-badge mp-badge-danger',
        {
            pending: t('Pendiente de aceptación'),
            accepted: t('Contacto de confianza'),
            blocked: t('Bloqueado'),
        }[contact.status] ?? contact.status,
    ));
    body.appendChild(titleRow);
    body.appendChild(el(
        'p',
        'mt-1 text-sm text-on-surface-variant',
        contact.status === 'accepted'
            ? t('Llave de intercambio verificada · Envíos cifrados habilitados')
            : contact.status === 'pending'
                ? t('Recibió invitación. Esperando confirmación de su llave de intercambio.')
                : t('Este contacto ya no puede recibir envíos.'),
    ));

    info.append(avatar, body);
    item.appendChild(info);

    const actions = el('div', 'flex flex-wrap items-center gap-2 self-start md:self-center');

    if (contact.status === 'pending') {
        const accept = el('button', 'mp-btn mp-btn-primary mp-btn-sm', t('Aceptar contacto'));
        accept.type = 'button';
        accept.addEventListener('click', () => acceptContact(contact.id));
        actions.appendChild(accept);
    }

    if (contact.status === 'accepted') {
        const manage = el('button', 'mp-btn mp-btn-secondary mp-btn-sm', t('Gestionar acceso'));
        manage.type = 'button';
        manage.addEventListener('click', () => openManageAccess(contact));
        actions.appendChild(manage);
    }

    if (contact.status !== 'blocked') {
        const block = el('button', 'mp-btn mp-btn-danger mp-btn-sm', t('Bloquear y borrar datos'));
        block.type = 'button';
        block.addEventListener('click', () => blockContact(contact, block));
        actions.appendChild(block);
    }

    if (actions.childElementCount > 0) {
        item.appendChild(actions);
    }

    contactList.appendChild(item);
}

async function blockContact(contact, button) {
    const confirmed = await confirmAction({
        title: t('¿Bloquear este contacto?'),
        message: t('Se bloquearán los envíos con :name. Esta acción no se puede deshacer.', { name: contact.name }),
        acceptLabel: t('Bloquear y borrar datos'),
    });

    if (!confirmed) {
        return;
    }

    button.disabled = true;
    try {
        const response = await fetch(`${contactsUrl}/${contact.id}/block`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo bloquear el contacto.'));
        }
        await loadContacts();
        await loadShares();
        showToast(t('Contacto bloqueado.'));
    } catch (error) {
        showError(contactError, error.message || t('No se pudo bloquear el contacto.'));
        button.disabled = false;
    }
}

function shareStatusLabel(status) {
    return {
        pending: t('Pendiente'),
        accepted: t('Aceptado'),
        declined: t('Rechazado'),
        revoked: t('Cancelado'),
        expired: t('Vencido'),
    }[status] ?? status;
}

function openManageAccess(contact) {
    if (!manageAccessModal) {
        return;
    }

    manageAccessTitle.textContent = tf('Acceso de :name', { name: contact.name });
    manageAccessList.replaceChildren();

    const shares = outgoingShares.filter((share) => share.recipient === contact.name && ['pending', 'accepted'].includes(share.status));

    if (shares.length === 0) {
        const empty = el('div', 'mp-empty');
        empty.appendChild(icon('folder_shared', 'text-[32px] text-outline'));
        empty.appendChild(el('p', 'mt-2 font-semibold text-on-surface', t('No hay envíos activos con este contacto.')));
        manageAccessList.appendChild(empty);
    }

    for (const share of shares) {
        const row = el('div', 'mp-list-card flex flex-col justify-between gap-3 sm:flex-row sm:items-center');
        const info = el('div');
        const titleRow = el('div', 'flex flex-wrap items-center gap-2');
        titleRow.appendChild(el('p', 'font-semibold text-on-surface', typeLabel(share.item_type)));
        titleRow.appendChild(el(
            'span',
            share.status === 'pending' ? 'mp-badge mp-badge-warning' : share.status === 'accepted' ? 'mp-badge mp-badge-primary' : 'mp-badge mp-badge-neutral',
            shareStatusLabel(share.status),
        ));
        titleRow.appendChild(el('span', 'mp-badge mp-badge-neutral', share.mode === 'transfer' ? t('Transferencia') : t('Copia')));
        info.appendChild(titleRow);
        info.appendChild(el(
            'p',
            'mt-1 text-sm text-on-surface-variant',
            share.expires_at
                ? tf('Caduca: :date', { date: new Date(share.expires_at).toLocaleDateString(dateLocale()) })
                : t('Sin caducidad'),
        ));
        row.appendChild(info);

        if (share.status === 'pending') {
            const revoke = el('button', 'mp-btn mp-btn-danger mp-btn-sm', t('Cancelar envío'));
            revoke.type = 'button';
            revoke.addEventListener('click', () => revokeShare(share.id, revoke));
            row.appendChild(revoke);
        }

        manageAccessList.appendChild(row);
    }

    openDialog(manageAccessModal);
}

async function revokeShare(id, button) {
    const confirmed = await confirmAction({
        title: t('¿Cancelar este envío?'),
        message: t('La otra persona ya no podrá aceptar este documento.'),
        acceptLabel: t('Cancelar envío'),
    });

    if (!confirmed) {
        return;
    }

    button.disabled = true;
    try {
        const response = await fetch(`${shareBase}/${id}/revoke`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            throw new Error(body.message || t('No se pudo cancelar el envío.'));
        }
        closeDialog(manageAccessModal);
        await loadShares();
        await loadItems();
        showToast(t('Envío cancelado.'));
    } catch (error) {
        showError(shareError, error.message || t('No se pudo cancelar el envío.'));
        button.disabled = false;
    }
}

async function addContact(event) {
    event.preventDefault();
    hideError(contactError);

    const form = event.currentTarget;
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = t('Agregando…');

    try {
        const response = await fetch(contactsUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
            body: JSON.stringify({ email: contactEmail.value.trim() }),
        });

        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            throw new Error(body.message || t('No se pudo agregar el contacto.'));
        }

        contactEmail.value = '';
        contactStatus.textContent = t('Solicitud enviada. La otra persona debe aceptarla.');
        await loadContacts();
    } catch (error) {
        showError(contactError, error.message || t('No se pudo agregar el contacto.'));
    } finally {
        button.disabled = false;
        button.textContent = t('Agregar contacto');
    }
}

async function acceptContact(id) {
    hideError(contactError);

    try {
        const response = await fetch(`${contactsUrl}/${id}/accept`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo aceptar el contacto.'));
        }
        await loadContacts();
    } catch (error) {
        showError(contactError, error.message || t('No se pudo aceptar el contacto.'));
    }
}

async function loadShares() {
    if (!shareInbox) {
        return;
    }

    hideError(shareError);
    shareInbox.replaceChildren();

    try {
        const response = await fetch(sharesUrl, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo consultar lo compartido.'));
        }

        const body = await response.json();
        const incoming = (body.incoming ?? []).filter((share) => share.status === 'pending');
        outgoingShares = body.outgoing ?? [];
        shareStatus.textContent = incoming.length === 0 ? '' : tf(':count envío(s) pendiente(s).', { count: incoming.length });
        sharesEmpty.hidden = incoming.length > 0;

        for (const share of incoming) {
            const item = el('li', 'mp-list-card flex flex-col justify-between gap-4 md:flex-row md:items-center');
            const info = el('div', 'flex items-start gap-3');

            const tile = el('span', share.mode === 'transfer' ? 'flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-secondary-fixed text-on-secondary-fixed' : 'flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary-fixed text-primary');
            tile.appendChild(icon(
                share.item_type === 'ticket'
                    ? 'confirmation_number'
                    : share.item_type === 'medical_card'
                        ? 'medical_services'
                        : 'description',
                'text-[24px]',
            ));

            const text = el('div');
            const titleRow = el('div', 'flex flex-wrap items-center gap-2');
            titleRow.appendChild(el(
                'p',
                'font-semibold text-on-surface',
                tf(
                    share.item_type === 'ticket'
                        ? ':sender te envió un boleto'
                        : share.item_type === 'medical_card'
                            ? ':sender te envió un carnet médico'
                            : ':sender te envió un documento',
                    { sender: share.sender },
                ),
            ));
            titleRow.appendChild(el('span', share.mode === 'transfer' ? 'mp-badge mp-badge-warning' : 'mp-badge mp-badge-neutral', share.mode === 'transfer' ? t('Transferencia') : t('Copia')));
            if (share.expires_at) {
                titleRow.appendChild(el('span', 'mp-badge mp-badge-danger', tf('Caduca: :date', { date: new Date(share.expires_at).toLocaleDateString(dateLocale()) })));
            }
            text.appendChild(titleRow);
            text.appendChild(el('p', 'mt-1 text-sm text-on-surface-variant', t('Cifrado de punto a punto para tu dispositivo.')));

            info.append(tile, text);
            item.appendChild(info);

            const actions = el('div', 'flex flex-wrap items-center gap-2 self-start md:self-center');

            const accept = el('button', 'mp-btn mp-btn-primary mp-btn-sm', t('Aceptar'));
            accept.type = 'button';
            accept.addEventListener('click', () => acceptShare(share.id, accept));
            actions.appendChild(accept);

            const decline = el('button', 'mp-btn mp-btn-danger mp-btn-sm', t('Rechazar'));
            decline.type = 'button';
            decline.addEventListener('click', () => declineShare(share.id));
            actions.appendChild(decline);

            item.appendChild(actions);
            shareInbox.appendChild(item);
        }
    } catch (error) {
        showError(shareError, error.message || t('No se pudo consultar lo compartido.'));
    }
}

async function acceptShare(id, button) {
    hideError(shareError);

    if (!identityPrivateKey || !identityPublicKey) {
        showError(shareError, t('Esta bóveda no tiene llaves de identidad para recibir envíos.'));
        return;
    }

    button.disabled = true;
    button.textContent = t('Aceptando…');

    try {
        const response = await fetch(`${shareBase}/${id}`, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo descargar el envío.'));
        }

        const share = (await response.json()).data;
        if (!share.encrypted_package) {
            throw new Error(t('El envío no contiene datos.'));
        }

        const opened = sodium.crypto_box_seal_open(
            fromB64(share.encrypted_package),
            identityPublicKey,
            identityPrivateKey,
        );
        const packageData = JSON.parse(sodium.to_string(opened));
        const metadata = packageData.metadata;
        if (!metadata?.title) {
            throw new Error(t('El envío no tiene metadatos válidos.'));
        }

        const encryptedMetadata = encryptJson(metadata);
        let blobId = null;

        if (packageData.file) {
            const fileBytes = fromB64(packageData.file);
            if (fileBytes.length > 25 * 1024 * 1024) {
                throw new Error(t('El archivo compartido supera los 25 MB.'));
            }
            const encrypted = encryptFile(fileBytes);
            const blob = await uploadEncryptedBytes(encrypted);
            blobId = blob.id;
        }

        const itemId = crypto.randomUUID();
        const create = await fetch(itemsUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
            body: JSON.stringify({
                id: itemId,
                type: metadata.type || 'document',
                encrypted_metadata: encryptedMetadata,
                blob_id: blobId,
                expires_at: metadata.expires_at || null,
            }),
        });

        if (!create.ok) {
            throw new Error(t('No se pudo guardar el documento recibido.'));
        }

        const accept = await fetch(`${shareBase}/${id}/accept`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
            body: JSON.stringify({ recipient_item_id: itemId }),
        });

        if (!accept.ok) {
            throw new Error(t('No se pudo confirmar el envío.'));
        }

        await loadItems();
        await loadShares();
    } catch (error) {
        showError(shareError, error.message || t('No se pudo aceptar el envío.'));
    } finally {
        button.disabled = false;
        button.textContent = t('Aceptar');
    }
}

async function declineShare(id) {
    try {
        const response = await fetch(`${shareBase}/${id}/decline`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo rechazar el envío.'));
        }
        await loadShares();
    } catch (error) {
        showError(shareError, error.message || t('No se pudo rechazar el envío.'));
    }
}

function openShareModal(item) {
    sharingItem = item;
    hideError(shareSendError);
    shareModalItem.textContent = item.metadata?.title || t('Documento');
    shareExpires.value = '';
    shareMode.value = 'copy';
    openDialog(shareModal);

    if (acceptedContacts.length === 0) {
        showError(shareSendError, t('Primero agrega y acepta un contacto de confianza.'));
    }
}

function closeShareModal() {
    sharingItem = null;
    closeDialog(shareModal);
    hideError(shareSendError);
}

async function sendShare() {
    if (!sharingItem) {
        return;
    }

    const contactId = shareContact.value;
    if (!contactId) {
        showError(shareSendError, t('Elige un contacto.'));
        return;
    }

    hideError(shareSendError);
    shareSend.disabled = true;
    setButtonText(shareSend, t('Enviando…'));

    try {
        const keyResponse = await fetch(`${contactsUrl}/${contactId}/public-key`, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
        });
        if (!keyResponse.ok) {
            throw new Error(t('No se pudo obtener la llave del contacto.'));
        }

        const publicKey = fromB64((await keyResponse.json()).data.public_key);
        let fileBase64 = null;

        if (sharingItem.blob_id) {
            const blobResponse = await fetch(`${blobBase}/${sharingItem.blob_id}`, {
                credentials: 'same-origin',
            });
            if (!blobResponse.ok) {
                throw new Error(t('No se pudo leer el archivo para compartir.'));
            }
            const fileBytes = decryptFile(new Uint8Array(await blobResponse.arrayBuffer()));
            if (fileBytes.length > 25 * 1024 * 1024) {
                throw new Error(t('El archivo supera los 25 MB para compartir.'));
            }
            fileBase64 = toB64(fileBytes);
        }

        const packageData = {
            metadata: sharingItem.metadata,
            file: fileBase64,
        };
        const sealed = sodium.crypto_box_seal(
            sodium.from_string(JSON.stringify(packageData)),
            publicKey,
        );

        const response = await fetch(sharesUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
            body: JSON.stringify({
                contact_id: contactId,
                item_id: sharingItem.id,
                mode: shareMode.value,
                encrypted_package: toB64(sealed),
                expires_at: shareExpires.value || null,
            }),
        });

        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            throw new Error(body.message || t('No se pudo enviar el documento.'));
        }

        closeShareModal();
        await loadItems();
        await loadShares();
        await loadRequests();
        showToast(t('Solicitud enviada. La otra persona debe aceptarla.'));
    } catch (error) {
        showError(shareSendError, error.message || t('No se pudo enviar el documento.'));
    } finally {
        shareSend.disabled = false;
        setButtonText(shareSend, t('Enviar'));
    }
}

async function loadRequests() {
    if (!requestList) {
        return;
    }

    hideError(requestError);
    requestList.replaceChildren();

    try {
        const response = await fetch(requestsUrl, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudieron cargar las solicitudes.'));
        }

        const body = await response.json();
        const incoming = (body.incoming ?? []).filter((item) => item.status === 'pending');
        const outgoing = (body.outgoing ?? []).filter((item) => item.status === 'pending');
        requestStatus.textContent = (incoming.length + outgoing.length) === 0
            ? ''
            : tf(':count solicitud(es) pendiente(s).', { count: incoming.length + outgoing.length });
        requestsEmpty.hidden = (incoming.length + outgoing.length) > 0;

        for (const item of incoming) {
            const li = el('li', 'mp-list-card flex flex-col justify-between gap-4 md:flex-row md:items-center');
            const info = el('div', 'flex items-start gap-3');
            const tile = el('span', 'flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary-fixed text-primary');
            tile.appendChild(icon('download', 'text-[24px]'));
            const text = el('div');
            const titleRow = el('div', 'flex flex-wrap items-center gap-2');
            titleRow.appendChild(el('span', 'mp-badge mp-badge-primary', t('Entrante')));
            titleRow.appendChild(el('h4', 'font-semibold text-on-surface', tf(':requester necesita tu :type', { requester: item.requester, type: item.document_type })));
            text.appendChild(titleRow);
            text.appendChild(el('p', 'mt-1 text-sm text-on-surface-variant', item.purpose));
            text.appendChild(el('p', 'mt-1 text-sm text-on-surface-variant', t('Usa “Enviar” en el documento para responder. Se vinculará a esta solicitud.')));
            info.append(tile, text);
            li.appendChild(info);

            const decline = el('button', 'mp-btn mp-btn-danger mp-btn-sm self-start md:self-center', t('Rechazar'));
            decline.type = 'button';
            decline.addEventListener('click', () => declineRequest(item.id));
            li.appendChild(decline);
            requestList.appendChild(li);
        }

        for (const item of outgoing) {
            const li = el('li', 'mp-list-card flex flex-col justify-between gap-4 md:flex-row md:items-center');
            const info = el('div', 'flex items-start gap-3');
            const tile = el('span', 'flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-surface-container-high text-on-surface');
            tile.appendChild(icon('upload', 'text-[24px]'));
            const text = el('div');
            const titleRow = el('div', 'flex flex-wrap items-center gap-2');
            titleRow.appendChild(el('span', 'mp-badge mp-badge-neutral', t('Saliente')));
            titleRow.appendChild(el('h4', 'font-semibold text-on-surface', tf('Solicitaste :type a :target', { type: item.document_type, target: item.target })));
            text.appendChild(titleRow);
            text.appendChild(el('p', 'mt-1 text-sm text-on-surface-variant', item.purpose));
            text.appendChild(el('p', 'mt-1 text-sm text-on-surface-variant', t('Esperando que la otra persona seleccione el documento y apruebe el envío.')));
            info.append(tile, text);
            li.appendChild(info);

            const cancel = el('button', 'mp-btn mp-btn-danger mp-btn-sm self-start md:self-center', t('Cancelar'));
            cancel.type = 'button';
            cancel.addEventListener('click', () => cancelRequest(item.id));
            li.appendChild(cancel);
            requestList.appendChild(li);
        }
    } catch (error) {
        showError(requestError, error.message || t('No se pudieron cargar las solicitudes.'));
    }
}

async function createRequest(event) {
    event.preventDefault();
    hideError(requestError);

    if (!requestContact.value) {
        showError(requestError, t('Agrega y acepta un contacto primero.'));
        return;
    }

    const button = event.currentTarget.querySelector('button[type="submit"]');
    button.disabled = true;

    try {
        const response = await fetch(requestsUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
            body: JSON.stringify({
                contact_id: requestContact.value,
                document_type: requestType.value.trim(),
                purpose: requestPurpose.value.trim(),
            }),
        });

        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            throw new Error(body.message || t('No se pudo crear la solicitud.'));
        }

        requestForm.reset();
        await loadRequests();
    } catch (error) {
        showError(requestError, error.message || t('No se pudo crear la solicitud.'));
    } finally {
        button.disabled = false;
    }
}

async function declineRequest(id) {
    try {
        const response = await fetch(`${requestBase}/${id}/decline`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo rechazar la solicitud.'));
        }
        await loadRequests();
    } catch (error) {
        showError(requestError, error.message || t('No se pudo rechazar la solicitud.'));
    }
}

async function cancelRequest(id) {
    try {
        const response = await fetch(`${requestBase}/${id}/cancel`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo cancelar la solicitud.'));
        }
        await loadRequests();
    } catch (error) {
        showError(requestError, error.message || t('No se pudo cancelar la solicitud.'));
    }
}

async function createPairing() {
    if (!masterKey) {
        showError(pairingError, t('Primero desbloquea la bóveda.'));
        return;
    }

    hideError(pairingError);
    stopPairing();
    pairingCreate.disabled = true;
    setButtonText(pairingCreate, t('Generando…'));

    try {
        const response = await fetch(pairingUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
            body: JSON.stringify({}),
        });

        if (!response.ok) {
            throw new Error(t('No se pudo generar el código.'));
        }

        const data = (await response.json()).data;
        pairingSessionId = data.id;
        pairingCode.textContent = `${data.code.slice(0, 3)} ${data.code.slice(3)}`;
        pairingStatus.textContent = t('Esperando a que escribas el código en el celular…');
        pairingCodeBox.hidden = false;
        setButtonText(pairingCreate, t('Generar otro código'));

        pairingTimer = setInterval(pollPairing, 2000);
        pollPairing();
    } catch (error) {
        showError(pairingError, error.message || t('No se pudo generar el código.'));
        setButtonText(pairingCreate, t('Generar código'));
    } finally {
        pairingCreate.disabled = false;
    }
}

async function loadItems() {
    hideError(vaultError);
    itemList.replaceChildren();
    const response = await fetch(itemsUrl, {
        headers: { Accept: 'application/json' },
        credentials: 'same-origin',
    });

    if (!response.ok) {
        showError(vaultError, t('No se pudieron cargar los documentos.'));
        return;
    }

    const body = await response.json();
    items = [];

    for (const item of body.data ?? []) {
        try {
            const metadata = decryptJson(item.encrypted_metadata);
            items.push({ ...item, metadata });
        } catch {
            items.push({
                ...item,
                metadata: { title: t('Documento ilegible'), type: item.type },
                unreadable: true,
            });
        }
    }

    renderItems();
    wallet?.setItems(items);
    credentials?.setItems(items);
    medical?.setItems(items);
    buildSearchIndex();
    if (searchModal?.open) {
        renderSearch(searchInput?.value || '');
    }
}

function normalizeSearch(value) {
    return String(value ?? '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .trim();
}

function itemSection(item) {
    if (isCredentialItem(item)) {
        return 'credential';
    }
    if (isMedicalItem(item)) {
        return 'medical';
    }
    if (item.metadata?.pass) {
        return 'pass';
    }
    return 'document';
}

function sectionLabel(section) {
    return {
        pass: t('Pase'),
        credential: t('Credencial'),
        medical: t('Salud'),
        document: t('Documento'),
    }[section] || t('Documento');
}

function sectionIcon(section) {
    return {
        pass: 'wallet',
        credential: 'key',
        medical: 'medical_services',
        document: 'folder',
    }[section] || 'folder';
}

function searchSubtitle(item, section) {
    if (section === 'pass') {
        const pass = item.metadata?.pass || {};
        return [pass.issuer, pass.holder, pass.fields?.origin && pass.fields?.destination
            ? `${pass.fields.origin} → ${pass.fields.destination}`
            : null].filter(Boolean).join(' · ');
    }
    if (section === 'credential') {
        const credential = credentialFromItem(item);
        return [credential.username, credential.url].filter(Boolean).join(' · ');
    }
    if (section === 'medical') {
        const medical = medicalFromItem(item);
        return [medical.holder, medical.insurance?.provider].filter(Boolean).join(' · ');
    }
    const expiry = item.expires_at
        ? new Date(item.expires_at).toLocaleDateString(dateLocale(), { day: 'numeric', month: 'short', year: 'numeric' })
        : null;
    return [item.metadata?.original_name, expiry]
        .filter(Boolean)
        .join(' · ');
}

function buildSearchIndex() {
    searchIndex = items.filter((item) => item.state !== 'transferred').map((item) => {
        const section = itemSection(item);
        const values = [
            item.metadata?.title,
            item.metadata?.original_name,
            item.metadata?.ocr_text,
        ];

        if (section === 'pass') {
            const pass = item.metadata?.pass || {};
            values.push(pass.issuer, pass.holder, ...(pass.notes || []));
            values.push(...Object.values(pass.fields || {}));
        } else if (section === 'credential') {
            const credential = credentialFromItem(item);
            values.push(
                credential.username,
                credential.url,
                credential.notes,
                credential.categoryLabel,
                ...(credential.tags || []),
            );
        } else if (section === 'medical') {
            const medical = medicalFromItem(item);
            values.push(
                medical.holder,
                medical.insurance?.provider,
                medical.insurance?.policyNumber,
                medical.doctor?.name,
                ...(medical.allergies || []),
                ...(medical.conditions || []),
                ...(medical.medications || []),
                ...(medical.medicationSchedule || []).map((entry) => `${entry.name} ${entry.dose}`),
                ...(medical.appointments || []).map((entry) => `${entry.title} ${entry.location}`),
            );
        }

        return {
            item,
            section,
            text: normalizeSearch(values.filter(Boolean).join(' ')),
        };
    });
}

function recentIds() {
    try {
        const value = JSON.parse(localStorage.getItem(recentKey) || '[]');
        return Array.isArray(value) ? value.filter((id) => typeof id === 'string').slice(0, 12) : [];
    } catch {
        return [];
    }
}

function recordRecent(id) {
    if (!id) {
        return;
    }
    const next = [id, ...recentIds().filter((value) => value !== id)].slice(0, 12);
    try {
        localStorage.setItem(recentKey, JSON.stringify(next));
    } catch {
        // El almacenamiento local puede estar deshabilitado; no es crítico.
    }
}

function searchResultButton(entry) {
    const button = el('button', 'mp-card flex w-full items-start gap-3 p-3 text-left transition-colors hover:bg-surface-container');
    button.type = 'button';
    const tile = el('span', 'flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-surface-container-high text-primary');
    tile.appendChild(icon(sectionIcon(entry.section), 'text-[22px]'));
    const copy = el('span', 'min-w-0 flex-1');
    copy.appendChild(el('span', 'block text-[12px] font-bold uppercase tracking-wide text-primary', sectionLabel(entry.section)));
    copy.appendChild(el('span', 'mt-0.5 block truncate font-semibold text-on-surface', entry.item.metadata?.title || t('Sin título')));
    const subtitle = searchSubtitle(entry.item, entry.section);
    if (subtitle) {
        copy.appendChild(el('span', 'mt-0.5 block truncate text-sm text-on-surface-variant', subtitle));
    }
    button.append(tile, copy);
    button.addEventListener('click', () => openSearchResult(entry));
    return button;
}

function renderSearch(query) {
    if (!searchResults) {
        return;
    }

    searchResults.replaceChildren();
    const normalized = normalizeSearch(query);
    let entries = [];

    if (!normalized) {
        const ids = recentIds();
        entries = ids
            .map((id) => searchIndex.find((entry) => entry.item.id === id))
            .filter(Boolean);
        if (searchStatus) {
            searchStatus.textContent = entries.length
                ? t('Recientes')
                : t('Escribe para buscar en toda tu bóveda.');
        }
        if (entries.length === 0) {
            return;
        }
    } else {
        entries = searchIndex
            .filter((entry) => entry.text.includes(normalized))
            .sort((a, b) => {
                const aTitle = normalizeSearch(a.item.metadata?.title || '');
                const bTitle = normalizeSearch(b.item.metadata?.title || '');
                const aStarts = aTitle.startsWith(normalized) ? 0 : 1;
                const bStarts = bTitle.startsWith(normalized) ? 0 : 1;
                return aStarts - bStarts || aTitle.localeCompare(bTitle);
            })
            .slice(0, 40);
        if (searchStatus) {
            searchStatus.textContent = entries.length === 1
                ? t('1 resultado')
                : tf(':count resultados', { count: entries.length });
        }
    }

    if (entries.length === 0) {
        searchResults.appendChild(el('p', 'mp-empty', t('No encontramos nada con esa búsqueda.')));
        return;
    }

    for (const entry of entries) {
        searchResults.appendChild(searchResultButton(entry));
    }
}

function openSearch() {
    if (!searchModal) {
        return;
    }
    renderSearch('');
    openDialog(searchModal);
    setTimeout(() => searchInput?.focus(), 50);
}

function closeSearch() {
    closeDialog(searchModal);
}

function openSearchResult(entry) {
    closeSearch();
    recordRecent(entry.item.id);

    if (entry.section === 'pass') {
        switchTab('pases');
        wallet?.select(entry.item.id);
        setTimeout(() => wallet?.inspector?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 80);
    } else if (entry.section === 'credential') {
        switchTab('credentials');
        credentials?.openEdit(entry.item);
    } else if (entry.section === 'medical') {
        switchTab('medical');
        medical?.openEdit(entry.item);
    } else {
        switchTab('docs');
        currentFilter = 'all';
        currentSearch = '';
        if (docSearch) {
            docSearch.value = '';
        }
        renderItems();
        setTimeout(() => {
            const card = itemList?.querySelector(`[data-item-id="${CSS.escape(entry.item.id)}"]`);
            if (card) {
                card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                card.classList.add('ring-2', 'ring-primary', 'ring-offset-2');
                setTimeout(() => card.classList.remove('ring-2', 'ring-primary', 'ring-offset-2'), 2500);
            }
        }, 80);
    }
}

function expiryDate(value) {
    if (!value) {
        return null;
    }

    const [year, month, day] = String(value).slice(0, 10).split('-').map(Number);
    if (year && month && day) {
        return new Date(year, month - 1, day);
    }

    return new Date(value);
}

function renderExpiryBanner() {
    if (!expiryBanner || !expiryList) {
        return;
    }

    const now = new Date();
    const upcoming = items
        .filter((item) => !item.metadata?.pass && !isCredentialItem(item) && item.expires_at && item.state !== 'transferred')
        .map((item) => ({ item, date: expiryDate(item.expires_at) }))
        .filter(({ date }) => !Number.isNaN(date.getTime()))
        .filter(({ date }) => date.getTime() - now.getTime() <= 30 * 24 * 60 * 60 * 1000)
        .sort((a, b) => a.date - b.date);

    expiryBanner.hidden = upcoming.length === 0;
    expiryList.replaceChildren();

    for (const { item, date } of upcoming) {
        const li = document.createElement('li');
        const expired = date.getTime() < now.getTime();
        const title = item.metadata?.title || t('Sin título');
        li.textContent = expired
            ? tf(':title — venció el :date', { title, date: date.toLocaleDateString(dateLocale()) })
            : tf(':title — vence el :date', { title, date: date.toLocaleDateString(dateLocale()) });
        expiryList.appendChild(li);
    }
}

function typeLabel(type) {
    return {
        document: t('Documento'),
        ticket: t('Boleto o pase'),
        card: t('Tarjeta'),
        credential: t('Credencial'),
        medical_card: t('Carnet médico'),
        other: t('Otro'),
    }[type] ?? type;
}

function typeIcon(type) {
    return {
        document: 'badge',
        ticket: 'confirmation_number',
        card: 'credit_card',
        credential: 'key',
        medical_card: 'medical_services',
        other: 'description',
    }[type] ?? 'description';
}

function renderItems() {
    const documentItems = items.filter((item) => !item.metadata?.pass && !isCredentialItem(item) && !isMedicalItem(item));
    const query = currentSearch.trim().toLowerCase();
    const filteredItems = documentItems.filter((item) => {
        if (currentFilter !== 'all' && item.type !== currentFilter) {
            return false;
        }
        if (!query) {
            return true;
        }

        return [item.metadata?.title, item.metadata?.original_name, typeLabel(item.type)]
            .filter(Boolean)
            .some((value) => String(value).toLowerCase().includes(query));
    });

    itemsEmpty.hidden = filteredItems.length > 0;
    if (documentItems.length === 0) {
        itemsEmptyTitle.textContent = t('No tienes documentos todavía.');
        itemsEmptyCopy.textContent = t('Agrega tu primer documento y quedará cifrado antes de salir de tu equipo.');
    } else if (filteredItems.length === 0) {
        itemsEmptyTitle.textContent = query
            ? t('No hay documentos que coincidan.')
            : t('No hay documentos en esta categoría.');
        itemsEmptyCopy.textContent = query
            ? t('Prueba con otra búsqueda.')
            : t('Prueba con otro filtro o agrega un documento nuevo.');
    }

    const counts = {
        all: documentItems.length,
        document: documentItems.filter((item) => item.type === 'document').length,
        ticket: documentItems.filter((item) => item.type === 'ticket').length,
        card: documentItems.filter((item) => item.type === 'card').length,
        other: documentItems.filter((item) => item.type === 'other').length,
    };

    document.querySelectorAll('[data-filter]').forEach((button) => {
        button.setAttribute('aria-pressed', button.dataset.filter === currentFilter ? 'true' : 'false');
    });
    document.querySelectorAll('[data-filter-count]').forEach((span) => {
        span.textContent = counts[span.dataset.filterCount] ?? 0;
    });

    itemList.replaceChildren();
    renderExpiryBanner();

    if (selectedDocumentId && !filteredItems.some((item) => item.id === selectedDocumentId)) {
        selectedDocumentId = null;
        flippedDocumentId = null;
    }
    if (flippedDocumentId && flippedDocumentId !== selectedDocumentId) {
        flippedDocumentId = null;
    }

    const deck = el('div', 'mp-deck');
    filteredItems.forEach((item, index) => {
        const selected = item.id === selectedDocumentId;
        const card = selected ? documentFlipCard(item) : documentCompactCard(item);
        card.style.setProperty('--deck-index', String(index));
        deck.appendChild(card);
        if (selected) {
            window.requestAnimationFrame(() => syncDeckHeight(card));
        }
    });
    itemList.appendChild(deck);
}

function documentHeader(item) {
    const header = el('div', 'flex items-start gap-3');
    const tile = el('span', 'flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary-fixed text-primary');
    tile.appendChild(icon(typeIcon(item.type), 'text-[24px]'));

    const text = el('div', 'min-w-0 flex-1');
    text.appendChild(el('p', 'text-sm font-semibold text-primary', typeLabel(item.type)));
    text.appendChild(el('h3', 'mt-0.5 truncate text-[19px] font-semibold text-on-surface', item.metadata.title || t('Sin título')));

    const badges = el('div', 'mt-2 flex flex-wrap items-center gap-1.5');
    if (item.state === 'transferred') {
        badges.appendChild(el('span', 'mp-badge mp-badge-neutral', t('Transferido')));
    }

    if (item.expires_at) {
        const date = expiryDate(item.expires_at);
        if (!Number.isNaN(date.getTime())) {
            const days = Math.ceil((date.getTime() - Date.now()) / (24 * 60 * 60 * 1000));
            if (days < 0) {
                badges.appendChild(el('span', 'mp-badge mp-badge-danger', tf('Venció el :date', { date: date.toLocaleDateString(dateLocale()) })));
            } else if (days <= 30) {
                badges.appendChild(el('span', 'mp-badge mp-badge-warning', tf('Vence en :days días', { days })));
            } else {
                badges.appendChild(el('span', 'mp-badge mp-badge-neutral', tf('Vigente hasta :date', { date: date.toLocaleDateString(dateLocale()) })));
            }
        }
    }

    text.appendChild(badges);
    header.append(tile, text);
    return header;
}

function documentActions(item) {
    const actions = el('div', 'mt-4 flex flex-wrap gap-2 border-t border-outline-variant/50 pt-4');

    if (item.blob_id) {
        const view = el('button', 'mp-btn mp-btn-secondary mp-btn-sm', t('Ver'));
        view.type = 'button';
        view.addEventListener('click', () => viewItem(item, view));
        actions.appendChild(view);
    }

    const edit = el('button', 'mp-btn mp-btn-secondary mp-btn-sm', t('Editar'));
    edit.type = 'button';
    edit.addEventListener('click', () => openEditItem(item));
    actions.appendChild(edit);

    if (acceptedContacts.length > 0 && item.state !== 'transferred') {
        const share = el('button', 'mp-btn mp-btn-secondary mp-btn-sm', t('Enviar'));
        share.type = 'button';
        share.addEventListener('click', () => openShareModal(item));
        actions.appendChild(share);
    }

    if (item.blob_id) {
        const download = el('button', 'mp-btn mp-btn-quiet mp-btn-sm', t('Descargar'));
        download.type = 'button';
        download.addEventListener('click', () => downloadItem(item, download));
        actions.appendChild(download);
    }

    const remove = el('button', 'mp-btn mp-btn-danger mp-btn-sm', t('Eliminar'));
    remove.type = 'button';
    remove.addEventListener('click', () => deleteItem(item, remove));
    actions.appendChild(remove);

    return actions;
}

function documentPreview(item, limit = 110) {
    const preview = item.metadata?.ocr_text?.trim() || item.metadata?.original_name || '';
    return preview ? el('p', 'mt-2 truncate text-[13px] text-on-surface-variant', preview.slice(0, limit)) : null;
}

function deckFlipButton(label, iconName) {
    const button = el('button', 'mp-deck-flip');
    button.type = 'button';
    button.dataset.deckFlip = 'true';
    button.appendChild(icon(iconName, 'text-[18px]'));
    button.appendChild(document.createTextNode(label));
    return button;
}

function documentCompactCard(item) {
    const content = el('div', 'p-4');
    content.appendChild(documentHeader(item));
    const preview = documentPreview(item);
    if (preview) {
        content.appendChild(preview);
    }

    const card = createCompactCard({
        className: 'mp-card',
        dataset: { itemId: item.id },
        content,
    });
    card.addEventListener('click', () => {
        selectedDocumentId = item.id;
        flippedDocumentId = null;
        renderItems();
    });
    return card;
}

function documentFlipCard(item) {
    const front = el('div', 'p-5');
    front.appendChild(documentHeader(item));
    const preview = documentPreview(item, 180);
    if (preview) {
        front.appendChild(preview);
    }
    const frontRow = el('div', 'mt-4 flex justify-end');
    frontRow.appendChild(deckFlipButton(t('Ver reverso'), 'flip'));
    front.appendChild(frontRow);

    const back = el('div', 'p-5');
    back.appendChild(documentHeader(item));
    if (item.metadata?.ocr_text?.trim()) {
        back.appendChild(el('p', 'mt-3 whitespace-pre-wrap text-[13px] leading-relaxed text-on-surface-variant', item.metadata.ocr_text.trim().slice(0, 700)));
    }
    back.appendChild(documentActions(item));
    const backRow = el('div', 'mt-4 flex justify-end');
    backRow.appendChild(deckFlipButton(t('Volver al frente'), 'undo'));
    back.appendChild(backRow);

    const card = createFlipCard({
        faceClassName: 'mp-card',
        dataset: { itemId: item.id },
        front,
        back,
        flipped: flippedDocumentId === item.id,
    });

    card.querySelectorAll('[data-deck-flip]').forEach((button) => {
        button.addEventListener('click', (event) => {
            event.preventDefault();
            event.stopPropagation();
            const next = !card.classList.contains('is-flipped');
            setDeckFlipped(card, next);
            flippedDocumentId = next ? item.id : null;
        });
    });

    return card;
}

async function viewItem(item, button) {
    if (!item.blob_id || !item.metadata) {
        return;
    }

    recordRecent(item.id);
    button.disabled = true;
    button.textContent = t('Abriendo…');

    try {
        const response = await fetch(`${blobBase}/${item.blob_id}`, {
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo abrir el archivo.'));
        }

        const decrypted = decryptFile(new Uint8Array(await response.arrayBuffer()));
        const blob = new Blob([decrypted], {
            type: item.metadata.mime || 'application/octet-stream',
        });
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (error) {
        showError(vaultError, error.message || t('No se pudo abrir el archivo.'));
    } finally {
        button.disabled = false;
        button.textContent = t('Ver');
    }
}

function openEditItem(item) {
    editingItem = item;
    recordRecent(item.id);
    hideError(editItemError);
    editItemName.value = item.metadata?.title || '';
    editItemExpires.value = item.expires_at ? item.expires_at.slice(0, 10) : '';
    openDialog(editItemModal);
    editItemName.focus();
}

async function saveItemMetadata(event) {
    event.preventDefault();
    hideError(editItemError);

    if (!editingItem) {
        return;
    }

    const title = editItemName.value.trim();
    if (!title) {
        showError(editItemError, t('Escribe un título.'));
        return;
    }

    editItemSubmit.disabled = true;
    setButtonText(editItemSubmit, t('Guardando…'));

    try {
        const expiresAt = editItemExpires.value || null;
        const metadata = {
            ...editingItem.metadata,
            title,
            expires_at: expiresAt,
        };
        const response = await fetch(`${itemsUrl}/${editingItem.id}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
            body: JSON.stringify({
                type: editingItem.type,
                encrypted_metadata: encryptJson(metadata),
                expires_at: expiresAt,
                version: editingItem.version,
            }),
        });

        if (response.status === 409) {
            throw new Error(t('El documento cambió en otro dispositivo. Vuelve a cargarlo.'));
        }

        if (!response.ok) {
            throw new Error(t('No se pudo guardar el documento.'));
        }

        closeDialog(editItemModal);
        editingItem = null;
        await loadItems();
        showToast(t('Documento actualizado.'));
    } catch (error) {
        showError(editItemError, error.message || t('No se pudo guardar el documento.'));
    } finally {
        editItemSubmit.disabled = false;
        setButtonText(editItemSubmit, t('Guardar cambios'));
    }
}

async function downloadItem(item, button) {
    if (!item.blob_id || !item.metadata) {
        return;
    }

    button.disabled = true;
    button.textContent = t('Descargando…');

    try {
        const response = await fetch(`${blobBase}/${item.blob_id}`, {
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo descargar el archivo.'));
        }

        const decrypted = decryptFile(new Uint8Array(await response.arrayBuffer()));
        const blob = new Blob([decrypted], {
            type: item.metadata.mime || 'application/octet-stream',
        });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = item.metadata.original_name || 'documento';
        anchor.click();
        setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (error) {
        showError(vaultError, error.message || t('No se pudo descargar el archivo.'));
    } finally {
        button.disabled = false;
        button.textContent = t('Descargar');
    }
}

async function deleteItem(item, button) {
    const confirmed = await confirmAction({
        title: t('¿Eliminar este documento?'),
        message: t('El documento se moverá a la papelera y dejará de estar disponible en tu bóveda.'),
        acceptLabel: t('Eliminar'),
    });

    if (!confirmed) {
        return;
    }

    button.disabled = true;
    try {
        const response = await fetch(`${itemsUrl}/${item.id}`, {
            method: 'DELETE',
            headers: {
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo eliminar el documento.'));
        }
        await loadItems();
        showToast(t('Movido a la papelera.'), {
            label: t('Deshacer'),
            onClick: () => restoreTrashItem(item.id),
        });
    } catch (error) {
        showError(vaultError, error.message || t('No se pudo eliminar el documento.'));
        button.disabled = false;
    }
}

function trashTypeLabel(item) {
    if (isCredentialItem(item)) {
        return t('Credencial');
    }
    if (isMedicalItem(item)) {
        return t('Carnet médico');
    }
    if (item.type === 'ticket' || item.type === 'card' || item.metadata?.pass) {
        return t('Pase');
    }
    return t('Documento');
}

function trashDateLabel(value) {
    if (!value) {
        return '';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return '';
    }
    return date.toLocaleDateString(dateLocale(), { day: 'numeric', month: 'short', year: 'numeric' });
}

async function loadTrash() {
    if (!trashList) {
        return;
    }

    hideError(trashError);
    trashList.replaceChildren();
    if (trashEmpty) {
        trashEmpty.hidden = true;
    }

    try {
        const response = await fetch(`${itemsUrl}?trashed=1`, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo cargar la papelera.'));
        }

        const body = await response.json();
        const entries = (body.data ?? []).map((item) => {
            let metadata = {};
            try {
                metadata = decryptJson(item.encrypted_metadata);
            } catch {
                metadata = { title: t('Documento ilegible') };
            }
            return { ...item, metadata };
        });

        if (entries.length === 0) {
            if (trashEmpty) {
                trashEmpty.hidden = false;
            }
            return;
        }

        for (const item of entries) {
            const card = el('li', 'mp-card flex flex-col gap-3 p-4');
            const head = el('div', 'flex items-start gap-3');
            const tile = el('span', 'flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-surface-container-high text-primary');
            tile.appendChild(icon('delete', 'text-[22px]'));
            head.appendChild(tile);

            const copy = el('div', 'min-w-0 flex-1');
            copy.appendChild(el('p', 'font-semibold text-on-surface', item.metadata?.title || t('Documento ilegible')));
            copy.appendChild(el('p', 'mt-1 text-sm text-on-surface-variant', `${trashTypeLabel(item)}${item.deleted_at ? ` · ${trashDateLabel(item.deleted_at)}` : ''}`));
            head.appendChild(copy);
            card.appendChild(head);

            const actions = el('div', 'flex flex-wrap gap-2');
            const restore = el('button', 'mp-btn mp-btn-secondary mp-btn-sm', t('Restaurar'));
            restore.type = 'button';
            restore.addEventListener('click', () => restoreTrashItem(item.id, restore));
            const purge = el('button', 'mp-btn mp-btn-danger mp-btn-sm', t('Eliminar para siempre'));
            purge.type = 'button';
            purge.addEventListener('click', () => purgeTrashItem(item, purge));
            actions.append(restore, purge);
            card.appendChild(actions);
            trashList.appendChild(card);
        }
    } catch (error) {
        showError(trashError, error.message || t('No se pudo cargar la papelera.'));
    }
}

async function restoreTrashItem(id, button = null) {
    if (button) {
        button.disabled = true;
    }
    try {
        const response = await fetch(`${itemsUrl}/${id}/restore`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo restaurar el elemento.'));
        }
        await loadItems();
        if (trashModal?.open) {
            await loadTrash();
        }
        showToast(t('Elemento restaurado.'));
    } catch (error) {
        showError(vaultError, error.message || t('No se pudo restaurar el elemento.'));
        if (button) {
            button.disabled = false;
        }
    }
}

async function purgeTrashItem(item, button) {
    const confirmed = await confirmAction({
        title: t('¿Eliminar para siempre?'),
        message: t('Esta acción no se puede deshacer. El elemento y su archivo se borrarán de tu bóveda y del servidor.'),
        acceptLabel: t('Eliminar para siempre'),
    });
    if (!confirmed) {
        return;
    }

    button.disabled = true;
    try {
        const response = await fetch(`${itemsUrl}/${item.id}/force`, {
            method: 'DELETE',
            headers: {
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
        });
        if (!response.ok) {
            throw new Error(t('No se pudo eliminar el elemento.'));
        }
        await loadTrash();
        showToast(t('Elemento eliminado definitivamente.'));
    } catch (error) {
        showError(trashError, error.message || t('No se pudo eliminar el elemento.'));
        button.disabled = false;
    }
}

function openBackup() {
    if (!backupModal || !masterKey) {
        return;
    }
    hideError(backupError);
    if (backupStatus) {
        backupStatus.hidden = true;
        backupStatus.textContent = '';
    }
    openDialog(backupModal);
}

function closeBackup() {
    closeDialog(backupModal);
}

function setBackupBusy(busy) {
    if (backupExportButton) {
        backupExportButton.disabled = busy;
    }
    if (backupImportButton) {
        backupImportButton.disabled = busy;
    }
}

async function backupExportClick() {
    hideError(backupError);
    const passphrase = backupPassphrase?.value || '';
    const confirmation = backupPassphraseConfirm?.value || '';
    if (passphrase.length < 12) {
        showError(backupError, t('Usa una frase de al menos 12 caracteres.'));
        return;
    }
    if (passphrase !== confirmation) {
        showError(backupError, t('Las frases no coinciden.'));
        return;
    }

    setBackupBusy(true);
    if (backupStatus) {
        backupStatus.hidden = false;
        backupStatus.textContent = t('Creando la copia cifrada…');
    }
    try {
        const count = await exportBackup(passphrase);
        if (backupStatus) {
            backupStatus.textContent = count === 1
                ? t('Copia creada con 1 elemento.')
                : tf('Copia creada con :count elementos.', { count });
        }
        if (backupPassphrase) {
            backupPassphrase.value = '';
        }
        if (backupPassphraseConfirm) {
            backupPassphraseConfirm.value = '';
        }
    } catch (error) {
        showError(backupError, error.message || t('No se pudo crear la copia.'));
        if (backupStatus) {
            backupStatus.hidden = true;
        }
    } finally {
        setBackupBusy(false);
    }
}

async function backupImportClick() {
    hideError(backupError);
    const file = backupFile?.files?.[0];
    const passphrase = backupRestorePassphrase?.value || '';
    if (!file) {
        showError(backupError, t('Elige el archivo de la copia.'));
        return;
    }
    if (!passphrase) {
        showError(backupError, t('Escribe la frase de la copia.'));
        return;
    }

    const confirmed = await confirmAction({
        title: t('Restaurar una copia'),
        message: t('Los elementos de la copia se agregarán a tu bóveda. No se borrará lo que ya tienes.'),
        acceptLabel: t('Restaurar'),
    });
    if (!confirmed) {
        return;
    }

    setBackupBusy(true);
    if (backupStatus) {
        backupStatus.hidden = false;
        backupStatus.textContent = t('Restaurando la copia…');
    }
    try {
        const result = await importBackup(file, passphrase);
        await loadItems();
        if (backupStatus) {
            backupStatus.textContent = result.failed === 0
                ? tf('Se restauraron :count elementos.', { count: result.imported })
                : tf('Se restauraron :imported elementos; :failed no se pudieron leer.', result);
        }
        if (backupFile) {
            backupFile.value = '';
        }
        if (backupRestorePassphrase) {
            backupRestorePassphrase.value = '';
        }
    } catch (error) {
        showError(backupError, error.message || t('No se pudo restaurar la copia. Revisa la frase y el archivo.'));
        if (backupStatus) {
            backupStatus.hidden = true;
        }
    } finally {
        setBackupBusy(false);
    }
}

function openTrash() {
    if (!trashModal) {
        return;
    }
    openDialog(trashModal);
    void loadTrash();
}

function closeTrash() {
    closeDialog(trashModal);
}

async function uploadEncryptedBytes(encrypted) {
    const formData = new FormData();
    formData.append('file', new Blob([encrypted], { type: 'application/octet-stream' }), 'blob.bin');

    const response = await fetch(blobsUrl, {
        method: 'POST',
        headers: {
            Accept: 'application/json',
            'X-CSRF-TOKEN': csrfToken,
        },
        credentials: 'same-origin',
        body: formData,
    });

    if (!response.ok) {
        throw new Error(t('No se pudo subir el archivo cifrado.'));
    }

    return (await response.json()).data;
}

async function uploadFile(file) {
    const bytes = new Uint8Array(await file.arrayBuffer());
    return uploadEncryptedBytes(encryptFile(bytes));
}

async function addItem(event) {
    event.preventDefault();
    hideError(addError);
    addStatus.textContent = '';

    const title = document.getElementById('item-title').value.trim();
    const type = document.getElementById('item-type').value;
    const fileInput = document.getElementById('item-file');
    const expires = document.getElementById('item-expires').value;
    const file = fileInput.files?.[0];

    if (!title) {
        showError(addError, t('Escribe un título.'));
        return;
    }

    addSubmit.disabled = true;
    setButtonText(addSubmit, t('Guardando…'));

    try {
        let blobId = null;
        let fileMetadata = {};

        if (file) {
            if (file.size > 100 * 1024 * 1024) {
                throw new Error(t('El archivo supera los 100 MB.'));
            }
            const blob = await uploadFile(file);
            blobId = blob.id;
            fileMetadata = {
                original_name: file.name,
                mime: file.type || 'application/octet-stream',
                size: file.size,
            };
        }

        const metadata = {
            title,
            type,
            expires_at: expires || null,
            ...fileMetadata,
        };

        const response = await fetch(itemsUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
            body: JSON.stringify({
                type,
                encrypted_metadata: encryptJson(metadata),
                blob_id: blobId,
                expires_at: expires || null,
            }),
        });

        if (!response.ok) {
            throw new Error(t('No se pudo guardar el documento.'));
        }

        addForm.reset();
        if (itemFileName) {
            itemFileName.textContent = t('PDF o imagen · máximo 100 MB');
        }
        addStatus.textContent = t('Documento guardado.');
        closeDialog(addDocumentModal);
        await loadItems();
        showToast(t('Documento guardado.'));
    } catch (error) {
        showError(addError, error.message || t('No se pudo guardar el documento.'));
    } finally {
        addSubmit.disabled = false;
        setButtonText(addSubmit, t('Guardar en mi bóveda'));
    }
}

function autoLockMinutes() {
    const fallback = easyMode ? '30' : '10';
    const value = Number.parseInt(localStorage.getItem(autoLockKey) ?? fallback, 10);
    return Number.isFinite(value) ? value : Number.parseInt(fallback, 10);
}

function markActivity() {
    lastActivityAt = Date.now();
}

function canAutoLock() {
    return Boolean(masterKey)
        && !wallet?.presentation?.open
        && !passGenerator?.ocrBusy
        && !document.querySelector('dialog[open]');
}

function checkAutoLock() {
    const minutes = autoLockMinutes();

    if (minutes <= 0 || !canAutoLock()) {
        return;
    }

    if (Date.now() - lastActivityAt >= minutes * 60_000) {
        lock(t('La bóveda se bloqueó por inactividad.'));
    }
}

function handleVisibilityChange() {
    if (document.hidden) {
        hiddenAt = Date.now();
        return;
    }

    const minutes = autoLockMinutes();
    const hiddenLimit = Math.min(Math.max(minutes, 1) * 60_000, 120_000);

    if (minutes > 0 && masterKey && hiddenAt && Date.now() - hiddenAt >= hiddenLimit) {
        lock(t('La bóveda se bloqueó mientras estabas fuera.'));
    }

    hiddenAt = null;
    markActivity();
}

function lock(reason = '') {
    if (masterKey) {
        sodium.memzero(masterKey);
    }
    if (identityPrivateKey) {
        sodium.memzero(identityPrivateKey);
    }
    masterKey = null;
    identityPrivateKey = null;
    identityPublicKey = null;
    acceptedContacts = [];
    items = [];
    outgoingShares = [];
    currentFilter = 'all';
    currentSearch = '';
    selectedDocumentId = null;
    flippedDocumentId = null;
    if (docSearch) {
        docSearch.value = '';
    }
    closeShareModal();
    closeDialog(addDocumentModal);
    closeDialog(editItemModal);
    closeDialog(manageAccessModal);
    closeDialog(confirmModal);
    closeDialog(trashModal);
    closeDialog(searchModal);
    closeDialog(backupModal);
    if (trashList) {
        trashList.replaceChildren();
    }
    if (searchResults) {
        searchResults.replaceChildren();
    }
    searchIndex = [];
    editingItem = null;
    if (toast) {
        toast.hidden = true;
    }
    itemList.replaceChildren();
    wallet?.reset();
    passGenerator?.reset();
    credentials?.reset();
    medical?.reset();
    setVaultStatus(false);
    unlocked.hidden = true;
    locked.hidden = false;
    passphraseInput.value = '';
    if (recoveryFile) {
        recoveryFile.value = '';
    }
    if (recoveryFileStatus) {
        recoveryFileStatus.textContent = '';
    }
    emergencyKeyInput.value = '';
    emergencyStatus.textContent = '';
    stopPairing();
    if (pairingCodeBox) {
        pairingCodeBox.hidden = true;
    }
    if (pairingCreate) {
        setButtonText(pairingCreate, t('Generar código'));
    }
    if (deviceList) {
        deviceList.replaceChildren();
    }
    if (deviceStatus) {
        deviceStatus.textContent = '';
    }
    if (requestList) {
        requestList.replaceChildren();
    }
    if (requestStatus) {
        requestStatus.textContent = '';
    }
    hideError(unlockError);
    hideError(recoveryFileError);
    hideError(emergencyError);
    hideError(pairingError);
    hideError(vaultError);
    lastActivityAt = Date.now();

    if (reason) {
        showError(unlockError, reason);
    }
}

function initialTab() {
    const tab = new URLSearchParams(window.location.search).get('tab');
    return ['docs', 'pases', 'generador', 'credentials', 'medical', 'family', 'devices'].includes(tab) ? tab : 'pases';
}

function setVaultStatus(open) {
    const label = document.getElementById('vault-status-label');
    if (label) {
        label.textContent = open ? t('Bóveda abierta') : t('Bóveda protegida');
    }
}

function updateNavState(tab) {
    const desktopTab = tab === 'generador' ? 'pases' : tab;
    const bottomTab = tab === 'generador'
        ? 'pases'
        : (['docs', 'credentials', 'medical', 'family', 'devices'].includes(tab) ? 'docs' : tab);

    document.querySelectorAll('[data-vault-tab]').forEach((link) => {
        const isBottom = link.classList.contains('mp-bottom-link');
        const active = link.dataset.vaultTab === (isBottom ? bottomTab : desktopTab);
        if (active) {
            link.setAttribute('aria-current', 'page');
        } else {
            link.removeAttribute('aria-current');
        }

        if (isBottom) {
            link.querySelector('.material-symbols-outlined')?.classList.toggle('mp-icon-filled', active);
        }
    });
}

function switchTab(tab) {
    const selectedTab = tab === 'generador' ? 'pases' : tab;
    document.querySelectorAll('[data-tab]').forEach((button) => {
        button.setAttribute('aria-selected', button.dataset.tab === selectedTab ? 'true' : 'false');
    });

    for (const panel of ['docs', 'passes', 'generador', 'credentials', 'medical', 'family', 'devices']) {
        const element = document.getElementById(`panel-${panel}`);
        if (element) {
            const panelKey = panel === 'passes' ? 'pases' : panel;
            element.hidden = panelKey !== tab;
        }
    }

    if (tab === 'pases') {
        wallet?.refresh();
    } else if (tab === 'credentials') {
        credentials?.render();
    } else if (tab === 'medical') {
        medical?.render();
    }

    updateNavState(tab);

    try {
        const url = new URL(window.location.href);
        url.searchParams.set('tab', tab);
        window.history.replaceState({}, '', url);
    } catch {
        // La navegación sigue funcionando aunque el navegador bloquee history.
    }

    if (window.innerWidth < 1024) {
        const anchor = document.getElementById('vault-app');
        if (anchor) {
            const headerHeight = document.querySelector('header')?.getBoundingClientRect().height ?? 72;
            const top = anchor.getBoundingClientRect().top + window.scrollY - headerHeight - 12;
            window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
        }
    }
}

async function saveVaultItem({
    itemId,
    newId,
    version,
    type,
    metadata,
    file,
    blobId,
    changedMessage = null,
    saveMessage = null,
}) {
    let finalBlobId = blobId ?? null;

    if (file) {
        const blob = await uploadFile(file);
        finalBlobId = blob.id;
    } else if (blobId === undefined) {
        finalBlobId = null;
    }

    const response = await fetch(itemId ? `${itemsUrl}/${itemId}` : itemsUrl, {
        method: itemId ? 'PATCH' : 'POST',
        headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
            'X-CSRF-TOKEN': csrfToken,
        },
        credentials: 'same-origin',
        body: JSON.stringify({
            ...(itemId ? {} : { id: newId || undefined }),
            type,
            encrypted_metadata: encryptJson(metadata),
            blob_id: finalBlobId,
            expires_at: metadata.expires_at || null,
            ...(itemId ? { version } : {}),
        }),
    });

    if (!response.ok) {
        if (response.status === 409) {
            throw new Error(changedMessage || t('El elemento cambió en otro dispositivo. Vuelve a cargarlo.'));
        }
        throw new Error(saveMessage || t('No se pudo guardar el elemento.'));
    }

    const data = (await response.json()).data;
    await loadItems();
    return data;
}

async function savePass(payload) {
    return saveVaultItem({
        ...payload,
        changedMessage: t('El pase cambió en otro dispositivo. Vuelve a cargarlo.'),
        saveMessage: t('No se pudo guardar el pase.'),
    });
}

function updateSharePendingBanner() {
    if (!sharePending || !sharePendingText) {
        return;
    }

    const count = pendingSharedFiles.length;
    sharePending.hidden = count === 0 || Boolean(masterKey);
    if (count === 0) {
        return;
    }

    sharePendingText.textContent = count === 1
        ? t('Tienes 1 archivo compartido. Se guardará cuando abras la bóveda.')
        : tf('Tienes :count archivos compartidos. Se guardarán cuando abras la bóveda.', { count });
}

async function requestSharedFiles() {
    if (shareRequestInFlight || !('serviceWorker' in navigator)) {
        return;
    }

    shareRequestInFlight = true;
    try {
        const registration = await navigator.serviceWorker.ready;
        const target = navigator.serviceWorker.controller || registration.active;
        if (!target) {
            return;
        }

        const files = await new Promise((resolve) => {
            const channel = new MessageChannel();
            const timer = setTimeout(() => resolve([]), 1800);
            channel.port1.onmessage = (event) => {
                clearTimeout(timer);
                resolve(Array.isArray(event.data?.files) ? event.data.files : []);
            };
            target.postMessage({ type: 'mypass-share-request' }, [channel.port2]);
        });

        if (files.length > 0) {
            await receiveSharedFiles(files);
        } else if (new URLSearchParams(window.location.search).get('share') === '1') {
            showToast(t('No se recibieron los archivos. Vuelve a compartirlos desde la otra app.'));
        }
    } catch {
        // El share target solo funciona en HTTPS o localhost; el resto de la bóveda sigue igual.
    } finally {
        shareRequestInFlight = false;
    }
}

async function receiveSharedFiles(files) {
    pendingSharedFiles = files.slice(0, 20);
    const url = new URL(window.location.href);
    if (url.searchParams.has('share')) {
        url.searchParams.delete('share');
        window.history.replaceState({}, '', url.toString());
    }
    updateSharePendingBanner();

    if (masterKey) {
        await confirmAndSaveSharedFiles();
    }
}

async function confirmAndSaveSharedFiles() {
    if (!masterKey || pendingSharedFiles.length === 0) {
        return;
    }

    const count = pendingSharedFiles.length;
    const confirmed = await confirmAction({
        title: t('Guardar archivos compartidos'),
        message: count === 1
            ? t('El archivo se cifrará en este navegador antes de guardarse en tu bóveda.')
            : tf('Los :count archivos se cifrarán en este navegador antes de guardarse en tu bóveda.', { count }),
        acceptLabel: t('Guardar'),
    });

    if (!confirmed) {
        pendingSharedFiles = [];
        updateSharePendingBanner();
        return;
    }

    await saveSharedFilesWeb(pendingSharedFiles.splice(0));
}

async function saveSharedFilesWeb(files) {
    if (files.length === 0) {
        return;
    }

    let saved = 0;
    let failed = 0;

    for (const [index, file] of files.entries()) {
        showToast(tf('Guardando :current de :total…', { current: index + 1, total: files.length }));
        try {
            if (file.size > 100 * 1024 * 1024) {
                throw new Error(t('El archivo supera los 100 MB.'));
            }
            const blob = await uploadFile(file);
            const title = file.name.replace(/\.[^.]+$/, '').trim() || t('Documento');
            const metadata = {
                title,
                type: 'document',
                original_name: file.name,
                mime: file.type || 'application/octet-stream',
                size: file.size,
                expires_at: null,
            };
            const response = await fetch(itemsUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'application/json',
                    'X-CSRF-TOKEN': csrfToken,
                },
                credentials: 'same-origin',
                body: JSON.stringify({
                    type: 'document',
                    encrypted_metadata: encryptJson(metadata),
                    blob_id: blob.id,
                    expires_at: null,
                }),
            });
            if (!response.ok) {
                throw new Error(t('No se pudo guardar el documento.'));
            }
            saved += 1;
        } catch {
            failed += 1;
        }
    }

    await loadItems();
    if (saved > 0 && failed === 0) {
        showToast(saved === 1 ? t('Archivo guardado en tu bóveda.') : tf(':count archivos guardados en tu bóveda.', { count: saved }));
    } else if (saved > 0) {
        showToast(tf(':saved archivos guardados; :failed no se pudieron leer.', { saved, failed }));
    } else {
        showToast(t('No se pudieron guardar los archivos compartidos.'));
    }
}

async function initShareTarget() {
    if (!('serviceWorker' in navigator)) {
        return;
    }

    try {
        await navigator.serviceWorker.register('/sw.js', { scope: '/' });
        navigator.serviceWorker.addEventListener('message', (event) => {
            if (event.data?.type === 'mypass-share-ready') {
                void requestSharedFiles();
            }
        });

        const mode = new URLSearchParams(window.location.search).get('share');
        if (mode === '1') {
            void requestSharedFiles();
        } else if (mode === 'error') {
            showToast(t('No se pudieron leer los archivos compartidos.'));
        }
    } catch {
        // La PWA es opcional; la bóveda funciona sin service worker.
    }
}

async function init() {
    await sodium.ready;
    void initShareTarget();
    await refreshTrustedDeviceCard();

    try {
        const keys = await fetchKeys();
        loading.hidden = true;

        if (!keys) {
            noKeys.hidden = false;
            return;
        }

        locked.hidden = false;
    } catch (error) {
        loading.hidden = true;
        showError(vaultError, error.message || t('No se pudo cargar la bóveda.'));
        noKeys.hidden = false;
    }
}

if (app) {
    wallet = new WalletApp({
        canShare: () => acceptedContacts.length > 0,
        openShare: (item) => openShareModal(item),
        openGenerator: (category, item, pass) => passGenerator?.open(category, item, pass),
        toast: (message) => showToast(message),
    });

    passGenerator = new PassGenerator({
        switchTab: (tab) => switchTab(tab),
        savePass,
        canShare: () => acceptedContacts.length > 0,
        getItems: () => items,
        openShare: (item) => openShareModal(item),
        toast: (message) => showToast(message),
    });

    credentials = new CredentialsApp({
        saveItem: (payload) => saveVaultItem({
            ...payload,
            changedMessage: t('La credencial cambió en otro dispositivo. Vuelve a cargarla.'),
            saveMessage: t('No se pudo guardar la credencial.'),
        }),
        toast: (message) => showToast(message),
        downloadItem: (item, button) => downloadItem(item, button),
        deleteItem: (item, button) => deleteItem(item, button),
    });

    medical = new MedicalApp({
        saveItem: (payload) => saveVaultItem({
            ...payload,
            changedMessage: t('El carnet cambió en otro dispositivo. Vuelve a cargarlo.'),
            saveMessage: t('No se pudo guardar el carnet médico.'),
        }),
        toast: (message) => showToast(message),
        canShare: () => acceptedContacts.length > 0,
        openShare: (item) => openShareModal(item),
        downloadItem: (item, button) => downloadItem(item, button),
        deleteItem: (item, button) => deleteItem(item, button),
    });

    document.addEventListener('click', (event) => {
        const link = event.target.closest('[data-vault-tab]');
        if (!link || unlocked.hidden || !masterKey) {
            return;
        }
        event.preventDefault();
        switchTab(link.dataset.vaultTab);
    });

    unlockForm?.addEventListener('submit', unlock);
    emergencyForm?.addEventListener('submit', unlockFromEmergencyKey);
    trustedUnlockButton?.addEventListener('click', unlockWithTrustedDevice);
    trustedForgetButton?.addEventListener('click', async () => {
        await deleteTrustedRecord().catch(() => {});
        await refreshTrustedDeviceCard();
        showToast(t('Este equipo ya no abrirá la bóveda automáticamente.'));
    });
    emailRecoverySend?.addEventListener('click', sendEmailRecoveryCode);
    emailRecoveryVerify?.addEventListener('click', verifyEmailRecoveryCode);
    emailRecoveryCode?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            verifyEmailRecoveryCode();
        }
    });
    addForm?.addEventListener('submit', addItem);
    lockButton?.addEventListener('click', () => lock());
    backupOpen?.addEventListener('click', openBackup);
    backupClose?.addEventListener('click', closeBackup);
    backupExportButton?.addEventListener('click', backupExportClick);
    backupImportButton?.addEventListener('click', backupImportClick);
    backupModal?.addEventListener('cancel', (event) => {
        event.preventDefault();
        closeBackup();
    });
    searchOpen?.addEventListener('click', openSearch);
    searchClose?.addEventListener('click', closeSearch);
    searchInput?.addEventListener('input', () => renderSearch(searchInput.value));
    searchModal?.addEventListener('cancel', (event) => {
        event.preventDefault();
        closeSearch();
    });
    document.addEventListener('keydown', (event) => {
        if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
            event.preventDefault();
            openSearch();
        }
    });
    window.addEventListener('mypass:item-open', (event) => {
        recordRecent(event.detail?.id);
        if (searchModal?.open) {
            renderSearch(searchInput?.value || '');
        }
    });
    trashOpen?.addEventListener('click', openTrash);
    trashClose?.addEventListener('click', closeTrash);
    trashModal?.addEventListener('cancel', (event) => {
        event.preventDefault();
        closeTrash();
    });
    document.addEventListener('pointerdown', markActivity, { passive: true });
    document.addEventListener('keydown', markActivity);
    document.addEventListener('wheel', markActivity, { passive: true });
    document.addEventListener('visibilitychange', handleVisibilityChange);
    setInterval(checkAutoLock, 30_000);
    pairingCreate?.addEventListener('click', createPairing);
    contactForm?.addEventListener('submit', addContact);
    requestForm?.addEventListener('submit', createRequest);
    shareSend?.addEventListener('click', sendShare);
    shareCancel?.addEventListener('click', closeShareModal);
    shareModalClose?.addEventListener('click', closeShareModal);
    shareModal?.addEventListener('close', () => {
        sharingItem = null;
        hideError(shareSendError);
    });
    addDocumentOpen?.addEventListener('click', () => {
        hideError(addError);
        addStatus.textContent = '';
        openDialog(addDocumentModal);
    });
    document.getElementById('item-file')?.addEventListener('change', (event) => {
        const file = event.currentTarget.files?.[0];
        if (itemFileName) {
            itemFileName.textContent = file ? file.name : t('PDF o imagen · máximo 100 MB');
        }
    });
    addDocumentClose?.addEventListener('click', () => closeDialog(addDocumentModal));
    addDocumentCancel?.addEventListener('click', () => closeDialog(addDocumentModal));
    editItemForm?.addEventListener('submit', saveItemMetadata);
    editItemClose?.addEventListener('click', () => closeDialog(editItemModal));
    editItemCancel?.addEventListener('click', () => closeDialog(editItemModal));
    manageAccessClose?.addEventListener('click', () => closeDialog(manageAccessModal));
    manageAccessDone?.addEventListener('click', () => closeDialog(manageAccessModal));
    pairingCopy?.addEventListener('click', async () => {
        if (!pairingCode?.textContent) {
            return;
        }
        try {
            await navigator.clipboard.writeText(pairingCode.textContent.replace(/\s/g, ''));
            showToast(t('Código copiado.'));
        } catch {
            showToast(t('No se pudo copiar el código.'));
        }
    });
    document.querySelectorAll('[data-tab]').forEach((button) => {
        button.addEventListener('click', () => switchTab(button.dataset.tab));
        button.addEventListener('keydown', (event) => {
            if (!['ArrowLeft', 'ArrowRight'].includes(event.key)) {
                return;
            }

            event.preventDefault();
            const tabs = Array.from(document.querySelectorAll('[data-tab]'));
            const index = tabs.indexOf(button);
            const next = event.key === 'ArrowRight'
                ? tabs[(index + 1) % tabs.length]
                : tabs[(index - 1 + tabs.length) % tabs.length];
            next.focus();
            switchTab(next.dataset.tab);
        });
    });
    document.querySelectorAll('[data-filter]').forEach((button) => {
        button.addEventListener('click', () => {
            currentFilter = button.dataset.filter;
            renderItems();
        });
    });
    docSearch?.addEventListener('input', () => {
        currentSearch = docSearch.value;
        renderItems();
    });
    for (const dialog of [addDocumentModal, editItemModal, shareModal, manageAccessModal]) {
        dialog?.addEventListener('click', (event) => {
            if (event.target === dialog) {
                closeDialog(dialog);
            }
        });
    }
    setupConfirmDialog();
    setupRecoveryDropZone();
    init();
}
