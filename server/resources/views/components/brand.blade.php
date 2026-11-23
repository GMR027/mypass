@props(['href' => null, 'inverse' => false, 'showName' => true, 'compact' => false])

<a href="{{ $href ?? (auth()->check() ? route('dashboard') : route('login')) }}"
   aria-label="My Pass"
   {{ $attributes->class(['group inline-flex items-center gap-2 rounded-2xl focus-visible:outline-none']) }}>
    <span @class([
        'relative flex shrink-0 items-center justify-center overflow-hidden rounded-2xl transition-transform duration-200 group-hover:-rotate-3',
        'h-11 w-11' => ! $compact,
        'h-10 w-10' => $compact,
        'bg-white/10 ring-1 ring-white/15' => $inverse,
        'bg-primary-fixed/70 ring-1 ring-primary/10' => ! $inverse,
    ])>
        <img src="{{ asset('images/mypass-wallet.png') }}"
             alt=""
             width="256"
             height="256"
             class="{{ $compact ? 'h-8 w-8' : 'h-9 w-9' }} object-contain"
             aria-hidden="true">
    </span>
    @if ($showName)
        <span @class([
            'font-display leading-none tracking-[-0.045em]',
            'text-[25px]' => ! $compact,
            'text-[23px]' => $compact,
            'text-white' => $inverse,
            'text-primary' => ! $inverse,
        ])>
            <span class="font-medium">My</span>&nbsp;<span class="ml-0.5 font-bold">Pass</span>
            <span @class([
                'ml-0.5 inline-block h-1.5 w-1.5 rounded-full align-super',
                'bg-accent' => true,
            ])></span>
        </span>
    @endif
</a>
