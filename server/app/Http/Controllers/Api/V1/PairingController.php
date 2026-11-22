<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\PairingSession;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class PairingController extends Controller
{
    public function create(Request $request): JsonResponse
    {
        $user = $request->user();

        PairingSession::where('user_id', $user->id)
            ->whereIn('status', ['pending', 'joined'])
            ->where('expires_at', '>', now())
            ->update(['status' => 'expired']);

        $code = str_pad((string) random_int(0, 999999), 6, '0', STR_PAD_LEFT);
        $session = PairingSession::create([
            'user_id' => $user->id,
            'purpose' => 'pair_device',
            'code_hash' => $this->hashCode($code),
            'status' => 'pending',
            'expires_at' => now()->addMinutes(10),
        ]);

        return response()->json([
            'data' => [
                'id' => $session->id,
                'code' => $code,
                'expires_at' => $session->expires_at->toIso8601String(),
            ],
        ], 201);
    }

    public function show(Request $request, string $session): JsonResponse
    {
        $pairing = $this->findForUser($request, $session);

        if ($this->isExpired($pairing)) {
            return response()->json([
                'data' => [
                    'id' => $pairing->id,
                    'status' => 'expired',
                ],
            ]);
        }

        $payload = [
            'id' => $pairing->id,
            'status' => $pairing->status,
            'expires_at' => $pairing->expires_at->toIso8601String(),
        ];

        if ($pairing->status === 'joined') {
            $payload['public_key'] = $pairing->public_key;
            $payload['device_name'] = $pairing->device_name;
        }

        return response()->json(['data' => $payload]);
    }

    public function approve(Request $request, string $session): JsonResponse
    {
        $data = $request->validate([
            'encrypted_master_key' => ['required', 'string'],
        ]);

        $pairing = $this->findForUser($request, $session);

        if ($this->isExpired($pairing)) {
            return response()->json(['message' => __('El código venció. Genera uno nuevo.')], 422);
        }

        if ($pairing->status !== 'joined') {
            return response()->json([
                'message' => __('El celular todavía no se ha conectado con el código.'),
            ], 422);
        }

        $pairing->forceFill([
            'payload' => $data['encrypted_master_key'],
            'status' => 'approved',
            'approved_at' => now(),
        ])->save();

        return response()->json([
            'data' => [
                'id' => $pairing->id,
                'status' => 'approved',
            ],
        ]);
    }

    public function join(Request $request): JsonResponse
    {
        $data = $request->validate([
            'code' => ['required', 'string', 'regex:/^\d{6}$/'],
            'public_key' => ['required', 'string'],
            'device_name' => ['required', 'string', 'max:120'],
        ]);

        PairingSession::where('user_id', $request->user()->id)
            ->whereIn('status', ['pending', 'joined'])
            ->where('expires_at', '<=', now())
            ->update(['status' => 'expired']);

        $candidates = PairingSession::where('user_id', $request->user()->id)
            ->whereIn('status', ['pending', 'joined'])
            ->where('expires_at', '>', now())
            ->orderByDesc('created_at')
            ->get();

        $pairing = $candidates->first(
            fn (PairingSession $candidate) => hash_equals(
                $candidate->code_hash,
                $this->hashCode($data['code']),
            )
        );

        if (! $pairing) {
            return response()->json([
                'message' => __('El código no es válido o venció.'),
            ], 422);
        }

        $pairing->forceFill([
            'public_key' => $data['public_key'],
            'device_name' => $data['device_name'],
            'status' => 'joined',
            'joined_at' => now(),
        ])->save();

        return response()->json([
            'data' => [
                'id' => $pairing->id,
                'status' => 'joined',
            ],
        ]);
    }

    public function androidShow(Request $request, string $session): JsonResponse
    {
        $pairing = $this->findForUser($request, $session);

        if ($this->isExpired($pairing)) {
            return response()->json([
                'data' => ['id' => $pairing->id, 'status' => 'expired'],
            ]);
        }

        $payload = [
            'id' => $pairing->id,
            'status' => $pairing->status,
        ];

        if ($pairing->status === 'approved') {
            $payload['encrypted_master_key'] = $pairing->payload;
        }

        return response()->json(['data' => $payload]);
    }

    private function findForUser(Request $request, string $session): PairingSession
    {
        return PairingSession::where('user_id', $request->user()->id)
            ->findOrFail($session);
    }

    private function hashCode(string $code): string
    {
        return hash_hmac('sha256', $code, (string) config('app.key'));
    }

    private function isExpired(PairingSession $session): bool
    {
        if ($session->status === 'expired' || $session->used_at !== null) {
            return true;
        }

        if ($session->expires_at->isPast()) {
            $session->forceFill(['status' => 'expired'])->save();

            return true;
        }

        return false;
    }
}
