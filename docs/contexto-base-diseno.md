# Contexto base para aplicar el diseño visual

**Proyecto:** My Pass
**Fecha:** 22 de noviembre de 2026  
**Uso:** este archivo resume el estado real del proyecto y las reglas que se deben respetar cuando lleguen los diseños. Sirve como punto de revisión antes de empezar a aplicar estilos.

---

## 1. Qué es My Pass

Bóveda personal cifrada de extremo a extremo para documentos, boletos, tarjetas y pases.

- **Web:** Laravel + MySQL + Blade + Tailwind CSS 4.
- **Android:** Kotlin + Jetpack Compose, APK release instalado por sideload.
- **Cifrado:** ocurre en el cliente; el servidor solo guarda blobs cifrados.
- **Llave de recuperación:** solo en la PC, nunca en Android. Puede ser de 16 o 32 dígitos (32 por defecto).
- **Kit de emergencia:** archivo `.mypss`.
- **Sin dependencias de Google:** sin FCM, ML Kit, Google Sign-In ni Play Integrity.
- **Idiomas:** español e inglés.
- **Modo simple:** textos y controles más grandes para usuarios con poca visión.
- **Tema:** claro únicamente. No hay modo oscuro.
- **Despliegue:** pendiente, solo en el clúster x86_64 del usuario y con HTTPS.

---

## 2. Estado actual del proyecto

### Funcionalidad terminada

- Registro, login, sesiones web y tokens Sanctum en la API.
- Onboarding criptográfico: master key, frase de bóveda con Argon2id, llave de emergencia y kit `.mypss`.
- Bóveda web: desbloqueo con frase o archivo, alta de documentos, cifrado por bloques, descarga, borrado y vencimientos.
- Vinculación PC–Android con código de 6 dígitos.
- Familia: contactos con aceptación, envío de copia o transferencia, bandeja de recibidos, solicitudes de documentos, bloqueo y borrado remoto.
- Android: bóveda local cifrada, PIN, auto-bloqueo, bloqueo de capturas, visor PDF/imagen, sincronización, escáner con OCR local (CameraX + Tesseract), recordatorios de vencimiento y 2FA.
- 2FA web/API/Android: TOTP, códigos de recuperación y código alternativo por correo.
- Modo simple en web y Android.
- Traducción completa ES/EN en web, incluidos mensajes de validación y autenticación.
- Página de inicio pública.
- Backend con **94 pruebas y 442 aserciones pasando**.
- Billetera de pases, generador OCR, presentación QR, notificaciones y desbloqueo con huella en Android.
- Recuperación de cuenta, cambio de frase de bóveda, llave de emergencia y auto-bloqueo web.

### Estado visual

**El diseño visual ya está aplicado en web y Android.**

- Tokens de marca, tipografía Plus Jakarta Sans autoalojada e iconografía Material Symbols.
- Componentes reutilizables (`mp-*`, `Components.kt`) y navegación inferior de cuatro elementos: **Inicio · Bóveda · Pases · Perfil**.
- Bóveda web con pestañas **Documentos / Pases / Familia / Dispositivos**.
- Billetera de pases, editor, presentación a pantalla completa y generador OCR.
- Páginas de perfil, seguridad, privacidad y términos.

---

## 3. Briefs de diseño ya entregados

Estos documentos ya describen estructura, textos exactos, estados, accesibilidad y hooks:

| Pantalla | Ruta | Archivos actuales | Brief |
|---|---|---|---|
| Página de inicio | `/` | `welcome.blade.php` | Entregado |
| Acceso: login y registro | `/login`, `/register` | `auth/login.blade.php`, `auth/register.blade.php` | Entregado |
| Segundo factor | `/two-factor-challenge` | `auth/two-factor-challenge.blade.php`, `auth/email-two-factor.blade.php` | Entregado |
| Panel | `/dashboard` | `dashboard.blade.php` | Entregado |
| Onboarding y llave de emergencia | `/onboarding` | `onboarding.blade.php`, `onboarding.js` | Entregado |
| Bóveda web | `/vault` | `vault.blade.php`, `vault.js` | Entregado, guardado aparte en `pantalla-boveda-web.md` |
| Seguridad y preferencias | `/settings/security` | `settings/security.blade.php` | Aplicado |

### Briefs de Android pendientes

Todavía no se han entregado los briefs de diseño para Android. Pantallas previstas:

1. Acceso y 2FA.
2. Vinculación con la PC.
3. PIN, desbloqueo y bóveda bloqueada.
4. Bóveda y documentos.
5. Visor de PDF e imágenes.
6. Escáner y OCR.
7. Familia.
8. Solicitudes de documentos.
9. Ajustes y seguridad.
10. Diálogos y estados vacíos.

---

## 4. Flujo de trabajo acordado

