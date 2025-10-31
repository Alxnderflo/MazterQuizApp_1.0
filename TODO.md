## Resumen de Activities y sus XML correspondientes

 1. SplashScreen (Archivo Java: `SplashScreen.java`)
   - Propósito: Pantalla de inicio animada que muestra el logo de la app con transiciones suaves (fade, zoom, rotación) durante 4 segundos. Sirve para una introducción visual atractiva antes de redirigir al login. Es la actividad principal (launcher) de la app.
   - XML correspondiente: `activity_splash_screen.xml` – Define los elementos visuales como ImageViews para el logo completo y abreviado, y un ProgressBar para la animación.

 2. LoginActivity (Archivo Java: `LoginActivity.java`)
   - Propósito: Maneja el inicio de sesión de usuarios existentes. Permite login con email/contraseña o Google Sign-In. Verifica si el email está confirmado; si no, redirige a verificación. Si es usuario nuevo de Google, va a selección de rol. Es el punto de entrada después del splash.
   - XML correspondiente: `activity_login.xml` – Contiene EditTexts para email y contraseña, botones para login y Google, y un TextView para ir al registro.

 3. RegistroActivity (Archivo Java: `RegistroActivity.java`)
   - Propósito: Permite a nuevos usuarios registrarse con email, contraseña, nombre y rol (estudiante/profesor). Envía un email de verificación y guarda datos en Firestore. Redirige a la actividad de verificación de email tras el registro exitoso.
   - XML correspondiente: `activity_registro.xml` – Incluye EditTexts para nombre, email, contraseña y confirmación, un RadioGroup para seleccionar rol, y un botón de registro.

 4. VerificacionEmailActivity (Archivo Java: `VerificacionEmailActivity.java`)
   - Propósito: Pantalla para verificar el email después del registro. Muestra instrucciones, permite reenviar el email de verificación, y tiene un botón para ir al login. Bloquea el retroceso para forzar verificación.
   - XML correspondiente: `activity_verificacion_email.xml` – Tiene TextViews para el email y instrucciones, y botones para reenviar email e ir al login.

 5. RoleSelectionActivity (Archivo Java: `RoleSelectionActivity.java`)
   - Propósito: Para usuarios nuevos que se registran con Google. Permite seleccionar rol (estudiante o profesor) y guarda los datos en Firestore. Redirige a MainActivity tras la selección.
   - XML correspondiente: `activity_role_selection.xml` – Contiene botones para seleccionar estudiante o profesor.

 6. MainActivity (Archivo Java: `MainActivity.java`)
   - Propósito: Actividad principal con navegación por pestañas (usando ViewPager2 y BottomNavigationView). Verifica autenticación al inicio y en cada reanudación. Contiene fragmentos: Home (quizzes), Score (puntuaciones), y Teacher (para profesores). Es el hub central de la app.
   - XML correspondiente: `activity_main.xml` – Define el ViewPager2 y el BottomNavigationView para la navegación por fragmentos.

 7. CrearQuizActivity (Archivo Java: `CrearQuizActivity.java`)
   - Propósito: Permite crear o editar un quiz. Incluye campos para título, descripción e imagen (subida a Cloudinary). Soporta modo edición para actualizar quizzes existentes. Redirige a PreguntasActivity tras guardar.
   - XML correspondiente: `activity_crear_quizz.xml` – Tiene EditTexts para título y descripción, un CardView para seleccionar imagen, ImageView para preview, y un botón para guardar.

 8. PreguntasActivity (Archivo Java: `PreguntasActivity.java`)
   - Propósito: Muestra la lista de preguntas de un quiz específico. Permite agregar, editar o eliminar preguntas. Carga datos del quiz (título, descripción, imagen) en el header. Usa un RecyclerView para listar preguntas.
   - XML correspondiente: `activity_preguntas.xml` – Incluye un RecyclerView para preguntas, un header con ImageView y TextViews para info del quiz, botones para agregar preguntas, y un layout vacío si no hay preguntas.

 9. CrearPreguntasActivity (Archivo Java: `CrearPreguntasActivity.java`)
   - Propósito: Para crear o editar preguntas de un quiz. Incluye campos para enunciado y 4 opciones (1 correcta, 3 incorrectas). Asigna un orden automático y actualiza el contador de preguntas en Firestore.
   - XML correspondiente: `activity_crear_preguntas.xml` – Contiene EditTexts para el enunciado y las opciones, y un botón para guardar.
   - 
 Notas adicionales:
- Fragments: Aunque no son Activities, se mencionan brevemente ya que están integrados en MainActivity:
  - HomeFragment: Lista quizzes del usuario, permite crear, editar o eliminar quizzes. XML: `fragment_home.xml`.
  - ScoreFragment: Para completar después (incompleto). XML: `fragment_score.xml`.
  - TeacherFragment: Para funcionalidades de profesor (incompleto). XML: `fragment_teacher.xml`.
- Los XML están en `app/src/main/res/layout/` y definen la UI (botones, textos, imágenes, etc.).
- La app usa Firebase Auth para login/registro y Firestore para quizzes/preguntas/usuarios.
