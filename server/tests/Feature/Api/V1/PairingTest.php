<?php

namespace Tests\Feature\Api\V1;

use App\Models\PairingSession;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class PairingTest extends TestCase
{
    use RefreshDatabase;

    public function test_web_can_create_a_pairing_code_and_android_can_join(): void
    {
        $user = User::factory()->create();
        $this->actingAs($user);

        $created = $this->postJson('/api/web/pairing')
            ->assertCreated()
            ->assertJsonStructure(['data' => ['id', 'code', 'expires_at']]);

        $sessionId = $created->json('data.id');
        $code = $created->json('data.code');

        $this->assertMatchesRegularExpression('/^\d{6}$/', $code);

        $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/pairing/join', [
                'code' => $code,
                'public_key' => base64_encode('android-public-key'),
                'device_name' => 'Xiaomi Pad 6',
            ])
            ->assertOk()
            ->assertJsonPath('data.status', 'joined');

        $this->actingAs($user)
            ->getJson("/api/web/pairing/{$sessionId}")
            ->assertOk()
            ->assertJsonPath('data.status', 'joined')
            ->assertJsonPath('data.public_key', base64_encode('android-public-key'));

        $this->actingAs($user)
            ->postJson("/api/web/pairing/{$sessionId}/approve", [
                'encrypted_master_key' => base64_encode('encrypted-master-key'),
            ])
            ->assertOk()
            ->assertJsonPath('data.status', 'approved');

        $this->actingAs($user, 'sanctum')
            ->getJson("/api/v1/pairing/{$sessionId}")
            ->assertOk()
            ->assertJsonPath('data.status', 'approved')
            ->assertJsonPath('data.encrypted_master_key', base64_encode('encrypted-master-key'));
    }

    public function test_pairing_rejects_a_wrong_code(): void
    {
        $user = User::factory()->create();
        $this->actingAs($user);

        $this->postJson('/api/web/pairing')->assertCreated();

        $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/pairing/join', [
                'code' => '000000',
                'public_key' => base64_encode('android-public-key'),
                'device_name' => 'Xiaomi Pad 6',
            ])
            ->assertStatus(422);
    }

    public function test_expired_pairing_is_rejected(): void
    {
        $user = User::factory()->create();
        $session = PairingSession::create([
            'user_id' => $user->id,
            'purpose' => 'pair_device',
            'code_hash' => hash_hmac('sha256', '123456', (string) config('app.key')),
            'status' => 'pending',
            'expires_at' => now()->subMinute(),
        ]);

        $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/pairing/join', [
                'code' => '123456',
                'public_key' => base64_encode('android-public-key'),
                'device_name' => 'Xiaomi Pad 6',
            ])
            ->assertStatus(422);

        $this->assertSame('expired', $session->fresh()->status);
    }
}
