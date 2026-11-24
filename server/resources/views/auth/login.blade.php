<x-auth-shell :title="__('Entrar')">
    <div class="mb-6">
        <div class="flex items-start justify-between gap-4">
            <span class="mp-icon-tile-lg">
                <x-icon name="lock_open" class="text-[30px]" />
            </span>
            <span class="mp-badge mp-badge-primary">
                <x-icon name="encrypted" class="text-[16px]" filled />
                {{ __('Cifrado local') }}
            </span>
        </div>
        <p class="mp-eyebrow mt-5">{{ __('Tu billetera digital') }}</p>
        <h1 class="mp-title-lg mt-2">{{ __('Entrar') }}</h1>
        <p class="mp-body mt-2">{{ __('Abre tu cuenta y ten tus pases a la mano en segundos.') }}</p>
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

        <div class="flex flex-wrap items-center justify-between gap-2">
            <label class="flex items-center gap-3 text-[16px] font-medium text-on-surface">
                <input type="checkbox" name="remember" value="1" class="mp-checkbox" @checked(old('remember'))>
                {{ __('Recordarme en este equipo') }}
            </label>
            <a href="{{ route('password.request') }}" class="text-[15px] font-semibold text-primary underline decoration-primary/40 underline-offset-4 hover:decoration-primary">
                {{ __('¿Olvidaste tu contraseña?') }}
            </a>
        </div>

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

    <p class="mt-3 text-center text-[14px] text-on-surface-variant">
        {{ __('Si acabas de crear tu cuenta, revisa tu correo para confirmarla.') }}
    </p>

    <div class="mt-6 flex items-start gap-3 rounded-2xl bg-accent-soft p-4 text-sm leading-relaxed text-on-surface-variant">
        <x-icon name="bolt" class="mt-0.5 text-[20px] text-accent-deep" />
        <p>{{ __('Al desbloquear tu bóveda, tus pases y boletos aparecen primero.') }}</p>
    </div>

    <div class="mt-6 rounded-2xl border border-primary/15 bg-primary-fixed/40 p-4">
        <div class="flex items-start gap-3">
            <span class="mp-icon-tile">
                <x-icon name="android" />
            </span>
            <div class="min-w-0 flex-1">
                <p class="font-semibold text-primary">{{ __('Lleva My Pass en tu Android') }}</p>
                <p class="mt-1 text-sm leading-relaxed text-on-surface-variant">{{ __('Descarga la app y vincula tu bóveda con un código de un solo uso.') }}</p>
                <a href="{{ route('app.download', ['v' => $androidVersionCode]) }}" class="mp-btn mp-btn-secondary mt-3 w-full" download>
                    <x-icon name="download" class="text-[19px]" />
                    {{ __('Descargar APK') }}
                </a>
            </div>
        </div>
    </div>
</x-auth-shell>
