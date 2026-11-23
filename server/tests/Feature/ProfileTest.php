<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

class ProfileTest extends TestCase
{
    use RefreshDatabase;

    public function test_profile_page_renders_account_details(): void
    {
        $user = User::factory()->create([
            'name' => 'Edgar',
            'email' => 'profile@example.com',
        ]);

        $this->actingAs($user)
            ->get(route('settings.profile'))
            ->assertOk()
            ->assertSee('Edgar')
            ->assertSee('profile@example.com');
    }

    public function test_user_can_update_name_and_email(): void
    {
        $user = User::factory()->create([
            'name' => 'Old Name',
            'email' => 'old@example.com',
        ]);

        $this->actingAs($user)
            ->put(route('user-profile-information.update'), [
                'name' => 'New Name',
                'email' => 'new@example.com',
            ])
            ->assertRedirect();

        $this->assertSame('New Name', $user->fresh()->name);
        $this->assertSame('new@example.com', $user->fresh()->email);
    }

    public function test_user_can_change_password_with_current_password(): void
    {
        $user = User::factory()->create([
            'password' => 'CurrentPassword123!',
        ]);

        $this->actingAs($user)
            ->put(route('user-password.update'), [
                'current_password' => 'CurrentPassword123!',
                'password' => 'NewPassword123!',
                'password_confirmation' => 'NewPassword123!',
            ])
            ->assertRedirect();

        $this->assertTrue(Hash::check('NewPassword123!', $user->fresh()->password));
    }

    public function test_user_can_delete_account_and_encrypted_files(): void
    {
        Storage::fake('local');

        $user = User::factory()->create([
            'password' => 'CurrentPassword123!',
        ]);
        $user->forceFill([
            'avatar_path' => 'avatars/'.$user->public_id.'/avatar.png',
        ])->save();
        $avatarPath = $user->avatar_path;
        Storage::disk('local')->put($avatarPath, 'avatar');
        Storage::disk('local')->put('vault/'.$user->id.'/document.bin', 'ciphertext');

        $this->actingAs($user)
            ->delete(route('settings.account.destroy'), [
                'password' => 'CurrentPassword123!',
            ])
            ->assertRedirect(route('login'));

        $this->assertGuest();
        $this->assertDatabaseMissing('users', ['id' => $user->id]);
        Storage::disk('local')->assertMissing('vault/'.$user->id.'/document.bin');
        Storage::disk('local')->assertMissing($avatarPath);
    }

    public function test_account_is_not_deleted_with_a_wrong_password(): void
    {
        $user = User::factory()->create([
            'password' => 'CurrentPassword123!',
        ]);

        $this->actingAs($user)
            ->delete(route('settings.account.destroy'), [
                'password' => 'WrongPassword123!',
            ])
            ->assertSessionHasErrors('password');

        $this->assertAuthenticatedAs($user);
        $this->assertDatabaseHas('users', ['id' => $user->id]);
    }

    public function test_guest_cannot_access_profile(): void
    {
        $this->get(route('settings.profile'))
            ->assertRedirect(route('login'));
    }
}
