package com.eam.proyecto.unit.service;

import com.eam.proyecto.businessLayer.dto.DocumentoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoPasoDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaUpdateDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioDTO;
import com.eam.proyecto.businessLayer.service.DocumentoService;
import com.eam.proyecto.businessLayer.service.FlujoTrabajoPasoService;
import com.eam.proyecto.businessLayer.service.UsuarioService;
import com.eam.proyecto.businessLayer.service.impl.FlujoTrabajoTareaServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.FlujoTrabajoTareaDAO;
import com.eam.proyecto.persistenceLayer.entity.enums.EstadoTareaEnum;
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
 * Unit Tests para FlujoTrabajoTareaServiceImpl
 *
 * OBJETIVO: Verificar la lógica de asignación, navegación y transición de
 * estado de las tareas dentro del flujo de aprobación documental:
 *   - Validación de campos obligatorios al asignar.
 *   - RF31: un documento no puede tener dos tareas PENDIENTE simultáneas.
 *   - Transiciones de estado: PENDIENTE → COMPLETADO / CANCELADO.
 *   - Restricciones: no completar/cancelar si ya está en estado final.
 *   - Consultas por documento, por usuario y por ID.
 *   - Actualización de campos editables de la tarea.
 *
 * Dependencias mockeadas: FlujoTrabajoTareaDAO, DocumentoService,
 *                         UsuarioService, FlujoTrabajoPasoService
 * SUT: FlujoTrabajoTareaServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlujoTrabajoTareaService - Unit Tests")
public class FlujoTrabajoTareaServiceTest {

    // ─── Dependencias mockeadas ───────────────────────────────────────────────

    @Mock
    private FlujoTrabajoTareaDAO flujoTrabajoTareaDAO;

    @Mock
    private DocumentoService documentoService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private FlujoTrabajoPasoService flujoTrabajoPasoService;

    // ─── Sistema bajo prueba (SUT) ────────────────────────────────────────────

    @InjectMocks
    private FlujoTrabajoTareaServiceImpl flujoTrabajoTareaService;

    // ─── Datos de prueba reutilizables ────────────────────────────────────────

    private Long validTareaId;
    private Long validDocumentoId;
    private Long validPasoId;
    private Long validCedula;
    private FlujoTrabajoTareaCreateDTO validCreateDTO;
    private FlujoTrabajoTareaDTO tareaPendienteDTO;
    private FlujoTrabajoTareaDTO tareaCompletadaDTO;
    private FlujoTrabajoTareaDTO tareaCanceladaDTO;
    private DocumentoDTO validDocumentoDTO;
    private UsuarioDTO validUsuarioDTO;
    private FlujoTrabajoPasoDTO validPasoDTO;

    /**
     * Configuración ejecutada ANTES de cada test.
     */
    @BeforeEach
    void setUp() {
        validTareaId     = 1L;
        validDocumentoId = 10L;
        validPasoId      = 2L;
        validCedula      = 123456789L;

        // Mocks de DTOs dependientes (objeto fake controlado)
        validDocumentoDTO = new DocumentoDTO();
        validDocumentoDTO.setId(validDocumentoId);

        validUsuarioDTO = new UsuarioDTO();
        validUsuarioDTO.setCedula(validCedula);
        validUsuarioDTO.setNombre("Ana Gómez");
        validUsuarioDTO.setActive(true);

        validPasoDTO = mock(FlujoTrabajoPasoDTO.class);
        when(validPasoDTO.getId()).thenReturn(validPasoId);

        // DTO de creación válido
        validCreateDTO = new FlujoTrabajoTareaCreateDTO();
        validCreateDTO.setDocumentoId(validDocumentoId);
        validCreateDTO.setPasoId(validPasoId);
        validCreateDTO.setAsignadoACedula(validCedula);
        validCreateDTO.setComentario("Asignado para revisión inicial");

        // Tarea en estado PENDIENTE (resultado del DAO)
        tareaPendienteDTO = mock(FlujoTrabajoTareaDTO.class);
        when(tareaPendienteDTO.getId()).thenReturn(validTareaId);
        when(tareaPendienteDTO.getEstado()).thenReturn(EstadoTareaEnum.PENDIENTE);

        // Tarea en estado COMPLETADO (para tests de restricción)
        tareaCompletadaDTO = mock(FlujoTrabajoTareaDTO.class);
        when(tareaCompletadaDTO.getId()).thenReturn(2L);
        when(tareaCompletadaDTO.getEstado()).thenReturn(EstadoTareaEnum.COMPLETADO);

        // Tarea en estado CANCELADO (para tests de restricción)
        tareaCanceladaDTO = mock(FlujoTrabajoTareaDTO.class);
        when(tareaCanceladaDTO.getId()).thenReturn(3L);
        when(tareaCanceladaDTO.getEstado()).thenReturn(EstadoTareaEnum.CANCELADO);
    }

