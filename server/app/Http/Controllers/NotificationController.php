<?php

namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;
use App\Services\AppUpdateAnnouncement;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class NotificationController extends Controller
{
    public function index(Request $request, AppUpdateAnnouncement $announcement): View
    {
        $announcement->ensureFor($request->user());

        $notifications = $request->user()->userNotifications()
            ->orderByDesc('created_at')
            ->limit(100)
            ->get();

        return view('notifications.index', [
            'notifications' => $notifications,
            'unreadCount' => $notifications->whereNull('read_at')->count(),
        ]);
    }

    public function markRead(Request $request, string $notification): RedirectResponse
    {
        $notification = $request->user()->userNotifications()->findOrFail($notification);

        if ($notification->read_at === null) {
            $notification->forceFill(['read_at' => now()])->save();
        }

        return back();
    }

    public function markAllRead(Request $request): RedirectResponse
    {
        $request->user()->userNotifications()
            ->whereNull('read_at')
            ->update(['read_at' => now()]);

        return back()->with('status', __('Notificaciones marcadas como leídas.'));
    }

    public function json(Request $request): JsonResponse
    {
        $notifications = $request->user()->userNotifications()
            ->orderByDesc('created_at')
            ->limit(100)
            ->get()
            ->map(fn ($notification) => [
                'id' => $notification->id,
                'type' => $notification->type,
                'data' => $notification->data ?? [],
                'read_at' => $notification->read_at?->toIso8601String(),
                'created_at' => $notification->created_at?->toIso8601String(),
            ]);

        return response()->json([
            'data' => $notifications,
            'unread' => $notifications->whereNull('read_at')->count(),
        ]);
    }

    public function markReadJson(Request $request, string $notification): JsonResponse
    {
        $notification = $request->user()->userNotifications()->findOrFail($notification);

        if ($notification->read_at === null) {
            $notification->forceFill(['read_at' => now()])->save();
        }

        return response()->json(['data' => ['id' => $notification->id]]);
    }
}
