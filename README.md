# mypass

Bóveda personal cifrada para boletos, documentos y tarjetas. Android + web, con backend Laravel y MySQL.

## Estado actual

- Backend Laravel 13 con MySQL.
- Modelos y migraciones base: usuarios, llaves, dispositivos, blobs, items, contactos, invitaciones, shares, solicitudes, notificaciones y auditoría.
- API v1 con registro, login, tokens Sanctum, dispositivos, llaves envueltas, items y blobs cifrados.
- Web de PC con registro/login y onboarding criptográfico:
  - master key generada en el navegador;
  - frase de bóveda con Argon2id;
  - llave de emergencia de 16 o 32 dígitos;
  - kit de recuperación `.mypss`.
- Bóveda web funcional:
  - desbloqueo con la frase de bóveda o con el archivo `.mypss`;
  - alta de documentos;
  - cifrado de archivos por bloques con XChaCha20-Poly1305;
  - descarga y descifrado local;
  - eliminación.
- Backend de vinculación PC–Android:
  - código temporal de 6 dígitos;
  - unión del celular con su llave pública RSA;
  - aprobación desde la web cifrando la master key;
  - consulta desde Android.
- Familia:
  - contactos con aceptación;
  - envío de copia o transferencia con paquete cifrado para la llave de identidad del destinatario;
  - aceptación o rechazo en web y Android;
  - solicitudes de documentos y vinculación automática con el envío;
  - descarga y borrado remoto.
- Android:
  - bóveda local cifrada, PIN con Argon2id, auto-bloqueo y bloqueo de capturas;
  - visor de PDF e imágenes;
  - sincronización bidireccional;
  - escáner con CameraX, OCR local con Tesseract (español e inglés) y PDF;
  - vencimientos con recordatorios locales por notificación;
  - segundo factor TOTP y código por correo;
  - modo simple y textos en español e inglés.
- Web:
  - segundo factor TOTP con códigos de recuperación;
  - código por correo alternativo;
  - modo simple;
  - aviso de documentos por vencer;
  - página de inicio pública;
  - interfaz completa en español e inglés, con selector de idioma.
- Pruebas de API y de navegador pasando (57 pruebas, 273 aserciones).
- App Android en Kotlin + Compose, instalada en la tablet de pruebas.
- Billetera de pases con pestañas Documentos, Pases, Familia y Dispositivos.
- Generador de pases con OCR local (Tesseract.js), PDF.js, ZXing y códigos QR/PDF417/Aztec/Code 128.
- Desbloqueo de Android con PIN y, cuando el dispositivo lo permite, huella o credencial del sistema.
- Recuperación de cuenta web, cambio de contraseña, cambio de frase de bóveda, regeneración de llave de emergencia y eliminación de cuenta.
- Auto-bloqueo de la bóveda web, cabeceras CSP y centro de notificaciones.
- Páginas legales de privacidad y términos.
- Tablet: Xiaomi Pad 6, Android 13 (API 33), sin biometría fuerte.

## Requisitos locales

- PHP 8.4 con `sodium`, `pdo_mysql`, `intl`, `mbstring`, `zip`.
- MySQL 8.
- Composer.
- JDK 21 en `~/.local/opt/jdk-21`.
- Gradle 9.9.2 en `~/.local/opt/gradle-9.9.2`.
- Android SDK en `~/Android/Sdk`.
- `adb` nativo en `~/.local/bin/adb`.

## Backend

```bash
cd server
cp .env.example .env
composer install
php artisan key:generate
php artisan migrate
./serve-local.sh
```

`serve-local.sh` usa el servidor integrado de PHP con un límite de subida de 128 MB.
Para desarrollo normal también sirve `php artisan serve`, pero PHP limita las subidas a 2 MB
si no se configura `upload_max_filesize` y `post_max_size`.

Las credenciales locales están en `server/.env` y no se versionan.

### Pruebas

```bash
cd server
php artisan test
```

Las pruebas usan `mypass_test` mediante `server/.env.testing`, también fuera de Git.

## Android

```bash
cd android
JAVA_HOME="$HOME/.local/opt/jdk-21" ./gradlew assembleRelease
```

APK release firmado con el keystore local:

```text
android/app/build/outputs/apk/release/app-release.apk
```

El keystore está en `android/keystore/` y la contraseña en `android/keystore.properties`; ambos fuera de Git. Hay que respaldarlos para poder actualizar la app instalada.

Instalación por ADB (cuando MIUI lo permita):

```bash
adb connect 192.168.1.73:<puerto-de-depuracion-inalambrica>
adb install -r android/app/build/outputs/apk/release/app-release.apk
```

La tablet tiene Android 13 y MIUI 14. MIUI bloquea la instalación de apps de desarrolladores no verificados durante las primeras 24 horas. Hay que esperar y activar:

`Ajustes → Opciones de desarrollador → Apps de desarrolladores no verificados`

También se puede usar otro dispositivo Android 13+ para probar antes.

## API v1

- `GET /api/v1/health`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/two-factor`
- `POST /api/v1/auth/two-factor/email`
- `POST /api/v1/auth/logout`
- `GET /api/v1/me`
- `GET|POST /api/v1/devices`
- `POST /api/v1/devices/{id}/revoke`
- `GET|PUT /api/v1/keys`
- `POST /api/v1/blobs`
- `GET /api/v1/blobs/{id}`
- `GET|POST /api/v1/items`
- `GET|PATCH|DELETE /api/v1/items/{id}`
- `GET /api/v1/notifications`
- `POST /api/v1/notifications/{id}/read`
- `POST /api/v1/notifications/read-all`
- `PATCH /api/v1/devices/{id}`
- `GET|POST /api/v1/contacts`
- `GET|POST /api/v1/shares`
- `GET|POST /api/v1/share-requests`

Todos los endpoints protegidos usan `Authorization: Bearer <token>`.

## Seguridad

- El servidor no descifra documentos ni llaves; los blobs se suben ya cifrados por el cliente.
- `.env`, `.env.testing`, `local.properties`, builds y vendors están fuera de Git.
- No usar documentos reales en pruebas.

## Siguiente trabajo

1. Desplegar en el clúster x86_64 con HTTPS, SMTP, respaldo local y monitorización.
2. Probar el escaneo con cámara y un boleto físico real en un Android con huella.
3. Añadir pruebas automatizadas en Android para OCR, biometría, notificaciones y sincronización.
4. Completar la paridad fina: visor de PDF en Android con selector de página, exportación/impresión de pases y búsqueda en la app.
5. Revisar el aviso legal con asesoría antes de abrir el servicio al público.
