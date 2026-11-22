<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\UserKey;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class KeyController extends Controller
{
    public function show(Request $request): JsonResponse
    {
        $keys = UserKey::where('user_id', $request->user()->id)->first();

        return response()->json([
            'data' => $keys ? $this->payload($keys) : null,
        ]);
    }

    public function update(Request $request): JsonResponse
    {
        $data = $request->validate([
            'pc_wrapped_master_key' => ['nullable', 'string'],
            'pc_kdf_salt' => ['nullable', 'string'],
            'pc_kdf_params' => ['nullable', 'array'],
            'recovery_wrapped_master_key' => ['nullable', 'string'],
            'recovery_nonce' => ['nullable', 'string'],
            'recovery_kdf_salt' => ['nullable', 'string'],
            'recovery_kdf_params' => ['nullable', 'array'],
            'recovery_version' => ['sometimes', 'integer', 'min:1'],
            'identity_public_encryption_key' => ['nullable', 'string'],
            'identity_public_signing_key' => ['nullable', 'string'],
            'identity_private_keys_encrypted' => ['nullable', 'string'],
        ]);

        $keys = UserKey::updateOrCreate(
            ['user_id' => $request->user()->id],
            $data
        );

        return response()->json(['data' => $this->payload($keys)]);
    }

    private function payload(UserKey $keys): array
    {
        return [
            'pc_wrapped_master_key' => $keys->pc_wrapped_master_key,
            'pc_kdf_salt' => $keys->pc_kdf_salt,
            'pc_kdf_params' => $keys->pc_kdf_params,
            'recovery_wrapped_master_key' => $keys->recovery_wrapped_master_key,
            'recovery_nonce' => $keys->recovery_nonce,
            'recovery_kdf_salt' => $keys->recovery_kdf_salt,
            'recovery_kdf_params' => $keys->recovery_kdf_params,
            'recovery_version' => $keys->recovery_version,
            'identity_public_encryption_key' => $keys->identity_public_encryption_key,
            'identity_public_signing_key' => $keys->identity_public_signing_key,
            'identity_private_keys_encrypted' => $keys->identity_private_keys_encrypted,
            'updated_at' => $keys->updated_at?->toIso8601String(),
        ];
    }
}
