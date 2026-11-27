<?php

namespace Tests\Feature;

use App\Models\PushSubscription;
use App\Models\User;
use App\Models\UserNotification;
use App\Services\AppUpdateAnnouncement;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

class PushSubscriptionTest extends TestCase
{
    use RefreshDatabase;

    public function test_user_can_register_and_remove_a_web_push_subscription(): void
    {
        $user = User::factory()->create();
        $payload = [
            'endpoint' => 'https://push.example.com/subscription/abc',
            'keys' => [
                'p256dh' => base64_encode('public-key'),
                'auth' => base64_encode('auth-token'),
            ],
        ];

        $this->actingAs($user)
            ->postJson('/api/web/push-subscriptions', $payload)
            ->assertCreated();

        $this->assertDatabaseHas('push_subscriptions', [
            'user_id' => $user->id,
            'endpoint' => $payload['endpoint'],
        ]);

        $this->actingAs($user)
            ->getJson('/api/web/push-subscriptions')
            ->assertOk()
            ->assertJsonPath('subscribed', true);

        $this->actingAs($user)
            ->postJson('/api/web/push-subscriptions', $payload)
            ->assertCreated();

        $this->assertSame(1, PushSubscription::where('user_id', $user->id)->count());

        $this->actingAs($user)
            ->deleteJson('/api/web/push-subscriptions', ['endpoint' => $payload['endpoint']])
            ->assertOk();

        $this->assertSame(0, PushSubscription::where('user_id', $user->id)->count());
    }

    public function test_app_update_is_announced_once_per_version(): void
    {
        Storage::fake('local');
        Storage::disk('local')->put('downloads/version.json', json_encode([
            'versionCode' => 99,
            'versionName' => '9.9.9',
            'apkUrl' => 'https://example.com/mypass.apk',
            'sha256' => str_repeat('a', 64),
            'sizeBytes' => 1024,
            'minSupportedVersionCode' => 0,
            'notes' => ['es' => ['Novedad'], 'en' => ['News']],
        ]));

        $user = User::factory()->create();
        $service = app(AppUpdateAnnouncement::class);

        $this->assertNotNull($service->ensureFor($user));
        $this->assertNull($service->ensureFor($user));
        $this->assertSame(1, UserNotification::where('user_id', $user->id)->where('type', 'app_update')->count());
    }
}
