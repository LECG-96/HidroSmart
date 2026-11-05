package com.devst.hidrosmart.view;

import android.graphics.Color;
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
    private TextView tvAlertas, tvLabelCultivo;
    private Spinner spnCultivo;
    private TabLayout tabTorres;

    private boolean actualizandoSpinner = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Compartir la misma instancia del ViewModel ligada a la Activity anfitriona
        viewModel = new ViewModelProvider(requireActivity()).get(HuertoViewModel.class);

        inicializarVistas(view);
        configurarSpinnerCultivos();
        configurarTabs();
        observarViewModel();
    }

    private void inicializarVistas(View view) {
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

    private void configurarTabs() {
        tabTorres.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.seleccionarTorre(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observarViewModel() {
        // Observa la lista de torres para recrear los tabs si se agregan o eliminan
        viewModel.getListaTorres().observe(getViewLifecycleOwner(), torres -> {
            if (torres == null) return;
            if (tabTorres.getTabCount() != torres.size()) {
                tabTorres.removeAllTabs();
                for (int i = 0; i < torres.size(); i++) {
                    tabTorres.addTab(tabTorres.newTab().setText(String.format("T%d · %s", torres.get(i).idx, torres.get(i).perfil.nombre)));
                }
                int sel = viewModel.getIndiceTorreSeleccionada();
                TabLayout.Tab tab = tabTorres.getTabAt(sel);
                if (tab != null) tab.select();
            } else {
                for (int i = 0; i < torres.size(); i++) {
                    TabLayout.Tab tab = tabTorres.getTabAt(i);
                    if (tab != null) {
                        tab.setText(String.format("T%d · %s", torres.get(i).idx, torres.get(i).perfil.nombre));
                    }
                }
            }
        });

        // Observa los datos de la torre seleccionada
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

        // Observa la lista de alertas
        viewModel.getAlertasActuales().observe(getViewLifecycleOwner(), alertas -> {
            if (alertas == null || alertas.isEmpty()) {
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
        });
    }

    private void pintarValor(TextView tv, boolean fueraDeRango) {
        tv.setTextColor(fueraDeRango ? Color.parseColor("#B71C1C") : Color.parseColor("#1B5E20"));
    }
}