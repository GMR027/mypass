@props(['active' => ''])

@php
    $items = [
        [
            'key' => 'inicio',
            'label' => __('Inicio'),
            'icon' => 'home',
            'href' => route('dashboard'),
            'tab' => null,
        ],
        [
            'key' => 'boveda',
            'label' => __('Bóveda'),
            'icon' => 'lock',
            'href' => route('vault', ['tab' => 'docs']),
            'tab' => 'docs',
        ],
        [
            'key' => 'pases',
            'label' => __('Pases'),
            'icon' => 'wallet',
            'href' => route('vault', ['tab' => 'pases']),
            'tab' => 'pases',
        ],
        [
            'key' => 'perfil',
            'label' => __('Perfil'),
            'icon' => 'account_circle',
            'href' => route('settings.security'),
            'tab' => null,
        ],
    ];
@endphp

<nav class="fixed inset-x-0 bottom-0 z-40 border-t border-outline-variant/60 bg-surface-container-lowest pb-[env(safe-area-inset-bottom)] shadow-[0_-2px_12px_rgba(18,28,38,0.06)] lg:hidden"
     aria-label="{{ __('Navegación principal') }}">
    <div class="mx-auto grid h-[72px] max-w-lg grid-cols-4 px-1">
        @foreach ($items as $item)
            <a href="{{ $item['href'] }}"
               @if ($item['tab']) data-vault-tab="{{ $item['tab'] }}" @endif
               @if ($active === $item['key']) aria-current="page" @endif
               class="mp-bottom-link flex min-h-12 flex-col items-center justify-center gap-0.5 rounded-xl px-1 text-center transition-colors">
                <x-icon :name="$item['icon']"
                        :filled="$active === $item['key']"
                        class="text-[25px]" />
                <span @class([
                    'mp-bottom-label text-[12px] leading-tight',
                    'font-bold' => $active === $item['key'],
                    'font-semibold' => $active !== $item['key'],
                ])>{{ $item['label'] }}</span>
            </a>
        @endforeach
    </div>
</nav>
