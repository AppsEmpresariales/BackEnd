package com.eam.proyecto.unit.service;

import com.eam.proyecto.businessLayer.dto.AuditRegistroCreateDTO;
import com.eam.proyecto.businessLayer.dto.AuditRegistroDTO;
import com.eam.proyecto.businessLayer.service.DocumentoService;
import com.eam.proyecto.businessLayer.service.UsuarioService;
import com.eam.proyecto.businessLayer.service.impl.AuditRegistroServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.AuditRegistroDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests para AuditRegistroServiceImpl
 *
 * SERVICIOS BAJO PRUEBA (RF33 / RF34 / RF35 / RF36):
 * - registrarAccion : registra una acción de auditoría (append-only).
 * - getRegistroById : busca un registro por ID.
 * - getHistorialByDocumento : línea de tiempo de un documento (RF34).
 * - getTrazabilidadByUsuario : trazabilidad por usuario (RF36).
 * - getTrazabilidadCompleta : panel global con filtros opcionales (RF36).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditRegistroService - Unit Tests")
class AuditRegistroServiceTest {

    // ─── Dependencias mockeadas ───────────────────────────────────────────────

    @Mock
    private AuditRegistroDAO auditRegistroDAO;

    @Mock
    private DocumentoService documentoService;

    @Mock
    private UsuarioService usuarioService;

    // ─── Sistema bajo prueba (SUT) ────────────────────────────────────────────

    @InjectMocks
    private AuditRegistroServiceImpl auditRegistroService;

    // ─── Datos de prueba reutilizables ────────────────────────────────────────

    private AuditRegistroCreateDTO validCreateDTO;
    private AuditRegistroDTO validAuditDTO;

    private final Long DOCUMENTO_ID   = 10L;
    private final Long USUARIO_CEDULA = 123456789L;
    private final Long REGISTRO_ID    = 1L;

    /**
     * Se ejecuta antes de cada test.
     * Inicializa un DTO de creación y un DTO de respuesta válidos.
     */
    @BeforeEach
    void setUp() {
        validCreateDTO = new AuditRegistroCreateDTO();
        validCreateDTO.setDocumentoId(DOCUMENTO_ID);
        validCreateDTO.setUsuarioCedula(USUARIO_CEDULA);
        validCreateDTO.setAccion("CREACION");
        validCreateDTO.setDescripcion("Documento creado por el usuario");
        validCreateDTO.setEstadoPrevio(null);
        validCreateDTO.setEstadoNuevo("BORRADOR");

        validAuditDTO = new AuditRegistroDTO();
        // getId() devuelve REGISTRO_ID gracias al stub configurado por cada test
    }

    // ==================== REGISTRAR ACCIÓN ====================

