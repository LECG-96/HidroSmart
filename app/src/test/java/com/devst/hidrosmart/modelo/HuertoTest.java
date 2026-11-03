package com.devst.hidrosmart.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HuertoTest {

    private Huerto nuevo(String perfil) {
        return new Huerto(perfil, 20.0, 60.0, 10.0, 1000.0, 1);
    }

    private interface Condicion {
        boolean evaluar(Huerto h);
    }

    private boolean avanzar(Huerto h, int ticks, Condicion cond) {
        for (int i = 1; i <= ticks; i++) {
            h.evolucionar();
            if (!h.modoManual) {
                h.automatizar(h.reloj);
            }
            for (int s = 0; s < 10; s++) {
                h.reloj += 1.0;
                h.temporizadores(h.reloj);
            }
            if (cond != null && cond.evaluar(h)) {
                return true;
            }
        }
        return cond != null ? cond.evaluar(h) : true;
    }

    private boolean tieneAlerta(Huerto h, String var, String nivel) {
        for (Alerta a : h.alertas()) {
            if (a.variable.equals(var) && a.nivel.equals(nivel)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testCalorVentiladorEntraYSale() {
        Huerto h = nuevo("lechuga");
        h.v.put("temp_ambiente", h.perfil.aireMax + 5.0);
        boolean on = avanzar(h, 3, x -> Boolean.TRUE.equals(x.act.get("ventilador")));
        boolean off = avanzar(h, 40, x -> !Boolean.TRUE.equals(x.act.get("ventilador")));
        assertTrue("El ventilador debe encender y luego apagar por histéresis", on && off);
    }

    @Test
    public void testFrioSinCalefaccion() {
        Huerto h = nuevo("lechuga");
        h.v.put("temp_ambiente", h.perfil.aireMin - 4.0);
        boolean alerta = tieneAlerta(h, "temp_ambiente", "baja");
        avanzar(h, 1, null);
        assertTrue(alerta);
        assertFalse(h.act.get("ventilador"));
    }

    @Test
    public void testPhAltoDosificaPhDown() {
        Huerto h = nuevo("lechuga");
        h.v.put("ph", h.perfil.phMax + 0.8);
        boolean on = avanzar(h, 3, x -> x.dosis.get("ph") > 0);
        boolean ok = avanzar(h, 20, x -> x.v.get("ph") <= x.perfil.phMax);
        assertTrue("Debe dosificar y regresar el pH al rango", on && ok);
    }

    @Test
    public void testEcBajaDosificaNutrientes() {
        Huerto h = nuevo("lechuga");
        h.v.put("ec", h.perfil.ecMin - 0.6);
        boolean on = avanzar(h, 3, x -> x.dosis.get("nutrientes") > 0);
        boolean ok = avanzar(h, 20, x -> x.v.get("ec") >= x.perfil.ecMin);
        assertTrue("Debe dosificar nutrientes y subir la conductividad", on && ok);
    }

    @Test
    public void testLuzHisteresisLed() {
        Huerto h = nuevo("lechuga");
        h.v.put("lux", 1500.0);
        h.base.put("lux", 1500.0);
        boolean on = avanzar(h, 3, x -> Boolean.TRUE.equals(x.act.get("led")));

        h.base.put("lux", 5000.0);
        boolean off = avanzar(h, 6, x -> !Boolean.TRUE.equals(x.act.get("led")));
        assertTrue("El LED debe encender con <3000 y apagar con >3500", on && off);
    }

    @Test
    public void testRiegoProgramado() {
        Huerto h = nuevo("lechuga");
        h.tRiego = h.reloj - 61.0;
        double nivel0 = h.v.get("nivel_agua");
        boolean on = avanzar(h, 2, x -> Boolean.TRUE.equals(x.act.get("bomba_riego")));
        boolean off = avanzar(h, 3, x -> !Boolean.TRUE.equals(x.act.get("bomba_riego")));
        assertTrue(on && off && h.v.get("nivel_agua") < nivel0);
    }

    @Test
    public void testNivelCriticoBloqueaRiego() {
        Huerto h = nuevo("lechuga");
        h.v.put("nivel_agua", 10.0);
        h.tRiego = h.reloj - 61.0;
        boolean alerta = tieneAlerta(h, "nivel_agua", "critico");
        avanzar(h, 2, null);
        assertTrue(alerta);
        assertFalse("El riego debe omitirse con nivel crítico", h.act.get("bomba_riego"));
    }

    @Test
    public void testComandosModoAutoVsManual() {
        Huerto h = nuevo("lechuga");
        RespuestaComando resAuto = h.comando("ventilador", "on", h.reloj);
        assertFalse(resAuto.aceptado);
        assertFalse(h.act.get("ventilador"));

        h.comando("modo", "manual", h.reloj);
        RespuestaComando resMan = h.comando("ventilador", "on", h.reloj);
        assertTrue(resMan.aceptado);
        assertTrue(h.act.get("ventilador"));

        h.comando("modo", "auto", h.reloj);
        assertFalse("Al volver a auto deben apagarse las salidas", h.act.get("ventilador"));
    }

    @Test
    public void testCerraduraTemporizada() {
        Huerto h = nuevo("lechuga");
        h.comando("modo", "manual", h.reloj);
        h.comando("cerradura", "on", h.reloj);
        assertTrue(h.act.get("cerradura"));
        avanzar(h, 1, null);
        assertFalse("La cerradura debe cerrarse sola tras 5 segundos", h.act.get("cerradura"));
    }

    @Test
    public void testMultitorreMismoPhDistintaRespuesta() {
        Huerto lech = new Huerto("lechuga", 20.0, 60.0, 10.0, 1000.0, 1);
        Huerto cil = new Huerto("cilantro", 20.0, 60.0, 10.0, 1000.0, 2);

        lech.v.put("ph", 6.2);
        lech.base.put("ph", 6.2);
        cil.v.put("ph", 6.2);
        cil.base.put("ph", 6.2);

        avanzar(lech, 2, null);
        avanzar(cil, 2, null);

        assertTrue("Lechuga (máx 6.0) debe dosificar", lech.dosis.get("ph") > 0 || lech.v.get("ph") <= 6.0);
        assertEquals("Cilantro (máx 6.5) no debe dosificar", Integer.valueOf(0), cil.dosis.get("ph"));
        assertFalse(tieneAlerta(cil, "ph", "alto"));
    }
}