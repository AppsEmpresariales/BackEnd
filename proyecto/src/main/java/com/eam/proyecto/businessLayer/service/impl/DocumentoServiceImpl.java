// DocumentoServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.DocumentoCreateDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoDTO;
import com.eam.proyecto.businessLayer.dto.DocumentoUpdateDTO;
import com.eam.proyecto.businessLayer.service.*;
import com.eam.proyecto.persistenceLayer.dao.DocumentoDAO;
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
public class DocumentoServiceImpl implements DocumentoService {

    private final DocumentoDAO documentoDAO;
    private final OrganizacionService organizacionService;
    private final UsuarioService usuarioService;
    private final TipoDocumentoService tipoDocumentoService;
    private final EstadoDocumentoService estadoDocumentoService;

    /**
     * CREATE — Crear documento con metadatos — RF17 / RF27.
     *
     * FLUJO:
     * 1. Validar datos obligatorios
     * 2. Verificar que la organización existe y está activa
     * 3. Verificar que el creador pertenece a la organización — RF10
     * 4. Verificar que el tipo documental existe en el tenant — RF27
     * 5. Asignar estado inicial de la organización si no se especifica — RF31
     * 6. Inicializar version=1 y timestamps
     * 7. Persistir
     */
    @Override
    public DocumentoDTO createDocumento(DocumentoCreateDTO createDTO) {
        log.info("Creando documento '{}' en organización NIT: {}", createDTO.getTitulo(), createDTO.getOrganizacionNit());

        validateDocumentoData(createDTO);

        organizacionService.getOrganizacionActivaByNit(createDTO.getOrganizacionNit());
        usuarioService.getUsuarioByCedula(createDTO.getCreadoPorCedula());
        tipoDocumentoService.getTipoDocumentoByIdAndOrganizacion(createDTO.getTipoDocumentoId(), createDTO.getOrganizacionNit());

        // Si no se especifica estado, asignar el estado inicial de la organización — RF17 / RF31
        if (createDTO.getEstadoDocumentoId() == null) {
            Long estadoInicialId = estadoDocumentoService
                    .getEstadoInicialByOrganizacion(createDTO.getOrganizacionNit())
                    .getId();
            createDTO.setEstadoDocumentoId(estadoInicialId);
            log.debug("Estado inicial asignado automáticamente: {}", estadoInicialId);
        }

        // El service inicializa versión y timestamps
        createDTO.setVersion(1);
        createDTO.setCreadoEn(LocalDateTime.now());
        createDTO.setActualizadoEn(LocalDateTime.now());

        DocumentoDTO result = documentoDAO.save(createDTO);

        log.info("Documento creado exitosamente con ID: {}", result.getId());
        return result;
    }

    /**
     * READ — Buscar documento por ID (sin restricción de tenant).
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentoDTO getDocumentoById(Long id) {
        log.debug("Buscando documento por ID: {}", id);

        return documentoDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("Documento no encontrado con ID: {}", id);
                    return new RuntimeException("Documento no encontrado con ID: " + id);
                });
    }

    /**
     * READ — Buscar documento por ID restringido al tenant — RF10.
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentoDTO getDocumentoByIdAndOrganizacion(Long id, Long organizacionNit) {
        log.debug("Buscando documento ID {} en organización NIT: {}", id, organizacionNit);

        return documentoDAO.findByIdAndOrganizacionNit(id, organizacionNit)
                .orElseThrow(() -> {
                    log.warn("Documento ID {} no encontrado en organización NIT: {}", id, organizacionNit);
                    return new RuntimeException("Documento no encontrado en esta organización");
                });
    }

    /**
     * READ ALL — Listar documentos de la organización — RF21.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentoDTO> getDocumentosByOrganizacion(Long organizacionNit) {
        log.debug("Obteniendo documentos de organización NIT: {}", organizacionNit);
        organizacionService.getOrganizacionByNit(organizacionNit);
        return documentoDAO.findByOrganizacionNit(organizacionNit);
    }

    /**
     * UPDATE — Editar metadatos del documento — RF19.
     *
     * RESTRICCIONES:
     * - id, creadoEn, creadoPor, organizacion son inmutables.
     * - El estado solo cambia a través de cambiarEstado() — RF30.
     * - La versión se incrementa automáticamente.
     */
    @Override
    public DocumentoDTO updateDocumento(Long id, DocumentoUpdateDTO updateDTO) {
        log.info("Actualizando metadatos del documento ID: {}", id);

        getDocumentoById(id);
        validateDocumentoUpdateData(updateDTO);

        DocumentoDTO result = documentoDAO.update(id, updateDTO)
                .orElseThrow(() -> new RuntimeException("Error al actualizar documento ID: " + id));

        log.info("Documento actualizado exitosamente ID: {}", id);
        return result;
    }

    /**
     * CAMBIAR ESTADO — Avanzar el documento en el flujo de aprobación — RF30.
     *
     * FLUJO:
     * 1. Verificar que el documento existe
     * 2. Verificar que el nuevo estado existe
     * 3. Cambiar el estado mediante el DAO dedicado
     *
     * NOTA: La validación de secuencia del flujo (RF31) se realiza
     * en el FlujoTrabajoTareaService al completar una tarea.
     */
    @Override
    public DocumentoDTO cambiarEstado(Long id, Long nuevoEstadoId) {
        log.info("Cambiando estado del documento ID {} al estado ID: {}", id, nuevoEstadoId);

        getDocumentoById(id);
        estadoDocumentoService.getEstadoDocumentoById(nuevoEstadoId);

        DocumentoDTO result = documentoDAO.cambiarEstado(id, nuevoEstadoId)
                .orElseThrow(() -> new RuntimeException("Error al cambiar estado del documento ID: " + id));

        log.info("Estado del documento ID {} cambiado exitosamente", id);
        return result;
    }

    /**
     * DELETE — Eliminar documento — RF20.
     *
     * RESTRICCIÓN: Verificar que no existan tareas activas asociadas.
     */
    @Override
    public void deleteDocumento(Long id) {
        log.info("Eliminando documento ID: {}", id);

        getDocumentoById(id);

        boolean deleted = documentoDAO.deleteById(id);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar documento ID: " + id);
        }

        log.info("Documento eliminado exitosamente ID: {}", id);
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validateDocumentoData(DocumentoCreateDTO dto) {
        if (dto.getTitulo() == null || dto.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("El título del documento es obligatorio");
        }
        if (dto.getTitulo().length() > 300) {
            throw new IllegalArgumentException("El título no puede exceder 300 caracteres");
        }
        if (dto.getCreadoPorCedula() == null) {
            throw new IllegalArgumentException("La cédula del creador es obligatoria");
        }
        if (dto.getOrganizacionNit() == null) {
            throw new IllegalArgumentException("El NIT de la organización es obligatorio");
        }
        if (dto.getTipoDocumentoId() == null) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio");
        }
    }

    private void validateDocumentoUpdateData(DocumentoUpdateDTO dto) {
        if (dto.getTitulo() != null) {
            if (dto.getTitulo().trim().isEmpty()) {
                throw new IllegalArgumentException("El título no puede estar vacío");
            }
            if (dto.getTitulo().length() > 300) {
                throw new IllegalArgumentException("El título no puede exceder 300 caracteres");
            }
        }
    }
}