#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/public"

# El servidor integrado de PHP usa 2 MB por defecto. Este arranque sube el
# límite para que la bóveda pueda subir documentos y escaneos grandes.
exec php \
    -d upload_max_filesize=128M \
    -d post_max_size=140M \
    -d memory_limit=512M \
    -S "${HOST:-127.0.0.1}:${PORT:-8000}" \
    -t . ../vendor/laravel/framework/src/Illuminate/Foundation/resources/server.php
