package com.devst.hidrosmart.data;

import com.devst.hidrosmart.modelo.UsuarioApp;

import java.util.List;

/** Operaciones de cuentas locales para el panel de administración. */
public interface GestionUsuariosDataSource extends AutenticacionDataSource {
    List<UsuarioApp> obtenerUsuarios();
    /** Perfil de una cuenta, o null si no existe. */
    UsuarioApp obtenerUsuario(String nombre);
    void crearUsuario(String nombre, String contrasena, String rol);
    void cambiarRol(String nombre, String rol);
    void cambiarContrasena(String nombre, String contrasena);
    boolean requiereCambioContrasena(String nombre);
    void cambiarEstado(String nombre, boolean activo);

    void eliminarUsuario(String nombre);
}
