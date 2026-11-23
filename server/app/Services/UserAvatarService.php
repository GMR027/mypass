<?php

namespace App\Services;

use App\Models\User;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

class UserAvatarService
{
    public function store(User $user, UploadedFile $file): void
    {
        $previousPath = $user->avatar_path;
        $extension = $file->guessExtension() ?: 'jpg';
        $path = $file->storeAs(
            'avatars/'.$user->public_id,
            Str::uuid().'.'.$extension,
            'local',
        );

        if ($path === false) {
            throw ValidationException::withMessages([
                'avatar' => __('No se pudo guardar la foto de perfil.'),
            ])->errorBag('updateAvatar');
        }

        $user->forceFill(['avatar_path' => $path])->save();

        if (filled($previousPath) && $previousPath !== $path) {
            Storage::disk('local')->delete($previousPath);
        }
    }

    public function remove(User $user): void
    {
        $path = $user->avatar_path;

        $user->forceFill(['avatar_path' => null])->save();

        if (filled($path)) {
            Storage::disk('local')->delete($path);
        }
    }

    public function removeDirectory(User $user): void
    {
        Storage::disk('local')->deleteDirectory('avatars/'.$user->public_id);
    }
}
