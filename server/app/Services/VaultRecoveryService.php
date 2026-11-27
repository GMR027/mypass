<?php

namespace App\Services;

use App\Models\User;
use Illuminate\Mail\Message;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Mail;
use Illuminate\Validation\ValidationException;

class VaultRecoveryService
{
    private const CODE_TTL_MINUTES = 10;

    private const CODE_COOLDOWN_SECONDS = 30;

    private const MAX_ATTEMPTS = 5;

    public function __construct(private AuditLogger $audit) {}

    /**
     * @return array{enabled: bool, email: string, masked_email: string}
     */
    public function status(User $user): array
    {
        $keys = $user->keys()->first();

        return [
            'enabled' => $keys?->emailRecoveryEnabled() ?? false,
            'email' => $user->email,
            'masked_email' => $this->maskEmail($user->email),
        ];
    }

    public function requestCode(User $user): string
    {
        $keys = $user->keys()->first();

        if (! $keys?->emailRecoveryEnabled()) {
            throw ValidationException::withMessages([
                'email_recovery' => __('La recuperación por correo no está activada en esta cuenta.'),
            ]);
        }

        $cached = Cache::get($this->cacheKey($user));
        $sentAt = $cached['sent_at'] ?? null;

        if ($sentAt !== null && now()->timestamp - $sentAt < self::CODE_COOLDOWN_SECONDS) {
            throw ValidationException::withMessages([
                'email_recovery' => __('Espera unos segundos antes de pedir otro código.'),
            ]);
        }

        $code = (string) random_int(100000, 999999);

        Cache::put($this->cacheKey($user), [
            'hash' => Hash::make($code),
            'expires' => now()->addMinutes(self::CODE_TTL_MINUTES)->timestamp,
            'attempts' => 0,
            'sent_at' => now()->timestamp,
        ], now()->addMinutes(self::CODE_TTL_MINUTES));

        Mail::raw(
            __('Tu código para recuperar la bóveda de My Pass es: :code. Vence en 10 minutos. Si no lo pediste, ignora este mensaje y cambia tu contraseña.', ['code' => $code]),
            function (Message $message) use ($user): void {
                $message->to($user->email)->subject(__('Código de recuperación de tu bóveda My Pass'));
            },
        );

        $this->audit->log($user, 'vault_recovery_requested');

        return $this->maskEmail($user->email);
    }

    /**
     * @return array{secret: string, wrapped_master_key: string, version: int}
     */
    public function verifyCode(User $user, string $code): array
    {
        $keys = $user->keys()->first();

        if (! $keys?->emailRecoveryEnabled()) {
            throw ValidationException::withMessages([
                'email_recovery' => __('La recuperación por correo no está activada en esta cuenta.'),
            ]);
        }

        $cached = Cache::get($this->cacheKey($user));

        if (! is_array($cached)) {
            throw ValidationException::withMessages([
                'code' => __('El código venció. Pide uno nuevo.'),
            ]);
        }

        if (now()->timestamp > ($cached['expires'] ?? 0)) {
            Cache::forget($this->cacheKey($user));

            throw ValidationException::withMessages([
                'code' => __('El código venció. Pide uno nuevo.'),
            ]);
        }

        if (($cached['attempts'] ?? 0) >= self::MAX_ATTEMPTS) {
            Cache::forget($this->cacheKey($user));

            throw ValidationException::withMessages([
                'code' => __('Demasiados intentos. Pide un código nuevo.'),
            ]);
        }

        if (! Hash::check(preg_replace('/\s+/', '', $code), $cached['hash'] ?? '')) {
            $cached['attempts'] = ($cached['attempts'] ?? 0) + 1;
            Cache::put($this->cacheKey($user), $cached, now()->addMinutes(self::CODE_TTL_MINUTES));

            throw ValidationException::withMessages([
                'code' => __('El código no es correcto.'),
            ]);
        }

        Cache::forget($this->cacheKey($user));

        $this->audit->log($user, 'vault_recovery_used');
        $this->notify($user, 'vault_recovery_used', [
            'message' => __('Tu bóveda se abrió con recuperación por correo.'),
        ]);

        return [
            'secret' => (string) $keys->email_recovery_secret,
            'wrapped_master_key' => (string) $keys->email_recovery_wrapped_master_key,
            'version' => (int) $keys->email_recovery_version,
        ];
    }

    /**
     * @param  array{secret: string, wrapped_master_key: string}  $payload
     */
    public function enable(User $user, array $payload): void
    {
        $keys = $user->keys()->firstOrFail();

        $keys->forceFill([
            'email_recovery_secret' => $payload['secret'],
            'email_recovery_wrapped_master_key' => $payload['wrapped_master_key'],
            'email_recovery_version' => ((int) $keys->email_recovery_version) + 1,
            'email_recovery_enabled_at' => now(),
        ])->save();

        Cache::forget($this->cacheKey($user));

        $this->audit->log($user, 'vault_recovery_enabled');
        $this->notify($user, 'vault_recovery_enabled', [
            'message' => __('Activaste la recuperación de tu bóveda por correo.'),
        ]);
    }

    public function disable(User $user): void
    {
        $keys = $user->keys()->first();

        if ($keys === null) {
            return;
        }

        $keys->forceFill([
            'email_recovery_secret' => null,
            'email_recovery_wrapped_master_key' => null,
            'email_recovery_enabled_at' => null,
        ])->save();

        Cache::forget($this->cacheKey($user));

        $this->audit->log($user, 'vault_recovery_disabled');
        $this->notify($user, 'vault_recovery_disabled', [
            'message' => __('Desactivaste la recuperación de tu bóveda por correo.'),
        ]);
    }

    public function maskEmail(string $email): string
    {
        [$name, $domain] = array_pad(explode('@', $email, 2), 2, '');

        return mb_substr($name, 0, 2).'***@'.$domain;
    }

    /**
     * @param  array<string, mixed>  $data
     */
    private function notify(User $user, string $type, array $data): void
    {
        AppNotifications::create($user, $type, $data);
    }

    private function cacheKey(User $user): string
    {
        return 'vault-recovery:'.$user->id;
    }
}
