import sodium from 'libsodium-wrappers-sumo';

const t = (key) => (window.mypassT ? window.mypassT(key) : key);
const tf = (key, values) => (window.mypassTF ? window.mypassTF(key, values) : key);

const root = document.getElementById('vault-security-app');

if (root) {
    const keysUrl = root.dataset.keysUrl;
    const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;
    const toB64 = (bytes) => sodium.to_base64(bytes, sodium.base64_variants.ORIGINAL);
    const fromB64 = (value) => sodium.from_base64(value, sodium.base64_variants.ORIGINAL);

    const changeForm = document.getElementById('vault-change-passphrase-form');
    const currentInput = document.getElementById('vault-current-passphrase');
    const newInput = document.getElementById('vault-new-passphrase');
    const confirmInput = document.getElementById('vault-new-passphrase-confirmation');
    const changeStatus = document.getElementById('vault-change-status');
    const changeError = document.getElementById('vault-change-error');

    const recoveryForm = document.getElementById('vault-recovery-form');
    const recoveryInput = document.getElementById('vault-recovery-passphrase');
    const recoveryLength = document.getElementById('vault-recovery-length');
    const recoveryStatus = document.getElementById('vault-recovery-status');
    const recoveryError = document.getElementById('vault-recovery-error');
    const recoveryOutput = document.getElementById('vault-recovery-output');
    const recoveryDigitsBox = document.getElementById('vault-recovery-digits');
    const recoveryCopy = document.getElementById('vault-recovery-copy');
    const recoveryDownload = document.getElementById('vault-recovery-download');

    let pendingRecovery = null;

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

    function openCombined(combined, key) {
        const bytes = fromB64(combined);
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

    function combined(nonce, ciphertext) {
        const bytes = new Uint8Array(nonce.length + ciphertext.length);
        bytes.set(nonce, 0);
        bytes.set(ciphertext, nonce.length);
        return bytes;
    }

    function formatDigits(digits) {
        return digits.match(/.{1,4}/g)?.join(' ') ?? digits;
    }

    async function fetchKeys() {
        const response = await fetch(keysUrl, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin',
        });

        if (!response.ok) {
            throw new Error(t('No se pudo consultar la bóveda.'));
        }

        return (await response.json()).data;
    }

    async function saveKeys(payload) {
        const response = await fetch(keysUrl, {
            method: 'PUT',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': csrfToken,
            },
            credentials: 'same-origin',
            body: JSON.stringify(payload),
        });

        if (!response.ok) {
            let message = t('No se pudieron guardar los cambios.');
            try {
                message = (await response.json()).message || message;
            } catch {
                // Keep the local fallback message.
            }
            throw new Error(message);
        }
    }

    async function unwrapMasterKey(passphrase, keys) {
        const params = keys.pc_kdf_params ?? {};
        const opsLimit = params.ops_limit;
        const memLimit = params.mem_limit;

        if (!keys.pc_wrapped_master_key || !keys.pc_kdf_salt || !opsLimit || !memLimit) {
            throw new Error(t('Esta bóveda no tiene una frase configurada.'));
        }

        const kek = sodium.crypto_pwhash(
            32,
            sodium.from_string(passphrase),
            fromB64(keys.pc_kdf_salt),
            opsLimit,
            memLimit,
            sodium.crypto_pwhash_ALG_ARGON2ID13,
        );

        try {
            return openCombined(keys.pc_wrapped_master_key, kek);
        } catch {
            throw new Error(t('La frase de la bóveda no es correcta.'));
        } finally {
            sodium.memzero(kek);
        }
    }

    function downloadKit(digits, salt, params) {
        const kit = {
            format: 'mypss',
            version: 1,
            type: 'recovery-key',
            account: root.dataset.email ?? '',
            created_at: new Date().toISOString(),
            key: formatDigits(digits),
            length: digits.length,
            kdf: {
                algorithm: 'argon2id13',
                salt: toB64(salt),
                ops_limit: params.ops_limit,
                mem_limit: params.mem_limit,
            },
            warning: t('Guarda este archivo como guardarías las escrituras de tu casa. No lo compartas. Si lo pierdes, no se puede recuperar la bóveda.'),
        };

        const blob = new Blob([JSON.stringify(kit, null, 2)], {
            type: 'application/json;charset=utf-8',
        });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = 'my-pass-llave-emergencia.mypss';
        anchor.click();
        setTimeout(() => URL.revokeObjectURL(url), 60000);
    }

    changeForm?.addEventListener('submit', async (event) => {
        event.preventDefault();
        hideError(changeError);
        changeStatus.textContent = t('Cambiando la frase…');

        const current = currentInput.value;
        const next = newInput.value;
        const confirmation = confirmInput.value;

        if (current.length < 12) {
            changeStatus.textContent = '';
            showError(changeError, t('Escribe tu frase actual.'));
            return;
        }

        if (next.length < 12) {
            changeStatus.textContent = '';
            showError(changeError, t('La frase nueva debe tener al menos 12 caracteres.'));
            return;
        }

        if (next !== confirmation) {
            changeStatus.textContent = '';
            showError(changeError, t('Las dos frases nuevas no coinciden.'));
            return;
        }

        let masterKey = null;

        try {
            await sodium.ready;
            const keys = await fetchKeys();
            masterKey = await unwrapMasterKey(current, keys);

            const salt = sodium.randombytes_buf(sodium.crypto_pwhash_SALTBYTES);
            const params = {
                algorithm: 'argon2id13',
                ops_limit: sodium.crypto_pwhash_OPSLIMIT_INTERACTIVE,
                mem_limit: sodium.crypto_pwhash_MEMLIMIT_INTERACTIVE,
            };
            const kek = sodium.crypto_pwhash(
                32,
                sodium.from_string(next),
                salt,
                params.ops_limit,
                params.mem_limit,
                sodium.crypto_pwhash_ALG_ARGON2ID13,
            );

            try {
                const wrapped = seal(masterKey, kek);
                await saveKeys({
                    pc_wrapped_master_key: toB64(combined(wrapped.nonce, wrapped.ciphertext)),
                    pc_kdf_salt: toB64(salt),
                    pc_kdf_params: params,
                });
            } finally {
                sodium.memzero(kek);
            }

            currentInput.value = '';
            newInput.value = '';
            confirmInput.value = '';
            changeStatus.textContent = t('Frase de la bóveda actualizada.');
        } catch (error) {
            changeStatus.textContent = '';
            showError(changeError, error.message || t('No se pudo cambiar la frase de la bóveda.'));
        } finally {
            if (masterKey) {
                sodium.memzero(masterKey);
            }
        }
    });

    recoveryForm?.addEventListener('submit', async (event) => {
        event.preventDefault();
        hideError(recoveryError);
        recoveryStatus.textContent = t('Generando llave nueva…');
        recoveryOutput.hidden = true;
        pendingRecovery = null;

        const passphrase = recoveryInput.value;
        const digitCount = Number.parseInt(recoveryLength.value, 10);

        if (passphrase.length < 12) {
            recoveryStatus.textContent = '';
            showError(recoveryError, t('Escribe tu frase actual.'));
            return;
        }

        let masterKey = null;

        try {
            await sodium.ready;
            const keys = await fetchKeys();
            masterKey = await unwrapMasterKey(passphrase, keys);

            const digits = Array.from(
                { length: digitCount },
                () => sodium.randombytes_uniform(10),
            ).join('');
            const salt = sodium.randombytes_buf(sodium.crypto_pwhash_SALTBYTES);
            const params = {
                algorithm: 'argon2id13',
                ops_limit: sodium.crypto_pwhash_OPSLIMIT_INTERACTIVE,
                mem_limit: sodium.crypto_pwhash_MEMLIMIT_INTERACTIVE,
            };
            const kek = sodium.crypto_pwhash(
                32,
                sodium.from_string(digits),
                salt,
                params.ops_limit,
                params.mem_limit,
                sodium.crypto_pwhash_ALG_ARGON2ID13,
            );

            try {
                const wrapped = seal(masterKey, kek);
                await saveKeys({
                    recovery_wrapped_master_key: toB64(wrapped.ciphertext),
                    recovery_nonce: toB64(wrapped.nonce),
                    recovery_kdf_salt: toB64(salt),
                    recovery_kdf_params: params,
                    recovery_version: (keys.recovery_version ?? 1) + 1,
                });
            } finally {
                sodium.memzero(kek);
            }

            pendingRecovery = { digits, salt, params };
            recoveryDigitsBox.textContent = formatDigits(digits);
            recoveryOutput.hidden = false;
            recoveryInput.value = '';
            recoveryStatus.textContent = t('Llave de emergencia regenerada. Guarda la nueva llave; la anterior ya no sirve.');
        } catch (error) {
            recoveryStatus.textContent = '';
            showError(recoveryError, error.message || t('No se pudo regenerar la llave de emergencia.'));
        } finally {
            if (masterKey) {
                sodium.memzero(masterKey);
            }
        }
    });

    recoveryCopy?.addEventListener('click', async () => {
        if (!pendingRecovery) {
            return;
        }

        try {
            await navigator.clipboard.writeText(pendingRecovery.digits);
            recoveryStatus.textContent = t('Llave copiada. Guárdala en un lugar seguro.');
            setTimeout(async () => {
                try {
                    const current = await navigator.clipboard.readText();
                    if (current === pendingRecovery?.digits) {
                        await navigator.clipboard.writeText('');
                    }
                } catch {
                    // Clipboard access can be denied; the key was still copied.
                }
            }, 60000);
        } catch {
            showError(recoveryError, t('No se pudo copiar la llave.'));
        }
    });

    recoveryDownload?.addEventListener('click', () => {
        if (!pendingRecovery) {
            return;
        }

        downloadKit(pendingRecovery.digits, pendingRecovery.salt, pendingRecovery.params);
        recoveryStatus.textContent = t('Archivo .mypss descargado. Guárdalo en un lugar seguro.');
    });
}
