<?php

namespace Tests\Feature\Api\V1;

use App\Models\Contact;
use App\Models\Share;
use App\Models\ShareRequest;
use App\Models\User;
use App\Models\UserKey;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class FamilyTest extends TestCase
{
    use RefreshDatabase;

    public function test_contacts_can_be_requested_accepted_and_shared(): void
    {
        $mother = User::factory()->create(['name' => 'Mamá', 'email' => 'mama@example.com']);
        $son = User::factory()->create(['name' => 'Hijo', 'email' => 'hijo@example.com']);

        UserKey::create([
            'user_id' => $mother->id,
            'identity_public_encryption_key' => base64_encode('mother-public-key'),
        ]);

        $this->actingAs($son)
            ->postJson('/api/web/contacts', ['email' => 'mama@example.com'])
            ->assertCreated()
            ->assertJsonPath('data.status', 'pending');

        $motherContact = Contact::where('user_id', $mother->id)
            ->where('contact_user_id', $son->id)
            ->firstOrFail();

        $this->actingAs($mother)
            ->postJson("/api/web/contacts/{$motherContact->id}/accept")
            ->assertOk()
            ->assertJsonPath('data.status', 'accepted');

        $sonContact = Contact::where('user_id', $son->id)
            ->where('contact_user_id', $mother->id)
            ->firstOrFail();

        $this->actingAs($son)
            ->getJson("/api/web/contacts/{$sonContact->id}/public-key")
            ->assertOk()
            ->assertJsonPath('data.public_key', base64_encode('mother-public-key'));

        $item = $son->items()->create([
            'type' => 'ticket',
            'encrypted_metadata' => base64_encode('boleto'),
        ]);

        $share = $this->actingAs($son)
            ->postJson('/api/web/shares', [
                'contact_id' => $sonContact->id,
                'item_id' => $item->id,
                'mode' => 'transfer',
                'encrypted_package' => base64_encode('package-para-mama'),
            ])
            ->assertCreated()
            ->assertJsonPath('data.status', 'pending')
            ->assertJsonPath('data.mode', 'transfer');

        $this->assertSame('transferred', $item->fresh()->state);

        $shareId = $share->json('data.id');

        $this->actingAs($mother)
            ->getJson('/api/web/shares')
            ->assertOk()
            ->assertJsonPath('incoming.0.id', $shareId);

        $motherItem = $mother->items()->create([
            'type' => 'ticket',
            'encrypted_metadata' => base64_encode('boleto-reencriptado'),
        ]);

        $this->actingAs($mother)
            ->postJson("/api/web/shares/{$shareId}/accept", [
                'recipient_item_id' => $motherItem->id,
            ])
            ->assertOk()
            ->assertJsonPath('data.status', 'accepted');

        $shareRecord = Share::findOrFail($shareId);
        $this->assertSame('accepted', $shareRecord->status);
        $this->assertSame($motherItem->id, $shareRecord->recipient_item_id);
    }

    public function test_document_requests_can_be_created_and_declined(): void
    {
        $requester = User::factory()->create(['name' => 'Hijo']);
        $target = User::factory()->create(['name' => 'Mamá']);
        $contact = Contact::create([
            'user_id' => $requester->id,
            'contact_user_id' => $target->id,
            'status' => 'accepted',
        ]);
        Contact::create([
            'user_id' => $target->id,
            'contact_user_id' => $requester->id,
            'status' => 'accepted',
        ]);

        $request = $this->actingAs($requester)
            ->postJson('/api/web/share-requests', [
                'contact_id' => $contact->id,
                'document_type' => 'INE',
                'purpose' => 'Trámite escolar',
            ])
            ->assertCreated()
            ->assertJsonPath('data.status', 'pending');

        $requestId = $request->json('data.id');

        $this->actingAs($target)
            ->getJson('/api/web/share-requests')
            ->assertOk()
            ->assertJsonPath('incoming.0.id', $requestId)
            ->assertJsonPath('incoming.0.document_type', 'INE');

        $this->actingAs($target)
            ->postJson("/api/web/share-requests/{$requestId}/decline")
            ->assertOk();

        $this->assertSame('declined', ShareRequest::findOrFail($requestId)->status);
    }

    public function test_sending_a_document_fulfills_the_pending_request(): void
    {
        $son = User::factory()->create(['name' => 'Hijo']);
        $mother = User::factory()->create(['name' => 'Mamá']);
        $sonContact = Contact::create([
            'user_id' => $son->id,
            'contact_user_id' => $mother->id,
            'status' => 'accepted',
        ]);
        $motherContact = Contact::create([
            'user_id' => $mother->id,
            'contact_user_id' => $son->id,
            'status' => 'accepted',
        ]);

        $request = ShareRequest::create([
            'requester_id' => $son->id,
            'target_id' => $mother->id,
            'document_type' => 'INE',
            'purpose' => 'Trámite escolar',
            'status' => 'pending',
        ]);

        $item = $mother->items()->create([
            'type' => 'document',
            'encrypted_metadata' => base64_encode('doc'),
        ]);

        $share = $this->actingAs($mother)
            ->postJson('/api/web/shares', [
                'contact_id' => $motherContact->id,
                'item_id' => $item->id,
                'mode' => 'copy',
                'encrypted_package' => 'sealed-package',
            ])
            ->assertCreated()
            ->assertJsonPath('data.share_request_id', $request->id);

        $recipientItem = $son->items()->create([
            'type' => 'document',
            'encrypted_metadata' => base64_encode('received'),
        ]);

        $this->actingAs($son)
            ->postJson("/api/web/shares/{$share->json('data.id')}/accept", [
                'recipient_item_id' => $recipientItem->id,
            ])
            ->assertOk();

        $this->assertSame('fulfilled', $request->fresh()->status);
        $this->assertNotNull($request->fresh()->responded_at);
    }

    public function test_share_detail_returns_only_the_requested_share(): void
    {
        $sender = User::factory()->create();
        $recipient = User::factory()->create();
        $item = $sender->items()->create([
            'type' => 'document',
            'encrypted_metadata' => base64_encode('doc'),
        ]);

        $first = Share::create([
            'sender_id' => $sender->id,
            'recipient_id' => $recipient->id,
            'item_id' => $item->id,
            'mode' => 'copy',
            'status' => 'pending',
            'encrypted_package' => 'package-one',
        ]);
        $second = Share::create([
            'sender_id' => $sender->id,
            'recipient_id' => $recipient->id,
            'item_id' => $item->id,
            'mode' => 'copy',
            'status' => 'pending',
            'encrypted_package' => 'package-two',
        ]);

        $this->actingAs($recipient)
            ->getJson("/api/web/shares/{$first->id}")
            ->assertOk()
            ->assertJsonPath('data.id', $first->id)
            ->assertJsonPath('data.encrypted_package', 'package-one');

        $this->actingAs($recipient)
            ->getJson("/api/web/shares/{$second->id}")
            ->assertOk()
            ->assertJsonPath('data.id', $second->id)
            ->assertJsonPath('data.encrypted_package', 'package-two');
    }

    public function test_only_accepted_contacts_can_receive_shares(): void
    {
        $user = User::factory()->create();
        $other = User::factory()->create();
        Contact::create([
            'user_id' => $user->id,
            'contact_user_id' => $other->id,
            'status' => 'pending',
        ]);
        $item = $user->items()->create([
            'type' => 'document',
            'encrypted_metadata' => base64_encode('doc'),
        ]);

        $this->actingAs($user)
            ->postJson('/api/web/shares', [
                'contact_id' => Contact::first()->id,
                'item_id' => $item->id,
                'mode' => 'copy',
                'encrypted_package' => base64_encode('package'),
            ])
            ->assertNotFound();
    }
}
