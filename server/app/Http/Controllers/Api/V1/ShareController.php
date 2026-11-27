<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Share;
use App\Models\ShareRequest;
use App\Services\AppNotifications;
use App\Services\AuditLogger;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class ShareController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $incoming = $request->user()->sharesReceived()
            ->with(['sender', 'item'])
            ->whereIn('status', ['pending', 'accepted'])
            ->orderByDesc('created_at')
            ->get()
            ->map(fn (Share $share) => $this->payload($share, $request->user()->id));

        $outgoing = $request->user()->sharesSent()
            ->with(['recipient', 'item'])
            ->orderByDesc('created_at')
            ->get()
            ->map(fn (Share $share) => $this->payload($share, $request->user()->id));

        return response()->json([
            'incoming' => $incoming,
            'outgoing' => $outgoing,
        ]);
    }

    public function store(Request $request, AuditLogger $audit): JsonResponse
    {
        $data = $request->validate([
            'contact_id' => ['required', 'uuid'],
            'item_id' => ['required', 'uuid'],
            'mode' => ['required', Rule::in(['copy', 'transfer'])],
            'encrypted_package' => ['required', 'string', 'max:30000000'],
            'expires_at' => ['nullable', 'date', 'after:now'],
        ]);

        $contact = $request->user()->contacts()
            ->where('status', 'accepted')
            ->findOrFail($data['contact_id']);

        $item = $request->user()->items()->findOrFail($data['item_id']);

        $expiresAt = $data['expires_at'] ?? null;

        $shareRequest = ShareRequest::where('requester_id', $contact->contact_user_id)
            ->where('target_id', $request->user()->id)
            ->where('status', 'pending')
            ->where(function ($query) {
                $query->whereNull('expires_at')
                    ->orWhere('expires_at', '>', now());
            })
            ->latest()
            ->first();

        $share = Share::create([
            'sender_id' => $request->user()->id,
            'recipient_id' => $contact->contact_user_id,
            'share_request_id' => $shareRequest?->id,
            'item_id' => $item->id,
            'mode' => $data['mode'],
            'status' => 'pending',
            'encrypted_package' => $data['encrypted_package'],
            'download_allowed' => true,
            'expires_at' => $expiresAt,
        ]);

        if ($data['mode'] === 'transfer') {
            $item->forceFill([
                'state' => 'transferred',
                'transferred_to_user_id' => $contact->contact_user_id,
                'transferred_at' => now(),
            ])->save();
        }

        AppNotifications::create($contact->contactUser, 'share_received', [
            'share_id' => $share->id,
            'item_id' => $item->id,
            'item_type' => $item->type,
            'mode' => $share->mode,
            'expires_at' => $share->expires_at?->toIso8601String(),
            'from' => $request->user()->name,
        ], url: '/vault?tab=family&share='.$share->id);

        $audit->log($request->user(), 'share_sent', [
            'share_id' => $share->id,
            'mode' => $share->mode,
            'item_type' => $item->type,
        ]);

        return response()->json(['data' => $this->payload($share, $request->user()->id)], 201);
    }

    public function show(Request $request, string $share): JsonResponse
    {
        $share = Share::where(function ($query) use ($request) {
            $query->where('sender_id', $request->user()->id)
                ->orWhere('recipient_id', $request->user()->id);
        })
            ->with(['sender', 'recipient', 'item'])
            ->findOrFail($share);

        return response()->json([
            'data' => $this->payload($share, $request->user()->id) + [
                'encrypted_package' => $share->encrypted_package,
            ],
        ]);
    }

    public function accept(Request $request, string $share, AuditLogger $audit): JsonResponse
    {
        $data = $request->validate([
            'recipient_item_id' => ['required', 'uuid'],
        ]);

        $share = $request->user()->sharesReceived()->findOrFail($share);

        if ($share->status !== 'pending') {
            return response()->json(['message' => __('Este envío ya fue procesado.')], 422);
        }

        if ($share->expires_at && $share->expires_at->isPast()) {
            $share->forceFill(['status' => 'expired'])->save();

            return response()->json(['message' => __('Este envío venció.')], 422);
        }

        $item = $request->user()->items()->findOrFail($data['recipient_item_id']);

        $share->forceFill([
            'recipient_item_id' => $item->id,
            'status' => 'accepted',
            'accepted_at' => now(),
        ])->save();

        if ($share->share_request_id) {
            ShareRequest::whereKey($share->share_request_id)->update([
                'status' => 'fulfilled',
                'responded_at' => now(),
            ]);
        }

        AppNotifications::create($share->sender, 'share_accepted', [
            'share_id' => $share->id,
            'item_id' => $share->item_id,
            'from' => $request->user()->name,
        ], url: '/vault?tab=family&outbox=1');

        $audit->log($request->user(), 'share_accepted', [
            'share_id' => $share->id,
        ]);

        return response()->json(['data' => $this->payload($share, $request->user()->id)]);
    }

    public function decline(Request $request, string $share, AuditLogger $audit): JsonResponse
    {
        $share = $request->user()->sharesReceived()->findOrFail($share);

        if ($share->status !== 'pending') {
            return response()->json(['message' => __('Este envío ya fue procesado.')], 422);
        }

        $share->forceFill(['status' => 'declined'])->save();

        $audit->log($request->user(), 'share_declined', [
            'share_id' => $share->id,
        ]);

        return response()->json(['message' => __('Envío rechazado.')]);
    }

    public function revoke(Request $request, string $share, AuditLogger $audit): JsonResponse
    {
        $share = $request->user()->sharesSent()->findOrFail($share);

        if ($share->status === 'accepted') {
            return response()->json([
                'message' => __('El destinatario ya aceptó una copia; no se puede borrar de su dispositivo.'),
            ], 422);
        }

        $share->forceFill(['status' => 'revoked', 'revoked_at' => now()])->save();

        if ($share->mode === 'transfer' && $share->item) {
            $share->item->forceFill([
                'state' => 'active',
                'transferred_to_user_id' => null,
                'transferred_at' => null,
            ])->save();
        }

        $audit->log($request->user(), 'share_revoked', [
            'share_id' => $share->id,
        ]);

        return response()->json(['message' => __('Envío cancelado.')]);
    }

    private function payload(Share $share, int $userId): array
    {
        return [
            'id' => $share->id,
            'status' => $share->status,
            'mode' => $share->mode,
            'direction' => $share->sender_id === $userId ? 'outgoing' : 'incoming',
            'sender' => $share->sender?->name,
            'recipient' => $share->recipient?->name,
            'item_id' => $share->item_id,
            'recipient_item_id' => $share->recipient_item_id,
            'item_type' => $share->item?->type,
            'share_request_id' => $share->share_request_id,
            'expires_at' => $share->expires_at?->toIso8601String(),
            'accepted_at' => $share->accepted_at?->toIso8601String(),
            'revoked_at' => $share->revoked_at?->toIso8601String(),
            'created_at' => $share->created_at?->toIso8601String(),
        ];
    }
}
