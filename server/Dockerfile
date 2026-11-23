# ===========================================================================
# Dockerfile — My Pass (Laravel 13 + PHP 8.4 + Apache + MySQL)
#
# Etapa 1: compila CSS/JS con Vite.
# Etapa 2: instala dependencias de Composer sin paquetes de desarrollo.
# Etapa 3: runtime Apache/PHP con pdo_mysql, mbstring, intl, zip y opcache.
#
# La configuración real llega por variables de entorno desde Kubernetes.
# No se copia ningún archivo .env ni datos locales de storage/.
# ===========================================================================

FROM node:24-bookworm-slim AS assets

WORKDIR /app

COPY package.json package-lock.json .npmrc ./
RUN npm ci

COPY vite.config.js ./
COPY resources ./resources

RUN npm run build

FROM composer:2 AS vendor

WORKDIR /app

COPY composer.json composer.lock ./
COPY app ./app

RUN composer install \
      --no-dev \
      --no-interaction \
      --no-scripts \
      --prefer-dist \
      --optimize-autoloader \
      --ignore-platform-req=ext-pdo_mysql

FROM php:8.4-apache

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      default-mysql-client \
      libicu-dev \
      libonig-dev \
      libzip-dev \
 && docker-php-ext-install -j"$(nproc)" pdo_mysql mbstring intl zip opcache \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# DocumentRoot en /public y fallback a index.php para rutas limpias.
RUN a2enmod rewrite headers expires \
 && sed -ri 's!/var/www/html!/var/www/html/public!g' /etc/apache2/sites-available/*.conf \
 && sed -ri 's/^Timeout .*/Timeout 300/' /etc/apache2/apache2.conf \
 && printf '%s\n' \
      '<Directory /var/www/html/public>' \
      '    AllowOverride All' \
      '    Require all granted' \
      '    FallbackResource /index.php' \
      '</Directory>' \
      'AddType application/wasm .wasm' \
      > /etc/apache2/conf-available/mypass.conf \
 && a2enconf mypass

RUN { \
      echo 'date.timezone = America/Mexico_City'; \
      echo 'upload_max_filesize = 128M'; \
      echo 'post_max_size = 128M'; \
      echo 'memory_limit = 512M'; \
      echo 'max_execution_time = 300'; \
      echo 'max_input_time = 300'; \
      echo 'session.save_path = /var/www/html/storage/framework/sessions'; \
      echo 'session.gc_maxlifetime = 604800'; \
      echo 'opcache.enable = 1'; \
      echo 'opcache.validate_timestamps = 1'; \
      echo 'opcache.memory_consumption = 256'; \
      echo 'opcache.max_accelerated_files = 20000'; \
      echo 'expose_php = Off'; \
      echo 'display_errors = Off'; \
      echo 'log_errors = On'; \
    } > /usr/local/etc/php/conf.d/mypass.ini

WORKDIR /var/www/html

COPY --chown=www-data:www-data . /var/www/html
COPY --from=vendor --chown=www-data:www-data /app/vendor /var/www/html/vendor
COPY --from=assets --chown=www-data:www-data /app/public/build /var/www/html/public/build

COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh

RUN chmod +x /usr/local/bin/entrypoint.sh \
 && mkdir -p \
      storage/logs \
      storage/framework/cache/data \
      storage/framework/sessions \
      storage/framework/views \
      storage/app/private/vault \
      storage/app/public \
 && chown -R www-data:www-data storage bootstrap/cache \
 && chmod -R ug+rwX storage bootstrap/cache

EXPOSE 80

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["apache2-foreground"]
