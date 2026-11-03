package com.devst.hidrosmart;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    /** Clave del Intent: es true cuando se entra con el usuario Demo. */
    public static final String EXTRA_MODO_DEMO = "modo_demo";

    private boolean modoDemo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        modoDemo = getIntent().getBooleanExtra(EXTRA_MODO_DEMO, false);
        TextView tvEstado = findViewById(R.id.tvEstado);
        tvEstado.setText(modoDemo ? R.string.main_modo_demo : R.string.main_modo_real);
    }
}