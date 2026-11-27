<?php

namespace App\Console\Commands;

use App\Services\AppUpdateAnnouncement;
use Illuminate\Console\Command;

class AnnounceAppUpdate extends Command
{
    protected $signature = 'mypass:announce-update
                            {--force : Notifica aunque ya exista el aviso para esa versión}';

    protected $description = 'Crea el aviso de nueva versión en la campana y envía Web Push a los usuarios';

    public function handle(AppUpdateAnnouncement $announcement): int
    {
        $version = $announcement->latest();

        if ($version === null) {
            $this->warn('No hay un manifiesto downloads/version.json válido.');

            return self::SUCCESS;
        }

        $created = $announcement->announceAll((bool) $this->option('force'));
        $this->info("Avisos de la versión {$version['versionName']} creados: {$created}.");

        return self::SUCCESS;
    }
}
