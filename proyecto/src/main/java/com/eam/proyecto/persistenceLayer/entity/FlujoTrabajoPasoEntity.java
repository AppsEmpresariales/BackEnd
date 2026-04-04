package com.docucloud.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "flujos_trabajo_pasos")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "objetivo_estado_id")
    private String objetivoEstado; // del documento
}