<?php

namespace App\Services;

use App\Models\PushSubscription;
use App\Models\User;
use Illuminate\Support\Facades\Log;

class WebPushService
{
    public function send(User $user, array $payload): void
    {
        if (! config('webpush.enabled')) {
            return;
        }

        $publicKey = (string) config('webpush.vapid.public_key');
        $privateKey = (string) config('webpush.vapid.private_key');

        if ($publicKey === '' || $privateKey === '') {
            Log::warning('Web Push habilitado sin llaves VAPID configuradas.');

            return;
        }

        if (! class_exists(\Minishlink\WebPush\WebPush::class)) {
            Log::warning('La librería de Web Push no está instalada; se omite el envío.');

            return;
        }

        $subscriptions = $user->pushSubscriptions()->get();

        if ($subscriptions->isEmpty()) {
            return;
        }

        $auth = [
            'VAPID' => [
                'subject' => (string) config('webpush.vapid.subject'),
                'publicKey' => $publicKey,
                'privateKey' => $privateKey,
            ],
        ];

        try {
            $webPush = new \Minishlink\WebPush\WebPush($auth);

            foreach ($subscriptions as $subscription) {
                $webPush->queueNotification(
                    new \Minishlink\WebPush\Subscription(
                        $subscription->endpoint,
                        $subscription->public_key,
                        $subscription->auth_token,
                    ),
                    json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
                );
            }

            foreach ($webPush->flush() as $report) {
                if ($report->isSuccess()) {
                    continue;
                }

                $status = $report->getResponse()?->getStatusCode();

                if (in_array($status, [404, 410], true)) {
                    PushSubscription::where('endpoint', $report->getEndpoint())->delete();
                } else {
                    Log::info('No se pudo entregar una notificación Web Push.', [
                        'status' => $status,
                        'reason' => $report->getReason(),
                    ]);
                }
            }
        } catch (\Throwable $error) {
            Log::warning('Error enviando Web Push: '.$error->getMessage());
        }
    }
}
