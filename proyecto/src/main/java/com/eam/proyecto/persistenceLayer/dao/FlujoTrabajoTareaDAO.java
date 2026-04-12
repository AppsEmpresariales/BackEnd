package com.eam.proyecto.persistenceLayer.dao;

import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaCreateDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaDTO;
import com.eam.proyecto.businessLayer.dto.FlujoTrabajoTareaUpdateDTO;
import com.eam.proyecto.persistenceLayer.mapper.FlujoTrabajoTareaMapper;
import com.eam.proyecto.persistenceLayer.entity.DocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoPasoEntity;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoTareaEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import com.eam.proyecto.persistenceLayer.entity.enums.EstadoTareaEnum;
import com.eam.proyecto.persistenceLayer.repository.FlujoTrabajoTareaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de tareas del flujo de trabajo.
 *
 * DESCRIPCION:
 * - FlujoTrabajoTareaEntity representa una tarea asignada a un usuario
 *   para ejecutar un paso del flujo sobre un documento.
 * - Estado: PENDIENTE → COMPLETADO o CANCELADO (EstadoTareaEnum).
 * - completadoEn: se asigna en el service al momento de completar la tarea.
 * - El usuario asignado (asignadoA) no se puede cambiar; se crea una nueva tarea.
 *
 * HISTORIAS CUBIERTAS:
 * - US-029 (RF29): Asignar tareas de revisión → save(createDTO)
 * - US-030 (RF30): Cambiar estado del documento → update (cambia estado de la tarea)
 * - US-031 (RF31): Validar secuencia → findActivaByDocumento, existeTareaPendiente
 * - US-039 (RF39): Enviar alertas de tareas pendientes → findPendientesByUsuarioCedula
 */
@Repository
@RequiredArgsConstructor
public class FlujoTrabajoTareaDAO {

    private final FlujoTrabajoTareaRepository flujoTrabajoTareaRepository;
    private final FlujoTrabajoTareaMapper flujoTrabajoTareaMapper;

    /**
     * Crear y asignar una tarea a un usuario dentro del flujo.
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper ignora estado, creadoEn y completadoEn)
     * 2. El service asigna estado=PENDIENTE y creadoEn=LocalDateTime.now()
     * 3. Guardar Entity → DTO
     *
     * US-029
     */
    public FlujoTrabajoTareaDTO save(FlujoTrabajoTareaCreateDTO createDTO) {
        FlujoTrabajoTareaEntity entity = flujoTrabajoTareaMapper.toEntity(createDTO);
        return flujoTrabajoTareaMapper.toDTO(flujoTrabajoTareaRepository.save(entity));
    }

    /**
     * Buscar tarea por ID.
     */
    public Optional<FlujoTrabajoTareaDTO> findById(Long id) {
        return flujoTrabajoTareaRepository.findById(id)
                .map(flujoTrabajoTareaMapper::toDTO);
    }

    /**
     * Actualizar estado o comentario de una tarea (completar / cancelar).
     *
     * RESTRICCIONES (aplicadas por el mapper):
     * - id, documento, paso, asignadoA, creadoEn, completadoEn son inmutables.
     * - completadoEn: el service lo fija con LocalDateTime.now() al completar.
     *
     * US-030
     */
    public Optional<FlujoTrabajoTareaDTO> update(Long id, FlujoTrabajoTareaUpdateDTO updateDTO) {
        return flujoTrabajoTareaRepository.findById(id)
                .map(existing -> {
                    flujoTrabajoTareaMapper.updateEntityFromDTO(updateDTO, existing);
                    return flujoTrabajoTareaMapper.toDTO(flujoTrabajoTareaRepository.save(existing));
                });
    }

    /**
     * Completar una tarea: cambia estado a COMPLETADO y registra fecha.
     *
     * CASO DE USO ESPECÍFICO: Flujo explícito para completar tareas.
     * US-030
     */
    public Optional<FlujoTrabajoTareaDTO> completar(Long id, String comentario) {
        return flujoTrabajoTareaRepository.findById(id)
                .map(existing -> {
                    existing.setEstado(EstadoTareaEnum.COMPLETADO);
                    existing.setCompletadoEn(LocalDateTime.now());
                    if (comentario != null) existing.setComentario(comentario);
                    return flujoTrabajoTareaMapper.toDTO(flujoTrabajoTareaRepository.save(existing));
                });
    }

    /**
     * Cancelar una tarea: cambia estado a CANCELADO.
     *
     * CASO DE USO: El administrador cancela una tarea bloqueada o redundante.
     */
    public Optional<FlujoTrabajoTareaDTO> cancelar(Long id) {
        return flujoTrabajoTareaRepository.findById(id)
                .map(existing -> {
                    existing.setEstado(EstadoTareaEnum.CANCELADO);
                    return flujoTrabajoTareaMapper.toDTO(flujoTrabajoTareaRepository.save(existing));
                });
    }

