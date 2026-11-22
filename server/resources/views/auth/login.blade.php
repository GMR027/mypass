<x-auth-shell :title="__('Entrar')">
    <div class="mb-6">
        <span class="mp-icon-tile-lg">
            <x-icon name="lock_open" class="text-[30px]" />
        </span>
        <h1 class="mp-title-lg mt-4">{{ __('Entrar') }}</h1>
        <p class="mp-body mt-2">{{ __('Usa tu cuenta para abrir la bóveda.') }}</p>
    </div>

    @if ($errors->any())
        <div class="mp-alert mp-alert-error mb-5" role="alert">
            <x-icon name="error" class="mt-0.5 text-[22px]" />
            <div>
                <p class="font-semibold">{{ __('Error de acceso') }}</p>
                <p class="mt-1">{{ $errors->first() }}</p>
            </div>
        </div>
    @endif

    <form method="POST" action="{{ route('login') }}" class="space-y-5">
        @csrf

        <div>
            <label for="email" class="mp-label">{{ __('Correo') }}</label>
            <input id="email" name="email" type="email" value="{{ old('email') }}" required autofocus
                   autocomplete="email" inputmode="email" class="mp-input"
                   placeholder="edgar@ejemplo.com">
        </div>

        <div>
            <label for="password" class="mp-label">{{ __('Contraseña') }}</label>
            <div class="relative">
                <input id="password" name="password" type="password" required
                       autocomplete="current-password" class="mp-input pr-14">
                <button type="button" data-toggle-password="password" aria-pressed="false"
                        aria-label="{{ __('Mostrar contraseña') }}"
                        class="absolute right-1.5 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-lg text-on-surface-variant hover:bg-surface-container">
                    <x-icon name="visibility" class="text-[22px]" />
                </button>
            </div>
        </div>

        <label class="flex items-center gap-3 text-[16px] font-medium text-on-surface">
            <input type="checkbox" name="remember" value="1" class="mp-checkbox" @checked(old('remember'))>
            {{ __('Recordarme en este equipo') }}
        </label>

        <button type="submit" class="mp-btn mp-btn-primary w-full">
            {{ __('Entrar') }}
            <x-icon name="arrow_forward" class="text-[21px]" />
        </button>
    </form>

    <div class="mp-divider">{{ __('o') }}</div>

    <p class="text-center text-[16px] text-on-surface-variant">
        {{ __('¿No tienes cuenta?') }}
        <a href="{{ route('register') }}" class="font-semibold text-primary underline decoration-primary/40 underline-offset-4 hover:decoration-primary">
            {{ __('Crear cuenta') }}
        </a>
    </p>

    <div class="mt-6 flex items-start gap-3 rounded-xl bg-surface-container-low p-4 text-sm leading-relaxed text-on-surface-variant">
        <x-icon name="verified_user" class="mt-0.5 text-[20px] text-primary" />
        <p>{{ __('Sesión protegida bajo arquitectura Zero-Knowledge') }}</p>
    </div>
</x-auth-shell>
