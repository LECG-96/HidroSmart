package com.devst.hidrosmart.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.AndroidViewModel;
import android.app.Application;

import com.devst.hidrosmart.data.HuertoDataSource;
import com.devst.hidrosmart.data.HuertoDataSourceProvider;
import com.devst.hidrosmart.data.ComentarioLocalDataSource;
import com.devst.hidrosmart.data.HistorialLocalDataSource;
import com.devst.hidrosmart.data.HistorialMedicionesDataSource;
import com.devst.hidrosmart.data.SimuladorDataSource;
import com.devst.hidrosmart.modelo.Alerta;
import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.HuertoSnapshot;
import com.devst.hidrosmart.modelo.EventoAlerta;
import com.devst.hidrosmart.modelo.MedicionMuestra;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.devst.hidrosmart.modelo.RespuestaComando;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HuertoViewModel extends AndroidViewModel implements HuertoDataSource.Observer {

    private final HuertoDataSource repository;
    private final ComentarioLocalDataSource comentarioDataSource;
    private final HistorialLocalDataSource historialDataSource;
    private final HistorialMedicionesDataSource historialMedicionesDataSource;
    private final SimuladorDataSource simulador;
    private final ExecutorService comentariosExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<HuertoSnapshot> huertoActual = new MutableLiveData<>();
    private final MutableLiveData<List<HuertoSnapshot>> listaTorres = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> alertasActuales = new MutableLiveData<>();
    private final MutableLiveData<List<ComentarioUsuario>> comentarios = new MutableLiveData<>();
    private final MutableLiveData<List<EventoAlerta>> historialAlertas = new MutableLiveData<>();
    private final MutableLiveData<List<EventoAlerta>> alertasActivas = new MutableLiveData<>();
    private final MutableLiveData<List<MedicionMuestra>> medicionesHistorial = new MutableLiveData<>();
    private final MutableLiveData<Boolean> corteDatosActivo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> conectadoSistemaReal = new MutableLiveData<>();
    private final MutableLiveData<Boolean> datosRealesDisponibles = new MutableLiveData<>();
    private Set<String> clavesAlertasAnteriores;
    private boolean registrarHistorialAlertas;
    private long ultimaMuestraMillis;
    private long ultimaSincronizacionAlertasMillis;
    private int torreGrafico = 1;
    private String variableGrafico = "temp_ambiente";
    private long periodoGraficoMillis = 60L * 60L * 1000L;
    private static final long INTERVALO_MUESTREO_MILLIS = 30L * 1000L;

    public HuertoViewModel(Application application) {
        super(application);
        this.repository = HuertoDataSourceProvider.get();
        this.comentarioDataSource = HuertoDataSourceProvider.getComentarios(application);
        this.historialDataSource = HuertoDataSourceProvider.getHistorial(application);
        this.historialMedicionesDataSource = HuertoDataSourceProvider.getHistorialMediciones(application);
        this.simulador = repository instanceof SimuladorDataSource
                ? (SimuladorDataSource) repository : null;
        this.registrarHistorialAlertas = simulador == null;
        comentarios.setValue(Collections.emptyList());
        historialAlertas.setValue(Collections.emptyList());
        alertasActivas.setValue(Collections.emptyList());
        medicionesHistorial.setValue(Collections.emptyList());
        this.repository.addObserver(this);
        comentariosExecutor.execute(() -> comentarios.postValue(comentarioDataSource.obtenerTodos()));
        actualizarDatos();
    }

    @Override
    public void onDataChanged() {
        actualizarDatos();
    }

    private synchronized void actualizarDatos() {
        Huerto actual = repository.getHuertoActual();
        HuertoSnapshot actualSnapshot = HuertoSnapshot.desde(actual);
        huertoActual.postValue(actualSnapshot);
        List<HuertoSnapshot> torresSnapshot = new java.util.ArrayList<>();
        List<EventoAlerta> alertasSimultaneas = new ArrayList<>();
        Set<String> clavesActuales = new HashSet<>();
        for (Huerto torre : repository.getTorres()) {
            HuertoSnapshot snapshot = HuertoSnapshot.desde(torre);
            torresSnapshot.add(snapshot);
            for (Alerta alerta : snapshot.alertas) {
                EventoAlerta evento = new EventoAlerta(0, snapshot.idx, snapshot.perfil.clave,
                        alerta.variable, alerta.nivel, alerta.valor, alerta.accion,
                        EventoAlerta.ACTIVA, EventoAlerta.ahora(), false);
                alertasSimultaneas.add(evento);
                clavesActuales.add(evento.clave());
            }
        }
        listaTorres.postValue(java.util.Collections.unmodifiableList(torresSnapshot));
        corteDatosActivo.postValue(simulador != null && simulador.isCorteDatos());
        conectadoSistemaReal.postValue(repository.estaConectadoAlSistemaReal());
        datosRealesDisponibles.postValue(repository.hayDatosRealesDisponibles());

        if (actualSnapshot != null) {
            alertasActuales.postValue(actualSnapshot.alertas);
        } else {
            alertasActuales.postValue(Collections.emptyList());
        }

        long ahora = System.currentTimeMillis();
        boolean puedeProcesarHistorial = registrarHistorialAlertas
                && (simulador != null || repository.hayDatosRealesDisponibles());
        boolean cambioEstadoAlertas = clavesAlertasAnteriores == null
                || !clavesAlertasAnteriores.equals(clavesActuales);
        if (puedeProcesarHistorial && (cambioEstadoAlertas
                || ahora - ultimaSincronizacionAlertasMillis >= INTERVALO_MUESTREO_MILLIS)) {
            clavesAlertasAnteriores = new HashSet<>(clavesActuales);
            ultimaSincronizacionAlertasMillis = ahora;
            List<EventoAlerta> estadoInmutable = java.util.Collections.unmodifiableList(
                    new ArrayList<>(alertasSimultaneas));
            comentariosExecutor.execute(() -> {
                historialDataSource.sincronizarAlertas(estadoInmutable);
                alertasActivas.postValue(historialDataSource.obtenerAlertasActivas());
                historialAlertas.postValue(historialDataSource.obtenerEventos(100));
            });
        }

        boolean puedeMuestrear = puedeProcesarHistorial
                && (simulador != null || repository.hayDatosRealesDisponibles());
        if (puedeMuestrear && (ultimaMuestraMillis == 0
                || ahora - ultimaMuestraMillis >= INTERVALO_MUESTREO_MILLIS)) {
            ultimaMuestraMillis = ahora;
            List<MedicionMuestra> muestras = new ArrayList<>();
            for (HuertoSnapshot snapshot : torresSnapshot) {
                String[] variables = {"temp_ambiente", "humedad", "temp_agua", "ph", "ec", "lux", "nivel_agua"};
                for (String variable : variables) {
                    Double valor = snapshot.v.get(variable);
                    if (valor != null) {
                        muestras.add(new MedicionMuestra(snapshot.idx, snapshot.perfil.clave,
                                variable, valor, ahora));
                    }
                }
            }
            List<MedicionMuestra> lote = java.util.Collections.unmodifiableList(muestras);
            comentariosExecutor.execute(() -> {
                historialMedicionesDataSource.guardarMuestras(lote);
                cargarMedicionesEnSegundoPlano();
            });
        }
    }

    public LiveData<HuertoSnapshot> getHuertoActual() { return huertoActual; }
    public LiveData<List<HuertoSnapshot>> getListaTorres() { return listaTorres; }
    public LiveData<List<Alerta>> getAlertasActuales() { return alertasActuales; }
    public LiveData<List<ComentarioUsuario>> getComentarios() { return comentarios; }
    public LiveData<List<EventoAlerta>> getHistorialAlertas() { return historialAlertas; }
    public LiveData<List<EventoAlerta>> getAlertasActivas() { return alertasActivas; }
    public LiveData<List<MedicionMuestra>> getMedicionesHistorial() { return medicionesHistorial; }
    public LiveData<Boolean> getCorteDatosActivo() { return corteDatosActivo; }
    public LiveData<Boolean> getConectadoSistemaReal() { return conectadoSistemaReal; }
    public LiveData<Boolean> getDatosRealesDisponibles() { return datosRealesDisponibles; }

    public int getIndiceTorreSeleccionada() { return repository.getTorreSeleccionada(); }
    public boolean esSimulador() { return simulador != null; }

    public void cargarMediciones(int torre, String variable, long periodoMillis) {
        synchronized (this) {
            torreGrafico = torre;
            variableGrafico = variable;
            periodoGraficoMillis = periodoMillis;
        }
        comentariosExecutor.execute(this::cargarMedicionesEnSegundoPlano);
    }

    private void cargarMedicionesEnSegundoPlano() {
        final int torre;
        final String variable;
        final long periodo;
        synchronized (this) {
            torre = torreGrafico;
            variable = variableGrafico;
            periodo = periodoGraficoMillis;
        }
        List<MedicionMuestra> datos = historialMedicionesDataSource.obtenerMuestras(
                torre, variable, System.currentTimeMillis() - periodo, 3000);
        medicionesHistorial.postValue(datos);
    }

    public synchronized void configurarRol(boolean esOperador) {
        if (simulador == null) return;
        registrarHistorialAlertas = !esOperador;
        clavesAlertasAnteriores = null;
        if (registrarHistorialAlertas) actualizarDatos();
    }

    public void pausarSimulacion() {
        repository.stop();
    }

    public void reanudarSimulacion(boolean soloDatosReales) {
        if (simulador == null || !soloDatosReales) repository.start();
    }

    public void seleccionarTorre(int indice) {
        repository.setTorreSeleccionada(indice);
        actualizarDatos();
    }

    public void cambiarCultivo(PerfilCultivo perfil) {
        if (perfil != null) {
            repository.cambiarCultivoTorreActual(perfil.clave);
            actualizarDatos();
        }
    }

    public boolean agregarTorre() {
        boolean res = repository.agregarTorre();
        actualizarDatos();
        return res;
    }

    public boolean eliminarTorre() {
        boolean res = repository.eliminarTorre();
        actualizarDatos();
        return res;
    }

    public RespuestaComando enviarComando(String actuador, boolean encendido) {
        RespuestaComando res = repository.enviarComando(actuador, encendido ? "on" : "off");
        actualizarDatos();
        return res;
    }

    public void setModoManual(boolean manual) {
        repository.enviarComando("modo", manual ? "manual" : "auto");
        actualizarDatos();
    }

    public void aplicarEscenario(String escenario) {
        if (simulador != null) {
            simulador.aplicarEscenario(escenario);
            actualizarDatos();
        }
    }

    public void forzarParametro(String variable, double valor, boolean persistente) {
        if (simulador != null) {
            simulador.forzarParametro(variable, valor, persistente);
            actualizarDatos();
        }
    }

    public void rellenarEstanque() {
        if (simulador != null) {
            simulador.rellenarEstanque();
            actualizarDatos();
        }
    }

    public void reiniciarDemo() {
        if (simulador != null) {
            simulador.reiniciarDemo();
            actualizarDatos();
        }
    }

    public void setCorteDatos(boolean activo) {
        if (simulador != null) simulador.setCorteDatos(activo);
    }

    public void registrarComentario(String usuario, String torre, String mensaje) {
        ComentarioUsuario comentario = new ComentarioUsuario(usuario, torre, mensaje);
        comentariosExecutor.execute(() -> {
            comentarioDataSource.guardar(comentario);
            comentarios.postValue(comentarioDataSource.obtenerTodos());
        });
    }

    public void marcarAlertasActivasAtendidas() {
        comentariosExecutor.execute(() -> {
            historialDataSource.marcarAlertasActivasAtendidas();
            alertasActivas.postValue(historialDataSource.obtenerAlertasActivas());
            historialAlertas.postValue(historialDataSource.obtenerEventos(100));
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeObserver(this);
        comentariosExecutor.shutdown();
    }
}
