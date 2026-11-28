<?php

namespace App\Console\Commands;

use App\Models\UserNotification;
use App\Models\VaultItem;
use App\Services\AppNotifications;
use Illuminate\Console\Command;

class SendInsuranceReminders extends Command
{
    protected $signature = 'mypass:insurance-reminders';

    protected $description = 'Crea avisos y Web Push para las pólizas de seguro próximas a vencer.';

    public function handle(): int
    {
        $today = now()->startOfDay();
        $limit = now()->addDays(7)->endOfDay();
        $created = 0;

        VaultItem::query()
            ->where('type', VaultItem::TYPE_INSURANCE)
            ->whereNull('deleted_at')
            ->where('state', '!=', 'transferred')
            ->whereNotNull('expires_at')
            ->whereBetween('expires_at', [$today, $limit])
            ->with('user')
            ->orderBy('id')
            ->chunkById(100, function ($items) use (&$created) {
                foreach ($items as $item) {
                    if ($item->user === null || $item->expires_at === null) {
                        continue;
                    }

                    $expiresAt = $item->expires_at->toDateString();
                    $alreadySent = UserNotification::query()
                        ->where('user_id', $item->user_id)
                        ->where('type', 'insurance_expiring')
                        ->where('data->item_id', $item->id)
                        ->where('data->expires_at', $expiresAt)
                        ->exists();

                    if ($alreadySent) {
                        continue;
                    }

                    $days = max(0, now()->startOfDay()->diffInDays($item->expires_at->copy()->startOfDay(), false));

                    AppNotifications::create($item->user, 'insurance_expiring', [
                        'item_id' => $item->id,
                        'expires_at' => $expiresAt,
                        'days' => $days,
                    ]);

                    $created++;
                }
            });

        $this->info("Avisos de seguros creados: {$created}");

        return self::SUCCESS;
    }
}
