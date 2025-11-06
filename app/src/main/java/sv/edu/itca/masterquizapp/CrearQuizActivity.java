package sv.edu.itca.masterquizapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CrearQuizActivity extends AppCompatActivity {
    private EditText etTituloQuiz, etDescripcionQuiz;
    private Button btnGuardarQuiz;
    private CardView cvContenedorImg;
    private ImageView imgPreview;
    private TextView tvImgHint;
    private FirebaseFirestore db;
    private String imagenUrl = null;
    private static final int CODIGO_SELECCION_IMG = 1;
    private static final int CODIGO_SOLICITUD_PERMISO = 100;

    // VARIABLES DE EDICIÓN
    private boolean esEdicion = false;
    private String quizIdEdit = null;

    // NUEVAS VARIABLES PARA CONTROL DE VISIBILIDAD
    private LinearLayout switchContainer;
    private SwitchCompat switchEsPublico;
    private String userRol = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_quiz);

        db = FirebaseFirestore.getInstance();

        etTituloQuiz = findViewById(R.id.etTituloQuiz);
        etDescripcionQuiz = findViewById(R.id.etDescripcionQuiz);
        btnGuardarQuiz = findViewById(R.id.btnGuardarQuiz);
        cvContenedorImg = findViewById(R.id.cvImgContenedor);
        imgPreview = findViewById(R.id.imgPreview);
        tvImgHint = findViewById(R.id.tvImgHint);

        // NUEVO: Inicializar vistas del switch
        switchContainer = findViewById(R.id.switchContainer);
        switchEsPublico = findViewById(R.id.switchEsPublico);

        // CAMBIO: Verificar si estamos en modo edición
        esEdicion = getIntent().getBooleanExtra("MODO_EDICION", false);
        if (esEdicion) {
            setTitle("Editar Quiz");
            quizIdEdit = getIntent().getStringExtra("quiz_id");
            String titulo = getIntent().getStringExtra("quiz_titulo");
            String descripcion = getIntent().getStringExtra("quiz_descripcion");
            imagenUrl = getIntent().getStringExtra("quiz_imagenUrl");

            etTituloQuiz.setText(titulo);
            etDescripcionQuiz.setText(descripcion);

            // Cargar la imagen si existe
            if (imagenUrl != null && !imagenUrl.isEmpty()) {
                // Limpiar el tint del ImageView antes de cargar la imagen
                imgPreview.setImageTintList(null);
                // Ajustar el tamaño del ImageView para que ocupe todo el contenedor
                imgPreview.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                imgPreview.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                imgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imgPreview.requestLayout();
                Glide.with(this)
                        .load(imagenUrl)
                        .into(imgPreview);
                tvImgHint.setVisibility(View.GONE);
            } else {
                // Restaurar el tamaño original del ícono
                imgPreview.getLayoutParams().width = (int) (64 * getResources().getDisplayMetrics().density);
                imgPreview.getLayoutParams().height = (int) (64 * getResources().getDisplayMetrics().density);
                imgPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imgPreview.requestLayout();
                imgPreview.setImageResource(R.drawable.ico_empty_quiz);
                imgPreview.setImageTintList(ColorStateList.valueOf(getColor(R.color.colorDefaultImg)));
                tvImgHint.setVisibility(View.VISIBLE);
            }
        } else {
            setTitle("Crear Quiz");
        }

        // Obtener el usuario actual y configurar el rol
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            obtenerRolYConfigurarInterfaz(userId, null);
        }

        cvContenedorImg.setOnClickListener(v -> verificarPermisos());
        btnGuardarQuiz.setOnClickListener(v -> guardarQuiz());
    }

    // NUEVO MÉTODO: Obtener rol del usuario y configurar interfaz
    private void obtenerRolYConfigurarInterfaz(String userId, Runnable onComplete) {
        FirebaseFirestore.getInstance().collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userRol = documentSnapshot.getString("rol");
                        Log.d("CrearQuiz", "Rol obtenido: " + userRol);

                        // Configurar visibilidad del switch según el rol
                        if ("profesor".equals(userRol)) {
                            switchContainer.setVisibility(View.VISIBLE);
                            // Para profesores, valor por defecto es true
                            switchEsPublico.setChecked(true);
                            Log.d("CrearQuiz", "Switch visible - checked: " + switchEsPublico.isChecked());

                            // Si estamos editando, cargar el estado actual de esPublico
                            if (esEdicion) {
                                cargarEstadoEsPublico();
                            }
                        } else {
                            // Para estudiantes, ocultar el switch
                            switchContainer.setVisibility(View.GONE);
                            Log.d("CrearQuiz", "Switch oculto (estudiante)");
                        }

                        if (onComplete != null) {
                            onComplete.run();
                        }
                    } else {
                        Toast.makeText(this, "Error: No se encontraron datos del usuario", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al obtener datos del usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    // NUEVO MÉTODO: Cargar estado de esPublico al editar
    private void cargarEstadoEsPublico() {
        if (quizIdEdit != null) {
            db.collection("quizzes").document(quizIdEdit)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean esPublico = documentSnapshot.getBoolean("esPublico");
                            if (esPublico != null) {
                                switchEsPublico.setChecked(esPublico);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CrearQuiz", "Error al cargar estado de esPublico: " + e.getMessage());
                    });
        }
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
                requestPermissions(new String[]{permission}, CODIGO_SOLICITUD_PERMISO);
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
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), CODIGO_SELECCION_IMG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODIGO_SELECCION_IMG && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            Log.d("CrearQuiz", "Imagen seleccionada: " + imageUri.toString());
            subirImagen(imageUri);
        } else {
            Log.d("CrearQuiz", "Selección de imagen cancelada o fallida");
        }
    }

    //para verificar conexión a internet
    private boolean hayConexionInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    private void subirImagen(Uri imageUri) {
        Log.d("CrearQuiz", "Iniciando subida de imagen: " + imageUri.toString());

        // Verificar conexión a internet
        if (!hayConexionInternet()) {
            Log.d("CrearQuiz", "Sin conexión a internet, usando imagen por defecto");
            usarImagenPorDefecto();
            return;
        }

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
                            // Ajustar el tamaño del ImageView para que ocupe todo el contenedor
                            imgPreview.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
                            imgPreview.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                            imgPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            imgPreview.requestLayout();
                            Glide.with(CrearQuizActivity.this)
                                    .load(imagenUrl)
                                    .into(imgPreview);
                            tvImgHint.setVisibility(View.GONE);
                            btnGuardarQuiz.setEnabled(true);
                            btnGuardarQuiz.setText(esEdicion ? "Actualizar Quiz" : "Guardar Quiz");
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
                            btnGuardarQuiz.setText(esEdicion ? "Actualizar Quiz" : "Guardar Quiz");
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

    private void usarImagenPorDefecto() {
        runOnUiThread(() -> {
            // Establecer imagen por defecto
            imagenUrl = null; // Esto hará que use la imagen por defecto del placeholder

            // Mostrar imagen por defecto
            imgPreview.setImageResource(R.drawable.ico_empty_quiz);
            imgPreview.setImageTintList(ColorStateList.valueOf(getColor(R.color.colorDefaultImg)));
            tvImgHint.setVisibility(View.VISIBLE);
            tvImgHint.setText("Imagen por defecto");

            btnGuardarQuiz.setEnabled(true);
            btnGuardarQuiz.setText(esEdicion ? "Actualizar Quiz" : "Guardar Quiz");

            Toast.makeText(this,
                    "¡Sin conexión a internet. Se usará imagen por defecto.",
                    Toast.LENGTH_LONG).show();
        });
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

        // Determinar el valor de esPublico según el rol ya obtenido
        boolean esPublico;
        if ("profesor".equals(userRol)) {
            esPublico = switchEsPublico.isChecked();
            Log.d("CrearQuiz", "Profesor - esPublico: " + esPublico);
        } else {
            // Estudiantes siempre crean quizzes privados
            esPublico = false;
            Log.d("CrearQuiz", "Estudiante - esPublico: " + esPublico);
        }

        //codigo para edicion
        if (esEdicion) {
            DocumentReference documentoQuiz = db.collection("quizzes").document(quizIdEdit);

            //Actualizar los campos editables
            Map<String, Object> updates = new HashMap<>();
            updates.put("titulo", titulo);
            updates.put("descripcion", descripcion);
            updates.put("imagenUrl", imagenUrl);
            updates.put("esPublico", esPublico); // NUEVO: Actualizar visibilidad

            documentoQuiz.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Quizz actualizado exitosamente", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } else {
            // Modo creación: crear un nuevo quiz

            // NUEVO: Obtener el nombre del usuario
            String userNombre = currentUser.getDisplayName();
            if (userNombre == null || userNombre.isEmpty()) {
                // Si no tiene display name, usar el email sin dominio
                String email = currentUser.getEmail();
                if (email != null && email.contains("@")) {
                    userNombre = email.substring(0, email.indexOf("@"));
                } else {
                    userNombre = "Usuario";
                }
            }

            DocumentReference nuevoDocumento = db.collection("quizzes").document();
            String nuevoId = nuevoDocumento.getId();

            // NUEVO: Usar el constructor corregido
            Quiz quiz = new Quiz(titulo, descripcion, imagenUrl, userId, userNombre, userRol, esPublico);

            nuevoDocumento.set(quiz)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("CrearQuiz", "Quiz guardado en Firestore - esPublico: " + esPublico);
                        Toast.makeText(this, "Quiz guardado exitosamente", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CrearQuizActivity.this, PreguntasActivity.class);
                        intent.putExtra("quiz_id", nuevoId);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CrearQuiz", "Error al sincronizar: " + e.getMessage());
                        Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_SOLICITUD_PERMISO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirSelectorImagen();
            } else {
                Toast.makeText(this, "Se necesitan permisos para seleccionar imágenes", Toast.LENGTH_SHORT).show();
            }
        }
    }
}