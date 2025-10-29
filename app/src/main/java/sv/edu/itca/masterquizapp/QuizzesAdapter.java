package sv.edu.itca.masterquizapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuizzesAdapter extends RecyclerView.Adapter<QuizzesAdapter.ViewHolderQuiz> {

    public interface OnQuizClickListener {
        void onQuizClick(Quiz quiz, String quizId);
    }

    public interface OnQuizMenuClickListener {
        void onEditQuiz(Quiz quiz, String quizId);

        void onDeleteQuiz(Quiz quiz, String quizId);

    }


    private final List<Quiz> listaQuizzes;
    private final List<String> listaQuizzesIds;
    private final Context contexto;
    private final OnQuizClickListener listener;
    private OnQuizMenuClickListener menuListener; //listener para ell menu de 3 puntitos

    public QuizzesAdapter(List<Quiz> listaQuizzes, Context context, OnQuizClickListener listener, OnQuizMenuClickListener menuListener) {
        this.listaQuizzes = listaQuizzes;
        this.contexto = context;
        this.listener = listener;
        this.menuListener = menuListener;
        this.listaQuizzesIds = new ArrayList<>();
    }

    public void actualizarIds(List<String> ids) {
        this.listaQuizzesIds.clear();
        this.listaQuizzesIds.addAll(ids);
    }

    @NonNull
    @Override
    public ViewHolderQuiz onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.item_quiz, parent, false);
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

        // Cargar imagen con Glide (si no hay imagen, usar una por defecto)
        if (quiz.getImagenUrl() != null && !quiz.getImagenUrl().isEmpty()) {
            Glide.with(contexto)
                    .load(quiz.getImagenUrl())
                    .placeholder(R.drawable.ico_empty_quiz)
                    .into(holder.imgQuiz);
        } else {
            holder.imgQuiz.setImageResource(R.drawable.ico_empty_quiz);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && quizId != null) {
                listener.onQuizClick(quiz, quizId);
            }
        });
        holder.btnMore.setOnClickListener(v -> {
            if (quizId != null) {
                mostrarMenuContextual(v, quiz, quizId);

            }
        });

    }

    private void mostrarMenuContextual(View v, Quiz quiz, String quizId) {
        PopupMenu popupMenu = new PopupMenu(contexto, v);
        popupMenu.inflate(R.menu.menu_quiz_item);
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.mquiz_edit) {
                if (menuListener != null) {
                    menuListener.onEditQuiz(quiz, quizId);
                }
                return true;
            } else if (id == R.id.mquizz_delete) {
                if (menuListener != null) {
                    menuListener.onDeleteQuiz(quiz, quizId);
                }
                return true;
            }
            return false;
        });
        popupMenu.show();

    }


    @Override
    public int getItemCount() {
        return listaQuizzes.size();
    }

    public static class ViewHolderQuiz extends RecyclerView.ViewHolder {
        ImageView imgQuiz;
        TextView txtTitulo;
        TextView txtNumPreguntas;
        TextView txtFecha;
        ImageButton btnMore;

        public ViewHolderQuiz(@NonNull View itemView) {
            super(itemView);
            imgQuiz = itemView.findViewById(R.id.imgQuiz);
            txtTitulo = itemView.findViewById(R.id.txtQuizT);
            txtNumPreguntas = itemView.findViewById(R.id.txtContarP);
            txtFecha = itemView.findViewById(R.id.txtFechaQ);
            btnMore = itemView.findViewById(R.id.btnMore);

        }
    }
}