package com.jonalabels.pedido.service;

import com.jonalabels.archivo.service.StorageService;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

    @Mock
    private ObjectProvider<CloudinaryService> cloudinaryServiceProvider;

    @Mock
    private StorageService storageService;

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
                5000,
                "Etiquetas de Satín Premium",
                "5cm x 3cm");
        var archivo = new MockMultipartFile(
                "archivo", "logo.png", "image/png", "contenido".getBytes());
        var urlCloudinary = "https://res.cloudinary.com/jonalabels/logo.png";

        var cotizacionEsperada = Cotizacion.builder()
                .id(1L)
                .nombre("María García")
                .whatsapp("5512345678")
                .email("maria@ejemplo.com")
                .cantidad(5000)
                .tipoProducto("Etiquetas de Satín Premium")
                .medidas("5cm x 3cm")
                .urlDiseno(urlCloudinary)
                .build();

        when(cloudinaryServiceProvider.getIfAvailable()).thenReturn(cloudinaryService);
        when(cloudinaryService.subirArchivo(any())).thenReturn(urlCloudinary);
        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotizacionEsperada);

        var resultado = cotizacionService.crear(request, archivo);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("María García");
        assertThat(resultado.getWhatsapp()).isEqualTo("5512345678");
        assertThat(resultado.getEmail()).isEqualTo("maria@ejemplo.com");
        assertThat(resultado.getCantidad()).isEqualTo(5000);
        assertThat(resultado.getTipoProducto()).isEqualTo("Etiquetas de Satín Premium");
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
                null,
                null);
        MultipartFile archivo = null;

        var cotizacionEsperada = Cotizacion.builder()
                .id(2L)
                .nombre("Carlos López")
                .whatsapp("5512345678")
                .email(null)
                .cantidad(null)
                .tipoProducto(null)
                .medidas(null)
                .urlDiseno(null)
                .build();

        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotizacionEsperada);

        var resultado = cotizacionService.crear(request, archivo);

        assertThat(resultado.getNombre()).isEqualTo("Carlos López");
        assertThat(resultado.getWhatsapp()).isEqualTo("5512345678");
        assertThat(resultado.getEmail()).isNull();
        assertThat(resultado.getCantidad()).isNull();
        assertThat(resultado.getTipoProducto()).isNull();
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
                10000,
                "Cartón Colgante",
                null);
        var archivo = new MockMultipartFile(
                "archivo", "logo-ana.png", "image/png", "contenido".getBytes());
        var urlCloudinary = "https://res.cloudinary.com/jonalabels/logo-ana.png";

        when(cloudinaryServiceProvider.getIfAvailable()).thenReturn(cloudinaryService);
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
        assertThat(capturada.getCantidad()).isEqualTo(10000);
        assertThat(capturada.getTipoProducto()).isEqualTo("Cartón Colgante");
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

    @Test
    void crear_conEmailValido_enviaConfirmacionAlCliente() {
        var request = new CotizacionRequestDTO(
                "Laura Sánchez",
                "5598765432",
                "laura@empresa.com",
                8000,
                "Etiquetas Adheribles",
                "7cm x 4cm");

        var cotizacionGuardada = Cotizacion.builder()
                .id(4L)
                .nombre("Laura Sánchez")
                .whatsapp("5598765432")
                .email("laura@empresa.com")
                .cantidad(8000)
                .tipoProducto("Etiquetas Adheribles")
                .medidas("7cm x 4cm")
                .build();

        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotizacionGuardada);

        cotizacionService.crear(request, null);

        verify(emailService).enviarNotificacionNuevaCotizacion(cotizacionGuardada);
        verify(emailService).enviarConfirmacionCliente(
                "laura@empresa.com",
                "Laura Sánchez",
                "Etiquetas Adheribles",
                8000);
    }

    @Test
    void crear_sinEmail_noEnviaConfirmacionAlCliente() {
        var request = new CotizacionRequestDTO(
                "Roberto Díaz",
                "5511223344",
                null,
                6000,
                "Bolsa Impresa",
                "10cm x 5cm");

        var cotizacionGuardada = Cotizacion.builder()
                .id(5L)
                .nombre("Roberto Díaz")
                .whatsapp("5511223344")
                .email(null)
                .cantidad(6000)
                .tipoProducto("Bolsa Impresa")
                .medidas("10cm x 5cm")
                .build();

        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotizacionGuardada);

        cotizacionService.crear(request, null);

        verify(emailService).enviarNotificacionNuevaCotizacion(cotizacionGuardada);
        verify(emailService, never()).enviarConfirmacionCliente(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void crear_conEmailEnBlanco_noEnviaConfirmacionAlCliente() {
        var request = new CotizacionRequestDTO(
                "Pedro Gómez",
                "5566778899",
                "   ",
                12000,
                "Avíos Textiles",
                "3cm x 2cm");

        var cotizacionGuardada = Cotizacion.builder()
                .id(6L)
                .nombre("Pedro Gómez")
                .whatsapp("5566778899")
                .email("   ")
                .cantidad(12000)
                .tipoProducto("Avíos Textiles")
                .medidas("3cm x 2cm")
                .build();

        when(cotizacionRepository.save(any(Cotizacion.class))).thenReturn(cotizacionGuardada);

        cotizacionService.crear(request, null);

        verify(emailService).enviarNotificacionNuevaCotizacion(cotizacionGuardada);
        verify(emailService, never()).enviarConfirmacionCliente(anyString(), anyString(), anyString(), anyInt());
    }
}
