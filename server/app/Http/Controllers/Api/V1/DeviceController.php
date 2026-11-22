<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\Device;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

class DeviceController extends Controller
{
    public function index(Request $request): JsonResponse
    {
        $devices = $request->user()->devices()
            ->whereNull('revoked_at')
            ->orderByDesc('last_seen_at')
            ->get()
            ->map(fn (Device $device) => $this->payload($device));

        return response()->json(['data' => $devices]);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:120'],
            'platform' => ['required', Rule::in(['android', 'web'])],
            'public_key' => ['required', 'string'],
            'wrapped_master_key' => ['required', 'string'],
            'pin_salt' => ['nullable', 'string'],
            'pin_params' => ['nullable', 'array'],
            'biometric_wrapped_master_key' => ['nullable', 'string'],
            'app_version' => ['nullable', 'string', 'max:40'],
        ]);

        $device = $request->user()->devices()->create($data + [
            'last_seen_at' => now(),
        ]);

        return response()->json(['data' => $this->payload($device)], 201);
    }

    public function show(Request $request, string $device): JsonResponse
    {
        $device = $request->user()->devices()->findOrFail($device);

        if (! $device->isRevoked()) {
            $device->forceFill(['last_seen_at' => now()])->save();
        }

        return response()->json([
            'data' => $this->payload($device) + [
                'revoked' => $device->revoked_at !== null,
            ],
        ]);
    }

    public function revoke(Request $request, string $device): JsonResponse
    {
        $device = $request->user()->devices()->findOrFail($device);
        $device->forceFill(['revoked_at' => now()])->save();

        return response()->json(['message' => __('Celular bloqueado.')]);
    }

    private function payload(Device $device): array
    {
        return [
            'id' => $device->id,
            'name' => $device->name,
            'platform' => $device->platform,
            'last_seen_at' => $device->last_seen_at?->toIso8601String(),
            'revoked_at' => $device->revoked_at?->toIso8601String(),
        ];
    }
}
