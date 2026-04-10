package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

/** US-017 / US-027 */
@Data
public class DocumentoCreateDTO {

    @NotBlank(message = "El título del documento es obligatorio")
    @Size(max = 300)
    private String titulo;

    @Size(max = 1000)
    private String descripcion;

    @NotNull(message = "La cédula del creador es obligatoria")
    private Long creadoPorCedula;

    @NotNull(message = "El NIT de la organización es obligatorio")
    private Long organizacionNit;

    @NotNull(message = "El tipo de documento es obligatorio")
    private Long tipoDocumentoId;

    private Long estadoDocumentoId;

    // Gestionados por el service antes de persistir
    private Integer version;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}