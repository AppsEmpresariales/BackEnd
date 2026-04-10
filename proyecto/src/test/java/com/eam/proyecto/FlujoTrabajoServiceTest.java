package com.eam.proyecto.unit.service;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoUpdateDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionDTO;
import com.eam.proyecto.businessLayer.dto.TipoDocumentoDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.TipoDocumentoService;
import com.eam.proyecto.businessLayer.service.impl.FlujoTrabajoServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.FlujoTrabajoDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests para FlujoTrabajoServiceImpl
 *
 * Cubre:
 *  - createFlujoTrabajo      : validaciones, verificación de organización y tipo documental,
 *                              regla crítica de un solo flujo activo por tipo documental (RF32)
 *  - getFlujoTrabajoById     : happy-path y not-found
 *  - getFlujoActivoByOrganizacionAndTipoDocumento : flujo configurado y sin configurar (RF31)
 *  - getFlujosActivosByOrganizacion : lista de flujos activos
 *  - updateFlujoTrabajo      : happy-path y not-found
 *  - deleteFlujoTrabajo      : éxito, fallo en DAO y not-found
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlujoTrabajoService - Unit Tests")
class FlujoTrabajoServiceTest {

    // ─── Dependencias mockeadas ────────────────────────────────────────────────
    @Mock
    private FlujoTrabajoDAO flujoTrabajoDAO;

    @Mock
    private OrganizacionService organizacionService;

    @Mock
    private TipoDocumentoService tipoDocumentoService;

    // ─── Sistema bajo prueba (SUT) ─────────────────────────────────────────────
    @InjectMocks
    private FlujoTrabajoServiceImpl flujoTrabajoService;

    // ─── Datos de prueba reutilizables ─────────────────────────────────────────
    private FlujoTrabajoCreateDTO validCreateDTO;
    private FlujoTrabajoDTO validFlujoDTO;
    private Long validOrganizacionNit;
    private Long validTipoDocumentoId;
    private Long validFlujoId;

