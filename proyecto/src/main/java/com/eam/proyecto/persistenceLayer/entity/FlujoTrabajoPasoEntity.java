package com.docucloud.persistence.entity;

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

    private String nombre;
    private String descripcion;

    @Column(name = "orden_paso")
    private Integer ordenPaso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flujo_trabajo_id")
    private FlujoTrabajoEntity flujoTrabajo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_requerido_id")
    private RolEntity rolRequerido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objetivo_estado_id") // FK real al catálogo
    private EstadoDocumentoEntity objetivoEstado;
}