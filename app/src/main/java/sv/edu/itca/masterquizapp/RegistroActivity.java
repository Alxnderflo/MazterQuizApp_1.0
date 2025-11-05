package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {
    private EditText editNombre, editEmail, editPassword, editConfirmPassword;
    private RadioGroup radioGroupRol;

    private Button btnRegister;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);


        // Enlazar los elementos de la vista
        editNombre = findViewById(R.id.editNombre);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        radioGroupRol = findViewById(R.id.radioGroupRol);
        btnRegister = findViewById(R.id.btnRegister);

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombre = editNombre.getText().toString().trim();
                String email = editEmail.getText().toString().trim();
                String password = editPassword.getText().toString().trim();
                String confirmPassword = editConfirmPassword.getText().toString().trim();

                // Obtener el rol seleccionado usando RadioGroup
                int selectedId = radioGroupRol.getCheckedRadioButtonId();
                String rol;

                if (selectedId == R.id.radioEstudiante) {
                    rol = "estudiante";
                } else if (selectedId == R.id.radioProfesor) {
                    rol = "profesor";
                } else {
                    Toast.makeText(RegistroActivity.this, "Selecciona un rol", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validaciones básicas
                if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegistroActivity.this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    Toast.makeText(RegistroActivity.this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegistroActivity.this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Deshabilitar botón durante el registro
                btnRegister.setEnabled(false);
                btnRegister.setText("Creando cuenta...");

                // Crear un nuevo usuario con Firebase Auth
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Usuario creado, ahora enviar correo de verificación
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                if (emailTask.isSuccessful()) {
                                    // Correo de verificación enviado, guardar datos en Firestore
                                    guardarUsuarioEnFirestore(nombre, email, rol);
                                } else {
                                    // Error al enviar correo, habilitar botón y mostrar error
                                    btnRegister.setEnabled(true);
                                    btnRegister.setText("Registrar");
                                    Toast.makeText(RegistroActivity.this, "Error al enviar correo de verificación: " + emailTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    // Opcional: eliminar el usuario si no se pudo enviar el correo
                                    user.delete();
                                }
                            });
                        } else {
                            // No se pudo obtener el usuario, habilitar botón y mostrar error
                            btnRegister.setEnabled(true);
                            btnRegister.setText("Registrar");
                            Toast.makeText(RegistroActivity.this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Re-habilitar el botón en caso de error
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Registrar");
                        Toast.makeText(RegistroActivity.this, "Error al crear usuario: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    // Metodo para guardar el usuario en Firestore
    private void guardarUsuarioEnFirestore(String nombre, String email, String rol) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            btnRegister.setEnabled(true);
            btnRegister.setText("Registrar");
            Toast.makeText(this, "Error: No se pudo obtener el usuario autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombre", nombre);
        usuario.put("email", email);
        usuario.put("rol", rol);
        usuario.put("fechaRegistro", new Date());

        // NUEVOS CAMPOS PARA SISTEMA PROFESOR-ALUMNO
        if (rol.equals("profesor")) {
            usuario.put("codigoProfesor", ""); // Se generará en FASE 2
        } else if (rol.equals("estudiante")) {
            usuario.put("profesoresAgregados", new ArrayList<String>()); // Lista vacía inicial
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usuarios")
                .document(user.getUid())
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    // Éxito al guardar en Firestore
                    // Redirigir a VerificacionEmailActivity
                    Intent intent = new Intent(RegistroActivity.this, VerificacionEmailActivity.class);
                    intent.putExtra("email", email);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Error al guardar en Firestore
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Registrar");
                    Toast.makeText(RegistroActivity.this, "Error al guardar datos del usuario: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    user.delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Toast.makeText(RegistroActivity.this, "Se eliminó el usuario por error en los datos", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

}