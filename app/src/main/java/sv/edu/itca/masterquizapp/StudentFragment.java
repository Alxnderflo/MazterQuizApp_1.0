package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentFragment extends Fragment {

    private RecyclerView rvEstudiantes;
    private EstudianteAdapter adapterEstudiantes;
    private List<Usuario> listaEstudiantes;
    private FirebaseFirestore db;
    private LinearLayout layoutVacio;
    private TextView tvNumEstudiantes, tvQuizzesCompartidos;

    // Listeners para updates en tiempo real
    private ListenerRegistration estudiantesListener;
    private ListenerRegistration quizzesListener;

    // 🔥 NUEVO: Mapa para tracking de estudiantes y sus listeners
    private Map<String, ListenerRegistration> estudiantesListenersMap;

    public StudentFragment() {
        // Required empty public constructor
    }

    public static StudentFragment newInstance() {
        return new StudentFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        listaEstudiantes = new ArrayList<>();
        estudiantesListenersMap = new HashMap<>(); // 🔥 NUEVO
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        rvEstudiantes = view.findViewById(R.id.rvEstudiantes);
        layoutVacio = view.findViewById(R.id.layoutVacio);
        tvNumEstudiantes = view.findViewById(R.id.tvNumEstudiantes);
        tvQuizzesCompartidos = view.findViewById(R.id.tvQuizzesCompartidos);

        // Verificar autenticación
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }

        // Inicializar RecyclerView y adapter
        inicializarRecyclerView();

        // Cargar datos iniciales
        cargarEstudiantesAgregados();
        calcularMetricas();
    }

    private void inicializarRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvEstudiantes.setLayoutManager(layoutManager);

        adapterEstudiantes = new EstudianteAdapter(listaEstudiantes, getContext(),
                (estudiante, estudianteId, position) -> mostrarDialogoConfirmacionExpulsion(estudiante, estudianteId, position));
        rvEstudiantes.setAdapter(adapterEstudiantes);
    }

    private void mostrarDialogoConfirmacionExpulsion(Usuario estudiante, String estudianteId, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Expulsar Estudiante");
        builder.setMessage("¿Estás seguro de que quieres expulsar a " + estudiante.getNombre() + " de tu lista de estudiantes?");

        builder.setPositiveButton("Expulsar", (dialog, which) -> {
            expulsarEstudiante(estudianteId, estudiante.getNombre(), position);
        });

        builder.setNegativeButton("Cancelar", null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    // 🔥 CAMBIO: Agregar parámetro position para remover localmente inmediatamente
    private void expulsarEstudiante(String estudianteId, String nombreEstudiante, int position) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // 🔥 NUEVO: Remover inmediatamente de la lista local para feedback instantáneo
        if (position >= 0 && position < listaEstudiantes.size()) {
            listaEstudiantes.remove(position);
            adapterEstudiantes.notifyItemRemoved(position);
            actualizarVisibilidad();
            calcularMetricas();
        }

        // Remover el profesor de la lista de profesoresAgregados del estudiante
        db.collection("usuarios").document(estudianteId)
                .update("profesoresAgregados", FieldValue.arrayRemove(currentUser.getUid()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Estudiante " + nombreEstudiante + " expulsado exitosamente", Toast.LENGTH_SHORT).show();
                    Log.d("StudentFragment", "Estudiante expulsado: " + estudianteId);

                    // 🔥 NUEVO: Remover listener específico de este estudiante
                    if (estudiantesListenersMap.containsKey(estudianteId)) {
                        estudiantesListenersMap.get(estudianteId).remove();
                        estudiantesListenersMap.remove(estudianteId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al expulsar estudiante: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("StudentFragment", "Error al expulsar estudiante: " + e.getMessage());

                    // 🔥 NUEVO: Si falla, revertir la eliminación local
                    cargarEstudiantesAgregados(); // Recargar para sincronizar
                });
    }

    private void cargarEstudiantesAgregados() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Remover listener anterior si existe
        if (estudiantesListener != null) {
            estudiantesListener.remove();
        }

        // 🔥 CAMBIO: Usar consulta más específica y eficiente
        estudiantesListener = db.collection("usuarios")
                .whereArrayContains("profesoresAgregados", currentUser.getUid())
                .whereEqualTo("rol", "estudiante")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e("StudentFragment", "Error en listener de estudiantes: " + e.getMessage());
                            return;
                        }

                        if (queryDocumentSnapshots != null) {
                            listaEstudiantes.clear();

                            // 🔥 NUEVO: Limpiar listeners anteriores
                            for (ListenerRegistration listener : estudiantesListenersMap.values()) {
                                listener.remove();
                            }
                            estudiantesListenersMap.clear();

                            for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                                Usuario estudiante = snapshot.toObject(Usuario.class);
                                if (estudiante != null) {
                                    estudiante.setId(snapshot.getId());
                                    listaEstudiantes.add(estudiante);

                                    // 🔥 NUEVO: Agregar listener individual para cada estudiante
                                    agregarListenerEstudianteIndividual(snapshot.getId());
                                }
                            }

                            adapterEstudiantes.notifyDataSetChanged();
                            actualizarVisibilidad();
                            calcularMetricas();

                            Log.d("StudentFragment", "Estudiantes cargados: " + listaEstudiantes.size());
                        }
                    }
                });
    }

    // 🔥 NUEVO MÉTODO: Listener individual para cambios en cada estudiante
    private void agregarListenerEstudianteIndividual(String estudianteId) {
        ListenerRegistration listener = db.collection("usuarios").document(estudianteId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("StudentFragment", "Error en listener individual de estudiante: " + e.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Verificar si el estudiante aún tiene al profesor en su lista
                        List<String> profesoresAgregados = (List<String>) documentSnapshot.get("profesoresAgregados");
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                        if (profesoresAgregados == null || !profesoresAgregados.contains(currentUser.getUid())) {
                            // El estudiante removió al profesor, eliminar de la lista local
                            for (int i = 0; i < listaEstudiantes.size(); i++) {
                                if (listaEstudiantes.get(i).getId().equals(estudianteId)) {
                                    listaEstudiantes.remove(i);
                                    adapterEstudiantes.notifyItemRemoved(i);
                                    actualizarVisibilidad();
                                    calcularMetricas();
                                    break;
                                }
                            }
                        }
                    }
                });

        estudiantesListenersMap.put(estudianteId, listener);
    }

    private void calcularMetricas() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Calcular número de estudiantes (ya tenemos la lista)
        int numEstudiantes = listaEstudiantes.size();
        tvNumEstudiantes.setText(String.valueOf(numEstudiantes));

        // Remover listener anterior si existe
        if (quizzesListener != null) {
            quizzesListener.remove();
        }

        // 🔥 CAMBIO: Listener más robusto para quizzes
        quizzesListener = db.collection("quizzes")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e("StudentFragment", "Error en listener de quizzes: " + e.getMessage());
                            tvQuizzesCompartidos.setText("0");
                            return;
                        }

                        if (queryDocumentSnapshots != null) {
                            int numQuizzesCompartidos = 0;
                            for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                                Boolean esPublico = snapshot.getBoolean("esPublico");
                                if (esPublico != null && esPublico) {
                                    numQuizzesCompartidos++;
                                }
                            }
                            tvQuizzesCompartidos.setText(String.valueOf(numQuizzesCompartidos));
                            Log.d("StudentFragment", "Quizzes compartidos actualizados: " + numQuizzesCompartidos);
                        }
                    }
                });
    }

    private void actualizarVisibilidad() {
        if (listaEstudiantes.isEmpty()) {
            layoutVacio.setVisibility(View.VISIBLE);
            rvEstudiantes.setVisibility(View.GONE);
        } else {
            layoutVacio.setVisibility(View.GONE);
            rvEstudiantes.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar todos los listeners
        if (estudiantesListener != null) {
            estudiantesListener.remove();
            estudiantesListener = null;
        }
        if (quizzesListener != null) {
            quizzesListener.remove();
            quizzesListener = null;
        }

        // 🔥 NUEVO: Limpiar listeners individuales de estudiantes
        for (ListenerRegistration listener : estudiantesListenersMap.values()) {
            listener.remove();
        }
        estudiantesListenersMap.clear();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Opcional: Puedes remover listeners si quieres optimizar
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar datos si es necesario
        if (estudiantesListener == null) {
            cargarEstudiantesAgregados();
        }
        if (quizzesListener == null) {
            calcularMetricas();
        }
    }
}