package com.devst.hidrosmart.data;

import android.content.Context;

import com.devst.hidrosmart.modelo.UsuarioApp;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

    /**
     * Cuentas en Firebase: Authentication guarda la contraseña y Firestore (colección "usuarios",
     * documento = UID) guarda nombre, rol y estado. Los métodos son bloqueantes: llamarlos siempre
     * desde un hilo secundario.
     */
    public final class UsuarioFirestore implements GestionUsuariosDataSource {
        private static final String COLECCION = "usuarios";
        private static final String DOMINIO = "@hidrosmart.app";
        private static final String APP_SECUNDARIA = "creacion-usuarios";
        private static final long TIMEOUT_SEGUNDOS = 15;

        private final Context context;
        private final FirebaseAuth auth;
        private final FirebaseFirestore db;

        public UsuarioFirestore(Context context) {
            this.context = context.getApplicationContext();
            this.auth = FirebaseAuth.getInstance();
            this.db = FirebaseFirestore.getInstance();
        }

        @Override
        public String autenticar(String usuario, String contrasena) {
            if (usuario == null || contrasena == null) return null;
            String nombre = normalizarNombre(usuario);
            if (nombre.isEmpty() || contrasena.isEmpty()) return null;
            try {
                esperar(auth.signInWithEmailAndPassword(correo(nombre), contrasena));
                FirebaseUser actual = auth.getCurrentUser();
                if (actual == null) return null;
                DocumentSnapshot doc = esperar(db.collection(COLECCION).document(actual.getUid()).get());
                if (!doc.exists() || !Boolean.TRUE.equals(doc.getBoolean("activo"))) {
                    auth.signOut();
                    return null;
                }
                String rol = doc.getString("rol");
                if (!ROL_ADMIN.equals(rol) && !ROL_USER.equals(rol)) {
                    auth.signOut();
                    return null;
                }
                return rol;
            } catch (CredencialesInvalidasException e) {
                return null;
            }
        }

        @Override
        public List<UsuarioApp> obtenerUsuarios() {
            QuerySnapshot resultado = esperar(db.collection(COLECCION).orderBy("nombre").get());
            List<UsuarioApp> lista = new ArrayList<>();
            for (QueryDocumentSnapshot doc : resultado) lista.add(aUsuario(doc));
            return lista;
        }

        @Override
        public UsuarioApp obtenerUsuario(String nombre) {
            DocumentSnapshot doc = documentoDe(nombre);
            return doc == null ? null : aUsuario(doc);
        }

        @Override
        public void crearUsuario(String nombre, String contrasena, String rol) {
            String nombreNormalizado = normalizarNombre(nombre);
            validarCuenta(nombreNormalizado, contrasena, rol);

            // La cuenta se crea con una instancia secundaria para no cerrar la sesión del administrador.
            FirebaseAuth authSecundaria = authSecundaria();
            AuthResult creado = esperar(authSecundaria.createUserWithEmailAndPassword(
                    correo(nombreNormalizado), contrasena));
            FirebaseUser nuevo = creado.getUser();
            if (nuevo == null) throw new IllegalStateException("No se pudo crear la cuenta.");
            try {
                Map<String, Object> doc = new HashMap<>();
                doc.put("nombre", nombreNormalizado);
                doc.put("rol", rol);
                doc.put("activo", true);
                doc.put("cambioClavePendiente", false);
                esperar(db.collection(COLECCION).document(nuevo.getUid()).set(doc));
            } catch (RuntimeException e) {
                try {
                    esperar(nuevo.delete());
                } catch (RuntimeException ignorada) {
                    // Si falla la limpieza, la cuenta queda sin perfil y no podrá ingresar.
                }
                throw e;
            } finally {
                authSecundaria.signOut();
            }
        }

        @Override
        public boolean requiereCambioContrasena(String nombre) {
            DocumentSnapshot doc = documentoDe(nombre);
            return doc != null && Boolean.TRUE.equals(doc.getBoolean("cambioClavePendiente"));
        }

        @Override
        public void cambiarRol(String nombre, String rol) {
            if (!ROL_ADMIN.equals(rol) && !ROL_USER.equals(rol)) {
                throw new IllegalArgumentException("Selecciona un rol válido.");
            }
            DocumentSnapshot doc = documentoDe(nombre);
            if (doc == null) throw new IllegalArgumentException("No se encontró la cuenta.");
            if (ROL_ADMIN.equals(doc.getString("rol")) && !ROL_ADMIN.equals(rol)
                    && Boolean.TRUE.equals(doc.getBoolean("activo"))
                    && contarAdministradoresActivos() <= 1) {
                throw new IllegalStateException("Debe quedar al menos un administrador activo.");
            }
            esperar(doc.getReference().update("rol", rol));
        }

        @Override
        public void cambiarContrasena(String nombre, String contrasena) {
            validarContrasena(contrasena);
            FirebaseUser actual = auth.getCurrentUser();
            if (actual == null || actual.getEmail() == null
                    || !actual.getEmail().equalsIgnoreCase(correo(normalizarNombre(nombre)))) {
                // El SDK de Android solo permite cambiar la clave de la sesión activa.
                throw new IllegalStateException(
                        "Solo se puede cambiar la contraseña de la cuenta con sesión iniciada.");
            }
            esperar(actual.updatePassword(contrasena));
            esperar(db.collection(COLECCION).document(actual.getUid())
                    .update("cambioClavePendiente", false));
        }

        @Override
        public void cambiarEstado(String nombre, boolean activo) {
            DocumentSnapshot doc = documentoDe(nombre);
            if (doc == null) throw new IllegalArgumentException("No se encontró la cuenta.");
            if (!activo && ROL_ADMIN.equals(doc.getString("rol"))
                    && Boolean.TRUE.equals(doc.getBoolean("activo"))
                    && contarAdministradoresActivos() <= 1) {
                throw new IllegalStateException("Debe quedar al menos un administrador activo.");
            }
            esperar(doc.getReference().update("activo", activo));
        }

        @Override
        public void eliminarUsuario(String nombre) {
            String nombreNormalizado = normalizarNombre(nombre);

            if (nombreNormalizado.isEmpty()) {
                throw new IllegalArgumentException("Nombre de usuario inválido.");
            }

            FirebaseUser actual = auth.getCurrentUser();
            if (actual == null) {
                throw new IllegalStateException("No hay una sesión activa.");
            }

            if (actual.getEmail() != null
                    && actual.getEmail().equalsIgnoreCase(correo(nombreNormalizado))) {
                throw new IllegalStateException("No puedes eliminar tu propia cuenta.");
            }

            // 1. Buscar el documento del usuario en Firestore
            DocumentSnapshot doc = documentoDe(nombreNormalizado);
            if (doc == null) {
                throw new IllegalArgumentException("No se encontró la cuenta.");
            }

            // 2. Evitar borrar al último administrador activo
            if (ROL_ADMIN.equals(doc.getString("rol"))
                    && Boolean.TRUE.equals(doc.getBoolean("activo"))
                    && contarAdministradoresActivos() <= 1) {
                throw new IllegalStateException("Debe quedar al menos un administrador activo.");
            }

            // 3. Eliminar el documento de Firestore (permitido por tus reglas actuales)
            esperar(doc.getReference().delete());
        }


        // ---- Auxiliares ----

        /** Perfil de la cuenta: por UID si es la sesión actual, por consulta si no (solo admin). */
        private DocumentSnapshot documentoDe(String nombre) {
            String normalizado = normalizarNombre(nombre);
            FirebaseUser actual = auth.getCurrentUser();
            if (actual != null && actual.getEmail() != null
                    && actual.getEmail().equalsIgnoreCase(correo(normalizado))) {
                DocumentSnapshot doc = esperar(db.collection(COLECCION).document(actual.getUid()).get());
                return doc.exists() ? doc : null;
            }
            QuerySnapshot resultado = esperar(db.collection(COLECCION)
                    .whereEqualTo("nombre", normalizado).limit(1).get());
            return resultado.isEmpty() ? null : resultado.getDocuments().get(0);
        }

        private int contarAdministradoresActivos() {
            return esperar(db.collection(COLECCION)
                    .whereEqualTo("rol", ROL_ADMIN)
                    .whereEqualTo("activo", true).get()).size();
        }

        private FirebaseAuth authSecundaria() {
            FirebaseApp secundaria;
            try {
                secundaria = FirebaseApp.getInstance(APP_SECUNDARIA);
            } catch (IllegalStateException noExiste) {
                secundaria = FirebaseApp.initializeApp(context,
                        FirebaseApp.getInstance().getOptions(), APP_SECUNDARIA);
            }
            return FirebaseAuth.getInstance(secundaria);
        }

        private static UsuarioApp aUsuario(DocumentSnapshot doc) {
            return new UsuarioApp(doc.getString("nombre"), doc.getString("rol"),
                    Boolean.TRUE.equals(doc.getBoolean("activo")));
        }

        private static <T> T esperar(Task<T> tarea) {
            try {
                return Tasks.await(tarea, TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                throw traducir(e.getCause() != null ? e.getCause() : e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Operación cancelada.", e);
            } catch (TimeoutException e) {
                throw new IllegalStateException("El servidor no respondió. Revisa tu conexión.", e);
            }
        }

        private static RuntimeException traducir(Throwable causa) {
            if (causa instanceof FirebaseAuthInvalidUserException
                    || causa instanceof FirebaseAuthInvalidCredentialsException) {
                return new CredencialesInvalidasException();
            }
            if (causa instanceof FirebaseAuthUserCollisionException) {
                return new IllegalArgumentException("Ya existe una cuenta con ese nombre.");
            }
            if (causa instanceof FirebaseNetworkException) {
                return new IllegalStateException("Sin conexión a internet.", causa);
            }
            if (causa instanceof FirebaseFirestoreException
                    && ((FirebaseFirestoreException) causa).getCode()
                    == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return new IllegalStateException("No tienes permisos para esta acción.", causa);
            }
            String mensaje = causa.getMessage();
            return new IllegalStateException(mensaje == null ? "Error de servidor." : mensaje, causa);
        }

        private static String correo(String nombreNormalizado) {
            return nombreNormalizado + DOMINIO;
        }

        private static String normalizarNombre(String nombre) {
            return nombre == null ? "" : nombre.trim().toLowerCase(Locale.ROOT);
        }

        private static void validarCuenta(String nombre, String contrasena, String rol) {
            if (nombre.length() < 3 || nombre.length() > 32 || !nombre.matches("[a-z0-9._-]+")) {
                throw new IllegalArgumentException("Usa un nombre de 3 a 32 caracteres: letras, números, punto, guion o guion bajo.");
            }
            validarContrasena(contrasena);
            if (!ROL_ADMIN.equals(rol) && !ROL_USER.equals(rol)) {
                throw new IllegalArgumentException("Selecciona un rol válido.");
            }
        }

        private static void validarContrasena(String contrasena) {
            if (contrasena == null || contrasena.trim().length() < 8) {
                throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
            }
        }

        /** Usuario inexistente o clave incorrecta: se traduce a "login inválido" sin mensaje técnico. */
        private static final class CredencialesInvalidasException extends RuntimeException {
            CredencialesInvalidasException() {
                super("Usuario o contraseña incorrectos.");
            }
        }
    }
