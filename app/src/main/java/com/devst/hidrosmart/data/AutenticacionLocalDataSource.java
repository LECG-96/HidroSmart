package com.devst.hidrosmart.data;

/** Credenciales de demostración local; no ofrece autenticación para producción. */
public class AutenticacionLocalDataSource implements AutenticacionDataSource {
    @Override
    public String autenticar(String usuario, String contrasena) {
        if ("admin".equals(usuario) && "1234".equals(contrasena)) {
            return ROL_ADMIN;
        }
        if ("user".equals(usuario) && "1234".equals(contrasena)) {
            return ROL_USER;
        }
        return null;
    }
}
