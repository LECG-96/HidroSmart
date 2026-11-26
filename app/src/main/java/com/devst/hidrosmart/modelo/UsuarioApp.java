package com.devst.hidrosmart.modelo;

/** Cuenta local administrable mientras la app funciona sin servidor. */
public final class UsuarioApp {
    public final String nombre;
    public final String rol;
    public final boolean activo;

    public UsuarioApp(String nombre, String rol, boolean activo) {
        this.nombre = nombre;
        this.rol = rol;
        this.activo = activo;
    }
}
