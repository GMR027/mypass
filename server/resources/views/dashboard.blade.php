<x-layout :title="__('Panel')">
    @php
        $user = auth()->user();
        $firstName = str($user->name)->trim()->explode(' ')->first() ?: $user->email;
        $hour = (int) now()->format('G');
        $greeting = $hour < 12 ? __('Buenos días') : ($hour < 19 ? __('Buenas tardes') : __('Buenas noches'));
    @endphp

    <div class="mb-7 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div class="flex items-center gap-4">
            <span class="flex h-14 w-14 items-center justify-center rounded-2xl bg-primary text-xl font-bold uppercase text-on-primary ring-4 ring-primary-fixed/60" aria-hidden="true">
                {{ mb_substr($user->name ?: $user->email, 0, 1) }}
            </span>
            <div>
                <p class="mp-eyebrow">{{ __('Mi bóveda') }}</p>
                <h1 class="mp-title-lg mt-1">{{ $greeting }}, {{ $firstName }}</h1>
                <p class="mp-hint mt-1 mp-break">{{ __('Sesión iniciada como :email.', ['email' => $user->email]) }}</p>
            </div>
        </div>
        <div class="flex flex-wrap items-center gap-2">
            <span class="mp-badge {{ $hasVault ? 'mp-badge-primary' : 'mp-badge-warning' }}">
                <x-icon :name="$hasVault ? 'verified_user' : 'shield'" class="text-[17px]" :filled="$hasVault" />
                {{ $hasVault ? __('Bóveda configurada') : __('Bóveda pendiente') }}
            </span>
            <span class="mp-badge mp-badge-neutral">
                <x-icon name="schedule" class="text-[17px]" />
                {{ now()->translatedFormat('d M Y') }}
            </span>
        </div>
    </div>

    <section class="mp-hero-panel p-6 sm:p-8 lg:p-10">
        <div class="mp-hero-copy grid items-center gap-8 lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)]">
            <div>
                <span class="mp-hero-pill">
                    <span class="h-1.5 w-1.5 rounded-full bg-accent"></span>
                    {{ __('Tu billetera, primero') }}
                </span>
                <h2 class="mt-4 text-[clamp(2.05rem,3.6vw,3.4rem)] font-bold leading-[1.01] tracking-[-0.045em] text-white">
                    {{ $hasVault ? __('Abre tus pases en un toque.') : __('Prepara tu bóveda y estrena tu billetera.') }}
                </h2>
                <p class="mt-4 max-w-xl text-lg leading-relaxed text-primary-fixed-dim">
                    {{ $hasVault
                        ? __('Al desbloquear, tus pases y boletos aparecen primero. Ten a la mano lo que vas a usar y deja el resto protegido.')
                        : __('Crea la llave de tu bóveda y guarda tu llave de emergencia en esta computadora. Después tus pases estarán siempre a la mano.') }}
                </p>
                <div class="mt-7 flex flex-col gap-3 sm:flex-row">
                    <a href="{{ $hasVault ? route('vault', ['tab' => 'pases']) : route('onboarding') }}" class="mp-btn mp-btn-primary">
                        <x-icon name="wallet" class="text-[21px]" />
                        {{ $hasVault ? __('Abrir mi billetera') : __('Configurar mi bóveda') }}
                    </a>
                    <a href="{{ route('vault', ['tab' => 'docs']) }}" class="mp-btn mp-btn-secondary border-white/20 bg-white/10 text-white hover:bg-white/15">
                        <x-icon name="folder_open" class="text-[20px]" />
                        {{ __('Ir a documentos') }}
                    </a>
                </div>
                <p class="mt-4 flex items-center gap-2 text-sm text-white/60">
                    <x-icon name="lock" class="text-[17px]" />
                    {{ __('Cifrado de punta a punta en este navegador.') }}
                </p>
            </div>

            <div class="relative mx-auto w-full max-w-[360px]">
                <div class="rounded-[1.75rem] border border-white/15 bg-white/10 p-4 backdrop-blur-sm">
                    <div class="rounded-[1.25rem] bg-white p-4 text-on-surface shadow-2xl">
                        <div class="flex items-center justify-between gap-2">
                            <span class="mp-badge mp-badge-primary">
                                <x-icon name="wallet" class="text-[16px]" filled />
                                {{ __('Mi billetera') }}
                            </span>
                            <span class="text-[11px] font-bold uppercase tracking-[0.14em] text-on-surface-variant">{{ __('Próximo') }}</span>
                        </div>
                        <div class="mt-4 flex items-end justify-between border-b border-outline-variant/40 pb-4">
                            <div>
                                <p class="text-[11px] font-bold uppercase tracking-[0.14em] text-outline">{{ __('Pase de abordar') }}</p>
                                <p class="mt-1 text-[2rem] font-bold leading-none tracking-[-0.06em] text-on-surface">MAD <span class="text-outline">→</span> MEX</p>
                            </div>
                            <span class="material-symbols-outlined text-[32px] text-primary">flight_takeoff</span>
                        </div>
                        <div class="mt-3 grid grid-cols-3 gap-2 rounded-xl bg-surface-container-low p-2.5 text-center">
                            <div><span class="block text-[9px] font-semibold uppercase tracking-[0.1em] text-outline">{{ __('Puerta') }}</span><strong class="text-sm">12</strong></div>
                            <div><span class="block text-[9px] font-semibold uppercase tracking-[0.1em] text-outline">{{ __('Asiento') }}</span><strong class="text-sm">14A</strong></div>
                            <div><span class="block text-[9px] font-semibold uppercase tracking-[0.1em] text-outline">{{ __('Hora') }}</span><strong class="text-sm">09:40</strong></div>
                        </div>
                        <div class="mt-4 flex items-center justify-between">
                            <span class="text-xs text-on-surface-variant">{{ __('Vista previa de tu billetera') }}</span>
                            <x-icon name="qr_code_2" class="text-[34px] text-on-surface" />
                        </div>
                    </div>
                </div>
                <div class="absolute -bottom-4 -right-3 rounded-2xl bg-accent px-4 py-3 text-xs font-bold text-accent-deep shadow-lift">
                    {{ __('Pases primero') }}
                </div>
            </div>
        </div>
    </section>

    @if ($expiringCount > 0)
        <div class="mp-alert mp-alert-warning mt-5" role="status">
            <x-icon name="schedule" class="mt-0.5 text-[22px]" />
            <div class="flex flex-1 flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                <p class="font-semibold">{{ $expiringCount === 1 ? __(':count documento por vencer', ['count' => $expiringCount]) : __(':count documentos por vencer', ['count' => $expiringCount]) }}</p>
                <a href="{{ route('vault', ['tab' => 'docs']) }}" class="font-semibold underline underline-offset-4">{{ __('Ver documentos') }}</a>
            </div>
        </div>
    @endif

    <div class="mt-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <a href="{{ route('vault', ['tab' => 'pases']) }}" class="mp-stat-card group transition-all hover:-translate-y-0.5 hover:shadow-lift">
            <div class="flex items-center justify-between">
                <span class="mp-quick-tile__icon"><x-icon name="wallet" class="text-[21px]" /></span>
                <x-icon name="arrow_forward" class="text-[18px] text-outline transition-transform group-hover:translate-x-0.5" />
            </div>
            <p class="mt-3 text-[26px] font-bold leading-none tracking-tight text-on-surface">{{ $passCount }}</p>
            <p class="mt-1 text-[13px] font-semibold text-on-surface-variant">{{ __('Pases y boletos') }}</p>
        </a>
        <a href="{{ route('vault', ['tab' => 'docs']) }}" class="mp-stat-card group transition-all hover:-translate-y-0.5 hover:shadow-lift">
            <div class="flex items-center justify-between">
                <span class="mp-quick-tile__icon"><x-icon name="folder" class="text-[21px]" /></span>
                <x-icon name="arrow_forward" class="text-[18px] text-outline transition-transform group-hover:translate-x-0.5" />
            </div>
            <p class="mt-3 text-[26px] font-bold leading-none tracking-tight text-on-surface">{{ $documentCount }}</p>
            <p class="mt-1 text-[13px] font-semibold text-on-surface-variant">{{ __('Documentos') }}</p>
        </a>
        <a href="{{ route('vault', ['tab' => 'family']) }}" class="mp-stat-card group transition-all hover:-translate-y-0.5 hover:shadow-lift">
            <div class="flex items-center justify-between">
                <span class="mp-quick-tile__icon"><x-icon name="family_restroom" class="text-[21px]" /></span>
                <x-icon name="arrow_forward" class="text-[18px] text-outline transition-transform group-hover:translate-x-0.5" />
            </div>
            <p class="mt-3 text-[26px] font-bold leading-none tracking-tight text-on-surface">{{ $contactCount }}</p>
            <p class="mt-1 text-[13px] font-semibold text-on-surface-variant">{{ __('Contactos de confianza') }}</p>
        </a>
        <a href="{{ route('vault', ['tab' => 'devices']) }}" class="mp-stat-card group transition-all hover:-translate-y-0.5 hover:shadow-lift">
            <div class="flex items-center justify-between">
                <span class="mp-quick-tile__icon"><x-icon name="devices" class="text-[21px]" /></span>
                <x-icon name="arrow_forward" class="text-[18px] text-outline transition-transform group-hover:translate-x-0.5" />
            </div>
            <p class="mt-3 text-[26px] font-bold leading-none tracking-tight text-on-surface">{{ $deviceCount }}</p>
            <p class="mt-1 text-[13px] font-semibold text-on-surface-variant">{{ __('Dispositivos conectados') }}</p>
        </a>
    </div>

    <div class="mt-5 grid gap-4 lg:grid-cols-3">
        <a href="{{ route('settings.security') }}" class="mp-card mp-card-pad group transition-all hover:-translate-y-0.5 hover:shadow-lift">
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

        <a href="{{ route('vault', ['tab' => 'docs']) }}" class="mp-card mp-card-pad group transition-all hover:-translate-y-0.5 hover:shadow-lift">
            <div class="flex items-start justify-between gap-3">
                <span class="mp-icon-tile"><x-icon name="folder_open" /></span>
                <x-icon name="arrow_forward" class="text-[21px] text-outline transition-transform group-hover:translate-x-1" />
            </div>
            <h2 class="mp-title-sm mt-4">{{ __('Documentos') }}</h2>
            <p class="mp-body-sm mt-2">{{ __('Tus archivos cifrados, listos para abrir cuando los necesites.') }}</p>
            <p class="mt-4 text-[15px] font-semibold text-primary">
                {{ __('Abrir mi bóveda') }}
            </p>
        </a>

        <a href="{{ route('vault', ['tab' => 'family']) }}" class="mp-card mp-card-pad group transition-all hover:-translate-y-0.5 hover:shadow-lift">
            <div class="flex items-start justify-between gap-3">
                <span class="mp-icon-tile"><x-icon name="group" /></span>
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
