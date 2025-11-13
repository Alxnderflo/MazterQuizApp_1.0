package sv.edu.itca.masterquizapp;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import sv.edu.itca.masterquizapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private ViewPager2 viewPager;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userRol;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        Log.d("SESSION_MANAGER", "MainActivity iniciada");

        // ✅ NUEVO: Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Verificar autenticación
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !currentUser.isEmailVerified()) {
            Log.d("SESSION_MANAGER", "Usuario no autenticado - redirigiendo a Login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Log.d("SESSION_MANAGER", "Usuario autenticado: " + currentUser.getUid());

        // ✅ NUEVO FLUJO: Primero intentar cargar desde SessionManager
        verificarYCargarDatos(currentUser.getUid());
    }

    // ✅ NUEVO: Crear menú de opciones
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // ✅ NUEVO: Mostrar/ocultar opciones según el rol
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem verCodigoItem = menu.findItem(R.id.menu_ver_codigo);

        if (userRol != null) {
            if (userRol.equals("estudiante")) {
                verCodigoItem.setVisible(false);
                Log.d("SESSION_MANAGER", "Ocultando 'Ver Código' para estudiante");
            } else if (userRol.equals("profesor")) {
                verCodigoItem.setVisible(true);
                Log.d("SESSION_MANAGER", "Mostrando 'Ver Código' para profesor");
            }
        } else {
            Log.d("SESSION_MANAGER", "userRol es null - ocultando 'Ver Código' por defecto");
            verCodigoItem.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    // ✅ NUEVO: Manejar clics del menú
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_ver_codigo) {
            mostrarDialogoCodigoDesdeSession();
            return true;
        } else if (id == R.id.menu_cerrar_sesion) {
            cerrarSesion();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ✅ NUEVO: Mostrar código desde SessionManager
    private void mostrarDialogoCodigoDesdeSession() {
        String codigo = sessionManager.getProfessorCode();

        if (codigo != null && !codigo.isEmpty()) {
            mostrarDialogoCodigoAsignado(codigo);
        } else {
            Toast.makeText(this, R.string.error_codigo_no_encontrado, Toast.LENGTH_SHORT).show();
            Log.d("SESSION_MANAGER", "No hay código en SessionManager");
        }
    }

    // ✅ NUEVO: Cerrar sesión con limpieza completa
    private void cerrarSesion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_cerrar_sesion_titulo);
        builder.setMessage(R.string.dialog_cerrar_sesion_mensaje);
        builder.setPositiveButton(R.string.dialog_btn_si, (dialog, which) -> {
            // 1. Limpiar SessionManager
            sessionManager.clearSession();

            // 2. Cerrar sesión en Firebase
            auth.signOut();

            // 3. Redirigir al Login
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

            Toast.makeText(MainActivity.this, R.string.toast_sesion_cerrada, Toast.LENGTH_SHORT).show();
            Log.d("SESSION_MANAGER", "Sesión cerrada - SessionManager limpiado");
        });
        builder.setNegativeButton(R.string.dialog_btn_cancelar, (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }

    private void verificarYCargarDatos(String userId) {
        // PRIMERO: Intentar cargar desde SessionManager
        String rolGuardado = sessionManager.getUserRole();

        if (rolGuardado != null) {
            // ✅ DATOS LOCALES ENCONTRADOS - Cargar inmediatamente
            Log.d("SESSION_MANAGER", "Rol cargado desde SessionManager: " + rolGuardado);
            userRol = rolGuardado;

            if ("profesor".equals(userRol)) {
                configurarMenuProfesor();
                String codigo = sessionManager.getProfessorCode();

                // ✅ MEJORADO: Solo generar código si NO existe en SessionManager
                if (codigo == null) {
                    Log.d("SESSION_MANAGER", "Profesor sin código - verificando Firestore");
                    verificarYGenerarCodigoProfesor(userId);
                } else {
                    Log.d("SESSION_MANAGER", "Profesor YA tiene código en SessionManager: " + codigo);
                }
            } else {
                configurarMenuEstudiante();
            }

            // ✅ ACTUALIZAR MENÚ después de cargar el rol
            invalidateOptionsMenu();
        } else {
            // ❌ NO hay datos locales - Consultar Firestore (solo esta vez)
            Log.d("SESSION_MANAGER", "No hay datos en SessionManager - consultando Firestore");
            obtenerRolYConfigurarNavegacion(userId);
        }
    }

    private void obtenerRolYConfigurarNavegacion(String userId) {
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userRol = documentSnapshot.getString("rol");
                        String codigoProfesor = documentSnapshot.getString("codigoProfesor");
                        Log.d("SESSION_MANAGER", "Rol obtenido de Firestore: " + userRol);

                        // ✅ GUARDAR SOLO ROL INICIALMENTE
                        sessionManager.saveUserRole(userId, userRol);

                        if ("profesor".equals(userRol)) {
                            configurarMenuProfesor();
                            // Si ya tiene código en Firestore, guardarlo en SessionManager
                            if (codigoProfesor != null && !codigoProfesor.isEmpty()) {
                                sessionManager.saveProfessorCode(codigoProfesor);
                                Log.d("SESSION_MANAGER", "Código existente guardado en SessionManager: " + codigoProfesor);
                            } else {
                                verificarYGenerarCodigoProfesor(userId);
                            }
                        } else {
                            configurarMenuEstudiante();
                        }

                        // ✅ ACTUALIZAR MENÚ después de cargar el rol
                        invalidateOptionsMenu();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SESSION_MANAGER", "Error al obtener rol: " + e.getMessage());
                    // Por defecto, configurar como estudiante
                    configurarMenuEstudiante();
                });
    }

    private void configurarMenuProfesor() {
        Log.d("SESSION_MANAGER", "Configurando menú para PROFESOR");

        viewPager = findViewById(R.id.viewPager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);

        adapter.addFragment(new HomeFragment());        // Sus quizzes
        adapter.addFragment(new ScoreFragment());       // Sus estadísticas
        adapter.addFragment(new StudentFragment());     // Lista de estudiantes

        viewPager.setAdapter(adapter);
        configurarBottomNavigationProfesor();
    }

    private void configurarMenuEstudiante() {
        Log.d("SESSION_MANAGER", "Configurando menú para ESTUDIANTE");

        viewPager = findViewById(R.id.viewPager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);

        adapter.addFragment(new HomeFragment());        // Quizzes disponibles
        adapter.addFragment(new ScoreFragment());       // Sus resultados
        adapter.addFragment(new TeacherFragment());     // Lista de profesores

        viewPager.setAdapter(adapter);
        configurarBottomNavigationEstudiante();
    }

    private void configurarBottomNavigationProfesor() {
        Log.d("SESSION_MANAGER", "Configurando BottomNavigation para PROFESOR");

        binding.btnNavView.getMenu().clear();
        binding.btnNavView.inflateMenu(R.menu.bottom_nav_menu_teacher);

        binding.btnNavView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.Inicio) {
                viewPager.setCurrentItem(0, true);
                Log.d("SESSION_MANAGER", "Profesor - Navegación a Inicio");
            } else if (item.getItemId() == R.id.Score) {
                viewPager.setCurrentItem(1, true);
                Log.d("SESSION_MANAGER", "Profesor - Navegación a Estadísticas");
            } else if (item.getItemId() == R.id.Students) {
                viewPager.setCurrentItem(2, true);
                Log.d("SESSION_MANAGER", "Profesor - Navegación a Estudiantes");
            }
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        binding.btnNavView.setSelectedItemId(R.id.Inicio);
                        break;
                    case 1:
                        binding.btnNavView.setSelectedItemId(R.id.Score);
                        break;
                    case 2:
                        binding.btnNavView.setSelectedItemId(R.id.Students);
                        break;
                }
            }
        });
    }

    private void configurarBottomNavigationEstudiante() {
        Log.d("SESSION_MANAGER", "Configurando BottomNavigation para ESTUDIANTE");

        binding.btnNavView.getMenu().clear();
        binding.btnNavView.inflateMenu(R.menu.bottom_nav_menu_student);

        binding.btnNavView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.Inicio) {
                viewPager.setCurrentItem(0, true);
                Log.d("SESSION_MANAGER", "Estudiante - Navegación a Inicio");
            } else if (item.getItemId() == R.id.Score) {
                viewPager.setCurrentItem(1, true);
                Log.d("SESSION_MANAGER", "Estudiante - Navegación a Estadísticas");
            } else if (item.getItemId() == R.id.Teachers) {
                viewPager.setCurrentItem(2, true);
                Log.d("SESSION_MANAGER", "Estudiante - Navegación a Profesores");
            }
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        binding.btnNavView.setSelectedItemId(R.id.Inicio);
                        break;
                    case 1:
                        binding.btnNavView.setSelectedItemId(R.id.Score);
                        break;
                    case 2:
                        binding.btnNavView.setSelectedItemId(R.id.Teachers);
                        break;
                }
            }
        });
    }

    private void verificarYGenerarCodigoProfesor(String userId) {
        Log.d("SESSION_MANAGER", "Verificando código para profesor - UserId: " + userId);

        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("SESSION_MANAGER", "Documento usuario obtenido - Existe: " + documentSnapshot.exists());

                    if (documentSnapshot.exists()) {
                        String rol = documentSnapshot.getString("rol");
                        String codigoProfesor = documentSnapshot.getString("codigoProfesor");

                        Log.d("SESSION_MANAGER", "Datos usuario - Rol: " + rol + ", Código: " + codigoProfesor);

                        if ("profesor".equals(rol)) {
                            Log.d("SESSION_MANAGER", "Usuario es PROFESOR");
                            if (codigoProfesor == null || codigoProfesor.isEmpty()) {
                                Log.d("SESSION_MANAGER", "Profesor sin código - Generando código único");
                                String nuevoCodigo = generarCodigo();
                                guardarCodigoProfesor(userId, nuevoCodigo);
                            } else {
                                Log.d("SESSION_MANAGER", "Profesor YA TIENE código en Firestore: " + codigoProfesor);
                                // ✅ Asegurar que el código existente se guarde en SessionManager
                                sessionManager.saveProfessorCode(codigoProfesor);
                            }
                        } else {
                            Log.d("SESSION_MANAGER", "Usuario NO es profesor - Rol: " + rol);
                        }
                    } else {
                        Log.e("SESSION_MANAGER", "ERROR: Documento de usuario no existe en Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SESSION_MANAGER", "ERROR al verificar rol de usuario: " + e.getMessage());
                });
    }

    private String generarCodigo() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder codigo = new StringBuilder(6);

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(caracteres.length());
            codigo.append(caracteres.charAt(index));
        }

        String codigoFinal = codigo.toString();
        Log.d("SESSION_MANAGER", "Código final generado: " + codigoFinal);
        return codigoFinal;
    }

    private void guardarCodigoProfesor(String userId, String codigo) {
        Log.d("SESSION_MANAGER", "Guardando código en Firestore - UserId: " + userId + ", Código: " + codigo);

        Map<String, Object> updates = new HashMap<>();
        updates.put("codigoProfesor", codigo);

        db.collection("usuarios").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // ✅ GUARDAR CÓDIGO EN SESSION MANAGER SOLO CUANDO SE GENERA
                    sessionManager.saveProfessorCode(codigo);
                    Log.d("SESSION_MANAGER", "✅ Código guardado EXITOSAMENTE en Firestore y SessionManager");
                    mostrarDialogoCodigoAsignado(codigo);
                })
                .addOnFailureListener(e -> {
                    Log.e("SESSION_MANAGER", "❌ ERROR al guardar código de profesor: " + e.getMessage());

                    // Intentar con set() si update falla
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("codigoProfesor", codigo);
                    db.collection("usuarios").document(userId)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                sessionManager.saveProfessorCode(codigo);
                                Log.d("SESSION_MANAGER", "✅ Código guardado con set() exitoso");
                                mostrarDialogoCodigoAsignado(codigo);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e("SESSION_MANAGER", "❌ ERROR también con set(): " + e2.getMessage());
                            });
                });
    }

    private void mostrarDialogoCodigoAsignado(String codigo) {
        Log.d("SESSION_MANAGER", "Mostrando diálogo con código asignado: " + codigo);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_codigo_profesor_titulo);

        // Usar el recurso con placeholder para el código
        String mensaje = getString(R.string.dialog_codigo_profesor_mensaje, codigo);
        builder.setMessage(mensaje);

        builder.setPositiveButton(R.string.dialog_btn_copiar_codigo, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(
                    getString(R.string.clipboard_codigo_label),
                    codigo
            );
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.toast_codigo_copiado, Toast.LENGTH_SHORT).show();
            Log.d("SESSION_MANAGER", "Código copiado al portapapeles: " + codigo);
        });
        builder.setNegativeButton(R.string.dialog_btn_entendido, (dialog, which) -> {
            Log.d("SESSION_MANAGER", "Diálogo cerrado");
            dialog.dismiss();
        });
        builder.setCancelable(false);

        try {
            builder.show();
            Log.d("SESSION_MANAGER", "Diálogo mostrado exitosamente");
        } catch (Exception e) {
            Log.e("SESSION_MANAGER", "Error al mostrar diálogo: " + e.getMessage());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("SESSION_MANAGER", "MainActivity onStart()");
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !currentUser.isEmailVerified()) {
            Log.d("SESSION_MANAGER", "Usuario no autenticado en onStart() - redirigiendo");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}