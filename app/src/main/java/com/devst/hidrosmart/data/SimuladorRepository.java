package com.devst.hidrosmart.data;

import android.os.Handler;
import android.os.Looper;

import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.devst.hidrosmart.modelo.RespuestaComando;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimuladorRepository {

    private static SimuladorRepository instancia;

    public interface ObservadorSimulador {
        void onTickSimulacion();
    }

    private final List<ObservadorSimulador> observadores = new ArrayList<>();
    private final List<Huerto> torres = new ArrayList<>();
    private final List<String> usuariosGenerales = new ArrayList<>();
    private final List<ComentarioUsuario> comentariosRegistrados = new ArrayList<>();

    private int torreSeleccionada = 0;
    private boolean corteDatos = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean simulacionActiva = false;

    private final Runnable runnableTick = new Runnable() {
        @Override
        public void run() {
            if (!simulacionActiva) return;
            if (!corteDatos) {
                synchronized (torres) {
                    for (Huerto h : torres) {
                        h.tick(1.0);
                    }
                }
            }
            notificarObservadores();
            handler.postDelayed(this, 1000);
        }
    };

    private SimuladorRepository() {
        usuariosGenerales.add("user");
        usuariosGenerales.add("operador1");

        // Inicialización base
        torres.add(new Huerto(1, PerfilCultivo.todos().get(0)));
        torres.add(new Huerto(2, PerfilCultivo.todos().get(1)));

        comentariosRegistrados.add(new ComentarioUsuario("user", "Torre 1 (Lechuga)", "Hojas firmes y coloración verde normal. Sin signos de plagas.", "09:15"));
    }

    public static synchronized SimuladorRepository getInstancia() {
        if (instancia == null) {
            instancia = new SimuladorRepository();
        }
        return instancia;
    }

    public synchronized void iniciarSimulacion() {
        if (!simulacionActiva) {
            simulacionActiva = true;
            handler.removeCallbacks(runnableTick);
            handler.post(runnableTick);
        }
    }

    public synchronized void pausarSimulacion() {
        simulacionActiva = false;
        handler.removeCallbacks(runnableTick);
    }

    public synchronized void reanudarSimulacion() {
        iniciarSimulacion();
    }

    public synchronized void agregarObservador(ObservadorSimulador obs) {
        if (!observadores.contains(obs)) observadores.add(obs);
    }

    public synchronized void removerObservador(ObservadorSimulador obs) {
        observadores.remove(obs);
    }

    private void notificarObservadores() {
        List<ObservadorSimulador> copia;
        synchronized (this) {
            copia = new ArrayList<>(observadores);
        }
        for (ObservadorSimulador obs : copia) {
            obs.onTickSimulacion();
        }
    }

    // Lista protegida e inmutable
    public synchronized List<Huerto> getTorres() {
        return Collections.unmodifiableList(new ArrayList<>(torres));
    }

    public synchronized Huerto getHuertoActual() {
        if (torres.isEmpty()) return null;
        if (torreSeleccionada >= torres.size()) torreSeleccionada = 0;
        return torres.get(torreSeleccionada);
    }

    public synchronized int getTorreSeleccionada() {
        return torreSeleccionada;
    }

    public synchronized void setTorreSeleccionada(int indice) {
        if (indice >= 0 && indice < torres.size()) {
            this.torreSeleccionada = indice;
        }
    }

    public synchronized boolean agregarTorre() {
        if (torres.size() >= 4) return false;
        int nuevoIdx = torres.size() + 1;
        List<PerfilCultivo> perfiles = PerfilCultivo.todos();
        PerfilCultivo perfil = perfiles.get((nuevoIdx - 1) % perfiles.size());
        torres.add(new Huerto(nuevoIdx, perfil));
        notificarObservadores();
        return true;
    }

    public synchronized boolean eliminarTorre() {
        if (torres.size() <= 1) return false;
        torres.remove(torres.size() - 1);
        if (torreSeleccionada >= torres.size()) {
            torreSeleccionada = torres.size() - 1;
        }
        notificarObservadores();
        return true;
    }

    public synchronized void cambiarCultivoTorreActual(String perfilClave) {
        Huerto actual = getHuertoActual();
        if (actual == null) return;
        for (PerfilCultivo p : PerfilCultivo.todos()) {
            if (p.clave.equals(perfilClave)) {
                actual.cambiarPerfil(p);
                notificarObservadores();
                break;
            }
        }
    }

    public synchronized RespuestaComando enviarComando(String actuador, String orden) {
        Huerto actual = getHuertoActual();
        if (actual == null) return new RespuestaComando(false, "No hay torre activa");
        return actual.enviarComando(actuador, orden);
    }

    public synchronized void aplicarEscenario(String escenario) {
        Huerto actual = getHuertoActual();
        if (actual != null) {
            actual.aplicarEscenario(escenario);
            notificarObservadores();
        }
    }

    public synchronized void forzarParametro(String variable, double valor, boolean persistente) {
        Huerto actual = getHuertoActual();
        if (actual != null) {
            actual.forzarParametro(variable, valor, persistente);
            notificarObservadores();
        }
    }

    public synchronized void rellenarEstanque() {
        Huerto actual = getHuertoActual();
        if (actual != null) {
            actual.rellenarEstanque();
            notificarObservadores();
        }
    }

    public synchronized void setCorteDatos(boolean corte) {
        this.corteDatos = corte;
        notificarObservadores();
    }

    public synchronized boolean isCorteDatos() {
        return corteDatos;
    }

    public synchronized List<String> getUsuariosGenerales() {
        return Collections.unmodifiableList(new ArrayList<>(usuariosGenerales));
    }

    public synchronized List<ComentarioUsuario> getComentariosRegistrados() {
        return Collections.unmodifiableList(new ArrayList<>(comentariosRegistrados));
    }

    public synchronized void agregarComentario(String usuario, String torre, String mensaje) {
        ComentarioUsuario nuevo = new ComentarioUsuario(usuario, torre, mensaje);
        comentariosRegistrados.add(0, nuevo);
        notificarObservadores();
    }
}