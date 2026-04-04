package com.docucloud.persistence.entity;

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

    private String nombre;
    private String color;

    @Column(name = "es_inicial")
    private Boolean esInicial;

    @Column(name = "es_final")
    private Boolean esFinal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizacionEntity organizacion;
}