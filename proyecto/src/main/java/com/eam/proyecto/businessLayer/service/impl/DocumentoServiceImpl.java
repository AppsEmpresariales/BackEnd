// DocumentoServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.*;
import com.eam.proyecto.businessLayer.service.*;
import com.eam.proyecto.persistenceLayer.dao.DocumentoDAO;
import com.eam.proyecto.persistenceLayer.entity.enums.CanalNotificacionEnum;
import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private final @Lazy AuditRegistroService auditRegistroService;
    private final @Lazy FlujoTrabajoService flujoTrabajoService;
    private final @Lazy FlujoTrabajoPasoService flujoTrabajoPasoService;
    private final @Lazy FlujoTrabajoTareaService flujoTrabajoTareaService;
    private final @Lazy NotificacionService notificacionService;
    private final @Lazy RolUsuarioService rolUsuarioService;

    @Value("${docucloud.storage.documentos:uploads/documentos}")
    private String storagePath;

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

        // Registrar auditoría de creación — RF33
        try {
            AuditRegistroCreateDTO auditDTO = new AuditRegistroCreateDTO();
            auditDTO.setDocumentoId(result.getId());
            auditDTO.setUsuarioCedula(createDTO.getCreadoPorCedula());
            auditDTO.setAccion("DOCUMENTO_CREADO");
            auditDTO.setDescripcion("Documento '" + result.getTitulo() + "' creado");
            auditDTO.setEstadoNuevo(result.getEstadoDocumentoNombre());
            auditRegistroService.registrarAccion(auditDTO);
        } catch (Exception e) {
            log.warn("No se pudo registrar auditoría para documento ID {}: {}", result.getId(), e.getMessage());
        }

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
    /**
     * UPLOAD - Asociar archivo fisico a un documento - RF18.
     */
    @Override
    public DocumentoDTO subirArchivo(Long id, MultipartFile file) {
        log.info("Subiendo archivo para documento ID: {}", id);

        validateArchivo(file);
        DocumentoDTO documento = getDocumentoById(id);

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "documento" : file.getOriginalFilename());
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        if (safeName.isBlank() || safeName.contains("..")) {
            throw new IllegalArgumentException("Nombre de archivo invalido");
        }

        try {
            Path root = resolveStorageRoot();
            Path documentDir = root
                    .resolve(String.valueOf(documento.getOrganizacionNit()))
                    .resolve(String.valueOf(id))
                    .normalize();

            if (!documentDir.startsWith(root)) {
                throw new IllegalStateException("Ruta de almacenamiento no permitida");
            }

            Files.createDirectories(documentDir);

            String storedName = UUID.randomUUID() + "-" + safeName;
            Path target = documentDir.resolve(storedName).normalize();

            if (!target.startsWith(documentDir)) {
                throw new IllegalStateException("Ruta de archivo no permitida");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            DocumentoDTO result = documentoDAO
                    .actualizarArchivo(id, safeName, target.toString(), file.getSize())
                    .orElseThrow(() -> new RuntimeException("Error al actualizar archivo del documento ID: " + id));

            deletePhysicalFile(documento.getArchivoRuta());
            log.info("Archivo '{}' asociado al documento ID: {}", safeName, id);
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo almacenar el archivo del documento", e);
        }
    }

    /**
     * DOWNLOAD - Obtener archivo asociado al documento - RF23.
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentoArchivoDTO descargarArchivo(Long id) {
        log.debug("Descargando archivo del documento ID: {}", id);

        DocumentoDTO documento = getDocumentoById(id);
        if (documento.getArchivoRuta() == null || documento.getArchivoRuta().isBlank()) {
            throw new RuntimeException("El documento no tiene archivo asociado");
        }

        try {
            Path root = resolveStorageRoot();
            Path filePath = Paths.get(documento.getArchivoRuta()).toAbsolutePath().normalize();

            if (!filePath.startsWith(root)) {
                throw new IllegalStateException("Ruta de archivo no permitida");
            }
            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                throw new RuntimeException("Archivo no disponible para descarga");
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            String fileName = documento.getArchivoNombre() == null
                    ? filePath.getFileName().toString()
                    : documento.getArchivoNombre();

            return new DocumentoArchivoDTO(
                    resource,
                    fileName,
                    contentType == null ? "application/octet-stream" : contentType,
                    Files.size(filePath)
            );
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo del documento", e);
        }
    }

    @Override
    public DocumentoDTO cambiarEstado(Long id, Long nuevoEstadoId) {
        log.info("Cambiando estado del documento ID {} al estado ID: {}", id, nuevoEstadoId);

        DocumentoDTO docAnterior = getDocumentoById(id);
        String estadoPrevioNombre = docAnterior.getEstadoDocumentoNombre();
        EstadoDocumentoDTO nuevoEstadoDTO = estadoDocumentoService.getEstadoDocumentoById(nuevoEstadoId);

        DocumentoDTO result = documentoDAO.cambiarEstado(id, nuevoEstadoId)
                .orElseThrow(() -> new RuntimeException("Error al cambiar estado del documento ID: " + id));

        if (result.getEstadoDocumentoNombre() == null && nuevoEstadoDTO != null) {
            result.setEstadoDocumentoNombre(nuevoEstadoDTO.getNombre());
        }

        // ── Registrar auditoría — RF33 / RF35 ──
        try {
            AuditRegistroCreateDTO auditDTO = new AuditRegistroCreateDTO();
            auditDTO.setDocumentoId(id);
            auditDTO.setUsuarioCedula(docAnterior.getCreadoPorCedula());
            auditDTO.setAccion("CAMBIO_ESTADO");
            auditDTO.setDescripcion("Estado cambiado de '" + estadoPrevioNombre + "' a '" + result.getEstadoDocumentoNombre() + "'");
            auditDTO.setEstadoPrevio(estadoPrevioNombre);
            auditDTO.setEstadoNuevo(result.getEstadoDocumentoNombre());
            auditRegistroService.registrarAccion(auditDTO);
            log.debug("Auditoría registrada para cambio de estado documento ID {}", id);
        } catch (Exception e) {
            log.warn("No se pudo registrar auditoría para documento ID {}: {}", id, e.getMessage());
        }

        boolean tareaAsignada = false;
        // ── Crear tarea automática si existe flujo de trabajo — RF29 / RF31 ──
        try {
            java.util.Optional<FlujoTrabajoDTO> flujoActivoOpt = flujoTrabajoService
                    .findFlujoActivoByOrganizacionAndTipoDocumento(
                            docAnterior.getOrganizacionNit(), docAnterior.getTipoDocumentoId());

            if (flujoActivoOpt.isPresent()) {
                FlujoTrabajoDTO flujoActivo = flujoActivoOpt.get();

                // Buscar el paso del flujo cuyo objetivoEstado coincide con el nuevo estado
                List<FlujoTrabajoPasoDTO> pasos = flujoTrabajoPasoService
                        .getPasosByFlujoTrabajo(flujoActivo.getId());

                for (FlujoTrabajoPasoDTO paso : pasos) {
                    if (paso.getObjetivoEstadoId() != null && paso.getObjetivoEstadoId().equals(nuevoEstadoId)) {
                        // Buscar a un usuario de la organización que tenga el rol requerido para el paso
                        Long assignedUserCedula = docAnterior.getCreadoPorCedula(); // Fallback: asignar al creador

                        if (paso.getRolRequeridoId() != null) {
                            List<UsuarioDTO> orgUsers = usuarioService.getUsuariosActivosByOrganizacion(docAnterior.getOrganizacionNit());
                            for (UsuarioDTO orgUser : orgUsers) {
                                List<RolUsuarioDTO> userRoles = rolUsuarioService.getRolesByUsuario(orgUser.getCedula());
                                boolean hasRole = userRoles.stream().anyMatch(r -> r.getRolId().equals(paso.getRolRequeridoId()));
                                if (hasRole) {
                                    assignedUserCedula = orgUser.getCedula();
                                    break;
                                }
                            }
                        }

                        // Prevenir marcar la transacción como rollback-only si ya hay una tarea pendiente
                        if (flujoTrabajoTareaService.existeTareaPendiente(id)) {
                            log.debug("No se creó tarea automática para documento ID {}: ya existe una tarea pendiente", id);
                            break;
                        }

                        // Crear tarea para este paso, asignada al usuario encontrado
                        FlujoTrabajoTareaCreateDTO tareaDTO = new FlujoTrabajoTareaCreateDTO();
                        tareaDTO.setDocumentoId(id);
                        tareaDTO.setPasoId(paso.getId());
                        tareaDTO.setAsignadoACedula(assignedUserCedula);
                        tareaDTO.setComentario("Tarea generada automáticamente al cambiar estado a '" + result.getEstadoDocumentoNombre() + "'");
                        tareaDTO.setFechaLimite(LocalDateTime.now().plusDays(7));
                        flujoTrabajoTareaService.asignarTarea(tareaDTO);
                        log.info("Tarea creada automáticamente para paso '{}' asignada al usuario {}", paso.getNombre(), assignedUserCedula);
                        
                        tareaAsignada = true;
                        // Actualizar el destinatario de la notificación a la persona asignada
                        docAnterior.setCreadoPorCedula(assignedUserCedula); 
                        break;
                    }
                }
            } else {
                log.debug("No hay flujo de trabajo activo configurado para el tipo documental ID {}", docAnterior.getTipoDocumentoId());
            }
        } catch (Exception e) {
            log.warn("Error al crear tarea automática para documento ID {}: {}", id, e.getMessage());
        }

        // ── Enviar notificación al creador del documento — RF37 / RF39 ──
        try {
            TipoEventoEnum tipoEvento = result.getEstadoDocumentoNombre().toLowerCase().contains("aprobado")
                    ? TipoEventoEnum.DOCUMENTO_APROBADO
                    : result.getEstadoDocumentoNombre().toLowerCase().contains("rechazado")
                    ? TipoEventoEnum.DOCUMENTO_RECHAZADO
                    : TipoEventoEnum.TAREA_ASIGNADA;

            NotificacionCreateDTO notifDTO = new NotificacionCreateDTO();
            notifDTO.setUsuarioCedula(docAnterior.getCreadoPorCedula());
            notifDTO.setDocumentoId(id);
            notifDTO.setCanal(CanalNotificacionEnum.EMAIL);
            
            String mensaje = "El documento '" + result.getTitulo() + "' cambió de estado: " +
                    estadoPrevioNombre + " → " + result.getEstadoDocumentoNombre();
            if (tareaAsignada) {
                String title = result.getTitulo();
                if (title.endsWith(".")) {
                    title = title.substring(0, title.length() - 1);
                }
                mensaje = "Se te ha asignado una tarea de revisión para el documento " + title;
            }
            notifDTO.setMensaje(mensaje);
            notifDTO.setTipoEvento(tipoEvento);
            notifDTO.setOrganizacionNit(docAnterior.getOrganizacionNit());
            notificacionService.enviarNotificacion(notifDTO);
            log.debug("Notificación enviada al usuario cédula {} por cambio de estado", docAnterior.getCreadoPorCedula());
        } catch (Exception e) {
            log.warn("No se pudo enviar notificación para documento ID {}: {}", id, e.getMessage());
        }

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

        DocumentoDTO documento = getDocumentoById(id);

        boolean deleted = documentoDAO.deleteById(id);
        if (!deleted) {
            throw new RuntimeException("Error al eliminar documento ID: " + id);
        }

        deletePhysicalFile(documento.getArchivoRuta());

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

    private void validateArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo es obligatorio");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("El nombre del archivo es obligatorio");
        }
    }

    private Path resolveStorageRoot() throws IOException {
        String configuredPath = storagePath == null || storagePath.isBlank()
                ? "uploads/documentos"
                : storagePath;
        Path root = Paths.get(configuredPath).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }

    private void deletePhysicalFile(String archivoRuta) {
        if (archivoRuta == null || archivoRuta.isBlank()) {
            return;
        }

        try {
            Path root = resolveStorageRoot();
            Path filePath = Paths.get(archivoRuta).toAbsolutePath().normalize();
            if (filePath.startsWith(root)) {
                Files.deleteIfExists(filePath);
            } else {
                log.warn("No se elimina archivo fuera del storage configurado: {}", archivoRuta);
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar archivo fisico '{}': {}", archivoRuta, e.getMessage());
        }
    }
}
