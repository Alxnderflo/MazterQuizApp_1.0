# TODO: Implementar Permisos de Almacenamiento para Subida de Imágenes a Cloudinary

## Tareas Pendientes

- [x] Agregar permisos de almacenamiento al AndroidManifest.xml
- [x] Implementar solicitud de permisos en runtime en CrearQuizActivity.java
- [ ] Verificar funcionamiento probando la subida de imágenes

## Detalles

- **Permisos a agregar**:
  - `READ_EXTERNAL_STORAGE` con `maxSdkVersion="32"` para versiones anteriores a Android 13
  - `READ_MEDIA_IMAGES` para Android 13+ (API 33+)

- **Lógica en CrearQuizActivity.java**:
  - Verificar si los permisos están concedidos antes de abrir el selector de imágenes
  - Solicitar permisos si no están concedidos
  - Manejar la respuesta de permisos en `onRequestPermissionsResult`

- **Pruebas**:
  - Ejecutar la app en un dispositivo/emulador con Android 12+ y 13+
  - Intentar subir una imagen y verificar logs de éxito/error

## Archivos que hacen consultas a Firebase

Los siguientes archivos contienen código que realiza consultas o interacciones con Firebase (Firestore y Auth). Estos son los que debes compartir con la IA para debugging:

- `app/src/main/java/sv/edu/itca/masterquizapp/LoginActivity.java`
- `app/src/main/java/sv/edu/itca/masterquizapp/PreguntasActivity.java`
- `app/src/main/java/sv/edu/itca/masterquizapp/RegistroActivity.java`
- `app/src/main/java/sv/edu/itca/masterquizapp/RoleSelectionActivity.java`
- `app/src/main/java/sv/edu/itca/masterquizapp/MyApplication.java`
- `app/src/main/java/sv/edu/itca/masterquizapp/VerificacionEmailActivity.java`
- `app/src/main/java/sv/edu/itca/masterquizapp/HomeFragment.java`
- `app/src/main/java/sv/edu/itca/masterquizapp/MainActivity.java`
- `app/src/main/java/sv/edu/itca/masterquizapp/CrearQuizActivity.java`
- `app/src/main/java/sv/edu/itca/masterquizapp/CrearPreguntasActivity.java`

## Archivos que se pueden optimizar para reducir lecturas de Firebase

Basándome en la revisión del código, los siguientes archivos tienen potencial para optimizaciones que reduzcan las lecturas de Firebase y mejoren el rendimiento:

1. **HomeFragment.java**:
   - **Problema**: Consulta a Firestore en `obtenerQuizzes()` se ejecuta en `onCreateView` y `onResume`, generando lecturas innecesarias en navegación frecuente.
   - **Optimización posible**: Implementar caching local o usar un patrón de observer más eficiente. Optimizar consulta para obtener solo campos necesarios.

2. **CrearQuizActivity.java**:
   - **Problema**: Consulta adicional a colección `usuarios` en `guardarQuiz()` para obtener nombre y rol, sumando una lectura extra por quiz creado.
   - **Optimización posible**: Almacenar información del usuario en SharedPreferences o memoria durante la sesión.

3. **PreguntasActivity.java**:
   - **Problema**: Múltiples consultas snapshot listener para datos del quiz y preguntas, generando lecturas repetitivas en navegación frecuente.
   - **Optimización posible**: Pasar datos del quiz como parámetro desde HomeFragment en lugar de consulta adicional.

4. **CrearPreguntasActivity.java**:
   - **Problema**: Consulta para contar preguntas existentes antes de agregar nueva, necesaria para el orden pero genera lectura extra.
   - **Optimización posible**: Usar contador local o transacción para evitar consulta de conteo.

Los archivos con mayor potencial de optimización son **HomeFragment.java**, **CrearQuizActivity.java**, **PreguntasActivity.java** y **CrearPreguntasActivity.java**.
