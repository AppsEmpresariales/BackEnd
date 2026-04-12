package com.eam.proyecto.businessLayer.dto;

import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import lombok.Data;

@Data
public class PlantillaCorreoDTO {

    private Long id;
    private String nombre;
    private String asunto;
    private String cuerpo;
    private TipoEventoEnum tipoEvento;
    private Boolean activo;

    /** Denormalizado desde organizacion.nit */
    private Long organizacionNit;

    /** Denormalizado desde organizacion.nombre */
    private String organizacionNombre;
}
