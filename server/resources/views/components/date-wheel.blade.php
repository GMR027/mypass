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
    $currentYear = now()->year;
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
    <legend class="mp-label">
        {{ $label ?? __('Fecha de nacimiento') }}
        @if ($required)
            <span class="text-error" aria-hidden="true">*</span>
        @endif
    </legend>

    <p class="mp-hint mb-3">{{ __('Desliza cada rueda o usa las flechas del teclado.') }}</p>

    <div class="mp-date-wheel__viewport" role="group" aria-label="{{ $label ?? __('Fecha de nacimiento') }}">
        <div class="mp-date-wheel__column" data-wheel="day" role="listbox" aria-label="{{ __('Día') }}" tabindex="0"></div>
        <div class="mp-date-wheel__column mp-date-wheel__column--month" data-wheel="month" role="listbox" aria-label="{{ __('Mes') }}" tabindex="0"></div>
        <div class="mp-date-wheel__column" data-wheel="year" role="listbox" aria-label="{{ __('Año') }}" tabindex="0"></div>
    </div>

    <input type="hidden" name="{{ $name }}" value="{{ $initial }}" data-wheel-input @if ($required) required @endif disabled>

    <div class="mp-date-wheel__footer">
        <p class="mp-hint" data-wheel-age aria-live="polite">{{ __('Elige tu fecha de nacimiento.') }}</p>
        <button type="button" class="mp-btn mp-btn-quiet mp-btn-sm" data-wheel-toggle aria-expanded="false">
            <x-icon name="edit_calendar" class="text-[19px]" />
            <span>{{ __('Prefiero escribir la fecha') }}</span>
        </button>
    </div>

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

    @if ($hint)
        <p class="mp-hint mt-2">{{ $hint }}</p>
    @endif

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
