package com.devst.hidrosmart.data;

/** Punto único para seleccionar la fuente de datos activa de la aplicación. */
public final class HuertoDataSourceProvider {
    private HuertoDataSourceProvider() {}

    public static HuertoDataSource get() {
        return SimuladorRepository.getInstancia();
    }
}
