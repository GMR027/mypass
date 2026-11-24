<?php

namespace App\Http\Controllers;

use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Mail\Message;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Mail;
use Illuminate\View\View;

class SecurityController extends Controller
{
    public function show(Request $request): View
    {
        $user = $request->user();

        $keys = $user->keys()->first();

        return view('settings.security', [
            'user' => $user,
            'hasVault' => $keys !== null,
            'emailRecoveryEnabled' => $keys?->emailRecoveryEnabled() ?? false,
            'emailRecoveryMasked' => $user->email,
            'totpEnabled' => $user->two_factor_secret !== null && $user->two_factor_confirmed_at !== null,
            'totpPending' => $user->two_factor_secret !== null && $user->two_factor_confirmed_at === null,
            'emailEnabled' => $user->email_two_factor_enabled,
            'events' => $user->auditEvents()->latest('created_at')->limit(10)->get(),
        ]);
    }

    public function status(Request $request): JsonResponse
    {
        $user = $request->user();

        return response()->json([
            'data' => [
                'totp_enabled' => $user->two_factor_secret !== null && $user->two_factor_confirmed_at !== null,
                'email_enabled' => $user->email_two_factor_enabled,
                'email' => $user->email,
            ],
        ]);
    }

    public function updatePreferences(Request $request): JsonResponse
    {
        $data = $request->validate([
            'locale' => ['required', 'string', 'in:es-MX,en'],
            'simple_mode' => ['required', 'boolean'],
            'easy_mode' => ['sometimes', 'nullable', 'boolean'],
        ]);

        $request->user()->forceFill([
            'locale' => $data['locale'],
            'simple_mode' => $data['simple_mode'],
            'easy_mode' => array_key_exists('easy_mode', $data)
                ? $data['easy_mode']
                : $request->user()->easy_mode,
        ])->save();

        app()->setLocale($data['locale']);

        return response()->json([
            'message' => $data['locale'] === 'en'
                ? 'Preferences saved.'
                : __('Preferencias guardadas.'),
            'data' => [
                'locale' => $data['locale'],
                'simple_mode' => $data['simple_mode'],
                'easy_mode' => $request->user()->usesEasyMode(),
            ],
        ]);
    }

    public function updateEmailTwoFactor(Request $request): JsonResponse
    {
        $data = $request->validate([
            'enabled' => ['required', 'boolean'],
        ]);

        $request->user()->forceFill([
            'email_two_factor_enabled' => $data['enabled'],
        ])->save();

        if ($data['enabled']) {
            $request->session()->put('email_two_factor_verified', $request->user()->id);
        } else {
            $request->session()->forget([
                'email_two_factor_verified',
                'email_two_factor_user_id',
                'email_two_factor_code_hash',
                'email_two_factor_code_expires',
                'email_two_factor_code_sent_at',
            ]);
        }

        return response()->json([
            'message' => $data['enabled']
                ? __('Código por correo activado.')
                : __('Código por correo desactivado.'),
            'data' => [
                'email_enabled' => $data['enabled'],
            ],
        ]);
    }

    public function showEmailChallenge(): View
    {
        return view('auth.email-two-factor');
    }

    public function sendEmailCode(Request $request): JsonResponse
    {
        $user = $this->pendingUser($request);

        if ($user === null) {
            return response()->json([
                'message' => __('Vuelve a iniciar sesión.'),
            ], 422);
        }

        $sentAt = $request->session()->get('email_two_factor_code_sent_at');
        if ($sentAt !== null && now()->timestamp - $sentAt < 30) {
            return response()->json([
                'message' => __('Espera unos segundos antes de pedir otro código.'),
            ], 429);
        }

        $code = (string) random_int(100000, 999999);
        $request->session()->put([
            'email_two_factor_code_hash' => Hash::make($code),
            'email_two_factor_code_expires' => now()->addMinutes(10)->timestamp,
            'email_two_factor_code_sent_at' => now()->timestamp,
        ]);

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

    public function verifyEmailCode(Request $request): JsonResponse
    {
        $data = $request->validate([
            'code' => ['required', 'string', 'max:10'],
        ]);

        $user = $this->pendingUser($request);

        if ($user === null) {
            return response()->json([
                'message' => __('Vuelve a iniciar sesión.'),
            ], 422);
        }

        $hash = $request->session()->get('email_two_factor_code_hash');
        $expires = $request->session()->get('email_two_factor_code_expires');

        if ($hash === null || $expires === null || now()->timestamp > $expires) {
            return response()->json([
                'message' => __('El código venció. Pide uno nuevo.'),
            ], 422);
        }

        if (! Hash::check(preg_replace('/\s+/', '', $data['code']), $hash)) {
            return response()->json([
                'message' => __('El código no es correcto.'),
            ], 422);
        }

        Auth::guard('web')->login($user);
        $request->session()->regenerate();
        $request->session()->put('email_two_factor_verified', $user->id);
        $request->session()->forget([
            'email_two_factor_user_id',
            'email_two_factor_code_hash',
            'email_two_factor_code_expires',
            'email_two_factor_code_sent_at',
            'login.id',
        ]);

        return response()->json([
            'message' => __('Verificación correcta.'),
            'redirect' => route('dashboard'),
        ]);
    }

    private function pendingUser(Request $request): ?User
    {
        $userId = $request->session()->get('email_two_factor_user_id')
            ?? $request->session()->get('login.id');

        if ($userId !== null) {
            return User::find($userId);
        }

        if ($request->user() !== null) {
            return $request->user();
        }

        return null;
    }

    private function maskEmail(string $email): string
    {
        [$name, $domain] = array_pad(explode('@', $email, 2), 2, '');

        return mb_substr($name, 0, 2).'***@'.$domain;
    }
}
