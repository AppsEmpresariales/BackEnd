package com.eam.proyecto.unit.service;

import com.eam.proyecto.businessLayer.dto.OrganizacionCreateDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionUpdateDTO;
import com.eam.proyecto.businessLayer.service.impl.OrganizacionServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.OrganizacionDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit Tests para OrganizacionServiceImpl
 *
 * OBJETIVO: Probar la lógica de negocio del servicio de forma aislada
 * - No requiere base de datos
 * - No requiere Spring Context
 * - Usa mock para OrganizacionDAO
 * - Cubre RF01 / RF08 / RF10 / RF11
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizacionService - Unit Tests")
public class OrganizacionServiceTest {

    // ─── Dependencia mockeada ─────────────────────────────────────────────────
    @Mock
    private OrganizacionDAO organizacionDAO;

    // ─── Clase bajo prueba (SUT) ──────────────────────────────────────────────
    @InjectMocks
    private OrganizacionServiceImpl organizacionService;

    // ─── Datos de prueba compartidos ─────────────────────────────────────────
    private OrganizacionCreateDTO validCreateDTO;
    private OrganizacionDTO       validOrganizacionDTO;
    private Long                  validNit;

    /**
     * Se ejecuta antes de cada test.
     * Inicializa objetos en estado válido para reutilizarlos.
     */
    @BeforeEach
    void setUp() {
        validNit = 900123456L;

        validCreateDTO = new OrganizacionCreateDTO();
        validCreateDTO.setNit(validNit);
        validCreateDTO.setNombre("Empresa Demo S.A.");
        validCreateDTO.setEmail("admin@empresa.com");

        validOrganizacionDTO = new OrganizacionDTO();
        validOrganizacionDTO.setNit(validNit);
        validOrganizacionDTO.setNombre("Empresa Demo S.A.");
        validOrganizacionDTO.setEmail("admin@empresa.com");
        validOrganizacionDTO.setActive(true);
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("CREATE - Datos válidos deben retornar organización creada y activa")
    void createOrganizacion_ValidData_ShouldReturnCreatedOrganizacion() {
        // Arrange
        when(organizacionDAO.existsByNit(validNit)).thenReturn(false);
        when(organizacionDAO.existsByEmail(validCreateDTO.getEmail())).thenReturn(false);
        when(organizacionDAO.save(any(OrganizacionCreateDTO.class))).thenReturn(validOrganizacionDTO);

        // Act
        OrganizacionDTO result = organizacionService.createOrganizacion(validCreateDTO);

        // Assert - estado
        assertThat(result).isNotNull();
        assertThat(result.getNit()).isEqualTo(validNit);
        assertThat(result.getActive()).isTrue();

        // Assert - comportamiento: el service debe setear active=true y creadoEn antes de persistir
        ArgumentCaptor<OrganizacionCreateDTO> captor = ArgumentCaptor.forClass(OrganizacionCreateDTO.class);
        verify(organizacionDAO, times(1)).existsByNit(validNit);
        verify(organizacionDAO, times(1)).existsByEmail(validCreateDTO.getEmail());
        verify(organizacionDAO, times(1)).save(captor.capture());
        assertThat(captor.getValue().getActive()).isTrue();
        assertThat(captor.getValue().getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CREATE - NIT duplicado debe lanzar IllegalArgumentException")
    void createOrganizacion_DuplicateNit_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(organizacionDAO.existsByNit(validNit)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.createOrganizacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NIT");

        verify(organizacionDAO, times(1)).existsByNit(validNit);
        verify(organizacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Email duplicado debe lanzar IllegalArgumentException")
    void createOrganizacion_DuplicateEmail_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(organizacionDAO.existsByNit(validNit)).thenReturn(false);
        when(organizacionDAO.existsByEmail(validCreateDTO.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.createOrganizacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");

        verify(organizacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - NIT null debe lanzar IllegalArgumentException antes de consultar el DAO")
    void createOrganizacion_NullNit_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setNit(null);

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.createOrganizacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NIT");

        // Validación debe ocurrir antes de tocar el DAO
        verify(organizacionDAO, never()).existsByNit(any());
        verify(organizacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - NIT con valor cero o negativo debe lanzar IllegalArgumentException")
    void createOrganizacion_NonPositiveNit_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setNit(-100L);

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.createOrganizacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NIT");

        verify(organizacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Nombre null debe lanzar IllegalArgumentException")
    void createOrganizacion_NullNombre_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setNombre(null);

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.createOrganizacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        verify(organizacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Nombre vacío debe lanzar IllegalArgumentException")
    void createOrganizacion_EmptyNombre_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setNombre("   ");

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.createOrganizacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        verify(organizacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Nombre mayor a 200 caracteres debe lanzar IllegalArgumentException")
    void createOrganizacion_NombreTooLong_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setNombre("A".repeat(201));

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.createOrganizacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("200 caracteres");

        verify(organizacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Email null debe lanzar IllegalArgumentException")
    void createOrganizacion_NullEmail_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setEmail(null);

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.createOrganizacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");

        verify(organizacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Formato de email inválido debe lanzar IllegalArgumentException")
    void createOrganizacion_InvalidEmailFormat_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setEmail("no-es-un-email");

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.createOrganizacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");

        verify(organizacionDAO, never()).save(any());
    }

    // ==================== READ ====================

    @Test
    @DisplayName("READ - NIT existente debe retornar organización")
    void getOrganizacionByNit_ExistingNit_ShouldReturnOrganizacion() {
        // Arrange
        when(organizacionDAO.findByNit(validNit)).thenReturn(Optional.of(validOrganizacionDTO));

        // Act
        OrganizacionDTO result = organizacionService.getOrganizacionByNit(validNit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNit()).isEqualTo(validNit);
        verify(organizacionDAO, times(1)).findByNit(validNit);
    }

    @Test
    @DisplayName("READ - NIT inexistente debe lanzar RuntimeException")
    void getOrganizacionByNit_NonExistentNit_ShouldThrowRuntimeException() {
        // Arrange
        when(organizacionDAO.findByNit(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.getOrganizacionByNit(999999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");

        verify(organizacionDAO, times(1)).findByNit(999999L);
    }

    @Test
    @DisplayName("READ ACTIVA - Organización activa debe retornar DTO")
    void getOrganizacionActivaByNit_ActiveOrganizacion_ShouldReturnDTO() {
        // Arrange
        when(organizacionDAO.findActivaByNit(validNit)).thenReturn(Optional.of(validOrganizacionDTO));

        // Act
        OrganizacionDTO result = organizacionService.getOrganizacionActivaByNit(validNit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getActive()).isTrue();
        verify(organizacionDAO, times(1)).findActivaByNit(validNit);
    }

    @Test
    @DisplayName("READ ACTIVA - Organización inactiva o inexistente debe lanzar RuntimeException")
    void getOrganizacionActivaByNit_InactiveOrNotFound_ShouldThrowRuntimeException() {
        // Arrange
        when(organizacionDAO.findActivaByNit(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.getOrganizacionActivaByNit(validNit))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inactiva");

        verify(organizacionDAO, times(1)).findActivaByNit(validNit);
    }

    @Test
    @DisplayName("READ ALL - Debe retornar lista completa de organizaciones")
    void getAllOrganizaciones_ShouldReturnList() {
        // Arrange
        OrganizacionDTO org2 = new OrganizacionDTO();
        org2.setNit(800000001L);
        when(organizacionDAO.findAll()).thenReturn(Arrays.asList(validOrganizacionDTO, org2));

        // Act
        List<OrganizacionDTO> result = organizacionService.getAllOrganizaciones();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting("nit").contains(validNit, 800000001L);
        verify(organizacionDAO, times(1)).findAll();
    }

    @Test
    @DisplayName("READ ALL - Lista vacía debe retornarse correctamente")
    void getAllOrganizaciones_EmptyList_ShouldReturnEmptyList() {
        // Arrange
        when(organizacionDAO.findAll()).thenReturn(List.of());

        // Act
        List<OrganizacionDTO> result = organizacionService.getAllOrganizaciones();

        // Assert
        assertThat(result).isEmpty();
        verify(organizacionDAO, times(1)).findAll();
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("UPDATE - Datos válidos deben retornar organización actualizada")
    void updateOrganizacion_ValidData_ShouldReturnUpdatedOrganizacion() {
        // Arrange
        OrganizacionUpdateDTO updateDTO = new OrganizacionUpdateDTO();
        updateDTO.setNombre("Empresa Actualizada S.A.");
        updateDTO.setEmail("nuevo@empresa.com");

        OrganizacionDTO updated = new OrganizacionDTO();
        updated.setNit(validNit);
        updated.setNombre("Empresa Actualizada S.A.");
        updated.setEmail("nuevo@empresa.com");

        when(organizacionDAO.findByNit(validNit)).thenReturn(Optional.of(validOrganizacionDTO));
        when(organizacionDAO.update(eq(validNit), any(OrganizacionUpdateDTO.class)))
                .thenReturn(Optional.of(updated));

        // Act
        OrganizacionDTO result = organizacionService.updateOrganizacion(validNit, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNombre()).isEqualTo("Empresa Actualizada S.A.");
        verify(organizacionDAO, times(1)).findByNit(validNit);
        verify(organizacionDAO, times(1)).update(eq(validNit), any(OrganizacionUpdateDTO.class));
    }

    @Test
    @DisplayName("UPDATE - NIT inexistente debe lanzar RuntimeException")
    void updateOrganizacion_NonExistentNit_ShouldThrowRuntimeException() {
        // Arrange
        when(organizacionDAO.findByNit(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.updateOrganizacion(999999L, new OrganizacionUpdateDTO()))
                .isInstanceOf(RuntimeException.class);

        verify(organizacionDAO, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("UPDATE - Nombre vacío en DTO debe lanzar IllegalArgumentException")
    void updateOrganizacion_EmptyNombre_ShouldThrowIllegalArgumentException() {
        // Arrange
        OrganizacionUpdateDTO updateDTO = new OrganizacionUpdateDTO();
        updateDTO.setNombre("   ");
        when(organizacionDAO.findByNit(validNit)).thenReturn(Optional.of(validOrganizacionDTO));

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.updateOrganizacion(validNit, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        verify(organizacionDAO, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("UPDATE - Email con formato inválido debe lanzar IllegalArgumentException")
    void updateOrganizacion_InvalidEmailFormat_ShouldThrowIllegalArgumentException() {
        // Arrange
        OrganizacionUpdateDTO updateDTO = new OrganizacionUpdateDTO();
        updateDTO.setEmail("correo-sin-arroba");
        when(organizacionDAO.findByNit(validNit)).thenReturn(Optional.of(validOrganizacionDTO));

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.updateOrganizacion(validNit, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");

        verify(organizacionDAO, never()).update(anyLong(), any());
    }

    // ==================== DELETE ====================

    @Test
    @DisplayName("DELETE - NIT existente debe completar sin excepción")
    void deleteOrganizacion_ExistingNit_ShouldCompleteWithoutException() {
        // Arrange
        when(organizacionDAO.findByNit(validNit)).thenReturn(Optional.of(validOrganizacionDTO));
        when(organizacionDAO.deleteByNit(validNit)).thenReturn(true);

        // Act & Assert
        assertThatCode(() -> organizacionService.deleteOrganizacion(validNit))
                .doesNotThrowAnyException();

        verify(organizacionDAO, times(1)).findByNit(validNit);
        verify(organizacionDAO, times(1)).deleteByNit(validNit);
    }

    @Test
    @DisplayName("DELETE - NIT inexistente debe lanzar RuntimeException antes de intentar borrar")
    void deleteOrganizacion_NonExistentNit_ShouldThrowRuntimeException() {
        // Arrange
        when(organizacionDAO.findByNit(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.deleteOrganizacion(999999L))
                .isInstanceOf(RuntimeException.class);

        verify(organizacionDAO, never()).deleteByNit(anyLong());
    }

    @Test
    @DisplayName("DELETE - DAO retorna false debe lanzar RuntimeException")
    void deleteOrganizacion_DaoReturnsFalse_ShouldThrowRuntimeException() {
        // Arrange
        when(organizacionDAO.findByNit(validNit)).thenReturn(Optional.of(validOrganizacionDTO));
        when(organizacionDAO.deleteByNit(validNit)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> organizacionService.deleteOrganizacion(validNit))
                .isInstanceOf(RuntimeException.class);

        verify(organizacionDAO, times(1)).deleteByNit(validNit);
    }
}
