#!/bin/sh
# Prepara los directorios de storage antes de arrancar Apache. El volumen
# persistente llega vacío la primera vez; el initContainer de Kubernetes
# también crea y ajusta permisos, pero esto mantiene la imagen utilizable
# fuera de Kubernetes.
set -e

mkdir -p \
    /var/www/html/storage/logs \
    /var/www/html/storage/framework/cache/data \
    /var/www/html/storage/framework/sessions \
    /var/www/html/storage/framework/views \
    /var/www/html/storage/app/private/vault \
    /var/www/html/storage/app/public

if [ "$(id -u)" = "0" ]; then
    chown -R www-data:www-data \
        /var/www/html/storage \
        /var/www/html/bootstrap/cache
    chmod -R ug+rwX \
        /var/www/html/storage \
        /var/www/html/bootstrap/cache
fi

exec "$@"
