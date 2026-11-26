package com.devst.hidrosmart.modelo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Evento persistido de activación, resolución o atención de una alerta. */
public final class EventoAlerta {
    public static final String ACTIVA = "ACTIVA";
    public static final String RESUELTA = "RESUELTA";

    public final long id;
    public final int torre;
    public final String cultivo;
    public final String variable;
    public final String nivel;
    public final double valor;
    public final String accion;
    public final String tipo;
    public final String fechaHora;
    public final boolean atendida;

    public EventoAlerta(long id, int torre, String cultivo, String variable, String nivel,
                        double valor, String accion, String tipo, String fechaHora, boolean atendida) {
        this.id = id;
        this.torre = torre;
        this.cultivo = cultivo;
        this.variable = variable;
        this.nivel = nivel;
        this.valor = valor;
        this.accion = accion;
        this.tipo = tipo;
        this.fechaHora = fechaHora;
        this.atendida = atendida;
    }

    public String clave() {
        return torre + "|" + cultivo + "|" + variable + "|" + nivel;
    }

    public static String ahora() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}
