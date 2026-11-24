<?php

namespace App\Http\Controllers;

use App\Services\VaultRecoveryService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class VaultRecoveryController extends Controller
{
    public function status(Request $request, VaultRecoveryService $recovery): JsonResponse
    {
        return response()->json(['data' => $recovery->status($request->user())]);
    }

    public function requestCode(Request $request, VaultRecoveryService $recovery): JsonResponse
    {
        $masked = $recovery->requestCode($request->user());

        return response()->json([
            'message' => __('Código enviado a :email.', ['email' => $masked]),
        ]);
    }

    public function verifyCode(Request $request, VaultRecoveryService $recovery): JsonResponse
    {
        $data = $request->validate([
            'code' => ['required', 'string', 'max:10'],
        ]);

        $payload = $recovery->verifyCode($request->user(), $data['code']);

        return response()->json(['data' => $payload])
            ->header('Cache-Control', 'no-store, private');
    }

    public function enable(Request $request, VaultRecoveryService $recovery): JsonResponse
    {
        $data = $request->validate([
            'secret' => ['required', 'string', 'max:2048'],
            'wrapped_master_key' => ['required', 'string', 'max:100000'],
        ]);

        abort_unless($request->user()->keys()->exists(), 422, __('Tu bóveda todavía no está configurada.'));

        $recovery->enable($request->user(), $data);

        return response()->json([
            'message' => __('Recuperación por correo activada.'),
            'data' => $recovery->status($request->user()),
        ]);
    }

    public function disable(Request $request, VaultRecoveryService $recovery): JsonResponse
    {
        $request->validate([
            'password' => ['required', 'string', 'current_password:web'],
        ], [
            'password.current_password' => __('La contraseña no es correcta.'),
        ]);

        $recovery->disable($request->user());

        return response()->json([
            'message' => __('Recuperación por correo desactivada.'),
            'data' => $recovery->status($request->user()),
        ]);
    }
}
