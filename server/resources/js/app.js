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
