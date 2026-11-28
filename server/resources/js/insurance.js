import {
    formatDate,
    isExpired,
    newUuid,
} from './vault-secrets';
import { centerCardInView, createCompactCard, createFlipCard, setDeckFlipped, syncDeckHeight } from './card-deck';

const t = (key) => (window.mypassT ? window.mypassT(key) : key);
const tf = (key, values) => (window.mypassTF ? window.mypassTF(key, values) : key);
const locale = () => (window.mypassLocale === 'en' ? 'en-US' : 'es-MX');

const CATEGORIES = {
    health: { label: 'Salud', icon: 'health_and_safety' },
    major_medical: { label: 'Gastos médicos mayores', icon: 'medical_services' },
    auto: { label: 'Auto', icon: 'directions_car' },
    motorcycle: { label: 'Moto', icon: 'two_wheeler' },
    home: { label: 'Hogar', icon: 'home' },
    life: { label: 'Vida', icon: 'favorite' },
    travel: { label: 'Viaje', icon: 'flight' },
    pet: { label: 'Mascotas', icon: 'pets' },
    business: { label: 'Negocio', icon: 'business_center' },
    other: { label: 'Otro', icon: 'shield' },
};

const CATEGORY_ORDER = [
    'auto',
    'motorcycle',
    'major_medical',
    'health',
    'home',
    'travel',
    'life',
    'pet',
    'business',
    'other',
];

const CATEGORY_TEMPLATES = {
    health: [
        { label: t('Plan'), value: '' },
        { label: t('Red hospitalaria'), value: '' },
        { label: t('Dependientes'), value: '' },
    ],
    major_medical: [
        { label: t('Plan'), value: '' },
        { label: t('Red hospitalaria'), value: '' },
        { label: t('Deducible'), value: '' },
        { label: t('Coaseguro'), value: '' },
    ],
    auto: [
        { label: t('Vehículo'), value: '' },
        { label: t('Placas'), value: '' },
        { label: t('Serie / VIN'), value: '' },
        { label: t('Color'), value: '' },
        { label: t('Uso'), value: '' },
    ],
    motorcycle: [
        { label: t('Moto'), value: '' },
        { label: t('Placas'), value: '' },
        { label: t('Serie / VIN'), value: '' },
        { label: t('Cilindraje'), value: '' },
        { label: t('Color'), value: '' },
    ],
    home: [
        { label: t('Dirección'), value: '' },
        { label: t('Tipo de inmueble'), value: '' },
        { label: t('Contenido asegurado'), value: '' },
        { label: t('Hipoteca'), value: '' },
    ],
    life: [
        { label: t('Beneficiarios'), value: '' },
        { label: t('Suma asegurada'), value: '' },
    ],
    travel: [
        { label: t('Destino'), value: '' },
        { label: t('Fechas del viaje'), value: '' },
        { label: t('Pasajeros'), value: '' },
    ],
    pet: [
        { label: t('Mascota'), value: '' },
        { label: t('Especie'), value: '' },
        { label: t('Raza'), value: '' },
        { label: t('Microchip'), value: '' },
    ],
    business: [
        { label: t('Negocio'), value: '' },
        { label: t('Dirección'), value: '' },
        { label: t('Giro'), value: '' },
    ],
    other: [
        { label: t('Bien asegurado'), value: '' },
    ],
};

const CLAIM_STATUS = {
    open: { label: 'Abierto', tone: 'warning' },
    review: { label: 'En revisión', tone: 'primary' },
    approved: { label: 'Aprobado', tone: 'primary' },
    rejected: { label: 'Rechazado', tone: 'danger' },
    closed: { label: 'Cerrado', tone: 'neutral' },
};

