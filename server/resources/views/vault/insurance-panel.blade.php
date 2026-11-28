<section id="panel-insurance" role="tabpanel" aria-labelledby="tab-btn-insurance" hidden>
    <div class="flex flex-col justify-between gap-4 xl:flex-row xl:items-end">
        <div class="max-w-2xl">
            <span class="mp-eyebrow">{{ __('Protección y siniestros') }}</span>
            <h2 class="mp-title-xl mt-2">{{ __('Seguros y pólizas') }}</h2>
            <p class="mp-body mt-2">{{ __('Ten a la mano tu póliza, los teléfonos de siniestros y los datos del bien asegurado.') }}</p>
        </div>
        <div class="flex flex-col gap-2 min-[440px]:flex-row">
            <button id="insurance-emergency-open" type="button" class="mp-btn mp-btn-secondary min-h-14 shrink-0">
                <x-icon name="emergency" class="text-[22px]" />
                {{ __('Emergencia') }}
            </button>
            <button id="insurance-add-open" type="button" class="mp-btn mp-btn-primary min-h-14 shrink-0">
                <x-icon name="add" class="text-[22px]" />
                {{ __('Agregar seguro') }}
            </button>
        </div>
    </div>

    <div id="insurance-reminder-banner" class="mp-alert mp-alert-warning mt-5" role="status" hidden>
        <x-icon name="alarm" class="mt-0.5 text-[22px]" />
        <div class="flex-1">
            <p class="font-semibold">{{ __('Seguros y pagos próximos') }}</p>
            <ul id="insurance-reminder-list" class="mt-1 list-inside list-disc space-y-1 text-[15px]"></ul>
        </div>
    </div>

    <div class="mp-card mt-5 flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-3">
            <span class="mp-icon-tile"><x-icon name="shield" /></span>
            <div>
                <p class="text-[15px] font-bold text-on-surface">{{ __('Pólizas y datos de siniestro cifrados de extremo a extremo') }}</p>
                <p class="text-sm text-on-surface-variant">{{ __('Solo se muestran con la bóveda desbloqueada.') }}</p>
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
            <input id="insurance-search" type="search" class="mp-input pl-12"
                   placeholder="{{ __('Buscar por aseguradora, póliza, placas o teléfono') }}"
                   aria-label="{{ __('Buscar seguros') }}">
        </div>
        <span class="mp-hint">{{ __('Puedes guardar una póliza por vehículo, casa, persona o plan médico.') }}</span>
    </div>

    <div class="mt-5 flex flex-wrap gap-2" aria-label="{{ __('Filtrar seguros') }}">
        @php($insuranceFilters = [
            'all' => __('Todos'),
            'health' => __('Salud'),
            'major_medical' => __('Gastos médicos mayores'),
            'auto' => __('Auto'),
            'motorcycle' => __('Moto'),
            'home' => __('Hogar'),
            'life' => __('Vida'),
            'travel' => __('Viaje'),
            'pet' => __('Mascotas'),
            'business' => __('Negocio'),
            'other' => __('Otro'),
        ])
        @foreach ($insuranceFilters as $filter => $label)
            <button type="button" data-insurance-filter="{{ $filter }}" aria-pressed="{{ $filter === 'all' ? 'true' : 'false' }}"
                    class="mp-filter">
                {{ $label }}
                <span data-insurance-count="{{ $filter }}" class="text-sm font-bold opacity-80">0</span>
            </button>
        @endforeach
    </div>

    <div id="insurance-empty" class="mp-empty mt-5" hidden>
        <span class="flex h-16 w-16 items-center justify-center rounded-full bg-primary-fixed text-primary">
            <x-icon name="shield" class="text-[32px]" />
        </span>
        <p id="insurance-empty-title" class="mt-3 font-semibold text-on-surface">{{ __('Aún no tienes un seguro guardado.') }}</p>
        <p id="insurance-empty-copy" class="mp-hint mt-1">{{ __('Agrega tu primera póliza para tener los datos y teléfonos a la mano en un siniestro.') }}</p>
    </div>

    <div id="insurance-list" class="mp-deck-list mt-5"></div>
</section>

