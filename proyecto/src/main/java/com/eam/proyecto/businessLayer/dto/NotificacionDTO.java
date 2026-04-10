package com.eam.proyecto.businessLayer.dto;

import com.eam.proyecto.persistenceLayer.entity.enums.CanalNotificacionEnum;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificacionDTO {

    private Long id;
    private CanalNotificacionEnum canal;
    private String mensaje;
    private Boolean estaLeida;
    private LocalDateTime enviadaA;

    /** Denormalizado desde usuario.cedula */
    private Long usuarioCedula;

    /** Denormalizado desde usuario.nombre */
    private String usuarioNombre;

    /** Denormalizado desde documento.id */
    private Long documentoId;

    /** Denormalizado desde documento.titulo */
    private String documentoTitulo;

    /** Denormalizado desde plantilla.id */
    private Long plantillaId;

    /** Denormalizado desde plantilla.nombre */
    private String plantillaNombre;
}
