package com.devst.hidrosmart.view;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.Alerta;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MODO_DEMO = "modo_demo";
    private static final String[] CULTIVOS_DEFECTO = {"lechuga", "albahaca", "cilantro", "menta"};

    private final List<Huerto> torres = new ArrayList<>();
    private int torreSeleccionada = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnableSimulador;

    // Vistas de Medidas
    private TextView tvTempAire, tvHumedad, tvTempAgua, tvPh, tvEc, tvLux, tvNivel;
    private TextView tvRangoAire, tvRangoAgua, tvRangoPh, tvRangoEc;

    // Vistas de Actuadores
    private TextView actVentilador, actLed, actBomba, actNutrientes, actPhPump, actCerradura;

    // AlertasFragment y control
    private TextView tvAlertas, tvLabelCultivo;
    private Spinner spnCultivo, spnCantidadTorres;
    private TabLayout tabTorres;
    private boolean actualizandoSpinnerCultivo = false;

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

        inicializarVistas();
        configurarSpinners();
        configurarBotonesEscenarios();
        reconstruirTorres(3); // 3 torres por defecto
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
        tvLabelCultivo = findViewById(R.id.tvLabelCultivo);
        spnCultivo = findViewById(R.id.spnCultivo);
        spnCantidadTorres = findViewById(R.id.spnCantidadTorres);
        tabTorres = findViewById(R.id.tabTorres);
    }

    private void configurarSpinners() {
        // Cantidad de torres (1 a 4)
        Integer[] opciones = {1, 2, 3, 4};
        ArrayAdapter<Integer> adapterCant = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opciones);
        adapterCant.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCantidadTorres.setAdapter(adapterCant);
        spnCantidadTorres.setSelection(2); // 3 torres

        spnCantidadTorres.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int cantidad = (Integer) parent.getItemAtPosition(position);
                if (cantidad != torres.size()) {
                    reconstruirTorres(cantidad);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Spinner Cultivos
        List<PerfilCultivo> lista = PerfilCultivo.todos();
        ArrayAdapter<PerfilCultivo> adapterCultivos = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lista);
        adapterCultivos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCultivo.setAdapter(adapterCultivos);

        spnCultivo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (actualizandoSpinnerCultivo) return;
                Huerto actual = getHuertoActual();
                if (actual != null) {
                    PerfilCultivo seleccionado = (PerfilCultivo) parent.getItemAtPosition(position);
                    actual.setPerfil(seleccionado.clave);
                    actualizarTitulosTabs();
                    actualizarRangosTexto(seleccionado);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        tabTorres.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                torreSeleccionada = tab.getPosition();
                sincronizarVistaConTorre();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void reconstruirTorres(int n) {
        torres.clear();
        for (int i = 0; i < n; i++) {
            String cultivo = CULTIVOS_DEFECTO[i % CULTIVOS_DEFECTO.length];
            torres.add(new Huerto(cultivo, 20.0, 60.0, 10.0, i + 1));
        }

        tabTorres.removeAllTabs();
        for (int i = 0; i < n; i++) {
            tabTorres.addTab(tabTorres.newTab().setText("T" + (i + 1)));
        }

        torreSeleccionada = 0;
        actualizarTitulosTabs();
        sincronizarVistaConTorre();
    }

    private void actualizarTitulosTabs() {
        for (int i = 0; i < torres.size(); i++) {
            TabLayout.Tab tab = tabTorres.getTabAt(i);
            if (tab != null) {
                tab.setText(String.format("T%d · %s", torres.get(i).idx, torres.get(i).perfil.nombre));
            }
        }
    }

    private Huerto getHuertoActual() {
        if (torreSeleccionada >= 0 && torreSeleccionada < torres.size()) {
            return torres.get(torreSeleccionada);
        }
        return null;
    }

    private void sincronizarVistaConTorre() {
        Huerto actual = getHuertoActual();
        if (actual == null) return;

        tvLabelCultivo.setText("Cultivo de Torre " + actual.idx);
        actualizandoSpinnerCultivo = true;
        List<PerfilCultivo> todos = PerfilCultivo.todos();
        for (int i = 0; i < todos.size(); i++) {
            if (todos.get(i).clave.equals(actual.perfil.clave)) {
                spnCultivo.setSelection(i);
                break;
            }
        }
        actualizandoSpinnerCultivo = false;
        actualizarRangosTexto(actual.perfil);
        actualizarUI();
    }

    private void actualizarRangosTexto(PerfilCultivo p) {
        tvRangoAire.setText(String.format(Locale.US, "(%.0f - %.0f °C)", p.aireMin, p.aireMax));
        tvRangoAgua.setText(String.format(Locale.US, "(%.0f - %.0f °C)", p.tempAguaMin, p.tempAguaMax));
        tvRangoPh.setText(String.format(Locale.US, "(%.1f - %.1f)", p.phMin, p.phMax));
        tvRangoEc.setText(String.format(Locale.US, "(%.1f - %.1f)", p.ecMin, p.ecMax));
    }

    private void configurarBotonesEscenarios() {
        vincularEscenario(R.id.btnEscCalor, "calor");
        vincularEscenario(R.id.btnEscFrio, "frio");
        vincularEscenario(R.id.btnEscAguaCal, "agua_caliente");
        vincularEscenario(R.id.btnEscAguaFria, "agua_fria");
        vincularEscenario(R.id.btnEscPhAlto, "ph_alto");
        vincularEscenario(R.id.btnEscPhBajo, "ph_bajo");
        vincularEscenario(R.id.btnEscEcBaja, "ec_bajo");
        vincularEscenario(R.id.btnEscEcAlta, "ec_alto");
        vincularEscenario(R.id.btnEscOscuridad, "oscuridad");
        vincularEscenario(R.id.btnEscNivelBajo, "nivel_bajo");
        vincularEscenario(R.id.btnEscNivelCritico, "nivel_critico");
        vincularEscenario(R.id.btnEscNormal, "normal");

        Button btnRellenar = findViewById(R.id.btnRellenarEstanque);
        btnRellenar.setOnClickListener(v -> {
            Huerto h = getHuertoActual();
            if (h != null) {
                synchronized (h.lock) {
                    h.v.put("nivel_agua", 100.0);
                }
                Toast.makeText(this, "Estanque T" + h.idx + " rellenado al 100%", Toast.LENGTH_SHORT).show();
                actualizarUI();
            }
        });
    }

    private void vincularEscenario(int botonId, String escenario) {
        Button btn = findViewById(botonId);
        btn.setOnClickListener(v -> {
            Huerto h = getHuertoActual();
            if (h != null) {
                h.aplicarEscenario(escenario);
                Toast.makeText(this, "Escenario aplicado: " + escenario, Toast.LENGTH_SHORT).show();
                actualizarUI();
            }
        });
    }

    private void iniciarBucleSimulacion() {
        runnableSimulador = new Runnable() {
            @Override
            public void run() {
                double ahora = System.currentTimeMillis() / 1000.0;
                for (Huerto h : torres) {
                    h.evolucionar();
                    if (!h.modoManual) {
                        h.automatizar(ahora);
                    }
                    h.temporizadores(ahora);
                }
                actualizarUI();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnableSimulador);
    }

    private void actualizarUI() {
        Huerto actual = getHuertoActual();
        if (actual == null) return;

        PerfilCultivo p = actual.perfil;

        double aire = actual.v.get("temp_ambiente");
        double hum = actual.v.get("humedad");
        double agua = actual.v.get("temp_agua");
        double ph = actual.v.get("ph");
        double ec = actual.v.get("ec");
        double lux = actual.v.get("lux");
        double nivel = actual.v.get("nivel_agua");

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

        actualizarActuador(actVentilador, "Ventilador", actual.act.get("ventilador"));
        actualizarActuador(actLed, "LED Cultivo", actual.act.get("led"));
        actualizarActuador(actBomba, "Riego", actual.act.get("bomba_riego"));
        actualizarActuador(actNutrientes, "Nutrientes", actual.act.get("nutrientes"));
        actualizarActuador(actPhPump, "pH Down", actual.act.get("ph_pump"));
        actualizarActuador(actCerradura, "Cerradura", actual.act.get("cerradura"));

        List<Alerta> alertas = actual.alertas();
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