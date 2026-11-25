const COOKIE_NAME = 'mypass_theme';
const VALID_PREFERENCES = new Set(['system', 'light', 'dark']);
const DARK_QUERY = window.matchMedia('(prefers-color-scheme: dark)');

const t = (key) => (window.mypassT ? window.mypassT(key) : key);

function currentPreference() {
    const value = document.documentElement.dataset.theme;

    return VALID_PREFERENCES.has(value) ? value : 'system';
}

function effectiveDark(preference) {
    return preference === 'dark' || (preference === 'system' && DARK_QUERY.matches);
}

function persistPreference(preference) {
    try {
        const secure = window.location.protocol === 'https:' ? '; Secure' : '';
        document.cookie = `${COOKIE_NAME}=${preference}; Max-Age=31536000; Path=/; SameSite=Lax${secure}`;
    } catch {
        // La preferencia sigue aplicada en esta vista aunque el navegador bloquee cookies.
    }
}

function syncControls(preference, dark) {
    document.querySelectorAll('[data-theme-option]').forEach((button) => {
        const active = button.dataset.themeOption === preference;
        button.setAttribute('aria-pressed', active ? 'true' : 'false');
        button.setAttribute('aria-checked', active ? 'true' : 'false');
    });

    document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
        button.setAttribute('aria-pressed', dark ? 'true' : 'false');
        button.setAttribute('aria-label', dark ? t('Cambiar a tema claro') : t('Cambiar a tema oscuro'));

        const icon = button.querySelector('[data-theme-icon]');
        if (icon) {
            icon.textContent = dark ? 'light_mode' : 'dark_mode';
        }
    });
}

function applyPreference(preference, { persist = false } = {}) {
    const choice = VALID_PREFERENCES.has(preference) ? preference : 'system';
    const dark = effectiveDark(choice);

    document.documentElement.dataset.theme = choice;
    document.documentElement.classList.toggle('dark', dark);
    document.documentElement.style.colorScheme = dark ? 'dark' : 'light';

    if (persist) {
        persistPreference(choice);
    }

    const themeColor = document.querySelector('meta[name="theme-color"]');
    if (themeColor) {
        themeColor.setAttribute('content', dark ? '#08120e' : '#012d1d');
    }

    syncControls(choice, dark);
    window.dispatchEvent(new CustomEvent('mypass:theme-change', {
        detail: { preference: choice, dark },
    }));
}

export function initTheme() {
    applyPreference(currentPreference());

    const handleSystemChange = () => {
        if (currentPreference() === 'system') {
            applyPreference('system');
        }
    };

    if (typeof DARK_QUERY.addEventListener === 'function') {
        DARK_QUERY.addEventListener('change', handleSystemChange);
    } else {
        DARK_QUERY.addListener(handleSystemChange);
    }

    document.addEventListener('click', (event) => {
        const option = event.target.closest('[data-theme-option]');
        if (option) {
            applyPreference(option.dataset.themeOption, { persist: true });
            return;
        }

        const toggle = event.target.closest('[data-theme-toggle]');
        if (toggle) {
            const dark = document.documentElement.classList.contains('dark');
            applyPreference(dark ? 'light' : 'dark', { persist: true });
        }
    });
}
