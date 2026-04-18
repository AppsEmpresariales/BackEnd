package com.eam.proyecto.persistenceLayer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// UsuarioEntity
@Entity
@Table(name = "usuarios")
@Data @NoArgsConstructor @AllArgsConstructor
public class UsuarioEntity {

    @Id
    private Long cedula;

    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash",  nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_nit")
    @Column(nullable = false)
    private OrganizacionEntity organizacion;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}