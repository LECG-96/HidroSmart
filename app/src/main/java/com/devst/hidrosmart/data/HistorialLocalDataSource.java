package com.devst.hidrosmart.data;

import com.devst.hidrosmart.modelo.EventoAlerta;

import java.util.List;

/** Historial local de cambios de estado de las alertas. */
public interface HistorialLocalDataSource {
    void sincronizarAlertas(List<EventoAlerta> alertasActuales);
    List<EventoAlerta> obtenerEventos(int limite);
    List<EventoAlerta> obtenerAlertasActivas();
    void marcarAlertasActivasAtendidas();
}
