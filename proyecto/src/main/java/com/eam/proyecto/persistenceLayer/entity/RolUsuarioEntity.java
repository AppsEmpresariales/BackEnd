package com.eam.proyecto.persistenceLayer.entity;

import jakarta.persistence.*;
import lombok.*;

// RolUsuarioEntity
@Entity
@Table(
        name = "roles_usuarios",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "rol_id"})
)
@Data @NoArgsConstructor @AllArgsConstructor
public class RolUsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id")
    private RolEntity rol;
}