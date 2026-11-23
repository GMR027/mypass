<section id="panel-medical" role="tabpanel" aria-labelledby="tab-btn-medical" hidden>
    <div class="flex flex-col justify-between gap-4 xl:flex-row xl:items-end">
        <div class="max-w-2xl">
            <span class="mp-eyebrow">{{ __('Salud y emergencias') }}</span>
            <h2 class="mp-title-xl mt-2">{{ __('Carnets médicos') }}</h2>
            <p class="mp-body mt-2">{{ __('Datos de emergencia, seguro médico y contactos importantes, siempre cifrados.') }}</p>
        </div>
        <button id="medical-add-open" type="button" class="mp-btn mp-btn-primary min-h-14 shrink-0">
            <x-icon name="add" class="text-[22px]" />
            {{ __('Agregar carnet') }}
        </button>
    </div>

    <div class="mp-card mt-5 flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-3">
            <span class="mp-icon-tile"><x-icon name="health_and_safety" /></span>
            <div>
                <p class="text-[15px] font-bold text-on-surface">{{ __('Información sensible, cifrada de extremo a extremo') }}</p>
                <p class="text-sm text-on-surface-variant">{{ __('Para mostrarla, la bóveda debe estar desbloqueada.') }}</p>
            </div>
        </div>
        <span class="mp-badge mp-badge-primary shrink-0">
            <x-icon name="encrypted" class="text-[17px]" filled />
            {{ __('Cifrado local') }}
        </span>
    </div>

    <div class="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div class="relative w-full max-w-md">
            <span class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-outline">
                <x-icon name="search" class="text-[21px]" />
            </span>
            <input id="medical-search" type="search" class="mp-input pl-12"
                   placeholder="{{ __('Buscar por titular, seguro o alergia') }}"
                   aria-label="{{ __('Buscar carnets médicos') }}">
        </div>
        <span class="mp-hint">{{ __('Puedes guardar un carnet por persona o por seguro.') }}</span>
    </div>

    <div id="medical-empty" class="mp-empty mt-5" hidden>
        <span class="flex h-16 w-16 items-center justify-center rounded-full bg-primary-fixed text-primary">
            <x-icon name="medical_services" class="text-[32px]" />
        </span>
        <p id="medical-empty-title" class="mt-3 font-semibold text-on-surface">{{ __('Aún no tienes un carnet médico.') }}</p>
        <p id="medical-empty-copy" class="mp-hint mt-1">{{ __('Agrega tus datos de emergencia y tu seguro para tenerlos a mano.') }}</p>
    </div>

    <ul id="medical-list" class="mp-medical-list mt-5"></ul>
</section>

