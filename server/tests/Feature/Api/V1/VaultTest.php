<?php

namespace Tests\Feature\Api\V1;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

class VaultTest extends TestCase
{
    use RefreshDatabase;

    public function test_user_can_create_list_update_and_delete_an_item(): void
    {
        $user = User::factory()->create();
        $this->actingAs($user, 'sanctum');

        $create = $this->postJson('/api/v1/items', [
            'type' => 'document',
            'encrypted_metadata' => base64_encode('metadata-cifrada'),
        ])->assertCreated()
            ->assertJsonPath('data.type', 'document')
            ->assertJsonPath('data.state', 'active')
            ->assertJsonPath('data.version', 1);

        $itemId = $create->json('data.id');

        $this->getJson('/api/v1/items')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.id', $itemId);

        $this->patchJson("/api/v1/items/{$itemId}", [
            'encrypted_metadata' => base64_encode('metadata-actualizada'),
            'version' => 1,
        ])->assertOk()
            ->assertJsonPath('data.version', 2);

        $this->patchJson("/api/v1/items/{$itemId}", [
            'encrypted_metadata' => base64_encode('conflicto'),
            'version' => 1,
        ])->assertStatus(409);

        $this->deleteJson("/api/v1/items/{$itemId}")->assertOk();
        $this->assertSoftDeleted('items', ['id' => $itemId]);
    }

    public function test_client_can_choose_item_id_for_offline_creation(): void
    {
        $user = User::factory()->create();
        $this->actingAs($user, 'sanctum');

        $id = 'a348f1f2-b472-4c78-a5b5-e4a7d1b0cdac';

        $this->postJson('/api/v1/items', [
            'id' => $id,
            'type' => 'document',
            'encrypted_metadata' => base64_encode('metadata'),
        ])->assertCreated()
            ->assertJsonPath('data.id', $id);

        $this->assertDatabaseHas('items', ['id' => $id]);
    }

    public function test_user_cannot_see_another_users_item(): void
    {
        $owner = User::factory()->create();
        $other = User::factory()->create();
        $item = $owner->items()->create([
            'type' => 'ticket',
            'encrypted_metadata' => base64_encode('boleto'),
        ]);

        $this->actingAs($other, 'sanctum')
            ->getJson("/api/v1/items/{$item->id}")
            ->assertNotFound();
    }

    public function test_encrypted_blob_can_be_uploaded_and_downloaded_only_by_owner(): void
    {
        Storage::fake('local');

        $owner = User::factory()->create();
        $other = User::factory()->create();
        $ciphertext = 'ciphertext-not-readable-by-server';

        $this->actingAs($owner, 'sanctum');
        $upload = $this->postJson('/api/v1/blobs', [
            'file' => UploadedFile::fake()->createWithContent('secret.bin', $ciphertext),
        ])->assertCreated();

        $blobId = $upload->json('data.id');

        $item = $this->postJson('/api/v1/items', [
            'type' => 'document',
            'encrypted_metadata' => base64_encode('documento'),
            'blob_id' => $blobId,
        ])->assertCreated();

        $this->get("/api/v1/blobs/{$blobId}")
            ->assertOk()
            ->assertHeader('content-type', 'application/octet-stream');

        $this->actingAs($other, 'sanctum')
            ->get("/api/v1/blobs/{$blobId}")
            ->assertNotFound();

        $this->assertDatabaseHas('blobs', [
            'id' => $blobId,
            'sha256' => hash('sha256', $ciphertext),
        ]);
    }

    public function test_device_can_be_registered_and_revoked(): void
    {
        $user = User::factory()->create();
        $this->actingAs($user, 'sanctum');

        $device = $this->postJson('/api/v1/devices', [
            'name' => 'Tablet de prueba',
            'platform' => 'android',
            'public_key' => base64_encode('public-key'),
            'wrapped_master_key' => base64_encode('wrapped-key'),
            'pin_salt' => base64_encode('salt'),
            'pin_params' => ['m' => 65536, 't' => 3, 'p' => 1],
        ])->assertCreated()->assertJsonPath('data.name', 'Tablet de prueba');

        $deviceId = $device->json('data.id');

        $this->getJson("/api/v1/devices/{$deviceId}")
            ->assertOk()
            ->assertJsonPath('data.revoked', false);

        $this->postJson("/api/v1/devices/{$deviceId}/revoke")->assertOk();
        $this->assertDatabaseMissing('devices', [
            'id' => $deviceId,
            'revoked_at' => null,
        ]);
    }

    public function test_web_can_list_and_revoke_devices(): void
    {
        $user = User::factory()->create();
        $device = $user->devices()->create([
            'name' => 'Tablet web',
            'platform' => 'android',
            'public_key' => 'pk',
            'wrapped_master_key' => 'wrapped',
            'last_seen_at' => now(),
        ]);

        $this->actingAs($user)
            ->getJson('/api/web/devices')
            ->assertOk()
            ->assertJsonPath('data.0.name', 'Tablet web');

        $this->actingAs($user)
            ->postJson("/api/web/devices/{$device->id}/revoke")
            ->assertOk();

        $this->assertNotNull($device->fresh()->revoked_at);
    }
}
