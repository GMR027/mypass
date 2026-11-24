<x-layout :title="__('Seguridad')">
    <div class="mx-auto max-w-4xl">
        <div class="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
            <div>
                <p class="mp-eyebrow">{{ __('Seguridad') }}</p>
                <h1 class="mp-title-lg mt-2">{{ __('Seguridad y preferencias') }}</h1>
                <p class="mp-body mt-2 max-w-2xl">{{ __('Agrega un segundo paso al entrar. Puedes usar una app de autenticación o recibir un código en tu correo.') }}</p>
            </div>
            <a href="{{ route('settings.profile') }}" class="mp-btn mp-btn-secondary shrink-0">
                <x-icon name="person" class="text-[20px]" />
                {{ __('Volver al perfil') }}
            </a>
        </div>

        <section class="mp-card mp-card-pad">
            <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
                <div class="flex items-start gap-4">
                    <span class="mp-icon-tile">
                        <x-icon name="shield_lock" class="text-[24px]" />
                    </span>
                    <div>
                        <h2 class="mp-title-sm">{{ __('App de autenticación (TOTP)') }}</h2>
                        <p class="mp-hint mt-1 max-w-2xl">{{ __('Compatible con cualquier app que lea códigos TOTP. No necesitas una app de Google.') }}</p>
                    </div>
                </div>
                <div>
                    @if ($totpEnabled)
                        <span class="mp-badge mp-badge-primary">
                            <x-icon name="check_circle" class="text-[17px]" filled />
                            {{ __('Activada') }}
                        </span>
                    @elseif ($totpPending)
                        <span class="mp-badge mp-badge-warning">
                            <x-icon name="schedule" class="text-[17px]" />
                            {{ __('Sin confirmar') }}
                        </span>
                    @else
                        <span class="mp-badge mp-badge-neutral">{{ __('Desactivada') }}</span>
                    @endif
                </div>
            </div>

            <p id="totp-status" class="mt-4 text-[15px] text-on-surface-variant" role="status">
                @if ($totpEnabled)
                    {{ __('Está activada.') }}
                @elseif ($totpPending)
                    {{ __('Tienes una configuración sin confirmar.') }}
                @else
                    {{ __('No está activada.') }}
                @endif
            </p>
            <p id="totp-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>

            <button id="totp-enable" type="button" class="mp-btn mp-btn-primary mt-4 {{ $totpEnabled ? 'hidden' : '' }}">
                <x-icon name="vpn_key" class="text-[21px]" />
                {{ $totpPending ? __('Continuar configuración') : __('Activar app de autenticación') }}
            </button>

            <div id="totp-setup" class="mt-6 rounded-xl border border-outline-variant/60 bg-surface-container-low p-4 md:p-5" @if (! $totpPending) hidden @endif>
                <p class="font-semibold text-on-surface">{{ __('1. Escanea este código con tu app') }}</p>
                <div class="mt-4 grid gap-5 sm:grid-cols-[auto_minmax(0,1fr)] sm:items-start">
                    <div id="totp-qr" class="w-fit rounded-xl border border-outline-variant/60 bg-white p-3 [&_svg]:h-44 [&_svg]:w-44"></div>
                    <div>
                        <p class="mp-hint">{{ __('Si no puedes escanearlo, escribe esta clave:') }}</p>
                        <code id="totp-secret" class="mt-2 block break-all rounded-lg border border-outline-variant/60 bg-white p-3 font-mono text-[15px] font-semibold text-on-surface"></code>

                        <p class="mt-5 font-semibold text-on-surface">{{ __('2. Escribe el código de 6 dígitos que muestra la app') }}</p>
                        <div class="mt-2 flex flex-col gap-2 sm:flex-row">
                            <input id="totp-code" inputmode="numeric" autocomplete="one-time-code" maxlength="6"
                                   class="mp-input text-center font-mono tracking-[0.3em] sm:max-w-44"
                                   aria-label="{{ __('Código de la app') }}" placeholder="000000">
                            <button id="totp-confirm" type="button" class="mp-btn mp-btn-primary">
                                {{ __('Confirmar') }}
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <div id="totp-enabled" class="mt-5" @if (! $totpEnabled) hidden @endif>
                <div class="flex flex-col gap-2 sm:flex-row sm:flex-wrap">
                    <button id="totp-show-codes" type="button" class="mp-btn mp-btn-secondary mp-btn-sm">
                        <x-icon name="visibility" class="text-[19px]" />
                        {{ __('Ver códigos de recuperación') }}
                    </button>
                    <button id="totp-regenerate" type="button" class="mp-btn mp-btn-secondary mp-btn-sm">
                        <x-icon name="refresh" class="text-[19px]" />
                        {{ __('Regenerar códigos') }}
                    </button>
                    <button id="totp-disable" type="button" class="mp-btn mp-btn-danger mp-btn-sm">
                        <x-icon name="close" class="text-[19px]" />
                        {{ __('Desactivar app') }}
                    </button>
                </div>

                <div class="mt-4 rounded-xl border border-outline-variant/60 bg-surface-container-low p-4">
                    <div class="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                        <p class="mp-hint max-w-xl">
                            {{ __('Guarda estos códigos en un lugar seguro. Cada uno sirve una sola vez si pierdes el teléfono.') }}
                        </p>
                        <button id="totp-copy-codes" type="button" class="mp-btn mp-btn-quiet mp-btn-sm shrink-0">
                            <x-icon name="content_copy" class="text-[19px]" />
                            {{ __('Copiar códigos') }}
                        </button>
                    </div>
                    <ul id="totp-codes" class="mt-3 grid grid-cols-1 gap-2 font-mono text-[16px] sm:grid-cols-2"></ul>
                </div>
            </div>
        </section>

        <section class="mp-card mp-card-pad mt-5">
            <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
                <div class="flex items-start gap-4">
                    <span class="mp-icon-tile">
                        <x-icon name="mark_email_read" class="text-[24px]" />
                    </span>
                    <div>
                        <h2 class="mp-title-sm">{{ __('Código por correo') }}</h2>
                        <p class="mp-hint mt-1 max-w-2xl mp-break">{{ __('Al entrar, te enviaremos un código de 6 dígitos a :email.', ['email' => $user->email]) }}</p>
                    </div>
                </div>

                <label class="flex shrink-0 cursor-pointer items-center gap-3 self-start rounded-xl border border-outline-variant/60 bg-surface-container-low px-4 py-3">
                    <input id="email-two-factor" type="checkbox" class="mp-switch-input" @checked($emailEnabled)>
                    <span class="mp-switch" aria-hidden="true"></span>
                    <span class="text-[15px] font-semibold">{{ __('Pedir código al entrar') }}</span>
                </label>
            </div>

            <p id="email-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
            <p id="email-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>
        </section>

        <section id="vault-security-app" class="mp-card mp-card-pad mt-5"
                 data-keys-url="{{ route('web.keys.show') }}"
                 data-email="{{ $user->email }}"
                 data-recovery-status-url="{{ route('web.vault-recovery.status') }}"
                 data-recovery-enable-url="{{ route('web.vault-recovery.enable') }}"
                 data-recovery-disable-url="{{ route('web.vault-recovery.disable') }}">
            <div class="flex items-start gap-4">
                <span class="mp-icon-tile">
                    <x-icon name="key" class="text-[24px]" />
                </span>
                <div>
                    <h2 class="mp-title-sm">{{ __('Seguridad de la bóveda') }}</h2>
                    <p class="mp-hint mt-1 max-w-2xl">{{ __('Cambia la frase que abre tus documentos o genera una llave de emergencia nueva. Estas acciones no modifican tus documentos.') }}</p>
                </div>
            </div>

            @if (! $hasVault)
                <div class="mt-5 rounded-xl border border-outline-variant/60 bg-surface-container-low p-4">
                    <p class="font-semibold text-on-surface">{{ __('Tu bóveda todavía no está configurada.') }}</p>
                    <p class="mp-hint mt-1">{{ __('Primero crea la llave de la bóveda y guarda tu llave de emergencia.') }}</p>
                    <a href="{{ route('onboarding') }}" class="mp-btn mp-btn-primary mt-4">
                        <x-icon name="vpn_key" class="text-[21px]" />
                        {{ __('Configurar mi bóveda') }}
                    </a>
                </div>
            @else
                <div class="mt-6 grid gap-6 lg:grid-cols-2">
                    <form id="vault-change-passphrase-form" class="rounded-xl border border-outline-variant/60 bg-surface-container-low p-4 md:p-5">
                        <h3 class="font-semibold text-on-surface">{{ __('Cambiar la frase de la bóveda') }}</h3>
                        <p class="mp-hint mt-1">{{ __('Necesitas la frase actual. La frase nueva debe tener al menos 12 caracteres.') }}</p>

                        <div class="mt-4 space-y-3">
                            <div>
                                <label for="vault-current-passphrase" class="mp-label">{{ __('Frase actual') }}</label>
                                <input id="vault-current-passphrase" type="password" required autocomplete="current-password" class="mp-input">
                            </div>
                            <div>
                                <label for="vault-new-passphrase" class="mp-label">{{ __('Frase nueva') }}</label>
                                <input id="vault-new-passphrase" type="password" required minlength="12" autocomplete="new-password" class="mp-input">
                            </div>
                            <div>
                                <label for="vault-new-passphrase-confirmation" class="mp-label">{{ __('Repite la frase nueva') }}</label>
                                <input id="vault-new-passphrase-confirmation" type="password" required minlength="12" autocomplete="new-password" class="mp-input">
                            </div>
                        </div>

                        <p id="vault-change-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
                        <p id="vault-change-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>

                        <button type="submit" class="mp-btn mp-btn-primary mt-4 w-full">
                            <x-icon name="lock_reset" class="text-[20px]" />
                            {{ __('Cambiar frase') }}
                        </button>
                    </form>

                    <form id="vault-recovery-form" class="rounded-xl border border-outline-variant/60 bg-surface-container-low p-4 md:p-5">
                        <h3 class="font-semibold text-on-surface">{{ __('Regenerar llave de emergencia') }}</h3>
                        <p class="mp-hint mt-1">{{ __('La llave anterior dejará de funcionar. Guarda la nueva antes de cerrar esta página.') }}</p>

                        <div class="mt-4 space-y-3">
                            <div>
                                <label for="vault-recovery-passphrase" class="mp-label">{{ __('Frase actual') }}</label>
                                <input id="vault-recovery-passphrase" type="password" required autocomplete="current-password" class="mp-input">
                            </div>
                            <div>
                                <label for="vault-recovery-length" class="mp-label">{{ __('Formato de la llave') }}</label>
                                <select id="vault-recovery-length" class="mp-select">
                                    <option value="32">{{ __('32 dígitos (recomendado)') }}</option>
                                    <option value="16">{{ __('16 dígitos') }}</option>
                                </select>
                            </div>
                        </div>

                        <p id="vault-recovery-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
                        <p id="vault-recovery-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>

                        <button type="submit" class="mp-btn mp-btn-secondary mt-4 w-full">
                            <x-icon name="refresh" class="text-[20px]" />
                            {{ __('Generar llave nueva') }}
                        </button>

                        <div id="vault-recovery-output" class="mt-4 rounded-xl border border-outline-variant/60 bg-white p-4" hidden>
                            <p class="text-sm font-semibold text-on-surface">{{ __('Tu llave de emergencia nueva') }}</p>
                            <p id="vault-recovery-digits" class="mt-2 break-words rounded-lg bg-surface-container-low px-3 py-4 text-center font-mono text-[19px] font-semibold tracking-[0.08em] text-on-surface"></p>
                            <div class="mt-3 flex flex-wrap gap-2">
                                <button id="vault-recovery-copy" type="button" class="mp-btn mp-btn-quiet mp-btn-sm">
                                    <x-icon name="content_copy" class="text-[18px]" />
                                    {{ __('Copiar') }}
                                </button>
                                <button id="vault-recovery-download" type="button" class="mp-btn mp-btn-secondary mp-btn-sm">
                                    <x-icon name="download" class="text-[18px]" />
                                    {{ __('Descargar .mypss') }}
                                </button>
                            </div>
                        </div>
                    </form>
                </div>

                <div class="mt-6 rounded-xl border border-outline-variant/60 bg-surface-container-low p-4 md:p-5">
                    <div class="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                        <div class="flex items-start gap-3">
                            <span class="mp-icon-tile shrink-0 bg-white">
                                <x-icon name="mark_email_read" class="text-[24px]" />
                            </span>
                            <div>
                                <h3 class="font-semibold text-on-surface">{{ __('Recuperación por correo') }}</h3>
                                <p class="mp-hint mt-1 max-w-2xl">{{ __('Si olvidas la llave de tu bóveda, te enviamos un código a :email y puedes abrirla de nuevo. Es la forma más fácil, pero el servidor guarda una copia recuperable de la llave maestra.', ['email' => $emailRecoveryMasked]) }}</p>
                            </div>
                        </div>
                        <span class="mp-badge {{ $emailRecoveryEnabled ? 'mp-badge-primary' : 'mp-badge-neutral' }} shrink-0">
                            <x-icon :name="$emailRecoveryEnabled ? 'check_circle' : 'mail'" class="text-[17px]" :filled="$emailRecoveryEnabled" />
                            {{ $emailRecoveryEnabled ? __('Activada') : __('Desactivada') }}
                        </span>
                    </div>

                    <p id="email-recovery-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
                    <p id="email-recovery-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>

                    @if ($emailRecoveryEnabled)
                        <form id="email-recovery-disable-form" class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
                            <div class="flex-1">
                                <label for="email-recovery-password" class="mp-label">{{ __('Contraseña de la cuenta') }}</label>
                                <input id="email-recovery-password" type="password" required autocomplete="current-password" class="mp-input">
                            </div>
                            <button id="email-recovery-disable" type="submit" class="mp-btn mp-btn-danger shrink-0">
                                <x-icon name="link_off" class="text-[20px]" />
                                {{ __('Desactivar recuperación') }}
                            </button>
                        </form>
                    @else
                        <form id="email-recovery-enable-form" class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
                            <div class="flex-1">
                                <label for="email-recovery-passphrase" class="mp-label">{{ __('Frase actual de la bóveda') }}</label>
                                <input id="email-recovery-passphrase" type="password" required autocomplete="current-password" class="mp-input">
                            </div>
                            <button id="email-recovery-enable" type="submit" class="mp-btn mp-btn-primary shrink-0">
                                <x-icon name="mark_email_read" class="text-[20px]" />
                                {{ __('Activar recuperación') }}
                            </button>
                        </form>
                    @endif
                </div>
            @endif
        </section>

        <section class="mp-card mp-card-pad mt-5">
            <div class="flex items-start gap-4">
                <span class="mp-icon-tile">
                    <x-icon name="accessibility_new" class="text-[24px]" />
                </span>
                <div>
                    <h2 class="mp-title-sm">{{ __('Modo fácil y texto grande') }}</h2>
                    <p class="mp-hint mt-1 max-w-2xl">{{ __('El modo fácil agranda textos, simplifica la navegación y deja la ayuda de recuperación a la mano. Se activa automáticamente a partir de los 51 años.') }}</p>
                </div>
            </div>

            <div class="mt-5 grid gap-5 sm:grid-cols-2">
                <label class="flex cursor-pointer items-center gap-3 rounded-xl border border-outline-variant/60 bg-surface-container-low px-4 py-3">
                    <input id="easy-mode" type="checkbox" class="mp-switch-input" @checked($user->usesEasyMode())>
                    <span class="mp-switch" aria-hidden="true"></span>
                    <span class="text-[15px] font-semibold">{{ __('Usar modo fácil') }}</span>
                </label>

                <label class="flex cursor-pointer items-center gap-3 rounded-xl border border-outline-variant/60 bg-surface-container-low px-4 py-3">
                    <input id="simple-mode" type="checkbox" class="mp-switch-input" @checked($user->simple_mode)>
                    <span class="mp-switch" aria-hidden="true"></span>
                    <span class="text-[15px] font-semibold">{{ __('Solo texto grande') }}</span>
                </label>

                <div class="sm:col-span-2">
                    <label for="locale" class="mp-label">{{ __('Idioma') }}</label>
                    <select id="locale" class="mp-select">
                        <option value="es-MX" @selected(($user->locale ?? 'es-MX') === 'es-MX')>{{ __('Español') }}</option>
                        <option value="en" @selected(($user->locale ?? 'es-MX') === 'en')>English</option>
                    </select>
                </div>
            </div>

            @if (! $user->birth_date)
                <p class="mp-hint mt-3">
                    <a href="{{ route('settings.profile') }}" class="font-semibold text-primary underline underline-offset-4">{{ __('Agrega tu fecha de nacimiento') }}</a>
                    {{ __('para que el modo fácil se active solo si te corresponde.') }}
                </p>
            @endif

            <p id="prefs-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
            <p id="prefs-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>
        </section>

        <section class="mp-card mp-card-pad mt-5">
            <div class="flex items-start gap-4">
                <span class="mp-icon-tile">
                    <x-icon name="timer" class="text-[24px]" />
                </span>
                <div>
                    <h2 class="mp-title-sm">{{ __('Bloqueo automático de la bóveda') }}</h2>
                    <p class="mp-hint mt-1 max-w-2xl">{{ __('La bóveda se bloquea sola en este navegador después de un tiempo sin actividad. Se guarda solo en este dispositivo.') }}</p>
                </div>
            </div>

            <div class="mt-5 max-w-sm">
                <label for="auto-lock" class="mp-label">{{ __('Bloquear después de') }}</label>
                <select id="auto-lock" class="mp-select">
                    <option value="1">{{ __('1 minuto') }}</option>
                    <option value="5">{{ __('5 minutos') }}</option>
                    <option value="10">{{ __('10 minutos') }}</option>
                    <option value="30">{{ __('30 minutos') }}</option>
                    <option value="0">{{ __('No bloquear automáticamente') }}</option>
                </select>
                <p id="auto-lock-status" class="mt-2 text-[15px] font-medium text-primary" role="status"></p>
            </div>
        </section>

        <section class="mp-card mp-card-pad mt-5">
            <div class="flex items-start gap-4">
                <span class="mp-icon-tile">
                    <x-icon name="history" class="text-[24px]" />
                </span>
                <div>
                    <h2 class="mp-title-sm">{{ __('Actividad reciente') }}</h2>
                    <p class="mp-hint mt-1">{{ __('Últimos eventos de seguridad de tu cuenta. No mostramos direcciones IP completas.') }}</p>
                </div>
            </div>

            @php
                $eventLabels = [
                    'login_web' => __('Inicio de sesión web'),
                    'logout_web' => __('Cierre de sesión web'),
                    'account_created' => __('Cuenta creada'),
                    'account_deleted' => __('Cuenta eliminada'),
                    'device_revoked' => __('Dispositivo bloqueado'),
                    'contact_requested' => __('Solicitud de contacto enviada'),
                    'contact_accepted' => __('Contacto aceptado'),
                    'contact_blocked' => __('Contacto bloqueado'),
                    'share_sent' => __('Documento enviado'),
                    'share_accepted' => __('Envío aceptado'),
                    'share_declined' => __('Envío rechazado'),
                    'share_revoked' => __('Envío cancelado'),
                    'document_requested' => __('Documento solicitado'),
                    'document_request_declined' => __('Solicitud rechazada'),
                    'document_request_cancelled' => __('Solicitud cancelada'),
                    'item_deleted' => __('Documento eliminado'),
                    'item_restored' => __('Documento restaurado'),
                    'item_purged' => __('Documento eliminado definitivamente'),
                    'vault_recovery_enabled' => __('Recuperación por correo activada'),
                    'vault_recovery_disabled' => __('Recuperación por correo desactivada'),
                    'vault_recovery_requested' => __('Código de recuperación solicitado'),
                    'vault_recovery_used' => __('Bóveda abierta con recuperación por correo'),
                ];
            @endphp

            @if ($events->isEmpty())
                <p class="mp-hint mt-4">{{ __('Todavía no hay actividad registrada.') }}</p>
            @else
                <ul class="mt-4 divide-y divide-outline-variant/40 rounded-xl border border-outline-variant/60">
                    @foreach ($events as $event)
                        <li class="flex flex-col gap-1 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                            <span class="font-medium text-on-surface">{{ $eventLabels[$event->event] ?? $event->event }}</span>
                            <span class="text-sm text-on-surface-variant">{{ $event->created_at?->diffForHumans() }}</span>
                        </li>
                    @endforeach
                </ul>
            @endif
        </section>
    </div>

    <dialog id="settings-confirm" class="mp-dialog" aria-labelledby="settings-confirm-title">
        <div class="mp-dialog-panel">
            <div class="mp-dialog-header">
                <div class="flex items-start gap-3">
                    <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-error-container text-on-error-container">
                        <x-icon name="warning" class="text-[23px]" />
                    </span>
                    <div>
                        <h2 id="settings-confirm-title" class="mp-title-sm">{{ __('Confirmar acción') }}</h2>
                        <p id="settings-confirm-message" class="mp-hint mt-1"></p>
                    </div>
                </div>
            </div>
            <div class="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <button id="settings-confirm-cancel" type="button" class="mp-btn mp-btn-secondary">{{ __('Cancelar') }}</button>
                <button id="settings-confirm-accept" type="button" class="mp-btn mp-btn-danger">{{ __('Continuar') }}</button>
            </div>
        </div>
    </dialog>

    <script nonce="{{ $cspNonce }}">
        const csrfToken = document.querySelector('meta[name="csrf-token"]').content;
        const totpEnabled = @json($totpEnabled);

        async function api(url, options = {}) {
            const response = await fetch(url, {
                method: options.method || 'GET',
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken,
                },
                credentials: 'same-origin',
                body: options.body ? JSON.stringify(options.body) : undefined,
            });

            let body = null;
            try {
                body = await response.json();
            } catch {}

            if (!response.ok) {
                throw new Error(body?.message || window.mypassT('No se pudo completar la operación.'));
            }

            return body;
        }

        const totpError = document.getElementById('totp-error');
        const totpSetup = document.getElementById('totp-setup');
        const totpEnabledBox = document.getElementById('totp-enabled');
        const totpEnableButton = document.getElementById('totp-enable');
        const totpQr = document.getElementById('totp-qr');
        const totpSecret = document.getElementById('totp-secret');
        const totpCode = document.getElementById('totp-code');
        const totpStatus = document.getElementById('totp-status');
        const totpCodes = document.getElementById('totp-codes');
        const confirmDialog = document.getElementById('settings-confirm');
        const confirmMessage = document.getElementById('settings-confirm-message');
        const confirmAccept = document.getElementById('settings-confirm-accept');
        const confirmCancel = document.getElementById('settings-confirm-cancel');

        let confirmResolver = null;

        function askConfirm(message, acceptLabel = window.mypassT('Continuar')) {
            confirmMessage.textContent = message;
            confirmAccept.textContent = acceptLabel;
            confirmDialog.showModal();

            return new Promise((resolve) => {
                confirmResolver = resolve;
            });
        }

        function finishConfirm(value) {
            if (confirmDialog.open) {
                confirmDialog.close();
            }
            confirmResolver?.(value);
            confirmResolver = null;
        }

        confirmAccept.addEventListener('click', () => finishConfirm(true));
        confirmCancel.addEventListener('click', () => finishConfirm(false));
        confirmDialog.addEventListener('cancel', (event) => {
            event.preventDefault();
            finishConfirm(false);
        });

        async function loadTotpSetup() {
            const qr = await api('/user/two-factor-qr-code');
            const secret = await api('/user/two-factor-secret-key');
            totpQr.innerHTML = qr.svg || '';
            totpSecret.textContent = secret.secretKey || '';
            totpSetup.hidden = false;
            totpEnableButton.hidden = true;
        }

        if (!totpEnabled && @json($totpPending)) {
            loadTotpSetup().catch((error) => {
                totpError.textContent = error.message;
                totpError.hidden = false;
            });
        }

        totpEnableButton?.addEventListener('click', async () => {
            totpError.hidden = true;
            totpEnableButton.disabled = true;
            try {
                await api('/user/two-factor-authentication', { method: 'POST' });
                await loadTotpSetup();
                totpStatus.textContent = window.mypassT('Escanea el código y confirma con los 6 dígitos.');
            } catch (error) {
                totpError.textContent = error.message;
                totpError.hidden = false;
            } finally {
                totpEnableButton.disabled = false;
            }
        });

        document.getElementById('totp-confirm')?.addEventListener('click', async () => {
            totpError.hidden = true;
            try {
                await api('/user/confirmed-two-factor-authentication', {
                    method: 'POST',
                    body: { code: totpCode.value.trim() },
                });
                window.location.reload();
            } catch (error) {
                totpError.textContent = error.message;
                totpError.hidden = false;
            }
        });

        async function showRecoveryCodes() {
            const codes = await api('/user/two-factor-recovery-codes');
            totpCodes.replaceChildren();
            for (const code of codes) {
                const li = document.createElement('li');
                li.textContent = code;
                li.className = 'rounded-lg border border-outline-variant/60 bg-white px-3 py-2 text-center tracking-wider';
                totpCodes.appendChild(li);
            }
        }

        document.getElementById('totp-show-codes')?.addEventListener('click', () => {
            totpError.hidden = true;
            showRecoveryCodes().catch((error) => {
                totpError.textContent = error.message;
                totpError.hidden = false;
            });
        });

        document.getElementById('totp-copy-codes')?.addEventListener('click', async () => {
            const codes = Array.from(totpCodes.children).map((item) => item.textContent.trim());
            if (codes.length === 0) {
                totpStatus.textContent = window.mypassT('Primero pulsa “Ver códigos de recuperación”.');
                return;
            }

            try {
                await navigator.clipboard.writeText(codes.join('\n'));
                totpStatus.textContent = window.mypassT('Códigos copiados.');
            } catch {
                totpStatus.textContent = window.mypassT('No se pudieron copiar los códigos.');
            }
        });

        document.getElementById('totp-regenerate')?.addEventListener('click', async () => {
            const confirmed = await askConfirm(
                window.mypassT('Se generarán códigos nuevos y los anteriores dejarán de funcionar. ¿Quieres continuar?'),
                window.mypassT('Regenerar códigos'),
            );
            if (!confirmed) {
                return;
            }

            totpError.hidden = true;
            try {
                await api('/user/two-factor-recovery-codes', { method: 'POST' });
                await showRecoveryCodes();
                totpStatus.textContent = window.mypassT('Códigos de recuperación regenerados.');
            } catch (error) {
                totpError.textContent = error.message;
                totpError.hidden = false;
            }
        });

        document.getElementById('totp-disable')?.addEventListener('click', async () => {
            const confirmed = await askConfirm(
                window.mypassT('¿Desactivar la app de autenticación? Tu cuenta quedará protegida solo con tu contraseña.'),
                window.mypassT('Desactivar app'),
            );
            if (!confirmed) {
                return;
            }

            try {
                await api('/user/two-factor-authentication', { method: 'DELETE' });
                window.location.reload();
            } catch (error) {
                totpError.textContent = error.message;
                totpError.hidden = false;
            }
        });

        const emailToggle = document.getElementById('email-two-factor');
        const emailStatus = document.getElementById('email-status');
        const emailError = document.getElementById('email-error');

        emailToggle?.addEventListener('change', async () => {
            emailError.hidden = true;
            emailStatus.textContent = window.mypassT('Guardando…');
            try {
                const body = await api(@json(route('web.security.email-two-factor')), {
                    method: 'POST',
                    body: { enabled: emailToggle.checked },
                });
                emailStatus.textContent = body.message;
            } catch (error) {
                emailToggle.checked = !emailToggle.checked;
                emailStatus.textContent = '';
                emailError.textContent = error.message;
                emailError.hidden = false;
            }
        });

        const simpleMode = document.getElementById('simple-mode');
        const easyMode = document.getElementById('easy-mode');
        const localeSelect = document.getElementById('locale');
        const prefsStatus = document.getElementById('prefs-status');
        const prefsError = document.getElementById('prefs-error');

        async function savePreferences() {
            prefsError.hidden = true;
            prefsStatus.textContent = window.mypassT('Guardando…');
            try {
                const body = await api(@json(route('web.security.preferences')), {
                    method: 'POST',
                    body: {
                        locale: localeSelect.value,
                        simple_mode: simpleMode.checked,
                        easy_mode: easyMode ? easyMode.checked : null,
                    },
                });
                prefsStatus.textContent = body.message;
                window.location.reload();
            } catch (error) {
                prefsStatus.textContent = '';
                prefsError.textContent = error.message;
                prefsError.hidden = false;
            }
        }

        simpleMode?.addEventListener('change', savePreferences);
        easyMode?.addEventListener('change', savePreferences);
        localeSelect?.addEventListener('change', savePreferences);

        const autoLock = document.getElementById('auto-lock');
        const autoLockStatus = document.getElementById('auto-lock-status');
        const autoLockKey = 'mypass.autoLockMinutes';
        const defaultAutoLock = @json($user->usesEasyMode() ? '30' : '10');

        if (autoLock) {
            const saved = localStorage.getItem(autoLockKey);
            autoLock.value = saved === null ? defaultAutoLock : saved;
            autoLock.addEventListener('change', () => {
                localStorage.setItem(autoLockKey, autoLock.value);
                autoLockStatus.textContent = autoLock.value === '0'
                    ? window.mypassT('El bloqueo automático está desactivado.')
                    : window.mypassTF('La bóveda se bloqueará tras :minutes minutos sin actividad.', { minutes: autoLock.value });
            });
        }
    </script>
</x-layout>
