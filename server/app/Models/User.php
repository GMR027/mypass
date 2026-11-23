<?php

namespace App\Models;

use Database\Factories\UserFactory;
use Illuminate\Contracts\Auth\MustVerifyEmail;
use Illuminate\Database\Eloquent\Attributes\Fillable;
use Illuminate\Database\Eloquent\Attributes\Hidden;
use Illuminate\Database\Eloquent\Casts\Attribute;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Illuminate\Support\Str;
use Laravel\Fortify\TwoFactorAuthenticatable;
use Laravel\Sanctum\HasApiTokens;

#[Fillable(['name', 'email', 'password', 'locale', 'simple_mode', 'is_admin', 'settings', 'email_two_factor_enabled', 'avatar_path'])]
#[Hidden(['password', 'remember_token', 'two_factor_secret', 'two_factor_recovery_codes', 'avatar_path'])]
class User extends Authenticatable implements MustVerifyEmail
{
    /** @use HasFactory<UserFactory> */
    use HasApiTokens, HasFactory, Notifiable, TwoFactorAuthenticatable;

    protected static function booted(): void
    {
        static::creating(function (User $user) {
            $user->public_id ??= (string) Str::uuid();
        });
    }

    /**
     * @return array<string, string>
     */
    protected function casts(): array
    {
        return [
            'email_verified_at' => 'datetime',
            'password' => 'hashed',
            'simple_mode' => 'boolean',
            'is_admin' => 'boolean',
            'email_two_factor_enabled' => 'boolean',
            'settings' => 'array',
            'two_factor_confirmed_at' => 'datetime',
        ];
    }

    protected function avatarUrl(): Attribute
    {
        return Attribute::get(function (): ?string {
            if (blank($this->avatar_path)) {
                return null;
            }

            $token = substr(md5((string) $this->avatar_path), 0, 12);

            return route('api.avatars.show', ['user' => $this->public_id]).'?v='.$token;
        });
    }

    public function devices(): HasMany
    {
        return $this->hasMany(Device::class);
    }

    public function keys(): HasOne
    {
        return $this->hasOne(UserKey::class, 'user_id');
    }

    public function items(): HasMany
    {
        return $this->hasMany(VaultItem::class);
    }

    public function contacts(): HasMany
    {
        return $this->hasMany(Contact::class);
    }

    public function contactOf(): HasMany
    {
        return $this->hasMany(Contact::class, 'contact_user_id');
    }

    public function sharesSent(): HasMany
    {
        return $this->hasMany(Share::class, 'sender_id');
    }

    public function sharesReceived(): HasMany
    {
        return $this->hasMany(Share::class, 'recipient_id');
    }

    public function shareRequestsMade(): HasMany
    {
        return $this->hasMany(ShareRequest::class, 'requester_id');
    }

    public function shareRequestsReceived(): HasMany
    {
        return $this->hasMany(ShareRequest::class, 'target_id');
    }

    public function userNotifications(): HasMany
    {
        return $this->hasMany(UserNotification::class);
    }

    public function auditEvents(): HasMany
    {
        return $this->hasMany(AuditEvent::class);
    }
}
