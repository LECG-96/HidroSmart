package com.devst.hidrosmart.modelo;

public class RespuestaComando {
    public final boolean aceptado;
    public final String mensaje;

    public RespuestaComando(boolean aceptado, String mensaje) {
        this.aceptado = aceptado;
        this.mensaje = mensaje;
    }
}