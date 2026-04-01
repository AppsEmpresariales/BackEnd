package com.docucloud.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "flujos_trabajo_tareas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlujoTrabajoTareaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    private DocumentoEntity documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flujo_trabajo_paso_id")
    private FlujoTrabajoPasoEntity paso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_a")
    private UsuarioEntity asignadoA;

    @Enumerated(EnumType.STRING)
    private EstadoTareaEnum estado;

    private String comentario;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "completado_en")
    private LocalDateTime completadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizacionEntity organizacion;

    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;
}