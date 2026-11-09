package com.devst.hidrosmart.view;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.data.GestionUsuariosDataSource;
import com.devst.hidrosmart.data.HuertoDataSourceProvider;
import com.devst.hidrosmart.modelo.UsuarioApp;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/** Panel local para que Admin gestione las cuentas de la app. */
public class GestionUsuariosActivity extends AppCompatActivity {
    public static final String EXTRA_USUARIO_ADMIN = "usuario_admin";

    private GestionUsuariosDataSource usuarios;
    private LinearLayout contenedor;
    private String usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Login.ROL_ADMIN.equals(getIntent().getStringExtra(Login.EXTRA_ROL))) {
            finish();
            return;
        }

        setContentView(R.layout.activity_gestion_usuarios);
        usuarios = HuertoDataSourceProvider.getAutenticacion(getApplicationContext());
        usuarioActual = getIntent().getStringExtra(EXTRA_USUARIO_ADMIN);
        contenedor = findViewById(R.id.contenedorUsuarios);
        findViewById(R.id.btnVolverUsuarios).setOnClickListener(v -> finish());
        findViewById(R.id.btnCrearUsuario).setOnClickListener(v -> mostrarFormularioCrear());
        mostrarUsuarios();
    }

    private void mostrarUsuarios() {
        if (contenedor == null || usuarios == null) return;
        contenedor.removeAllViews();
        List<UsuarioApp> lista = usuarios.obtenerUsuarios();
        if (lista.isEmpty()) {
            TextView vacio = new TextView(this);
            vacio.setText(R.string.usuarios_sin_cuentas);
            vacio.setTextColor(getColor(R.color.texto_secundario));
            vacio.setPadding(16, 24, 16, 24);
            contenedor.addView(vacio);
            return;
        }

        for (UsuarioApp cuenta : lista) {
            MaterialCardView tarjeta = new MaterialCardView(this);
            tarjeta.setUseCompatPadding(false);
            tarjeta.setStrokeWidth(dp(1));
            tarjeta.setStrokeColor(getColor(R.color.divisor));
            tarjeta.setCardBackgroundColor(getColor(R.color.superficie_tarjeta));
            tarjeta.setRadius(dp(20));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(10);
            tarjeta.setLayoutParams(params);
            tarjeta.setClickable(true);
            tarjeta.setFocusable(true);

            LinearLayout contenido = new LinearLayout(this);
            contenido.setOrientation(LinearLayout.VERTICAL);
            contenido.setPadding(dp(16), dp(14), dp(16), dp(14));
            TextView nombre = new TextView(this);
            nombre.setText(cuenta.nombre);
            nombre.setTextColor(getColor(R.color.texto_principal));
            nombre.setTextSize(17);
            nombre.setTypeface(null, android.graphics.Typeface.BOLD);
            TextView detalle = new TextView(this);
            detalle.setText(getString(cuenta.activo ? R.string.usuarios_activo : R.string.usuarios_inactivo)
                    + " · " + getString(Login.ROL_ADMIN.equals(cuenta.rol)
                    ? R.string.usuarios_rol_admin : R.string.usuarios_rol_operador));
            detalle.setTextColor(getColor(cuenta.activo ? R.color.alerta_ok : R.color.texto_terciario));
            detalle.setTextSize(13);
            detalle.setPadding(0, dp(4), 0, 0);
            contenido.addView(nombre);
            contenido.addView(detalle);
            tarjeta.addView(contenido);
            tarjeta.setOnClickListener(v -> mostrarAcciones(cuenta));
            contenedor.addView(tarjeta);
        }
    }

    private void mostrarFormularioCrear() {
        LinearLayout formulario = new LinearLayout(this);
        formulario.setOrientation(LinearLayout.VERTICAL);
        formulario.setPadding(dp(8), dp(4), dp(8), 0);
        EditText nombre = campo(getString(R.string.usuarios_nombre), InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        EditText clave = campo(getString(R.string.usuarios_contrasena), InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Spinner selectorRol = new Spinner(this);
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.usuarios_rol_operador), getString(R.string.usuarios_rol_admin)});
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectorRol.setAdapter(adaptador);
        selectorRol.setPadding(dp(4), dp(8), dp(4), dp(8));
        formulario.addView(nombre);
        formulario.addView(clave);
        formulario.addView(selectorRol);

        new AlertDialog.Builder(this)
                .setTitle(R.string.usuarios_crear)
                .setView(formulario)
                .setNegativeButton(R.string.accion_cancelar, null)
                .setPositiveButton(R.string.usuarios_crear, (dialog, which) -> {
                    String nombreValor = nombre.getText().toString().trim();
                    String claveValor = clave.getText().toString();
                    if (!nombreValido(nombreValor)) {
                        mostrarMensaje(R.string.usuarios_error_nombre);
                        return;
                    }
                    if (claveValor.trim().length() < 4) {
                        mostrarMensaje(R.string.usuarios_error_clave);
                        return;
                    }
                    String rol = selectorRol.getSelectedItemPosition() == 1
                            ? Login.ROL_ADMIN : Login.ROL_USER;
                    try {
                        usuarios.crearUsuario(nombreValor, claveValor, rol);
                        mostrarUsuarios();
                        mostrarMensaje(R.string.usuarios_creada);
                    } catch (android.database.sqlite.SQLiteConstraintException e) {
                        mostrarMensaje(R.string.usuarios_error_duplicado);
                    } catch (RuntimeException e) {
                        mostrarMensaje(e.getMessage());
                    }
                })
                .show();
    }

    private void mostrarAcciones(UsuarioApp cuenta) {
        boolean esPropia = usuarioActual != null && usuarioActual.equalsIgnoreCase(cuenta.nombre);
        String[] acciones;
        if (esPropia) {
            acciones = new String[]{getString(R.string.usuarios_menu_clave)};
        } else if (cuenta.activo) {
            acciones = new String[]{getString(R.string.usuarios_menu_rol),
                    getString(R.string.usuarios_menu_clave), getString(R.string.usuarios_menu_desactivar)};
        } else {
            acciones = new String[]{getString(R.string.usuarios_menu_rol),
                    getString(R.string.usuarios_menu_clave), getString(R.string.usuarios_menu_activar)};
        }
        new AlertDialog.Builder(this)
                .setTitle(cuenta.nombre)
                .setItems(acciones, (dialog, index) -> {
                    if (esPropia) {
                        mostrarFormularioClave(cuenta.nombre);
                    } else if (index == 0) {
                        mostrarSelectorRol(cuenta);
                    } else if (index == 1) {
                        mostrarFormularioClave(cuenta.nombre);
                    } else {
                        confirmarCambioEstado(cuenta);
                    }
                })
                .setNegativeButton(R.string.accion_cancelar, null)
                .show();
    }

    private void mostrarSelectorRol(UsuarioApp cuenta) {
        String[] roles = {getString(R.string.usuarios_rol_operador), getString(R.string.usuarios_rol_admin)};
        int seleccionado = Login.ROL_ADMIN.equals(cuenta.rol) ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle(R.string.usuarios_rol)
                .setSingleChoiceItems(roles, seleccionado, (dialog, which) -> {
                    String rol = which == 1 ? Login.ROL_ADMIN : Login.ROL_USER;
                    try {
                        usuarios.cambiarRol(cuenta.nombre, rol);
                        mostrarUsuarios();
                        mostrarMensaje(R.string.usuarios_actualizada);
                        dialog.dismiss();
                    } catch (RuntimeException e) {
                        dialog.dismiss();
                        mostrarMensaje(e.getMessage());
                    }
                })
                .setNegativeButton(R.string.accion_cancelar, null)
                .show();
    }

    private void mostrarFormularioClave(String nombre) {
        EditText clave = campo(getString(R.string.usuarios_contrasena), InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.usuarios_menu_clave) + " · " + nombre)
                .setView(clave)
                .setNegativeButton(R.string.accion_cancelar, null)
                .setPositiveButton(R.string.accion_guardar, (dialog, which) -> {
                    try {
                        usuarios.cambiarContrasena(nombre, clave.getText().toString());
                        mostrarMensaje(R.string.usuarios_clave_actualizada);
                    } catch (RuntimeException e) {
                        mostrarMensaje(e.getMessage());
                    }
                })
                .show();
    }

    private void confirmarCambioEstado(UsuarioApp cuenta) {
        boolean activar = !cuenta.activo;
        new AlertDialog.Builder(this)
                .setTitle(activar ? R.string.usuarios_menu_activar : R.string.usuarios_menu_desactivar)
                .setMessage(activar ? getString(R.string.usuarios_confirmar_activar, cuenta.nombre)
                        : getString(R.string.usuarios_confirmar_desactivar, cuenta.nombre))
                .setNegativeButton(R.string.accion_cancelar, null)
                .setPositiveButton(R.string.accion_confirmar, (dialog, which) -> {
                    try {
                        usuarios.cambiarEstado(cuenta.nombre, activar);
                        mostrarUsuarios();
                        mostrarMensaje(R.string.usuarios_actualizada);
                    } catch (RuntimeException e) {
                        mostrarMensaje(e.getMessage());
                    }
                })
                .show();
    }

    private EditText campo(String hint, int inputType) {
        EditText campo = new EditText(this);
        campo.setHint(hint);
        campo.setInputType(inputType);
        campo.setSingleLine(true);
        campo.setPadding(dp(12), dp(12), dp(12), dp(12));
        return campo;
    }

    private boolean nombreValido(String nombre) {
        return nombre.length() >= 3 && nombre.length() <= 32
                && nombre.matches("[a-zA-Z0-9._-]+");
    }

    private int dp(float valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private void mostrarMensaje(int recurso) {
        Toast.makeText(this, recurso, Toast.LENGTH_SHORT).show();
    }

    private void mostrarMensaje(String mensaje) {
        String texto = mensaje == null || mensaje.trim().isEmpty()
                ? getString(R.string.mensaje_error_generico) : mensaje;
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }
}
