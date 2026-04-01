package com.docucloud.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tipos_documento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoDocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String descripcion;

    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizacionEntity organizacion;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;
}