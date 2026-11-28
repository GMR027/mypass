<?php

namespace Tests\Feature;

use App\Models\User;
use App\Models\VaultItem;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class InsuranceReminderTest extends TestCase
{
    use RefreshDatabase;

    public function test_insurance_reminder_is_created_once_per_policy_expiry(): void
    {
        $user = User::factory()->create();
        $item = VaultItem::create([
            'user_id' => $user->id,
            'type' => VaultItem::TYPE_INSURANCE,
            'state' => 'active',
            'encrypted_metadata' => 'poliza-cifrada',
            'version' => 1,
            'expires_at' => now()->addDays(3),
        ]);

        $this->artisan('mypass:insurance-reminders')
            ->expectsOutput('Avisos de seguros creados: 1')
            ->assertSuccessful();

        $this->assertDatabaseCount('user_notifications', 1);
        $this->assertDatabaseHas('user_notifications', [
            'user_id' => $user->id,
            'type' => 'insurance_expiring',
        ]);

        $notification = $user->userNotifications()->firstOrFail();
        $this->assertSame($item->id, $notification->data['item_id']);
        $this->assertSame($item->expires_at->toDateString(), $notification->data['expires_at']);

        $this->artisan('mypass:insurance-reminders')
            ->expectsOutput('Avisos de seguros creados: 0')
            ->assertSuccessful();

        $this->assertDatabaseCount('user_notifications', 1);
    }

    public function test_insurance_reminders_ignore_expired_and_distant_policies(): void
    {
        $user = User::factory()->create();

        VaultItem::create([
            'user_id' => $user->id,
            'type' => VaultItem::TYPE_INSURANCE,
            'state' => 'active',
            'encrypted_metadata' => 'vencida',
            'version' => 1,
            'expires_at' => now()->subDay(),
        ]);

        VaultItem::create([
            'user_id' => $user->id,
            'type' => VaultItem::TYPE_INSURANCE,
            'state' => 'active',
            'encrypted_metadata' => 'lejana',
            'version' => 1,
            'expires_at' => now()->addDays(30),
        ]);

        $this->artisan('mypass:insurance-reminders')
            ->expectsOutput('Avisos de seguros creados: 0')
            ->assertSuccessful();

        $this->assertDatabaseCount('user_notifications', 0);
    }
}
