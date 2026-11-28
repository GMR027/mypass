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

    public function test_user_can_create_credentials_and_medical_cards(): void
    {
        $user = User::factory()->create();
        $this->actingAs($user, 'sanctum');

        $this->postJson('/api/v1/items', [
            'type' => 'credential',
            'encrypted_metadata' => base64_encode('credencial-cifrada'),
        ])->assertCreated()
            ->assertJsonPath('data.type', 'credential');

        $this->postJson('/api/v1/items', [
            'type' => 'medical_card',
            'encrypted_metadata' => base64_encode('carnet-cifrado'),
            'expires_at' => '2027-06-30',
        ])->assertCreated()
            ->assertJsonPath('data.type', 'medical_card')
            ->assertJsonPath('data.expires_at', '2027-06-30T00:00:00+00:00');

        $this->postJson('/api/v1/items', [
            'type' => 'insurance',
            'encrypted_metadata' => base64_encode('poliza-cifrada'),
            'expires_at' => '2027-02-15',
        ])->assertCreated()
            ->assertJsonPath('data.type', 'insurance')
            ->assertJsonPath('data.expires_at', '2027-02-15T00:00:00+00:00');

        $this->getJson('/api/v1/items')
            ->assertOk()
            ->assertJsonCount(3, 'data');
    }

    public function test_unknown_item_types_are_rejected(): void
    {
        $user = User::factory()->create();
        $this->actingAs($user, 'sanctum');

        $this->postJson('/api/v1/items', [
            'type' => 'plain-password',
            'encrypted_metadata' => base64_encode('dato'),
        ])->assertStatus(422)
            ->assertJsonValidationErrors('type');
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

    public function test_device_pin_and_biometric_wrapped_key_can_be_updated(): void
    {
        $user = User::factory()->create();
        $this->actingAs($user, 'sanctum');

        $device = $user->devices()->create([
            'name' => 'Tablet',
            'platform' => 'android',
            'public_key' => 'pk',
            'wrapped_master_key' => 'wrapped',
        ]);

        $this->patchJson("/api/v1/devices/{$device->id}", [
            'pin_salt' => base64_encode('new-salt'),
            'pin_params' => ['ops_limit' => 2, 'mem_limit' => 64, 'algorithm' => 'argon2id13'],
            'biometric_wrapped_master_key' => base64_encode('biometric-key'),
        ])->assertOk();

        $device->refresh();
        $this->assertSame(base64_encode('new-salt'), $device->pin_salt);
        $this->assertSame('biometric-key', base64_decode($device->biometric_wrapped_master_key));
        $this->assertSame('argon2id13', $device->pin_params['algorithm']);
    }

    public function test_revoking_a_device_is_recorded_in_the_audit_log(): void
    {
        $user = User::factory()->create();
        $this->actingAs($user, 'sanctum');

        $device = $user->devices()->create([
            'name' => 'Tablet audit',
            'platform' => 'android',
            'public_key' => 'pk',
            'wrapped_master_key' => 'wrapped',
        ]);

        $this->postJson("/api/v1/devices/{$device->id}/revoke")->assertOk();

        $this->assertDatabaseHas('audit_events', [
            'user_id' => $user->id,
            'event' => 'device_revoked',
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

    public function test_trashed_items_can_be_listed_restored_and_permanently_deleted(): void
    {
        Storage::fake('local');

        $user = User::factory()->create();
        $this->actingAs($user, 'sanctum');

        $upload = $this->postJson('/api/v1/blobs', [
            'file' => UploadedFile::fake()->createWithContent('secret.bin', 'ciphertext'),
        ])->assertCreated();
        $blobId = $upload->json('data.id');

        $item = $this->postJson('/api/v1/items', [
            'type' => 'document',
            'encrypted_metadata' => base64_encode('documento'),
            'blob_id' => $blobId,
        ])->assertCreated();
        $itemId = $item->json('data.id');

        $this->deleteJson("/api/v1/items/{$itemId}")
            ->assertOk()
            ->assertJsonPath('data.state', 'trashed');

        $this->getJson('/api/v1/items')
            ->assertOk()
            ->assertJsonCount(0, 'data');

        $this->getJson('/api/v1/items?trashed=1')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.state', 'trashed')
            ->assertJsonPath('data.0.id', $itemId);

        $this->postJson("/api/v1/items/{$itemId}/restore")
            ->assertOk()
            ->assertJsonPath('data.state', 'active')
            ->assertJsonPath('data.version', 2);

        $this->getJson('/api/v1/items')
            ->assertOk()
            ->assertJsonCount(1, 'data');

        $this->deleteJson("/api/v1/items/{$itemId}")->assertOk();
        $this->deleteJson("/api/v1/items/{$itemId}/force")
            ->assertOk()
            ->assertJsonPath('message', 'Documento eliminado definitivamente.');

        $this->assertDatabaseMissing('items', ['id' => $itemId]);
        $this->assertDatabaseMissing('blobs', ['id' => $blobId]);
    }

    public function test_user_cannot_restore_another_users_trashed_item(): void
    {
        $owner = User::factory()->create();
        $other = User::factory()->create();
        $item = $owner->items()->create([
            'type' => 'document',
            'encrypted_metadata' => base64_encode('documento'),
        ]);
        $item->delete();

        $this->actingAs($other, 'sanctum')
            ->postJson("/api/v1/items/{$item->id}/restore")
            ->assertNotFound();

        $this->actingAs($other, 'sanctum')
            ->deleteJson("/api/v1/items/{$item->id}/force")
            ->assertNotFound();
    }

    public function test_trash_older_than_thirty_days_is_purged_when_listing(): void
    {
        Storage::fake('local');

        $user = User::factory()->create();
        $item = $user->items()->create([
            'type' => 'document',
            'encrypted_metadata' => base64_encode('documento'),
        ]);
        $item->delete();
        $item->forceFill(['deleted_at' => now()->subDays(31)])->save();

        $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/items?trashed=1')
            ->assertOk()
            ->assertJsonCount(0, 'data');

        $this->assertDatabaseMissing('items', ['id' => $item->id]);
    }
}