<dialog id="medical-modal" class="mp-dialog mp-dialog--wide">
    <form id="medical-form" class="mp-dialog-panel max-w-3xl" novalidate>
        <input type="hidden" id="medical-item-id">
        <input type="hidden" id="medical-item-version">
        <input type="hidden" id="medical-blob-id">

        <div class="flex items-start justify-between gap-4">
            <div>
                <span class="mp-eyebrow">{{ __('Salud y emergencias') }}</span>
                <h2 id="medical-modal-title" class="mp-title-lg mt-1">{{ __('Nuevo carnet médico') }}</h2>
                <p class="mp-hint mt-1">{{ __('Se guarda cifrado y solo se muestra al desbloquear la bóveda.') }}</p>
            </div>
            <button id="medical-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                <x-icon name="close" class="text-[22px]" />
            </button>
        </div>

        <p id="medical-error" class="mp-alert mp-alert-error mt-4" role="alert" hidden></p>

        <div class="mt-4 grid gap-4 sm:grid-cols-2">
            <div class="sm:col-span-2">
                <label for="medical-title" class="mp-label">{{ __('Nombre del carnet') }}</label>
                <input id="medical-title" type="text" required maxlength="120" class="mp-input"
                       placeholder="{{ __('Carnet médico — Edgar') }}" autocomplete="off">
            </div>
            <div>
                <label for="medical-holder" class="mp-label">{{ __('Titular') }}</label>
                <input id="medical-holder" type="text" maxlength="160" class="mp-input"
                       placeholder="{{ __('Nombre completo') }}" autocomplete="off">
            </div>
            <div>
                <label for="medical-birth" class="mp-label">{{ __('Fecha de nacimiento') }}</label>
                <input id="medical-birth" type="date" class="mp-input">
            </div>
            <div>
                <label for="medical-blood" class="mp-label">{{ __('Grupo sanguíneo') }}</label>
                <select id="medical-blood" class="mp-select">
                    <option value="">{{ __('No indicado') }}</option>
                    <option value="O+">O+</option>
                    <option value="O-">O−</option>
                    <option value="A+">A+</option>
                    <option value="A-">A−</option>
                    <option value="B+">B+</option>
                    <option value="B-">B−</option>
                    <option value="AB+">AB+</option>
                    <option value="AB-">AB−</option>
                </select>
            </div>
            <div class="flex items-end">
                <label class="flex min-h-14 w-full cursor-pointer items-center gap-3 rounded-xl border border-outline-variant/70 bg-surface-container-low px-4">
                    <input id="medical-organ-donor" type="checkbox" class="h-5 w-5 accent-primary">
                    <span class="font-semibold text-on-surface">{{ __('Donante de órganos') }}</span>
                </label>
            </div>
            <div class="sm:col-span-2">
                <label for="medical-allergies" class="mp-label">{{ __('Alergias (separadas por comas)') }}</label>
                <input id="medical-allergies" type="text" maxlength="500" class="mp-input"
                       placeholder="{{ __('penicilina, látex…') }}" autocomplete="off">
            </div>
            <div class="sm:col-span-2">
                <label for="medical-conditions" class="mp-label">{{ __('Condiciones o diagnósticos') }}</label>
                <input id="medical-conditions" type="text" maxlength="500" class="mp-input"
                       placeholder="{{ __('asma, diabetes…') }}" autocomplete="off">
            </div>
            <div class="sm:col-span-2">
                <label for="medical-medications" class="mp-label">{{ __('Medicamentos actuales') }}</label>
                <input id="medical-medications" type="text" maxlength="500" class="mp-input"
                       placeholder="{{ __('salbutamol, metformina…') }}" autocomplete="off">
            </div>

            <div class="sm:col-span-2">
                <h3 class="mp-title-sm">{{ __('Contacto de emergencia principal') }}</h3>
            </div>
            <div>
                <label for="medical-contact-name" class="mp-label">{{ __('Nombre') }}</label>
                <input id="medical-contact-name" type="text" maxlength="120" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="medical-contact-relation" class="mp-label">{{ __('Parentesco') }}</label>
                <input id="medical-contact-relation" type="text" maxlength="80" class="mp-input"
                       placeholder="{{ __('Esposa, hijo, amigo…') }}" autocomplete="off">
            </div>
            <div class="sm:col-span-2">
                <label for="medical-contact-phone" class="mp-label">{{ __('Teléfono') }}</label>
                <input id="medical-contact-phone" type="tel" maxlength="40" class="mp-input"
                       placeholder="+52 55 0000 0000" autocomplete="off">
            </div>

            <div class="sm:col-span-2">
                <h3 class="mp-title-sm">{{ __('Seguro médico') }}</h3>
            </div>
            <div>
                <label for="medical-insurance-provider" class="mp-label">{{ __('Aseguradora') }}</label>
                <input id="medical-insurance-provider" type="text" maxlength="120" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="medical-insurance-plan" class="mp-label">{{ __('Plan o cobertura') }}</label>
                <input id="medical-insurance-plan" type="text" maxlength="120" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="medical-insurance-policy" class="mp-label">{{ __('Número de póliza') }}</label>
                <input id="medical-insurance-policy" type="text" maxlength="80" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="medical-insurance-member" class="mp-label">{{ __('Número de afiliado') }}</label>
                <input id="medical-insurance-member" type="text" maxlength="80" class="mp-input" autocomplete="off">
            </div>
            <div class="sm:col-span-2">
                <label for="medical-insurance-phone" class="mp-label">{{ __('Teléfono de la aseguradora') }}</label>
                <input id="medical-insurance-phone" type="tel" maxlength="40" class="mp-input" autocomplete="off">
            </div>

            <div class="sm:col-span-2">
                <h3 class="mp-title-sm">{{ __('Médico de cabecera') }}</h3>
            </div>
            <div>
                <label for="medical-doctor-name" class="mp-label">{{ __('Nombre') }}</label>
                <input id="medical-doctor-name" type="text" maxlength="120" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="medical-doctor-phone" class="mp-label">{{ __('Teléfono') }}</label>
                <input id="medical-doctor-phone" type="tel" maxlength="40" class="mp-input" autocomplete="off">
            </div>
            <div class="sm:col-span-2">
                <label for="medical-doctor-clinic" class="mp-label">{{ __('Clínica u hospital') }}</label>
                <input id="medical-doctor-clinic" type="text" maxlength="160" class="mp-input" autocomplete="off">
            </div>

            <div>
                <label for="medical-expires" class="mp-label">{{ __('Vigencia del carnet o seguro') }}</label>
                <input id="medical-expires" type="date" class="mp-input">
            </div>
            <div>
                <label for="medical-file" class="mp-label">{{ __('Foto o PDF del carnet') }}</label>
                <input id="medical-file" type="file" accept="application/pdf,image/*" class="mp-input file:mr-3 file:rounded-lg file:border-0 file:bg-primary-fixed file:px-3 file:py-2 file:text-sm file:font-semibold file:text-primary">
            </div>
            <div id="medical-file-current" class="sm:col-span-2 flex flex-wrap items-center gap-2 text-sm text-on-surface-variant" hidden>
                <x-icon name="attach_file" class="text-[18px]" />
                <span id="medical-file-name" class="truncate"></span>
                <button id="medical-file-clear" type="button" class="mp-btn mp-btn-quiet mp-btn-sm">{{ __('Quitar') }}</button>
            </div>
            <div class="sm:col-span-2">
                <label for="medical-notes" class="mp-label">{{ __('Notas para emergencias') }}</label>
                <textarea id="medical-notes" rows="3" maxlength="2000" class="mp-input resize-y"
                          placeholder="{{ __('Instrucciones, voluntad anticipada, etc.') }}"></textarea>
            </div>
        </div>

        <div class="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button id="medical-cancel" type="button" class="mp-btn mp-btn-secondary">{{ __('Cancelar') }}</button>
            <button id="medical-save" type="submit" class="mp-btn mp-btn-primary">
                <x-icon name="lock" class="text-[20px]" />
                {{ __('Guardar carnet') }}
            </button>
        </div>
    </form>
