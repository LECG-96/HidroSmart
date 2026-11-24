package com.devst.hidrosmart.data;

import android.util.Log;

import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Observaciones del operador en Firestore (colección "observaciones").
 * Métodos bloqueantes: se llaman desde el executor del ViewModel, nunca desde el hilo principal.
 */
public final class ComentarioFirestore implements ComentarioLocalDataSource {
    private static final String TAG = "ComentarioFirestore";
    private static final String COLECCION = "observaciones";
    private static final int LIMITE = 200;
    private static final long TIMEOUT_SEGUNDOS = 15;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void guardar(ComentarioUsuario comentario) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("usuario", comentario.usuario);
        doc.put("torre", comentario.torre);
        doc.put("mensaje", comentario.mensaje);
        doc.put("fechaHora", comentario.fechaHora);
        // Texto para mostrar; este número es el que permite ordenar correctamente.
        doc.put("creadoEn", System.currentTimeMillis());
        try {
            Tasks.await(db.collection(COLECCION).add(doc), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación cancelada.", e);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo guardar la observación.", e);
        }
    }

    @Override
    public List<ComentarioUsuario> obtenerTodos() {
        List<ComentarioUsuario> lista = new ArrayList<>();
        try {
            QuerySnapshot resultado = Tasks.await(db.collection(COLECCION)
                    .orderBy("creadoEn", Query.Direction.DESCENDING)
                    .limit(LIMITE).get(), TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            for (DocumentSnapshot doc : resultado.getDocuments()) {
                lista.add(new ComentarioUsuario(
                        doc.getString("usuario"),
                        doc.getString("torre"),
                        doc.getString("mensaje"),
                        doc.getString("fechaHora")));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Sin sesión (modo Demo), sin red o sin permisos: la lista queda vacía.
            Log.w(TAG, "No se pudieron leer las observaciones", e);
        }
        return lista;
    }
}
