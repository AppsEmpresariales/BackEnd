package com.eam.proyecto.persistenceLayer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "flujos_trabajo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlujoTrabajoEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_tipo_id")
    @Column(nullable = false)
    private TipoDocumentoEntity tipoDocumento;
}