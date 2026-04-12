// DocumentoService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.DocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoUpdateDTO;
import java.util.List;

public interface DocumentoService {
    DocumentoDTO createDocumento(DocumentoCreateDTO createDTO);
    DocumentoDTO getDocumentoById(Long id);
    DocumentoDTO getDocumentoByIdAndOrganizacion(Long id, Long organizacionNit);
    List<DocumentoDTO> getDocumentosByOrganizacion(Long organizacionNit);
    DocumentoDTO updateDocumento(Long id, DocumentoUpdateDTO updateDTO);
    DocumentoDTO cambiarEstado(Long id, Long nuevoEstadoId);
    void deleteDocumento(Long id);
}