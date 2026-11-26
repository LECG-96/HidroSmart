package com.devst.hidrosmart.data;

import com.devst.hidrosmart.modelo.MedicionMuestra;

import java.util.List;

/** Lecturas históricas agregadas localmente en intervalos para ver tendencias. */
public interface HistorialMedicionesDataSource {
    void guardarMuestras(List<MedicionMuestra> muestras);
    List<MedicionMuestra> obtenerMuestras(int torre, String variable, long desdeMillis, int limite);
}
