package sv.edu.itca.masterquizapp;

public class Pregunta {
    private String enunciado;
    private String correcta;
    private String incorrecta1;
    private String incorrecta2;
    private String incorrecta3;
    private int orden;

    public Pregunta() {}

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
    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

    public String getCorrecta() { return correcta; }
    public void setCorrecta(String correcta) { this.correcta = correcta; }

    public String getIncorrecta1() { return incorrecta1; }
    public void setIncorrecta1(String incorrecta1) { this.incorrecta1 = incorrecta1; }

    public String getIncorrecta2() { return incorrecta2; }
    public void setIncorrecta2(String incorrecta2) { this.incorrecta2 = incorrecta2; }

    public String getIncorrecta3() { return incorrecta3; }
    public void setIncorrecta3(String incorrecta3) { this.incorrecta3 = incorrecta3; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}