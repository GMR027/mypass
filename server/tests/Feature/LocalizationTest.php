<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class LocalizationTest extends TestCase
{
    use RefreshDatabase;

    public function test_guests_can_switch_the_website_language(): void
    {
        $this->from('/')
            ->get('/language/en')
            ->assertRedirect('/');

        $this->get('/')
            ->assertOk()
            ->assertSee('Create my vault');

        $this->get('/login')
            ->assertOk()
            ->assertSee('Sign in');
    }

    public function test_invalid_language_is_rejected(): void
    {
        $this->get('/language/fr')->assertNotFound();
    }

    public function test_authenticated_user_language_is_persisted(): void
    {
        $user = User::factory()->create(['locale' => 'es-MX']);

        $this->actingAs($user)
            ->from('/dashboard')
            ->get('/language/en')
            ->assertRedirect('/dashboard');

        $this->assertSame('en', $user->fresh()->locale);

        $this->actingAs($user)
            ->get('/dashboard')
            ->assertOk()
            ->assertSee('My vault');
    }

    public function test_vault_page_uses_the_selected_language(): void
    {
        $user = User::factory()->create(['locale' => 'en']);

        $this->actingAs($user)
            ->get('/vault')
            ->assertOk()
            ->assertSee('Add document');

        $user->forceFill(['locale' => 'es-MX'])->save();

        $this->actingAs($user)
            ->get('/vault')
            ->assertOk()
            ->assertSee('Agregar documento')
            ->assertDontSee('Add document');
    }

    public function test_api_messages_use_the_web_user_language(): void
    {
        $user = User::factory()->create(['locale' => 'en']);

        $this->actingAs($user)
            ->postJson('/api/web/security/email-two-factor', ['enabled' => true])
            ->assertOk()
            ->assertJsonPath('message', 'Email code enabled.');
    }

    public function test_registration_inherits_the_session_language(): void
    {
        $this->withSession(['locale' => 'en'])
            ->post('/register', [
                'name' => 'English User',
                'email' => 'english-register@example.com',
                'password' => 'Password12345!',
                'password_confirmation' => 'Password12345!',
                'birth_date' => now()->subYears(30)->toDateString(),
            ])
            ->assertRedirect();

        $this->assertSame('en', User::firstWhere('email', 'english-register@example.com')->locale);
    }
}
