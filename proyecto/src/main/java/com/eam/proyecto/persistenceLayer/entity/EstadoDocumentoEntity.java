package com.eam.proyecto.persistenceLayer.entity;

import jakarta.persistence.*;
import lombok.*;

// EstadoDocumentoEntity — catálogo real de estados
@Entity
@Table(name = "estados_documento")
@Data @NoArgsConstructor @AllArgsConstructor
public class EstadoDocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;
    private String color;

    @Column(name = "es_inicial",  nullable = false)
    private Boolean esInicial;

    @Column(name = "es_final",   nullable = false)
    private Boolean esFinal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizacionEntity organizacion;
}