package com.docucloud.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// AuditRegistroEntity — entidad que faltaba
@Entity
@Table(name = "audit_registros")
@Data @NoArgsConstructor @AllArgsConstructor
public class AuditRegistroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    private DocumentoEntity documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    private String accion;
    private String descripcion;

    @Column(name = "estado_previo")
    private String estadoPrevio;

    @Column(name = "estado_nuevo")
    private String estadoNuevo;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;
}