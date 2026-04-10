// AuditRegistroServiceImpl.java
package com.eam.proyecto.businessLayer.service.impl;

import com.eam.proyecto.businessLayer.dto.AuditRegistroCreateDTO;
import com.eam.proyecto.businessLayer.dto.AuditRegistroDTO;
import com.eam.proyecto.businessLayer.service.AuditRegistroService;
import com.eam.proyecto.businessLayer.service.DocumentoService;
import com.eam.proyecto.businessLayer.service.UsuarioService;
import com.eam.proyecto.persistenceLayer.dao.AuditRegistroDAO;
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
public class AuditRegistroServiceImpl implements AuditRegistroService {

    private final AuditRegistroDAO auditRegistroDAO;
    private final DocumentoService documentoService;
    private final UsuarioService usuarioService;

    /**
     * REGISTRAR ACCIÓN — Append-only, nunca se edita ni elimina — RF33 / RF35.
     *
     * FLUJO:
     * 1. Validar datos obligatorios
     * 2. Verificar que el documento y el usuario existen
     * 3. Asignar creadoEn=LocalDateTime.now() (el DAO no lo hace)
     * 4. Persistir
     *
     * IMPORTANTE: Este método es llamado internamente por otros services
     * después de cada acción significativa sobre un documento (creación,
     * cambio de estado, actualización de metadatos, etc.).
     */
    @Override
    public AuditRegistroDTO registrarAccion(AuditRegistroCreateDTO createDTO) {
        log.info("Registrando acción '{}' sobre documento ID {} por usuario cédula {}",
                createDTO.getAccion(), createDTO.getDocumentoId(), createDTO.getUsuarioCedula());

        validateAuditRegistroData(createDTO);

        documentoService.getDocumentoById(createDTO.getDocumentoId());
        usuarioService.getUsuarioByCedula(createDTO.getUsuarioCedula());

        // El service asigna la fecha de registro — RF35
        createDTO.setCreadoEn(LocalDateTime.now());

        AuditRegistroDTO result = auditRegistroDAO.save(createDTO);

        log.debug("Acción registrada exitosamente con ID: {}", result.getId());
        return result;
    }

    /**
     * READ — Buscar registro de auditoría por ID.
     */
    @Override
    @Transactional(readOnly = true)
    public AuditRegistroDTO getRegistroById(Long id) {
        log.debug("Buscando registro de auditoría por ID: {}", id);

        return auditRegistroDAO.findById(id)
                .orElseThrow(() -> {
                    log.warn("Registro de auditoría no encontrado con ID: {}", id);
                    return new RuntimeException("Registro de auditoría no encontrado con ID: " + id);
                });
    }

    /**
     * HISTORIAL — Obtener la línea de tiempo completa de un documento — RF34.
     *
     * RETORNA: Registros en orden cronológico ascendente (del más antiguo al más reciente).
     * Muestra el ciclo de vida completo del documento desde su creación.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuditRegistroDTO> getHistorialByDocumento(Long documentoId) {
        log.debug("Obteniendo historial del documento ID: {}", documentoId);

        documentoService.getDocumentoById(documentoId);

        List<AuditRegistroDTO> historial = auditRegistroDAO.findByDocumentoIdAsc(documentoId);

        log.debug("Historial del documento ID {}: {} registros encontrados", documentoId, historial.size());
        return historial;
    }

    /**
     * TRAZABILIDAD POR USUARIO — Listar todas las acciones de un usuario — RF36.
     *
     * CASO DE USO: Panel de auditoría global. Ver qué hizo un usuario en el sistema.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuditRegistroDTO> getTrazabilidadByUsuario(Long cedula) {
        log.debug("Obteniendo trazabilidad del usuario cédula: {}", cedula);

        usuarioService.getUsuarioByCedula(cedula);

        return auditRegistroDAO.findByUsuarioCedula(cedula);
    }

    /**
     * TRAZABILIDAD COMPLETA — Panel global con filtros opcionales — RF36.
     *
     * PARÁMETROS OPCIONALES: Cualquier parámetro puede ser null.
     * - accion: filtrar por tipo de acción (ej: "CREACION", "CAMBIO_ESTADO")
     * - desde / hasta: filtrar por rango de fechas
     *
     * CASO DE USO: Panel de auditoría del administrador del sistema.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuditRegistroDTO> getTrazabilidadCompleta(String accion, LocalDateTime desde, LocalDateTime hasta) {
        log.debug("Consultando trazabilidad completa. Acción: {}, Desde: {}, Hasta: {}", accion, desde, hasta);

        // Validar rango de fechas si ambas se proporcionan
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }

        return auditRegistroDAO.trazabilidadCompleta(accion, desde, hasta);
    }

    // ─── Validaciones privadas ────────────────────────────────────────────────

    private void validateAuditRegistroData(AuditRegistroCreateDTO dto) {
        if (dto.getDocumentoId() == null) {
            throw new IllegalArgumentException("El ID del documento es obligatorio para el registro de auditoría");
        }
        if (dto.getUsuarioCedula() == null) {
            throw new IllegalArgumentException("La cédula del usuario es obligatoria para el registro de auditoría");
        }
        if (dto.getAccion() == null || dto.getAccion().trim().isEmpty()) {
            throw new IllegalArgumentException("La acción debe ser descrita en el registro de auditoría");
        }
    }
}