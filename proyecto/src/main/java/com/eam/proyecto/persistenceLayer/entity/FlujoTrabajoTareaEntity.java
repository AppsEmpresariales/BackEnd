package com.eam.proyecto.persistenceLayer.entity;

import com.eam.proyecto.persistenceLayer.entity.enums.EstadoTareaEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// FlujoTrabajoTareaEntity — sin organization_id
@Entity
@Table(name = "flujos_trabajo_tareas")
@Data @NoArgsConstructor @AllArgsConstructor
public class FlujoTrabajoTareaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    @Column(nullable = false)
    private DocumentoEntity documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flujo_trabajo_paso_id")
    @Column(nullable = false)
    private FlujoTrabajoPasoEntity paso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignado_a")
    @Column(nullable = false)
    private UsuarioEntity asignadoA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTareaEnum estado;

    private String comentario;

    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "completado_en")
    private LocalDateTime completadoEn;
    // organization_id eliminado: no existe en el DDL
}