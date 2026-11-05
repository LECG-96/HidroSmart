package com.devst.hidrosmart.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.devst.hidrosmart.data.SimuladorRepository;
import com.devst.hidrosmart.modelo.Alerta;
import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.devst.hidrosmart.modelo.RespuestaComando;

import java.util.Collections;
import java.util.List;

public class HuertoViewModel extends ViewModel implements SimuladorRepository.ObservadorSimulador {

    private final SimuladorRepository repository;

    // Estados observables para la UI
    private final MutableLiveData<Huerto> huertoActual = new MutableLiveData<>();
    private final MutableLiveData<List<Huerto>> listaTorres = new MutableLiveData<>();
    private final MutableLiveData<List<Alerta>> alertasActuales = new MutableLiveData<>();
    private final MutableLiveData<List<String>> usuariosGenerales = new MutableLiveData<>();
    private final MutableLiveData<List<ComentarioUsuario>> comentarios = new MutableLiveData<>();

    public HuertoViewModel() {
        this.repository = SimuladorRepository.getInstancia();
        this.repository.agregarObservador(this);
        this.repository.iniciarSimulacion();
        actualizarDatos();
    }

    @Override
    public void onTickSimulacion() {
        actualizarDatos();
    }

    private void actualizarDatos() {
        Huerto actual = repository.getHuertoActual();
        huertoActual.postValue(actual);
        listaTorres.postValue(repository.getTorres());
        usuariosGenerales.postValue(repository.getUsuariosGenerales());
        comentarios.postValue(repository.getComentariosRegistrados());

        if (actual != null) {
            alertasActuales.postValue(actual.alertas());
        } else {
            alertasActuales.postValue(Collections.emptyList());
        }
    }

    // ------------------------------------------------------------------------
    // GETTERS DE LIVEDATA (OBSERVABLES DESDE FRAGMENTS/ACTIVITIES)
    // ------------------------------------------------------------------------
    public LiveData<Huerto> getHuertoActual() {
        return huertoActual;
    }

    public LiveData<List<Huerto>> getListaTorres() {
        return listaTorres;
    }

    public LiveData<List<Alerta>> getAlertasActuales() {
        return alertasActuales;
    }

    public LiveData<List<String>> getUsuariosGenerales() {
        return usuariosGenerales;
    }

    public LiveData<List<ComentarioUsuario>> getComentarios() {
        return comentarios;
    }

    public int getIndiceTorreSeleccionada() {
        return repository.getTorreSeleccionada();
    }

    // ------------------------------------------------------------------------
    // ACCIONES DISPARADAS DESDE LA UI
    // ------------------------------------------------------------------------
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
        repository.aplicarEscenario(escenario);
        actualizarDatos();
    }

    public void forzarParametro(String variable, double valor, boolean persistente) {
        repository.forzarParametro(variable, valor, persistente);
        actualizarDatos();
    }

    public void rellenarEstanque() {
        repository.rellenarEstanque();
        actualizarDatos();
    }

    public void setCorteDatos(boolean activo) {
        repository.setCorteDatos(activo);
    }

    public void registrarComentario(String usuario, String torre, String mensaje) {
        repository.agregarComentario(usuario, torre, mensaje);
        actualizarDatos();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removerObservador(this);
    }
}