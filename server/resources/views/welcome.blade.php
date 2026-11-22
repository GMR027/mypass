@php($isSpanish = str_starts_with(app()->getLocale(), 'es'))

<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <title>{{ __('mypass — tu bóveda personal') }}</title>
    <link rel="icon" type="image/png" sizes="32x32" href="{{ asset('images/favicon-32.png') }}">
    <link rel="apple-touch-icon" href="{{ asset('images/favicon-180.png') }}">
    <x-translations />
    @vite(['resources/css/app.css', 'resources/js/app.js'])
</head>
<body class="min-h-screen bg-surface text-on-surface">
    <header class="sticky top-0 z-40 border-b border-outline-variant/50 bg-surface/95">
        <div class="mp-container flex min-h-20 flex-wrap items-center justify-between gap-3 py-3">
            <x-brand :href="url('/')" />

            <nav class="flex flex-wrap items-center gap-2" aria-label="{{ __('Navegación principal') }}">
                <div class="flex items-center gap-0.5 rounded-xl bg-surface-container-low p-1" aria-label="{{ __('Idioma') }}">
                    <a href="{{ route('language.switch', 'es-MX') }}"
                       @class(['inline-flex min-h-12 items-center rounded-lg px-2.5 text-sm font-semibold', 'bg-white text-primary shadow-sm' => $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => ! $isSpanish])
                       @if ($isSpanish) aria-current="true" @endif>ES</a>
                    <a href="{{ route('language.switch', 'en') }}"
                       @class(['inline-flex min-h-12 items-center rounded-lg px-2.5 text-sm font-semibold', 'bg-white text-primary shadow-sm' => ! $isSpanish, 'text-on-surface-variant hover:bg-surface-container' => $isSpanish])
                       @if (! $isSpanish) aria-current="true" @endif>EN</a>
                </div>

                <a href="{{ route('login') }}" class="mp-btn mp-btn-quiet mp-btn-sm">{{ __('Entrar') }}</a>
                <a href="{{ route('register') }}" class="mp-btn mp-btn-primary mp-btn-sm">{{ __('Crear cuenta') }}</a>
            </nav>
        </div>
    </header>

    <main>
        <section class="mp-container grid items-center gap-10 py-12 lg:grid-cols-2 lg:gap-14 lg:py-16">
            <div>
                <p class="mp-eyebrow">{{ __('MYPASS') }}</p>
                <h1 class="mp-title-xl mt-4 max-w-xl">
                    {{ __('Tus documentos importantes, cifrados y siempre contigo.') }}
                </h1>
                <p class="mp-body mt-5 max-w-xl">
                    {{ __('Guarda boletos, identificaciones, tarjetas y pases en una bóveda personal. Todo se cifra en tu dispositivo antes de salir; el servidor nunca ve tus documentos.') }}
                </p>

                <div class="mt-7 flex flex-col gap-3 sm:flex-row">
                    <a href="{{ route('register') }}" class="mp-btn mp-btn-primary">
                        <x-icon name="lock" class="text-[20px]" />
                        {{ __('Crear mi bóveda') }}
                    </a>
                    <a href="{{ route('login') }}" class="mp-btn mp-btn-secondary">
                        {{ __('Ya tengo cuenta') }}
                    </a>
                </div>

                <ul class="mt-7 flex flex-wrap gap-x-6 gap-y-3 text-[15px] font-semibold text-on-surface-variant">
                    <li class="flex items-center gap-2">
                        <x-icon name="verified_user" class="text-[20px] text-primary" />
                        {{ __('Cifrado local sin custodia') }}
                    </li>
                    <li class="flex items-center gap-2">
                        <x-icon name="no_accounts" class="text-[20px] text-primary" />
                        {{ __('Sin rastreo ni publicidad') }}
                    </li>
                </ul>
            </div>

            <div class="relative mx-auto w-full max-w-[460px]">
                <div class="absolute inset-6 -z-10 rounded-[2.5rem] bg-primary-fixed/60"></div>
                <div class="mp-card overflow-hidden p-5 sm:p-7">
                    <div class="rounded-2xl bg-surface-container-low p-4 sm:p-6">
                        <img src="{{ asset('images/mypass-illustration.png') }}"
                             alt="{{ __('Ilustración de documentos y tarjetas resguardados en una bóveda segura') }}"
                             class="mx-auto w-full max-w-[340px] object-contain"
                             width="1024" height="1024">
                    </div>
                    <div class="mt-4 flex items-center gap-3 rounded-xl border border-outline-variant/60 bg-white p-4 shadow-sm">
                        <span class="flex h-11 w-11 items-center justify-center rounded-full bg-primary-fixed text-primary">
                            <x-icon name="key" class="text-[22px]" />
                        </span>
                        <div>
                            <p class="font-semibold text-on-surface">{{ __('Claves privadas') }}</p>
                            <p class="text-sm text-on-surface-variant">{{ __('Solo en tu equipo') }}</p>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <section class="border-y border-outline-variant/40 bg-white py-14 lg:py-16">
            <div class="mp-container">
                <div class="max-w-2xl">
                    <p class="mp-eyebrow">{{ __('Tranquilidad por diseño') }}</p>
                    <h2 class="mp-title-lg mt-3">{{ __('Seguridad tangible, sin letra pequeña.') }}</h2>
                </div>

                <div class="mt-8 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                    <article class="mp-card mp-card-pad flex flex-col">
                        <span class="mp-icon-tile"><x-icon name="lock" /></span>
                        <h3 class="mp-title-sm mt-4">{{ __('Cifrado de punta a punta') }}</h3>
                        <p class="mp-body-sm mt-2 flex-1">{{ __('Tus archivos viajan y se guardan cifrados. Solo tu PIN y tu llave maestra pueden abrirlos.') }}</p>
                        <p class="mt-5 flex items-center gap-2 text-sm font-bold text-primary">
                            <x-icon name="check_circle" class="text-[19px]" filled />
                            {{ __('Privacidad total') }}
                        </p>
                    </article>

                    <article class="mp-card mp-card-pad flex flex-col">
                        <span class="mp-icon-tile"><x-icon name="devices" /></span>
                        <h3 class="mp-title-sm mt-4">{{ __('Computadora y Android') }}</h3>
                        <p class="mp-body-sm mt-2 flex-1">{{ __('Configura todo desde la computadora y conecta tu tablet o celular con un código de un solo uso.') }}</p>
                        <p class="mt-5 flex items-center gap-2 text-sm font-bold text-primary">
                            <x-icon name="sync_alt" class="text-[19px]" />
                            {{ __('Sincronización fácil') }}
                        </p>
                    </article>

                    <article class="mp-card mp-card-pad flex flex-col">
                        <span class="mp-icon-tile"><x-icon name="key" /></span>
                        <h3 class="mp-title-sm mt-4">{{ __('Llave de recuperación') }}</h3>
                        <p class="mp-body-sm mt-2 flex-1">{{ __('Guarda tu kit de emergencia en la computadora. Nunca se muestra en el celular.') }}</p>
                        <p class="mt-5 flex items-center gap-2 text-sm font-bold text-primary">
                            <x-icon name="download" class="text-[19px]" />
                            {{ __('Kit descargable') }}
                        </p>
                    </article>

                    <article class="mp-card mp-card-pad flex flex-col">
                        <span class="mp-icon-tile"><x-icon name="group" /></span>
                        <h3 class="mp-title-sm mt-4">{{ __('Compartir en familia') }}</h3>
                        <p class="mp-body-sm mt-2 flex-1">{{ __('Envía una copia o transfiere un documento a un familiar, con vencimiento y sin reenvíos.') }}</p>
                        <p class="mt-5 flex items-center gap-2 text-sm font-bold text-primary">
                            <x-icon name="timer" class="text-[19px]" />
                            {{ __('Con caducidad') }}
                        </p>
                    </article>
                </div>
            </div>
        </section>

        <section class="mp-container py-14 lg:py-16">
            <div class="rounded-[2rem] bg-surface-container-low p-6 sm:p-10 lg:p-12">
                <div class="grid items-center gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(0,0.85fr)]">
                    <div>
                        <span class="mp-badge mp-badge-primary">
                            <x-icon name="accessibility_new" class="text-[18px]" />
                            {{ __('Diseño accesible para todos') }}
                        </span>
                        <h2 class="mp-title-lg mt-4">{{ __('Pensada para usarse sin complicaciones') }}</h2>
                        <p class="mp-body mt-4 max-w-xl">
                            {{ __('Botones grandes, modo simple y textos claros. También puedes pedir un código por correo o usar una app de autenticación para proteger tu cuenta.') }}
                        </p>
                        <a href="{{ route('register') }}" class="mp-btn mp-btn-primary mt-6">
                            {{ __('Empezar ahora') }}
                            <x-icon name="arrow_forward" class="text-[20px]" />
                        </a>
                    </div>

                    <ul class="space-y-3">
                        <li class="flex items-center gap-3 rounded-xl bg-white p-4 shadow-sm">
                            <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-fixed text-primary">
                                <x-icon name="text_fields" class="text-[22px]" />
                            </span>
                            <span class="font-semibold">{{ __('Tipografía amplia y alto contraste') }}</span>
                        </li>
                        <li class="flex items-center gap-3 rounded-xl bg-white p-4 shadow-sm">
                            <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-fixed text-primary">
                                <x-icon name="touch_app" class="text-[22px]" />
                            </span>
                            <span class="font-semibold">{{ __('Botones táctiles de al menos 48 px') }}</span>
                        </li>
                        <li class="flex items-center gap-3 rounded-xl bg-white p-4 shadow-sm">
                            <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-fixed text-primary">
                                <x-icon name="mark_email_read" class="text-[22px]" />
                            </span>
                            <span class="font-semibold">{{ __('Acceso seguro con código al correo') }}</span>
                        </li>
                    </ul>
                </div>
            </div>
        </section>
    </main>

    <footer class="border-t border-outline-variant/60 py-8">
        <div class="mp-container flex flex-col items-center justify-between gap-2 text-sm text-on-surface-variant sm:flex-row">
            <p>© {{ date('Y') }} mypass</p>
            <p>{{ __('Sin anuncios · Tus llaves se quedan contigo') }}</p>
        </div>
    </footer>
</body>
</html>
