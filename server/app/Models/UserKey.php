<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class UserKey extends Model
{
    protected $table = 'user_keys';

    protected $primaryKey = 'user_id';

    public $incrementing = false;

    protected $keyType = 'int';

    protected $fillable = [
        'user_id',
        'pc_wrapped_master_key',
        'pc_kdf_salt',
        'pc_kdf_params',
        'recovery_wrapped_master_key',
        'recovery_nonce',
        'recovery_kdf_salt',
        'recovery_kdf_params',
        'recovery_version',
        'identity_public_encryption_key',
        'identity_public_signing_key',
        'identity_private_keys_encrypted',
    ];

    protected function casts(): array
    {
        return [
            'pc_kdf_params' => 'array',
            'recovery_kdf_params' => 'array',
            'recovery_version' => 'integer',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }
}
