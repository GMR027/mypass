@php($isSpanish = str_starts_with(app()->getLocale(), 'es'))

<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <title>{{ __('My Pass — tu bóveda personal') }}</title>
    <meta name="description" content="{{ __('Tus pases, boletos y documentos importantes, cifrados y siempre contigo.') }}">
    <link rel="icon" type="image/png" sizes="32x32" href="{{ asset('images/favicon-32.png') }}">
    <link rel="apple-touch-icon" href="{{ asset('images/favicon-180.png') }}">
    <x-translations />
    @vite(['resources/css/app.css', 'resources/js/app.js'])
</head>
<body class="min-h-screen bg-surface text-on-surface">
    <header class="sticky top-0 z-40 border-b border-white/70 bg-surface/80 backdrop-blur-xl">
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
        <section class="mp-container grid items-center gap-10 py-12 lg:grid-cols-[minmax(0,1.02fr)_minmax(0,0.98fr)] lg:gap-14 lg:py-20">
            <div class="mp-reveal">
                <span class="mp-hero-pill border-primary/10 bg-white text-primary shadow-sm">
                    <span class="h-1.5 w-1.5 rounded-full bg-accent"></span>
                    {{ __('Tu billetera digital') }}
                </span>
                <h1 class="mp-title-xl mt-5 max-w-2xl">
                    {{ __('Tus pases,') }}<br>
                    <span class="text-primary">{{ __('siempre a la mano.') }}</span>
                </h1>
                <p class="mp-body mt-6 max-w-xl">
                    {{ __('Guarda boletos, identificaciones, tarjetas y pases en una bóveda personal. Todo se cifra en tu dispositivo antes de salir; el servidor nunca ve tus documentos.') }}
                </p>

                <div class="mt-8 flex flex-col gap-3 sm:flex-row">
                    <a href="{{ route('register') }}" class="mp-btn mp-btn-primary">
                        <x-icon name="lock" class="text-[20px]" />
                        {{ __('Crear mi bóveda') }}
                    </a>
                    <a href="{{ route('login') }}" class="mp-btn mp-btn-secondary">
                        {{ __('Ya tengo cuenta') }}
                        <x-icon name="arrow_forward" class="text-[20px]" />
                    </a>
                </div>

                <ul class="mt-8 flex flex-wrap gap-x-6 gap-y-3 text-[15px] font-semibold text-on-surface-variant">
                    <li class="flex items-center gap-2">
                        <x-icon name="verified_user" class="text-[20px] text-primary" filled />
                        {{ __('Cifrado local sin custodia') }}
                    </li>
                    <li class="flex items-center gap-2">
                        <x-icon name="no_accounts" class="text-[20px] text-primary" />
                        {{ __('Sin rastreo ni publicidad') }}
                    </li>
                    <li class="flex items-center gap-2">
                        <x-icon name="bolt" class="text-[20px] text-accent-deep" />
                        {{ __('Acceso directo a tus pases') }}
                    </li>
                </ul>
            </div>

            <div class="mp-reveal mp-reveal-delay-1 relative">
                <div class="pointer-events-none absolute inset-8 -z-10 rounded-[3rem] bg-primary-fixed/50 blur-2xl"></div>
                <div class="mp-wallet-stage mx-auto max-w-[460px]" aria-hidden="true">
                    <article class="mp-demo-pass mp-demo-pass--flight mp-wallet-float">
                        <div class="flex items-start justify-between gap-3">
                            <div>
                                <p class="mp-demo-pass__eyebrow">{{ __('Pase de abordar') }}</p>
                                <p class="mt-1 text-sm font-semibold text-white/90">Iberia · IB 6401</p>
                            </div>
                            <span class="rounded-full bg-white/15 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.1em]">{{ __('Próximo') }}</span>
                        </div>
                        <div class="mp-demo-pass__route">
                            <div>
                                <span class="mp-demo-pass__airport">MAD</span>
                                <p class="mt-1 text-xs text-white/55">Madrid · 07:10</p>
                            </div>
                            <span class="text-2xl text-white/45">✈</span>
                            <div class="text-right">
                                <span class="mp-demo-pass__airport">MEX</span>
                                <p class="mt-1 text-xs text-white/55">Ciudad de México</p>
                            </div>
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
                                <p class="mt-1 text-xl font-bold">Noche de jazz</p>
                                <p class="mt-1 text-sm text-white/65">Teatro Real · 21:00</p>
                            </div>
                            <span class="material-symbols-outlined text-[34px] text-white/50">confirmation_number</span>
                        </div>
                    </article>

                    <article class="mp-demo-pass mp-demo-pass--member">
                        <div class="flex items-center justify-between gap-3">
                            <div>
                                <p class="mp-demo-pass__eyebrow">{{ __('Membresía') }}</p>
                                <p class="mt-1 text-xl font-bold">Círculo My Pass</p>
                            </div>
                            <span class="material-symbols-outlined text-[34px] text-white/50">loyalty</span>
                        </div>
                    </article>
                </div>

                <div class="mp-card relative z-10 mx-auto -mt-5 flex max-w-sm items-center gap-3 p-3 shadow-lift">
                    <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-fixed text-primary">
                        <x-icon name="encrypted" class="text-[22px]" filled />
                    </span>
                    <div>
                        <p class="text-sm font-bold text-on-surface">{{ __('Cifrado local') }}</p>
                        <p class="text-xs text-on-surface-variant">{{ __('Solo tu llave abre tus pases') }}</p>
                    </div>
                </div>
            </div>
        </section>

        <section class="border-y border-white/70 bg-white/70 py-5">
            <div class="mp-container grid gap-4 text-center sm:grid-cols-3">
                <div class="flex items-center justify-center gap-3">
                    <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-fixed text-primary"><x-icon name="wallet" class="text-[21px]" /></span>
                    <p class="text-left text-sm font-semibold text-on-surface">{{ __('Pases y boletos primero') }}<span class="block text-xs font-normal text-on-surface-variant">{{ __('Abre lo que vas a usar') }}</span></p>
                </div>
                <div class="flex items-center justify-center gap-3">
                    <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-fixed text-primary"><x-icon name="devices" class="text-[21px]" /></span>
                    <p class="text-left text-sm font-semibold text-on-surface">{{ __('Web y Android') }}<span class="block text-xs font-normal text-on-surface-variant">{{ __('Tu bóveda en tus dispositivos') }}</span></p>
                </div>
                <div class="flex items-center justify-center gap-3">
                    <span class="flex h-10 w-10 items-center justify-center rounded-xl bg-accent-soft text-accent-deep"><x-icon name="key" class="text-[21px]" /></span>
                    <p class="text-left text-sm font-semibold text-on-surface">{{ __('Llave de recuperación') }}<span class="block text-xs font-normal text-on-surface-variant">{{ __('Sin custodia del servidor') }}</span></p>
                </div>
            </div>
        </section>

        <section class="mp-container py-14 lg:py-20">
            <div class="grid gap-10 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)] lg:items-end">
                <div>
                    <p class="mp-eyebrow">{{ __('Así de simple') }}</p>
                    <h2 class="mp-title-lg mt-3">{{ __('De la cuenta a tu pase, en tres pasos.') }}</h2>
                    <p class="mp-body mt-4 max-w-lg">{{ __('Configura tu bóveda una vez y después entra directo a tu billetera. Tus pases recientes y próximos aparecen antes que cualquier otra cosa.') }}</p>
                </div>
                <div class="grid gap-4 sm:grid-cols-3">
                    @foreach ([
                        ['lock', __('Crea tu cuenta'), __('Tu correo y una contraseña segura.')],
                        ['vpn_key', __('Configura tu bóveda'), __('Tu llave maestra y tu kit de recuperación.')],
                        ['wallet', __('Abre tus pases'), __('Desbloquea y presenta tu código al instante.')],
                    ] as $index => [$icon, $title, $copy])
                        <article class="mp-card mp-card-pad relative overflow-hidden">
                            <span class="absolute right-4 top-4 text-[42px] font-bold leading-none text-primary-fixed/70">{{ $index + 1 }}</span>
                            <span class="mp-icon-tile"><x-icon :name="$icon" class="text-[24px]" /></span>
                            <h3 class="mp-title-sm mt-4">{{ $title }}</h3>
                            <p class="mp-body-sm mt-2">{{ $copy }}</p>
                        </article>
                    @endforeach
                </div>
            </div>
        </section>

        <section class="border-y border-white/70 bg-white py-14 lg:py-16">
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

        <section class="mp-container py-14 lg:py-20">
            <div class="mp-hero-panel p-7 sm:p-10 lg:p-14">
                <div class="mp-hero-copy grid items-center gap-10 lg:grid-cols-[minmax(0,1fr)_minmax(0,0.8fr)]">
                    <div>
                        <span class="mp-hero-pill">
                            <x-icon name="accessibility_new" class="text-[18px]" />
                            {{ __('Diseño accesible para todos') }}
                        </span>
                        <h2 class="mt-5 text-[clamp(2rem,3.6vw,3.2rem)] font-bold leading-[1.02] tracking-[-0.045em] text-white">
                            {{ __('Pensada para abrirse y usarse sin complicaciones.') }}
                        </h2>
                        <p class="mt-4 max-w-xl text-lg leading-relaxed text-primary-fixed-dim">
                            {{ __('Botones grandes, modo simple y textos claros. Tus pases primero, con un código listo para presentar y la tranquilidad de que todo está cifrado.') }}
                        </p>
                        <div class="mt-7 flex flex-col gap-3 sm:flex-row">
                            <a href="{{ route('register') }}" class="mp-btn mp-btn-primary">
                                {{ __('Empezar ahora') }}
                                <x-icon name="arrow_forward" class="text-[20px]" />
                            </a>
                            <a href="{{ route('login') }}" class="mp-btn mp-btn-secondary border-white/25 bg-white/10 text-white hover:bg-white/15">
                                {{ __('Ya tengo cuenta') }}
                            </a>
                        </div>
                    </div>

                    <ul class="space-y-3">
                        <li class="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/10 p-4 backdrop-blur">
                            <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-white/10 text-white">
                                <x-icon name="text_fields" class="text-[22px]" />
                            </span>
                            <span class="font-semibold text-white">{{ __('Tipografía amplia y alto contraste') }}</span>
                        </li>
                        <li class="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/10 p-4 backdrop-blur">
                            <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-white/10 text-white">
                                <x-icon name="touch_app" class="text-[22px]" />
                            </span>
                            <span class="font-semibold text-white">{{ __('Botones táctiles de al menos 48 px') }}</span>
                        </li>
                        <li class="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/10 p-4 backdrop-blur">
                            <span class="flex h-11 w-11 items-center justify-center rounded-xl bg-white/10 text-white">
                                <x-icon name="mark_email_read" class="text-[22px]" />
                            </span>
                            <span class="font-semibold text-white">{{ __('Acceso seguro con código al correo') }}</span>
                        </li>
                    </ul>
                </div>
            </div>
        </section>
    </main>

    <footer class="border-t border-outline-variant/40 py-8">
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
</body>
</html>
