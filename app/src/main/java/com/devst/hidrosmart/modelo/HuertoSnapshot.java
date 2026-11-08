package com.devst.hidrosmart.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Vista inmutable de una torre para publicar a la interfaz. */
public final class HuertoSnapshot {
    public final int idx;
    public final PerfilCultivo perfil;
    public final Map<String, Double> v;
    public final Map<String, Boolean> act;
    public final boolean modoManual;
    public final List<Alerta> alertas;

    private HuertoSnapshot(int idx, PerfilCultivo perfil,
                           Map<String, Double> valores,
                           Map<String, Boolean> actuadores,
                           boolean modoManual,
                           List<Alerta> alertas) {
        this.idx = idx;
        this.perfil = perfil;
        this.v = Collections.unmodifiableMap(valores);
        this.act = Collections.unmodifiableMap(actuadores);
        this.modoManual = modoManual;
        this.alertas = Collections.unmodifiableList(alertas);
    }

    public static HuertoSnapshot desde(Huerto huerto) {
        if (huerto == null) return null;
        synchronized (huerto.lock) {
            return new HuertoSnapshot(
                    huerto.idx,
                    huerto.perfil,
                    new HashMap<>(huerto.v),
                    new HashMap<>(huerto.act),
                    huerto.modoManual,
                    new ArrayList<>(huerto.alertas()));
        }
    }
}
