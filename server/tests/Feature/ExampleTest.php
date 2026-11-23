<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ExampleTest extends TestCase
{
    use RefreshDatabase;

    /**
     * A basic test example.
     */
    public function test_guests_see_the_landing_page(): void
    {
        $this->get('/')
            ->assertOk()
            ->assertSee('My Pass')
            ->assertSee('Crear mi bóveda');
    }

    public function test_authenticated_users_are_redirected_to_the_dashboard(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user)
            ->get('/')
            ->assertRedirect(route('dashboard'));
    }

    public function test_login_redirects_to_the_pass_wallet(): void
    {
        $user = User::factory()->create([
            'password' => bcrypt('Password12345!'),
        ]);

        $this->post('/login', [
            'email' => $user->email,
            'password' => 'Password12345!',
        ])->assertRedirect('/vault?tab=pases');

        $this->assertAuthenticatedAs($user);
    }
}
