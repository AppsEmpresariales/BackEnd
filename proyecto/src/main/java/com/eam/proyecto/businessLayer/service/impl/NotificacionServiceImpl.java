// NotificacionServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.NotificacionCreateDTO;
import com.eam.proyecto.businessLayer.dto.NotificacionDTO;
import com.eam.proyecto.businessLayer.service.DocumentoService;
import com.eam.proyecto.businessLayer.service.NotificacionService;
import com.eam.proyecto.businessLayer.service.PlantillaCorreoService;
import com.eam.proyecto.businessLayer.service.UsuarioService;
import com.eam.proyecto.persistenceLayer.dao.NotificacionDAO;
import com.eam.proyecto.persistenceLayer.entity.enums.CanalNotificacionEnum;
import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionDAO notificacionDAO;
    private final UsuarioService usuarioService;
    private final DocumentoService documentoService;
    private final PlantillaCorreoService plantillaCorreoService;

    /**
     * ENVIAR NOTIFICACIÓN — Crear y registrar notificación para un usuario.
     *
     * FLUJO:
     * 1. Validar datos obligatorios
     * 2. Verificar que el usuario destinatario existe
     * 3. Si hay documentoId, verificar que el documento existe
     * 4. Si canal=EMAIL y hay tipoEvento, resolver la plantilla activa — RF40
     * 5. Asignar estaLeida=false y enviadaA=LocalDateTime.now()
     * 6. Persistir
     *
     * RF37 / RF38 / RF39
     */
    @Override
    public NotificacionDTO enviarNotificacion(NotificacionCreateDTO createDTO) {
        log.info("Enviando notificación canal {} al usuario cédula {}",
                createDTO.getCanal(), createDTO.getUsuarioCedula());

        validateNotificacionData(createDTO);

        usuarioService.getUsuarioByCedula(createDTO.getUsuarioCedula());

        // Si viene con documentoId, verificar que existe
        if (createDTO.getDocumentoId() != null) {
            documentoService.getDocumentoById(createDTO.getDocumentoId());
        }

        // Si es EMAIL y tiene tipoEvento y organizacionNit, intentar enriquecer
        // con la plantilla activa (asunto + cuerpo) — RF40
        if (CanalNotificacionEnum.EMAIL.equals(createDTO.getCanal())
                && createDTO.getTipoEvento() != null
                && createDTO.getOrganizacionNit() != null
                && (createDTO.getMensaje() == null || createDTO.getMensaje().trim().isEmpty())) {

            try {
                var plantilla = plantillaCorreoService
                        .getPlantillaActivaByOrganizacionAndEvento(
                                createDTO.getOrganizacionNit(), createDTO.getTipoEvento());
                createDTO.setMensaje(plantilla.getCuerpo());
                createDTO.setAsunto(plantilla.getAsunto());
                log.debug("Plantilla aplicada para evento {}: plantilla ID {}",
                        createDTO.getTipoEvento(), plantilla.getId());
            } catch (IllegalStateException e) {
                // Sin plantilla configurada: se envía igual con el mensaje que traiga
                log.warn("Sin plantilla activa para evento {}. Se enviará sin plantilla.", createDTO.getTipoEvento());
            }
        }

        // El service asigna los campos gestionados internamente
        createDTO.setEstaLeida(false);
        createDTO.setEnviadaA(LocalDateTime.now());

        NotificacionDTO result = notificacionDAO.save(createDTO);

        log.info("Notificación registrada exitosamente con ID: {}", result.getId());
        return result;
    }

    /**
     * READ — Buscar notificación por ID.
     */
    @Override
    @Transactional(readOnly = true)
    public NotificacionDTO getNotificacionById(Long id) {
        log.debug("Buscando notificación por ID: {}", id);

        return notificacionDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("Notificación no encontrada con ID: {}", id);
                    return new RuntimeException("Notificación no encontrada con ID: " + id);
                });
    }

    /**
     * READ ALL — Centro de notificaciones del usuario (más recientes primero) — RF39.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificacionDTO> getNotificacionesByUsuario(Long cedula) {
        log.debug("Obteniendo notificaciones del usuario cédula: {}", cedula);
        usuarioService.getUsuarioByCedula(cedula);
        return notificacionDAO.findByUsuarioCedula(cedula);
    }

    /**
     * READ NO LEÍDAS — Para el badge del campana en la interfaz — RF39.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificacionDTO> getNoLeidasByUsuario(Long cedula) {
        log.debug("Obteniendo notificaciones no leídas del usuario cédula: {}", cedula);
        usuarioService.getUsuarioByCedula(cedula);
        return notificacionDAO.findNoLeidasByUsuarioCedula(cedula);
    }

    /**
     * COUNT NO LEÍDAS — Contador numérico del badge de notificaciones — RF39.
     */
    @Override
    @Transactional(readOnly = true)
    public long countNoLeidasByUsuario(Long cedula) {
        log.debug("Contando notificaciones no leídas del usuario cédula: {}", cedula);
        usuarioService.getUsuarioByCedula(cedula);
        return notificacionDAO.countNoLeidasByUsuarioCedula(cedula);
    }

    /**
     * MARCAR LEÍDA — El usuario abre una notificación individual.
     */
    @Override
    public NotificacionDTO marcarLeida(Long id) {
        log.info("Marcando como leída la notificación ID: {}", id);

        NotificacionDTO notificacion = getNotificacionById(id);

        if (Boolean.TRUE.equals(notificacion.getEstaLeida())) {
            log.debug("Notificación ID {} ya estaba marcada como leída", id);
            return notificacion;
        }

        NotificacionDTO result = notificacionDAO.marcarLeida(id)
                .orElseThrow(() -> new RuntimeException("Error al marcar como leída la notificación ID: " + id));

        log.info("Notificación ID {} marcada como leída", id);
        return result;
    }

    /**
     * MARCAR TODAS LEÍDAS — Botón "Marcar todo como leído" — RF39.
     */
    @Override
    public void marcarTodasComoLeidas(Long cedula) {
        log.info("Marcando todas las notificaciones como leídas para el usuario cédula: {}", cedula);

        usuarioService.getUsuarioByCedula(cedula);
        notificacionDAO.marcarTodasComoLeidas(cedula);

        log.info("Todas las notificaciones del usuario cédula {} marcadas como leídas", cedula);
    }

    /**
     * DELETE — Eliminar notificación.
     *
     * RESTRICCIÓN: Solo para limpiezas administrativas.
     */
    @Override
    public void deleteNotificacion(Long id) {
        log.info("Eliminando notificación ID: {}", id);

        getNotificacionById(id);

        boolean deleted = notificacionDAO.deleteById(id);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar notificación ID: " + id);
        }

        log.info("Notificación eliminada exitosamente ID: {}", id);
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validateNotificacionData(NotificacionCreateDTO dto) {
        if (dto.getUsuarioCedula() == null) {
            throw new IllegalArgumentException("La cédula del usuario destinatario es obligatoria");
        }
        if (dto.getCanal() == null) {
            throw new IllegalArgumentException("El canal de notificación es obligatorio (EMAIL o SISTEMA)");
        }
        if (dto.getMensaje() == null || dto.getMensaje().trim().isEmpty()) {
            // Permitido si se resolverá con plantilla, pero se valida en tiempo de ejecución
            if (dto.getTipoEvento() == null || dto.getOrganizacionNit() == null) {
                throw new IllegalArgumentException(
                        "El mensaje es obligatorio si no se especifica tipoEvento y organizacionNit para resolver plantilla");
            }
        }
    }
}