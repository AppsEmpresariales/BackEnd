package com.eam.proyecto.persistenceLayer.entity;

import jakarta.persistence.*;
import lombok.*;

// FlujoTrabajoPasoEntity — objetivoEstado como FK
@Entity
@Table(name = "flujos_trabajo_pasos")
@Data @NoArgsConstructor @AllArgsConstructor
public class FlujoTrabajoPasoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;
    private String descripcion;

    @Column(name = "orden_paso", nullable = false)
    private Integer ordenPaso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flujo_trabajo_id")
    @Column(nullable = false)
    private FlujoTrabajoEntity flujoTrabajo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_requerido_id")
    @Column(nullable = false)
    private RolEntity rolRequerido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objetivo_estado_id") // FK real al catálogo
    @Column(nullable = false)
    private EstadoDocumentoEntity objetivoEstado;
}