<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\PushSubscription;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class PushSubscriptionController extends Controller
{
    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'endpoint' => ['required', 'url', 'max:500'],
            'keys.p256dh' => ['required', 'string', 'max:255'],
            'keys.auth' => ['required', 'string', 'max:255'],
        ]);

        $subscription = PushSubscription::updateOrCreate(
            [
                'user_id' => $request->user()->id,
                'endpoint' => $data['endpoint'],
            ],
            [
                'public_key' => $data['keys']['p256dh'],
                'auth_token' => $data['keys']['auth'],
                'content_encoding' => 'aes128gcm',
                'user_agent' => mb_substr((string) $request->userAgent(), 0, 255),
            ],
        );

        return response()->json(['data' => ['id' => $subscription->id]], 201);
    }

    public function destroy(Request $request): JsonResponse
    {
        $data = $request->validate([
            'endpoint' => ['required', 'url', 'max:500'],
        ]);

        $request->user()->pushSubscriptions()
            ->where('endpoint', $data['endpoint'])
            ->delete();

        return response()->json(['message' => __('Suscripción eliminada.')]);
    }

    public function show(Request $request): JsonResponse
    {
        return response()->json([
            'enabled' => (bool) config('webpush.enabled'),
            'subscribed' => $request->user()->pushSubscriptions()->exists(),
        ]);
    }
}
