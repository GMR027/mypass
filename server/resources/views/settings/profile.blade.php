<x-layout :title="__('Perfil')">
    <div class="mx-auto max-w-4xl">
        <div class="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
            <div>
                <p class="mp-eyebrow">{{ __('Tu cuenta') }}</p>
                <h1 class="mp-title-lg mt-2">{{ __('Perfil') }}</h1>
                <p class="mp-body mt-2 max-w-2xl">{{ __('Administra tus datos de acceso, tu contraseña y la seguridad de tu cuenta.') }}</p>
            </div>
            <a href="{{ route('settings.security') }}" class="mp-btn mp-btn-secondary shrink-0">
                <x-icon name="shield" class="text-[20px]" />
                {{ __('Seguridad y preferencias') }}
            </a>
        </div>

        @if (session('status'))
            <p class="mp-alert mp-alert-success mb-5" role="status">{{ session('status') }}</p>
        @endif

        <section class="mp-card mp-card-pad mb-5">
            <div class="flex flex-col gap-5 sm:flex-row sm:items-center">
                <div class="relative h-24 w-24 shrink-0">
                    <span class="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full bg-primary text-3xl font-bold uppercase text-on-primary ring-4 ring-primary-fixed/60" aria-hidden="true">
                        @if ($user->avatar_url)
                            <img src="{{ $user->avatar_url }}" alt="" class="h-full w-full object-cover">
                        @else
                            {{ mb_substr($user->name ?: $user->email, 0, 1) }}
                        @endif
                    </span>
                    <span class="absolute -bottom-1 -right-1 flex h-8 w-8 items-center justify-center rounded-full bg-primary text-on-primary ring-2 ring-white" aria-hidden="true">
                        <x-icon name="photo_camera" class="text-[17px]" />
                    </span>
                </div>

                <div class="min-w-0 flex-1">
                    <h2 class="mp-title-sm">{{ __('Foto de perfil') }}</h2>
                    <p class="mp-hint mt-1">{{ __('Sube una foto para que tu cuenta sea más fácil de reconocer.') }}</p>

                    @if ($errors->updateAvatar->any())
                        <div class="mp-alert mp-alert-error mt-3" role="alert">
                            <x-icon name="error" class="mt-0.5 text-[21px]" />
                            <ul class="list-inside list-disc space-y-1">
                                @foreach ($errors->updateAvatar->all() as $error)
                                    <li>{{ $error }}</li>
                                @endforeach
                            </ul>
                        </div>
                    @endif

                    <form method="POST" action="{{ route('settings.profile.avatar.update') }}"
                          enctype="multipart/form-data" class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
                        @csrf
                        <div class="min-w-0 flex-1">
                            <label for="profile-avatar" class="mp-label">{{ __('Archivo de imagen') }}</label>
                            <input id="profile-avatar" name="avatar" type="file" required
                                   accept="image/jpeg,image/png,image/webp"
                                   class="mp-file-input">
                            <p class="mp-hint mt-1">{{ __('JPG, PNG o WEBP. Máximo 4 MB.') }}</p>
                        </div>
                        <button type="submit" class="mp-btn mp-btn-primary shrink-0">
                            <x-icon name="add_photo_alternate" class="text-[20px]" />
                            {{ $user->avatar_url ? __('Cambiar foto') : __('Subir foto') }}
                        </button>
                    </form>

                    @if ($user->avatar_url)
                        <form method="POST" action="{{ route('settings.profile.avatar.destroy') }}" class="mt-2">
                            @csrf
                            @method('DELETE')
                            <button type="submit" class="mp-btn mp-btn-quiet">
                                <x-icon name="delete" class="text-[20px]" />
                                {{ __('Quitar foto') }}
                            </button>
                        </form>
                    @endif
                </div>
            </div>
        </section>

        <section class="mp-card mp-card-pad">
            <div class="flex items-start gap-4">
                <span class="mp-icon-tile">
                    <x-icon name="person" class="text-[24px]" />
                </span>
                <div>
                    <h2 class="mp-title-sm">{{ __('Datos de la cuenta') }}</h2>
                    <p class="mp-hint mt-1">{{ __('Este nombre y correo identifican tu cuenta. No cambian las llaves de tu bóveda.') }}</p>
                </div>
            </div>

            @if ($errors->updateProfileInformation->any())
                <div class="mp-alert mp-alert-error mt-4" role="alert">
                    <x-icon name="error" class="mt-0.5 text-[21px]" />
                    <ul class="list-inside list-disc space-y-1">
                        @foreach ($errors->updateProfileInformation->all() as $error)
                            <li>{{ $error }}</li>
                        @endforeach
                    </ul>
                </div>
            @endif

            <form method="POST" action="{{ route('user-profile-information.update') }}" class="mt-5 grid gap-4 sm:grid-cols-2">
                @csrf
                @method('PUT')

                <div>
                    <label for="name" class="mp-label">{{ __('Nombre o alias') }}</label>
                    <input id="name" name="name" type="text" value="{{ old('name', $user->name) }}" required
                           autocomplete="name" class="mp-input">
                </div>

                <div>
                    <label for="profile-email" class="mp-label">{{ __('Correo') }}</label>
                    <input id="profile-email" name="email" type="email" value="{{ old('email', $user->email) }}" required
                           autocomplete="email" inputmode="email" class="mp-input">
                </div>

                <div class="sm:col-span-2 flex justify-end">
                    <button type="submit" class="mp-btn mp-btn-primary">
                        <x-icon name="save" class="text-[20px]" />
                        {{ __('Guardar datos') }}
                    </button>
                </div>
            </form>
        </section>

        <section class="mp-card mp-card-pad mt-5">
            <div class="flex items-start gap-4">
                <span class="mp-icon-tile">
                    <x-icon name="password" class="text-[24px]" />
                </span>
                <div>
                    <h2 class="mp-title-sm">{{ __('Contraseña de la cuenta') }}</h2>
                    <p class="mp-hint mt-1">{{ __('Es la contraseña con la que entras a la web. No es la frase que descifra tu bóveda.') }}</p>
                </div>
            </div>

            @if ($errors->updatePassword->any())
                <div class="mp-alert mp-alert-error mt-4" role="alert">
                    <x-icon name="error" class="mt-0.5 text-[21px]" />
                    <ul class="list-inside list-disc space-y-1">
                        @foreach ($errors->updatePassword->all() as $error)
                            <li>{{ $error }}</li>
                        @endforeach
                    </ul>
                </div>
            @endif

            <form method="POST" action="{{ route('user-password.update') }}" class="mt-5 grid gap-4 sm:grid-cols-3">
                @csrf
                @method('PUT')

                <div>
                    <label for="current_password" class="mp-label">{{ __('Contraseña actual') }}</label>
                    <input id="current_password" name="current_password" type="password" required
                           autocomplete="current-password" class="mp-input">
                </div>

                <div>
                    <label for="new_password" class="mp-label">{{ __('Contraseña nueva') }}</label>
                    <input id="new_password" name="password" type="password" required minlength="12"
                           autocomplete="new-password" class="mp-input">
                </div>

                <div>
                    <label for="new_password_confirmation" class="mp-label">{{ __('Repite la contraseña') }}</label>
                    <input id="new_password_confirmation" name="password_confirmation" type="password" required
                           autocomplete="new-password" class="mp-input">
                </div>

                <p class="mp-hint sm:col-span-3">{{ __('Mínimo 12 caracteres, con al menos una letra y un número.') }}</p>

                <div class="sm:col-span-3 flex justify-end">
                    <button type="submit" class="mp-btn mp-btn-primary">
                        <x-icon name="lock_reset" class="text-[20px]" />
                        {{ __('Cambiar contraseña') }}
                    </button>
                </div>
            </form>
        </section>

        <section class="mp-card mp-card-pad mt-5">
            <div class="flex items-start gap-4">
                <span class="mp-icon-tile">
                    <x-icon name="logout" class="text-[24px]" />
                </span>
                <div>
                    <h2 class="mp-title-sm">{{ __('Sesión y dispositivos') }}</h2>
                    <p class="mp-hint mt-1">{{ __('Cierra esta sesión o revisa los celulares autorizados para abrir tu bóveda.') }}</p>
                </div>
            </div>

            <div class="mt-5 flex flex-wrap gap-2">
                <a href="{{ route('vault', ['tab' => 'devices']) }}" class="mp-btn mp-btn-secondary">
                    <x-icon name="devices" class="text-[20px]" />
                    {{ __('Revisar dispositivos') }}
                </a>
                <form method="POST" action="{{ route('logout') }}">
                    @csrf
                    <button type="submit" class="mp-btn mp-btn-quiet">
                        <x-icon name="logout" class="text-[20px]" />
                        {{ __('Cerrar sesión') }}
                    </button>
                </form>
            </div>
        </section>

        <section class="mt-8 rounded-2xl border border-error/30 bg-error-container/30 p-5 sm:p-6">
            <div class="flex items-start gap-4">
                <span class="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-error-container text-on-error-container">
                    <x-icon name="delete_forever" class="text-[25px]" />
                </span>
                <div class="min-w-0 flex-1">
                    <h2 class="mp-title-sm text-on-error-container">{{ __('Eliminar mi cuenta') }}</h2>
                    <p class="mp-body-sm mt-1 text-on-surface-variant">{{ __('Se borrarán tu cuenta, tus documentos cifrados y tus dispositivos. Esta acción no se puede deshacer y no permite recuperar la bóveda.') }}</p>

                    @if ($errors->any() && ! $errors->updateProfileInformation->any() && ! $errors->updatePassword->any())
                        <p class="mp-alert mp-alert-error mt-3" role="alert">{{ $errors->first() }}</p>
                    @endif

                    <form method="POST" action="{{ route('settings.account.destroy') }}" class="mt-4 flex flex-col gap-2 sm:flex-row sm:items-end">
                        @csrf
                        @method('DELETE')
                        <div class="flex-1">
                            <label for="delete-password" class="mp-label">{{ __('Confirma con tu contraseña') }}</label>
                            <input id="delete-password" name="password" type="password" required
                                   autocomplete="current-password" class="mp-input">
                        </div>
                        <button type="submit" class="mp-btn mp-btn-danger shrink-0"
                                data-confirm="{{ __('¿Eliminar tu cuenta y todos tus datos? Esta acción no se puede deshacer.') }}">
                            <x-icon name="delete_forever" class="text-[20px]" />
                            {{ __('Eliminar cuenta') }}
                        </button>
                    </form>
                </div>
            </div>
        </section>
    </div>
</x-layout>
