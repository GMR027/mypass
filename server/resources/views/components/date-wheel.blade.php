@props([
    'name' => 'birth_date',
    'value' => null,
    'label' => null,
    'hint' => null,
    'required' => true,
    'id' => null,
])

@php
    $fieldId = $id ?? 'date-wheel-'.$name;
    $initial = old($name, $value instanceof \DateTimeInterface ? $value->format('Y-m-d') : $value);
    $initialDisplay = $initial
        ? \Illuminate\Support\Carbon::parse($initial)->locale(app()->getLocale())->isoFormat('LL')
        : __('Elegir fecha');
    $minDate = now()->subYears(\App\Models\User::MAXIMUM_AGE)->toDateString();
    $maxDate = now()->subYears(\App\Models\User::MINIMUM_AGE)->toDateString();
@endphp

<fieldset id="{{ $fieldId }}"
          class="mp-date-wheel"
          data-date-wheel
          data-name="{{ $name }}"
          data-value="{{ $initial }}"
          data-locale="{{ app()->getLocale() }}"
          data-min-age="{{ \App\Models\User::MINIMUM_AGE }}"
          data-max-age="{{ \App\Models\User::MAXIMUM_AGE }}"
          data-senior-age="{{ \App\Models\User::SENIOR_AGE }}">
    <legend class="sr-only">
        {{ $label ?? __('Fecha de nacimiento') }}
        @if ($required)
            <span class="sr-only">{{ __('Obligatorio') }}</span>
        @endif
    </legend>

    <input type="hidden" name="{{ $name }}" value="{{ $initial }}" data-wheel-input @if ($required) required @endif disabled>

    <button type="button" class="mp-date-trigger" data-wheel-trigger aria-haspopup="dialog" aria-expanded="false">
        <span class="mp-date-trigger__icon" aria-hidden="true">
            <x-icon name="calendar_month" class="text-[24px]" />
        </span>
        <span class="mp-date-trigger__copy">
            <span class="mp-date-trigger__label">{{ $label ?? __('Fecha de nacimiento') }}</span>
            <span class="mp-date-trigger__value" data-wheel-display>{{ $initialDisplay }}</span>
        </span>
        <x-icon name="expand_more" class="text-[22px] text-outline" />
    </button>

    <p class="mp-hint mt-2" data-wheel-age aria-live="polite">{{ __('Elige tu fecha de nacimiento.') }}</p>

    @if ($hint)
        <p class="mp-hint mt-1">{{ $hint }}</p>
    @endif

    <dialog class="mp-date-dialog" data-wheel-dialog aria-labelledby="{{ $fieldId }}-title">
        <div class="mp-date-dialog__panel">
            <header class="mp-date-dialog__header">
                <div>
                    <p class="mp-eyebrow">{{ __('Selecciona una fecha') }}</p>
                    <h2 id="{{ $fieldId }}-title" class="mp-title-md mt-1">{{ $label ?? __('Fecha de nacimiento') }}</h2>
                </div>
                <button type="button" class="mp-btn mp-btn-quiet mp-btn-icon" data-wheel-close aria-label="{{ __('Cerrar') }}">
                    <x-icon name="close" class="text-[22px]" />
                </button>
            </header>

            <div class="mp-date-dialog__wheels">
                <div class="mp-date-dialog__labels" aria-hidden="true">
                    <span>{{ __('Día') }}</span>
                    <span>{{ __('Mes') }}</span>
                    <span>{{ __('Año') }}</span>
                </div>
                <div class="mp-date-wheel__viewport" data-wheel-viewport>
                    <span class="mp-date-dialog__highlight" aria-hidden="true"></span>
                    <div class="mp-date-wheel__column" data-wheel="day" role="listbox" aria-label="{{ __('Día') }}" tabindex="0"></div>
                    <div class="mp-date-wheel__column" data-wheel="month" role="listbox" aria-label="{{ __('Mes') }}" tabindex="0"></div>
                    <div class="mp-date-wheel__column" data-wheel="year" role="listbox" aria-label="{{ __('Año') }}" tabindex="0"></div>
                </div>
            </div>

            <p class="mp-date-dialog__summary" data-wheel-summary>{{ $initialDisplay }}</p>
            <p class="mp-alert mp-alert-error mt-3" data-wheel-error role="alert" hidden></p>

            <div class="mt-3 hidden" data-wheel-native>
                <label for="{{ $fieldId }}-native" class="mp-label">{{ __('Escribe la fecha') }}</label>
                <input id="{{ $fieldId }}-native"
                       type="date"
                       value="{{ $initial }}"
                       min="{{ $minDate }}"
                       max="{{ $maxDate }}"
                       class="mp-input mp-date-input"
                       data-wheel-native-input>
            </div>

            <footer class="mp-date-dialog__footer">
                <button type="button" class="mp-btn mp-btn-quiet" data-wheel-toggle aria-expanded="false">
                    <x-icon name="edit_calendar" class="text-[20px]" />
                    <span data-wheel-toggle-label>{{ __('Escribir la fecha') }}</span>
                </button>
                <button type="button" class="mp-btn mp-btn-primary" data-wheel-confirm>
                    <x-icon name="check" class="text-[21px]" />
                    {{ __('Listo') }}
                </button>
            </footer>
        </div>
    </dialog>

    <noscript>
        <div class="mt-3">
            <label for="{{ $fieldId }}-noscript" class="mp-label">{{ __('Fecha de nacimiento') }}</label>
            <input id="{{ $fieldId }}-noscript"
                   type="date"
                   name="{{ $name }}"
                   value="{{ $initial }}"
                   min="{{ $minDate }}"
                   max="{{ $maxDate }}"
                   class="mp-input mp-date-input"
                   @if ($required) required @endif>
        </div>
    </noscript>
</fieldset>