</dialog>

<dialog id="medical-presentation" class="mp-dialog mp-medical-presentation">
    <div class="mp-medical-presentation__card">
        <div class="flex items-start justify-between gap-4">
            <div>
                <span class="mp-eyebrow">{{ __('Carnet médico') }}</span>
                <h2 id="medical-presentation-title" class="mt-1 text-[clamp(1.8rem,3vw,2.6rem)] font-bold leading-tight"></h2>
                <p id="medical-presentation-holder" class="mt-1 text-lg text-on-surface-variant"></p>
            </div>
            <button id="medical-presentation-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                <x-icon name="close" class="text-[26px]" />
            </button>
        </div>

        <div class="mt-6 grid gap-4 sm:grid-cols-2">
            <div class="mp-medical-presentation__box">
                <span class="mp-medical-presentation__label">{{ __('Grupo sanguíneo') }}</span>
                <p id="medical-presentation-blood" class="mp-medical-presentation__value"></p>
            </div>
            <div class="mp-medical-presentation__box">
                <span class="mp-medical-presentation__label">{{ __('Donante de órganos') }}</span>
                <p id="medical-presentation-donor" class="mp-medical-presentation__value"></p>
            </div>
            <div class="mp-medical-presentation__box sm:col-span-2">
                <span class="mp-medical-presentation__label">{{ __('Alergias') }}</span>
                <p id="medical-presentation-allergies" class="mp-medical-presentation__value"></p>
            </div>
            <div class="mp-medical-presentation__box sm:col-span-2">
                <span class="mp-medical-presentation__label">{{ __('Condiciones y medicamentos') }}</span>
                <p id="medical-presentation-conditions" class="mp-medical-presentation__value"></p>
            </div>
            <div class="mp-medical-presentation__box sm:col-span-2">
                <span class="mp-medical-presentation__label">{{ __('Contacto de emergencia') }}</span>
                <p id="medical-presentation-contact" class="mp-medical-presentation__value"></p>
            </div>
            <div class="mp-medical-presentation__box sm:col-span-2">
                <span class="mp-medical-presentation__label">{{ __('Seguro médico') }}</span>
                <p id="medical-presentation-insurance" class="mp-medical-presentation__value"></p>
            </div>
            <div class="mp-medical-presentation__box sm:col-span-2">
                <span class="mp-medical-presentation__label">{{ __('Médico de cabecera') }}</span>
                <p id="medical-presentation-doctor" class="mp-medical-presentation__value"></p>
            </div>
        </div>

        <p class="mt-5 text-sm text-on-surface-variant">{{ __('Pantalla activa mientras presentas el carnet. Mantén el dispositivo bajo tu control.') }}</p>
    </div>
</dialog>
