package com.eam.proyecto;

import com.eam.proyecto.businessLayer.dto.NotificacionCreateDTO;
import com.eam.proyecto.businessLayer.dto.NotificacionDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoDTO;
import com.eam.proyecto.businessLayer.service.DocumentoService;
import com.eam.proyecto.businessLayer.service.PlantillaCorreoService;
import com.eam.proyecto.businessLayer.service.UsuarioService;
import com.eam.proyecto.businessLayer.service.impl.NotificacionServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.NotificacionDAO;
import com.eam.proyecto.persistenceLayer.entity.enums.CanalNotificacionEnum;
import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests para NotificacionServiceImpl
 *
 * OPERACIONES BAJO PRUEBA (RF37 / RF38 / RF39 / RF40):
 * - enviarNotificacion : crea y registra la notificación.
 * - getNotificacionById : búsqueda por ID.
 * - getNoLeidasByUsuario : centro de notificaciones (badge de campana).
 * - marcarLeida : marcar una notificación como leída.
 * - marcarTodasComoLeidas : marcar todo como leído.
 * - deleteNotificacion : eliminación administrativa.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionService - Unit Tests")
class NotificacionServiceTest {

    // ─── Dependencias mockeadas ───────────────────────────────────────────────

    @Mock
    private NotificacionDAO notificacionDAO;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private DocumentoService documentoService;

    @Mock
    private PlantillaCorreoService plantillaCorreoService;

    // ─── Sistema bajo prueba (SUT) ────────────────────────────────────────────

    @InjectMocks
    private NotificacionServiceImpl notificacionService;

    // ─── Datos de prueba reutilizables ────────────────────────────────────────

    private NotificacionCreateDTO validCreateDTO;
    private NotificacionDTO validNotificacionDTO;

    private final Long USUARIO_CEDULA   = 987654321L;
    private final Long DOCUMENTO_ID     = 20L;
    private final Long NOTIFICACION_ID  = 1L;
    private final Long ORGANIZACION_NIT = 900123456L;

    /**
     * Se ejecuta antes de cada test.
     * Construye un DTO de creación completo y un DTO de respuesta base.
     */
    @BeforeEach
    void setUp() {
        validCreateDTO = new NotificacionCreateDTO();
        validCreateDTO.setUsuarioCedula(USUARIO_CEDULA);
        validCreateDTO.setCanal(CanalNotificacionEnum.SISTEMA);
        validCreateDTO.setMensaje("Su documento ha cambiado de estado");

        validNotificacionDTO = new NotificacionDTO();
        // Los campos id, estaLeida, etc. se configura vía stub en cada test
    }

    // ==================== ENVIAR NOTIFICACIÓN ====================

