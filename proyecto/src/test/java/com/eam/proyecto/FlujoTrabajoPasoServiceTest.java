package com.eam.proyecto;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoUpdateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoDTO;
import com.eam.proyecto.businessLayer.dto.RolDTO;
import com.eam.proyecto.businessLayer.service.FlujoTrabajoService;
import com.eam.proyecto.businessLayer.service.RolService;
import com.eam.proyecto.businessLayer.service.impl.FlujoTrabajoPasoServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.FlujoTrabajoPasoDAO;
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
 * Unit Tests para FlujoTrabajoPasoServiceImpl
 *
 * OBJETIVO: Verificar la lógica de negocio de los pasos dentro de un
 * flujo de trabajo (aprobación), que incluye:
 *   - Validación de campos obligatorios del paso.
 *   - Verificación de existencia del flujo y del rol requerido.
 *   - RF31: unicidad de ordenPaso dentro del mismo flujo.
 *   - Navegación del flujo: primer paso, siguiente paso, lista ordenada.
 *   - Actualización con control de orden duplicado al cambiar.
 *   - Eliminación con verificación previa de existencia.
 *
 * Dependencias mockeadas: FlujoTrabajoPasoDAO, FlujoTrabajoService, RolService
 * SUT: FlujoTrabajoPasoServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlujoTrabajoPasoService - Unit Tests")
public class FlujoTrabajoPasoServiceTest {

    // ─── Dependencias mockeadas ───────────────────────────────────────────────

    @Mock
    private FlujoTrabajoPasoDAO flujoTrabajoPasoDAO;

    @Mock
    private FlujoTrabajoService flujoTrabajoService;

    @Mock
    private RolService rolService;

    // ─── Sistema bajo prueba (SUT) ────────────────────────────────────────────

    @InjectMocks
    private FlujoTrabajoPasoServiceImpl flujoTrabajoPasoService;

    // ─── Datos de prueba reutilizables ────────────────────────────────────────

    private Long validPasoId;
    private Long validFlujoId;
    private Long validRolId;
    private Long validEstadoObjetivoId;
    private FlujoTrabajoPasoCreateDTO validCreateDTO;
    private FlujoTrabajoPasoDTO validPasoDTO;
    private FlujoTrabajoDTO validFlujoDTO;
    private RolDTO validRolDTO;

    /**
     * Configuración ejecutada ANTES de cada test.
     */
    @BeforeEach
    void setUp() {
        validPasoId         = 1L;
        validFlujoId        = 10L;
        validRolId          = 2L;
        validEstadoObjetivoId = 5L;

        // Flujo de trabajo existente
        validFlujoDTO = new FlujoTrabajoDTO();

        // Rol existente en catálogo
        validRolDTO = new RolDTO(validRolId, "ADMIN_ORG", "Administrador");

        // DTO de creación válido
        validCreateDTO = new FlujoTrabajoPasoCreateDTO();
        validCreateDTO.setFlujoTrabajoId(validFlujoId);
        validCreateDTO.setRolRequeridoId(validRolId);
        validCreateDTO.setOrdenPaso(1);
        validCreateDTO.setObjetivoEstadoId(validEstadoObjetivoId);
        validCreateDTO.setNombre("Revisión inicial");
        validCreateDTO.setDescripcion("Primer paso de revisión");

        // DTO de respuesta del DAO tras crear
        validPasoDTO = mock(FlujoTrabajoPasoDTO.class);
        when(validPasoDTO.getId()).thenReturn(validPasoId);
        when(validPasoDTO.getOrdenPaso()).thenReturn(1);
        when(validPasoDTO.getFlujoTrabajoId()).thenReturn(validFlujoId);
    }

    // ==================== createPaso ====================

