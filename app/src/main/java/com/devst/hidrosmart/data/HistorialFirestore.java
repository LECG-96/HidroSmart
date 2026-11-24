package com.devst.hidrosmart.data;
import android.util.Log;

import com.devst.hidrosmart.modelo.EventoAlerta;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class HistorialFirestore implements HistorialLocalDataSource {
    private static final String TAG = "HistorialFirestore";
    private static final String COLECCION = "eventos_alerta";
    private static final int LIMITE_LECTURA = 100;
    private static final long TIMEOUT_SEGUNDOS = 15;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Memoria: evita leer Firestore en cada sincronización.
    private final Map<String, Activa> activas = new HashMap<>();
    private boolean activasCargadas = false;
    private List<EventoAlerta> cacheEventos = null;

    private static final class Activa {
        final String docId;
        final EventoAlerta evento;

        Activa(String docId, EventoAlerta evento) {
            this.docId = docId;
            this.evento = evento;
        }
    }

    @Override
    public synchronized void sincronizarAlertas(List<EventoAlerta> alertasActuales) {
        try {
            cargarActivasSiHaceFalta();
            Map<String, EventoAlerta> nuevas = new HashMap<>();
            for (EventoAlerta alerta : alertasActuales) nuevas.put(alerta.clave(), alerta);

            WriteBatch lote = db.batch();
            Map<String, Activa> resultado = new HashMap<>(activas);
            String ahoraTexto = EventoAlerta.ahora();
            long marca = System.currentTimeMillis();
            boolean hayCambios = false;

            // Alertas que ya no están: se cierran y se registra el evento RESUELTA.
            for (Map.Entry<String, Activa> entrada : activas.entrySet()) {
                if (nuevas.containsKey(entrada.getKey())) continue;
                EventoAlerta previa = entrada.getValue().evento;
                lote.update(db.collection(COLECCION).document(entrada.getValue().docId),
                        "activa", false);
                lote.set(db.collection(COLECCION).document(),
                        aMapa(previa, EventoAlerta.RESUELTA, false, ahoraTexto, marca++));
                resultado.remove(entrada.getKey());
                hayCambios = true;
            }

            // Alertas nuevas: se crea el evento ACTIVA.
            for (Map.Entry<String, EventoAlerta> entrada : nuevas.entrySet()) {
                if (activas.containsKey(entrada.getKey())) continue;
                EventoAlerta a = entrada.getValue();
                DocumentReference ref = db.collection(COLECCION).document();
                lote.set(ref, aMapa(a, EventoAlerta.ACTIVA, true, ahoraTexto, marca));
                resultado.put(entrada.getKey(), new Activa(ref.getId(), new EventoAlerta(
                        marca, a.torre, a.cultivo, a.variable, a.nivel, a.valor, a.accion,
                        EventoAlerta.ACTIVA, ahoraTexto, false)));
                marca++;
                hayCambios = true;
            }

            if (!hayCambios) return;
            Tasks.await(lote.commit(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            activas.clear();
            activas.putAll(resultado);
            cacheEventos = null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.w(TAG, "No se pudo sincronizar las alertas", e);
            activasCargadas = false;
            cacheEventos = null;
        }
    }

    @Override
    public synchronized List<EventoAlerta> obtenerEventos(int limite) {
        int seguro = Math.max(1, Math.min(limite, LIMITE_LECTURA));
        if (cacheEventos == null) {
            try {
                QuerySnapshot resultado = Tasks.await(db.collection(COLECCION)
                        .orderBy("creadoEn", Query.Direction.DESCENDING)
                        .limit(LIMITE_LECTURA).get(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
                List<EventoAlerta> lista = new ArrayList<>();
                for (DocumentSnapshot doc : resultado.getDocuments()) lista.add(aEvento(doc));
                cacheEventos = lista;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new ArrayList<>();
            } catch (Exception e) {
                Log.w(TAG, "No se pudo leer el historial", e);
                return new ArrayList<>();
            }
        }
        return new ArrayList<>(cacheEventos.subList(0, Math.min(seguro, cacheEventos.size())));
    }

    @Override
    public synchronized List<EventoAlerta> obtenerAlertasActivas() {
        try {
            cargarActivasSiHaceFalta();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        } catch (Exception e) {
            Log.w(TAG, "No se pudieron leer las alertas activas", e);
            return new ArrayList<>();
        }
        List<EventoAlerta> lista = new ArrayList<>();
        for (Activa activa : activas.values()) lista.add(activa.evento);
        Collections.sort(lista, (x, y) -> Long.compare(y.id, x.id));
        return lista;
    }

    @Override
    public synchronized void marcarAlertasActivasAtendidas() {
        try {
            cargarActivasSiHaceFalta();
            if (activas.isEmpty()) return;
            WriteBatch lote = db.batch();
            for (Activa activa : activas.values()) {
                lote.update(db.collection(COLECCION).document(activa.docId), "atendida", true);
            }
            Tasks.await(lote.commit(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            Map<String, Activa> actualizadas = new HashMap<>();
            for (Map.Entry<String, Activa> e : activas.entrySet()) {
                EventoAlerta v = e.getValue().evento;
                actualizadas.put(e.getKey(), new Activa(e.getValue().docId, new EventoAlerta(
                        v.id, v.torre, v.cultivo, v.variable, v.nivel, v.valor, v.accion,
                        v.tipo, v.fechaHora, true)));
            }
            activas.clear();
            activas.putAll(actualizadas);
            cacheEventos = null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.w(TAG, "No se pudieron marcar las alertas como atendidas", e);
            activasCargadas = false;
        }
    }

    // ---- Auxiliares ----

    private void cargarActivasSiHaceFalta() throws Exception {
        if (activasCargadas) return;
        QuerySnapshot resultado = Tasks.await(db.collection(COLECCION)
                .whereEqualTo("activa", true).get(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        activas.clear();
        for (DocumentSnapshot doc : resultado.getDocuments()) {
            String clave = doc.getString("clave");
            if (clave != null) activas.put(clave, new Activa(doc.getId(), aEvento(doc)));
        }
        activasCargadas = true;
    }

    private static Map<String, Object> aMapa(EventoAlerta a, String tipo, boolean activa,
                                             String fechaHora, long creadoEn) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("torre", a.torre);
        doc.put("cultivo", a.cultivo);
        doc.put("clave", a.clave());
        doc.put("variable", a.variable);
        doc.put("nivel", a.nivel);
        doc.put("valor", a.valor);
        doc.put("accion", a.accion);
        doc.put("tipo", tipo);
        doc.put("fechaHora", fechaHora);
        doc.put("atendida", false);
        doc.put("activa", activa);
        doc.put("creadoEn", creadoEn);
        return doc;
    }

    private static EventoAlerta aEvento(DocumentSnapshot doc) {
        Long torre = doc.getLong("torre");
        Double valor = doc.getDouble("valor");
        Long creadoEn = doc.getLong("creadoEn");
        return new EventoAlerta(
                creadoEn == null ? 0L : creadoEn,
                torre == null ? 0 : torre.intValue(),
                doc.getString("cultivo"), doc.getString("variable"), doc.getString("nivel"),
                valor == null ? 0d : valor,
                doc.getString("accion"), doc.getString("tipo"), doc.getString("fechaHora"),
                Boolean.TRUE.equals(doc.getBoolean("atendida")));
    }
}