package sv.edu.itca.masterquizapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// CAMBIO-QUIZ: Adaptador para el RecyclerView de resultados
public class ResultadosAdapter extends RecyclerView.Adapter<ResultadosAdapter.ViewHolder> {
    private List<ResultadoPregunta> resultados;

    public ResultadosAdapter(List<ResultadoPregunta> resultados) {
        this.resultados = resultados;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ResultadoPregunta resultado = resultados.get(position);
        holder.bind(resultado);
    }

    @Override
    public int getItemCount() {
        return resultados.size();
    }

    // CAMBIO-QUIZ: ViewHolder para cada item de resultado
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvEnunciado, tvRespuestaCorrecta, tvRespuestaIncorrecta;
        private CardView cardRespuestaCorrecta, cardRespuestaIncorrecta;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEnunciado = itemView.findViewById(R.id.tvResultEnunciado);
            tvRespuestaCorrecta = itemView.findViewById(R.id.tvRespuestaCorrecta);
            tvRespuestaIncorrecta = itemView.findViewById(R.id.tvRespuestaIncorrecta);
            cardRespuestaCorrecta = itemView.findViewById(R.id.cardRespuestaCorrecta);
            cardRespuestaIncorrecta = itemView.findViewById(R.id.cardRespuestaIncorrecta);
        }

        // CAMBIO-QUIZ: Metodo para enlazar datos con las views
        public void bind(ResultadoPregunta resultado) {
            // Configurar el enunciado de la pregunta
            tvEnunciado.setText(resultado.getNumeroPregunta() + ". " + resultado.getEnunciado());

            // Configurar la respuesta correcta (siempre visible)
            String textoCorrecta = "Correcta: \"" + resultado.getRespuestaCorrecta() + "\"";
            tvRespuestaCorrecta.setText(textoCorrecta);

            // CAMBIO-QUIZ: Configurar la respuesta incorrecta del usuario (solo si falló)
            if (resultado.isEsCorrecta()) {
                // Si es correcta, ocultar la tarjeta de respuesta incorrecta
                cardRespuestaIncorrecta.setVisibility(View.GONE);

                // CAMBIO-QUIZ: Opcional - agregar ícono de check si es correcta
                tvEnunciado.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ico_check, 0);
                tvEnunciado.setCompoundDrawablePadding(16);
            } else {
                // Si es incorrecta, mostrar la respuesta incorrecta del usuario
                cardRespuestaIncorrecta.setVisibility(View.VISIBLE);
                String textoIncorrecta = "Tu respuesta: \"" + resultado.getRespuestaUsuario() + "\"";
                tvRespuestaIncorrecta.setText(textoIncorrecta);

                // CAMBIO-QUIZ: Opcional - agregar ícono de cruz si es incorrecta
                tvEnunciado.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ico_close, 0);
                tvEnunciado.setCompoundDrawablePadding(16);
            }
        }
    }

    // CAMBIO-QUIZ: Metodo para actualizar datos si es necesario
    public void actualizarResultados(List<ResultadoPregunta> nuevosResultados) {
        this.resultados = nuevosResultados;
        notifyDataSetChanged();
    }
}