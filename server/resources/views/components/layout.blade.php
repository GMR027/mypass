@php
    $themePreference = in_array(request()->cookie('mypass_theme'), ['light', 'dark', 'system'], true)
        ? request()->cookie('mypass_theme')
        : 'system';
@endphp

<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}"
      data-theme="{{ $themePreference }}"
      @class(['dark' => $themePreference === 'dark'])>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <title>{{ $title ?? 'My Pass' }} · My Pass</title>
    <link rel="icon" type="image/png" sizes="32x32" href="{{ asset('images/favicon-32.png') }}">
    <link rel="apple-touch-icon" href="{{ asset('images/favicon-180.png') }}">
    <link rel="manifest" href="/manifest.webmanifest">
    <meta name="theme-color" content="{{ $themePreference === 'dark' ? '#08120e' : '#012d1d' }}">
    <x-translations />
    <script nonce="{{ $cspNonce }}">
        (function () {
            var preference = document.documentElement.getAttribute('data-theme') || 'system';
            var dark = preference === 'dark' || (preference === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
            document.documentElement.classList.toggle('dark', dark);
            var themeColor = document.querySelector('meta[name="theme-color"]');
            if (themeColor) {
                themeColor.setAttribute('content', dark ? '#08120e' : '#012d1d');
            }
        })();
    </script>
    @vite(['resources/css/app.css', 'resources/js/app.js'])
</head>
@php
    $shellUser = auth()->user();
@endphp
<body @if ($shellUser) data-easy="{{ $shellUser->usesEasyMode() ? '1' : '0' }}" @endif class="min-h-screen bg-surface text-on-surface {{ $shellUser?->usesLargeText() ? 'simple-mode' : '' }} {{ $shellUser?->usesEasyMode() ? 'easy-mode' : '' }}">
    <a href="#contenido" class="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[70] focus:rounded-xl focus:bg-primary focus:px-4 focus:py-3 focus:text-on-primary">
        {{ __('Saltar al contenido') }}
    </a>

    @php
        $isSpanish = str_starts_with(app()->getLocale(), 'es');
        $user = auth()->user();
        $unreadNotifications = $user?->userNotifications()->whereNull('read_at')->count() ?? 0;
        $vaultTab = request()->routeIs('vault') ? request()->query('tab', 'pases') : null;
        $activeNav = match (true) {
            request()->routeIs('dashboard') => 'inicio',
            request()->routeIs('settings.*') => 'perfil',
            request()->routeIs('vault') && in_array($vaultTab, ['pases', 'generador'], true) => 'pases',
            request()->routeIs('vault') && $vaultTab === 'docs' => 'boveda',
            request()->routeIs('vault') => 'boveda',
            default => '',
        };
    @endphp

    <header class="sticky top-0 z-40 border-b border-outline-variant/40 bg-surface/80 backdrop-blur-xl">
        <div class="mp-container flex min-h-20 flex-wrap items-center justify-between gap-2 py-2.5">
            <x-brand :href="$user ? route('dashboard') : route('login')" />

            @auth
                <nav class="hidden items-center gap-0.5 lg:flex" aria-label="{{ __('Navegación principal') }}">
                    @php
                        $desktopLinks = [
                            ['key' => 'inicio', 'label' => __('Inicio'), 'href' => route('dashboard'), 'tab' => null, 'active' => $activeNav === 'inicio'],
                            ['key' => 'pases', 'label' => __('Pases & Boletos'), 'href' => route('vault', ['tab' => 'pases']), 'tab' => 'pases', 'active' => $activeNav === 'pases'],
                            ['key' => 'boveda', 'label' => __('Bóveda'), 'href' => route('vault', ['tab' => 'docs']), 'tab' => 'docs', 'active' => $activeNav === 'boveda' && $vaultTab === 'docs'],
                            ['key' => 'family', 'label' => __('Familia'), 'href' => route('vault', ['tab' => 'family']), 'tab' => 'family', 'active' => $vaultTab === 'family'],
                            ['key' => 'devices', 'label' => __('Dispositivos'), 'href' => route('vault', ['tab' => 'devices']), 'tab' => 'devices', 'active' => $vaultTab === 'devices'],
                        ];
                    @endphp
                    @foreach ($desktopLinks as $link)
                        <a href="{{ $link['href'] }}"
                           @if ($link['tab']) data-vault-tab="{{ $link['tab'] }}" @endif
                           @if ($link['active']) aria-current="page" @endif
                           class="mp-nav-link inline-flex min-h-11 items-center rounded-xl px-3 text-[15px] font-semibold transition-all">{{ $link['label'] }}</a>
                    @endforeach
                </nav>

                <div class="flex items-center gap-1.5">
                    <span id="vault-status-pill"
                          class="hidden items-center gap-1.5 rounded-full bg-primary-fixed px-3 py-1.5 text-[13px] font-bold text-primary 2xl:inline-flex">
                        <x-icon name="shield_lock" class="text-[18px]" filled />
                        <span id="vault-status-label">{{ __('Bóveda protegida') }}</span>
                    </span>

                    <a href="{{ route('vault', ['tab' => 'pases']) }}"
                       class="hidden min-h-11 items-center gap-1.5 rounded-xl bg-accent-soft px-3 text-[14px] font-bold text-accent-deep transition-colors hover:bg-accent/20 sm:inline-flex">
                        <x-icon name="wallet" class="text-[19px]" />
                        {{ __('Mi billetera') }}
                    </a>

                    <x-theme-toggle />

                    <div class="flex items-center gap-0.5 rounded-xl bg-surface-container-low p-1" aria-label="{{ __('Idioma') }}">
                        <a href="{{ route('language.switch', 'es-MX') }}"
                           @class(['inline-flex min-h-10 items-center rounded-lg px-2.5 text-sm font-semibold', 'bg-paper text-primary shadow-sm' => $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => ! $isSpanish])
                           @if ($isSpanish) aria-current="true" @endif>ES</a>
                        <a href="{{ route('language.switch', 'en') }}"
                           @class(['inline-flex min-h-10 items-center rounded-lg px-2.5 text-sm font-semibold', 'bg-paper text-primary shadow-sm' => ! $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => $isSpanish])
                           @if (! $isSpanish) aria-current="true" @endif>EN</a>
                    </div>

                    <a href="{{ route('notifications.index') }}"
                       aria-label="{{ __('Notificaciones') }}"
                       class="relative inline-flex h-11 w-11 items-center justify-center rounded-xl text-on-surface-variant transition-colors hover:bg-surface-container hover:text-on-surface">
                        <x-icon name="notifications" class="text-[21px]" />
                        @if ($unreadNotifications > 0)
                            <span class="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-[11px] font-bold text-on-primary">
                                {{ $unreadNotifications > 9 ? '9+' : $unreadNotifications }}
                            </span>
                        @endif
                    </a>

                    <a href="{{ route('settings.security') }}"
                       aria-label="{{ __('Seguridad y perfil') }}"
                       class="hidden min-h-11 items-center gap-1.5 rounded-xl px-3 text-[15px] font-semibold text-on-surface-variant transition-colors hover:bg-surface-container hover:text-on-surface md:inline-flex">
                        <x-icon name="shield" class="text-[20px]" />
                    </a>

                    <a href="{{ route('settings.profile') }}"
                       class="ml-0.5 flex h-11 w-11 items-center justify-center overflow-hidden rounded-full bg-primary text-base font-bold uppercase text-on-primary ring-4 ring-primary-fixed/60 transition-transform hover:scale-105"
                       aria-label="{{ __('Abrir mi perfil') }}">
                        @if ($user->avatar_url)
                            <img src="{{ $user->avatar_url }}" alt="" class="h-full w-full object-cover">
                        @else
                            {{ mb_substr($user->name ?: $user->email, 0, 1) }}
                        @endif
                    </a>
                </div>
            @else
                <nav class="flex flex-wrap items-center gap-1.5" aria-label="{{ __('Navegación principal') }}">
                    <x-theme-toggle />

                    <div class="flex items-center gap-0.5 rounded-xl bg-surface-container-low p-1" aria-label="{{ __('Idioma') }}">
                        <a href="{{ route('language.switch', 'es-MX') }}"
                           @class(['inline-flex min-h-12 items-center rounded-lg px-2.5 text-sm font-semibold', 'bg-paper text-primary shadow-sm' => $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => ! $isSpanish])
                           @if ($isSpanish) aria-current="true" @endif>ES</a>
                        <a href="{{ route('language.switch', 'en') }}"
                           @class(['inline-flex min-h-12 items-center rounded-lg px-2.5 text-sm font-semibold', 'bg-paper text-primary shadow-sm' => ! $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => $isSpanish])
                           @if (! $isSpanish) aria-current="true" @endif>EN</a>
                    </div>

                    <a href="{{ route('login') }}" class="mp-btn mp-btn-quiet mp-btn-sm">{{ __('Entrar') }}</a>
                    <a href="{{ route('register') }}" class="mp-btn mp-btn-primary mp-btn-sm">{{ __('Crear cuenta') }}</a>
                </nav>
            @endauth
        </div>
    </header>

    <main id="contenido" class="mp-container py-8 lg:py-10">
        {{ $slot }}
    </main>

    <footer class="border-t border-outline-variant/40 pt-8 pb-28 lg:pb-8">
        <div class="mp-container flex flex-col items-center justify-between gap-2 text-sm text-on-surface-variant sm:flex-row">
            <p class="flex items-center gap-2 font-semibold text-on-surface">
                <span class="h-1.5 w-1.5 rounded-full bg-accent"></span>
                © {{ date('Y') }} My Pass
            </p>
            <div class="flex flex-wrap items-center justify-center gap-3">
                <a href="{{ route('legal.privacy') }}" class="hover:text-on-surface hover:underline">{{ __('Privacidad') }}</a>
                <a href="{{ route('legal.terms') }}" class="hover:text-on-surface hover:underline">{{ __('Términos') }}</a>
                <span>{{ __('Sin anuncios · Tus llaves se quedan contigo') }}</span>
            </div>
        </div>
    </footer>

    @auth
        @unless (request()->routeIs('onboarding'))
            <x-bottom-nav :active="$activeNav" />
        @endunless
    @endauth
</body>
</html>
