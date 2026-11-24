const t = (key) => (window.mypassT ? window.mypassT(key) : key);
const tf = (key, values) => (window.mypassTF ? window.mypassTF(key, values) : key);

const ITEM_HEIGHT = 44;

function pad(value) {
    return String(value).padStart(2, '0');
}

function daysInMonth(year, month) {
    return new Date(year, month, 0).getDate();
}

function calculateAge(year, month, day) {
    const today = new Date();
    let age = today.getFullYear() - year;
    const hasHadBirthday = today.getMonth() + 1 > month
        || (today.getMonth() + 1 === month && today.getDate() >= day);

    if (!hasHadBirthday) {
        age -= 1;
    }

    return age;
}

function formatIso(year, month, day) {
    return `${year}-${pad(month)}-${pad(day)}`;
}

function parseIso(value) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value || ''));
    if (!match) {
        return null;
    }

    return {
        year: Number.parseInt(match[1], 10),
        month: Number.parseInt(match[2], 10),
        day: Number.parseInt(match[3], 10),
    };
}

function monthNames(locale) {
    const formatter = new Intl.DateTimeFormat(locale === 'en' ? 'en-US' : 'es-MX', {
        month: 'long',
    });

    return Array.from({ length: 12 }, (_, index) => {
        const name = formatter.format(new Date(2020, index, 1));

        return name.charAt(0).toUpperCase() + name.slice(1);
    });
}

class DateWheel {
    constructor(root) {
        this.root = root;
        this.name = root.dataset.name || 'birth_date';
        this.locale = root.dataset.locale || 'es-MX';
        this.minAge = Number.parseInt(root.dataset.minAge || '18', 10);
        this.maxAge = Number.parseInt(root.dataset.maxAge || '120', 10);
        this.seniorAge = Number.parseInt(root.dataset.seniorAge || '51', 10);
        this.hidden = root.querySelector('[data-wheel-input]');
        this.ageBox = root.querySelector('[data-wheel-age]');
        this.toggle = root.querySelector('[data-wheel-toggle]');
        this.nativeWrap = root.querySelector('[data-wheel-native]');
        this.native = root.querySelector('[data-wheel-native-input]');
        this.columns = {};
        this.state = {
            day: null,
            month: null,
            year: null,
        };
        this.timers = {};
        this.required = this.hidden?.required ?? false;
        this.nativeVisible = false;
        this.valid = false;
        this.months = monthNames(this.locale);
        this.currentYear = new Date().getFullYear();
        this.years = Array.from(
            { length: this.maxAge - this.minAge + 1 },
            (_, index) => this.currentYear - this.minAge - index,
        );

        for (const column of root.querySelectorAll('[data-wheel]')) {
            this.columns[column.dataset.wheel] = {
                element: column,
                items: [],
                index: 0,
            };
        }

        const initial = parseIso(root.dataset.value || this.hidden?.value || '');
        if (initial) {
            this.state = initial;
        }

        this.render();
        this.bind();
    }

    render() {
        this.renderColumn('day', [
            { value: '', label: '—' },
            ...Array.from({ length: 31 }, (_, index) => ({
                value: String(index + 1),
                label: String(index + 1),
            })),
        ]);

        this.renderColumn('month', [
            { value: '', label: '—' },
            ...this.months.map((label, index) => ({
                value: String(index + 1),
                label,
            })),
        ]);

        this.renderColumn('year', [
            { value: '', label: '—' },
            ...this.years.map((year) => ({
                value: String(year),
                label: String(year),
            })),
        ]);

        this.setInitialPositions();
        this.syncValue();
        this.updateAge();
    }

    centerSelectedColumns() {
        for (const key of ['day', 'month', 'year']) {
            const column = this.columns[key];
            if (!column) {
                continue;
            }

            column.element.scrollTop = column.index * ITEM_HEIGHT;
            const item = column.items[column.index];
            if (item) {
                column.element.setAttribute('aria-activedescendant', item.id);
            }
        }
    }

    renderColumn(key, entries) {
        const column = this.columns[key];
        if (!column) {
            return;
        }

        column.element.replaceChildren();
        column.items = entries.map((entry, index) => {
            const item = document.createElement('div');
            item.className = 'mp-date-wheel__item';
            item.textContent = entry.label;
            item.dataset.value = entry.value;
            item.id = `${this.root.id || this.name}-${key}-${index}`;
            item.setAttribute('role', 'option');
            item.setAttribute('aria-selected', 'false');
            item.addEventListener('click', () => this.scrollToIndex(key, index));
            column.element.appendChild(item);

            return item;
        });
    }

