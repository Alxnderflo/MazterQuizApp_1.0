package sv.edu.itca.masterquizapp;

import android.content.Context;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiAIService {
    private static final String TAG = "GeminiAIService";
    private final GenerativeModelFutures model;
    private final Context context;

    public GeminiAIService(Context context) {
        this.context = context;

        // CORRECCIÓN: Usar gemini-2.5-flash-lite como solicitaste
        String apiKey = context.getString(R.string.gemini_api_key);
        GenerativeModel generativeModel = new GenerativeModel(
                "gemini-2.5-flash-lite", // Modelo estable que sí existe
                apiKey
        );
        model = GenerativeModelFutures.from(generativeModel);
    }

    public interface PreguntasGeneratedCallback {
        void onSuccess(List<Pregunta> preguntas);
        void onError(String error);
    }

    public void generarPreguntas(String tema, String descripcion, int numPreguntas, PreguntasGeneratedCallback callback) {
        String prompt = construirPrompt(tema, descripcion, numPreguntas);

        Log.d(TAG, "Enviando prompt a IA: " + prompt);

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                Log.d(TAG, "Respuesta de IA recibida: " + text);

                if (text == null || text.isEmpty()) {
                    callback.onError("La IA no generó ninguna respuesta");
                    return;
                }

                try {
                    List<Pregunta> preguntas = parsearRespuesta(text, numPreguntas);
                    callback.onSuccess(preguntas);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parseando respuesta JSON: " + e.getMessage());
                    callback.onError("Error al procesar el formato de las preguntas: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Error inesperado: " + e.getMessage());
                    callback.onError("Error inesperado: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error llamando a la IA: " + t.getMessage());
                callback.onError("Error de conexión con IA: " + t.getMessage());
            }
        }, executor);
    }

    private String construirPrompt(String tema, String descripcion, int numPreguntas) {
        return "Eres un experto en crear preguntas educativas. Genera EXACTAMENTE " + numPreguntas +
                " preguntas de opción múltiple sobre: '" + tema + "'" +
                (descripcion.isEmpty() ? "" : ". Contexto: " + descripcion) +
                "\n\nREQUISITOS:\n" +
                "- Formato JSON estricto\n" +
                "- " + numPreguntas + " preguntas exactamente\n" +
                "- Cada pregunta con 4 opciones\n" +
                "- 1 correcta y 3 incorrectas plausibles\n" +
                "- Dificultad media\n" +
                "- Variedad de aspectos del tema\n\n" +
                "FORMATO EXACTO:\n" +
                "{\n" +
                "  \"preguntas\": [\n" +
                "    {\n" +
                "      \"enunciado\": \"¿Pregunta aquí?\",\n" +
                "      \"correcta\": \"Respuesta correcta\",\n" +
                "      \"incorrecta1\": \"Opción incorrecta 1\",\n" +
                "      \"incorrecta2\": \"Opción incorrecta 2\",\n" +
                "      \"incorrecta3\": \"Opción incorrecta 3\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n\n" +
                "Devuelve SOLO el JSON, sin texto adicional.";
    }

    private List<Pregunta> parsearRespuesta(String text, int numPreguntas) throws JSONException {
        // Limpieza más robusta del JSON
        String jsonString = text.trim();

        // Remover markdown code blocks
        jsonString = jsonString.replaceAll("```json", "").replaceAll("```", "").trim();

        // Buscar el JSON real dentro del texto
        int startIndex = jsonString.indexOf("{");
        int endIndex = jsonString.lastIndexOf("}") + 1;

        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            jsonString = jsonString.substring(startIndex, endIndex);
        } else {
            throw new JSONException("No se encontró JSON válido en la respuesta");
        }

        Log.d(TAG, "JSON limpio para parsear: " + jsonString);

        JSONObject jsonResponse = new JSONObject(jsonString);
        JSONArray preguntasArray = jsonResponse.getJSONArray("preguntas");

        List<Pregunta> preguntas = new ArrayList<>();
        for (int i = 0; i < preguntasArray.length(); i++) {
            JSONObject preguntaJson = preguntasArray.getJSONObject(i);

            // Validar que tenga todos los campos necesarios
            if (!preguntaJson.has("enunciado") || !preguntaJson.has("correcta") ||
                    !preguntaJson.has("incorrecta1") || !preguntaJson.has("incorrecta2") ||
                    !preguntaJson.has("incorrecta3")) {
                throw new JSONException("Pregunta incompleta en la posición " + i);
            }

            Pregunta pregunta = new Pregunta();
            pregunta.setEnunciado(preguntaJson.getString("enunciado").trim());
            pregunta.setCorrecta(preguntaJson.getString("correcta").trim());
            pregunta.setIncorrecta1(preguntaJson.getString("incorrecta1").trim());
            pregunta.setIncorrecta2(preguntaJson.getString("incorrecta2").trim());
            pregunta.setIncorrecta3(preguntaJson.getString("incorrecta3").trim());
            pregunta.setOrden(i + 1);

            preguntas.add(pregunta);

            Log.d(TAG, "Pregunta " + (i + 1) + " parseada: " + pregunta.getEnunciado());
        }

        Log.d(TAG, "Total de preguntas parseadas: " + preguntas.size());
        return preguntas;
    }
}