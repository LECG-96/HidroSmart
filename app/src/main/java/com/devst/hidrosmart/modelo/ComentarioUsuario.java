package com.devst.hidrosmart.modelo;

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
}