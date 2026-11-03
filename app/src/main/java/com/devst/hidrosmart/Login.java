package com.devst.hidrosmart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class Login extends AppCompatActivity {

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

            if(usuario.isEmpty() || contrasena.isEmpty())
            {
                Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if(usuario.equals("admin") && contrasena.equals("1234"))
            {
                Intent intent = new Intent(Login.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            else
            {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }

        });



        btnDemo.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_MODO_DEMO, true);
            startActivity(intent);
        });
    }
}