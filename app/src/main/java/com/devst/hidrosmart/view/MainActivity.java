package com.devst.hidrosmart.view;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.Alerta;
import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.devst.hidrosmart.modelo.RespuestaComando;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MODO_DEMO = "modo_demo";
    private static final String[] CULTIVOS_DEFECTO = {"lechuga", "albahaca", "cilantro", "menta", "acelga", "tomate_cherry"};

    // Solo los usuarios registrados realmente en el sistema
    public static final List<String> USUARIOS_GENERALES = new ArrayList<>();
    public static final List<ComentarioUsuario> COMENTARIOS_REGISTRADOS = new ArrayList<>();

    static {
        // Cuenta general base registrada en la app
        USUARIOS_GENERALES.add("user");
    }

    private final List<Huerto> torres = new ArrayList<>();
    private int torreSeleccionada = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnableSimulador;

    // Vistas contenedoras
    private View vistaAdmin;
    private View vistaUsuario;
    private TabLayout tabSeccionesAdmin;
    private View seccionControlTorres;
    private View seccionUsuariosAdmin;
    private ListView lvUsuariosGenerales;

    // Controles Admin
    private TextView tvTempAire, tvHumedad, tvTempAgua, tvPh, tvEc, tvLux, tvNivel;
    private TextView tvRangoAire, tvRangoAgua, tvRangoPh, tvRangoEc;
    private SwitchMaterial swModoManual, swCorteDatos;
    private MaterialCheckBox chkVentilador, chkLed, chkBomba, chkNutrientes, chkPhPump, chkCerradura;
    private SeekBar seekAire, seekHum, seekAgua, seekPh, seekEc, seekLux, seekNivel;
    private TextView lblSeekAire, lblSeekHum, lblSeekAgua, lblSeekPh, lblSeekEc, lblSeekLux, lblSeekNivel;
    private TextView tvAlertas, tvLabelCultivo;
    private Spinner spnCultivo;
    private TabLayout tabTorres;
    private boolean usuarioArrastrandoSlider = false;
    private boolean actualizandoSpinnerCultivo = false;
    private boolean actualizandoCheckboxes = false;

    // Controles Usuario General
    private Spinner spnTorreComentario;
    private EditText etComentario;
    private Button btnEnviarComentario;
    private TextView tvUltimoComentario;

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

        String rol = getIntent().getStringExtra(Login.EXTRA_ROL);
        if (rol == null) rol = Login.ROL_ADMIN;
        boolean esModoDemo = getIntent().getBooleanExtra(EXTRA_MODO_DEMO, false);

        vistaAdmin = findViewById(R.id.vistaAdmin);
        vistaUsuario = findViewById(R.id.vistaUsuario);
        tabSeccionesAdmin = findViewById(R.id.tabSeccionesAdmin);
        seccionControlTorres = findViewById(R.id.seccionControlTorres);
        seccionUsuariosAdmin = findViewById(R.id.seccionUsuariosAdmin);
        lvUsuariosGenerales = findViewById(R.id.lvUsuariosGenerales);

        reconstruirTorres(2);

        if (Login.ROL_ADMIN.equals(rol)) {
            vistaAdmin.setVisibility(View.VISIBLE);
            vistaUsuario.setVisibility(View.GONE);

            // Si es MODO DEMO, bloquear/ocultar completamente la pestaña de supervisión de usuarios
            if (esModoDemo) {
                tabSeccionesAdmin.setVisibility(View.GONE);
                seccionUsuariosAdmin.setVisibility(View.GONE);
                seccionControlTorres.setVisibility(View.VISIBLE);
                TextView tvAdminTitulo = findViewById(R.id.tvAdminTitulo);
                if (tvAdminTitulo != null) {
                    tvAdminTitulo.setText("HidroSmart • Modo Demo");
                }
            } else {
                tabSeccionesAdmin.setVisibility(View.VISIBLE);
                configurarPestanasAdmin();
                configurarListaUsuariosAdmin();
            }

            inicializarVistasAdmin();
            configurarControlesAdmin();
            iniciarBucleSimulacion();
        } else {
            vistaAdmin.setVisibility(View.GONE);
            vistaUsuario.setVisibility(View.VISIBLE);
            inicializarVistasUsuario();
        }
    }

    // ----------------------------------------------------
    // GESTIÓN DE PESTAÑAS ADMIN (CONTROL VS USUARIOS REGISTRADOS)
    // ----------------------------------------------------
    private void configurarPestanasAdmin() {
        tabSeccionesAdmin.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    seccionControlTorres.setVisibility(View.VISIBLE);
                    seccionUsuariosAdmin.setVisibility(View.GONE);
                } else {
                    seccionControlTorres.setVisibility(View.GONE);
                    seccionUsuariosAdmin.setVisibility(View.VISIBLE);
                    actualizarListaUsuariosAdmin();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void configurarListaUsuariosAdmin() {
        actualizarListaUsuariosAdmin();
        lvUsuariosGenerales.setOnItemClickListener((parent, view, position, id) -> {
            String usuarioSeleccionado = USUARIOS_GENERALES.get(position);
            mostrarDialogoComentariosUsuario(usuarioSeleccionado);
        });
    }

    private void actualizarListaUsuariosAdmin() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, USUARIOS_GENERALES);
        lvUsuariosGenerales.setAdapter(adapter);
    }

    private void mostrarDialogoComentariosUsuario(String usuarioNombre) {
        StringBuilder sb = new StringBuilder();

        for (ComentarioUsuario c : COMENTARIOS_REGISTRADOS) {
            if (c.usuario.equals(usuarioNombre)) {
                sb.append("• [").append(c.fechaHora).append("] ").append(c.torre).append(":\n")
                        .append("\"").append(c.mensaje).append("\"\n\n");
            }
        }

        if (sb.length() == 0) {
            sb.append("Este usuario aún no registra comentarios u observaciones.");
        }

        new AlertDialog.Builder(this)
                .setTitle("Observaciones de: " + usuarioNombre)
                .setMessage(sb.toString().trim())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    // ----------------------------------------------------
    // INICIALIZACIÓN VISTA ADMIN (SIMULADOR / TORRES)
    // ----------------------------------------------------
    private void inicializarVistasAdmin() {
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
        tabTorres = findViewById(R.id.tabTorres);
    }

    private void configurarControlesAdmin() {
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

        findViewById(R.id.btnAgregarTorre).setOnClickListener(v -> {
            if (torres.size() >= 4) {
                Toast.makeText(this, "Máximo 4 torres alcanzado", Toast.LENGTH_SHORT).show();
                return;
            }
            int nuevoIdx = torres.size() + 1;
            String cultivo = CULTIVOS_DEFECTO[(nuevoIdx - 1) % CULTIVOS_DEFECTO.length];
            torres.add(new Huerto(cultivo, 20.0, 60.0, 10.0, nuevoIdx));
            actualizarTabsDesdeCero();
            Toast.makeText(this, "Torre " + nuevoIdx + " agregada", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnEliminarTorre).setOnClickListener(v -> {
            if (torres.size() <= 1) {
                Toast.makeText(this, "Debe quedar al menos 1 torre", Toast.LENGTH_SHORT).show();
                return;
            }
            int removedIdx = torres.size();
            torres.remove(torres.size() - 1);
            if (torreSeleccionada >= torres.size()) {
                torreSeleccionada = torres.size() - 1;
            }
            actualizarTabsDesdeCero();
            Toast.makeText(this, "Torre " + removedIdx + " eliminada", Toast.LENGTH_SHORT).show();
        });

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

        configurarSliders();
        configurarBotonesEscenarios();
        actualizarTabsDesdeCero();
    }

    // ----------------------------------------------------
    // INICIALIZACIÓN VISTA USUARIO GENERAL
    // ----------------------------------------------------
    private void inicializarVistasUsuario() {
        spnTorreComentario = findViewById(R.id.spnTorreComentario);
        etComentario = findViewById(R.id.etComentario);
        btnEnviarComentario = findViewById(R.id.btnEnviarComentario);
        tvUltimoComentario = findViewById(R.id.tvUltimoComentario);

        String usuarioActual = getIntent().getStringExtra(Login.EXTRA_USUARIO_ACTUAL);
        if (usuarioActual == null) usuarioActual = "user";
        final String remitente = usuarioActual;

        List<String> opciones = new ArrayList<>();
        for (Huerto h : torres) {
            opciones.add(String.format("Torre %d (%s)", h.idx, h.perfil.nombre));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTorreComentario.setAdapter(adapter);

        btnEnviarComentario.setOnClickListener(v -> {
            String texto = etComentario.getText().toString().trim();
            if (texto.isEmpty()) {
                Toast.makeText(this, "Escribe una observación antes de enviar", Toast.LENGTH_SHORT).show();
                return;
            }
            String seleccion = spnTorreComentario.getSelectedItem().toString();
            String horaActual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            COMENTARIOS_REGISTRADOS.add(new ComentarioUsuario(remitente, seleccion, texto, horaActual));

            tvUltimoComentario.setVisibility(View.VISIBLE);
            tvUltimoComentario.setText(String.format("✓ Observación guardada para %s:\n\"%s\"", seleccion, texto));
            etComentario.setText("");
            Toast.makeText(this, "Observación enviada al supervisor", Toast.LENGTH_SHORT).show();
        });
    }

    // ----------------------------------------------------
    // MÉTODOS DEL SIMULADOR
    // ----------------------------------------------------
    private void reconstruirTorres(int n) {
        torres.clear();
        for (int i = 0; i < n; i++) {
            String cultivo = CULTIVOS_DEFECTO[i % CULTIVOS_DEFECTO.length];
            torres.add(new Huerto(cultivo, 20.0, 60.0, 10.0, i + 1));
        }
        torreSeleccionada = 0;
    }

    private void actualizarTabsDesdeCero() {
        if (tabTorres == null) return;
        tabTorres.removeAllTabs();
        for (int i = 0; i < torres.size(); i++) {
            tabTorres.addTab(tabTorres.newTab().setText("T" + (i + 1)));
        }
        actualizarTitulosTabs();
        TabLayout.Tab tab = tabTorres.getTabAt(torreSeleccionada);
        if (tab != null) tab.select();
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
        if (actual == null || tvLabelCultivo == null) return;

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
        seekAire.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekAire.setText(String.format(Locale.US, "Temp. Aire: %d.0 °C", progress));
                if (fromUser) aplicarValorSlider("temp_ambiente", (double) progress, true);
            }
        });

        seekHum.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekHum.setText(String.format(Locale.US, "Humedad: %d %%", progress));
                if (fromUser) aplicarValorSlider("humedad", (double) progress, true);
            }
        });

        seekAgua.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = 5.0 + progress;
                lblSeekAgua.setText(String.format(Locale.US, "Temp. Agua: %.1f °C", val));
                if (fromUser) aplicarValorSlider("temp_agua", val, true);
            }
        });

        seekPh.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = 3.0 + (progress / 10.0);
                lblSeekPh.setText(String.format(Locale.US, "pH: %.1f", val));
                if (fromUser) aplicarValorSlider("ph", val, false);
            }
        });

        seekEc.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = progress / 10.0;
                lblSeekEc.setText(String.format(Locale.US, "EC: %.1f mS/cm", val));
                if (fromUser) aplicarValorSlider("ec", val, false);
            }
        });

        seekLux.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = progress * 100.0;
                lblSeekLux.setText(String.format(Locale.US, "Luz: %.0f lux", val));
                if (fromUser) aplicarValorSlider("lux", val, true);
            }
        });

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
        if (usuarioArrastrandoSlider || seekAire == null) return;

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

                if (swCorteDatos != null && !swCorteDatos.isChecked()) {
                    actualizarUI();
                }

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnableSimulador);
    }

    private void actualizarUI() {
        Huerto actual = getHuertoActual();
        if (actual == null || tvTempAire == null) return;

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