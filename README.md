# My Pass

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
- Pruebas de API y de navegador pasando (105 pruebas, 524 aserciones).
- App Android en Kotlin + Compose, instalada en la tablet de pruebas.
- Billetera de pases con pestañas Documentos, Pases, Familia y Dispositivos.
- Generador de pases con OCR local (Tesseract.js), PDF.js, ZXing y códigos QR/PDF417/Aztec/Code 128.
- Desbloqueo de Android con PIN y, cuando el dispositivo lo permite, huella o credencial del sistema.
- Registro con fecha de nacimiento, cálculo de edad y modo fácil automático a partir de los 51 años, con opción de desactivarlo.
- Recuperación de bóveda por correo opcional, equipo de confianza para abrir la bóveda con un clic y centro de recuperación con llave de emergencia.
- Recuperación de cuenta web, cambio de contraseña, cambio de frase de bóveda, regeneración de llave de emergencia y eliminación de cuenta.
- Auto-bloqueo de la bóveda web, cabeceras CSP y centro de notificaciones.
- Páginas legales de privacidad y términos.
- Pases de vuelo y transporte con terminal, puerta e inicio/cierre de abordaje, además de OCR para esos campos.
- Credenciales con logo, banner o fondo, tipos de cuenta personalizados y grupos definidos por el usuario.
- Android: fechas de pases con selector del sistema, recuperación de la bóveda por correo desde el celular y notificaciones con el ícono de la app.
- Pases de evento y transporte con ubicación y enlaces a Google Maps u otras apps, avisos de check-in y calendario.
- Importación de TOTP escaneando el código QR, papelera de 30 días con Deshacer, búsqueda global y recientes.
- Recordatorios locales de medicamentos y citas médicas, con alarmas exactas cuando el sistema lo permite.
- Compartir archivos desde el menú de Android directamente a My Pass y atajos de la app para buscar, escanear o crear un pase.
- Copia de seguridad cifrada completa, compatible entre web y Android.
- Web instalable como PWA con share target para recibir archivos compartidos.
- Campana con avisos de envíos, solicitudes de contacto y actualizaciones de la app, además de Web Push para recibirlos con la app cerrada.
- Bandeja de Enviados en Familia con estado de cada pase o documento, y distintivo “Compartido” en las tarjetas.
- Ventana de confirmación al recibir un pase, contacto o solicitud de documento, con opciones para aceptar, rechazar o bloquear.
- Android consulta avisos en segundo plano cada 15 minutos y muestra notificaciones del sistema aunque la app esté cerrada; la web usa Web Push inmediato.
- Estilos móviles corregidos para pantallas de 360–412 dp, modo fácil y tarjetas de contacto.
- Pases muestra primero la billetera; crear, escanear e importar viven en el botón + y el filtro Compartidos agrupa los pases enviados.
- Familia usa pestañas Contactos, Recibidos, Enviados y Solicitudes con búsqueda y contadores, en lugar de una lista vertical interminable.
- Gestión de envíos por elemento para consultar destinatarios, estado y cancelar envíos pendientes desde el propio pase, documento, credencial o carnet.
- Sección Seguros con pólizas de salud, gastos médicos mayores, auto, moto, hogar, vida, viaje, mascotas y negocio, con modo siniestro, llamadas con un toque, póliza adjunta e historial de siniestros.
- El carnet médico puede vincularse a una póliza de salud o gastos médicos mayores para mostrar sus datos actualizados.
- Recordatorios de seguros para vigencia y próximo pago, configurables a 1, 7, 15 o 30 días antes, con notificación local en Android, aviso dentro de la sección Seguros y Web Push a 7 días de la vigencia cuando el usuario tiene avisos activos.
- Tablet: Xiaomi Pad 6, Android 13 (API 33), sin biometría fuerte.

## Requisitos locales

- PHP 8.4 con `sodium`, `pdo_mysql`, `intl`, `mbstring`, `zip`.
- MySQL 8.
- Composer.
- JDK 21 en `~/.local/opt/jdk-21`.
- Gradle 9.9.2 en `~/.local/opt/gradle-9.9.2`.
- Android SDK en `~/Android/Sdk`.
- `adb` nativo en `~/.local/bin/adb`.

