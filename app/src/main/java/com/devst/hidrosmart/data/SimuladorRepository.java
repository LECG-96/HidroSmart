package com.devst.hidrosmart.data;

import android.os.Handler;
import android.os.Looper;

import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.modelo.PerfilCultivo;
import com.devst.hidrosmart.modelo.RespuestaComando;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SimuladorRepository {

    public interface ObservadorSimulador {
        void onTickSimulacion();
    }

    private static SimuladorRepository instancia;

    private static final String[] CULTIVOS_DEFECTO = {
            "lechuga", "albahaca", "cilantro", "menta", "acelga", "tomate_cherry"
    };

    private final List<Huerto> torres = new ArrayList<>();
    private final List<String> usuariosGenerales = new ArrayList<>();
    private final List<ComentarioUsuario> comentariosRegistrados = new ArrayList<>();
    private final List<ObservadorSimulador> observadores = new ArrayList<>();

    private int torreSeleccionada = 0;
    private boolean corteDatosActivo = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnableSimulacion;
    private boolean simulando = false;

    private SimuladorRepository() {
        // Inicialización de usuarios del sistema
        usuariosGenerales.add("user");

        // Inicialización base de 2 torres
        reconstruirTorres(2);
    }

    public static synchronized SimuladorRepository getInstancia() {
        if (instancia == null) {
            instancia = new SimuladorRepository();
        }
        return instancia;
    }

    // ------------------------------------------------------------------------
    // GESTIÓN DEL CICLO DE SIMULACIÓN
    // ------------------------------------------------------------------------
    public synchronized void iniciarSimulacion() {
        if (simulando) return;
        simulando = true;

        runnableSimulacion = new Runnable() {
            @Override
            public void run() {
                if (!simulando) return;

                double ahora = System.currentTimeMillis() / 1000.0;
                synchronized (torres) {
                    for (Huerto h : torres) {
                        h.evolucionar();
                        if (!h.modoManual) {
                            h.automatizar(ahora);
                        }
                        h.temporizadores(ahora);
                    }
                }

                if (!corteDatosActivo) {
                    notificarObservadores();
                }

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnableSimulacion);
    }

    public synchronized void detenerSimulacion() {
        simulando = false;
        if (runnableSimulacion != null) {
            handler.removeCallbacks(runnableSimulacion);
        }
    }

    public void agregarObservador(ObservadorSimulador obs) {
        if (!observadores.contains(obs)) {
            observadores.add(obs);
        }
    }

    public void removerObservador(ObservadorSimulador obs) {
        observadores.remove(obs);
    }

    private void notificarObservadores() {
        for (ObservadorSimulador obs : observadores) {
            obs.onTickSimulacion();
        }
    }

    // ------------------------------------------------------------------------
    // GESTIÓN DE TORRES Y CULTIVOS
    // ------------------------------------------------------------------------
    public void reconstruirTorres(int cantidad) {
        synchronized (torres) {
            torres.clear();
            for (int i = 0; i < cantidad; i++) {
                String cultivo = CULTIVOS_DEFECTO[i % CULTIVOS_DEFECTO.length];
                torres.add(new Huerto(cultivo, 20.0, 60.0, 10.0, i + 1));
            }
            torreSeleccionada = 0;
        }
        notificarObservadores();
    }

    public boolean agregarTorre() {
        synchronized (torres) {
            if (torres.size() >= 4) return false;
            int nuevoIdx = torres.size() + 1;
            String cultivo = CULTIVOS_DEFECTO[(nuevoIdx - 1) % CULTIVOS_DEFECTO.length];
            torres.add(new Huerto(cultivo, 20.0, 60.0, 10.0, nuevoIdx));
        }
        notificarObservadores();
        return true;
    }

    public boolean eliminarTorre() {
        synchronized (torres) {
            if (torres.size() <= 1) return false;
            torres.remove(torres.size() - 1);
            if (torreSeleccionada >= torres.size()) {
                torreSeleccionada = torres.size() - 1;
            }
        }
        notificarObservadores();
        return true;
    }

    public List<Huerto> getTorres() {
        return torres;
    }

    public int getTorreSeleccionada() {
        return torreSeleccionada;
    }

    public void setTorreSeleccionada(int indice) {
        if (indice >= 0 && indice < torres.size()) {
            this.torreSeleccionada = indice;
            notificarObservadores();
        }
    }

    public Huerto getHuertoActual() {
        synchronized (torres) {
            if (torreSeleccionada >= 0 && torreSeleccionada < torres.size()) {
                return torres.get(torreSeleccionada);
            }
            return null;
        }
    }

    public void cambiarCultivoTorreActual(String claveCultivo) {
        Huerto actual = getHuertoActual();
        if (actual != null) {
            actual.setPerfil(claveCultivo);
            notificarObservadores();
        }
    }

    // ------------------------------------------------------------------------
    // COMANDOS, ESCENARIOS Y FORZADO
    // ------------------------------------------------------------------------
    public RespuestaComando enviarComando(String actuador, String accion) {
        Huerto actual = getHuertoActual();
        if (actual == null) {
            return new RespuestaComando(false, "No hay torre seleccionada");
        }
        double ahora = System.currentTimeMillis() / 1000.0;
        RespuestaComando res = actual.comando(actuador, accion, ahora);
        notificarObservadores();
        return res;
    }

    public void aplicarEscenario(String escenario) {
        Huerto actual = getHuertoActual();
        if (actual != null) {
            actual.aplicarEscenario(escenario);
            notificarObservadores();
        }
    }

    public void forzarParametro(String variable, double valor, boolean persistente) {
        Huerto actual = getHuertoActual();
        if (actual != null) {
            synchronized (actual.lock) {
                actual.v.put(variable, valor);
                if (persistente && actual.base.containsKey(variable)) {
                    actual.base.put(variable, valor);
                }
            }
            notificarObservadores();
        }
    }

    public void rellenarEstanque() {
        Huerto actual = getHuertoActual();
        if (actual != null) {
            synchronized (actual.lock) {
                actual.v.put("nivel_agua", 100.0);
            }
            notificarObservadores();
        }
    }

    public void setCorteDatos(boolean activo) {
        this.corteDatosActivo = activo;
    }

    public boolean isCorteDatosActivo() {
        return corteDatosActivo;
    }

    // ------------------------------------------------------------------------
    // USUARIOS Y OBSERVACIONES
    // ------------------------------------------------------------------------
    public List<String> getUsuariosGenerales() {
        return new ArrayList<>(usuariosGenerales);
    }

    public List<ComentarioUsuario> getComentariosRegistrados() {
        return new ArrayList<>(comentariosRegistrados);
    }

    public void agregarComentario(String usuario, String torre, String mensaje) {
        String horaActual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        comentariosRegistrados.add(new ComentarioUsuario(usuario, torre, mensaje, horaActual));
    }
}