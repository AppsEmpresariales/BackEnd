// TipoDocumentoService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.TipoDocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.TipoDocumentoDTO;
import com.eam.proyecto.businessLayer.dto.TipoDocumentoUpdateDTO;
import java.util.List;

public interface TipoDocumentoService {
    TipoDocumentoDTO createTipoDocumento(TipoDocumentoCreateDTO createDTO);
    TipoDocumentoDTO getTipoDocumentoById(Long id);
    TipoDocumentoDTO getTipoDocumentoByIdAndOrganizacion(Long id, Long organizacionNit);
    List<TipoDocumentoDTO> getTiposActivosByOrganizacion(Long organizacionNit);
    List<TipoDocumentoDTO> getAllTiposByOrganizacion(Long organizacionNit);
    TipoDocumentoDTO updateTipoDocumento(Long id, TipoDocumentoUpdateDTO updateDTO);
    TipoDocumentoDTO desactivarTipoDocumento(Long id);
    TipoDocumentoDTO activarTipoDocumento(Long id);
    void deleteTipoDocumento(Long id);
}