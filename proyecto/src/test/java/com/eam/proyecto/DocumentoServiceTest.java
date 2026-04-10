package com.eam.proyecto;

import com.eam.proyecto.businessLayer.dto.DocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoUpdateDTO;
import com.eam.proyecto.businessLayer.dto.EstadoDocumentoDTO;
import com.eam.proyecto.businessLayer.service.EstadoDocumentoService;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.TipoDocumentoService;
import com.eam.proyecto.businessLayer.service.UsuarioService;
import com.eam.proyecto.businessLayer.service.impl.DocumentoServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.DocumentoDAO;
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
 * Unit Tests para DocumentoServiceImpl
 *
 * OBJETIVO: Probar la lógica de negocio del servicio de forma aislada
 * - No requiere base de datos
 * - No requiere Spring Context
 * - Usa mocks para DocumentoDAO, OrganizacionService, UsuarioService,
 *   TipoDocumentoService y EstadoDocumentoService
 * - Cubre RF17 / RF19 / RF20 / RF21 / RF30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoService - Unit Tests")
public class DocumentoServiceTest {

    // ─── Dependencias mockeadas ───────────────────────────────────────────────
    @Mock
    private DocumentoDAO documentoDAO;

    @Mock
    private OrganizacionService organizacionService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private TipoDocumentoService tipoDocumentoService;

    @Mock
    private EstadoDocumentoService estadoDocumentoService;

    // ─── Clase bajo prueba (SUT) ──────────────────────────────────────────────
    @InjectMocks
    private DocumentoServiceImpl documentoService;

    // ─── Datos de prueba compartidos ─────────────────────────────────────────
    private DocumentoCreateDTO validCreateDTO;
    private DocumentoDTO       validDocumentoDTO;
    private EstadoDocumentoDTO estadoInicialDTO;
    private Long               validDocumentoId;
    private Long               validOrganizacionNit;
    private Long               validCedula;
    private Long               validTipoDocumentoId;
    private Long               validEstadoId;

    /**
     * Se ejecuta antes de cada test.
     * Inicializa objetos en estado válido para reutilizarlos.
     */
    @BeforeEach
    void setUp() {
        validDocumentoId      = 1L;
        validOrganizacionNit  = 900123456L;
        validCedula           = 10203040L;
        validTipoDocumentoId  = 5L;
        validEstadoId         = 10L;

        estadoInicialDTO = new EstadoDocumentoDTO();
        estadoInicialDTO.setId(validEstadoId);
        estadoInicialDTO.setNombre("Borrador");

        validCreateDTO = new DocumentoCreateDTO();
        validCreateDTO.setTitulo("Contrato de Servicios 2026");
        validCreateDTO.setCreadoPorCedula(validCedula);
        validCreateDTO.setOrganizacionNit(validOrganizacionNit);
        validCreateDTO.setTipoDocumentoId(validTipoDocumentoId);
        // estadoDocumentoId = null → el service debe asignarlo automáticamente

        validDocumentoDTO = new DocumentoDTO();
        validDocumentoDTO.setId(validDocumentoId);
        validDocumentoDTO.setTitulo("Contrato de Servicios 2026");
        validDocumentoDTO.setOrganizacionNit(validOrganizacionNit);
        validDocumentoDTO.setVersion(1);
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("CREATE - Datos válidos sin estadoId deben crear documento con estado inicial automático")
    void createDocumento_ValidDataNoEstado_ShouldAssignEstadoInicialAutomatically() {
        // Arrange — estadoDocumentoId = null, el service debe consultar el estado inicial
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit)).thenReturn(null);
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(null);
        when(tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(validTipoDocumentoId, validOrganizacionNit))
                .thenReturn(null);
        when(estadoDocumentoService.getEstadoInicialByOrganizacion(validOrganizacionNit))
                .thenReturn(estadoInicialDTO);
        when(documentoDAO.save(any(DocumentoCreateDTO.class))).thenReturn(validDocumentoDTO);

