<section id="panel-generador" role="tabpanel" aria-labelledby="tab-btn-passes" hidden>
    <div id="pass-generator-app">
        <input type="hidden" id="generator-item-id">
        <input type="hidden" id="generator-item-version">

        <div class="mb-5 flex flex-col justify-between gap-4 xl:flex-row xl:items-end">
            <div class="max-w-2xl">
                <button id="generator-back" type="button" class="mb-3 inline-flex min-h-10 items-center gap-1.5 rounded-lg px-2 text-[15px] font-semibold text-primary hover:bg-surface-container">
                    <x-icon name="arrow_back" class="text-[20px]" />
                    {{ __('Volver a Pases') }}
                </button>
                <p class="mp-eyebrow">{{ __('Generador') }}</p>
                <h2 class="mp-title-xl mt-2">{{ __('Generador y editor de pases') }}</h2>
                <p class="mp-body mt-2">{{ __('Carga un boleto en PDF, JPG o PNG. El OCR se ejecuta en tu navegador y los datos se cifran antes de guardarse.') }}</p>
            </div>
            <div class="flex flex-col gap-2 sm:flex-row">
                <div class="relative">
                    <button id="generator-templates-toggle" type="button" class="mp-btn mp-btn-secondary min-h-12" aria-expanded="false" aria-controls="generator-templates-menu">
                        <x-icon name="dashboard_customize" class="text-[21px]" />
                        {{ __('Plantillas') }}
                    </button>
                    <div id="generator-templates-menu" class="absolute right-0 z-30 mt-2 w-64 rounded-2xl border border-outline-variant/60 bg-white p-2 shadow-dialog" hidden>
                        @foreach ([
                            'flight' => __('Vuelo / embarque'),
                            'event' => __('Evento / concierto'),
                            'transit' => __('Transporte'),
                            'id' => __('Identificación'),
                            'membership' => __('Membresía'),
                        ] as $template => $label)
                            <button type="button" data-generator-template="{{ $template }}" class="flex min-h-11 w-full items-center gap-2 rounded-xl px-3 text-left text-[15px] font-semibold text-on-surface hover:bg-surface-container">
                                <x-icon name="style" class="text-[19px] text-primary" />
                                {{ $label }}
                            </button>
                        @endforeach
                    </div>
                </div>
                <button id="generator-save-top" type="button" class="mp-btn mp-btn-primary min-h-12">
                    <x-icon name="lock" class="text-[20px]" />
                    {{ __('Guardar en bóveda') }}
                </button>
            </div>
        </div>

        <div class="grid gap-5 xl:grid-cols-12 xl:items-start">
            <div class="space-y-5 xl:col-span-7">
                <section class="mp-card p-5 md:p-6">
                    <div class="flex flex-wrap items-center justify-between gap-2">
                        <div class="flex items-center gap-3">
                            <span class="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-bold text-on-primary">01</span>
                            <h3 class="mp-title-sm">{{ __('Ingesta de documento') }}</h3>
                        </div>
                        <span class="mp-badge mp-badge-neutral">
                            <x-icon name="document_scanner" class="text-[16px]" />
                            {{ __('OCR local') }}
                        </span>
                    </div>

                    <label id="generator-dropzone" class="mt-4 block cursor-pointer rounded-2xl border-2 border-dashed border-outline/35 bg-surface-container-low px-5 py-8 text-center transition-colors hover:border-primary hover:bg-surface-container">
                        <span class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-white text-primary shadow-sm">
                            <x-icon name="upload_file" class="text-[28px]" />
                        </span>
                        <span class="mt-3 block font-semibold text-on-surface">{{ __('Arrastra aquí tu boleto o documento') }}</span>
                        <span class="mt-1 block text-sm text-on-surface-variant">{{ __('PDF, JPG, PNG o WEBP · máximo 25 MB') }}</span>
                        <span class="mt-2 block text-sm text-outline">{{ __('El archivo no se sube a ningún servidor durante el OCR.') }}</span>
                        <input id="generator-file" type="file" accept="image/*,application/pdf" class="sr-only">
                    </label>

                    <button id="generator-camera" type="button" class="mp-btn mp-btn-secondary mp-btn-sm mt-3">
                        <x-icon name="photo_camera" class="text-[19px]" />
                        {{ __('Usar cámara') }}
                    </button>

                    <div id="generator-file-info" class="mt-3 hidden items-center justify-between gap-3 rounded-xl bg-surface-container-low p-3">
                        <div class="flex min-w-0 items-center gap-2">
                            <x-icon name="description" class="text-[22px] shrink-0 text-primary" />
                            <span id="generator-file-name" class="truncate text-sm font-semibold text-on-surface"></span>
                        </div>
                        <button id="generator-file-clear" type="button" class="mp-btn mp-btn-quiet mp-btn-sm shrink-0" aria-label="{{ __('Quitar archivo') }}">
                            <x-icon name="close" class="text-[18px]" />
                        </button>
                    </div>

                    <div id="generator-pdf-page-wrap" class="mt-3 flex items-center gap-3 rounded-xl bg-surface-container-low p-3" hidden>
                        <label for="generator-pdf-page" class="text-sm font-semibold text-on-surface">{{ __('Página del PDF para OCR') }}</label>
                        <input id="generator-pdf-page" type="number" min="1" max="50" value="1"
                               class="mp-input w-24 text-center" inputmode="numeric">
                        <span class="text-sm text-on-surface-variant">{{ __('Se usa para el código y el OCR.') }}</span>
                    </div>

                    <div id="generator-ocr-progress-wrap" class="mt-4" hidden>
                        <div class="flex items-center justify-between gap-2 text-sm">
                            <span id="generator-ocr-status" class="font-semibold text-on-surface">{{ __('Preparando OCR…') }}</span>
                            <span id="generator-ocr-percent" class="font-mono text-on-surface-variant">0%</span>
                        </div>
                        <div class="mt-2 h-2 overflow-hidden rounded-full bg-surface-container-high">
                            <div id="generator-ocr-progress" class="h-full w-0 rounded-full bg-primary transition-[width] duration-300"></div>
                        </div>
                    </div>

                    <div class="mt-3 flex flex-wrap items-center gap-2">
                        <button id="generator-reextract" type="button" class="mp-btn mp-btn-secondary mp-btn-sm" disabled>
                            <x-icon name="refresh" class="text-[18px]" />
                            {{ __('Re-extraer') }}
                        </button>
                        <span id="generator-confidence" class="text-sm text-on-surface-variant"></span>
                    </div>
                </section>

                <section class="mp-card p-5 md:p-6">
                    <div class="flex items-center gap-3">
                        <span class="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-bold text-on-primary">02</span>
                        <div>
                            <h3 class="mp-title-sm">{{ __('Tipo de credencial') }}</h3>
                            <p class="text-sm text-on-surface-variant">{{ __('Elige la plantilla que mejor describe tu pase.') }}</p>
                        </div>
                    </div>
                    <div id="generator-categories" class="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-5">
                        @foreach ([
                            'flight' => ['icon' => 'flight_takeoff', 'label' => __('Vuelo')],
                            'event' => ['icon' => 'confirmation_number', 'label' => __('Evento')],
                            'transit' => ['icon' => 'train', 'label' => __('Transporte')],
                            'id' => ['icon' => 'badge', 'label' => __('Identidad')],
                            'membership' => ['icon' => 'loyalty', 'label' => __('Membresía')],
                        ] as $category => $option)
                            <button type="button" data-generator-category="{{ $category }}"
                                    class="flex min-h-16 flex-col items-center justify-center gap-1 rounded-xl border-2 border-transparent bg-surface-container-low px-2 text-[13px] font-semibold text-on-surface-variant transition-colors hover:bg-surface-container">
                                <x-icon :name="$option['icon']" class="text-[22px]" />
                                {{ $option['label'] }}
                            </button>
                        @endforeach
                    </div>
                </section>

                <section class="mp-card p-5 md:p-6">
                    <div class="flex items-center gap-3">
                        <span class="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-bold text-on-primary">03</span>
                        <div>
                            <h3 class="mp-title-sm">{{ __('Personalización visual') }}</h3>
                            <p class="text-sm text-on-surface-variant">{{ __('Color, textura, imagen, logo y formato del código.') }}</p>
                        </div>
                    </div>

                    <div class="mt-4">
                        <div class="flex flex-wrap items-center justify-between gap-2">
                            <p class="mp-label">{{ __('Color de la tarjeta') }}</p>
                            <span id="generator-color-value" class="font-mono text-[13px] font-semibold text-on-surface-variant">#0c2340</span>
                        </div>
                        <div id="generator-themes" class="flex flex-wrap items-center gap-2">
                            @foreach ([
                                'midnight' => '#0c2340',
                                'violet' => '#4c1d95',
                                'forest' => '#064e3b',
                                'obsidian' => '#18181b',
                                'burgundy' => '#7f1d1d',
                                'amber' => '#9a3412',
                            ] as $theme => $color)
                                <button type="button" data-pass-theme="{{ $theme }}" aria-label="{{ $theme }}"
                                        class="flex h-11 w-11 items-center justify-center rounded-full border-2 border-white shadow-sm ring-2 ring-outline-variant/50 transition-transform hover:scale-105"
                                        style="background-color: {{ $color }}">
                                    <span class="material-symbols-outlined mp-icon-filled text-[18px] text-white">check</span>
                                </button>
                            @endforeach
                            <label class="flex h-11 items-center gap-2 rounded-full border border-outline-variant/60 bg-white px-3 text-sm font-semibold text-on-surface-variant">
                                {{ __('Otro') }}
                                <input id="generator-color" type="color" value="#0c2340"
                                       class="h-7 w-9 cursor-pointer rounded border-0 bg-transparent p-0">
                            </label>
                        </div>
                    </div>

                    <div class="mt-5 border-t border-outline-variant/50 pt-5">
                        <div class="flex flex-wrap items-start justify-between gap-2">
                            <div>
                                <p class="mp-label">{{ __('Textura de la tarjeta') }}</p>
                                <p class="text-sm text-on-surface-variant">{{ __('Añade un patrón al color. Puedes combinarlo con una imagen.') }}</p>
                            </div>
                            <button id="generator-appearance-reset" type="button" class="mp-btn mp-btn-quiet mp-btn-sm shrink-0">
                                <x-icon name="restart_alt" class="text-[18px]" />
                                {{ __('Restablecer fondo') }}
                            </button>
                        </div>
                        <div id="generator-textures" class="mt-3 grid grid-cols-3 gap-2 sm:grid-cols-4"></div>
                        <div id="generator-texture-controls" class="mt-3 grid gap-3 rounded-xl bg-surface-container/60 p-3 sm:grid-cols-2" hidden>
                            <div>
                                <label for="generator-texture-size" class="mp-label">{{ __('Tamaño de la textura') }}</label>
                                <select id="generator-texture-size" class="mp-select">
                                    <option value="sm">{{ __('Pequeña') }}</option>
                                    <option value="md">{{ __('Mediana') }}</option>
                                    <option value="lg">{{ __('Grande') }}</option>
                                </select>
                            </div>
                            <div>
                                <label for="generator-texture-repeat" class="mp-label">{{ __('Repetición') }}</label>
                                <select id="generator-texture-repeat" class="mp-select">
                                    <option value="repeat">{{ __('Repetir') }}</option>
                                    <option value="repeat-x">{{ __('Horizontal') }}</option>
                                    <option value="repeat-y">{{ __('Vertical') }}</option>
                                    <option value="no-repeat">{{ __('Sin repetir') }}</option>
                                </select>
                            </div>
                            <div class="sm:col-span-2">
                                <label for="generator-texture-opacity" class="mp-label">{{ __('Opacidad de la textura') }}</label>
                                <div class="flex items-center gap-3">
                                    <input id="generator-texture-opacity" type="range" min="0" max="100" step="5" value="100" class="h-2 flex-1 cursor-pointer accent-primary">
                                    <output id="generator-texture-opacity-value" class="min-w-10 text-right text-sm font-semibold tabular-nums text-on-surface-variant" for="generator-texture-opacity">100%</output>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="mt-4 rounded-2xl border border-outline-variant/50 bg-surface-container-low p-4">
                        <div class="flex flex-wrap items-center justify-between gap-3">
                            <div class="min-w-0">
                                <p class="mp-label">{{ __('Imagen de fondo') }}</p>
                                <p class="text-sm text-on-surface-variant">{{ __('La imagen se muestra sobre la textura y se guarda cifrada dentro del pase.') }}</p>
                            </div>
                            <div class="flex shrink-0 items-center gap-2">
                                <label class="mp-btn mp-btn-quiet mp-btn-sm cursor-pointer">
                                    <x-icon name="add_photo_alternate" class="text-[19px]" />
                                    {{ __('Elegir imagen') }}
                                    <input id="generator-background-image" type="file" accept="image/*" class="sr-only">
                                </label>
                                <button id="generator-background-clear" type="button" class="mp-btn mp-btn-quiet mp-btn-sm" hidden aria-label="{{ __('Quitar imagen de fondo') }}">
                                    <x-icon name="delete" class="text-[19px]" />
                                </button>
                                <img id="generator-background-preview" alt="" class="h-14 w-14 rounded-xl border border-black/10 object-cover" width="64" height="64" hidden>
                            </div>
                        </div>
                        <div id="generator-background-controls" class="mt-3 grid gap-3 sm:grid-cols-2" hidden>
                            <div>
                                <label for="generator-background-size" class="mp-label">{{ __('Tamaño de la imagen') }}</label>
                                <select id="generator-background-size" class="mp-select">
                                    <option value="cover">{{ __('Cubrir') }}</option>
                                    <option value="contain">{{ __('Contener') }}</option>
                                    <option value="50%">50%</option>
                                    <option value="100%">100%</option>
                                    <option value="200%">200%</option>
                                </select>
                            </div>
                            <div>
                                <label for="generator-background-repeat" class="mp-label">{{ __('Repetición') }}</label>
                                <select id="generator-background-repeat" class="mp-select">
                                    <option value="no-repeat">{{ __('Sin repetir') }}</option>
                                    <option value="repeat">{{ __('Repetir') }}</option>
                                    <option value="repeat-x">{{ __('Horizontal') }}</option>
                                    <option value="repeat-y">{{ __('Vertical') }}</option>
                                </select>
                            </div>
                            <div class="sm:col-span-2">
                                <label for="generator-background-shade" class="mp-label">{{ __('Oscurecer imagen') }}</label>
                                <div class="flex items-center gap-3">
                                    <input id="generator-background-shade" type="range" min="0" max="70" step="5" value="30" class="h-2 flex-1 cursor-pointer accent-primary">
                                    <output id="generator-background-shade-value" class="min-w-10 text-right text-sm font-semibold tabular-nums text-on-surface-variant" for="generator-background-shade">30%</output>
                                </div>
                                <p class="mt-1 text-[13px] leading-snug text-on-surface-variant">{{ __('Mejora el contraste del texto sobre la imagen.') }}</p>
                            </div>
                        </div>
                    </div>

                    <div class="mt-4 grid gap-3 sm:grid-cols-2">
                        <div>
                            <label for="generator-code-type" class="mp-label">{{ __('Tipo de código') }}</label>
                            <select id="generator-code-type" class="mp-select">
                                <option value="qr">{{ __('QR') }}</option>
                                <option value="aztec">{{ __('Aztec') }}</option>
                                <option value="pdf417">{{ __('PDF417') }}</option>
                                <option value="code128">{{ __('Código de barras Code 128') }}</option>
                            </select>
                        </div>
                        <div>
                            <span class="mp-label">{{ __('Logo de la entidad emisora') }}</span>
                            <div class="flex items-center gap-2">
                                <label class="mp-btn mp-btn-quiet mp-btn-sm cursor-pointer">
                                    <x-icon name="cloud_upload" class="text-[19px]" />
                                    {{ __('Elegir logo') }}
                                    <input id="generator-logo" type="file" accept="image/*" class="sr-only">
                                </label>
                                <button id="generator-logo-clear" type="button" class="mp-btn mp-btn-quiet mp-btn-sm" hidden aria-label="{{ __('Quitar logo') }}">
                                    <x-icon name="delete" class="text-[19px]" />
                                </button>
                                <img id="generator-logo-preview" alt="" class="h-10 w-10 rounded-lg object-contain" width="64" height="64" hidden>
                            </div>
                        </div>
                    </div>
                </section>

                <section class="mp-card p-5 md:p-6">
                    <div class="flex flex-wrap items-center justify-between gap-2">
                        <div class="flex items-center gap-3">
                            <span class="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-bold text-on-primary">04</span>
                            <div>
                                <h3 class="mp-title-sm">{{ __('Campos del pase') }}</h3>
                                <p class="text-sm text-on-surface-variant">{{ __('Revisa y corrige lo que haya detectado el OCR.') }}</p>
                            </div>
                        </div>
                    </div>

                    <div id="generator-fields" class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3"></div>

                    <div class="mt-5">
                        <p class="mp-label">{{ __('Notas y beneficios') }}</p>
                        <div id="generator-note-list" class="flex flex-wrap gap-2"></div>
                        <div class="mt-2 flex flex-col gap-2 sm:flex-row">
                            <input id="generator-note-input" type="text" class="mp-input mp-input-sm flex-1" placeholder="{{ __('Ej. Acceso VIP, equipaje incluido…') }}">
                            <button id="generator-note-add" type="button" class="mp-btn mp-btn-secondary mp-btn-sm shrink-0">
                                <x-icon name="add" class="text-[19px]" />
                                {{ __('Agregar nota') }}
                            </button>
                        </div>
                    </div>
                </section>

                <section class="mp-card p-5 md:p-6">
                    <div class="flex items-center gap-3">
                        <span class="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-bold text-on-primary">05</span>
                        <div>
                            <h3 class="mp-title-sm">{{ __('Funciones y seguridad') }}</h3>
                            <p class="text-sm text-on-surface-variant">{{ __('Solo opciones que funcionan de verdad.') }}</p>
                        </div>
                    </div>
                    <div class="mt-4 grid gap-3 sm:grid-cols-2">
                        <label class="flex cursor-pointer items-start gap-3 rounded-xl bg-surface-container-low p-4">
                            <input id="generator-opt-presentation" type="checkbox" checked class="mp-checkbox mt-1">
                            <span>
                                <span class="block font-semibold text-on-surface">{{ __('Modo presentación') }}</span>
                                <span class="block text-sm text-on-surface-variant">{{ __('Muestra el código a pantalla completa y con el brillo alto.') }}</span>
                            </span>
                        </label>
                        <label class="flex cursor-pointer items-start gap-3 rounded-xl bg-surface-container-low p-4">
                            <input id="generator-opt-share" type="checkbox" checked class="mp-checkbox mt-1">
                            <span>
                                <span class="block font-semibold text-on-surface">{{ __('Compartir con familia') }}</span>
                                <span class="block text-sm text-on-surface-variant">{{ __('Permite enviar una copia cifrada desde la bóveda.') }}</span>
                            </span>
                        </label>
                        <label class="flex cursor-pointer items-start gap-3 rounded-xl bg-surface-container-low p-4">
                            <input id="generator-opt-reminder" type="checkbox" checked class="mp-checkbox mt-1">
                            <span>
                                <span class="block font-semibold text-on-surface">{{ __('Recordatorio de vencimiento') }}</span>
                                <span class="block text-sm text-on-surface-variant">{{ __('Usa la fecha del pase para avisarte antes.') }}</span>
                            </span>
                        </label>
                        <label class="flex cursor-pointer items-start gap-3 rounded-xl bg-surface-container-low p-4">
                            <input id="generator-opt-attach" type="checkbox" class="mp-checkbox mt-1">
                            <span>
                                <span class="block font-semibold text-on-surface">{{ __('Guardar archivo original') }}</span>
                                <span class="block text-sm text-on-surface-variant">{{ __('Adjunta una copia cifrada del PDF o imagen.') }}</span>
                            </span>
                        </label>
                    </div>
                </section>
            </div>

            <aside class="xl:sticky xl:top-28 xl:col-span-5">
                <div class="mp-card overflow-hidden">
                    <div class="flex items-center justify-between gap-2 border-b border-outline-variant/50 px-4 py-3">
                        <div class="flex items-center gap-2">
                            <span class="h-2.5 w-2.5 rounded-full bg-primary"></span>
                            <span class="text-[15px] font-bold text-on-surface">{{ __('Vista previa') }}</span>
                        </div>
                        <span class="mp-badge mp-badge-neutral">{{ __('Local') }}</span>
                    </div>

                    <div class="bg-surface-container-low p-4">
                        <div id="generator-preview" class="mx-auto max-w-[360px]"></div>
                    </div>

                    <div class="space-y-2 p-4">
                        <button id="generator-save" type="button" class="mp-btn mp-btn-primary w-full">
                            <x-icon name="lock" class="text-[21px]" />
                            {{ __('Guardar en bóveda') }}
                        </button>
                        <div class="grid grid-cols-2 gap-2">
                            <button id="generator-copy" type="button" class="mp-btn mp-btn-quiet mp-btn-sm whitespace-nowrap text-[13px]">
                                <x-icon name="content_copy" class="text-[18px]" />
                                {{ __('Copiar código') }}
                            </button>
                            <button id="generator-share" type="button" class="mp-btn mp-btn-quiet mp-btn-sm whitespace-nowrap text-[13px]" disabled>
                                <x-icon name="share" class="text-[18px]" />
                                {{ __('Compartir') }}
                            </button>
                        </div>
                        <p id="generator-status" class="text-sm font-semibold text-primary" role="status"></p>
                        <p id="generator-error" class="mp-alert mp-alert-error" role="alert" hidden></p>
                    </div>
                </div>

                <p class="mt-3 flex items-start gap-2 rounded-xl bg-surface-container-low p-3 text-[13px] leading-relaxed text-on-surface-variant">
                    <x-icon name="info" class="mt-0.5 text-[18px] text-primary" />
                    {{ __('El código se genera en tu dispositivo. No sustituye un pase oficial emitido por la aerolínea o el organizador.') }}
                </p>
            </aside>
        </div>
    </div>

    <dialog id="generator-camera-modal" class="mp-dialog" aria-labelledby="generator-camera-title">
        <div class="mp-dialog-panel">
            <div class="mp-dialog-header">
                <div class="flex items-start gap-3">
                    <span class="mp-icon-tile">
                        <x-icon name="photo_camera" class="text-[24px]" />
                    </span>
                    <div>
                        <h2 id="generator-camera-title" class="mp-title-md">{{ __('Capturar con la cámara') }}</h2>
                        <p class="mp-hint mt-1">{{ __('Enfoca el boleto completo. La imagen se procesa solo en este navegador.') }}</p>
                    </div>
                </div>
                <button id="generator-camera-close" type="button" class="mp-btn mp-btn-quiet mp-btn-icon" aria-label="{{ __('Cerrar') }}">
                    <x-icon name="close" class="text-[21px]" />
                </button>
            </div>

            <div class="overflow-hidden rounded-xl bg-black">
                <video id="generator-camera-video" class="max-h-[60vh] w-full object-contain" autoplay playsinline muted></video>
            </div>

            <p id="generator-camera-error" class="mp-alert mp-alert-error mt-3" role="alert" hidden></p>

            <div class="mt-4 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <button id="generator-camera-cancel" type="button" class="mp-btn mp-btn-secondary">{{ __('Cancelar') }}</button>
                <button id="generator-camera-capture" type="button" class="mp-btn mp-btn-primary">
                    <x-icon name="photo_camera" class="text-[20px]" />
                    {{ __('Capturar foto') }}
                </button>
            </div>
        </div>
    </dialog>
</section>
