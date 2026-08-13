package com.jonalabels.pedido.service;

import com.jonalabels.archivo.service.StorageService;
import com.jonalabels.cloudinary.service.CloudinaryService;
import com.jonalabels.email.service.EmailService;
import com.jonalabels.pedido.domain.Cotizacion;
import com.jonalabels.pedido.domain.EstadoCotizacion;
import com.jonalabels.pedido.dto.CotizacionRequestDTO;
import com.jonalabels.pedido.dto.CotizacionResponseDTO;
import com.jonalabels.pedido.dto.MetricasDashboardDTO;
import com.jonalabels.pedido.repository.CotizacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class CotizacionServiceImpl implements CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final EmailService emailService;
    private final ObjectProvider<CloudinaryService> cloudinaryServiceProvider;
    private final StorageService storageService;

    @Override
    public Cotizacion crear(CotizacionRequestDTO request, MultipartFile archivo) {
        String urlDiseno = null;
        if (archivo != null && !archivo.isEmpty()) {
            urlDiseno = subirDiseno(archivo);
        }
        var cotizacion = Cotizacion.builder()
                .nombre(request.nombre())
                .whatsapp(request.whatsapp())
                .email(request.email())
                .cantidad(request.cantidad())
                .tipoProducto(request.tipoProducto())
                .medidas(request.medidas())
                .urlDiseno(urlDiseno)
                .build();
        var guardada = cotizacionRepository.save(cotizacion);
        emailService.enviarNotificacionNuevaCotizacion(guardada);
        if (guardada.getEmail() != null && !guardada.getEmail().isBlank()) {
            emailService.enviarConfirmacionCliente(
                    guardada.getEmail(),
                    guardada.getNombre(),
                    guardada.getTipoProducto(),
                    guardada.getCantidad());
        }
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
        var sb = new StringBuilder();
        sb.append("ID,Fecha,Nombre,WhatsApp,Email,Cantidad,Tipo Producto,Medidas,Estado\n");
        var fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (Stream<Cotizacion> stream = cotizacionRepository.streamConFiltros(busqueda, estado)) {
            stream.forEach(c -> {
                sb.append(c.getId()).append(",");
                sb.append(c.getFechaCreacion() != null ? c.getFechaCreacion().format(fmt) : "").append(",");
                sb.append(celdaCsv(c.getNombre())).append(",");
                sb.append(celdaCsv(c.getWhatsapp())).append(",");
                sb.append(celdaCsv(c.getEmail())).append(",");
                sb.append(c.getCantidad() != null ? c.getCantidad() : "").append(",");
                sb.append(celdaCsv(c.getTipoProducto())).append(",");
                sb.append(celdaCsv(c.getMedidas())).append(",");
                sb.append(c.getEstado()).append("\n");
            });
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String celdaCsv(String valor) {
        if (valor == null) return "";
        String saneado = sanitizarFormula(valor);
        if (saneado.contains(",") || saneado.contains("\"") || saneado.contains("\n")) {
            return "\"" + saneado.replace("\"", "\"\"") + "\"";
        }
        return saneado;
    }

    private static String sanitizarFormula(String valor) {
        if (valor.isEmpty()) return valor;
        char primero = valor.charAt(0);
        if (primero == '=' || primero == '+' || primero == '-' || primero == '@') {
            return "'" + valor;
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
                c.getTipoProducto(),
                c.getMedidas(),
                c.getFechaCreacion(),
                c.getEstado(),
                c.getUrlDiseno());
    }

    private String subirDiseno(MultipartFile archivo) {
        CloudinaryService cloudinaryService = cloudinaryServiceProvider.getIfAvailable();
        if (cloudinaryService != null) {
            return cloudinaryService.subirArchivo(archivo);
        }
        String nombreGuardado = storageService.guardar(archivo);
        return "/api/v1/archivos/" + nombreGuardado;
    }
}
