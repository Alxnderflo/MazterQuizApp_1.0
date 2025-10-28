package sv.edu.itca.masterquizapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class CrearPreguntasActivity extends AppCompatActivity {
    private EditText etPregunta, etRespuestaCorrecta, etRespuestaIncorrecta1, etRespuestaIncorrecta2, etRespuestaIncorrecta3;
    private Button btnGuardarPregunta;
    private FirebaseFirestore db;
    private String quizId;

    public static Intent newIntent(Context context, String quizId) {
        Intent intent = new Intent(context, CrearPreguntasActivity.class);
        intent.putExtra("quiz_id", quizId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_preguntas);

        quizId = getIntent().getStringExtra("quiz_id");
        if (quizId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        etPregunta = findViewById(R.id.etPregunta);
        etRespuestaCorrecta = findViewById(R.id.etRespuestaCorrecta);
        etRespuestaIncorrecta1 = findViewById(R.id.etRespuestaIncorrecta1);
        etRespuestaIncorrecta2 = findViewById(R.id.etRespuestaIncorrecta2);
        etRespuestaIncorrecta3 = findViewById(R.id.etRespuestaIncorrecta3);
        btnGuardarPregunta = findViewById(R.id.btnGuardarPregunta);

        btnGuardarPregunta.setOnClickListener(v -> guardarPregunta());
    }

    private void guardarPregunta() {
        String enunciado = etPregunta.getText().toString().trim();
        String correcta = etRespuestaCorrecta.getText().toString().trim();
        String incorrecta1 = etRespuestaIncorrecta1.getText().toString().trim();
        String incorrecta2 = etRespuestaIncorrecta2.getText().toString().trim();
        String incorrecta3 = etRespuestaIncorrecta3.getText().toString().trim();

        if (enunciado.isEmpty() || correcta.isEmpty() || incorrecta1.isEmpty() || incorrecta2.isEmpty() || incorrecta3.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("quizzes").document(quizId)
                .collection("preguntas")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int orden = queryDocumentSnapshots.size() + 1;

                    Pregunta pregunta = new Pregunta(enunciado, correcta, incorrecta1, incorrecta2, incorrecta3, orden);

                    db.collection("quizzes").document(quizId)
                            .collection("preguntas")
                            .add(pregunta)
                            .addOnSuccessListener(documentReference -> {
                                actualizarContadorPreguntas(orden);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("CrearPregunta", "Error al sincronizar: " + e.getMessage());
                            });

                    Toast.makeText(this, "Pregunta guardada localmente", Toast.LENGTH_SHORT).show();
                    limpiarCampos();
                })
                .addOnFailureListener(e -> {
                    Log.e("CrearPregunta", "Error al obtener el número de preguntas: " + e.getMessage());
                });
    }

    private void actualizarContadorPreguntas(int nuevoContador) {
        db.collection("quizzes").document(quizId)
                .update("numPreguntas", nuevoContador)
                .addOnSuccessListener(aVoid -> {
                    // Contador actualizado
                })
                .addOnFailureListener(e -> {
                    Log.e("CrearPregunta", "Error al actualizar contador: " + e.getMessage());
                });
    }

    private void limpiarCampos() {
        etPregunta.setText("");
        etRespuestaCorrecta.setText("");
        etRespuestaIncorrecta1.setText("");
        etRespuestaIncorrecta2.setText("");
        etRespuestaIncorrecta3.setText("");
    }
}