package com.jonalabels.pedido.repository;

import com.jonalabels.pedido.domain.Cotizacion;
import com.jonalabels.pedido.domain.EstadoCotizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    @Query(value = """
            SELECT c.* FROM cotizaciones c
            WHERE (:busqueda IS NULL OR :busqueda = ''
                  OR LOWER(c.nombre) LIKE '%' || LOWER(:busqueda) || '%'
                  OR c.whatsapp LIKE '%' || :busqueda || '%')
            AND (:estado IS NULL OR c.estado = :estado)
            ORDER BY c.fecha_creacion DESC
            """, countQuery = """
            SELECT COUNT(*) FROM cotizaciones c
            WHERE (:busqueda IS NULL OR :busqueda = ''
                  OR LOWER(c.nombre) LIKE '%' || LOWER(:busqueda) || '%'
                  OR c.whatsapp LIKE '%' || :busqueda || '%')
            AND (:estado IS NULL OR c.estado = :estado)
            """, nativeQuery = true)
    Page<Cotizacion> buscarConFiltros(@Param("busqueda") String busqueda,
                                      @Param("estado") EstadoCotizacion estado,
                                      Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.cantidad), 0) FROM Cotizacion c")
    Long sumTotalPiezas();

    long countByEstado(EstadoCotizacion estado);
}
