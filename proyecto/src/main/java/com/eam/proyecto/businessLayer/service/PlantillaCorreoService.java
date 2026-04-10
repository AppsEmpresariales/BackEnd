// PlantillaCorreoService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.PlantillaCorreoCreateDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoUpdateDTO;
import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import java.util.List;

public interface PlantillaCorreoService {
    PlantillaCorreoDTO createPlantillaCorreo(PlantillaCorreoCreateDTO createDTO);
    PlantillaCorreoDTO getPlantillaCorreoById(Long id);
    PlantillaCorreoDTO getPlantillaActivaByOrganizacionAndEvento(Long organizacionNit, TipoEventoEnum tipoEvento);
    List<PlantillaCorreoDTO> getPlantillasActivasByOrganizacion(Long organizacionNit);
    List<PlantillaCorreoDTO> getAllPlantillasByOrganizacion(Long organizacionNit);
    PlantillaCorreoDTO updatePlantillaCorreo(Long id, PlantillaCorreoUpdateDTO updateDTO);
    PlantillaCorreoDTO activarPlantilla(Long id);
    PlantillaCorreoDTO desactivarPlantilla(Long id);
    void deletePlantillaCorreo(Long id);
}