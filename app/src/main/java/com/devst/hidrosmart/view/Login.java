package com.devst.hidrosmart.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.data.AutenticacionDataSource;
import com.devst.hidrosmart.data.HuertoDataSourceProvider;

public class Login extends AppCompatActivity {

    public static final String EXTRA_ROL = "extra_rol";
    public static final String EXTRA_USUARIO_ACTUAL = "extra_usuario_actual";
    public static final String ROL_ADMIN = AutenticacionDataSource.ROL_ADMIN;
    public static final String ROL_USER = AutenticacionDataSource.ROL_USER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etUsuario = findViewById(R.id.etUsuario);
        EditText etContrasena = findViewById(R.id.etContrasena);
        Button btnIngresar = findViewById(R.id.btnIngresar);
        Button btnDemo = findViewById(R.id.btnDemo);
        AutenticacionDataSource autenticacion = HuertoDataSourceProvider.getAutenticacion();

        btnIngresar.setOnClickListener(v -> {
            String usuario = etUsuario.getText().toString().trim();
            String contrasena = etContrasena.getText().toString();

            if (usuario.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, R.string.mensaje_campos_incompletos, Toast.LENGTH_SHORT).show();
                return;
            }

            String rol = autenticacion.autenticar(usuario, contrasena);
            if (rol == null) {
                Toast.makeText(this, R.string.mensaje_login_incorrecto, Toast.LENGTH_SHORT).show();
            } else {
                abrirMain(rol, false, usuario);
            }
        });

        // Modo Demo: entra como admin para ver la simulación, pero bloquea pestañas de supervisión
        btnDemo.setOnClickListener(v -> abrirMain(ROL_ADMIN, true, "demo"));
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
