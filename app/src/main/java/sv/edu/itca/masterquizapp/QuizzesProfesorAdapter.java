package sv.edu.itca.masterquizapp;

import static sv.edu.itca.masterquizapp.R.drawable.ico_empty_quiz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuizzesProfesorAdapter extends RecyclerView.Adapter<QuizzesProfesorAdapter.ViewHolderQuiz> {

    public interface OnQuizClickListener {
        void onQuizClick(Quiz quiz, String quizId);
    }

    private final List<Quiz> listaQuizzes;
    private final List<String> listaQuizzesIds;
    private final Context contexto;
    private final OnQuizClickListener listener;
    private final int[] coloresProfesores;
    private final Map<String, Integer> mapaColoresProfesores;

    public QuizzesProfesorAdapter(List<Quiz> listaQuizzes, Context context, OnQuizClickListener listener, int[] coloresProfesores, Map<String, Integer> mapaColoresProfesores) {
        this.listaQuizzes = listaQuizzes;
        this.contexto = context;
        this.listener = listener;
        this.listaQuizzesIds = new ArrayList<>();
        this.coloresProfesores = coloresProfesores;
        this.mapaColoresProfesores = mapaColoresProfesores;
    }

    public void actualizarIds(List<String> ids) {
        this.listaQuizzesIds.clear();
        this.listaQuizzesIds.addAll(ids);
    }

    @NonNull
    @Override
    public ViewHolderQuiz onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.item_quiz_profesor, parent, false);
        return new ViewHolderQuiz(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderQuiz holder, int position) {
        Quiz quiz = listaQuizzes.get(position);
        String quizId = position < listaQuizzesIds.size() ? listaQuizzesIds.get(position) : null;

        // Configurar los views con los datos del quiz
        holder.txtTitulo.setText(quiz.getTitulo());
        holder.txtNumPreguntas.setText(quiz.getNumPreguntas() + " preguntas");

        // Formatear la fecha
        if (quiz.getFechaCreacion() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.txtFecha.setText(sdf.format(quiz.getFechaCreacion()));
        } else {
            holder.txtFecha.setText("");
        }

        // Cargar imagen con Glide
        if (quiz.getImagenUrl() != null && !quiz.getImagenUrl().isEmpty()) {
            Glide.with(contexto)
                    .load(quiz.getImagenUrl())
                    .placeholder(ico_empty_quiz)
                    .into(holder.imgQuiz);
        } else {
            holder.imgQuiz.setImageResource(ico_empty_quiz);
        }

        // Configurar color de la barra lateral según el profesor
        String profesorId = quiz.getUserId();
        Integer colorProfesor = mapaColoresProfesores.get(profesorId);
        if (colorProfesor != null) {
            holder.barraLateral.setBackgroundColor(colorProfesor);
        } else {
            // Color por defecto si no se encuentra
            holder.barraLateral.setBackgroundColor(coloresProfesores[0]);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && quizId != null) {
                listener.onQuizClick(quiz, quizId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaQuizzes.size();
    }

    public static class ViewHolderQuiz extends RecyclerView.ViewHolder {
        View barraLateral;
        ImageView imgQuiz;
        TextView txtTitulo;
        TextView txtNumPreguntas;
        TextView txtFecha;

        public ViewHolderQuiz(@NonNull View itemView) {
            super(itemView);
            barraLateral = itemView.findViewById(R.id.barraLateral);
            imgQuiz = itemView.findViewById(R.id.imgQuiz);
            txtTitulo = itemView.findViewById(R.id.txtQuizT);
            txtNumPreguntas = itemView.findViewById(R.id.txtContarP);
            txtFecha = itemView.findViewById(R.id.txtFechaQ);
        }
    }
}