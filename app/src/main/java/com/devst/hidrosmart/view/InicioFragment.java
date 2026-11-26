package com.devst.hidrosmart.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.GridLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.Alerta;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.HuertoSnapshot;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.devst.hidrosmart.viewmodel.HuertoViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

public class InicioFragment extends Fragment {

    private HuertoViewModel viewModel;

    private TextView tvTempAire, tvHumedad, tvTempAgua, tvPh, tvEc, tvLux, tvNivel;
    private TextView tvRangoAire, tvRangoAgua, tvRangoPh, tvRangoEc;
    private TextView tvAlertas, tvLabelCultivo, tvBannerCorte;
    private TextView tvAvisoSoloLectura, tvSinDatosReales;
    private LinearLayout layoutDatosHuerto;
    private Spinner spnCultivo;
    private TabLayout tabTorres;
    private View layoutAccionesDemo;
    private Button btnGuiaDemo, btnReiniciarDemo;
    private Button btnEditarRangos;
    private GridLayout gridEstadoSistema;
    private final Map<String, IndicadorSistemaView> indicadoresSistema = new LinkedHashMap<>();
    private final Map<String, TextView> textosEstadoSistema = new LinkedHashMap<>();
    private final Map<Integer, Double> nivelesAnteriores = new LinkedHashMap<>();

    private boolean actualizandoSpinner = false;
    private boolean soloDatosReales = false;
    private boolean esModoDemo = false;

    private final TabLayout.OnTabSelectedListener tabListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            viewModel.seleccionarTorre(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {}

        @Override
        public void onTabReselected(TabLayout.Tab tab) {}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(HuertoViewModel.class);

        inicializarVistas(view);
        configurarSpinnerCultivos();
        configurarModoSoloLectura();
        configurarAccionesDemo();
        tabTorres.addOnTabSelectedListener(tabListener);
        observarViewModel();
    }

    private void inicializarVistas(View view) {
        tvBannerCorte = view.findViewById(R.id.tvBannerCorte);
        tvTempAire = view.findViewById(R.id.tvTempAire);
        tvHumedad = view.findViewById(R.id.tvHumedad);
        tvTempAgua = view.findViewById(R.id.tvTempAgua);
        tvPh = view.findViewById(R.id.tvPh);
        tvEc = view.findViewById(R.id.tvEc);
        tvLux = view.findViewById(R.id.tvLux);
        tvNivel = view.findViewById(R.id.tvNivel);

        tvRangoAire = view.findViewById(R.id.tvRangoAire);
        tvRangoAgua = view.findViewById(R.id.tvRangoAgua);
        tvRangoPh = view.findViewById(R.id.tvRangoPh);
        tvRangoEc = view.findViewById(R.id.tvRangoEc);

        tvAlertas = view.findViewById(R.id.tvAlertas);
        tvLabelCultivo = view.findViewById(R.id.tvLabelCultivo);
        tvAvisoSoloLectura = view.findViewById(R.id.tvAvisoSoloLectura);
        tvSinDatosReales = view.findViewById(R.id.tvSinDatosReales);
        layoutAccionesDemo = view.findViewById(R.id.layoutAccionesDemo);
        btnGuiaDemo = view.findViewById(R.id.btnGuiaDemo);
        btnReiniciarDemo = view.findViewById(R.id.btnReiniciarDemo);
        btnEditarRangos = view.findViewById(R.id.btnEditarRangos);
        layoutDatosHuerto = view.findViewById(R.id.layoutDatosHuerto);
        spnCultivo = view.findViewById(R.id.spnCultivo);
        tabTorres = view.findViewById(R.id.tabTorres);
        gridEstadoSistema = view.findViewById(R.id.gridEstadoSistema);
        crearTarjetasEstadoSistema();
    }

    private void crearTarjetasEstadoSistema() {
        if (gridEstadoSistema == null || getContext() == null) return;
        gridEstadoSistema.removeAllViews();
        indicadoresSistema.clear();
        textosEstadoSistema.clear();
        agregarTarjetaEstado("ventilador", R.string.control_ventilador, IndicadorSistemaView.VENTILADOR);
        agregarTarjetaEstado("led", R.string.control_led, IndicadorSistemaView.LUZ);
        agregarTarjetaEstado("bomba_riego", R.string.control_bomba_riego, IndicadorSistemaView.BOMBA);
        agregarTarjetaEstado("nutrientes", R.string.control_nutrientes, IndicadorSistemaView.BOMBA);
        agregarTarjetaEstado("ph_pump", R.string.control_ph_down, IndicadorSistemaView.BOMBA);
        agregarTarjetaEstado("cerradura", R.string.control_cerradura, IndicadorSistemaView.CERRADURA);
        agregarTarjetaEstado("nivel_agua", R.string.inicio_nivel_estanque, IndicadorSistemaView.ESTANQUE);
    }

