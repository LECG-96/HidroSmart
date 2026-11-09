package com.devst.hidrosmart.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rangos ideales de un cultivo. Los valores vienen del documento
 * "Parametros_Cultivos_Hidroponia" (cada rango tiene su fuente allí).
 */
public final class PerfilCultivo {

    public final String clave;
    public final String nombre;
    public final double phMin, phMax;
    public final double ecMin, ecMax;               // mS/cm
    public final double tempAguaMin, tempAguaMax;   // °C
    public final double aireMin, aireMax;           // °C. aireMax = umbral en que entra el ventilador

    private PerfilCultivo(String clave, String nombre,
                          double phMin, double phMax,
                          double ecMin, double ecMax,
                          double tempAguaMin, double tempAguaMax,
                          double aireMin, double aireMax) {
        this.clave = clave;
        this.nombre = nombre;
        this.phMin = phMin;
        this.phMax = phMax;
        this.ecMin = ecMin;
        this.ecMax = ecMax;
        this.tempAguaMin = tempAguaMin;
        this.tempAguaMax = tempAguaMax;
        this.aireMin = aireMin;
        this.aireMax = aireMax;
    }

    private static final Map<String, PerfilCultivo> PERFILES = new LinkedHashMap<>();

    static {
        //             clave            nombre                          pH         EC         agua      aire
        agregar(new PerfilCultivo("guia", "Genérico (valores de la Guía)", 5.5, 6.5, 1.0, 2.5, 18, 26, 15, 28));
        agregar(new PerfilCultivo("lechuga", "Lechuga",                    5.5, 6.0, 1.2, 1.8, 18, 22, 15, 24));
        agregar(new PerfilCultivo("albahaca", "Albahaca",                  5.8, 6.2, 1.2, 2.2, 18, 22, 15, 28));
        agregar(new PerfilCultivo("menta", "Menta",                        5.5, 6.5, 1.2, 2.0, 18, 26, 15, 28));
        agregar(new PerfilCultivo("cilantro", "Cilantro",                  6.0, 6.5, 1.2, 2.0, 18, 24, 15, 28));
        agregar(new PerfilCultivo("acelga", "Acelga",                      5.5, 6.5, 1.5, 2.5, 18, 26, 15, 28));
        agregar(new PerfilCultivo("tomate_cherry", "Tomate cherry",        5.5, 6.5, 2.0, 3.5, 18, 26, 15, 25));
    }

    private static void agregar(PerfilCultivo p) {
        PERFILES.put(p.clave, p);
    }

    /** Todos los perfiles, en orden. */
    public static List<PerfilCultivo> todos() {
        return Collections.unmodifiableList(new ArrayList<>(PERFILES.values()));
    }

    /** Devuelve el perfil o null si la clave no existe. */
    public static PerfilCultivo porClave(String clave) {
        return PERFILES.get(clave);
    }

    /** El ventilador entra sobre este valor. */
    public double ventiladorOn() {
        return aireMax;
    }

    /** El ventilador se apaga 2 °C más abajo (histéresis). */
    public double ventiladorOff() {
        return aireMax - 2;
    }

    /** Crea una copia del perfil con rangos ajustados para una torre concreta. */
    public PerfilCultivo conRangos(double nuevoPhMin, double nuevoPhMax,
                                   double nuevoEcMin, double nuevoEcMax,
                                   double nuevaTempAguaMin, double nuevaTempAguaMax,
                                   double nuevoAireMin, double nuevoAireMax) {
        return new PerfilCultivo(clave, nombre, nuevoPhMin, nuevoPhMax,
                nuevoEcMin, nuevoEcMax, nuevaTempAguaMin, nuevaTempAguaMax,
                nuevoAireMin, nuevoAireMax);
    }

    /** Así se verá el perfil en un Spinner. */
    @Override
    public String toString() {
        return nombre;
    }
}