    @BeforeEach
    void setUp() {
        validOrganizacionNit  = 900123456L;
        validTipoDocumentoId  = 10L;
        validFlujoId          = 1L;

        validCreateDTO = new FlujoTrabajoCreateDTO();
        validCreateDTO.setNombre("Flujo Aprobación Contratos");
        validCreateDTO.setDescripcion("Flujo de dos niveles para contratos");
        validCreateDTO.setOrganizacionNit(validOrganizacionNit);
        validCreateDTO.setTipoDocumentoId(validTipoDocumentoId);

        validFlujoDTO = new FlujoTrabajoDTO();
        validFlujoDTO.setId(validFlujoId);
        validFlujoDTO.setNombre("Flujo Aprobación Contratos");
        validFlujoDTO.setDescripcion("Flujo de dos niveles para contratos");
        validFlujoDTO.setOrganizacionNit(validOrganizacionNit);
        validFlujoDTO.setTipoDocumentoId(validTipoDocumentoId);
        validFlujoDTO.setActivo(true);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("CREATE - Datos válidos deben retornar el flujo creado")
    void createFlujoTrabajo_DatosValidos_DebeRetornarFlujoCreado() {
        // ARRANGE
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(new OrganizacionDTO());
        when(tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(
                validTipoDocumentoId, validOrganizacionNit))
                .thenReturn(new TipoDocumentoDTO());
        when(flujoTrabajoDAO.existeActivoPorTipoDocumental(
                validOrganizacionNit, validTipoDocumentoId))
                .thenReturn(false);
        when(flujoTrabajoDAO.save(any(FlujoTrabajoCreateDTO.class)))
                .thenReturn(validFlujoDTO);

        // ACT
        FlujoTrabajoDTO result = flujoTrabajoService.createFlujoTrabajo(validCreateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validFlujoId);
        assertThat(result.getNombre()).isEqualTo("Flujo Aprobación Contratos");
        assertThat(result.getActivo()).isTrue();

        verify(organizacionService, times(1)).getOrganizacionActivaByNit(validOrganizacionNit);
        verify(tipoDocumentoService, times(1))
                .getTipoDocumentoByIdAndOrganizacion(validTipoDocumentoId, validOrganizacionNit);
        verify(flujoTrabajoDAO, times(1)).save(any(FlujoTrabajoCreateDTO.class));
    }

    @Test
    @DisplayName("CREATE - Nombre null debe lanzar IllegalArgumentException")
    void createFlujoTrabajo_NombreNull_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setNombre(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService.createFlujoTrabajo(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre del flujo de trabajo es obligatorio");

        verify(flujoTrabajoDAO, never()).save(any());
        verify(organizacionService, never()).getOrganizacionActivaByNit(anyLong());
        verify(tipoDocumentoService, never())
                .getTipoDocumentoByIdAndOrganizacion(anyLong(), anyLong());
    }

    @Test
    @DisplayName("CREATE - Nombre vacío debe lanzar IllegalArgumentException")
    void createFlujoTrabajo_NombreVacio_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setNombre("   ");

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService.createFlujoTrabajo(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre del flujo de trabajo es obligatorio");

        verify(flujoTrabajoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - NIT null debe lanzar IllegalArgumentException")
    void createFlujoTrabajo_NitNull_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setOrganizacionNit(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService.createFlujoTrabajo(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NIT de la organización es obligatorio");

        verify(flujoTrabajoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - TipoDocumentoId null debe lanzar IllegalArgumentException")
    void createFlujoTrabajo_TipoDocumentoNull_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setTipoDocumentoId(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService.createFlujoTrabajo(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo documental es obligatorio");

        verify(flujoTrabajoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Organización inexistente debe propagar RuntimeException del servicio")
    void createFlujoTrabajo_OrganizacionInexistente_DebePropararExcepcion() {
        // ARRANGE
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenThrow(new RuntimeException("Organización no encontrada o inactiva"));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService.createFlujoTrabajo(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Organización no encontrada");

        verify(flujoTrabajoDAO, never()).save(any());
        verify(tipoDocumentoService, never())
                .getTipoDocumentoByIdAndOrganizacion(anyLong(), anyLong());
    }

    @Test
    @DisplayName("CREATE - Tipo documental inexistente en el tenant debe propagar RuntimeException")
    void createFlujoTrabajo_TipoDocumentoInexistente_DebePropagarExcepcion() {
        // ARRANGE
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(new OrganizacionDTO());
        when(tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(
                validTipoDocumentoId, validOrganizacionNit))
                .thenThrow(new RuntimeException("Tipo documental no encontrado en esta organización"));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService.createFlujoTrabajo(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tipo documental no encontrado");

        verify(flujoTrabajoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Flujo activo duplicado para el mismo tipo documental debe lanzar IllegalStateException (RF32)")
    void createFlujoTrabajo_FlujoActivoYaExiste_DebeLanzarIllegalStateException() {
        // ARRANGE
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(new OrganizacionDTO());
        when(tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(
                validTipoDocumentoId, validOrganizacionNit))
                .thenReturn(new TipoDocumentoDTO());
        when(flujoTrabajoDAO.existeActivoPorTipoDocumental(
                validOrganizacionNit, validTipoDocumentoId))
                .thenReturn(true);        // ya hay un flujo activo

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService.createFlujoTrabajo(validCreateDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un flujo activo para este tipo documental");

        verify(flujoTrabajoDAO, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET por ID - Flujo existente debe retornarse correctamente")
    void getFlujoTrabajoById_Existente_DebeRetornarDTO() {
        // ARRANGE
        when(flujoTrabajoDAO.findById(validFlujoId))
                .thenReturn(Optional.of(validFlujoDTO));

        // ACT
        FlujoTrabajoDTO result = flujoTrabajoService.getFlujoTrabajoById(validFlujoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validFlujoId);
        assertThat(result.getNombre()).isEqualTo("Flujo Aprobación Contratos");

        verify(flujoTrabajoDAO, times(1)).findById(validFlujoId);
    }

    @Test
    @DisplayName("GET por ID - Flujo inexistente debe lanzar RuntimeException")
    void getFlujoTrabajoById_Inexistente_DebeLanzarRuntimeException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(flujoTrabajoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService.getFlujoTrabajoById(idInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flujo de trabajo no encontrado con ID: " + idInexistente);
    }

    @Test
    @DisplayName("GET flujo activo por organización y tipo documental - Configurado debe retornarlo (RF31)")
    void getFlujoActivoByOrganizacionAndTipoDocumento_Configurado_DebeRetornarFlujo() {
        // ARRANGE
        when(flujoTrabajoDAO.findActivoByOrganizacionNitAndTipoDocumentoId(
                validOrganizacionNit, validTipoDocumentoId))
                .thenReturn(Optional.of(validFlujoDTO));

        // ACT
        FlujoTrabajoDTO result = flujoTrabajoService
                .getFlujoActivoByOrganizacionAndTipoDocumento(validOrganizacionNit, validTipoDocumentoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getActivo()).isTrue();
        assertThat(result.getTipoDocumentoId()).isEqualTo(validTipoDocumentoId);

        verify(flujoTrabajoDAO, times(1))
                .findActivoByOrganizacionNitAndTipoDocumentoId(validOrganizacionNit, validTipoDocumentoId);
    }

    @Test
    @DisplayName("GET flujo activo - Sin flujo configurado debe lanzar IllegalStateException (RF31)")
    void getFlujoActivoByOrganizacionAndTipoDocumento_SinFlujo_DebeLanzarIllegalStateException() {
        // ARRANGE
        when(flujoTrabajoDAO.findActivoByOrganizacionNitAndTipoDocumentoId(
                validOrganizacionNit, validTipoDocumentoId))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService
                .getFlujoActivoByOrganizacionAndTipoDocumento(validOrganizacionNit, validTipoDocumentoId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay flujo de trabajo activo configurado");
    }

    @Test
    @DisplayName("GET flujos activos por organización - Debe retornar solo flujos activos")
    void getFlujosActivosByOrganizacion_DebeRetornarSoloActivos() {
        // ARRANGE
        FlujoTrabajoDTO flujo2 = new FlujoTrabajoDTO();
        flujo2.setId(2L);
        flujo2.setNombre("Flujo Facturas");
        flujo2.setActivo(true);

        when(organizacionService.getOrganizacionByNit(validOrganizacionNit))
                .thenReturn(new OrganizacionDTO());
        when(flujoTrabajoDAO.findActivosByOrganizacionNit(validOrganizacionNit))
                .thenReturn(List.of(validFlujoDTO, flujo2));

        // ACT
        List<FlujoTrabajoDTO> result =
                flujoTrabajoService.getFlujosActivosByOrganizacion(validOrganizacionNit);

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result).extracting("nombre")
                .containsExactlyInAnyOrder("Flujo Aprobación Contratos", "Flujo Facturas");

        verify(flujoTrabajoDAO, times(1)).findActivosByOrganizacionNit(validOrganizacionNit);
    }

    @Test
    @DisplayName("GET todos - Organización sin flujos debe retornar lista vacía")
    void getAllFlujosByOrganizacion_SinFlujos_DebeRetornarListaVacia() {
        // ARRANGE
        when(organizacionService.getOrganizacionByNit(validOrganizacionNit))
                .thenReturn(new OrganizacionDTO());
        when(flujoTrabajoDAO.findAllByOrganizacionNit(validOrganizacionNit))
                .thenReturn(List.of());

        // ACT
        List<FlujoTrabajoDTO> result =
                flujoTrabajoService.getAllFlujosByOrganizacion(validOrganizacionNit);

        // ASSERT
        assertThat(result).isEmpty();
        verify(flujoTrabajoDAO, times(1)).findAllByOrganizacionNit(validOrganizacionNit);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("UPDATE - Datos válidos deben retornar el flujo actualizado")
    void updateFlujoTrabajo_DatosValidos_DebeRetornarFlujoActualizado() {
        // ARRANGE
        FlujoTrabajoUpdateDTO updateDTO = new FlujoTrabajoUpdateDTO();
        updateDTO.setNombre("Flujo Actualizado");
        updateDTO.setDescripcion("Descripción actualizada");

        FlujoTrabajoDTO flujoActualizado = new FlujoTrabajoDTO();
        flujoActualizado.setId(validFlujoId);
        flujoActualizado.setNombre("Flujo Actualizado");
        flujoActualizado.setDescripcion("Descripción actualizada");
        flujoActualizado.setActivo(true);

        when(flujoTrabajoDAO.findById(validFlujoId))
                .thenReturn(Optional.of(validFlujoDTO));
        when(flujoTrabajoDAO.update(eq(validFlujoId), any(FlujoTrabajoUpdateDTO.class)))
                .thenReturn(Optional.of(flujoActualizado));

        // ACT
        FlujoTrabajoDTO result = flujoTrabajoService.updateFlujoTrabajo(validFlujoId, updateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getNombre()).isEqualTo("Flujo Actualizado");
        assertThat(result.getDescripcion()).isEqualTo("Descripción actualizada");

        verify(flujoTrabajoDAO, times(1)).update(eq(validFlujoId), any(FlujoTrabajoUpdateDTO.class));
    }

    @Test
    @DisplayName("UPDATE - Flujo inexistente debe lanzar RuntimeException")
    void updateFlujoTrabajo_Inexistente_DebeLanzarRuntimeException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(flujoTrabajoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() ->
                flujoTrabajoService.updateFlujoTrabajo(idInexistente, new FlujoTrabajoUpdateDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Flujo de trabajo no encontrado con ID: " + idInexistente);

        verify(flujoTrabajoDAO, never()).update(anyLong(), any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE - Flujo existente debe eliminarse sin lanzar excepción")
    void deleteFlujoTrabajo_Existente_DebeEliminarseCorrectamente() {
        // ARRANGE
        when(flujoTrabajoDAO.findById(validFlujoId))
                .thenReturn(Optional.of(validFlujoDTO));
        when(flujoTrabajoDAO.deleteById(validFlujoId))
                .thenReturn(true);

        // ACT & ASSERT
        assertThatCode(() -> flujoTrabajoService.deleteFlujoTrabajo(validFlujoId))
                .doesNotThrowAnyException();

        verify(flujoTrabajoDAO, times(1)).findById(validFlujoId);
        verify(flujoTrabajoDAO, times(1)).deleteById(validFlujoId);
    }

    @Test
    @DisplayName("DELETE - Error en DAO debe lanzar RuntimeException")
    void deleteFlujoTrabajo_FalloEnDAO_DebeLanzarRuntimeException() {
        // ARRANGE
        when(flujoTrabajoDAO.findById(validFlujoId))
                .thenReturn(Optional.of(validFlujoDTO));
        when(flujoTrabajoDAO.deleteById(validFlujoId))
                .thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService.deleteFlujoTrabajo(validFlujoId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al eliminar flujo de trabajo ID: " + validFlujoId);
    }

    @Test
    @DisplayName("DELETE - Flujo inexistente no debe llamar al delete del DAO")
    void deleteFlujoTrabajo_Inexistente_NoDebeLlamarDeleteEnDAO() {
        // ARRANGE
        Long idInexistente = 999L;
        when(flujoTrabajoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoService.deleteFlujoTrabajo(idInexistente))
                .isInstanceOf(RuntimeException.class);

        verify(flujoTrabajoDAO, never()).deleteById(anyLong());
    }
}
