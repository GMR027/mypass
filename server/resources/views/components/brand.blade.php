@props(['href' => null, 'inverse' => false, 'showName' => true])

<a href="{{ $href ?? (auth()->check() ? route('dashboard') : route('login')) }}"
   {{ $attributes->class(['inline-flex items-center gap-2 rounded-xl focus-visible:outline-none']) }}>
    <span @class([
        'flex h-10 w-10 shrink-0 items-center justify-center rounded-full',
        'bg-white text-primary' => $inverse,
        'bg-primary text-on-primary' => ! $inverse,
    ])>
        <x-icon name="lock" class="text-[21px]" filled />
    </span>
    @if ($showName)
        <span @class([
            'font-display text-[26px] font-bold lowercase leading-none tracking-tight',
            'text-white' => $inverse,
            'text-primary' => ! $inverse,
        ])>mypass</span>
    @endif
</a>
