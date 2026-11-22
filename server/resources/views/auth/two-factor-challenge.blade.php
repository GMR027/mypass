@php($pendingUser = \App\Models\User::find(session('login.id')))

<x-auth-shell :title="__('Verificación en dos pasos')">
    <div class="mb-6">
        <span class="mp-icon-tile-lg">
            <x-icon name="shield_lock" class="text-[30px]" />
        </span>
        <h1 class="mp-title-lg mt-4">{{ __('Verificación en dos pasos') }}</h1>
        <p class="mp-body mt-2">{{ __('Escribe el código de 6 dígitos de tu app de autenticación.') }}</p>
        @if ($pendingUser)
            <p class="mt-2 text-[15px] font-semibold text-primary">{{ $pendingUser->email }}</p>
        @endif
    </div>

    @if ($errors->any())
        <div class="mp-alert mp-alert-error mb-5" role="alert">
            <x-icon name="error" class="mt-0.5 text-[22px]" />
            <div>
                <p class="font-semibold">{{ __('No pudimos verificar el código') }}</p>
                <p class="mt-1">{{ $errors->first() }}</p>
            </div>
        </div>
    @endif

    <div class="mp-card mp-card-pad">
        <form method="POST" action="{{ route('two-factor.login.store') }}" class="space-y-5">
            @csrf

            <div>
                <label for="code" class="mp-label">{{ __('Código de la app') }}</label>
                <input id="code" name="code" inputmode="numeric" autocomplete="one-time-code" maxlength="6"
                       required autofocus class="mp-input text-center font-mono text-2xl tracking-[0.35em]"
                       placeholder="000000">
                <p class="mp-hint mt-2">{{ __('Abre tu app de autenticación y escribe el código actual.') }}</p>
            </div>

            <button type="submit" class="mp-btn mp-btn-primary w-full">
                {{ __('Verificar') }}
                <x-icon name="arrow_forward" class="text-[21px]" />
            </button>
        </form>

        <div class="mp-divider">{{ __('o') }}</div>

        <details class="group rounded-xl border border-outline-variant/60 bg-surface-container-low/60">
            <summary class="flex min-h-14 cursor-pointer list-none items-center justify-between gap-3 rounded-xl px-4 font-semibold text-on-surface hover:bg-surface-container">
                <span class="flex items-center gap-2">
                    <x-icon name="key" class="text-[21px] text-primary" />
                    {{ __('Usar un código de recuperación') }}
                </span>
                <x-icon name="expand_more" class="text-[21px] transition-transform group-open:rotate-180" />
            </summary>

            <form method="POST" action="{{ route('two-factor.login.store') }}" class="space-y-4 border-t border-outline-variant/60 p-4">
                @csrf
                <div>
                    <label for="recovery_code" class="mp-label">{{ __('Código de recuperación') }}</label>
                    <input id="recovery_code" name="recovery_code" required
                           autocomplete="one-time-code" class="mp-input font-mono">
                    <p class="mp-hint mt-2">{{ __('Cada código de recuperación sirve una sola vez.') }}</p>
                </div>
                <button type="submit" class="mp-btn mp-btn-secondary w-full">
                    {{ __('Verificar código de recuperación') }}
                </button>
            </form>
        </details>
    </div>

    @if ($pendingUser?->email_two_factor_enabled)
        <div class="mp-card mp-card-pad mt-5">
            <div class="flex items-start gap-3">
                <span class="mp-icon-tile">
                    <x-icon name="mark_email_read" class="text-[23px]" />
                </span>
                <div>
                    <h2 class="font-semibold text-on-surface">{{ __('¿No tienes la app?') }}</h2>
                    <p class="mp-hint mt-1">{{ __('Podemos enviarte un código a :email.', ['email' => $pendingUser->email]) }}</p>
                </div>
            </div>

            <button id="send-email-code" type="button" class="mp-btn mp-btn-secondary mt-4 w-full">
                <x-icon name="mail" class="text-[21px]" />
                {{ __('Enviar código por correo') }}
            </button>

            <p id="email-status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
            <p id="email-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>

            <form id="email-code-form" class="mt-4 space-y-3" hidden>
                <label for="email-code" class="mp-label">{{ __('Código del correo') }}</label>
                <input id="email-code" inputmode="numeric" autocomplete="one-time-code" maxlength="6"
                       class="mp-input text-center font-mono text-2xl tracking-[0.35em]" placeholder="000000">
                <button type="submit" class="mp-btn mp-btn-primary w-full">{{ __('Verificar código del correo') }}</button>
            </form>
        </div>

        <script>
            const csrfToken = document.querySelector('meta[name="csrf-token"]').content;
            const sendButton = document.getElementById('send-email-code');
            const emailStatus = document.getElementById('email-status');
            const emailError = document.getElementById('email-error');
            const emailForm = document.getElementById('email-code-form');

            async function post(url, body) {
                const response = await fetch(url, {
                    method: 'POST',
                    headers: {
                        Accept: 'application/json',
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': csrfToken,
                    },
                    credentials: 'same-origin',
                    body: JSON.stringify(body ?? {}),
                });
                const data = await response.json().catch(() => ({}));
                if (!response.ok) {
                    throw new Error(data.message || window.mypassT('No se pudo completar la operación.'));
                }
                return data;
            }

            sendButton?.addEventListener('click', async () => {
                emailError.hidden = true;
                emailStatus.textContent = window.mypassT('Enviando…');
                sendButton.disabled = true;
                try {
                    const data = await post(@json(route('email-two-factor.send')));
                    emailStatus.textContent = data.message;
                    emailForm.hidden = false;
                } catch (error) {
                    emailStatus.textContent = '';
                    emailError.textContent = error.message;
                    emailError.hidden = false;
                } finally {
                    sendButton.disabled = false;
                }
            });

            emailForm?.addEventListener('submit', async (event) => {
                event.preventDefault();
                emailError.hidden = true;
                try {
                    const data = await post(@json(route('email-two-factor.verify')), {
                        code: document.getElementById('email-code').value.trim(),
                    });
                    window.location.href = data.redirect;
                } catch (error) {
                    emailError.textContent = error.message;
                    emailError.hidden = false;
                }
            });
        </script>
    @endif
</x-auth-shell>
