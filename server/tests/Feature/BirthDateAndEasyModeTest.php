<?php

namespace Tests\Feature;

use App\Models\User;
use App\Models\UserKey;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Notification;
use Tests\TestCase;

class BirthDateAndEasyModeTest extends TestCase
{
    use RefreshDatabase;

    public function test_web_registration_sets_easy_mode_for_senior_users(): void
    {
        Notification::fake();

        $this->post('/register', [
            'name' => 'Senior User',
            'email' => 'senior@example.com',
            'password' => 'Password12345!',
            'password_confirmation' => 'Password12345!',
            'birth_date' => now()->subYears(60)->toDateString(),
        ])->assertRedirect('/dashboard');

        $user = User::firstWhere('email', 'senior@example.com');

        $this->assertNotNull($user);
        $this->assertSame('senior', $user->age_group);
        $this->assertTrue($user->simple_mode);
        $this->assertTrue($user->usesEasyMode());
        $this->assertNull($user->easy_mode);
    }

    public function test_web_registration_keeps_standard_mode_for_adult_users(): void
    {
        Notification::fake();

        $this->post('/register', [
            'name' => 'Adult User',
            'email' => 'adult@example.com',
            'password' => 'Password12345!',
            'password_confirmation' => 'Password12345!',
            'birth_date' => now()->subYears(30)->toDateString(),
        ])->assertRedirect('/dashboard');

        $user = User::firstWhere('email', 'adult@example.com');

        $this->assertNotNull($user);
        $this->assertSame('adult', $user->age_group);
        $this->assertFalse($user->simple_mode);
        $this->assertFalse($user->usesEasyMode());
    }

    public function test_registration_rejects_underage_users(): void
    {
        Notification::fake();

        $this->post('/register', [
            'name' => 'Minor User',
            'email' => 'minor@example.com',
            'password' => 'Password12345!',
            'password_confirmation' => 'Password12345!',
            'birth_date' => now()->subYears(17)->toDateString(),
        ])->assertSessionHasErrors('birth_date');

        $this->assertDatabaseMissing('users', ['email' => 'minor@example.com']);
    }

    public function test_existing_user_can_add_a_birth_date(): void
    {
        $user = User::factory()->create([
            'birth_date' => null,
            'simple_mode' => true,
            'easy_mode' => null,
        ]);

        $this->actingAs($user)
            ->post(route('settings.profile.birth-date.update'), [
                'birth_date' => now()->subYears(64)->toDateString(),
            ])
            ->assertRedirect();

        $user->refresh();

        $this->assertSame('senior', $user->age_group);
        $this->assertTrue($user->usesEasyMode());
    }

    public function test_user_can_override_easy_mode(): void
    {
        $user = User::factory()->create([
            'birth_date' => now()->subYears(60)->toDateString(),
            'simple_mode' => true,
            'easy_mode' => null,
        ]);

        $this->actingAs($user)
            ->postJson(route('web.security.preferences'), [
                'locale' => 'es-MX',
                'simple_mode' => true,
                'easy_mode' => false,
            ])
            ->assertOk()
            ->assertJsonPath('data.easy_mode', false);

        $this->assertFalse($user->fresh()->usesEasyMode());
    }

    public function test_api_registration_and_me_expose_age_group_and_easy_mode(): void
    {
        Notification::fake();

        $this->postJson('/api/v1/auth/register', [
            'name' => 'Senior API',
            'email' => 'senior-api@example.com',
            'password' => 'Password12345!',
            'password_confirmation' => 'Password12345!',
            'birth_date' => now()->subYears(58)->toDateString(),
            'locale' => 'es-MX',
        ])->assertCreated()
            ->assertJsonPath('user.age_group', 'senior')
            ->assertJsonPath('user.easy_mode', true)
            ->assertJsonPath('user.simple_mode', true);

        $user = User::firstWhere('email', 'senior-api@example.com');
        $user->forceFill(['email_verified_at' => now()])->save();

        $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/me')
            ->assertOk()
            ->assertJsonPath('user.age_group', 'senior')
            ->assertJsonPath('user.easy_mode', true);
    }

    public function test_api_rejects_underage_registration(): void
    {
        Notification::fake();

        $this->postJson('/api/v1/auth/register', [
            'name' => 'Minor API',
            'email' => 'minor-api@example.com',
            'password' => 'Password12345!',
            'password_confirmation' => 'Password12345!',
            'birth_date' => now()->subYears(17)->toDateString(),
        ])->assertStatus(422)
            ->assertJsonValidationErrors('birth_date');
    }

    public function test_registration_page_renders_the_birth_date_wheel(): void
    {
        $this->get('/register')
            ->assertOk()
            ->assertSee('data-date-wheel', false)
            ->assertSee('Fecha de nacimiento');
    }

    public function test_senior_pages_render_with_easy_mode(): void
    {
        $user = User::factory()->create([
            'birth_date' => now()->subYears(66)->toDateString(),
            'simple_mode' => true,
            'easy_mode' => null,
        ]);
        UserKey::create([
            'user_id' => $user->id,
            'pc_wrapped_master_key' => 'wrapped-pc',
            'pc_kdf_salt' => 'salt',
            'pc_kdf_params' => ['ops_limit' => 1, 'mem_limit' => 1],
        ]);

        $this->actingAs($user)->get('/dashboard')->assertOk()->assertSee('easy-mode', false);
        $this->actingAs($user)->get('/vault')->assertOk()->assertSee('easy-mode', false);
        $this->actingAs($user)->get('/settings/profile')->assertOk()->assertSee('Modo fácil activo');
        $this->actingAs($user)->get('/settings/security')->assertOk()->assertSee('Recuperación por correo');
    }

    public function test_android_can_update_birth_date_and_preferences(): void
    {
        $user = User::factory()->create([
            'birth_date' => null,
            'simple_mode' => false,
            'easy_mode' => null,
        ]);

        $this->actingAs($user, 'sanctum')
            ->patchJson('/api/v1/me/profile', [
                'birth_date' => now()->subYears(70)->toDateString(),
            ])
            ->assertOk()
            ->assertJsonPath('data.age_group', 'senior')
            ->assertJsonPath('data.easy_mode', true);

        $this->actingAs($user, 'sanctum')
            ->patchJson('/api/v1/me/preferences', [
                'simple_mode' => false,
                'easy_mode' => false,
            ])
            ->assertOk()
            ->assertJsonPath('data.easy_mode', false);
    }
}
