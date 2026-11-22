<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Concerns\HasVersion4Uuids;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Share extends Model
{
    use HasFactory, HasVersion4Uuids;

    protected $fillable = [
        'sender_id',
        'recipient_id',
        'share_request_id',
        'item_id',
        'recipient_item_id',
        'mode',
        'status',
        'encrypted_package',
        'download_allowed',
        'expires_at',
        'accepted_at',
        'revoked_at',
    ];

    protected function casts(): array
    {
        return [
            'download_allowed' => 'boolean',
            'expires_at' => 'datetime',
            'accepted_at' => 'datetime',
            'revoked_at' => 'datetime',
        ];
    }

    public function sender(): BelongsTo
    {
        return $this->belongsTo(User::class, 'sender_id');
    }

    public function recipient(): BelongsTo
    {
        return $this->belongsTo(User::class, 'recipient_id');
    }

    public function item(): BelongsTo
    {
        return $this->belongsTo(VaultItem::class, 'item_id');
    }

    public function shareRequest(): BelongsTo
    {
        return $this->belongsTo(ShareRequest::class);
    }

    public function recipientItem(): BelongsTo
    {
        return $this->belongsTo(VaultItem::class, 'recipient_item_id');
    }
}
