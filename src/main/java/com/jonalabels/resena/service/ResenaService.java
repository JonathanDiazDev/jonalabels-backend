package com.jonalabels.resena.service;

import com.jonalabels.resena.domain.EstadoModeracion;
import com.jonalabels.resena.domain.Resena;

import java.util.List;

public interface ResenaService {

    Resena crearResena(Long usuarioId, Long pedidoId, int calificacion, String comentario);

    Resena moderarResena(Long resenaId, EstadoModeracion estado);

    List<Resena> obtenerResenasAprobadas();
}
