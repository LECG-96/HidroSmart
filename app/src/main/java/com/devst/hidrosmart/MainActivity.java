package com.devst.hidrosmart;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.devst.hidrosmart.modelo.Alerta;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.PerfilCultivo;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MODO_DEMO = "modo_demo";

    private Huerto huerto;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnableSimulador;

    // Vistas de Medidas
    private TextView tvTempAire, tvHumedad, tvTempAgua, tvPh, tvEc, tvLux, tvNivel;
    private TextView tvRangoAire, tvRangoAgua, tvRangoPh, tvRangoEc;

    // Vistas de Actuadores
    private TextView actVentilador, actLed, actBomba, actNutrientes, actPhPump, actCerradura;

    // Alertas y Modo
    private TextView tvAlertas, tvModoBadge;
    private Spinner spnCultivo;

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

        // Inicializar Huerto con tiempos de simulación ágiles (20s espera dosis, 60s riego, 10s duración)
        huerto = new Huerto("lechuga", 20.0, 60.0, 10.0, 1);

        inicializarVistas();
        configurarSpinnerCultivos();
        iniciarBucleSimulacion();
    }

    private void inicializarVistas() {
        tvTempAire = findViewById(R.id.tvTempAire);
        tvHumedad = findViewById(R.id.tvHumedad);
        tvTempAgua = findViewById(R.id.tvTempAgua);
        tvPh = findViewById(R.id.tvPh);
        tvEc = findViewById(R.id.tvEc);
        tvLux = findViewById(R.id.tvLux);
        tvNivel = findViewById(R.id.tvNivel);

        tvRangoAire = findViewById(R.id.tvRangoAire);
        tvRangoAgua = findViewById(R.id.tvRangoAgua);
        tvRangoPh = findViewById(R.id.tvRangoPh);
        tvRangoEc = findViewById(R.id.tvRangoEc);

        actVentilador = findViewById(R.id.actVentilador);
        actLed = findViewById(R.id.actLed);
        actBomba = findViewById(R.id.actBomba);
        actNutrientes = findViewById(R.id.actNutrientes);
        actPhPump = findViewById(R.id.actPhPump);
        actCerradura = findViewById(R.id.actCerradura);

        tvAlertas = findViewById(R.id.tvAlertas);
        tvModoBadge = findViewById(R.id.tvModoBadge);
        spnCultivo = findViewById(R.id.spnCultivo);
    }

    private void configurarSpinnerCultivos() {
        List<PerfilCultivo> lista = PerfilCultivo.todos();
        ArrayAdapter<PerfilCultivo> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lista);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCultivo.setAdapter(adapter);

        spnCultivo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PerfilCultivo seleccionado = (PerfilCultivo) parent.getItemAtPosition(position);
                huerto.setPerfil(seleccionado.clave);
                actualizarRangosTexto(seleccionado);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void actualizarRangosTexto(PerfilCultivo p) {
        tvRangoAire.setText(String.format(Locale.US, "(%.0f - %.0f °C)", p.aireMin, p.aireMax));
        tvRangoAgua.setText(String.format(Locale.US, "(%.0f - %.0f °C)", p.tempAguaMin, p.tempAguaMax));
        tvRangoPh.setText(String.format(Locale.US, "(%.1f - %.1f)", p.phMin, p.phMax));
        tvRangoEc.setText(String.format(Locale.US, "(%.1f - %.1f)", p.ecMin, p.ecMax));
    }

    private void iniciarBucleSimulacion() {
        runnableSimulador = new Runnable() {
            @Override
            public void run() {
                double ahora = System.currentTimeMillis() / 1000.0;

                // Paso de simulación
                huerto.evolucionar();
                if (!huerto.modoManual) {
                    huerto.automatizar(ahora);
                }
                huerto.temporizadores(ahora);

                actualizarUI();

                // Ciclo cada 1 segundo
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnableSimulador);
    }

    private void actualizarUI() {
        PerfilCultivo p = huerto.perfil;

        // Lecturas de sensores
        double aire = huerto.v.get("temp_ambiente");
        double hum = huerto.v.get("humedad");
        double agua = huerto.v.get("temp_agua");
        double ph = huerto.v.get("ph");
        double ec = huerto.v.get("ec");
        double lux = huerto.v.get("lux");
        double nivel = huerto.v.get("nivel_agua");

        tvTempAire.setText(String.format(Locale.US, "%.1f °C", aire));
        pintarValor(tvTempAire, aire < p.aireMin || aire > p.aireMax);

        tvHumedad.setText(String.format(Locale.US, "%.0f %%", hum));

        tvTempAgua.setText(String.format(Locale.US, "%.1f °C", agua));
        pintarValor(tvTempAgua, agua < p.tempAguaMin || agua > p.tempAguaMax);

        tvPh.setText(String.format(Locale.US, "%.2f", ph));
        pintarValor(tvPh, ph < p.phMin || ph > p.phMax);

        tvEc.setText(String.format(Locale.US, "%.2f mS/cm", ec));
        pintarValor(tvEc, ec < p.ecMin || ec > p.ecMax);

        tvLux.setText(String.format(Locale.US, "%.0f lux", lux));
        pintarValor(tvLux, lux < Huerto.LUX_LED_ON);

        tvNivel.setText(String.format(Locale.US, "%.0f %%", nivel));
        pintarValor(tvNivel, nivel < Huerto.NIVEL_BAJO);

        // Actuadores
        actualizarActuador(actVentilador, "Ventilador", huerto.act.get("ventilador"));
        actualizarActuador(actLed, "LED Cultivo", huerto.act.get("led"));
        actualizarActuador(actBomba, "Riego", huerto.act.get("bomba_riego"));
        actualizarActuador(actNutrientes, "Nutrientes", huerto.act.get("nutrientes"));
        actualizarActuador(actPhPump, "pH Down", huerto.act.get("ph_pump"));
        actualizarActuador(actCerradura, "Cerradura", huerto.act.get("cerradura"));

        // Alertas
        List<Alerta> alertas = huerto.alertas();
        if (alertas.isEmpty()) {
            tvAlertas.setText("✓ Todos los parámetros dentro de rango.");
            tvAlertas.setTextColor(Color.parseColor("#1B5E20"));
        } else {
            StringBuilder sb = new StringBuilder();
            for (Alerta a : alertas) {
                sb.append("⚠ ").append(a.variable.toUpperCase())
                        .append(" ").append(a.nivel.toUpperCase())
                        .append(" (").append(String.format(Locale.US, "%.1f", a.valor)).append(")\n")
                        .append("→ ").append(a.accion)
                        .append(a.automatica ? " [Auto]\n\n" : " [Acción manual requerida]\n\n");
            }
            tvAlertas.setText(sb.toString().trim());
            tvAlertas.setTextColor(Color.parseColor("#B71C1C"));
        }
    }

    private void pintarValor(TextView tv, boolean fueraDeRango) {
        tv.setTextColor(fueraDeRango ? Color.parseColor("#B71C1C") : Color.parseColor("#1B5E20"));
    }

    private void actualizarActuador(TextView tv, String nombre, Boolean activo) {
        boolean isOn = Boolean.TRUE.equals(activo);
        tv.setText(String.format("%s %s: %s", isOn ? "●" : "○", nombre, isOn ? "ON" : "OFF"));
        tv.setTextColor(isOn ? Color.parseColor("#1B5E20") : Color.parseColor("#757575"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnableSimulador != null) {
            handler.removeCallbacks(runnableSimulador);
        }
    }
}