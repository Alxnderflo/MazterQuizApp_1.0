package sv.edu.itca.masterquizapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScoreAdapter extends RecyclerView.Adapter<ScoreAdapter.ViewHolder> {

    private final List<EstadisticaQuiz> estadisticasList;
    private final int[] coloresIconos = {
            Color.parseColor("#A855F7"),
            Color.parseColor("#6B7EF7"),
            Color.parseColor("#F75555"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FF9800")
    };

    public ScoreAdapter(List<EstadisticaQuiz> estadisticasList) {
        this.estadisticasList = estadisticasList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_score_quiz, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EstadisticaQuiz estadistica = estadisticasList.get(position);

        // Asignar color al ícono basado en el hash del quizId
        int colorIndex = Math.abs(estadistica.getQuizId().hashCode()) % coloresIconos.length;
        holder.imgQuiz.setColorFilter(coloresIconos[colorIndex]);

        // Configurar los textos
        holder.txtQuizT.setText(estadistica.getTitulo());
        holder.txtIntentos.setText(String.valueOf(estadistica.getIntentos()));
        holder.txtMejor.setText(estadistica.getMejorResultado() + "%");
        holder.txtUltimo.setText(estadistica.getUltimoPuntaje() + "%");
    }

    @Override
    public int getItemCount() {
        return estadisticasList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgQuiz;
        TextView txtQuizT;
        TextView txtIntentos;
        TextView txtMejor;
        TextView txtUltimo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgQuiz = itemView.findViewById(R.id.imgQuiz);
            txtQuizT = itemView.findViewById(R.id.txtQuizT);
            txtIntentos = itemView.findViewById(R.id.txtIntentos);
            txtMejor = itemView.findViewById(R.id.txtMejor);
            txtUltimo = itemView.findViewById(R.id.txtUltimo);
        }
    }
}