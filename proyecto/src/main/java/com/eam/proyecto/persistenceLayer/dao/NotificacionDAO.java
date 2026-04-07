package com.docucloud.persistence.dao;

import com.docucloud.businessLayer.dto.NotificacionCreateDTO;
import com.docucloud.businessLayer.dto.NotificacionDTO;
import com.docucloud.businessLayer.mapper.NotificacionMapper;
import com.docucloud.persistence.entity.DocumentoEntity;
import com.docucloud.persistence.entity.NotificacionEntity;
import com.docucloud.persistence.entity.UsuarioEntity;
import com.docucloud.persistence.enums.CanalNotificacionEnum;
import com.docucloud.persistence.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de notificaciones.
 *
 * DESCRIPCION:
 * - NotificacionEntity es casi-inmutable: se crea y se marca como leída,
 *   pero no se edita ni elimina en operaciones normales.
 * - estaLeida se gestiona mediante marcarLeida() y marcarTodasComoLeidas().
 * - El campo enviadaA lo asigna el service al momento del envío real.
 * - Soporta dos canales: EMAIL y SISTEMA (CanalNotificacionEnum).
 *
 * HISTORIAS CUBIERTAS:
 * - US-037 (RF37): Correo al crear documento → save(createDTO) canal EMAIL
 * - US-038 (RF38): Notificar cambios de estado → save(createDTO)
 * - US-039 (RF39): Alertas de tareas pendientes → save(createDTO) + findPendientesByUsuario
 * - US-040 (RF40): Gestionar plantillas de correo → referencia a PlantillaCorreoEntity
 */
@Repository
@RequiredArgsConstructor
public class NotificacionDAO {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionMapper notificacionMapper;

    /**
     * Crear y registrar una notificación para un usuario.
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper ignora id, enviadaA y estaLeida)
     * 2. El service asigna estaLeida=false y enviadaA=LocalDateTime.now()
     * 3. Guardar Entity → DTO
     *
     * US-037 / US-038 / US-039
     */
    public NotificacionDTO save(NotificacionCreateDTO createDTO) {
        NotificacionEntity entity = notificacionMapper.toEntity(createDTO);
        return notificacionMapper.toDTO(notificacionRepository.save(entity));
    }

    /**
     * Buscar notificación por ID.
     */
    public Optional<NotificacionDTO> findById(Long id) {
        return notificacionRepository.findById(id)
                .map(notificacionMapper::toDTO);
    }

    /**
     * Marcar una notificación individual como leída.
     *
     * CASO DE USO: El usuario abre una notificación en el centro de mensajes.
     */
    public Optional<NotificacionDTO> marcarLeida(Long id) {
        return notificacionRepository.findById(id)
                .map(existing -> {
                    existing.setEstaLeida(true);
                    return notificacionMapper.toDTO(notificacionRepository.save(existing));
                });
    }

    /**
     * Marcar todas las notificaciones de un usuario como leídas.
     *
     * CASO DE USO: Botón "Marcar todo como leído" en el centro de notificaciones.
     * IMPLEMENTACIÓN: Usa query @Modifying para actualización masiva eficiente.
     */
    public void marcarTodasComoLeidas(Long cedula) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        notificacionRepository.marcarTodasComoLeidas(usuarioRef);
    }

    /**
     * Eliminar notificación por ID.
     *
     * RESTRICCIÓN: Solo para limpiezas administrativas o notificaciones de prueba.
     */
    public boolean deleteById(Long id) {
        if (notificacionRepository.existsById(id)) {
            notificacionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Listar todas las notificaciones de un usuario (más recientes primero).
     *
     * CASO DE USO: Centro de notificaciones del usuario.
     * US-039
     */
    public List<NotificacionDTO> findByUsuarioCedula(Long cedula) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return notificacionMapper.toDTOList(
                notificacionRepository.findByUsuarioOrderByEnviadaADesc(usuarioRef));
    }

    /**
     * Listar notificaciones no leídas de un usuario.
     *
     * CASO DE USO: Badge del campana — muestra el número de no leídas.
     * US-039
     */
    public List<NotificacionDTO> findNoLeidasByUsuarioCedula(Long cedula) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return notificacionMapper.toDTOList(
                notificacionRepository.findByUsuarioAndEstaLeidaFalse(usuarioRef));
    }

    /**
     * Listar notificaciones leídas de un usuario.
     *
     * CASO DE USO: Historial completo de notificaciones pasadas.
     */
    public List<NotificacionDTO> findLeidasByUsuarioCedula(Long cedula) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return notificacionMapper.toDTOList(
                notificacionRepository.findByUsuarioAndEstaLeidaTrue(usuarioRef));
    }

    /**
     * Listar notificaciones de un usuario por canal específico.
     *
     * CASO DE USO: Ver solo notificaciones de sistema o solo correos enviados.
     */
    public List<NotificacionDTO> findByUsuarioCedulaAndCanal(Long cedula, CanalNotificacionEnum canal) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return notificacionMapper.toDTOList(
                notificacionRepository.findByUsuarioAndCanal(usuarioRef, canal));
    }

    /**
     * Listar notificaciones no leídas de un canal específico del usuario.
     *
     * CASO DE USO: Filtrar alertas de sistema pendientes de revisar.
     * US-039
     */
    public List<NotificacionDTO> findNoLeidasByUsuarioCedulaAndCanal(
            Long cedula, CanalNotificacionEnum canal) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return notificacionMapper.toDTOList(
                notificacionRepository.findByUsuarioAndCanalAndEstaLeidaFalse(usuarioRef, canal));
    }

    /**
     * Listar todas las notificaciones relacionadas con un documento.
     *
     * US-038: Ver qué notificaciones se han enviado por cambios de estado del documento.
     */
    public List<NotificacionDTO> findByDocumentoId(Long documentoId) {
        DocumentoEntity docRef = buildDocumentoRef(documentoId);
        return notificacionMapper.toDTOList(
                notificacionRepository.findByDocumento(docRef));
    }

    /**
     * Listar notificaciones de un canal sin fecha de envío (pendientes de procesar).
     *
     * CASO DE USO: Cola de envío — el scheduler recoge y envía las notificaciones
     * que aún no tienen enviadaA asignado (US-039).
     */
    public List<NotificacionDTO> findPendientesDeEnvioByUsuarioCedulaAndCanal(
            Long cedula, CanalNotificacionEnum canal) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return notificacionMapper.toDTOList(
                notificacionRepository.findByUsuarioAndCanalAndEnviadaAIsNull(usuarioRef, canal));
    }

    /**
     * Contar notificaciones no leídas de un usuario.
     *
     * CASO DE USO: Badge numérico en el icono de notificaciones del header.
     * US-039
     */
    public long countNoLeidasByUsuarioCedula(Long cedula) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return notificacionRepository.countByUsuarioAndEstaLeidaFalse(usuarioRef);
    }

    /**
     * Contar total de notificaciones.
     */
    public long count() {
        return notificacionRepository.count();
    }

    // ─── Métodos auxiliares privados ─────────────────────────────────────────

    private UsuarioEntity buildUsuarioRef(Long cedula) {
        UsuarioEntity u = new UsuarioEntity();
        u.setCedula(cedula);
        return u;
    }

    private DocumentoEntity buildDocumentoRef(Long id) {
        DocumentoEntity d = new DocumentoEntity();
        d.setId(id);
        return d;
    }
}
