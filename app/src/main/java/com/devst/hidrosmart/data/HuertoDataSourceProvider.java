package com.devst.hidrosmart.data;

import android.content.Context;

/** Punto único para seleccionar la fuente de datos activa de la aplicación. */
public final class HuertoDataSourceProvider {
    private static GestionUsuariosDataSource usuarios;
    private static ComentarioLocalDataSource comentarios;
    private static HistorialLocalDataSource historial;
    private static HistorialMedicionesDataSource mediciones;

    private HuertoDataSourceProvider() {}

    public static HuertoDataSource get() {
        return SimuladorRepository.getInstancia();
    }

    public static synchronized GestionUsuariosDataSource getAutenticacion(Context context) {
        if (usuarios == null) {
            usuarios = new UsuarioFirestore(context.getApplicationContext());
        }
        return usuarios;
    }

    public static synchronized ComentarioLocalDataSource getComentarios(Context context) {
        if (comentarios == null) {
            comentarios = new ComentarioFirestore();
        }
        return comentarios;
    }

    public static synchronized HistorialLocalDataSource getHistorial(Context context) {
        if (historial == null) {
            historial = new HistorialFirestore();
        }
        return historial;
    }

    public static synchronized HistorialMedicionesDataSource getHistorialMediciones(Context context) {
        if (mediciones == null) {
            mediciones = new MedicionesFirestore();
        }
        return mediciones;
    }
}