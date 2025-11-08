package sv.edu.itca.masterquizapp;

import java.util.Date;

public class Quiz {
    private String titulo;
    private String descripcion;
    private String imagenUrl;
    private Date fechaCreacion;
    private int numPreguntas;
    private String userId;
    private String userNombre;
    private String userRol;
    private boolean esPublico; // NUEVO CAMPO PARA ROLES

    public Quiz() {
    }

    public Quiz(String titulo, String descripcion, String imagenUrl, String userId, String userNombre, String userRol) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.imagenUrl = imagenUrl;
        this.userId = userId;
        this.userNombre = userNombre;
        this.userRol = userRol;
        this.fechaCreacion = new Date();
        this.numPreguntas = 0;
        this.esPublico = true; // NUEVO: Quiz público por defecto
    }

    // NUEVO CONSTRUCTOR (agregar este)
    public Quiz(String titulo, String descripcion, String imagenUrl, String userId, String userNombre, String userRol, boolean esPublico) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.imagenUrl = imagenUrl;
        this.userId = userId;
        this.userNombre = userNombre;
        this.userRol = userRol;
        this.fechaCreacion = new Date();
        this.numPreguntas = 0;
        this.esPublico = esPublico; // ← Usa el valor que pasamos
    }

    // Getters y setters
    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public int getNumPreguntas() {
        return numPreguntas;
    }

    public void setNumPreguntas(int numPreguntas) {
        this.numPreguntas = numPreguntas;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserNombre() {
        return userNombre;
    }

    public void setUserNombre(String userNombre) {
        this.userNombre = userNombre;
    }

    public String getUserRol() {
        return userRol;
    }

    public void setUserRol(String userRol) {
        this.userRol = userRol;
    }

    public boolean isEsPublico() {
        return esPublico;
    }

    public void setEsPublico(boolean esPublico) {
        this.esPublico = esPublico;
    }
}