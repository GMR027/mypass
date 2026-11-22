<x-auth-shell :title="__('Verificación por correo')">
    <div class="mb-6">
        <span class="mp-icon-tile-lg">
            <x-icon name="mark_email_read" class="text-[30px]" />
        </span>
        <h1 class="mp-title-lg mt-4">{{ __('Verificación por correo') }}</h1>
        <p class="mp-body mt-2">{{ __('Para proteger tu cuenta, enviamos un código de 6 dígitos a tu correo.') }}</p>
    </div>

    <div class="mp-card mp-card-pad">
        <div class="flex items-start gap-3 rounded-xl bg-surface-container-low p-4">
            <x-icon name="info" class="mt-0.5 text-[21px] text-primary" />
            <p class="mp-hint">{{ __('Si no lo recibes, revisa la carpeta de spam o pide un código nuevo.') }}</p>
        </div>

        <button id="send-code" type="button" class="mp-btn mp-btn-primary mt-5 w-full">
            <x-icon name="mail" class="text-[21px]" />
            <span>{{ __('Enviar código por correo') }}</span>
        </button>

        <p id="status" class="mt-3 text-[15px] font-medium text-primary" role="status"></p>
        <p id="error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>

        <form id="code-form" class="mt-6 space-y-4">
            <div>
                <label for="code" class="mp-label">{{ __('Código del correo') }}</label>
                <input id="code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" required
                       class="mp-input text-center font-mono text-2xl tracking-[0.35em]" placeholder="000000">
            </div>
            <button type="submit" class="mp-btn mp-btn-secondary w-full">{{ __('Verificar') }}</button>
        </form>
    </div>

    <p class="mt-6 text-center text-[16px]">
        <a class="inline-flex items-center gap-1.5 font-semibold text-primary underline decoration-primary/40 underline-offset-4 hover:decoration-primary"
           href="{{ route('login') }}">
            <x-icon name="arrow_back" class="text-[19px]" />
            {{ __('Volver a iniciar sesión') }}
        </a>
    </p>

    <script nonce="{{ $cspNonce }}">
        const csrfToken = document.querySelector('meta[name="csrf-token"]').content;
        const status = document.getElementById('status');
        const error = document.getElementById('error');
        const form = document.getElementById('code-form');
        const sendButton = document.getElementById('send-code');

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

        sendButton.addEventListener('click', async () => {
            error.hidden = true;
            status.textContent = window.mypassT('Enviando…');
            sendButton.disabled = true;
            try {
                const data = await post(@json(route('email-two-factor.send')));
                status.textContent = data.message;
                sendButton.querySelector('span').textContent = window.mypassT('Enviar otro código');
            } catch (exception) {
                status.textContent = '';
                error.textContent = exception.message;
                error.hidden = false;
            } finally {
                sendButton.disabled = false;
            }
        });

        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            error.hidden = true;
            try {
                const data = await post(@json(route('email-two-factor.verify')), {
                    code: document.getElementById('code').value.trim(),
                });
                window.location.href = data.redirect;
            } catch (exception) {
                error.textContent = exception.message;
                error.hidden = false;
            }
        });
    </script>
</x-auth-shell>
