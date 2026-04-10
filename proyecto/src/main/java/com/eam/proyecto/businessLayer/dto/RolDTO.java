package com.eam.proyecto.businessLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información del rol del sistema")
public class RolDTO {

    @Schema(description = "ID único del rol", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Nombre del rol", example = "ADMIN_ORG", required = true)
    private String nombre;

    @Schema(description = "Descripción del rol", example = "Administrador de la organización")
    private String descripcion;
}