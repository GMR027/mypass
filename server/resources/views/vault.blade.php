<x-layout :title="__('Mi bóveda')">
    @php
        $email = auth()->user()->email;
        $vaultKeys = auth()->user()->keys()->first();
        $emailRecoveryEnabled = $vaultKeys?->emailRecoveryEnabled() ?? false;
        $easyMode = auth()->user()->usesEasyMode();
    @endphp

    <div id="vault-app"
         data-easy-mode="{{ $easyMode ? '1' : '0' }}"
         data-email-recovery-enabled="{{ $emailRecoveryEnabled ? '1' : '0' }}"
         data-recovery-request-url="{{ route('web.vault-recovery.request') }}"
         data-recovery-verify-url="{{ route('web.vault-recovery.verify') }}"
         data-keys-url="{{ route('web.keys.show') }}"
         data-items-url="{{ route('web.items.index') }}"
         data-blobs-url="{{ route('web.blobs.store') }}"
         data-blob-base="/api/web/blobs"
         data-pairing-url="{{ route('web.pairing.create') }}"
         data-pairing-base="/api/web/pairing"
         data-devices-url="{{ route('web.devices.index') }}"
         data-device-base="/api/web/devices"
         data-contacts-url="{{ route('web.contacts.index') }}"
         data-shares-url="{{ route('web.shares.index') }}"
         data-share-base="/api/web/shares"
         data-requests-url="{{ route('web.share-requests.index') }}"
         data-request-base="/api/web/share-requests"
         data-email="{{ $email }}">

        <p id="vault-error" class="mp-alert mp-alert-error mb-5" role="alert" hidden></p>

        <p id="vault-loading" class="mp-card mp-card-pad flex items-center gap-4 text-on-surface" role="status">
            <span class="mp-spinner" aria-hidden="true"></span>
            <span>
                <span class="block font-semibold">{{ __('Cargando…') }}</span>
                <span class="mp-hint">{{ __('Verificando llaves seguras en este dispositivo.') }}</span>
            </span>
        </p>

        <section id="vault-no-keys" hidden>
            <div class="mp-hero-panel mx-auto max-w-3xl p-6 text-center sm:p-10">
                <span class="mx-auto flex h-16 w-16 items-center justify-center rounded-[1.5rem] bg-white/10 text-white">
                    <x-icon name="shield_lock" class="text-[34px]" />
                </span>
                <span class="mp-hero-pill mt-5">
                    <span class="h-1.5 w-1.5 rounded-full bg-accent"></span>
                    {{ __('Tu billetera digital') }}
                </span>
                <h1 class="mx-auto mt-4 max-w-xl text-[clamp(1.9rem,3vw,2.8rem)] font-bold leading-[1.05] tracking-[-0.04em] text-white">{{ __('Tu bóveda aún no está configurada') }}</h1>
                <p class="mx-auto mt-3 max-w-lg text-[17px] leading-relaxed text-primary-fixed-dim">{{ __('Primero crea la llave de la bóveda y guarda tu llave de emergencia en esta computadora. Después podrás abrir tus pases en un toque.') }}</p>
                <a href="{{ route('onboarding') }}" class="mp-btn mp-btn-primary mt-6">
                    <x-icon name="vpn_key" class="text-[21px]" />
                    {{ __('Configurar mi bóveda') }}
                </a>
            </div>
        </section>

        <section id="vault-locked" hidden>
            <div class="mp-hero-panel mb-5 p-6 sm:p-8 lg:p-10">
                <div class="mp-hero-copy grid items-center gap-8 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)]">
                    <div>
                        <span class="mp-hero-pill">
                            <span class="h-1.5 w-1.5 rounded-full bg-accent"></span>
                            {{ __('Tu billetera está protegida') }}
                        </span>
                        <h1 class="mt-4 text-[clamp(2rem,3.4vw,3.1rem)] font-bold leading-[1.02] tracking-[-0.045em] text-white">
                            {{ __('Tus pases te esperan.') }}
                        </h1>
                        <p class="mt-4 max-w-xl text-lg leading-relaxed text-primary-fixed-dim">
                            {{ __('Escribe tu llave de bóveda y entra directo a tus pases, boletos y tarjetas. El contenido se descifra únicamente en este navegador.') }}
                        </p>
                        <p class="mt-5 flex items-center gap-2 text-sm text-white/60">
                            <x-icon name="shield_lock" class="text-[18px]" />
                            {{ $email }}
                        </p>
                    </div>
                    <div class="relative mx-auto w-full max-w-[340px]">
                        <div class="rounded-[1.75rem] border border-white/15 bg-white/10 p-4 backdrop-blur-sm">
                            <div class="rounded-[1.25rem] bg-paper p-4 shadow-2xl">
                                <div class="flex items-center justify-between gap-2">
                                    <span class="mp-badge mp-badge-primary">
                                        <x-icon name="wallet" class="text-[16px]" filled />
                                        {{ __('Mi billetera') }}
                                    </span>
                                    <x-icon name="lock" class="text-[20px] text-outline" />
                                </div>
                                <div class="mt-4 space-y-2.5">
                                    <div class="h-3 w-3/4 rounded-full bg-surface-container-high"></div>
                                    <div class="h-3 w-1/2 rounded-full bg-surface-container"></div>
                                </div>
                                <div class="mt-4 rounded-xl bg-surface-container-low p-3">
                                    <div class="flex items-end justify-between">
                                        <span class="font-mono text-[15px] font-bold tracking-[0.2em] text-on-surface">•••• ••••</span>
                                        <x-icon name="qr_code_2" class="text-[36px] text-primary" />
                                    </div>
                                </div>
                                <p class="mt-3 text-center text-[11px] font-semibold text-on-surface-variant">{{ __('Desbloquea para ver tus pases') }}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div id="trusted-device-card" class="mp-card mp-card-pad mb-5" hidden>
                <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                    <div class="flex items-start gap-3">
                        <span class="mp-icon-tile bg-primary-fixed text-primary">
                            <x-icon name="verified_user" class="text-[25px]" filled />
                        </span>
                        <div>
                            <h2 class="mp-title-sm">{{ __('Este equipo ya está autorizado') }}</h2>
                            <p class="mp-hint mt-1">{{ __('Abre tu bóveda sin escribir la llave. Solo funciona en este navegador.') }}</p>
                        </div>
                    </div>
                    <div class="flex flex-wrap gap-2">
                        <button id="trusted-unlock" type="button" class="mp-btn mp-btn-primary">
                            <x-icon name="lock_open" class="text-[21px]" />
                            {{ __('Abrir mi bóveda') }}
                        </button>
                        <button id="trusted-forget" type="button" class="mp-btn mp-btn-quiet">
                            <x-icon name="delete" class="text-[20px]" />
                            {{ __('Olvidar este equipo') }}
                        </button>
                    </div>
                </div>
            </div>

            <div id="email-recovery-card" class="mp-card mp-card-pad mb-5" @if (! $emailRecoveryEnabled) hidden @endif>
                <div class="flex items-start gap-3">
                    <span class="mp-icon-tile bg-primary-fixed text-primary">
                        <x-icon name="mark_email_read" class="text-[25px]" />
                    </span>
                    <div class="min-w-0 flex-1">
                        <h2 class="mp-title-sm">{{ __('Recuperar con mi correo') }}</h2>
                        <p class="mp-hint mt-1">{{ __('Si olvidaste tu llave, te enviamos un código a :email y abrimos tu bóveda.', ['email' => $email]) }}</p>

                        <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
                            <button id="email-recovery-send" type="button" class="mp-btn mp-btn-primary shrink-0">
                                <x-icon name="send" class="text-[21px]" />
                                {{ __('Enviar código a mi correo') }}
                            </button>
                            <div class="flex-1">
                                <label for="email-recovery-code" class="mp-label">{{ __('Código de 6 dígitos') }}</label>
                                <input id="email-recovery-code" inputmode="numeric" autocomplete="one-time-code"
                                       maxlength="6" class="mp-input text-center font-mono tracking-[0.3em]">
                            </div>
                            <button id="email-recovery-verify" type="button" class="mp-btn mp-btn-secondary shrink-0">
                                <x-icon name="key" class="text-[21px]" />
                                {{ __('Abrir bóveda') }}
                            </button>
                        </div>

                        <p id="email-recovery-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
                        <p id="email-recovery-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>
                    </div>
                </div>
            </div>

            <div class="grid gap-5 lg:grid-cols-2">
                <div class="mp-card mp-card-pad">
                    <span class="mp-badge mp-badge-warning">
                        <x-icon name="lock" class="text-[17px]" />
                        {{ __('Bóveda bloqueada') }}
                    </span>
                    <h2 class="mp-title-md mt-3">{{ __('Abrir mi bóveda') }}</h2>
                    <p class="mp-body-sm mt-2">{{ __('Escribe la llave de la bóveda que creaste en esta computadora.') }}</p>

                    <p id="share-pending" class="mp-alert mp-alert-info mt-5" role="status" hidden>
                        <x-icon name="upload_file" class="mt-0.5 text-[21px]" />
                        <span id="share-pending-text"></span>
                    </p>

                    <form id="unlock-form" class="mt-6 space-y-4">
                        <div>
                            <label for="vault-passphrase" class="mp-label">{{ __('Llave de la bóveda') }}</label>
                            <div class="relative">
                                <input id="vault-passphrase" type="password" required autocomplete="current-password"
                                       class="mp-input pr-14">
                                <button type="button" data-toggle-password="vault-passphrase" aria-pressed="false"
                                        aria-label="{{ __('Mostrar contraseña') }}"
                                        class="absolute right-1.5 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-lg text-on-surface-variant hover:bg-surface-container">
                                    <x-icon name="visibility" class="text-[22px]" />
                                </button>
                            </div>
                        </div>

                        <label class="flex items-start gap-3 rounded-xl border border-outline-variant/60 bg-surface-container-low px-4 py-3">
                            <input id="remember-device" type="checkbox" class="mp-checkbox mt-0.5" @checked($easyMode)>
                            <span class="text-[15px] leading-relaxed">
                                <strong class="block font-semibold text-on-surface">{{ __('Abrir sin escribir la llave la próxima vez') }}</strong>
                                <span class="mp-hint">{{ __('Solo en este navegador. Puedes olvidarlo desde esta misma pantalla.') }}</span>
                            </span>
                        </label>

                        <p id="unlock-error" class="mp-alert mp-alert-error" role="alert" hidden></p>

                        <button type="submit" class="mp-btn mp-btn-primary w-full">
                            <x-icon name="lock_open" class="text-[21px]" />
                            {{ __('Desbloquear y ver mis pases') }}
                        </button>
                    </form>

                    <p class="mt-4 flex items-start gap-2 text-sm text-on-surface-variant">
                        <x-icon name="security" class="mt-0.5 text-[19px] text-primary" />
                        {{ __('Llave descifrada localmente en este navegador.') }}
                    </p>
                </div>

                <div class="mp-card mp-card-pad">
                    <div class="flex items-start gap-3">
                        <span class="mp-icon-tile">
                            <x-icon name="vpn_key_alert" class="text-[23px]" />
                        </span>
                        <div>
                            <h2 class="mp-title-sm">{{ __('Abrir con archivo de emergencia') }}</h2>
                            <p class="mp-hint mt-1">
                                {!! __("También puedes abrir la bóveda cargando el archivo :file que descargaste.", ['file' => '<strong>.mypss</strong>']) !!}
                            </p>
                        </div>
                    </div>

                    <label id="recovery-drop-zone"
                           class="mt-5 block cursor-pointer rounded-2xl border-2 border-dashed border-outline/30 bg-surface-container-low/70 px-5 py-8 text-center transition-colors hover:border-primary hover:bg-primary-fixed/30">
                        <span class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-paper text-primary shadow-sm">
                            <x-icon name="upload_file" class="text-[28px]" />
                        </span>
                        <span class="mt-3 block font-semibold text-on-surface">{{ __('Arrastra el archivo .mypss aquí') }}</span>
                        <span class="mt-1 block text-sm text-on-surface-variant">{{ __('o haz clic para elegirlo') }}</span>
                        <span class="mt-2 block text-sm text-outline">{{ __('Archivo de recuperación personal') }}</span>
                        <input id="recovery-file" type="file" accept=".mypss,application/json" class="sr-only">
                    </label>

                    <p id="recovery-file-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
                    <p id="recovery-file-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>

                    <div class="mt-5 flex items-start gap-3 rounded-xl bg-surface-container-low p-4">
                        <x-icon name="cloud_off" class="mt-0.5 text-[21px] text-primary" />
                        <p class="mp-hint">{{ __('Cero servidores involucrados: el archivo se verifica y descifra en tu navegador.') }}</p>
                    </div>
                </div>
            </div>

            <div class="mp-card mp-card-pad mt-5">
                <div class="flex flex-col justify-between gap-4 md:flex-row md:items-start">
                    <div class="flex items-start gap-3">
                        <span class="mp-icon-tile">
                            <x-icon name="dialpad" class="text-[23px]" />
                        </span>
                        <div>
                            <h2 class="mp-title-sm">{{ __('Abrir con llave de emergencia') }}</h2>
                            <p class="mp-hint mt-1">{{ __('Si guardaste el número de 16 o 32 dígitos, escríbelo aquí. No necesitas el archivo .mypss.') }}</p>
                        </div>
                    </div>
                    <span class="mp-badge mp-badge-neutral shrink-0">{{ __('Solo en este navegador') }}</span>
                </div>

                <form id="emergency-key-form" class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
                    <div class="flex-1">
                        <label for="emergency-key" class="mp-label">{{ __('Llave de emergencia') }}</label>
                        <input id="emergency-key" type="text" inputmode="numeric" autocomplete="off"
                               maxlength="64" class="mp-input font-mono tracking-[0.12em]"
                               placeholder="0000 0000 0000 0000 0000 0000 0000 0000">
                    </div>
                    <button type="submit" class="mp-btn mp-btn-secondary shrink-0">
                        <x-icon name="lock_open" class="text-[20px]" />
                        {{ __('Abrir bóveda') }}
                    </button>
                </form>

                <p id="emergency-key-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
                <p id="emergency-key-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>
            </div>
        </section>

        <section id="vault-unlocked" hidden>
            <div class="mp-card mp-card-pad flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
                <div class="flex items-center gap-4">
                    <span class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-primary-fixed text-primary">
                        <x-icon name="lock_open" class="text-[30px]" />
                    </span>
                    <div>
                        <div class="flex flex-wrap items-center gap-2">
                            <h1 class="mp-title-md">{{ __('Mi bóveda') }}</h1>
                            <span class="mp-badge mp-badge-primary">
                                <x-icon name="verified_user" class="text-[16px]" filled />
                                {{ __('Bóveda abierta') }}
                            </span>
                        </div>
                        <p class="mp-hint mt-1 mp-break">{{ $email }} · {{ __('Cifrado de punto a punto activo en este navegador.') }}</p>
                    </div>
                </div>
                <div class="grid w-full grid-cols-1 gap-2 min-[360px]:grid-cols-2 sm:flex sm:w-auto sm:flex-wrap">
                    <button id="vault-search-open" type="button" class="mp-btn mp-btn-quiet w-full sm:w-auto">
                        <x-icon name="search" class="text-[20px]" />
                        {{ __('Buscar') }}
                    </button>
                    <button id="backup-open" type="button" class="mp-btn mp-btn-quiet w-full sm:w-auto">
                        <x-icon name="save" class="text-[20px]" />
                        {{ __('Respaldo') }}
                    </button>
                    <button id="trash-open" type="button" class="mp-btn mp-btn-quiet w-full sm:w-auto">
                        <x-icon name="delete" class="text-[20px]" />
                        {{ __('Papelera') }}
                    </button>
                    <button id="vault-lock" type="button" class="mp-btn mp-btn-secondary w-full sm:w-auto">
                        <x-icon name="lock" class="text-[20px]" />
                        {{ __('Bloquear') }}
                    </button>
                </div>
            </div>

            <div class="mp-tabs mt-5" role="tablist" aria-label="{{ __('Secciones de la bóveda') }}">
                <button id="tab-btn-docs" type="button" role="tab" aria-selected="true" aria-controls="panel-docs" data-tab="docs" class="mp-tab">
                    <x-icon name="folder" class="text-[22px]" />
                    {{ __('Documentos') }}
                </button>
                <button id="tab-btn-passes" type="button" role="tab" aria-selected="false" aria-controls="panel-passes" data-tab="pases" class="mp-tab">
                    <x-icon name="wallet" class="text-[22px]" />
                    {{ __('Pases') }}
                </button>
                <button id="tab-btn-credentials" type="button" role="tab" aria-selected="false" aria-controls="panel-credentials" data-tab="credentials" class="mp-tab">
                    <x-icon name="key" class="text-[22px]" />
                    {{ __('Credenciales') }}
                </button>
                <button id="tab-btn-medical" type="button" role="tab" aria-selected="false" aria-controls="panel-medical" data-tab="medical" class="mp-tab">
                    <x-icon name="medical_services" class="text-[22px]" />
                    {{ __('Salud') }}
                </button>
                <button id="tab-btn-family" type="button" role="tab" aria-selected="false" aria-controls="panel-family" data-tab="family" class="mp-tab">
                    <x-icon name="groups" class="text-[22px]" />
                    {{ __('Familia') }}
                </button>
                <button id="tab-btn-devices" type="button" role="tab" aria-selected="false" aria-controls="panel-devices" data-tab="devices" class="mp-tab">
                    <x-icon name="devices" class="text-[22px]" />
                    {{ __('Dispositivos') }}
                </button>
            </div>

            <div class="mt-5">
                <section id="panel-docs" role="tabpanel" aria-labelledby="tab-btn-docs">
                    <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
                        <div>
                            <h2 class="mp-title-lg">{{ __('Tus documentos seguros') }}</h2>
                            <p class="mp-body-sm mt-1">{{ __('Cifrados en tu dispositivo localmente. Solo legibles por ti y tus autorizados.') }}</p>
                        </div>
                        <button id="add-document-open" type="button" class="mp-btn mp-btn-primary shrink-0">
                            <x-icon name="add" class="text-[22px]" />
                            {{ __('Agregar documento') }}
                        </button>
                    </div>

                    <div id="expiry-banner" class="mp-alert mp-alert-warning mt-5" role="status" hidden>
                        <x-icon name="alarm" class="mt-0.5 text-[22px]" />
                        <div class="flex-1">
                            <p class="font-semibold">{{ __('Documentos por vencer') }}</p>
                            <ul id="expiry-list" class="mt-1 list-inside list-disc space-y-1 text-[15px]"></ul>
                        </div>
                    </div>

                    <div class="mt-5 flex flex-wrap gap-2" aria-label="{{ __('Filtrar documentos') }}">
                        @php($filters = [
                            'all' => __('Todos'),
                            'document' => __('Documentos'),
                            'ticket' => __('Boletos y pases'),
                            'card' => __('Tarjetas'),
                            'other' => __('Otros'),
                        ])
                        @foreach ($filters as $filter => $label)
                            <button type="button" data-filter="{{ $filter }}" aria-pressed="{{ $filter === 'all' ? 'true' : 'false' }}"
                                    class="mp-filter">
                                {{ $label }}
                                <span data-filter-count="{{ $filter }}" class="text-sm font-bold opacity-80">0</span>
                            </button>
                        @endforeach
                    </div>

                    <div class="relative mt-4 max-w-md">
                        <span class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-outline">
                            <x-icon name="search" class="text-[21px]" />
                        </span>
                        <input id="doc-search" type="search" class="mp-input pl-12"
                               placeholder="{{ __('Buscar por título o archivo') }}"
                               aria-label="{{ __('Buscar documentos') }}">
                    </div>

                    <div id="items-empty" class="mp-empty mt-5" hidden>
                        <span class="flex h-14 w-14 items-center justify-center rounded-full bg-surface-container-high text-outline">
                            <x-icon name="folder_open" class="text-[28px]" />
                        </span>
                        <p id="items-empty-title" class="mt-3 font-semibold text-on-surface">{{ __('No tienes documentos todavía.') }}</p>
                        <p id="items-empty-copy" class="mp-hint mt-1">{{ __('Agrega tu primer documento y quedará cifrado antes de salir de tu equipo.') }}</p>
                    </div>

                    <div id="item-list" class="mp-deck-list mt-5"></div>
                </section>

                @include('vault.passes-panel')
                @include('vault.pass-generator')
                @include('vault.credentials-panel')
                @include('vault.medical-panel')

                <section id="panel-family" role="tabpanel" aria-labelledby="tab-btn-family" hidden>
                    <div class="space-y-5">
                        <section class="mp-card mp-card-pad">
                            <div class="flex items-start gap-4">
                                <span class="mp-icon-tile">
                                    <x-icon name="person_add" class="text-[24px]" />
                                </span>
                                <div>
                                    <h2 class="mp-title-md">{{ __('Familia') }}</h2>
                                    <p class="mp-body-sm mt-1">{{ __('Agrega a una persona por su correo. Cada envío requiere aceptación.') }}</p>
                                </div>
                            </div>

                            <form id="contact-form" class="mt-5 flex flex-col gap-2 sm:flex-row">
                                <div class="relative flex-1">
                                    <span class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-outline">
                                        <x-icon name="mail" class="text-[21px]" />
                                    </span>
                                    <input id="contact-email" type="email" required placeholder="{{ __('correo@ejemplo.com') }}"
                                           class="mp-input pl-12">
                                </div>
                                <button type="submit" class="mp-btn mp-btn-primary shrink-0">
                                    <x-icon name="person_add" class="text-[20px]" />
                                    {{ __('Agregar contacto') }}
                                </button>
                            </form>

                            <p id="contact-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
                            <p id="contact-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>
                        </section>

                        <section>
                            <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
                                <h3 class="mp-title-md flex items-center gap-2">
                                    <x-icon name="contacts" class="text-[24px] text-primary" />
                                    {{ __('Contactos') }}
                                </h3>
                            </div>
                            <div id="contacts-empty" class="mp-empty" hidden>
                                <x-icon name="group_off" class="text-[34px] text-outline" />
                                <p class="mt-2 font-semibold">{{ __('Todavía no tienes contactos aceptados.') }}</p>
                            </div>
                            <ul id="contact-list" class="space-y-3"></ul>
                        </section>

                        <section class="mp-card mp-card-pad">
                            <div class="flex flex-wrap items-center justify-between gap-2">
                                <h3 class="mp-title-md flex items-center gap-2">
                                    <x-icon name="inbox" class="text-[24px] text-primary" />
                                    {{ __('Compartido conmigo') }}
                                </h3>
                                <p id="share-status" class="text-[15px] font-medium text-primary" role="status"></p>
                            </div>
                            <p id="share-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>
                            <div id="shares-empty" class="mp-empty mt-3" hidden>
                                <x-icon name="drafts" class="text-[34px] text-outline" />
                                <p class="mt-2 font-semibold">{{ __('No tienes envíos pendientes.') }}</p>
                            </div>
                            <ul id="share-inbox" class="mt-3 space-y-3"></ul>
                        </section>

                        <section class="mp-card mp-card-pad">
                            <h3 class="mp-title-md flex items-center gap-2">
                                <x-icon name="contact_support" class="text-[24px] text-primary" />
                                {{ __('Solicitudes de documentos') }}
                            </h3>

                            <form id="request-form" class="mt-4 grid gap-3 md:grid-cols-3">
                                <div>
                                    <label for="request-contact" class="mp-label">{{ __('Contacto') }}</label>
                                    <select id="request-contact" class="mp-select"></select>
                                </div>
                                <div>
                                    <label for="request-type" class="mp-label">{{ __('Tipo de documento') }}</label>
                                    <input id="request-type" type="text" required placeholder="{{ __('Documento') }}" class="mp-input">
                                </div>
                                <div>
                                    <label for="request-purpose" class="mp-label">{{ __('Para qué lo necesitas') }}</label>
                                    <input id="request-purpose" type="text" required placeholder="{{ __('Ej. Trámite bancario') }}" class="mp-input">
                                </div>
                                <div class="md:col-span-3 flex justify-end">
                                    <button type="submit" class="mp-btn mp-btn-primary">
                                        <x-icon name="send" class="text-[20px]" />
                                        {{ __('Solicitar') }}
                                    </button>
                                </div>
                            </form>

                            <p id="request-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
                            <p id="request-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>
                            <div id="requests-empty" class="mp-empty mt-3" hidden>
                                <x-icon name="notifications_off" class="text-[34px] text-outline" />
                                <p class="mt-2 font-semibold">{{ __('No hay solicitudes pendientes.') }}</p>
                            </div>
                            <ul id="request-list" class="mt-3 space-y-3"></ul>
                        </section>
                    </div>
                </section>

                <section id="panel-devices" role="tabpanel" aria-labelledby="tab-btn-devices" hidden>
                    <div class="space-y-5">
                        <section class="mp-card mp-card-pad">
                            <div class="flex items-start gap-4">
                                <span class="mp-icon-tile">
                                    <x-icon name="phone_android" class="text-[24px]" />
                                </span>
                                <div>
                                    <h2 class="mp-title-md">{{ __('Conectar un celular') }}</h2>
                                    <p class="mp-body-sm mt-1">{{ __('Genera un código y escríbelo en la app de Android. La bóveda se transferirá cifrada.') }}</p>
                                </div>
                            </div>

                            <button id="pairing-create" type="button" class="mp-btn mp-btn-primary mt-4">
                                <x-icon name="qr_code_2" class="text-[21px]" />
                                {{ __('Generar código') }}
                            </button>

                            <p class="mp-hint mt-3">
                                {{ __('¿Aún no tienes la app?') }}
                                <a href="{{ route('app.download', ['v' => $androidVersionCode]) }}" class="font-semibold text-primary underline decoration-primary/40 underline-offset-4 hover:decoration-primary" download>
                                    {{ __('Descargar APK para Android') }}
                                </a>
                            </p>

                            <div id="pairing-code-box" class="mt-5 rounded-2xl border border-outline-variant/60 bg-surface-container-low p-5" hidden>
                                <div class="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
                                    <div>
                                        <p class="text-sm font-bold uppercase tracking-[0.12em] text-primary">{{ __('Código de conexión') }}</p>
                                        <p id="pairing-code" class="mt-2 font-mono text-[34px] font-bold tracking-[0.18em] text-on-surface"></p>
                                    </div>
                                    <button id="pairing-copy" type="button" class="mp-btn mp-btn-secondary mp-btn-sm shrink-0">
                                        <x-icon name="content_copy" class="text-[19px]" />
                                        {{ __('Copiar') }}
                                    </button>
                                </div>
                                <p class="mp-hint mt-3">{{ __('Válido por 10 minutos. No compartas este código con nadie ajeno a tu red de confianza.') }}</p>
                                <p id="pairing-status" class="mt-3 text-[15px] font-semibold text-primary" role="status"></p>
                                <p id="pairing-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>
                            </div>

                            <div class="mt-4 flex items-start gap-3 rounded-xl bg-surface-container-low p-4">
                                <x-icon name="verified_user" class="mt-0.5 text-[21px] text-primary" />
                                <p class="mp-hint">{{ __('La llave maestra viaja cifrada con la llave pública del celular. El servidor no puede leerla.') }}</p>
                            </div>
                        </section>

                        <section class="mp-card mp-card-pad">
                            <div class="flex items-start gap-4">
                                <span class="mp-icon-tile">
                                    <x-icon name="devices" class="text-[24px]" />
                                </span>
                                <div>
                                    <h2 class="mp-title-md">{{ __('Dispositivos conectados') }}</h2>
                                    <p class="mp-body-sm mt-1">{{ __('Equipos autorizados para descifrar y sincronizar tu bóveda con un código seguro de un solo uso.') }}</p>
                                </div>
                            </div>

                            <p id="device-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
                            <p id="device-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>

                            <div class="mt-4 flex flex-col justify-between gap-3 rounded-xl border border-outline-variant/60 bg-surface-container-low p-4 sm:flex-row sm:items-center">
                                <div class="flex items-center gap-3">
                                    <span class="flex h-12 w-12 items-center justify-center rounded-xl bg-paper text-primary">
                                        <x-icon name="laptop_mac" class="text-[24px]" />
                                    </span>
                                    <div>
                                        <p class="font-semibold text-on-surface">{{ __('Esta computadora') }}</p>
                                        <p class="text-sm text-on-surface-variant">{{ __('Sesión actual · Llave maestra en memoria temporal') }}</p>
                                    </div>
                                </div>
                                <span class="mp-badge mp-badge-primary self-start sm:self-center">{{ __('En línea') }}</span>
                            </div>

                            <ul id="device-list" class="mt-3 space-y-3"></ul>
                        </section>
                    </div>
                </section>
            </div>
        </section>
    </div>

    <dialog id="add-document-modal" class="mp-dialog" aria-labelledby="add-document-title">
        <div class="mp-dialog-panel">
            <div class="mp-dialog-header">
                <div class="flex items-start gap-3">
                    <span class="mp-icon-tile">
                        <x-icon name="note_add" class="text-[24px]" />
                    </span>
                    <div>
                        <h2 id="add-document-title" class="mp-title-md">{{ __('Nuevo documento') }}</h2>
                        <p class="mp-hint mt-1">{{ __('El archivo se cifra en tu navegador antes de subirse.') }}</p>
                    </div>
                </div>
                <button id="add-document-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                    <x-icon name="close" class="text-[21px]" />
                </button>
            </div>

            <form id="add-form" class="space-y-4">
                <div>
                    <label for="item-title" class="mp-label">{{ __('Título') }} <span class="text-error">*</span></label>
                    <input id="item-title" type="text" required maxlength="200" class="mp-input" placeholder="{{ __('Escribe un título.') }}">
                </div>

                <div class="grid gap-4 sm:grid-cols-2">
                    <div>
                        <label for="item-type" class="mp-label">{{ __('Tipo') }}</label>
                        <select id="item-type" class="mp-select">
                            <option value="document">{{ __('Documento') }}</option>
                            <option value="ticket">{{ __('Boleto o pase') }}</option>
                            <option value="card">{{ __('Tarjeta') }}</option>
                            <option value="other">{{ __('Otro') }}</option>
                        </select>
                    </div>
                    <div>
                        <label for="item-expires" class="mp-label">{{ __('Caducidad (opcional)') }}</label>
                        <input id="item-expires" type="date" class="mp-input">
                    </div>
                </div>

                <div>
                    <span class="mp-label">{{ __('Archivo') }}</span>
                    <div class="flex flex-col gap-2 sm:flex-row sm:items-center">
                        <label for="item-file" class="mp-btn mp-btn-secondary mp-btn-sm w-fit cursor-pointer">
                            <x-icon name="upload_file" class="text-[20px]" />
                            {{ __('Elegir archivo') }}
                        </label>
                        <span id="item-file-name" class="mp-hint">{{ __('PDF o imagen · máximo 100 MB') }}</span>
                    </div>
                    <input id="item-file" type="file" accept="application/pdf,image/*" class="sr-only">
                </div>

                <p id="add-error" class="mp-alert mp-alert-error" role="alert" hidden></p>
                <p id="add-status" class="text-[15px] font-medium text-primary" role="status"></p>

                <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                    <button id="add-document-cancel" type="button" class="mp-btn mp-btn-secondary">{{ __('Cancelar') }}</button>
                    <button id="add-submit" type="submit" class="mp-btn mp-btn-primary">
                        <x-icon name="save" class="text-[20px]" />
                        {{ __('Guardar en mi bóveda') }}
                    </button>
                </div>
            </form>
        </div>
    </dialog>

    <dialog id="edit-item-modal" class="mp-dialog" aria-labelledby="edit-item-title">
        <div class="mp-dialog-panel">
            <div class="mp-dialog-header">
                <div class="flex items-start gap-3">
                    <span class="mp-icon-tile">
                        <x-icon name="edit" class="text-[24px]" />
                    </span>
                    <div>
                        <h2 id="edit-item-title" class="mp-title-md">{{ __('Editar documento') }}</h2>
                        <p class="mp-hint mt-1">{{ __('Cambia el título o la caducidad. El archivo no se modifica.') }}</p>
                    </div>
                </div>
                <button id="edit-item-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                    <x-icon name="close" class="text-[21px]" />
                </button>
            </div>

            <form id="edit-item-form" class="space-y-4">
                <div>
                    <label for="edit-item-name" class="mp-label">{{ __('Título') }}</label>
                    <input id="edit-item-name" type="text" required maxlength="200" class="mp-input">
                </div>

                <div>
                    <label for="edit-item-expires" class="mp-label">{{ __('Caducidad (opcional)') }}</label>
                    <input id="edit-item-expires" type="date" class="mp-input">
                </div>

                <p id="edit-item-error" class="mp-alert mp-alert-error" role="alert" hidden></p>

                <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                    <button id="edit-item-cancel" type="button" class="mp-btn mp-btn-secondary">{{ __('Cancelar') }}</button>
                    <button id="edit-item-submit" type="submit" class="mp-btn mp-btn-primary">
                        <x-icon name="save" class="text-[20px]" />
                        {{ __('Guardar cambios') }}
                    </button>
                </div>
            </form>
        </div>
    </dialog>

    <dialog id="share-modal" class="mp-dialog" aria-labelledby="share-modal-title">
        <div class="mp-dialog-panel">
            <div class="mp-dialog-header">
                <div class="flex items-start gap-3">
                    <span class="mp-icon-tile">
                        <x-icon name="forward_to_inbox" class="text-[24px]" />
                    </span>
                    <div>
                        <h2 id="share-modal-title" class="mp-title-md">{{ __('Enviar a un contacto') }}</h2>
                        <p class="mp-hint mt-1">{{ __('Transfiere este documento cifrado de extremo a extremo.') }}</p>
                    </div>
                </div>
                <button id="share-modal-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                    <x-icon name="close" class="text-[21px]" />
                </button>
            </div>

            <div class="mb-4 flex items-center gap-3 rounded-xl bg-surface-container-low p-4">
                <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-paper text-primary">
                    <x-icon name="description" class="text-[22px]" />
                </span>
                <div class="min-w-0">
                    <p class="text-sm text-on-surface-variant">{{ __('Documento a transferir') }}</p>
                    <p id="share-modal-item" class="truncate font-semibold text-on-surface"></p>
                </div>
            </div>

            <div class="space-y-4">
                <div>
                    <label for="share-contact" class="mp-label">{{ __('Contacto') }}</label>
                    <select id="share-contact" class="mp-select"></select>
                </div>

                <div>
                    <label for="share-mode" class="mp-label">{{ __('Forma de envío') }}</label>
                    <select id="share-mode" class="mp-select">
                        <option value="copy">{{ __('Enviar copia') }} — {{ __('el documento se queda en tu bóveda') }}</option>
                        <option value="transfer">{{ __('Transferir') }} — {{ __('cede la titularidad') }}</option>
                    </select>
                </div>

                <div>
                    <label for="share-expires" class="mp-label">{{ __('Caducidad (opcional)') }}</label>
                    <input id="share-expires" type="date" class="mp-input">
                    <p class="mp-hint mt-1.5">{{ __('Si es una copia, caduca automáticamente en el dispositivo receptor.') }}</p>
                </div>

                <p class="mp-alert mp-alert-warning" role="note">
                    <x-icon name="warning" class="mt-0.5 text-[21px]" />
                    <span>{{ __('Al transferir, el documento saldrá de tu bóveda.') }}</span>
                </p>

                <p id="share-send-error" class="mp-alert mp-alert-error" role="alert" hidden></p>
            </div>

            <div class="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <button id="share-cancel" type="button" class="mp-btn mp-btn-secondary">{{ __('Cancelar') }}</button>
                <button id="share-send" type="button" class="mp-btn mp-btn-primary">
                    <x-icon name="send" class="text-[20px]" />
                    {{ __('Enviar') }}
                </button>
            </div>
        </div>
    </dialog>

    <dialog id="manage-access-modal" class="mp-dialog" aria-labelledby="manage-access-title">
        <div class="mp-dialog-panel">
            <div class="mp-dialog-header">
                <div class="flex items-start gap-3">
                    <span class="mp-icon-tile">
                        <x-icon name="manage_accounts" class="text-[24px]" />
                    </span>
                    <div>
                        <h2 id="manage-access-title" class="mp-title-md">{{ __('Gestionar acceso') }}</h2>
                        <p class="mp-hint mt-1">{{ __('Revisa los envíos activos y cancela los que ya no necesites.') }}</p>
                    </div>
                </div>
                <button id="manage-access-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                    <x-icon name="close" class="text-[21px]" />
                </button>
            </div>
            <div id="manage-access-list" class="space-y-3"></div>
            <div class="mt-5 flex justify-end">
                <button id="manage-access-done" type="button" class="mp-btn mp-btn-secondary">{{ __('Cerrar') }}</button>
            </div>
        </div>
    </dialog>

    <dialog id="confirm-modal" class="mp-dialog" aria-labelledby="confirm-title">
        <div class="mp-dialog-panel">
            <div class="mp-dialog-header">
                <div class="flex items-start gap-3">
                    <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-error-container text-on-error-container">
                        <x-icon name="warning" class="text-[23px]" />
                    </span>
                    <div>
                        <h2 id="confirm-title" class="mp-title-sm">{{ __('Confirmar acción') }}</h2>
                        <p id="confirm-message" class="mp-hint mt-1"></p>
                    </div>
                </div>
            </div>
            <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <button id="confirm-cancel" type="button" class="mp-btn mp-btn-secondary">{{ __('Cancelar') }}</button>
                <button id="confirm-accept" type="button" class="mp-btn mp-btn-danger">{{ __('Continuar') }}</button>
            </div>
        </div>
    </dialog>

    <dialog id="backup-modal" class="mp-dialog" aria-labelledby="backup-modal-title">
        <div class="mp-dialog-panel">
            <div class="mp-dialog-header">
                <div class="flex items-start gap-3">
                    <span class="mp-icon-tile">
                        <x-icon name="save" class="text-[24px]" />
                    </span>
                    <div>
                        <h2 id="backup-modal-title" class="mp-title-md">{{ __('Copia de seguridad') }}</h2>
                        <p class="mp-hint mt-1">{{ __('Cifrada en tu navegador. Incluye documentos, pases, credenciales y salud.') }}</p>
                    </div>
                </div>
                <button id="backup-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                    <x-icon name="close" class="text-[21px]" />
                </button>
            </div>

            <p id="backup-error" class="mp-alert mp-alert-error" role="alert" hidden></p>
            <p id="backup-status" class="mp-alert mp-alert-info" role="status" hidden></p>

            <div class="mt-4 grid gap-4 lg:grid-cols-2">
                <section class="mp-card p-4">
                    <h3 class="mp-title-sm">{{ __('Crear una copia') }}</h3>
                    <p class="mp-hint mt-1">{{ __('Protege la copia con una frase que solo tú conozcas.') }}</p>
                    <div class="mt-3 space-y-3">
                        <div>
                            <label for="backup-passphrase" class="mp-label">{{ __('Frase de la copia') }}</label>
                            <input id="backup-passphrase" type="password" class="mp-input" autocomplete="new-password">
                        </div>
                        <div>
                            <label for="backup-passphrase-confirm" class="mp-label">{{ __('Confirma la frase') }}</label>
                            <input id="backup-passphrase-confirm" type="password" class="mp-input" autocomplete="new-password">
                        </div>
                        <button id="backup-export" type="button" class="mp-btn mp-btn-primary w-full">
                            <x-icon name="download" class="text-[20px]" />
                            {{ __('Crear copia') }}
                        </button>
                    </div>
                </section>

                <section class="mp-card p-4">
                    <h3 class="mp-title-sm">{{ __('Restaurar una copia') }}</h3>
                    <p class="mp-hint mt-1">{{ __('Agrega los elementos a esta bóveda. No se borra lo que ya tienes.') }}</p>
                    <div class="mt-3 space-y-3">
                        <div>
                            <label for="backup-file" class="mp-label">{{ __('Archivo .mypassbackup') }}</label>
                            <input id="backup-file" type="file" accept=".mypassbackup,application/octet-stream" class="mp-file-input">
                        </div>
                        <div>
                            <label for="backup-restore-passphrase" class="mp-label">{{ __('Frase de la copia') }}</label>
                            <input id="backup-restore-passphrase" type="password" class="mp-input" autocomplete="off">
                        </div>
                        <button id="backup-import" type="button" class="mp-btn mp-btn-secondary w-full">
                            <x-icon name="upload" class="text-[20px]" />
                            {{ __('Restaurar copia') }}
                        </button>
                    </div>
                </section>
            </div>

            <p class="mp-hint mt-4">{{ __('Guarda el archivo en un lugar seguro. Sin la frase no se puede recuperar.') }}</p>
        </div>
    </dialog>

    <dialog id="vault-search-modal" class="mp-dialog" aria-labelledby="vault-search-title">
        <div class="mp-dialog-panel">
            <div class="mp-dialog-header">
                <div class="flex items-start gap-3">
                    <span class="mp-icon-tile">
                        <x-icon name="search" class="text-[24px]" />
                    </span>
                    <div>
                        <h2 id="vault-search-title" class="mp-title-md">{{ __('Buscar en mi bóveda') }}</h2>
                        <p class="mp-hint mt-1">{{ __('Documentos, pases, credenciales y salud. Todo se busca en este dispositivo.') }}</p>
                    </div>
                </div>
                <button id="vault-search-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                    <x-icon name="close" class="text-[21px]" />
                </button>
            </div>

            <div class="relative mt-4">
                <span class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-outline">
                    <x-icon name="search" class="text-[21px]" />
                </span>
                <input id="vault-search-input" type="search" class="mp-input pl-12 text-[17px]"
                       placeholder="{{ __('Busca un vuelo, una contraseña o un documento') }}"
                       aria-label="{{ __('Buscar en mi bóveda') }}" autocomplete="off">
            </div>

            <p id="vault-search-status" class="mp-hint mt-2" role="status"></p>
            <div id="vault-search-results" class="mt-4 space-y-2"></div>
        </div>
    </dialog>

    <dialog id="trash-modal" class="mp-dialog" aria-labelledby="trash-modal-title">
        <div class="mp-dialog-panel">
            <div class="mp-dialog-header">
                <div class="flex items-start gap-3">
                    <span class="mp-icon-tile">
                        <x-icon name="delete" class="text-[24px]" />
                    </span>
                    <div>
                        <h2 id="trash-modal-title" class="mp-title-md">{{ __('Papelera') }}</h2>
                        <p class="mp-hint mt-1">{{ __('Los elementos se guardan 30 días. Puedes restaurarlos o eliminarlos para siempre.') }}</p>
                    </div>
                </div>
                <button id="trash-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                    <x-icon name="close" class="text-[21px]" />
                </button>
            </div>

            <p id="trash-error" class="mp-alert mp-alert-error" role="alert" hidden></p>
            <p id="trash-empty" class="mp-empty mt-4" hidden>{{ __('La papelera está vacía.') }}</p>
            <ul id="trash-list" class="mt-4 space-y-3"></ul>
        </div>
    </dialog>

    <div id="vault-toast" class="mp-toast" role="status" hidden></div>
</x-layout>
