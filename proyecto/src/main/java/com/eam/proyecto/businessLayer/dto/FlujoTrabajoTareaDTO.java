package com.eam.proyecto.businessLayer.dto;

import com.eam.proyecto.persistenceLayer.entity.enums.EstadoTareaEnum;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FlujoTrabajoTareaDTO {

    private Long id;
    private EstadoTareaEnum estado;
    private String comentario;
    private LocalDateTime fechaLimite;
    private LocalDateTime creadoEn;
    private LocalDateTime completadoEn;

    /** Denormalizado desde documento.id */
    private Long documentoId;

    /** Denormalizado desde documento.titulo */
    private String documentoTitulo;

    /** Denormalizado desde paso.id */
    private Long pasoId;

    /** Denormalizado desde paso.nombre */
    private String pasoNombre;

    /** Denormalizado desde asignadoA.cedula */
    private Long asignadoACedula;

    /** Denormalizado desde asignadoA.nombre */
    private String asignadoANombre;
}
