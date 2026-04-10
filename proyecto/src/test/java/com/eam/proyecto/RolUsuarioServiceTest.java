package com.eam.proyecto.unit.service;

import com.eam.proyecto.businessLayer.dto.RolDTO;
import com.eam.proyecto.businessLayer.dto.RolUsuarioAsignarDTO;
import com.eam.proyecto.businessLayer.dto.RolUsuarioDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioDTO;
import com.eam.proyecto.businessLayer.service.RolService;
import com.eam.proyecto.businessLayer.service.UsuarioService;
import com.eam.proyecto.businessLayer.service.impl.RolUsuarioServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.RolUsuarioDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests para RolUsuarioServiceImpl
 *
 * OBJETIVO: Verificar la lógica de asignación y revocación de roles sobre
 * usuarios, incluyendo:
 *   - Validación de existencia de usuario y rol antes de operar.
 *   - Prevención de asignaciones duplicadas.
 *   - Prevención de revocar un rol no asignado.
 *   - Consulta de roles de un usuario.
 *
 * Dependencias mockeadas: RolUsuarioDAO, UsuarioService, RolService
 * SUT: RolUsuarioServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RolUsuarioService - Unit Tests")
public class RolUsuarioServiceTest {

    // ─── Dependencias mockeadas ───────────────────────────────────────────────

    @Mock
    private RolUsuarioDAO rolUsuarioDAO;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private RolService rolService;

    // ─── Sistema bajo prueba (SUT) ────────────────────────────────────────────

    @InjectMocks
    private RolUsuarioServiceImpl rolUsuarioService;

    // ─── Datos de prueba reutilizables ────────────────────────────────────────

    private Long validCedula;
    private Long validRolId;
    private RolUsuarioAsignarDTO validAsignarDTO;
    private RolDTO rolAdmin;
    private UsuarioDTO validUsuarioDTO;
    private RolUsuarioDTO rolUsuarioDTO;

    /**
     * Configuración ejecutada ANTES de cada test.
     */
    @BeforeEach
    void setUp() {
        validCedula = 123456789L;
        validRolId  = 1L;

        // DTO de asignación válido
        validAsignarDTO = new RolUsuarioAsignarDTO();
        validAsignarDTO.setUsuarioCedula(validCedula);
        validAsignarDTO.setRolId(validRolId);

        // Rol existente en catálogo
        rolAdmin = new RolDTO(validRolId, "ADMIN_ORG", "Administrador de la organización");

        // Usuario existente
        validUsuarioDTO = new UsuarioDTO();
        validUsuarioDTO.setCedula(validCedula);
        validUsuarioDTO.setNombre("Juan Pérez");
        validUsuarioDTO.setEmail("juan@empresa.com");
        validUsuarioDTO.setActive(true);

        // Resultado esperado de asignación
        rolUsuarioDTO = new RolUsuarioDTO();
        rolUsuarioDTO.setId(10L);
        rolUsuarioDTO.setUsuarioCedula(validCedula);
        rolUsuarioDTO.setUsuarioNombre("Juan Pérez");
        rolUsuarioDTO.setRolId(validRolId);
        rolUsuarioDTO.setRolNombre("ADMIN_ORG");
    }

    // ==================== asignarRol ====================

    @Test
    @DisplayName("ASIGNAR - datos válidos debe retornar RolUsuarioDTO con la asignación creada")
    void asignarRol_ValidData_ShouldReturnRolUsuarioDTO() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(rolService.getRolById(validRolId)).thenReturn(rolAdmin);
        when(rolUsuarioDAO.existeAsignacion(validCedula, validRolId)).thenReturn(false);
        when(rolUsuarioDAO.save(any(RolUsuarioAsignarDTO.class))).thenReturn(rolUsuarioDTO);

        // ACT
        RolUsuarioDTO result = rolUsuarioService.asignarRol(validAsignarDTO);

        // ASSERT - estado
        assertThat(result).isNotNull();
        assertThat(result.getUsuarioCedula()).isEqualTo(validCedula);
        assertThat(result.getRolId()).isEqualTo(validRolId);
        assertThat(result.getRolNombre()).isEqualTo("ADMIN_ORG");

