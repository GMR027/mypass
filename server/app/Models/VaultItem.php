<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasVersion4Uuids;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\SoftDeletes;

class VaultItem extends Model
{
    use HasFactory, HasVersion4Uuids, SoftDeletes;

    protected $table = 'items';

    protected $fillable = [
        'id',
        'user_id',
        'type',
        'state',
        'encrypted_metadata',
        'blob_id',
        'version',
        'expires_at',
        'source_share_id',
        'transferred_to_user_id',
        'transferred_at',
    ];

    protected function casts(): array
    {
        return [
            'version' => 'integer',
            'expires_at' => 'datetime',
            'transferred_at' => 'datetime',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function blob(): BelongsTo
    {
        return $this->belongsTo(Blob::class);
    }

    public function transferredTo(): BelongsTo
    {
        return $this->belongsTo(User::class, 'transferred_to_user_id');
    }

    public function sourceShare(): BelongsTo
    {
        return $this->belongsTo(Share::class, 'source_share_id');
    }

    public function shares(): HasMany
    {
        return $this->hasMany(Share::class, 'item_id');
    }

    public function recipientShares(): HasMany
    {
        return $this->hasMany(Share::class, 'recipient_item_id');
    }
}