<dialog id="insurance-modal" class="mp-dialog mp-dialog--wide" aria-labelledby="insurance-modal-title">
    <form id="insurance-form" class="mp-dialog-panel max-w-4xl" novalidate>
        <input type="hidden" id="insurance-item-id">
        <input type="hidden" id="insurance-item-version">
        <input type="hidden" id="insurance-blob-id">

        <div class="flex items-start justify-between gap-4">
            <div>
                <span class="mp-eyebrow">{{ __('Protección y siniestros') }}</span>
                <h2 id="insurance-modal-title" class="mp-title-lg mt-1">{{ __('Nuevo seguro') }}</h2>
                <p class="mp-hint mt-1">{{ __('La póliza y sus datos se cifran antes de salir de tu equipo.') }}</p>
            </div>
            <button id="insurance-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                <x-icon name="close" class="text-[22px]" />
            </button>
        </div>

        <p id="insurance-error" class="mp-alert mp-alert-error mt-4" role="alert" hidden></p>

        <div class="mt-4 grid gap-4 sm:grid-cols-2">
            <div class="sm:col-span-2">
                <label for="insurance-title" class="mp-label">{{ __('Nombre del seguro') }}</label>
                <input id="insurance-title" type="text" required maxlength="140" class="mp-input"
                       placeholder="{{ __('Seguro de auto — Versa') }}" autocomplete="off">
            </div>
            <div>
                <label for="insurance-category" class="mp-label">{{ __('Tipo de seguro') }}</label>
                <select id="insurance-category" class="mp-select">
                    <option value="health">{{ __('Salud') }}</option>
                    <option value="major_medical">{{ __('Gastos médicos mayores') }}</option>
                    <option value="auto">{{ __('Auto') }}</option>
                    <option value="motorcycle">{{ __('Moto') }}</option>
                    <option value="home">{{ __('Hogar') }}</option>
                    <option value="life">{{ __('Vida') }}</option>
                    <option value="travel">{{ __('Viaje') }}</option>
                    <option value="pet">{{ __('Mascotas') }}</option>
                    <option value="business">{{ __('Negocio') }}</option>
                    <option value="other">{{ __('Otro') }}</option>
                </select>
            </div>
            <div>
                <label for="insurance-status" class="mp-label">{{ __('Estado') }}</label>
                <select id="insurance-status" class="mp-select">
                    <option value="active">{{ __('Vigente') }}</option>
                    <option value="pending">{{ __('Por renovar') }}</option>
                    <option value="expired">{{ __('Vencida') }}</option>
                    <option value="cancelled">{{ __('Cancelada') }}</option>
                </select>
            </div>
            <div>
                <label for="insurance-provider" class="mp-label">{{ __('Aseguradora') }}</label>
                <input id="insurance-provider" type="text" maxlength="120" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-product" class="mp-label">{{ __('Producto o plan') }}</label>
                <input id="insurance-product" type="text" maxlength="120" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-policy" class="mp-label">{{ __('Número de póliza') }}</label>
                <input id="insurance-policy" type="text" maxlength="80" class="mp-input font-mono" autocomplete="off">
            </div>
            <div>
                <label for="insurance-member" class="mp-label">{{ __('Número de afiliado o contrato') }}</label>
                <input id="insurance-member" type="text" maxlength="80" class="mp-input font-mono" autocomplete="off">
            </div>
            <div class="sm:col-span-2">
                <label for="insurance-holder" class="mp-label">{{ __('Titular') }}</label>
                <input id="insurance-holder" type="text" maxlength="160" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-valid-from" class="mp-label">{{ __('Vigencia desde') }}</label>
                <input id="insurance-valid-from" type="date" class="mp-input">
            </div>
            <div>
                <label for="insurance-valid-until" class="mp-label">{{ __('Vigencia hasta') }}</label>
                <input id="insurance-valid-until" type="date" class="mp-input">
            </div>

            <div class="sm:col-span-2 mt-1 border-t border-outline-variant/60 pt-4">
                <h3 class="mp-title-sm">{{ __('Teléfonos para un siniestro') }}</h3>
                <p class="mp-hint mt-1">{{ __('Estos números aparecerán en el modo siniestro para llamar con un toque.') }}</p>
            </div>
            <div>
                <label for="insurance-emergency-phone" class="mp-label">{{ __('Teléfono de siniestros') }}</label>
                <input id="insurance-emergency-phone" type="tel" maxlength="40" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-assistance-phone" class="mp-label">{{ __('Asistencia 24 horas') }}</label>
                <input id="insurance-assistance-phone" type="tel" maxlength="40" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-agent-name" class="mp-label">{{ __('Agente o ajustador') }}</label>
                <input id="insurance-agent-name" type="text" maxlength="120" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-agent-phone" class="mp-label">{{ __('Teléfono del agente') }}</label>
                <input id="insurance-agent-phone" type="tel" maxlength="40" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-email" class="mp-label">{{ __('Correo de la aseguradora') }}</label>
                <input id="insurance-email" type="email" maxlength="160" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-website" class="mp-label">{{ __('Portal o app') }}</label>
                <input id="insurance-website" type="text" maxlength="240" class="mp-input" autocomplete="off"
                       placeholder="https://">
            </div>

            <div class="sm:col-span-2 mt-1 border-t border-outline-variant/60 pt-4">
                <div class="flex flex-wrap items-end justify-between gap-2">
                    <div>
                        <h3 class="mp-title-sm">{{ __('Datos del bien asegurado') }}</h3>
                        <p class="mp-hint mt-1">{{ __('Vehículo, casa, persona, mascota o negocio.') }}</p>
                    </div>
                    <button id="insurance-subject-add" type="button" class="mp-btn mp-btn-secondary mp-btn-sm">
                        <x-icon name="add" class="text-[18px]" />
                        {{ __('Agregar dato') }}
                    </button>
                </div>
                <div id="insurance-subject-list" class="mt-3 space-y-3"></div>
            </div>

            <div class="sm:col-span-2 mt-1 border-t border-outline-variant/60 pt-4">
                <div class="flex flex-wrap items-end justify-between gap-2">
                    <div>
                        <h3 class="mp-title-sm">{{ __('Coberturas') }}</h3>
                        <p class="mp-hint mt-1">{{ __('Anota lo principal que cubre la póliza.') }}</p>
                    </div>
                    <button id="insurance-coverage-add" type="button" class="mp-btn mp-btn-secondary mp-btn-sm">
                        <x-icon name="add" class="text-[18px]" />
                        {{ __('Agregar cobertura') }}
                    </button>
                </div>
                <div id="insurance-coverages-list" class="mt-3 space-y-3"></div>
            </div>

            <div class="sm:col-span-2">
                <h3 class="mp-title-sm">{{ __('Pago de la póliza') }}</h3>
            </div>
            <div>
                <label for="insurance-premium" class="mp-label">{{ __('Prima') }}</label>
                <input id="insurance-premium" type="text" inputmode="decimal" maxlength="40" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-currency" class="mp-label">{{ __('Moneda') }}</label>
                <input id="insurance-currency" type="text" maxlength="8" class="mp-input" value="MXN" autocomplete="off">
            </div>
            <div>
                <label for="insurance-payment-frequency" class="mp-label">{{ __('Frecuencia de pago') }}</label>
                <select id="insurance-payment-frequency" class="mp-select">
                    <option value="">{{ __('No indicada') }}</option>
                    <option value="monthly">{{ __('Mensual') }}</option>
                    <option value="quarterly">{{ __('Trimestral') }}</option>
                    <option value="semiannual">{{ __('Semestral') }}</option>
                    <option value="annual">{{ __('Anual') }}</option>
                    <option value="single">{{ __('Pago único') }}</option>
                </select>
            </div>
            <div>
                <label for="insurance-next-payment" class="mp-label">{{ __('Próximo pago') }}</label>
                <input id="insurance-next-payment" type="date" class="mp-input">
            </div>
            <div>
                <label for="insurance-reminder-days" class="mp-label">{{ __('Avisarme antes') }}</label>
                <select id="insurance-reminder-days" class="mp-select">
                    <option value="1">{{ __('1 día antes') }}</option>
                    <option value="7" selected>{{ __('7 días antes') }}</option>
                    <option value="15">{{ __('15 días antes') }}</option>
                    <option value="30">{{ __('30 días antes') }}</option>
                </select>
                <p class="mp-hint mt-1">{{ __('Aplica a la vigencia y al próximo pago.') }}</p>
            </div>
            <div>
                <label for="insurance-insured-sum" class="mp-label">{{ __('Suma asegurada') }}</label>
                <input id="insurance-insured-sum" type="text" maxlength="60" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-deductible" class="mp-label">{{ __('Deducible o coaseguro') }}</label>
                <input id="insurance-deductible" type="text" maxlength="80" class="mp-input" autocomplete="off">
            </div>

            <div class="sm:col-span-2">
                <label for="insurance-notes" class="mp-label">{{ __('Notas para un siniestro') }}</label>
                <textarea id="insurance-notes" rows="3" maxlength="3000" class="mp-input resize-y"
                          placeholder="{{ __('Qué hacer, qué tener a mano, instrucciones de la aseguradora…') }}"></textarea>
            </div>

            <div>
                <label for="insurance-file" class="mp-label">{{ __('Póliza en PDF o foto') }}</label>
                <input id="insurance-file" type="file" accept="application/pdf,image/*" class="mp-input file:mr-3 file:rounded-lg file:border-0 file:bg-primary-fixed file:px-3 file:py-2 file:text-sm file:font-semibold file:text-primary">
            </div>
            <div class="flex items-end">
                <p class="mp-hint">{{ __('Un solo archivo por seguro. Se guarda cifrado.') }}</p>
            </div>
            <div id="insurance-file-current" class="sm:col-span-2 flex flex-wrap items-center gap-2 text-sm text-on-surface-variant" hidden>
                <x-icon name="attach_file" class="text-[18px]" />
                <span id="insurance-file-name" class="truncate"></span>
                <button id="insurance-file-clear" type="button" class="mp-btn mp-btn-quiet mp-btn-sm">{{ __('Quitar') }}</button>
            </div>
        </div>

        <div class="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button id="insurance-cancel" type="button" class="mp-btn mp-btn-secondary">{{ __('Cancelar') }}</button>
            <button id="insurance-save" type="submit" class="mp-btn mp-btn-primary">
                <x-icon name="lock" class="text-[20px]" />
                {{ __('Guardar seguro') }}
            </button>
        </div>
    </form>