    @Test
    @DisplayName("CREATE - datos válidos con orden único debe retornar PasoDTO creado")
    void createPaso_ValidData_ShouldReturnCreatedPasoDTO() {
        // ARRANGE
        when(flujoTrabajoService.getFlujoTrabajoById(validFlujoId)).thenReturn(validFlujoDTO);
        when(rolService.getRolById(validRolId)).thenReturn(validRolDTO);
        when(flujoTrabajoPasoDAO.existsByFlujoTrabajoIdAndOrdenPaso(validFlujoId, 1)).thenReturn(false);
        when(flujoTrabajoPasoDAO.save(any(FlujoTrabajoPasoCreateDTO.class))).thenReturn(validPasoDTO);

        // ACT
        FlujoTrabajoPasoDTO result = flujoTrabajoPasoService.createPaso(validCreateDTO);

        // ASSERT - estado
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validPasoId);

        // ASSERT - comportamiento: se verificaron flujo, rol y unicidad antes de persistir
        verify(flujoTrabajoService, times(1)).getFlujoTrabajoById(validFlujoId);
        verify(rolService,          times(1)).getRolById(validRolId);
        verify(flujoTrabajoPasoDAO, times(1)).existsByFlujoTrabajoIdAndOrdenPaso(validFlujoId, 1);
        verify(flujoTrabajoPasoDAO, times(1)).save(any(FlujoTrabajoPasoCreateDTO.class));
    }

    @Test
    @DisplayName("CREATE - flujoTrabajoId null debe lanzar IllegalArgumentException sin consultar DAO")
    void createPaso_NullFlujoTrabajoId_ShouldThrowIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setFlujoTrabajoId(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.createPaso(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("flujo de trabajo es obligatorio");

        verify(flujoTrabajoPasoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - rolRequeridoId null debe lanzar IllegalArgumentException sin consultar DAO")
    void createPaso_NullRolRequeridoId_ShouldThrowIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setRolRequeridoId(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.createPaso(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rol requerido para el paso es obligatorio");

        verify(flujoTrabajoPasoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - ordenPaso null debe lanzar IllegalArgumentException sin consultar DAO")
    void createPaso_NullOrdenPaso_ShouldThrowIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setOrdenPaso(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.createPaso(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orden del paso debe ser un número mayor a cero");

        verify(flujoTrabajoPasoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - ordenPaso cero debe lanzar IllegalArgumentException sin consultar DAO")
    void createPaso_ZeroOrdenPaso_ShouldThrowIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setOrdenPaso(0);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.createPaso(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orden del paso debe ser un número mayor a cero");

        verify(flujoTrabajoPasoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - objetivoEstadoId null debe lanzar IllegalArgumentException sin consultar DAO")
    void createPaso_NullObjetivoEstadoId_ShouldThrowIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setObjetivoEstadoId(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.createPaso(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estado objetivo del paso es obligatorio");

        verify(flujoTrabajoPasoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - flujo inexistente debe lanzar RuntimeException sin persistir")
    void createPaso_NonExistentFlujo_ShouldThrowRuntimeException() {
        // ARRANGE
        when(flujoTrabajoService.getFlujoTrabajoById(validFlujoId))
                .thenThrow(new RuntimeException("Flujo no encontrado con ID: " + validFlujoId));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.createPaso(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flujo no encontrado");

        verify(flujoTrabajoPasoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - rol inexistente debe lanzar RuntimeException sin persistir")
    void createPaso_NonExistentRol_ShouldThrowRuntimeException() {
        // ARRANGE
        when(flujoTrabajoService.getFlujoTrabajoById(validFlujoId)).thenReturn(validFlujoDTO);
        when(rolService.getRolById(validRolId))
                .thenThrow(new RuntimeException("Rol no encontrado con ID: " + validRolId));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.createPaso(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol no encontrado");

        verify(flujoTrabajoPasoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - RF31: orden duplicado en el mismo flujo debe lanzar IllegalArgumentException")
    void createPaso_DuplicateOrdenInFlujo_ShouldThrowIllegalArgumentException() {
        // ARRANGE
        when(flujoTrabajoService.getFlujoTrabajoById(validFlujoId)).thenReturn(validFlujoDTO);
        when(rolService.getRolById(validRolId)).thenReturn(validRolDTO);
        when(flujoTrabajoPasoDAO.existsByFlujoTrabajoIdAndOrdenPaso(validFlujoId, 1)).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.createPaso(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un paso con el orden 1 en este flujo de trabajo");

        verify(flujoTrabajoPasoDAO, never()).save(any());
    }

    // ==================== getPasoById ====================

    @Test
    @DisplayName("GET by ID - paso existente debe retornar FlujoTrabajoPasoDTO")
    void getPasoById_ExistingId_ShouldReturnPasoDTO() {
        // ARRANGE
        when(flujoTrabajoPasoDAO.findById(validPasoId)).thenReturn(Optional.of(validPasoDTO));

        // ACT
        FlujoTrabajoPasoDTO result = flujoTrabajoPasoService.getPasoById(validPasoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validPasoId);

        verify(flujoTrabajoPasoDAO, times(1)).findById(validPasoId);
    }

    @Test
    @DisplayName("GET by ID - paso inexistente debe lanzar RuntimeException")
    void getPasoById_NonExistentId_ShouldThrowRuntimeException() {
        // ARRANGE
        Long nonExistentId = 999L;
        when(flujoTrabajoPasoDAO.findById(nonExistentId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.getPasoById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Paso del flujo no encontrado con ID: " + nonExistentId);

        verify(flujoTrabajoPasoDAO, times(1)).findById(nonExistentId);
    }

    // ==================== getPrimerPaso ====================

    @Test
    @DisplayName("GET primer paso - flujo con pasos configurados debe retornar el paso con orden 1")
    void getPrimerPaso_FlujoWithSteps_ShouldReturnFirstStep() {
        // ARRANGE
        FlujoTrabajoPasoDTO primerPaso = mock(FlujoTrabajoPasoDTO.class);
        when(primerPaso.getOrdenPaso()).thenReturn(1);

        when(flujoTrabajoService.getFlujoTrabajoById(validFlujoId)).thenReturn(validFlujoDTO);
        when(flujoTrabajoPasoDAO.findPrimerPaso(validFlujoId)).thenReturn(Optional.of(primerPaso));

        // ACT
        FlujoTrabajoPasoDTO result = flujoTrabajoPasoService.getPrimerPaso(validFlujoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getOrdenPaso()).isEqualTo(1);

        verify(flujoTrabajoService, times(1)).getFlujoTrabajoById(validFlujoId);
        verify(flujoTrabajoPasoDAO, times(1)).findPrimerPaso(validFlujoId);
    }

    @Test
    @DisplayName("GET primer paso - flujo sin pasos debe lanzar IllegalStateException")
    void getPrimerPaso_FlujoWithoutSteps_ShouldThrowIllegalStateException() {
        // ARRANGE
        when(flujoTrabajoService.getFlujoTrabajoById(validFlujoId)).thenReturn(validFlujoDTO);
        when(flujoTrabajoPasoDAO.findPrimerPaso(validFlujoId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.getPrimerPaso(validFlujoId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("flujo de trabajo no tiene pasos configurados");

        verify(flujoTrabajoPasoDAO, times(1)).findPrimerPaso(validFlujoId);
    }

    // ==================== getSiguientePaso ====================

    @Test
    @DisplayName("GET siguiente paso - orden válido con paso siguiente disponible debe retornar PasoDTO")
    void getSiguientePaso_OrderWithNextStep_ShouldReturnNextStep() {
        // ARRANGE
        FlujoTrabajoPasoDTO siguientePaso = mock(FlujoTrabajoPasoDTO.class);
        when(siguientePaso.getOrdenPaso()).thenReturn(2);

        when(flujoTrabajoPasoDAO.findSiguientePaso(validFlujoId, 1)).thenReturn(Optional.of(siguientePaso));

        // ACT
        FlujoTrabajoPasoDTO result = flujoTrabajoPasoService.getSiguientePaso(validFlujoId, 1);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getOrdenPaso()).isEqualTo(2);

        verify(flujoTrabajoPasoDAO, times(1)).findSiguientePaso(validFlujoId, 1);
    }

    @Test
    @DisplayName("GET siguiente paso - paso actual es el último debe retornar null (fin de flujo)")
    void getSiguientePaso_LastStep_ShouldReturnNull() {
        // ARRANGE - el DAO retorna Optional.empty() cuando no hay más pasos
        when(flujoTrabajoPasoDAO.findSiguientePaso(validFlujoId, 3)).thenReturn(Optional.empty());

        // ACT
        FlujoTrabajoPasoDTO result = flujoTrabajoPasoService.getSiguientePaso(validFlujoId, 3);

        // ASSERT - null indica fin del flujo (diseño intencional del servicio)
        assertThat(result).isNull();

        verify(flujoTrabajoPasoDAO, times(1)).findSiguientePaso(validFlujoId, 3);
    }

    @Test
    @DisplayName("GET siguiente paso - ordenActual null debe lanzar IllegalArgumentException")
    void getSiguientePaso_NullOrden_ShouldThrowIllegalArgumentException() {
        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.getSiguientePaso(validFlujoId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orden actual del paso debe ser un número positivo");

        verify(flujoTrabajoPasoDAO, never()).findSiguientePaso(anyLong(), anyInt());
    }

    @Test
    @DisplayName("GET siguiente paso - ordenActual negativo debe lanzar IllegalArgumentException")
    void getSiguientePaso_NegativeOrden_ShouldThrowIllegalArgumentException() {
        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.getSiguientePaso(validFlujoId, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orden actual del paso debe ser un número positivo");

        verify(flujoTrabajoPasoDAO, never()).findSiguientePaso(anyLong(), anyInt());
    }

    // ==================== getPasosByFlujoTrabajo ====================

    @Test
    @DisplayName("GET pasos by flujo - flujo con pasos debe retornar lista ordenada")
    void getPasosByFlujoTrabajo_FlujoWithSteps_ShouldReturnOrderedList() {
        // ARRANGE
        FlujoTrabajoPasoDTO paso1 = mock(FlujoTrabajoPasoDTO.class);
        FlujoTrabajoPasoDTO paso2 = mock(FlujoTrabajoPasoDTO.class);
        when(paso1.getOrdenPaso()).thenReturn(1);
        when(paso2.getOrdenPaso()).thenReturn(2);

        when(flujoTrabajoService.getFlujoTrabajoById(validFlujoId)).thenReturn(validFlujoDTO);
        when(flujoTrabajoPasoDAO.findByFlujoTrabajoIdOrdenados(validFlujoId))
                .thenReturn(Arrays.asList(paso1, paso2));

        // ACT
        List<FlujoTrabajoPasoDTO> result = flujoTrabajoPasoService.getPasosByFlujoTrabajo(validFlujoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        // El servicio retorna en el orden que el DAO proporciona (ya ordenado)
        assertThat(result.get(0).getOrdenPaso()).isEqualTo(1);
        assertThat(result.get(1).getOrdenPaso()).isEqualTo(2);

        verify(flujoTrabajoService, times(1)).getFlujoTrabajoById(validFlujoId);
        verify(flujoTrabajoPasoDAO, times(1)).findByFlujoTrabajoIdOrdenados(validFlujoId);
    }

    // ==================== updatePaso ====================

    @Test
    @DisplayName("UPDATE - cambio de orden libre debe actualizar y retornar PasoDTO")
    void updatePaso_ValidDataNoOrderConflict_ShouldReturnUpdatedPaso() {
        // ARRANGE
        FlujoTrabajoPasoUpdateDTO updateDTO = new FlujoTrabajoPasoUpdateDTO();
        updateDTO.setNombre("Revisión final");
        // No se cambia el orden, por lo que no se verifica duplicado

        FlujoTrabajoPasoDTO updatedPaso = mock(FlujoTrabajoPasoDTO.class);
        when(updatedPaso.getId()).thenReturn(validPasoId);

        when(flujoTrabajoPasoDAO.findById(validPasoId)).thenReturn(Optional.of(validPasoDTO));
        when(flujoTrabajoPasoDAO.update(eq(validPasoId), any(FlujoTrabajoPasoUpdateDTO.class)))
                .thenReturn(Optional.of(updatedPaso));

        // ACT
        FlujoTrabajoPasoDTO result = flujoTrabajoPasoService.updatePaso(validPasoId, updateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validPasoId);

        verify(flujoTrabajoPasoDAO, times(1)).update(eq(validPasoId), any(FlujoTrabajoPasoUpdateDTO.class));
    }

    @Test
    @DisplayName("UPDATE - cambio de orden a uno ya ocupado debe lanzar IllegalArgumentException sin actualizar")
    void updatePaso_NewOrderAlreadyTaken_ShouldThrowIllegalArgumentException() {
        // ARRANGE
        FlujoTrabajoPasoUpdateDTO updateDTO = new FlujoTrabajoPasoUpdateDTO();
        updateDTO.setOrdenPaso(2); // El paso actual tiene orden=1; intentamos cambiar a 2 que ya existe

        when(flujoTrabajoPasoDAO.findById(validPasoId)).thenReturn(Optional.of(validPasoDTO));
        // El validPasoDTO tiene ordenPaso=1 (configurado en setUp); el nuevo orden 2 está ocupado
        when(flujoTrabajoPasoDAO.existsByFlujoTrabajoIdAndOrdenPaso(validFlujoId, 2)).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.updatePaso(validPasoId, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un paso con el orden 2 en este flujo");

        verify(flujoTrabajoPasoDAO, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("UPDATE - paso inexistente debe lanzar RuntimeException sin actualizar")
    void updatePaso_NonExistentPaso_ShouldThrowRuntimeException() {
        // ARRANGE
        Long nonExistentId = 999L;
        FlujoTrabajoPasoUpdateDTO updateDTO = new FlujoTrabajoPasoUpdateDTO();
        when(flujoTrabajoPasoDAO.findById(nonExistentId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.updatePaso(nonExistentId, updateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Paso del flujo no encontrado con ID: " + nonExistentId);

        verify(flujoTrabajoPasoDAO, never()).update(anyLong(), any());
    }

    // ==================== deletePaso ====================

    @Test
    @DisplayName("DELETE - paso existente debe eliminarse sin lanzar excepción")
    void deletePaso_ExistingPaso_ShouldCompleteWithoutException() {
        // ARRANGE
        when(flujoTrabajoPasoDAO.findById(validPasoId)).thenReturn(Optional.of(validPasoDTO));
        when(flujoTrabajoPasoDAO.deleteById(validPasoId)).thenReturn(true);

        // ACT & ASSERT
        assertThatCode(() -> flujoTrabajoPasoService.deletePaso(validPasoId))
                .doesNotThrowAnyException();

        verify(flujoTrabajoPasoDAO, times(1)).findById(validPasoId);
        verify(flujoTrabajoPasoDAO, times(1)).deleteById(validPasoId);
    }

    @Test
    @DisplayName("DELETE - paso inexistente debe lanzar RuntimeException sin intentar borrar")
    void deletePaso_NonExistentPaso_ShouldThrowRuntimeException() {
        // ARRANGE
        Long nonExistentId = 999L;
        when(flujoTrabajoPasoDAO.findById(nonExistentId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoPasoService.deletePaso(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Paso del flujo no encontrado con ID: " + nonExistentId);

        verify(flujoTrabajoPasoDAO, never()).deleteById(anyLong());
    }
}
