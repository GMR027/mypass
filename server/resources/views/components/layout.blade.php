<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <title>{{ $title ?? 'mypass' }} · mypass</title>
    <link rel="icon" type="image/png" sizes="32x32" href="{{ asset('images/favicon-32.png') }}">
    <link rel="apple-touch-icon" href="{{ asset('images/favicon-180.png') }}">
    <x-translations />
    @vite(['resources/css/app.css', 'resources/js/app.js'])
</head>
<body class="min-h-screen bg-surface text-on-surface {{ auth()->user()?->simple_mode ? 'simple-mode' : '' }}">
    <a href="#contenido" class="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[70] focus:rounded-xl focus:bg-primary focus:px-4 focus:py-3 focus:text-on-primary">
        {{ __('Saltar al contenido') }}
    </a>

    @php
        $isSpanish = str_starts_with(app()->getLocale(), 'es');
        $user = auth()->user();
    @endphp

    <header class="sticky top-0 z-40 border-b border-outline-variant/60 bg-surface/95">
        <div class="mp-container flex min-h-20 flex-wrap items-center justify-between gap-3 py-3">
            <x-brand :href="$user ? route('dashboard') : route('login')" />

            <nav class="flex flex-wrap items-center gap-1.5" aria-label="{{ __('Navegación principal') }}">
                <div class="flex items-center gap-0.5 rounded-xl bg-surface-container-low p-1" aria-label="{{ __('Idioma') }}">
                    <a href="{{ route('language.switch', 'es-MX') }}"
                       @class(['inline-flex min-h-12 items-center rounded-lg px-2.5 text-sm font-semibold', 'bg-white text-primary shadow-sm' => $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => ! $isSpanish])
                       @if ($isSpanish) aria-current="true" @endif>ES</a>
                    <a href="{{ route('language.switch', 'en') }}"
                       @class(['inline-flex min-h-12 items-center rounded-lg px-2.5 text-sm font-semibold', 'bg-white text-primary shadow-sm' => ! $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => $isSpanish])
                       @if (! $isSpanish) aria-current="true" @endif>EN</a>
                </div>

                @auth
                    <a href="{{ route('settings.security') }}"
                       aria-label="{{ __('Seguridad') }}"
                       class="inline-flex min-h-12 items-center gap-1.5 rounded-xl px-3 text-[15px] font-semibold text-on-surface-variant transition-colors hover:bg-surface-container hover:text-on-surface">
                        <x-icon name="shield" class="text-[20px]" />
                        <span class="hidden sm:inline">{{ __('Seguridad') }}</span>
                    </a>

                    <form method="POST" action="{{ route('logout') }}">
                        @csrf
                        <button type="submit"
                                aria-label="{{ __('Salir') }}"
                                class="inline-flex min-h-12 items-center gap-1.5 rounded-xl px-3 text-[15px] font-semibold text-on-surface-variant transition-colors hover:bg-surface-container hover:text-on-surface">
                            <x-icon name="logout" class="text-[20px]" />
                            <span class="hidden sm:inline">{{ __('Salir') }}</span>
                        </button>
                    </form>

                    <span class="ml-1 flex h-10 w-10 items-center justify-center rounded-full bg-primary text-base font-bold uppercase text-on-primary" aria-hidden="true">
                        {{ mb_substr($user->name ?: $user->email, 0, 1) }}
                    </span>
                @else
                    <a href="{{ route('login') }}" class="mp-btn mp-btn-quiet mp-btn-sm">{{ __('Entrar') }}</a>
                    <a href="{{ route('register') }}" class="mp-btn mp-btn-primary mp-btn-sm">{{ __('Crear cuenta') }}</a>
                @endauth
            </nav>
        </div>
    </header>

    <main id="contenido" class="mp-container py-8 lg:py-10">
        {{ $slot }}
    </main>

    <footer class="border-t border-outline-variant/60 py-8">
        <div class="mp-container flex flex-col items-center justify-between gap-2 text-sm text-on-surface-variant sm:flex-row">
            <p>© {{ date('Y') }} mypass</p>
            <p>{{ __('Sin anuncios · Tus llaves se quedan contigo') }}</p>
        </div>
    </footer>
</body>
</html>
