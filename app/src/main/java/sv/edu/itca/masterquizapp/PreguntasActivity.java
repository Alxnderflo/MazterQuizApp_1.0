package sv.edu.itca.masterquizapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PreguntasActivity extends AppCompatActivity {
    private RecyclerView rvQuestions;
    private LinearLayout layoutVacio;
    private Button btnAdd, btnCrearPregunta;
    private PreguntasAdapter adapter;
    private List<Pregunta> listaPreguntas;
    private List<String> listaPreguntasIds; // CAMBIO: Lista de IDs
    private FirebaseFirestore db;
    private String quizId;

    // Views del header
    private ImageView imgQuizHeader;
    private TextView tvHeaderT, tvHeaderD, tvHeaderNumQ, tvHeaderFecha;

    public static Intent newIntent(Context context, String quizId) {
        Intent intent = new Intent(context, PreguntasActivity.class);
        intent.putExtra("quiz_id", quizId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas);

        quizId = getIntent().getStringExtra("quiz_id");
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(this, "Error: No se pudo identificar el quiz", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        listaPreguntas = new ArrayList<>();
        listaPreguntasIds = new ArrayList<>(); // CAMBIO: Inicializar la lista de IDs

        inicializarViews();
        cargarDatosQuiz();
        cargarPreguntas();
    }

    private void inicializarViews() {
        rvQuestions = findViewById(R.id.rvQuestions);
        layoutVacio = findViewById(R.id.layoutVacio);
        btnAdd = findViewById(R.id.btnAdd);
        btnCrearPregunta = findViewById(R.id.btnCrearPregunta);

        // Header views
        imgQuizHeader = findViewById(R.id.imgQuizHeader);
        tvHeaderT = findViewById(R.id.tvHeaderT);
        tvHeaderD = findViewById(R.id.tvHeaderD);
        tvHeaderNumQ = findViewById(R.id.tvHeaderNumQ);
        tvHeaderFecha = findViewById(R.id.tvHeaderFecha);

        rvQuestions.setLayoutManager(new LinearLayoutManager(this));

        // CAMBIO: Inicializar el adapter con los listeners
        adapter = new PreguntasAdapter(listaPreguntas, new PreguntasAdapter.OnPreguntaClickListener() {
            @Override
            public void onPreguntaClick(Pregunta pregunta, String preguntaId, int position) {
                // CAMBIO: Abrir CrearPreguntasActivity en modo edición
                Intent intent = new Intent(PreguntasActivity.this, CrearPreguntasActivity.class);
                intent.putExtra("MODO_EDICION", true);
                intent.putExtra("quiz_id", quizId);
                intent.putExtra("pregunta_id", preguntaId);
                intent.putExtra("pregunta_enunciado", pregunta.getEnunciado());
                intent.putExtra("pregunta_correcta", pregunta.getCorrecta());
                intent.putExtra("pregunta_incorrecta1", pregunta.getIncorrecta1());
                intent.putExtra("pregunta_incorrecta2", pregunta.getIncorrecta2());
                intent.putExtra("pregunta_incorrecta3", pregunta.getIncorrecta3());
                intent.putExtra("pregunta_orden", pregunta.getOrden());
                startActivity(intent);
            }
        }, new PreguntasAdapter.OnPreguntaDeleteListener() {
            @Override
            public void onPreguntaDeleteClick(Pregunta pregunta, String preguntaId, int position) {
                // CAMBIO: Mostrar diálogo de confirmación para eliminar
                mostrarDialogoConfirmacionEliminacion(pregunta, preguntaId, position);
            }
        });
        rvQuestions.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> abrirCrearPregunta());
        btnCrearPregunta.setOnClickListener(v -> abrirCrearPregunta());
    }

    // CAMBIO: Método para mostrar diálogo de confirmación de eliminación
    private void mostrarDialogoConfirmacionEliminacion(Pregunta pregunta, String preguntaId, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Eliminar Pregunta");
        builder.setMessage("¿Estás seguro de que quieres eliminar esta pregunta?");
        builder.setPositiveButton("Eliminar", (dialog, which) -> {
            eliminarPregunta(preguntaId, position);
        });
        builder.setNegativeButton("Cancelar", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        // CAMBIO: Personalizar el color del botón eliminar
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    // CAMBIO: Metodo para eliminar una pregunta
    private void eliminarPregunta(String preguntaId, int position) {
        db.collection("quizzes").document(quizId)
                .collection("preguntas").document(preguntaId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // CAMBIO: Eliminado de Firestore, ahora actualizar el contador y reordenar
                    actualizarContadorYReordenar();
                    Toast.makeText(PreguntasActivity.this, "Pregunta eliminada", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PreguntasActivity.this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // CAMBIO: Metodo para actualizar el contador de preguntas y reordenar las restantes
    private void actualizarContadorYReordenar() {
        // Volver a cargar las preguntas para reordenar y actualizar el contador
        cargarPreguntas();
    }

    private void cargarDatosQuiz() {
        db.collection("quizzes").document(quizId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("PreguntasActivity", "Error al cargar quiz: " + error.getMessage());
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            Quiz quiz = snapshot.toObject(Quiz.class);
                            if (quiz != null) {
                                tvHeaderT.setText(quiz.getTitulo());
                                tvHeaderD.setText(quiz.getDescripcion() != null ? quiz.getDescripcion() : "");
                                tvHeaderNumQ.setText(quiz.getNumPreguntas() + " preguntas");

                                if (quiz.getFechaCreacion() != null) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                    tvHeaderFecha.setText(sdf.format(quiz.getFechaCreacion()));
                                }

                                if (quiz.getImagenUrl() != null && !quiz.getImagenUrl().isEmpty()) {
                                    Glide.with(PreguntasActivity.this)
                                            .load(quiz.getImagenUrl())
                                            .placeholder(R.drawable.ico_empty_quiz)
                                            .into(imgQuizHeader);
                                } else {
                                    imgQuizHeader.setImageResource(R.drawable.ico_empty_quiz);
                                }
                            }
                        }
                    }
                });
    }

    private void cargarPreguntas() {
        db.collection("quizzes").document(quizId)
                .collection("preguntas")
                .orderBy("orden")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("PreguntasActivity", "Error al cargar preguntas: " + error.getMessage());
                            mostrarVistaVacia();
                            return;
                        }
                        if (value != null && !value.isEmpty()) {
                            listaPreguntas.clear();
                            listaPreguntasIds.clear(); // CAMBIO: Limpiar la lista de IDs
                            for (DocumentSnapshot snapshot : value.getDocuments()) {
                                Pregunta pregunta = snapshot.toObject(Pregunta.class);
                                if (pregunta != null) {
                                    listaPreguntas.add(pregunta);
                                    listaPreguntasIds.add(snapshot.getId()); // CAMBIO: Guardar el ID
                                }
                            }
                            adapter.actualizarIds(listaPreguntasIds); // CAMBIO: Actualizar los IDs en el adapter
                            adapter.notifyDataSetChanged();
                            mostrarListaPreguntas();

                            // CAMBIO: Actualizar el contador de preguntas en el quiz (por si hay discrepancias)
                            actualizarContadorPreguntasEnQuiz(listaPreguntas.size());
                        } else {
                            mostrarVistaVacia();
                            actualizarContadorPreguntasEnQuiz(0);
                        }
                    }
                });
    }

    // CAMBIO: Metodo para actualizar el contador de preguntas en el quiz
    private void actualizarContadorPreguntasEnQuiz(int nuevoContador) {
        db.collection("quizzes").document(quizId)
                .update("numPreguntas", nuevoContador)
                .addOnSuccessListener(aVoid -> {
                    Log.d("PreguntasActivity", "Contador de preguntas actualizado a " + nuevoContador);
                })
                .addOnFailureListener(e -> {
                    Log.e("PreguntasActivity", "Error al actualizar contador de preguntas", e);
                });
    }

    private void abrirCrearPregunta() {
        startActivity(CrearPreguntasActivity.newIntent(this, quizId));
    }

    private void mostrarListaPreguntas() {
        rvQuestions.setVisibility(View.VISIBLE);
        layoutVacio.setVisibility(View.GONE);
    }

    private void mostrarVistaVacia() {
        rvQuestions.setVisibility(View.GONE);
        layoutVacio.setVisibility(View.VISIBLE);
    }
}