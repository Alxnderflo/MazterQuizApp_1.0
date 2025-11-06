package sv.edu.itca.masterquizapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProfesoresAdapter extends RecyclerView.Adapter<ProfesoresAdapter.ViewHolderProfesor> {

    private final List<Usuario> listaProfesores;
    private final int[] coloresProfesores;
    private final Context contexto;

    public ProfesoresAdapter(List<Usuario> listaProfesores, int[] coloresProfesores, Context context) {
        this.listaProfesores = listaProfesores;
        this.coloresProfesores = coloresProfesores;
        this.contexto = context;
    }

    @NonNull
    @Override
    public ViewHolderProfesor onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.item_profesor, parent, false);
        return new ViewHolderProfesor(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderProfesor holder, int position) {
        Usuario profesor = listaProfesores.get(position);

        // Configurar nombre
        holder.txtNombreProfesor.setText(profesor.getNombre());

        // Asignar color al ícono del profesor
        int colorIndex = position % coloresProfesores.length;
        holder.iconoProfesor.setColorFilter(coloresProfesores[colorIndex]);

        // Opcional: Mostrar email en tooltip
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(contexto, profesor.getEmail(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return listaProfesores.size();
    }

    public static class ViewHolderProfesor extends RecyclerView.ViewHolder {
        ImageView iconoProfesor;
        TextView txtNombreProfesor;

        public ViewHolderProfesor(@NonNull View itemView) {
            super(itemView);
            iconoProfesor = itemView.findViewById(R.id.iconoProfesor);
            txtNombreProfesor = itemView.findViewById(R.id.txtNombreProfesor);
        }
    }
}