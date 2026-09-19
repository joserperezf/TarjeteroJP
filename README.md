# Tarjetero JP

**Tarjetero JP** es una aplicación de tarjeta de presentación digital diseñada para funcionar de manera totalmente offline, permitiendo a los profesionales intercambiar su información de contacto de forma rápida y segura mediante tecnología Bluetooth.

## Descripción

La aplicación actúa como una tarjeta de presentación digital offline. Su objetivo es eliminar la necesidad de tarjetas de papel, proporcionando una solución ecológica y siempre disponible para el networking profesional sin depender de una conexión a internet.

## Características

- **Perfil:** Crea y gestiona tu información de contacto profesional (nombre, cargo, empresa, email, teléfono, etc.).
- **Tablero Bluetooth:** Interfaz dedicada para el intercambio de tarjetas mediante Bluetooth RFCOMM. Permite descubrir otros dispositivos y ser descubierto.
- **Bandeja:** Almacena y organiza todas las tarjetas de presentación recibidas de otros usuarios.

## Tecnología

- **Jetpack Compose:** Interfaz de usuario moderna, declarativa y reactiva.
- **Bluetooth RFCOMM:** Protocolo para la transferencia de datos punto a punto entre dispositivos Android.
- **API 29+:** Optimizado para versiones recientes de Android (Android 10 y superiores).
- **Coroutines:** Gestión eficiente de tareas en segundo plano, especialmente para la comunicación Bluetooth.

## Guía de Uso

1. **Configuración Inicial:** Al abrir la app por primera vez, completa tu perfil profesional.
2. **Intercambio:**
   - Dirígete a la sección de "Tablero Bluetooth".
   - Asegúrate de tener el Bluetooth activado.
   - Selecciona "Hacer visible" para que otros puedan encontrarte o "Buscar" para encontrar a un colega.
   - Selecciona el contacto y confirma el envío/recepción.
3. **Gestión:** Consulta las tarjetas guardadas en tu "Bandeja" en cualquier momento.

## Hoja de Ruta (Roadmap) para v2.0

- **Intercambio mediante NFC:** Implementación de "Tap-to-Share" para un intercambio aún más rápido.
- **Compatibilidad para usuarios sin la App:** Generación de vCard mediante códigos QR o etiquetas NFC para que personas que no tengan la aplicación instalada puedan guardar tu contacto directamente en su agenda telefónica.

---
*Desarrollado con pasión por el networking eficiente.*
