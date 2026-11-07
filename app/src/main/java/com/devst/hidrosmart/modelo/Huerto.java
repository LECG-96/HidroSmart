package com.devst.hidrosmart.modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Huerto {
    public static final double LUX_LED_ON = 3000.0;
    public static final double LUX_LED_OFF = 3500.0;
    public static final double NIVEL_BAJO = 30.0;
    public static final double NIVEL_CRITICO = 15.0;
    public static final double DOSIS_S = 3.0;
    public static final double CERRADURA_S = 5.0;

    public final Object lock = new Object();
    public final int idx;
    private final Random random = new Random();

    public PerfilCultivo perfil;
    public String nombrePerfil;

    public double esperaDosis;
    public double riegoCada;
    public double riegoDura;
    public double reloj;

    public final Map<String, Double> base = new HashMap<>();
    public final Map<String, Double> v = new HashMap<>();
    public final Map<String, Boolean> act = new HashMap<>();
    public final Map<String, Double> hasta = new HashMap<>();
    public final Map<String, Integer> dosis = new HashMap<>();

    public boolean modoManual = false;
    public double tDosisNut;
    public double tDosisPh;
    public double tRiego;
    public final List<String> mensajes = new ArrayList<>();

    public Huerto(String clavePerfil, double esperaDosis, double riegoCada, double riegoDura, Double t0, int idx) {
        this.idx = idx;
        this.esperaDosis = esperaDosis;
        this.riegoCada = riegoCada;
        this.riegoDura = riegoDura;
        this.reloj = (t0 != null) ? t0 : (System.currentTimeMillis() / 1000.0);

        setPerfil(clavePerfil);
        inicializarValoresBase();
    }

    public Huerto(int idx, PerfilCultivo perfilInicial) {
        this.idx = idx;
        this.esperaDosis = 10.0;
        this.riegoCada = 30.0;
        this.riegoDura = 5.0;
        this.reloj = System.currentTimeMillis() / 1000.0;

        this.perfil = (perfilInicial != null) ? perfilInicial : PerfilCultivo.todos().get(0);
        this.nombrePerfil = this.perfil.clave;
        inicializarValoresBase();
    }

    public Huerto(String clavePerfil, double esperaDosis, double riegoCada, double riegoDura, int idx) {
        this(clavePerfil, esperaDosis, riegoCada, riegoDura, null, idx);
    }

    private void inicializarValoresBase() {
        double ph0 = Math.round(((perfil.phMin + perfil.phMax) / 2.0) * 100.0) / 100.0;
        double ec0 = Math.round(((perfil.ecMin + perfil.ecMax) / 2.0) * 100.0) / 100.0;

        base.put("temp_ambiente", Math.min(24.0, perfil.aireMax - 3.0));
        base.put("humedad", 62.0);
        base.put("temp_agua", 21.0);
        base.put("lux", 4500.0);
        base.put("ph", ph0);
        base.put("ec", ec0);
        base.put("nivel_agua", 80.0);

        v.putAll(base);

        String[] actuadores = {"bomba_riego", "nutrientes", "ph_pump", "ventilador", "led", "cerradura"};
        for (String a : actuadores) {
            act.put(a, false);
        }

        hasta.put("nutrientes", 0.0);
        hasta.put("ph_pump", 0.0);
        hasta.put("bomba_riego", 0.0);
        hasta.put("cerradura", 0.0);

        dosis.put("nutrientes", 0);
        dosis.put("ph", 0);

        this.tDosisNut = this.reloj - esperaDosis;
        this.tDosisPh = this.reloj - esperaDosis;
        this.tRiego = this.reloj;
    }

    public void setPerfil(String clave) {
        synchronized (lock) {
            PerfilCultivo p = PerfilCultivo.porClave(clave);
            if (p == null) {
                p = PerfilCultivo.todos().get(0);
                clave = p.clave;
            }
            this.perfil = p;
            this.nombrePerfil = clave;
        }
    }

    public void cambiarPerfil(PerfilCultivo nuevoPerfil) {
        synchronized (lock) {
            if (nuevoPerfil == null) return;
            this.perfil = nuevoPerfil;
            this.nombrePerfil = nuevoPerfil.clave;
            aplicarEscenario("normal");
        }
    }

    private double ruido(double s) {
        return (random.nextDouble() * 2.0 - 1.0) * s;
    }

    public void tick(double dt) {
        synchronized (lock) {
            this.reloj += dt;
            evolucionar();
            if (!modoManual) {
                automatizar(this.reloj);
            }
            temporizadores(this.reloj);
        }
    }

    public void tick() {
        tick(1.0);
    }

    public void evolucionar() {
        synchronized (lock) {
            double tempAmb = v.get("temp_ambiente");
            // Si el ventilador está prendido, baja la temperatura progresivamente (0.2 °C por segundo)
            if (Boolean.TRUE.equals(act.get("ventilador"))) {
                tempAmb -= 0.2;
            } else {
                tempAmb += (base.get("temp_ambiente") - tempAmb) * 0.1;
            }
            tempAmb += ruido(0.05);
            v.put("temp_ambiente", tempAmb);

            double hum = v.get("humedad") + (base.get("humedad") - v.get("humedad")) * 0.1 + ruido(0.2);
            v.put("humedad", Math.min(Math.max(hum, 0.0), 100.0));

            double tempAgua = v.get("temp_agua") + (base.get("temp_agua") - v.get("temp_agua")) * 0.1 + ruido(0.02);
            v.put("temp_agua", tempAgua);

            // Si el LED está encendido se asegura la iluminación
            double luxActual = Boolean.TRUE.equals(act.get("led")) ? Math.max(v.get("lux"), 4200.0) : v.get("lux");
            double lux = luxActual + (base.get("lux") - luxActual) * 0.2 + ruido(20.0);
            v.put("lux", Math.max(lux, 0.0));

            double ec = v.get("ec") + (base.get("ec") - v.get("ec")) * 0.05 + ruido(0.01);
            v.put("ec", Math.max(ec, 0.0));

            double ph = v.get("ph") + (base.get("ph") - v.get("ph")) * 0.05 + ruido(0.01);
            v.put("ph", ph);

            double consumo = 0.05 + (Boolean.TRUE.equals(act.get("bomba_riego")) ? 0.4 : 0.0);
            double nivel = v.get("nivel_agua") - consumo;
            v.put("nivel_agua", Math.min(Math.max(nivel, 0.0), 100.0));
        }
    }

    public void automatizar(double ahora) {
        synchronized (lock) {
            double tempAire = v.get("temp_ambiente");
            if (!act.get("ventilador") && tempAire > perfil.ventiladorOn()) {
                act.put("ventilador", true);
                mensajes.add("AUTO: Ventilador activado por alta temperatura");
            } else if (act.get("ventilador") && tempAire < perfil.ventiladorOff()) {
                act.put("ventilador", false);
                mensajes.add("AUTO: Ventilador apagado, temperatura normalizada");
            }

            double lux = v.get("lux");
            if (!act.get("led") && lux < LUX_LED_ON) {
                act.put("led", true);
            } else if (act.get("led") && lux > LUX_LED_OFF) {
                act.put("led", false);
            }

            double ec = v.get("ec");
            if (ec < perfil.ecMin && !act.get("nutrientes") && (ahora - tDosisNut >= esperaDosis)) {
                act.put("nutrientes", true);
                hasta.put("nutrientes", ahora + DOSIS_S);
                tDosisNut = ahora;
                v.put("ec", ec + 0.35);
                base.put("ec", ec + 0.35);
                dosis.put("nutrientes", dosis.get("nutrientes") + 1);
                mensajes.add("AUTO: EC baja -> dosis de nutrientes A+B");
            }

            double ph = v.get("ph");
            if (ph > perfil.phMax && !act.get("ph_pump") && (ahora - tDosisPh >= esperaDosis)) {
                act.put("ph_pump", true);
                hasta.put("ph_pump", ahora + DOSIS_S);
                tDosisPh = ahora;
                v.put("ph", ph - 0.4);
                base.put("ph", ph - 0.4);
                dosis.put("ph", dosis.get("ph") + 1);
                mensajes.add("AUTO: pH alto -> dosis de pH Down");
            }
        }
    }

    public boolean temporizadores(double ahora) {
        synchronized (lock) {
            boolean cambio = false;
            if (!modoManual) {
                String[] peristalticas = {"nutrientes", "ph_pump"};
                for (String k : peristalticas) {
                    if (Boolean.TRUE.equals(act.get(k)) && ahora >= hasta.get(k)) {
                        act.put(k, false);
                        cambio = true;
                    }
                }

                if (!act.get("bomba_riego") && (ahora - tRiego >= riegoCada)) {
                    if (v.get("nivel_agua") >= NIVEL_CRITICO) {
                        act.put("bomba_riego", true);
                        hasta.put("bomba_riego", ahora + riegoDura);
                        tRiego = ahora;
                        cambio = true;
                        mensajes.add("AUTO: riego programado ON");
                    } else {
                        tRiego = ahora;
                        mensajes.add("AUTO: riego omitido, nivel de agua crítico");
                    }
                }

                if (Boolean.TRUE.equals(act.get("bomba_riego")) &&
                        (ahora >= hasta.get("bomba_riego") || v.get("nivel_agua") < NIVEL_CRITICO)) {
                    act.put("bomba_riego", false);
                    cambio = true;
                }
            }

            if (Boolean.TRUE.equals(act.get("cerradura")) && ahora >= hasta.get("cerradura")) {
                act.put("cerradura", false);
                cambio = true;
            }

            return cambio;
        }
    }

    public void apagarTodo() {
        synchronized (lock) {
            for (String k : act.keySet()) {
                act.put(k, false);
            }
        }
    }

    public RespuestaComando comando(String cmd, String val, double ahora) {
        synchronized (lock) {
            cmd = cmd.trim().toLowerCase();
            val = val.trim().toLowerCase();

            if ("modo".equals(cmd)) {
                modoManual = "manual".equals(val);
                if (!modoManual) {
                    apagarTodo();
                }
                return new RespuestaComando(true, "modo " + (modoManual ? "MANUAL" : "AUTO"));
            }

            if (!modoManual) {
                return new RespuestaComando(false, "ignorado: activa el switch Modo Manual para operar actuadores");
            }

            if ("ph".equals(cmd)) {
                cmd = "ph_pump";
            }

            if (!act.containsKey(cmd)) {
                return new RespuestaComando(false, "comando desconocido");
            }

            boolean on = "on".equals(val);
            if ("bomba_riego".equals(cmd) && on && v.get("nivel_agua") < NIVEL_CRITICO) {
                return new RespuestaComando(false, "bloqueado: nivel de agua crítico (protección de la bomba)");
            }

            act.put(cmd, on);
            if ("cerradura".equals(cmd) && on) {
                hasta.put("cerradura", ahora + CERRADURA_S);
            }

            return new RespuestaComando(true, cmd + " " + (on ? "ON" : "OFF"));
        }
    }

    public RespuestaComando enviarComando(String actuador, String orden) {
        return comando(actuador, orden, this.reloj);
    }

    public void forzarParametro(String variable, double valor, boolean persistente) {
        synchronized (lock) {
            v.put(variable, valor);
            if (persistente) {
                base.put(variable, valor);
            }
        }
    }

    public void rellenarEstanque() {
        synchronized (lock) {
            v.put("nivel_agua", 100.0);
            base.put("nivel_agua", 100.0);
        }
    }

    public List<Alerta> alertas() {
        synchronized (lock) {
            List<Alerta> out = new ArrayList<>();
            double tempAmb = v.get("temp_ambiente");
            if (tempAmb > perfil.aireMax) {
                out.add(new Alerta("temp_ambiente", "alta", tempAmb, "Ventilador ON (automático)", true));
            } else if (tempAmb < perfil.aireMin) {
                out.add(new Alerta("temp_ambiente", "baja", tempAmb, "El diseño no tiene calefacción: abrigar o mover huerto", false));
            }

            double tempAgua = v.get("temp_agua");
            if (tempAgua > perfil.tempAguaMax) {
                out.add(new Alerta("temp_agua", "alta", tempAgua, "Sin enfriador: sombrear estanque o agregar agua fresca", false));
            } else if (tempAgua < perfil.tempAguaMin) {
                out.add(new Alerta("temp_agua", "baja", tempAgua, "Sin calefactor: revisar ubicación del estanque", false));
            }

            double ph = v.get("ph");
            if (ph > perfil.phMax) {
                out.add(new Alerta("ph", "alto", ph, "Bomba pH Down (automático)", true));
            } else if (ph < perfil.phMin) {
                out.add(new Alerta("ph", "bajo", ph, "Sin bomba pH Up: corregir manualmente", false));
            }

            double ec = v.get("ec");
            if (ec > perfil.ecMax) {
                out.add(new Alerta("ec", "alto", ec, "Sin dilución automática: agregar agua limpia", false));
            } else if (ec < perfil.ecMin) {
                out.add(new Alerta("ec", "bajo", ec, "Nutrientes A+B (automático)", true));
            }

            double nivel = v.get("nivel_agua");
            if (nivel < NIVEL_CRITICO) {
                out.add(new Alerta("nivel_agua", "critico", nivel, "Riego bloqueado: rellenar ya", false));
            } else if (nivel < NIVEL_BAJO) {
                out.add(new Alerta("nivel_agua", "bajo", nivel, "Rellenar estanque pronto", false));
            }

            double lux = v.get("lux");
            if (lux < LUX_LED_ON) {
                out.add(new Alerta("lux", "bajo", lux, "LED de cultivo ON (automático)", true));
            }

            return out;
        }
    }

    public void aplicarEscenario(String escenario) {
        synchronized (lock) {
            if ("normal".equals(escenario)) {
                double phIdeal = Math.round(((perfil.phMin + perfil.phMax) / 2.0) * 100.0) / 100.0;
                double ecIdeal = Math.round(((perfil.ecMin + perfil.ecMax) / 2.0) * 100.0) / 100.0;
                double tempAireIdeal = (perfil.aireMin + perfil.aireMax) / 2.0;
                double tempAguaIdeal = (perfil.tempAguaMin + perfil.tempAguaMax) / 2.0;

                base.put("temp_ambiente", tempAireIdeal);
                base.put("humedad", 62.0);
                base.put("temp_agua", tempAguaIdeal);
                base.put("lux", 4500.0);
                base.put("ph", phIdeal);
                base.put("ec", ecIdeal);
                base.put("nivel_agua", 85.0);

                v.putAll(base);
                apagarTodo();
                return;
            }

            switch (escenario) {
                case "calor":
                    v.put("temp_ambiente", 34.0);
                    base.put("temp_ambiente", 34.0);
                    break;
                case "frio":
                    v.put("temp_ambiente", 10.0);
                    base.put("temp_ambiente", 10.0);
                    break;
                case "agua_caliente":
                    v.put("temp_agua", 29.0);
                    base.put("temp_agua", 29.0);
                    break;
                case "agua_fria":
                    v.put("temp_agua", 11.0);
                    base.put("temp_agua", 11.0);
                    break;
                case "ph_alto":
                    v.put("ph", 7.6);
                    base.put("ph", 7.6);
                    break;
                case "ph_bajo":
                    v.put("ph", 4.5);
                    base.put("ph", 4.5);
                    break;
                case "ec_bajo":
                    v.put("ec", 0.5);
                    base.put("ec", 0.5);
                    break;
                case "ec_alto":
                    v.put("ec", 3.8);
                    base.put("ec", 3.8);
                    break;
                case "oscuridad":
                    v.put("lux", 800.0);
                    base.put("lux", 800.0);
                    break;
                case "nivel_bajo":
                    v.put("nivel_agua", 24.0);
                    base.put("nivel_agua", 24.0);
                    break;
                case "nivel_critico":
                    v.put("nivel_agua", 8.0);
                    base.put("nivel_agua", 8.0);
                    break;
                default:
                    break;
            }
        }
    }
}
