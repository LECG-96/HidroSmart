package com.devst.hidrosmart.data;

import com.devst.hidrosmart.modelo.ComentarioUsuario;

import java.util.List;

/** Fuente local independiente para observaciones del operador. */
public interface ComentarioLocalDataSource {
    void guardar(ComentarioUsuario comentario);
    List<ComentarioUsuario> obtenerTodos();
}