    // ==================== asignarTarea ====================

    @Test
    @DisplayName("ASIGNAR - datos válidos sin tarea pendiente previa debe retornar TareaDTO creada en PENDIENTE")
    void asignarTarea_ValidDataNoPendingTask_ShouldReturnCreatedTareaDTO() {
        // ARRANGE
        when(documentoService.getDocumentoById(validDocumentoId)).thenReturn(validDocumentoDTO);
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(flujoTrabajoPasoService.getPasoById(validPasoId)).thenReturn(validPasoDTO);
        when(flujoTrabajoTareaDAO.existeTareaPendienteByDocumentoId(validDocumentoId)).thenReturn(false);
        when(flujoTrabajoTareaDAO.save(any(FlujoTrabajoTareaCreateDTO.class))).thenReturn(tareaPendienteDTO);

        // ACT
        FlujoTrabajoTareaDTO result = flujoTrabajoTareaService.asignarTarea(validCreateDTO);

        // ASSERT - estado
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validTareaId);
        assertThat(result.getEstado()).isEqualTo(EstadoTareaEnum.PENDIENTE);

        // ASSERT - comportamiento: se validaron dependencias antes de persistir
        verify(documentoService,      times(1)).getDocumentoById(validDocumentoId);
        verify(usuarioService,         times(1)).getUsuarioByCedula(validCedula);
        verify(flujoTrabajoPasoService, times(1)).getPasoById(validPasoId);
        verify(flujoTrabajoTareaDAO,   times(1)).existeTareaPendienteByDocumentoId(validDocumentoId);
        verify(flujoTrabajoTareaDAO,   times(1)).save(any(FlujoTrabajoTareaCreateDTO.class));
    }

    @Test
    @DisplayName("ASIGNAR - documentoId null debe lanzar IllegalArgumentException sin consultar servicios")
    void asignarTarea_NullDocumentoId_ShouldThrowIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setDocumentoId(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.asignarTarea(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID del documento es obligatorio");

        verify(flujoTrabajoTareaDAO, never()).save(any());
    }

    @Test
    @DisplayName("ASIGNAR - pasoId null debe lanzar IllegalArgumentException sin consultar servicios")
    void asignarTarea_NullPasoId_ShouldThrowIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setPasoId(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.asignarTarea(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paso del flujo es obligatorio");

        verify(flujoTrabajoTareaDAO, never()).save(any());
    }

    @Test
    @DisplayName("ASIGNAR - asignadoACedula null debe lanzar IllegalArgumentException sin consultar servicios")
    void asignarTarea_NullCedula_ShouldThrowIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setAsignadoACedula(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.asignarTarea(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula del usuario asignado es obligatoria");

        verify(flujoTrabajoTareaDAO, never()).save(any());
    }

    @Test
    @DisplayName("ASIGNAR - documento inexistente debe lanzar RuntimeException sin persistir")
    void asignarTarea_NonExistentDocumento_ShouldThrowRuntimeException() {
        // ARRANGE
        when(documentoService.getDocumentoById(validDocumentoId))
                .thenThrow(new RuntimeException("Documento no encontrado con ID: " + validDocumentoId));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.asignarTarea(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Documento no encontrado");

        verify(flujoTrabajoTareaDAO, never()).save(any());
    }

    @Test
    @DisplayName("ASIGNAR - usuario inexistente debe lanzar RuntimeException sin persistir")
    void asignarTarea_NonExistentUsuario_ShouldThrowRuntimeException() {
        // ARRANGE
        when(documentoService.getDocumentoById(validDocumentoId)).thenReturn(validDocumentoDTO);
        when(usuarioService.getUsuarioByCedula(validCedula))
                .thenThrow(new RuntimeException("Usuario no encontrado con cédula: " + validCedula));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.asignarTarea(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");

        verify(flujoTrabajoTareaDAO, never()).save(any());
    }

    @Test
    @DisplayName("ASIGNAR - paso inexistente debe lanzar RuntimeException sin persistir")
    void asignarTarea_NonExistentPaso_ShouldThrowRuntimeException() {
        // ARRANGE
        when(documentoService.getDocumentoById(validDocumentoId)).thenReturn(validDocumentoDTO);
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(flujoTrabajoPasoService.getPasoById(validPasoId))
                .thenThrow(new RuntimeException("Paso del flujo no encontrado con ID: " + validPasoId));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.asignarTarea(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Paso del flujo no encontrado");

        verify(flujoTrabajoTareaDAO, never()).save(any());
    }

    @Test
    @DisplayName("ASIGNAR - RF31: documento con tarea PENDIENTE activa debe lanzar IllegalStateException")
    void asignarTarea_DocumentWithActivePendingTask_ShouldThrowIllegalStateException() {
        // ARRANGE
        when(documentoService.getDocumentoById(validDocumentoId)).thenReturn(validDocumentoDTO);
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(flujoTrabajoPasoService.getPasoById(validPasoId)).thenReturn(validPasoDTO);
        when(flujoTrabajoTareaDAO.existeTareaPendienteByDocumentoId(validDocumentoId)).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.asignarTarea(validCreateDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("documento ya tiene una tarea pendiente");

        verify(flujoTrabajoTareaDAO, never()).save(any());
    }

    // ==================== getTareaById ====================

    @Test
    @DisplayName("GET by ID - tarea existente debe retornar TareaDTO")
    void getTareaById_ExistingId_ShouldReturnTareaDTO() {
        // ARRANGE
        when(flujoTrabajoTareaDAO.findById(validTareaId)).thenReturn(Optional.of(tareaPendienteDTO));

        // ACT
        FlujoTrabajoTareaDTO result = flujoTrabajoTareaService.getTareaById(validTareaId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validTareaId);
        assertThat(result.getEstado()).isEqualTo(EstadoTareaEnum.PENDIENTE);

        verify(flujoTrabajoTareaDAO, times(1)).findById(validTareaId);
    }

    @Test
    @DisplayName("GET by ID - tarea inexistente debe lanzar RuntimeException")
    void getTareaById_NonExistentId_ShouldThrowRuntimeException() {
        // ARRANGE
        Long nonExistentId = 999L;
        when(flujoTrabajoTareaDAO.findById(nonExistentId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.getTareaById(nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tarea del flujo no encontrada con ID: " + nonExistentId);
    }

    // ==================== getTareaActivaByDocumento ====================

    @Test
    @DisplayName("GET tarea activa - documento con tarea PENDIENTE debe retornar esa tarea")
    void getTareaActivaByDocumento_DocumentWithPendingTask_ShouldReturnTask() {
        // ARRANGE
        when(documentoService.getDocumentoById(validDocumentoId)).thenReturn(validDocumentoDTO);
        when(flujoTrabajoTareaDAO.findTareaActivaByDocumentoId(validDocumentoId))
                .thenReturn(Optional.of(tareaPendienteDTO));

        // ACT
        FlujoTrabajoTareaDTO result = flujoTrabajoTareaService.getTareaActivaByDocumento(validDocumentoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getEstado()).isEqualTo(EstadoTareaEnum.PENDIENTE);

        verify(documentoService, times(1)).getDocumentoById(validDocumentoId);
        verify(flujoTrabajoTareaDAO, times(1)).findTareaActivaByDocumentoId(validDocumentoId);
    }

    @Test
    @DisplayName("GET tarea activa - documento sin tarea pendiente debe lanzar RuntimeException")
    void getTareaActivaByDocumento_NoPendingTask_ShouldThrowRuntimeException() {
        // ARRANGE
        when(documentoService.getDocumentoById(validDocumentoId)).thenReturn(validDocumentoDTO);
        when(flujoTrabajoTareaDAO.findTareaActivaByDocumentoId(validDocumentoId))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.getTareaActivaByDocumento(validDocumentoId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("documento no tiene ninguna tarea pendiente activa");
    }

    // ==================== getTareasByDocumento ====================

    @Test
    @DisplayName("GET tareas by documento - debe retornar historial completo del documento")
    void getTareasByDocumento_ExistingDocumento_ShouldReturnTaskHistory() {
        // ARRANGE
        when(documentoService.getDocumentoById(validDocumentoId)).thenReturn(validDocumentoDTO);
        when(flujoTrabajoTareaDAO.findByDocumentoId(validDocumentoId))
                .thenReturn(Arrays.asList(tareaCompletadaDTO, tareaPendienteDTO));

        // ACT
        List<FlujoTrabajoTareaDTO> result = flujoTrabajoTareaService.getTareasByDocumento(validDocumentoId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        verify(documentoService, times(1)).getDocumentoById(validDocumentoId);
        verify(flujoTrabajoTareaDAO, times(1)).findByDocumentoId(validDocumentoId);
    }

    // ==================== getTareasPendientesByUsuario ====================

    @Test
    @DisplayName("GET tareas pendientes by usuario - usuario con tareas pendientes debe retornar lista")
    void getTareasPendientesByUsuario_UserWithPendingTasks_ShouldReturnList() {
        // ARRANGE
        FlujoTrabajoTareaDTO otraTareaDTO = mock(FlujoTrabajoTareaDTO.class);
        when(otraTareaDTO.getEstado()).thenReturn(EstadoTareaEnum.PENDIENTE);

        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(validUsuarioDTO);
        when(flujoTrabajoTareaDAO.findPendientesByUsuarioCedula(validCedula))
                .thenReturn(Arrays.asList(tareaPendienteDTO, otraTareaDTO));

        // ACT
        List<FlujoTrabajoTareaDTO> result =
                flujoTrabajoTareaService.getTareasPendientesByUsuario(validCedula);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getEstado() == EstadoTareaEnum.PENDIENTE);

        verify(usuarioService, times(1)).getUsuarioByCedula(validCedula);
        verify(flujoTrabajoTareaDAO, times(1)).findPendientesByUsuarioCedula(validCedula);
    }

    // ==================== completarTarea ====================

    @Test
    @DisplayName("COMPLETAR - tarea PENDIENTE debe pasar a COMPLETADO y retornar TareaDTO")
    void completarTarea_PendingTask_ShouldReturnCompletedTask() {
        // ARRANGE
        String comentario = "Revisión aprobada sin observaciones";

        FlujoTrabajoTareaDTO tareaResultado = mock(FlujoTrabajoTareaDTO.class);
        when(tareaResultado.getEstado()).thenReturn(EstadoTareaEnum.COMPLETADO);

        when(flujoTrabajoTareaDAO.findById(validTareaId)).thenReturn(Optional.of(tareaPendienteDTO));
        when(flujoTrabajoTareaDAO.completar(validTareaId, comentario))
                .thenReturn(Optional.of(tareaResultado));

        // ACT
        FlujoTrabajoTareaDTO result = flujoTrabajoTareaService.completarTarea(validTareaId, comentario);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getEstado()).isEqualTo(EstadoTareaEnum.COMPLETADO);

        verify(flujoTrabajoTareaDAO, times(1)).findById(validTareaId);
        verify(flujoTrabajoTareaDAO, times(1)).completar(validTareaId, comentario);
    }

    @Test
    @DisplayName("COMPLETAR - tarea ya COMPLETADA debe lanzar IllegalStateException sin operar en DAO")
    void completarTarea_AlreadyCompletedTask_ShouldThrowIllegalStateException() {
        // ARRANGE
        when(flujoTrabajoTareaDAO.findById(2L)).thenReturn(Optional.of(tareaCompletadaDTO));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.completarTarea(2L, "comentario"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden completar tareas en estado PENDIENTE")
                .hasMessageContaining("COMPLETADO");

        verify(flujoTrabajoTareaDAO, never()).completar(anyLong(), anyString());
    }

    @Test
    @DisplayName("COMPLETAR - tarea CANCELADA debe lanzar IllegalStateException sin operar en DAO")
    void completarTarea_CancelledTask_ShouldThrowIllegalStateException() {
        // ARRANGE
        when(flujoTrabajoTareaDAO.findById(3L)).thenReturn(Optional.of(tareaCanceladaDTO));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.completarTarea(3L, "comentario"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden completar tareas en estado PENDIENTE")
                .hasMessageContaining("CANCELADO");

        verify(flujoTrabajoTareaDAO, never()).completar(anyLong(), anyString());
    }

    // ==================== cancelarTarea ====================

    @Test
    @DisplayName("CANCELAR - tarea PENDIENTE debe cancelarse y retornar TareaDTO")
    void cancelarTarea_PendingTask_ShouldReturnCancelledTask() {
        // ARRANGE
        FlujoTrabajoTareaDTO tareaResultado = mock(FlujoTrabajoTareaDTO.class);
        when(tareaResultado.getEstado()).thenReturn(EstadoTareaEnum.CANCELADO);

        when(flujoTrabajoTareaDAO.findById(validTareaId)).thenReturn(Optional.of(tareaPendienteDTO));
        when(flujoTrabajoTareaDAO.cancelar(validTareaId)).thenReturn(Optional.of(tareaResultado));

        // ACT
        FlujoTrabajoTareaDTO result = flujoTrabajoTareaService.cancelarTarea(validTareaId);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getEstado()).isEqualTo(EstadoTareaEnum.CANCELADO);

        verify(flujoTrabajoTareaDAO, times(1)).findById(validTareaId);
        verify(flujoTrabajoTareaDAO, times(1)).cancelar(validTareaId);
    }

    @Test
    @DisplayName("CANCELAR - tarea ya CANCELADA debe lanzar IllegalStateException sin operar en DAO")
    void cancelarTarea_AlreadyCancelledTask_ShouldThrowIllegalStateException() {
        // ARRANGE
        when(flujoTrabajoTareaDAO.findById(3L)).thenReturn(Optional.of(tareaCanceladaDTO));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.cancelarTarea(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tarea ya se encuentra cancelada");

        verify(flujoTrabajoTareaDAO, never()).cancelar(anyLong());
    }

    @Test
    @DisplayName("CANCELAR - tarea COMPLETADA no puede cancelarse, debe lanzar IllegalStateException")
    void cancelarTarea_CompletedTask_ShouldThrowIllegalStateException() {
        // ARRANGE
        when(flujoTrabajoTareaDAO.findById(2L)).thenReturn(Optional.of(tareaCompletadaDTO));

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.cancelarTarea(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede cancelar una tarea que ya fue completada");

        verify(flujoTrabajoTareaDAO, never()).cancelar(anyLong());
    }

    // ==================== updateTarea ====================

    @Test
    @DisplayName("UPDATE - tarea existente con datos válidos debe retornar TareaDTO actualizada")
    void updateTarea_ExistingTask_ShouldReturnUpdatedTareaDTO() {
        // ARRANGE
        FlujoTrabajoTareaUpdateDTO updateDTO = new FlujoTrabajoTareaUpdateDTO();
        updateDTO.setComentario("Comentario actualizado por el revisor");

        FlujoTrabajoTareaDTO tareaActualizada = mock(FlujoTrabajoTareaDTO.class);
        when(tareaActualizada.getId()).thenReturn(validTareaId);

        when(flujoTrabajoTareaDAO.findById(validTareaId)).thenReturn(Optional.of(tareaPendienteDTO));
        when(flujoTrabajoTareaDAO.update(eq(validTareaId), any(FlujoTrabajoTareaUpdateDTO.class)))
                .thenReturn(Optional.of(tareaActualizada));

        // ACT
        FlujoTrabajoTareaDTO result = flujoTrabajoTareaService.updateTarea(validTareaId, updateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validTareaId);

        verify(flujoTrabajoTareaDAO, times(1)).findById(validTareaId);
        verify(flujoTrabajoTareaDAO, times(1))
                .update(eq(validTareaId), any(FlujoTrabajoTareaUpdateDTO.class));
    }

    @Test
    @DisplayName("UPDATE - tarea inexistente debe lanzar RuntimeException sin actualizar")
    void updateTarea_NonExistentTask_ShouldThrowRuntimeException() {
        // ARRANGE
        Long nonExistentId = 999L;
        FlujoTrabajoTareaUpdateDTO updateDTO = new FlujoTrabajoTareaUpdateDTO();
        when(flujoTrabajoTareaDAO.findById(nonExistentId)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> flujoTrabajoTareaService.updateTarea(nonExistentId, updateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tarea del flujo no encontrada con ID: " + nonExistentId);

        verify(flujoTrabajoTareaDAO, never()).update(anyLong(), any());
    }
}
