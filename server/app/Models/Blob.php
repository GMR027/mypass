<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasVersion4Uuids;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Blob extends Model
{
    use HasFactory, HasVersion4Uuids;

    protected $fillable = [
        'user_id',
        'path',
        'size',
        'sha256',
        'ref_count',
    ];

    protected function casts(): array
    {
        return [
            'size' => 'integer',
            'ref_count' => 'integer',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function items(): HasMany
    {
        return $this->hasMany(VaultItem::class);
    }
}
