package com.devst.hidrosmart.modelo;

/** Lectura muestreada para mostrar tendencias locales en la pantalla de historial. */
public final class MedicionMuestra {
    public final int torre;
    public final String cultivo;
    public final String variable;
    public final double valor;
    public final long fechaMillis;

    public MedicionMuestra(int torre, String cultivo, String variable, double valor, long fechaMillis) {
        this.torre = torre;
        this.cultivo = cultivo;
        this.variable = variable;
        this.valor = valor;
        this.fechaMillis = fechaMillis;
    }
}
