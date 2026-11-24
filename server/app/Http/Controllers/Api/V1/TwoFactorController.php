<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Services\TwoFactorChallenge;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Mail\Message;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Mail;

class TwoFactorController extends Controller
{
    public function verify(Request $request, TwoFactorChallenge $challenges): JsonResponse
    {
        $data = $request->validate([
            'challenge_token' => ['required', 'string', 'size:64'],
            'code' => ['required', 'string', 'max:20'],
            'method' => ['sometimes', 'string', 'in:totp,email,recovery'],
        ]);

        $challenge = $challenges->get($data['challenge_token']);

        if ($challenge === null) {
            return response()->json([
                'message' => __('La verificación venció. Vuelve a iniciar sesión.'),
            ], 422);
        }

        if (($challenge['attempts'] ?? 0) >= 5) {
            $challenges->forget($data['challenge_token']);

            return response()->json([
                'message' => __('Demasiados intentos. Vuelve a iniciar sesión.'),
            ], 429);
        }

        $user = User::find($challenge['user_id']);

        if ($user === null) {
            $challenges->forget($data['challenge_token']);

            return response()->json([
                'message' => __('Vuelve a iniciar sesión.'),
            ], 422);
        }

        $method = $data['method'] ?? $this->defaultMethod($challenge);

        if (! in_array($method, $challenge['methods'] ?? [], true)) {
            return response()->json([
                'message' => __('Ese método de verificación no está disponible.'),
            ], 422);
        }

        if (! $challenges->verify($user, $challenge, $method, $data['code'])) {
            $challenge['attempts'] = ($challenge['attempts'] ?? 0) + 1;
            $challenges->put($data['challenge_token'], $challenge);

            return response()->json([
                'message' => __('El código no es correcto.'),
            ], 422);
        }

        $challenges->forget($data['challenge_token']);
        $token = $user->createToken($challenge['device_name'] ?? 'Android')->plainTextToken;

        return response()->json([
            'user' => $this->userPayload($user),
            'token' => $token,
        ]);
    }

    public function sendEmail(Request $request, TwoFactorChallenge $challenges): JsonResponse
    {
        $data = $request->validate([
            'challenge_token' => ['required', 'string', 'size:64'],
        ]);

        $challenge = $challenges->get($data['challenge_token']);

        if ($challenge === null) {
            return response()->json([
                'message' => __('La verificación venció. Vuelve a iniciar sesión.'),
            ], 422);
        }

        if (! in_array('email', $challenge['methods'] ?? [], true)) {
            return response()->json([
                'message' => __('Esta cuenta no tiene código por correo.'),
            ], 422);
        }

        $sentAt = $challenge['email_sent_at'] ?? null;
        if ($sentAt !== null && now()->timestamp - $sentAt < 30) {
            return response()->json([
                'message' => __('Espera unos segundos antes de pedir otro código.'),
            ], 429);
        }

        $user = User::find($challenge['user_id']);
        if ($user === null) {
            return response()->json([
                'message' => __('Vuelve a iniciar sesión.'),
            ], 422);
        }

        $code = (string) random_int(100000, 999999);
        $challenge['email_hash'] = Hash::make($code);
        $challenge['email_expires'] = now()->addMinutes(10)->timestamp;
        $challenge['email_sent_at'] = now()->timestamp;
        $challenges->put($data['challenge_token'], $challenge);

        Mail::raw(
            __('Tu código de verificación de My Pass es: :code. Vence en 10 minutos.', ['code' => $code]),
            function (Message $message) use ($user): void {
                $message->to($user->email)->subject(__('Código de verificación de My Pass'));
            },
        );

        return response()->json([
            'message' => __('Código enviado a :email.', ['email' => $this->maskEmail($user->email)]),
        ]);
    }

    /**
     * @param  array<string, mixed>  $challenge
     */
    private function defaultMethod(array $challenge): string
    {
        $methods = $challenge['methods'] ?? [];

        return in_array('totp', $methods, true)
            ? 'totp'
            : (in_array('email', $methods, true) ? 'email' : 'recovery');
    }

    private function userPayload(User $user): array
    {
        return [
            'id' => $user->public_id,
            'name' => $user->name,
            'email' => $user->email,
            'avatar_url' => $user->avatar_url,
            'locale' => $user->locale,
            'simple_mode' => $user->simple_mode,
            'easy_mode' => $user->usesEasyMode(),
            'age_group' => $user->age_group,
            'birth_date' => $user->birth_date?->toDateString(),
            'has_two_factor' => $user->two_factor_secret !== null,
        ];
    }

    private function maskEmail(string $email): string
    {
        [$name, $domain] = array_pad(explode('@', $email, 2), 2, '');

        return mb_substr($name, 0, 2).'***@'.$domain;
    }
}
