package com.devst.hidrosmart.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

import java.util.List;
import java.util.Locale;

public class InicioFragment extends Fragment {

    private HuertoViewModel viewModel;

    private TextView tvTempAire, tvHumedad, tvTempAgua, tvPh, tvEc, tvLux, tvNivel;
    private TextView tvRangoAire, tvRangoAgua, tvRangoPh, tvRangoEc;
    private TextView tvAlertas, tvLabelCultivo, tvBannerCorte;
    private Spinner spnCultivo;
    private TabLayout tabTorres;

    private boolean actualizandoSpinner = false;

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
        spnCultivo = view.findViewById(R.id.spnCultivo);
        tabTorres = view.findViewById(R.id.tabTorres);
    }

    private void configurarSpinnerCultivos() {
        List<PerfilCultivo> lista = PerfilCultivo.todos();
        ArrayAdapter<PerfilCultivo> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, lista);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCultivo.setAdapter(adapter);

        spnCultivo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (actualizandoSpinner) return;
                PerfilCultivo seleccionado = (PerfilCultivo) parent.getItemAtPosition(position);
                viewModel.cambiarCultivo(seleccionado);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void observarViewModel() {
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
                    tabTorres.addTab(tabTorres.newTab().setText(String.format("T%d · %s", torres.get(i).idx, torres.get(i).perfil.nombre)));
                }
                TabLayout.Tab tab = tabTorres.getTabAt(sel);
                if (tab != null) tab.select();
                tabTorres.addOnTabSelectedListener(tabListener);
            } else {
                for (int i = 0; i < torres.size(); i++) {
                    TabLayout.Tab tab = tabTorres.getTabAt(i);
                    if (tab != null) {
                        tab.setText(String.format("T%d · %s", torres.get(i).idx, torres.get(i).perfil.nombre));
                    }
                }
            }
        });

        viewModel.getHuertoActual().observe(getViewLifecycleOwner(), huerto -> {
            if (huerto == null) return;

            tvLabelCultivo.setText("Cultivo de Torre " + huerto.idx);

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
            tvRangoAire.setText(String.format(Locale.US, "(%.0f - %.0f °C)", p.aireMin, p.aireMax));
            tvRangoAgua.setText(String.format(Locale.US, "(%.0f - %.0f °C)", p.tempAguaMin, p.tempAguaMax));
            tvRangoPh.setText(String.format(Locale.US, "(%.1f - %.1f)", p.phMin, p.phMax));
            tvRangoEc.setText(String.format(Locale.US, "(%.1f - %.1f)", p.ecMin, p.ecMax));

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
        });

        viewModel.getAlertasActuales().observe(getViewLifecycleOwner(), alertas -> {
            if (alertas == null || alertas.isEmpty()) {
                tvAlertas.setText(getString(R.string.alertas_todas_ok));
                tvAlertas.setTextColor(ContextCompat.getColor(requireContext(), R.color.alerta_ok));
            } else {
                StringBuilder sb = new StringBuilder();
                for (Alerta a : alertas) {
                    sb.append("⚠ ").append(formatearNombreVariable(a.variable))
                            .append(" ").append(a.nivel.toUpperCase())
                            .append(" (").append(String.format(Locale.US, "%.1f", a.valor)).append(")\n")
                            .append("→ ").append(a.accion)
                            .append(a.automatica ? " [Auto]\n\n" : " [Acción manual requerida]\n\n");
                }
                tvAlertas.setText(sb.toString().trim());
                tvAlertas.setTextColor(ContextCompat.getColor(requireContext(), R.color.alerta_error));
            }
        });
    }

    private String formatearNombreVariable(String raw) {
        if (raw == null) return "";
        switch (raw.toLowerCase()) {
            case "temp_ambiente": return "Temperatura Aire";
            case "temp_agua": return "Temperatura Agua";
            case "ph": return "pH Solución";
            case "ec": return "Conductividad (EC)";
            case "lux": return "Luminosidad";
            case "nivel_agua": return "Nivel de Estanque";
            default: return raw.replace("_", " ").toUpperCase();
        }
    }

    private void pintarValor(TextView tv, boolean fueraDeRango) {
        int colorRes = fueraDeRango ? R.color.alerta_error : R.color.alerta_ok;
        tv.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }
}