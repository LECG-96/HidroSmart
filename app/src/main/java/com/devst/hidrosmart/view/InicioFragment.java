package com.devst.hidrosmart.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.Alerta;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.devst.hidrosmart.viewmodel.HuertoViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InicioFragment extends Fragment {

    private HuertoViewModel viewModel;

    private TextView tvTempAire, tvHumedad, tvTempAgua, tvPh, tvEc, tvLux, tvNivel;
    private TextView tvRangoAire, tvRangoAgua, tvRangoPh, tvRangoEc;
    private TextView tvAlertas, tvLabelCultivo, tvBannerCorte;
    private TextView tvAvisoSoloLectura, tvSinDatosReales;
    private LinearLayout layoutDatosHuerto;
    private Spinner spnCultivo;
    private TabLayout tabTorres;

    private boolean actualizandoSpinner = false;
    private boolean soloDatosReales = false;

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
        layoutDatosHuerto = view.findViewById(R.id.layoutDatosHuerto);
        spnCultivo = view.findViewById(R.id.spnCultivo);
        tabTorres = view.findViewById(R.id.tabTorres);
    }

    private void configurarModoSoloLectura() {
        String rol = requireActivity().getIntent().getStringExtra(Login.EXTRA_ROL);
        soloDatosReales = Login.ROL_USER.equals(rol);
        boolean esModoDemo = requireActivity().getIntent()
                .getBooleanExtra(MainActivity.EXTRA_MODO_DEMO, false);
        boolean soloLecturaCultivo = esModoDemo || soloDatosReales;
        spnCultivo.setEnabled(!soloLecturaCultivo);
        tvAvisoSoloLectura.setVisibility(soloLecturaCultivo ? View.VISIBLE : View.GONE);
        tvAvisoSoloLectura.setText(esModoDemo
                ? R.string.inicio_demo_solo_lectura
                : R.string.inicio_operador_solo_lectura);
        actualizarVisibilidadTelemetria(Boolean.TRUE.equals(
                viewModel.getDatosRealesDisponibles().getValue()));
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
                if (actualizandoSpinner || soloDatosReales) return;
                List<PerfilCultivo> perfiles = PerfilCultivo.todos();
                if (position >= 0 && position < perfiles.size()) {
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
                    if (tab != null) {
                        tab.setText(getString(R.string.tab_torre_cultivo,
                                torres.get(i).idx, nombreCultivoLocalizado(torres.get(i).perfil)));
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

    private void actualizarDatosHuerto(Huerto huerto) {
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
