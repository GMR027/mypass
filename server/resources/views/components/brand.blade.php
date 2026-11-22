@props(['href' => null, 'inverse' => false, 'showName' => true])

<a href="{{ $href ?? (auth()->check() ? route('dashboard') : route('login')) }}"
   {{ $attributes->class(['inline-flex items-center gap-1.5 rounded-xl focus-visible:outline-none']) }}>
    <img src="{{ asset('images/mypass-wallet.png') }}"
         alt=""
         width="256"
         height="256"
         class="h-11 w-11 shrink-0 object-contain"
         aria-hidden="true">
    @if ($showName)
        <span @class([
            'font-display text-[27px] font-bold lowercase leading-none tracking-tight',
            'text-white' => $inverse,
            'text-primary' => ! $inverse,
        ])>mypass</span>
    @endif
</a>
