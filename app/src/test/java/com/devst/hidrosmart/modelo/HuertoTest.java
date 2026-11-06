package com.devst.hidrosmart.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class HuertoTest {

    private Huerto huerto;
    private PerfilCultivo lechuga;

    @Before
    public void setUp() {
        List<PerfilCultivo> perfiles = PerfilCultivo.todos();
        lechuga = perfiles.get(0);
        huerto = new Huerto(1, lechuga);
    }

    @Test
    public void testInicializacionCorrecta() {
        assertNotNull(huerto.v);
        assertNotNull(huerto.act);
        assertEquals(1, huerto.idx);
        assertEquals("lechuga", huerto.perfil.clave);
        assertFalse(huerto.modoManual);
    }

    @Test
    public void testEscenarioNormalRestauraActuadoresYRangos() {
        // 1. Simular desbalance crítico: ola de calor y activación de actuador manual
        huerto.aplicarEscenario("calor");
        huerto.modoManual = true;
        huerto.act.put("ventilador", true);

        assertTrue("La temperatura de aire debe superar el máximo tras forzar calor",
                huerto.v.get("temp_ambiente") > lechuga.aireMax);

        // 2. Aplicar restablecimiento al escenario normal
        huerto.aplicarEscenario("normal");

        // 3. Validar restauración completa
        assertFalse("El modo manual debe apagarse al normalizar", huerto.modoManual);

        double tempOptimaEsperada = (lechuga.aireMin + lechuga.aireMax) / 2.0;
        assertEquals("La temperatura ambiente debe restablecerse al punto medio óptimo",
                tempOptimaEsperada, huerto.v.get("temp_ambiente"), 0.1);

        double aguaOptimaEsperada = (lechuga.tempAguaMin + lechuga.tempAguaMax) / 2.0;
        assertEquals("La temperatura de agua debe restablecerse al punto medio",
                aguaOptimaEsperada, huerto.v.get("temp_agua"), 0.1);

        double phOptimoEsperado = (lechuga.phMin + lechuga.phMax) / 2.0;
        assertEquals("El pH debe volver al punto neutro óptimo",
                phOptimoEsperado, huerto.v.get("ph"), 0.05);

        double ecOptimaEsperada = (lechuga.ecMin + lechuga.ecMax) / 2.0;
        assertEquals("La conductividad debe centrarse",
                ecOptimaEsperada, huerto.v.get("ec"), 0.05);

        assertEquals("El nivel de estanque debe volver al 85%",
                85.0, huerto.v.get("nivel_agua"), 0.1);

        // Validar que los actuadores queden apagados
        assertFalse("El ventilador debe quedar apagado", huerto.act.get("ventilador"));
        assertFalse("La bomba de riego debe quedar apagada", huerto.act.get("bomba_riego"));
        assertFalse("Los nutrientes deben quedar apagados", huerto.act.get("nutrientes"));
        assertFalse("La bomba de pH debe quedar apagada", huerto.act.get("ph_pump"));
    }

    @Test
    public void testCambioDePerfilCultivo() {
        List<PerfilCultivo> perfiles = PerfilCultivo.todos();
        PerfilCultivo menta = perfiles.get(3);

        huerto.cambiarPerfil(menta);
        assertEquals("menta", huerto.perfil.clave);

        double phMedioMenta = (menta.phMin + menta.phMax) / 2.0;
        assertEquals(phMedioMenta, huerto.v.get("ph"), 0.05);
    }
}