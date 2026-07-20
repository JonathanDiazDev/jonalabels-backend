package com.jonalabels.pedido.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.jonalabels.entity.Diseno;
import com.jonalabels.entity.Producto;
import com.jonalabels.entity.Taller;
import com.jonalabels.auth.domain.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diseno_id", nullable = false)
    private Diseno diseno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id")
    private Taller taller;

    @Column(nullable = false, length = 50)
    private String estado;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_final_cotizado", precision = 10, scale = 2)
    private BigDecimal precioFinalCotizado;

    @Column(name = "costo_taller_acordado", precision = 10, scale = 2)
    private BigDecimal costoTallerAcordado;

    @Column(name = "comentarios_admin")
    private String comentariosAdmin;

    @Column(name = "url_diseno", length = 500)
    private String urlDiseno;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
