<x-layout :title="__('Panel')">
    @php
        $user = auth()->user();
        $firstName = str($user->name)->trim()->explode(' ')->first() ?: $user->email;
    @endphp

    <div class="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div class="flex items-center gap-4">
            <span class="flex h-14 w-14 items-center justify-center rounded-full bg-primary-fixed text-xl font-bold uppercase text-primary" aria-hidden="true">
                {{ mb_substr($user->name ?: $user->email, 0, 1) }}
            </span>
            <div>
                <p class="mp-eyebrow">{{ __('Panel') }}</p>
                <h1 class="mp-title-lg mt-1">{{ __('Hola, :name', ['name' => $firstName]) }}</h1>
                <p class="mp-hint mt-1 mp-break">{{ __('Sesión iniciada como :email.', ['email' => $user->email]) }}</p>
            </div>
        </div>
    </div>

    <section class="mp-card overflow-hidden">
        <div class="grid gap-0 md:grid-cols-[minmax(0,1fr)_auto] md:items-center">
            <div class="p-6 md:p-8">
                <span class="mp-badge {{ $hasVault ? 'mp-badge-primary' : 'mp-badge-warning' }}">
                    <x-icon :name="$hasVault ? 'verified_user' : 'shield'" class="text-[17px]" />
                    {{ $hasVault ? __('Bóveda configurada') : __('Bóveda pendiente') }}
                </span>
                <h2 class="mp-title-lg mt-3">{{ $hasVault ? __('Tu bóveda está lista') : __('Configura tu bóveda') }}</h2>
                <p class="mp-body mt-2 max-w-xl">
                    {{ $hasVault
                        ? __('Abre tu bóveda para ver y administrar tus documentos.')
                        : __('Crea la llave de tu bóveda y guarda tu llave de emergencia en esta computadora.') }}
                </p>
                <div class="mt-6 flex flex-col gap-3 sm:flex-row">
                    <a href="{{ route('vault') }}" class="mp-btn mp-btn-primary">
                        <x-icon name="lock_open" class="text-[21px]" />
                        {{ $hasVault ? __('Abrir mi bóveda') : __('Configurar mi bóveda') }}
                    </a>
                    <a href="{{ route('settings.security') }}" class="mp-btn mp-btn-secondary">
                        <x-icon name="settings" class="text-[21px]" />
                        {{ __('Revisar configuración') }}
                    </a>
                </div>
            </div>
            <div class="hidden h-full min-h-56 items-center justify-center bg-surface-container-low p-8 md:flex">
                <img src="{{ asset('images/mypass-illustration.png') }}" alt=""
                     class="h-44 w-44 object-contain" loading="lazy" width="1024" height="1024">
            </div>
        </div>
    </section>

    @if ($expiringCount > 0)
        <div class="mp-alert mp-alert-warning mt-5" role="status">
            <x-icon name="schedule" class="mt-0.5 text-[22px]" />
            <div class="flex flex-1 flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                <p class="font-semibold">{{ $expiringCount === 1 ? __(':count documento por vencer', ['count' => $expiringCount]) : __(':count documentos por vencer', ['count' => $expiringCount]) }}</p>
                <a href="{{ route('vault') }}" class="font-semibold underline underline-offset-4">{{ __('Ver documentos') }}</a>
            </div>
        </div>
    @endif

    <div class="mt-5 grid gap-4 lg:grid-cols-3">
        <a href="{{ route('settings.security') }}" class="mp-card mp-card-pad group transition-shadow hover:shadow-lift">
            <div class="flex items-start justify-between gap-3">
                <span class="mp-icon-tile"><x-icon name="security" /></span>
                <x-icon name="arrow_forward" class="text-[21px] text-outline transition-transform group-hover:translate-x-1" />
            </div>
            <h2 class="mp-title-sm mt-4">{{ __('Seguridad') }}</h2>
            <p class="mp-body-sm mt-2">
                {{ $twoFactorEnabled ? __('Verificación en dos pasos activada') : __('Activa un segundo paso para proteger tu cuenta.') }}
            </p>
            <div class="mt-4">
                @if ($twoFactorEnabled)
                    <span class="mp-badge mp-badge-primary">
                        <x-icon name="check_circle" class="text-[17px]" filled />
                        {{ __('2FA activo') }}
                    </span>
                @else
                    <span class="mp-badge mp-badge-warning">
                        <x-icon name="warning" class="text-[17px]" />
                        {{ __('2FA inactivo') }}
                    </span>
                @endif
            </div>
        </a>

        <a href="{{ route('vault') }}" class="mp-card mp-card-pad group transition-shadow hover:shadow-lift">
            <div class="flex items-start justify-between gap-3">
                <span class="mp-icon-tile"><x-icon name="devices" /></span>
                <x-icon name="arrow_forward" class="text-[21px] text-outline transition-transform group-hover:translate-x-1" />
            </div>
            <h2 class="mp-title-sm mt-4">{{ __('Dispositivos') }}</h2>
            <p class="mp-body-sm mt-2">{{ __('Administra los celulares que pueden abrir tu bóveda.') }}</p>
            <p class="mt-4 text-[15px] font-semibold text-primary">
                {{ $deviceCount === 1 ? __(':count celular conectado', ['count' => $deviceCount]) : __(':count celulares conectados', ['count' => $deviceCount]) }}
            </p>
        </a>

        <a href="{{ route('vault') }}" class="mp-card mp-card-pad group transition-shadow hover:shadow-lift">
            <div class="flex items-start justify-between gap-3">
                <span class="mp-icon-tile"><x-icon name="family_restroom" /></span>
                <x-icon name="arrow_forward" class="text-[21px] text-outline transition-transform group-hover:translate-x-1" />
            </div>
            <h2 class="mp-title-sm mt-4">{{ __('Familia') }}</h2>
            <p class="mp-body-sm mt-2">{{ __('Comparte documentos con las personas que tú elijas.') }}</p>
            <p class="mt-4 text-[15px] font-semibold text-primary">
                {{ $contactCount === 1 ? __(':count contacto de confianza', ['count' => $contactCount]) : __(':count contactos de confianza', ['count' => $contactCount]) }}
            </p>
        </a>
    </div>
</x-layout>
