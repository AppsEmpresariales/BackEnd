package com.eam.proyecto.persistenceLayer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// OrganizacionEntity
@Entity
@Table(name = "organizaciones")
@Data @NoArgsConstructor @AllArgsConstructor
public class OrganizacionEntity {

    @Id
    private Long nit;

    @Column (nullable = false)
    private String nombre;
    @Column (nullable = false)
    private String email;
    private String telefono;

    @Column(name = "dir_calle")
    private String dirCalle;

    @Column(name = "dir_numero")
    private String dirNumero;

    @Column(name = "dir_comuna")
    private String dirComuna;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}