## Clonar en otro equipo

```bash
git clone https://github.com/GMR027/mypass.git
cd mypass
```

No se versionan `.env`, `.env.testing`, `local.properties`, el keystore, los builds ni los APK. Hay que crearlos en el equipo nuevo siguiendo las secciones siguientes.

Para el clúster, el backend debe publicarse con HTTPS y un dominio estable. Configura en `server/.env` al menos `APP_URL`, `DB_*`, `MAIL_*` y `SESSION_DOMAIN`, y mantén `upload_max_filesize` y `post_max_size` en 128 MB o más.

Para los avisos Web Push de la PWA, genera las llaves VAPID con `php artisan mypass:vapid` y configura `WEBPUSH_ENABLED=true`, `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` y `VAPID_SUBJECT`. En Kubernetes, `deploy.sh` las guarda en `values.local.yaml` y las inyecta como secretos.

El chart incluye un CronJob diario (`mypass:insurance-reminders`) que crea el aviso en la campana y envía Web Push cuando una póliza de seguro vence en los próximos 7 días.

En Android, los avisos se consultan cada 15 minutos con WorkManager y se muestran como notificación del sistema. En la web, el usuario debe activar los avisos desde la campana; en iPhone/iPad, Web Push requiere instalar la PWA en la pantalla de inicio (iOS 16.4 o posterior).

## Backend

```bash
cd server
cp .env.example .env
composer install
php artisan key:generate
# Crea la base `mypass` y su usuario en MySQL, o ajusta DB_* en .env.
php artisan migrate
npm install
npm run build
PORT=38021 ./serve-local.sh
```

`serve-local.sh` usa el servidor integrado de PHP con un límite de subida de 128 MB.
Para desarrollo normal también sirve `php artisan serve`, pero PHP limita las subidas a 2 MB
si no se configura `upload_max_filesize` y `post_max_size`.

En el clúster no uses `serve-local.sh`: publica `server/public` detrás de Nginx o Apache con HTTPS y ejecuta `php artisan optimize` tras el despliegue.

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

En un equipo nuevo, si no tienes el keystore original, puedes generar uno propio:

```bash
mkdir -p android/keystore
keytool -genkeypair -v \
  -keystore android/keystore/mypass-release.jks \
  -alias mypass -keyalg RSA -keysize 4096 -validity 10000
cp android/keystore.properties.example android/keystore.properties
# Edita las contraseñas en android/keystore.properties
```

Si no existe `android/keystore.properties`, el build release se firma con la clave debug para poder instalarlo en pruebas. Para producción usa siempre el keystore original: una app instalada solo se puede actualizar con la misma firma.

Instalación por ADB (cuando MIUI lo permita):

```bash
adb connect 192.168.1.73:<puerto-de-depuracion-inalambrica>
adb reverse tcp:38022 tcp:38021
adb install -r android/app/build/outputs/apk/release/app-release.apk
```

La app compila por defecto contra `http://127.0.0.1:38022/api/v1`, por eso el `adb reverse` debe estar activo mientras se prueba contra el servidor local. El puerto 38021 queda para la web y para la depuración inalámbrica de la tablet.

Para apuntar cualquier modelo Android 13+ al servidor del clúster, compila con la URL pública:

```bash
cd android
JAVA_HOME="$HOME/.local/opt/jdk-21" ./gradlew assembleRelease \
  -Pmypass.apiBaseUrl=https://mypass.example.com/api/v1
```

Ese valor queda dentro de `BuildConfig.API_BASE_URL` y no necesita `adb reverse`. Usa HTTPS con un certificado válido: la configuración de red del APK solo permite tráfico sin cifrar hacia `localhost` y `127.0.0.1`.

`deploy.sh` publica ese mismo APK en `/downloads/mypass.apk`, así que antes de desplegar verifica la URL grabada dentro del APK y aborta si apunta a `127.0.0.1:38022`: un APK con la URL local no puede conectarse al clúster desde el teléfono.

### Versión y actualizaciones

La versión de la app vive en `android/version.properties`:

