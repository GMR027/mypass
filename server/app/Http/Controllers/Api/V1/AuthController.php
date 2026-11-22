<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Services\TwoFactorChallenge;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\Rules\Password;

class AuthController extends Controller
{
    public function register(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:120'],
            'email' => ['required', 'string', 'lowercase', 'email', 'max:255', 'unique:users,email'],
            'password' => ['required', 'confirmed', Password::min(12)->letters()->numbers()],
            'locale' => ['sometimes', 'string', 'in:es-MX,en'],
            'simple_mode' => ['sometimes', 'boolean'],
            'device_name' => ['sometimes', 'string', 'max:120'],
        ]);

        $user = User::create([
            'name' => $data['name'],
            'email' => $data['email'],
            'password' => $data['password'],
            'locale' => $data['locale'] ?? 'es-MX',
            'simple_mode' => $data['simple_mode'] ?? true,
            'is_admin' => User::query()->count() === 0,
        ]);

        $token = $user->createToken($data['device_name'] ?? 'web')->plainTextToken;

        return response()->json([
            'user' => $this->userPayload($user),
            'token' => $token,
        ], 201);
    }

    public function login(Request $request, TwoFactorChallenge $twoFactorChallenge): JsonResponse
    {
        $data = $request->validate([
            'email' => ['required', 'string', 'lowercase', 'email'],
            'password' => ['required', 'string'],
            'device_name' => ['sometimes', 'string', 'max:120'],
        ]);

        $user = User::where('email', $data['email'])->first();

        if (! $user || ! Hash::check($data['password'], $user->password)) {
            return response()->json([
                'message' => __('Las credenciales no son correctas.'),
            ], 422);
        }

        if ($twoFactorChallenge->requiresVerification($user)) {
            return response()->json(
                $twoFactorChallenge->start($user, $data['device_name'] ?? 'Android'),
            );
        }

        $token = $user->createToken($data['device_name'] ?? 'web')->plainTextToken;

        return response()->json([
            'user' => $this->userPayload($user),
            'token' => $token,
        ]);
    }

    public function me(Request $request): JsonResponse
    {
        return response()->json([
            'user' => $this->userPayload($request->user()),
        ]);
    }

    public function logout(Request $request): JsonResponse
    {
        $request->user()->currentAccessToken()?->delete();

        return response()->json(['message' => __('Sesión cerrada.')]);
    }

    private function userPayload(User $user): array
    {
        return [
            'id' => $user->public_id,
            'name' => $user->name,
            'email' => $user->email,
            'locale' => $user->locale,
            'simple_mode' => $user->simple_mode,
            'has_two_factor' => $user->two_factor_secret !== null && $user->two_factor_confirmed_at !== null,
            'has_email_two_factor' => $user->email_two_factor_enabled,
        ];
    }
}
