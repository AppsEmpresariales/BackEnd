package com.eam.proyecto.persistenceLayer.dao;

import com.eam.proyecto.businessLayer.dto.DocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoUpdateDTO;
import com.eam.proyecto.persistenceLayer.mapper.DocumentoMapper;
import com.eam.proyecto.persistenceLayer.entity.*;
import com.eam.proyecto.persistenceLayer.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de documentos.
 *
 * DESCRIPCION:
 * - DocumentoEntity tiene 4 relaciones ManyToOne:
 *     → UsuarioEntity (creadoPor)
 *     → OrganizacionEntity (organizacion) — aislamiento multi-tenant
 *     → TipoDocumentoEntity (tipoDocumento)
 *     → EstadoDocumentoEntity (estadoDocumento) — FK real, NO enum
 * - El estado del documento se cambia mediante el flujo de trabajo (no en update).
 * - archivoRuta/archivoNombre/tamanioArchivo se gestionan desde el service de storage.
 *
 * HISTORIAS CUBIERTAS:
 * - US-017 (RF17): Crear documento con metadatos → save(createDTO)
 * - US-018 (RF18): Subir archivo adjunto → campos archivo en Entity
 * - US-019 (RF19): Editar metadatos → update(id, updateDTO)
 * - US-020 (RF20): Eliminar documento → deleteById
 * - US-021 (RF21): Consultar documentos → findByOrganizacionNit
 * - US-022 (RF22): Filtrar por tipo, fecha y estado → filtrar(...)
 * - US-023 (RF23): Descargar archivo → findConArchivo(id)
 * - US-027 (RF27): Asociar a tipo documental → campo tipoDocumentoId en createDTO
 * - US-030 (RF30): Cambiar estado en flujo → cambiarEstado(id, estadoId)
 */
@Repository
@RequiredArgsConstructor
public class DocumentoDAO {

    private final DocumentoRepository documentoRepository;
    private final DocumentoMapper documentoMapper;

    /**
     * Crear un nuevo documento con metadatos.
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper convierte todas las FKs)
     * 2. El service asigna version=1, creadoEn y estadoDocumentoId inicial
     * 3. Guardar Entity → DTO con información denormalizada
     *
     * US-017 / US-027
     */
    public DocumentoDTO save(DocumentoCreateDTO createDTO) {
        DocumentoEntity entity = documentoMapper.toEntity(createDTO);
        return documentoMapper.toDTO(documentoRepository.save(entity));
    }

    /**
     * Buscar documento por ID (sin restricción de organización).
     *
     * ADVERTENCIA: Usar findByIdAndOrganizacionNit en operaciones multi-tenant.
     */
    public Optional<DocumentoDTO> findById(Long id) {
        return documentoRepository.findById(id)
                .map(documentoMapper::toDTO);
    }

    /**
     * Buscar documento por ID restringido a una organización.
     *
     * US-010: Aislamiento lógico — un documento solo es visible
     * para usuarios de su propia organización.
     */
    public Optional<DocumentoDTO> findByIdAndOrganizacionNit(Long id, Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return documentoRepository.findByIdAndOrganizacion(id, org)
                .map(documentoMapper::toDTO);
    }

    /**
     * Actualizar metadatos de un documento existente.
     *
     * RESTRICCIONES (aplicadas por el mapper):
     * - id, creadoEn, creadoPor, organizacion son inmutables.
     * - estadoDocumento: cambia solo a través de cambiarEstado().
     * - Campos de archivo: gestionados por el service de storage.
     * - version: el service incrementa después de llamar este método.
     *
     * US-019
     */
    public Optional<DocumentoDTO> update(Long id, DocumentoUpdateDTO updateDTO) {
        return documentoRepository.findById(id)
                .map(existing -> {
                    documentoMapper.updateEntityFromDTO(updateDTO, existing);
                    return documentoMapper.toDTO(documentoRepository.save(existing));
                });
    }

    /**
     * Cambiar el estado del documento durante el flujo de trabajo.
     *
     * CASO DE USO ESPECÍFICO: Avanzar pasos del flujo de aprobación.
     * El estado solo se cambia aquí, nunca en update().
     *
     * US-030
     */
    public Optional<DocumentoDTO> cambiarEstado(Long id, Long nuevoEstadoId) {
        return documentoRepository.findById(id)
                .map(existing -> {
                    EstadoDocumentoEntity nuevoEstado = new EstadoDocumentoEntity();
                    nuevoEstado.setId(nuevoEstadoId);
                    existing.setEstadoDocumento(nuevoEstado);
                    existing.setActualizadoEn(LocalDateTime.now());
                    return documentoMapper.toDTO(documentoRepository.save(existing));
                });
    }

