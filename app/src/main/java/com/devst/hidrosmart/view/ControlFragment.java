package com.devst.hidrosmart.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.RespuestaComando;
import com.devst.hidrosmart.viewmodel.HuertoViewModel;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public class ControlFragment extends Fragment {

    private HuertoViewModel viewModel;

    private SwitchMaterial swModoManual, swCorteDatos;
    private MaterialCheckBox chkVentilador, chkLed, chkBomba, chkNutrientes, chkPhPump, chkCerradura;

    private SeekBar seekAire, seekHum, seekAgua, seekPh, seekEc, seekLux, seekNivel;
    private TextView lblSeekAire, lblSeekHum, lblSeekAgua, lblSeekPh, lblSeekEc, lblSeekLux, lblSeekNivel;

    private boolean usuarioArrastrandoSlider = false;
    private boolean actualizandoCheckboxes = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(HuertoViewModel.class);

        inicializarVistas(view);
        configurarSwitches();
        configurarCheckboxesActuadores();
        configurarSliders();
        configurarBotonesEscenarios(view);
        observarViewModel();
    }

    private void inicializarVistas(View view) {
        swModoManual = view.findViewById(R.id.swModoManual);
        swCorteDatos = view.findViewById(R.id.swCorteDatos);

        chkVentilador = view.findViewById(R.id.chkVentilador);
        chkLed = view.findViewById(R.id.chkLed);
        chkBomba = view.findViewById(R.id.chkBomba);
        chkNutrientes = view.findViewById(R.id.chkNutrientes);
        chkPhPump = view.findViewById(R.id.chkPhPump);
        chkCerradura = view.findViewById(R.id.chkCerradura);

        seekAire = view.findViewById(R.id.seekAire);
        seekHum = view.findViewById(R.id.seekHum);
        seekAgua = view.findViewById(R.id.seekAgua);
        seekPh = view.findViewById(R.id.seekPh);
        seekEc = view.findViewById(R.id.seekEc);
        seekLux = view.findViewById(R.id.seekLux);
        seekNivel = view.findViewById(R.id.seekNivel);

        lblSeekAire = view.findViewById(R.id.lblSeekAire);
        lblSeekHum = view.findViewById(R.id.lblSeekHum);
        lblSeekAgua = view.findViewById(R.id.lblSeekAgua);
        lblSeekPh = view.findViewById(R.id.lblSeekPh);
        lblSeekEc = view.findViewById(R.id.lblSeekEc);
        lblSeekLux = view.findViewById(R.id.lblSeekLux);
        lblSeekNivel = view.findViewById(R.id.lblSeekNivel);
    }

    private void configurarSwitches() {
        swModoManual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setModoManual(isChecked);
            habilitarCheckboxesManuales(isChecked);
        });

        swCorteDatos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setCorteDatos(isChecked);
        });
    }

    private void configurarCheckboxesActuadores() {
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
            RespuestaComando res = viewModel.enviarComando(actuador, isChecked);
            if (!res.aceptado) {
                actualizandoCheckboxes = true;
                chk.setChecked(!isChecked);
                actualizandoCheckboxes = false;
                Toast.makeText(requireContext(), res.mensaje, Toast.LENGTH_SHORT).show();
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

    private void configurarSliders() {
        seekAire.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekAire.setText(String.format(Locale.US, "Temp. Aire: %d.0 °C", progress));
                if (fromUser) viewModel.forzarParametro("temp_ambiente", (double) progress, true);
            }
        });

        seekHum.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekHum.setText(String.format(Locale.US, "Humedad: %d %%", progress));
                if (fromUser) viewModel.forzarParametro("humedad", (double) progress, true);
            }
        });

        seekAgua.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = 5.0 + progress;
                lblSeekAgua.setText(String.format(Locale.US, "Temp. Agua: %.1f °C", val));
                if (fromUser) viewModel.forzarParametro("temp_agua", val, true);
            }
        });

        seekPh.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = 3.0 + (progress / 10.0);
                lblSeekPh.setText(String.format(Locale.US, "pH: %.1f", val));
                if (fromUser) viewModel.forzarParametro("ph", val, false);
            }
        });

        seekEc.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = progress / 10.0;
                lblSeekEc.setText(String.format(Locale.US, "EC: %.1f mS/cm", val));
                if (fromUser) viewModel.forzarParametro("ec", val, false);
            }
        });

        seekLux.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = progress * 100.0;
                lblSeekLux.setText(String.format(Locale.US, "Luz: %.0f lux", val));
                if (fromUser) viewModel.forzarParametro("lux", val, true);
            }
        });

        seekNivel.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekNivel.setText(String.format(Locale.US, "Nivel Estanque: %d %%", progress));
                if (fromUser) viewModel.forzarParametro("nivel_agua", (double) progress, false);
            }
        });
    }

    private void configurarBotonesEscenarios(View view) {
        vincularEscenario(view, R.id.btnEscCalor, "calor");
        vincularEscenario(view, R.id.btnEscFrio, "frio");
        vincularEscenario(view, R.id.btnEscAguaCal, "agua_caliente");
        vincularEscenario(view, R.id.btnEscAguaFria, "agua_fria");
        vincularEscenario(view, R.id.btnEscPhAlto, "ph_alto");
        vincularEscenario(view, R.id.btnEscPhBajo, "ph_bajo");
        vincularEscenario(view, R.id.btnEscEcBaja, "ec_bajo");
        vincularEscenario(view, R.id.btnEscEcAlta, "ec_alto");
        vincularEscenario(view, R.id.btnEscOscuridad, "oscuridad");
        vincularEscenario(view, R.id.btnEscNivelBajo, "nivel_bajo");
        vincularEscenario(view, R.id.btnEscNivelCritico, "nivel_critico");
        vincularEscenario(view, R.id.btnEscNormal, "normal");

        Button btnRellenar = view.findViewById(R.id.btnRellenarEstanque);
        btnRellenar.setOnClickListener(v -> {
            viewModel.rellenarEstanque();
            Toast.makeText(requireContext(), "Estanque rellenado al 100%", Toast.LENGTH_SHORT).show();
        });
    }

    private void vincularEscenario(View root, int botonId, String escenario) {
        Button btn = root.findViewById(botonId);
        btn.setOnClickListener(v -> {
            viewModel.aplicarEscenario(escenario);
            Toast.makeText(requireContext(), "Escenario aplicado: " + escenario, Toast.LENGTH_SHORT).show();
        });
    }

    private void observarViewModel() {
        viewModel.getHuertoActual().observe(getViewLifecycleOwner(), huerto -> {
            if (huerto == null) return;

            // Sincronizar switch manual
            if (swModoManual.isChecked() != huerto.modoManual) {
                swModoManual.setChecked(huerto.modoManual);
                habilitarCheckboxesManuales(huerto.modoManual);
            }

            // Sincronizar checkboxes de actuadores
            actualizandoCheckboxes = true;
            chkVentilador.setChecked(Boolean.TRUE.equals(huerto.act.get("ventilador")));
            chkLed.setChecked(Boolean.TRUE.equals(huerto.act.get("led")));
            chkBomba.setChecked(Boolean.TRUE.equals(huerto.act.get("bomba_riego")));
            chkNutrientes.setChecked(Boolean.TRUE.equals(huerto.act.get("nutrientes")));
            chkPhPump.setChecked(Boolean.TRUE.equals(huerto.act.get("ph_pump")));
            chkCerradura.setChecked(Boolean.TRUE.equals(huerto.act.get("cerradura")));
            actualizandoCheckboxes = false;

            // Sincronizar sliders si el usuario no los está manipulando
            if (!usuarioArrastrandoSlider) {
                double aire = huerto.v.get("temp_ambiente");
                seekAire.setProgress((int) Math.round(aire));
                lblSeekAire.setText(String.format(Locale.US, "Temp. Aire: %.1f °C", aire));

                double hum = huerto.v.get("humedad");
                seekHum.setProgress((int) Math.round(hum));
                lblSeekHum.setText(String.format(Locale.US, "Humedad: %.0f %%", hum));

                double agua = huerto.v.get("temp_agua");
                seekAgua.setProgress((int) Math.max(0, Math.round(agua - 5.0)));
                lblSeekAgua.setText(String.format(Locale.US, "Temp. Agua: %.1f °C", agua));

                double ph = huerto.v.get("ph");
                seekPh.setProgress((int) Math.max(0, Math.round((ph - 3.0) * 10.0)));
                lblSeekPh.setText(String.format(Locale.US, "pH: %.2f", ph));

                double ec = huerto.v.get("ec");
                seekEc.setProgress((int) Math.max(0, Math.round(ec * 10.0)));
                lblSeekEc.setText(String.format(Locale.US, "EC: %.2f mS/cm", ec));

                double lux = huerto.v.get("lux");
                seekLux.setProgress((int) Math.max(0, Math.round(lux / 100.0)));
                lblSeekLux.setText(String.format(Locale.US, "Luz: %.0f lux", lux));

                double nivel = huerto.v.get("nivel_agua");
                seekNivel.setProgress((int) Math.round(nivel));
                lblSeekNivel.setText(String.format(Locale.US, "Nivel Estanque: %.0f %%", nivel));
            }
        });
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
}