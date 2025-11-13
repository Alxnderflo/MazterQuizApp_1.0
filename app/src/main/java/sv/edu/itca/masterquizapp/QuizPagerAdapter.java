package sv.edu.itca.masterquizapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

// CAMBIO-QUIZ: Adaptador personalizado para el ViewPager2 de preguntas
public class QuizPagerAdapter extends FragmentStateAdapter {
    private List<Pregunta> preguntas;
    private String quizTitulo;
    private ResolverQuizActivity.OnPreguntaRespondidaListener respuestaListener;

    public QuizPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                            List<Pregunta> preguntas,
                            String quizTitulo,
                            ResolverQuizActivity.OnPreguntaRespondidaListener respuestaListener) {
        super(fragmentActivity);
        this.preguntas = preguntas;
        this.quizTitulo = quizTitulo;
        this.respuestaListener = respuestaListener;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // CAMBIO-QUIZ: Crear fragment para la pregunta en la posición actual
        if (position < preguntas.size()) {
            Pregunta pregunta = preguntas.get(position);
            return PreguntaFragment.newInstance(pregunta, quizTitulo, position, preguntas.size(), respuestaListener);
        }
        // Fallback por si hay algún error
        return new Fragment();
    }

    @Override
    public int getItemCount() {
        return preguntas.size();
    }

    // CAMBIO-QUIZ: Metodo para actualizar la lista de preguntas
    public void actualizarPreguntas(List<Pregunta> nuevasPreguntas) {
        this.preguntas = nuevasPreguntas;
        notifyDataSetChanged();
    }
}