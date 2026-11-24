<?php

use App\Http\Controllers\Api\V1\BlobController;
use App\Http\Controllers\Api\V1\ContactController;
use App\Http\Controllers\Api\V1\DeviceController;
use App\Http\Controllers\Api\V1\KeyController;
use App\Http\Controllers\Api\V1\PairingController;
use App\Http\Controllers\Api\V1\ShareController;
use App\Http\Controllers\Api\V1\ShareRequestController;
use App\Http\Controllers\Api\V1\VaultItemController;
use App\Http\Controllers\AppDownloadController;
use App\Http\Controllers\LanguageController;
use App\Http\Controllers\NotificationController;
use App\Http\Controllers\ProfileController;
use App\Http\Controllers\SecurityController;
use App\Http\Controllers\VaultRecoveryController;
use App\Models\VaultItem;
use Illuminate\Support\Facades\Route;

Route::get('/', function () {
    return auth()->check()
        ? redirect()->route('dashboard')
        : view('welcome');
});

Route::get('/language/{locale}', [LanguageController::class, 'switch'])->name('language.switch');

Route::get('/downloads/mypass.apk', [AppDownloadController::class, 'android'])
    ->middleware('throttle:60,1')
    ->name('app.download');

Route::view('/privacy', 'legal.privacy')->name('legal.privacy');
Route::view('/terms', 'legal.terms')->name('legal.terms');

