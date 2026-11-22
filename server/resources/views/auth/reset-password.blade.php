<x-auth-shell :title="__('Crear contraseña nueva')">
    <div class="mb-6">
        <span class="mp-icon-tile-lg">
            <x-icon name="key" class="text-[30px]" />
        </span>
        <h1 class="mp-title-lg mt-4">{{ __('Crear contraseña nueva') }}</h1>
        <p class="mp-body mt-2">{{ __('Elige una contraseña distinta a la anterior. La usarás junto con tu frase de bóveda.') }}</p>
    </div>

    @if ($errors->any())
        <div class="mp-alert mp-alert-error mb-5" role="alert">
            <x-icon name="error" class="mt-0.5 text-[22px]" />
            <ul class="list-inside list-disc space-y-1">
                @foreach ($errors->all() as $error)
                    <li>{{ $error }}</li>
                @endforeach
            </ul>
        </div>
    @endif

    <form method="POST" action="{{ route('password.update') }}" class="space-y-5">
        @csrf
        <input type="hidden" name="token" value="{{ $request->route('token') }}">

        <div>
            <label for="email" class="mp-label">{{ __('Correo') }}</label>
            <input id="email" name="email" type="email" value="{{ old('email', $request->email) }}" required
                   autocomplete="email" inputmode="email" class="mp-input" placeholder="edgar@ejemplo.com">
        </div>

        <div>
            <label for="password" class="mp-label">{{ __('Contraseña nueva') }}</label>
            <div class="relative">
                <input id="password" name="password" type="password" required minlength="12"
                       autocomplete="new-password" class="mp-input pr-14">
                <button type="button" data-toggle-password="password" aria-pressed="false"
                        aria-label="{{ __('Mostrar contraseña') }}"
                        class="absolute right-1.5 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-lg text-on-surface-variant hover:bg-surface-container">
                    <x-icon name="visibility" class="text-[22px]" />
                </button>
            </div>
            <p class="mp-hint mt-1.5">{{ __('Usa una frase memorable de 12 caracteres o más, con al menos una letra y un número.') }}</p>
        </div>

        <div>
            <label for="password_confirmation" class="mp-label">{{ __('Repite la contraseña') }}</label>
            <div class="relative">
                <input id="password_confirmation" name="password_confirmation" type="password" required
                       autocomplete="new-password" class="mp-input pr-14">
                <button type="button" data-toggle-password="password_confirmation" aria-pressed="false"
                        aria-label="{{ __('Mostrar contraseña') }}"
                        class="absolute right-1.5 top-1/2 flex h-11 w-11 -translate-y-1/2 items-center justify-center rounded-lg text-on-surface-variant hover:bg-surface-container">
                    <x-icon name="visibility" class="text-[22px]" />
                </button>
            </div>
        </div>

        <button type="submit" class="mp-btn mp-btn-primary w-full">
            {{ __('Guardar contraseña') }}
            <x-icon name="arrow_forward" class="text-[21px]" />
        </button>
    </form>
</x-auth-shell>