</dialog>

<dialog id="insurance-claims-modal" class="mp-dialog mp-dialog--wide" aria-labelledby="insurance-claims-title">
    <div class="mp-dialog-panel max-w-3xl">
        <div class="flex items-start justify-between gap-4">
            <div>
                <span class="mp-eyebrow">{{ __('Historial de siniestros') }}</span>
                <h2 id="insurance-claims-title" class="mp-title-lg mt-1">{{ __('Siniestros') }}</h2>
                <p id="insurance-claims-subtitle" class="mp-hint mt-1"></p>
            </div>
            <button id="insurance-claims-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                <x-icon name="close" class="text-[22px]" />
            </button>
        </div>

        <p id="insurance-claims-error" class="mp-alert mp-alert-error mt-4" role="alert" hidden></p>
        <div id="insurance-claims-empty" class="mp-empty mt-4" hidden>
            <x-icon name="receipt_long" class="text-[32px] text-outline" />
            <p class="mt-2 font-semibold">{{ __('Todavía no hay siniestros registrados.') }}</p>
            <p class="mp-hint mt-1">{{ __('Registra el reporte para tener el número, el estado y el contacto del ajustador a la mano.') }}</p>
        </div>
        <div id="insurance-claims-list" class="mt-4 space-y-3"></div>

        <div class="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button id="insurance-claims-done" type="button" class="mp-btn mp-btn-secondary">{{ __('Cerrar') }}</button>
            <button id="insurance-claim-add" type="button" class="mp-btn mp-btn-primary">
                <x-icon name="add" class="text-[20px]" />
                {{ __('Registrar siniestro') }}
            </button>
        </div>
    </div>
