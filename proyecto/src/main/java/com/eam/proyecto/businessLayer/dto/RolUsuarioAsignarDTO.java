package com.eam.proyecto.businessLayer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RolUsuarioAsignarDTO {

    @NotNull
    private Long usuarioCedula;

    @NotNull
    private Long rolId;
}
