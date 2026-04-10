// FlujoTrabajoTareaService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaUpdateDTO;
import com.eam.proyecto.persistenceLayer.entity.enums.EstadoTareaEnum;
import java.util.List;

public interface FlujoTrabajoTareaService {
    FlujoTrabajoTareaDTO asignarTarea(FlujoTrabajoTareaCreateDTO createDTO);
    FlujoTrabajoTareaDTO getTareaById(Long id);
    FlujoTrabajoTareaDTO getTareaActivaByDocumento(Long documentoId);
    List<FlujoTrabajoTareaDTO> getTareasByDocumento(Long documentoId);
    List<FlujoTrabajoTareaDTO> getTareasPendientesByUsuario(Long cedula);
    FlujoTrabajoTareaDTO completarTarea(Long id, String comentario);
    FlujoTrabajoTareaDTO cancelarTarea(Long id);
    FlujoTrabajoTareaDTO updateTarea(Long id, FlujoTrabajoTareaUpdateDTO updateDTO);
}