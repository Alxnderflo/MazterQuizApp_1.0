package sv.edu.itca.masterquizapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PreguntasAdapter extends RecyclerView.Adapter<PreguntasAdapter.ViewHolder> {
    private List<Pregunta> listaPreguntas;
    private List<String> listaPreguntasIds; //lista de IDs
    private OnPreguntaClickListener onPreguntaClickListener;
    private OnPreguntaDeleteListener onPreguntaDeleteListener;

    // CAMBIO: Interfaces para los clics
    public interface OnPreguntaClickListener {
        void onPreguntaClick(Pregunta pregunta, String preguntaId, int position);
    }

    public interface OnPreguntaDeleteListener {
        void onPreguntaDeleteClick(Pregunta pregunta, String preguntaId, int position);
    }

    public PreguntasAdapter(List<Pregunta> listaPreguntas, OnPreguntaClickListener onPreguntaClickListener, OnPreguntaDeleteListener onPreguntaDeleteListener) {
        this.listaPreguntas = listaPreguntas;
        this.onPreguntaClickListener = onPreguntaClickListener;
        this.onPreguntaDeleteListener = onPreguntaDeleteListener;
        this.listaPreguntasIds = new ArrayList<>();
    }

    // CAMBIO: Metodo para actualizar los IDs
    public void actualizarIds(List<String> ids) {
        this.listaPreguntasIds.clear();
        this.listaPreguntasIds.addAll(ids);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pregunta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pregunta pregunta = listaPreguntas.get(position);
        String preguntaId = position < listaPreguntasIds.size() ? listaPreguntasIds.get(position) : null;

        holder.tvNumPregunta.setText(String.valueOf(pregunta.getOrden()) + ".");
        holder.tvEnunciado.setText(pregunta.getEnunciado());
        holder.tvCorrecta.setText("R/ " + pregunta.getCorrecta());

        //Configurar el clic en el item (para editar)
        holder.itemView.setOnClickListener(v -> {
            if (onPreguntaClickListener != null && preguntaId != null) {
                onPreguntaClickListener.onPreguntaClick(pregunta, preguntaId, position);
            }
        });

        //Configurar el clic en el botón de eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            if (onPreguntaDeleteListener != null && preguntaId != null) {
                onPreguntaDeleteListener.onPreguntaDeleteClick(pregunta, preguntaId, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaPreguntas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumPregunta, tvEnunciado, tvCorrecta;
        ImageButton btnEliminar; //referencia al botón de eliminar

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumPregunta = itemView.findViewById(R.id.tvNumPregunta);
            tvEnunciado = itemView.findViewById(R.id.tvEnunciado);
            tvCorrecta = itemView.findViewById(R.id.tvCorrecta);
            btnEliminar = itemView.findViewById(R.id.btnElimP); //Inicializar el botón
        }
    }
}