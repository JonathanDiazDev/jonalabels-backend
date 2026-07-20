package com.jonalabels.resena.service;

import com.jonalabels.common.exception.RecursoNoEncontradoException;
import com.jonalabels.entity.Producto;
import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.pedido.domain.Pedido;
import com.jonalabels.pedido.repository.PedidoRepository;
import com.jonalabels.resena.domain.EstadoModeracion;
import com.jonalabels.resena.domain.Resena;
import com.jonalabels.resena.repository.ResenaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    @Mock
    private ResenaRepository resenaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private ResenaServiceImpl resenaService;

    private Usuario usuario;
    private Producto producto;
    private Pedido pedidoPagado;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .email("cliente@test.com")
                .rol("CLIENTE")
                .build();

        producto = Producto.builder()
                .id(1L)
                .nombre("Etiqueta Satin Premium")
                .tipoMaterial("Satin")
                .build();

        pedidoPagado = Pedido.builder()
                .id(1L)
                .usuario(usuario)
                .producto(producto)
                .estado("PAGADO")
                .cantidad(500)
                .build();
    }

    @Test
    void crearResena_pedidoPertenecienteAlUsuarioEnEstadoPagado_creaResena() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoPagado));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocation -> {
            Resena r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        Resena resena = resenaService.crearResena(1L, 1L, 5, "Excelente producto");

        assertThat(resena).isNotNull();
        assertThat(resena.getUsuarioId()).isEqualTo(1L);
        assertThat(resena.getPedidoId()).isEqualTo(1L);
        assertThat(resena.getProductoId()).isEqualTo(1L);
        assertThat(resena.getCalificacion()).isEqualTo(5);
        assertThat(resena.getComentario()).isEqualTo("Excelente producto");
        assertThat(resena.getEstadoModeracion()).isEqualTo(EstadoModeracion.PENDIENTE);

        verify(resenaRepository).save(any(Resena.class));
    }

    @Test
    void crearResena_pedidoNoPerteneceAlUsuario_lanzaExcepcion() {
        Pedido pedidoOtroUsuario = Pedido.builder()
                .id(2L)
                .usuario(Usuario.builder().id(99L).email("otro@test.com").build())
                .producto(producto)
                .estado("PAGADO")
                .cantidad(500)
                .build();
        when(pedidoRepository.findById(2L)).thenReturn(Optional.of(pedidoOtroUsuario));

        assertThatThrownBy(() -> resenaService.crearResena(1L, 2L, 5, "test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo compradores verificados pueden dejar una reseña");

        verify(resenaRepository, never()).save(any());
    }

    @Test
    void crearResena_pedidoNoEnEstadoPagado_lanzaExcepcion() {
        Pedido pedidoPendiente = Pedido.builder()
                .id(3L)
                .usuario(usuario)
                .producto(producto)
                .estado("ESPERANDO_FACTIBILIDAD")
                .cantidad(500)
                .build();
        when(pedidoRepository.findById(3L)).thenReturn(Optional.of(pedidoPendiente));

        assertThatThrownBy(() -> resenaService.crearResena(1L, 3L, 4, "test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo compradores verificados pueden dejar una reseña");

        verify(resenaRepository, never()).save(any());
    }

    @Test
    void crearResena_pedidoNoExiste_lanzaExcepcion() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resenaService.crearResena(1L, 99L, 5, "test"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Pedido");

        verify(resenaRepository, never()).save(any());
    }

    @Test
    void moderarResena_estadoAprobado_cambiaEstado() {
        Resena resena = Resena.builder()
                .id(1L)
                .usuarioId(1L)
                .pedidoId(1L)
                .productoId(1L)
                .calificacion(5)
                .estadoModeracion(EstadoModeracion.PENDIENTE)
                .build();
        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Resena moderada = resenaService.moderarResena(1L, EstadoModeracion.APROBADA);

        assertThat(moderada.getEstadoModeracion()).isEqualTo(EstadoModeracion.APROBADA);
        verify(resenaRepository).save(resena);
    }

    @Test
    void moderarResena_estadoRechazado_cambiaEstado() {
        Resena resena = Resena.builder()
                .id(1L)
                .usuarioId(1L)
                .pedidoId(1L)
                .productoId(1L)
                .calificacion(2)
                .estadoModeracion(EstadoModeracion.PENDIENTE)
                .build();
        when(resenaRepository.findById(1L)).thenReturn(Optional.of(resena));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Resena moderada = resenaService.moderarResena(1L, EstadoModeracion.RECHAZADA);

        assertThat(moderada.getEstadoModeracion()).isEqualTo(EstadoModeracion.RECHAZADA);
    }

    @Test
    void moderarResena_resenaNoExiste_lanzaExcepcion() {
        when(resenaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resenaService.moderarResena(99L, EstadoModeracion.APROBADA))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Resena");

        verify(resenaRepository, never()).save(any());
    }

    @Test
    void obtenerResenasAprobadas_retornaSoloAprobadas() {
        Resena aprobada1 = Resena.builder().id(1L).estadoModeracion(EstadoModeracion.APROBADA).calificacion(5).build();
        Resena aprobada2 = Resena.builder().id(2L).estadoModeracion(EstadoModeracion.APROBADA).calificacion(4).build();
        when(resenaRepository.findByEstadoModeracion(EstadoModeracion.APROBADA))
                .thenReturn(List.of(aprobada1, aprobada2));

        List<Resena> resultado = resenaService.obtenerResenasAprobadas();

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(r -> r.getEstadoModeracion() == EstadoModeracion.APROBADA);
    }
}
