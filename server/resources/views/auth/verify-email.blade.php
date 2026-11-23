<x-auth-shell :title="__('Confirma tu correo')">
    <div class="mb-6">
        <div class="flex items-start justify-between gap-4">
            <span class="mp-icon-tile-lg">
                <x-icon name="mark_email_unread" class="text-[30px]" />
            </span>
            <span class="mp-badge mp-badge-primary">
                <x-icon name="verified_user" class="text-[16px]" filled />
                {{ __('Un último paso') }}
            </span>
        </div>
        <p class="mp-eyebrow mt-5">{{ __('Tu cuenta está casi lista') }}</p>
        <h1 class="mp-title-lg mt-2">{{ __('Confirma tu correo') }}</h1>
        <p class="mp-body mt-2">{{ __('Antes de entrar a tu bóveda, confirma que :email es tuyo.', ['email' => auth()->user()->email]) }}</p>
    </div>

    @if (session('status') === 'verification-link-sent')
        <div class="mp-alert mp-alert-success mb-5" role="status">
            <x-icon name="mark_email_read" class="mt-0.5 text-[22px]" />
            <p>{{ __('Te enviamos un nuevo enlace de confirmación. Revisa tu bandeja y la carpeta de spam.') }}</p>
        </div>
    @endif

    <div class="mp-card mp-card-pad">
        <div class="flex items-start gap-3 rounded-xl bg-surface-container-low p-4">
            <x-icon name="info" class="mt-0.5 text-[21px] text-primary" />
            <p class="mp-hint">{{ __('Te enviamos un enlace de confirmación. El enlace vence en 60 minutos; si no lo ves, revisa spam o pide uno nuevo.') }}</p>
        </div>

        <form method="POST" action="{{ route('verification.send') }}" class="mt-5">
            @csrf
            <button type="submit" class="mp-btn mp-btn-primary w-full">
                <x-icon name="send" class="text-[21px]" />
                {{ __('Reenviar enlace de confirmación') }}
            </button>
        </form>

        <form method="POST" action="{{ route('logout') }}" class="mt-3">
            @csrf
            <button type="submit" class="mp-btn mp-btn-quiet w-full">
                <x-icon name="logout" class="text-[20px]" />
                {{ __('Cerrar sesión') }}
            </button>
        </form>
    </div>

    <div class="mt-6 rounded-xl bg-surface-container-low p-4 text-sm leading-relaxed text-on-surface-variant">
        <p class="font-semibold text-on-surface">{{ __('¿El correo no es correcto?') }}</p>
        <p class="mt-1">{{ __('Cierra sesión y crea la cuenta de nuevo con el correo correcto. La cuenta sin confirmar no podrá abrir la bóveda.') }}</p>
    </div>
</x-auth-shell>
