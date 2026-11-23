<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

class ProfileAvatarTest extends TestCase
{
    use RefreshDatabase;

    private const AVATAR_PNG = 'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAFklEQVR4nGOUdjZiYGBgYmBgYGBgAAAG0wCU/qEsEgAAAABJRU5ErkJggg==';

    public function test_profile_page_shows_the_avatar_upload_form(): void
    {
        $user = User::factory()->create();

        $this->actingAs($user)
            ->get(route('settings.profile'))
            ->assertOk()
            ->assertSee('name="avatar"', false)
            ->assertSee(route('settings.profile.avatar.update'), false);
    }

    public function test_user_can_upload_and_remove_a_profile_avatar(): void
    {
        Storage::fake('local');

        $user = User::factory()->create();

        $this->actingAs($user)
            ->post(route('settings.profile.avatar.update'), [
                'avatar' => $this->avatarFile(),
            ])
            ->assertRedirect();

        $user->refresh();
        $path = $user->avatar_path;

        $this->assertNotNull($path);
        $this->assertNotNull($user->avatar_url);
        Storage::disk('local')->assertExists($path);

        $response = $this->get(route('api.avatars.show', ['user' => $user->public_id]));
        $response->assertOk();
        $this->assertStringStartsWith('image/', (string) $response->headers->get('Content-Type'));

        $this->actingAs($user)
            ->delete(route('settings.profile.avatar.destroy'))
            ->assertRedirect();

        $this->assertNull($user->fresh()->avatar_path);
        Storage::disk('local')->assertMissing($path);
    }

    public function test_avatar_must_be_a_supported_image(): void
    {
        Storage::fake('local');

        $user = User::factory()->create();

        $this->actingAs($user)
            ->post(route('settings.profile.avatar.update'), [
                'avatar' => UploadedFile::fake()->create('notes.txt', 1, 'text/plain'),
            ])
            ->assertSessionHasErrors('avatar', null, 'updateAvatar');

        $this->assertNull($user->fresh()->avatar_path);
    }

    public function test_guest_cannot_upload_a_profile_avatar(): void
    {
        $this->post(route('settings.profile.avatar.update'), [
            'avatar' => $this->avatarFile(),
        ])->assertRedirect(route('login'));
    }

    private function avatarFile(): UploadedFile
    {
        return UploadedFile::fake()->createWithContent(
            'avatar.png',
            (string) base64_decode(self::AVATAR_PNG, true),
        );
    }
}
