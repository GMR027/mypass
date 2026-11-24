import sodium from 'libsodium-wrappers-sumo';

const t = (key) => window.mypassT ? window.mypassT(key) : key;
const tf = (key, values) => window.mypassTF ? window.mypassTF(key, values) : key;

const app = document.getElementById('onboarding-app');
const loading = document.getElementById('onboarding-loading');
const existing = document.getElementById('onboarding-existing');
const form = document.getElementById('onboarding-form');
const errorBox = document.getElementById('onboarding-error');
const recoverySection = document.getElementById('onboarding-recovery');
const recoveryCode = document.getElementById('recovery-code');
const recoveryDescription = document.getElementById('recovery-description');
const recoveryDownload = document.getElementById('recovery-download');
const recoveryPrint = document.getElementById('recovery-print');
const recoveryCopy = document.getElementById('recovery-copy');
const recoveryConfirm = document.getElementById('recovery-confirm');
const recoveryConfirmLabel = document.getElementById('recovery-confirm-label');
const recoverySave = document.getElementById('recovery-save');
const recoveryError = document.getElementById('recovery-error');
const recoveryLength = document.getElementById('recovery-length');
const recoveryLengthWarning = document.getElementById('recovery-length-warning');
const emailRecoveryOptIn = document.getElementById('email-recovery-opt-in');
const done = document.getElementById('onboarding-done');

const csrfToken = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
const keysUrl = app?.dataset.keysUrl;
const saveUrl = app?.dataset.saveUrl;

let pendingPayload = null;
let recoveryDigits = null;

const toB64 = (bytes) => sodium.to_base64(bytes, sodium.base64_variants.ORIGINAL);

function seal(message, key) {
    const nonce = sodium.randombytes_buf(sodium.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES);
    const ciphertext = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
        message,
        new Uint8Array(0),
        null,
        nonce,
        key,
    );

    return { nonce, ciphertext };
}

function formatDigits(digits) {
    return digits.match(/.{1,4}/g)?.join(' ') ?? digits;
}

function combined(nonce, ciphertext) {
    const bytes = new Uint8Array(nonce.length + ciphertext.length);
    bytes.set(nonce, 0);
    bytes.set(ciphertext, nonce.length);

    return bytes;
}

function showError(element, message) {
    if (!element) {
        return;
    }

    element.textContent = message;
    element.hidden = false;
}

function hideError(element) {
    if (element) {
        element.hidden = true;
    }
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

function setStep(activeStep) {
    document.querySelectorAll('.mp-step[data-step]').forEach((step) => {
        const stepNumber = Number(step.dataset.step);
        step.dataset.active = stepNumber === activeStep ? 'true' : 'false';
        step.dataset.done = stepNumber < activeStep ? 'true' : 'false';
    });
}

async function load() {
    await sodium.ready;

    try {
        const response = await fetch(keysUrl, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
        });

        if (!response.ok) {
            throw new Error(t('No se pudo consultar la bóveda.'));
        }

        const body = await response.json();
        loading.hidden = true;

        if (body.data) {
            existing.hidden = false;
            setStep(3);
            return;
        }

        form.hidden = false;
        setStep(1);
    } catch (error) {
        loading.hidden = true;
        showError(errorBox, error.message || t('No se pudo cargar la bóveda.'));
    }
}

