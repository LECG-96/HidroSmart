package com.devst.hidrosmart.data;

import android.content.Context;

/** Punto único para seleccionar la fuente de datos activa de la aplicación. */
public final class HuertoDataSourceProvider {
    private static ComentarioDatabase almacenamientoLocal;
    private static GestionUsuariosDataSource usuarios;

    private HuertoDataSourceProvider() {}

    public static HuertoDataSource get() {
        return SimuladorRepository.getInstancia();
    }

    public static synchronized GestionUsuariosDataSource getAutenticacion(Context context) {
        if (usuarios == null) {
            usuarios = new UsuarioDatabase(context.getApplicationContext());
        }
        return usuarios;
    }

    public static synchronized ComentarioLocalDataSource getComentarios(Context context) {
        return getAlmacenamientoLocal(context);
    }

    public static synchronized HistorialLocalDataSource getHistorial(Context context) {
        return getAlmacenamientoLocal(context);
    }

    public static synchronized HistorialMedicionesDataSource getHistorialMediciones(Context context) {
        return getAlmacenamientoLocal(context);
    }

    private static ComentarioDatabase getAlmacenamientoLocal(Context context) {
        if (almacenamientoLocal == null) {
            almacenamientoLocal = new ComentarioDatabase(context.getApplicationContext());
        }
        return almacenamientoLocal;
    }
}
