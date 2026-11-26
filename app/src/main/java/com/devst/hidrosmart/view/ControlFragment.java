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
    private boolean actualizandoSwitches = false;
    private boolean actualizandoCorteDatos = false;

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
        habilitarCheckboxesManuales(swModoManual.isChecked());
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
            if (actualizandoSwitches) return;
            viewModel.setModoManual(isChecked);
            habilitarCheckboxesManuales(isChecked);
        });

        swCorteDatos.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (actualizandoCorteDatos) return;
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
            if (!swModoManual.isChecked()) {
                actualizandoCheckboxes = true;
                chk.setChecked(!isChecked);
                actualizandoCheckboxes = false;
                Toast.makeText(requireContext(), R.string.mensaje_activa_modo_manual, Toast.LENGTH_SHORT).show();
                return;
            }
            RespuestaComando res = viewModel.enviarComando(actuador, isChecked);
            if (!res.aceptado) {
                actualizandoCheckboxes = true;
                chk.setChecked(!isChecked);
                actualizandoCheckboxes = false;
                Toast.makeText(requireContext(), obtenerMensajeRespuesta(res), Toast.LENGTH_SHORT).show();
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

    private String obtenerMensajeRespuesta(RespuestaComando respuesta) {
        switch (respuesta.codigoMensaje) {
            case SIN_TORRE_ACTIVA:
                return getString(R.string.error_sin_torre_activa);
            case MODO_AUTOMATICO:
                return getString(R.string.error_modo_automatico);
            case COMANDO_DESCONOCIDO:
                return getString(R.string.error_comando_desconocido);
            case NIVEL_AGUA_CRITICO:
                return getString(R.string.error_nivel_agua_critico);
            default:
                return respuesta.mensaje;
        }
    }

    private void configurarSliders() {
        seekAire.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekAire.setText(String.format(Locale.getDefault(), getString(R.string.control_label_temp_aire_entero), progress));
                if (fromUser) {
                    viewModel.forzarParametro("temp_ambiente", (double) progress, true);
                }
            }
        });

        seekHum.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekHum.setText(String.format(Locale.getDefault(), getString(R.string.control_label_humedad_entera), progress));
                if (fromUser) {
                    viewModel.forzarParametro("humedad", (double) progress, true);
                }
            }
        });

        seekAgua.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = 5.0 + progress;
                lblSeekAgua.setText(String.format(Locale.getDefault(), getString(R.string.control_label_temp_agua), val));
                if (fromUser) {
                    viewModel.forzarParametro("temp_agua", val, true);
                }
            }
        });

        seekPh.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = 3.0 + (progress / 10.0);
                lblSeekPh.setText(String.format(Locale.getDefault(), getString(R.string.control_label_ph), val));
                if (fromUser) {
                    viewModel.forzarParametro("ph", val, true);
                }
            }
        });

        seekEc.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = progress / 10.0;
                lblSeekEc.setText(String.format(Locale.getDefault(), getString(R.string.control_label_ec), val));
                if (fromUser) {
                    viewModel.forzarParametro("ec", val, true);
                }
            }
        });

        seekLux.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double val = progress * 100.0;
                lblSeekLux.setText(String.format(Locale.getDefault(), getString(R.string.control_label_lux), val));
                if (fromUser) {
                    viewModel.forzarParametro("lux", val, true);
                }
            }
        });

        seekNivel.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lblSeekNivel.setText(String.format(Locale.getDefault(), getString(R.string.control_label_nivel_entero), progress));
                if (fromUser) {
                    viewModel.forzarParametro("nivel_agua", (double) progress, true);
                }
            }
        });
    }

    private void configurarBotonesEscenarios(View view) {
        vincularEscenario(view, R.id.btnEscCalor, "calor", R.string.control_escenario_calor);
        vincularEscenario(view, R.id.btnEscFrio, "frio", R.string.control_escenario_frio);
        vincularEscenario(view, R.id.btnEscAguaCal, "agua_caliente", R.string.control_escenario_agua_caliente);
        vincularEscenario(view, R.id.btnEscAguaFria, "agua_fria", R.string.control_escenario_agua_fria);
        vincularEscenario(view, R.id.btnEscPhAlto, "ph_alto", R.string.control_escenario_ph_alto);
        vincularEscenario(view, R.id.btnEscPhBajo, "ph_bajo", R.string.control_escenario_ph_bajo);
        vincularEscenario(view, R.id.btnEscEcBaja, "ec_bajo", R.string.control_escenario_ec_baja);
        vincularEscenario(view, R.id.btnEscEcAlta, "ec_alto", R.string.control_escenario_ec_alta);
        vincularEscenario(view, R.id.btnEscOscuridad, "oscuridad", R.string.control_escenario_poca_luz);
        vincularEscenario(view, R.id.btnEscNivelBajo, "nivel_bajo", R.string.control_escenario_nivel_bajo);
        vincularEscenario(view, R.id.btnEscNivelCritico, "nivel_critico", R.string.control_escenario_nivel_critico);
        vincularEscenario(view, R.id.btnEscNormal, "normal", R.string.control_escenario_normal);

        Button btnRellenar = view.findViewById(R.id.btnRellenarEstanque);
        btnRellenar.setOnClickListener(v -> {
            viewModel.rellenarEstanque();
            Toast.makeText(requireContext(), R.string.mensaje_estanque_rellenado, Toast.LENGTH_SHORT).show();
        });
    }

    private void vincularEscenario(View root, int botonId, String escenario, int nombreResId) {
        Button btn = root.findViewById(botonId);
        btn.setOnClickListener(v -> {
            viewModel.aplicarEscenario(escenario);
            Toast.makeText(requireContext(),
                    getString(R.string.mensaje_escenario_aplicado, getString(nombreResId)),
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void observarViewModel() {
        viewModel.getCorteDatosActivo().observe(getViewLifecycleOwner(), activo -> {
            boolean corte = Boolean.TRUE.equals(activo);
            if (swCorteDatos.isChecked() != corte) {
                actualizandoCorteDatos = true;
                swCorteDatos.setChecked(corte);
                actualizandoCorteDatos = false;
            }
        });

        viewModel.getHuertoActual().observe(getViewLifecycleOwner(), huerto -> {
            if (huerto == null) return;

            actualizandoSwitches = true;
            if (swModoManual.isChecked() != huerto.modoManual) {
                swModoManual.setChecked(huerto.modoManual);
                habilitarCheckboxesManuales(huerto.modoManual);
            }
            actualizandoSwitches = false;

            actualizandoCheckboxes = true;
            chkVentilador.setChecked(Boolean.TRUE.equals(huerto.act.get("ventilador")));
            chkLed.setChecked(Boolean.TRUE.equals(huerto.act.get("led")));
            chkBomba.setChecked(Boolean.TRUE.equals(huerto.act.get("bomba_riego")));
            chkNutrientes.setChecked(Boolean.TRUE.equals(huerto.act.get("nutrientes")));
            chkPhPump.setChecked(Boolean.TRUE.equals(huerto.act.get("ph_pump")));
            chkCerradura.setChecked(Boolean.TRUE.equals(huerto.act.get("cerradura")));
            actualizandoCheckboxes = false;

            if (!usuarioArrastrandoSlider) {
                double aire = huerto.v.get("temp_ambiente");
                establecerProgresoSiCambio(seekAire, (int) Math.round(aire));
                establecerTextoSiCambio(lblSeekAire,
                        String.format(Locale.getDefault(), getString(R.string.control_label_temp_aire), aire));

                double hum = huerto.v.get("humedad");
                establecerProgresoSiCambio(seekHum, (int) Math.round(hum));
                establecerTextoSiCambio(lblSeekHum,
                        String.format(Locale.getDefault(), getString(R.string.control_label_humedad), hum));

                double agua = huerto.v.get("temp_agua");
                establecerProgresoSiCambio(seekAgua, (int) Math.max(0, Math.round(agua - 5.0)));
                establecerTextoSiCambio(lblSeekAgua,
                        String.format(Locale.getDefault(), getString(R.string.control_label_temp_agua), agua));

                double ph = huerto.v.get("ph");
                establecerProgresoSiCambio(seekPh, (int) Math.max(0, Math.round((ph - 3.0) * 10.0)));
                establecerTextoSiCambio(lblSeekPh,
                        String.format(Locale.getDefault(), getString(R.string.control_label_ph), ph));

                double ec = huerto.v.get("ec");
                establecerProgresoSiCambio(seekEc, (int) Math.max(0, Math.round(ec * 10.0)));
                establecerTextoSiCambio(lblSeekEc,
                        String.format(Locale.getDefault(), getString(R.string.control_label_ec), ec));

                double lux = huerto.v.get("lux");
                establecerProgresoSiCambio(seekLux, (int) Math.max(0, Math.round(lux / 100.0)));
                establecerTextoSiCambio(lblSeekLux,
                        String.format(Locale.getDefault(), getString(R.string.control_label_lux), lux));

                double nivel = huerto.v.get("nivel_agua");
                establecerProgresoSiCambio(seekNivel, (int) Math.round(nivel));
                establecerTextoSiCambio(lblSeekNivel,
                        String.format(Locale.getDefault(), getString(R.string.control_label_nivel), nivel));
            }
        });
    }

    private void establecerProgresoSiCambio(SeekBar seekBar, int progreso) {
        int progresoLimitado = Math.max(0, Math.min(seekBar.getMax(), progreso));
        if (seekBar.getProgress() != progresoLimitado) seekBar.setProgress(progresoLimitado);
    }

    private void establecerTextoSiCambio(TextView vista, String texto) {
        if (!texto.contentEquals(vista.getText())) vista.setText(texto);
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
