// RolUsuarioService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.RolUsuarioAsignarDTO;
import com.eam.proyecto.businessLayer.dto.RolUsuarioDTO;
import java.util.List;

public interface RolUsuarioService {
    RolUsuarioDTO asignarRol(RolUsuarioAsignarDTO asignarDTO);
    void revocarRol(Long cedula, Long rolId);
    List<RolUsuarioDTO> getRolesByUsuario(Long cedula);
}