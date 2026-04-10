package com.eam.proyecto.persistenceLayer.dao;

import com.eam.proyecto.businessLayer.dto.AuditRegistroCreateDTO;
import com.eam.proyecto.businessLayer.dto.AuditRegistroDTO;
import com.eam.proyecto.persistenceLayer.mapper.AuditRegistroMapper;
import com.eam.proyecto.persistenceLayer.entity.AuditRegistroEntity;
import com.eam.proyecto.persistenceLayer.entity.DocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import com.eam.proyecto.persistenceLayer.repository.AuditRegistroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de registros de auditoría.
 *
 * DESCRIPCION:
 * - AuditRegistroEntity es INMUTABLE: solo se crea, nunca se edita ni elimina.
 * - Append-only: cada acción sobre un documento genera un nuevo registro.
 * - Relaciona DocumentoEntity y UsuarioEntity con los campos de la acción.
 * - creadoEn: el service lo asigna con LocalDateTime.now() antes de persistir.
 *
 * HISTORIAS CUBIERTAS:
 * - US-033 (RF33): Registrar historial de acciones → save(createDTO)
 * - US-034 (RF34): Mostrar historial de cambios → findByDocumentoId
 * - US-035 (RF35): Registrar usuario, fecha y acción → campos usuario + accion + creadoEn
 * - US-036 (RF36): Consultar trazabilidad completa → trazabilidadCompleta(...)
 */
@Repository
@RequiredArgsConstructor
public class AuditRegistroDAO {

    private final AuditRegistroRepository auditRegistroRepository;
    private final AuditRegistroMapper auditRegistroMapper;

    /**
     * Registrar una nueva acción de auditoría (append-only).
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper ignora id y creadoEn)
     * 2. El service asigna creadoEn=LocalDateTime.now() antes de llamar aquí
     * 3. Guardar Entity → DTO
     *
     * US-033 / US-035
     */
    public AuditRegistroDTO save(AuditRegistroCreateDTO createDTO) {
        AuditRegistroEntity entity = auditRegistroMapper.toEntity(createDTO);
        return auditRegistroMapper.toDTO(auditRegistroRepository.save(entity));
    }

    /**
     * Buscar registro de auditoría por ID.
     *
     * CASO DE USO: Obtener el detalle de un evento específico.
     */
    public Optional<AuditRegistroDTO> findById(Long id) {
        return auditRegistroRepository.findById(id)
                .map(auditRegistroMapper::toDTO);
    }

    /**
     * Obtener el historial completo de un documento (orden cronológico ascendente).
     *
     * US-034: Mostrar la línea de tiempo de cambios de un documento,
     * desde su creación hasta el estado actual.
     */
    public List<AuditRegistroDTO> findByDocumentoIdAsc(Long documentoId) {
        DocumentoEntity docRef = buildDocumentoRef(documentoId);
        return auditRegistroMapper.toDTOList(
                auditRegistroRepository.findByDocumentoOrderByCreadoEnAsc(docRef));
    }

    /**
     * Obtener el historial de un documento (orden cronológico descendente).
     *
     * CASO DE USO: "Últimas acciones" — el evento más reciente aparece primero.
     * US-034
     */
    public List<AuditRegistroDTO> findByDocumentoIdDesc(Long documentoId) {
        DocumentoEntity docRef = buildDocumentoRef(documentoId);
        return auditRegistroMapper.toDTOList(
                auditRegistroRepository.findByDocumentoOrderByCreadoEnDesc(docRef));
    }

    /**
     * Obtener el último evento registrado de un documento.
     *
     * CASO DE USO: Mostrar la acción más reciente en la vista del documento.
     * US-035
     */
    public Optional<AuditRegistroDTO> findUltimoEventoByDocumentoId(Long documentoId) {
        DocumentoEntity docRef = buildDocumentoRef(documentoId);
        AuditRegistroEntity ultimo =
                auditRegistroRepository.findFirstByDocumentoOrderByCreadoEnDesc(docRef);
        return Optional.ofNullable(ultimo).map(auditRegistroMapper::toDTO);
    }

    /**
     * Listar todos los registros de un usuario.
     *
     * US-036: Trazabilidad de las acciones realizadas por un usuario específico.
     */
    public List<AuditRegistroDTO> findByUsuarioCedula(Long cedula) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return auditRegistroMapper.toDTOList(
                auditRegistroRepository.findByUsuario(usuarioRef));
    }

    /**
     * Listar registros de un documento filtrados por tipo de acción.
     *
     * CASO DE USO: Ver solo los cambios de estado, o solo las subidas de archivo.
     * US-034
     */
    public List<AuditRegistroDTO> findByDocumentoIdAndAccion(Long documentoId, String accion) {
        DocumentoEntity docRef = buildDocumentoRef(documentoId);
        return auditRegistroMapper.toDTOList(
                auditRegistroRepository.findByDocumentoAndAccion(docRef, accion));
    }

    /**
     * Consultar trazabilidad completa del sistema con filtros opcionales.
     *
     * PARÁMETROS OPCIONALES: Cualquier parámetro puede ser null (el query JPQL lo maneja).
     * US-036: Panel de auditoría global para el administrador.
     */
    public List<AuditRegistroDTO> trazabilidadCompleta(String accion,
                                                         LocalDateTime desde,
                                                         LocalDateTime hasta) {
        return auditRegistroMapper.toDTOList(
                auditRegistroRepository.trazabilidadCompleta(accion, desde, hasta));
    }

    /**
     * Listar registros en un rango de fechas.
     *
     * US-036: Filtrado por período en el panel de trazabilidad.
     */
    public List<AuditRegistroDTO> findByRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        return auditRegistroMapper.toDTOList(
                auditRegistroRepository.findByCreadoEnBetweenOrderByCreadoEnDesc(desde, hasta));
    }

    /**
     * Listar registros de un usuario en un rango de fechas.
     *
     * US-036: Trazabilidad por usuario y período.
     */
    public List<AuditRegistroDTO> findByUsuarioCedulaAndRangoFechas(Long cedula,
                                                                      LocalDateTime desde,
                                                                      LocalDateTime hasta) {
        UsuarioEntity usuarioRef = buildUsuarioRef(cedula);
        return auditRegistroMapper.toDTOList(
                auditRegistroRepository.findByUsuarioAndCreadoEnBetweenOrderByCreadoEnDesc(
                        usuarioRef, desde, hasta));
    }

    /**
     * Contar total de registros de auditoría.
     *
     * CASO DE USO: Estadísticas del sistema, verificación de integridad del log.
     */
    public long count() {
        return auditRegistroRepository.count();
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
