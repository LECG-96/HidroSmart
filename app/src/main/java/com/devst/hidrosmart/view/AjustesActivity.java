package com.devst.hidrosmart.view;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.devst.hidrosmart.HidroSmartApp;
import com.devst.hidrosmart.R;

public class AjustesActivity extends AppCompatActivity {
    private static final String TEMA_SISTEMA = "sistema";
    private static final String TEMA_CLARO = "claro";
    private static final String TEMA_OSCURO = "oscuro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        Button btnVolver = findViewById(R.id.btnVolverAjustes);
        RadioGroup grupoTema = findViewById(R.id.grupoTema);
        btnVolver.setOnClickListener(v -> finish());

        SharedPreferences preferencias = getSharedPreferences(HidroSmartApp.PREFERENCIAS, MODE_PRIVATE);
        String temaActual = preferencias.getString(HidroSmartApp.CLAVE_TEMA, TEMA_SISTEMA);
        grupoTema.check(idParaTema(temaActual));

        grupoTema.setOnCheckedChangeListener((group, checkedId) -> {
            String tema = temaParaId(checkedId);
            preferencias.edit().putString(HidroSmartApp.CLAVE_TEMA, tema).apply();
            AppCompatDelegate.setDefaultNightMode(HidroSmartApp.modoNoche(tema));
        });
    }

    private int idParaTema(String tema) {
        if (TEMA_CLARO.equals(tema)) return R.id.radioTemaClaro;
        if (TEMA_OSCURO.equals(tema)) return R.id.radioTemaOscuro;
        return R.id.radioTemaSistema;
    }

    private String temaParaId(int id) {
        if (id == R.id.radioTemaClaro) return TEMA_CLARO;
        if (id == R.id.radioTemaOscuro) return TEMA_OSCURO;
        return TEMA_SISTEMA;
    }
}
