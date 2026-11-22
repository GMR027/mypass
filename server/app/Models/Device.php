<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasVersion4Uuids;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Device extends Model
{
    use HasFactory, HasVersion4Uuids;

    protected $fillable = [
        'user_id',
        'name',
        'platform',
        'public_key',
        'wrapped_master_key',
        'pin_salt',
        'pin_params',
        'biometric_wrapped_master_key',
        'app_version',
        'last_seen_at',
        'revoked_at',
    ];

    protected function casts(): array
    {
        return [
            'pin_params' => 'array',
            'last_seen_at' => 'datetime',
            'revoked_at' => 'datetime',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function pairingSessions(): HasMany
    {
        return $this->hasMany(PairingSession::class);
    }

    public function isRevoked(): bool
    {
        return $this->revoked_at !== null;
    }
}
