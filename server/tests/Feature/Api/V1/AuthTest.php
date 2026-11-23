<?php

namespace Tests\Feature\Api\V1;

use App\Models\User;
use Illuminate\Auth\Notifications\VerifyEmail;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Mail;
use Illuminate\Support\Facades\Notification;
use PragmaRX\Google2FA\Google2FA;
use Tests\TestCase;

class AuthTest extends TestCase
{
    use RefreshDatabase;

    public function test_health_endpoint_is_public(): void
    {
        $this->getJson('/api/v1/health')
            ->assertOk()
            ->assertJsonPath('status', 'ok')
            ->assertJsonPath('app', 'mypass');
    }

    public function test_user_can_register_and_receives_verification_email(): void
    {
        Notification::fake();

        $response = $this->postJson('/api/v1/auth/register', [
            'name' => 'Edgar',
            'email' => 'edgar@example.com',
            'password' => 'Password12345!',
            'password_confirmation' => 'Password12345!',
            'locale' => 'es-MX',
            'simple_mode' => true,
            'device_name' => 'Tablet',
        ]);

        $response->assertCreated()
            ->assertJsonPath('user.email', 'edgar@example.com')
            ->assertJsonPath('user.locale', 'es-MX')
            ->assertJsonPath('user.email_verified', false)
            ->assertJsonPath('email_verified', false)
            ->assertJsonMissingPath('token');

        $user = User::where('email', 'edgar@example.com')->firstOrFail();

        $this->assertNotNull($user->public_id);
        $this->assertNull($user->email_verified_at);
        Notification::assertSentTo($user, VerifyEmail::class);
    }

    public function test_unverified_user_cannot_login(): void
    {
        $user = User::factory()->unverified()->create([
            'email' => 'unverified@example.com',
            'password' => 'Password12345!',
        ]);

        $this->postJson('/api/v1/auth/login', [
            'email' => 'unverified@example.com',
            'password' => 'Password12345!',
            'device_name' => 'Tablet',
        ])->assertForbidden()
            ->assertJsonPath('email_unverified', true);
    }

    public function test_user_can_login_and_logout_with_token(): void
    {
        $user = User::factory()->create([
            'email' => 'login@example.com',
            'password' => 'Password12345!',
        ]);

        $login = $this->postJson('/api/v1/auth/login', [
            'email' => 'login@example.com',
            'password' => 'Password12345!',
            'device_name' => 'Tablet',
        ])->assertOk()->assertJsonStructure(['token']);

        $token = $login->json('token');

        $this->withToken($token)->getJson('/api/v1/me')
            ->assertOk()
            ->assertJsonPath('user.email', 'login@example.com');

        $this->withToken($token)->postJson('/api/v1/auth/logout')->assertOk();

        $this->assertDatabaseCount('personal_access_tokens', 0);
    }

    public function test_login_rejects_wrong_password(): void
    {
        User::factory()->create([
            'email' => 'wrong@example.com',
            'password' => 'Password12345!',
        ]);

        $this->postJson('/api/v1/auth/login', [
            'email' => 'wrong@example.com',
            'password' => 'incorrecta',
        ])->assertStatus(422);
    }

    public function test_totp_two_factor_is_required_and_can_be_verified(): void
    {
        $secret = 'JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP';
        $user = User::factory()->create([
            'email' => 'totp@example.com',
            'password' => 'Password12345!',
            'two_factor_secret' => encrypt($secret),
            'two_factor_confirmed_at' => now(),
        ]);
        $user->forceFill([
            'two_factor_recovery_codes' => encrypt(json_encode(['rec-11111', 'rec-22222'])),
        ])->save();

        $login = $this->postJson('/api/v1/auth/login', [
            'email' => 'totp@example.com',
            'password' => 'Password12345!',
            'device_name' => 'Tablet',
        ])->assertOk()
            ->assertJsonPath('two_factor_required', true)
            ->assertJsonPath('methods.0', 'totp');

        $token = $login->json('challenge_token');
        $code = (new Google2FA)->getCurrentOtp($secret);

        $verified = $this->postJson('/api/v1/auth/two-factor', [
            'challenge_token' => $token,
            'code' => $code,
            'method' => 'totp',
        ])->assertOk()->assertJsonStructure(['token', 'user']);

        $this->withToken($verified->json('token'))->getJson('/api/v1/me')
            ->assertOk()
            ->assertJsonPath('user.email', 'totp@example.com');
    }

    public function test_email_two_factor_code_can_be_verified(): void
    {
        $user = User::factory()->create([
            'email' => 'email2fa@example.com',
            'password' => 'Password12345!',
            'email_two_factor_enabled' => true,
        ]);

        $login = $this->postJson('/api/v1/auth/login', [
            'email' => 'email2fa@example.com',
            'password' => 'Password12345!',
            'device_name' => 'Tablet',
        ])->assertOk()
            ->assertJsonPath('two_factor_required', true)
            ->assertJsonPath('methods.0', 'email');

        $token = $login->json('challenge_token');

        Mail::fake();
        $this->postJson('/api/v1/auth/two-factor/email', [
            'challenge_token' => $token,
        ])->assertOk();

        $challenge = Cache::get('two-factor-challenge:'.$token);
        $challenge['email_hash'] = Hash::make('654321');
        $challenge['email_expires'] = now()->addMinutes(5)->timestamp;
        Cache::put('two-factor-challenge:'.$token, $challenge, now()->addMinutes(10));

        $this->postJson('/api/v1/auth/two-factor', [
            'challenge_token' => $token,
            'code' => '654321',
            'method' => 'email',
        ])->assertOk()->assertJsonStructure(['token']);

        $this->postJson('/api/v1/auth/two-factor', [
            'challenge_token' => $token,
            'code' => '654321',
            'method' => 'email',
        ])->assertStatus(422);
    }
}
