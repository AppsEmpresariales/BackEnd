package com.eam.proyecto;

import com.eam.proyecto.businessLayer.dto.EstadoDocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoUpdateDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.impl.EstadoDocumentoServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.EstadoDocumentoDAO;
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
 * Unit Tests para EstadoDocumentoServiceImpl
 *
 * Cubre:
 *  - createEstadoDocumento  : validaciones de entrada, unicidad de nombre,
 *                             regla crítica de estado inicial único por tenant (RF31)
 *  - getEstadoDocumentoById : happy-path y not-found
 *  - getEstadoInicialByOrganizacion : configurado y sin configurar
 *  - getEstadosByOrganizacion       : lista completa
 *  - updateEstadoDocumento  : happy-path y not-found
 *  - deleteEstadoDocumento  : éxito y fallo en DAO
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EstadoDocumentoService - Unit Tests")
class EstadoDocumentoServiceTest {

    // ─── Dependencias mockeadas ────────────────────────────────────────────────
    @Mock
    private EstadoDocumentoDAO estadoDocumentoDAO;

    @Mock
    private OrganizacionService organizacionService;

    // ─── Sistema bajo prueba (SUT) ─────────────────────────────────────────────
    @InjectMocks
    private EstadoDocumentoServiceImpl estadoDocumentoService;

    // ─── Datos de prueba reutilizables ─────────────────────────────────────────
    private EstadoDocumentoCreateDTO validCreateDTO;
    private EstadoDocumentoDTO validEstadoDTO;
    private Long validOrganizacionNit;
    private Long validEstadoId;