Route::middleware(['auth', 'verified', 'email-2fa'])->group(function () {
    Route::get('/dashboard', function () {
        $user = auth()->user();

        return view('dashboard', [
            'hasVault' => $user->keys()->exists(),
            'twoFactorEnabled' => filled($user->two_factor_confirmed_at),
            'deviceCount' => $user->devices()->whereNull('revoked_at')->count(),
            'contactCount' => $user->contacts()->where('status', 'accepted')->count(),
            'documentCount' => $user->items()
                ->whereIn('type', VaultItem::DOCUMENT_TYPES)
                ->where('state', '!=', 'transferred')
                ->whereNull('deleted_at')
                ->count(),
            'credentialCount' => $user->items()
                ->where('type', VaultItem::TYPE_CREDENTIAL)
                ->where('state', '!=', 'transferred')
                ->whereNull('deleted_at')
                ->count(),
            'medicalCount' => $user->items()
                ->where('type', VaultItem::TYPE_MEDICAL_CARD)
                ->where('state', '!=', 'transferred')
                ->whereNull('deleted_at')
                ->count(),
            'passCount' => $user->items()
                ->whereIn('type', VaultItem::PASS_TYPES)
                ->where('state', '!=', 'transferred')
                ->whereNull('deleted_at')
                ->count(),
            'expiringCount' => $user->items()
                ->whereNotNull('expires_at')
                ->where('state', '!=', 'transferred')
                ->where('expires_at', '<=', now()->addDays(30))
                ->count(),
        ]);
    })->name('dashboard');
    Route::view('/onboarding', 'onboarding')->name('onboarding');
    Route::view('/vault', 'vault')->name('vault');

    Route::redirect('/passes', '/vault?tab=pases')->name('passes');
    Route::redirect('/passes/generator', '/vault?tab=generador')->name('passes.generator');

    Route::get('/notifications', [NotificationController::class, 'index'])->name('notifications.index');
    Route::post('/notifications/read-all', [NotificationController::class, 'markAllRead'])->name('notifications.read-all');
    Route::post('/notifications/{notification}/read', [NotificationController::class, 'markRead'])->name('notifications.read');

    Route::get('/settings/profile', [ProfileController::class, 'show'])->name('settings.profile');
    Route::post('/settings/profile/birth-date', [ProfileController::class, 'updateBirthDate'])->name('settings.profile.birth-date.update');
    Route::post('/settings/profile/avatar', [ProfileController::class, 'updateAvatar'])->name('settings.profile.avatar.update');
    Route::delete('/settings/profile/avatar', [ProfileController::class, 'destroyAvatar'])->name('settings.profile.avatar.destroy');
    Route::delete('/settings/account', [ProfileController::class, 'destroy'])->name('settings.account.destroy');
    Route::get('/settings/security', [SecurityController::class, 'show'])->name('settings.security');
    Route::get('/api/web/security', [SecurityController::class, 'status'])->name('web.security.status');
    Route::post('/api/web/security/email-two-factor', [SecurityController::class, 'updateEmailTwoFactor'])
        ->name('web.security.email-two-factor');
    Route::post('/api/web/security/preferences', [SecurityController::class, 'updatePreferences'])
        ->name('web.security.preferences');

    Route::get('/api/web/keys', [KeyController::class, 'show'])->name('web.keys.show');
    Route::put('/api/web/keys', [KeyController::class, 'update'])->name('web.keys.update');

    Route::get('/api/web/vault-recovery/status', [VaultRecoveryController::class, 'status'])->name('web.vault-recovery.status');
    Route::post('/api/web/vault-recovery/request', [VaultRecoveryController::class, 'requestCode'])->name('web.vault-recovery.request');
    Route::post('/api/web/vault-recovery/verify', [VaultRecoveryController::class, 'verifyCode'])->name('web.vault-recovery.verify');
    Route::post('/api/web/vault-recovery/enable', [VaultRecoveryController::class, 'enable'])->name('web.vault-recovery.enable');
    Route::post('/api/web/vault-recovery/disable', [VaultRecoveryController::class, 'disable'])->name('web.vault-recovery.disable');

    Route::get('/api/web/items', [VaultItemController::class, 'index'])->name('web.items.index');
    Route::post('/api/web/items', [VaultItemController::class, 'store'])->name('web.items.store');
    Route::get('/api/web/items/{item}', [VaultItemController::class, 'show'])->name('web.items.show');
    Route::patch('/api/web/items/{item}', [VaultItemController::class, 'update'])->name('web.items.update');
    Route::delete('/api/web/items/{item}', [VaultItemController::class, 'destroy'])->name('web.items.destroy');

    Route::post('/api/web/blobs', [BlobController::class, 'store'])->name('web.blobs.store');
    Route::get('/api/web/blobs/{blob}', [BlobController::class, 'show'])->name('web.blobs.show');

    Route::get('/api/web/devices', [DeviceController::class, 'index'])->name('web.devices.index');
    Route::post('/api/web/devices/{device}/revoke', [DeviceController::class, 'revoke'])->name('web.devices.revoke');

    Route::get('/api/web/contacts', [ContactController::class, 'index'])->name('web.contacts.index');
    Route::post('/api/web/contacts', [ContactController::class, 'store'])->name('web.contacts.store');
    Route::post('/api/web/contacts/{contact}/accept', [ContactController::class, 'accept'])->name('web.contacts.accept');
    Route::post('/api/web/contacts/{contact}/block', [ContactController::class, 'block'])->name('web.contacts.block');
    Route::get('/api/web/contacts/{contact}/public-key', [ContactController::class, 'publicKey'])->name('web.contacts.public-key');

    Route::get('/api/web/shares', [ShareController::class, 'index'])->name('web.shares.index');
    Route::post('/api/web/shares', [ShareController::class, 'store'])->name('web.shares.store');
    Route::get('/api/web/shares/{share}', [ShareController::class, 'show'])->name('web.shares.show');
    Route::post('/api/web/shares/{share}/accept', [ShareController::class, 'accept'])->name('web.shares.accept');
    Route::post('/api/web/shares/{share}/decline', [ShareController::class, 'decline'])->name('web.shares.decline');
    Route::post('/api/web/shares/{share}/revoke', [ShareController::class, 'revoke'])->name('web.shares.revoke');

    Route::get('/api/web/share-requests', [ShareRequestController::class, 'index'])->name('web.share-requests.index');
    Route::post('/api/web/share-requests', [ShareRequestController::class, 'store'])->name('web.share-requests.store');
    Route::post('/api/web/share-requests/{shareRequest}/decline', [ShareRequestController::class, 'decline'])->name('web.share-requests.decline');
    Route::post('/api/web/share-requests/{shareRequest}/cancel', [ShareRequestController::class, 'cancel'])->name('web.share-requests.cancel');

    Route::post('/api/web/pairing', [PairingController::class, 'create'])->name('web.pairing.create');
    Route::get('/api/web/pairing/{session}', [PairingController::class, 'show'])->name('web.pairing.show');
    Route::post('/api/web/pairing/{session}/approve', [PairingController::class, 'approve'])->name('web.pairing.approve');
});

Route::middleware('throttle:10,1')->group(function () {
    Route::get('/email-two-factor', [SecurityController::class, 'showEmailChallenge'])
        ->name('email-two-factor.challenge');
    Route::post('/email-two-factor/send', [SecurityController::class, 'sendEmailCode'])
        ->name('email-two-factor.send');
    Route::post('/email-two-factor/verify', [SecurityController::class, 'verifyEmailCode'])
        ->name('email-two-factor.verify');
});
