package com.eam.proyecto.persistenceLayer.entity;

import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import jakarta.persistence.*;
import lombok.*;

// PlantillaCorreoEntity
@Entity
@Table(name = "plantillas_correo")
@Data @NoArgsConstructor @AllArgsConstructor
public class PlantillaCorreoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;
    private String asunto;

    @Column(columnDefinition = "TEXT")
    private String cuerpo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento")
    private TipoEventoEnum tipoEvento;

    @Column(nullable = false)
    private Boolean activo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizacionEntity organizacion;
}