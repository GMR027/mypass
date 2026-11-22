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
                            default => 'notifications',
                        };
                        $title = match ($type) {
                            'contact_request' => __('Solicitud de contacto'),
                            'contact_accepted' => __('Contacto aceptado'),
                            'share_received' => __('Documento compartido contigo'),
                            'share_accepted' => __('Envío aceptado'),
                            'document_request' => __('Solicitud de documento'),
                            default => __('Actividad de la bóveda'),
                        };
                        $description = match ($type) {
                            'contact_request' => __(':name quiere agregarte como contacto.', ['name' => $data['from'] ?? __('Alguien')]),
                            'contact_accepted' => __(':name aceptó tu solicitud de contacto.', ['name' => $data['from'] ?? __('Alguien')]),
                            'share_received' => __(':name te envió un documento cifrado.', ['name' => $data['from'] ?? __('Alguien')]),
                            'share_accepted' => __(':name aceptó el documento que le enviaste.', ['name' => $data['from'] ?? __('Alguien')]),
                            'document_request' => __(':name te pidió un documento: :type.', ['name' => $data['from'] ?? __('Alguien'), 'type' => $data['document_type'] ?? __('Documento')]),
                            default => __('Tienes actividad nueva en tu bóveda.'),
                        };
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
