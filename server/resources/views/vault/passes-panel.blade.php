<section id="panel-passes" role="tabpanel" aria-labelledby="tab-btn-passes" hidden>
    <div id="passes-app">
        <div class="mp-card mb-5 flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex items-center gap-3">
                <span class="relative flex h-3 w-3 shrink-0">
                    <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-40"></span>
                    <span class="relative inline-flex h-3 w-3 rounded-full bg-primary"></span>
                </span>
                <div>
                    <p class="text-[15px] font-bold text-on-surface">{{ __('Bóveda abierta · tus pases están listos') }}</p>
                    <p class="text-sm text-on-surface-variant">{{ __('Los datos del pase se cifran en este navegador antes de guardarse.') }}</p>
                </div>
            </div>
            <span class="mp-badge mp-badge-primary shrink-0">
                <x-icon name="encrypted" class="text-[17px]" filled />
                {{ __('Cifrado local') }}
            </span>
        </div>

        <div class="mb-5 flex flex-col justify-between gap-4 xl:flex-row xl:items-end">
            <div class="max-w-2xl">
                <div class="flex flex-wrap items-center gap-2">
                    <span class="mp-eyebrow">{{ __('Billetera digital') }}</span>
                    <span class="text-on-surface-variant">·</span>
                    <span id="pass-active-count" class="text-sm font-semibold text-on-surface-variant">{{ __('0 pases activos') }}</span>
                </div>
                <h2 class="mp-title-xl mt-2">{{ __('Mis Pases y Boletos') }}</h2>
                <p class="mp-body mt-2">{{ __('Acceso rápido, códigos reales y presentación a pantalla completa. Todo se queda en tu bóveda.') }}</p>
            </div>

            <div class="flex flex-col gap-2 sm:flex-row">
                <button id="pass-quick-present" type="button" class="mp-btn mp-btn-primary min-h-14 whitespace-nowrap">
                    <x-icon name="qr_code_2" class="text-[23px]" />
                    {{ __('Presentación rápida') }}
                    <span class="hidden rounded-md bg-white/15 px-2 py-0.5 font-mono text-[11px] xl:inline">{{ __('Espacio') }}</span>
                </button>

                <div class="relative">
                    <button id="pass-add-toggle" type="button" class="mp-btn mp-btn-secondary min-h-14 w-full whitespace-nowrap sm:w-auto" aria-expanded="false" aria-controls="pass-add-menu">
                        <x-icon name="add_circle" class="text-[22px]" />
                        {{ __('Agregar pase') }}
                        <x-icon name="expand_more" class="text-[18px]" />
                    </button>
                    <div id="pass-add-menu" class="absolute right-0 z-30 mt-2 w-72 rounded-2xl border border-outline-variant/60 bg-white p-2 shadow-dialog" hidden>
                        @php
                            $newPassOptions = [
                                'flight' => ['icon' => 'flight_takeoff', 'label' => __('Vuelo / embarque')],
                                'event' => ['icon' => 'confirmation_number', 'label' => __('Evento / concierto')],
                                'transit' => ['icon' => 'directions_subway', 'label' => __('Transporte')],
                                'id' => ['icon' => 'badge', 'label' => __('Identificación')],
                                'membership' => ['icon' => 'loyalty', 'label' => __('Membresía')],
                            ];
                        @endphp
                        @foreach ($newPassOptions as $category => $option)
                            <button type="button" data-pass-new="{{ $category }}"
                                    class="flex min-h-12 w-full items-center gap-3 rounded-xl px-3 text-left text-[16px] font-semibold text-on-surface transition-colors hover:bg-surface-container">
                                <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-fixed text-primary">
                                    <x-icon :name="$option['icon']" class="text-[20px]" />
                                </span>
                                {{ $option['label'] }}
                            </button>
                        @endforeach
                        <div class="my-1 border-t border-outline-variant/50"></div>
                        <button type="button" id="pass-open-generator" class="flex min-h-12 w-full items-center gap-3 rounded-xl px-3 text-left text-[16px] font-semibold text-primary transition-colors hover:bg-primary-fixed/40">
                            <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-on-primary">
                                <x-icon name="document_scanner" class="text-[20px]" />
                            </span>
                            {{ __('Importar desde un boleto') }}
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="mb-5 flex flex-col justify-between gap-3 border-b border-outline-variant/50 pb-3 sm:flex-row sm:items-center">
            <div id="pass-filters" class="flex gap-2 overflow-x-auto pb-1 sm:pb-0" aria-label="{{ __('Filtrar pases') }}">
                @php
                    $passFilters = [
                        'all' => __('Todos'),
                        'flight' => __('Vuelos'),
                        'event' => __('Eventos'),
                        'transit' => __('Transporte'),
                        'id' => __('Identidad'),
                        'membership' => __('Membresías'),
                    ];
                @endphp
                @foreach ($passFilters as $filter => $label)
                    <button type="button" data-pass-filter="{{ $filter }}"
                            aria-pressed="{{ $filter === 'all' ? 'true' : 'false' }}"
                            class="mp-filter min-h-10 shrink-0 whitespace-nowrap px-4 py-1.5 text-[14px]">
                        {{ $label }}
                        <span data-pass-count="{{ $filter }}" class="text-[12px] font-bold opacity-80">0</span>
                    </button>
                @endforeach
            </div>

            <div class="flex shrink-0 items-center gap-1 self-end rounded-xl bg-surface-container p-1 sm:self-auto" aria-label="{{ __('Vista de pases') }}">
                <button id="pass-view-stack" type="button" aria-pressed="true"
                        class="inline-flex min-h-10 items-center gap-1.5 rounded-lg bg-white px-3 text-[14px] font-semibold text-on-surface shadow-sm">
                    <x-icon name="layers" class="text-[18px]" />
                    {{ __('Cartera') }}
                </button>
                <button id="pass-view-grid" type="button" aria-pressed="false"
                        class="inline-flex min-h-10 items-center gap-1.5 rounded-lg px-3 text-[14px] font-semibold text-on-surface-variant hover:text-on-surface">
                    <x-icon name="grid_view" class="text-[18px]" />
                    {{ __('Cuadrícula') }}
                </button>
            </div>
        </div>

        <div id="passes-empty" class="mp-empty" hidden>
            <span class="flex h-16 w-16 items-center justify-center rounded-full bg-primary-fixed text-primary">
                <x-icon name="wallet" class="text-[32px]" />
            </span>
            <h3 class="mp-title-sm mt-4">{{ __('Todavía no tienes pases') }}</h3>
            <p class="mp-hint mt-2 max-w-md">{{ __('Importa un boleto en PDF o imagen, revisa los datos detectados y guárdalo cifrado en tu bóveda.') }}</p>
            <button type="button" id="passes-empty-create" class="mp-btn mp-btn-primary mt-5">
                <x-icon name="add" class="text-[21px]" />
                {{ __('Crear mi primer pase') }}
            </button>
        </div>

        <div id="passes-workspace" class="grid gap-5 lg:grid-cols-12 lg:items-start">
            <div class="min-w-0 lg:col-span-7">
                <div class="mb-3 flex flex-wrap items-center justify-between gap-2 px-1">
                    <div class="flex flex-wrap items-center gap-2">
                        <h3 class="mp-title-sm">{{ __('Pila de tarjetas') }}</h3>
                        <span class="rounded-full bg-surface-container px-2 py-0.5 text-[12px] font-medium text-on-surface-variant">{{ __('Toca para abrir') }}</span>
                    </div>
                    <span id="pass-sort-label" class="hidden text-sm font-semibold text-primary sm:inline">{{ __('Más recientes') }}</span>
                </div>
                <div id="pass-stack" class="mp-wallet-stack" aria-live="polite"></div>
            </div>

            <aside class="min-w-0 lg:col-span-5">
                <div id="pass-inspector" class="mp-pass-inspector">
                    <div class="flex items-center justify-between gap-2 border-b border-outline-variant/50 pb-3">
                        <div class="flex items-center gap-2">
                            <span class="h-2.5 w-2.5 rounded-full bg-primary"></span>
                            <span class="text-[15px] font-bold text-on-surface">{{ __('Código listo') }}</span>
                        </div>
                        <span id="pass-code-format-badge" class="mp-badge mp-badge-primary">{{ __('QR') }}</span>
                    </div>

                    <div class="mt-4">
                        <p id="pass-detail-tag" class="mp-eyebrow">{{ __('Selecciona un pase') }}</p>
                        <h3 id="pass-detail-title" class="mp-title-md mt-1">{{ __('Tus pases aparecerán aquí') }}</h3>
                        <p id="pass-detail-subtitle" class="mp-body-sm mt-1">{{ __('Elige una tarjeta de la pila para ver su código.') }}</p>
                    </div>

                    <div class="mt-4 rounded-2xl border border-outline-variant/50 bg-white p-4 shadow-inner">
                        <div id="pass-code-canvas" class="mx-auto flex min-h-[190px] w-full max-w-[260px] items-center justify-center"></div>
                        <p id="pass-code-caption" class="mt-3 break-all text-center font-mono text-[12px] text-on-surface-variant"></p>
                        <p id="pass-code-hash" class="mt-1 text-center font-mono text-[11px] text-outline"></p>
                    </div>

                    <div id="pass-detail-notes" class="mt-4 flex flex-wrap gap-1.5"></div>

                    <div class="mt-4 flex flex-col gap-2">
                        <button id="pass-present" type="button" class="mp-btn mp-btn-primary w-full">
                            <x-icon name="qr_code_2" class="text-[21px]" />
                            {{ __('Presentar código') }}
                        </button>
                        <div class="grid grid-cols-2 gap-2">
                            <button id="pass-share" type="button" class="mp-btn mp-btn-quiet mp-btn-sm whitespace-nowrap text-[13px]">
                                <x-icon name="share" class="text-[18px]" />
                                {{ __('Compartir') }}
                            </button>
                            <button id="pass-copy" type="button" class="mp-btn mp-btn-quiet mp-btn-sm whitespace-nowrap text-[13px]">
                                <x-icon name="content_copy" class="text-[18px]" />
                                {{ __('Copiar código') }}
                            </button>
                        </div>
                        <button id="pass-edit" type="button" class="mp-btn mp-btn-secondary mp-btn-sm">
                            <x-icon name="edit" class="text-[19px]" />
                            {{ __('Editar pase') }}
                        </button>
                    </div>

                    <div class="mt-4 flex items-center justify-between border-t border-outline-variant/50 pt-3 text-[12px] text-on-surface-variant">
                        <span class="flex items-center gap-1">
                            <x-icon name="lock" class="text-[15px] text-primary" />
                            {{ __('Protegido por mypass') }}
                        </span>
                        <span id="pass-item-id" class="font-mono text-[11px] text-outline"></span>
                    </div>
                </div>

                <div class="mp-card mt-4 flex items-start gap-4 p-5">
                    <span class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary-fixed text-primary">
                        <x-icon name="cloud_off" class="text-[26px]" />
                    </span>
                    <div>
                        <h3 class="mp-title-sm">{{ __('Cifrado de extremo a extremo') }}</h3>
                        <p class="mp-body-sm mt-1">{{ __('El código y los datos del pase se guardan cifrados. Solo se descifran en este dispositivo.') }}</p>
                    </div>
                </div>
            </aside>
        </div>

        <div class="mt-10 border-t border-outline-variant/60 pt-6">
            <button id="pass-archive-toggle" type="button" aria-expanded="false" aria-controls="pass-archive"
                    class="flex w-full flex-col justify-between gap-3 rounded-2xl p-2 text-left transition-colors hover:bg-surface-container-low sm:flex-row sm:items-center">
                <span class="flex items-center gap-3">
                    <x-icon name="archive" class="text-[25px] text-on-surface-variant" />
                    <span>
                        <span class="block font-semibold text-on-surface">{{ __('Pases pasados y archivados') }}</span>
                        <span id="pass-archive-count" class="block text-sm text-on-surface-variant">{{ __('0 pases vencidos') }}</span>
                    </span>
                </span>
                <span class="flex items-center gap-2 text-sm font-semibold text-on-surface-variant">
                    <span id="pass-archive-label">{{ __('Mostrar') }}</span>
                    <x-icon name="expand_more" id="pass-archive-chevron" class="text-[22px] transition-transform" />
                </span>
            </button>
            <div id="pass-archive" class="mt-3 grid gap-3 md:grid-cols-2 xl:grid-cols-3" hidden></div>
        </div>
    </div>

    <dialog id="pass-presentation" class="mp-dialog mp-presentation-dialog" aria-labelledby="pass-presentation-title">
        <div class="mp-presentation-panel">
            <div class="flex items-start justify-between gap-3">
                <div>
                    <p class="mp-eyebrow">{{ __('Presentación') }}</p>
                    <h2 id="pass-presentation-title" class="mp-title-md mt-1"></h2>
                </div>
                <button id="pass-presentation-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar presentación') }}">
                    <x-icon name="close" class="text-[22px]" />
                </button>
            </div>
            <p id="pass-presentation-meta" class="mp-body-sm mt-2"></p>
            <div class="mt-4 flex flex-1 items-center justify-center rounded-2xl border border-outline-variant/40 bg-white p-4">
                <div id="pass-presentation-code" class="flex w-full max-w-[380px] items-center justify-center"></div>
            </div>
            <p id="pass-presentation-hint" class="mt-3 text-center text-sm text-on-surface-variant">{{ __('Sube el brillo de la pantalla para facilitar la lectura.') }}</p>
            <button id="pass-presentation-done" type="button" class="mp-btn mp-btn-primary mt-4 w-full">{{ __('Cerrar y volver a la billetera') }}</button>
        </div>
    </dialog>
</section>
