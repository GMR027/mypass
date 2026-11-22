<?php

namespace Tests\Feature;

// use Illuminate\Foundation\Testing\RefreshDatabase;
use App\Models\User;
use Tests\TestCase;

class ExampleTest extends TestCase
{
    /**
     * A basic test example.
     */
    public function test_guests_see_the_landing_page(): void
    {
        $this->get('/')
            ->assertOk()
            ->assertSee('mypass')
            ->assertSee('Crear mi bóveda');
    }

    public function test_authenticated_users_are_redirected_to_the_dashboard(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user)
            ->get('/')
            ->assertRedirect(route('dashboard'));
    }
}
