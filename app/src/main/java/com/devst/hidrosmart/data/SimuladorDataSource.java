package com.devst.hidrosmart.data;

/** Operaciones exclusivas del modo de simulación y sus controles de diagnóstico. */
public interface SimuladorDataSource extends HuertoDataSource {
    void aplicarEscenario(String escenario);
    void forzarParametro(String variable, double valor, boolean persistente);
    void rellenarEstanque();
    void reiniciarDemo();
    void setCorteDatos(boolean activo);
    boolean isCorteDatos();
}