        // Act
        DocumentoDTO result = documentoService.createDocumento(validCreateDTO);

        // Assert - estado
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validDocumentoId);

        // Assert - comportamiento: se consultó el estado inicial y se asignó al DTO
        ArgumentCaptor<DocumentoCreateDTO> captor = ArgumentCaptor.forClass(DocumentoCreateDTO.class);
        verify(estadoDocumentoService, times(1)).getEstadoInicialByOrganizacion(validOrganizacionNit);
        verify(documentoDAO, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEstadoDocumentoId()).isEqualTo(validEstadoId);
        assertThat(captor.getValue().getVersion()).isEqualTo(1);
        assertThat(captor.getValue().getCreadoEn()).isNotNull();
        assertThat(captor.getValue().getActualizadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CREATE - estadoId explícito no debe consultar estado inicial de la organización")
    void createDocumento_WithEstadoIdExplicit_ShouldSkipEstadoInicialLookup() {
        // Arrange — se especifica estadoDocumentoId directamente
        validCreateDTO.setEstadoDocumentoId(validEstadoId);
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit)).thenReturn(null);
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(null);
        when(tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(validTipoDocumentoId, validOrganizacionNit))
                .thenReturn(null);
        when(documentoDAO.save(any(DocumentoCreateDTO.class))).thenReturn(validDocumentoDTO);

        // Act
        DocumentoDTO result = documentoService.createDocumento(validCreateDTO);

        // Assert - el método de estado inicial NO debe ser invocado
        assertThat(result).isNotNull();
        verify(estadoDocumentoService, never()).getEstadoInicialByOrganizacion(any());
        verify(documentoDAO, times(1)).save(any(DocumentoCreateDTO.class));
    }

    @Test
    @DisplayName("CREATE - Organización sin estado inicial configurado debe propagar IllegalStateException")
    void createDocumento_OrganizacionWithoutEstadoInicial_ShouldThrowIllegalStateException() {
        // Arrange
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit)).thenReturn(null);
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(null);
        when(tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(validTipoDocumentoId, validOrganizacionNit))
                .thenReturn(null);
        when(estadoDocumentoService.getEstadoInicialByOrganizacion(validOrganizacionNit))
                .thenThrow(new IllegalStateException("La organización no tiene un estado inicial configurado"));

        // Act & Assert
        assertThatThrownBy(() -> documentoService.createDocumento(validCreateDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("estado inicial");

        verify(documentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Título null debe lanzar IllegalArgumentException")
    void createDocumento_NullTitulo_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setTitulo(null);

        // Act & Assert
        assertThatThrownBy(() -> documentoService.createDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("título");

        verify(organizacionService, never()).getOrganizacionActivaByNit(any());
        verify(documentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Título vacío debe lanzar IllegalArgumentException")
    void createDocumento_EmptyTitulo_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setTitulo("   ");

        // Act & Assert
        assertThatThrownBy(() -> documentoService.createDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("título");

        verify(documentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Título mayor a 300 caracteres debe lanzar IllegalArgumentException")
    void createDocumento_TituloTooLong_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setTitulo("T".repeat(301));

        // Act & Assert
        assertThatThrownBy(() -> documentoService.createDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("300 caracteres");

        verify(documentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Cédula del creador null debe lanzar IllegalArgumentException")
    void createDocumento_NullCedula_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setCreadoPorCedula(null);

        // Act & Assert
        assertThatThrownBy(() -> documentoService.createDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula");

        verify(documentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - NIT de organización null debe lanzar IllegalArgumentException")
    void createDocumento_NullOrganizacionNit_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setOrganizacionNit(null);

        // Act & Assert
        assertThatThrownBy(() -> documentoService.createDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NIT");

        verify(documentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - TipoDocumentoId null debe lanzar IllegalArgumentException")
    void createDocumento_NullTipoDocumentoId_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setTipoDocumentoId(null);

        // Act & Assert
        assertThatThrownBy(() -> documentoService.createDocumento(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo de documento");

        verify(documentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Organización inactiva debe propagar RuntimeException sin persistir")
    void createDocumento_InactiveOrganizacion_ShouldThrowRuntimeException() {
        // Arrange
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenThrow(new RuntimeException("Organización no encontrada o inactiva"));

        // Act & Assert
        assertThatThrownBy(() -> documentoService.createDocumento(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inactiva");

        verify(documentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Usuario creador inexistente debe propagar RuntimeException sin persistir")
    void createDocumento_NonExistentCreator_ShouldThrowRuntimeException() {
        // Arrange
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit)).thenReturn(null);
        when(usuarioService.getUsuarioByCedula(validCedula))
                .thenThrow(new RuntimeException("Usuario no encontrado con cédula: " + validCedula));

        // Act & Assert
        assertThatThrownBy(() -> documentoService.createDocumento(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");

        verify(documentoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Tipo documental no pertenece al tenant debe propagar RuntimeException sin persistir")
    void createDocumento_TipoDocumentoNotInTenant_ShouldThrowRuntimeException() {
        // Arrange
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit)).thenReturn(null);
        when(usuarioService.getUsuarioByCedula(validCedula)).thenReturn(null);
        when(tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(validTipoDocumentoId, validOrganizacionNit))
                .thenThrow(new RuntimeException("Tipo documental no encontrado en esta organización"));

        // Act & Assert
        assertThatThrownBy(() -> documentoService.createDocumento(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("organización");

        verify(documentoDAO, never()).save(any());
    }

    // ==================== READ ====================

    @Test
    @DisplayName("READ - ID existente debe retornar DocumentoDTO")
    void getDocumentoById_ExistingId_ShouldReturnDocumento() {
        // Arrange
        when(documentoDAO.findById(validDocumentoId)).thenReturn(Optional.of(validDocumentoDTO));

        // Act
        DocumentoDTO result = documentoService.getDocumentoById(validDocumentoId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validDocumentoId);
        assertThat(result.getTitulo()).isEqualTo("Contrato de Servicios 2026");
        verify(documentoDAO, times(1)).findById(validDocumentoId);
    }

    @Test
    @DisplayName("READ - ID inexistente debe lanzar RuntimeException")
    void getDocumentoById_NonExistentId_ShouldThrowRuntimeException() {
        // Arrange
        when(documentoDAO.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentoService.getDocumentoById(9999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");

        verify(documentoDAO, times(1)).findById(9999L);
    }

    @Test
    @DisplayName("READ BY TENANT - ID + NIT deben retornar documento del tenant correcto")
    void getDocumentoByIdAndOrganizacion_ValidIdAndNit_ShouldReturnDocumento() {
        // Arrange
        when(documentoDAO.findByIdAndOrganizacionNit(validDocumentoId, validOrganizacionNit))
                .thenReturn(Optional.of(validDocumentoDTO));

        // Act
        DocumentoDTO result = documentoService.getDocumentoByIdAndOrganizacion(validDocumentoId, validOrganizacionNit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(validDocumentoId);
        verify(documentoDAO, times(1)).findByIdAndOrganizacionNit(validDocumentoId, validOrganizacionNit);
    }

    @Test
    @DisplayName("READ BY TENANT - Documento de otro tenant debe lanzar RuntimeException")
    void getDocumentoByIdAndOrganizacion_WrongTenant_ShouldThrowRuntimeException() {
        // Arrange
        Long otroNit = 111111111L;
        when(documentoDAO.findByIdAndOrganizacionNit(validDocumentoId, otroNit))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentoService.getDocumentoByIdAndOrganizacion(validDocumentoId, otroNit))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("organización");
    }

    @Test
    @DisplayName("READ ALL - Debe retornar lista de documentos de la organización")
    void getDocumentosByOrganizacion_ShouldReturnList() {
        // Arrange
        DocumentoDTO doc2 = new DocumentoDTO();
        doc2.setId(2L);
        when(organizacionService.getOrganizacionByNit(validOrganizacionNit)).thenReturn(null);
        when(documentoDAO.findByOrganizacionNit(validOrganizacionNit))
                .thenReturn(Arrays.asList(validDocumentoDTO, doc2));

        // Act
        List<DocumentoDTO> result = documentoService.getDocumentosByOrganizacion(validOrganizacionNit);

        // Assert
        assertThat(result).hasSize(2);
        verify(organizacionService, times(1)).getOrganizacionByNit(validOrganizacionNit);
        verify(documentoDAO, times(1)).findByOrganizacionNit(validOrganizacionNit);
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("UPDATE - Metadatos válidos deben retornar documento actualizado")
    void updateDocumento_ValidData_ShouldReturnUpdatedDocumento() {
        // Arrange
        DocumentoUpdateDTO updateDTO = new DocumentoUpdateDTO();
        updateDTO.setTitulo("Contrato Actualizado 2026");

        DocumentoDTO updated = new DocumentoDTO();
        updated.setId(validDocumentoId);
        updated.setTitulo("Contrato Actualizado 2026");

        when(documentoDAO.findById(validDocumentoId)).thenReturn(Optional.of(validDocumentoDTO));
        when(documentoDAO.update(eq(validDocumentoId), any(DocumentoUpdateDTO.class)))
                .thenReturn(Optional.of(updated));

        // Act
        DocumentoDTO result = documentoService.updateDocumento(validDocumentoId, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitulo()).isEqualTo("Contrato Actualizado 2026");
        verify(documentoDAO, times(1)).update(eq(validDocumentoId), any(DocumentoUpdateDTO.class));
    }

    @Test
    @DisplayName("UPDATE - Documento inexistente debe lanzar RuntimeException")
    void updateDocumento_NonExistentId_ShouldThrowRuntimeException() {
        // Arrange
        when(documentoDAO.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentoService.updateDocumento(9999L, new DocumentoUpdateDTO()))
                .isInstanceOf(RuntimeException.class);

        verify(documentoDAO, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("UPDATE - Título vacío debe lanzar IllegalArgumentException")
    void updateDocumento_EmptyTitulo_ShouldThrowIllegalArgumentException() {
        // Arrange
        DocumentoUpdateDTO updateDTO = new DocumentoUpdateDTO();
        updateDTO.setTitulo("   ");
        when(documentoDAO.findById(validDocumentoId)).thenReturn(Optional.of(validDocumentoDTO));

        // Act & Assert
        assertThatThrownBy(() -> documentoService.updateDocumento(validDocumentoId, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("título");

        verify(documentoDAO, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("UPDATE - Título mayor a 300 caracteres debe lanzar IllegalArgumentException")
    void updateDocumento_TituloTooLong_ShouldThrowIllegalArgumentException() {
        // Arrange
        DocumentoUpdateDTO updateDTO = new DocumentoUpdateDTO();
        updateDTO.setTitulo("X".repeat(301));
        when(documentoDAO.findById(validDocumentoId)).thenReturn(Optional.of(validDocumentoDTO));

        // Act & Assert
        assertThatThrownBy(() -> documentoService.updateDocumento(validDocumentoId, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("300 caracteres");

        verify(documentoDAO, never()).update(anyLong(), any());
    }

    // ==================== CAMBIAR ESTADO ====================

    @Test
    @DisplayName("CAMBIAR ESTADO - Estado válido debe retornar documento con nuevo estado")
    void cambiarEstado_ValidState_ShouldReturnDocumentoWithNewState() {
        // Arrange
        Long nuevoEstadoId = 20L;
        EstadoDocumentoDTO nuevoEstado = new EstadoDocumentoDTO();
        nuevoEstado.setId(nuevoEstadoId);
        nuevoEstado.setNombre("En Revisión");

        DocumentoDTO updatedDoc = new DocumentoDTO();
        updatedDoc.setId(validDocumentoId);
        updatedDoc.setEstadoDocumentoId(nuevoEstadoId);

        when(documentoDAO.findById(validDocumentoId)).thenReturn(Optional.of(validDocumentoDTO));
        when(estadoDocumentoService.getEstadoDocumentoById(nuevoEstadoId)).thenReturn(nuevoEstado);
        when(documentoDAO.cambiarEstado(validDocumentoId, nuevoEstadoId)).thenReturn(Optional.of(updatedDoc));

        // Act
        DocumentoDTO result = documentoService.cambiarEstado(validDocumentoId, nuevoEstadoId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEstadoDocumentoId()).isEqualTo(nuevoEstadoId);
        verify(documentoDAO, times(1)).findById(validDocumentoId);
        verify(estadoDocumentoService, times(1)).getEstadoDocumentoById(nuevoEstadoId);
        verify(documentoDAO, times(1)).cambiarEstado(validDocumentoId, nuevoEstadoId);
    }

    @Test
    @DisplayName("CAMBIAR ESTADO - Documento inexistente debe lanzar RuntimeException")
    void cambiarEstado_NonExistentDocumento_ShouldThrowRuntimeException() {
        // Arrange
        when(documentoDAO.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentoService.cambiarEstado(9999L, validEstadoId))
                .isInstanceOf(RuntimeException.class);

        verify(estadoDocumentoService, never()).getEstadoDocumentoById(any());
        verify(documentoDAO, never()).cambiarEstado(anyLong(), anyLong());
    }

    @Test
    @DisplayName("CAMBIAR ESTADO - Estado inexistente debe lanzar RuntimeException")
    void cambiarEstado_NonExistentEstado_ShouldThrowRuntimeException() {
        // Arrange
        Long estadoInexistenteId = 9999L;
        when(documentoDAO.findById(validDocumentoId)).thenReturn(Optional.of(validDocumentoDTO));
        when(estadoDocumentoService.getEstadoDocumentoById(estadoInexistenteId))
                .thenThrow(new RuntimeException("Estado documental no encontrado con ID: " + estadoInexistenteId));

        // Act & Assert
        assertThatThrownBy(() -> documentoService.cambiarEstado(validDocumentoId, estadoInexistenteId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");

        verify(documentoDAO, never()).cambiarEstado(anyLong(), anyLong());
    }

    // ==================== DELETE ====================

    @Test
    @DisplayName("DELETE - ID existente debe completar sin excepción")
    void deleteDocumento_ExistingId_ShouldCompleteWithoutException() {
        // Arrange
        when(documentoDAO.findById(validDocumentoId)).thenReturn(Optional.of(validDocumentoDTO));
        when(documentoDAO.deleteById(validDocumentoId)).thenReturn(true);

        // Act & Assert
        assertThatCode(() -> documentoService.deleteDocumento(validDocumentoId))
                .doesNotThrowAnyException();

        verify(documentoDAO, times(1)).findById(validDocumentoId);
        verify(documentoDAO, times(1)).deleteById(validDocumentoId);
    }

    @Test
    @DisplayName("DELETE - ID inexistente debe lanzar RuntimeException antes de borrar")
    void deleteDocumento_NonExistentId_ShouldThrowRuntimeException() {
        // Arrange
        when(documentoDAO.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentoService.deleteDocumento(9999L))
                .isInstanceOf(RuntimeException.class);

        verify(documentoDAO, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("DELETE - DAO retorna false debe lanzar RuntimeException")
    void deleteDocumento_DaoReturnsFalse_ShouldThrowRuntimeException() {
        // Arrange
        when(documentoDAO.findById(validDocumentoId)).thenReturn(Optional.of(validDocumentoDTO));
        when(documentoDAO.deleteById(validDocumentoId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> documentoService.deleteDocumento(validDocumentoId))
                .isInstanceOf(RuntimeException.class);

        verify(documentoDAO, times(1)).deleteById(validDocumentoId);
    }
}
