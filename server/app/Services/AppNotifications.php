<?php

namespace App\Services;

use App\Models\User;
use App\Models\UserNotification;

class AppNotifications
{
    public static function create(
        User $user,
        string $type,
        array $data = [],
        ?string $title = null,
        ?string $body = null,
        ?string $url = null,
    ): UserNotification {
        $notification = UserNotification::create([
            'user_id' => $user->id,
            'type' => $type,
            'data' => $data,
        ]);

        [$defaultTitle, $defaultBody, $defaultUrl] = self::message($user, $type, $data);

        app(WebPushService::class)->send($user, [
            'title' => $title ?? $defaultTitle,
            'body' => $body ?? $defaultBody,
            'url' => $url ?? $defaultUrl,
            'tag' => $type.':'.$notification->id,
            'type' => $type,
            'notification_id' => $notification->id,
            'icon' => '/images/favicon-180.png',
        ]);

        return $notification;
    }

    /**
     * @return array{0:string,1:string,2:string}
     */
    private static function message(User $user, string $type, array $data): array
    {
        $locale = str_starts_with((string) ($user->locale ?? ''), 'en') ? 'en' : 'es';
        $name = $data['from'] ?? trans('Alguien', [], $locale);
        $document = $data['document_type'] ?? trans('Documento', [], $locale);
        $version = $data['version_name'] ?? '';

        [$title, $body, $url] = match ($type) {
            'contact_request' => [
                trans('Solicitud de contacto', [], $locale),
                trans(':name quiere agregarte como contacto.', ['name' => $name], $locale),
                '/vault?tab=family&contact='.($data['contact_id'] ?? ''),
            ],
            'contact_accepted' => [
                trans('Contacto aceptado', [], $locale),
                trans(':name aceptó tu solicitud de contacto.', ['name' => $name], $locale),
                '/vault?tab=family',
            ],
            'share_received' => [
                trans('Pase compartido contigo', [], $locale),
                trans(':name te envió un documento cifrado.', ['name' => $name], $locale),
                '/vault?tab=family&share='.($data['share_id'] ?? ''),
            ],
            'share_accepted' => [
                trans('Envío aceptado', [], $locale),
                trans(':name aceptó el documento que le enviaste.', ['name' => $name], $locale),
                '/vault?tab=family&outbox=1',
            ],
            'document_request' => [
                trans('Solicitud de documento', [], $locale),
                trans(':name te pidió un documento: :type.', ['name' => $name, 'type' => $document], $locale),
                '/vault?tab=family&request='.($data['request_id'] ?? ''),
            ],
            'app_update' => [
                trans('Nueva versión de My Pass', [], $locale),
                trans('La versión :version ya está disponible. Toca para ver los cambios.', ['version' => $version], $locale),
                '/notifications',
            ],
            'vault_recovery_enabled', 'vault_recovery_disabled', 'vault_recovery_used' => [
                trans('Seguridad de tu bóveda', [], $locale),
                $data['message'] ?? trans('Revisa la seguridad de tu bóveda.', [], $locale),
                '/notifications',
            ],
            default => [
                trans('Actividad de la bóveda', [], $locale),
                trans('Tienes actividad nueva en tu bóveda.', [], $locale),
                '/notifications',
            ],
        };

        return [$title, $body, $url];
    }
}
