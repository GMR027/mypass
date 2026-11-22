<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\ShareRequest;
use App\Models\UserNotification;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class ShareRequestController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $incoming = $request->user()->shareRequestsReceived()
            ->with('requester')
            ->orderByDesc('created_at')
            ->get()
            ->map(fn (ShareRequest $shareRequest) => $this->payload($shareRequest, $request->user()->id));

        $outgoing = $request->user()->shareRequestsMade()
            ->with('target')
            ->orderByDesc('created_at')
            ->get()
            ->map(fn (ShareRequest $shareRequest) => $this->payload($shareRequest, $request->user()->id));

        return response()->json([
            'incoming' => $incoming,
            'outgoing' => $outgoing,
        ]);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'contact_id' => ['required', 'uuid'],
            'document_type' => ['required', 'string', 'max:60'],
            'purpose' => ['required', 'string', 'max:255'],
            'expires_at' => ['nullable', 'date', 'after:now'],
        ]);

        $contact = $request->user()->contacts()
            ->where('status', 'accepted')
            ->findOrFail($data['contact_id']);

        $shareRequest = ShareRequest::create([
            'requester_id' => $request->user()->id,
            'target_id' => $contact->contact_user_id,
            'document_type' => $data['document_type'],
            'purpose' => $data['purpose'],
            'status' => 'pending',
            'expires_at' => $data['expires_at'] ?? null,
        ]);

        UserNotification::create([
            'user_id' => $contact->contact_user_id,
            'type' => 'document_request',
            'data' => [
                'request_id' => $shareRequest->id,
                'from' => $request->user()->name,
                'document_type' => $data['document_type'],
            ],
        ]);

        return response()->json(['data' => $this->payload($shareRequest, $request->user()->id)], 201);
    }

    public function decline(Request $request, string $shareRequest): JsonResponse
    {
        $shareRequest = $request->user()->shareRequestsReceived()->findOrFail($shareRequest);

        if ($shareRequest->status !== 'pending') {
            return response()->json(['message' => __('Esta solicitud ya fue procesada.')], 422);
        }

        $shareRequest->forceFill([
            'status' => 'declined',
            'responded_at' => now(),
        ])->save();

        return response()->json(['message' => __('Solicitud rechazada.')]);
    }

    public function cancel(Request $request, string $shareRequest): JsonResponse
    {
        $shareRequest = $request->user()->shareRequestsMade()->findOrFail($shareRequest);

        if ($shareRequest->status !== 'pending') {
            return response()->json(['message' => __('Esta solicitud ya fue procesada.')], 422);
        }

        $shareRequest->forceFill([
            'status' => 'cancelled',
            'responded_at' => now(),
        ])->save();

        return response()->json(['message' => __('Solicitud cancelada.')]);
    }

    private function payload(ShareRequest $shareRequest, int $userId): array
    {
        return [
            'id' => $shareRequest->id,
            'status' => $shareRequest->status,
            'direction' => $shareRequest->requester_id === $userId ? 'outgoing' : 'incoming',
            'requester' => $shareRequest->requester?->name,
            'target' => $shareRequest->target?->name,
            'document_type' => $shareRequest->document_type,
            'purpose' => $shareRequest->purpose,
            'expires_at' => $shareRequest->expires_at?->toIso8601String(),
            'created_at' => $shareRequest->created_at?->toIso8601String(),
        ];
    }
}
