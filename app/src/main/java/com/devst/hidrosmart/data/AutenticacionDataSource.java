package com.devst.hidrosmart.data;

/** Contrato para validar credenciales desde la fuente que use la aplicación. */
public interface AutenticacionDataSource {
    String ROL_ADMIN = "ADMIN";
    String ROL_USER = "USER";

    /** Devuelve el rol autorizado o null si las credenciales no son válidas. */
    String autenticar(String usuario, String contrasena);
}
