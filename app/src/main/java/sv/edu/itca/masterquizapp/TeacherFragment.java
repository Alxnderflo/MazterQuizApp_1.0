package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherFragment extends Fragment {
    private RecyclerView rvProfesores, rvQuizzesProfesores;
    private ProfesoresAdapter adapterProfesores;
    private QuizzesProfesorAdapter adapterQuizzesProfesores;
    private List<Usuario> listaProfesores;
    private List<Quiz> listaQuizzesProfesores;
    private List<String> listaQuizzesIdsProfesores;
    private FirebaseFirestore db;
    private LinearLayout layoutVacio;
    private FloatingActionButton fabAgregarProfesor;
    private TextView tvQuizzesProfesores;

    // Colores predefinidos
    private int[] coloresProfesores;
    private Map<String, Integer> mapaColoresProfesores;

    // Listeners para updates en tiempo real
    private ListenerRegistration quizzesListener;
    private ListenerRegistration profesoresListener;

    public TeacherFragment() {
        // Required empty public constructor
    }

    public static TeacherFragment newInstance() {
        return new TeacherFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        listaProfesores = new ArrayList<>();
        listaQuizzesProfesores = new ArrayList<>();
        listaQuizzesIdsProfesores = new ArrayList<>();
        mapaColoresProfesores = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        rvProfesores = view.findViewById(R.id.rvProfesores);
        rvQuizzesProfesores = view.findViewById(R.id.rvQuizzesProfesores);
        layoutVacio = view.findViewById(R.id.layoutVacio);
        fabAgregarProfesor = view.findViewById(R.id.fabAgregarProfesor);
        tvQuizzesProfesores = view.findViewById(R.id.tvQuizzesProfesores);

        // Inicializar colores
        coloresProfesores = new int[]{
                ContextCompat.getColor(requireContext(), R.color.color_profesor_1),
                ContextCompat.getColor(requireContext(), R.color.color_profesor_2),
                ContextCompat.getColor(requireContext(), R.color.color_profesor_3),
                ContextCompat.getColor(requireContext(), R.color.color_profesor_4),
                ContextCompat.getColor(requireContext(), R.color.color_profesor_5),
                ContextCompat.getColor(requireContext(), R.color.color_profesor_6)
        };

        // Verificar autenticación y rol
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }

        // Verificar si es estudiante
        verificarRolYConfigurar(currentUser.getUid());
    }

    private void verificarRolYConfigurar(String userId) {
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String rol = documentSnapshot.getString("rol");
                        if ("estudiante".equals(rol)) {
                            // 🔥 CAMBIO: Inicializar adapters PRIMERO
                            inicializarAdaptersParaEstudiante();
                            cargarProfesoresAgregados();
                        } else {
                            // Si es profesor, ocultar FAB y mostrar mensaje
                            fabAgregarProfesor.setVisibility(View.GONE);
                            mostrarMensajeProfesor();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TeacherFragment", "Error al verificar rol: " + e.getMessage());
                });
    }

    // 🔥 NUEVO MÉTODO: Inicializar todos los adapters para estudiante
    private void inicializarAdaptersParaEstudiante() {
        // Configurar RecyclerViews
        LinearLayoutManager layoutManagerHorizontal = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvProfesores.setLayoutManager(layoutManagerHorizontal);

        LinearLayoutManager layoutManagerVertical = new LinearLayoutManager(getContext());
        rvQuizzesProfesores.setLayoutManager(layoutManagerVertical);

        // Configurar FAB
        fabAgregarProfesor.setOnClickListener(v -> mostrarDialogoAgregarProfesor());

        // INICIALIZAR ADAPTERS
        adapterProfesores = new ProfesoresAdapter(listaProfesores, coloresProfesores, getContext());
        rvProfesores.setAdapter(adapterProfesores);

        inicializarAdapterQuizzes(); // 🔥 Asegurar que el adapter de quizzes esté listo
    }

    private void configurarParaEstudiante() {
        // Configurar RecyclerViews
        LinearLayoutManager layoutManagerHorizontal = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvProfesores.setLayoutManager(layoutManagerHorizontal);

        LinearLayoutManager layoutManagerVertical = new LinearLayoutManager(getContext());
        rvQuizzesProfesores.setLayoutManager(layoutManagerVertical);

        // Configurar FAB
        fabAgregarProfesor.setOnClickListener(v -> mostrarDialogoAgregarProfesor());

        // CONFIGURAR ADAPTERS
        adapterProfesores = new ProfesoresAdapter(listaProfesores, coloresProfesores, getContext());
        rvProfesores.setAdapter(adapterProfesores);

        adapterQuizzesProfesores = new QuizzesProfesorAdapter(
                listaQuizzesProfesores,
                getContext(),
                new QuizzesProfesorAdapter.OnQuizClickListener() {
                    @Override
                    public void onQuizClick(Quiz quiz, String quizId) {
                        // FASE 5: Abrir directamente ResolverQuizActivity para quizzes de profesores
                        abrirResolverQuiz(quiz, quizId);
                    }
                },
                coloresProfesores,
                mapaColoresProfesores
        );

        rvQuizzesProfesores.setAdapter(adapterQuizzesProfesores);
        // Cargar datos con listeners en tiempo real
        cargarProfesoresAgregados();
    }

    private void abrirResolverQuiz(Quiz quiz, String quizId) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), ResolverQuizActivity.class);
            intent.putExtra("quiz_id", quizId);
            intent.putExtra("quiz_titulo", quiz.getTitulo());
            intent.putExtra("total_preguntas", quiz.getNumPreguntas());
            startActivity(intent);
        }
    }

    private void mostrarMensajeProfesor() {
        layoutVacio.setVisibility(View.VISIBLE);
        TextView titulo = layoutVacio.findViewById(R.id.textViewTitulo);
        TextView subtitulo = layoutVacio.findViewById(R.id.textViewSubtitulo);

        if (titulo != null) titulo.setText("Vista de Profesor");
        if (subtitulo != null)
            subtitulo.setText("Los estudiantes te verán aquí cuando agreguen tu código");
    }

    private void mostrarDialogoAgregarProfesor() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Agregar Profesor");
        builder.setMessage("Ingresa el código de 6 caracteres del profesor:");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(6)});
        input.setHint("ABCD12");
        builder.setView(input);

        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String codigo = input.getText().toString().trim();
            if (codigo.length() == 6) {
                buscarProfesorPorCodigo(codigo);
            } else {
                Toast.makeText(getContext(), "El código debe tener exactamente 6 caracteres", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void buscarProfesorPorCodigo(String codigo) {
        db.collection("usuarios")
                .whereEqualTo("codigoProfesor", codigo)
                .whereEqualTo("rol", "profesor")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty() && queryDocumentSnapshots.size() > 0) {
                        DocumentSnapshot snapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String profesorId = snapshot.getId();
                        String nombreProfesor = snapshot.getString("nombre");

                        if (!estaProfesorAgregado(profesorId)) {
                            vincularProfesor(profesorId, nombreProfesor);
                        } else {
                            Toast.makeText(getContext(), "Ya tienes agregado a este profesor", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Código no válido o no pertenece a un profesor", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al buscar profesor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean estaProfesorAgregado(String profesorId) {
        for (Usuario profesor : listaProfesores) {
            if (profesor.getId() != null && profesor.getId().equals(profesorId)) {
                return true;
            }
        }
        return false;
    }

    private void vincularProfesor(String profesorId, String nombreProfesor) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        db.collection("usuarios").document(currentUser.getUid())
                .update("profesoresAgregados", FieldValue.arrayUnion(profesorId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profesor " + nombreProfesor + " agregado exitosamente", Toast.LENGTH_SHORT).show();
                    // No necesitamos recargar manualmente porque el listener se encargará
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al agregar profesor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarProfesoresAgregados() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Remover listener anterior si existe
        if (profesoresListener != null) {
            profesoresListener.remove();
        }

        // Usar SnapshotListener para updates en tiempo real
        profesoresListener = db.collection("usuarios").document(currentUser.getUid())
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("TeacherFragment", "Error en listener de profesores: " + e.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        List<String> profesoresAgregados = (List<String>) documentSnapshot.get("profesoresAgregados");
                        if (profesoresAgregados != null && !profesoresAgregados.isEmpty()) {
                            cargarDatosProfesores(profesoresAgregados);
                            cargarQuizzesProfesores(profesoresAgregados);
                        } else {
                            mostrarVistaVacia();
                        }
                    }
                });
    }

    private void cargarDatosProfesores(List<String> profesoresIds) {
        listaProfesores.clear();
        mapaColoresProfesores.clear();

        for (int i = 0; i < profesoresIds.size(); i++) {
            String profesorId = profesoresIds.get(i);

            // Asignar color al profesor
            int colorIndex = i % coloresProfesores.length;
            mapaColoresProfesores.put(profesorId, coloresProfesores[colorIndex]);

            db.collection("usuarios").document(profesorId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Usuario profesor = documentSnapshot.toObject(Usuario.class);
                            if (profesor != null) {
                                profesor.setId(documentSnapshot.getId());
                                listaProfesores.add(profesor);
                                adapterProfesores.notifyDataSetChanged();
                                actualizarVisibilidad();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TeacherFragment", "Error al cargar datos del profesor: " + e.getMessage());
                    });
        }
    }

    private void cargarQuizzesProfesores(List<String> profesoresIds) {
        // Limitar a 10 profesores por límite de Firestore
        if (profesoresIds.size() > 10) {
            profesoresIds = profesoresIds.subList(0, 10);
        }

        // Remover listener anterior si existe
        if (quizzesListener != null) {
            quizzesListener.remove();
        }

        // 🔥 NUEVO: Verificar que el adapter esté inicializado antes de usarlo
        if (adapterQuizzesProfesores == null) {
            Log.e("TeacherFragment", "Adapter de quizzes es null - inicializando...");
            inicializarAdapterQuizzes();
        }

        // Usar SnapshotListener para updates en tiempo real
        quizzesListener = db.collection("quizzes")
                .whereIn("userId", profesoresIds)
                .whereEqualTo("esPublico", true)
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e("TeacherFragment", "Error en listener de quizzes: " + e.getMessage());
                            return;
                        }

                        if (queryDocumentSnapshots != null) {
                            listaQuizzesProfesores.clear();
                            listaQuizzesIdsProfesores.clear();

                            for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                                Quiz quiz = snapshot.toObject(Quiz.class);
                                if (quiz != null) {
                                    listaQuizzesProfesores.add(quiz);
                                    listaQuizzesIdsProfesores.add(snapshot.getId());
                                }
                            }

                            // 🔥 NUEVO: Verificación adicional de null
                            if (adapterQuizzesProfesores != null) {
                                adapterQuizzesProfesores.actualizarIds(listaQuizzesIdsProfesores);
                                adapterQuizzesProfesores.notifyDataSetChanged();
                                actualizarVisibilidadQuizzes();
                            } else {
                                Log.e("TeacherFragment", "Adapter sigue siendo null - no se puede actualizar");
                            }
                        }
                    }
                });
    }

    // 🔥 NUEVO MÉTODO: Inicializar adapter de quizzes
    private void inicializarAdapterQuizzes() {
        if (adapterQuizzesProfesores == null) {
            adapterQuizzesProfesores = new QuizzesProfesorAdapter(
                    listaQuizzesProfesores,
                    getContext(),
                    new QuizzesProfesorAdapter.OnQuizClickListener() {
                        @Override
                        public void onQuizClick(Quiz quiz, String quizId) {
                            abrirResolverQuiz(quiz, quizId);
                        }
                    },
                    coloresProfesores,
                    mapaColoresProfesores
            );
            rvQuizzesProfesores.setAdapter(adapterQuizzesProfesores);
        }
    }

    private void mostrarVistaVacia() {
        layoutVacio.setVisibility(View.VISIBLE);
        rvProfesores.setVisibility(View.GONE);
        tvQuizzesProfesores.setVisibility(View.GONE);
        rvQuizzesProfesores.setVisibility(View.GONE);
    }

    private void actualizarVisibilidad() {
        if (listaProfesores.isEmpty()) {
            mostrarVistaVacia();
        } else {
            layoutVacio.setVisibility(View.GONE);
            rvProfesores.setVisibility(View.VISIBLE);
            tvQuizzesProfesores.setVisibility(View.VISIBLE);
        }
    }

    private void actualizarVisibilidadQuizzes() {
        if (listaQuizzesProfesores.isEmpty()) {
            rvQuizzesProfesores.setVisibility(View.GONE);
        } else {
            rvQuizzesProfesores.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar listeners cuando el fragment se destruye para evitar memory leaks
        if (quizzesListener != null) {
            quizzesListener.remove();
            quizzesListener = null;
        }
        if (profesoresListener != null) {
            profesoresListener.remove();
            profesoresListener = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Opcional: Limpiar listeners cuando el fragment se pausa
        if (quizzesListener != null) {
            quizzesListener.remove();
        }
        if (profesoresListener != null) {
            profesoresListener.remove();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar datos cuando el fragment se vuelve visible
        // PERO ahora usamos listeners en tiempo real, así que solo necesitamos
        // recargar si los listeners no están activos
        if (quizzesListener == null || profesoresListener == null) {
            cargarProfesoresAgregados();
        }
    }
}