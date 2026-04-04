package com.docucloud.persistence.entity;

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

    private String nombre;
    private String email;
    private String telefono;

    @Column(name = "dir_calle")
    private String dirCalle;

    @Column(name = "dir_numero")
    private String dirNumero;

    @Column(name = "dir_comuna")
    private String dirComuna;

    private Boolean active;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;
}