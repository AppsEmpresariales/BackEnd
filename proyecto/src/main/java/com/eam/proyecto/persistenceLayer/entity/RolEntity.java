package com.eam.proyecto.persistenceLayer.entity;

import jakarta.persistence.*;
import lombok.*;

// RolEntity
@Entity
@Table(name = "roles")
@Data @NoArgsConstructor @AllArgsConstructor
public class RolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;
    private String descripcion;
}