package com.jonalabels.pedido.service;

import com.jonalabels.cloudinary.service.CloudinaryService;
import com.jonalabels.email.service.EmailService;
import com.jonalabels.pedido.domain.Cotizacion;
import com.jonalabels.pedido.domain.EstadoCotizacion;
import com.jonalabels.pedido.dto.CotizacionRequestDTO;
import com.jonalabels.pedido.repository.CotizacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    @Mock
    private CotizacionRepository cotizacionRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private CotizacionServiceImpl cotizacionService;

    @Captor
    private ArgumentCaptor<Cotizacion> cotizacionCaptor;

    @Test
    void crear_conArchivo_subACloudinaryYGuardaUrlDiseno() {
        var request = new CotizacionRequestDTO(
                "María García",
                "5512345678",
                "maria@ejemplo.com",
                500,
                "5cm x 3cm");
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "contenido".getBytes());
        var urlCloudinary = "https://res.cloudinary.com/jonalabels/logo.png";

        var cotizacionEsperada = Cotizacion.builder()
                .id(1L)
                .nombre("María García")
                .whatsapp("5512345678")
                .email("maria@ejemplo.com")
                .cantidad(500)
                .medidas("5cm x 3cm")
                .urlDiseno(urlCloudinary)
                .build();

        when(cloudinaryService.subirArchivo(any())).thenReturn(urlCloudinary);
        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotizacionEsperada);

        var resultado = cotizacionService.crear(request, archivo);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("María García");
        assertThat(resultado.getWhatsapp()).isEqualTo("5512345678");
        assertThat(resultado.getEmail()).isEqualTo("maria@ejemplo.com");
        assertThat(resultado.getCantidad()).isEqualTo(500);
        assertThat(resultado.getMedidas()).isEqualTo("5cm x 3cm");
        assertThat(resultado.getUrlDiseno()).isEqualTo(urlCloudinary);

        verify(cloudinaryService).subirArchivo(any());
        verify(cotizacionRepository).save(any(Cotizacion.class));
        verify(emailService).enviarNotificacionNuevaCotizacion(cotizacionEsperada);
    }

    @Test
    void crear_sinArchivo_guardaSinUrlDiseno() {
        var request = new CotizacionRequestDTO(
                "Carlos López",
                "5512345678",
                null,
                null,
                null);
        MultipartFile archivo = null;

        var cotizacionEsperada = Cotizacion.builder()
                .id(2L)
                .nombre("Carlos López")
                .whatsapp("5512345678")
                .email(null)
                .cantidad(null)
                .medidas(null)
                .urlDiseno(null)
                .build();

        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotizacionEsperada);

        var resultado = cotizacionService.crear(request, archivo);

        assertThat(resultado.getNombre()).isEqualTo("Carlos López");
        assertThat(resultado.getWhatsapp()).isEqualTo("5512345678");
        assertThat(resultado.getEmail()).isNull();
        assertThat(resultado.getCantidad()).isNull();
        assertThat(resultado.getMedidas()).isNull();
        assertThat(resultado.getUrlDiseno()).isNull();
        verify(cotizacionRepository).save(any(Cotizacion.class));
        verify(emailService).enviarNotificacionNuevaCotizacion(cotizacionEsperada);
    }

    @Test
    void crear_mapeaCorrectamenteElRequestADominio() {
        var request = new CotizacionRequestDTO(
                "Ana Martínez",
                "5512345678",
                "ana@test.com",
                1000,
                null);
        var archivo = new MockMultipartFile(
                "archivo", "logo-ana.png", "image/png", "contenido".getBytes());
        var urlCloudinary = "https://res.cloudinary.com/jonalabels/logo-ana.png";

        when(cloudinaryService.subirArchivo(any())).thenReturn(urlCloudinary);
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(invocation -> {
            Cotizacion c = invocation.getArgument(0);
            c.setId(3L);
            return c;
        });

        cotizacionService.crear(request, archivo);

        verify(cotizacionRepository).save(cotizacionCaptor.capture());
        var capturada = cotizacionCaptor.getValue();

        assertThat(capturada.getNombre()).isEqualTo("Ana Martínez");
        assertThat(capturada.getWhatsapp()).isEqualTo("5512345678");
        assertThat(capturada.getEmail()).isEqualTo("ana@test.com");
        assertThat(capturada.getCantidad()).isEqualTo(1000);
        assertThat(capturada.getMedidas()).isNull();
        assertThat(capturada.getUrlDiseno()).isEqualTo(urlCloudinary);

        verify(cloudinaryService).subirArchivo(any());
        verify(emailService).enviarNotificacionNuevaCotizacion(capturada);
    }

    @Test
    void actualizarEstado_cambiaEstadoCorrectamente() {
        var cotizacionExistente = Cotizacion.builder()
                .id(1L)
                .nombre("María García")
                .whatsapp("5512345678")
                .email("maria@ejemplo.com")
                .cantidad(5000)
                .medidas("5cm x 3cm")
                .estado(EstadoCotizacion.NUEVO)
                .build();

        when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cotizacionExistente));
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = cotizacionService.actualizarEstado(1L, EstadoCotizacion.COTIZADO);

        assertThat(resultado.estado()).isEqualTo(EstadoCotizacion.COTIZADO);
        verify(cotizacionRepository).findById(1L);
        verify(cotizacionRepository).save(cotizacionExistente);
    }
}
