<x-layout :title="__('Privacidad')">
    <div class="mx-auto max-w-3xl">
        <p class="mp-eyebrow">{{ __('Legal') }}</p>
        <h1 class="mp-title-lg mt-2">{{ __('Política de privacidad') }}</h1>
        <p class="mp-hint mt-2">{{ __('Última actualización: noviembre de 2026') }}</p>

        <div class="mp-card mp-card-pad mt-6 space-y-6">
            <section>
                <h2 class="mp-title-sm">{{ __('Qué es My Pass') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('My Pass es una bóveda personal para guardar documentos, boletos, tarjetas y pases. El contenido se cifra en tu dispositivo antes de salir de él. El servidor guarda blobs cifrados y metadatos también cifrados; no puede leer tus documentos ni tus llaves.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Datos que tratamos') }}</h2>
                <ul class="mp-body-sm mt-2 list-inside list-disc space-y-1">
                    <li>{{ __('Datos de cuenta: nombre, correo, contraseña cifrada con hash, idioma y preferencias.') }}</li>
                    <li>{{ __('Contenido cifrado: archivos, metadatos de documentos y pases, y blobs asociados.') }}</li>
                    <li>{{ __('Dispositivos: nombre, plataforma, última conexión y llaves públicas o envueltas.') }}</li>
                    <li>{{ __('Relaciones: contactos, envíos, solicitudes y notificaciones internas.') }}</li>
                    <li>{{ __('Registros técnicos: eventos de seguridad, dirección IP resumida y fecha de la actividad.') }}</li>
                </ul>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Para qué los usamos') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Para autenticarte, sincronizar tu bóveda entre tus dispositivos, permitir envíos cifrados a tus contactos, avisarte de vencimientos, prevenir abusos y mantener la seguridad del servicio. No vendemos tus datos ni los usamos para publicidad.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Base legal y conservación') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Tratamos los datos para ejecutar el servicio que solicitas y por interés legítimo en mantenerlo seguro. Conservamos los datos mientras tu cuenta exista. Si eliminas tu cuenta desde Perfil, borramos tus datos y archivos; pueden quedar copias de seguridad temporales hasta que se roten.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Con quién se comparten') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Solo con el proveedor de alojamiento y, si activas el código por correo, con el proveedor de correo. No compartimos contenido con anunciantes ni con terceros ajenos al servicio. Los envíos a contactos solo los puede abrir el destinatario con su llave.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Tus derechos') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Puedes acceder, corregir o eliminar tus datos desde Perfil. También puedes solicitar una copia de tu información y oponerte a determinados tratamientos. Si eliminas la cuenta, perderás el acceso a la bóveda y a sus llaves; no se puede recuperar si no conservas tu llave de emergencia.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Cookies y almacenamiento local') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Usamos cookies necesarias para la sesión y la protección CSRF, y almacenamiento local para el idioma, el modo simple y el tiempo de bloqueo automático. No usamos cookies de publicidad ni analítica de terceros.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Seguridad') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Las llaves se derivan con Argon2id y el contenido se cifra con XChaCha20-Poly1305 en el cliente. En producción el servicio debe servirse por HTTPS. Ningún sistema es infalible: guarda tu llave de emergencia y mantén actualizados tus dispositivos.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Menores y cambios') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('El servicio está pensado para personas adultas. Si cambiamos esta política, publicaremos la fecha de actualización y, cuando el cambio sea relevante, te avisaremos dentro de la aplicación.') }}</p>
            </section>
        </div>

        <p class="mt-6 text-center">
            <a href="{{ route('legal.terms') }}" class="font-semibold text-primary underline decoration-primary/40 underline-offset-4 hover:decoration-primary">{{ __('Leer los términos de uso') }}</a>
        </p>
    </div>
</x-layout>
