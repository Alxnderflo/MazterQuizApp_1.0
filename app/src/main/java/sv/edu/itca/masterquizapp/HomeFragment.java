package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView rvQuizzes;
    private QuizzesAdapter adapterQuizzes;
    private List<Quiz> listaQuizzes;
    private List<String> listaQuizzesIds;
    private FirebaseFirestore bd;
    private LinearLayout layoutVacio;

    public HomeFragment() {
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bd = FirebaseFirestore.getInstance();
        listaQuizzes = new ArrayList<>();
        listaQuizzesIds = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d("TestLog", "HomeFragment cargado correctamente");

        super.onViewCreated(view, savedInstanceState);
        rvQuizzes = view.findViewById(R.id.rvQuizzes);
        layoutVacio = view.findViewById(R.id.layoutVacio);

        rvQuizzes.setLayoutManager(new LinearLayoutManager(getContext()));

        // Verificar autenticación
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }

        // Configurar FAB
        FloatingActionButton fab = view.findViewById(R.id.fabCrearQuiz);
        fab.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CrearQuizActivity.class));
        });

        // Configurar adapter
        adapterQuizzes = new QuizzesAdapter(listaQuizzes, getContext(), new QuizzesAdapter.OnQuizClickListener() {
            @Override
            public void onQuizClick(Quiz quiz, String quizId) {
                if (quizId != null && !quizId.isEmpty()) {
                    Intent intent = new Intent(getActivity(), PreguntasActivity.class);
                    intent.putExtra("quiz_id", quizId);
                    startActivity(intent);
                } else {
                    Log.e("HomeFragment", "quizId es null o vacío");
                }
            }
        }, new QuizzesAdapter.OnQuizMenuClickListener() {
            @Override
            public void onEditQuiz(Quiz quiz, String quizId) {
                //iniciar el activity en modo edición
                Intent ventana = new Intent(getActivity(), CrearQuizActivity.class);
                ventana.putExtra("MODO_EDICION", true);
                ventana.putExtra("quiz_id", quizId);
                ventana.putExtra("quiz_titulo", quiz.getTitulo());
                ventana.putExtra("quiz_descripcion", quiz.getDescripcion());
                ventana.putExtra("quiz_imagenUrl", quiz.getImagenUrl());
                startActivity(ventana);
            }

            @Override
            public void onDeleteQuiz(Quiz quiz, String quizId) {
                mostrarDialogEliminar(quiz, quizId);
            }
        });
        rvQuizzes.setAdapter(adapterQuizzes);

        obtenerQuizzes();
    }

    private void mostrarDialogEliminar(Quiz quiz, String quizId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.dialog_eliminar_quiz_titulo);

        // Usar recurso con placeholder para el título del quiz
        String mensaje = getString(R.string.dialog_eliminar_quiz_mensaje, quiz.getTitulo());
        builder.setMessage(mensaje);

        builder.setPositiveButton(R.string.dialog_btn_eliminar, (dialog, which) -> {
            eliminarQuiz(quizId);
        });
        builder.setNegativeButton(R.string.dialog_btn_cancelar, (dialog, which) -> {
            dialog.dismiss();
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    private void eliminarQuiz(String quizId) {
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_error_id_quiz_invalido, Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progreso
        Toast.makeText(getContext(), R.string.toast_eliminando_quiz, Toast.LENGTH_SHORT).show();

        // PRIMERO eliminar las preguntas, LUEGO los resultados, LUEGO el quiz
        eliminarPreguntasDeQuiz(quizId, new OnPreguntasEliminadasListener() {
            @Override
            public void onExito() {
                // Ahora eliminar los resultados asociados al quiz
                eliminarResultadosDeQuiz(quizId, new OnResultadosEliminadosListener() {
                    @Override
                    public void onExito() {
                        // Finalmente eliminar el quiz
                        bd.collection("quizzes").document(quizId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), R.string.toast_quiz_eliminado_exito, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    String mensajeError = getString(R.string.toast_error_eliminar_quiz, e.getMessage());
                                    Toast.makeText(getContext(), mensajeError, Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onError(String error) {
                        String mensajeError = getString(R.string.toast_error_eliminar_resultados, error);
                        Toast.makeText(getContext(), mensajeError, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                String mensajeError = getString(R.string.toast_error_eliminar_preguntas, error);
                Toast.makeText(getContext(), mensajeError, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Interface para callback de eliminación de preguntas
    interface OnPreguntasEliminadasListener {
        void onExito();
        void onError(String error);
    }

    // Interface para callback de eliminación de resultados
    interface OnResultadosEliminadosListener {
        void onExito();
        void onError(String error);
    }

    private void eliminarPreguntasDeQuiz(String quizId, OnPreguntasEliminadasListener listener) {
        bd.collection("quizzes").document(quizId).collection("preguntas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        WriteBatch batch = bd.batch();
                        for (DocumentSnapshot documento : queryDocumentSnapshots) {
                            batch.delete(documento.getReference());
                        }
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("HomeFragment", "Preguntas eliminadas para el quiz: " + quizId);
                                    listener.onExito();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("HomeFragment", "Error al eliminar preguntas: " + e.getMessage());
                                    listener.onError(e.getMessage());
                                });
                    } else {
                        // No hay preguntas, continuar con eliminación del quiz
                        listener.onExito();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error al obtener preguntas para eliminar: " + e.getMessage());
                    listener.onError(e.getMessage());
                });
    }

    private void eliminarResultadosDeQuiz(String quizId, OnResultadosEliminadosListener listener) {
        bd.collection("resultados")
                .whereEqualTo("quizId", quizId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        WriteBatch batch = bd.batch();
                        for (DocumentSnapshot documento : queryDocumentSnapshots) {
                            batch.delete(documento.getReference());
                        }
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("HomeFragment", "Resultados eliminados para el quiz: " + quizId);
                                    listener.onExito();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("HomeFragment", "Error al eliminar resultados: " + e.getMessage());
                                    listener.onError(e.getMessage());
                                });
                    } else {
                        // No hay resultados, continuar con eliminación del quiz
                        Log.d("HomeFragment", "No hay resultados para eliminar del quiz: " + quizId);
                        listener.onExito();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error al obtener resultados para eliminar: " + e.getMessage());
                    listener.onError(e.getMessage());
                });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void obtenerQuizzes() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
            return;
        }
        String userId = currentUser.getUid();

        bd.collection("quizzes").whereEqualTo("userId", userId).orderBy("fechaCreacion", Query.Direction.DESCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("HomeFragment", "Error cargando quizzes: " + error.getMessage());
                    return;
                }
                if (value != null) {
                    listaQuizzes.clear();
                    listaQuizzesIds.clear();
                    for (DocumentSnapshot snapshot : value.getDocuments()) {
                        Quiz quiz = snapshot.toObject(Quiz.class);
                        if (quiz != null) {
                            listaQuizzes.add(quiz);
                            listaQuizzesIds.add(snapshot.getId());
                        }
                    }
                    adapterQuizzes.actualizarIds(listaQuizzesIds);
                    adapterQuizzes.notifyDataSetChanged();

                    if (listaQuizzes.isEmpty()) {
                        layoutVacio.setVisibility(View.VISIBLE);
                        rvQuizzes.setVisibility(View.GONE);
                    } else {
                        layoutVacio.setVisibility(View.GONE);
                        rvQuizzes.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }
}