    @Test
    @DisplayName("REGISTRAR - datos válidos deben retornar el registro guardado")
    void registrarAccion_DatosValidos_RetornaRegistroGuardado() {
        // ARRANGE
        when(auditRegistroDAO.save(any(AuditRegistroCreateDTO.class)))
                .thenReturn(validAuditDTO);

        // ACT
        AuditRegistroDTO result = auditRegistroService.registrarAccion(validCreateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        verify(documentoService, times(1)).getDocumentoById(DOCUMENTO_ID);
        verify(usuarioService,   times(1)).getUsuarioByCedula(USUARIO_CEDULA);
        verify(auditRegistroDAO, times(1)).save(any(AuditRegistroCreateDTO.class));
    }

    @Test
    @DisplayName("REGISTRAR - documentoId null debe lanzar IllegalArgumentException")
    void registrarAccion_DocumentoIdNull_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setDocumentoId(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> auditRegistroService.registrarAccion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID del documento es obligatorio");

        // No debe consultar nada ni persistir
        verify(documentoService, never()).getDocumentoById(any());
        verify(usuarioService,   never()).getUsuarioByCedula(any());
        verify(auditRegistroDAO, never()).save(any());
    }

    @Test
    @DisplayName("REGISTRAR - usuarioCedula null debe lanzar IllegalArgumentException")
    void registrarAccion_UsuarioCedulaNull_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setUsuarioCedula(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> auditRegistroService.registrarAccion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula del usuario es obligatoria");

        verify(documentoService, never()).getDocumentoById(any());
        verify(auditRegistroDAO, never()).save(any());
    }

    @Test
    @DisplayName("REGISTRAR - accion null debe lanzar IllegalArgumentException")
    void registrarAccion_AccionNull_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setAccion(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> auditRegistroService.registrarAccion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acción debe ser descrita");

        verify(auditRegistroDAO, never()).save(any());
    }

    @Test
    @DisplayName("REGISTRAR - accion vacía debe lanzar IllegalArgumentException")
    void registrarAccion_AccionVacia_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setAccion("   ");

        // ACT & ASSERT
        assertThatThrownBy(() -> auditRegistroService.registrarAccion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acción debe ser descrita");

        verify(auditRegistroDAO, never()).save(any());
    }

    @Test
    @DisplayName("REGISTRAR - documento inexistente debe lanzar RuntimeException sin persistir")
    void registrarAccion_DocumentoInexistente_LanzaRuntimeExceptionSinPersistir() {
        // ARRANGE
        when(documentoService.getDocumentoById(DOCUMENTO_ID))
                .thenThrow(new RuntimeException("Documento no encontrado con ID: " + DOCUMENTO_ID));

        // ACT & ASSERT
        assertThatThrownBy(() -> auditRegistroService.registrarAccion(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Documento no encontrado");

        // El DAO nunca debe ser llamado
        verify(auditRegistroDAO, never()).save(any());
    }

    @Test
    @DisplayName("REGISTRAR - usuario inexistente debe lanzar RuntimeException sin persistir")
    void registrarAccion_UsuarioInexistente_LanzaRuntimeExceptionSinPersistir() {
        // ARRANGE — el documento sí existe, el usuario no
        when(usuarioService.getUsuarioByCedula(USUARIO_CEDULA))
                .thenThrow(new RuntimeException("Usuario no encontrado con cédula: " + USUARIO_CEDULA));

        // ACT & ASSERT
        assertThatThrownBy(() -> auditRegistroService.registrarAccion(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");

        verify(auditRegistroDAO, never()).save(any());
    }

    @Test
    @DisplayName("REGISTRAR - el service asigna creadoEn antes de llamar al DAO")
    void registrarAccion_DatosValidos_AsignaCreadoEnAntesDeGuardar() {
        // ARRANGE
        when(auditRegistroDAO.save(any(AuditRegistroCreateDTO.class)))
                .thenReturn(validAuditDTO);

        // ACT
        auditRegistroService.registrarAccion(validCreateDTO);

        // ASSERT — creadoEn debe haber sido asignado internamente
        assertThat(validCreateDTO.getCreadoEn()).isNotNull();
        verify(auditRegistroDAO, times(1)).save(validCreateDTO);
    }

    // ==================== GET REGISTRO BY ID ====================

    @Test
    @DisplayName("GET BY ID - registro existente debe retornar DTO")
    void getRegistroById_Existente_RetornaDTO() {
        // ARRANGE
        when(auditRegistroDAO.findById(REGISTRO_ID))
                .thenReturn(Optional.of(validAuditDTO));

        // ACT
        AuditRegistroDTO result = auditRegistroService.getRegistroById(REGISTRO_ID);

        // ASSERT
        assertThat(result).isNotNull();
        verify(auditRegistroDAO, times(1)).findById(REGISTRO_ID);
    }

    @Test
    @DisplayName("GET BY ID - registro inexistente debe lanzar RuntimeException")
    void getRegistroById_Inexistente_LanzaRuntimeException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(auditRegistroDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> auditRegistroService.getRegistroById(idInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Registro de auditoría no encontrado con ID: " + idInexistente);

        verify(auditRegistroDAO, times(1)).findById(idInexistente);
    }

    // ==================== HISTORIAL POR DOCUMENTO ====================

    @Test
    @DisplayName("HISTORIAL - documento existente debe retornar lista en orden ascendente")
    void getHistorialByDocumento_DocumentoExistente_RetornaListaAscendente() {
        // ARRANGE
        List<AuditRegistroDTO> historial = Arrays.asList(
                new AuditRegistroDTO(),
                new AuditRegistroDTO()
        );
        when(auditRegistroDAO.findByDocumentoIdAsc(DOCUMENTO_ID))
                .thenReturn(historial);

        // ACT
        List<AuditRegistroDTO> result = auditRegistroService.getHistorialByDocumento(DOCUMENTO_ID);

        // ASSERT
        assertThat(result).hasSize(2);
        verify(documentoService,  times(1)).getDocumentoById(DOCUMENTO_ID);
        verify(auditRegistroDAO,  times(1)).findByDocumentoIdAsc(DOCUMENTO_ID);
    }

    @Test
    @DisplayName("HISTORIAL - documento inexistente debe lanzar RuntimeException sin consultar DAO")
    void getHistorialByDocumento_DocumentoInexistente_LanzaRuntimeExceptionSinConsultarDAO() {
        // ARRANGE
        Long docInexistente = 888L;
        when(documentoService.getDocumentoById(docInexistente))
                .thenThrow(new RuntimeException("Documento no encontrado con ID: " + docInexistente));

        // ACT & ASSERT
        assertThatThrownBy(() -> auditRegistroService.getHistorialByDocumento(docInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Documento no encontrado");

        verify(auditRegistroDAO, never()).findByDocumentoIdAsc(any());
    }

    // ==================== TRAZABILIDAD POR USUARIO ====================

    @Test
    @DisplayName("TRAZABILIDAD USUARIO - usuario existente debe retornar lista de acciones")
    void getTrazabilidadByUsuario_UsuarioExistente_RetornaLista() {
        // ARRANGE
        List<AuditRegistroDTO> acciones = Arrays.asList(
                new AuditRegistroDTO(),
                new AuditRegistroDTO(),
                new AuditRegistroDTO()
        );
        when(auditRegistroDAO.findByUsuarioCedula(USUARIO_CEDULA))
                .thenReturn(acciones);

        // ACT
        List<AuditRegistroDTO> result = auditRegistroService.getTrazabilidadByUsuario(USUARIO_CEDULA);

        // ASSERT
        assertThat(result).hasSize(3);
        verify(usuarioService,   times(1)).getUsuarioByCedula(USUARIO_CEDULA);
        verify(auditRegistroDAO, times(1)).findByUsuarioCedula(USUARIO_CEDULA);
    }

    @Test
    @DisplayName("TRAZABILIDAD USUARIO - usuario inexistente debe lanzar RuntimeException")
    void getTrazabilidadByUsuario_UsuarioInexistente_LanzaRuntimeException() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(USUARIO_CEDULA))
                .thenThrow(new RuntimeException("Usuario no encontrado con cédula: " + USUARIO_CEDULA));

        // ACT & ASSERT
        assertThatThrownBy(() -> auditRegistroService.getTrazabilidadByUsuario(USUARIO_CEDULA))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");

        verify(auditRegistroDAO, never()).findByUsuarioCedula(any());
    }

    // ==================== TRAZABILIDAD COMPLETA ====================

    @Test
    @DisplayName("TRAZABILIDAD COMPLETA - rango de fechas válido debe retornar lista")
    void getTrazabilidadCompleta_RangoFechasValido_RetornaLista() {
        // ARRANGE
        LocalDateTime desde = LocalDateTime.now().minusDays(7);
        LocalDateTime hasta = LocalDateTime.now();
        List<AuditRegistroDTO> registros = List.of(new AuditRegistroDTO());

        when(auditRegistroDAO.trazabilidadCompleta("CREACION", desde, hasta))
                .thenReturn(registros);

        // ACT
        List<AuditRegistroDTO> result =
                auditRegistroService.getTrazabilidadCompleta("CREACION", desde, hasta);

        // ASSERT
        assertThat(result).hasSize(1);
        verify(auditRegistroDAO, times(1)).trazabilidadCompleta("CREACION", desde, hasta);
    }

    @Test
    @DisplayName("TRAZABILIDAD COMPLETA - filtros null deben retornar todos los registros")
    void getTrazabilidadCompleta_FiltrosNull_RetornaTodos() {
        // ARRANGE
        List<AuditRegistroDTO> todos = Arrays.asList(
                new AuditRegistroDTO(), new AuditRegistroDTO(), new AuditRegistroDTO()
        );
        when(auditRegistroDAO.trazabilidadCompleta(null, null, null))
                .thenReturn(todos);

        // ACT
        List<AuditRegistroDTO> result =
                auditRegistroService.getTrazabilidadCompleta(null, null, null);

        // ASSERT
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("TRAZABILIDAD COMPLETA - fecha 'desde' posterior a 'hasta' debe lanzar IllegalArgumentException")
    void getTrazabilidadCompleta_DesdePosterioresAHasta_LanzaIllegalArgumentException() {
        // ARRANGE — rango inválido: desde > hasta
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = LocalDateTime.now().minusDays(1);

        // ACT & ASSERT
        assertThatThrownBy(() ->
                auditRegistroService.getTrazabilidadCompleta(null, desde, hasta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'desde' no puede ser posterior a la fecha 'hasta'");

        verify(auditRegistroDAO, never()).trazabilidadCompleta(any(), any(), any());
    }
}