async function createVault(event) {
    event.preventDefault();
    hideError(errorBox);

    const passphrase = document.getElementById('passphrase').value;
    const confirmation = document.getElementById('passphrase-confirmation').value;
    const digitCount = Number.parseInt(recoveryLength.value, 10);

    if (passphrase.length < 12) {
        showError(errorBox, t('La llave de la bóveda debe tener al menos 12 caracteres.'));
        return;
    }

    if (passphrase !== confirmation) {
        showError(errorBox, t('Las dos llaves no coinciden.'));
        return;
    }

    const button = document.getElementById('onboarding-create');
    button.disabled = true;
    setButtonText(button, t('Generando…'));

    try {
        await sodium.ready;

        const masterKey = sodium.randombytes_buf(32);
        const pcSalt = sodium.randombytes_buf(sodium.crypto_pwhash_SALTBYTES);
        const opsLimit = sodium.crypto_pwhash_OPSLIMIT_INTERACTIVE;
        const memLimit = sodium.crypto_pwhash_MEMLIMIT_INTERACTIVE;
        const pcKek = sodium.crypto_pwhash(
            32,
            sodium.from_string(passphrase),
            pcSalt,
            opsLimit,
            memLimit,
            sodium.crypto_pwhash_ALG_ARGON2ID13,
        );

        const pcWrapped = seal(masterKey, pcKek);
        const pcWrappedCombined = new Uint8Array(pcWrapped.nonce.length + pcWrapped.ciphertext.length);
        pcWrappedCombined.set(pcWrapped.nonce, 0);
        pcWrappedCombined.set(pcWrapped.ciphertext, pcWrapped.nonce.length);

        recoveryDigits = Array.from(
            { length: digitCount },
            () => sodium.randombytes_uniform(10),
        ).join('');

        const recoverySalt = sodium.randombytes_buf(sodium.crypto_pwhash_SALTBYTES);
        const recoveryKek = sodium.crypto_pwhash(
            32,
            sodium.from_string(recoveryDigits),
            recoverySalt,
            opsLimit,
            memLimit,
            sodium.crypto_pwhash_ALG_ARGON2ID13,
        );
        const recoveryWrapped = seal(masterKey, recoveryKek);

        const encryptionKeypair = sodium.crypto_box_keypair();
        const signingKeypair = sodium.crypto_sign_keypair();
        const privateKeys = sodium.from_string(JSON.stringify({
            encryption: toB64(encryptionKeypair.privateKey),
            signing: toB64(signingKeypair.privateKey),
        }));
        const privateKeysWrapped = seal(privateKeys, masterKey);
        const privateKeysCombined = new Uint8Array(
            privateKeysWrapped.nonce.length + privateKeysWrapped.ciphertext.length,
        );
        privateKeysCombined.set(privateKeysWrapped.nonce, 0);
        privateKeysCombined.set(privateKeysWrapped.ciphertext, privateKeysWrapped.nonce.length);

        pendingPayload = {
            pc_wrapped_master_key: toB64(pcWrappedCombined),
            pc_kdf_salt: toB64(pcSalt),
            pc_kdf_params: {
                algorithm: 'argon2id13',
                ops_limit: opsLimit,
                mem_limit: memLimit,
            },
            recovery_wrapped_master_key: toB64(recoveryWrapped.ciphertext),
            recovery_nonce: toB64(recoveryWrapped.nonce),
            recovery_kdf_salt: toB64(recoverySalt),
            recovery_kdf_params: {
                algorithm: 'argon2id13',
                ops_limit: opsLimit,
                mem_limit: memLimit,
            },
            recovery_version: 1,
            identity_public_encryption_key: toB64(encryptionKeypair.publicKey),
            identity_public_signing_key: toB64(signingKeypair.publicKey),
            identity_private_keys_encrypted: toB64(privateKeysCombined),
        };

        let emailRecoverySecret = null;

        if (emailRecoveryOptIn?.checked) {
            emailRecoverySecret = sodium.randombytes_buf(32);
            const emailWrapped = seal(masterKey, emailRecoverySecret);
            pendingPayload.email_recovery_secret = toB64(emailRecoverySecret);
            pendingPayload.email_recovery_wrapped_master_key = toB64(
                combined(emailWrapped.nonce, emailWrapped.ciphertext),
            );
            pendingPayload.email_recovery_version = 1;
            pendingPayload.email_recovery_enabled_at = new Date().toISOString();
        }

        recoveryCode.textContent = formatDigits(recoveryDigits);
        recoveryDescription.textContent = tf(
            'Esta clave de :count dígitos es la única forma de recuperar tu bóveda si pierdes la computadora y el celular. Escríbela en papel y guárdala en un lugar seguro.',
            { count: digitCount },
        );
        recoveryConfirmLabel.textContent = tf(
            'Confirmo que guardé la clave de :count dígitos en un lugar seguro y que entiendo que no se puede recuperar si la pierdo.',
            { count: digitCount },
        );

        sodium.memzero(pcKek);
        sodium.memzero(recoveryKek);
        sodium.memzero(encryptionKeypair.privateKey);
        sodium.memzero(signingKeypair.privateKey);
        if (emailRecoverySecret) {
            sodium.memzero(emailRecoverySecret);
        }
        sodium.memzero(masterKey);

        form.hidden = true;
        recoverySection.hidden = false;
        setStep(2);
    } catch (error) {
        console.error(error);
        showError(errorBox, t('No se pudo generar la bóveda. Inténtalo de nuevo.'));
        button.disabled = false;
        setButtonText(button, t('Crear bóveda'));
    }
}

