import {
    formatDate,
    isExpired,
    newUuid,
    splitList,
} from './vault-secrets';
import { downloadMedicalCalendar, medicalCalendar } from './calendar';

const t = (key) => (window.mypassT ? window.mypassT(key) : key);
const tf = (key, values) => (window.mypassTF ? window.mypassTF(key, values) : key);
const locale = () => (window.mypassLocale === 'en' ? 'en-US' : 'es-MX');

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

function safeObject(value) {
    return value && typeof value === 'object' && !Array.isArray(value) ? value : {};
}

function stringList(value) {
    return Array.isArray(value) ? value.filter(Boolean).map(String) : [];
}

export function isMedicalItem(item) {
    return item?.type === 'medical_card' || Boolean(item?.metadata?.medical);
}

export function medicalFromItem(item) {
    const raw = safeObject(item?.metadata?.medical);
    const insurance = safeObject(raw.insurance);
    const doctor = safeObject(raw.doctor);

    return {
        holder: typeof raw.holder === 'string' ? raw.holder : '',
        birthDate: typeof raw.birth_date === 'string' ? raw.birth_date : '',
        bloodType: typeof raw.blood_type === 'string' ? raw.blood_type : '',
        allergies: stringList(raw.allergies),
        conditions: stringList(raw.conditions),
        medications: stringList(raw.medications),
        medicationSchedule: Array.isArray(raw.medication_schedule)
            ? raw.medication_schedule.filter((entry) => entry && typeof entry === 'object').map((entry) => ({
                id: typeof entry.id === 'string' ? entry.id : newUuid(),
                name: typeof entry.name === 'string' ? entry.name : '',
                dose: typeof entry.dose === 'string' ? entry.dose : '',
                times: stringList(entry.times).map((time) => String(time).slice(0, 5)),
                startDate: typeof entry.start_date === 'string' ? entry.start_date : '',
                endDate: typeof entry.end_date === 'string' ? entry.end_date : '',
                notes: typeof entry.notes === 'string' ? entry.notes : '',
            }))
            : [],
        appointments: Array.isArray(raw.appointments)
            ? raw.appointments.filter((entry) => entry && typeof entry === 'object').map((entry) => ({
                id: typeof entry.id === 'string' ? entry.id : newUuid(),
                title: typeof entry.title === 'string' ? entry.title : '',
                date: typeof entry.date === 'string' ? entry.date : '',
                time: typeof entry.time === 'string' ? entry.time : '',
                location: typeof entry.location === 'string' ? entry.location : '',
                notes: typeof entry.notes === 'string' ? entry.notes : '',
                reminderMinutes: Number.isFinite(Number(entry.reminder_minutes))
                    ? Math.max(0, Number(entry.reminder_minutes))
                    : 1440,
            }))
            : [],
        emergencyContacts: Array.isArray(raw.emergency_contacts)
            ? raw.emergency_contacts.filter((contact) => contact && typeof contact === 'object').map((contact) => ({
                name: typeof contact.name === 'string' ? contact.name : '',
                relation: typeof contact.relation === 'string' ? contact.relation : '',
                phone: typeof contact.phone === 'string' ? contact.phone : '',
            }))
            : [],
        insurance: {
            provider: typeof insurance.provider === 'string' ? insurance.provider : '',
            plan: typeof insurance.plan === 'string' ? insurance.plan : '',
            policyNumber: typeof insurance.policy_number === 'string' ? insurance.policy_number : '',
            memberId: typeof insurance.member_id === 'string' ? insurance.member_id : '',
            phone: typeof insurance.phone === 'string' ? insurance.phone : '',
        },
        doctor: {
            name: typeof doctor.name === 'string' ? doctor.name : '',
            phone: typeof doctor.phone === 'string' ? doctor.phone : '',
            clinic: typeof doctor.clinic === 'string' ? doctor.clinic : '',
        },
        organDonor: raw.organ_donor === true,
        notes: typeof raw.notes === 'string' ? raw.notes : '',
    };
}

export class MedicalApp {
    constructor(options) {
        this.options = options;
        this.items = [];
        this.search = '';
        this.editing = null;
        this.baseMetadata = {};
        this.existingBlobId = null;
        this.clearAttachment = false;
        this.fileLabel = '';
        this.wakeLock = null;
        this.presentingItem = null;
        this.bind();
    }

