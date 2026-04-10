package com.eam.proyecto.unit.service;

import com.eam.proyecto.businessLayer.dto.OrganizacionDTO;
import com.eam.proyecto.businessLayer.dto.TipoDocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.TipoDocumentoDTO;
import com.eam.proyecto.businessLayer.dto.TipoDocumentoUpdateDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.impl.TipoDocumentoServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.TipoDocumentoDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests para TipoDocumentoServiceImpl
 *
 * Cubre:
 *  - createTipoDocumento     : validaciones, unicidad de nombre en tenant, active=true automático (RF24)
 *  - getTipoDocumentoById    : happy-path y not-found
 *  - getTipoDocumentoByIdAndOrganizacion : happy-path y not-found (RF10)
 *  - getTiposActivosByOrganizacion       : lista filtrada
 *  - updateTipoDocumento     : happy-path, nombre duplicado, not-found
 *  - desactivarTipoDocumento : éxito, ya inactivo, not-found (RF26)
 *  - activarTipoDocumento    : éxito, ya activo (RF42)
 *  - deleteTipoDocumento     : éxito y fallo en DAO
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TipoDocumentoService - Unit Tests")
class TipoDocumentoServiceTest {

    // ─── Dependencias mockeadas ────────────────────────────────────────────────
    @Mock
    private TipoDocumentoDAO tipoDocumentoDAO;

    @Mock
    private OrganizacionService organizacionService;

    // ─── Sistema bajo prueba (SUT) ─────────────────────────────────────────────
    @InjectMocks
    private TipoDocumentoServiceImpl tipoDocumentoService;

    // ─── Datos de prueba reutilizables ─────────────────────────────────────────
    private TipoDocumentoCreateDTO validCreateDTO;
    private TipoDocumentoDTO validTipoDTO;
    private Long validOrganizacionNit;
    private Long validTipoId;

    @BeforeEach
    void setUp() {
        validOrganizacionNit = 900123456L;
        validTipoId          = 1L;

        validCreateDTO = new TipoDocumentoCreateDTO();
        validCreateDTO.setNombre("Contrato");
        validCreateDTO.setDescripcion("Tipo para contratos empresariales");
        validCreateDTO.setOrganizacionNit(validOrganizacionNit);

        validTipoDTO = new TipoDocumentoDTO();
        validTipoDTO.setId(validTipoId);
        validTipoDTO.setNombre("Contrato");
        validTipoDTO.setDescripcion("Tipo para contratos empresariales");
        validTipoDTO.setActive(true);
        validTipoDTO.setOrganizacionNit(validOrganizacionNit);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("CREATE - Datos válidos deben retornar tipo documental creado con active=true")
    void createTipoDocumento_DatosValidos_DebeRetornarTipoConActiveTrue() {
        // ARRANGE
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(new OrganizacionDTO());
        when(tipoDocumentoDAO.existsByNombreAndOrganizacionNit(
                validCreateDTO.getNombre(), validOrganizacionNit))
                .thenReturn(false);
        when(tipoDocumentoDAO.save(any(TipoDocumentoCreateDTO.class)))
                .thenReturn(validTipoDTO);

        // ACT
        TipoDocumentoDTO result = tipoDocumentoService.createTipoDocumento(validCreateDTO);

        // ASSERT - estado
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validTipoId);
        assertThat(result.getNombre()).isEqualTo("Contrato");
        assertThat(result.getActive()).isTrue();

        // ASSERT - el service debe haber marcado active=true antes de persistir
        ArgumentCaptor<TipoDocumentoCreateDTO> captor =
                ArgumentCaptor.forClass(TipoDocumentoCreateDTO.class);
        verify(tipoDocumentoDAO, times(1)).save(captor.capture());
        assertThat(captor.getValue().getActive()).isTrue();

        verify(organizacionService, times(1)).getOrganizacionActivaByNit(validOrganizacionNit);
    }

    @Test
    @DisplayName("CREATE - Nombre null debe lanzar IllegalArgumentException")
    void createTipoDocumento_NombreNull_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setNombre(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.createTipoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre del tipo documental es obligatorio");

        verify(tipoDocumentoDAO, never()).save(any());
        verify(organizacionService, never()).getOrganizacionActivaByNit(anyLong());
    }

