package com.devst.hidrosmart;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class HidroSmartApp extends Application {
    public static final String PREFERENCIAS = "preferencias_hidrosmart";
    public static final String CLAVE_TEMA = "tema_aplicacion";

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences preferencias = getSharedPreferences(PREFERENCIAS, MODE_PRIVATE);
        String tema = preferencias.getString(CLAVE_TEMA, "sistema");
        AppCompatDelegate.setDefaultNightMode(modoNoche(tema));
    }

    public static int modoNoche(String tema) {
        if ("claro".equals(tema)) return AppCompatDelegate.MODE_NIGHT_NO;
        if ("oscuro".equals(tema)) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }
}
