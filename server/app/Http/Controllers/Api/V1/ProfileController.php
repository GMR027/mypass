<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Requests\UpdateAvatarRequest;
use App\Models\User;
use App\Rules\AdultBirthDate;
use App\Services\UserAvatarService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Carbon;
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

    public function updateProfile(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name' => ['sometimes', 'string', 'max:120'],
            'birth_date' => ['sometimes', 'date_format:Y-m-d', new AdultBirthDate],
            'simple_mode' => ['sometimes', 'boolean'],
        ]);

        $user = $request->user();
        $updates = [];

        if (array_key_exists('name', $data)) {
            $updates['name'] = $data['name'];
        }

        if (array_key_exists('birth_date', $data)) {
            $updates['birth_date'] = $data['birth_date'];
        }

        if (array_key_exists('simple_mode', $data)) {
            $updates['simple_mode'] = $data['simple_mode'];
        } elseif (array_key_exists('birth_date', $data)) {
            $updates['simple_mode'] = Carbon::parse($data['birth_date'])->age >= User::SENIOR_AGE;
        }

        $user->forceFill($updates)->save();

        return response()->json([
            'data' => [
                'name' => $user->name,
                'birth_date' => $user->birth_date?->toDateString(),
                'age_group' => $user->age_group,
                'simple_mode' => $user->simple_mode,
                'easy_mode' => $user->usesEasyMode(),
            ],
        ]);
    }

    public function updatePreferences(Request $request): JsonResponse
    {
        $data = $request->validate([
            'locale' => ['sometimes', 'string', 'in:es-MX,en'],
            'simple_mode' => ['sometimes', 'boolean'],
            'easy_mode' => ['sometimes', 'nullable', 'boolean'],
        ]);

        $user = $request->user();
        $updates = [];

        foreach (['locale', 'simple_mode', 'easy_mode'] as $field) {
            if (array_key_exists($field, $data)) {
                $updates[$field] = $data[$field];
            }
        }

        $user->forceFill($updates)->save();

        if (array_key_exists('locale', $data)) {
            app()->setLocale($data['locale']);
        }

        return response()->json([
            'message' => __('Preferencias guardadas.'),
            'data' => [
                'locale' => $user->locale,
                'simple_mode' => $user->simple_mode,
                'easy_mode' => $user->usesEasyMode(),
                'age_group' => $user->age_group,
            ],
        ]);
    }
}
