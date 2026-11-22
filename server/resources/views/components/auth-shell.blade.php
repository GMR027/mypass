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
    <title>{{ $title ?? 'mypass' }} · mypass</title>
    <link rel="icon" type="image/png" sizes="32x32" href="{{ asset('images/favicon-32.png') }}">
    <link rel="apple-touch-icon" href="{{ asset('images/favicon-180.png') }}">
    <x-translations />
    @vite(['resources/css/app.css', 'resources/js/app.js'])
</head>
<body class="min-h-screen bg-surface text-on-surface {{ $shellUser?->simple_mode ? 'simple-mode' : '' }}">
    <div class="min-h-screen lg:grid lg:grid-cols-[minmax(0,0.92fr)_minmax(0,1.08fr)]">
        <aside class="relative hidden overflow-hidden bg-primary px-10 py-12 text-on-primary lg:flex lg:flex-col lg:justify-between xl:px-14">
            <div class="pointer-events-none absolute -right-24 -top-24 h-80 w-80 rounded-full border border-white/10"></div>
            <div class="pointer-events-none absolute -bottom-32 -left-20 h-96 w-96 rounded-full bg-primary-container"></div>

            <div class="relative">
                <x-brand :href="url('/')" inverse />

                <div class="mt-14 max-w-md">
                    <p class="text-sm font-bold uppercase tracking-[0.16em] text-primary-fixed">{{ __('Bóveda de seguridad') }}</p>
                    <h2 class="mt-4 text-[34px] font-bold leading-tight text-white xl:text-[40px]">
                        {{ __('Tu bóveda personal') }}
                    </h2>
                    <p class="mt-5 text-lg leading-relaxed text-primary-fixed-dim">
                        {{ __('Un espacio digno y seguro, diseñado para proteger tus pases y documentos esenciales con total tranquilidad.') }}
                    </p>
                </div>

                <ul class="mt-9 space-y-5 text-[17px] text-white/90">
                    <li class="flex items-start gap-4">
                        <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white/10">
                            <x-icon name="lock" class="text-[22px]" />
                        </span>
                        <span>
                            <span class="block font-semibold">{{ __('Cifrado de punta a punta') }}</span>
                            <span class="block text-primary-fixed-dim">{{ __('Solo tu llave puede abrir tus documentos.') }}</span>
                        </span>
                    </li>
                    <li class="flex items-start gap-4">
                        <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white/10">
                            <x-icon name="devices" class="text-[22px]" />
                        </span>
                        <span>
                            <span class="block font-semibold">{{ __('Disponible en web y Android') }}</span>
                            <span class="block text-primary-fixed-dim">{{ __('Conecta tu celular con un código de un solo uso.') }}</span>
                        </span>
                    </li>
                    <li class="flex items-start gap-4">
                        <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white/10">
                            <x-icon name="visibility_off" class="text-[22px]" />
                        </span>
                        <span>
                            <span class="block font-semibold">{{ __('Sin custodia ni rastreo') }}</span>
                            <span class="block text-primary-fixed-dim">{{ __('Tus llaves se quedan contigo.') }}</span>
                        </span>
                    </li>
                </ul>
            </div>

            <div class="relative mt-10 flex items-center gap-4 rounded-2xl border border-white/10 bg-white/5 p-4">
                <img src="{{ asset('images/mypass-illustration.png') }}" alt="" class="h-16 w-16 shrink-0 object-contain" loading="lazy">
                <p class="text-sm leading-relaxed text-primary-fixed-dim">
                    {{ __('Custodia privada · Llave en tu dispositivo') }}
                </p>
            </div>
        </aside>

        <main class="flex min-h-screen flex-col">
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
                <div class="w-full max-w-[500px]">
                    {{ $slot }}
                </div>
            </div>

            <footer class="px-5 py-6 text-center text-sm text-on-surface-variant">
                <a href="{{ url('/') }}" class="inline-flex items-center gap-1.5 font-semibold text-primary hover:underline">
                    <x-icon name="arrow_back" class="text-[18px]" />
                    {{ __('Volver al inicio') }}
                </a>
                <p class="mt-3">© {{ date('Y') }} mypass · {{ __('Sin anuncios · Tus llaves se quedan contigo') }}</p>
            </footer>
        </main>
    </div>
</body>
</html>
