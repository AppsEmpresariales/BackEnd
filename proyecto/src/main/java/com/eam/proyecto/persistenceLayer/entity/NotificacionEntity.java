package com.docucloud.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// NotificacionEntity — esta_leida corregido
@Entity
@Table(name = "notificaciones")
@Data @NoArgsConstructor @AllArgsConstructor
public class NotificacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    private DocumentoEntity documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plantilla_id")
    private PlantillaCorreoEntity plantilla;

    @Enumerated(EnumType.STRING)
    private CanalNotificacionEnum canal;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "esta_leida") // era "leido"
    private Boolean estaLeida;

    @Column(name = "enviada_a")
    private LocalDateTime enviadaA;
}