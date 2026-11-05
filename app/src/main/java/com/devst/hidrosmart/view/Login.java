package com.devst.hidrosmart.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.devst.hidrosmart.R;

public class Login extends AppCompatActivity {

    public static final String EXTRA_ROL = "extra_rol";
    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_USER = "USER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etUsuario = findViewById(R.id.etUsuario);
        EditText etContrasena = findViewById(R.id.etContrasena);
        Button btnIngresar = findViewById(R.id.btnIngresar);
        Button btnDemo = findViewById(R.id.btnDemo);

        btnIngresar.setOnClickListener(v -> {
            String usuario = etUsuario.getText().toString().trim();
            String contrasena = etContrasena.getText().toString().trim();

            if (usuario.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (usuario.equals("admin") && contrasena.equals("1234")) {
                abrirMain(ROL_ADMIN, false);
            } else if (usuario.equals("user") && contrasena.equals("1234")) {
                abrirMain(ROL_USER, false);
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }
        });

        btnDemo.setOnClickListener(v -> abrirMain(ROL_ADMIN, true));
    }

    private void abrirMain(String rol, boolean esDemo) {
        Intent intent = new Intent(Login.this, MainActivity.class);
        intent.putExtra(EXTRA_ROL, rol);
        intent.putExtra(MainActivity.EXTRA_MODO_DEMO, esDemo);
        startActivity(intent);
        if (!esDemo) finish();
    }
}