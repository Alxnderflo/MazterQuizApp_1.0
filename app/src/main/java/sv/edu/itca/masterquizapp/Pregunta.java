package sv.edu.itca.masterquizapp;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Pregunta implements Parcelable {
    protected Pregunta(Parcel in) {
        enunciado = in.readString();
        correcta = in.readString();
        incorrecta1 = in.readString();
        incorrecta2 = in.readString();
        incorrecta3 = in.readString();
        orden = in.readInt();
    }

    private String enunciado;
    private String correcta;
    private String incorrecta1;
    private String incorrecta2;
    private String incorrecta3;
    private int orden;

    public Pregunta() {
    }

    public Pregunta(String enunciado, String correcta, String incorrecta1,
                    String incorrecta2, String incorrecta3, int orden) {
        this.enunciado = enunciado;
        this.correcta = correcta;
        this.incorrecta1 = incorrecta1;
        this.incorrecta2 = incorrecta2;
        this.incorrecta3 = incorrecta3;
        this.orden = orden;
    }

    // Getters y setters
    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public String getCorrecta() {
        return correcta;
    }

    public void setCorrecta(String correcta) {
        this.correcta = correcta;
    }

    public String getIncorrecta1() {
        return incorrecta1;
    }

    public void setIncorrecta1(String incorrecta1) {
        this.incorrecta1 = incorrecta1;
    }

    public String getIncorrecta2() {
        return incorrecta2;
    }

    public void setIncorrecta2(String incorrecta2) {
        this.incorrecta2 = incorrecta2;
    }

    public String getIncorrecta3() {
        return incorrecta3;
    }

    public void setIncorrecta3(String incorrecta3) {
        this.incorrecta3 = incorrecta3;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    // CAMBIO: Metodo para obtener opciones mezcladas
    public Map<String, String> getOpcionesMezcladas() {
        Map<String, String> opciones = new LinkedHashMap<>(); // LinkedHashMap mantiene el orden
        List<String> todasLasOpciones = new ArrayList<>();

        // Agregar todas las opciones a la lista
        todasLasOpciones.add(correcta);
        todasLasOpciones.add(incorrecta1);
        todasLasOpciones.add(incorrecta2);
        todasLasOpciones.add(incorrecta3);

        // Mezclar las opciones aleatoriamente
        Collections.shuffle(todasLasOpciones);

        // Asignar letras a las opciones mezcladas
        char letra = 'A';
        for (String opcion : todasLasOpciones) {
            opciones.put(String.valueOf(letra), opcion);
            letra++;
        }

        return opciones;
    }

    // CAMBIO: Metodo para obtener la letra de la respuesta correcta después de mezclar
    public String obtenerLetraCorrecta(Map<String, String> opcionesMezcladas) {
        for (Map.Entry<String, String> entry : opcionesMezcladas.entrySet()) {
            if (entry.getValue().equals(correcta)) {
                return entry.getKey();
            }
        }
        return null; // Esto no debería pasar
    }// CAMBIO-QUIZ: Métodos para Parcelable

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(enunciado);
        dest.writeString(correcta);
        dest.writeString(incorrecta1);
        dest.writeString(incorrecta2);
        dest.writeString(incorrecta3);
        dest.writeInt(orden);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Pregunta> CREATOR = new Creator<Pregunta>() {
        @Override
        public Pregunta createFromParcel(Parcel in) {
            return new Pregunta(in);
        }

        @Override
        public Pregunta[] newArray(int size) {
            return new Pregunta[size];
        }
    };
}