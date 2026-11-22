<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use PragmaRX\Google2FA\Google2FA;
use Tests\TestCase;

class SecurityTest extends TestCase
{
    use RefreshDatabase;

    public function test_totp_can_be_enabled_and_confirmed(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user)
            ->postJson('/user/two-factor-authentication')
            ->assertOk();

        $user->refresh();
        $this->assertNotNull($user->two_factor_secret);

        $secret = decrypt($user->two_factor_secret);
        $this->getJson('/user/two-factor-qr-code')
            ->assertOk()
            ->assertJsonStructure(['svg', 'url']);

        $code = (new Google2FA)->getCurrentOtp($secret);

        $this->postJson('/user/confirmed-two-factor-authentication', [
            'code' => $code,
        ])->assertOk();

        $this->assertNotNull($user->fresh()->two_factor_confirmed_at);
    }

    public function test_totp_is_required_at_login(): void
    {
        $secret = 'JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP';
        $user = User::factory()->create([
            'email' => 'totp-web@example.com',
            'password' => 'Password12345!',
            'two_factor_secret' => encrypt($secret),
            'two_factor_confirmed_at' => now(),
            'email_two_factor_enabled' => true,
        ]);
        $user->forceFill([
            'two_factor_recovery_codes' => encrypt(json_encode(['rec-11111', 'rec-22222'])),
        ])->save();

        $this->post('/login', [
            'email' => 'totp-web@example.com',
            'password' => 'Password12345!',
        ])->assertRedirect(route('two-factor.login'));

        $this->assertGuest();

        $code = (new Google2FA)->getCurrentOtp($secret);

        $this->post('/two-factor-challenge', [
            'code' => $code,
        ])->assertRedirect();

        $this->assertAuthenticatedAs($user);

        $this->get('/dashboard')->assertOk();
    }

    public function test_web_login_is_recorded_in_the_audit_log(): void
    {
        $user = User::factory()->create([
            'email' => 'audit@example.com',
            'password' => 'Password12345!',
        ]);

        $this->post('/login', [
            'email' => 'audit@example.com',
            'password' => 'Password12345!',
        ])->assertRedirect();

        $this->assertDatabaseHas('audit_events', [
            'user_id' => $user->id,
            'event' => 'login_web',
        ]);
    }

    public function test_email_two_factor_can_be_enabled_from_settings(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user)
            ->postJson(route('web.security.email-two-factor'), ['enabled' => true])
            ->assertOk()
            ->assertJsonPath('data.email_enabled', true);

        $this->assertTrue($user->fresh()->email_two_factor_enabled);
    }

    public function test_email_two_factor_blocks_login_until_code_is_verified(): void
    {
        $user = User::factory()->create([
            'email' => 'email-web@example.com',
            'password' => 'Password12345!',
            'email_two_factor_enabled' => true,
        ]);

        $this->post('/login', [
            'email' => 'email-web@example.com',
            'password' => 'Password12345!',
        ])->assertRedirect();

        $this->get('/dashboard')
            ->assertRedirect(route('email-two-factor.challenge'));

        $this->assertGuest();

        $this->withSession([
            'email_two_factor_user_id' => $user->id,
            'email_two_factor_code_hash' => Hash::make('123456'),
            'email_two_factor_code_expires' => now()->addMinutes(10)->timestamp,
        ])->postJson(route('email-two-factor.verify'), [
            'code' => '123456',
        ])->assertOk()
            ->assertJsonPath('redirect', route('dashboard'));

        $this->assertAuthenticatedAs($user);

        $this->get('/dashboard')->assertOk();
    }

    public function test_email_two_factor_rejects_wrong_code(): void
    {
        $user = User::factory()->create([
            'email_two_factor_enabled' => true,
        ]);

        $this->withSession([
            'email_two_factor_user_id' => $user->id,
            'email_two_factor_code_hash' => Hash::make('123456'),
            'email_two_factor_code_expires' => now()->addMinutes(10)->timestamp,
        ])->postJson(route('email-two-factor.verify'), [
            'code' => '000000',
        ])->assertStatus(422);

        $this->assertGuest();
    }
}
