(() => {
const canUsePush = 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;

if (canUsePush) {
    document.querySelectorAll('form[action$="/logout"]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (form.dataset.pushCleanup === 'done') {
                return;
            }

            event.preventDefault();
            const csrf = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content') || '';

            (async () => {
                try {
                    const registration = await Promise.race([
                        navigator.serviceWorker.ready,
                        new Promise((resolve) => window.setTimeout(() => resolve(null), 1500)),
                    ]);
                    const subscription = registration ? await registration.pushManager.getSubscription() : null;

                    if (subscription) {
                        await fetch('/api/web/push-subscriptions', {
                            method: 'DELETE',
                            headers: {
                                'Content-Type': 'application/json',
                                Accept: 'application/json',
                                'X-CSRF-TOKEN': csrf,
                            },
                            credentials: 'same-origin',
                            body: JSON.stringify({ endpoint: subscription.endpoint }),
                        });
                        await subscription.unsubscribe();
                    }
                } catch {
                    // El cierre de sesión no debe bloquearse por los avisos.
                } finally {
                    form.dataset.pushCleanup = 'done';
                    form.submit();
                }
            })();
        });
    });
}

const pushCard = document.getElementById('push-notifications');

if (pushCard) {
    const vapidMeta = document.querySelector('meta[name="vapid-public-key"]');
    const vapidKey = vapidMeta?.getAttribute('content')?.trim() || '';
    const status = document.getElementById('push-status');
    const enable = document.getElementById('push-enable');
    const disable = document.getElementById('push-disable');
    const csrf = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content') || '';

    const t = (key) => (window.mypassT ? window.mypassT(key) : key);

    const setStatus = (message) => {
        if (status) {
            status.textContent = message;
        }
    };

    const showButtons = (subscribed) => {
        if (enable) {
            enable.hidden = subscribed;
        }
        if (disable) {
            disable.hidden = !subscribed;
        }
    };

    if (!vapidKey || !('serviceWorker' in navigator) || !('PushManager' in window) || !('Notification' in window)) {
        return;
    }

    pushCard.hidden = false;

    const base64UrlToBytes = (value) => {
        const padding = '='.repeat((4 - (value.length % 4)) % 4);
        const base64 = (value + padding).replace(/-/g, '+').replace(/_/g, '/');
        const raw = window.atob(base64);
        return Uint8Array.from([...raw].map((character) => character.charCodeAt(0)));
    };

    const registerSubscription = async (subscription) => {
        const response = await fetch('/api/web/push-subscriptions', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                'X-CSRF-TOKEN': csrf,
            },
            credentials: 'same-origin',
            body: JSON.stringify(subscription.toJSON()),
        });

        if (!response.ok) {
            throw new Error(t('No se pudo activar los avisos.'));
        }
    };

    const syncState = async () => {
        try {
            const registration = await navigator.serviceWorker.ready;
            const subscription = await registration.pushManager.getSubscription();
            showButtons(Boolean(subscription));

            if (Notification.permission === 'denied') {
                setStatus(t('Los avisos están bloqueados para este sitio en el navegador.'));
                if (enable) {
                    enable.hidden = true;
                }
                return;
            }

            if (subscription) {
                setStatus(t('Los avisos están activos en este dispositivo.'));
                await registerSubscription(subscription);
            } else if (Notification.permission === 'granted') {
                setStatus(t('Activa los avisos para recibir novedades con la app cerrada.'));
            } else {
                setStatus(t('Activa los avisos para recibir novedades con la app cerrada.'));
            }
        } catch (error) {
            setStatus(error.message || t('No se pudieron consultar los avisos.'));
        }
    };

    enable?.addEventListener('click', async () => {
        enable.disabled = true;
        setStatus(t('Solicitando permiso…'));

        try {
            const permission = await Notification.requestPermission();

            if (permission !== 'granted') {
                setStatus(t('No se concedió permiso para mostrar avisos.'));
                return;
            }

            const registration = await navigator.serviceWorker.ready;
            let subscription = await registration.pushManager.getSubscription();

            if (!subscription) {
                subscription = await registration.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: base64UrlToBytes(vapidKey),
                });
            }

            await registerSubscription(subscription);
            showButtons(true);
            setStatus(t('Avisos activados. Te avisaremos aunque la app esté cerrada.'));
        } catch (error) {
            setStatus(error.message || t('No se pudieron activar los avisos.'));
        } finally {
            enable.disabled = false;
        }
    });

    disable?.addEventListener('click', async () => {
        disable.disabled = true;

        try {
            const registration = await navigator.serviceWorker.ready;
            const subscription = await registration.pushManager.getSubscription();

            if (subscription) {
                await fetch('/api/web/push-subscriptions', {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json',
                        Accept: 'application/json',
                        'X-CSRF-TOKEN': csrf,
                    },
                    credentials: 'same-origin',
                    body: JSON.stringify({ endpoint: subscription.endpoint }),
                });
                await subscription.unsubscribe();
            }

            showButtons(false);
            setStatus(t('Avisos desactivados en este dispositivo.'));
        } catch (error) {
            setStatus(error.message || t('No se pudieron desactivar los avisos.'));
        } finally {
            disable.disabled = false;
        }
    });

    syncState();
}

})();
