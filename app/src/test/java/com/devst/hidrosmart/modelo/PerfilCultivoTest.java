package com.devst.hidrosmart.modelo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PerfilCultivoTest {

    private static final double DELTA = 0.001;

    @Test
    public void hay_siete_perfiles() {
        assertEquals(7, PerfilCultivo.todos().size());
    }

    @Test
    public void lechuga_tiene_sus_rangos() {
        PerfilCultivo p = PerfilCultivo.porClave("lechuga");
        assertNotNull(p);
        assertEquals("Lechuga", p.nombre);
        assertEquals(5.5, p.phMin, DELTA);
        assertEquals(6.0, p.phMax, DELTA);
        assertEquals(1.2, p.ecMin, DELTA);
        assertEquals(1.8, p.ecMax, DELTA);
        assertEquals(22.0, p.tempAguaMax, DELTA);
        assertEquals(24.0, p.aireMax, DELTA);
    }

    @Test
    public void una_clave_desconocida_devuelve_null() {
        assertNull(PerfilCultivo.porClave("papa"));
    }

    @Test
    public void todos_los_rangos_son_coherentes() {
        for (PerfilCultivo p : PerfilCultivo.todos()) {
            assertTrue(p.nombre, p.phMin < p.phMax);
            assertTrue(p.nombre, p.ecMin < p.ecMax);
            assertTrue(p.nombre, p.tempAguaMin < p.tempAguaMax);
            assertTrue(p.nombre, p.aireMin < p.aireMax);
        }
    }

    @Test
    public void el_ventilador_apaga_2_grados_bajo_el_umbral() {
        PerfilCultivo p = PerfilCultivo.porClave("lechuga");
        assertEquals(24.0, p.ventiladorOn(), DELTA);
        assertEquals(22.0, p.ventiladorOff(), DELTA);
    }

    @Test
    public void el_tomate_exige_mas_ec_que_la_lechuga() {
        assertTrue(PerfilCultivo.porClave("tomate_cherry").ecMin
                > PerfilCultivo.porClave("lechuga").ecMin);
    }
}