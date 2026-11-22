@props(['name', 'filled' => false])

<span {{ $attributes->class(['material-symbols-outlined', 'mp-icon-filled' => $filled]) }} aria-hidden="true">{{ $name }}</span>
