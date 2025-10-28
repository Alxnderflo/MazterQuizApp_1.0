package sv.edu.itca.masterquizapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PreguntasActivity extends AppCompatActivity {
    private RecyclerView rvQuestions;
    private LinearLayout layoutVacio;
    private Button btnAdd, btnCrearPregunta;
    private PreguntasAdapter adapter;
    private List<Pregunta> listaPreguntas;
    private FirebaseFirestore db;
    private String quizId;

    // Views del header
    private ImageView imgQuizHeader;
    private TextView tvHeaderT, tvHeaderD, tvHeaderNumQ, tvHeaderFecha;

    public static Intent newIntent(Context context, String quizId) {
        Intent intent = new Intent(context, PreguntasActivity.class);
        intent.putExtra("quiz_id", quizId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preguntas);

        quizId = getIntent().getStringExtra("quiz_id");
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(this, "Error: No se pudo identificar el quiz", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        listaPreguntas = new ArrayList<>();

        inicializarViews();
        cargarDatosQuiz();
        cargarPreguntas();
    }

    private void inicializarViews() {
        rvQuestions = findViewById(R.id.rvQuestions);
        layoutVacio = findViewById(R.id.layoutVacio);
        btnAdd = findViewById(R.id.btnAdd);
        btnCrearPregunta = findViewById(R.id.btnCrearPregunta);

        // Header views
        imgQuizHeader = findViewById(R.id.imgQuizHeader);
        tvHeaderT = findViewById(R.id.tvHeaderT);
        tvHeaderD = findViewById(R.id.tvHeaderD);
        tvHeaderNumQ = findViewById(R.id.tvHeaderNumQ);
        tvHeaderFecha = findViewById(R.id.tvHeaderFecha);

        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PreguntasAdapter(listaPreguntas);
        rvQuestions.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> abrirCrearPregunta());
        btnCrearPregunta.setOnClickListener(v -> abrirCrearPregunta());
    }

    private void cargarDatosQuiz() {
        db.collection("quizzes").document(quizId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("PreguntasActivity", "Error al cargar quiz: " + error.getMessage());
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            Quiz quiz = snapshot.toObject(Quiz.class);
                            if (quiz != null) {
                                tvHeaderT.setText(quiz.getTitulo());
                                tvHeaderD.setText(quiz.getDescripcion() != null ? quiz.getDescripcion() : "");
                                tvHeaderNumQ.setText(quiz.getNumPreguntas() + " preguntas");

                                if (quiz.getFechaCreacion() != null) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                    tvHeaderFecha.setText(sdf.format(quiz.getFechaCreacion()));
                                }

                                if (quiz.getImagenUrl() != null && !quiz.getImagenUrl().isEmpty()) {
                                    Glide.with(PreguntasActivity.this)
                                            .load(quiz.getImagenUrl())
                                            .placeholder(R.drawable.ico_empty_quiz)
                                            .into(imgQuizHeader);
                                } else {
                                    imgQuizHeader.setImageResource(R.drawable.ico_empty_quiz);
                                }
                            }
                        }
                    }
                });
    }

    private void cargarPreguntas() {
        db.collection("quizzes").document(quizId)
                .collection("preguntas")
                .orderBy("orden")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("PreguntasActivity", "Error al cargar preguntas: " + error.getMessage());
                            mostrarVistaVacia();
                            return;
                        }
                        if (value != null && !value.isEmpty()) {
                            listaPreguntas.clear();
                            for (DocumentSnapshot snapshot : value.getDocuments()) {
                                Pregunta pregunta = snapshot.toObject(Pregunta.class);
                                if (pregunta != null) {
                                    listaPreguntas.add(pregunta);
                                }
                            }
                            adapter.notifyDataSetChanged();
                            mostrarListaPreguntas();
                        } else {
                            mostrarVistaVacia();
                        }
                    }
                });
    }

    private void abrirCrearPregunta() {
        startActivity(CrearPreguntasActivity.newIntent(this, quizId));
    }

    private void mostrarListaPreguntas() {
        rvQuestions.setVisibility(View.VISIBLE);
        layoutVacio.setVisibility(View.GONE);
    }

    private void mostrarVistaVacia() {
        rvQuestions.setVisibility(View.GONE);
        layoutVacio.setVisibility(View.VISIBLE);
    }
}