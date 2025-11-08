package sv.edu.itca.masterquizapp;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Log.d("FASE2", "MainActivity iniciada");

        // Verificar autenticación
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !currentUser.isEmailVerified()) {
            Log.d("FASE2", "Usuario no autenticado o email no verificado - redirigiendo a Login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Log.d("FASE2", "Usuario autenticado: " + currentUser.getUid() + ", email: " + currentUser.getEmail());
        configViewPager();
        configBottomNavigation();

        // Verificar y generar código para profesores
        verificarYGenerarCodigoProfesor(currentUser.getUid());
    }

    // Verificar si es profesor y generar código si no tiene
    private void verificarYGenerarCodigoProfesor(String userId) {
        Log.d("FASE2", "Iniciando verificación de código para profesor - UserId: " + userId);

        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("FASE2", "Documento usuario obtenido - Existe: " + documentSnapshot.exists());

                    if (documentSnapshot.exists()) {
                        String rol = documentSnapshot.getString("rol");
                        String codigoProfesor = documentSnapshot.getString("codigoProfesor");

                        Log.d("FASE2", "Datos usuario - Rol: " + rol + ", Código: " + codigoProfesor);

                        // Si es profesor y no tiene código, generarlo
                        if ("profesor".equals(rol)) {
                            Log.d("FASE2", "Usuario es PROFESOR");
                            if (codigoProfesor == null || codigoProfesor.isEmpty()) {
                                Log.d("FASE2", "Profesor sin código - Generando código único");
                                // CAMBIO: Generar directamente sin verificar unicidad
                                String nuevoCodigo = generarCodigo();
                                guardarCodigoProfesor(userId, nuevoCodigo);
                            } else {
                                Log.d("FASE2", "Profesor YA TIENE código: " + codigoProfesor);
                            }
                        } else {
                            Log.d("FASE2", "Usuario NO es profesor - Rol: " + rol);
                        }
                    } else {
                        Log.e("FASE2", "ERROR: Documento de usuario no existe en Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FASE2", "ERROR al verificar rol de usuario: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    // Generar código de 6 caracteres alfanuméricos
    private String generarCodigo() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Excluye O/0, I/1
        Random random = new Random();
        StringBuilder codigo = new StringBuilder(6);

        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(caracteres.length());
            codigo.append(caracteres.charAt(index));
            Log.v("FASE2", "Carácter " + (i+1) + ": " + caracteres.charAt(index));
        }

        String codigoFinal = codigo.toString();
        Log.d("FASE2", "Código final generado: " + codigoFinal);
        return codigoFinal;
    }

    // Guardar código en Firestore y mostrar diálogo
    private void guardarCodigoProfesor(String userId, String codigo) {
        Log.d("FASE2", "Guardando código en Firestore - UserId: " + userId + ", Código: " + codigo);

        Map<String, Object> updates = new HashMap<>();
        updates.put("codigoProfesor", codigo);

        db.collection("usuarios").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FASE2", "✅ Código guardado EXITOSAMENTE en Firestore");
                    mostrarDialogoCodigoAsignado(codigo);
                })
                .addOnFailureListener(e -> {
                    Log.e("FASE2", "❌ ERROR al guardar código de profesor: " + e.getMessage());
                    e.printStackTrace();

                    // Intentar con set() si update falla
                    Log.d("FASE2", "Intentando guardar con set()...");
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("codigoProfesor", codigo);
                    db.collection("usuarios").document(userId)
                            .set(userData, SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d("FASE2", "✅ Código guardado con set() exitoso");
                                mostrarDialogoCodigoAsignado(codigo);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e("FASE2", "❌ ERROR también con set(): " + e2.getMessage());
                            });
                });
    }

    // Mostrar diálogo con el código asignado
    private void mostrarDialogoCodigoAsignado(String codigo) {
        Log.d("FASE2", "Mostrando diálogo con código asignado: " + codigo);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Código de Profesor Asignado");
        builder.setMessage("Tu código único es: " + codigo + "\n\nComparte este código con tus estudiantes para que puedan agregarte.");
        builder.setPositiveButton("Copiar Código", (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Código Profesor", codigo);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show();
            Log.d("FASE2", "Código copiado al portapapeles: " + codigo);
        });
        builder.setNegativeButton("Entendido", (dialog, which) -> {
            Log.d("FASE2", "Diálogo cerrado");
            dialog.dismiss();
        });
        builder.setCancelable(false);

        try {
            builder.show();
            Log.d("FASE2", "Diálogo mostrado exitosamente");
        } catch (Exception e) {
            Log.e("FASE2", "Error al mostrar diálogo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configViewPager() {
        Log.d("FASE2", "Configurando ViewPager");
        viewPager = findViewById(R.id.viewPager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);

        adapter.addFragment(new HomeFragment());
        adapter.addFragment(new ScoreFragment());
        adapter.addFragment(new TeacherFragment());

        viewPager.setAdapter(adapter);
        Log.d("FASE2", "ViewPager configurado");
    }

    private void configBottomNavigation() {
        Log.d("FASE2", "Configurando BottomNavigation");
        binding.btnNavView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.Inicio) {
                viewPager.setCurrentItem(0, true);
                Log.d("FASE2", "Navegación a Inicio");
            } else if (item.getItemId() == R.id.Score) {
                viewPager.setCurrentItem(1, true);
                Log.d("FASE2", "Navegación a Score");
            } else if (item.getItemId() == R.id.Teacher) {
                viewPager.setCurrentItem(2, true);
                Log.d("FASE2", "Navegación a Teacher");
            }
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Log.d("FASE2", "Página seleccionada: " + position);
                switch (position) {
                    case 0:
                        binding.btnNavView.setSelectedItemId(R.id.Inicio);
                        break;
                    case 1:
                        binding.btnNavView.setSelectedItemId(R.id.Score);
                        break;
                    case 2:
                        binding.btnNavView.setSelectedItemId(R.id.Teacher);
                        break;
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("FASE2", "MainActivity onStart()");
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !currentUser.isEmailVerified()) {
            Log.d("FASE2", "Usuario no autenticado en onStart() - redirigiendo");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}