    bind() {
        this.panel = document.getElementById('panel-medical');
        if (!this.panel) {
            return;
        }

        this.list = document.getElementById('medical-list');
        this.empty = document.getElementById('medical-empty');
        this.emptyTitle = document.getElementById('medical-empty-title');
        this.emptyCopy = document.getElementById('medical-empty-copy');
        this.searchInput = document.getElementById('medical-search');
        this.addButton = document.getElementById('medical-add-open');
        this.modal = document.getElementById('medical-modal');
        this.form = document.getElementById('medical-form');
        this.modalTitle = document.getElementById('medical-modal-title');
        this.error = document.getElementById('medical-error');
        this.idInput = document.getElementById('medical-item-id');
        this.versionInput = document.getElementById('medical-item-version');
        this.blobInput = document.getElementById('medical-blob-id');
        this.titleInput = document.getElementById('medical-title');
        this.holderInput = document.getElementById('medical-holder');
        this.birthInput = document.getElementById('medical-birth');
        this.bloodInput = document.getElementById('medical-blood');
        this.organInput = document.getElementById('medical-organ-donor');
        this.allergiesInput = document.getElementById('medical-allergies');
        this.conditionsInput = document.getElementById('medical-conditions');
        this.medicationsList = document.getElementById('medical-medications-list');
        this.medicationAdd = document.getElementById('medical-medication-add');
        this.appointmentsList = document.getElementById('medical-appointments-list');
        this.appointmentAdd = document.getElementById('medical-appointment-add');
        this.contactName = document.getElementById('medical-contact-name');
        this.contactRelation = document.getElementById('medical-contact-relation');
        this.contactPhone = document.getElementById('medical-contact-phone');
        this.insuranceProvider = document.getElementById('medical-insurance-provider');
        this.insurancePlan = document.getElementById('medical-insurance-plan');
        this.insurancePolicy = document.getElementById('medical-insurance-policy');
        this.insuranceMember = document.getElementById('medical-insurance-member');
        this.insurancePhone = document.getElementById('medical-insurance-phone');
        this.doctorName = document.getElementById('medical-doctor-name');
        this.doctorPhone = document.getElementById('medical-doctor-phone');
        this.doctorClinic = document.getElementById('medical-doctor-clinic');
        this.expiresInput = document.getElementById('medical-expires');
        this.fileInput = document.getElementById('medical-file');
        this.fileCurrent = document.getElementById('medical-file-current');
        this.fileName = document.getElementById('medical-file-name');
        this.fileClear = document.getElementById('medical-file-clear');
        this.notesInput = document.getElementById('medical-notes');
        this.saveButton = document.getElementById('medical-save');
        this.closeButton = document.getElementById('medical-close');
        this.cancelButton = document.getElementById('medical-cancel');
        this.presentation = document.getElementById('medical-presentation');
        this.presentationClose = document.getElementById('medical-presentation-close');

        this.addButton?.addEventListener('click', () => this.openNew());
        this.closeButton?.addEventListener('click', () => this.close());
        this.cancelButton?.addEventListener('click', () => this.close());
        this.modal?.addEventListener('close', () => this.resetForm());
        this.presentationClose?.addEventListener('click', () => this.closePresentation());
        this.presentation?.addEventListener('close', () => this.releasePresentation());

        this.searchInput?.addEventListener('input', () => {
            this.search = this.searchInput.value;
            this.render();
        });

        this.form?.addEventListener('submit', (event) => this.save(event));
        this.medicationAdd?.addEventListener('click', () => this.addMedicationRow());
        this.appointmentAdd?.addEventListener('click', () => this.addAppointmentRow());
        this.medicationsList?.addEventListener('click', (event) => {
            const remove = event.target.closest('[data-med-remove]');
            if (remove) {
                remove.closest('[data-med-row]')?.remove();
            }
        });
        this.appointmentsList?.addEventListener('click', (event) => {
            const remove = event.target.closest('[data-appointment-remove]');
            if (remove) {
                remove.closest('[data-appointment-row]')?.remove();
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
    }

    setItems(items) {
        this.items = (Array.isArray(items) ? items : []).filter(isMedicalItem);
        this.render();
    }

    reset() {
        this.items = [];
        this.search = '';
        this.closePresentation();
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

    addMedicationRow(medication = {}) {
        if (!this.medicationsList) {
            return;
        }

        const row = el('div', 'rounded-xl border border-outline-variant/60 bg-surface-container-low p-3');
        row.dataset.medRow = '';
        row.dataset.medId = medication.id || newUuid();

        const grid = el('div', 'grid gap-2 sm:grid-cols-2');
        const name = this.inputField(t('Medicamento'), 'text', {
            placeholder: t('Losartán'),
            maxlength: 120,
            value: medication.name || '',
        });
        const dose = this.inputField(t('Dosis'), 'text', {
            placeholder: t('50 mg'),
            maxlength: 80,
            value: medication.dose || '',
        });
        const times = this.inputField(t('Horas (separadas por comas)'), 'text', {
            placeholder: '08:00, 20:00',
            maxlength: 120,
            value: (medication.times || []).join(', '),
        });
        name.input.dataset.medName = '';
        dose.input.dataset.medDose = '';
        times.input.dataset.medTimes = '';
        grid.append(name.wrapper, dose.wrapper, times.wrapper);

        const dates = el('div', 'mt-2 grid gap-2 sm:grid-cols-2');
        const start = this.inputField(t('Desde (opcional)'), 'date', { value: medication.startDate || '' });
        const end = this.inputField(t('Hasta (opcional)'), 'date', { value: medication.endDate || '' });
        start.input.dataset.medStart = '';
        end.input.dataset.medEnd = '';
        dates.append(start.wrapper, end.wrapper);

        const footer = el('div', 'mt-2 flex justify-end');
        const remove = el('button', 'mp-btn mp-btn-danger mp-btn-sm', t('Quitar'));
        remove.type = 'button';
        remove.dataset.medRemove = '';
        footer.appendChild(remove);

        row.append(grid, dates, footer);
        this.medicationsList.appendChild(row);

        if (!medication.name) {
            name.input.focus();
        }
    }

    addAppointmentRow(appointment = {}) {
        if (!this.appointmentsList) {
            return;
        }

        const row = el('div', 'rounded-xl border border-outline-variant/60 bg-surface-container-low p-3');
        row.dataset.appointmentRow = '';
        row.dataset.appointmentId = appointment.id || newUuid();

        const grid = el('div', 'grid gap-2 sm:grid-cols-2');
        const title = this.inputField(t('Motivo o especialidad'), 'text', {
            placeholder: t('Cardiólogo'),
            maxlength: 120,
            value: appointment.title || '',
        });
        const location = this.inputField(t('Lugar (opcional)'), 'text', {
            placeholder: t('Consultorio, clínica…'),
            maxlength: 180,
            value: appointment.location || '',
        });
        title.input.dataset.appointmentTitle = '';
        location.input.dataset.appointmentLocation = '';
        grid.append(title.wrapper, location.wrapper);

        const when = el('div', 'mt-2 grid gap-2 sm:grid-cols-2');
        const date = this.inputField(t('Fecha'), 'date', { value: appointment.date || '' });
        const time = this.inputField(t('Hora'), 'time', { value: appointment.time || '' });
        date.input.dataset.appointmentDate = '';
        time.input.dataset.appointmentTime = '';
        when.append(date.wrapper, time.wrapper);

        const options = el('div', 'mt-2 grid gap-2 sm:grid-cols-2');
        const notes = this.inputField(t('Notas (opcional)'), 'text', {
            placeholder: t('Llevar estudios…'),
            maxlength: 240,
            value: appointment.notes || '',
        });
        notes.input.dataset.appointmentNotes = '';
        const reminderWrap = el('label', 'block');
        reminderWrap.appendChild(el('span', 'mp-hint block', t('Avisarme')));
        const reminder = document.createElement('select');
        reminder.className = 'mp-select mt-1';
        reminder.dataset.appointmentReminder = '';
        for (const [value, label] of [
            ['1440', t('1 día antes')],
            ['120', t('2 horas antes')],
            ['30', t('30 minutos antes')],
        ]) {
            const option = document.createElement('option');
            option.value = value;
            option.textContent = label;
            option.selected = Number(appointment.reminderMinutes || 1440) === Number(value);
            reminder.appendChild(option);
        }
        reminderWrap.appendChild(reminder);
        options.append(notes.wrapper, reminderWrap);

        const footer = el('div', 'mt-2 flex justify-end');
        const remove = el('button', 'mp-btn mp-btn-danger mp-btn-sm', t('Quitar'));
        remove.type = 'button';
        remove.dataset.appointmentRemove = '';
        footer.appendChild(remove);

        row.append(grid, when, options, footer);
        this.appointmentsList.appendChild(row);

        if (!appointment.title) {
            title.input.focus();
        }
    }

    readMedications() {
        return Array.from(this.medicationsList?.querySelectorAll('[data-med-row]') || [])
            .map((row) => {
                const name = row.querySelector('[data-med-name]')?.value.trim() || '';
                const times = (row.querySelector('[data-med-times]')?.value || '')
                    .split(/[,\s;]+/)
                    .map((value) => value.trim().slice(0, 5))
                    .filter((value) => /^\d{1,2}:\d{2}$/.test(value))
                    .map((value) => value.padStart(5, '0'));
                return {
                    id: row.dataset.medId || newUuid(),
                    name,
                    dose: row.querySelector('[data-med-dose]')?.value.trim() || '',
                    times: [...new Set(times)],
                    start_date: row.querySelector('[data-med-start]')?.value || '',
                    end_date: row.querySelector('[data-med-end]')?.value || '',
                };
            })
            .filter((entry) => entry.name);
    }

    readAppointments() {
        return Array.from(this.appointmentsList?.querySelectorAll('[data-appointment-row]') || [])
            .map((row) => ({
                id: row.dataset.appointmentId || newUuid(),
                title: row.querySelector('[data-appointment-title]')?.value.trim() || '',
                date: row.querySelector('[data-appointment-date]')?.value || '',
                time: row.querySelector('[data-appointment-time]')?.value.slice(0, 5) || '',
                location: row.querySelector('[data-appointment-location]')?.value.trim() || '',
                notes: row.querySelector('[data-appointment-notes]')?.value.trim() || '',
                reminder_minutes: Number(row.querySelector('[data-appointment-reminder]')?.value || 1440),
            }))
            .filter((entry) => entry.title);
    }

    renderMedicationRows(schedule, legacy) {
        if (!this.medicationsList) {
            return;
        }
        this.medicationsList.replaceChildren();
        const rows = Array.isArray(schedule) && schedule.length > 0
            ? schedule
            : (legacy || []).map((name) => ({ name }));
        for (const medication of rows) {
            this.addMedicationRow(medication);
        }
        if (rows.length === 0) {
            this.addMedicationRow();
        }
    }

    renderAppointmentRows(appointments) {
        if (!this.appointmentsList) {
            return;
        }
        this.appointmentsList.replaceChildren();
        for (const appointment of appointments || []) {
            this.addAppointmentRow(appointment);
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
        this.modalTitle.textContent = t('Nuevo carnet médico');
        this.form.reset();
        this.idInput.value = '';
        this.versionInput.value = '';
        this.blobInput.value = '';
        this.renderMedicationRows([], []);
        this.renderAppointmentRows([]);
        this.renderFileState();
        this.hideError();
        this.modal.showModal();
        this.titleInput?.focus();
    }

    openEdit(item) {
        if (!this.modal) {
            return;
        }

        const medical = medicalFromItem(item);
        const contact = medical.emergencyContacts[0] || { name: '', relation: '', phone: '' };
        this.editing = item;
        window.dispatchEvent(new CustomEvent('mypass:item-open', { detail: { id: item.id } }));
        this.baseMetadata = item.metadata ? { ...item.metadata } : {};
        this.existingBlobId = item.blob_id || null;
        this.clearAttachment = false;
        this.fileLabel = item.blob_id ? (item.metadata?.original_name || '') : '';
        this.modalTitle.textContent = t('Editar carnet médico');
        this.form.reset();
        this.idInput.value = item.id;
        this.versionInput.value = String(item.version || '');
        this.blobInput.value = item.blob_id || '';
        this.titleInput.value = item.metadata?.title || '';
        this.holderInput.value = medical.holder;
        this.birthInput.value = medical.birthDate;
        this.bloodInput.value = medical.bloodType;
        this.organInput.checked = medical.organDonor;
        this.allergiesInput.value = medical.allergies.join(', ');
        this.conditionsInput.value = medical.conditions.join(', ');
        this.renderMedicationRows(medical.medicationSchedule, medical.medications);
        this.renderAppointmentRows(medical.appointments);
        this.contactName.value = contact.name;
        this.contactRelation.value = contact.relation;
        this.contactPhone.value = contact.phone;
        this.insuranceProvider.value = medical.insurance.provider;
        this.insurancePlan.value = medical.insurance.plan;
        this.insurancePolicy.value = medical.insurance.policyNumber;
        this.insuranceMember.value = medical.insurance.memberId;
        this.insurancePhone.value = medical.insurance.phone;
        this.doctorName.value = medical.doctor.name;
        this.doctorPhone.value = medical.doctor.phone;
        this.doctorClinic.value = medical.doctor.clinic;
        this.expiresInput.value = item.expires_at ? String(item.expires_at).slice(0, 10) : '';
        this.notesInput.value = medical.notes;
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
        this.medicationsList?.replaceChildren();
        this.appointmentsList?.replaceChildren();
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
            this.showError(t('Escribe un nombre para el carnet.'));
            this.titleInput.focus();
            return;
        }

        const file = this.fileInput?.files?.[0] || null;
        if (file && file.size > 25 * 1024 * 1024) {
            this.showError(t('El archivo supera los 25 MB.'));
            return;
        }

        const previous = this.editing ? medicalFromItem(this.editing) : null;
        const medicationSchedule = this.readMedications();
        const appointments = this.readAppointments();
        const contacts = previous?.emergencyContacts ? [...previous.emergencyContacts] : [];
        const primaryContact = {
            name: this.contactName.value.trim(),
            relation: this.contactRelation.value.trim(),
            phone: this.contactPhone.value.trim(),
        };
        if (primaryContact.name || primaryContact.phone) {
            contacts[0] = primaryContact;
        } else {
            contacts.shift();
        }

        const metadata = {
            ...this.baseMetadata,
            title,
            type: 'medical_card',
            expires_at: this.expiresInput.value || null,
            medical: {
                ...(this.baseMetadata.medical || {}),
                holder: this.holderInput.value.trim(),
                birth_date: this.birthInput.value || '',
                blood_type: this.bloodInput.value,
                allergies: splitList(this.allergiesInput.value),
                conditions: splitList(this.conditionsInput.value),
                medications: medicationSchedule.map((entry) => entry.name),
                medication_schedule: medicationSchedule,
                appointments,
                emergency_contacts: contacts,
                insurance: {
                    provider: this.insuranceProvider.value.trim(),
                    plan: this.insurancePlan.value.trim(),
                    policy_number: this.insurancePolicy.value.trim(),
                    member_id: this.insuranceMember.value.trim(),
                    phone: this.insurancePhone.value.trim(),
                },
                doctor: {
                    name: this.doctorName.value.trim(),
                    phone: this.doctorPhone.value.trim(),
                    clinic: this.doctorClinic.value.trim(),
                },
                organ_donor: this.organInput.checked,
                notes: this.notesInput.value.trim(),
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
                type: 'medical_card',
                metadata,
                file,
                blobId: file ? this.existingBlobId : (this.clearAttachment ? null : this.existingBlobId),
            });
            this.close();
            this.options.toast?.(t('Carnet médico guardado.'));
        } catch (error) {
            this.showError(error.message || t('No se pudo guardar el carnet médico.'));
        } finally {
            this.saveButton.disabled = false;
            this.saveButton.textContent = original;
        }
    }

    handleListClick(event) {
        const action = event.target.closest('[data-medical-action]');
        if (!action) {
            return;
        }

        const card = action.closest('[data-medical-id]');
        const item = this.items.find((candidate) => candidate.id === card?.dataset.medicalId);
        if (!item) {
            return;
        }

        const actionName = action.dataset.medicalAction;
        if (actionName === 'present') {
            this.present(item);
        } else if (actionName === 'edit') {
            this.openEdit(item);
        } else if (actionName === 'calendar') {
            if (!downloadMedicalCalendar(item, medicalFromItem(item))) {
                this.options.toast?.(t('Agrega una hora o una cita para crear recordatorios.'));
            }
        } else if (actionName === 'share') {
            this.options.openShare?.(item);
        } else if (actionName === 'download') {
            this.options.downloadItem?.(item, action);
        } else if (actionName === 'delete') {
            this.options.deleteItem?.(item, action);
        }
    }

    render() {
        if (!this.list) {
            return;
        }

        const query = this.search.trim().toLowerCase();
        const filtered = this.items
            .filter((item) => item.state !== 'transferred')
            .filter((item) => {
                if (!query) {
                    return true;
                }
                const medical = medicalFromItem(item);
                return [
                    item.metadata?.title,
                    medical.holder,
                    medical.insurance.provider,
                    medical.insurance.policyNumber,
                    medical.doctor.name,
                    ...medical.allergies,
                    ...medical.conditions,
                    ...medical.medications,
                    ...medical.medicationSchedule.map((entry) => `${entry.name} ${entry.dose}`),
                    ...medical.appointments.map((entry) => `${entry.title} ${entry.location}`),
                ].filter(Boolean).some((value) => String(value).toLowerCase().includes(query));
            })
            .sort((a, b) => String(a.metadata?.title || '').localeCompare(String(b.metadata?.title || ''), locale()));

        this.empty.hidden = filtered.length > 0;
        if (this.items.length === 0) {
            this.emptyTitle.textContent = t('Aún no tienes un carnet médico.');
            this.emptyCopy.textContent = t('Agrega tus datos de emergencia y tu seguro para tenerlos a mano.');
        } else if (filtered.length === 0) {
            this.emptyTitle.textContent = t('No hay carnets que coincidan.');
            this.emptyCopy.textContent = t('Prueba con otra búsqueda.');
        }

        this.list.replaceChildren();
        for (const item of filtered) {
            this.list.appendChild(this.renderCard(item));
        }
    }

    renderCard(item) {
        const medical = medicalFromItem(item);
        const contacts = medical.emergencyContacts.filter((contact) => contact.name || contact.phone);
        const contact = contacts[0];
        const expired = isExpired(item.expires_at);

        const card = el('li', 'mp-medical-card mp-fade-in');
        card.dataset.medicalId = item.id;

        const head = el('div', 'mp-medical-card__head');
        const names = el('div', 'min-w-0');
        names.appendChild(el('p', 'mp-medical-card__title', item.metadata?.title || t('Carnet médico')));
        names.appendChild(el('p', 'mp-medical-card__holder', medical.holder || t('Sin titular')));
        head.appendChild(names);
        const badge = el('span', 'mp-badge mp-badge-primary shrink-0');
        badge.textContent = expired ? t('Vencido') : t('Carnet médico');
        head.appendChild(badge);
        card.appendChild(head);

        const grid = el('div', 'mp-medical-card__grid');
        grid.appendChild(this.stat(t('Grupo sanguíneo'), medical.bloodType || '—'));
        grid.appendChild(this.stat(t('Donante'), medical.organDonor ? t('Sí') : t('No')));
        if (medical.allergies.length > 0) {
            grid.appendChild(this.stat(t('Alergias'), medical.allergies.join(', '), true));
        }
        if (medical.conditions.length > 0) {
            grid.appendChild(this.stat(t('Condiciones'), medical.conditions.join(', '), true));
        }
        const scheduledMeds = medical.medicationSchedule.filter((entry) => entry.name);
        if (scheduledMeds.length > 0) {
            grid.appendChild(this.stat(
                t('Medicamentos y horarios'),
                scheduledMeds.map((entry) => [
                    entry.name,
                    entry.dose,
                    entry.times?.length ? `(${entry.times.join(', ')})` : '',
                ].filter(Boolean).join(' ')).join(' · '),
                true,
            ));
        }
        const nextAppointment = medical.appointments
            .filter((entry) => entry.title && entry.date)
            .sort((a, b) => `${a.date} ${a.time || ''}`.localeCompare(`${b.date} ${b.time || ''}`))[0];
        if (nextAppointment) {
            grid.appendChild(this.stat(
                t('Próxima cita'),
                [nextAppointment.title, nextAppointment.date, nextAppointment.time, nextAppointment.location]
                    .filter(Boolean)
                    .join(' · '),
                true,
            ));
        }
        if (contact) {
            grid.appendChild(this.stat(
                t('Contacto de emergencia'),
                [contact.name, contact.relation, contact.phone].filter(Boolean).join(' · '),
                true,
            ));
        }
        if (medical.insurance.provider || medical.insurance.policyNumber) {
            grid.appendChild(this.stat(
                t('Seguro'),
                [medical.insurance.provider, medical.insurance.policyNumber].filter(Boolean).join(' · '),
                true,
            ));
        }
        if (item.expires_at) {
            grid.appendChild(this.stat(
                t('Vigencia'),
                formatDate(item.expires_at, locale()),
                true,
            ));
        }
        card.appendChild(grid);

        const actions = el('div', 'mp-medical-card__actions');
        actions.appendChild(this.actionButton(t('Presentar carnet'), 'present', 'badge'));
        actions.appendChild(this.actionButton(t('Editar'), 'edit', 'edit'));
        if (medicalCalendar(item, medical)) {
            actions.appendChild(this.actionButton(t('Agregar al calendario'), 'calendar', 'calendar_month'));
        }
        if (this.options.canShare?.(item) && item.state !== 'transferred') {
            actions.appendChild(this.actionButton(t('Compartir'), 'share', 'send'));
        }
        if (item.blob_id) {
            actions.appendChild(this.actionButton(t('Adjunto'), 'download', 'download'));
        }
        actions.appendChild(this.actionButton(t('Eliminar'), 'delete', 'delete', 'mp-btn-quiet'));
        card.appendChild(actions);

        if (medical.notes) {
            card.appendChild(el('p', 'relative z-10 mt-3 whitespace-pre-wrap text-[13px] leading-relaxed text-on-surface-variant', medical.notes));
        }

        return card;
    }

    stat(label, value, wide = false) {
        const node = el('div', `mp-medical-card__stat${wide ? ' mp-medical-card__wide' : ''}`);
        node.appendChild(el('span', 'mp-medical-card__label', label));
        node.appendChild(el('span', 'mp-medical-card__value', value));
        return node;
    }

    actionButton(label, actionName, iconName, extraClass = 'mp-btn-secondary') {
        const node = button(label, `mp-btn ${extraClass} mp-btn-sm`);
        node.dataset.medicalAction = actionName;
        node.replaceChildren(icon(iconName, 'text-[18px]'), document.createTextNode(label));
        return node;
    }

    present(item) {
        if (!this.presentation) {
            return;
        }

        const medical = medicalFromItem(item);
        const contact = medical.emergencyContacts.find((candidate) => candidate.name || candidate.phone);
        const insurance = [medical.insurance.provider, medical.insurance.plan, medical.insurance.policyNumber]
            .filter(Boolean)
            .join(' · ');
        const doctor = [medical.doctor.name, medical.doctor.clinic, medical.doctor.phone]
            .filter(Boolean)
            .join(' · ');

        this.presentingItem = item;
        document.getElementById('medical-presentation-title').textContent = item.metadata?.title || t('Carnet médico');
        document.getElementById('medical-presentation-holder').textContent = [
            medical.holder,
            medical.birthDate ? tf('Nacimiento: :date', { date: formatDate(medical.birthDate, locale()) }) : '',
        ].filter(Boolean).join(' · ');
        document.getElementById('medical-presentation-blood').textContent = medical.bloodType || '—';
        document.getElementById('medical-presentation-donor').textContent = medical.organDonor ? t('Sí') : t('No');
        document.getElementById('medical-presentation-allergies').textContent = medical.allergies.join(', ') || t('Sin alergias registradas');
        document.getElementById('medical-presentation-conditions').textContent = [
            medical.conditions.length ? tf('Condiciones: :value', { value: medical.conditions.join(', ') }) : '',
            medical.medications.length ? tf('Medicamentos: :value', { value: medical.medications.join(', ') }) : '',
        ].filter(Boolean).join(' · ') || t('Sin datos');
        document.getElementById('medical-presentation-contact').textContent = contact
            ? [contact.name, contact.relation, contact.phone].filter(Boolean).join(' · ')
            : t('Sin contacto registrado');
        document.getElementById('medical-presentation-insurance').textContent = insurance || t('Sin seguro registrado');
        document.getElementById('medical-presentation-doctor').textContent = doctor || t('Sin médico registrado');

        this.presentation.showModal();
        this.presentation.requestFullscreen?.().catch(() => {});
        this.requestWakeLock();
    }

    async requestWakeLock() {
        try {
            if ('wakeLock' in navigator) {
                this.wakeLock = await navigator.wakeLock.request('screen');
            }
        } catch {
            this.wakeLock = null;
        }
    }

    releasePresentation() {
        this.wakeLock?.release?.().catch(() => {});
        this.wakeLock = null;
        if (document.fullscreenElement) {
            document.exitFullscreen?.().catch(() => {});
        }
        this.presentingItem = null;
    }

    closePresentation() {
        if (this.presentation?.open) {
            this.presentation.close();
        }
        this.releasePresentation();
    }
}
