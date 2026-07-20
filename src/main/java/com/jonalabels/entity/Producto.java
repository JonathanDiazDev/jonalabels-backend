package com.jonalabels.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "tipo_material", nullable = false, length = 100)
    private String tipoMaterial;

    @Column(name = "descripcion_corta", length = 255)
    private String descripcionCorta;

    @Column(name = "descripcion_detallada")
    private String descripcionDetallada;

    @Column(name = "recurso_capa_base")
    private String recursoCapaBase;

    @Column(name = "recurso_capa_acabado")
    private String recursoCapaAcabado;

    @Column(name = "precio_base_referencia", precision = 10, scale = 2)
    private BigDecimal precioBaseReferencia;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "calificacion_promedio", precision = 3, scale = 2)
    private BigDecimal calificacionPromedio;

    @Column(name = "total_resenas")
    private Integer totalResenas;
}
