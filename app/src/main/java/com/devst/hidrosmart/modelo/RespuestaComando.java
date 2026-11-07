package com.devst.hidrosmart.modelo;

public class RespuestaComando {
    public enum CodigoMensaje {
        GENERICO,
        SIN_TORRE_ACTIVA,
        MODO_AUTOMATICO,
        COMANDO_DESCONOCIDO,
        NIVEL_AGUA_CRITICO
    }

    public final boolean aceptado;
    public final String mensaje;
    public final CodigoMensaje codigoMensaje;

    public RespuestaComando(boolean aceptado, String mensaje) {
        this(aceptado, mensaje, CodigoMensaje.GENERICO);
    }

    public RespuestaComando(boolean aceptado, String mensaje, CodigoMensaje codigoMensaje) {
        this.aceptado = aceptado;
        this.mensaje = mensaje;
        this.codigoMensaje = codigoMensaje;
    }
}
