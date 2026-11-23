<?php

namespace Tests\Feature\Api\V1;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Tests\TestCase;

class AvatarTest extends TestCase
{
    use RefreshDatabase;

    private const AVATAR_WEBP = 'UklGRh4AAABXRUJQVlA4TBEAAAAvAUAAAAfQoW5Upv+BiOh/AAA=';

    public function test_user_can_upload_and_remove_an_avatar_through_the_api(): void
    {
        Storage::fake('local');

        $user = User::factory()->create();
        $token = $user->createToken('Tablet')->plainTextToken;

        $this->withToken($token)
            ->getJson('/api/v1/me')
            ->assertOk()
            ->assertJsonPath('user.avatar_url', null);

        $upload = $this->withToken($token)->post('/api/v1/me/avatar', [
            'avatar' => $this->avatarFile(),
        ]);

        $upload->assertOk()->assertJsonStructure(['avatar_url']);
        $avatarUrl = $upload->json('avatar_url');

        $this->assertNotNull($avatarUrl);
        $this->assertNotNull($user->fresh()->avatar_path);
        Storage::disk('local')->assertExists($user->fresh()->avatar_path);

        $this->withToken($token)
            ->getJson('/api/v1/me')
            ->assertOk()
            ->assertJsonPath('user.avatar_url', $avatarUrl);

        $this->get($avatarUrl)->assertOk();

        $this->withToken($token)
            ->deleteJson('/api/v1/me/avatar')
            ->assertOk()
            ->assertJsonPath('avatar_url', null);

        $this->assertNull($user->fresh()->avatar_path);
    }

    public function test_avatar_upload_requires_authentication(): void
    {
        $this->postJson('/api/v1/me/avatar', [])
            ->assertUnauthorized();
    }

    public function test_avatar_upload_rejects_an_invalid_file(): void
    {
        Storage::fake('local');

        $user = User::factory()->create();
        $token = $user->createToken('Tablet')->plainTextToken;

        $this->withToken($token)
            ->post('/api/v1/me/avatar', [
                'avatar' => UploadedFile::fake()->create('notes.txt', 1, 'text/plain'),
            ])
            ->assertStatus(422)
            ->assertJsonValidationErrors('avatar');

        $this->assertNull($user->fresh()->avatar_path);
    }

    public function test_public_avatar_route_returns_not_found_without_a_photo(): void
    {
        $user = User::factory()->create();

        $this->get(route('api.avatars.show', ['user' => $user->public_id]))
            ->assertNotFound();
    }

    private function avatarFile(): UploadedFile
    {
        return UploadedFile::fake()->createWithContent(
            'avatar.webp',
            (string) base64_decode(self::AVATAR_WEBP, true),
        );
    }
}
