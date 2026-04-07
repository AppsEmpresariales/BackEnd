package com.docucloud.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de creación para DocumentoEntity.
 *
 * CAMPOS OMITIDOS (los gestiona el service o JPA):
 * - id: autogenerado.
 * - version: el service lo inicializa en 1.
 * - creadoEn / actualizadoEn: gestionados por JPA.
 * - archivoNombre / archivoRuta / tamanioArchivo: se asignan en un paso
 *   separado al subir el archivo (US-018).
 * - estadoDocumentoId: el service asigna el estado inicial de la organización.
 *
 * US-017 / US-027
 */
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

    /** Tipo documental asociado al documento (US-027). */
    @NotNull(message = "El tipo de documento es obligatorio")
    private Long tipoDocumentoId;

    /**
     * Estado inicial explícito (opcional).
     * Si es null, el service asigna el estado inicial de la organización.
     */
    private Long estadoDocumentoId;
}
