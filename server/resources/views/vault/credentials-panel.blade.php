<section id="panel-credentials" role="tabpanel" aria-labelledby="tab-btn-credentials" hidden>
    <div class="flex flex-col justify-between gap-4 xl:flex-row xl:items-end">
        <div class="max-w-2xl">
            <span class="mp-eyebrow">{{ __('Cuentas y accesos') }}</span>
            <h2 class="mp-title-xl mt-2">{{ __('Tus credenciales') }}</h2>
            <p class="mp-body mt-2">{{ __('Usuarios, contraseñas, códigos TOTP y archivos de respaldo cifrados en tu bóveda.') }}</p>
        </div>
        <button id="credential-add-open" type="button" class="mp-btn mp-btn-primary min-h-14 shrink-0">
            <x-icon name="add" class="text-[22px]" />
            {{ __('Agregar credencial') }}
        </button>
    </div>

    <div class="mp-card mt-5 flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-3">
            <span class="mp-icon-tile"><x-icon name="shield_lock" /></span>
            <div>
                <p class="text-[15px] font-bold text-on-surface">{{ __('Solo se descifran en este dispositivo') }}</p>
                <p class="text-sm text-on-surface-variant">{{ __('El servidor guarda las credenciales como texto cifrado.') }}</p>
            </div>
        </div>
        <span class="mp-badge mp-badge-primary shrink-0">
            <x-icon name="encrypted" class="text-[17px]" filled />
            {{ __('Cifrado local') }}
        </span>
    </div>

    <div class="mt-5 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div class="relative w-full max-w-md">
            <span class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-outline">
                <x-icon name="search" class="text-[21px]" />
            </span>
            <input id="credential-search" type="search" class="mp-input pl-12"
                   placeholder="{{ __('Buscar por servicio, usuario o etiqueta') }}"
                   aria-label="{{ __('Buscar credenciales') }}">
        </div>
        <div class="flex gap-2 overflow-x-auto pb-1" aria-label="{{ __('Filtrar credenciales') }}">
            <button type="button" data-credential-filter="all" aria-pressed="true" class="mp-filter min-h-10 shrink-0 px-4 py-1.5 text-[14px]">
                {{ __('Todas') }}
                <span data-credential-count="all" class="text-[12px] font-bold opacity-80">0</span>
            </button>
            <button type="button" data-credential-filter="favorite" aria-pressed="false" class="mp-filter min-h-10 shrink-0 px-4 py-1.5 text-[14px]">
                <x-icon name="star" class="text-[17px]" />
                {{ __('Favoritas') }}
                <span data-credential-count="favorite" class="text-[12px] font-bold opacity-80">0</span>
            </button>
            <button type="button" data-credential-filter="totp" aria-pressed="false" class="mp-filter min-h-10 shrink-0 px-4 py-1.5 text-[14px]">
                <x-icon name="timer" class="text-[17px]" />
                {{ __('Con TOTP') }}
                <span data-credential-count="totp" class="text-[12px] font-bold opacity-80">0</span>
            </button>
        </div>
    </div>

    <div id="credentials-empty" class="mp-empty mt-5" hidden>
        <span class="flex h-16 w-16 items-center justify-center rounded-full bg-primary-fixed text-primary">
            <x-icon name="key" class="text-[32px]" />
        </span>
        <p id="credentials-empty-title" class="mt-3 font-semibold text-on-surface">{{ __('Aún no tienes credenciales.') }}</p>
        <p id="credentials-empty-copy" class="mp-hint mt-1">{{ __('Agrega tu primera cuenta y guarda su contraseña cifrada.') }}</p>
    </div>

    <ul id="credential-list" class="mp-credential-list mt-5"></ul>
</section>

