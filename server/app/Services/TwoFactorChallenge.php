<?php

namespace App\Services;

use App\Models\User;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
use Laravel\Fortify\Contracts\TwoFactorAuthenticationProvider;
use Laravel\Fortify\Fortify;

class TwoFactorChallenge
{
    private const TTL_MINUTES = 10;

    /**
     * @return array{two_factor_required: bool, challenge_token: string, methods: array<int, string>}
     */
    public function start(User $user, string $deviceName): array
    {
        $token = Str::random(64);
        $methods = $this->methodsFor($user);

        Cache::put($this->key($token), [
            'user_id' => $user->id,
            'device_name' => $deviceName,
            'methods' => $methods,
            'attempts' => 0,
            'email_hash' => null,
            'email_expires' => null,
            'email_sent_at' => null,
        ], now()->addMinutes(self::TTL_MINUTES));

        return [
            'two_factor_required' => true,
            'challenge_token' => $token,
            'methods' => $methods,
            'message' => __('Escribe el código de verificación.'),
        ];
    }

    /**
     * @return array<string, mixed>|null
     */
    public function get(string $token): ?array
    {
        $challenge = Cache::get($this->key($token));

        return is_array($challenge) ? $challenge : null;
    }

    /**
     * @param  array<string, mixed>  $challenge
     */
    public function put(string $token, array $challenge): void
    {
        Cache::put($this->key($token), $challenge, now()->addMinutes(self::TTL_MINUTES));
    }

    public function forget(string $token): void
    {
        Cache::forget($this->key($token));
    }

    /**
     * @return array<int, string>
     */
    public function methodsFor(User $user): array
    {
        $methods = [];

        if ($user->two_factor_secret !== null && $user->two_factor_confirmed_at !== null) {
            $methods[] = 'totp';
        }

        if ($user->email_two_factor_enabled) {
            $methods[] = 'email';
        }

        if ($user->two_factor_recovery_codes !== null) {
            $methods[] = 'recovery';
        }

        return $methods;
    }

    public function requiresVerification(User $user): bool
    {
        return $this->methodsFor($user) !== [];
    }

    /**
     * @param  array<string, mixed>  $challenge
     */
    public function verify(User $user, array $challenge, string $method, string $code): bool
    {
        $code = preg_replace('/\s+/', '', $code);

        if ($method === 'recovery') {
            $codes = $user->recoveryCodes();
            if (in_array($code, $codes, true)) {
                $user->replaceRecoveryCode($code);

                return true;
            }

            return false;
        }

        if ($method === 'email') {
            $hash = $challenge['email_hash'] ?? null;
            $expires = $challenge['email_expires'] ?? null;

            if ($hash === null || $expires === null || now()->timestamp > $expires) {
                return false;
            }

            return Hash::check($code, $hash);
        }

        if ($user->two_factor_secret === null) {
            return false;
        }

        return app(TwoFactorAuthenticationProvider::class)->verify(
            Fortify::currentEncrypter()->decrypt($user->two_factor_secret),
            $code,
        );
    }

    private function key(string $token): string
    {
        return 'two-factor-challenge:'.$token;
    }
}
