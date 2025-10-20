package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerificacionEmailActivity extends AppCompatActivity {
    private TextView tvEmailInfo, tvInstrucciones;
    private Button btnReenviarCorreo, btnIrALogin;
    private FirebaseAuth auth;
    private String userEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verificacion_email);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });// Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Obtener el email del intent
        Intent intent = getIntent();
        userEmail = intent.getStringExtra("email");

        // Enlazar vistas
        tvEmailInfo = findViewById(R.id.tvEmailInfo);
        tvInstrucciones = findViewById(R.id.tvInstrucciones);
        btnReenviarCorreo = findViewById(R.id.btnReenviarCorreo);
        btnIrALogin = findViewById(R.id.btnIrALogin);

        // Configurar la información del email
        if (userEmail != null) {
            tvEmailInfo.setText("Hemos enviado un correo de verificación a:\n" + userEmail);
        }

        // Botón para reenviar correo
        btnReenviarCorreo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reenviarCorreoVerificacion();
            }
        });

        // Botón para ir al login
        btnIrALogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                irALogin();
            }
        });

        // Manejar el botón de retroceso con el nuevo método
        setupBackPressedHandler();
    }

    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Evitar que el usuario regrese al registro, redirigir al login
                irALogin();
            }
        });
    }

    private void reenviarCorreoVerificacion() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Mostrar que se está reenviando
            btnReenviarCorreo.setEnabled(false);
            btnReenviarCorreo.setText("Enviando...");

            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        // Restaurar el botón
                        btnReenviarCorreo.setEnabled(true);
                        btnReenviarCorreo.setText("Reenviar correo de verificación");

                        if (task.isSuccessful()) {
                            Toast.makeText(VerificacionEmailActivity.this,
                                    "Correo de verificación reenviado",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(VerificacionEmailActivity.this,
                                    "Error al reenviar correo: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            Toast.makeText(this, "No se encontró usuario autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    private void irALogin() {
        // Cerrar sesión para forzar la verificación
        auth.signOut();

        Intent intent = new Intent(VerificacionEmailActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}