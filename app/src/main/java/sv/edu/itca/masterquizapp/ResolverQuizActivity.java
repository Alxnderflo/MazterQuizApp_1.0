package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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

    // Interfaz para comunicación con fragments
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
            Toast.makeText(this, R.string.error_cargar_quiz, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        listaPreguntas = new ArrayList<>();
        resultados = new ArrayList<>();

        // Inicializar el listener de respuestas
        respuestaListener = new OnPreguntaRespondidaListener() {
            @Override
            public void onPreguntaRespondida(int posicionPregunta, String respuestaUsuario, boolean esCorrecta) {
                procesarRespuesta(posicionPregunta, respuestaUsuario, esCorrecta);
            }
        };

        inicializarViews();
        cargarPreguntas();

        // Configurar el manejo del botón de retroceso
        configurarBackPressed();
    }

    private void configurarBackPressed() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarDialogoSalirQuiz();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void mostrarDialogoSalirQuiz() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_salir_quiz_titulo)
                .setMessage(R.string.dialog_salir_quiz_mensaje)
                .setPositiveButton(R.string.dialog_btn_salir, (dialog, which) -> finish())
                .setNegativeButton(R.string.btn_quiz_continuar, null)
                .show();
    }

    private void inicializarViews() {
        vpPreguntas = findViewById(R.id.vpPreguntas);
        btnContinuar = findViewById(R.id.btnContinuarQuiz);

        // Configurar ViewPager2 para deshabilitar deslizamiento manual
        vpPreguntas.setUserInputEnabled(false);

        // Inicializar adapter con lista vacía temporalmente
        adapter = new QuizPagerAdapter(this, listaPreguntas, quizTitulo, respuestaListener);
        vpPreguntas.setAdapter(adapter);

        // Configurar botón Continuar
        btnContinuar.setOnClickListener(v -> {
            avanzarSiguientePregunta();
        });
    }

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

                    // Verificar que tenemos entre 5 y 20 preguntas
                    if (listaPreguntas.size() < 5 || listaPreguntas.size() > 20) {
                        Toast.makeText(this, R.string.toast_rango_preguntas_invalido, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    // Actualizar adapter con las preguntas reales
                    adapter.actualizarPreguntas(listaPreguntas);
                    adapter.notifyDataSetChanged();

                    // Inicializar lista de resultados
                    inicializarResultados();
                })
                .addOnFailureListener(e -> {
                    String mensajeError = getString(R.string.toast_error_cargar_preguntas, e.getMessage());
                    Toast.makeText(this, mensajeError, Toast.LENGTH_SHORT).show();
                    Log.e("ResolverQuiz", "Error cargando preguntas: " + e.getMessage());
                });
    }

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

    private void procesarRespuesta(int posicionPregunta, String respuestaUsuario, boolean esCorrecta) {
        if (posicionPregunta < resultados.size()) {
            ResultadoPregunta resultado = resultados.get(posicionPregunta);
            resultado.setRespuestaUsuario(respuestaUsuario);
            resultado.setEsCorrecta(esCorrecta);
            resultado.setRespuestaCorrecta(listaPreguntas.get(posicionPregunta).getCorrecta());

            // Mostrar botón Continuar
            btnContinuar.setVisibility(View.VISIBLE);
        }
    }

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

    // ✅ CORREGIDO: Método para navegar a resultados
    private void irAResultados() {
        // Calcular puntuación
        int respuestasCorrectas = 0;
        for (ResultadoPregunta resultado : resultados) {
            if (resultado.isEsCorrecta()) {
                respuestasCorrectas++;
            }
        }

        Intent intent = new Intent(this, ResultadosActivity.class);
        intent.putExtra("quiz_id", quizId);
        intent.putExtra("quiz_titulo", quizTitulo);
        intent.putExtra("total_preguntas", listaPreguntas.size()); // Usar el tamaño real de preguntas cargadas
        intent.putExtra("respuestas_correctas", respuestasCorrectas);
        intent.putParcelableArrayListExtra("resultados", new ArrayList<>(resultados));
        startActivity(intent);
        finish();
    }
}