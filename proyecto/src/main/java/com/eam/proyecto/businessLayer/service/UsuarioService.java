// UsuarioService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.UsuarioCreateDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioUpdateDTO;
import java.util.List;

public interface UsuarioService {
    UsuarioDTO createUsuario(UsuarioCreateDTO createDTO);
    UsuarioDTO getUsuarioByCedula(Long cedula);
    UsuarioDTO getUsuarioByEmail(String email);
    List<UsuarioDTO> getUsuariosByOrganizacion(Long organizacionNit);
    List<UsuarioDTO> getUsuariosActivosByOrganizacion(Long organizacionNit);
    UsuarioDTO updateUsuario(Long cedula, UsuarioUpdateDTO updateDTO);
    UsuarioDTO activarUsuario(Long cedula);
    UsuarioDTO inactivarUsuario(Long cedula);
    void deleteUsuario(Long cedula);
}