    setInitialPositions() {
        const state = this.state;
        let day = state.day;
        let month = state.month;
        let year = state.year;

        if (month && year && day) {
            day = Math.min(day, daysInMonth(year, month));
        }

        for (const key of ['day', 'month', 'year']) {
            const value = key === 'day' ? day : (key === 'month' ? month : year);
            const index = value
                ? this.columns[key].items.findIndex((item) => item.dataset.value === String(value))
                : 0;
            this.setIndex(key, index >= 0 ? index : 0, false);
        }

        requestAnimationFrame(() => this.centerSelectedColumns());
    }

    bind() {
        for (const key of ['day', 'month', 'year']) {
            const column = this.columns[key];
            if (!column) {
                continue;
            }

            column.element.addEventListener('scroll', () => {
                clearTimeout(this.timers[key]);
                this.timers[key] = setTimeout(() => this.commitScroll(key), 110);
            }, { passive: true });

            column.element.addEventListener('keydown', (event) => this.onKeydown(event, key));
            column.element.addEventListener('focus', () => this.markActive(key));
        }

        this.toggle?.addEventListener('click', () => this.toggleNative());

        this.native?.addEventListener('change', () => {
            const date = parseIso(this.native.value);

            if (!date) {
                this.state = { day: null, month: null, year: null };
            } else {
                this.state = date;
            }

            this.setInitialPositions();
            this.syncValue();
            this.updateAge();
        });

        const form = this.root.closest('form');
        form?.addEventListener('submit', (event) => {
            this.syncValue();

            if (!this.required || (this.hidden?.value && this.valid)) {
                return;
            }

            event.preventDefault();
            if (this.ageBox) {
                this.ageBox.textContent = this.hidden?.value
                    ? t('Revisa la fecha de nacimiento: debes tener al menos 18 años.')
                    : t('Elige tu fecha de nacimiento.');
                this.ageBox.classList.add('text-error');
                this.ageBox.classList.remove('text-primary');
            }
            this.root.scrollIntoView({ behavior: 'smooth', block: 'center' });
            this.columns.day?.element.focus({ preventScroll: true });
        });
    }

    scrollToIndex(key, index) {
        const column = this.columns[key];
        if (!column) {
            return;
        }

        column.element.scrollTo({
            top: index * ITEM_HEIGHT,
            behavior: 'smooth',
        });

        this.setIndex(key, index, true);
        this.syncValue();
        this.updateAge();
    }

    onKeydown(event, key) {
        const column = this.columns[key];
        if (!column) {
            return;
        }

        let next = column.index;

        switch (event.key) {
            case 'ArrowDown':
                next += 1;
                break;
            case 'ArrowUp':
                next -= 1;
                break;
            case 'PageDown':
                next += 5;
                break;
            case 'PageUp':
                next -= 5;
                break;
            case 'Home':
                next = 0;
                break;
            case 'End':
                next = column.items.length - 1;
                break;
            default:
                return;
        }

        event.preventDefault();
        next = Math.max(0, Math.min(next, column.items.length - 1));
        this.scrollToIndex(key, next);
        column.element.focus({ preventScroll: true });
    }

    commitScroll(key) {
        const column = this.columns[key];
        if (!column) {
            return;
        }

        const index = Math.max(0, Math.min(
            Math.round(column.element.scrollTop / ITEM_HEIGHT),
            column.items.length - 1,
        ));

        this.setIndex(key, index, true);
        this.syncValue();
        this.updateAge();
    }

    setIndex(key, index, updateScroll) {
        const column = this.columns[key];
        if (!column || !column.items.length) {
            return;
        }

        index = Math.max(0, Math.min(index, column.items.length - 1));
        column.index = index;

        column.items.forEach((item, itemIndex) => {
            const selected = itemIndex === index;
            item.dataset.selected = selected ? 'true' : 'false';
            item.setAttribute('aria-selected', selected ? 'true' : 'false');
        });

        if (updateScroll) {
            const item = column.items[index];
            if (item) {
                column.element.setAttribute('aria-activedescendant', item.id);
            }
        }

        const value = column.items[index]?.dataset.value ?? '';
        this.state[key] = value === '' ? null : Number.parseInt(value, 10);

        if (key === 'month' || key === 'year') {
            this.refreshDays();
        }

        if (key === 'day' || key === 'month' || key === 'year') {
            this.markActive(key);
        }
    }

