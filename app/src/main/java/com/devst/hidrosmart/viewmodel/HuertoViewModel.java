package com.devst.hidrosmart.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.devst.hidrosmart.data.HuertoDataSource;
import com.devst.hidrosmart.data.HuertoDataSourceProvider;
import com.devst.hidrosmart.data.SimuladorDataSource;
import com.devst.hidrosmart.modelo.Alerta;
import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.devst.hidrosmart.modelo.RespuestaComando;

import java.util.Collections;
import java.util.List;

public class HuertoViewModel extends ViewModel implements HuertoDataSource.Observer {

    private final HuertoDataSource repository;
    private final SimuladorDataSource simulador;

    private final MutableLiveData<Huerto> huertoActual = new MutableLiveData<>();
    private final MutableLiveData<List<Huerto>> listaTorres = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> alertasActuales = new MutableLiveData<>();
    private final MutableLiveData<List<String>> usuariosGenerales = new MutableLiveData<>();
    private final MutableLiveData<List<ComentarioUsuario>> comentarios = new MutableLiveData<>();
    private final MutableLiveData<Boolean> corteDatosActivo = new MutableLiveData<>();

    public HuertoViewModel() {
        this.repository = HuertoDataSourceProvider.get();
        this.simulador = repository instanceof SimuladorDataSource
                ? (SimuladorDataSource) repository : null;
        this.repository.addObserver(this);
        this.repository.start();
        actualizarDatos();
    }

    @Override
    public void onDataChanged() {
        actualizarDatos();
    }

    private void actualizarDatos() {
        Huerto actual = repository.getHuertoActual();
        huertoActual.postValue(actual);
        listaTorres.postValue(repository.getTorres());
        usuariosGenerales.postValue(repository.getUsuariosGenerales());
        comentarios.postValue(repository.getComentariosRegistrados());
        corteDatosActivo.postValue(simulador != null && simulador.isCorteDatos());

        if (actual != null) {
            alertasActuales.postValue(actual.alertas());
        } else {
            alertasActuales.postValue(Collections.emptyList());
        }
    }

    public LiveData<Huerto> getHuertoActual() { return huertoActual; }
    public LiveData<List<Huerto>> getListaTorres() { return listaTorres; }
    public LiveData<List<Alerta>> getAlertasActuales() { return alertasActuales; }
    public LiveData<List<String>> getUsuariosGenerales() { return usuariosGenerales; }
    public LiveData<List<ComentarioUsuario>> getComentarios() { return comentarios; }
    public LiveData<Boolean> getCorteDatosActivo() { return corteDatosActivo; }

    public int getIndiceTorreSeleccionada() { return repository.getTorreSeleccionada(); }

    public void pausarSimulacion() {
        repository.stop();
    }

    public void reanudarSimulacion() {
        repository.start();
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

    public void setCorteDatos(boolean activo) {
        if (simulador != null) simulador.setCorteDatos(activo);
    }

    public void registrarComentario(String usuario, String torre, String mensaje) {
        repository.agregarComentario(usuario, torre, mensaje);
        actualizarDatos();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeObserver(this);
    }
}
