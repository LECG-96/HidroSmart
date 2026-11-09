package com.devst.hidrosmart.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.modelo.EventoAlerta;
import com.devst.hidrosmart.modelo.MedicionMuestra;

import java.util.ArrayList;
import java.util.List;

/** Almacenamiento local de observaciones del operador. */
public class ComentarioDatabase extends SQLiteOpenHelper implements ComentarioLocalDataSource,
        HistorialLocalDataSource, HistorialMedicionesDataSource {
    private static final String DATABASE_NAME = "hidrosmart_observaciones.db";
    private static final int DATABASE_VERSION = 3;
    private static final String TABLE_OBSERVACIONES = "observaciones";
    private static final String TABLE_EVENTOS = "eventos_alerta";
    private static final String TABLE_ACTIVAS = "alertas_activas";
    private static final String TABLE_MUESTRAS = "mediciones_historial";

    public ComentarioDatabase(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_OBSERVACIONES + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "usuario TEXT NOT NULL, "
                + "torre TEXT NOT NULL, "
                + "mensaje TEXT NOT NULL, "
                + "fecha_hora TEXT NOT NULL)");
        crearTablasHistorial(db);
        crearTablaMuestras(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) crearTablasHistorial(db);
        if (oldVersion < 3) crearTablaMuestras(db);
    }

    private void crearTablasHistorial(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EVENTOS + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "torre INTEGER NOT NULL, cultivo TEXT NOT NULL, clave TEXT NOT NULL, "
                + "variable TEXT NOT NULL, nivel TEXT NOT NULL, valor REAL NOT NULL, "
                + "accion TEXT NOT NULL, tipo TEXT NOT NULL, fecha_hora TEXT NOT NULL, "
                + "atendida INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_eventos_fecha ON " + TABLE_EVENTOS + "(_id DESC)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ACTIVAS + " ("
                + "clave TEXT PRIMARY KEY, evento_id INTEGER NOT NULL)");
    }

    private void crearTablaMuestras(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MUESTRAS + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, torre INTEGER NOT NULL, "
                + "cultivo TEXT NOT NULL, variable TEXT NOT NULL, valor REAL NOT NULL, "
                + "fecha_millis INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_muestras_busqueda ON " + TABLE_MUESTRAS
                + "(torre, variable, fecha_millis)");
    }

    @Override
    public void guardar(ComentarioUsuario comentario) {
        ContentValues valores = new ContentValues();
        valores.put("usuario", comentario.usuario);
        valores.put("torre", comentario.torre);
        valores.put("mensaje", comentario.mensaje);
        valores.put("fecha_hora", comentario.fechaHora);
        getWritableDatabase().insertOrThrow(TABLE_OBSERVACIONES, null, valores);
    }

    @Override
    public List<ComentarioUsuario> obtenerTodos() {
        List<ComentarioUsuario> comentarios = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_OBSERVACIONES,
                new String[]{"usuario", "torre", "mensaje", "fecha_hora"},
                null, null, null, null, "_id DESC")) {
            while (cursor.moveToNext()) {
                comentarios.add(new ComentarioUsuario(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3)));
            }
        }
        return comentarios;
    }

    @Override
    public void sincronizarAlertas(List<EventoAlerta> alertasActuales) {
        SQLiteDatabase db = getWritableDatabase();
        java.util.Map<String, EventoAlerta> nuevas = new java.util.HashMap<>();
        for (EventoAlerta alerta : alertasActuales) nuevas.put(alerta.clave(), alerta);

        db.beginTransaction();
        try {
            java.util.Map<String, EventoAlerta> almacenadas = new java.util.HashMap<>();
            String consulta = "SELECT a.clave, e._id, e.torre, e.cultivo, e.variable, e.nivel, e.valor, "
                    + "e.accion, e.fecha_hora, e.atendida FROM " + TABLE_ACTIVAS + " a "
                    + "JOIN " + TABLE_EVENTOS + " e ON e._id = a.evento_id";
            try (Cursor cursor = db.rawQuery(consulta, null)) {
                while (cursor.moveToNext()) {
                    EventoAlerta alerta = new EventoAlerta(cursor.getLong(1), cursor.getInt(2), cursor.getString(3),
                            cursor.getString(4), cursor.getString(5), cursor.getDouble(6),
                            cursor.getString(7), EventoAlerta.ACTIVA, cursor.getString(8), cursor.getInt(9) == 1);
                    almacenadas.put(cursor.getString(0), alerta);
                }
            }

            for (java.util.Map.Entry<String, EventoAlerta> anterior : almacenadas.entrySet()) {
                if (!nuevas.containsKey(anterior.getKey())) {
                    insertarEvento(db, anterior.getValue(), EventoAlerta.RESUELTA, false);
                    db.delete(TABLE_ACTIVAS, "clave = ?", new String[]{anterior.getKey()});
                }
            }

            for (java.util.Map.Entry<String, EventoAlerta> actual : nuevas.entrySet()) {
                if (!almacenadas.containsKey(actual.getKey())) {
                    long id = insertarEvento(db, actual.getValue(), EventoAlerta.ACTIVA, false);
                    ContentValues activa = new ContentValues();
                    activa.put("clave", actual.getKey());
                    activa.put("evento_id", id);
                    db.insertOrThrow(TABLE_ACTIVAS, null, activa);
                } else {
                    EventoAlerta vigente = actual.getValue();
                    ContentValues valores = new ContentValues();
                    valores.put("cultivo", vigente.cultivo);
                    valores.put("valor", vigente.valor);
                    valores.put("accion", vigente.accion);
                    db.update(TABLE_EVENTOS, valores, "_id = ?",
                            new String[]{String.valueOf(almacenadas.get(actual.getKey()).id)});
                }
            }
            db.execSQL("DELETE FROM " + TABLE_EVENTOS + " WHERE _id NOT IN "
                    + "(SELECT evento_id FROM " + TABLE_ACTIVAS + ") AND _id NOT IN "
                    + "(SELECT _id FROM " + TABLE_EVENTOS + " ORDER BY _id DESC LIMIT 500)");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private long insertarEvento(SQLiteDatabase db, EventoAlerta alerta, String tipo, boolean atendida) {
        ContentValues valores = new ContentValues();
        valores.put("torre", alerta.torre);
        valores.put("cultivo", alerta.cultivo);
        valores.put("clave", alerta.clave());
        valores.put("variable", alerta.variable);
        valores.put("nivel", alerta.nivel);
        valores.put("valor", alerta.valor);
        valores.put("accion", alerta.accion);
        valores.put("tipo", tipo);
        valores.put("fecha_hora", EventoAlerta.ahora());
        valores.put("atendida", atendida ? 1 : 0);
        return db.insertOrThrow(TABLE_EVENTOS, null, valores);
    }

    @Override
    public List<EventoAlerta> obtenerEventos(int limite) {
        List<EventoAlerta> eventos = new ArrayList<>();
        int limiteSeguro = Math.max(1, Math.min(limite, 500));
        try (Cursor cursor = getReadableDatabase().query(TABLE_EVENTOS,
                new String[]{"_id", "torre", "cultivo", "variable", "nivel", "valor", "accion", "tipo", "fecha_hora", "atendida"},
                null, null, null, null, "_id DESC", String.valueOf(limiteSeguro))) {
            while (cursor.moveToNext()) eventos.add(leerEvento(cursor));
        }
        return eventos;
    }

    @Override
    public List<EventoAlerta> obtenerAlertasActivas() {
        List<EventoAlerta> activas = new ArrayList<>();
        String consulta = "SELECT e._id, e.torre, e.cultivo, e.variable, e.nivel, e.valor, "
                + "e.accion, e.tipo, e.fecha_hora, e.atendida FROM " + TABLE_ACTIVAS + " a "
                + "JOIN " + TABLE_EVENTOS + " e ON e._id = a.evento_id ORDER BY e._id DESC";
        try (Cursor cursor = getReadableDatabase().rawQuery(consulta, null)) {
            while (cursor.moveToNext()) activas.add(leerEvento(cursor));
        }
        return activas;
    }

    private EventoAlerta leerEvento(Cursor cursor) {
        return new EventoAlerta(cursor.getLong(0), cursor.getInt(1), cursor.getString(2),
                cursor.getString(3), cursor.getString(4), cursor.getDouble(5), cursor.getString(6),
                cursor.getString(7), cursor.getString(8), cursor.getInt(9) == 1);
    }

    @Override
    public void marcarAlertasActivasAtendidas() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_EVENTOS + " SET atendida = 1 WHERE _id IN (SELECT evento_id FROM "
                + TABLE_ACTIVAS + ")");
    }

    @Override
    public void guardarMuestras(List<MedicionMuestra> muestras) {
        if (muestras == null || muestras.isEmpty()) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (MedicionMuestra muestra : muestras) {
                ContentValues valores = new ContentValues();
                valores.put("torre", muestra.torre);
                valores.put("cultivo", muestra.cultivo);
                valores.put("variable", muestra.variable);
                valores.put("valor", muestra.valor);
                valores.put("fecha_millis", muestra.fechaMillis);
                db.insertOrThrow(TABLE_MUESTRAS, null, valores);
            }
            long limiteRetencion = System.currentTimeMillis() - 24L * 60L * 60L * 1000L;
            db.delete(TABLE_MUESTRAS, "fecha_millis < ?", new String[]{String.valueOf(limiteRetencion)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public List<MedicionMuestra> obtenerMuestras(int torre, String variable, long desdeMillis, int limite) {
        List<MedicionMuestra> muestras = new ArrayList<>();
        int limiteSeguro = Math.max(2, Math.min(limite, 3000));
        try (Cursor cursor = getReadableDatabase().query(TABLE_MUESTRAS,
                new String[]{"torre", "cultivo", "variable", "valor", "fecha_millis"},
                "torre = ? AND variable = ? AND fecha_millis >= ?",
                new String[]{String.valueOf(torre), variable, String.valueOf(desdeMillis)},
                null, null, "fecha_millis ASC", String.valueOf(limiteSeguro))) {
            while (cursor.moveToNext()) {
                muestras.add(new MedicionMuestra(cursor.getInt(0), cursor.getString(1),
                        cursor.getString(2), cursor.getDouble(3), cursor.getLong(4)));
            }
        }
        return muestras;
    }
}