</dialog>

<dialog id="insurance-claim-modal" class="mp-dialog mp-dialog--wide" aria-labelledby="insurance-claim-modal-title">
    <form id="insurance-claim-form" class="mp-dialog-panel max-w-2xl" novalidate>
        <input type="hidden" id="insurance-claim-id">
        <div class="flex items-start justify-between gap-4">
            <div>
                <span class="mp-eyebrow">{{ __('Historial de siniestros') }}</span>
                <h2 id="insurance-claim-modal-title" class="mp-title-lg mt-1">{{ __('Nuevo siniestro') }}</h2>
                <p class="mp-hint mt-1">{{ __('El reporte se guarda dentro de esta póliza, cifrado.') }}</p>
            </div>
            <button id="insurance-claim-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                <x-icon name="close" class="text-[22px]" />
            </button>
        </div>

        <p id="insurance-claim-error" class="mp-alert mp-alert-error mt-4" role="alert" hidden></p>

        <div class="mt-4 grid gap-4 sm:grid-cols-2">
            <div>
                <label for="insurance-claim-date" class="mp-label">{{ __('Fecha del siniestro') }}</label>
                <input id="insurance-claim-date" type="date" class="mp-input">
            </div>
            <div>
                <label for="insurance-claim-number" class="mp-label">{{ __('Número de reporte') }}</label>
                <input id="insurance-claim-number" type="text" maxlength="80" class="mp-input font-mono" autocomplete="off">
            </div>
            <div>
                <label for="insurance-claim-type" class="mp-label">{{ __('Motivo') }}</label>
                <input id="insurance-claim-type" type="text" maxlength="120" class="mp-input" autocomplete="off"
                       placeholder="{{ __('Choque, robo, daños, atención médica…') }}">
            </div>
            <div>
                <label for="insurance-claim-status" class="mp-label">{{ __('Estado') }}</label>
                <select id="insurance-claim-status" class="mp-select">
                    <option value="open">{{ __('Abierto') }}</option>
                    <option value="review">{{ __('En revisión') }}</option>
                    <option value="approved">{{ __('Aprobado') }}</option>
                    <option value="rejected">{{ __('Rechazado') }}</option>
                    <option value="closed">{{ __('Cerrado') }}</option>
                </select>
            </div>
            <div class="sm:col-span-2">
                <label for="insurance-claim-description" class="mp-label">{{ __('Descripción') }}</label>
                <textarea id="insurance-claim-description" rows="2" maxlength="1500" class="mp-input resize-y"></textarea>
            </div>
            <div>
                <label for="insurance-claim-adjuster-name" class="mp-label">{{ __('Ajustador') }}</label>
                <input id="insurance-claim-adjuster-name" type="text" maxlength="120" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-claim-adjuster-phone" class="mp-label">{{ __('Teléfono del ajustador') }}</label>
                <input id="insurance-claim-adjuster-phone" type="tel" maxlength="40" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-claim-amount" class="mp-label">{{ __('Monto estimado') }}</label>
                <input id="insurance-claim-amount" type="text" inputmode="decimal" maxlength="40" class="mp-input" autocomplete="off">
            </div>
            <div>
                <label for="insurance-claim-notes" class="mp-label">{{ __('Notas') }}</label>
                <input id="insurance-claim-notes" type="text" maxlength="500" class="mp-input" autocomplete="off">
            </div>
        </div>

        <div class="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button id="insurance-claim-cancel" type="button" class="mp-btn mp-btn-secondary">{{ __('Cancelar') }}</button>
            <button id="insurance-claim-save" type="submit" class="mp-btn mp-btn-primary">
                <x-icon name="save" class="text-[20px]" />
                {{ __('Guardar siniestro') }}
            </button>
        </div>
    </form>
