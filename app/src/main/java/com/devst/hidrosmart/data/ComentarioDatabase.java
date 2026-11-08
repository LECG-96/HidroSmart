package com.devst.hidrosmart.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.devst.hidrosmart.modelo.ComentarioUsuario;

import java.util.ArrayList;
import java.util.List;

/** Almacenamiento local de observaciones del operador. */
public class ComentarioDatabase extends SQLiteOpenHelper implements ComentarioLocalDataSource {
    private static final String DATABASE_NAME = "hidrosmart_observaciones.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_OBSERVACIONES = "observaciones";

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Las migraciones se agregan aquí al cambiar la estructura de la base local.
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
}
