package com.eam.proyecto.persistenceLayer.dao;

import com.eam.proyecto.businessLayer.dto.UsuarioCreateDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioUpdateDTO;
import com.eam.proyecto.persistenceLayer.mapper.UsuarioMapper;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import com.eam.proyecto.persistenceLayer.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DAO para operaciones de persistencia de usuarios.
 *
 * DESCRIPCION:
 * - UsuarioEntity usa 'cedula' como PK natural (no autogenerado).
 * - Los usuarios pertenecen a una organización (multi-tenancy).
 * - El campo passwordHash nunca se expone en los DTOs de salida.
 * - active se gestiona mediante métodos dedicados (activar/inactivar).
 *
 * HISTORIAS CUBIERTAS:
 * - US-002 (RF02): Iniciar sesión → findByEmailAndActive
 * - US-009 (RF09): Asociar usuario a su organización → save con organizacionNit
 * - US-012 (RF12): Crear nuevo usuario → save(createDTO)
 * - US-013 (RF13): Editar datos de usuario → update(cedula, updateDTO)
 * - US-014 (RF14): Activar / inactivar cuenta → métodos activar / inactivar
 * - US-016 (RF16): Listar usuarios de la organización → findByOrganizacionNit
 */
@Repository
@RequiredArgsConstructor
public class UsuarioDAO {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    /**
     * Crear nuevo usuario dentro de una organización.
     *
     * FLUJO:
     * 1. CreateDTO → Entity (mapper establece la FK organizacion con solo el NIT)
     * 2. El service asigna active=true y encripta passwordHash antes de llamar aquí
     * 3. Guardar Entity → DTO
     *
     * US-012 / US-009
     */
    public UsuarioDTO save(UsuarioCreateDTO createDTO) {
        UsuarioEntity entity = usuarioMapper.toEntity(createDTO);
        return usuarioMapper.toDTO(usuarioRepository.save(entity));
    }

    /**
     * Buscar usuario por cédula.
     *
     * INCLUYE: Información denormalizada de la organización (organizacionNit, organizacionNombre).
     */
    public Optional<UsuarioDTO> findByCedula(Long cedula) {
        return usuarioRepository.findByCedula(cedula)
                .map(usuarioMapper::toDTO);
    }

    /**
     * Buscar todos los usuarios (todas las organizaciones).
     *
     * ADVERTENCIA: Solo para uso administrativo global. En operaciones normales
     * usar findByOrganizacionNit para respetar el aislamiento multi-tenant.
     */
    public List<UsuarioDTO> findAll() {
        return usuarioMapper.toDTOList(usuarioRepository.findAll());
    }

    /**
     * Actualizar datos de un usuario existente.
     *
     * RESTRICCIONES (aplicadas por el mapper):
     * - cédula es inmutable (PK natural).
     * - organizacion es inmutable (no se puede cambiar de tenant).
     * - passwordHash: se actualiza desde el service tras encriptar con BCrypt.
     *
     * US-013
     */
    public Optional<UsuarioDTO> update(Long cedula, UsuarioUpdateDTO updateDTO) {
        return usuarioRepository.findByCedula(cedula)
                .map(existing -> {
                    usuarioMapper.updateEntityFromDTO(updateDTO, existing);
                    return usuarioMapper.toDTO(usuarioRepository.save(existing));
                });
    }

    /**
     * Eliminar usuario por cédula.
     *
     * NOTA: En producción se prefiere inactivar antes que eliminar físicamente.
     */
    public boolean deleteByCedula(Long cedula) {
        if (usuarioRepository.existsById(cedula)) {
            usuarioRepository.deleteById(cedula);
            return true;
        }
        return false;
    }

