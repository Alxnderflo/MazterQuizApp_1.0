package sv.edu.itca.masterquizapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class EstudianteAdapter extends RecyclerView.Adapter<EstudianteAdapter.ViewHolderEstudiante> {

    private final List<Usuario> listaEstudiantes;
    private final Context contexto;
    private final OnEstudianteExpulsarListener expulsarListener;

    // Interface para expulsar estudiante
    public interface OnEstudianteExpulsarListener {
        void onEstudianteExpulsarClick(Usuario estudiante, String estudianteId, int position);
    }

    public EstudianteAdapter(List<Usuario> listaEstudiantes, Context context, OnEstudianteExpulsarListener expulsarListener) {
        this.listaEstudiantes = listaEstudiantes;
        this.contexto = context;
        this.expulsarListener = expulsarListener;
    }

    @NonNull
    @Override
    public ViewHolderEstudiante onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.item_estudiante, parent, false);
        return new ViewHolderEstudiante(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderEstudiante holder, int position) {
        Usuario estudiante = listaEstudiantes.get(position);
        String estudianteId = estudiante.getId();

        // Configurar nombre y email
        holder.txtNombreEstudiante.setText(estudiante.getNombre());
        holder.txtEmailEstudiante.setText(estudiante.getEmail());

        // Configurar clic para expulsar
        holder.btnExpulsarEstudiante.setOnClickListener(v -> {
            if (expulsarListener != null && estudianteId != null) {
                expulsarListener.onEstudianteExpulsarClick(estudiante, estudianteId, position);
            }
        });

        // Opcional: Mostrar información adicional en clic en la card
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(contexto, "Estudiante: " + estudiante.getNombre(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return listaEstudiantes.size();
    }

    public static class ViewHolderEstudiante extends RecyclerView.ViewHolder {
        TextView txtNombreEstudiante;
        TextView txtEmailEstudiante;
        MaterialButton btnExpulsarEstudiante;

        public ViewHolderEstudiante(@NonNull View itemView) {
            super(itemView);
            txtNombreEstudiante = itemView.findViewById(R.id.txtNombreEstudiante);
            txtEmailEstudiante = itemView.findViewById(R.id.txtEmailEstudiante);
            btnExpulsarEstudiante = itemView.findViewById(R.id.btnExpulsarEstudiante);
        }
    }
}
