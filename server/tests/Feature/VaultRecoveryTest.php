<?php

namespace Tests\Feature;

use App\Models\User;
use App\Models\UserKey;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Mail;
use Tests\TestCase;

class VaultRecoveryTest extends TestCase
{
    use RefreshDatabase;

    private function userWithVault(array $attributes = []): User
    {
        $user = User::factory()->create($attributes);

        UserKey::create([
            'user_id' => $user->id,
            'pc_wrapped_master_key' => 'wrapped-pc',
            'pc_kdf_salt' => 'salt',
            'pc_kdf_params' => ['ops_limit' => 1, 'mem_limit' => 1],
            'email_recovery_secret' => base64_encode(random_bytes(32)),
            'email_recovery_wrapped_master_key' => base64_encode(random_bytes(64)),
            'email_recovery_version' => 1,
            'email_recovery_enabled_at' => now(),
        ]);

        return $user;
    }

    public function test_keys_endpoint_does_not_expose_the_recovery_secret(): void
    {
        $user = $this->userWithVault();

        $this->actingAs($user)
            ->getJson(route('web.keys.show'))
            ->assertOk()
            ->assertJsonPath('data.email_recovery_enabled', true)
            ->assertJsonMissingPath('data.email_recovery_secret')
            ->assertJsonMissingPath('data.email_recovery_wrapped_master_key');
    }

    public function test_user_can_request_and_verify_an_email_recovery_code(): void
    {
        Mail::fake();

        $user = $this->userWithVault();
        $storedSecret = $user->keys->email_recovery_secret;

        $this->actingAs($user)
            ->postJson(route('web.vault-recovery.request'))
            ->assertOk();

        $cached = Cache::get('vault-recovery:'.$user->id);
        $this->assertIsArray($cached);

        $cached['hash'] = bcrypt('123456');
        Cache::put('vault-recovery:'.$user->id, $cached, now()->addMinutes(10));

        $this->actingAs($user)
            ->postJson(route('web.vault-recovery.verify'), ['code' => '123456'])
            ->assertOk()
            ->assertJsonPath('data.secret', $storedSecret)
            ->assertJsonPath('data.wrapped_master_key', $user->keys->email_recovery_wrapped_master_key);
    }

    public function test_wrong_recovery_code_is_rejected(): void
    {
        Mail::fake();

        $user = $this->userWithVault();

        $this->actingAs($user)
            ->postJson(route('web.vault-recovery.request'))
            ->assertOk();

        $this->actingAs($user)
            ->postJson(route('web.vault-recovery.verify'), ['code' => '000000'])
            ->assertStatus(422)
            ->assertJsonValidationErrors('code');
    }

    public function test_user_can_enable_and_disable_email_recovery(): void
    {
        $user = User::factory()->create([
            'password' => 'Password12345!',
        ]);

        UserKey::create([
            'user_id' => $user->id,
            'pc_wrapped_master_key' => 'wrapped-pc',
            'pc_kdf_salt' => 'salt',
            'pc_kdf_params' => ['ops_limit' => 1, 'mem_limit' => 1],
        ]);

        $this->actingAs($user)
            ->postJson(route('web.vault-recovery.enable'), [
                'secret' => base64_encode(random_bytes(32)),
                'wrapped_master_key' => base64_encode(random_bytes(64)),
            ])
            ->assertOk()
            ->assertJsonPath('data.enabled', true);

        $this->assertTrue($user->keys()->first()->emailRecoveryEnabled());

        $this->actingAs($user)
            ->postJson(route('web.vault-recovery.disable'), [
                'password' => 'Password12345!',
            ])
            ->assertOk()
            ->assertJsonPath('data.enabled', false);

        $this->assertFalse($user->keys()->first()->emailRecoveryEnabled());
    }

    public function test_android_recovery_endpoints_use_the_same_payload(): void
    {
        Mail::fake();

        $user = $this->userWithVault();
        $storedSecret = $user->keys->email_recovery_secret;

        $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/vault-recovery/status')
            ->assertOk()
            ->assertJsonPath('data.enabled', true);

        $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/vault-recovery/request')
            ->assertOk();

        $cached = Cache::get('vault-recovery:'.$user->id);
        $cached['hash'] = bcrypt('654321');
        Cache::put('vault-recovery:'.$user->id, $cached, now()->addMinutes(10));

        $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/vault-recovery/verify', ['code' => '654321'])
            ->assertOk()
            ->assertJsonPath('data.secret', $storedSecret);
    }
}
