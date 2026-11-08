package com.devst.hidrosmart.modelo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ComentarioUsuario {
    public final String usuario;
    public final String torre;
    public final String mensaje;
    public final String fechaHora;

    public ComentarioUsuario(String usuario, String torre, String mensaje, String fechaHora) {
        this.usuario = usuario;
        this.torre = torre;
        this.mensaje = mensaje;
        this.fechaHora = fechaHora;
    }

    public ComentarioUsuario(String usuario, String torre, String mensaje) {
        this(usuario, torre, mensaje, new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
    }
}
