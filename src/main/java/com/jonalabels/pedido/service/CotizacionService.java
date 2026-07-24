package com.jonalabels.pedido.service;

import com.jonalabels.pedido.domain.Cotizacion;
import com.jonalabels.pedido.domain.EstadoCotizacion;
import com.jonalabels.pedido.dto.CotizacionRequestDTO;
import com.jonalabels.pedido.dto.CotizacionResponseDTO;
import com.jonalabels.pedido.dto.MetricasDashboardDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CotizacionService {

    Cotizacion crear(CotizacionRequestDTO request, MultipartFile archivo);

    Page<CotizacionResponseDTO> obtenerCotizacionesPaginadas(String busqueda, EstadoCotizacion estado, Pageable pageable);

    CotizacionResponseDTO actualizarEstado(Long id, EstadoCotizacion nuevoEstado);

    MetricasDashboardDTO obtenerMetricas();

    byte[] exportarCotizacionesCsv(String busqueda, EstadoCotizacion estado);
}
