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
    private String preguntaIdEdicion; // CAMBIO: ID de la pregunta en modo edición
    private boolean esModoEdicion = false; // CAMBIO: Bandera para modo edición

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

        // CAMBIO: Verificar si estamos en modo edición
        esModoEdicion = getIntent().getBooleanExtra("MODO_EDICION", false);
        if (esModoEdicion) {
            setTitle("Editar Pregunta");
            preguntaIdEdicion = getIntent().getStringExtra("pregunta_id");
            String enunciado = getIntent().getStringExtra("pregunta_enunciado");
            String correcta = getIntent().getStringExtra("pregunta_correcta");
            String incorrecta1 = getIntent().getStringExtra("pregunta_incorrecta1");
            String incorrecta2 = getIntent().getStringExtra("pregunta_incorrecta2");
            String incorrecta3 = getIntent().getStringExtra("pregunta_incorrecta3");

            etPregunta.setText(enunciado);
            etRespuestaCorrecta.setText(correcta);
            etRespuestaIncorrecta1.setText(incorrecta1);
            etRespuestaIncorrecta2.setText(incorrecta2);
            etRespuestaIncorrecta3.setText(incorrecta3);
        } else {
            setTitle("Crear Pregunta");
        }

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

        // CAMBIO: Decidir si actualizar o crear nueva
        if (esModoEdicion) {
            actualizarPregunta(existingPregunta -> {
                // Actualizar la pregunta existente
                existingPregunta.setEnunciado(enunciado);
                existingPregunta.setCorrecta(correcta);
                existingPregunta.setIncorrecta1(incorrecta1);
                existingPregunta.setIncorrecta2(incorrecta2);
                existingPregunta.setIncorrecta3(incorrecta3);
                // El orden se mantiene

                db.collection("quizzes").document(quizId)
                        .collection("preguntas").document(preguntaIdEdicion)
                        .set(existingPregunta)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(CrearPreguntasActivity.this, "Pregunta actualizada", Toast.LENGTH_SHORT).show();
                            finish(); // CAMBIO: Cerrar la actividad después de actualizar
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(CrearPreguntasActivity.this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        } else {
            // Modo creación: crear nueva pregunta
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
                                    Toast.makeText(CrearPreguntasActivity.this, "Pregunta guardada", Toast.LENGTH_SHORT).show();
                                    limpiarCampos();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("CrearPregunta", "Error al sincronizar: " + e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CrearPregunta", "Error al obtener el número de preguntas: " + e.getMessage());
                    });
        }
    }

    // CAMBIO: Metodo para obtener la pregunta existente y luego actualizarla
    private void actualizarPregunta(final FirestoreCallback<Pregunta> callback) {
        db.collection("quizzes").document(quizId)
                .collection("preguntas").document(preguntaIdEdicion)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Pregunta existingPregunta = documentSnapshot.toObject(Pregunta.class);
                    callback.onCallback(existingPregunta);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar pregunta existente", Toast.LENGTH_SHORT).show();
                });
    }

    // CAMBIO: Interfaz para el callback de Firestore
    private interface FirestoreCallback<T> {
        void onCallback(T data);
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