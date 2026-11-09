package com.devst.hidrosmart.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.data.AutenticacionDataSource;
import com.devst.hidrosmart.data.GestionUsuariosDataSource;
import com.devst.hidrosmart.data.HuertoDataSourceProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Login extends AppCompatActivity {

    public static final String EXTRA_ROL = "extra_rol";
    public static final String EXTRA_USUARIO_ACTUAL = "extra_usuario_actual";
    public static final String ROL_ADMIN = AutenticacionDataSource.ROL_ADMIN;
    public static final String ROL_USER = AutenticacionDataSource.ROL_USER;
    private final ExecutorService loginExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etUsuario = findViewById(R.id.etUsuario);
        EditText etContrasena = findViewById(R.id.etContrasena);
        Button btnIngresar = findViewById(R.id.btnIngresar);
        Button btnDemo = findViewById(R.id.btnDemo);
        GestionUsuariosDataSource autenticacion = HuertoDataSourceProvider.getAutenticacion(getApplicationContext());

        btnIngresar.setOnClickListener(v -> {
            String usuario = etUsuario.getText().toString().trim();
            String contrasena = etContrasena.getText().toString();

            if (usuario.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, R.string.mensaje_campos_incompletos, Toast.LENGTH_SHORT).show();
                return;
            }

            btnIngresar.setEnabled(false);
            loginExecutor.execute(() -> {
                String rol = autenticacion.autenticar(usuario, contrasena);
                boolean cambioObligatorio = rol != null && autenticacion.requiereCambioContrasena(usuario);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    btnIngresar.setEnabled(true);
                    if (rol == null) {
                        Toast.makeText(this, R.string.mensaje_login_incorrecto, Toast.LENGTH_SHORT).show();
                    } else if (cambioObligatorio) {
                        mostrarCambioClaveObligatorio(usuario, rol, autenticacion);
                    } else {
                        abrirMain(rol, false, usuario);
                    }
                });
            });
        });

        // Modo Demo permite interactuar con Control & Sim sin mostrar acciones de administración.
        btnDemo.setOnClickListener(v -> abrirMain(ROL_ADMIN, true, "demo"));
    }

    private void mostrarCambioClaveObligatorio(String usuario, String rol,
                                               GestionUsuariosDataSource autenticacion) {
        LinearLayout formulario = new LinearLayout(this);
        formulario.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        formulario.setPadding(padding, padding / 2, padding, 0);
        EditText nueva = new EditText(this);
        nueva.setSingleLine(true);
        nueva.setHint(R.string.login_nueva_contrasena);
        nueva.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirmar = new EditText(this);
        confirmar.setSingleLine(true);
        confirmar.setHint(R.string.usuarios_confirmar_contrasena);
        confirmar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        formulario.addView(nueva);
        formulario.addView(confirmar);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.login_cambio_clave_obligatorio)
                .setMessage(R.string.login_cambio_clave_descripcion)
                .setView(formulario)
                .setPositiveButton(R.string.accion_guardar, null)
                .create();
        dialogo.setCancelable(false);
        dialogo.setCanceledOnTouchOutside(false);
        dialogo.setOnShowListener(ignorado -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String clave = nueva.getText().toString();
            if (clave.trim().length() < 8) {
                Toast.makeText(this, R.string.usuarios_error_clave, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!clave.equals(confirmar.getText().toString())) {
                Toast.makeText(this, R.string.usuarios_error_confirmar_clave, Toast.LENGTH_SHORT).show();
                return;
            }
            Button guardar = dialogo.getButton(AlertDialog.BUTTON_POSITIVE);
            guardar.setEnabled(false);
            loginExecutor.execute(() -> {
                RuntimeException error = null;
                try {
                    autenticacion.cambiarContrasena(usuario, clave);
                } catch (RuntimeException e) {
                    error = e;
                }
                RuntimeException resultadoError = error;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    guardar.setEnabled(true);
                    if (resultadoError != null) {
                        Toast.makeText(this, resultadoError.getMessage() == null
                                ? getString(R.string.mensaje_error_generico) : resultadoError.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    dialogo.dismiss();
                    abrirMain(rol, false, usuario);
                });
            });
        }));
        dialogo.show();
    }

    @Override
    protected void onDestroy() {
        loginExecutor.shutdown();
        super.onDestroy();
    }

    private void abrirMain(String rol, boolean esDemo, String nombreUsuario) {
        Intent intent = new Intent(Login.this, MainActivity.class);
        intent.putExtra(EXTRA_ROL, rol);
        intent.putExtra(MainActivity.EXTRA_MODO_DEMO, esDemo);
        intent.putExtra(EXTRA_USUARIO_ACTUAL, nombreUsuario);
        startActivity(intent);
        finish();
    }
}