</dialog>

<dialog id="insurance-presentation" class="mp-dialog mp-medical-presentation mp-insurance-presentation">
    <div class="mp-medical-presentation__card">
        <div class="flex items-start justify-between gap-4">
            <div>
                <span id="insurance-presentation-category" class="mp-eyebrow"></span>
                <h2 id="insurance-presentation-title" class="mt-1 text-[clamp(1.8rem,3vw,2.6rem)] font-bold leading-tight"></h2>
                <p id="insurance-presentation-provider" class="mt-1 text-lg text-on-surface-variant"></p>
            </div>
            <button id="insurance-presentation-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                <x-icon name="close" class="text-[26px]" />
            </button>
        </div>

        <div id="insurance-presentation-expired" class="mp-alert mp-alert-warning mt-4" role="status" hidden>
            <x-icon name="warning" class="mt-0.5 text-[22px]" />
            <span>{{ __('Esta póliza está vencida o cancelada. Verifica la vigencia antes de reportar el siniestro.') }}</span>
        </div>

        <div class="mt-5 grid gap-3 sm:grid-cols-2">
            <a id="insurance-presentation-call" class="mp-btn mp-btn-primary min-h-16 text-[18px]" href="#" hidden>
                <x-icon name="call" class="text-[26px]" />
                <span id="insurance-presentation-call-label">{{ __('Llamar a siniestros') }}</span>
            </a>
            <a id="insurance-presentation-assistance" class="mp-btn mp-btn-secondary min-h-16 text-[18px]" href="#" hidden>
                <x-icon name="support_agent" class="text-[26px]" />
                <span id="insurance-presentation-assistance-label">{{ __('Asistencia 24 h') }}</span>
            </a>
        </div>

        <div class="mt-4 rounded-2xl border border-primary/20 bg-primary-fixed/50 p-4">
            <span class="mp-medical-presentation__label">{{ __('Número de póliza') }}</span>
            <div class="mt-1 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <p id="insurance-presentation-policy" class="mp-insurance-policy-number"></p>
                <button id="insurance-presentation-copy-policy" type="button" class="mp-btn mp-btn-secondary mp-btn-sm shrink-0">
                    <x-icon name="content_copy" class="text-[18px]" />
                    {{ __('Copiar') }}
                </button>
            </div>
        </div>

        <div id="insurance-presentation-grid" class="mt-4 grid gap-3 sm:grid-cols-2"></div>

        <div id="insurance-presentation-claim-box" class="mt-4 rounded-2xl border border-warning/40 bg-warning-container/60 p-4" hidden>
            <span class="mp-medical-presentation__label">{{ __('Siniestro activo') }}</span>
            <p id="insurance-presentation-claim" class="mt-1 text-[19px] font-semibold text-on-surface"></p>
            <a id="insurance-presentation-adjuster" class="mt-3 inline-flex min-h-12 items-center gap-2 font-semibold text-primary" href="#" hidden></a>
        </div>

        <div id="insurance-presentation-notes-box" class="mt-4 rounded-2xl border border-outline-variant/60 bg-surface-container-low p-4" hidden>
            <span class="mp-medical-presentation__label">{{ __('Qué hacer en caso de siniestro') }}</span>
            <p id="insurance-presentation-notes" class="mt-2 whitespace-pre-wrap text-[16px] leading-relaxed text-on-surface"></p>
        </div>

        <div class="mt-5 flex flex-wrap gap-2">
            <button id="insurance-presentation-open-file" type="button" class="mp-btn mp-btn-secondary" hidden>
                <x-icon name="picture_as_pdf" class="text-[20px]" />
                {{ __('Abrir póliza') }}
            </button>
            <button id="insurance-presentation-copy-summary" type="button" class="mp-btn mp-btn-secondary">
                <x-icon name="content_copy" class="text-[20px]" />
                {{ __('Copiar datos') }}
            </button>
            <button id="insurance-presentation-share" type="button" class="mp-btn mp-btn-secondary" hidden>
                <x-icon name="send" class="text-[20px]" />
                {{ __('Compartir') }}
            </button>
        </div>

        <p class="mt-5 text-sm text-on-surface-variant">{{ __('Pantalla activa mientras atiendes el siniestro. Mantén el dispositivo bajo tu control.') }}</p>
    </div>
