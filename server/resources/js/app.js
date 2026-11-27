import { initTheme } from './theme';
import './push';

initTheme();

if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js', { scope: '/' }).catch(() => {
        // La PWA es opcional; la aplicación funciona sin service worker.
    });

    navigator.serviceWorker.addEventListener('message', (event) => {
        if (event.data?.type !== 'mypass-open-url' || !event.data.url) {
            return;
        }

        if (event.data.url !== window.location.href) {
            window.location.href = event.data.url;
        }
    });
}

if (document.body?.dataset.easy) {
    document.cookie = `mypass_easy=${document.body.dataset.easy}; Max-Age=31536000; Path=/; SameSite=Lax`;
}

document.addEventListener('click', (event) => {
    const easyToggle = event.target.closest('[data-easy-toggle]');

    if (!easyToggle) {
        return;
    }

    const enabled = !document.body.classList.contains('easy-mode');
    document.body.classList.toggle('easy-mode', enabled);
    document.body.classList.toggle('simple-mode', enabled);
    easyToggle.setAttribute('aria-pressed', enabled ? 'true' : 'false');

    const label = easyToggle.querySelector('[data-easy-toggle-label]');
    if (label) {
        label.textContent = enabled
            ? (window.mypassT ? window.mypassT('Modo normal') : 'Modo normal')
            : (window.mypassT ? window.mypassT('Modo fácil') : 'Modo fácil');
    }

    document.cookie = `mypass_easy=${enabled ? '1' : '0'}; Max-Age=31536000; Path=/; SameSite=Lax`;
});

document.addEventListener('click', (event) => {
    const toggle = event.target.closest('[data-toggle-password]');

    if (!toggle) {
        return;
    }

    const input = document.getElementById(toggle.dataset.togglePassword);

    if (!input) {
        return;
    }

    const show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    toggle.setAttribute('aria-pressed', show ? 'true' : 'false');

    const icon = toggle.querySelector('.material-symbols-outlined');
    if (icon) {
        icon.textContent = show ? 'visibility_off' : 'visibility';
    }
});

if (document.querySelector('[data-date-wheel]')) {
    import('./date-wheel')
        .then(({ initDateWheels }) => initDateWheels())
        .catch((error) => {
            console.error('No se pudo cargar el selector de fecha', error);
        });
}

if (document.getElementById('onboarding-app')) {
    import('./onboarding').catch((error) => {
        console.error('No se pudo cargar el onboarding', error);
    });
}

if (document.getElementById('vault-app')) {
    import('./vault').catch((error) => {
        console.error('No se pudo cargar la bóveda', error);
    });
}

if (document.getElementById('vault-security-app')) {
    import('./vault-security').catch((error) => {
        console.error('No se pudo cargar la seguridad de la bóveda', error);
    });
}

document.addEventListener('click', (event) => {
    const target = event.target.closest('[data-confirm]');

    if (!target) {
        return;
    }

    if (!window.confirm(target.dataset.confirm)) {
        event.preventDefault();
        event.stopImmediatePropagation();
    }
});
