package com.devst.hidrosmart.data;

import android.content.Context;

/** Punto único para seleccionar la fuente de datos activa de la aplicación. */
public final class HuertoDataSourceProvider {
    private static ComentarioLocalDataSource comentarios;
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
        if (comentarios == null) {
            comentarios = new ComentarioDatabase(context.getApplicationContext());
        }
        return comentarios;
    }
}
