package sv.edu.itca.masterquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

// CAMBIO-QUIZ: Actividad para mostrar los resultados del quiz
public class ResultadosActivity extends AppCompatActivity {
    private RecyclerView rvResultados;
    private MaterialButton btnVolverInicio;
    private TextView tvPuntuacion;
    private ResultadosAdapter adapter;

    private List<ResultadoPregunta> resultados;
    private String quizTitulo;
    private int totalPreguntas;
    private int respuestasCorrectas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados);

        // CAMBIO-QUIZ: Obtener datos del intent
        obtenerDatosIntent();

        inicializarViews();
        configurarUI();
        configurarRecyclerView();

        // CAMBIO-QUIZ: Configurar el manejo del botón de retroceso
        configurarBackPressed();
    }

    // CAMBIO-QUIZ: Obtener datos pasados desde ResolverQuizActivity
    private void obtenerDatosIntent() {
        Intent intent = getIntent();
        quizTitulo = intent.getStringExtra("quiz_titulo");
        totalPreguntas = intent.getIntExtra("total_preguntas", 0);
        respuestasCorrectas = intent.getIntExtra("respuestas_correctas", 0);
        resultados = intent.getParcelableArrayListExtra("resultados");

        // CAMBIO-QUIZ: Validar datos recibidos
        if (resultados == null) {
            Toast.makeText(this, "Error al cargar resultados", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void inicializarViews() {
        rvResultados = findViewById(R.id.rvResultadosQuiz);
        btnVolverInicio = findViewById(R.id.btnVolverInicio);
        tvPuntuacion = findViewById(R.id.tvPuntuacion);

        // CAMBIO-QUIZ: Configurar botón para volver al inicio
        btnVolverInicio.setOnClickListener(v -> volverAlInicio());
    }

    // CAMBIO-QUIZ: Configurar la interfaz de usuario
    private void configurarUI() {
        // Configurar la puntuación
        tvPuntuacion.setText(respuestasCorrectas + "/" + totalPreguntas);

        // CAMBIO-QUIZ: Opcional - Personalizar color según el desempeño
        double porcentaje = (double) respuestasCorrectas / totalPreguntas * 100;
        if (porcentaje >= 80) {
            // Excelente - Verde
            tvPuntuacion.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (porcentaje >= 60) {
            // Bueno - Naranja
            tvPuntuacion.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            // Necesita mejorar - Rojo
            tvPuntuacion.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    // CAMBIO-QUIZ: Configurar el RecyclerView con los resultados
    private void configurarRecyclerView() {
        rvResultados.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ResultadosAdapter(resultados);
        rvResultados.setAdapter(adapter);

        // CAMBIO-QUIZ: Opcional - Agregar separación entre items
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        rvResultados.addItemDecoration(dividerItemDecoration);
    }

    // CAMBIO-QUIZ: Volver a la actividad principal
    private void volverAlInicio() {
        // Intent para volver a MainActivity (actividad principal)
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // CAMBIO-QUIZ: Configurar el manejo del botón de retroceso
    private void configurarBackPressed() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                volverAlInicio();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
}