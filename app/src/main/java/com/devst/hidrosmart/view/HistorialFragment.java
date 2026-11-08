package com.devst.hidrosmart.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.Alerta;
import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.devst.hidrosmart.viewmodel.HuertoViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistorialFragment extends Fragment {

    private HuertoViewModel viewModel;

    private Spinner spnTorreComentario;
    private EditText etComentario;
    private Button btnEnviarComentario;
    private TextView tvEstadoConexion;
    private TextView tvAlertasReales;
    private TextView tvObservacionesGuardadas;

    // Control para no resetear el spinner a cada segundo
    private final List<String> opcionesPrevias = new ArrayList<>();
    private List<Huerto> torresDisponibles = new ArrayList<>();
    private boolean conectadoReal = false;
    private boolean datosRealesDisponibles = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(HuertoViewModel.class);

        spnTorreComentario = view.findViewById(R.id.spnTorreComentario);
        etComentario = view.findViewById(R.id.etComentario);
        btnEnviarComentario = view.findViewById(R.id.btnEnviarComentario);
        tvEstadoConexion = view.findViewById(R.id.tvEstadoConexion);
        tvAlertasReales = view.findViewById(R.id.tvAlertasReales);
        tvObservacionesGuardadas = view.findViewById(R.id.tvObservacionesGuardadas);
        viewModel.getConectadoSistemaReal().observe(getViewLifecycleOwner(), conectado -> {
            conectadoReal = Boolean.TRUE.equals(conectado);
            actualizarEstadoConexion();
            actualizarAlertasReales();
        });
        viewModel.getDatosRealesDisponibles().observe(getViewLifecycleOwner(), disponibles -> {
            datosRealesDisponibles = Boolean.TRUE.equals(disponibles);
            actualizarEstadoConexion();
            actualizarSelectorTorre();
            actualizarAlertasReales();
        });
        viewModel.getAlertasActuales().observe(getViewLifecycleOwner(), alertas -> actualizarAlertasReales());
        String usuarioActual = requireActivity().getIntent().getStringExtra(Login.EXTRA_USUARIO_ACTUAL);
        if (usuarioActual == null) usuarioActual = "user";
        final String remitente = usuarioActual;

        viewModel.getComentarios().observe(getViewLifecycleOwner(), comentarios ->
                mostrarObservacionesGuardadas(comentarios, remitente));

        viewModel.getListaTorres().observe(getViewLifecycleOwner(), torres -> {
            if (torres == null) return;
            torresDisponibles = torres;
            actualizarSelectorTorre();
        });

        btnEnviarComentario.setOnClickListener(v -> {
            String texto = etComentario.getText().toString().trim();
            if (texto.isEmpty()) {
                Toast.makeText(requireContext(), R.string.mensaje_escribe_observacion, Toast.LENGTH_SHORT).show();
                return;
            }

            if (spnTorreComentario.getSelectedItem() == null) {
                Toast.makeText(requireContext(), R.string.mensaje_selecciona_torre, Toast.LENGTH_SHORT).show();
                return;
            }

            String torreSeleccionada = spnTorreComentario.getSelectedItem().toString();
            viewModel.registrarComentario(remitente, torreSeleccionada, texto);

            etComentario.setText("");
            Toast.makeText(requireContext(), R.string.mensaje_observacion_enviada, Toast.LENGTH_SHORT).show();
        });
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

    private void actualizarSelectorTorre() {
        List<String> nuevasOpciones = new ArrayList<>();
        if (!datosRealesDisponibles) {
            nuevasOpciones.add(getString(R.string.historial_torre_sin_conexion));
        } else {
            for (Huerto huerto : torresDisponibles) {
                nuevasOpciones.add(getString(R.string.formato_torre_cultivo,
                        huerto.idx, nombreCultivoLocalizado(huerto.perfil)));
            }
        }

        if (nuevasOpciones.equals(opcionesPrevias)) return;
        int posicionSeleccionada = spnTorreComentario.getSelectedItemPosition();
        opcionesPrevias.clear();
        opcionesPrevias.addAll(nuevasOpciones);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, nuevasOpciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTorreComentario.setAdapter(adapter);
        if (posicionSeleccionada >= 0 && posicionSeleccionada < nuevasOpciones.size()) {
            spnTorreComentario.setSelection(posicionSeleccionada);
        }
        spnTorreComentario.setEnabled(datosRealesDisponibles && !nuevasOpciones.isEmpty());
    }

    private void actualizarEstadoConexion() {
        if (!conectadoReal) {
            tvEstadoConexion.setText(R.string.historial_sin_conexion);
        } else if (!datosRealesDisponibles) {
            tvEstadoConexion.setText(R.string.historial_esperando_datos);
        } else {
            tvEstadoConexion.setText(R.string.historial_conectado);
        }
    }

    private void actualizarAlertasReales() {
        if (tvAlertasReales == null || viewModel == null) return;
        if (!datosRealesDisponibles) {
            tvAlertasReales.setText(conectadoReal
                    ? R.string.historial_alertas_esperando_datos
                    : R.string.historial_alertas_sin_conexion);
            return;
        }

        List<Alerta> alertas = viewModel.getAlertasActuales().getValue();
        if (alertas == null || alertas.isEmpty()) {
            tvAlertasReales.setText(R.string.historial_alertas_vacias);
            return;
        }

        StringBuilder texto = new StringBuilder();
        for (Alerta alerta : alertas) {
            texto.append(getString(R.string.historial_alerta_formato,
                    alerta.variable.replace('_', ' ').toUpperCase(Locale.getDefault()),
                    alerta.nivel.toUpperCase(Locale.getDefault()),
                    alerta.valor,
                    alerta.accion))
                    .append('\n');
        }
        tvAlertasReales.setText(texto.toString().trim());
    }

    private void mostrarObservacionesGuardadas(List<ComentarioUsuario> comentarios, String usuarioActual) {
        if (comentarios == null || comentarios.isEmpty()) {
            tvObservacionesGuardadas.setText(R.string.historial_sin_observaciones);
            return;
        }

        StringBuilder texto = new StringBuilder();
        for (ComentarioUsuario comentario : comentarios) {
            if (usuarioActual.equals(comentario.usuario)) {
                texto.append(getString(R.string.historial_observacion_formato,
                        comentario.fechaHora, comentario.torre, comentario.mensaje))
                        .append('\n');
            }
        }
        tvObservacionesGuardadas.setText(texto.length() == 0
                ? getString(R.string.historial_sin_observaciones)
                : texto.toString().trim());
    }
}
