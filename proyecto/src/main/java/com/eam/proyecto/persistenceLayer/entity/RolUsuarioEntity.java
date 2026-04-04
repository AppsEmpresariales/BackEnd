package com.docucloud.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles_usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolUsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // agregado para simplicidad

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id")
    private RolEntity rol;
}