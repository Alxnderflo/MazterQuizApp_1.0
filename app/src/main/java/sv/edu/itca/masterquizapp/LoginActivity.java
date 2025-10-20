package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private EditText editEmail, editPassword;
    private TextView tvRegister;
    private Button btnLogin, btnGoogle;
    private FirebaseAuth auth;
    private GoogleSignInClient client;
    private static final int RC_SIGN_IN = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar vistas
        tvRegister = findViewById(R.id.tvRegister);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Configurar Google Sign In
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        client = GoogleSignIn.getClient(this, options);

        // Listener para botón de Google
        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cambiar estado de los botones
                btnGoogle.setEnabled(false);
                btnGoogle.setText("Iniciando...");
                btnLogin.setEnabled(false);

                Intent i = client.getSignInIntent();
                startActivityForResult(i, RC_SIGN_IN);
            }
        });

        // Listener para login normal
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editEmail.getText().toString();
                String password = editPassword.getText().toString();
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Cambiar estado de los botones
                btnLogin.setEnabled(false);
                btnLogin.setText("Iniciando sesión...");
                btnGoogle.setEnabled(false);
                editEmail.setEnabled(false);
                editPassword.setEnabled(false);

                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                // Verificar si el correo está confirmado
                                if (user.isEmailVerified()) {
                                    // Correo verificado, permitir acceso
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    // Correo no verificado, mostrar mensaje y cerrar sesión
                                    auth.signOut();
                                    Toast.makeText(LoginActivity.this, "Por favor, verifica tu correo electrónico antes de iniciar sesión. Revisa tu bandeja de entrada.", Toast.LENGTH_LONG).show();

                                    // Restaurar estado de los botones
                                    btnLogin.setEnabled(true);
                                    btnLogin.setText("Iniciar Sesión");
                                    btnGoogle.setEnabled(true);
                                    editEmail.setEnabled(true);
                                    editPassword.setEnabled(true);
                                }
                            }
                        } else {
                            // Restaurar estado de los botones en caso de error
                            btnLogin.setEnabled(true);
                            btnLogin.setText("Iniciar Sesión");
                            btnGoogle.setEnabled(true);
                            editEmail.setEnabled(true);
                            editPassword.setEnabled(true);

                            Toast.makeText(LoginActivity.this, "Error al iniciar sesión", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

                auth.signInWithCredential(credential)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = auth.getCurrentUser();
                                    if (user != null) {
                                        // Verificar si es usuario nuevo
                                        if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                                            // USUARIO NUEVO: Redirigir a selección de rol
                                            Intent intent = new Intent(getApplicationContext(), RoleSelectionActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            // USUARIO EXISTENTE: Redirigir directamente
                                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    }
                                } else {
                                    // Restaurar estado de los botones si hay errores
                                    btnGoogle.setEnabled(true);
                                    btnGoogle.setText("Continuar con Google");
                                    btnLogin.setEnabled(true);

                                    Toast.makeText(LoginActivity.this, "Error en la autenticación: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

            } catch (ApiException e) {
                // Restaurar estado de los botones en caso de error
                btnGoogle.setEnabled(true);
                btnGoogle.setText("Continuar con Google");
                btnLogin.setEnabled(true);

                Toast.makeText(LoginActivity.this, "Error al iniciar sesión con Google: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verificar si ya está logueado
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }
}