    /**
     * Eliminar documento por ID.
     *
     * US-020: Eliminación física. El service debe verificar permisos y
     * que no existan tareas activas antes de proceder.
     */
    public boolean deleteById(Long id) {
        if (documentoRepository.existsById(id)) {
            documentoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Listar todos los documentos de una organización.
     *
     * US-021: Consulta general del catálogo de documentos del tenant.
     */
    public List<DocumentoDTO> findByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return documentoMapper.toDTOList(
                documentoRepository.findByOrganizacionOrderByCreadoEnDesc(org));
    }

    /**
     * Filtrar documentos por tipo, estado y rango de fechas.
     *
     * PARÁMETROS OPCIONALES: Cualquier parámetro puede ser null (el query JPQL lo maneja).
     * US-022: Filtros combinados en la búsqueda del catálogo.
     */
    public List<DocumentoDTO> filtrar(Long organizacionNit,
                                      Long tipoDocumentoId,
                                      Long estadoDocumentoId,
                                      LocalDateTime desde,
                                      LocalDateTime hasta) {
        OrganizacionEntity org = buildOrganizacionRef(organizacionNit);

        TipoDocumentoEntity tipo = null;
        if (tipoDocumentoId != null) {
            tipo = new TipoDocumentoEntity();
            tipo.setId(tipoDocumentoId);
        }

        EstadoDocumentoEntity estado = null;
        if (estadoDocumentoId != null) {
            estado = new EstadoDocumentoEntity();
            estado.setId(estadoDocumentoId);
        }

        return documentoMapper.toDTOList(
                documentoRepository.filtrar(org, tipo, estado, desde, hasta));
    }

    /**
     * Buscar documentos por tipo documental dentro de una organización.
     *
     * US-022 / US-027: Filtro por tipo en el catálogo.
     */
    public List<DocumentoDTO> findByOrganizacionNitAndTipoDocumentoId(Long nit, Long tipoDocumentoId) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        TipoDocumentoEntity tipo = new TipoDocumentoEntity();
        tipo.setId(tipoDocumentoId);
        return documentoMapper.toDTOList(
                documentoRepository.findByOrganizacionAndTipoDocumento(org, tipo));
    }

    /**
     * Buscar documentos por estado dentro de una organización.
     *
     * US-022 / US-030: Filtro por estado en el catálogo.
     */
    public List<DocumentoDTO> findByOrganizacionNitAndEstadoId(Long nit, Long estadoId) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        EstadoDocumentoEntity estado = new EstadoDocumentoEntity();
        estado.setId(estadoId);
        return documentoMapper.toDTOList(
                documentoRepository.findByOrganizacionAndEstadoDocumento(org, estado));
    }

    /**
     * Buscar documentos por rango de fechas dentro de una organización.
     *
     * US-022: Filtro por fecha de creación.
     */
    public List<DocumentoDTO> findByOrganizacionNitAndFechas(Long nit,
                                                               LocalDateTime desde,
                                                               LocalDateTime hasta) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return documentoMapper.toDTOList(
                documentoRepository.findByOrganizacionAndCreadoEnBetween(org, desde, hasta));
    }

    /**
     * Buscar documentos por título (contiene texto).
     *
     * US-022: Búsqueda por texto libre en el catálogo.
     */
    public List<DocumentoDTO> findByOrganizacionNitAndTituloContaining(Long nit, String titulo) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return documentoMapper.toDTOList(
                documentoRepository.findByOrganizacionAndTituloContainingIgnoreCase(org, titulo));
    }

    /**
     * Buscar documentos creados por un usuario específico.
     *
     * CASO DE USO: Vista "mis documentos" del usuario.
     */
    public List<DocumentoDTO> findByOrganizacionNitAndCreadoPorCedula(Long nit, Long cedula) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setCedula(cedula);
        return documentoMapper.toDTOList(
                documentoRepository.findByOrganizacionAndCreadoPor(org, usuario));
    }

    /**
     * Buscar documento que tenga archivo adjunto disponible.
     *
     * US-023: Verificar que existe archivo antes de generar URL de descarga.
     */
    public Optional<DocumentoDTO> findConArchivo(Long id) {
        return documentoRepository.findByIdAndArchivoRutaIsNotNull(id)
                .map(documentoMapper::toDTO);
    }

    /**
     * Obtener los 10 documentos más recientes de la organización.
     *
     * CASO DE USO: Widget "últimos documentos" en el dashboard.
     */
    public List<DocumentoDTO> findTop10ByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return documentoMapper.toDTOList(
                documentoRepository.findTop10ByOrganizacionOrderByCreadoEnDesc(org));
    }

    /**
     * Contar documentos por estado dentro de una organización.
     *
     * CASO DE USO: Dashboard — cuántos documentos están en revisión, aprobados, etc.
     */
    public long countByOrganizacionNitAndEstadoId(Long nit, Long estadoId) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        EstadoDocumentoEntity estado = new EstadoDocumentoEntity();
        estado.setId(estadoId);
        return documentoRepository.countByOrganizacionAndEstadoDocumento(org, estado);
    }

    /**
     * Contar total de documentos.
     *
     * CASO DE USO: Estadísticas globales del sistema.
     */
    public long count() {
        return documentoRepository.count();
    }

    // ─── Método auxiliar privado ──────────────────────────────────────────────

    private OrganizacionEntity buildOrganizacionRef(Long nit) {
        OrganizacionEntity org = new OrganizacionEntity();
        org.setNit(nit);
        return org;
    }
}