    @Test
    @DisplayName("ENVIAR - datos válidos con canal SISTEMA deben retornar notificación persistida")
    void enviarNotificacion_DatosValidosSistema_RetornaNotificacionGuardada() {
        // ARRANGE
        when(notificacionDAO.save(any(NotificacionCreateDTO.class)))
                .thenReturn(validNotificacionDTO);

        // ACT
        NotificacionDTO result = notificacionService.enviarNotificacion(validCreateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        verify(usuarioService,   times(1)).getUsuarioByCedula(USUARIO_CEDULA);
        verify(documentoService, never()).getDocumentoById(any());   // sin documentoId
        verify(notificacionDAO,  times(1)).save(any(NotificacionCreateDTO.class));
    }

    @Test
    @DisplayName("ENVIAR - con documentoId válido debe verificar existencia del documento")
    void enviarNotificacion_ConDocumentoId_VerificaExistenciaDocumento() {
        // ARRANGE
        validCreateDTO.setDocumentoId(DOCUMENTO_ID);
        when(notificacionDAO.save(any(NotificacionCreateDTO.class)))
                .thenReturn(validNotificacionDTO);

        // ACT
        notificacionService.enviarNotificacion(validCreateDTO);

        // ASSERT
        verify(documentoService, times(1)).getDocumentoById(DOCUMENTO_ID);
    }

    @Test
    @DisplayName("ENVIAR - canal EMAIL con tipoEvento y plantilla disponible aplica plantilla")
    void enviarNotificacion_EmailConPlantillaDisponible_AplicaPlantilla() {
        // ARRANGE — mensaje vacío, pero hay plantilla para el evento
        validCreateDTO.setCanal(CanalNotificacionEnum.EMAIL);
        validCreateDTO.setMensaje(null);
        validCreateDTO.setTipoEvento(TipoEventoEnum.DOCUMENTO_CREADO);
        validCreateDTO.setOrganizacionNit(ORGANIZACION_NIT);

        PlantillaCorreoDTO plantilla = new PlantillaCorreoDTO();
        plantilla.setAsunto("Nuevo documento registrado");
        plantilla.setCuerpo("Se ha creado el documento en el sistema.");

        when(plantillaCorreoService.getPlantillaActivaByOrganizacionAndEvento(
                ORGANIZACION_NIT, TipoEventoEnum.DOCUMENTO_CREADO))
                .thenReturn(plantilla);
        when(notificacionDAO.save(any(NotificacionCreateDTO.class)))
                .thenReturn(validNotificacionDTO);

        // ACT
        NotificacionDTO result = notificacionService.enviarNotificacion(validCreateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        verify(plantillaCorreoService, times(1))
                .getPlantillaActivaByOrganizacionAndEvento(
                        ORGANIZACION_NIT, TipoEventoEnum.DOCUMENTO_CREADO);
        verify(notificacionDAO, times(1)).save(any(NotificacionCreateDTO.class));
    }

    @Test
    @DisplayName("ENVIAR - canal EMAIL sin plantilla configurada debe enviar igual (sin error crítico)")
    void enviarNotificacion_EmailSinPlantilla_EnviaIgualSinLanzarError() {
        // ARRANGE — el service captura IllegalStateException de plantillaCorreoService
        validCreateDTO.setCanal(CanalNotificacionEnum.EMAIL);
        validCreateDTO.setMensaje(null);
        validCreateDTO.setTipoEvento(TipoEventoEnum.TAREA_ASIGNADA);
        validCreateDTO.setOrganizacionNit(ORGANIZACION_NIT);

        when(plantillaCorreoService.getPlantillaActivaByOrganizacionAndEvento(
                ORGANIZACION_NIT, TipoEventoEnum.TAREA_ASIGNADA))
                .thenThrow(new IllegalStateException("No hay plantilla activa para el evento"));
        when(notificacionDAO.save(any(NotificacionCreateDTO.class)))
                .thenReturn(validNotificacionDTO);

        // ACT — no debe propagarse la excepción
        assertThatCode(() -> notificacionService.enviarNotificacion(validCreateDTO))
                .doesNotThrowAnyException();

        // El DAO sí debe ser llamado aunque no haya plantilla
        verify(notificacionDAO, times(1)).save(any(NotificacionCreateDTO.class));
    }

    @Test
    @DisplayName("ENVIAR - usuarioCedula null debe lanzar IllegalArgumentException")
    void enviarNotificacion_UsuarioCedulaNull_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setUsuarioCedula(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> notificacionService.enviarNotificacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula del usuario destinatario es obligatoria");

        verify(usuarioService,   never()).getUsuarioByCedula(any());
        verify(notificacionDAO,  never()).save(any());
    }

    @Test
    @DisplayName("ENVIAR - canal null debe lanzar IllegalArgumentException")
    void enviarNotificacion_CanalNull_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setCanal(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> notificacionService.enviarNotificacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canal de notificación es obligatorio");

        verify(notificacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("ENVIAR - mensaje null sin tipoEvento/orgNit debe lanzar IllegalArgumentException")
    void enviarNotificacion_MensajeNullSinPlantillaOrigen_LanzaIllegalArgumentException() {
        // ARRANGE — mensaje nulo, sin datos para resolver plantilla
        validCreateDTO.setMensaje(null);
        validCreateDTO.setTipoEvento(null);
        validCreateDTO.setOrganizacionNit(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> notificacionService.enviarNotificacion(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mensaje es obligatorio");

        verify(notificacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("ENVIAR - usuario destinatario inexistente debe lanzar RuntimeException sin persistir")
    void enviarNotificacion_UsuarioInexistente_LanzaRuntimeExceptionSinPersistir() {
        // ARRANGE
        when(usuarioService.getUsuarioByCedula(USUARIO_CEDULA))
                .thenThrow(new RuntimeException("Usuario no encontrado con cédula: " + USUARIO_CEDULA));

        // ACT & ASSERT
        assertThatThrownBy(() -> notificacionService.enviarNotificacion(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario no encontrado");

        verify(notificacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("ENVIAR - el service asigna estaLeida=false y enviadaA antes de persistir")
    void enviarNotificacion_DatosValidos_AsignaCamposInternosAntesDeGuardar() {
        // ARRANGE
        when(notificacionDAO.save(any(NotificacionCreateDTO.class)))
                .thenReturn(validNotificacionDTO);

        // ACT
        notificacionService.enviarNotificacion(validCreateDTO);

        // ASSERT — el service debe haber mutado el DTO antes de pasarlo al DAO
        assertThat(validCreateDTO.getEstaLeida()).isFalse();
        assertThat(validCreateDTO.getEnviadaA()).isNotNull();
    }

    // ==================== GET NOTIFICACIÓN BY ID ====================

    @Test
    @DisplayName("GET BY ID - notificación existente debe retornar DTO")
    void getNotificacionById_Existente_RetornaDTO() {
        // ARRANGE
        when(notificacionDAO.findById(NOTIFICACION_ID))
                .thenReturn(Optional.of(validNotificacionDTO));

        // ACT
        NotificacionDTO result = notificacionService.getNotificacionById(NOTIFICACION_ID);

        // ASSERT
        assertThat(result).isNotNull();
        verify(notificacionDAO, times(1)).findById(NOTIFICACION_ID);
    }

    @Test
    @DisplayName("GET BY ID - notificación inexistente debe lanzar RuntimeException")
    void getNotificacionById_Inexistente_LanzaRuntimeException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(notificacionDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> notificacionService.getNotificacionById(idInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Notificación no encontrada con ID: " + idInexistente);
    }

    // ==================== NO LEÍDAS POR USUARIO ====================

    @Test
    @DisplayName("NO LEÍDAS - usuario válido debe retornar lista de notificaciones no leídas")
    void getNoLeidasByUsuario_UsuarioValido_RetornaLista() {
        // ARRANGE
        List<NotificacionDTO> noLeidas = Arrays.asList(
                new NotificacionDTO(), new NotificacionDTO()
        );
        when(notificacionDAO.findNoLeidasByUsuarioCedula(USUARIO_CEDULA))
                .thenReturn(noLeidas);

        // ACT
        List<NotificacionDTO> result =
                notificacionService.getNoLeidasByUsuario(USUARIO_CEDULA);

        // ASSERT
        assertThat(result).hasSize(2);
        verify(usuarioService,   times(1)).getUsuarioByCedula(USUARIO_CEDULA);
        verify(notificacionDAO,  times(1)).findNoLeidasByUsuarioCedula(USUARIO_CEDULA);
    }

    @Test
    @DisplayName("NO LEÍDAS - usuario sin notificaciones debe retornar lista vacía")
    void getNoLeidasByUsuario_SinNotificaciones_RetornaListaVacia() {
        // ARRANGE
        when(notificacionDAO.findNoLeidasByUsuarioCedula(USUARIO_CEDULA))
                .thenReturn(Collections.emptyList());

        // ACT
        List<NotificacionDTO> result =
                notificacionService.getNoLeidasByUsuario(USUARIO_CEDULA);

        // ASSERT
        assertThat(result).isEmpty();
    }

    // ==================== MARCAR LEÍDA ====================

    @Test
    @DisplayName("MARCAR LEÍDA - notificación no leída debe actualizar estado y retornar DTO")
    void marcarLeida_NotificacionNoLeida_ActualizaYRetornaDTO() {
        // ARRANGE — la notificación existe y NO está leída
        NotificacionDTO noLeida = new NotificacionDTO();
        noLeida.setEstaLeida(false);

        NotificacionDTO marcadaLeida = new NotificacionDTO();
        marcadaLeida.setEstaLeida(true);

        when(notificacionDAO.findById(NOTIFICACION_ID))
                .thenReturn(Optional.of(noLeida));
        when(notificacionDAO.marcarLeida(NOTIFICACION_ID))
                .thenReturn(Optional.of(marcadaLeida));

        // ACT
        NotificacionDTO result = notificacionService.marcarLeida(NOTIFICACION_ID);

        // ASSERT
        assertThat(result).isNotNull();
        verify(notificacionDAO, times(1)).marcarLeida(NOTIFICACION_ID);
    }

    @Test
    @DisplayName("MARCAR LEÍDA - notificación ya leída debe retornar la misma sin llamar al DAO")
    void marcarLeida_NotificacionYaLeida_RetornaMismaSinActualizar() {
        // ARRANGE — la notificación ya estaba leída
        NotificacionDTO yaLeida = new NotificacionDTO();
        yaLeida.setEstaLeida(true);

        when(notificacionDAO.findById(NOTIFICACION_ID))
                .thenReturn(Optional.of(yaLeida));

        // ACT
        NotificacionDTO result = notificacionService.marcarLeida(NOTIFICACION_ID);

        // ASSERT — el DAO de actualización no debe ser invocado
        assertThat(result).isNotNull();
        assertThat(result.getEstaLeida()).isTrue();
        verify(notificacionDAO, never()).marcarLeida(any());
    }

    @Test
    @DisplayName("MARCAR LEÍDA - notificación inexistente debe lanzar RuntimeException")
    void marcarLeida_Inexistente_LanzaRuntimeException() {
        // ARRANGE
        Long idInexistente = 777L;
        when(notificacionDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> notificacionService.marcarLeida(idInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Notificación no encontrada con ID: " + idInexistente);

        verify(notificacionDAO, never()).marcarLeida(any());
    }

    // ==================== MARCAR TODAS COMO LEÍDAS ====================

    @Test
    @DisplayName("MARCAR TODAS - usuario válido debe llamar al DAO para marcar todas como leídas")
    void marcarTodasComoLeidas_UsuarioValido_InvocaDAO() {
        // ARRANGE — doNothing es el comportamiento por defecto de métodos void en mocks

        // ACT
        assertThatCode(() -> notificacionService.marcarTodasComoLeidas(USUARIO_CEDULA))
                .doesNotThrowAnyException();

        // ASSERT
        verify(usuarioService,  times(1)).getUsuarioByCedula(USUARIO_CEDULA);
        verify(notificacionDAO, times(1)).marcarTodasComoLeidas(USUARIO_CEDULA);
    }

    // ==================== DELETE NOTIFICACIÓN ====================

    @Test
    @DisplayName("DELETE - notificación existente debe eliminarse sin lanzar excepción")
    void deleteNotificacion_Existente_EliminaSinError() {
        // ARRANGE
        when(notificacionDAO.findById(NOTIFICACION_ID))
                .thenReturn(Optional.of(validNotificacionDTO));
        when(notificacionDAO.deleteById(NOTIFICACION_ID))
                .thenReturn(true);

        // ACT & ASSERT
        assertThatCode(() -> notificacionService.deleteNotificacion(NOTIFICACION_ID))
                .doesNotThrowAnyException();

        verify(notificacionDAO, times(1)).deleteById(NOTIFICACION_ID);
    }

    @Test
    @DisplayName("DELETE - error en DAO debe lanzar RuntimeException")
    void deleteNotificacion_ErrorEnDAO_LanzaRuntimeException() {
        // ARRANGE — el DAO devuelve false (no pudo eliminar)
        when(notificacionDAO.findById(NOTIFICACION_ID))
                .thenReturn(Optional.of(validNotificacionDTO));
        when(notificacionDAO.deleteById(NOTIFICACION_ID))
                .thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> notificacionService.deleteNotificacion(NOTIFICACION_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al eliminar notificación ID: " + NOTIFICACION_ID);
    }

    @Test
    @DisplayName("DELETE - notificación inexistente debe lanzar RuntimeException sin llamar deleteById")
    void deleteNotificacion_Inexistente_LanzaRuntimeExceptionSinLlamarDelete() {
        // ARRANGE
        Long idInexistente = 555L;
        when(notificacionDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> notificacionService.deleteNotificacion(idInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Notificación no encontrada con ID: " + idInexistente);

        verify(notificacionDAO, never()).deleteById(any());
    }
}