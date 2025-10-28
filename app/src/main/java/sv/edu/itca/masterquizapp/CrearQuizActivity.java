package sv.edu.itca.masterquizapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class CrearQuizActivity extends AppCompatActivity {
    private EditText etTituloQuiz, etDescripcionQuiz;
    private Button btnGuardarQuiz;
    private CardView cvImageContainer;
    private ImageView imgPreview;
    private TextView tvImgHint;
    private FirebaseFirestore db;
    private String imagenUrl = null;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_quizz);

        db = FirebaseFirestore.getInstance();

        etTituloQuiz = findViewById(R.id.etTituloQuiz);
        etDescripcionQuiz = findViewById(R.id.etDescripcionQuiz);
        btnGuardarQuiz = findViewById(R.id.btnGuardarQuiz);
        cvImageContainer = findViewById(R.id.cvImageContainer);
        imgPreview = findViewById(R.id.imgPreview);
        tvImgHint = findViewById(R.id.tvImgHint);

        cvImageContainer.setOnClickListener(v -> verificarPermisos());
        btnGuardarQuiz.setOnClickListener(v -> guardarQuiz());
    }

    private void verificarPermisos() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            // Android 12 y anteriores
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{permission}, PERMISSION_REQUEST_CODE);
            } else {
                abrirSelectorImagen();
            }
        } else {
            abrirSelectorImagen();
        }
    }

    private void abrirSelectorImagen() {
        Log.d("CrearQuiz", "Permisos concedidos, abriendo selector de imagen");

        // Abrir selector de imagen
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            Log.d("CrearQuiz", "Imagen seleccionada: " + imageUri.toString());
            subirImagen(imageUri);
        } else {
            Log.d("CrearQuiz", "Selección de imagen cancelada o fallida");
        }
    }

    private void subirImagen(Uri imageUri) {
        Log.d("CrearQuiz", "Iniciando subida de imagen: " + imageUri.toString());

        // Mostrar indicador de carga
        btnGuardarQuiz.setEnabled(false);
        btnGuardarQuiz.setText("Subiendo imagen...");

        MediaManager.get().upload(imageUri)
                .option("folder", "quiz_images")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // Upload started
                        Log.d("Cloudinary", "Subida iniciada: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Opcional: mostrar progreso
                        int progress = (int) ((bytes * 100) / totalBytes);
                        Log.d("Cloudinary", "Progreso: " + progress + "%");
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        imagenUrl = (String) resultData.get("url");
                        // Convertir HTTP a HTTPS para compatibilidad con Android moderno
                        if (imagenUrl != null && imagenUrl.startsWith("http://")) {
                            imagenUrl = imagenUrl.replace("http://", "https://");
                        }
                        Log.d("Cloudinary", "Imagen subida exitosamente: " + imagenUrl);
                        Log.d("Cloudinary", "ResultData completo: " + resultData.toString());

                        runOnUiThread(() -> {
                            // Limpiar el tint del ImageView antes de cargar la imagen
                            imgPreview.setImageTintList(null);
                            Glide.with(CrearQuizActivity.this)
                                .load(imagenUrl)
                                .into(imgPreview);
                            tvImgHint.setVisibility(View.GONE);
                            btnGuardarQuiz.setEnabled(true);
                            btnGuardarQuiz.setText("Guardar Quiz");
                            Toast.makeText(CrearQuizActivity.this, "Imagen subida exitosamente", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e("Cloudinary", "Error subiendo imagen: " + error.getDescription());
                        Log.e("Cloudinary", "Error details: " + error.toString());

                        runOnUiThread(() -> {
                            Toast.makeText(CrearQuizActivity.this, "Error al subir imagen: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                            btnGuardarQuiz.setEnabled(true);
                            btnGuardarQuiz.setText("Guardar Quiz");
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Reintentar en caso de error de red
                        Log.d("Cloudinary", "Reintentando subida: " + error.getDescription());
                    }
                })
                .dispatch();
    }

    private void guardarQuiz() {
        String titulo = etTituloQuiz.getText().toString().trim();
        String descripcion = etDescripcionQuiz.getText().toString().trim();

        if (titulo.isEmpty()) {
            Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();

        FirebaseFirestore.getInstance().collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userNombre = documentSnapshot.getString("nombre");
                        String userRol = documentSnapshot.getString("rol");

                        DocumentReference nuevoDocumento = db.collection("quizzes").document();
                        String nuevoId = nuevoDocumento.getId();

                        Quiz quiz = new Quiz(titulo, descripcion, imagenUrl, userId, userNombre, userRol);

                        nuevoDocumento.set(quiz)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("CrearQuiz", "Quiz guardado en Firestore");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("CrearQuiz", "Error al sincronizar: " + e.getMessage());
                                });

                        Toast.makeText(this, "Quiz guardado exitosamente", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CrearQuizActivity.this, PreguntasActivity.class);
                        intent.putExtra("quiz_id", nuevoId);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Error: No se encontraron datos del usuario", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al obtener datos del usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirSelectorImagen();
            } else {
                Toast.makeText(this, "Se necesitan permisos para seleccionar imágenes", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
