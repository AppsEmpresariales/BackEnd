package com.docucloud.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "estado_documento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoDocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(name = "es_final")
    private Boolean esFinal;

    @Column(name = "es_inicial")
    private Boolean esInicial;

    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizacionEntity organizacion;
}