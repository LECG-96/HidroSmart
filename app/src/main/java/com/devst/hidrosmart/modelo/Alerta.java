package com.devst.hidrosmart.modelo;

import java.util.Objects;

public class Alerta {
    public final String variable;
    public final String nivel;
    public final double valor;
    public final String accion;
    public final boolean automatica;

    public Alerta(String variable, String nivel, double valor, String accion, boolean automatica) {
        this.variable = variable;
        this.nivel = nivel;
        this.valor = valor;
        this.accion = accion;
        this.automatica = automatica;
    }

    public String getClave() {
        return variable + "_" + nivel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alerta)) return false;
        Alerta alerta = (Alerta) o;
        return Objects.equals(variable, alerta.variable) && Objects.equals(nivel, alerta.nivel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variable, nivel);
    }
}