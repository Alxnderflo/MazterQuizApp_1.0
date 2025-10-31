package sv.edu.itca.masterquizapp;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ResultadoPregunta implements Parcelable {
    // CAMBIO-QUIZ: Constructor para Parcelable
    protected ResultadoPregunta(Parcel in) {
        numeroPregunta = in.readInt();
        enunciado = in.readString();
        respuestaCorrecta = in.readString();
        respuestaUsuario = in.readString();
        esCorrecta = in.readByte() != 0;
    }

    private int numeroPregunta;
    private String enunciado;
    private String respuestaCorrecta;
    private String respuestaUsuario;
    private boolean esCorrecta;

    // Constructor vacío necesario para Firebase
    public ResultadoPregunta() {
    }

    // Constructor completo
    public ResultadoPregunta(int numeroPregunta, String enunciado, String respuestaCorrecta,
                             String respuestaUsuario, boolean esCorrecta) {
        this.numeroPregunta = numeroPregunta;
        this.enunciado = enunciado;
        this.respuestaCorrecta = respuestaCorrecta;
        this.respuestaUsuario = respuestaUsuario;
        this.esCorrecta = esCorrecta;
    }

    // Getters y setters
    public int getNumeroPregunta() {
        return numeroPregunta;
    }

    public void setNumeroPregunta(int numeroPregunta) {
        this.numeroPregunta = numeroPregunta;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public String getRespuestaCorrecta() {
        return respuestaCorrecta;
    }

    public void setRespuestaCorrecta(String respuestaCorrecta) {
        this.respuestaCorrecta = respuestaCorrecta;
    }

    public String getRespuestaUsuario() {
        return respuestaUsuario;
    }

    public void setRespuestaUsuario(String respuestaUsuario) {
        this.respuestaUsuario = respuestaUsuario;
    }

    public boolean isEsCorrecta() {
        return esCorrecta;
    }

    public void setEsCorrecta(boolean esCorrecta) {
        this.esCorrecta = esCorrecta;
    }

    // Metodo auxiliar para obtener el texto del resultado
    public String getTextoResultado() {
        return esCorrecta ? "Correcta" : "Incorrecta";
    }

    // CAMBIO-QUIZ: Métodos para Parcelable
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(numeroPregunta);
        dest.writeString(enunciado);
        dest.writeString(respuestaCorrecta);
        dest.writeString(respuestaUsuario);
        dest.writeByte((byte) (esCorrecta ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ResultadoPregunta> CREATOR = new Creator<ResultadoPregunta>() {
        @Override
        public ResultadoPregunta createFromParcel(Parcel in) {
            return new ResultadoPregunta(in);
        }

        @Override
        public ResultadoPregunta[] newArray(int size) {
            return new ResultadoPregunta[size];
        }
    };
}