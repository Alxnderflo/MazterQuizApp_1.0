package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView rvQuizzes;
    private QuizzesAdapter adapterQuizzes;
    private List<Quiz> listaQuizzes;
    private List<String> listaQuizzesIds;
    private FirebaseFirestore bd;
    private LinearLayout layoutVacio;
    private EditText searchBar;

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
        super.onViewCreated(view, savedInstanceState);
        rvQuizzes = view.findViewById(R.id.rvQuizzes);
        layoutVacio = view.findViewById(R.id.layoutVacio);
        searchBar = view.findViewById(R.id.searchBar);

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
                //BORRAR IMPLEMENTA DESPUES

            }
        });
        rvQuizzes.setAdapter(adapterQuizzes);

        obtenerQuizzes();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refrescar la lista cuando el fragmento se reanuda (al regresar de crear quiz)
        if (bd != null) {
            obtenerQuizzes();
        }
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