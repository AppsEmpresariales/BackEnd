// FlujoTrabajoPasoService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoUpdateDTO;
import java.util.List;

public interface FlujoTrabajoPasoService {
    FlujoTrabajoPasoDTO createPaso(FlujoTrabajoPasoCreateDTO createDTO);
    FlujoTrabajoPasoDTO getPasoById(Long id);
    FlujoTrabajoPasoDTO getPrimerPaso(Long flujoTrabajoId);
    FlujoTrabajoPasoDTO getSiguientePaso(Long flujoTrabajoId, Integer ordenActual);
    List<FlujoTrabajoPasoDTO> getPasosByFlujoTrabajo(Long flujoTrabajoId);
    FlujoTrabajoPasoDTO updatePaso(Long id, FlujoTrabajoPasoUpdateDTO updateDTO);
    void deletePaso(Long id);
}