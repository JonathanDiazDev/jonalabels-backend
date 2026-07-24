package com.jonalabels.pedido.repository;

import com.jonalabels.pedido.domain.Cotizacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CotizacionRepositoryTest {

    @Autowired
    private CotizacionRepository cotizacionRepository;

    @Test
    void guardarYRecuperarCotizacion() {
        var cotizacion = Cotizacion.builder()
                .nombre("María García")
                .whatsapp("5512345678")
                .email("maria@ejemplo.com")
                .cantidad(500)
                .medidas("5cm x 3cm")
                .urlArchivo("/api/v1/archivos/logo-maria.png")
                .build();

        var guardada = cotizacionRepository.save(cotizacion);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getNombre()).isEqualTo("María García");
        assertThat(guardada.getWhatsapp()).isEqualTo("5512345678");
        assertThat(guardada.getEmail()).isEqualTo("maria@ejemplo.com");
        assertThat(guardada.getCantidad()).isEqualTo(500);
        assertThat(guardada.getMedidas()).isEqualTo("5cm x 3cm");
        assertThat(guardada.getUrlArchivo()).isEqualTo("/api/v1/archivos/logo-maria.png");
        assertThat(guardada.getFechaCreacion()).isNotNull();
    }

    @Test
    void guardarCotizacionSinCamposOpcionales() {
        var cotizacion = Cotizacion.builder()
                .nombre("Carlos López")
                .whatsapp("5512345678")
                .build();

        var guardada = cotizacionRepository.save(cotizacion);

        assertThat(guardada.getId()).isNotNull();
        assertThat(guardada.getNombre()).isEqualTo("Carlos López");
        assertThat(guardada.getWhatsapp()).isEqualTo("5512345678");
        assertThat(guardada.getEmail()).isNull();
        assertThat(guardada.getCantidad()).isNull();
        assertThat(guardada.getMedidas()).isNull();
        assertThat(guardada.getUrlArchivo()).isNull();
        assertThat(guardada.getFechaCreacion()).isNotNull();
    }

    @Test
    void guardarYRecuperarPorId() {
        var cotizacion = Cotizacion.builder()
                .nombre("Ana Martínez")
                .whatsapp("5512345678")
                .email("ana@test.com")
                .cantidad(1000)
                .build();

        var guardada = cotizacionRepository.save(cotizacion);

        var recuperada = cotizacionRepository.findById(guardada.getId());

        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getNombre()).isEqualTo("Ana Martínez");
        assertThat(recuperada.get().getWhatsapp()).isEqualTo("5512345678");
        assertThat(recuperada.get().getEmail()).isEqualTo("ana@test.com");
        assertThat(recuperada.get().getCantidad()).isEqualTo(1000);
    }
}
