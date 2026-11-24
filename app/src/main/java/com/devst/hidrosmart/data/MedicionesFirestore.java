package com.devst.hidrosmart.data;

import android.util.Log;

import com.devst.hidrosmart.modelo.MedicionMuestra;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class MedicionesFirestore implements HistorialMedicionesDataSource {
    private static final String TAG = "MedicionesFirestore";
    private static final String COLECCION = "mediciones";
    private static final long INTERVALO_GUARDADO_MILLIS = 5L * 60L * 1000L;
    private static final long RETENCION_MILLIS = 24L * 60L * 60L * 1000L;
    private static final long REINTENTO_CARGA_MILLIS = 60L * 1000L;
    private static final long TIMEOUT_SEGUNDOS = 15;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Todas las muestras de las últimas 24 h, por torre. El gráfico lee de aquí.
    private final Map<Integer, List<Punto>> puntos = new HashMap<>();
    private final Map<Integer, Long> ultimaEscritura = new HashMap<>();
    private boolean cargado = false;
    private long ultimoIntentoCarga = 0;

    private static final class Punto {
        final long fecha;
        final String cultivo;
        final Map<String, Double> valores = new HashMap<>();

        Punto(long fecha, String cultivo) {
            this.fecha = fecha;
            this.cultivo = cultivo;
        }
    }

    @Override
    public synchronized void guardarMuestras(List<MedicionMuestra> muestras) {
        if (muestras == null || muestras.isEmpty()) return;
        cargarHistoricoSiHaceFalta();

        // Una torre = un punto con todas sus variables.
        Map<Integer, Punto> porTorre = new HashMap<>();
        for (MedicionMuestra m : muestras) {
            Punto p = porTorre.get(m.torre);
            if (p == null) {
                p = new Punto(m.fechaMillis, m.cultivo);
                porTorre.put(m.torre, p);
            }
            p.valores.put(m.variable, m.valor);
        }

        WriteBatch lote = null;
        for (Map.Entry<Integer, Punto> entrada : porTorre.entrySet()) {
            int torre = entrada.getKey();
            Punto p = entrada.getValue();
            listaDe(torre).add(p);

            Long ultima = ultimaEscritura.get(torre);
            if (ultima == null || p.fecha - ultima >= INTERVALO_GUARDADO_MILLIS) {
                if (lote == null) lote = db.batch();
                lote.set(db.collection(COLECCION).document(), aMapa(torre, p));
                ultimaEscritura.put(torre, p.fecha);
            }
        }
        recortarAntiguos();

        if (lote == null) return;
        try {
            Tasks.await(lote.commit(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.w(TAG, "No se pudieron guardar las mediciones", e);
        }
    }

    @Override
    public synchronized List<MedicionMuestra> obtenerMuestras(int torre, String variable,
                                                              long desdeMillis, int limite) {
        cargarHistoricoSiHaceFalta();
        List<MedicionMuestra> resultado = new ArrayList<>();
        List<Punto> lista = puntos.get(torre);
        if (lista == null) return resultado;

        for (Punto p : lista) {
            if (p.fecha < desdeMillis) continue;
            Double valor = p.valores.get(variable);
            if (valor != null) {
                resultado.add(new MedicionMuestra(torre, p.cultivo, variable, valor, p.fecha));
            }
        }
        int seguro = Math.max(2, Math.min(limite, 3000));
        if (resultado.size() > seguro) {
            resultado = new ArrayList<>(resultado.subList(resultado.size() - seguro, resultado.size()));
        }
        return resultado;
    }

    // ---- Auxiliares ----

    /** Lee Firestore una sola vez (últimas 24 h) y deja los datos en memoria. */
    private void cargarHistoricoSiHaceFalta() {
        if (cargado) return;
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoIntentoCarga < REINTENTO_CARGA_MILLIS) return;
        ultimoIntentoCarga = ahora;
        try {
            QuerySnapshot resultado = Tasks.await(db.collection(COLECCION)
                    .whereGreaterThanOrEqualTo("fechaMillis", ahora - RETENCION_MILLIS)
                    .orderBy("fechaMillis").get(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            for (DocumentSnapshot doc : resultado.getDocuments()) {
                Long torre = doc.getLong("torre");
                Long fecha = doc.getLong("fechaMillis");
                if (torre == null || fecha == null) continue;
                Punto p = new Punto(fecha, doc.getString("cultivo"));
                Object valores = doc.get("valores");
                if (valores instanceof Map) {
                    for (Map.Entry<?, ?> v : ((Map<?, ?>) valores).entrySet()) {
                        if (v.getValue() instanceof Number) {
                            p.valores.put(String.valueOf(v.getKey()),
                                    ((Number) v.getValue()).doubleValue());
                        }
                    }
                }
                listaDe(torre.intValue()).add(p);
                Long previa = ultimaEscritura.get(torre.intValue());
                if (previa == null || fecha > previa) ultimaEscritura.put(torre.intValue(), fecha);
            }
            for (List<Punto> lista : puntos.values()) {
                Collections.sort(lista, (a, b) -> Long.compare(a.fecha, b.fecha));
            }
            cargado = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Sin sesión, sin red o sin permisos: el gráfico sigue funcionando con la memoria.
            Log.w(TAG, "No se pudo cargar el historial de mediciones", e);
        }
    }

    private List<Punto> listaDe(int torre) {
        List<Punto> lista = puntos.get(torre);
        if (lista == null) {
            lista = new ArrayList<>();
            puntos.put(torre, lista);
        }
        return lista;
    }

    private void recortarAntiguos() {
        long limite = System.currentTimeMillis() - RETENCION_MILLIS;
        for (List<Punto> lista : puntos.values()) {
            lista.removeIf(p -> p.fecha < limite);
        }
    }

    private static Map<String, Object> aMapa(int torre, Punto p) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("torre", torre);
        doc.put("cultivo", p.cultivo);
        doc.put("fechaMillis", p.fecha);
        doc.put("valores", new HashMap<String, Object>(p.valores));
        return doc;
    }
}