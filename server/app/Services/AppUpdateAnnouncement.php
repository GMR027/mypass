<?php

namespace App\Services;

use App\Models\User;
use App\Models\UserNotification;
use Illuminate\Support\Facades\Storage;

class AppUpdateAnnouncement
{
    /**
     * @return array<string, mixed>|null
     */
    public function latest(): ?array
    {
        $path = 'downloads/version.json';

        if (! Storage::disk('local')->exists($path)) {
            return null;
        }

        $version = json_decode(Storage::disk('local')->get($path), true);

        if (! is_array($version) || empty($version['versionCode']) || empty($version['versionName'])) {
            return null;
        }

        return $version;
    }

    public function ensureFor(User $user, ?array $version = null): ?UserNotification
    {
        $version ??= $this->latest();

        if ($version === null) {
            return null;
        }

        $versionCode = (int) $version['versionCode'];
        $already = UserNotification::where('user_id', $user->id)
            ->where('type', 'app_update')
            ->where('data->version_code', $versionCode)
            ->exists();

        if ($already) {
            return null;
        }

        return AppNotifications::create($user, 'app_update', [
            'version_code' => $versionCode,
            'version_name' => (string) $version['versionName'],
            'notes' => is_array($version['notes'] ?? null) ? $version['notes'] : [],
            'apk_url' => $version['apkUrl'] ?? '/downloads/mypass.apk',
        ]);
    }

    public function announceAll(bool $force = false): int
    {
        $version = $this->latest();

        if ($version === null) {
            return 0;
        }

        $created = 0;

        User::query()
            ->orderBy('id')
            ->chunkById(200, function ($users) use ($version, $force, &$created) {
                foreach ($users as $user) {
                    if ($force) {
                        $versionCode = (int) $version['versionCode'];
                        $already = UserNotification::where('user_id', $user->id)
                            ->where('type', 'app_update')
                            ->where('data->version_code', $versionCode)
                            ->exists();

                        if ($already) {
                            continue;
                        }
                    }

                    if ($this->ensureFor($user, $version) !== null) {
                        $created++;
                    }
                }
            });

        return $created;
    }
}
