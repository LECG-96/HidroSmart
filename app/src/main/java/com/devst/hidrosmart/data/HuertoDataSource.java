package com.devst.hidrosmart.data;

import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.RespuestaComando;

import java.util.List;

/** Fuente intercambiable de datos y comandos del huerto. */
public interface HuertoDataSource {
    interface Observer { void onDataChanged(); }

    void addObserver(Observer observer);
    void removeObserver(Observer observer);
    void start();
    void stop();
    boolean estaConectadoAlSistemaReal();
    List<Huerto> getTorres();
    Huerto getHuertoActual();
    int getTorreSeleccionada();
    void setTorreSeleccionada(int indice);
    boolean agregarTorre();
    boolean eliminarTorre();
    void cambiarCultivoTorreActual(String perfilClave);
    RespuestaComando enviarComando(String actuador, String orden);
    List<String> getUsuariosGenerales();
    List<ComentarioUsuario> getComentariosRegistrados();
    void agregarComentario(String usuario, String torre, String mensaje);
}
