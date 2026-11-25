<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class ThemeTest extends TestCase
{
    use RefreshDatabase;

    public function test_dark_theme_cookie_is_applied_server_side(): void
    {
        $this->withUnencryptedCookie('mypass_theme', 'dark')
            ->get('/')
            ->assertOk()
            ->assertSee('data-theme="dark"', false)
            ->assertSee('class="dark"', false);
    }

    public function test_unknown_theme_cookie_falls_back_to_system(): void
    {
        $this->withUnencryptedCookie('mypass_theme', 'neon')
            ->get('/')
            ->assertOk()
            ->assertSee('data-theme="system"', false)
            ->assertDontSee('class="dark"', false);
    }

    public function test_security_settings_offer_the_theme_selector(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user)
            ->get('/settings/security')
            ->assertOk()
            ->assertSee('Apariencia')
            ->assertSee('data-theme-option="system"', false)
            ->assertSee('data-theme-option="light"', false)
            ->assertSee('data-theme-option="dark"', false);
    }
}
