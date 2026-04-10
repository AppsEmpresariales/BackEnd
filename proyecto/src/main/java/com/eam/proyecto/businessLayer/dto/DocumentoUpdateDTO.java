package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de actualización de metadatos para DocumentoEntity.
 * Todos los campos son opcionales (actualización parcial PATCH).
 *
 * CAMPOS NO ACTUALIZABLES (ignorados por el mapper):
 * - id, creadoEn, creadoPor, organizacion: inmutables.
 * - estadoDocumento: solo cambia a través del flujo de trabajo (US-030).
 * - archivoNombre / archivoRuta / tamanioArchivo: gestionados por el service de storage.
 * - version: el service lo incrementa tras cada actualización.
 *
 * US-019
 */
@Data
public class DocumentoUpdateDTO {

    @Size(max = 300)
    private String titulo;

    @Size(max = 1000)
    private String descripcion;

    /** Reasignar tipo documental (US-027). */
    private Long tipoDocumentoId;
}
