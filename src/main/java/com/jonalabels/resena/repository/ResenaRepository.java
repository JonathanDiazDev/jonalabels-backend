package com.jonalabels.resena.repository;

import com.jonalabels.resena.domain.EstadoModeracion;
import com.jonalabels.resena.domain.Resena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByEstadoModeracion(EstadoModeracion estado);

    List<Resena> findByUsuarioId(Long usuarioId);
}
