package com.jonalabels.pedido.service;

import com.jonalabels.cloudinary.service.CloudinaryService;
import com.jonalabels.email.service.EmailService;
import com.jonalabels.pedido.domain.Cotizacion;
import com.jonalabels.pedido.domain.EstadoCotizacion;
import com.jonalabels.pedido.dto.CotizacionRequestDTO;
import com.jonalabels.pedido.dto.CotizacionResponseDTO;
import com.jonalabels.pedido.dto.MetricasDashboardDTO;
import com.jonalabels.pedido.repository.CotizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional
public class CotizacionServiceImpl implements CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;

    @Override
    public Cotizacion crear(CotizacionRequestDTO request, MultipartFile archivo) {
        String urlDiseno = null;
        if (archivo != null && !archivo.isEmpty()) {
            urlDiseno = cloudinaryService.subirArchivo(archivo);
        }
        var cotizacion = Cotizacion.builder()
                .nombre(request.nombre())
                .whatsapp(request.whatsapp())
                .email(request.email())
                .cantidad(request.cantidad())
                .medidas(request.medidas())
                .urlDiseno(urlDiseno)
                .build();
        var guardada = cotizacionRepository.save(cotizacion);
        emailService.enviarNotificacionNuevaCotizacion(guardada);
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CotizacionResponseDTO> obtenerCotizacionesPaginadas(String busqueda, EstadoCotizacion estado, Pageable pageable) {
        return cotizacionRepository.buscarConFiltros(busqueda, estado, pageable)
                .map(this::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public MetricasDashboardDTO obtenerMetricas() {
        return new MetricasDashboardDTO(
                cotizacionRepository.count(),
                cotizacionRepository.sumTotalPiezas(),
                cotizacionRepository.countByEstado(EstadoCotizacion.NUEVO));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarCotizacionesCsv(String busqueda, EstadoCotizacion estado) {
        var cotizaciones = cotizacionRepository.buscarConFiltros(busqueda, estado, Pageable.unpaged());

        var sb = new StringBuilder();
        sb.append("ID,Fecha,Nombre,WhatsApp,Email,Cantidad,Medidas,Estado\n");
        var fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (var c : cotizaciones) {
            sb.append(c.getId()).append(",");
            sb.append(c.getFechaCreacion() != null ? c.getFechaCreacion().format(fmt) : "").append(",");
            sb.append(escaparCsv(c.getNombre())).append(",");
            sb.append(escaparCsv(c.getWhatsapp())).append(",");
            sb.append(escaparCsv(c.getEmail())).append(",");
            sb.append(c.getCantidad() != null ? c.getCantidad() : "").append(",");
            sb.append(escaparCsv(c.getMedidas())).append(",");
            sb.append(c.getEstado()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escaparCsv(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    @Override
    public CotizacionResponseDTO actualizarEstado(Long id, EstadoCotizacion nuevoEstado) {
        var cotizacion = cotizacionRepository.findById(id)
                .orElseThrow(() -> new com.jonalabels.common.exception.RecursoNoEncontradoException(
                        "Cotización con id " + id + " no encontrada"));
        cotizacion.setEstado(nuevoEstado);
        return toResponseDTO(cotizacionRepository.save(cotizacion));
    }

    private CotizacionResponseDTO toResponseDTO(Cotizacion c) {
        return new CotizacionResponseDTO(
                c.getId(),
                c.getNombre(),
                c.getWhatsapp(),
                c.getEmail(),
                c.getCantidad(),
                c.getMedidas(),
                c.getFechaCreacion(),
                c.getEstado(),
                c.getUrlDiseno());
    }
}
