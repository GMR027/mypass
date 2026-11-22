<x-auth-shell :title="__('Crear cuenta')">
    <div class="mb-6">
        <span class="mp-icon-tile-lg">
            <x-icon name="badge" class="text-[30px]" />
        </span>
        <h1 class="mp-title-lg mt-4">{{ __('Crear cuenta') }}</h1>
        <p class="mp-body mt-2">{{ __('Crea tu cuenta y después configura tu bóveda personal con calma.') }}</p>
    </div>

    @if ($errors->any())
        <div class="mp-alert mp-alert-error mb-5" role="alert">
            <x-icon name="error" class="mt-0.5 text-[22px]" />
            <div>
                <p class="font-semibold">{{ __('Revisa la información indicada abajo') }}</p>
                <ul class="mt-1 list-inside list-disc space-y-1">
                    @foreach ($errors->all() as $error)
                        <li>{{ $error }}</li>
                    @endforeach
                </ul>
            </div>
        </div>
    @endif

    <form method="POST" action="{{ route('register') }}" class="space-y-5" id="register-form">
        @csrf

        <div>
            <label for="name" class="mp-label">{{ __('Nombre o alias completo') }}</label>
            <input id="name" name="name" type="text" value="{{ old('name') }}" required autofocus
                   autocomplete="name" class="mp-input" placeholder="{{ __('Nombre') }}">
            <p class="mp-hint mt-1.5">{{ __('Así nos dirigiremos a ti en tus avisos de seguridad.') }}</p>
        </div>

        <div>
            <label for="email" class="mp-label">{{ __('Correo electrónico') }}</label>
            <input id="email" name="email" type="email" value="{{ old('email') }}" required
                   autocomplete="email" inputmode="email" class="mp-input" placeholder="edgar@ejemplo.com">
        </div>

        <div>
            <label for="password" class="mp-label">{{ __('Contraseña segura') }}</label>
            <div class="relative">
                <input id="password" name="password" type="password" required minlength="12"
                       autocomplete="new-password" class="mp-input pr-14" aria-describedby="password-rules">
                <button type="button" data-toggle-password="password" aria-pressed="false"
                        aria-label="{{ __('Mostrar contraseña') }}"
                        class="absolute right-1.5 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-lg text-on-surface-variant hover:bg-surface-container">
                    <x-icon name="visibility" class="text-[22px]" />
                </button>
            </div>
            <p class="mp-hint mt-1.5">{{ __('Usa una frase memorable de 12 caracteres o más.') }}</p>

            <ul id="password-rules" class="mt-3 grid gap-1.5 text-[15px] text-on-surface-variant sm:grid-cols-3">
                <li data-rule="length" class="flex items-center gap-1.5">
                    <x-icon name="radio_button_unchecked" class="text-[18px]" />
                    {{ __('12+ caracteres') }}
                </li>
                <li data-rule="letter" class="flex items-center gap-1.5">
                    <x-icon name="radio_button_unchecked" class="text-[18px]" />
                    {{ __('Al menos 1 letra') }}
                </li>
                <li data-rule="number" class="flex items-center gap-1.5">
                    <x-icon name="radio_button_unchecked" class="text-[18px]" />
                    {{ __('Al menos 1 número') }}
                </li>
            </ul>
        </div>

        <div>
            <label for="password_confirmation" class="mp-label">{{ __('Repite la contraseña') }}</label>
            <div class="relative">
                <input id="password_confirmation" name="password_confirmation" type="password" required
                       autocomplete="new-password" class="mp-input pr-14">
                <button type="button" data-toggle-password="password_confirmation" aria-pressed="false"
                        aria-label="{{ __('Mostrar contraseña') }}"
                        class="absolute right-1.5 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-lg text-on-surface-variant hover:bg-surface-container">
                    <x-icon name="visibility" class="text-[22px]" />
                </button>
            </div>
            <p id="password-match" class="mp-hint mt-1.5 hidden" role="status"></p>
        </div>

        <button type="submit" class="mp-btn mp-btn-primary w-full">
            {{ __('Crear cuenta') }}
            <x-icon name="arrow_forward" class="text-[21px]" />
        </button>
    </form>

    <div class="mp-divider">{{ __('o') }}</div>

    <p class="text-center text-[16px] text-on-surface-variant">
        {{ __('¿Ya tienes cuenta?') }}
        <a href="{{ route('login') }}" class="font-semibold text-primary underline decoration-primary/40 underline-offset-4 hover:decoration-primary">
            {{ __('Entrar') }}
        </a>
    </p>

    <div class="mt-6 flex items-start gap-3 rounded-xl bg-surface-container-low p-4 text-sm leading-relaxed text-on-surface-variant">
        <x-icon name="security" class="mt-0.5 text-[20px] text-primary" />
        <p>{{ __('No requerimos tarjeta de crédito ni datos bancarios para comenzar.') }}</p>
    </div>

    <script>
        const password = document.getElementById('password');
        const confirmation = document.getElementById('password_confirmation');
        const match = document.getElementById('password-match');

        function updateRules() {
            const value = password.value;
            const checks = {
                length: value.length >= 12,
                letter: /[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]/.test(value),
                number: /\d/.test(value),
            };

            for (const [rule, valid] of Object.entries(checks)) {
                const item = document.querySelector(`[data-rule="${rule}"]`);
                const icon = item?.querySelector('.material-symbols-outlined');
                item?.classList.toggle('text-primary', valid);
                item?.classList.toggle('font-semibold', valid);
                if (icon) {
                    icon.textContent = valid ? 'check_circle' : 'radio_button_unchecked';
                }
            }

            if (!confirmation.value) {
                match.classList.add('hidden');
                match.classList.remove('text-primary', 'text-error');
            } else if (confirmation.value === value) {
                match.textContent = @json(__('Las contraseñas coinciden.'));
                match.classList.remove('hidden', 'text-error');
                match.classList.add('text-primary');
            } else {
                match.textContent = @json(__('Las contraseñas no coinciden.'));
                match.classList.remove('hidden', 'text-primary');
                match.classList.add('text-error');
            }
        }

        password.addEventListener('input', updateRules);
        confirmation.addEventListener('input', updateRules);
    </script>
</x-auth-shell>
