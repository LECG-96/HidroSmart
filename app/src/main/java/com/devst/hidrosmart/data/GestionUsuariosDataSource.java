package com.devst.hidrosmart.data;

import com.devst.hidrosmart.modelo.UsuarioApp;

import java.util.List;

/** Operaciones de cuentas locales para el panel de administración. */
public interface GestionUsuariosDataSource extends AutenticacionDataSource {
    List<UsuarioApp> obtenerUsuarios();
    void crearUsuario(String nombre, String contrasena, String rol);
    void cambiarRol(String nombre, String rol);
    void cambiarContrasena(String nombre, String contrasena);
    boolean requiereCambioContrasena(String nombre);
    void cambiarEstado(String nombre, boolean activo);
}
