<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Requests\UpdateAvatarRequest;
use App\Models\User;
use App\Services\UserAvatarService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Symfony\Component\HttpFoundation\StreamedResponse;

class ProfileController extends Controller
{
    public function showAvatar(User $user): StreamedResponse
    {
        abort_if(blank($user->avatar_path), 404);

        $disk = Storage::disk('local');
        abort_unless($disk->exists($user->avatar_path), 404);

        return $disk->response($user->avatar_path, null, [
            'Cache-Control' => 'public, max-age=86400',
            'X-Content-Type-Options' => 'nosniff',
        ]);
    }

    public function updateAvatar(UpdateAvatarRequest $request, UserAvatarService $avatars): JsonResponse
    {
        $user = $request->user();

        /** @var UploadedFile $avatar */
        $avatar = $request->file('avatar');
        $avatars->store($user, $avatar);

        return response()->json([
            'avatar_url' => $user->avatar_url,
        ]);
    }

    public function destroyAvatar(Request $request, UserAvatarService $avatars): JsonResponse
    {
        $avatars->remove($request->user());

        return response()->json([
            'avatar_url' => null,
        ]);
    }
}
