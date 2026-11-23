@props(['title'])

@php
    $isSpanish = str_starts_with(app()->getLocale(), 'es');
    $shellUser = \App\Models\User::find(session('login.id'));
@endphp

<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <title>{{ $title ?? 'My Pass' }} · My Pass</title>
    <link rel="icon" type="image/png" sizes="32x32" href="{{ asset('images/favicon-32.png') }}">
    <link rel="apple-touch-icon" href="{{ asset('images/favicon-180.png') }}">
    <x-translations />
    @vite(['resources/css/app.css', 'resources/js/app.js'])
</head>
<body class="min-h-screen bg-surface text-on-surface {{ $shellUser?->simple_mode ? 'simple-mode' : '' }}">
    <div class="min-h-screen lg:grid lg:grid-cols-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
        <aside class="mp-auth-panel relative hidden overflow-hidden px-10 py-12 text-on-primary lg:flex lg:flex-col lg:justify-between xl:px-14">
            <div class="relative z-10">
                <x-brand :href="url('/')" inverse />

                <div class="mp-reveal mt-12 max-w-md">
                    <span class="mp-hero-pill">
                        <span class="h-1.5 w-1.5 rounded-full bg-accent"></span>
                        {{ __('Tu billetera digital') }}
                    </span>
                    <h2 class="mt-5 text-[38px] font-bold leading-[1.02] tracking-[-0.045em] text-white xl:text-[46px]">
                        {{ __('Tus pases, siempre a la mano.') }}
                    </h2>
                    <p class="mt-5 text-lg leading-relaxed text-primary-fixed-dim">
                        {{ __('Entra a My Pass y abre primero lo que vas a usar: tus pases, boletos y tarjetas. El resto queda protegido bajo tu llave.') }}
                    </p>
                </div>

                <div class="mp-wallet-stage mp-reveal mp-reveal-delay-1 mt-8 max-w-md" aria-hidden="true">
                    <article class="mp-demo-pass mp-demo-pass--flight mp-wallet-float">
                        <div class="flex items-start justify-between gap-3">
                            <div>
                                <p class="mp-demo-pass__eyebrow">{{ __('Pase de abordar') }}</p>
                                <p class="mt-1 text-sm font-semibold text-white/90">Iberia · IB 6401</p>
                            </div>
                            <span class="rounded-full bg-white/15 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.1em]">{{ __('Próximo') }}</span>
                        </div>
                        <div class="mp-demo-pass__route">
                            <span class="mp-demo-pass__airport">MAD</span>
                            <span class="text-white/50">✈</span>
                            <span class="mp-demo-pass__airport">MEX</span>
                        </div>
                        <div class="mp-demo-pass__meta">
                            <div><span>{{ __('Puerta') }}</span><strong>12</strong></div>
                            <div><span>{{ __('Asiento') }}</span><strong>14A</strong></div>
                            <div><span>{{ __('Hora') }}</span><strong>09:40</strong></div>
                        </div>
                        <div class="mp-demo-pass__barcode"></div>
                    </article>

                    <article class="mp-demo-pass mp-demo-pass--event mp-wallet-float-delayed">
                        <div class="flex items-center justify-between gap-3">
                            <div>
                                <p class="mp-demo-pass__eyebrow">{{ __('Evento') }}</p>
                                <p class="mt-1 text-lg font-bold">Noche de jazz</p>
                            </div>
                            <span class="material-symbols-outlined text-[30px] text-white/60">confirmation_number</span>
                        </div>
                        <div class="mt-4 flex items-center justify-between text-sm text-white/70">
                            <span>Teatro Real</span>
                            <span>21:00</span>
                        </div>
                    </article>

                    <article class="mp-demo-pass mp-demo-pass--member">
                        <div class="flex items-center justify-between gap-3">
                            <div>
                                <p class="mp-demo-pass__eyebrow">{{ __('Membresía') }}</p>
                                <p class="mt-1 text-lg font-bold">Círculo My Pass</p>
                            </div>
                            <span class="material-symbols-outlined text-[30px] text-white/60">loyalty</span>
                        </div>
                    </article>
                </div>
            </div>

            <div class="relative z-10 mt-8 grid gap-3 sm:grid-cols-2">
                <div class="flex items-start gap-3 rounded-2xl border border-white/10 bg-white/5 p-3.5 backdrop-blur">
                    <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white/10">
                        <x-icon name="encrypted" class="text-[19px]" />
                    </span>
                    <div>
                        <p class="text-sm font-semibold text-white">{{ __('Cifrado real') }}</p>
                        <p class="text-xs leading-relaxed text-primary-fixed-dim">{{ __('Solo tu llave abre tus pases.') }}</p>
                    </div>
                </div>
                <div class="flex items-start gap-3 rounded-2xl border border-white/10 bg-white/5 p-3.5 backdrop-blur">
                    <span class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white/10">
                        <x-icon name="bolt" class="text-[19px]" />
                    </span>
                    <div>
                        <p class="text-sm font-semibold text-white">{{ __('Acceso directo') }}</p>
                        <p class="text-xs leading-relaxed text-primary-fixed-dim">{{ __('Desbloquea y ve a tus pases.') }}</p>
                    </div>
                </div>
            </div>
        </aside>

        <main class="relative flex min-h-screen flex-col">
            <header class="flex items-center justify-between gap-3 px-5 py-4 lg:hidden">
                <x-brand :href="url('/')" />
                <div class="flex items-center gap-1.5">
                    <a href="{{ route('language.switch', 'es-MX') }}"
                       @class(['inline-flex min-h-12 items-center rounded-lg px-2.5 text-sm font-semibold', 'bg-primary text-on-primary' => $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => ! $isSpanish])
                       @if ($isSpanish) aria-current="true" @endif>ES</a>
                    <a href="{{ route('language.switch', 'en') }}"
                       @class(['inline-flex min-h-12 items-center rounded-lg px-2.5 text-sm font-semibold', 'bg-primary text-on-primary' => ! $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => $isSpanish])
                       @if (! $isSpanish) aria-current="true" @endif>EN</a>
                </div>
            </header>

            <div class="hidden justify-end gap-2 px-8 pt-6 lg:flex">
                <a href="{{ route('language.switch', 'es-MX') }}"
                   @class(['inline-flex min-h-12 items-center rounded-lg px-3 text-sm font-semibold', 'bg-primary text-on-primary' => $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => ! $isSpanish])>ES</a>
                <a href="{{ route('language.switch', 'en') }}"
                   @class(['inline-flex min-h-12 items-center rounded-lg px-3 text-sm font-semibold', 'bg-primary text-on-primary' => ! $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => $isSpanish])>EN</a>
            </div>

            <div class="flex flex-1 items-center justify-center px-5 py-8 sm:px-8">
                <div class="mp-auth-card w-full max-w-[520px]">
                    {{ $slot }}
                </div>
            </div>

            <footer class="px-5 py-6 text-center text-sm text-on-surface-variant">
                <a href="{{ url('/') }}" class="inline-flex items-center gap-1.5 font-semibold text-primary hover:underline">
                    <x-icon name="arrow_back" class="text-[18px]" />
                    {{ __('Volver al inicio') }}
                </a>
                <div class="mt-3 flex flex-wrap items-center justify-center gap-3">
                    <a href="{{ route('app.download', ['v' => '3']) }}" class="hover:text-on-surface hover:underline" download>{{ __('Descargar Android') }}</a>
                    <a href="{{ route('legal.privacy') }}" class="hover:text-on-surface hover:underline">{{ __('Privacidad') }}</a>
                    <a href="{{ route('legal.terms') }}" class="hover:text-on-surface hover:underline">{{ __('Términos') }}</a>
                </div>
                <p class="mt-3">© {{ date('Y') }} My Pass · {{ __('Sin anuncios · Tus llaves se quedan contigo') }}</p>
            </footer>
        </main>
    </div>
</body>
</html>
