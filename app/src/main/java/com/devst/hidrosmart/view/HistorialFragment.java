package com.devst.hidrosmart.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.modelo.EventoAlerta;
import com.devst.hidrosmart.modelo.HuertoSnapshot;
import com.devst.hidrosmart.modelo.MedicionMuestra;
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
    private TextView tvHistorialEventos;
    private TextView tvTituloObservaciones;
    private LinearLayout layoutHistorialEventos;
    private Button btnAtenderAlertas;
    private Spinner spnTorreGrafico, spnVariableGrafico, spnPeriodoGrafico;
    private GraficoMedicionesView graficoMediciones;
    private TextView tvResumenMediciones;
    private LinearLayout layoutGraficoMediciones;

    // Control para no resetear el spinner a cada segundo
    private final List<String> opcionesPrevias = new ArrayList<>();
    private final List<String> opcionesGraficoPrevias = new ArrayList<>();
    private List<HuertoSnapshot> torresDisponibles = new ArrayList<>();
    private boolean conectadoReal = false;
    private boolean datosRealesDisponibles = false;
    private boolean esModoDemo = false;
    private boolean esModoAdmin = false;

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
        tvTituloObservaciones = view.findViewById(R.id.tvTituloObservaciones);
        tvHistorialEventos = view.findViewById(R.id.tvHistorialEventos);
        layoutHistorialEventos = view.findViewById(R.id.layoutHistorialEventos);
        btnAtenderAlertas = view.findViewById(R.id.btnAtenderAlertas);
        spnTorreGrafico = view.findViewById(R.id.spnTorreGrafico);
        spnVariableGrafico = view.findViewById(R.id.spnVariableGrafico);
        spnPeriodoGrafico = view.findViewById(R.id.spnPeriodoGrafico);
        graficoMediciones = view.findViewById(R.id.graficoMediciones);
        tvResumenMediciones = view.findViewById(R.id.tvResumenMediciones);
        layoutGraficoMediciones = view.findViewById(R.id.layoutGraficoMediciones);
        esModoDemo = requireActivity().getIntent()
                .getBooleanExtra(MainActivity.EXTRA_MODO_DEMO, false);
        esModoAdmin = Login.ROL_ADMIN.equals(requireActivity().getIntent().getStringExtra(Login.EXTRA_ROL))
                && !esModoDemo;
        if (esModoAdmin) tvTituloObservaciones.setText(R.string.historial_todas_observaciones);
        btnAtenderAlertas.setOnClickListener(v -> viewModel.marcarAlertasActivasAtendidas());
        configurarGrafico();
        viewModel.getConectadoSistemaReal().observe(getViewLifecycleOwner(), conectado -> {
            conectadoReal = Boolean.TRUE.equals(conectado);
            actualizarEstadoConexion();
            actualizarAlertasReales();
        });
        viewModel.getDatosRealesDisponibles().observe(getViewLifecycleOwner(), disponibles -> {
            datosRealesDisponibles = esModoDemo || Boolean.TRUE.equals(disponibles);
            actualizarEstadoConexion();
            actualizarSelectorTorre();
            actualizarAlertasReales();
            actualizarVisibilidadHistorial();
        });
        viewModel.getAlertasActivas().observe(getViewLifecycleOwner(), this::mostrarAlertasActivas);
        viewModel.getHistorialAlertas().observe(getViewLifecycleOwner(), this::mostrarHistorialEventos);
        viewModel.getMedicionesHistorial().observe(getViewLifecycleOwner(), this::mostrarMediciones);
        String usuarioActual = requireActivity().getIntent().getStringExtra(Login.EXTRA_USUARIO_ACTUAL);
        if (usuarioActual == null) usuarioActual = "user";
        final String remitente = usuarioActual;

        viewModel.getComentarios().observe(getViewLifecycleOwner(), comentarios ->
                mostrarObservacionesGuardadas(comentarios, remitente));

        viewModel.getListaTorres().observe(getViewLifecycleOwner(), torres -> {
            if (torres == null) return;
            torresDisponibles = torres;
            actualizarSelectorTorre();
            actualizarSelectorGrafico();
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

    private void configurarGrafico() {
        ArrayAdapter<String> variables = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.historial_variable_nombres));
        variables.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnVariableGrafico.setAdapter(variables);

        ArrayAdapter<String> periodos = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.historial_periodo_nombres));
        periodos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnPeriodoGrafico.setAdapter(periodos);

        android.widget.AdapterView.OnItemSelectedListener cambios = new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                cargarGraficoSeleccionado();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        };
        spnVariableGrafico.setOnItemSelectedListener(cambios);
        spnPeriodoGrafico.setOnItemSelectedListener(cambios);
        spnTorreGrafico.setOnItemSelectedListener(cambios);
    }

    private void actualizarSelectorGrafico() {
        List<String> nuevasOpciones = new ArrayList<>();
        for (HuertoSnapshot huerto : torresDisponibles) {
            nuevasOpciones.add(getString(R.string.formato_torre_cultivo,
                    huerto.idx, nombreCultivoLocalizado(huerto.perfil)));
        }
        if (nuevasOpciones.equals(opcionesGraficoPrevias)) return;
        int posicion = spnTorreGrafico.getSelectedItemPosition();
        opcionesGraficoPrevias.clear();
        opcionesGraficoPrevias.addAll(nuevasOpciones);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, nuevasOpciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnTorreGrafico.setAdapter(adapter);
        if (posicion >= 0 && posicion < nuevasOpciones.size()) spnTorreGrafico.setSelection(posicion);
    }

    private void cargarGraficoSeleccionado() {
        if (spnVariableGrafico.getSelectedItemPosition() < 0
                || spnPeriodoGrafico.getSelectedItemPosition() < 0) return;
        int posicionTorre = spnTorreGrafico.getSelectedItemPosition();
        int torre = posicionTorre >= 0 && posicionTorre < torresDisponibles.size()
                ? torresDisponibles.get(posicionTorre).idx : 1;
        String[] claves = getResources().getStringArray(R.array.historial_variable_claves);
        int posicionVariable = spnVariableGrafico.getSelectedItemPosition();
        if (posicionVariable >= claves.length) return;
        long[] periodos = {60L * 60L * 1000L, 6L * 60L * 60L * 1000L, 24L * 60L * 60L * 1000L};
        int posicionPeriodo = Math.min(spnPeriodoGrafico.getSelectedItemPosition(), periodos.length - 1);
        viewModel.cargarMediciones(torre, claves[posicionVariable], periodos[posicionPeriodo]);
    }

    private void mostrarMediciones(List<MedicionMuestra> muestras) {
        if (graficoMediciones == null || tvResumenMediciones == null) return;
        graficoMediciones.setMuestras(muestras);
        if (muestras == null || muestras.isEmpty()) {
            graficoMediciones.setMensajeVacio(getString(R.string.historial_grafico_sin_muestras));
            tvResumenMediciones.setText(R.string.historial_grafico_vacio);
            return;
        }
        String[] unidades = getResources().getStringArray(R.array.historial_variable_unidades);
        int variable = Math.min(spnVariableGrafico.getSelectedItemPosition(), unidades.length - 1);
        MedicionMuestra ultima = muestras.get(muestras.size() - 1);
        tvResumenMediciones.setText(getString(R.string.historial_grafico_resumen,
                ultima.valor, unidades[variable], muestras.size()));
    }

    private String nombreCultivoLocalizado(PerfilCultivo perfil) {
        if (perfil == null) return "Cultivo";
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

    private String nombreCultivoLocalizado(String clave) {
        PerfilCultivo perfil = PerfilCultivo.porClave(clave);
        return perfil == null ? clave : nombreCultivoLocalizado(perfil);
    }

    private void actualizarSelectorTorre() {
        List<String> nuevasOpciones = new ArrayList<>();
        boolean datosLocalesDisponibles = esModoDemo || (esModoAdmin && viewModel.esSimulador());
        if (!datosLocalesDisponibles && !datosRealesDisponibles) {
            nuevasOpciones.add(getString(R.string.historial_torre_sin_conexion));
        } else {
            for (HuertoSnapshot huerto : torresDisponibles) {
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
        spnTorreComentario.setEnabled((esModoDemo || (esModoAdmin && viewModel.esSimulador()) || datosRealesDisponibles)
                && !nuevasOpciones.isEmpty());
    }

    private void actualizarEstadoConexion() {
        if (esModoDemo) {
            tvEstadoConexion.setText(R.string.historial_demo_simulacion);
            return;
        }
        if (esModoAdmin && viewModel.esSimulador() && !conectadoReal) {
            tvEstadoConexion.setText(R.string.historial_admin_simulacion);
            return;
        }
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
        if (!esModoDemo && !(esModoAdmin && viewModel.esSimulador()) && !datosRealesDisponibles) {
            tvAlertasReales.setText(conectadoReal
                    ? R.string.historial_alertas_esperando_datos
                    : R.string.historial_alertas_sin_conexion);
            btnAtenderAlertas.setVisibility(View.GONE);
            return;
        }
        mostrarAlertasActivas(viewModel.getAlertasActivas().getValue());
    }

    private void mostrarAlertasActivas(List<EventoAlerta> alertas) {
        if (tvAlertasReales == null) return;
        if (!esModoDemo && !(esModoAdmin && viewModel.esSimulador()) && !datosRealesDisponibles) {
            actualizarAlertasReales();
            return;
        }
        if (alertas == null || alertas.isEmpty()) {
            tvAlertasReales.setText(R.string.historial_alertas_vacias);
            btnAtenderAlertas.setVisibility(View.GONE);
            return;
        }

        boolean hayPendientes = false;
        StringBuilder texto = new StringBuilder();
        for (EventoAlerta alerta : alertas) {
            hayPendientes |= !alerta.atendida;
            texto.append(getString(R.string.historial_alerta_activa_formato,
                    alerta.torre,
                    nombreCultivoLocalizado(alerta.cultivo),
                    alerta.variable.replace('_', ' ').toUpperCase(Locale.getDefault()),
                    alerta.nivel.toUpperCase(Locale.getDefault()),
                    alerta.valor,
                    alerta.accion,
                    getString(alerta.atendida ? R.string.historial_evento_atendida
                            : R.string.historial_evento_pendiente)))
                    .append('\n');
        }
        tvAlertasReales.setText(texto.toString().trim());
        btnAtenderAlertas.setVisibility(View.VISIBLE);
        btnAtenderAlertas.setEnabled(hayPendientes);
        btnAtenderAlertas.setText(hayPendientes
                ? R.string.historial_atender_alertas
                : R.string.historial_alertas_revisadas);
    }

    private void mostrarHistorialEventos(List<EventoAlerta> eventos) {
        if (tvHistorialEventos == null || layoutHistorialEventos == null) return;
        boolean visible = actualizarVisibilidadHistorial();
        if (!visible) return;
        if (eventos == null || eventos.isEmpty()) {
            tvHistorialEventos.setText(R.string.historial_eventos_vacio);
            return;
        }

        StringBuilder texto = new StringBuilder();
        for (EventoAlerta evento : eventos) {
            texto.append(getString(R.string.historial_evento_formato,
                    evento.fechaHora,
                    evento.torre,
                    nombreCultivoLocalizado(evento.cultivo),
                    evento.variable.replace('_', ' ').toUpperCase(Locale.getDefault())
                            + " · " + evento.nivel.toUpperCase(Locale.getDefault()),
                    evento.valor,
                    getString(EventoAlerta.ACTIVA.equals(evento.tipo)
                            ? R.string.historial_evento_activada : R.string.historial_evento_resuelta),
                    getString(EventoAlerta.RESUELTA.equals(evento.tipo)
                            ? R.string.historial_evento_cerrada
                            : (evento.atendida ? R.string.historial_evento_atendida
                            : R.string.historial_evento_pendiente))))
                    .append('\n');
        }
        tvHistorialEventos.setText(texto.toString().trim());
    }

    private boolean actualizarVisibilidadHistorial() {
        boolean visible = esModoDemo || (esModoAdmin && viewModel.esSimulador()) || datosRealesDisponibles;
        if (layoutHistorialEventos != null) {
            layoutHistorialEventos.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (layoutGraficoMediciones != null) {
            layoutGraficoMediciones.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        return visible;
    }

    private void mostrarObservacionesGuardadas(List<ComentarioUsuario> comentarios, String usuarioActual) {
        if (comentarios == null || comentarios.isEmpty()) {
            tvObservacionesGuardadas.setText(R.string.historial_sin_observaciones);
            return;
        }

        StringBuilder texto = new StringBuilder();
        for (ComentarioUsuario comentario : comentarios) {
            if (esModoAdmin || usuarioActual.equals(comentario.usuario)) {
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
