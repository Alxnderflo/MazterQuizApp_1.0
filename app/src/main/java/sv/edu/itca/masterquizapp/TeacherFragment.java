package sv.edu.itca.masterquizapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TeacherFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TeacherFragment extends Fragment {
    private RecyclerView rvProfesores, rvQuizzesProfesores;
    private ProfesoresAdapter adapterProfesores;
    private QuizzesAdapter adapterQuizzesProfesores;
    private List<Usuario> listaProfesores;
    private List<Quiz> listaQuizzesProfesores;
    private List<String> listaQuizzesIdsProfesores;
    private FirebaseFirestore db;
    private LinearLayout layoutVacio;
    private FloatingActionButton fabAgregarProfesor;
    private TextView tvQuizzesProfesores;

    // Colores predefinidos
    private int[] coloresProfesores;

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
        coloresProfesores = new int[] {
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
                            configurarParaEstudiante();
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

    private void configurarParaEstudiante() {
        // Configurar RecyclerViews
        LinearLayoutManager layoutManagerHorizontal = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvProfesores.setLayoutManager(layoutManagerHorizontal);

        LinearLayoutManager layoutManagerVertical = new LinearLayoutManager(getContext());
        rvQuizzesProfesores.setLayoutManager(layoutManagerVertical);

        // Configurar FAB
        fabAgregarProfesor.setOnClickListener(v -> mostrarDialogoAgregarProfesor());

        // Configurar adapters
        adapterProfesores = new ProfesoresAdapter(listaProfesores, coloresProfesores, getContext());
        rvProfesores.setAdapter(adapterProfesores);

        adapterQuizzesProfesores = new QuizzesAdapter(listaQuizzesProfesores, getContext(),
                (quiz, quizId) -> {
                    // Para FASE 4 - resolución de quizzes
                    Toast.makeText(getContext(), "Funcionalidad de resolución próximamente", Toast.LENGTH_SHORT).show();
                }, null);

        rvQuizzesProfesores.setAdapter(adapterQuizzesProfesores);

        // Cargar datos
        cargarProfesoresAgregados();
    }

    private void mostrarMensajeProfesor() {
        layoutVacio.setVisibility(View.VISIBLE);
        TextView titulo = layoutVacio.findViewById(R.id.textViewTitulo);
        TextView subtitulo = layoutVacio.findViewById(R.id.textViewSubtitulo);

        if (titulo != null) titulo.setText("Vista de Profesor");
        if (subtitulo != null) subtitulo.setText("Los estudiantes te verán aquí cuando agreguen tu código");
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
                    cargarProfesoresAgregados();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al agregar profesor: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarProfesoresAgregados() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        db.collection("usuarios").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> profesoresAgregados = (List<String>) documentSnapshot.get("profesoresAgregados");
                        if (profesoresAgregados != null && !profesoresAgregados.isEmpty()) {
                            cargarDatosProfesores(profesoresAgregados);
                            cargarQuizzesProfesores(profesoresAgregados);
                        } else {
                            mostrarVistaVacia();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TeacherFragment", "Error al cargar profesores agregados: " + e.getMessage());
                });
    }

    private void cargarDatosProfesores(List<String> profesoresIds) {
        listaProfesores.clear();

        for (String profesorId : profesoresIds) {
            db.collection("usuarios").document(profesorId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Usuario profesor = documentSnapshot.toObject(Usuario.class);
                            if (profesor != null) {
                                profesor.setId(documentSnapshot.getId()); // Asignar ID del documento
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

        db.collection("quizzes")
                .whereIn("userId", profesoresIds)
                .whereEqualTo("esPublico", true)
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaQuizzesProfesores.clear();
                    listaQuizzesIdsProfesores.clear();

                    for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                        Quiz quiz = snapshot.toObject(Quiz.class);
                        if (quiz != null) {
                            listaQuizzesProfesores.add(quiz);
                            listaQuizzesIdsProfesores.add(snapshot.getId());
                        }
                    }

                    adapterQuizzesProfesores.actualizarIds(listaQuizzesIdsProfesores);
                    adapterQuizzesProfesores.notifyDataSetChanged();
                    actualizarVisibilidadQuizzes();
                })
                .addOnFailureListener(e -> {
                    Log.e("TeacherFragment", "Error al cargar quizzes de profesores: " + e.getMessage());
                });
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
    public void onResume() {
        super.onResume();
        // Recargar datos cuando el fragment se vuelve visible
        cargarProfesoresAgregados();
    }
}