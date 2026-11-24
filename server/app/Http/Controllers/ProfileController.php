<?php

namespace App\Http\Controllers;

use App\Http\Requests\UpdateAvatarRequest;
use App\Rules\AdultBirthDate;
use App\Services\AuditLogger;
use App\Services\UserAvatarService;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Storage;
use Illuminate\View\View;

class ProfileController extends Controller
{
    public function show(Request $request): View
    {
        return view('settings.profile', [
            'user' => $request->user(),
        ]);
    }

    public function destroy(Request $request, AuditLogger $audit, UserAvatarService $avatars): RedirectResponse
    {
        $request->validate([
            'password' => ['required', 'string', 'current_password:web'],
        ], [
            'password.current_password' => __('La contraseña no es correcta.'),
        ]);

        $user = $request->user();

        $audit->log($user, 'account_deleted');
        $user->tokens()->delete();
        Storage::disk('local')->deleteDirectory('vault/'.$user->id);
        $avatars->removeDirectory($user);

        Auth::guard('web')->logout();
        $user->delete();

        $request->session()->invalidate();
        $request->session()->regenerateToken();

        return redirect()->route('login')->with('status', __('Tu cuenta y tus datos se eliminaron.'));
    }

    public function updateBirthDate(Request $request): RedirectResponse
    {
        $data = $request->validateWithBag('updateBirthDate', [
            'birth_date' => ['required', 'date_format:Y-m-d', new AdultBirthDate],
        ]);

        $request->user()->forceFill([
            'birth_date' => $data['birth_date'],
        ])->save();

        return back()->with('status', __('Fecha de nacimiento guardada.'));
    }

    public function updateAvatar(UpdateAvatarRequest $request, UserAvatarService $avatars): RedirectResponse
    {
        /** @var UploadedFile $avatar */
        $avatar = $request->file('avatar');
        $avatars->store($request->user(), $avatar);

        return back()->with('status', __('Foto de perfil actualizada.'));
    }

    public function destroyAvatar(Request $request, UserAvatarService $avatars): RedirectResponse
    {
        $avatars->remove($request->user());

        return back()->with('status', __('Foto de perfil eliminada.'));
    }
}
