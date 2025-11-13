package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoreFragment extends Fragment {

    private RecyclerView rvQuizzesScore;
    private ScoreAdapter adapter;
    private List<EstadisticaQuiz> estadisticasList;
    private FirebaseFirestore db;
    private LinearLayout layoutVacio;
    private TextView tvTotalQuizzes, tvPromedio, tvMejorPuntaje;

    private ListenerRegistration resultadosListener;

    public ScoreFragment() {
    }

    public static ScoreFragment newInstance() {
        return new ScoreFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        estadisticasList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_score, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        rvQuizzesScore = view.findViewById(R.id.rvQuizzesScore);
        layoutVacio = view.findViewById(R.id.layoutVacio);
        tvTotalQuizzes = view.findViewById(R.id.tvTotalQuizzes);
        tvPromedio = view.findViewById(R.id.tvPromedio);
        tvMejorPuntaje = view.findViewById(R.id.tvMejorPuntaje);

        // Verificar autenticación
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }

        // Inicializar RecyclerView
        inicializarRecyclerView();

        // Cargar estadísticas
        cargarEstadisticas();
    }

    private void inicializarRecyclerView() {
        rvQuizzesScore.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ScoreAdapter(estadisticasList);
        rvQuizzesScore.setAdapter(adapter);
    }

    private void cargarEstadisticas() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Remover listener anterior si existe
        if (resultadosListener != null) {
            resultadosListener.remove();
        }

        Log.d("ScoreFragment", "🔄 Iniciando listener en tiempo real para resultados...");

        // Usar SnapshotListener para updates en tiempo real
        resultadosListener = db.collection("resultados")
                .whereEqualTo("userId", currentUser.getUid())
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e("ScoreFragment", "❌ Error en listener de resultados: " + e.getMessage());
                            mostrarVistaVacia();
                            return;
                        }

                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            Log.d("ScoreFragment", "📊 Datos recibidos: " + queryDocumentSnapshots.size() + " resultados totales");
                            procesarResultados(queryDocumentSnapshots.getDocuments());
                        } else {
                            Log.d("ScoreFragment", "📊 No hay resultados aún");
                            mostrarVistaVacia();
                        }
                    }
                });
    }

    private void procesarResultados(List<DocumentSnapshot> documentos) {
        Log.d("ScoreFragment", "🔄 Procesando " + documentos.size() + " documentos de resultados");

        // Agrupar resultados por quizId
        Map<String, List<Resultado>> resultadosPorQuiz = new HashMap<>();

        for (DocumentSnapshot snapshot : documentos) {
            Resultado resultado = snapshot.toObject(Resultado.class);
            if (resultado != null) {
                String quizId = resultado.getQuizId();
                if (!resultadosPorQuiz.containsKey(quizId)) {
                    resultadosPorQuiz.put(quizId, new ArrayList<>());
                }
                resultadosPorQuiz.get(quizId).add(resultado);

                Log.d("ScoreFragment", "📝 Resultado - Quiz: " + resultado.getQuizTitulo() +
                        ", Puntuación: " + resultado.getPuntuacion() + "%, " +
                        "Fecha: " + resultado.getFecha());
            }
        }

        // Procesar cada grupo para crear EstadisticaQuiz
        estadisticasList.clear();

        for (Map.Entry<String, List<Resultado>> entry : resultadosPorQuiz.entrySet()) {
            String quizId = entry.getKey();
            List<Resultado> resultados = entry.getValue();

            // Ordenar por fecha descendente para obtener el último
            resultados.sort((r1, r2) -> r2.getFecha().compareTo(r1.getFecha()));

            int intentos = resultados.size();
            int mejorResultado = 0;
            int ultimoPuntaje = resultados.get(0).getPuntuacion();

            for (Resultado r : resultados) {
                if (r.getPuntuacion() > mejorResultado) {
                    mejorResultado = r.getPuntuacion();
                }
            }

            // Usar el título del primer resultado (todos deberían tener el mismo)
            String titulo = resultados.get(0).getQuizTitulo();

            EstadisticaQuiz estadistica = new EstadisticaQuiz(
                    quizId,
                    titulo,
                    intentos,
                    mejorResultado,
                    ultimoPuntaje,
                    resultados.get(0).getFecha()
            );
            estadisticasList.add(estadistica);

            Log.d("ScoreFragment", "📊 Estadística - Quiz: " + titulo +
                    ", Intentos: " + intentos +
                    ", Mejor: " + mejorResultado + "%" +
                    ", Último: " + ultimoPuntaje + "%");
        }

        adapter.notifyDataSetChanged();
        calcularMetricasGenerales();
        actualizarVisibilidad();

        Log.d("ScoreFragment", "✅ Estadísticas actualizadas: " + estadisticasList.size() + " quizzes");
    }

    private void calcularMetricasGenerales() {
        int totalQuizzes = estadisticasList.size();
        int sumaMejores = 0;
        int mejorPuntajeGlobal = 0;

        for (EstadisticaQuiz estadistica : estadisticasList) {
            sumaMejores += estadistica.getMejorResultado();
            if (estadistica.getMejorResultado() > mejorPuntajeGlobal) {
                mejorPuntajeGlobal = estadistica.getMejorResultado();
            }
        }

        int promedio = totalQuizzes > 0 ? sumaMejores / totalQuizzes : 0;

        tvTotalQuizzes.setText(String.valueOf(totalQuizzes));
        tvPromedio.setText(promedio + "%");
        tvMejorPuntaje.setText(mejorPuntajeGlobal + "%");

        Log.d("ScoreFragment", "📈 Métricas - Total: " + totalQuizzes +
                ", Promedio: " + promedio + "%, " +
                "Mejor: " + mejorPuntajeGlobal + "%");
    }

    private void actualizarVisibilidad() {
        if (estadisticasList.isEmpty()) {
            layoutVacio.setVisibility(View.VISIBLE);
            rvQuizzesScore.setVisibility(View.GONE);
        } else {
            layoutVacio.setVisibility(View.GONE);
            rvQuizzesScore.setVisibility(View.VISIBLE);
        }
    }

    private void mostrarVistaVacia() {
        layoutVacio.setVisibility(View.VISIBLE);
        rvQuizzesScore.setVisibility(View.GONE);
        tvTotalQuizzes.setText("0");
        tvPromedio.setText("0%");
        tvMejorPuntaje.setText("0%");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (resultadosListener != null) {
            resultadosListener.remove();
            Log.d("ScoreFragment", "🔴 Listener de resultados removido");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("ScoreFragment", "🔄 Fragment reanudado");
        if (resultadosListener == null) {
            cargarEstadisticas();
        }
    }
}