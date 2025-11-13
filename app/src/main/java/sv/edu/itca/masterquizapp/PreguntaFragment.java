package sv.edu.itca.masterquizapp;

import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PreguntaFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

// CAMBIO-QUIZ: Fragment para mostrar una pregunta individual
public class PreguntaFragment extends Fragment {
    private static final String ARG_PREGUNTA = "pregunta";
    private static final String ARG_QUIZ_TITULO = "quiz_titulo";
    private static final String ARG_POSICION = "posicion";
    private static final String ARG_TOTAL = "total";

    private Pregunta pregunta;
    private String quizTitulo;
    private int posicion;
    private int totalPreguntas;
    private ResolverQuizActivity.OnPreguntaRespondidaListener respuestaListener;

    // CAMBIO-QUIZ: Views
    private ProgressBar barraProgreso;
    private TextView tvProgreso, tvTituloQuiz, tvNumPregunta, tvEnunciado;
    private MaterialButton btnOpcionA, btnOpcionB, btnOpcionC, btnOpcionD;

    // CAMBIO-QUIZ: Variables para manejar opciones
    private Map<String, MaterialButton> botonesOpciones;
    private Map<String, String> opcionesMezcladas;
    private String letraCorrecta;
    private boolean yaRespondido = false;

    public PreguntaFragment() {
        // Constructor público vacío requerido
    }

    // CAMBIO-QUIZ: Metodo factory para crear instancias
    public static PreguntaFragment newInstance(Pregunta pregunta, String quizTitulo,
                                               int posicion, int totalPreguntas,
                                               ResolverQuizActivity.OnPreguntaRespondidaListener listener) {
        PreguntaFragment fragment = new PreguntaFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PREGUNTA, (Parcelable) pregunta);
        args.putString(ARG_QUIZ_TITULO, quizTitulo);
        args.putInt(ARG_POSICION, posicion);
        args.putInt(ARG_TOTAL, totalPreguntas);
        fragment.setArguments(args);
        fragment.respuestaListener = listener;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pregunta = getArguments().getParcelable(ARG_PREGUNTA);
            quizTitulo = getArguments().getString(ARG_QUIZ_TITULO);
            posicion = getArguments().getInt(ARG_POSICION);
            totalPreguntas = getArguments().getInt(ARG_TOTAL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preguntas, container, false);

        inicializarViews(view);
        configurarUI();

        return view;
    }

    // CAMBIO-QUIZ: Inicializar todas las views
    private void inicializarViews(View view) {
        barraProgreso = view.findViewById(R.id.barraProgreso);
        tvProgreso = view.findViewById(R.id.tvProgreso);
        tvTituloQuiz = view.findViewById(R.id.ivTituloQuiz);
        tvNumPregunta = view.findViewById(R.id.tvNumPreguntaQuiz);
        tvEnunciado = view.findViewById(R.id.tvEnunciadoPregunta);

        btnOpcionA = view.findViewById(R.id.btnOpcionA);
        btnOpcionB = view.findViewById(R.id.btnOpcionB);
        btnOpcionC = view.findViewById(R.id.btnOpcionC);
        btnOpcionD = view.findViewById(R.id.btnOpcionD);

        // CAMBIO-QUIZ: Configurar mapa de botones para fácil acceso
        botonesOpciones = new HashMap<>();
        botonesOpciones.put("A", btnOpcionA);
        botonesOpciones.put("B", btnOpcionB);
        botonesOpciones.put("C", btnOpcionC);
        botonesOpciones.put("D", btnOpcionD);

        // CAMBIO-QUIZ: Configurar listeners para los botones de opciones
        for (Map.Entry<String, MaterialButton> entry : botonesOpciones.entrySet()) {
            entry.getValue().setOnClickListener(v -> onOpcionSeleccionada(entry.getKey()));
        }
    }

    // CAMBIO-QUIZ: Configurar la UI con los datos de la pregunta
    private void configurarUI() {
        if (pregunta == null) return;

        // Configurar progreso
        barraProgreso.setMax(totalPreguntas);
        barraProgreso.setProgress(posicion + 1);
        tvProgreso.setText((posicion + 1) + "/" + totalPreguntas);

        // Configurar textos
        tvTituloQuiz.setText(quizTitulo);
        tvNumPregunta.setText("Pregunta: " + (posicion + 1));
        tvEnunciado.setText(pregunta.getEnunciado());

        // CAMBIO-QUIZ: Obtener y mezclar opciones
        opcionesMezcladas = pregunta.getOpcionesMezcladas();
        letraCorrecta = pregunta.obtenerLetraCorrecta(opcionesMezcladas);

        // Asignar texto a los botones
        for (Map.Entry<String, String> entry : opcionesMezcladas.entrySet()) {
            MaterialButton boton = botonesOpciones.get(entry.getKey());
            if (boton != null) {
                boton.setText(entry.getKey() + ". " + entry.getValue());
                // Reiniciar estado del botón
                boton.setEnabled(true);
                reiniciarEstiloBoton(boton);
            }
        }

        yaRespondido = false;
    }

    // CAMBIO-QUIZ: Manejar selección de opción
    private void onOpcionSeleccionada(String letraSeleccionada) {
        if (yaRespondido) return;

        yaRespondido = true;

        // Deshabilitar todos los botones
        for (MaterialButton boton : botonesOpciones.values()) {
            boton.setEnabled(false);
        }

        MaterialButton botonSeleccionado = botonesOpciones.get(letraSeleccionada);
        boolean esCorrecta = letraSeleccionada.equals(letraCorrecta);
        String respuestaUsuario = opcionesMezcladas.get(letraSeleccionada);

        // CAMBIO-QUIZ: Aplicar estilos visuales según la respuesta
        if (esCorrecta) {
            // Respuesta correcta - solo verde en el seleccionado
            aplicarEstiloCorrecto(botonSeleccionado);
        } else {
            // Respuesta incorrecta - rojo en seleccionado, verde en correcto
            aplicarEstiloIncorrecto(botonSeleccionado);
            MaterialButton botonCorrecto = botonesOpciones.get(letraCorrecta);
            aplicarEstiloCorrecto(botonCorrecto);
        }

        // CAMBIO-QUIZ: Notificar a la actividad
        if (respuestaListener != null) {
            respuestaListener.onPreguntaRespondida(posicion, respuestaUsuario, esCorrecta);
        }
    }

    // CAMBIO-QUIZ: Aplicar estilo para respuesta correcta (verde)
    private void aplicarEstiloCorrecto(MaterialButton boton) {
        boton.setStrokeColor(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_green_dark)));
        boton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_correcta)));
        boton.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
    }

    // CAMBIO-QUIZ: Aplicar estilo para respuesta incorrecta (rojo)
    private void aplicarEstiloIncorrecto(MaterialButton boton) {
        boton.setStrokeColor(ColorStateList.valueOf(getResources().getColor(android.R.color.holo_red_dark)));
        boton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_incorrecta)));
        boton.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    // CAMBIO-QUIZ: Reiniciar estilo del botón a estado inicial
    private void reiniciarEstiloBoton(MaterialButton boton) {
        boton.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.color_borde_inicial)));
        boton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.white)));
        boton.setTextColor(getResources().getColor(R.color.color_texto_inicial));
    }
}