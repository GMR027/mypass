<x-layout :title="__('Notificaciones')">
    <div class="mx-auto max-w-3xl">
        <div class="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
            <div>
                <p class="mp-eyebrow">{{ __('Actividad') }}</p>
                <h1 class="mp-title-lg mt-2">{{ __('Notificaciones') }}</h1>
                <p class="mp-body mt-2">{{ __('Contactos, envíos y solicitudes de tu bóveda.') }}</p>
            </div>
            @if ($unreadCount > 0)
                <form method="POST" action="{{ route('notifications.read-all') }}">
                    @csrf
                    <button type="submit" class="mp-btn mp-btn-secondary shrink-0">
                        <x-icon name="mark_email_read" class="text-[20px]" />
                        {{ __('Marcar todo como leído') }}
                    </button>
                </form>
            @endif
        </div>

        @if (session('status'))
            <p class="mp-alert mp-alert-success mb-5" role="status">{{ session('status') }}</p>
        @endif

        <section id="push-notifications" class="mp-card mp-card-pad mb-5" hidden>
            <div class="flex items-start gap-4">
                <span class="mp-icon-tile shrink-0">
                    <x-icon name="notifications_active" class="text-[24px]" />
                </span>
                <div class="min-w-0 flex-1">
                    <h2 class="mp-title-sm">{{ __('Avisos en este dispositivo') }}</h2>
                    <p class="mp-hint mt-1">{{ __('Recibe avisos de envíos, contactos y actualizaciones aunque la app esté cerrada.') }}</p>
                    <p id="push-status" class="mp-hint mt-2" role="status"></p>
                    <div class="mt-3 flex flex-wrap gap-2">
                        <button id="push-enable" type="button" class="mp-btn mp-btn-primary mp-btn-sm" hidden>
                            <x-icon name="notifications_active" class="text-[19px]" />
                            {{ __('Activar avisos') }}
                        </button>
                        <button id="push-disable" type="button" class="mp-btn mp-btn-quiet mp-btn-sm" hidden>
                            {{ __('Desactivar en este dispositivo') }}
                        </button>
                    </div>
                </div>
            </div>
        </section>

        @if ($notifications->isEmpty())
            <div class="mp-empty">
                <span class="flex h-16 w-16 items-center justify-center rounded-full bg-surface-container-high text-outline">
                    <x-icon name="notifications_off" class="text-[32px]" />
                </span>
                <h2 class="mp-title-sm mt-4">{{ __('No tienes notificaciones') }}</h2>
                <p class="mp-hint mt-2">{{ __('Aquí aparecerán los contactos, envíos y solicitudes de tu bóveda.') }}</p>
            </div>
        @else
            <ul class="space-y-3">
                @foreach ($notifications as $notification)
                    @php
                        $data = $notification->data ?? [];
                        $type = $notification->type;
                        $icon = match ($type) {
                            'contact_request' => 'person_add',
                            'contact_accepted' => 'verified_user',
                            'share_received' => 'forward_to_inbox',
                            'share_accepted' => 'mark_email_read',
                            'document_request' => 'contact_support',
                            'vault_recovery_enabled' => 'mark_email_read',
                            'vault_recovery_disabled' => 'link_off',
                            'vault_recovery_used' => 'lock_open',
                            'insurance_expiring' => 'shield',
                            'app_update' => 'system_update',
                            default => 'notifications',
                        };
                        $title = match ($type) {
                            'contact_request' => __('Solicitud de contacto'),
                            'contact_accepted' => __('Contacto aceptado'),
                            'share_received' => __('Documento compartido contigo'),
                            'share_accepted' => __('Envío aceptado'),
                            'document_request' => __('Solicitud de documento'),
                            'vault_recovery_enabled' => __('Recuperación por correo activada'),
                            'vault_recovery_disabled' => __('Recuperación por correo desactivada'),
                            'vault_recovery_used' => __('Bóveda abierta con recuperación por correo'),
                            'insurance_expiring' => __('Seguro por vencer'),
                            'app_update' => __('Nueva versión disponible'),
                            default => __('Actividad de la bóveda'),
                        };
                        $description = match ($type) {
                            'contact_request' => __(':name quiere agregarte como contacto.', ['name' => $data['from'] ?? __('Alguien')]),
                            'contact_accepted' => __(':name aceptó tu solicitud de contacto.', ['name' => $data['from'] ?? __('Alguien')]),
                            'share_received' => __(':name te envió un documento cifrado.', ['name' => $data['from'] ?? __('Alguien')]),
                            'share_accepted' => __(':name aceptó el documento que le enviaste.', ['name' => $data['from'] ?? __('Alguien')]),
                            'document_request' => __(':name te pidió un documento: :type.', ['name' => $data['from'] ?? __('Alguien'), 'type' => $data['document_type'] ?? __('Documento')]),
                            'vault_recovery_enabled', 'vault_recovery_disabled', 'vault_recovery_used' => $data['message'] ?? __('Revisa la seguridad de tu bóveda.'),
                            'insurance_expiring' => ($data['days'] ?? 0) === 0
                                ? __('Tu póliza vence hoy.')
                                : __('Tu póliza vence en :days días.', ['days' => $data['days'] ?? 0]),
                            'app_update' => __('My Pass :version ya está disponible. Revisa los cambios y actualiza cuando quieras.', ['version' => $data['version_name'] ?? '']),
                            default => __('Tienes actividad nueva en tu bóveda.'),
                        };
                        $actionUrl = match ($type) {
                            'contact_request' => route('vault', ['tab' => 'family', 'contact' => $data['contact_id'] ?? '']),
                            'share_received' => route('vault', ['tab' => 'family', 'share' => $data['share_id'] ?? '']),
                            'document_request' => route('vault', ['tab' => 'family', 'request' => $data['request_id'] ?? '']),
                            'share_accepted' => route('vault', ['tab' => 'family', 'outbox' => 1]),
                            'insurance_expiring' => route('vault', ['tab' => 'insurance']),
                            'app_update' => $data['apk_url'] ?? route('app.download', ['v' => $data['version_code'] ?? $androidVersionCode]),
                            default => null,
                        };
                        $actionLabel = match ($type) {
                            'app_update' => __('Descargar actualización'),
                            'contact_request', 'share_received', 'document_request', 'share_accepted', 'insurance_expiring' => __('Revisar'),
                            default => null,
                        };
                        $notes = $type === 'app_update' && is_array($data['notes'] ?? null)
                            ? ($data['notes'][str_starts_with(app()->getLocale(), 'en') ? 'en' : 'es'] ?? [])
                            : [];
                    @endphp
                    <li @class(['mp-card flex flex-col gap-4 p-5 sm:flex-row sm:items-start', 'border-primary/30' => $notification->read_at === null])>
                        <span class="mp-icon-tile shrink-0">
                            <x-icon :name="$icon" class="text-[23px]" />
                        </span>
                        <div class="min-w-0 flex-1">
                            <div class="flex flex-wrap items-center gap-2">
                                <h2 class="font-semibold text-on-surface">{{ $title }}</h2>
                                @if ($notification->read_at === null)
                                    <span class="mp-badge mp-badge-primary">{{ __('Nueva') }}</span>
                                @endif
                                <span class="text-sm text-on-surface-variant">{{ $notification->created_at?->diffForHumans() }}</span>
                            </div>
                            <p class="mp-body-sm mt-1">{{ $description }}</p>
                            @if ($notes !== [])
                                <ul class="mt-2 list-inside list-disc space-y-1 text-sm text-on-surface-variant">
                                    @foreach (array_slice($notes, 0, 4) as $note)
                                        <li>{{ $note }}</li>
                                    @endforeach
                                </ul>
                            @endif
                            @if ($actionUrl && $actionLabel)
                                <a href="{{ $actionUrl }}" class="mp-btn mp-btn-secondary mp-btn-sm mt-3"
                                   @if ($type === 'app_update') download @endif>
                                    <x-icon name="{{ $type === 'app_update' ? 'download' : 'arrow_forward' }}" class="text-[18px]" />
                                    {{ $actionLabel }}
                                </a>
                            @endif
                        </div>
                        @if ($notification->read_at === null)
                            <form method="POST" action="{{ route('notifications.read', $notification) }}" class="shrink-0">
                                @csrf
                                <button type="submit" class="mp-btn mp-btn-quiet mp-btn-sm">
                                    {{ __('Marcar leída') }}
                                </button>
                            </form>
                        @endif
                    </li>
                @endforeach
            </ul>
        @endif
    </div>
</x-layout>
