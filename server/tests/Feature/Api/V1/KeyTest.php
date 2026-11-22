<?php

namespace Tests\Feature\Api\V1;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class KeyTest extends TestCase
{
    use RefreshDatabase;

    public function test_user_can_store_and_read_wrapped_keys(): void
    {
        $user = User::factory()->create();
        $this->actingAs($user, 'sanctum');

        $this->getJson('/api/v1/keys')
            ->assertOk()
            ->assertJsonPath('data', null);

        $this->putJson('/api/v1/keys', [
            'pc_wrapped_master_key' => base64_encode('pc-wrapped'),
            'pc_kdf_salt' => base64_encode('pc-salt'),
            'pc_kdf_params' => ['m' => 65536, 't' => 3, 'p' => 1],
            'recovery_wrapped_master_key' => base64_encode('recovery-wrapped'),
            'recovery_nonce' => base64_encode('recovery-nonce'),
            'recovery_kdf_salt' => base64_encode('recovery-salt'),
            'recovery_kdf_params' => ['algorithm' => 'argon2id13', 'ops_limit' => 2, 'mem_limit' => 67108864],
            'recovery_version' => 1,
            'identity_public_encryption_key' => base64_encode('enc-pub'),
            'identity_public_signing_key' => base64_encode('sign-pub'),
            'identity_private_keys_encrypted' => base64_encode('private-keys'),
        ])->assertOk()
            ->assertJsonPath('data.recovery_version', 1);

        $this->getJson('/api/v1/keys')
            ->assertOk()
            ->assertJsonPath('data.pc_wrapped_master_key', base64_encode('pc-wrapped'))
            ->assertJsonPath('data.recovery_kdf_params.algorithm', 'argon2id13')
            ->assertJsonPath('data.recovery_version', 1);
    }
}
