package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

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

    // Variables para control de quiz eliminado
    private ListenerRegistration quizListener;
    private boolean quizActivo = true;

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

        // Configurar listener para detectar borrado del quiz
        configurarListenerQuiz();

        cargarPreguntas();

        // Configurar el manejo del botón de retroceso
        configurarBackPressed();
    }

    // Método para configurar listener del quiz - CORREGIDO
    private void configurarListenerQuiz() {
        quizListener = db.collection("quizzes").document(quizId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {

                        // Si ya cerramos la actividad, ignorar
                        if (!quizActivo) return;

                        if (e != null) {
                            Log.e("ResolverQuiz", "Error en listener del quiz: " + e.getMessage());
                            return;
                        }

                        // Verificar si el quiz fue eliminado
                        if (snapshot == null || !snapshot.exists()) {
                            // Quiz eliminado
                            mostrarQuizEliminado();
                        } else {
                            // Verificar si el quiz es privado y NO es del usuario actual
                            Boolean esPublico = snapshot.getBoolean("esPublico");
                            String quizUserId = snapshot.getString("userId");

                            // Obtener usuario actual
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            String currentUserId = currentUser != null ? currentUser.getUid() : null;

                            // Si el quiz es privado Y el usuario actual NO es el propietario
                            if (esPublico != null && !esPublico && !currentUserId.equals(quizUserId)) {
                                mostrarQuizPrivado();
                            }
                            // En cualquier otro caso (público, o privado pero del usuario actual), permitir continuar
                        }
                    }
                });
    }

    // Método para manejar quiz eliminado
    private void mostrarQuizEliminado() {
        quizActivo = false;

        runOnUiThread(() -> {
            new AlertDialog.Builder(ResolverQuizActivity.this)
                    .setTitle(R.string.quiz_eliminado_titulo)
                    .setMessage(R.string.quiz_eliminado_mensaje)
                    .setPositiveButton(R.string.btn_entendido, (dialog, which) -> {
                        notificarQuizEliminado(); // Notificar al TeacherFragment
                        finish();
                    })
                    .setOnDismissListener(dialog -> {
                        notificarQuizEliminado(); // Notificar al TeacherFragment
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    // Método para manejar quiz hecho privado - SOLO para quizzes de otros usuarios
    private void mostrarQuizPrivado() {
        quizActivo = false;

        runOnUiThread(() -> {
            new AlertDialog.Builder(ResolverQuizActivity.this)
                    .setTitle(R.string.quiz_no_disponible_titulo)
                    .setMessage(R.string.quiz_privado_mensaje)
                    .setPositiveButton(R.string.btn_entendido, (dialog, which) -> {
                        notificarQuizEliminado(); // Notificar al TeacherFragment
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    // NUEVO: Método para notificar al TeacherFragment que debe recargar
    private void notificarQuizEliminado() {
        // Enviar broadcast para notificar a TeacherFragment
        Intent intent = new Intent("QUIZ_ELIMINADO");
        intent.putExtra("quiz_id", quizId);
        sendBroadcast(intent);

        Log.d("ResolverQuiz", "Notificando eliminación del quiz: " + quizId);
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
                    // Verificar que el quiz sigue activo
                    if (!quizActivo) {
                        return;
                    }

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
                    // Solo mostrar error si el quiz sigue activo
                    if (!quizActivo) {
                        return;
                    }

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
        // Verificar que el quiz sigue activo
        if (!quizActivo) {
            return;
        }

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
        // Verificar que el quiz sigue activo
        if (!quizActivo) {
            return;
        }

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

    // Método para navegar a resultados
    private void irAResultados() {
        // Verificar que el quiz sigue activo
        if (!quizActivo) {
            return;
        }

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

    // Limpiar listener cuando se destruya la actividad
    @Override
    protected void onDestroy() {
        super.onDestroy();
        quizActivo = false;
        if (quizListener != null) {
            quizListener.remove();
        }
    }
}