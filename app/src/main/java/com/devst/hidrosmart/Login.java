package com.devst.hidrosmart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnIngresar = findViewById(R.id.btnIngresar);
        Button btnDemo = findViewById(R.id.btnDemo);

        // El login con usuarios reales se hace en un paso posterior
        btnIngresar.setOnClickListener(v ->
                Toast.makeText(this, R.string.login_proximamente, Toast.LENGTH_SHORT).show());

        // Usuario Demo: entra sin clave y sin tocar el sistema real
        btnDemo.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_MODO_DEMO, true);
            startActivity(intent);
        });
    }
}