    /**
     * Se ejecuta antes de cada test.
     * Inicializa datos base en estado limpio.
     */
    @BeforeEach
    void setUp() {
        validOrganizacionNit = 900123456L;
        validEstadoId        = 1L;

        // DTO de creación válido (sin estado inicial)
        validCreateDTO = new EstadoDocumentoCreateDTO();
        validCreateDTO.setNombre("En Revisión");
        validCreateDTO.setColor("#4CAF50");
        validCreateDTO.setEsInicial(false);
        validCreateDTO.setEsFinal(false);
        validCreateDTO.setOrganizacionNit(validOrganizacionNit);

        // DTO de respuesta válido (simula lo que devuelve el DAO)
        validEstadoDTO = new EstadoDocumentoDTO();
        validEstadoDTO.setId(validEstadoId);
        validEstadoDTO.setNombre("En Revisión");
        validEstadoDTO.setColor("#4CAF50");
        validEstadoDTO.setEsInicial(false);
        validEstadoDTO.setEsFinal(false);
        validEstadoDTO.setOrganizacionNit(validOrganizacionNit);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("CREATE - Datos válidos deben retornar el estado creado")
    void createEstadoDocumento_DatosValidos_DebeRetornarEstadoCreado() {
        // ARRANGE
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(new com.eam.proyecto.businessLayer.dto.OrganizacionDTO());
        when(estadoDocumentoDAO.findByNombreAndOrganizacionNit(
                validCreateDTO.getNombre(), validOrganizacionNit))
                .thenReturn(Optional.empty());
        when(estadoDocumentoDAO.save(any(EstadoDocumentoCreateDTO.class)))
                .thenReturn(validEstadoDTO);

        // ACT
        EstadoDocumentoDTO result = estadoDocumentoService.createEstadoDocumento(validCreateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validEstadoId);
        assertThat(result.getNombre()).isEqualTo("En Revisión");
        assertThat(result.getColor()).isEqualTo("#4CAF50");

        verify(organizacionService, times(1)).getOrganizacionActivaByNit(validOrganizacionNit);
        verify(estadoDocumentoDAO, times(1)).findByNombreAndOrganizacionNit(
                validCreateDTO.getNombre(), validOrganizacionNit);
        verify(estadoDocumentoDAO, times(1)).save(any(EstadoDocumentoCreateDTO.class));
    }

    @Test
    @DisplayName("CREATE - Nombre null debe lanzar IllegalArgumentException")
    void createEstadoDocumento_NombreNull_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setNombre(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> estadoDocumentoService.createEstadoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre del estado es obligatorio");

        verify(estadoDocumentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Nombre vacío debe lanzar IllegalArgumentException")
    void createEstadoDocumento_NombreVacio_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setNombre("   ");

        // ACT & ASSERT
        assertThatThrownBy(() -> estadoDocumentoService.createEstadoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre del estado es obligatorio");

        verify(estadoDocumentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Nombre mayor a 100 caracteres debe lanzar IllegalArgumentException")
    void createEstadoDocumento_NombreExcede100Caracteres_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setNombre("A".repeat(101));

        // ACT & ASSERT
        assertThatThrownBy(() -> estadoDocumentoService.createEstadoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre no puede exceder 100 caracteres");

        verify(estadoDocumentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - NIT null debe lanzar IllegalArgumentException")
    void createEstadoDocumento_NitNull_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setOrganizacionNit(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> estadoDocumentoService.createEstadoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NIT de la organización es obligatorio");

        verify(estadoDocumentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Color con formato inválido debe lanzar IllegalArgumentException")
    void createEstadoDocumento_ColorFormatoInvalido_DebeLanzarExcepcion() {
        // ARRANGE
        validCreateDTO.setColor("verde");   // no es código hex válido

        // ACT & ASSERT
        assertThatThrownBy(() -> estadoDocumentoService.createEstadoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("color debe ser un código hexadecimal válido");

        verify(estadoDocumentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Nombre duplicado en el mismo tenant debe lanzar IllegalArgumentException")
    void createEstadoDocumento_NombreDuplicadoEnTenant_DebeLanzarExcepcion() {
        // ARRANGE
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(new com.eam.proyecto.businessLayer.dto.OrganizacionDTO());
        when(estadoDocumentoDAO.findByNombreAndOrganizacionNit(
                validCreateDTO.getNombre(), validOrganizacionNit))
                .thenReturn(Optional.of(validEstadoDTO));     // ya existe

        // ACT & ASSERT
        assertThatThrownBy(() -> estadoDocumentoService.createEstadoDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un estado con el nombre");

        verify(estadoDocumentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Segundo estado inicial en la misma organización debe lanzar IllegalStateException (RF31)")
    void createEstadoDocumento_SegundoEstadoInicial_DebeLanzarIllegalStateException() {
        // ARRANGE — se intenta crear un segundo estado con esInicial=true
        validCreateDTO.setEsInicial(true);

        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(new com.eam.proyecto.businessLayer.dto.OrganizacionDTO());
        when(estadoDocumentoDAO.findByNombreAndOrganizacionNit(
                validCreateDTO.getNombre(), validOrganizacionNit))
                .thenReturn(Optional.empty());
        // Simula que ya existe un estado inicial en esta organización
        when(estadoDocumentoDAO.findInicialByOrganizacionNit(validOrganizacionNit))
                .thenReturn(Optional.of(validEstadoDTO));

        // ACT & ASSERT
        assertThatThrownBy(() -> estadoDocumentoService.createEstadoDocumento(validCreateDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un estado inicial para esta organización");

        verify(estadoDocumentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Estado inicial cuando no hay uno previo debe crearse correctamente")
    void createEstadoDocumento_PrimerEstadoInicial_DebeCrearseCorrectamente() {
        // ARRANGE
        validCreateDTO.setEsInicial(true);

        EstadoDocumentoDTO estadoInicial = new EstadoDocumentoDTO();
        estadoInicial.setId(2L);
        estadoInicial.setNombre("En Revisión");
        estadoInicial.setEsInicial(true);
        estadoInicial.setOrganizacionNit(validOrganizacionNit);

        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(new com.eam.proyecto.businessLayer.dto.OrganizacionDTO());
        when(estadoDocumentoDAO.findByNombreAndOrganizacionNit(
                validCreateDTO.getNombre(), validOrganizacionNit))
                .thenReturn(Optional.empty());
        when(estadoDocumentoDAO.findInicialByOrganizacionNit(validOrganizacionNit))
                .thenReturn(Optional.empty());           // no hay estado inicial previo
        when(estadoDocumentoDAO.save(any(EstadoDocumentoCreateDTO.class)))
                .thenReturn(estadoInicial);

        // ACT
        EstadoDocumentoDTO result = estadoDocumentoService.createEstadoDocumento(validCreateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getEsInicial()).isTrue();
        verify(estadoDocumentoDAO, times(1)).save(any(EstadoDocumentoCreateDTO.class));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET por ID - Estado existente debe retornar el DTO correctamente")
    void getEstadoDocumentoById_Existente_DebeRetornarDTO() {
        // ARRANGE
        when(estadoDocumentoDAO.findById(validEstadoId))
                .thenReturn(Optional.of(validEstadoDTO));

        // ACT
        EstadoDocumentoDTO result = estadoDocumentoService.getEstadoDocumentoById(validEstadoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validEstadoId);
        assertThat(result.getNombre()).isEqualTo("En Revisión");

        verify(estadoDocumentoDAO, times(1)).findById(validEstadoId);
    }

    @Test
    @DisplayName("GET por ID - Estado inexistente debe lanzar RuntimeException")
    void getEstadoDocumentoById_Inexistente_DebeLanzarRuntimeException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(estadoDocumentoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> estadoDocumentoService.getEstadoDocumentoById(idInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Estado documental no encontrado con ID: " + idInexistente);

        verify(estadoDocumentoDAO, times(1)).findById(idInexistente);
    }

    @Test
    @DisplayName("GET estado inicial - Organización con estado inicial configurado debe retornarlo")
    void getEstadoInicialByOrganizacion_Configurado_DebeRetornarEstado() {
        // ARRANGE
        EstadoDocumentoDTO estadoInicial = new EstadoDocumentoDTO();
        estadoInicial.setId(5L);
        estadoInicial.setNombre("Borrador");
        estadoInicial.setEsInicial(true);
        estadoInicial.setOrganizacionNit(validOrganizacionNit);

        when(estadoDocumentoDAO.findInicialByOrganizacionNit(validOrganizacionNit))
                .thenReturn(Optional.of(estadoInicial));

        // ACT
        EstadoDocumentoDTO result =
                estadoDocumentoService.getEstadoInicialByOrganizacion(validOrganizacionNit);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getEsInicial()).isTrue();
        assertThat(result.getNombre()).isEqualTo("Borrador");

        verify(estadoDocumentoDAO, times(1)).findInicialByOrganizacionNit(validOrganizacionNit);
    }

    @Test
    @DisplayName("GET estado inicial - Sin estado inicial configurado debe lanzar IllegalStateException")
    void getEstadoInicialByOrganizacion_SinConfigurar_DebeLanzarIllegalStateException() {
        // ARRANGE
        when(estadoDocumentoDAO.findInicialByOrganizacionNit(validOrganizacionNit))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() ->
                estadoDocumentoService.getEstadoInicialByOrganizacion(validOrganizacionNit))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no tiene un estado inicial configurado");
    }

    @Test
    @DisplayName("GET todos - Debe retornar la lista completa de estados de la organización")
    void getEstadosByOrganizacion_DebeRetornarListaCompleta() {
        // ARRANGE
        EstadoDocumentoDTO estado2 = new EstadoDocumentoDTO();
        estado2.setId(2L);
        estado2.setNombre("Aprobado");

        when(organizacionService.getOrganizacionByNit(validOrganizacionNit))
                .thenReturn(new com.eam.proyecto.businessLayer.dto.OrganizacionDTO());
        when(estadoDocumentoDAO.findByOrganizacionNit(validOrganizacionNit))
                .thenReturn(List.of(validEstadoDTO, estado2));

        // ACT
        List<EstadoDocumentoDTO> result =
                estadoDocumentoService.getEstadosByOrganizacion(validOrganizacionNit);

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result).extracting("nombre")
                .containsExactlyInAnyOrder("En Revisión", "Aprobado");

        verify(estadoDocumentoDAO, times(1)).findByOrganizacionNit(validOrganizacionNit);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("UPDATE - Datos válidos deben retornar el estado actualizado")
    void updateEstadoDocumento_DatosValidos_DebeRetornarEstadoActualizado() {
        // ARRANGE
        EstadoDocumentoUpdateDTO updateDTO = new EstadoDocumentoUpdateDTO();
        updateDTO.setNombre("Rechazado");
        updateDTO.setColor("#F44336");

        EstadoDocumentoDTO estadoActualizado = new EstadoDocumentoDTO();
        estadoActualizado.setId(validEstadoId);
        estadoActualizado.setNombre("Rechazado");
        estadoActualizado.setColor("#F44336");

        when(estadoDocumentoDAO.findById(validEstadoId))
                .thenReturn(Optional.of(validEstadoDTO));
        when(estadoDocumentoDAO.update(eq(validEstadoId), any(EstadoDocumentoUpdateDTO.class)))
                .thenReturn(Optional.of(estadoActualizado));

        // ACT
        EstadoDocumentoDTO result =
                estadoDocumentoService.updateEstadoDocumento(validEstadoId, updateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getNombre()).isEqualTo("Rechazado");
        assertThat(result.getColor()).isEqualTo("#F44336");

        verify(estadoDocumentoDAO, times(1)).update(eq(validEstadoId), any(EstadoDocumentoUpdateDTO.class));
    }

    @Test
    @DisplayName("UPDATE - Estado inexistente debe lanzar RuntimeException")
    void updateEstadoDocumento_Inexistente_DebeLanzarRuntimeException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(estadoDocumentoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() ->
                estadoDocumentoService.updateEstadoDocumento(idInexistente, new EstadoDocumentoUpdateDTO()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Estado documental no encontrado con ID: " + idInexistente);

        verify(estadoDocumentoDAO, never()).update(anyLong(), any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE - Estado existente debe eliminarse sin lanzar excepción")
    void deleteEstadoDocumento_Existente_DebeEliminarseCorrectamente() {
        // ARRANGE
        when(estadoDocumentoDAO.findById(validEstadoId))
                .thenReturn(Optional.of(validEstadoDTO));
        when(estadoDocumentoDAO.deleteById(validEstadoId))
                .thenReturn(true);

        // ACT & ASSERT
        assertThatCode(() -> estadoDocumentoService.deleteEstadoDocumento(validEstadoId))
                .doesNotThrowAnyException();

        verify(estadoDocumentoDAO, times(1)).findById(validEstadoId);
        verify(estadoDocumentoDAO, times(1)).deleteById(validEstadoId);
    }

    @Test
    @DisplayName("DELETE - Error en DAO debe lanzar RuntimeException")
    void deleteEstadoDocumento_FalloEnDAO_DebeLanzarRuntimeException() {
        // ARRANGE
        when(estadoDocumentoDAO.findById(validEstadoId))
                .thenReturn(Optional.of(validEstadoDTO));
        when(estadoDocumentoDAO.deleteById(validEstadoId))
                .thenReturn(false);       // el DAO reporta que no se pudo eliminar

        // ACT & ASSERT
        assertThatThrownBy(() -> estadoDocumentoService.deleteEstadoDocumento(validEstadoId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al eliminar estado documental ID: " + validEstadoId);
    }

    @Test
    @DisplayName("DELETE - Estado inexistente debe lanzar RuntimeException sin llamar al delete")
    void deleteEstadoDocumento_Inexistente_DebeLanzarRuntimeExceptionSinLlamarDelete() {
        // ARRANGE
        Long idInexistente = 999L;
        when(estadoDocumentoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> estadoDocumentoService.deleteEstadoDocumento(idInexistente))
                .isInstanceOf(RuntimeException.class);

        verify(estadoDocumentoDAO, never()).deleteById(anyLong());
    }
}
