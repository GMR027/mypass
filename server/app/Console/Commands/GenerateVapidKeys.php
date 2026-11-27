<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;

class GenerateVapidKeys extends Command
{
    protected $signature = 'mypass:vapid';

    protected $description = 'Genera un par de llaves VAPID para Web Push';

    public function handle(): int
    {
        $resource = openssl_pkey_new([
            'private_key_type' => OPENSSL_KEYTYPE_EC,
            'curve_name' => 'prime256v1',
        ]);

        if ($resource === false) {
            $this->error('No se pudo generar la llave EC.');

            return self::FAILURE;
        }

        $details = openssl_pkey_get_details($resource);

        if (! is_array($details) || empty($details['ec']['x']) || empty($details['ec']['y']) || empty($details['ec']['d'])) {
            $this->error('No se pudo leer la llave EC generada.');

            return self::FAILURE;
        }

        $public = "\x04".str_pad($details['ec']['x'], 32, "\0", STR_PAD_LEFT).str_pad($details['ec']['y'], 32, "\0", STR_PAD_LEFT);
        $private = str_pad($details['ec']['d'], 32, "\0", STR_PAD_LEFT);

        $this->line('VAPID_PUBLIC_KEY='.rtrim(strtr(base64_encode($public), '+/', '-_'), '='));
        $this->line('VAPID_PRIVATE_KEY='.rtrim(strtr(base64_encode($private), '+/', '-_'), '='));

        return self::SUCCESS;
    }
}
