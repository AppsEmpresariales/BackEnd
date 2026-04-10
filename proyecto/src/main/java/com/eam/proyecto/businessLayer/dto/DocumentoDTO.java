package com.eam.proyecto.businessLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para Documento.
 *
 * Se usa cuando DEVOLVEMOS la información completa de un documento al cliente.
 * Las relaciones (organización, tipo, estado, creador) se representan
 * por sus IDs para mantener el DTO plano y liviano.
 *
 * RF17 / RF18 / RF19 / RF20 / RF21 / RF30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información del documento digital gestionado en la plataforma")
public class DocumentoDTO {

    @Schema(description = "ID único del documento", example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Título del documento", example = "Contrato de prestación de servicios 2026",
            required = true, maxLength = 300)
    private String titulo;

    @Schema(description = "Descripción o resumen del contenido del documento",
            example = "Contrato firmado con proveedor X para servicios de mantenimiento", maxLength = 1000)
    private String descripcion;

    @Schema(description = "Número de versión del documento",
            example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer version;

    @Schema(description = "Nombre original del archivo adjunto",
            example = "contrato_proveedor_x.pdf", accessMode = Schema.AccessMode.READ_ONLY)
    private String archivoNombre;

    @Schema(description = "Ruta interna de almacenamiento del archivo",
            example = "/storage/org_900123456/2026/04/contrato_proveedor_x.pdf",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String archivoRuta;

    @Schema(description = "Tamaño del archivo en bytes",
            example = "204800", accessMode = Schema.AccessMode.READ_ONLY)
    private Long tamanioArchivo;

    @Schema(description = "Cédula del usuario que creó el documento",
            example = "1234567890", accessMode = Schema.AccessMode.READ_ONLY)
    private Long creadoPorCedula;

    @Schema(description = "NIT de la organización propietaria del documento",
            example = "900123456", required = true)
    private Long organizacionNit;

    @Schema(description = "ID del tipo documental asociado",
            example = "3", required = true)
    private Long tipoDocumentoId;

    @Schema(description = "ID del estado documental actual del documento",
            example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long estadoDocumentoId;

    @Schema(description = "Fecha y hora de creación del documento",
            example = "2026-04-01T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime creadoEn;

    @Schema(description = "Fecha y hora de la última modificación del documento",
            example = "2026-04-05T15:45:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime actualizadoEn;
}