const PAYMENT_FREQUENCY = {
    monthly: 'Mensual',
    quarterly: 'Trimestral',
    semiannual: 'Semestral',
    annual: 'Anual',
    single: 'Pago único',
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

function safeObject(value) {
    return value && typeof value === 'object' && !Array.isArray(value) ? value : {};
}

function stringValue(value) {
    return typeof value === 'string' ? value : '';
}

function stringList(value) {
    return Array.isArray(value) ? value.filter(Boolean).map(String) : [];
}

function telHref(value) {
    const sanitized = String(value || '').replace(/[^\d+*#]/g, '');
    return sanitized ? `tel:${sanitized}` : '';
}

function safeLink(value) {
    const text = String(value || '').trim();
    if (!text) {
        return '';
    }
    if (/^https?:\/\//i.test(text)) {
        return text;
    }
    if (/^[\w.-]+\.[a-z]{2,}/i.test(text)) {
        return `https://${text}`;
    }
    return '';
}

function categoryInfo(category) {
    return CATEGORIES[category] || CATEGORIES.other;
}

export function isInsuranceItem(item) {
    return item?.type === 'insurance' || Boolean(item?.metadata?.insurance);
}

export function insuranceFromItem(item) {
    const raw = safeObject(item?.metadata?.insurance);

    return {
        category: typeof raw.category === 'string' && CATEGORIES[raw.category] ? raw.category : 'other',
        provider: stringValue(raw.provider),
        product: stringValue(raw.product),
        policyNumber: stringValue(raw.policy_number),
        memberId: stringValue(raw.member_id),
        holder: stringValue(raw.holder),
        validFrom: stringValue(raw.valid_from),
        validUntil: stringValue(raw.valid_until),
        status: stringValue(raw.status) || 'active',
        emergencyPhone: stringValue(raw.emergency_phone),
        assistancePhone: stringValue(raw.assistance_phone),
        agentName: stringValue(raw.agent_name),
        agentPhone: stringValue(raw.agent_phone),
        email: stringValue(raw.email),
        website: stringValue(raw.website),
        premium: stringValue(raw.premium),
        currency: stringValue(raw.currency) || 'MXN',
        paymentFrequency: stringValue(raw.payment_frequency),
        nextPaymentDate: stringValue(raw.next_payment_date),
        insuredSum: stringValue(raw.insured_sum),
        deductible: stringValue(raw.deductible),
        subject: Array.isArray(raw.subject)
            ? raw.subject.filter((entry) => entry && typeof entry === 'object').map((entry) => ({
                id: typeof entry.id === 'string' ? entry.id : newUuid(),
                label: stringValue(entry.label),
                value: stringValue(entry.value),
            })).filter((entry) => entry.label || entry.value)
            : [],
        coverages: Array.isArray(raw.coverages)
            ? raw.coverages.filter((entry) => entry && typeof entry === 'object').map((entry) => ({
                id: typeof entry.id === 'string' ? entry.id : newUuid(),
                name: stringValue(entry.name),
                amount: stringValue(entry.amount),
                notes: stringValue(entry.notes),
            })).filter((entry) => entry.name || entry.amount || entry.notes)
            : [],
        notes: stringValue(raw.notes),
        reminderDays: Number.isFinite(Number(raw.reminder_days))
            ? Math.min(90, Math.max(1, Number(raw.reminder_days)))
            : 7,
        claims: Array.isArray(raw.claims)
            ? raw.claims.filter((entry) => entry && typeof entry === 'object').map((entry) => ({
                id: typeof entry.id === 'string' ? entry.id : newUuid(),
                date: stringValue(entry.date),
                claimNumber: stringValue(entry.claim_number),
                type: stringValue(entry.type),
                description: stringValue(entry.description),
                status: typeof entry.status === 'string' && CLAIM_STATUS[entry.status] ? entry.status : 'open',
                adjusterName: stringValue(entry.adjuster_name),
                adjusterPhone: stringValue(entry.adjuster_phone),
                amount: stringValue(entry.amount),
                notes: stringValue(entry.notes),
            }))
            : [],
    };
}

function insuranceStatus(item) {
    const insurance = insuranceFromItem(item);
    if (insurance.status === 'cancelled') {
        return { label: t('Cancelada'), tone: 'neutral', expired: true };
    }
    if (insurance.status === 'expired' || isExpired(item.expires_at) || isExpired(insurance.validUntil)) {
        return { label: t('Vencida'), tone: 'danger', expired: true };
    }
    if (insurance.status === 'pending') {
        return { label: t('Por renovar'), tone: 'warning', expired: false };
    }
    return { label: t('Vigente'), tone: 'primary', expired: false };
}

function claimStatusInfo(status) {
    return CLAIM_STATUS[status] || CLAIM_STATUS.open;
}

function activeClaim(insurance) {
    return insurance.claims
        .filter((claim) => claim.status === 'open' || claim.status === 'review')
        .sort((a, b) => `${b.date} ${b.claimNumber}`.localeCompare(`${a.date} ${a.claimNumber}`))[0] || null;
}

function sortPolicies(items) {
    return [...items].sort((a, b) => {
        const aInsurance = insuranceFromItem(a);
        const bInsurance = insuranceFromItem(b);
        const categoryDelta = CATEGORY_ORDER.indexOf(aInsurance.category) - CATEGORY_ORDER.indexOf(bInsurance.category);
        if (categoryDelta !== 0) {
            return categoryDelta;
        }
        return String(aInsurance.provider || a.metadata?.title || '')
            .localeCompare(String(bInsurance.provider || b.metadata?.title || ''), locale());
    });
}

function searchValues(item) {
    const insurance = insuranceFromItem(item);
    return [
        item.metadata?.title,
        item.metadata?.original_name,
        insurance.provider,
        insurance.product,
        insurance.policyNumber,
        insurance.memberId,
        insurance.holder,
        insurance.emergencyPhone,
        insurance.assistancePhone,
        insurance.agentName,
        insurance.deductible,
        insurance.notes,
        ...insurance.subject.flatMap((entry) => [entry.label, entry.value]),
        ...insurance.coverages.flatMap((entry) => [entry.name, entry.amount, entry.notes]),
        ...insurance.claims.flatMap((claim) => [
            claim.claimNumber,
            claim.type,
            claim.description,
            claim.adjusterName,
            claim.notes,
        ]),
    ].filter(Boolean).map(String);
}

function statusBadgeClass(tone) {
    return {
        primary: 'mp-badge-primary',
        warning: 'mp-badge-warning',
        danger: 'mp-badge-danger',
        neutral: 'mp-badge-neutral',
    }[tone] || 'mp-badge-primary';
}

function today() {
    return new Date().toISOString().slice(0, 10);
}

function parseInsuranceDate(value) {
    if (!value) {
        return null;
    }
    const [year, month, day] = String(value).slice(0, 10).split('-').map(Number);
    if (!year || !month || !day) {
        return null;
    }
    const date = new Date(year, month - 1, day, 23, 59, 59);
    return Number.isNaN(date.getTime()) ? null : date;
}

export class InsuranceApp {
    constructor(options) {
        this.options = options;
        this.items = [];
        this.search = '';
        this.filter = 'all';
        this.editing = null;
        this.baseMetadata = {};
        this.existingBlobId = null;
        this.clearAttachment = false;
        this.fileLabel = '';
        this.selectedInsuranceId = null;
        this.flippedInsuranceId = null;
        this.presentingItem = null;
        this.claimsItem = null;
        this.claimEditing = null;
        this.wakeLock = null;
        this.bind();
    }

    bind() {
        this.panel = document.getElementById('panel-insurance');
        if (!this.panel) {
            return;
        }

        this.list = document.getElementById('insurance-list');
        this.empty = document.getElementById('insurance-empty');
        this.emptyTitle = document.getElementById('insurance-empty-title');
        this.emptyCopy = document.getElementById('insurance-empty-copy');
        this.searchInput = document.getElementById('insurance-search');
        this.addButton = document.getElementById('insurance-add-open');
        this.emergencyButton = document.getElementById('insurance-emergency-open');

        this.modal = document.getElementById('insurance-modal');
        this.form = document.getElementById('insurance-form');
        this.modalTitle = document.getElementById('insurance-modal-title');
        this.error = document.getElementById('insurance-error');
        this.idInput = document.getElementById('insurance-item-id');
        this.versionInput = document.getElementById('insurance-item-version');
        this.blobInput = document.getElementById('insurance-blob-id');
        this.titleInput = document.getElementById('insurance-title');
        this.categoryInput = document.getElementById('insurance-category');
        this.statusInput = document.getElementById('insurance-status');
        this.providerInput = document.getElementById('insurance-provider');
        this.productInput = document.getElementById('insurance-product');
        this.policyInput = document.getElementById('insurance-policy');
        this.memberInput = document.getElementById('insurance-member');
        this.holderInput = document.getElementById('insurance-holder');
        this.validFromInput = document.getElementById('insurance-valid-from');
        this.validUntilInput = document.getElementById('insurance-valid-until');
        this.emergencyPhoneInput = document.getElementById('insurance-emergency-phone');
        this.assistancePhoneInput = document.getElementById('insurance-assistance-phone');
        this.agentNameInput = document.getElementById('insurance-agent-name');
        this.agentPhoneInput = document.getElementById('insurance-agent-phone');
        this.emailInput = document.getElementById('insurance-email');
        this.websiteInput = document.getElementById('insurance-website');
        this.premiumInput = document.getElementById('insurance-premium');
        this.currencyInput = document.getElementById('insurance-currency');
        this.paymentFrequencyInput = document.getElementById('insurance-payment-frequency');
        this.nextPaymentInput = document.getElementById('insurance-next-payment');
        this.insuredSumInput = document.getElementById('insurance-insured-sum');
        this.deductibleInput = document.getElementById('insurance-deductible');
        this.notesInput = document.getElementById('insurance-notes');
        this.reminderDaysInput = document.getElementById('insurance-reminder-days');
        this.reminderBanner = document.getElementById('insurance-reminder-banner');
        this.reminderList = document.getElementById('insurance-reminder-list');
        this.fileInput = document.getElementById('insurance-file');
        this.fileCurrent = document.getElementById('insurance-file-current');
        this.fileName = document.getElementById('insurance-file-name');
        this.fileClear = document.getElementById('insurance-file-clear');
        this.subjectList = document.getElementById('insurance-subject-list');
        this.subjectAdd = document.getElementById('insurance-subject-add');
        this.coveragesList = document.getElementById('insurance-coverages-list');
        this.coverageAdd = document.getElementById('insurance-coverage-add');
        this.saveButton = document.getElementById('insurance-save');
        this.closeButton = document.getElementById('insurance-close');
        this.cancelButton = document.getElementById('insurance-cancel');

        this.claimsModal = document.getElementById('insurance-claims-modal');
        this.claimsTitle = document.getElementById('insurance-claims-title');
        this.claimsSubtitle = document.getElementById('insurance-claims-subtitle');
        this.claimsError = document.getElementById('insurance-claims-error');
        this.claimsEmpty = document.getElementById('insurance-claims-empty');
        this.claimsList = document.getElementById('insurance-claims-list');
        this.claimsClose = document.getElementById('insurance-claims-close');
        this.claimsDone = document.getElementById('insurance-claims-done');
        this.claimAdd = document.getElementById('insurance-claim-add');

        this.claimModal = document.getElementById('insurance-claim-modal');
        this.claimForm = document.getElementById('insurance-claim-form');
        this.claimModalTitle = document.getElementById('insurance-claim-modal-title');
        this.claimError = document.getElementById('insurance-claim-error');
        this.claimId = document.getElementById('insurance-claim-id');
        this.claimDate = document.getElementById('insurance-claim-date');
        this.claimNumber = document.getElementById('insurance-claim-number');
        this.claimType = document.getElementById('insurance-claim-type');
        this.claimStatus = document.getElementById('insurance-claim-status');
        this.claimDescription = document.getElementById('insurance-claim-description');
        this.claimAdjusterName = document.getElementById('insurance-claim-adjuster-name');
        this.claimAdjusterPhone = document.getElementById('insurance-claim-adjuster-phone');
        this.claimAmount = document.getElementById('insurance-claim-amount');
        this.claimNotes = document.getElementById('insurance-claim-notes');
        this.claimSave = document.getElementById('insurance-claim-save');
        this.claimClose = document.getElementById('insurance-claim-close');
        this.claimCancel = document.getElementById('insurance-claim-cancel');

        this.presentation = document.getElementById('insurance-presentation');
        this.presentationClose = document.getElementById('insurance-presentation-close');
        this.presentationCategory = document.getElementById('insurance-presentation-category');
        this.presentationTitle = document.getElementById('insurance-presentation-title');
        this.presentationProvider = document.getElementById('insurance-presentation-provider');
        this.presentationExpired = document.getElementById('insurance-presentation-expired');
        this.presentationCall = document.getElementById('insurance-presentation-call');
        this.presentationCallLabel = document.getElementById('insurance-presentation-call-label');
        this.presentationAssistance = document.getElementById('insurance-presentation-assistance');
        this.presentationAssistanceLabel = document.getElementById('insurance-presentation-assistance-label');
        this.presentationPolicy = document.getElementById('insurance-presentation-policy');
        this.presentationCopyPolicy = document.getElementById('insurance-presentation-copy-policy');
        this.presentationGrid = document.getElementById('insurance-presentation-grid');
        this.presentationClaimBox = document.getElementById('insurance-presentation-claim-box');
        this.presentationClaim = document.getElementById('insurance-presentation-claim');
        this.presentationAdjuster = document.getElementById('insurance-presentation-adjuster');
        this.presentationNotesBox = document.getElementById('insurance-presentation-notes-box');
        this.presentationNotes = document.getElementById('insurance-presentation-notes');
        this.presentationOpenFile = document.getElementById('insurance-presentation-open-file');
        this.presentationCopySummary = document.getElementById('insurance-presentation-copy-summary');
        this.presentationShare = document.getElementById('insurance-presentation-share');

        this.emergency = document.getElementById('insurance-emergency');
        this.emergencyClose = document.getElementById('insurance-emergency-close');
        this.emergencyEmpty = document.getElementById('insurance-emergency-empty');
        this.emergencyList = document.getElementById('insurance-emergency-list');

        this.addButton?.addEventListener('click', () => this.openNew());
        this.emergencyButton?.addEventListener('click', () => this.openEmergency());
        this.closeButton?.addEventListener('click', () => this.close());
        this.cancelButton?.addEventListener('click', () => this.close());
        this.modal?.addEventListener('close', () => this.resetForm());
        this.form?.addEventListener('submit', (event) => this.save(event));
        this.subjectAdd?.addEventListener('click', () => this.addSubjectRow());
        this.coverageAdd?.addEventListener('click', () => this.addCoverageRow());
        this.subjectList?.addEventListener('click', (event) => {
            if (event.target.closest('[data-insurance-subject-remove]')) {
                event.target.closest('[data-insurance-subject-row]')?.remove();
            }
        });
        this.coveragesList?.addEventListener('click', (event) => {
            if (event.target.closest('[data-insurance-coverage-remove]')) {
                event.target.closest('[data-insurance-coverage-row]')?.remove();
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
        this.searchInput?.addEventListener('input', () => {
            this.search = this.searchInput.value;
            this.render();
        });
        this.panel.querySelectorAll('[data-insurance-filter]').forEach((button) => {
            button.addEventListener('click', () => {
                this.filter = button.dataset.insuranceFilter || 'all';
                this.render();
            });
        });

        this.claimsClose?.addEventListener('click', () => this.closeClaims());
        this.claimsDone?.addEventListener('click', () => this.closeClaims());
        this.claimAdd?.addEventListener('click', () => this.openClaimForm());
        this.claimClose?.addEventListener('click', () => this.closeClaimForm());
        this.claimCancel?.addEventListener('click', () => this.closeClaimForm());
        this.claimsList?.addEventListener('click', (event) => this.handleClaimsClick(event));
        this.claimForm?.addEventListener('submit', (event) => this.saveClaim(event));
        this.claimsModal?.addEventListener('close', () => {
            this.claimsItem = null;
            this.claimEditing = null;
        });

        this.presentationClose?.addEventListener('click', () => this.closePresentation());
        this.presentation?.addEventListener('close', () => this.releasePresentation());
        this.presentationCopyPolicy?.addEventListener('click', () => this.copyPolicyNumber());
        this.presentationCopySummary?.addEventListener('click', () => this.copySummary());
        this.presentationOpenFile?.addEventListener('click', () => {
            if (this.presentingItem) {
                this.options.openItem?.(this.presentingItem);
            }
        });
        this.presentationShare?.addEventListener('click', () => {
            if (this.presentingItem) {
                this.closePresentation();
                this.options.openShare?.(this.presentingItem);
            }
        });

        this.emergencyClose?.addEventListener('click', () => this.closeEmergency());
        this.emergency?.addEventListener('close', () => this.releasePresentation());
        this.emergencyList?.addEventListener('click', (event) => {
            const open = event.target.closest('[data-emergency-open]');
            if (!open) {
                return;
            }
            const item = this.items.find((candidate) => candidate.id === open.dataset.emergencyOpen);
            if (item) {
                this.closeEmergency();
                this.openPresentation(item);
            }
        });

        this.categoryInput?.addEventListener('change', () => {
            if (this.subjectList && this.readSubjectRows().every((entry) => !entry.value.trim())) {
                this.renderSubjectRows(CATEGORY_TEMPLATES[this.categoryInput.value] || []);
            }
        });
    }

    setItems(items) {
        this.items = (Array.isArray(items) ? items : []).filter(isInsuranceItem);
        this.render();
    }

    reset() {
        this.items = [];
        this.search = '';
        this.filter = 'all';
        this.selectedInsuranceId = null;
        this.flippedInsuranceId = null;
        this.closePresentation();
        this.closeEmergency();
        this.closeClaims();
        this.close();
        this.render();
    }

    inputField(label, type, options = {}) {
        const wrapper = el('label', 'block');
        wrapper.appendChild(el('span', 'mp-hint block', label));
        const input = document.createElement('input');
        input.type = type;
        input.className = 'mp-input mt-1';
        input.autocomplete = 'off';
        if (options.placeholder) {
            input.placeholder = options.placeholder;
        }
        if (options.maxlength) {
            input.maxLength = options.maxlength;
        }
        if (options.value) {
            input.value = options.value;
        }
        wrapper.appendChild(input);
        return { wrapper, input };
    }

    addSubjectRow(subject = {}) {
        if (!this.subjectList) {
            return;
        }

        const row = el('div', 'rounded-xl border border-outline-variant/60 bg-surface-container-low p-3');
        row.dataset.insuranceSubjectRow = '';
        row.dataset.insuranceSubjectId = subject.id || newUuid();

        const grid = el('div', 'grid gap-2 sm:grid-cols-2');
        const label = this.inputField(t('Dato'), 'text', {
            placeholder: t('Placas, dirección, beneficiario…'),
            maxlength: 80,
            value: subject.label || '',
        });
        const value = this.inputField(t('Valor'), 'text', {
            placeholder: t('ABC-123-A'),
            maxlength: 240,
            value: subject.value || '',
        });
        label.input.dataset.insuranceSubjectLabel = '';
        value.input.dataset.insuranceSubjectValue = '';
        grid.append(label.wrapper, value.wrapper);

        const footer = el('div', 'mt-2 flex justify-end');
        const remove = el('button', 'mp-btn mp-btn-danger mp-btn-sm', t('Quitar'));
        remove.type = 'button';
        remove.dataset.insuranceSubjectRemove = '';
        footer.appendChild(remove);
        row.append(grid, footer);
        this.subjectList.appendChild(row);
    }

    readSubjectRows() {
        return Array.from(this.subjectList?.querySelectorAll('[data-insurance-subject-row]') || [])
            .map((row) => ({
                id: row.dataset.insuranceSubjectId || newUuid(),
                label: row.querySelector('[data-insurance-subject-label]')?.value.trim() || '',
                value: row.querySelector('[data-insurance-subject-value]')?.value.trim() || '',
            }))
            .filter((entry) => entry.label || entry.value);
    }

    addCoverageRow(coverage = {}) {
        if (!this.coveragesList) {
            return;
        }

        const row = el('div', 'rounded-xl border border-outline-variant/60 bg-surface-container-low p-3');
        row.dataset.insuranceCoverageRow = '';
        row.dataset.insuranceCoverageId = coverage.id || newUuid();

        const grid = el('div', 'grid gap-2 sm:grid-cols-2');
        const name = this.inputField(t('Cobertura'), 'text', {
            placeholder: t('Daños materiales'),
            maxlength: 140,
            value: coverage.name || '',
        });
        const amount = this.inputField(t('Monto o límite'), 'text', {
            placeholder: t('$50,000'),
            maxlength: 80,
            value: coverage.amount || '',
        });
        name.input.dataset.insuranceCoverageName = '';
        amount.input.dataset.insuranceCoverageAmount = '';
        grid.append(name.wrapper, amount.wrapper);

        const notes = this.inputField(t('Notas (opcional)'), 'text', {
            placeholder: t('Aplica con deducible…'),
            maxlength: 240,
            value: coverage.notes || '',
        });
        notes.input.dataset.insuranceCoverageNotes = '';

        const footer = el('div', 'mt-2 flex justify-end');
        const remove = el('button', 'mp-btn mp-btn-danger mp-btn-sm', t('Quitar'));
        remove.type = 'button';
        remove.dataset.insuranceCoverageRemove = '';
        footer.appendChild(remove);
        row.append(grid, notes.wrapper, footer);
        this.coveragesList.appendChild(row);
    }

    readCoverageRows() {
        return Array.from(this.coveragesList?.querySelectorAll('[data-insurance-coverage-row]') || [])
            .map((row) => ({
                id: row.dataset.insuranceCoverageId || newUuid(),
                name: row.querySelector('[data-insurance-coverage-name]')?.value.trim() || '',
                amount: row.querySelector('[data-insurance-coverage-amount]')?.value.trim() || '',
                notes: row.querySelector('[data-insurance-coverage-notes]')?.value.trim() || '',
            }))
            .filter((entry) => entry.name || entry.amount || entry.notes);
    }

    renderSubjectRows(subjects) {
        if (!this.subjectList) {
            return;
        }
        this.subjectList.replaceChildren();
        (subjects || []).forEach((subject) => this.addSubjectRow(subject));
        if (!subjects || subjects.length === 0) {
            this.addSubjectRow();
        }
    }

    renderCoverageRows(coverages) {
        if (!this.coveragesList) {
            return;
        }
        this.coveragesList.replaceChildren();
        (coverages || []).forEach((coverage) => this.addCoverageRow(coverage));
        if (!coverages || coverages.length === 0) {
            this.addCoverageRow();
        }
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
        this.modalTitle.textContent = t('Nuevo seguro');
        this.form.reset();
        this.idInput.value = '';
        this.versionInput.value = '';
        this.blobInput.value = '';
        this.categoryInput.value = 'auto';
        this.statusInput.value = 'active';
        this.currencyInput.value = 'MXN';
        this.reminderDaysInput.value = '7';
        this.renderSubjectRows(CATEGORY_TEMPLATES.auto);
        this.renderCoverageRows([]);
        this.renderFileState();
        this.hideError();
        this.modal.showModal();
        this.titleInput?.focus();
    }

    openEdit(item) {
        if (!this.modal) {
            return;
        }

        const insurance = insuranceFromItem(item);
        this.editing = item;
        window.dispatchEvent(new CustomEvent('mypass:item-open', { detail: { id: item.id } }));
        this.baseMetadata = item.metadata ? { ...item.metadata } : {};
        this.existingBlobId = item.blob_id || null;
        this.clearAttachment = false;
        this.fileLabel = item.blob_id ? (item.metadata?.original_name || '') : '';
        this.modalTitle.textContent = t('Editar seguro');
        this.form.reset();
        this.idInput.value = item.id;
        this.versionInput.value = String(item.version || '');
        this.blobInput.value = item.blob_id || '';
        this.titleInput.value = item.metadata?.title || '';
        this.categoryInput.value = insurance.category;
        this.statusInput.value = insurance.status;
        this.providerInput.value = insurance.provider;
        this.productInput.value = insurance.product;
        this.policyInput.value = insurance.policyNumber;
        this.memberInput.value = insurance.memberId;
        this.holderInput.value = insurance.holder;
        this.validFromInput.value = insurance.validFrom;
        this.validUntilInput.value = insurance.validUntil;
        this.emergencyPhoneInput.value = insurance.emergencyPhone;
        this.assistancePhoneInput.value = insurance.assistancePhone;
        this.agentNameInput.value = insurance.agentName;
        this.agentPhoneInput.value = insurance.agentPhone;
        this.emailInput.value = insurance.email;
        this.websiteInput.value = insurance.website;
        this.premiumInput.value = insurance.premium;
        this.currencyInput.value = insurance.currency;
        this.paymentFrequencyInput.value = insurance.paymentFrequency;
        this.nextPaymentInput.value = insurance.nextPaymentDate;
        this.insuredSumInput.value = insurance.insuredSum;
        this.deductibleInput.value = insurance.deductible;
        this.notesInput.value = insurance.notes;
        this.reminderDaysInput.value = String(insurance.reminderDays);
        this.renderSubjectRows(insurance.subject.length > 0 ? insurance.subject : (CATEGORY_TEMPLATES[insurance.category] || []));
        this.renderCoverageRows(insurance.coverages);
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
        this.subjectList?.replaceChildren();
        this.coveragesList?.replaceChildren();
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

    showError(message, target = this.error) {
        if (!target) {
            return;
        }
        target.textContent = message;
        target.hidden = false;
    }

    hideError(target = this.error) {
        if (target) {
            target.hidden = true;
        }
    }

    async save(event) {
        event.preventDefault();
        this.hideError();

        const title = this.titleInput.value.trim();
        if (!title) {
            this.showError(t('Escribe un nombre para el seguro.'));
            this.titleInput.focus();
            return;
        }

        const file = this.fileInput?.files?.[0] || null;
        if (file && file.size > 25 * 1024 * 1024) {
            this.showError(t('El archivo supera los 25 MB.'));
            return;
        }

        const previous = this.editing ? insuranceFromItem(this.editing) : null;
        const validUntil = this.validUntilInput.value || '';
        const metadata = {
            ...this.baseMetadata,
            title,
            type: 'insurance',
            expires_at: validUntil || this.nextPaymentInput.value || null,
            insurance: {
                ...(this.baseMetadata.insurance || {}),
                category: this.categoryInput.value,
                provider: this.providerInput.value.trim(),
                product: this.productInput.value.trim(),
                policy_number: this.policyInput.value.trim(),
                member_id: this.memberInput.value.trim(),
                holder: this.holderInput.value.trim(),
                valid_from: this.validFromInput.value || '',
                valid_until: validUntil,
                status: this.statusInput.value,
                emergency_phone: this.emergencyPhoneInput.value.trim(),
                assistance_phone: this.assistancePhoneInput.value.trim(),
                agent_name: this.agentNameInput.value.trim(),
                agent_phone: this.agentPhoneInput.value.trim(),
                email: this.emailInput.value.trim(),
                website: this.websiteInput.value.trim(),
                premium: this.premiumInput.value.trim(),
                currency: this.currencyInput.value.trim() || 'MXN',
                payment_frequency: this.paymentFrequencyInput.value,
                next_payment_date: this.nextPaymentInput.value || '',
                insured_sum: this.insuredSumInput.value.trim(),
                deductible: this.deductibleInput.value.trim(),
                subject: this.readSubjectRows(),
                coverages: this.readCoverageRows(),
                notes: this.notesInput.value.trim(),
                reminder_days: Math.min(90, Math.max(1, Number(this.reminderDaysInput.value || 7))),
                claims: previous?.claims || [],
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
                type: 'insurance',
                metadata,
                file,
                blobId: file ? this.existingBlobId : (this.clearAttachment ? null : this.existingBlobId),
            });
            this.close();
            this.options.toast?.(t('Seguro guardado.'));
        } catch (error) {
            this.showError(error.message || t('No se pudo guardar el seguro.'));
        } finally {
            this.saveButton.disabled = false;
            this.saveButton.textContent = original;
        }
    }

    handleListClick(event) {
        const targetCard = event.target.closest('[data-insurance-id]');
        const action = event.target.closest('[data-insurance-action]');
        if (!action) {
            if (targetCard?.classList.contains('mp-deck-card--compact')) {
                const targetItem = this.items.find((candidate) => candidate.id === targetCard.dataset.insuranceId);
                if (targetItem) {
                    this.selectedInsuranceId = targetItem.id;
                    this.flippedInsuranceId = null;
                    this.render();
                    window.requestAnimationFrame(() => {
                        centerCardInView(this.list?.querySelector(`[data-insurance-id="${CSS.escape(targetItem.id)}"]`));
                    });
                }
            }
            return;
        }

        const card = action.closest('[data-insurance-id]');
        const item = this.items.find((candidate) => candidate.id === card?.dataset.insuranceId);
        if (!item) {
            return;
        }

        const actionName = action.dataset.insuranceAction;
        if (actionName === 'present') {
            this.openPresentation(item);
        } else if (actionName === 'claims') {
            this.openClaims(item);
        } else if (actionName === 'edit') {
            this.openEdit(item);
        } else if (actionName === 'open') {
            this.options.openItem?.(item);
        } else if (actionName === 'share') {
            this.options.openShare?.(item);
        } else if (actionName === 'delete') {
            this.options.deleteItem?.(item, action);
        }
    }

    render() {
        if (!this.list) {
            return;
        }

        const query = this.search.trim().toLowerCase();
        const counts = { all: this.items.length };
        for (const item of this.items) {
            const category = insuranceFromItem(item).category;
            counts[category] = (counts[category] || 0) + 1;
        }
        this.panel.querySelectorAll('[data-insurance-count]').forEach((node) => {
            node.textContent = String(counts[node.dataset.insuranceCount] || 0);
        });
        this.panel.querySelectorAll('[data-insurance-filter]').forEach((button) => {
            button.setAttribute('aria-pressed', button.dataset.insuranceFilter === this.filter ? 'true' : 'false');
        });

        const filtered = sortPolicies(this.items
            .filter((item) => item.state !== 'transferred')
            .filter((item) => this.filter === 'all' || insuranceFromItem(item).category === this.filter)
            .filter((item) => {
                if (!query) {
                    return true;
                }
                return searchValues(item).some((value) => value.toLowerCase().includes(query));
            }));

        this.empty.hidden = filtered.length > 0;
        if (this.items.length === 0) {
            this.emptyTitle.textContent = t('Aún no tienes un seguro guardado.');
            this.emptyCopy.textContent = t('Agrega tu primera póliza para tener los datos y teléfonos a la mano en un siniestro.');
        } else if (filtered.length === 0) {
            this.emptyTitle.textContent = t('No hay seguros que coincidan.');
            this.emptyCopy.textContent = t('Prueba con otra búsqueda o categoría.');
        }

        if (this.selectedInsuranceId && !filtered.some((item) => item.id === this.selectedInsuranceId)) {
            this.selectedInsuranceId = null;
            this.flippedInsuranceId = null;
        }
        if (this.flippedInsuranceId && this.flippedInsuranceId !== this.selectedInsuranceId) {
            this.flippedInsuranceId = null;
        }

        this.list.replaceChildren();
        const deck = el('div', 'mp-deck');
        filtered.forEach((item, index) => {
            const selected = item.id === this.selectedInsuranceId;
            const card = selected ? this.renderFlipCard(item) : this.renderCompactCard(item);
            card.style.setProperty('--deck-index', String(index));
            deck.appendChild(card);
            if (selected) {
                window.requestAnimationFrame(() => syncDeckHeight(card));
            }
        });
        this.list.appendChild(deck);
        this.renderReminderBanner();
    }

    renderReminderBanner() {
        if (!this.reminderBanner || !this.reminderList) {
            return;
        }
        const now = new Date();
        const entries = [];
        for (const item of this.items.filter((candidate) => candidate.state !== 'transferred')) {
            const insurance = insuranceFromItem(item);
            const days = insurance.reminderDays;
            const add = (value, kind) => {
                const date = parseInsuranceDate(value);
                if (!date) {
                    return;
                }
                const diff = Math.ceil((date.getTime() - now.getTime()) / 86400000);
                if (diff > days || diff < -30) {
                    return;
                }
                entries.push({ item, insurance, kind, date, diff });
            };
            add(insurance.validUntil || item.expires_at, 'expiry');
            add(insurance.nextPaymentDate, 'payment');
        }
        entries.sort((a, b) => a.date - b.date);
        this.reminderBanner.hidden = entries.length === 0;
        this.reminderList.replaceChildren();
        for (const entry of entries) {
            const title = entry.item.metadata?.title || entry.insurance.provider || t('Seguro');
            const date = entry.date.toLocaleDateString(locale(), { day: 'numeric', month: 'short', year: 'numeric' });
            const prefix = entry.kind === 'payment' ? t('Pago') : t('Vigencia');
            const line = entry.diff < 0
                ? (entry.kind === 'payment'
                    ? tf(':title — el pago venció el :date', { title, date })
                    : tf(':title — venció el :date', { title, date }))
                : (entry.kind === 'payment'
                    ? tf(':title — pago el :date', { title, date })
                    : tf(':title — vence el :date', { title, date }));
            this.reminderList.appendChild(el('li', '', `${prefix}: ${line}`));
        }
    }

    renderCompactCard(item) {
        const insurance = insuranceFromItem(item);
        const content = el('div', 'flex flex-col');
        content.appendChild(this.insuranceHeader(item, insurance));
        content.appendChild(el(
            'p',
            'mp-insurance-card__policy',
            insurance.policyNumber ? tf('Póliza :number', { number: insurance.policyNumber }) : t('Sin número de póliza'),
        ));
        content.appendChild(el('p', 'mt-2 text-[14px] text-on-surface-variant', [
            insurance.holder,
            insurance.policyNumber ? t('Ver datos y teléfonos') : '',
        ].filter(Boolean).join(' · ')));

        const footer = el('div', 'mt-3 flex flex-wrap items-center gap-2 text-[13px] text-on-surface-variant');
        const status = insuranceStatus(item);
        footer.appendChild(el('span', `mp-badge ${statusBadgeClass(status.tone)}`, status.label));
        if (item.expires_at) {
            footer.appendChild(el('span', '', tf('Vence :date', { date: formatDate(item.expires_at, locale()) })));
        }
        if (insurance.claims.length > 0) {
            footer.appendChild(el(
                'span',
                '',
                insurance.claims.length === 1 ? t('1 siniestro') : tf(':count siniestros', { count: insurance.claims.length }),
            ));
        }
        content.appendChild(footer);

        const card = createCompactCard({
            className: 'mp-medical-card mp-insurance-card',
            dataset: { insuranceId: item.id },
            content,
        });
        return card;
    }

    renderFlipCard(item) {
        const insurance = insuranceFromItem(item);
        const status = insuranceStatus(item);
        const front = el('div', 'flex h-full flex-col');
        front.appendChild(this.insuranceHeader(item, insurance));
        front.appendChild(el(
            'p',
            'mp-insurance-card__policy',
            insurance.policyNumber ? tf('Póliza :number', { number: insurance.policyNumber }) : t('Sin número de póliza'),
        ));
        const frontGrid = el('div', 'mp-medical-card__grid');
        frontGrid.appendChild(this.stat(t('Titular'), insurance.holder || '—'));
        frontGrid.appendChild(this.stat(t('Vigencia'), insurance.validUntil ? formatDate(insurance.validUntil, locale()) : '—'));
        frontGrid.appendChild(this.stat(t('Estado'), status.label));
        frontGrid.appendChild(this.stat(t('Siniestros'), String(insurance.claims.length)));
        front.appendChild(frontGrid);
        front.appendChild(this.flipRow(t('Ver reverso'), 'flip'));

        const back = el('div', 'flex h-full flex-col');
        back.appendChild(this.insuranceHeader(item, insurance));
        back.appendChild(this.detailGrid(item, insurance));
        back.appendChild(this.insuranceActions(item, insurance));
        if (insurance.notes) {
            back.appendChild(el('p', 'relative z-10 mt-3 whitespace-pre-wrap text-[13px] leading-relaxed text-on-surface-variant', insurance.notes));
        }
        back.appendChild(this.flipRow(t('Volver al frente'), 'undo'));

        const card = createFlipCard({
            faceClassName: 'mp-medical-card mp-insurance-card',
            dataset: { insuranceId: item.id },
            front,
            back,
            flipped: this.flippedInsuranceId === item.id,
        });
        card.classList.add('is-selected');

        card.querySelectorAll('[data-deck-flip]').forEach((button) => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopPropagation();
                const next = !card.classList.contains('is-flipped');
                setDeckFlipped(card, next);
                this.flippedInsuranceId = next ? item.id : null;
            });
        });

        return card;
    }

    insuranceHeader(item, insurance) {
        const category = categoryInfo(insurance.category);
        const status = insuranceStatus(item);
        const head = el('div', 'mp-medical-card__head');
        const names = el('div', 'min-w-0');
        names.appendChild(el('p', 'mp-medical-card__title', item.metadata?.title || insurance.product || t('Seguro')));
        names.appendChild(el('p', 'mp-medical-card__holder', insurance.provider || t('Sin aseguradora')));
        if (window.mypassSharedItemIds?.has(item.id)) {
            names.appendChild(el('span', 'mp-badge mp-badge-primary mt-1', t('Compartido')));
        }
        head.appendChild(names);
        const badges = el('div', 'flex shrink-0 flex-col items-end gap-1');
        const categoryBadge = el('span', 'mp-badge mp-badge-neutral');
        categoryBadge.append(icon(category.icon, 'text-[15px]'), document.createTextNode(t(category.label)));
        badges.appendChild(categoryBadge);
        badges.appendChild(el('span', `mp-badge ${statusBadgeClass(status.tone)}`, status.label));
        head.appendChild(badges);
        return head;
    }

    detailGrid(item, insurance) {
        const grid = el('div', 'mp-medical-card__grid');
        if (insurance.product) {
            grid.appendChild(this.stat(t('Producto'), insurance.product, true));
        }
        if (insurance.memberId) {
            grid.appendChild(this.stat(t('Afiliado o contrato'), insurance.memberId));
        }
        if (insurance.deductible) {
            grid.appendChild(this.stat(t('Deducible'), insurance.deductible));
        }
        if (insurance.insuredSum) {
            grid.appendChild(this.stat(t('Suma asegurada'), insurance.insuredSum, true));
        }
        if (insurance.emergencyPhone) {
            const phone = el('a', 'mp-insurance-phone', '');
            phone.href = telHref(insurance.emergencyPhone);
            phone.append(icon('call', 'text-[18px]'), document.createTextNode(insurance.emergencyPhone));
            const stat = el('div', 'mp-medical-card__stat');
            stat.appendChild(el('span', 'mp-medical-card__label', t('Siniestros')));
            stat.appendChild(phone);
            grid.appendChild(stat);
        }
        if (insurance.assistancePhone) {
            const phone = el('a', 'mp-insurance-phone', '');
            phone.href = telHref(insurance.assistancePhone);
            phone.append(icon('support_agent', 'text-[18px]'), document.createTextNode(insurance.assistancePhone));
            const stat = el('div', 'mp-medical-card__stat');
            stat.appendChild(el('span', 'mp-medical-card__label', t('Asistencia 24 h')));
            stat.appendChild(phone);
            grid.appendChild(stat);
        }
        const subject = insurance.subject.filter((entry) => entry.value.trim());
        subject.slice(0, 4).forEach((entry) => {
            grid.appendChild(this.stat(entry.label || t('Dato'), entry.value || '—', true));
        });
        if (insurance.reminderDays) {
            grid.appendChild(this.stat(t('Aviso'), tf(':count días antes', { count: insurance.reminderDays })));
        }
        if (insurance.claims.length > 0) {
            const latest = [...insurance.claims].sort((a, b) => `${b.date}`.localeCompare(`${a.date}`))[0];
            grid.appendChild(this.stat(t('Último siniestro'), [
                latest.claimNumber,
                latest.date ? formatDate(latest.date, locale()) : '',
                claimStatusInfo(latest.status).label,
            ].filter(Boolean).join(' · '), true));
        }
        if (item.expires_at) {
            grid.appendChild(this.stat(t('Vigencia'), formatDate(item.expires_at, locale()), true));
        }
        return grid;
    }

    insuranceActions(item, insurance) {
        const actions = el('div', 'mp-medical-card__actions');
        actions.appendChild(this.actionButton(t('Modo siniestro'), 'present', 'emergency', 'mp-btn-primary'));
        actions.appendChild(this.actionButton(t('Siniestros'), 'claims', 'receipt_long'));
        actions.appendChild(this.actionButton(t('Editar'), 'edit', 'edit'));
        if (item.blob_id) {
            actions.appendChild(this.actionButton(t('Póliza'), 'open', 'picture_as_pdf'));
        }
        if (this.options.canShare?.(item) && item.state !== 'transferred') {
            actions.appendChild(this.actionButton(t('Compartir'), 'share', 'send'));
        }
        actions.appendChild(this.actionButton(t('Eliminar'), 'delete', 'delete', 'mp-btn-quiet'));
        return actions;
    }

    flipRow(label, iconName) {
        const row = el('div', 'mt-auto flex justify-end pt-4');
        const button = el('button', 'mp-deck-flip');
        button.type = 'button';
        button.dataset.deckFlip = 'true';
        button.append(icon(iconName, 'text-[18px]'), document.createTextNode(label));
        row.appendChild(button);
        return row;
    }

    stat(label, value, wide = false) {
        const node = el('div', `mp-medical-card__stat${wide ? ' mp-medical-card__wide' : ''}`);
        node.appendChild(el('span', 'mp-medical-card__label', label));
        node.appendChild(el('span', 'mp-medical-card__value', value));
        return node;
    }

    actionButton(label, actionName, iconName, extraClass = 'mp-btn-secondary') {
        const node = el('button', `mp-btn ${extraClass} mp-btn-sm`);
        node.type = 'button';
        node.dataset.insuranceAction = actionName;
        node.replaceChildren(icon(iconName, 'text-[18px]'), document.createTextNode(label));
        return node;
    }

    openClaims(item) {
        if (!this.claimsModal) {
            return;
        }
        this.claimsItem = item;
        const insurance = insuranceFromItem(item);
        this.claimsTitle.textContent = t('Siniestros');
        this.claimsSubtitle.textContent = [
            item.metadata?.title,
            insurance.provider,
            insurance.policyNumber ? `#${insurance.policyNumber}` : '',
        ].filter(Boolean).join(' · ');
        this.hideError(this.claimsError);
        this.renderClaims();
        this.claimsModal.showModal();
    }

    closeClaims() {
        if (this.claimsModal?.open) {
            this.claimsModal.close();
        }
        this.claimsItem = null;
        this.claimEditing = null;
    }

    renderClaims() {
        if (!this.claimsList || !this.claimsItem) {
            return;
        }
        const insurance = insuranceFromItem(this.claimsItem);
        const claims = [...insurance.claims].sort((a, b) => `${b.date} ${b.claimNumber}`.localeCompare(`${a.date} ${a.claimNumber}`));
        this.claimsEmpty.hidden = claims.length > 0;
        this.claimsList.replaceChildren();

        for (const claim of claims) {
            const status = claimStatusInfo(claim.status);
            const card = el('article', 'rounded-2xl border border-outline-variant/60 bg-surface-container-low p-4');
            const head = el('div', 'flex flex-wrap items-start justify-between gap-2');
            const title = el('div', 'min-w-0');
            title.appendChild(el('p', 'font-semibold text-on-surface', claim.claimNumber || t('Siniestro')));
            title.appendChild(el('p', 'mt-1 text-sm text-on-surface-variant', [
                claim.date ? formatDate(claim.date, locale()) : '',
                claim.type,
                claim.amount,
            ].filter(Boolean).join(' · ') || t('Sin fecha')));
            head.appendChild(title);
            head.appendChild(el('span', `mp-badge ${statusBadgeClass(status.tone)}`, status.label));
            card.appendChild(head);
            if (claim.description) {
                card.appendChild(el('p', 'mt-3 whitespace-pre-wrap text-sm leading-relaxed text-on-surface', claim.description));
            }
            const meta = el('div', 'mt-3 flex flex-wrap gap-3 text-sm text-on-surface-variant');
            if (claim.adjusterName) {
                meta.appendChild(el('span', '', tf('Ajustador: :name', { name: claim.adjusterName })));
            }
            if (claim.adjusterPhone) {
                const phone = el('a', 'font-semibold text-primary underline underline-offset-4', claim.adjusterPhone);
                phone.href = telHref(claim.adjusterPhone);
                meta.appendChild(phone);
            }
            if (claim.notes) {
                meta.appendChild(el('span', '', claim.notes));
            }
            if (meta.childNodes.length > 0) {
                card.appendChild(meta);
            }
            const actions = el('div', 'mt-3 flex flex-wrap gap-2');
            const edit = el('button', 'mp-btn mp-btn-secondary mp-btn-sm');
            edit.type = 'button';
            edit.dataset.claimEdit = claim.id;
            edit.append(icon('edit', 'text-[18px]'), document.createTextNode(t('Editar')));
            const remove = el('button', 'mp-btn mp-btn-danger mp-btn-sm');
            remove.type = 'button';
            remove.dataset.claimRemove = claim.id;
            remove.append(icon('delete', 'text-[18px]'), document.createTextNode(t('Eliminar')));
            actions.append(edit, remove);
            card.appendChild(actions);
            this.claimsList.appendChild(card);
        }
    }

    handleClaimsClick(event) {
        const edit = event.target.closest('[data-claim-edit]');
        const remove = event.target.closest('[data-claim-remove]');
        if (edit) {
            const insurance = insuranceFromItem(this.claimsItem);
            const claim = insurance.claims.find((candidate) => candidate.id === edit.dataset.claimEdit);
            if (claim) {
                this.openClaimForm(claim);
            }
        } else if (remove) {
            this.removeClaim(remove.dataset.claimRemove);
        }
    }

    openClaimForm(claim = null) {
        if (!this.claimModal) {
            return;
        }
        this.claimEditing = claim;
        this.claimForm.reset();
        this.claimId.value = claim?.id || '';
        this.claimModalTitle.textContent = claim ? t('Editar siniestro') : t('Nuevo siniestro');
        this.claimDate.value = claim?.date || today();
        this.claimNumber.value = claim?.claimNumber || '';
        this.claimType.value = claim?.type || '';
        this.claimStatus.value = claim?.status || 'open';
        this.claimDescription.value = claim?.description || '';
        this.claimAdjusterName.value = claim?.adjusterName || '';
        this.claimAdjusterPhone.value = claim?.adjusterPhone || '';
        this.claimAmount.value = claim?.amount || '';
        this.claimNotes.value = claim?.notes || '';
        this.hideError(this.claimError);
        this.claimModal.showModal();
        this.claimNumber.focus();
    }

    closeClaimForm() {
        if (this.claimModal?.open) {
            this.claimModal.close();
        }
        this.claimEditing = null;
    }

    async saveClaim(event) {
        event.preventDefault();
        if (!this.claimsItem) {
            return;
        }
        this.hideError(this.claimError);
        const number = this.claimNumber.value.trim();
        const type = this.claimType.value.trim();
        if (!number && !type && !this.claimDescription.value.trim()) {
            this.showError(t('Escribe al menos el número de reporte, el motivo o una descripción.'), this.claimError);
            return;
        }

        const claim = {
            id: this.claimEditing?.id || newUuid(),
            date: this.claimDate.value || today(),
            claim_number: number,
            type,
            description: this.claimDescription.value.trim(),
            status: this.claimStatus.value,
            adjuster_name: this.claimAdjusterName.value.trim(),
            adjuster_phone: this.claimAdjusterPhone.value.trim(),
            amount: this.claimAmount.value.trim(),
            notes: this.claimNotes.value.trim(),
        };

        const insurance = insuranceFromItem(this.claimsItem);
        const claims = this.claimEditing
            ? insurance.claims.map((entry) => entry.id === claim.id ? claim : entry)
            : [...insurance.claims, claim];

        await this.persistClaims(claims, () => {
            this.closeClaimForm();
            const updated = this.items.find((item) => item.id === this.claimsItem?.id);
            if (updated) {
                this.claimsItem = updated;
            }
            this.renderClaims();
            this.options.toast?.(t('Siniestro guardado.'));
        });
    }

    async removeClaim(claimId) {
        if (!this.claimsItem || !this.options.confirm) {
            return;
        }
        const confirmed = await this.options.confirm({
            title: t('¿Eliminar este siniestro?'),
            message: t('Se quitará del historial de la póliza.'),
            acceptLabel: t('Eliminar'),
        });
        if (!confirmed) {
            return;
        }
        const insurance = insuranceFromItem(this.claimsItem);
        const claims = insurance.claims.filter((claim) => claim.id !== claimId);
        await this.persistClaims(claims, () => {
            const updated = this.items.find((item) => item.id === this.claimsItem?.id);
            if (updated) {
                this.claimsItem = updated;
            }
            this.renderClaims();
            this.options.toast?.(t('Siniestro eliminado.'));
        });
    }

    async persistClaims(claims, onSuccess) {
        if (!this.claimsItem) {
            return;
        }
        const item = this.claimsItem;
        const rawInsurance = safeObject(item.metadata?.insurance);
        const metadata = {
            ...(item.metadata || {}),
            insurance: {
                ...rawInsurance,
                claims,
            },
        };
        this.claimSave.disabled = true;
        try {
            await this.options.saveItem({
                itemId: item.id,
                newId: null,
                version: item.version,
                type: 'insurance',
                metadata,
                file: null,
                blobId: item.blob_id || null,
            });
            onSuccess?.();
        } catch (error) {
            this.showError(error.message || t('No se pudo guardar el siniestro.'), this.claimError);
        } finally {
            this.claimSave.disabled = false;
        }
    }

    openPresentation(item) {
        if (!this.presentation) {
            return;
        }
        const insurance = insuranceFromItem(item);
        const category = categoryInfo(insurance.category);
        const status = insuranceStatus(item);
        const claim = activeClaim(insurance);
        const latestClaim = claim || [...insurance.claims].sort((a, b) => `${b.date}`.localeCompare(`${a.date}`))[0];
        this.presentingItem = item;

        this.presentationCategory.textContent = `${t(category.label)} · ${t('Modo siniestro')}`;
        this.presentationTitle.textContent = item.metadata?.title || insurance.product || t('Seguro');
        this.presentationProvider.textContent = [insurance.provider, insurance.product].filter(Boolean).join(' · ');
        this.presentationExpired.hidden = !status.expired;
        this.presentationPolicy.textContent = insurance.policyNumber || t('Sin número de póliza');

        const call = telHref(insurance.emergencyPhone);
        this.presentationCall.hidden = !call;
        if (call) {
            this.presentationCall.href = call;
            this.presentationCallLabel.textContent = `${t('Llamar a siniestros')} · ${insurance.emergencyPhone}`;
        }
        const assistance = telHref(insurance.assistancePhone);
        this.presentationAssistance.hidden = !assistance;
        if (assistance) {
            this.presentationAssistance.href = assistance;
            this.presentationAssistanceLabel.textContent = `${t('Asistencia 24 h')} · ${insurance.assistancePhone}`;
        }

        this.presentationGrid.replaceChildren();
        this.presentationGrid.appendChild(this.presentationBox(t('Titular'), insurance.holder || '—'));
        this.presentationGrid.appendChild(this.presentationBox(t('Estado'), status.label));
        this.presentationGrid.appendChild(this.presentationBox(t('Vigencia'), [
            insurance.validFrom ? formatDate(insurance.validFrom, locale()) : '',
            insurance.validUntil ? formatDate(insurance.validUntil, locale()) : '',
        ].filter(Boolean).join(' — ') || '—'));
        if (insurance.memberId) {
            this.presentationGrid.appendChild(this.presentationBox(t('Afiliado o contrato'), insurance.memberId));
        }
        if (insurance.deductible) {
            this.presentationGrid.appendChild(this.presentationBox(t('Deducible'), insurance.deductible));
        }
        if (insurance.insuredSum) {
            this.presentationGrid.appendChild(this.presentationBox(t('Suma asegurada'), insurance.insuredSum));
        }
        for (const subject of insurance.subject) {
            if (subject.value.trim()) {
                this.presentationGrid.appendChild(this.presentationBox(subject.label || t('Dato'), subject.value, true));
            }
        }
        const coverages = insurance.coverages.filter((entry) => entry.name || entry.amount || entry.notes);
        if (coverages.length > 0) {
            const coverageText = coverages.map((entry) => [
                entry.name,
                entry.amount,
                entry.notes ? `(${entry.notes})` : '',
            ].filter(Boolean).join(' ')).join(' · ');
            this.presentationGrid.appendChild(this.presentationBox(t('Coberturas'), coverageText, true));
        }

        this.presentationClaimBox.hidden = !latestClaim;
        if (latestClaim) {
            this.presentationClaim.textContent = [
                latestClaim.claimNumber || t('Siniestro'),
                latestClaim.date ? formatDate(latestClaim.date, locale()) : '',
                latestClaim.type,
                claimStatusInfo(latestClaim.status).label,
            ].filter(Boolean).join(' · ');
            const adjuster = telHref(latestClaim.adjusterPhone);
            this.presentationAdjuster.hidden = !adjuster;
            if (adjuster) {
                this.presentationAdjuster.href = adjuster;
                this.presentationAdjuster.replaceChildren(
                    icon('call', 'text-[19px]'),
                    document.createTextNode([latestClaim.adjusterName || t('Ajustador'), latestClaim.adjusterPhone].filter(Boolean).join(' · ')),
                );
            }
        }

        this.presentationNotesBox.hidden = !insurance.notes;
        this.presentationNotes.textContent = insurance.notes;
        this.presentationOpenFile.hidden = !item.blob_id;
        this.presentationShare.hidden = !(this.options.canShare?.(item) && item.state !== 'transferred');

        this.presentation.showModal();
        this.presentation.requestFullscreen?.().catch(() => {});
        this.requestWakeLock();
    }

    presentationBox(label, value, wide = false) {
        const box = el('div', `mp-medical-presentation__box${wide ? ' sm:col-span-2' : ''}`);
        box.appendChild(el('span', 'mp-medical-presentation__label', label));
        box.appendChild(el('p', 'mp-medical-presentation__value', value));
        return box;
    }

    async copyPolicyNumber() {
        const insurance = this.presentingItem ? insuranceFromItem(this.presentingItem) : null;
        if (!insurance?.policyNumber) {
            return;
        }
        await this.copyText(insurance.policyNumber, t('Número de póliza copiado.'));
    }

    async copySummary() {
        if (!this.presentingItem) {
            return;
        }
        const item = this.presentingItem;
        const insurance = insuranceFromItem(item);
        const status = insuranceStatus(item);
        const lines = [
            item.metadata?.title || t('Seguro'),
            insurance.provider,
            insurance.product,
            insurance.policyNumber ? tf('Póliza: :number', { number: insurance.policyNumber }) : '',
            insurance.holder ? tf('Titular: :name', { name: insurance.holder }) : '',
            insurance.validUntil ? tf('Vigencia: :date', { date: formatDate(insurance.validUntil, locale()) }) : '',
            tf('Estado: :status', { status: status.label }),
            insurance.emergencyPhone ? tf('Siniestros: :phone', { phone: insurance.emergencyPhone }) : '',
            insurance.assistancePhone ? tf('Asistencia: :phone', { phone: insurance.assistancePhone }) : '',
            ...insurance.subject
                .filter((entry) => entry.label || entry.value)
                .map((entry) => `${entry.label}: ${entry.value}`),
        ].filter(Boolean);
        await this.copyText(lines.join('\n'), t('Datos del seguro copiados.'));
    }

    async copyText(value, message) {
        try {
            await navigator.clipboard.writeText(value);
            this.options.toast?.(message);
        } catch {
            const textarea = document.createElement('textarea');
            textarea.value = value;
            textarea.style.position = 'fixed';
            textarea.style.opacity = '0';
            document.body.appendChild(textarea);
            textarea.select();
            try {
                document.execCommand('copy');
                this.options.toast?.(message);
            } catch {
                this.options.toast?.(t('No se pudo copiar. Intenta de nuevo.'));
            } finally {
                textarea.remove();
            }
        }
    }

    openEmergency() {
        if (!this.emergency) {
            return;
        }
        this.renderEmergency();
        this.emergency.showModal();
        this.emergency.requestFullscreen?.().catch(() => {});
        this.requestWakeLock();
    }

    closeEmergency() {
        if (this.emergency?.open) {
            this.emergency.close();
        }
        this.releasePresentation();
    }

    renderEmergency() {
        if (!this.emergencyList) {
            return;
        }
        const items = sortPolicies(this.items.filter((item) => item.state !== 'transferred'));
        this.emergencyEmpty.hidden = items.length > 0;
        this.emergencyList.replaceChildren();

        for (const item of items) {
            const insurance = insuranceFromItem(item);
            const status = insuranceStatus(item);
            const category = categoryInfo(insurance.category);
            const card = el('article', 'rounded-2xl border border-outline-variant/60 bg-surface-container-low p-4');
            const head = el('div', 'flex flex-wrap items-start justify-between gap-2');
            const names = el('div', 'min-w-0');
            names.appendChild(el('p', 'text-[13px] font-bold uppercase tracking-[0.1em] text-primary', `${t(category.label)} · ${status.label}`));
            names.appendChild(el('p', 'mt-1 text-lg font-bold text-on-surface', item.metadata?.title || insurance.product || t('Seguro')));
            names.appendChild(el('p', 'mt-1 text-sm text-on-surface-variant', [
                insurance.provider,
                insurance.policyNumber ? tf('Póliza :number', { number: insurance.policyNumber }) : '',
            ].filter(Boolean).join(' · ')));
            head.appendChild(names);
            const open = el('button', 'mp-btn mp-btn-secondary mp-btn-sm');
            open.type = 'button';
            open.dataset.emergencyOpen = item.id;
            open.append(icon('open_in_new', 'text-[18px]'), document.createTextNode(t('Ver póliza')));
            head.appendChild(open);
            card.appendChild(head);

            const phones = el('div', 'mt-3 grid gap-2 sm:grid-cols-2');
            if (insurance.emergencyPhone) {
                const call = el('a', 'mp-btn mp-btn-primary min-h-14 text-[16px]', '');
                call.href = telHref(insurance.emergencyPhone);
                call.append(icon('call', 'text-[22px]'), document.createTextNode(`${t('Siniestros')} · ${insurance.emergencyPhone}`));
                phones.appendChild(call);
            }
            if (insurance.assistancePhone) {
                const call = el('a', 'mp-btn mp-btn-secondary min-h-14 text-[16px]', '');
                call.href = telHref(insurance.assistancePhone);
                call.append(icon('support_agent', 'text-[22px]'), document.createTextNode(`${t('Asistencia')} · ${insurance.assistancePhone}`));
                phones.appendChild(call);
            }
            const claim = activeClaim(insurance);
            if (claim) {
                const claimBox = el('div', 'sm:col-span-2 rounded-xl border border-warning/40 bg-warning-container/60 p-3 text-sm');
                claimBox.textContent = [
                    t('Siniestro activo'),
                    claim.claimNumber,
                    claim.type,
                    claimStatusInfo(claim.status).label,
                ].filter(Boolean).join(' · ');
                phones.appendChild(claimBox);
            }
            if (phones.childNodes.length > 0) {
                card.appendChild(phones);
            }
            this.emergencyList.appendChild(card);
        }
    }

    closePresentation() {
        if (this.presentation?.open) {
            this.presentation.close();
        }
        this.releasePresentation();
    }

    requestWakeLock() {
        if (!('wakeLock' in navigator) || this.wakeLock) {
            return;
        }
        navigator.wakeLock.request('screen')
            .then((lock) => {
                this.wakeLock = lock;
            })
            .catch(() => {
                this.wakeLock = null;
            });
    }

    releasePresentation() {
        this.wakeLock?.release?.().catch(() => {});
        this.wakeLock = null;
        if (document.fullscreenElement) {
            document.exitFullscreen?.().catch(() => {});
        }
        this.presentingItem = null;
    }
}