    /**
     * Eliminar tarea por ID.
     *
     * RESTRICCIÓN: Solo para correcciones administrativas. En general
     * las tareas se cancelan, no se eliminan físicamente.
     */
    public boolean deleteById(Long id) {
        if (flujoTrabajoTareaRepository.existsById(id)) {
            flujoTrabajoTareaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Listar todas las tareas de un documento (historial completo).
     *
     * US-034: Mostrar historial de actividad de revisión del documento.
     */
    public List<FlujoTrabajoTareaDTO> findByDocumentoId(Long documentoId) {
        DocumentoEntity docRef = buildDocumentoRef(documentoId);
        return flujoTrabajoTareaMapper.toDTOList(
                flujoTrabajoTareaRepository.findByDocumentoOrderByCreadoEnAsc(docRef));
    }

    /**
     * Listar tareas pendientes asignadas a un usuario.
     *
     * US-039: Alertas de tareas pendientes — base para notificaciones.
     */
    public List<FlujoTrabajoTareaDTO> findPendientesByUsuarioCedula(Long cedula) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return flujoTrabajoTareaMapper.toDTOList(
                flujoTrabajoTareaRepository.findByAsignadoAAndEstadoOrderByCreadoEnAsc(
                        usuarioRef, EstadoTareaEnum.PENDIENTE));
    }

    /**
     * Listar tareas por estado asignadas a un usuario.
     *
     * CASO DE USO: Ver tareas completadas, canceladas o pendientes del usuario.
     */
    public List<FlujoTrabajoTareaDTO> findByUsuarioCedulaAndEstado(Long cedula, EstadoTareaEnum estado) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return flujoTrabajoTareaMapper.toDTOList(
                flujoTrabajoTareaRepository.findByAsignadoAAndEstado(usuarioRef, estado));
    }

    /**
     * Obtener la tarea activa más reciente de un documento.
     *
     * US-031: Al avanzar el flujo, verificar cuál tarea pendiente está activa.
     */
    public Optional<FlujoTrabajoTareaDTO> findTareaActivaByDocumentoId(Long documentoId) {
        DocumentoEntity docRef = buildDocumentoRef(documentoId);
        return flujoTrabajoTareaRepository.findFirstByDocumentoAndEstadoOrderByCreadoEnDesc(
                        docRef, EstadoTareaEnum.PENDIENTE)
                .map(flujoTrabajoTareaMapper::toDTO);
    }

    /**
     * Listar tareas vencidas de un usuario.
     *
     * US-039: Base para el scheduler que envía alertas de vencimiento.
     * Incluye tareas con fechaLimite < ahora y estado PENDIENTE.
     */
    public List<FlujoTrabajoTareaDTO> findTareasVencidasByUsuarioCedula(Long cedula) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return flujoTrabajoTareaMapper.toDTOList(
                flujoTrabajoTareaRepository.findTareasVencidas(usuarioRef, LocalDateTime.now()));
    }

    /**
     * Verificar si un documento tiene alguna tarea pendiente.
     *
     * US-031: Antes de avanzar el flujo, asegurar que la tarea actual fue completada.
     */
    public boolean existeTareaPendienteByDocumentoId(Long documentoId) {
        DocumentoEntity docRef = buildDocumentoRef(documentoId);
        return flujoTrabajoTareaRepository.existsByDocumentoAndEstado(
                docRef, EstadoTareaEnum.PENDIENTE);
    }

    /**
     * Listar tareas de un paso específico por estado.
     *
     * CASO DE USO: Ver cuántas tareas de un paso están pendientes o completadas.
     */
    public List<FlujoTrabajoTareaDTO> findByPasoIdAndEstado(Long pasoId, EstadoTareaEnum estado) {
        FlujoTrabajoPasoEntity pasoRef = new FlujoTrabajoPasoEntity();
        pasoRef.setId(pasoId);
        return flujoTrabajoTareaMapper.toDTOList(
                flujoTrabajoTareaRepository.findByPasoAndEstado(pasoRef, estado));
    }

    /**
     * Contar tareas de un usuario por estado.
     *
     * CASO DE USO: Badge de tareas pendientes en el dashboard del usuario.
     */
    public long countByUsuarioCedulaAndEstado(Long cedula, EstadoTareaEnum estado) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return flujoTrabajoTareaRepository.countByAsignadoAAndEstado(usuarioRef, estado);
    }

    /**
     * Contar total de tareas.
     */
    public long count() {
        return flujoTrabajoTareaRepository.count();
    }

    // ─── Métodos auxiliares privados ─────────────────────────────────────────

    private DocumentoEntity buildDocumentoRef(Long id) {
        DocumentoEntity d = new DocumentoEntity();
        d.setId(id);
        return d;
    }

    private UsuarioEntity buildUsuarioRef(Long cedula) {
        UsuarioEntity u = new UsuarioEntity();
        u.setCedula(cedula);
        return u;
    }
}
