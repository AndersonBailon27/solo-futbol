# Solo Fútbol

App Android con canales de TV gratuitos de fútbol y deportes (FIFA+, Real Madrid TV, TOP Barça y otros canales oficiales de acceso libre).

## Instalar

Descarga **SoloFutbol.apk** desde [la última versión](../../releases/latest) en tu celular Android e instálala.

## Cómo funciona

- La guía de canales es una página (`app/src/main/assets/index.html`) dentro de la app.
- El video se reproduce con un reproductor nativo (Media3 / ExoPlayer).
- La app comprueba qué canales están al aire y actualiza sus señales desde la lista pública de [iptv-org](https://github.com/iptv-org/iptv).
- Cada cambio en `main` compila el APK con GitHub Actions y lo publica en Releases.

Solo incluye canales que sus dueños emiten gratis. No trae canales de pago.