```properties
versionCode=15
versionName=0.7.1
minSupportedVersionCode=0
```

Hay que incrementar `versionCode` en cada release; `deploy.sh` rechaza publicar una versión igual o menor que la que ya está en el clúster. `versionName` es el texto visible y `minSupportedVersionCode` permite marcar una actualización como obligatoria (0 la desactiva).

Al desplegar se publican el APK y un manifiesto `version.json` con SHA-256, tamaño, notas y URL versionada. La app consulta:

```text
GET https://mypass.iguzman.com.mx/api/v1/app/version
```

El chequeo automático se hace al abrir o volver a la app, como máximo una vez cada 24 horas. También se puede forzar desde `Ajustes → Actualizaciones`. Cuando hay una versión nueva:

- Aparece la campana de notificaciones en la barra superior.
- La tarjeta de notificaciones de Inicio muestra la novedad.
- Ajustes permite descargar e instalar el APK dentro de la app.

Android siempre muestra su propia confirmación de instalación; al ser una app fuera de Play Store no se puede instalar en silencio. Si es la primera vez, hay que permitir `Instalar apps desconocidas` para My Pass. La actualización conserva los datos porque usa la misma firma y no se desinstala.

Las notas de cada release se editan en `android/release-notes.json` (español e inglés). Flujo habitual:

```bash
# 1. Edita android/version.properties y android/release-notes.json
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew assembleRelease \
  -Pmypass.apiBaseUrl=https://mypass.iguzman.com.mx/api/v1
cd ..
./deploy.sh
```

La tablet tiene Android 13 y MIUI 14. MIUI bloquea la instalación de apps de desarrolladores no verificados durante las primeras 24 horas. Hay que esperar y activar:

`Ajustes → Opciones de desarrollador → Apps de desarrolladores no verificados`

También se puede usar otro dispositivo Android 13+ para probar antes.

## Prototipos de la landing

En `docs/landing-preview/` están los HTML exportados de Stitch para escritorio y móvil, además del APK de descarga local (ignorado por Git). Para verlos:

```bash
python3 -m http.server 38100 --directory docs/landing-preview
```

- `index.html`: landing de escritorio.
- `login.html`: acceso de escritorio con descarga de APK.
- `mobile.html`: landing móvil.
- `login-mobile.html`: acceso móvil con descarga de APK.

## API v1

- `GET /api/v1/health`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/two-factor`
- `POST /api/v1/auth/two-factor/email`
- `POST /api/v1/auth/logout`
- `GET /api/v1/me`
- `PATCH /api/v1/me/profile`
- `PATCH /api/v1/me/preferences`
- `GET|POST /api/v1/devices`
- `POST /api/v1/devices/{id}/revoke`
- `GET|PUT /api/v1/keys`
- `GET /api/v1/vault-recovery/status`
- `POST /api/v1/vault-recovery/request`
- `POST /api/v1/vault-recovery/verify`
- `POST /api/v1/vault-recovery/enable`
- `DELETE /api/v1/vault-recovery`
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

- Por defecto, el servidor no descifra documentos ni llaves; los blobs se suben ya cifrados por el cliente.
- La recuperación por correo es opcional. Si la persona la activa, el servidor guarda una copia recuperable de la llave maestra y la entrega solo después de verificar un código enviado al correo. El resto de cuentas mantiene el modelo de conocimiento cero.
- La fecha de nacimiento se usa para confirmar que la cuenta es de una persona de 18 años o más y para activar el modo fácil a partir de los 51 años.
- `.env`, `.env.testing`, `local.properties`, builds y vendors están fuera de Git.
- No usar documentos reales en pruebas.

## Siguiente trabajo

1. Desplegar en el clúster x86_64 con HTTPS, SMTP, respaldo local y monitorización.
2. Probar el escaneo con cámara y un boleto físico real en un Android con huella.
3. Añadir pruebas automatizadas en Android para OCR, biometría, notificaciones y sincronización.
4. Completar la paridad fina: visor de PDF en Android con selector de página, exportación/impresión de pases y búsqueda en la app.
5. Revisar el aviso legal con asesoría antes de abrir el servicio al público.
