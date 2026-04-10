package com.eam.proyecto.businessLayer.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditRegistroDTO {

    private Long id;
    private String accion;
    private String descripcion;
    private String estadoPrevio;
    private String estadoNuevo;
    private LocalDateTime creadoEn;

    /** Denormalizado desde documento.id */
    private Long documentoId;

    /** Denormalizado desde documento.titulo */
    private String documentoTitulo;

    /** Denormalizado desde usuario.cedula */
    private Long usuarioCedula;

    /** Denormalizado desde usuario.nombre */
    private String usuarioNombre;
}

