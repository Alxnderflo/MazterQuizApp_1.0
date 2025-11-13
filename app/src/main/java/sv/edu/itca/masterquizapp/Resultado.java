package sv.edu.itca.masterquizapp;

import com.google.firebase.Timestamp;

public class Resultado {
    private String id;
    private String userId;
    private String quizId;
    private String quizTitulo;
    private int puntuacion; // Porcentaje 0-100
    private Timestamp fecha;
    private int totalPreguntas;
    private int respuestasCorrectas;

    // Constructor vacío necesario para Firestore
    public Resultado() {
    }

    // Constructor sin ID (Firestore lo generará automáticamente)
    public Resultado(String userId, String quizId, String quizTitulo, int puntuacion, Timestamp fecha, int totalPreguntas, int respuestasCorrectas) {
        this.userId = userId;
        this.quizId = quizId;
        this.quizTitulo = quizTitulo;
        this.puntuacion = puntuacion;
        this.fecha = fecha;
        this.totalPreguntas = totalPreguntas;
        this.respuestasCorrectas = respuestasCorrectas;
    }

    // Getters y setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitulo() {
        return quizTitulo;
    }

    public void setQuizTitulo(String quizTitulo) {
        this.quizTitulo = quizTitulo;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    public Timestamp getFecha() {
        return fecha;
    }

    public void setFecha(Timestamp fecha) {
        this.fecha = fecha;
    }

    public int getTotalPreguntas() {
        return totalPreguntas;
    }

    public void setTotalPreguntas(int totalPreguntas) {
        this.totalPreguntas = totalPreguntas;
    }

    public int getRespuestasCorrectas() {
        return respuestasCorrectas;
    }

    public void setRespuestasCorrectas(int respuestasCorrectas) {
        this.respuestasCorrectas = respuestasCorrectas;
    }
}