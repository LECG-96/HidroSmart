package com.devst.hidrosmart.data;

import android.content.Context;

/** Punto único para seleccionar la fuente de datos activa de la aplicación. */
public final class HuertoDataSourceProvider {
    private static ComentarioLocalDataSource comentarios;
    private static final AutenticacionDataSource autenticacion = new AutenticacionLocalDataSource();

    private HuertoDataSourceProvider() {}

    public static HuertoDataSource get() {
        return SimuladorRepository.getInstancia();
    }

    public static AutenticacionDataSource getAutenticacion() {
        return autenticacion;
    }

    public static synchronized ComentarioLocalDataSource getComentarios(Context context) {
        if (comentarios == null) {
            comentarios = new ComentarioDatabase(context.getApplicationContext());
        }
        return comentarios;
    }
}
