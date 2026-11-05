package com.devst.hidrosmart.view;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.Alerta;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.devst.hidrosmart.modelo.RespuestaComando;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;
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

    // Vistas de Controles Manuales
    private SwitchMaterial swModoManual, swCorteDatos;
    private MaterialCheckBox chkVentilador, chkLed, chkBomba, chkNutrientes, chkPhPump, chkCerradura;

    // Sliders para forzar valores
    private SeekBar seekAire, seekHum, seekAgua, seekPh, seekEc, seekLux, seekNivel;
    private TextView lblSeekAire, lblSeekHum, lblSeekAgua, lblSeekPh, lblSeekEc, lblSeekLux, lblSeekNivel;
    private boolean usuarioArrastrandoSlider = false;

    // Alertas y control
    private TextView tvAlertas, tvLabelCultivo;
    private Spinner spnCultivo, spnCantidadTorres;
    private TabLayout tabTorres;
    private boolean actualizandoSpinnerCultivo = false;
    private boolean actualizandoCheckboxes = false;

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
        configurarControlesManuales();
        configurarSliders();
        configurarBotonesEscenarios();
        reconstruirTorres(3);
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

        swModoManual = findViewById(R.id.swModoManual);
        swCorteDatos = findViewById(R.id.swCorteDatos);

        chkVentilador = findViewById(R.id.chkVentilador);
        chkLed = findViewById(R.id.chkLed);
        chkBomba = findViewById(R.id.chkBomba);
        chkNutrientes = findViewById(R.id.chkNutrientes);
        chkPhPump = findViewById(R.id.chkPhPump);
        chkCerradura = findViewById(R.id.chkCerradura);

        seekAire = findViewById(R.id.seekAire);
        seekHum = findViewById(R.id.seekHum);
        seekAgua = findViewById(R.id.seekAgua);
        seekPh = findViewById(R.id.seekPh);
        seekEc = findViewById(R.id.seekEc);
        seekLux = findViewById(R.id.seekLux);
        seekNivel = findViewById(R.id.seekNivel);

        lblSeekAire = findViewById(R.id.lblSeekAire);
        lblSeekHum = findViewById(R.id.lblSeekHum);
        lblSeekAgua = findViewById(R.id.lblSeekAgua);
        lblSeekPh = findViewById(R.id.lblSeekPh);
        lblSeekEc = findViewById(R.id.lblSeekEc);
        lblSeekLux = findViewById(R.id.lblSeekLux);
        lblSeekNivel = findViewById(R.id.lblSeekNivel);

        tvAlertas = findViewById(R.id.tvAlertas);
        tvLabelCultivo = findViewById(R.id.tvLabelCultivo);
        spnCultivo = findViewById(R.id.spnCultivo);
        spnCantidadTorres = findViewById(R.id.spnCantidadTorres);
        tabTorres = findViewById(R.id.tabTorres);
    }

    private void configurarSpinners() {
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

    private void configurarControlesManuales() {
        swModoManual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Huerto h = getHuertoActual();
            if (h != null) {
                double ahora = System.currentTimeMillis() / 1000.0;
                h.comando("modo", isChecked ? "manual" : "auto", ahora);
                habilitarCheckboxesManuales(isChecked);
                if (!isChecked) {
                    sincronizarCheckboxes(h);
                }
                actualizarUI();
            }
        });

        vincularComandoCheckbox(chkVentilador, "ventilador");
        vincularComandoCheckbox(chkLed, "led");
        vincularComandoCheckbox(chkBomba, "bomba_riego");
        vincularComandoCheckbox(chkNutrientes, "nutrientes");
        vincularComandoCheckbox(chkPhPump, "ph_pump");
        vincularComandoCheckbox(chkCerradura, "cerradura");
    }

    private void vincularComandoCheckbox(MaterialCheckBox chk, String actuador) {
        chk.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (actualizandoCheckboxes) return;
            Huerto h = getHuertoActual();
            if (h != null && h.modoManual) {
                double ahora = System.currentTimeMillis() / 1000.0;
                RespuestaComando res = h.comando(actuador, isChecked ? "on" : "off", ahora);
                if (!res.aceptado) {
                    actualizandoCheckboxes = true;
                    chk.setChecked(!isChecked);
                    actualizandoCheckboxes = false;
                    Toast.makeText(this, res.mensaje, Toast.LENGTH_SHORT).show();
                }
                actualizarUI();
            }
        });
    }

    private void habilitarCheckboxesManuales(boolean manual) {
        chkVentilador.setEnabled(manual);
        chkLed.setEnabled(manual);
        chkBomba.setEnabled(manual);
        chkNutrientes.setEnabled(manual);
        chkPhPump.setEnabled(manual);
        chkCerradura.setEnabled(manual);
    }

    private void sincronizarCheckboxes(Huerto h) {
        actualizandoCheckboxes = true;
        chkVentilador.setChecked(Boolean.TRUE.equals(h.act.get("ventilador")));
        chkLed.setChecked(Boolean.TRUE.equals(h.act.get("led")));
        chkBomba.setChecked(Boolean.TRUE.equals(h.act.get("bomba_riego")));
        chkNutrientes.setChecked(Boolean.TRUE.equals(h.act.get("nutrientes")));
        chkPhPump.setChecked(Boolean.TRUE.equals(h.act.get("ph_pump")));
        chkCerradura.setChecked(Boolean.TRUE.equals(h.act.get("cerradura")));
        actualizandoCheckboxes = false;
    }

    private void configurarSliders() {
        // Aire (0 a 45 °C, persistente)
        seekAire.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekAire.setText(String.format(Locale.US, "Temp. Aire: %d.0 °C", progress));
                if (fromUser) aplicarValorSlider("temp_ambiente", (double) progress, true);
            }
        });

        // Humedad (0 a 100 %, persistente)
        seekHum.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekHum.setText(String.format(Locale.US, "Humedad: %d %%", progress));
                if (fromUser) aplicarValorSlider("humedad", (double) progress, true);
            }
        });

        // Agua (5 a 35 °C, progress 0 a 30)
        seekAgua.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = 5.0 + progress;
                lblSeekAgua.setText(String.format(Locale.US, "Temp. Agua: %.1f °C", val));
                if (fromUser) aplicarValorSlider("temp_agua", val, true);
            }
        });

        // pH (3.0 a 9.0, progress 0 a 60, paso 0.1)
        seekPh.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = 3.0 + (progress / 10.0);
                lblSeekPh.setText(String.format(Locale.US, "pH: %.1f", val));
                if (fromUser) aplicarValorSlider("ph", val, false);
            }
        });

        // EC (0.0 a 4.5, progress 0 a 45, paso 0.1)
        seekEc.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = progress / 10.0;
                lblSeekEc.setText(String.format(Locale.US, "EC: %.1f mS/cm", val));
                if (fromUser) aplicarValorSlider("ec", val, false);
            }
        });

        // Luz (0 a 10000 lux, progress 0 a 100, paso 100)
        seekLux.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = progress * 100.0;
                lblSeekLux.setText(String.format(Locale.US, "Luz: %.0f lux", val));
                if (fromUser) aplicarValorSlider("lux", val, true);
            }
        });

        // Nivel (0 a 100 %)
        seekNivel.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekNivel.setText(String.format(Locale.US, "Nivel Estanque: %d %%", progress));
                if (fromUser) aplicarValorSlider("nivel_agua", (double) progress, false);
            }
        });
    }

    private void aplicarValorSlider(String variable, double valor, boolean persistente) {
        Huerto h = getHuertoActual();
        if (h != null) {
            synchronized (h.lock) {
                h.v.put(variable, valor);
                if (persistente && h.base.containsKey(variable)) {
                    h.base.put(variable, valor);
                }
            }
            actualizarUI();
        }
    }

    private void sincronizarSliders(Huerto h) {
        if (usuarioArrastrandoSlider) return;

        double aire = h.v.get("temp_ambiente");
        seekAire.setProgress((int) Math.round(aire));
        lblSeekAire.setText(String.format(Locale.US, "Temp. Aire: %.1f °C", aire));

        double hum = h.v.get("humedad");
        seekHum.setProgress((int) Math.round(hum));
        lblSeekHum.setText(String.format(Locale.US, "Humedad: %.0f %%", hum));

        double agua = h.v.get("temp_agua");
        seekAgua.setProgress((int) Math.max(0, Math.round(agua - 5.0)));
        lblSeekAgua.setText(String.format(Locale.US, "Temp. Agua: %.1f °C", agua));

        double ph = h.v.get("ph");
        seekPh.setProgress((int) Math.max(0, Math.round((ph - 3.0) * 10.0)));
        lblSeekPh.setText(String.format(Locale.US, "pH: %.2f", ph));

        double ec = h.v.get("ec");
        seekEc.setProgress((int) Math.max(0, Math.round(ec * 10.0)));
        lblSeekEc.setText(String.format(Locale.US, "EC: %.2f mS/cm", ec));

        double lux = h.v.get("lux");
        seekLux.setProgress((int) Math.max(0, Math.round(lux / 100.0)));
        lblSeekLux.setText(String.format(Locale.US, "Luz: %.0f lux", lux));

        double nivel = h.v.get("nivel_agua");
        seekNivel.setProgress((int) Math.round(nivel));
        lblSeekNivel.setText(String.format(Locale.US, "Nivel Estanque: %.0f %%", nivel));
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

        swModoManual.setChecked(actual.modoManual);
        habilitarCheckboxesManuales(actual.modoManual);
        sincronizarCheckboxes(actual);
        sincronizarSliders(actual);

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
                sincronizarSliders(h);
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
                sincronizarSliders(h);
                sincronizarCheckboxes(h);
                actualizarUI();
                Toast.makeText(this, "Escenario aplicado: " + escenario, Toast.LENGTH_SHORT).show();
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

                // Si está activo el corte de datos, no refresca la pantalla
                if (!swCorteDatos.isChecked()) {
                    actualizarUI();
                }

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

        sincronizarCheckboxes(actual);
        sincronizarSliders(actual);

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

    private abstract class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            usuarioArrastrandoSlider = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            usuarioArrastrandoSlider = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnableSimulador != null) {
            handler.removeCallbacks(runnableSimulador);
        }
    }
}