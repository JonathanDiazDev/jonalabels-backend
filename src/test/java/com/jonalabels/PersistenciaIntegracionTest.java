package com.jonalabels;

import com.jonalabels.entity.Diseno;
import com.jonalabels.entity.Producto;
import com.jonalabels.entity.Taller;
import com.jonalabels.auth.domain.Usuario;
import com.jonalabels.auth.repository.UsuarioRepository;
import com.jonalabels.pedido.domain.EstadoPedido;
import com.jonalabels.pedido.domain.Pedido;
import com.jonalabels.pedido.repository.PedidoRepository;
import com.jonalabels.repository.DisenoRepository;
import com.jonalabels.repository.ProductoRepository;
import com.jonalabels.repository.TallerRepository;
import com.jonalabels.resena.domain.EstadoModeracion;
import com.jonalabels.resena.domain.Resena;
import com.jonalabels.resena.repository.ResenaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class PersistenciaIntegracionTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TallerRepository tallerRepository;

    @Autowired
    private DisenoRepository disenoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ResenaRepository resenaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void flujoCompletoPersistenciaYRecuperacion() {
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .email("cliente@jonalabels.com")
                .passwordHash("$2a$10$abc123hash")
                .rol("CLIENTE")
                .direccionEnvio("Av. Reforma 500, CDMX")
                .build());

        Producto producto = productoRepository.save(Producto.builder()
                .nombre("Etiqueta de Satin Premium")
                .tipoMaterial("Satin")
                .descripcionCorta("Etiqueta premium de alta calidad")
                .descripcionDetallada("Etiqueta de satin con acabado premium para moda alta")
                .recursoCapaBase("/assets/labels/base-satin.svg")
                .recursoCapaAcabado("/assets/labels/acabado-premium.svg")
                .precioBaseReferencia(new BigDecimal("15.00"))
                .build());

        Taller taller = tallerRepository.save(Taller.builder()
                .nombreContacto("Juan Perez - Taller Textil MX")
                .telefono("+52 55 1234 5678")
                .costoMaquilaEstimado(new BigDecimal("8.50"))
                .build());

        Diseno diseno = disenoRepository.save(Diseno.builder()
                .usuario(usuario)
                .urlArchivoLogo("https://s3.amazonaws.com/jonalabels/logos/cliente-123.png")
                .notasCliente("Colores: negro y dorado. Fondo transparente")
                .build());

        Pedido pedido = pedidoRepository.save(Pedido.builder()
                .usuario(usuario)
                .producto(producto)
                .diseno(diseno)
                .taller(taller)
                .estado(EstadoPedido.FINALIZADO)
                .cantidad(500)
                .precioFinalCotizado(new BigDecimal("1250.00"))
                .costoTallerAcordado(new BigDecimal("750.00"))
                .comentariosAdmin("Pedido completado. Etiquetas entregadas en tiempo y forma.")
                .build());

        Resena resena = resenaRepository.save(Resena.builder()
                .usuarioId(usuario.getId())
                .pedidoId(pedido.getId())
                .productoId(producto.getId())
                .calificacion(5)
                .comentario("Excelente calidad y servicio. Recomendado.")
                .estadoModeracion(EstadoModeracion.APROBADA)
                .build());

        entityManager.flush();
        entityManager.clear();

        Pedido pedidoRecuperado = pedidoRepository.findById(pedido.getId()).orElseThrow();

        assertThat(pedidoRecuperado.getId()).isNotNull();
        assertThat(pedidoRecuperado.getEstado()).isEqualTo(EstadoPedido.FINALIZADO);
        assertThat(pedidoRecuperado.getCantidad()).isEqualTo(500);
        assertThat(pedidoRecuperado.getPrecioFinalCotizado()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(pedidoRecuperado.getCostoTallerAcordado()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(pedidoRecuperado.getComentariosAdmin()).isEqualTo("Pedido completado. Etiquetas entregadas en tiempo y forma.");
        assertThat(pedidoRecuperado.getFechaCreacion()).isNotNull();
        assertThat(pedidoRecuperado.getFechaActualizacion()).isNotNull();

        assertThat(pedidoRecuperado.getUsuario()).isNotNull();
        assertThat(pedidoRecuperado.getUsuario().getEmail()).isEqualTo("cliente@jonalabels.com");
        assertThat(pedidoRecuperado.getUsuario().getRol()).isEqualTo("CLIENTE");

        assertThat(pedidoRecuperado.getProducto()).isNotNull();
        assertThat(pedidoRecuperado.getProducto().getNombre()).isEqualTo("Etiqueta de Satin Premium");
        assertThat(pedidoRecuperado.getProducto().getTipoMaterial()).isEqualTo("Satin");
        assertThat(pedidoRecuperado.getProducto().getPrecioBaseReferencia()).isEqualByComparingTo(new BigDecimal("15.00"));

        assertThat(pedidoRecuperado.getDiseno()).isNotNull();
        assertThat(pedidoRecuperado.getDiseno().getUrlArchivoLogo()).isEqualTo("https://s3.amazonaws.com/jonalabels/logos/cliente-123.png");
        assertThat(pedidoRecuperado.getDiseno().getNotasCliente()).isEqualTo("Colores: negro y dorado. Fondo transparente");

        assertThat(pedidoRecuperado.getTaller()).isNotNull();
        assertThat(pedidoRecuperado.getTaller().getNombreContacto()).isEqualTo("Juan Perez - Taller Textil MX");
        assertThat(pedidoRecuperado.getTaller().getTelefono()).isEqualTo("+52 55 1234 5678");
        assertThat(pedidoRecuperado.getTaller().getCostoMaquilaEstimado()).isEqualByComparingTo(new BigDecimal("8.50"));

        Resena resenaRecuperada = resenaRepository.findById(resena.getId()).orElseThrow();

        assertThat(resenaRecuperada.getId()).isNotNull();
        assertThat(resenaRecuperada.getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(resenaRecuperada.getPedidoId()).isEqualTo(pedido.getId());
        assertThat(resenaRecuperada.getProductoId()).isEqualTo(producto.getId());
        assertThat(resenaRecuperada.getCalificacion()).isEqualTo(5);
        assertThat(resenaRecuperada.getComentario()).isEqualTo("Excelente calidad y servicio. Recomendado.");
        assertThat(resenaRecuperada.getEstadoModeracion()).isEqualTo(EstadoModeracion.APROBADA);
        assertThat(resenaRecuperada.getFechaCreacion()).isNotNull();
        assertThat(resenaRecuperada.getFechaActualizacion()).isNotNull();
    }
}
