<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class PassesPageTest extends TestCase
{
    use RefreshDatabase;

    public function test_guests_are_redirected_to_login_from_passes(): void
    {
        $this->get('/passes')->assertRedirect('/login');
        $this->get('/passes/generator')->assertRedirect('/login');
    }

    public function test_passes_routes_open_the_passes_tab(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user)
            ->get('/passes')
            ->assertRedirect('/vault?tab=pases');

        $this->actingAs($user)
            ->get('/passes/generator')
            ->assertRedirect('/vault?tab=generador');
    }

    public function test_vault_renders_the_credentials_and_medical_panels(): void
    {
        $user = User::factory()->create(['locale' => 'es-MX']);

        $this->actingAs($user)
            ->get('/vault?tab=credentials')
            ->assertOk()
            ->assertSee('Tus credenciales')
            ->assertSee('Agregar credencial')
            ->assertSee('Secreto TOTP')
            ->assertSee('Carnets médicos')
            ->assertSee('Agregar carnet');

        $user->forceFill(['locale' => 'en'])->save();

        $this->actingAs($user)
            ->get('/vault?tab=medical')
            ->assertOk()
            ->assertSee('Your credentials')
            ->assertSee('Add credential')
            ->assertSee('TOTP secret')
            ->assertSee('Medical cards')
            ->assertSee('Add card');
    }

    public function test_vault_renders_the_passes_workspace(): void
    {
        $user = User::factory()->create(['locale' => 'es-MX']);

        $this->actingAs($user)
            ->get('/vault?tab=pases')
            ->assertOk()
            ->assertSee('Mis Pases y Boletos')
            ->assertSee('Generador y editor de pases')
            ->assertSee('Agregar pase')
            ->assertSee('Textura de la tarjeta')
            ->assertSee('Opacidad de la textura')
            ->assertSee('Imagen de fondo')
            ->assertSee('Oscurecer imagen');

        $user->forceFill(['locale' => 'en'])->save();

        $this->actingAs($user)
            ->get('/vault?tab=pases')
            ->assertOk()
            ->assertSee('My Passes and Tickets')
            ->assertSee('Pass generator and editor')
            ->assertSee('Add pass')
            ->assertSee('Card texture')
            ->assertSee('Texture opacity')
            ->assertSee('Background image')
            ->assertSee('Darken image');
    }
}
