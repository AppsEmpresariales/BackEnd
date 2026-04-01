package com.docucloud.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "organizaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizacionEntity {

    @Id
    @Column(name = "nit")
    private Long nit;

    private String nombre;

    private String email;

    private String telefono;

    private String direccion;

    private Boolean active;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;
}