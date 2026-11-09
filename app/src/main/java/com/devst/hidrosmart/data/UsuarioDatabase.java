package com.devst.hidrosmart.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.devst.hidrosmart.modelo.UsuarioApp;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Cuentas locales de prueba con contraseñas almacenadas como hashes con sal. */
public final class UsuarioDatabase extends SQLiteOpenHelper implements GestionUsuariosDataSource {
    private static final String NOMBRE_BD = "hidrosmart_usuarios.db";
    private static final int VERSION_BD = 1;
    private static final String TABLA = "usuarios";
    private static final int ITERACIONES_HASH = 120_000;
    private static final int BYTES_HASH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    public UsuarioDatabase(Context context) {
        super(context.getApplicationContext(), NOMBRE_BD, null, VERSION_BD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLA + " ("
                + "nombre TEXT PRIMARY KEY COLLATE NOCASE, "
                + "rol TEXT NOT NULL CHECK (rol IN ('ADMIN','USER')), "
                + "activo INTEGER NOT NULL DEFAULT 1, "
                + "sal BLOB NOT NULL, "
                + "hash_clave BLOB NOT NULL)");
        insertarCuentaInicial(db, "admin", "1234", ROL_ADMIN);
        insertarCuentaInicial(db, "user", "1234", ROL_USER);
    }

    private void insertarCuentaInicial(SQLiteDatabase db, String nombre, String clave, String rol) {
        byte[] sal = nuevaSal();
        ContentValues valores = new ContentValues();
        valores.put("nombre", normalizarNombre(nombre));
        valores.put("rol", rol);
        valores.put("activo", 1);
        valores.put("sal", sal);
        valores.put("hash_clave", derivarHash(clave, sal));
        db.insertOrThrow(TABLA, null, valores);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Agregar migraciones explícitas cuando cambie el esquema local.
    }

    @Override
    public String autenticar(String usuario, String contrasena) {
        if (usuario == null || contrasena == null) return null;
        try (Cursor cursor = getReadableDatabase().query(TABLA,
                new String[]{"rol", "sal", "hash_clave"},
                "nombre = ? AND activo = 1",
                new String[]{normalizarNombre(usuario)}, null, null, null)) {
            if (!cursor.moveToFirst()) return null;
            byte[] sal = cursor.getBlob(1);
            byte[] esperado = cursor.getBlob(2);
            byte[] recibido = derivarHash(contrasena, sal);
            return MessageDigest.isEqual(esperado, recibido) ? cursor.getString(0) : null;
        }
    }

    @Override
    public List<UsuarioApp> obtenerUsuarios() {
        List<UsuarioApp> resultado = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLA,
                new String[]{"nombre", "rol", "activo"}, null, null, null, null, "nombre COLLATE NOCASE")) {
            while (cursor.moveToNext()) {
                resultado.add(new UsuarioApp(cursor.getString(0), cursor.getString(1), cursor.getInt(2) == 1));
            }
        }
        return resultado;
    }

    @Override
    public void crearUsuario(String nombre, String contrasena, String rol) {
        String nombreNormalizado = normalizarNombre(nombre);
        validarCuenta(nombreNormalizado, contrasena, rol);
        byte[] sal = nuevaSal();
        ContentValues valores = new ContentValues();
        valores.put("nombre", nombreNormalizado);
        valores.put("rol", rol);
        valores.put("activo", 1);
        valores.put("sal", sal);
        valores.put("hash_clave", derivarHash(contrasena, sal));
        getWritableDatabase().insertOrThrow(TABLA, null, valores);
    }

    @Override
    public void cambiarRol(String nombre, String rol) {
        if (!ROL_ADMIN.equals(rol) && !ROL_USER.equals(rol)) {
            throw new IllegalArgumentException("Selecciona un rol válido.");
        }
        SQLiteDatabase db = getWritableDatabase();
        String usuario = normalizarNombre(nombre);
        db.beginTransaction();
        try {
            String rolActual = consultarRol(db, usuario);
            if (rolActual == null) throw new IllegalArgumentException("No se encontró la cuenta.");
            if (ROL_ADMIN.equals(rolActual) && !ROL_ADMIN.equals(rol)
                    && contarAdministradoresActivos(db) <= 1 && cuentaActiva(db, usuario)) {
                throw new IllegalStateException("Debe quedar al menos un administrador activo.");
            }
            ContentValues valores = new ContentValues();
            valores.put("rol", rol);
            db.update(TABLA, valores, "nombre = ?", new String[]{usuario});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void cambiarContrasena(String nombre, String contrasena) {
        if (contrasena == null || contrasena.trim().length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres.");
        }
        String usuario = normalizarNombre(nombre);
        byte[] sal = nuevaSal();
        ContentValues valores = new ContentValues();
        valores.put("sal", sal);
        valores.put("hash_clave", derivarHash(contrasena, sal));
        if (getWritableDatabase().update(TABLA, valores, "nombre = ?", new String[]{usuario}) == 0) {
            throw new IllegalArgumentException("No se encontró la cuenta.");
        }
    }

    @Override
    public void cambiarEstado(String nombre, boolean activo) {
        String usuario = normalizarNombre(nombre);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String rolActual = consultarRol(db, usuario);
            if (rolActual == null) throw new IllegalArgumentException("No se encontró la cuenta.");
            if (!activo && ROL_ADMIN.equals(rolActual) && cuentaActiva(db, usuario)
                    && contarAdministradoresActivos(db) <= 1) {
                throw new IllegalStateException("Debe quedar al menos un administrador activo.");
            }
            ContentValues valores = new ContentValues();
            valores.put("activo", activo ? 1 : 0);
            db.update(TABLA, valores, "nombre = ?", new String[]{usuario});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void validarCuenta(String nombre, String contrasena, String rol) {
        if (nombre.length() < 3 || nombre.length() > 32 || !nombre.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException("Usa un nombre de 3 a 32 caracteres: letras, números, punto, guion o guion bajo.");
        }
        if (contrasena == null || contrasena.trim().length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres.");
        }
        if (!ROL_ADMIN.equals(rol) && !ROL_USER.equals(rol)) {
            throw new IllegalArgumentException("Selecciona un rol válido.");
        }
    }

    private static String normalizarNombre(String nombre) {
        return nombre == null ? "" : nombre.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] nuevaSal() {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);
        return sal;
    }

    private static byte[] derivarHash(String contrasena, byte[] sal) {
        PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), sal, ITERACIONES_HASH, BYTES_HASH * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo verificar la contraseña.", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String consultarRol(SQLiteDatabase db, String usuario) {
        try (Cursor cursor = db.query(TABLA, new String[]{"rol"}, "nombre = ?",
                new String[]{usuario}, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    private static boolean cuentaActiva(SQLiteDatabase db, String usuario) {
        try (Cursor cursor = db.query(TABLA, new String[]{"activo"}, "nombre = ?",
                new String[]{usuario}, null, null, null)) {
            return cursor.moveToFirst() && cursor.getInt(0) == 1;
        }
    }

    private static int contarAdministradoresActivos(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLA
                + " WHERE rol = ? AND activo = 1", new String[]{ROL_ADMIN})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }
}
