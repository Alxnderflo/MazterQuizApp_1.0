package sv.edu.itca.masterquizapp;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.annotation.Nullable;

public class SplashScreen extends Activity {

    private static final int DURACION_TOTAL = 4000;
    private static final int DURACION_NOMBRE_COMPLETO = 1800;
    private static final int DURACION_TRANSICION = 400;
    private static final int DURACION_NOMBRE_ABREVIADO = 1800;

    private ImageView logoNombreCompleto;
    private ImageView logoNombreAbreviado;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        logoNombreCompleto = findViewById(R.id.nombreCompleto);
        logoNombreAbreviado = findViewById(R.id.nombreAbreviado);
        progressBar = findViewById(R.id.progressBar);

        iniciarAnimaciones();
    }

    private void iniciarAnimaciones() {
        // FASE 1: Aparición del nombre completo (0ms - 1800ms)
        mostrarNombreCompleto();

        // FASE 2: Desaparición del nombre completo y aparición del abreviado (1800ms - 2200ms)
        Handler handler1 = new Handler();
        handler1.postDelayed(() -> {
            desaparecerNombreCompleto();
            mostrarNombreAbreviado();
        }, DURACION_NOMBRE_COMPLETO);

        // FASE 3: Transición final y carga de siguiente pantalla (4000ms)
        Handler handler2 = new Handler();
        handler2.postDelayed(() -> {
            desaparecerNombreAbreviado();
            navegarALogin();
        }, DURACION_TOTAL);
    }

    private void mostrarNombreCompleto() {
        // Fade In
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logoNombreCompleto, "alpha", 0f, 1f);
        fadeIn.setDuration(600);

        // Zoom In (escala)
        ObjectAnimator scaleXIn = ObjectAnimator.ofFloat(logoNombreCompleto, "scaleX", 0.7f, 1f);
        scaleXIn.setDuration(600);
        ObjectAnimator scaleYIn = ObjectAnimator.ofFloat(logoNombreCompleto, "scaleY", 0.7f, 1f);
        scaleYIn.setDuration(600);

        // Rotación sutil
        ObjectAnimator rotateIn = ObjectAnimator.ofFloat(logoNombreCompleto, "rotation", -10f, 0f);
        rotateIn.setDuration(600);

        AnimatorSet setIn = new AnimatorSet();
        setIn.playTogether(fadeIn, scaleXIn, scaleYIn, rotateIn);
        setIn.start();

        // Mostrar barra de progreso
        ObjectAnimator progressFadeIn = ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 0.6f);
        progressFadeIn.setDuration(400);
        progressFadeIn.start();

        // Animar barra de progreso
        ObjectAnimator progressAnim = ObjectAnimator.ofInt(progressBar, "progress", 0, 20);
        progressAnim.setDuration(DURACION_NOMBRE_COMPLETO);
        progressAnim.start();
    }

    private void desaparecerNombreCompleto() {
        // Fade Out
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(logoNombreCompleto, "alpha", 1f, 0f);
        fadeOut.setDuration(DURACION_TRANSICION);

        // Zoom Out
        ObjectAnimator scaleXOut = ObjectAnimator.ofFloat(logoNombreCompleto, "scaleX", 1f, 0.7f);
        scaleXOut.setDuration(DURACION_TRANSICION);
        ObjectAnimator scaleYOut = ObjectAnimator.ofFloat(logoNombreCompleto, "scaleY", 1f, 0.7f);
        scaleYOut.setDuration(DURACION_TRANSICION);

        // Rotación inversa
        ObjectAnimator rotateOut = ObjectAnimator.ofFloat(logoNombreCompleto, "rotation", 0f, 10f);
        rotateOut.setDuration(DURACION_TRANSICION);

        AnimatorSet setOut = new AnimatorSet();
        setOut.playTogether(fadeOut, scaleXOut, scaleYOut, rotateOut);
        setOut.start();
    }

    private void mostrarNombreAbreviado() {
        // Fade In
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logoNombreAbreviado, "alpha", 0f, 1f);
        fadeIn.setDuration(600);

        // Zoom In
        ObjectAnimator scaleXIn = ObjectAnimator.ofFloat(logoNombreAbreviado, "scaleX", 0.6f, 1f);
        scaleXIn.setDuration(600);
        ObjectAnimator scaleYIn = ObjectAnimator.ofFloat(logoNombreAbreviado, "scaleY", 0.6f, 1f);
        scaleYIn.setDuration(600);

        // Rotación desde el lado contrario
        ObjectAnimator rotateIn = ObjectAnimator.ofFloat(logoNombreAbreviado, "rotation", 15f, 0f);
        rotateIn.setDuration(600);

        AnimatorSet setIn = new AnimatorSet();
        setIn.playTogether(fadeIn, scaleXIn, scaleYIn, rotateIn);
        setIn.start();

        // Continuar la barra de progreso
        ObjectAnimator progressAnim = ObjectAnimator.ofInt(progressBar, "progress", 20, 100);
        progressAnim.setDuration(DURACION_NOMBRE_ABREVIADO);
        progressAnim.start();
    }

    private void desaparecerNombreAbreviado() {
        // Fade Out final
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(logoNombreAbreviado, "alpha", 1f, 0f);
        fadeOut.setDuration(400);

        // Zoom Out
        ObjectAnimator scaleXOut = ObjectAnimator.ofFloat(logoNombreAbreviado, "scaleX", 1f, 1.1f);
        scaleXOut.setDuration(400);
        ObjectAnimator scaleYOut = ObjectAnimator.ofFloat(logoNombreAbreviado, "scaleY", 1f, 1.1f);
        scaleYOut.setDuration(400);

        AnimatorSet setOut = new AnimatorSet();
        setOut.playTogether(fadeOut, scaleXOut, scaleYOut);
        setOut.start();

        // Desaparecer barra de progreso
        ObjectAnimator progressFadeOut = ObjectAnimator.ofFloat(progressBar, "alpha", 0.6f, 0f);
        progressFadeOut.setDuration(400);
        progressFadeOut.start();
    }

    private void navegarALogin() {
        Intent ventana = new Intent(SplashScreen.this, LoginActivity.class);
        startActivity(ventana);
        finish();
    }
}