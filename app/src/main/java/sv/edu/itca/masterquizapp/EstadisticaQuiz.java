package sv.edu.itca.masterquizapp;

import com.google.firebase.Timestamp;

public class EstadisticaQuiz {
    private String quizId;
    private String titulo;
    private int intentos;
    private int mejorResultado; // % máximo
    private int ultimoPuntaje; // % del último intento
    private Timestamp fechaUltimoIntento;

    public EstadisticaQuiz() {
    }

    public EstadisticaQuiz(String quizId, String titulo, int intentos, int mejorResultado,
                           int ultimoPuntaje, Timestamp fechaUltimoIntento) {
        this.quizId = quizId;
        this.titulo = titulo;
        this.intentos = intentos;
        this.mejorResultado = mejorResultado;
        this.ultimoPuntaje = ultimoPuntaje;
        this.fechaUltimoIntento = fechaUltimoIntento;
    }

    // Getters y setters
    public String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public int getIntentos() {
        return intentos;
    }

    public void setIntentos(int intentos) {
        this.intentos = intentos;
    }

    public int getMejorResultado() {
        return mejorResultado;
    }

    public void setMejorResultado(int mejorResultado) {
        this.mejorResultado = mejorResultado;
    }

    public int getUltimoPuntaje() {
        return ultimoPuntaje;
    }

    public void setUltimoPuntaje(int ultimoPuntaje) {
        this.ultimoPuntaje = ultimoPuntaje;
    }

    public Timestamp getFechaUltimoIntento() {
        return fechaUltimoIntento;
    }

    public void setFechaUltimoIntento(Timestamp fechaUltimoIntento) {
        this.fechaUltimoIntento = fechaUltimoIntento;
    }
}