function downloadRecovery() {
    if (!recoveryDigits || !pendingPayload) {
        showError(recoveryError, t('La llave de emergencia todavía no está lista.'));
        return;
    }

    const kit = {
        format: 'mypss',
        version: 1,
        type: 'recovery-key',
        account: app?.dataset.email || '',
        created_at: new Date().toISOString(),
        key: formatDigits(recoveryDigits),
        length: recoveryDigits.length,
        kdf: {
            algorithm: pendingPayload.recovery_kdf_params.algorithm,
            salt: pendingPayload.recovery_kdf_salt,
            ops_limit: pendingPayload.recovery_kdf_params.ops_limit,
            mem_limit: pendingPayload.recovery_kdf_params.mem_limit,
        },
        warning: t('Guarda este archivo como guardarías las escrituras de tu casa. No lo compartas. Si lo pierdes, no se puede recuperar la bóveda.'),
    };

    const blob = new Blob([JSON.stringify(kit, null, 2)], {
        type: 'application/json;charset=utf-8',
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'my-pass-recovery.mypss';
    anchor.click();
    URL.revokeObjectURL(url);
}

function printRecovery() {
    if (!recoveryDigits) {
        showError(recoveryError, t('La llave de emergencia todavía no está lista.'));
        return;
    }

    const printWindow = window.open('', '_blank');
    if (!printWindow) {
        showError(recoveryError, t('El navegador bloqueó la ventana de impresión.'));
        return;
    }

    printWindow.document.write(`
        <html lang="${window.mypassLocale === 'en' ? 'en' : 'es'}">
        <head><title>${t('My Pass — llave de emergencia')}</title></head>
        <body style="font-family: sans-serif; padding: 2rem;">
            <h1>${t('My Pass — llave de emergencia')}</h1>
            <p>${t('Guarda esta hoja en un lugar seguro. No la compartas.')}</p>
            <p style="font-family: monospace; font-size: 1.8rem; letter-spacing: 0.2rem;">
                ${formatDigits(recoveryDigits)}
            </p>
            <p>${t('Si pierdes esta llave y el acceso a tu bóveda, no se puede recuperar.')}</p>
        </body>
        </html>
    `);
    printWindow.document.close();
    printWindow.focus();
    printWindow.print();
}

async function copyRecovery() {
    if (!recoveryDigits) {
        return;
    }

    try {
        await navigator.clipboard.writeText(recoveryDigits);
        setButtonText(recoveryCopy, t('Copiada'));
        setTimeout(() => {
            setButtonText(recoveryCopy, t('Copiar clave'));
        }, 2000);
    } catch (error) {
        showError(recoveryError, t('No se pudo copiar. Escribe la clave desde la pantalla.'));
    }
}

async function saveVault() {
    hideError(recoveryError);

    if (!recoveryConfirm.checked) {
        showError(recoveryError, t('Confirma que guardaste la clave de emergencia.'));
        return;
    }

    if (!pendingPayload) {
        showError(recoveryError, t('No hay datos para guardar.'));
        return;
    }

    recoverySave.disabled = true;
    setButtonText(recoverySave, t('Guardando…'));

    try {
        const response = await fetch(saveUrl, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
            body: JSON.stringify(pendingPayload),
        });

        if (!response.ok) {
            throw new Error(t('El servidor no pudo guardar la bóveda.'));
        }

        recoverySection.hidden = true;
        done.hidden = false;
        setStep(3);
        recoveryDigits = null;
        pendingPayload = null;
    } catch (error) {
        console.error(error);
        showError(recoveryError, error.message || t('No se pudo guardar la bóveda.'));
        recoverySave.disabled = false;
        setButtonText(recoverySave, t('Terminar configuración'));
    }
}

if (app) {
    form?.addEventListener('submit', createVault);
    recoveryDownload?.addEventListener('click', downloadRecovery);
    recoveryPrint?.addEventListener('click', printRecovery);
    recoveryCopy?.addEventListener('click', copyRecovery);
    recoveryLength?.addEventListener('change', () => {
        recoveryLengthWarning.hidden = recoveryLength.value !== '16';
    });
    recoveryConfirm?.addEventListener('change', () => {
        recoverySave.disabled = !recoveryConfirm.checked;
    });
    recoverySave?.addEventListener('click', saveVault);
    load();
}
