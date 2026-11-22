<x-auth-shell :title="__('Recuperar contraseña')">
    <div class="mb-6">
        <span class="mp-icon-tile-lg">
            <x-icon name="lock_reset" class="text-[30px]" />
        </span>
        <h1 class="mp-title-lg mt-4">{{ __('Recuperar contraseña') }}</h1>
        <p class="mp-body mt-2">{{ __('Escribe el correo de tu cuenta y te enviaremos un enlace para crear una contraseña nueva.') }}</p>
    </div>

    @if (session('status'))
        <div class="mp-alert mp-alert-success mb-5" role="status">
            <x-icon name="mark_email_read" class="mt-0.5 text-[22px]" />
            <p>{{ session('status') }}</p>
        </div>
    @endif

    @if ($errors->any())
        <div class="mp-alert mp-alert-error mb-5" role="alert">
            <x-icon name="error" class="mt-0.5 text-[22px]" />
            <p>{{ $errors->first() }}</p>
        </div>
    @endif

    <form method="POST" action="{{ route('password.email') }}" class="space-y-5">
        @csrf

        <div>
            <label for="email" class="mp-label">{{ __('Correo') }}</label>
            <input id="email" name="email" type="email" value="{{ old('email') }}" required autofocus
                   autocomplete="email" inputmode="email" class="mp-input" placeholder="edgar@ejemplo.com">
        </div>

        <button type="submit" class="mp-btn mp-btn-primary w-full">
            {{ __('Enviar enlace') }}
            <x-icon name="arrow_forward" class="text-[21px]" />
        </button>
    </form>

    <div class="mt-6 rounded-xl bg-surface-container-low p-4 text-sm leading-relaxed text-on-surface-variant">
        <p class="font-semibold text-on-surface">{{ __('Tu bóveda no se recupera con este enlace') }}</p>
        <p class="mt-1">{{ __('Este proceso cambia la contraseña de tu cuenta. La frase de tu bóveda y tu llave de emergencia siguen siendo necesarias para descifrar tus documentos.') }}</p>
    </div>

    <p class="mt-6 text-center text-[16px] text-on-surface-variant">
        <a href="{{ route('login') }}" class="font-semibold text-primary underline decoration-primary/40 underline-offset-4 hover:decoration-primary">
            {{ __('Volver a entrar') }}
        </a>
    </p>
</x-auth-shell>
