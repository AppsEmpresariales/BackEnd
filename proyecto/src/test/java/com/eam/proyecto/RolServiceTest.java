package com.eam.proyecto;

import com.eam.proyecto.businessLayer.dto.RolDTO;
import com.eam.proyecto.businessLayer.service.impl.RolServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.RolDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests para RolServiceImpl
 *
 * OBJETIVO: Verificar la lógica del catálogo de roles de forma aislada.
 * RolService es un servicio de solo lectura (READ-ONLY), por lo que
 * todos los tests cubren consultas y sus casos de error.
 *
 * Dependencias mockeadas: RolDAO
 * SUT: RolServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RolService - Unit Tests")
public class RolServiceTest {

    // ─── Dependencias mockeadas ───────────────────────────────────────────────

    @Mock
    private RolDAO rolDAO;

    // ─── Sistema bajo prueba (SUT) ────────────────────────────────────────────

    @InjectMocks
    private RolServiceImpl rolService;

    // ─── Datos de prueba reutilizables ────────────────────────────────────────

    private RolDTO rolAdmin;
    private RolDTO rolUser;
    private Long validRolId;

    /**
     * Configuración ejecutada ANTES de cada test.
     * Inicializa los DTOs de rol que se usan en múltiples casos.
     */
    @BeforeEach
    void setUp() {
        validRolId = 1L;

        rolAdmin = new RolDTO(1L, "ADMIN_ORG", "Administrador de la organización");
        rolUser  = new RolDTO(2L, "USER_ESTANDAR", "Usuario estándar");
    }

    // ==================== getRolById ====================

    @Test
    @DisplayName("GET by ID - rol existente debe retornar RolDTO")
    void getRolById_ExistingId_ShouldReturnRolDTO() {
        // ARRANGE
        when(rolDAO.findById(validRolId)).thenReturn(Optional.of(rolAdmin));

        // ACT
        RolDTO result = rolService.getRolById(validRolId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validRolId);
        assertThat(result.getNombre()).isEqualTo("ADMIN_ORG");
        assertThat(result.getDescripcion()).isEqualTo("Administrador de la organización");

        verify(rolDAO, times(1)).findById(validRolId);
    }

    @Test
    @DisplayName("GET by ID - rol inexistente debe lanzar RuntimeException")
    void getRolById_NonExistentId_ShouldThrowRuntimeException() {
        // ARRANGE
        Long nonExistentId = 999L;
        when(rolDAO.findById(nonExistentId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> rolService.getRolById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol no encontrado con ID: " + nonExistentId);

        verify(rolDAO, times(1)).findById(nonExistentId);
    }

    // ==================== getRolByNombre ====================

    @Test
    @DisplayName("GET by nombre - nombre válido existente debe retornar RolDTO")
    void getRolByNombre_ExistingName_ShouldReturnRolDTO() {
        // ARRANGE
        String nombre = "ADMIN_ORG";
        when(rolDAO.findByNombre(nombre)).thenReturn(Optional.of(rolAdmin));

        // ACT
        RolDTO result = rolService.getRolByNombre(nombre);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getNombre()).isEqualTo("ADMIN_ORG");
        assertThat(result.getId()).isEqualTo(1L);

        verify(rolDAO, times(1)).findByNombre(nombre);
    }

    @Test
    @DisplayName("GET by nombre - nombre null debe lanzar IllegalArgumentException antes de consultar DAO")
    void getRolByNombre_NullName_ShouldThrowIllegalArgumentException() {
        // ARRANGE - nombre null

        // ACT & ASSERT
        assertThatThrownBy(() -> rolService.getRolByNombre(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre del rol es obligatorio");

        // Verificar que el DAO nunca fue invocado (validación previa)
        verify(rolDAO, never()).findByNombre(anyString());
    }

    @Test
    @DisplayName("GET by nombre - nombre vacío debe lanzar IllegalArgumentException antes de consultar DAO")
    void getRolByNombre_EmptyName_ShouldThrowIllegalArgumentException() {
        // ARRANGE - nombre en blanco

        // ACT & ASSERT
        assertThatThrownBy(() -> rolService.getRolByNombre("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre del rol es obligatorio");

        verify(rolDAO, never()).findByNombre(anyString());
    }

    @Test
    @DisplayName("GET by nombre - nombre no registrado debe lanzar RuntimeException")
    void getRolByNombre_NonExistentName_ShouldThrowRuntimeException() {
        // ARRANGE
        String nombreInexistente = "ROL_INVENTADO";
        when(rolDAO.findByNombre(nombreInexistente)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> rolService.getRolByNombre(nombreInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol no encontrado con nombre: " + nombreInexistente);

        verify(rolDAO, times(1)).findByNombre(nombreInexistente);
    }

    // ==================== getAllRoles ====================

    @Test
    @DisplayName("GET all - debe retornar lista completa de roles del catálogo")
    void getAllRoles_ShouldReturnAllRoles() {
        // ARRANGE
        List<RolDTO> roles = Arrays.asList(rolAdmin, rolUser);
        when(rolDAO.findAll()).thenReturn(roles);

        // ACT
        List<RolDTO> result = rolService.getAllRoles();

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).extracting("nombre")
                .containsExactlyInAnyOrder("ADMIN_ORG", "USER_ESTANDAR");

        verify(rolDAO, times(1)).findAll();
    }

    @Test
    @DisplayName("GET all - catálogo vacío debe retornar lista vacía sin lanzar excepción")
    void getAllRoles_EmptyCatalog_ShouldReturnEmptyList() {
        // ARRANGE
        when(rolDAO.findAll()).thenReturn(List.of());

        // ACT
        List<RolDTO> result = rolService.getAllRoles();

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(rolDAO, times(1)).findAll();
    }
}
