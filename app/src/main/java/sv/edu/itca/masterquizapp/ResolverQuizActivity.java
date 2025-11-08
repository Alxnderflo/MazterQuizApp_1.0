package sv.edu.itca.masterquizapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// CAMBIO-QUIZ: Nueva actividad para resolver el quiz
public class ResolverQuizActivity extends AppCompatActivity {
    private ViewPager2 vpPreguntas;
    private MaterialButton btnContinuar;
    private QuizPagerAdapter adapter;
    private FirebaseFirestore db;
    private String quizId;
    private String quizTitulo;
    private int totalPreguntas;
    private List<Pregunta> listaPreguntas;
    private List<ResultadoPregunta> resultados;

    // CAMBIO-QUIZ: Interfaz para comunicación con fragments
    public interface OnPreguntaRespondidaListener {
        void onPreguntaRespondida(int posicionPregunta, String respuestaUsuario, boolean esCorrecta);
    }

    private OnPreguntaRespondidaListener respuestaListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resolver_quiz);

        // Obtener datos del intent
        quizId = getIntent().getStringExtra("quiz_id");
        quizTitulo = getIntent().getStringExtra("quiz_titulo");
        totalPreguntas = getIntent().getIntExtra("total_preguntas", 0);

        if (quizId == null) {
            Toast.makeText(this, "Error: No se pudo cargar el quiz", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        listaPreguntas = new ArrayList<>();
        resultados = new ArrayList<>();

        // CAMBIO-QUIZ: Inicializar el listener de respuestas
        respuestaListener = new OnPreguntaRespondidaListener() {
            @Override
            public void onPreguntaRespondida(int posicionPregunta, String respuestaUsuario, boolean esCorrecta) {
                procesarRespuesta(posicionPregunta, respuestaUsuario, esCorrecta);
            }
        };

        inicializarViews();
        cargarPreguntas();

        // CAMBIO-QUIZ: Configurar el manejo del botón de retroceso (nueva forma)
        configurarBackPressed();
    }

    // CAMBIO-QUIZ: Nueva forma de manejar el back pressed
    private void configurarBackPressed() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarDialogoSalirQuiz();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    // CAMBIO-QUIZ: Diálogo para salir del quiz
    private void mostrarDialogoSalirQuiz() {
        new AlertDialog.Builder(this)
                .setTitle("Salir del Quiz")
                .setMessage("¿Estás seguro de que quieres salir? Se perderá tu progreso.")
                .setPositiveButton("Salir", (dialog, which) -> finish())
                .setNegativeButton("Continuar", null)
                .show();
    }

    private void inicializarViews() {
        vpPreguntas = findViewById(R.id.vpPreguntas);
        btnContinuar = findViewById(R.id.btnContinuarQuiz);

        // CAMBIO-QUIZ: Configurar ViewPager2 para deshabilitar deslizamiento manual
        vpPreguntas.setUserInputEnabled(false);

        // CAMBIO-QUIZ: Inicializar adapter con lista vacía temporalmente
        adapter = new QuizPagerAdapter(this, listaPreguntas, quizTitulo, respuestaListener);
        vpPreguntas.setAdapter(adapter);

        // CAMBIO-QUIZ: Configurar botón Continuar
        btnContinuar.setOnClickListener(v -> {
            avanzarSiguientePregunta();
        });
    }

    // CAMBIO-QUIZ: Método para cargar preguntas desde Firestore
    private void cargarPreguntas() {
        db.collection("quizzes").document(quizId)
                .collection("preguntas")
                .orderBy("orden")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaPreguntas.clear();
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                        Pregunta pregunta = snapshot.toObject(Pregunta.class);
                        if (pregunta != null) {
                            listaPreguntas.add(pregunta);
                        }
                    }

                    // CAMBIO-QUIZ: Verificar que tenemos entre 5 y 20 preguntas
                    if (listaPreguntas.size() < 5 || listaPreguntas.size() > 20) {
                        Toast.makeText(this, "El quiz debe tener entre 5 y 20 preguntas", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    // CAMBIO-QUIZ: Actualizar adapter con las preguntas reales
                    adapter.actualizarPreguntas(listaPreguntas);
                    adapter.notifyDataSetChanged();

                    // CAMBIO-QUIZ: Inicializar lista de resultados
                    inicializarResultados();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar preguntas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ResolverQuiz", "Error cargando preguntas: " + e.getMessage());
                });
    }

    // CAMBIO-QUIZ: Inicializar la lista de resultados
    private void inicializarResultados() {
        resultados.clear();
        for (int i = 0; i < listaPreguntas.size(); i++) {
            resultados.add(new ResultadoPregunta(
                    i + 1,
                    listaPreguntas.get(i).getEnunciado(),
                    "", // respuestaCorrecta se llenará después
                    "", // respuestaUsuario se llenará cuando responda
                    false // esCorrecta inicialmente false
            ));
        }
    }

    // CAMBIO-QUIZ: Procesar respuesta de una pregunta
    private void procesarRespuesta(int posicionPregunta, String respuestaUsuario, boolean esCorrecta) {
        if (posicionPregunta < resultados.size()) {
            ResultadoPregunta resultado = resultados.get(posicionPregunta);
            resultado.setRespuestaUsuario(respuestaUsuario);
            resultado.setEsCorrecta(esCorrecta);
            resultado.setRespuestaCorrecta(listaPreguntas.get(posicionPregunta).getCorrecta());

            // CAMBIO-QUIZ: Mostrar botón Continuar
            btnContinuar.setVisibility(View.VISIBLE);
        }
    }

    // CAMBIO-QUIZ: Avanzar a la siguiente pregunta o terminar quiz
    private void avanzarSiguientePregunta() {
        int currentItem = vpPreguntas.getCurrentItem();
        if (currentItem < listaPreguntas.size() - 1) {
            // Avanzar a la siguiente pregunta
            vpPreguntas.setCurrentItem(currentItem + 1, true);
            btnContinuar.setVisibility(View.GONE);
        } else {
            // Última pregunta completada - ir a resultados
            irAResultados();
        }
    }

    // CAMBIO-QUIZ: Navegar a la actividad de resultados
    private void irAResultados() {
        // Calcular puntuación
        int respuestasCorrectas = 0;
        for (ResultadoPregunta resultado : resultados) {
            if (resultado.isEsCorrecta()) {
                respuestasCorrectas++;
            }
        }

        Intent intent = new Intent(this, ResultadosActivity.class);
        intent.putExtra("quiz_titulo", quizTitulo);
        intent.putExtra("total_preguntas", totalPreguntas);
        intent.putExtra("respuestas_correctas", respuestasCorrectas);

        // CAMBIO-QUIZ: Corrección - pasar ArrayList de Parcelable correctamente
        intent.putParcelableArrayListExtra("resultados", new ArrayList<>(resultados));
        startActivity(intent);
        finish();
    }


}