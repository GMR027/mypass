<?php

namespace Tests\Feature;

use App\Models\User;
use App\Models\UserNotification;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class NotificationTest extends TestCase
{
    use RefreshDatabase;

    public function test_web_notifications_page_lists_and_marks_notifications_as_read(): void
    {
        $user = User::factory()->create();
        $notification = UserNotification::create([
            'user_id' => $user->id,
            'type' => 'contact_request',
            'data' => ['from' => 'Ana'],
        ]);

        $this->actingAs($user)
            ->get(route('notifications.index'))
            ->assertOk()
            ->assertSee('Solicitud de contacto')
            ->assertSee('Ana');

        $this->actingAs($user)
            ->post(route('notifications.read', $notification))
            ->assertRedirect();

        $this->assertNotNull($notification->fresh()->read_at);
    }

    public function test_api_notifications_can_be_listed_and_marked_as_read(): void
    {
        $user = User::factory()->create();
        $notification = UserNotification::create([
            'user_id' => $user->id,
            'type' => 'share_received',
            'data' => ['from' => 'Luis', 'item_type' => 'ticket'],
        ]);

        $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/notifications')
            ->assertOk()
            ->assertJsonPath('unread', 1)
            ->assertJsonPath('data.0.type', 'share_received');

        $this->actingAs($user, 'sanctum')
            ->postJson("/api/v1/notifications/{$notification->id}/read")
            ->assertOk();

        $this->assertNotNull($notification->fresh()->read_at);
    }

    public function test_user_cannot_mark_another_users_notification_as_read(): void
    {
        $owner = User::factory()->create();
        $other = User::factory()->create();
        $notification = UserNotification::create([
            'user_id' => $owner->id,
            'type' => 'contact_request',
            'data' => ['from' => 'Ana'],
        ]);

        $this->actingAs($other, 'sanctum')
            ->postJson("/api/v1/notifications/{$notification->id}/read")
            ->assertNotFound();
    }
}