    /**
     * Buscar usuario por email.
     *
     * CASO DE USO: Login (US-002), validaciones de unicidad.
     * SEGURIDAD: El service no debe indicar si el correo existe o no (US-002 Nota).
     */
    public Optional<UsuarioDTO> findByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(usuarioMapper::toDTO);
    }

    /**
     * Verificar si el email ya está registrado.
     *
     * CASO DE USO: Validación de unicidad al crear o editar usuario.
     */
    public boolean existsByEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    /**
     * Buscar usuario activo por email.
     *
     * CASO DE USO: Autenticación — US-002 Escenario 3 (cuenta inactiva).
     */
    public Optional<UsuarioDTO> findByEmailAndActive(String email) {
        return usuarioRepository.findByEmailAndActiveTrue(email)
                .map(usuarioMapper::toDTO);
    }

    /**
     * Listar todos los usuarios de una organización.
     *
     * US-016: Listado completo para administrador de la organización.
     */
    public List<UsuarioDTO> findByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return usuarioMapper.toDTOList(usuarioRepository.findByOrganizacion(org));
    }

    /**
     * Listar usuarios activos de una organización.
     *
     * CASO DE USO: Vista operativa — solo usuarios que pueden operar en el sistema.
     */
    public List<UsuarioDTO> findActivosByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return usuarioMapper.toDTOList(usuarioRepository.findByOrganizacionAndActiveTrue(org));
    }

    /**
     * Listar usuarios inactivos de una organización.
     *
     * CASO DE USO: Gestión de cuentas desactivadas (US-014).
     */
    public List<UsuarioDTO> findInactivosByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return usuarioMapper.toDTOList(usuarioRepository.findByOrganizacionAndActiveFalse(org));
    }

    /**
     * Buscar usuario por email dentro de una organización específica.
     *
     * CASO DE USO: Validar que el email no esté duplicado dentro del mismo tenant.
     */
    public Optional<UsuarioDTO> findByEmailAndOrganizacionNit(String email, Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return usuarioRepository.findByEmailAndOrganizacion(email, org)
                .map(usuarioMapper::toDTO);
    }

    /**
     * Buscar usuarios de una organización por nombre (contiene texto).
     *
     * CASO DE USO: Buscador de usuarios en el panel de admin (US-016).
     */
    public List<UsuarioDTO> findByOrganizacionNitAndNombreContaining(Long nit, String nombre) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return usuarioMapper.toDTOList(
                usuarioRepository.findByOrganizacionAndNombreContainingIgnoreCase(org, nombre));
    }

    /**
     * Listar usuarios de una organización ordenados por nombre.
     *
     * CASO DE USO: Vista ordenada para asignación de tareas y roles.
     */
    public List<UsuarioDTO> findByOrganizacionNitOrderByNombre(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return usuarioMapper.toDTOList(usuarioRepository.findByOrganizacionOrderByNombreAsc(org));
    }

    /**
     * Verificar si la cédula pertenece a la organización indicada.
     *
     * CASO DE USO: Validar que el usuario al que se le asigna una tarea
     * pertenece al mismo tenant (US-009 / US-010).
     */
    public boolean existsByCedulaAndOrganizacionNit(Long cedula, Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return usuarioRepository.existsByCedulaAndOrganizacion(cedula, org);
    }

    /**
     * Contar usuarios activos de una organización.
     *
     * CASO DE USO: Dashboard del administrador, límites de licencia.
     */
    public long countActivosByOrganizacionNit(Long nit) {
        OrganizacionEntity org = buildOrganizacionRef(nit);
        return usuarioRepository.countByOrganizacionAndActiveTrue(org);
    }

    /**
     * Contar total de usuarios.
     *
     * CASO DE USO: Estadísticas globales del sistema.
     */
    public long count() {
        return usuarioRepository.count();
    }

    // ─── Método auxiliar privado ──────────────────────────────────────────────

    /**
     * Construye una referencia ligera de OrganizacionEntity con solo el NIT.
     * JPA resuelve la FK sin necesidad de cargar el objeto completo desde BD.
     */
    private OrganizacionEntity buildOrganizacionRef(Long nit) {
        OrganizacionEntity org = new OrganizacionEntity();
        org.setNit(nit);
        return org;
    }
}
