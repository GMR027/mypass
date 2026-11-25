@props(['compact' => false])

@php($themeIsDark = request()->cookie('mypass_theme') === 'dark')

<button type="button" data-theme-toggle
        class="{{ $compact ? 'inline-flex h-11 w-11' : 'inline-flex min-h-11 min-w-11' }} items-center justify-center rounded-xl text-on-surface-variant transition-colors hover:bg-surface-container hover:text-on-surface"
        aria-label="{{ __('Cambiar tema') }}" title="{{ __('Cambiar tema') }}">
    <x-icon :name="$themeIsDark ? 'light_mode' : 'dark_mode'" data-theme-icon class="text-[21px]" />
</button>
