package sv.edu.itca.masterquizapp;

import java.util.Date;
import java.util.List;

public class Usuario {
    private String id;           // Solo para uso interno - NO se guarda en Firestore
    private String nombre;
    private String email;
    private String rol;
    private String codigoProfesor;
    private List<String> profesoresAgregados;
    private Date fechaRegistro;
    private String proveedor;

    // Constructor vacío necesario para Firestore
    public Usuario() {
    }

    // Getters y setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getCodigoProfesor() {
        return codigoProfesor;
    }

    public void setCodigoProfesor(String codigoProfesor) {
        this.codigoProfesor = codigoProfesor;
    }

    public List<String> getProfesoresAgregados() {
        return profesoresAgregados;
    }

    public void setProfesoresAgregados(List<String> profesoresAgregados) {
        this.profesoresAgregados = profesoresAgregados;
    }

    public Date getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Date fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }
}