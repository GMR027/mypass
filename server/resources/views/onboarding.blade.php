<x-layout :title="__('Configurar bóveda')">
    <div class="mx-auto max-w-3xl">
        <div class="mp-hero-panel mb-6 p-6 sm:p-8">
            <div class="mp-hero-copy grid items-center gap-6 sm:grid-cols-[minmax(0,1fr)_auto]">
                <div>
                    <span class="mp-hero-pill">
                        <span class="h-1.5 w-1.5 rounded-full bg-accent"></span>
                        {{ __('Tu billetera digital') }}
                    </span>
                    <h1 class="mt-4 text-[clamp(2rem,3.4vw,3rem)] font-bold leading-[1.02] tracking-[-0.045em] text-white">{{ __('Configura tu bóveda y abre tus pases.') }}</h1>
                    <p class="mt-3 max-w-xl text-[17px] leading-relaxed text-primary-fixed-dim">{{ __('Crea la llave de tu bóveda y guarda la llave de emergencia. La llave de emergencia no se guarda en el servidor.') }}</p>
                </div>
                <span class="hidden h-24 w-24 items-center justify-center rounded-[2rem] bg-white/10 text-white sm:flex">
                    <x-icon name="vpn_key" class="text-[46px]" />
                </span>
            </div>
        </div>

        <ol class="mp-stepper" aria-label="{{ __('Pasos de configuración') }}">
            <li id="onboarding-step-1" class="mp-step" data-step="1" data-active="true">
                <span class="mp-step-number">1</span>
                <span>{{ __('Llave de bóveda') }}</span>
            </li>
            <li id="onboarding-step-2" class="mp-step" data-step="2">
                <span class="mp-step-number">2</span>
                <span>{{ __('Llave de emergencia') }}</span>
            </li>
            <li id="onboarding-step-3" class="mp-step" data-step="3">
                <span class="mp-step-number">3</span>
                <span>{{ __('Listo') }}</span>
            </li>
        </ol>

        <div id="onboarding-app"
             data-keys-url="{{ route('web.keys.show') }}"
             data-save-url="{{ route('web.keys.update') }}"
             data-email="{{ auth()->user()->email }}">
            <p id="onboarding-loading" class="mp-card mp-card-pad flex items-center gap-4" role="status">
                <span class="mp-spinner" aria-hidden="true"></span>
                <span>
                    <span class="block font-semibold text-on-surface">{{ __('Cargando…') }}</span>
                    <span class="mp-hint">{{ __('Verificando el estado de seguridad de tu cuenta. Un momento, por favor.') }}</span>
                </span>
            </p>

            <section id="onboarding-existing" hidden>
                <div class="mp-card mp-card-pad text-center">
                    <span class="mp-icon-tile-lg mx-auto bg-primary-fixed text-primary">
                        <x-icon name="verified" class="text-[32px]" filled />
                    </span>
                    <h2 class="mp-title-md mt-4">{{ __('Esta cuenta ya tiene una bóveda configurada.') }}</h2>
                    <p class="mp-body mt-2">{{ __('Puedes abrirla y desbloquearla con tu llave de bóveda de forma segura en cualquier momento.') }}</p>
                    <a href="{{ route('vault', ['tab' => 'pases']) }}" class="mp-btn mp-btn-primary mt-6">
                        <x-icon name="wallet" class="text-[21px]" />
                        {{ __('Abrir mi billetera') }}
                    </a>
                </div>
            </section>

            <form id="onboarding-form" class="mp-card mp-card-pad space-y-6" hidden>
                <div class="flex items-start gap-4 rounded-xl bg-surface-container-low p-4">
                    <span class="mp-icon-tile shrink-0 bg-white">
                        <x-icon name="enhanced_encryption" class="text-[24px]" />
                    </span>
                    <div>
                        <p class="text-sm font-bold uppercase tracking-[0.12em] text-primary">{{ __('Modo simple y seguro') }}</p>
                        <p class="mp-hint mt-1">{{ __('Crea una frase fácil de recordar. Nadie más puede verla, ni siquiera el servidor.') }}</p>
                    </div>
                </div>

                <div>
                    <label for="passphrase" class="mp-label">{{ __('Llave de la bóveda') }}</label>
                    <p class="mp-hint mb-2">{{ __('Una frase de 12 caracteres o más. La necesitarás para abrir la bóveda en esta computadora.') }}</p>
                    <div class="relative">
                        <input id="passphrase" name="passphrase" type="password" required minlength="12"
                               autocomplete="new-password" class="mp-input pr-14">
                        <button type="button" data-toggle-password="passphrase" aria-pressed="false"
                                aria-label="{{ __('Mostrar contraseña') }}"
                                class="absolute right-1.5 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-lg text-on-surface-variant hover:bg-surface-container">
                            <x-icon name="visibility" class="text-[22px]" />
                        </button>
                    </div>
                    <p class="mp-hint mt-2 flex items-start gap-2">
                        <x-icon name="info" class="mt-0.5 text-[18px] text-primary" />
                        {{ __('Mínimo 12 caracteres recomendados (puedes usar palabras fáciles de recordar con espacios).') }}
                    </p>
                </div>

                <div>
                    <label for="passphrase-confirmation" class="mp-label">{{ __('Repite la llave de la bóveda') }}</label>
                    <div class="relative">
                        <input id="passphrase-confirmation" name="passphrase_confirmation" type="password" required minlength="12"
                               autocomplete="new-password" class="mp-input pr-14">
                        <button type="button" data-toggle-password="passphrase-confirmation" aria-pressed="false"
                                aria-label="{{ __('Mostrar contraseña') }}"
                                class="absolute right-1.5 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-lg text-on-surface-variant hover:bg-surface-container">
                            <x-icon name="visibility" class="text-[22px]" />
                        </button>
                    </div>
                </div>

                <div>
                    <label for="recovery-length" class="mp-label">{{ __('Formato de la llave de emergencia') }}</label>
                    <select id="recovery-length" class="mp-select">
                        <option value="32" selected>{{ __('32 dígitos numéricos — altamente recomendado') }}</option>
                        <option value="16">{{ __('16 dígitos numéricos — menos seguro') }}</option>
                    </select>
                    <p id="recovery-length-warning" class="mp-alert mp-alert-warning mt-3" hidden>
                        <x-icon name="warning" class="mt-0.5 text-[21px]" />
                        <span>{{ __('Una llave de 16 dígitos es más fácil de adivinar. Úsala solo si necesitas escribirla a mano y aceptas el riesgo.') }}</span>
                    </p>
                </div>

                <p id="onboarding-error" class="mp-alert mp-alert-error" role="alert" hidden></p>

                <button id="onboarding-create" type="submit" class="mp-btn mp-btn-primary w-full sm:w-auto">
                    <x-icon name="vpn_key" class="text-[21px]" />
                    {{ __('Crear bóveda') }}
                </button>
            </form>

            <section id="onboarding-recovery" hidden>
                <div class="mp-card overflow-hidden">
                    <div class="border-b border-secondary/20 bg-secondary-fixed/60 p-5 md:p-7">
                        <div class="flex items-start gap-4">
                            <span class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-white text-secondary">
                                <x-icon name="security" class="text-[25px]" />
                            </span>
                            <div>
                                <p class="text-sm font-bold uppercase tracking-[0.12em] text-on-secondary-fixed">{{ __('Paso crítico: copia de seguridad física') }}</p>
                                <h2 class="mp-title-md mt-1 text-on-secondary-fixed">{{ __('Guarda tu llave de emergencia') }}</h2>
                            </div>
                        </div>
                        <p id="recovery-description" class="mt-4 max-w-2xl text-[16px] leading-relaxed text-on-secondary-fixed">
                            {{ __('Esta clave es la única forma de recuperar tu bóveda si pierdes la computadora y el celular. Escríbela en papel y guárdala en un lugar seguro.') }}
                        </p>
                    </div>

                    <div class="p-5 md:p-7">
                        <p class="flex items-center gap-2 text-sm font-bold uppercase tracking-[0.12em] text-secondary">
                            <x-icon name="pin" class="text-[19px]" />
                            {{ __('Llave numérica') }}
                        </p>
                        <p id="recovery-code" class="mt-3 break-words rounded-xl border border-outline-variant/60 bg-surface-container-low px-4 py-5 font-mono text-[20px] font-semibold tracking-[0.1em] text-on-surface md:text-[24px]"></p>

                        <div class="mt-4 flex flex-col gap-2 sm:flex-row sm:flex-wrap">
                            <button id="recovery-copy" type="button" class="mp-btn mp-btn-secondary">
                                <x-icon name="content_copy" class="text-[20px]" />
                                {{ __('Copiar clave') }}
                            </button>
                            <button id="recovery-download" type="button" class="mp-btn mp-btn-secondary">
                                <x-icon name="download" class="text-[20px]" />
                                {{ __('Descargar .mypss') }}
                            </button>
                            <button id="recovery-print" type="button" class="mp-btn mp-btn-secondary">
                                <x-icon name="print" class="text-[20px]" />
                                {{ __('Imprimir hoja') }}
                            </button>
                        </div>

                        <label class="mt-6 flex items-start gap-3 rounded-xl border border-outline-variant/60 p-4">
                            <input id="recovery-confirm" type="checkbox" class="mp-checkbox">
                            <span id="recovery-confirm-label" class="text-[16px] leading-relaxed text-on-surface">
                                {{ __('Confirmo que guardé la clave de emergencia en un lugar seguro y que entiendo que no se puede recuperar si la pierdo.') }}
                            </span>
                        </label>

                        <p id="recovery-error" class="mp-alert mp-alert-error mt-4" role="alert" hidden></p>

                        <button id="recovery-save" type="button" class="mp-btn mp-btn-primary mt-5 w-full sm:w-auto" disabled>
                            {{ __('Terminar configuración') }}
                            <x-icon name="arrow_forward" class="text-[21px]" />
                        </button>
                    </div>
                </div>
            </section>

            <section id="onboarding-done" hidden>
                <div class="mp-card mp-card-pad text-center">
                    <span class="mp-icon-tile-lg mx-auto bg-primary-fixed text-primary">
                        <x-icon name="check" class="text-[32px]" filled />
                    </span>
                    <p class="mp-eyebrow mt-4">{{ __('Paso 3 completado') }}</p>
                    <h2 class="mp-title-lg mt-2">{{ __('Bóveda configurada.') }}</h2>
                    <p class="mp-body mx-auto mt-2 max-w-lg">{{ __('Guarda el archivo o la hoja de recuperación antes de continuar. Tus credenciales y documentos ahora están protegidos con cifrado de nivel bancario.') }}</p>
                    <a href="{{ route('vault', ['tab' => 'pases']) }}" class="mp-btn mp-btn-primary mt-6">
                        <x-icon name="wallet" class="text-[21px]" />
                        {{ __('Abrir mi billetera') }}
                    </a>
                </div>
            </section>
        </div>
    </div>

    <script type="module" nonce="{{ $cspNonce }}">
        // El código de onboarding se carga desde resources/js/onboarding.js.
    </script>
</x-layout>
