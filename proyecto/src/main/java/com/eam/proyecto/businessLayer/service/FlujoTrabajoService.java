// FlujoTrabajoService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoUpdateDTO;
import java.util.List;

public interface FlujoTrabajoService {
    FlujoTrabajoDTO createFlujoTrabajo(FlujoTrabajoCreateDTO createDTO);
    FlujoTrabajoDTO getFlujoTrabajoById(Long id);
    FlujoTrabajoDTO getFlujoActivoByOrganizacionAndTipoDocumento(Long organizacionNit, Long tipoDocumentoId);
    List<FlujoTrabajoDTO> getFlujosActivosByOrganizacion(Long organizacionNit);
    List<FlujoTrabajoDTO> getAllFlujosByOrganizacion(Long organizacionNit);
    FlujoTrabajoDTO updateFlujoTrabajo(Long id, FlujoTrabajoUpdateDTO updateDTO);
    void deleteFlujoTrabajo(Long id);
}