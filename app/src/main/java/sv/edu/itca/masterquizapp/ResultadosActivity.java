package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ResultadosActivity extends AppCompatActivity {
    private RecyclerView rvResultados;
    private MaterialButton btnVolverInicio;
    private TextView tvPuntuacion;
    private ResultadosAdapter adapter;

    private List<ResultadoPregunta> resultados;
    private String quizTitulo;
    private String quizId;
    private int totalPreguntas;
    private int respuestasCorrectas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados);

        obtenerDatosIntent();
        inicializarViews();
        configurarUI();
        configurarRecyclerView();
        configurarBackPressed();

        // ✅ CORREGIDO: Guardar resultado después de inicializar todo
        guardarResultadoEnFirestore();
    }

    private void obtenerDatosIntent() {
        Intent intent = getIntent();
        quizTitulo = intent.getStringExtra("quiz_titulo");
        quizId = intent.getStringExtra("quiz_id");
        totalPreguntas = intent.getIntExtra("total_preguntas", 0);
        respuestasCorrectas = intent.getIntExtra("respuestas_correctas", 0);
        resultados = intent.getParcelableArrayListExtra("resultados");

        if (resultados == null || quizId == null) {
            Toast.makeText(this, "Error al cargar resultados", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("ResultadosActivity", "Datos recibidos - Quiz: " + quizTitulo +
                ", Correctas: " + respuestasCorrectas + "/" + totalPreguntas);
    }

    private void inicializarViews() {
        rvResultados = findViewById(R.id.rvResultadosQuiz);
        btnVolverInicio = findViewById(R.id.btnVolverInicio);
        tvPuntuacion = findViewById(R.id.tvPuntuacion);

        btnVolverInicio.setOnClickListener(v -> volverAlInicio());
    }

    private void configurarUI() {
        tvPuntuacion.setText(respuestasCorrectas + "/" + totalPreguntas);

        double porcentaje = (double) respuestasCorrectas / totalPreguntas * 100;
        if (porcentaje >= 80) {
            tvPuntuacion.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (porcentaje >= 60) {
            tvPuntuacion.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            tvPuntuacion.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void configurarRecyclerView() {
        rvResultados.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultadosAdapter(resultados);
        rvResultados.setAdapter(adapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        rvResultados.addItemDecoration(dividerItemDecoration);
    }

    private void volverAlInicio() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void configurarBackPressed() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                volverAlInicio();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    // ✅ CORREGIDO: Método para guardar resultado en Firestore
    private void guardarResultadoEnFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("ResultadosActivity", "Usuario no autenticado");
            return;
        }

        // Calcular porcentaje
        int porcentaje = (int) ((double) respuestasCorrectas / totalPreguntas * 100);

        // Crear objeto Resultado SIN ID específico
        Resultado resultado = new Resultado(
                currentUser.getUid(),
                quizId,
                quizTitulo,
                porcentaje,
                Timestamp.now(),
                totalPreguntas,
                respuestasCorrectas
        );

        // ✅ CORRECCIÓN: Usar add() para crear NUEVOS documentos cada vez
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("resultados")
                .add(resultado) // ✅ Esto crea un nuevo documento con ID automático
                .addOnSuccessListener(documentReference -> {
                    // Opcional: Actualizar el objeto con el ID generado
                    resultado.setId(documentReference.getId());
                    Log.d("ResultadosActivity", "✅ NUEVO resultado guardado con ID: " + documentReference.getId() +
                            " - Quiz: " + quizTitulo + " - Puntuación: " + porcentaje + "%");
                })
                .addOnFailureListener(e -> {
                    Log.e("ResultadosActivity", "❌ Error al guardar resultado: " + e.getMessage());
                    Toast.makeText(this, "Error al guardar resultado", Toast.LENGTH_SHORT).show();
                });
    }
}