        // ASSERT - comportamiento: se verificó usuario, rol y unicidad antes de persistir
        verify(usuarioService, times(1)).getUsuarioByCedula(validCedula);
        verify(rolService, times(1)).getRolById(validRolId);
        verify(rolUsuarioDAO, times(1)).existeAsignacion(validCedula, validRolId);
        verify(rolUsuarioDAO, times(1)).save(any(RolUsuarioAsignarDTO.class));
    }

    @Test
    @DisplayName("ASIGNAR - cédula null debe lanzar IllegalArgumentException sin consultar servicios")
    void asignarRol_NullCedula_ShouldThrowIllegalArgumentExceptionBeforeServices() {
        // ARRANGE
        validAsignarDTO.setUsuarioCedula(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> rolUsuarioService.asignarRol(validAsignarDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula del usuario es obligatoria");

        // Nunca se deben consultar servicios si la validación básica falla
        verify(usuarioService, never()).getUsuarioByCedula(anyLong());
        verify(rolService,     never()).getRolById(anyLong());
        verify(rolUsuarioDAO,  never()).save(any());
    }

    @Test
    @DisplayName("ASIGNAR - rolId null debe lanzar IllegalArgumentException sin consultar servicios")
    void asignarRol_NullRolId_ShouldThrowIllegalArgumentExceptionBeforeServices() {
        // ARRANGE
        validAsignarDTO.setRolId(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> rolUsuarioService.asignarRol(validAsignarDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID del rol es obligatorio");

        verify(usuarioService, never()).getUsuarioByCedula(anyLong());
        verify(rolService,     never()).getRolById(anyLong());
        verify(rolUsuarioDAO,  never()).save(any());
    }

    @Test
    @DisplayName("ASIGNAR - usuario inexistente debe lanzar RuntimeException sin persistir")
    void asignarRol_NonExistentUser_ShouldThrowRuntimeException() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(validCedula))
                .thenThrow(new RuntimeException("Usuario no encontrado con cédula: " + validCedula));

        // ACT & ASSERT
        assertThatThrownBy(() -> rolUsuarioService.asignarRol(validAsignarDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");

        verify(rolUsuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("ASIGNAR - rol inexistente debe lanzar RuntimeException sin persistir")
    void asignarRol_NonExistentRol_ShouldThrowRuntimeException() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(rolService.getRolById(validRolId))
                .thenThrow(new RuntimeException("Rol no encontrado con ID: " + validRolId));

        // ACT & ASSERT
        assertThatThrownBy(() -> rolUsuarioService.asignarRol(validAsignarDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol no encontrado");

        verify(rolUsuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("ASIGNAR - asignación duplicada debe lanzar IllegalStateException sin persistir")
    void asignarRol_DuplicateAssignment_ShouldThrowIllegalStateException() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(rolService.getRolById(validRolId)).thenReturn(rolAdmin);
        when(rolUsuarioDAO.existeAsignacion(validCedula, validRolId)).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> rolUsuarioService.asignarRol(validAsignarDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usuario ya tiene asignado este rol");

        verify(rolUsuarioDAO, never()).save(any());
    }

    // ==================== revocarRol ====================

    @Test
    @DisplayName("REVOCAR - rol asignado existente debe completarse sin excepción")
    void revocarRol_ExistingAssignment_ShouldCompleteWithoutException() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(rolService.getRolById(validRolId)).thenReturn(rolAdmin);
        when(rolUsuarioDAO.existeAsignacion(validCedula, validRolId)).thenReturn(true);
        doNothing().when(rolUsuarioDAO).deleteByUsuarioCedulaAndRolId(validCedula, validRolId);

        // ACT & ASSERT
        assertThatCode(() -> rolUsuarioService.revocarRol(validCedula, validRolId))
                .doesNotThrowAnyException();

        // Verificar que se ejecutaron todos los pasos
        verify(usuarioService, times(1)).getUsuarioByCedula(validCedula);
        verify(rolService,     times(1)).getRolById(validRolId);
        verify(rolUsuarioDAO,  times(1)).existeAsignacion(validCedula, validRolId);
        verify(rolUsuarioDAO,  times(1)).deleteByUsuarioCedulaAndRolId(validCedula, validRolId);
    }

    @Test
    @DisplayName("REVOCAR - usuario inexistente debe lanzar RuntimeException sin eliminar")
    void revocarRol_NonExistentUser_ShouldThrowRuntimeException() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(validCedula))
                .thenThrow(new RuntimeException("Usuario no encontrado con cédula: " + validCedula));

        // ACT & ASSERT
        assertThatThrownBy(() -> rolUsuarioService.revocarRol(validCedula, validRolId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");

        verify(rolUsuarioDAO, never()).deleteByUsuarioCedulaAndRolId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("REVOCAR - rol inexistente debe lanzar RuntimeException sin eliminar")
    void revocarRol_NonExistentRol_ShouldThrowRuntimeException() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(rolService.getRolById(validRolId))
                .thenThrow(new RuntimeException("Rol no encontrado con ID: " + validRolId));

        // ACT & ASSERT
        assertThatThrownBy(() -> rolUsuarioService.revocarRol(validCedula, validRolId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol no encontrado");

        verify(rolUsuarioDAO, never()).deleteByUsuarioCedulaAndRolId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("REVOCAR - asignación no existente debe lanzar IllegalStateException sin eliminar")
    void revocarRol_AssignmentNotFound_ShouldThrowIllegalStateException() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(rolService.getRolById(validRolId)).thenReturn(rolAdmin);
        when(rolUsuarioDAO.existeAsignacion(validCedula, validRolId)).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> rolUsuarioService.revocarRol(validCedula, validRolId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usuario no tiene asignado el rol indicado");

        verify(rolUsuarioDAO, never()).deleteByUsuarioCedulaAndRolId(anyLong(), anyLong());
    }

    // ==================== getRolesByUsuario ====================

    @Test
    @DisplayName("GET roles by usuario - usuario existente debe retornar lista de roles asignados")
    void getRolesByUsuario_ExistingUser_ShouldReturnRolList() {
        // ARRANGE
        RolUsuarioDTO rolUser = new RolUsuarioDTO();
        rolUser.setId(20L);
        rolUser.setUsuarioCedula(validCedula);
        rolUser.setRolId(2L);
        rolUser.setRolNombre("USER_ESTANDAR");

        List<RolUsuarioDTO> assignedRoles = Arrays.asList(rolUsuarioDTO, rolUser);

        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(rolUsuarioDAO.findByUsuarioCedula(validCedula)).thenReturn(assignedRoles);

        // ACT
        List<RolUsuarioDTO> result = rolUsuarioService.getRolesByUsuario(validCedula);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).extracting("rolNombre")
                .containsExactlyInAnyOrder("ADMIN_ORG", "USER_ESTANDAR");

        verify(usuarioService, times(1)).getUsuarioByCedula(validCedula);
        verify(rolUsuarioDAO,  times(1)).findByUsuarioCedula(validCedula);
    }

    @Test
    @DisplayName("GET roles by usuario - usuario sin roles debe retornar lista vacía")
    void getRolesByUsuario_UserWithNoRoles_ShouldReturnEmptyList() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(rolUsuarioDAO.findByUsuarioCedula(validCedula)).thenReturn(List.of());

        // ACT
        List<RolUsuarioDTO> result = rolUsuarioService.getRolesByUsuario(validCedula);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(usuarioService, times(1)).getUsuarioByCedula(validCedula);
        verify(rolUsuarioDAO,  times(1)).findByUsuarioCedula(validCedula);
    }

    @Test
    @DisplayName("GET roles by usuario - usuario inexistente debe lanzar RuntimeException sin consultar DAO")
    void getRolesByUsuario_NonExistentUser_ShouldThrowRuntimeException() {
        // ARRANGE
        Long inexistentCedula = 999999L;
        when(usuarioService.getUsuarioByCedula(inexistentCedula))
                .thenThrow(new RuntimeException("Usuario no encontrado con cédula: " + inexistentCedula));

        // ACT & ASSERT
        assertThatThrownBy(() -> rolUsuarioService.getRolesByUsuario(inexistentCedula))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");

        verify(rolUsuarioDAO, never()).findByUsuarioCedula(anyLong());
    }
}
