package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.UsuarioCreateDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioUpdateDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.UsuarioService;
import com.eam.proyecto.persistenceLayer.dao.UsuarioDAO;
import com.eam.proyecto.persistenceLayer.entity.RolEntity;
import com.eam.proyecto.persistenceLayer.entity.RolUsuarioEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import com.eam.proyecto.persistenceLayer.repository.RolRepository;
import com.eam.proyecto.persistenceLayer.repository.RolUsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final OrganizacionService organizacionService;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;
    private final RolUsuarioRepository rolUsuarioRepository;

    @Override
    public UsuarioDTO createUsuario(UsuarioCreateDTO createDTO) {
        log.info("Creando usuario con cedula: {}", createDTO.getCedula());

        validateUsuarioData(createDTO);
        organizacionService.getOrganizacionActivaByNit(createDTO.getOrganizacionNit());

        if (usuarioDAO.existsByEmail(createDTO.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + createDTO.getEmail());
        }

        createDTO.setPasswordHash(passwordEncoder.encode(createDTO.getPassword()));
        createDTO.setActive(true);
        createDTO.setCreadoEn(LocalDateTime.now());

        UsuarioDTO result = usuarioDAO.save(createDTO);
        assignDefaultUserRole(createDTO.getCedula());

        log.info("Usuario creado exitosamente con cedula: {}", result.getCedula());
        return populateUserRoles(result);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO getUsuarioByCedula(Long cedula) {
        return usuarioDAO.findByCedula(cedula)
                .map(this::populateUserRoles)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con cedula: " + cedula));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO getUsuarioByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }

        return usuarioDAO.findByEmailAndActive(email)
                .map(this::populateUserRoles)
                .orElseThrow(() -> new RuntimeException("Credenciales invalidas"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> getUsuariosByOrganizacion(Long organizacionNit) {
        organizacionService.getOrganizacionByNit(organizacionNit);
        List<UsuarioDTO> dtos = usuarioDAO.findByOrganizacionNit(organizacionNit);
        dtos.forEach(this::populateUserRoles);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> getUsuariosActivosByOrganizacion(Long organizacionNit) {
        organizacionService.getOrganizacionByNit(organizacionNit);
        List<UsuarioDTO> dtos = usuarioDAO.findActivosByOrganizacionNit(organizacionNit);
        dtos.forEach(this::populateUserRoles);
        return dtos;
    }

    @Override
    public UsuarioDTO updateUsuario(Long cedula, UsuarioUpdateDTO updateDTO) {
        getUsuarioByCedula(cedula);
        validateUsuarioUpdateData(updateDTO);

        if (updateDTO.getPassword() != null && !updateDTO.getPassword().trim().isEmpty()) {
            updateDTO.setPasswordHash(passwordEncoder.encode(updateDTO.getPassword()));
        }

        return usuarioDAO.update(cedula, updateDTO)
                .map(this::populateUserRoles)
                .orElseThrow(() -> new RuntimeException("Error al actualizar usuario con cedula: " + cedula));
    }

    @Override
    public UsuarioDTO activarUsuario(Long cedula) {
        UsuarioDTO usuario = getUsuarioByCedula(cedula);

        if (Boolean.TRUE.equals(usuario.getActive())) {
            throw new IllegalStateException("El usuario ya se encuentra activo");
        }

        UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
        updateDTO.setActive(true);

        return usuarioDAO.update(cedula, updateDTO)
                .map(this::populateUserRoles)
                .orElseThrow(() -> new RuntimeException("Error al activar usuario con cedula: " + cedula));
    }

    @Override
    public UsuarioDTO inactivarUsuario(Long cedula) {
        UsuarioDTO usuario = getUsuarioByCedula(cedula);

        if (Boolean.FALSE.equals(usuario.getActive())) {
            throw new IllegalStateException("El usuario ya se encuentra inactivo");
        }

        UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
        updateDTO.setActive(false);

        return usuarioDAO.update(cedula, updateDTO)
                .map(this::populateUserRoles)
                .orElseThrow(() -> new RuntimeException("Error al inactivar usuario con cedula: " + cedula));
    }

    @Override
    public void deleteUsuario(Long cedula) {
        getUsuarioByCedula(cedula);

        boolean deleted = usuarioDAO.deleteByCedula(cedula);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar usuario con cedula: " + cedula);
        }
    }

    private void validateUsuarioData(UsuarioCreateDTO dto) {
        if (dto.getCedula() == null || dto.getCedula() <= 0) {
            throw new IllegalArgumentException("La cedula debe ser un numero positivo");
        }
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio");
        }
        if (dto.getNombre().length() > 150) {
            throw new IllegalArgumentException("El nombre no puede exceder 150 caracteres");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 8 caracteres");
        }
        if (dto.getOrganizacionNit() == null) {
            throw new IllegalArgumentException("El NIT de la organizacion es obligatorio");
        }
    }

    private void validateUsuarioUpdateData(UsuarioUpdateDTO dto) {
        if (dto.getNombre() != null) {
            if (dto.getNombre().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre no puede estar vacio");
            }
            if (dto.getNombre().length() > 150) {
                throw new IllegalArgumentException("El nombre no puede exceder 150 caracteres");
            }
        }
        if (dto.getPassword() != null && dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("La contrasena debe tener al menos 8 caracteres");
        }
    }

    private void assignDefaultUserRole(Long cedula) {
        RolEntity userRole = rolRepository.findByNombre("USER")
                .orElseGet(() -> {
                    RolEntity rol = new RolEntity();
                    rol.setNombre("USER");
                    rol.setDescripcion("Usuario Estandar");
                    return rolRepository.save(rol);
                });

        UsuarioEntity usuarioRef = new UsuarioEntity();
        usuarioRef.setCedula(cedula);

        if (!rolUsuarioRepository.existsByUsuarioAndRol(usuarioRef, userRole)) {
            RolUsuarioEntity rolUsuario = new RolUsuarioEntity();
            rolUsuario.setUsuario(usuarioRef);
            rolUsuario.setRol(userRole);
            rolUsuarioRepository.save(rolUsuario);
        }
    }

    private UsuarioDTO populateUserRoles(UsuarioDTO dto) {
        if (dto == null || dto.getCedula() == null) return dto;
        List<RolUsuarioEntity> roles = rolUsuarioRepository.findByUsuarioCedula(dto.getCedula());
        if (roles != null && !roles.isEmpty()) {
            String rolNombres = roles.stream()
                .map(ru -> ru.getRol().getNombre())
                .collect(java.util.stream.Collectors.joining(", "));
            dto.setRolNombre(rolNombres);
        }
        return dto;
    }
}
