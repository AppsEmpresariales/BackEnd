package com.eam.proyecto.persistenceLayer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// TipoDocumentoEntity
@Entity
@Table(name = "tipos_documento")
@Data @NoArgsConstructor @AllArgsConstructor
public class TipoDocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;
    private String descripcion;
    @Column(nullable = false)
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @Column(nullable = false)
    private OrganizacionEntity organizacion;

    @Column(name = "creado_en",  nullable = false)
    private LocalDateTime creadoEn;
}