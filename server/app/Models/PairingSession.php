<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasVersion4Uuids;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class PairingSession extends Model
{
    use HasFactory, HasVersion4Uuids;

    protected $fillable = [
        'user_id',
        'device_id',
        'public_key',
        'device_name',
        'purpose',
        'code_hash',
        'status',
        'payload',
        'expires_at',
        'joined_at',
        'approved_at',
        'used_at',
    ];

    protected function casts(): array
    {
        return [
            'expires_at' => 'datetime',
            'joined_at' => 'datetime',
            'approved_at' => 'datetime',
            'used_at' => 'datetime',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function device(): BelongsTo
    {
        return $this->belongsTo(Device::class);
    }

    public function isExpired(): bool
    {
        return $this->expires_at->isPast() || $this->used_at !== null;
    }
}
