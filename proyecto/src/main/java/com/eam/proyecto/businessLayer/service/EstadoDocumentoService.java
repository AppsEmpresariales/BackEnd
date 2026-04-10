// EstadoDocumentoService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.EstadoDocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoUpdateDTO;
import java.util.List;

public interface EstadoDocumentoService {
    EstadoDocumentoDTO createEstadoDocumento(EstadoDocumentoCreateDTO createDTO);
    EstadoDocumentoDTO getEstadoDocumentoById(Long id);
    EstadoDocumentoDTO getEstadoInicialByOrganizacion(Long organizacionNit);
    List<EstadoDocumentoDTO> getEstadosByOrganizacion(Long organizacionNit);
    List<EstadoDocumentoDTO> getEstadosFinalesByOrganizacion(Long organizacionNit);
    EstadoDocumentoDTO updateEstadoDocumento(Long id, EstadoDocumentoUpdateDTO updateDTO);
    void deleteEstadoDocumento(Long id);
}