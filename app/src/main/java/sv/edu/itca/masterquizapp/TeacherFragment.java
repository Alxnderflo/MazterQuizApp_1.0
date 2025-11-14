package sv.edu.itca.masterquizapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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

    // NUEVO: Control de recarga
    private boolean necesitaRecarga = true;
    private BroadcastReceiver quizEliminadoReceiver;

    // Interface para callback de eliminación
    interface OnProfesorEliminadoListener {
        void onExito();

        void onError(String error);
    }

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

        // Verificar autenticación
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }

        // Configurar BroadcastReceiver para quizzes eliminados
        configurarBroadcastReceiver();

        // Inicializar directamente para estudiante
        inicializarAdaptersParaEstudiante();

        // Recargar datos con pequeño delay para asegurar que la UI está lista
        new Handler().postDelayed(() -> {
            if (isAdded() && getActivity() != null) {
                recargarDatosCompletos();
            }
        }, 100);
    }

    // NUEVO: Configurar BroadcastReceiver para detectar quizzes eliminados
    private void configurarBroadcastReceiver() {
        quizEliminadoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("QUIZ_ELIMINADO".equals(intent.getAction())) {
                    String quizIdEliminado = intent.getStringExtra("quiz_id");
                    Log.d("TeacherFragment", "Recibido broadcast - quiz eliminado: " + quizIdEliminado);

                    // Forzar recarga inmediata
                    if (isAdded() && getActivity() != null) {
                        recargarDatosCompletos();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("QUIZ_ELIMINADO");
        ContextCompat.registerReceiver(requireContext(), quizEliminadoReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    // NUEVO: Método para recargar todos los datos
    private void recargarDatosCompletos() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Log.d("TeacherFragment", "Recargando datos completos...");

        // Limpiar listeners anteriores
        if (profesoresListener != null) {
            profesoresListener.remove();
            profesoresListener = null;
        }
        if (quizzesListener != null) {
            quizzesListener.remove();
            quizzesListener = null;
        }

        // Limpiar listas
        listaProfesores.clear();
        listaQuizzesProfesores.clear();
        listaQuizzesIdsProfesores.clear();

        // Notificar adapters
        if (adapterProfesores != null) {
            adapterProfesores.notifyDataSetChanged();
        }
        if (adapterQuizzesProfesores != null) {
            adapterQuizzesProfesores.notifyDataSetChanged();
        }

        // Recargar datos
        cargarProfesoresAgregados();
        necesitaRecarga = false;
    }

    // MÉTODO: Inicializar todos los adapters para estudiante
    private void inicializarAdaptersParaEstudiante() {
        // Configurar RecyclerViews
        LinearLayoutManager layoutManagerHorizontal = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvProfesores.setLayoutManager(layoutManagerHorizontal);

        LinearLayoutManager layoutManagerVertical = new LinearLayoutManager(getContext());
        rvQuizzesProfesores.setLayoutManager(layoutManagerVertical);

        // Configurar FAB
        fabAgregarProfesor.setOnClickListener(v -> mostrarDialogoAgregarProfesor());

        // INICIALIZAR ADAPTERS
        adapterProfesores = new ProfesoresAdapter(listaProfesores, coloresProfesores, getContext(), (profesor, profesorId, position) -> mostrarDialogoConfirmacionEliminacion(profesor, profesorId, position));
        rvProfesores.setAdapter(adapterProfesores);
        inicializarAdapterQuizzes();
    }

    // MÉTODO: Mostrar diálogo de confirmación para eliminar profesor
    private void mostrarDialogoConfirmacionEliminacion(Usuario profesor, String profesorId, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.dialog_eliminar_profesor_titulo);

        // Usar recurso con placeholder para el nombre del profesor
        String mensaje = getString(R.string.dialog_eliminar_profesor_mensaje, profesor.getNombre());
        builder.setMessage(mensaje);

        builder.setPositiveButton(R.string.dialog_btn_eliminar, (dialog, which) -> {
            eliminarProfesor(profesorId, new OnProfesorEliminadoListener() {
                @Override
                public void onExito() {
                    Toast.makeText(getContext(), R.string.toast_profesor_eliminado_exito, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    String mensajeError = getString(R.string.toast_error_eliminar_profesor, error);
                    Toast.makeText(getContext(), mensajeError, Toast.LENGTH_SHORT).show();
                }
            });
        });

        builder.setNegativeButton(R.string.dialog_btn_cancelar, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    // MÉTODO: Eliminar profesor de la lista del usuario actual
    private void eliminarProfesor(String profesorId, OnProfesorEliminadoListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            listener.onError(getString(R.string.error_usuario_no_autenticado));
            return;
        }

        db.collection("usuarios").document(currentUser.getUid()).update("profesoresAgregados", FieldValue.arrayRemove(profesorId)).addOnSuccessListener(aVoid -> {
            Log.d("TeacherFragment", "Profesor eliminado de la lista: " + profesorId);
            listener.onExito();
        }).addOnFailureListener(e -> {
            Log.e("TeacherFragment", "Error al eliminar profesor: " + e.getMessage());
            listener.onError(e.getMessage());
        });
    }

    private void mostrarDialogoAgregarProfesor() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.dialog_agregar_profesor_titulo);
        builder.setMessage(R.string.dialog_agregar_profesor_mensaje);

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(6)});
        input.setHint(R.string.hint_codigo_profesor);
        builder.setView(input);

        builder.setPositiveButton(R.string.dialog_btn_agregar, (dialog, which) -> {
            String codigo = input.getText().toString().trim();
            if (codigo.length() == 6) {
                buscarProfesorPorCodigo(codigo);
            } else {
                Toast.makeText(getContext(), R.string.toast_codigo_longitud_invalida, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.dialog_btn_cancelar, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void buscarProfesorPorCodigo(String codigo) {
        db.collection("usuarios").whereEqualTo("codigoProfesor", codigo).whereEqualTo("rol", "profesor").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty() && queryDocumentSnapshots.size() > 0) {
                DocumentSnapshot snapshot = queryDocumentSnapshots.getDocuments().get(0);
                String profesorId = snapshot.getId();
                String nombreProfesor = snapshot.getString("nombre");

                if (!estaProfesorAgregado(profesorId)) {
                    vincularProfesor(profesorId, nombreProfesor);
                } else {
                    Toast.makeText(getContext(), R.string.toast_profesor_ya_agregado, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), R.string.toast_codigo_no_valido, Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            String mensajeError = getString(R.string.toast_error_buscar_profesor, e.getMessage());
            Toast.makeText(getContext(), mensajeError, Toast.LENGTH_SHORT).show();
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

        db.collection("usuarios").document(currentUser.getUid()).update("profesoresAgregados", FieldValue.arrayUnion(profesorId)).addOnSuccessListener(aVoid -> {
            String mensajeExito = getString(R.string.toast_profesor_agregado_exito, nombreProfesor);
            Toast.makeText(getContext(), mensajeExito, Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            String mensajeError = getString(R.string.toast_error_agregar_profesor, e.getMessage());
            Toast.makeText(getContext(), mensajeError, Toast.LENGTH_SHORT).show();
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
        profesoresListener = db.collection("usuarios").document(currentUser.getUid()).addSnapshotListener((documentSnapshot, e) -> {
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

            db.collection("usuarios").document(profesorId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Usuario profesor = documentSnapshot.toObject(Usuario.class);
                    if (profesor != null) {
                        profesor.setId(documentSnapshot.getId());
                        listaProfesores.add(profesor);
                        adapterProfesores.notifyDataSetChanged();
                        actualizarVisibilidad();
                    }
                }
            }).addOnFailureListener(e -> {
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

        // Verificar que el adapter esté inicializado antes de usarlo
        if (adapterQuizzesProfesores == null) {
            Log.e("TeacherFragment", "Adapter de quizzes es null - inicializando...");
            inicializarAdapterQuizzes();
        }

        // Usar SnapshotListener para updates en tiempo real
        quizzesListener = db.collection("quizzes").whereIn("userId", profesoresIds).whereEqualTo("esPublico", true).orderBy("fechaCreacion", Query.Direction.DESCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
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

    // MÉTODO: Inicializar adapter de quizzes
    private void inicializarAdapterQuizzes() {
        if (adapterQuizzesProfesores == null) {
            adapterQuizzesProfesores = new QuizzesProfesorAdapter(listaQuizzesProfesores, getContext(), new QuizzesProfesorAdapter.OnQuizClickListener() {
                @Override
                public void onQuizClick(Quiz quiz, String quizId) {
                    abrirResolverQuiz(quiz, quizId);
                }
            }, coloresProfesores, mapaColoresProfesores);
            rvQuizzesProfesores.setAdapter(adapterQuizzesProfesores);
        }
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

    // NUEVO: Forzar recarga cuando el fragment se hace visible
    @Override
    public void onResume() {
        super.onResume();

        // Forzar recarga cada vez que el fragment se muestra
        if (necesitaRecarga) {
            new Handler().postDelayed(() -> {
                if (isAdded() && getActivity() != null) {
                    recargarDatosCompletos();
                }
            }, 100);
        }
        necesitaRecarga = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar listeners cuando el fragment se destruye
        if (quizzesListener != null) {
            quizzesListener.remove();
            quizzesListener = null;
        }
        if (profesoresListener != null) {
            profesoresListener.remove();
            profesoresListener = null;
        }

        // Limpiar BroadcastReceiver
        if (quizEliminadoReceiver != null) {
            requireContext().unregisterReceiver(quizEliminadoReceiver);
            quizEliminadoReceiver = null;
        }
    }
}