    private void agregarTarjetaEstado(String clave, int tituloRes, int tipo) {
        int margen = dp(4);
        MaterialCardView tarjeta = new MaterialCardView(requireContext());
        tarjeta.setRadius(dp(16));
        tarjeta.setCardElevation(0);
        tarjeta.setStrokeWidth(dp(1));
        tarjeta.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.divisor));
        tarjeta.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.superficie_tarjeta));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f));
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.setMargins(margen, margen, margen, margen);
        tarjeta.setLayoutParams(params);

        LinearLayout contenido = new LinearLayout(requireContext());
        contenido.setGravity(android.view.Gravity.CENTER_VERTICAL);
        contenido.setPadding(dp(10), dp(12), dp(8), dp(12));
        contenido.setMinimumHeight(dp(76));

        IndicadorSistemaView indicador = new IndicadorSistemaView(requireContext());
        indicador.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        indicador.configurar(tipo, false);
        contenido.addView(indicador);

        LinearLayout columna = new LinearLayout(requireContext());
        columna.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams columnaParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        columnaParams.setMargins(dp(6), 0, 0, 0);
        columna.setLayoutParams(columnaParams);
        TextView titulo = new TextView(requireContext());
        titulo.setText(tituloRes);
        titulo.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_secundario));
        titulo.setTextSize(12);
        TextView estado = new TextView(requireContext());
        estado.setText(R.string.estado_apagado);
        estado.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_terciario));
        estado.setTextSize(14);
        estado.setTypeface(null, android.graphics.Typeface.BOLD);
        columna.addView(titulo);
        columna.addView(estado);
        contenido.addView(columna);
        tarjeta.addView(contenido);
        gridEstadoSistema.addView(tarjeta);
        indicadoresSistema.put(clave, indicador);
        textosEstadoSistema.put(clave, estado);
    }

    private int dp(float valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private void actualizarIndicadoresSistema(HuertoSnapshot huerto, double nivel) {
        boolean hayDatos = !soloDatosReales
                || Boolean.TRUE.equals(viewModel.getDatosRealesDisponibles().getValue());
        if (!hayDatos) return;
        actualizarIndicador(huerto, "ventilador", R.string.estado_encendido, R.string.estado_apagado);
        actualizarIndicador(huerto, "led", R.string.estado_encendido, R.string.estado_apagado);
        actualizarIndicador(huerto, "bomba_riego", R.string.estado_regando, R.string.estado_apagado);
        actualizarIndicador(huerto, "nutrientes", R.string.estado_dosificando, R.string.estado_apagado);
        actualizarIndicador(huerto, "ph_pump", R.string.estado_dosificando, R.string.estado_apagado);
        actualizarIndicador(huerto, "cerradura", R.string.estado_desbloqueada, R.string.estado_cerrada);

        IndicadorSistemaView estanque = indicadoresSistema.get("nivel_agua");
        TextView estadoEstanque = textosEstadoSistema.get("nivel_agua");
        if (estanque != null && estadoEstanque != null) {
            estanque.configurarNivel((float) nivel);
            String tendencia = getString(R.string.estado_nivel_estable);
            Double anterior = nivelesAnteriores.get(huerto.idx);
            if (anterior != null) {
                if (nivel > anterior + 0.5) tendencia = getString(R.string.estado_nivel_subiendo);
                else if (nivel < anterior - 0.5) tendencia = getString(R.string.estado_nivel_bajando);
            }
            estadoEstanque.setText(String.format(Locale.getDefault(), "%s · %.0f%%", tendencia, nivel));
            estadoEstanque.setContentDescription(getString(R.string.estado_estanque_accesible, tendencia, nivel));
            nivelesAnteriores.put(huerto.idx, nivel);
        }
    }

    private void actualizarIndicador(HuertoSnapshot huerto, String clave, int activoRes, int apagadoRes) {
        boolean activo = Boolean.TRUE.equals(huerto.act.get(clave));
        IndicadorSistemaView indicador = indicadoresSistema.get(clave);
        TextView estado = textosEstadoSistema.get(clave);
        if (indicador == null || estado == null) return;
        indicador.configurar(indicadorTipo(clave), activo);
        estado.setText(activo ? activoRes : apagadoRes);
        estado.setTextColor(ContextCompat.getColor(requireContext(), activo
                ? R.color.alerta_ok : R.color.texto_terciario));
        estado.setContentDescription(getString(R.string.estado_actuador_accesible,
                getString(activo ? activoRes : apagadoRes)));
    }

    private int indicadorTipo(String clave) {
        if ("ventilador".equals(clave)) return IndicadorSistemaView.VENTILADOR;
        if ("led".equals(clave)) return IndicadorSistemaView.LUZ;
        if ("cerradura".equals(clave)) return IndicadorSistemaView.CERRADURA;
        return IndicadorSistemaView.BOMBA;
    }

    private void configurarModoSoloLectura() {
        String rol = requireActivity().getIntent().getStringExtra(Login.EXTRA_ROL);
        soloDatosReales = Login.ROL_USER.equals(rol);
        esModoDemo = requireActivity().getIntent()
                .getBooleanExtra(MainActivity.EXTRA_MODO_DEMO, false);
        boolean soloLecturaCultivo = esModoDemo || soloDatosReales;
        boolean esAdmin = Login.ROL_ADMIN.equals(rol);
        btnEditarRangos.setVisibility(esAdmin && !esModoDemo ? View.VISIBLE : View.GONE);
        btnEditarRangos.setOnClickListener(v -> mostrarEditorRangos());
        layoutAccionesDemo.setVisibility(esModoDemo ? View.VISIBLE : View.GONE);
        spnCultivo.setEnabled(!soloLecturaCultivo);
        tvAvisoSoloLectura.setVisibility(soloLecturaCultivo ? View.VISIBLE : View.GONE);
        tvAvisoSoloLectura.setText(esModoDemo
                ? R.string.inicio_demo_solo_lectura
                : R.string.inicio_operador_solo_lectura);
        actualizarVisibilidadTelemetria(Boolean.TRUE.equals(
                viewModel.getDatosRealesDisponibles().getValue()));
    }

    private void mostrarEditorRangos() {
        HuertoSnapshot actual = viewModel.getHuertoActual().getValue();
        if (actual == null) return;
        PerfilCultivo p = actual.perfil;
        String[] etiquetas = {"pH mínimo", "pH máximo", "EC mínima (mS/cm)", "EC máxima (mS/cm)",
                "Temperatura del agua mínima (°C)", "Temperatura del agua máxima (°C)",
                "Temperatura del aire mínima (°C)", "Temperatura del aire máxima (°C)"};
        double[] valores = {p.phMin, p.phMax, p.ecMin, p.ecMax,
                p.tempAguaMin, p.tempAguaMax, p.aireMin, p.aireMax};
        LinearLayout contenido = new LinearLayout(requireContext());
        contenido.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        contenido.setPadding(padding, padding / 2, padding, 0);
        EditText[] campos = new EditText[valores.length];
        for (int i = 0; i < valores.length; i += 2) {
            LinearLayout fila = new LinearLayout(requireContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = i; j < i + 2; j++) {
                LinearLayout columna = new LinearLayout(requireContext());
                columna.setOrientation(LinearLayout.VERTICAL);
                columna.setPadding(0, 0, padding / 3, 0);
                TextView etiqueta = new TextView(requireContext());
                etiqueta.setText(etiquetas[j]);
                EditText campo = new EditText(requireContext());
                campo.setSingleLine(true);
                campo.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                campo.setText(String.valueOf(valores[j]));
                campo.setSelectAllOnFocus(true);
                campos[j] = campo;
                columna.addView(etiqueta);
                columna.addView(campo);
                fila.addView(columna, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            }
            contenido.addView(fila);
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.inicio_editar_rangos_titulo, actual.idx))
                .setMessage(R.string.inicio_editar_rangos_descripcion)
                .setView(contenido)
                .setNegativeButton(R.string.accion_cancelar, null)
                .setNeutralButton(R.string.inicio_restaurar_rangos, (dialog, which) -> {
                    PerfilCultivo base = PerfilCultivo.porClave(p.clave);
                    if (base != null) viewModel.guardarRangosCultivo(base.phMin, base.phMax,
                            base.ecMin, base.ecMax, base.tempAguaMin, base.tempAguaMax,
                            base.aireMin, base.aireMax);
                })
                .setPositiveButton(R.string.accion_guardar, (dialog, which) -> {
                    try {
                        double[] n = new double[campos.length];
                        for (int i = 0; i < campos.length; i++)
                            n[i] = Double.parseDouble(campos[i].getText().toString().trim().replace(',', '.'));
                        boolean guardado = viewModel.guardarRangosCultivo(n[0], n[1], n[2], n[3],
                                n[4], n[5], n[6], n[7]);
                        Toast.makeText(requireContext(), guardado ? R.string.inicio_rangos_guardados
                                : R.string.inicio_rangos_invalidos, Toast.LENGTH_LONG).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), R.string.inicio_rangos_invalidos, Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void configurarAccionesDemo() {
        if (!esModoDemo) return;
        btnReiniciarDemo.setOnClickListener(v -> {
            viewModel.reiniciarDemo();
            Toast.makeText(requireContext(), R.string.demo_reiniciado, Toast.LENGTH_SHORT).show();
        });
        btnGuiaDemo.setOnClickListener(v -> mostrarPasoGuiaDemo(0));
    }

    private void mostrarPasoGuiaDemo(int paso) {
        if (!isAdded() || !esModoDemo) return;
        String mensaje;
        boolean esFinal = paso == 4;
        switch (paso) {
            case 0:
                mensaje = getString(R.string.demo_guia_inicio);
                break;
            case 1:
                viewModel.setModoManual(false);
                viewModel.aplicarEscenario("calor");
                mensaje = getString(R.string.demo_guia_paso_calor);
                break;
            case 2:
                viewModel.aplicarEscenario("ph_alto");
                mensaje = getString(R.string.demo_guia_paso_ph);
                break;
            case 3:
                viewModel.aplicarEscenario("nivel_critico");
                mensaje = getString(R.string.demo_guia_paso_nivel);
                break;
            default:
                viewModel.reiniciarDemo();
                mensaje = getString(R.string.demo_guia_final);
                break;
        }

        AlertDialog.Builder dialogo = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.demo_guia_titulo)
                .setMessage(mensaje)
                .setNegativeButton(esFinal ? R.string.accion_cerrar : R.string.demo_guia_terminar, null);
        if (!esFinal) {
            dialogo.setPositiveButton(paso == 0 ? R.string.demo_guia_comenzar
                    : R.string.demo_guia_siguiente, (d, which) -> mostrarPasoGuiaDemo(paso + 1));
        }
        dialogo.show();
    }

    private void configurarSpinnerCultivos() {
        List<PerfilCultivo> lista = PerfilCultivo.todos();
        List<String> nombresCultivos = new ArrayList<>();
        for (PerfilCultivo perfil : lista) {
            nombresCultivos.add(nombreCultivoLocalizado(perfil));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, nombresCultivos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCultivo.setAdapter(adapter);

        spnCultivo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (actualizandoSpinner || soloDatosReales || esModoDemo) return;
                List<PerfilCultivo> perfiles = PerfilCultivo.todos();
                if (position >= 0 && position < perfiles.size()) {
                    HuertoSnapshot actual = viewModel.getHuertoActual().getValue();
                    if (actual != null && actual.perfil.clave.equals(perfiles.get(position).clave)) return;
                    viewModel.cambiarCultivo(perfiles.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observarViewModel() {
        viewModel.getConectadoSistemaReal().observe(getViewLifecycleOwner(), conectado -> {
            actualizarVisibilidadTelemetria(Boolean.TRUE.equals(
                    viewModel.getDatosRealesDisponibles().getValue()));
        });
        viewModel.getDatosRealesDisponibles().observe(getViewLifecycleOwner(), disponibles -> {
            actualizarVisibilidadTelemetria(Boolean.TRUE.equals(disponibles));
            if (Boolean.TRUE.equals(disponibles)) {
                actualizarDatosHuerto(viewModel.getHuertoActual().getValue());
            }
        });

        viewModel.getCorteDatosActivo().observe(getViewLifecycleOwner(), corte -> {
            tvBannerCorte.setVisibility(Boolean.TRUE.equals(corte) ? View.VISIBLE : View.GONE);
        });

        viewModel.getListaTorres().observe(getViewLifecycleOwner(), torres -> {
            if (torres == null) return;
            int sel = viewModel.getIndiceTorreSeleccionada();

            if (tabTorres.getTabCount() != torres.size()) {
                tabTorres.removeOnTabSelectedListener(tabListener);
                tabTorres.removeAllTabs();
                for (int i = 0; i < torres.size(); i++) {
                    tabTorres.addTab(tabTorres.newTab().setText(getString(R.string.tab_torre_cultivo,
                            torres.get(i).idx, nombreCultivoLocalizado(torres.get(i).perfil))));
                }
                TabLayout.Tab tab = tabTorres.getTabAt(sel);
                if (tab != null) tab.select();
                tabTorres.addOnTabSelectedListener(tabListener);
            } else {
                for (int i = 0; i < torres.size(); i++) {
                    TabLayout.Tab tab = tabTorres.getTabAt(i);
                    String textoTab = getString(R.string.tab_torre_cultivo,
                            torres.get(i).idx, nombreCultivoLocalizado(torres.get(i).perfil));
                    if (tab != null && !textoTab.contentEquals(tab.getText())) {
                        tab.setText(textoTab);
                    }
                }
            }
        });

        viewModel.getHuertoActual().observe(getViewLifecycleOwner(), this::actualizarDatosHuerto);

        viewModel.getAlertasActuales().observe(getViewLifecycleOwner(), alertas -> {
            if (alertas == null || alertas.isEmpty()) {
                tvAlertas.setText(getString(R.string.alertas_todas_ok));
                tvAlertas.setTextColor(ContextCompat.getColor(requireContext(), R.color.alerta_ok));
            } else {
                StringBuilder sb = new StringBuilder();
                for (Alerta a : alertas) {
                    String sufijo = getString(a.automatica
                            ? R.string.alerta_auto_sufijo
                            : R.string.alerta_manual_sufijo);
                    sb.append(String.format(Locale.getDefault(), getString(R.string.alerta_formato),
                            formatearNombreVariable(a.variable),
                            a.nivel.toUpperCase(Locale.getDefault()),
                            a.valor,
                            obtenerAccionAlerta(a),
                            sufijo));
                }
                tvAlertas.setText(sb.toString().trim());
                tvAlertas.setTextColor(ContextCompat.getColor(requireContext(), R.color.alerta_error));
            }
        });
    }

    private void actualizarVisibilidadTelemetria(boolean hayDatosReales) {
        boolean ocultarDatos = soloDatosReales && !hayDatosReales;
        layoutDatosHuerto.setVisibility(ocultarDatos ? View.GONE : View.VISIBLE);
        tvSinDatosReales.setVisibility(ocultarDatos ? View.VISIBLE : View.GONE);
        if (ocultarDatos) {
            boolean conectado = Boolean.TRUE.equals(viewModel.getConectadoSistemaReal().getValue());
            tvSinDatosReales.setText(conectado
                    ? R.string.inicio_esperando_datos_reales
                    : R.string.inicio_sin_datos_reales);
        }
    }

    private void actualizarDatosHuerto(HuertoSnapshot huerto) {
        if (huerto == null) return;
        boolean datosRealesDisponibles = Boolean.TRUE.equals(viewModel.getDatosRealesDisponibles().getValue());
        if (soloDatosReales && !datosRealesDisponibles) {
            actualizarVisibilidadTelemetria(false);
            return;
        }
        actualizarVisibilidadTelemetria(datosRealesDisponibles);

        tvLabelCultivo.setText(getString(R.string.inicio_cultivo_torre, huerto.idx));

        actualizandoSpinner = true;
        List<PerfilCultivo> lista = PerfilCultivo.todos();
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).clave.equals(huerto.perfil.clave)) {
                spnCultivo.setSelection(i);
                break;
            }
        }
        actualizandoSpinner = false;

        PerfilCultivo p = huerto.perfil;
        tvRangoAire.setText(String.format(Locale.getDefault(), getString(R.string.formato_rango_temperatura), p.aireMin, p.aireMax));
        tvRangoAgua.setText(String.format(Locale.getDefault(), getString(R.string.formato_rango_temperatura), p.tempAguaMin, p.tempAguaMax));
        tvRangoPh.setText(String.format(Locale.getDefault(), getString(R.string.formato_rango_ph), p.phMin, p.phMax));
        tvRangoEc.setText(String.format(Locale.getDefault(), getString(R.string.formato_rango_ec), p.ecMin, p.ecMax));

        double aire = huerto.v.get("temp_ambiente");
        double hum = huerto.v.get("humedad");
        double agua = huerto.v.get("temp_agua");
        double ph = huerto.v.get("ph");
        double ec = huerto.v.get("ec");
        double lux = huerto.v.get("lux");
        double nivel = huerto.v.get("nivel_agua");

        tvTempAire.setText(String.format(Locale.getDefault(), getString(R.string.formato_temperatura), aire));
        pintarValor(tvTempAire, aire < p.aireMin || aire > p.aireMax);

        tvHumedad.setText(String.format(Locale.getDefault(), getString(R.string.formato_porcentaje), hum));

        tvTempAgua.setText(String.format(Locale.getDefault(), getString(R.string.formato_temperatura), agua));
        pintarValor(tvTempAgua, agua < p.tempAguaMin || agua > p.tempAguaMax);

        tvPh.setText(String.format(Locale.getDefault(), getString(R.string.formato_ph), ph));
        pintarValor(tvPh, ph < p.phMin || ph > p.phMax);

        tvEc.setText(String.format(Locale.getDefault(), getString(R.string.formato_ec), ec));
        pintarValor(tvEc, ec < p.ecMin || ec > p.ecMax);

        tvLux.setText(String.format(Locale.getDefault(), getString(R.string.formato_lux), lux));
        pintarValor(tvLux, lux < Huerto.LUX_LED_ON);

        tvNivel.setText(String.format(Locale.getDefault(), getString(R.string.formato_porcentaje), nivel));
        pintarValor(tvNivel, nivel < Huerto.NIVEL_BAJO);
        actualizarIndicadoresSistema(huerto, nivel);
    }

    private String formatearNombreVariable(String raw) {
        if (raw == null) return "";
        switch (raw.toLowerCase()) {
            case "temp_ambiente": return getString(R.string.variable_temp_aire);
            case "temp_agua": return getString(R.string.variable_temp_agua);
            case "ph": return getString(R.string.variable_ph);
            case "ec": return getString(R.string.variable_ec);
            case "lux": return getString(R.string.variable_luz);
            case "nivel_agua": return getString(R.string.variable_nivel_agua);
            default: return raw.replace("_", " ").toUpperCase(Locale.getDefault());
        }
    }

    private String nombreCultivoLocalizado(PerfilCultivo perfil) {
        switch (perfil.clave) {
            case "guia": return getString(R.string.cultivo_guia);
            case "lechuga": return getString(R.string.cultivo_lechuga);
            case "albahaca": return getString(R.string.cultivo_albahaca);
            case "menta": return getString(R.string.cultivo_menta);
            case "cilantro": return getString(R.string.cultivo_cilantro);
            case "acelga": return getString(R.string.cultivo_acelga);
            case "tomate_cherry": return getString(R.string.cultivo_tomate_cherry);
            default: return perfil.nombre;
        }
    }

    private String obtenerAccionAlerta(Alerta alerta) {
        switch (alerta.getClave()) {
            case "temp_ambiente_alta": return getString(R.string.accion_temp_aire_alta);
            case "temp_ambiente_baja": return getString(R.string.accion_temp_aire_baja);
            case "temp_agua_alta": return getString(R.string.accion_temp_agua_alta);
            case "temp_agua_baja": return getString(R.string.accion_temp_agua_baja);
            case "ph_alto": return getString(R.string.accion_ph_alto);
            case "ph_bajo": return getString(R.string.accion_ph_bajo);
            case "ec_alto": return getString(R.string.accion_ec_alto);
            case "ec_bajo": return getString(R.string.accion_ec_bajo);
            case "nivel_agua_critico": return getString(R.string.accion_nivel_critico);
            case "nivel_agua_bajo": return getString(R.string.accion_nivel_bajo);
            case "lux_bajo": return getString(R.string.accion_luz_baja);
            default: return alerta.accion;
        }
    }

    private void pintarValor(TextView tv, boolean fueraDeRango) {
        int colorRes = fueraDeRango ? R.color.alerta_error : R.color.alerta_ok;
        tv.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }
}
