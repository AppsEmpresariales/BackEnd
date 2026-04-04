package com.docucloud.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plantillas_correo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaCorreoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String asunto;

    @Column(columnDefinition = "TEXT")
    private String cuerpo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento")
    private TipoEventoEnum tipoEvento;

    private Boolean activo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizacionEntity organizacion;
}