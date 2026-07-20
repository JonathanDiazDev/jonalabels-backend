package com.jonalabels.pedido.service;

import com.jonalabels.common.exception.RecursoNoEncontradoException;
import com.jonalabels.entity.Diseno;
import com.jonalabels.entity.Producto;
import com.jonalabels.entity.Taller;
import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.auth.repository.UsuarioRepository;
import com.jonalabels.repository.DisenoRepository;
import com.jonalabels.repository.ProductoRepository;
import com.jonalabels.repository.TallerRepository;
import com.jonalabels.pedido.domain.IllegalPedidoStateException;
import com.jonalabels.pedido.domain.Pedido;
import com.jonalabels.pedido.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private DisenoRepository disenoRepository;

    @Mock
    private TallerRepository tallerRepository;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    private Usuario usuario;
    private Producto producto;
    private Diseno diseno;
    private Taller taller;
    private Pedido pedidoEsperandoFactibilidad;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .email("cliente@test.com")
                .passwordHash("hash")
                .rol("CLIENTE")
                .build();

        producto = Producto.builder()
                .id(1L)
                .nombre("Etiqueta Satin Premium")
                .tipoMaterial("Satin")
                .build();

        diseno = Diseno.builder()
                .id(1L)
                .usuario(usuario)
                .urlArchivoLogo("https://s3.amazonaws.com/logos/test.png")
                .build();

        taller = Taller.builder()
                .id(1L)
                .nombreContacto("Juan Perez")
                .telefono("+52 55 1234 5678")
                .costoMaquilaEstimado(new BigDecimal("8.50"))
                .build();

        pedidoEsperandoFactibilidad = Pedido.builder()
                .id(1L)
                .usuario(usuario)
                .producto(producto)
                .diseno(diseno)
                .cantidad(500)
                .estado("ESPERANDO_FACTIBILIDAD")
                .build();
    }

    @Test
    void crearSolicitud_creaPedidoEnEstadoEsperandoFactibilidad() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(disenoRepository.findById(1L)).thenReturn(Optional.of(diseno));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        Pedido resultado = pedidoService.crearSolicitud(1L, 1L, 1L, 500, null);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo("ESPERANDO_FACTIBILIDAD");
        assertThat(resultado.getUsuario()).isEqualTo(usuario);
        assertThat(resultado.getProducto()).isEqualTo(producto);
        assertThat(resultado.getDiseno()).isEqualTo(diseno);
        assertThat(resultado.getCantidad()).isEqualTo(500);
        assertThat(resultado.getUrlDiseno()).isNull();
        assertThat(resultado.getTaller()).isNull();
        assertThat(resultado.getPrecioFinalCotizado()).isNull();
        assertThat(resultado.getCostoTallerAcordado()).isNull();

        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    void crearSolicitud_lanzaExcepcionSiUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.crearSolicitud(99L, 1L, 1L, 500, null))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Usuario")
                .hasMessageContaining("99");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crearSolicitud_lanzaExcepcionSiProductoNoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.crearSolicitud(1L, 99L, 1L, 500, null))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Producto");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crearSolicitud_lanzaExcepcionSiDisenoNoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(disenoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.crearSolicitud(1L, 1L, 99L, 500, null))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Diseno");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cotizarPedido_transicionaAEstadoCotizado() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEsperandoFactibilidad));
        when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido resultado = pedidoService.cotizarPedido(
                1L, 1L, new BigDecimal("750.00"), new BigDecimal("1250.00"), "Pedido factible");

        assertThat(resultado.getEstado()).isEqualTo("COTIZADO");
        assertThat(resultado.getTaller()).isEqualTo(taller);
        assertThat(resultado.getCostoTallerAcordado()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(resultado.getPrecioFinalCotizado()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(resultado.getComentariosAdmin()).isEqualTo("Pedido factible");

        verify(pedidoRepository).save(pedidoEsperandoFactibilidad);
    }

    @Test
    void cotizarPedido_lanzaExcepcionSiEstadoNoEsEsperandoFactibilidad() {
        pedidoEsperandoFactibilidad.setEstado("COTIZADO");
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEsperandoFactibilidad));

        assertThatThrownBy(() -> pedidoService.cotizarPedido(
                1L, 1L, new BigDecimal("750.00"), new BigDecimal("1250.00"), "comentario"))
                .isInstanceOf(IllegalPedidoStateException.class)
                .hasMessageContaining("1")
                .hasMessageContaining("COTIZADO")
                .hasMessageContaining("ESPERANDO_FACTIBILIDAD");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cotizarPedido_lanzaExcepcionSiPedidoNoExiste() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.cotizarPedido(
                99L, 1L, new BigDecimal("750.00"), new BigDecimal("1250.00"), "comentario"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Pedido");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cotizarPedido_lanzaExcepcionSiTallerNoExiste() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEsperandoFactibilidad));
        when(tallerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.cotizarPedido(
                1L, 99L, new BigDecimal("750.00"), new BigDecimal("1250.00"), "comentario"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Taller");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void registrarPago_transicionaAEstadoPagado() {
        Pedido pedidoCotizado = Pedido.builder()
                .id(1L)
                .usuario(usuario)
                .producto(producto)
                .diseno(diseno)
                .taller(taller)
                .cantidad(500)
                .estado("COTIZADO")
                .precioFinalCotizado(new BigDecimal("1250.00"))
                .costoTallerAcordado(new BigDecimal("750.00"))
                .build();

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoCotizado));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido resultado = pedidoService.registrarPago(1L);

        assertThat(resultado.getEstado()).isEqualTo("PAGADO");

        verify(pedidoRepository).save(pedidoCotizado);
    }

    @Test
    void registrarPago_lanzaExcepcionSiEstadoNoEsCotizado() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEsperandoFactibilidad));

        assertThatThrownBy(() -> pedidoService.registrarPago(1L))
                .isInstanceOf(IllegalPedidoStateException.class)
                .hasMessageContaining("1")
                .hasMessageContaining("ESPERANDO_FACTIBILIDAD")
                .hasMessageContaining("COTIZADO");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void registrarPago_lanzaExcepcionSiPedidoNoExiste() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.registrarPago(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Pedido");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void registrarPago_lanzaExcepcionSiEstadoEsFinalizado() {
        Pedido pedidoFinalizado = Pedido.builder()
                .id(1L)
                .usuario(usuario)
                .producto(producto)
                .diseno(diseno)
                .taller(taller)
                .cantidad(500)
                .estado("FINALIZADO")
                .build();

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoFinalizado));

        assertThatThrownBy(() -> pedidoService.registrarPago(1L))
                .isInstanceOf(IllegalPedidoStateException.class)
                .hasMessageContaining("FINALIZADO");

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void crearSolicitud_conUrlDiseno_guardaUrl() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(disenoRepository.findById(1L)).thenReturn(Optional.of(diseno));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        Pedido resultado = pedidoService.crearSolicitud(1L, 1L, 1L, 500, "https://s3.amazonaws.com/logos/mi-diseno.png");

        assertThat(resultado.getUrlDiseno()).isEqualTo("https://s3.amazonaws.com/logos/mi-diseno.png");
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    void flujoCompleto_crear_cotizar_pagar() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(disenoRepository.findById(1L)).thenReturn(Optional.of(diseno));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(1L);
            }
            return p;
        });

        Pedido creado = pedidoService.crearSolicitud(1L, 1L, 1L, 500, null);
        assertThat(creado.getEstado()).isEqualTo("ESPERANDO_FACTIBILIDAD");

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(creado));
        when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));

        Pedido cotizado = pedidoService.cotizarPedido(
                1L, 1L, new BigDecimal("750.00"), new BigDecimal("1250.00"), "Factible");
        assertThat(cotizado.getEstado()).isEqualTo("COTIZADO");

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(cotizado));

        Pedido pagado = pedidoService.registrarPago(1L);
        assertThat(pagado.getEstado()).isEqualTo("PAGADO");
    }
}