1. Se diseña **una pantalla a la vez**.
2. Primero web; después Android.
3. El usuario diseña por fuera (Figma, HTML + Tailwind, HTML + CSS u otra herramienta).
4. El usuario devuelve un **esqueleto visual** de esa pantalla.
5. Yo adapto el esqueleto al proyecto real, sin romper criptografía, estados, traducciones, modo simple ni hooks de JavaScript.
6. Se revisa en escritorio, tablet y móvil.
7. Solo entonces se pasa a la siguiente pantalla.

Los briefs ya incluyen el copy exacto en español e inglés. El esqueleto visual no necesita volver a explicar la lógica; debe centrarse en cómo se ve y cómo se comporta.

---

## 5. Qué necesito de ti para aplicar estilos

Por cada pantalla que quieras estilizar:

- [ ] Enlace o archivo del diseño (Figma, HTML + Tailwind, HTML + CSS o capturas con medidas).
- [ ] Frames de escritorio 1440, tablet 768 y móvil 390.
- [ ] Estados: normal, foco, hover, presionado, deshabilitado, cargando, vacío y error.
- [ ] Versión en español y en inglés (o confirmación de que el copy del brief no cambia).
- [ ] Variante de modo simple.
- [ ] Tokens: colores con hex, tipografía, escala de texto, espaciados, radios, sombras y bordes.
- [ ] Logo, iconos e ilustraciones en SVG o PNG.
- [ ] Fuentes (archivos o nombre exacto); deben poder autoalojarse.
- [ ] Comportamiento responsive y qué cambia en cada breakpoint.
- [ ] Confirmación de si se implementan las pestañas de la bóveda.

Si el diseño llega solo como imagen, igual puedo adaptarlo, pero los valores exactos de color, tamaño y espaciado aceleran mucho el trabajo y evitan interpretaciones.

---

## 6. Cómo aplicaré los estilos

1. **Leer el esqueleto** y contrastarlo con el brief y el estado real de la pantalla.
2. **Extraer tokens** a `resources/css/app.css` usando `@theme` de Tailwind 4.
3. **Crear componentes Blade** reutilizables cuando un patrón se repita (botón, tarjeta, campo, aviso, modal, pestaña).
4. **Ajustar `components/layout.blade.php`** para cabecera, navegación, contenedor y fondo.
5. **Aplicar la pantalla** manteniendo los IDs y hooks existentes.
6. **Verificar ES/EN** cambiando el idioma real, no solo leyendo el código.
7. **Verificar modo simple** con una cuenta que lo tenga activo.
8. **Revisar accesibilidad:** foco visible, contraste, tamaño táctil, `role="alert"`, navegación por teclado.
9. **Compilar y probar:** `npm run build`, pruebas de Laravel y revisión visual en 1440, 768 y 390.
10. **Entregar** la pantalla adaptada y una nota corta de qué cambió y qué hooks se conservaron.

No se tocará la criptografía ni la lógica de negocio salvo que el diseño exija mover un control de sitio. En ese caso se reubica el hook, no se reescribe el flujo.

---

## 7. Mapa de archivos relevantes

### Web

```text
server/
├── resources/
│   ├── css/app.css                     Tokens y estilos globales (Tailwind 4)
│   ├── js/app.js                       Entrada de Vite
│   ├── js/vault.js                     Cripto, documentos, familia, dispositivos
│   ├── js/onboarding.js                Cripto y copy dinámico del onboarding
│   ├── views/
│   │   ├── components/layout.blade.php Cabecera, idioma, modo simple
│   │   ├── components/translations.blade.php
│   │   ├── welcome.blade.php           Página pública
│   │   ├── auth/login.blade.php
│   │   ├── auth/register.blade.php
│   │   ├── auth/two-factor-challenge.blade.php
│   │   ├── auth/email-two-factor.blade.php
│   │   ├── dashboard.blade.php
│   │   ├── onboarding.blade.php
│   │   ├── vault.blade.php
│   │   └── settings/security.blade.php
│   └── lang/
│       ├── es.json                     Diccionario español
│       └── en.json                     Diccionario inglés
├── routes/web.php
└── package.json                        Tailwind 4 + Vite 8
```

### Android

```text
android/app/src/main/java/app/mypass/
├── MainActivity.kt       Pantallas Compose y ajustes
├── MainViewModel.kt      Estado, sincronización, familia y 2FA
├── ScannerScreen.kt      Cámara, OCR y escaneo
├── VaultStore.kt         Bóveda local cifrada
├── Crypto.kt             Criptografía
├── ApiClient.kt          Cliente HTTP
├── Ocr.kt                OCR
├── Reminders.kt          Recordatorios
└── ReminderWorker.kt     WorkManager
```

---

## 8. Reglas técnicas que se deben conservar

