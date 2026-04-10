// AuditRegistroService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.AuditRegistroCreateDTO;
import com.eam.proyecto.businessLayer.dto.AuditRegistroDTO;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditRegistroService {
    AuditRegistroDTO registrarAccion(AuditRegistroCreateDTO createDTO);
    AuditRegistroDTO getRegistroById(Long id);
    List<AuditRegistroDTO> getHistorialByDocumento(Long documentoId);
    List<AuditRegistroDTO> getTrazabilidadByUsuario(Long cedula);
    List<AuditRegistroDTO> getTrazabilidadCompleta(String accion, LocalDateTime desde, LocalDateTime hasta);
}