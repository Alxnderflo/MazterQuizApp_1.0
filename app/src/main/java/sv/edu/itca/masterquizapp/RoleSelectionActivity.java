package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
public class RoleSelectionActivity extends AppCompatActivity {

    private Button btnEstudiante, btnProfesor;
    private FirebaseAuth auth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        btnEstudiante = findViewById(R.id.btnEstudiante);
        btnProfesor = findViewById(R.id.btnProfesor);

        btnEstudiante.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cambiar estado de los botones
                btnEstudiante.setEnabled(false);
                btnEstudiante.setText("Guardando...");
                btnProfesor.setEnabled(false);

                guardarUsuarioYRedirigir("estudiante");
            }
        });

        btnProfesor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cambiar estado de los botones
                btnProfesor.setEnabled(false);
                btnProfesor.setText("Guardando...");
                btnEstudiante.setEnabled(false);

                guardarUsuarioYRedirigir("profesor");
            }
        });
    }

    private void guardarUsuarioYRedirigir(String rol) {
        if (user == null) {
            finish();
            return;
        }

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombre", user.getDisplayName());
        usuario.put("email", user.getEmail());
        usuario.put("rol", rol);
        usuario.put("fechaRegistro", new Date());
        usuario.put("proveedor", "google");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usuarios")
                .document(user.getUid())
                .set(usuario)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RoleSelectionActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Restaurar estado de los botones en caso de error
                    btnEstudiante.setEnabled(true);
                    btnEstudiante.setText("Estudiante");
                    btnProfesor.setEnabled(true);
                    btnProfesor.setText("Profesor");

                    Toast.makeText(RoleSelectionActivity.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}