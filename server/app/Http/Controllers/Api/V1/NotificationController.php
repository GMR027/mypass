<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\UserNotification;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class NotificationController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $notifications = $request->user()->userNotifications()
            ->orderByDesc('created_at')
            ->limit(50)
            ->get()
            ->map(fn (UserNotification $notification) => $this->payload($notification));

        return response()->json([
            'data' => $notifications,
            'unread' => $request->user()->userNotifications()->whereNull('read_at')->count(),
        ]);
    }

    public function markRead(Request $request, string $notification): JsonResponse
    {
        $notification = $request->user()->userNotifications()->findOrFail($notification);
        $notification->forceFill(['read_at' => $notification->read_at ?? now()])->save();

        return response()->json(['data' => $this->payload($notification)]);
    }

    public function markAllRead(Request $request): JsonResponse
    {
        $request->user()->userNotifications()
            ->whereNull('read_at')
            ->update(['read_at' => now()]);

        return response()->json(['message' => __('Notificaciones marcadas como leídas.')]);
    }

    /**
     * @return array<string, mixed>
     */
    private function payload(UserNotification $notification): array
    {
        return [
            'id' => $notification->id,
            'type' => $notification->type,
            'data' => $notification->data,
            'read_at' => $notification->read_at?->toIso8601String(),
            'created_at' => $notification->created_at?->toIso8601String(),
        ];
    }
}