### Tailwind y estilos

- Tailwind CSS **4**, configuración CSS-first con `@theme` en `resources/css/app.css`.
- No existe `tailwind.config.js`; no crear uno salvo necesidad justificada.
- Los estilos se compilan con Vite: `npm run build` o `npm run dev`.
- Las fuentes deben autoalojarse; no añadir dependencias de Google Fonts.
- El tema es claro. No agregar modo oscuro sin pedirlo.

### Traducciones

- Textos de Blade con `__('…')`.
- Textos dinámicos de JS con `t('…')` y `tf('…', { … })`.
- Los diccionarios viven en `server/lang/es.json` y `server/lang/en.json`.
- No hardcodear texto visible en JavaScript ni en HTML si ya existe una clave.
- El español es el idioma base; el inglés debe quedar completo.
- El idioma se guarda por usuario autenticado y por sesión para invitados.

### Modo simple

- Se activa con la clase `simple-mode` en `<body>`.
- Reglas actuales en `app.css`: texto base más grande, `line-height` amplio y controles de al menos 3.25 rem.
- El diseño debe funcionar en una sola columna, con botones amplios y texto legible.
- No ocultar funciones en modo simple; solo simplificar la presentación.

### Accesibilidad

- Controles táctiles de al menos 48 px.
- Foco visible en enlaces, botones, campos, pestañas y zonas de carga.
- Errores con `role="alert"`.
- El estado activo no debe depender solo del color.
- Diálogos con foco atrapado, cierre con Escape y botón de cancelar visible.
- Formularios con etiquetas reales, no solo placeholders.
- Contraste suficiente; el modo simple también debe cumplirlo.

### Hooks de la bóveda web

Estos IDs los usa `vault.js`; se pueden mover en el HTML, pero no renombrar sin actualizar el JS:

```text
#vault-loading        #vault-no-keys         #vault-locked
#vault-unlocked       #unlock-form           #vault-passphrase
#recovery-file        #recovery-drop-zone    #add-form
#item-title           #item-type             #item-file
#item-expires         #item-list             #expiry-banner
#pairing-create       #pairing-code          #device-list
#contact-form         #contact-list          #share-inbox
#request-form         #request-list          #share-modal
#share-contact        #share-mode           #share-expires
#share-send           #vault-lock
```

### Hooks del onboarding

`onboarding.js` controla el flujo de llaves, la frase de bóveda, la llave de emergencia y la descarga del kit `.mypss`. Antes de mover campos o botones de `onboarding.blade.php`, hay que revisar qué IDs consume ese archivo.

### Seguridad

- No incluir credenciales, llaves ni frases de bóveda en archivos versionados.
- No usar documentos reales en pruebas.
- El servidor no debe recibir texto plano ni llaves sin cifrar.
- El archivo `.env`, `.env.testing`, `local.properties`, builds y vendors están fuera de Git.

---

## 9. Entorno y verificación

### Servidor local

```bash
cd server
./serve-local.sh
```

Disponible en `http://127.0.0.1:38021`. Usar `serve-local.sh` para permitir subidas de hasta 128 MB.

### Compilar estilos

```bash
cd server
npm run build
```

### Pruebas

```bash
cd server
php artisan test --compact
```

### Android

```bash
cd android
JAVA_HOME="$HOME/.local/opt/jdk-21" ./gradlew assembleRelease
```

APK resultante:

```text
android/app/build/outputs/apk/release/app-release.apk
```

La tablet de pruebas se conecta por ADB inalámbrico y requiere `adb reverse tcp:38022 tcp:38021` para hablar con el servidor local.

---

## 10. Decisiones de diseño aún abiertas

Antes de aplicar la primera pantalla conviene confirmar:

- [ ] ¿Habrá logo definitivo o se usa el logotipo tipográfico `My Pass`?
- [ ] ¿Paleta de marca y color de acento?
- [ ] ¿Tipografía principal y secundaria?
- [ ] ¿Librería de iconos o set propio en SVG?
- [ ] ¿Se implementan las pestañas de la bóveda?
- [ ] ¿Se mantiene el tema claro únicamente?
- [ ] ¿El diseño web y el de Android comparten tokens o son sistemas separados?
- [ ] ¿Qué navegadores y versiones de Android son el objetivo mínimo?

---

## 11. Próximo paso

1. Revisar este documento.
2. Entregar el brief de **Seguridad y preferencias** si se quiere completar antes el mapa web.
3. Empezar a enviar los esqueletos visuales, una pantalla a la vez.
4. Adaptar la primera pantalla y validar el resultado antes de continuar.

Cuando llegue el primer esqueleto, el orden será: **tokens → layout → componentes → pantalla → ES/EN → modo simple → accesibilidad → pruebas → revisión visual**.
