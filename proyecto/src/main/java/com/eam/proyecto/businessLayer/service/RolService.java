// RolService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.RolDTO;
import java.util.List;

public interface RolService {

    RolDTO getRolById(Long id);
    RolDTO getRolByNombre(String nombre);
    List<RolDTO> getAllRoles();
}