    refreshDays() {
        const { year, month, day } = this.state;

        if (!year || !month) {
            return;
        }

        const max = daysInMonth(year, month);
        const column = this.columns.day;
        const selected = day && day <= max ? day : max;

        if (column.items.length !== max + 1) {
            this.renderColumn('day', [
                { value: '', label: '—' },
                ...Array.from({ length: max }, (_, index) => ({
                    value: String(index + 1),
                    label: String(index + 1),
                })),
            ]);
        }

        this.state.day = selected;
        const index = column.items.findIndex((item) => item.dataset.value === String(selected));
        this.setIndex('day', index >= 0 ? index : 0, false);
        column.element.scrollTop = column.index * ITEM_HEIGHT;
    }

    markActive(key) {
        for (const [name, column] of Object.entries(this.columns)) {
            column.element.dataset.active = name === key ? 'true' : 'false';
        }
    }

    syncValue() {
        if (!this.hidden) {
            return;
        }

        const { year, month, day } = this.state;

        if (year && month && day && day <= daysInMonth(year, month)) {
            this.hidden.value = formatIso(year, month, day);
            this.root.dataset.complete = 'true';
        } else {
            this.hidden.value = '';
            this.root.dataset.complete = 'false';
        }

        this.hidden.disabled = this.nativeVisible;
    }

    updateAge() {
        const { year, month, day } = this.state;

        if (!year || !month || !day) {
            this.valid = false;
            if (this.ageBox) {
                this.ageBox.textContent = t('Elige tu fecha de nacimiento.');
                this.ageBox.classList.remove('text-error', 'text-primary');
            }
            this.root.closest('form')?.classList.remove('mp-form-easy');
            return;
        }

        const age = calculateAge(year, month, day);

        if (age < this.minAge) {
            this.valid = false;
            if (this.ageBox) {
                this.ageBox.textContent = t('Debes tener al menos 18 años para crear una cuenta.');
                this.ageBox.classList.add('text-error');
                this.ageBox.classList.remove('text-primary');
            }
            this.root.closest('form')?.classList.remove('mp-form-easy');
            return;
        }

        if (age > this.maxAge) {
            this.valid = false;
            if (this.ageBox) {
                this.ageBox.textContent = t('Revisa la fecha de nacimiento: el año no parece correcto.');
                this.ageBox.classList.add('text-error');
                this.ageBox.classList.remove('text-primary');
            }
            this.root.closest('form')?.classList.remove('mp-form-easy');
            return;
        }

        this.valid = true;
        const isSenior = age >= this.seniorAge;

        if (this.ageBox) {
            this.ageBox.textContent = isSenior
                ? tf('Tienes :age años. Activaremos el modo fácil para ti.', { age })
                : tf('Tienes :age años.', { age });
            this.ageBox.classList.toggle('text-primary', isSenior);
            this.ageBox.classList.remove('text-error');
        }

        this.root.closest('form')?.classList.toggle('mp-form-easy', isSenior);
    }

    toggleNative() {
        if (!this.nativeWrap || !this.native) {
            return;
        }

        const willShow = this.nativeWrap.classList.contains('hidden');
        this.nativeVisible = willShow;
        this.nativeWrap.classList.toggle('hidden', !willShow);
        this.toggle?.setAttribute('aria-expanded', willShow ? 'true' : 'false');

        const label = this.toggle?.querySelector('span');
        if (label) {
            label.textContent = willShow
                ? t('Prefiero usar la rueda')
                : t('Prefiero escribir la fecha');
        }

        if (willShow) {
            this.native.name = this.name;
            this.native.required = this.required;
            if (this.hidden) {
                this.hidden.disabled = true;
            }

            if (this.hidden?.value) {
                this.native.value = this.hidden.value;
            }

            this.native.focus();
            return;
        }

        this.native.removeAttribute('name');
        this.native.required = false;
        if (this.hidden) {
            this.hidden.disabled = false;
        }
    }
}

export function initDateWheels() {
    document.querySelectorAll('[data-date-wheel]').forEach((root) => {
        new DateWheel(root);
    });
}