    @Test
    @DisplayName("CREATE - Nombre vacío debe lanzar IllegalArgumentException")
    void createTipoDocumento_NombreVacio_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setNombre("  ");

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.createTipoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre del tipo documental es obligatorio");

        verify(tipoDocumentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Nombre mayor a 150 caracteres debe lanzar IllegalArgumentException")
    void createTipoDocumento_NombreExcede150Caracteres_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setNombre("T".repeat(151));

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.createTipoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre no puede exceder 150 caracteres");

        verify(tipoDocumentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - NIT null debe lanzar IllegalArgumentException")
    void createTipoDocumento_NitNull_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setOrganizacionNit(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.createTipoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NIT de la organización es obligatorio");

        verify(tipoDocumentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Nombre duplicado en el mismo tenant debe lanzar IllegalArgumentException")
    void createTipoDocumento_NombreDuplicadoEnTenant_DebeLanzarExcepcion() {
        // ARRANGE
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(new OrganizacionDTO());
        when(tipoDocumentoDAO.existsByNombreAndOrganizacionNit(
                validCreateDTO.getNombre(), validOrganizacionNit))
                .thenReturn(true);          // ya existe

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.createTipoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un tipo documental con el nombre");

        verify(tipoDocumentoDAO, never()).save(any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET por ID - Tipo existente debe retornarse correctamente")
    void getTipoDocumentoById_Existente_DebeRetornarDTO() {
        // ARRANGE
        when(tipoDocumentoDAO.findById(validTipoId))
                .thenReturn(Optional.of(validTipoDTO));

        // ACT
        TipoDocumentoDTO result = tipoDocumentoService.getTipoDocumentoById(validTipoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validTipoId);
        assertThat(result.getNombre()).isEqualTo("Contrato");

        verify(tipoDocumentoDAO, times(1)).findById(validTipoId);
    }

    @Test
    @DisplayName("GET por ID - Tipo inexistente debe lanzar RuntimeException")
    void getTipoDocumentoById_Inexistente_DebeLanzarRuntimeException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(tipoDocumentoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.getTipoDocumentoById(idInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tipo documental no encontrado con ID: " + idInexistente);
    }

    @Test
    @DisplayName("GET por ID y tenant - Tipo del tenant correcto debe retornarse (RF10)")
    void getTipoDocumentoByIdAndOrganizacion_TenantCorrecto_DebeRetornarDTO() {
        // ARRANGE
        when(tipoDocumentoDAO.findByIdAndOrganizacionNit(validTipoId, validOrganizacionNit))
                .thenReturn(Optional.of(validTipoDTO));

        // ACT
        TipoDocumentoDTO result = tipoDocumentoService
                .getTipoDocumentoByIdAndOrganizacion(validTipoId, validOrganizacionNit);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getOrganizacionNit()).isEqualTo(validOrganizacionNit);

        verify(tipoDocumentoDAO, times(1))
                .findByIdAndOrganizacionNit(validTipoId, validOrganizacionNit);
    }

    @Test
    @DisplayName("GET por ID y tenant - Tipo de otro tenant debe lanzar RuntimeException (RF10)")
    void getTipoDocumentoByIdAndOrganizacion_TenantEquivocado_DebeLanzarRuntimeException() {
        // ARRANGE
        Long otroNit = 111111111L;
        when(tipoDocumentoDAO.findByIdAndOrganizacionNit(validTipoId, otroNit))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() ->
                tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(validTipoId, otroNit))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado en esta organización");
    }

    @Test
    @DisplayName("GET activos por organización - Debe retornar solo los tipos activos")
    void getTiposActivosByOrganizacion_DebeRetornarSoloActivos() {
        // ARRANGE
        TipoDocumentoDTO tipoActivo2 = new TipoDocumentoDTO();
        tipoActivo2.setId(2L);
        tipoActivo2.setNombre("Factura");
        tipoActivo2.setActive(true);

        when(organizacionService.getOrganizacionByNit(validOrganizacionNit))
                .thenReturn(new OrganizacionDTO());
        when(tipoDocumentoDAO.findActivosByOrganizacionNit(validOrganizacionNit))
                .thenReturn(List.of(validTipoDTO, tipoActivo2));

        // ACT
        List<TipoDocumentoDTO> result =
                tipoDocumentoService.getTiposActivosByOrganizacion(validOrganizacionNit);

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result).extracting("nombre")
                .containsExactlyInAnyOrder("Contrato", "Factura");

        verify(tipoDocumentoDAO, times(1)).findActivosByOrganizacionNit(validOrganizacionNit);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("UPDATE - Datos válidos deben retornar el tipo actualizado")
    void updateTipoDocumento_DatosValidos_DebeRetornarTipoActualizado() {
        // ARRANGE
        TipoDocumentoUpdateDTO updateDTO = new TipoDocumentoUpdateDTO();
        updateDTO.setNombre("Contrato Actualizado");
        updateDTO.setDescripcion("Nueva descripción");

        TipoDocumentoDTO tipoActualizado = new TipoDocumentoDTO();
        tipoActualizado.setId(validTipoId);
        tipoActualizado.setNombre("Contrato Actualizado");
        tipoActualizado.setDescripcion("Nueva descripción");
        tipoActualizado.setActive(true);
        tipoActualizado.setOrganizacionNit(validOrganizacionNit);

        when(tipoDocumentoDAO.findById(validTipoId))
                .thenReturn(Optional.of(validTipoDTO));
        when(tipoDocumentoDAO.existsByNombreAndOrganizacionNit(
                updateDTO.getNombre(), validOrganizacionNit))
                .thenReturn(false);
        when(tipoDocumentoDAO.update(eq(validTipoId), any(TipoDocumentoUpdateDTO.class)))
                .thenReturn(Optional.of(tipoActualizado));

        // ACT
        TipoDocumentoDTO result = tipoDocumentoService.updateTipoDocumento(validTipoId, updateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getNombre()).isEqualTo("Contrato Actualizado");
        assertThat(result.getDescripcion()).isEqualTo("Nueva descripción");

        verify(tipoDocumentoDAO, times(1)).update(eq(validTipoId), any(TipoDocumentoUpdateDTO.class));
    }

    @Test
    @DisplayName("UPDATE - Nuevo nombre duplicado en el tenant debe lanzar IllegalArgumentException")
    void updateTipoDocumento_NombreDuplicadoEnTenant_DebeLanzarExcepcion() {
        // ARRANGE
        TipoDocumentoUpdateDTO updateDTO = new TipoDocumentoUpdateDTO();
        updateDTO.setNombre("Factura");   // ya existe en el mismo tenant

        when(tipoDocumentoDAO.findById(validTipoId))
                .thenReturn(Optional.of(validTipoDTO));
        when(tipoDocumentoDAO.existsByNombreAndOrganizacionNit("Factura", validOrganizacionNit))
                .thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.updateTipoDocumento(validTipoId, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un tipo documental con el nombre");

        verify(tipoDocumentoDAO, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("UPDATE - Tipo inexistente debe lanzar RuntimeException")
    void updateTipoDocumento_Inexistente_DebeLanzarRuntimeException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(tipoDocumentoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() ->
                tipoDocumentoService.updateTipoDocumento(idInexistente, new TipoDocumentoUpdateDTO()))
                .isInstanceOf(RuntimeException.class);

        verify(tipoDocumentoDAO, never()).update(anyLong(), any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DESACTIVAR / ACTIVAR  (RF26 / RF42)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DESACTIVAR - Tipo activo debe desactivarse correctamente (RF26)")
    void desactivarTipoDocumento_Activo_DebeDesactivarseCorrectamente() {
        // ARRANGE
        TipoDocumentoDTO tipoDesactivado = new TipoDocumentoDTO();
        tipoDesactivado.setId(validTipoId);
        tipoDesactivado.setNombre("Contrato");
        tipoDesactivado.setActive(false);

        when(tipoDocumentoDAO.findById(validTipoId))
                .thenReturn(Optional.of(validTipoDTO));       // active=true
        when(tipoDocumentoDAO.desactivar(validTipoId))
                .thenReturn(Optional.of(tipoDesactivado));

        // ACT
        TipoDocumentoDTO result = tipoDocumentoService.desactivarTipoDocumento(validTipoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getActive()).isFalse();

        verify(tipoDocumentoDAO, times(1)).desactivar(validTipoId);
    }

    @Test
    @DisplayName("DESACTIVAR - Tipo ya inactivo debe lanzar IllegalStateException")
    void desactivarTipoDocumento_YaInactivo_DebeLanzarIllegalStateException() {
        // ARRANGE
        validTipoDTO.setActive(false);      // ya está inactivo

        when(tipoDocumentoDAO.findById(validTipoId))
                .thenReturn(Optional.of(validTipoDTO));

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.desactivarTipoDocumento(validTipoId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya se encuentra inactivo");

        verify(tipoDocumentoDAO, never()).desactivar(anyLong());
    }

    @Test
    @DisplayName("ACTIVAR - Tipo inactivo debe activarse correctamente (RF42)")
    void activarTipoDocumento_Inactivo_DebeActivarseCorrectamente() {
        // ARRANGE
        validTipoDTO.setActive(false);      // está inactivo

        TipoDocumentoDTO tipoActivado = new TipoDocumentoDTO();
        tipoActivado.setId(validTipoId);
        tipoActivado.setNombre("Contrato");
        tipoActivado.setActive(true);

        when(tipoDocumentoDAO.findById(validTipoId))
                .thenReturn(Optional.of(validTipoDTO));
        when(tipoDocumentoDAO.activar(validTipoId))
                .thenReturn(Optional.of(tipoActivado));

        // ACT
        TipoDocumentoDTO result = tipoDocumentoService.activarTipoDocumento(validTipoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getActive()).isTrue();

        verify(tipoDocumentoDAO, times(1)).activar(validTipoId);
    }

    @Test
    @DisplayName("ACTIVAR - Tipo ya activo debe lanzar IllegalStateException")
    void activarTipoDocumento_YaActivo_DebeLanzarIllegalStateException() {
        // ARRANGE — validTipoDTO ya tiene active=true por setUp()
        when(tipoDocumentoDAO.findById(validTipoId))
                .thenReturn(Optional.of(validTipoDTO));

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.activarTipoDocumento(validTipoId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya se encuentra activo");

        verify(tipoDocumentoDAO, never()).activar(anyLong());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE - Tipo existente debe eliminarse sin lanzar excepción")
    void deleteTipoDocumento_Existente_DebeEliminarseCorrectamente() {
        // ARRANGE
        when(tipoDocumentoDAO.findById(validTipoId))
                .thenReturn(Optional.of(validTipoDTO));
        when(tipoDocumentoDAO.deleteById(validTipoId))
                .thenReturn(true);

        // ACT & ASSERT
        assertThatCode(() -> tipoDocumentoService.deleteTipoDocumento(validTipoId))
                .doesNotThrowAnyException();

        verify(tipoDocumentoDAO, times(1)).findById(validTipoId);
        verify(tipoDocumentoDAO, times(1)).deleteById(validTipoId);
    }

    @Test
    @DisplayName("DELETE - Error en DAO debe lanzar RuntimeException")
    void deleteTipoDocumento_FalloEnDAO_DebeLanzarRuntimeException() {
        // ARRANGE
        when(tipoDocumentoDAO.findById(validTipoId))
                .thenReturn(Optional.of(validTipoDTO));
        when(tipoDocumentoDAO.deleteById(validTipoId))
                .thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.deleteTipoDocumento(validTipoId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al eliminar tipo documental ID: " + validTipoId);
    }

    @Test
    @DisplayName("DELETE - Tipo inexistente no debe llamar al delete del DAO")
    void deleteTipoDocumento_Inexistente_NoDebeLlamarDeleteEnDAO() {
        // ARRANGE
        Long idInexistente = 999L;
        when(tipoDocumentoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> tipoDocumentoService.deleteTipoDocumento(idInexistente))
                .isInstanceOf(RuntimeException.class);

        verify(tipoDocumentoDAO, never()).deleteById(anyLong());
    }
}
