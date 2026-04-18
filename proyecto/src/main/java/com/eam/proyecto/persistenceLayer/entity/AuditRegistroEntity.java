package com.eam.proyecto.persistenceLayer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "audit_registros")
@Data @NoArgsConstructor @AllArgsConstructor
public class AuditRegistroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoEntity documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private String accion;

    private String descripcion;

    @Column(name = "estado_previo")
    private String estadoPrevio;

    @Column(name = "estado_nuevo")
    private String estadoNuevo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}