<dialog id="credential-modal" class="mp-dialog mp-dialog--wide">
    <form id="credential-form" class="mp-dialog-panel max-w-3xl" novalidate>
        <input type="hidden" id="credential-item-id">
        <input type="hidden" id="credential-item-version">
        <input type="hidden" id="credential-blob-id">

        <div class="flex items-start justify-between gap-4">
            <div>
                <span class="mp-eyebrow">{{ __('Bóveda segura') }}</span>
                <h2 id="credential-modal-title" class="mp-title-lg mt-1">{{ __('Nueva credencial') }}</h2>
                <p class="mp-hint mt-1">{{ __('La contraseña viaja cifrada antes de guardarse.') }}</p>
            </div>
            <button id="credential-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                <x-icon name="close" class="text-[22px]" />
            </button>
        </div>

        <p id="credential-error" class="mp-alert mp-alert-error mt-4" role="alert" hidden></p>

        <div class="mt-4 grid gap-4 sm:grid-cols-2">
            <div class="sm:col-span-2">
                <label for="credential-title" class="mp-label">{{ __('Servicio o cuenta') }}</label>
                <input id="credential-title" type="text" required maxlength="120" class="mp-input"
                       placeholder="{{ __('Banco, correo, red social…') }}" autocomplete="off" spellcheck="false">
            </div>
            <div>
                <label for="credential-category" class="mp-label">{{ __('Tipo de cuenta') }}</label>
                <select id="credential-category" class="mp-select">
                    <option value="login">{{ __('Acceso general') }}</option>
                    <option value="bank">{{ __('Banco y finanzas') }}</option>
                    <option value="email">{{ __('Correo electrónico') }}</option>
                    <option value="social">{{ __('Red social') }}</option>
                    <option value="work">{{ __('Trabajo') }}</option>
                    <option value="wifi">{{ __('Wi-Fi') }}</option>
                    <option value="server">{{ __('Servidor o API') }}</option>
                    <option value="other">{{ __('Otra') }}</option>
                </select>
            </div>
            <div>
                <label for="credential-username" class="mp-label">{{ __('Usuario o correo') }}</label>
                <input id="credential-username" type="text" maxlength="180" class="mp-input"
                       autocomplete="off" spellcheck="false" placeholder="usuario@ejemplo.com">
            </div>
            <div class="sm:col-span-2">
                <label for="credential-password" class="mp-label">{{ __('Contraseña') }}</label>
                <div class="flex flex-col gap-2 sm:flex-row">
                    <div class="relative flex-1">
                        <input id="credential-password" type="password" maxlength="512" class="mp-input pr-14"
                               autocomplete="new-password" spellcheck="false" autocapitalize="off">
                        <button id="credential-password-toggle" type="button" class="absolute right-1.5 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-lg text-on-surface-variant hover:bg-surface-container"
                                aria-label="{{ __('Mostrar contraseña') }}" aria-pressed="false">
                            <x-icon name="visibility" class="text-[22px]" />
                        </button>
                    </div>
                    <button id="credential-generate" type="button" class="mp-btn mp-btn-secondary shrink-0">
                        <x-icon name="auto_awesome" class="text-[20px]" />
                        {{ __('Generar') }}
                    </button>
                </div>
                <p id="credential-password-strength" class="mp-hint mt-1" role="status"></p>
            </div>
            <div class="sm:col-span-2">
                <label for="credential-url" class="mp-label">{{ __('Sitio web (opcional)') }}</label>
                <input id="credential-url" type="url" maxlength="500" class="mp-input"
                       placeholder="https://ejemplo.com" autocomplete="off" spellcheck="false">
            </div>
            <div class="sm:col-span-2">
                <label for="credential-totp" class="mp-label">{{ __('Secreto TOTP (opcional)') }}</label>
                <input id="credential-totp" type="text" maxlength="512" class="mp-input font-mono"
                       placeholder="JBSWY3DPEHPK3PXP o otpauth://totp/..." autocomplete="off" spellcheck="false">
                <p class="mp-hint mt-1">{{ __('Pega el secreto o la URL otpauth de tu app de autenticación. Se guarda cifrado.') }}</p>
            </div>
            <div class="sm:col-span-2">
                <label for="credential-tags" class="mp-label">{{ __('Etiquetas (separadas por comas)') }}</label>
                <input id="credential-tags" type="text" maxlength="240" class="mp-input"
                       placeholder="{{ __('personal, trabajo, banca…') }}" autocomplete="off">
            </div>
            <div class="sm:col-span-2">
                <label for="credential-notes" class="mp-label">{{ __('Notas') }}</label>
                <textarea id="credential-notes" rows="3" maxlength="2000" class="mp-input resize-y"
                          placeholder="{{ __('Preguntas de seguridad, instrucciones, etc.') }}"></textarea>
            </div>
            <div class="sm:col-span-2">
                <label for="credential-file" class="mp-label">{{ __('Archivo complementario (opcional)') }}</label>
                <input id="credential-file" type="file" class="mp-input file:mr-3 file:rounded-lg file:border-0 file:bg-primary-fixed file:px-3 file:py-2 file:text-sm file:font-semibold file:text-primary">
                <div id="credential-file-current" class="mt-2 flex flex-wrap items-center gap-2 text-sm text-on-surface-variant" hidden>
                    <x-icon name="attach_file" class="text-[18px]" />
                    <span id="credential-file-name" class="truncate"></span>
                    <button id="credential-file-clear" type="button" class="mp-btn mp-btn-quiet mp-btn-sm">{{ __('Quitar') }}</button>
                </div>
                <p class="mp-hint mt-1">{{ __('Se cifra igual que el resto de la bóveda. Máximo 25 MB.') }}</p>
            </div>
        </div>

        <div class="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button id="credential-cancel" type="button" class="mp-btn mp-btn-secondary">{{ __('Cancelar') }}</button>
            <button id="credential-save" type="submit" class="mp-btn mp-btn-primary">
                <x-icon name="lock" class="text-[20px]" />
                {{ __('Guardar credencial') }}
            </button>
        </div>
    </form>
</dialog>