</dialog>

<dialog id="insurance-emergency" class="mp-dialog mp-medical-presentation mp-insurance-presentation">
    <div class="mp-medical-presentation__card">
        <div class="flex items-start justify-between gap-4">
            <div>
                <span class="mp-eyebrow">{{ __('Acceso rápido') }}</span>
                <h2 class="mt-1 text-[clamp(1.8rem,3vw,2.6rem)] font-bold leading-tight">{{ __('Emergencia') }}</h2>
                <p class="mt-1 text-lg text-on-surface-variant">{{ __('Toca el teléfono de la póliza que necesites.') }}</p>
            </div>
            <button id="insurance-emergency-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                <x-icon name="close" class="text-[26px]" />
            </button>
        </div>

        <div id="insurance-emergency-empty" class="mp-empty mt-5" hidden>
            <x-icon name="shield" class="text-[32px] text-outline" />
            <p class="mt-2 font-semibold">{{ __('Todavía no hay teléfonos de seguros guardados.') }}</p>
        </div>
        <div id="insurance-emergency-list" class="mt-5 space-y-3"></div>

        <p class="mt-5 text-sm text-on-surface-variant">{{ __('Mantén el dispositivo bajo tu control. Esta pantalla se apaga al cerrarla.') }}</p>
    </div>
</dialog>
