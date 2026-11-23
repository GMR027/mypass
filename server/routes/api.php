<?php

use App\Http\Controllers\Api\V1\AuthController;
use App\Http\Controllers\Api\V1\BlobController;
use App\Http\Controllers\Api\V1\ContactController;
use App\Http\Controllers\Api\V1\DeviceController;
use App\Http\Controllers\Api\V1\KeyController;
use App\Http\Controllers\Api\V1\NotificationController;
use App\Http\Controllers\Api\V1\PairingController;
use App\Http\Controllers\Api\V1\ProfileController;
use App\Http\Controllers\Api\V1\ShareController;
use App\Http\Controllers\Api\V1\ShareRequestController;
use App\Http\Controllers\Api\V1\TwoFactorController;
use App\Http\Controllers\Api\V1\VaultItemController;
use Illuminate\Support\Facades\Route;

Route::prefix('v1')->group(function () {
    Route::get('/health', fn () => response()->json([
        'status' => 'ok',
        'app' => 'mypass',
        'version' => '0.3.0',
        'time' => now()->toIso8601String(),
    ]));

    Route::middleware('throttle:10,1')->group(function () {
        Route::post('/auth/register', [AuthController::class, 'register']);
        Route::post('/auth/login', [AuthController::class, 'login']);
        Route::post('/auth/two-factor', [TwoFactorController::class, 'verify']);
    });

    Route::post('/auth/two-factor/email', [TwoFactorController::class, 'sendEmail'])
        ->middleware('throttle:3,1');

    Route::get('/avatars/{user:public_id}', [ProfileController::class, 'showAvatar'])
        ->middleware('throttle:60,1')
        ->name('api.avatars.show');

    Route::middleware(['auth:sanctum', 'throttle:120,1'])->group(function () {
        Route::get('/me', [AuthController::class, 'me']);
        Route::post('/auth/logout', [AuthController::class, 'logout']);
        Route::post('/me/avatar', [ProfileController::class, 'updateAvatar']);
        Route::delete('/me/avatar', [ProfileController::class, 'destroyAvatar']);

        Route::get('/notifications', [NotificationController::class, 'index']);
        Route::post('/notifications/read-all', [NotificationController::class, 'markAllRead']);
        Route::post('/notifications/{notification}/read', [NotificationController::class, 'markRead']);

        Route::get('/devices', [DeviceController::class, 'index']);
        Route::get('/devices/{device}', [DeviceController::class, 'show']);
        Route::post('/devices', [DeviceController::class, 'store']);
        Route::patch('/devices/{device}', [DeviceController::class, 'update']);
        Route::post('/devices/{device}/revoke', [DeviceController::class, 'revoke']);

        Route::get('/keys', [KeyController::class, 'show']);
        Route::put('/keys', [KeyController::class, 'update']);

        Route::get('/contacts', [ContactController::class, 'index']);
        Route::post('/contacts', [ContactController::class, 'store']);
        Route::post('/contacts/{contact}/accept', [ContactController::class, 'accept']);
        Route::post('/contacts/{contact}/block', [ContactController::class, 'block']);
        Route::get('/contacts/{contact}/public-key', [ContactController::class, 'publicKey']);

        Route::get('/shares', [ShareController::class, 'index']);
        Route::post('/shares', [ShareController::class, 'store']);
        Route::get('/shares/{share}', [ShareController::class, 'show']);
        Route::post('/shares/{share}/accept', [ShareController::class, 'accept']);
        Route::post('/shares/{share}/decline', [ShareController::class, 'decline']);
        Route::post('/shares/{share}/revoke', [ShareController::class, 'revoke']);

        Route::get('/share-requests', [ShareRequestController::class, 'index']);
        Route::post('/share-requests', [ShareRequestController::class, 'store']);
        Route::post('/share-requests/{shareRequest}/decline', [ShareRequestController::class, 'decline']);
        Route::post('/share-requests/{shareRequest}/cancel', [ShareRequestController::class, 'cancel']);

        Route::post('/pairing/join', [PairingController::class, 'join'])->middleware('throttle:10,1');
        Route::get('/pairing/{session}', [PairingController::class, 'androidShow']);

        Route::post('/blobs', [BlobController::class, 'store']);
        Route::get('/blobs/{blob}', [BlobController::class, 'show']);

        Route::apiResource('items', VaultItemController::class)
            ->only(['index', 'store', 'show', 'update', 'destroy']);
    });
});
