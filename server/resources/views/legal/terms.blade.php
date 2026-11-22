<x-layout :title="__('Términos de uso')">
    <div class="mx-auto max-w-3xl">
        <p class="mp-eyebrow">{{ __('Legal') }}</p>
        <h1 class="mp-title-lg mt-2">{{ __('Términos de uso') }}</h1>
        <p class="mp-hint mt-2">{{ __('Última actualización: noviembre de 2026') }}</p>

        <div class="mp-card mp-card-pad mt-6 space-y-6">
            <section>
                <h2 class="mp-title-sm">{{ __('El servicio') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('mypass te permite organizar documentos y pases en una bóveda cifrada, sincronizarla con tus dispositivos y compartir copias cifradas con contactos que acepten el envío. El servicio se ofrece tal como está y puede cambiar o interrumpirse.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Tu cuenta y tus llaves') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Eres responsable de tu contraseña, tu frase de bóveda, tu PIN y tu llave de emergencia. Si pierdes la llave de emergencia y olvidas la frase, no podemos recuperar el contenido cifrado. No compartas tus llaves con nadie.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Uso aceptable') }}</h2>
                <ul class="mp-body-sm mt-2 list-inside list-disc space-y-1">
                    <li>{{ __('No uses el servicio para actividades ilegales, fraude o para vulnerar derechos de terceros.') }}</li>
                    <li>{{ __('No intentes acceder a bóvedas ajenas ni eludir los controles de seguridad.') }}</li>
                    <li>{{ __('No subas malware ni contenido que no tengas derecho a guardar o compartir.') }}</li>
                    <li>{{ __('No uses los pases generados como documentos oficiales si la entidad emisora no los acepta.') }}</li>
                </ul>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Contenido y responsabilidad') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Conservas la titularidad de tu contenido. No podemos revisarlo porque está cifrado. Debes mantener tus propios respaldos y verificar que los datos de tus pases sean correctos antes de usarlos. No garantizamos que un lector externo acepte un código generado por mypass.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Disponibilidad y límites') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Intentamos mantener el servicio disponible, pero no garantizamos que sea ininterrumpido ni libre de errores. Podemos suspender cuentas que abusen del servicio o incumplan estos términos. En la medida permitida por la ley, no respondemos por daños indirectos derivados del uso o de la pérdida de llaves.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Terminación') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Puedes eliminar tu cuenta desde Perfil en cualquier momento. Si incumples estos términos, podemos suspender o cerrar el acceso. Al terminar, se borran tus datos según la política de privacidad, salvo las copias de seguridad temporales.') }}</p>
            </section>

            <section>
                <h2 class="mp-title-sm">{{ __('Cambios y ley aplicable') }}</h2>
                <p class="mp-body-sm mt-2">{{ __('Podemos actualizar estos términos publicando una versión nueva. El uso continuado después del cambio implica aceptación. Estos términos se interpretan conforme a la legislación aplicable en la jurisdicción de la instalación que utilizas.') }}</p>
            </section>
        </div>

        <p class="mt-6 text-center">
            <a href="{{ route('legal.privacy') }}" class="font-semibold text-primary underline decoration-primary/40 underline-offset-4 hover:decoration-primary">{{ __('Leer la política de privacidad') }}</a>
        </p>
    </div>
</x-layout>
