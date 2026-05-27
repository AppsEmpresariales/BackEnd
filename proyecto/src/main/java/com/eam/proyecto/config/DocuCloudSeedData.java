package com.eam.proyecto.config;

import com.eam.proyecto.persistenceLayer.entity.EstadoDocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.RolEntity;
import com.eam.proyecto.persistenceLayer.entity.RolUsuarioEntity;
import com.eam.proyecto.persistenceLayer.entity.TipoDocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoEntity;
import com.eam.proyecto.persistenceLayer.entity.FlujoTrabajoPasoEntity;
import com.eam.proyecto.persistenceLayer.repository.EstadoDocumentoRepository;
import com.eam.proyecto.persistenceLayer.repository.OrganizacionRepository;
import com.eam.proyecto.persistenceLayer.repository.RolRepository;
import com.eam.proyecto.persistenceLayer.repository.RolUsuarioRepository;
import com.eam.proyecto.persistenceLayer.repository.TipoDocumentoRepository;
import com.eam.proyecto.persistenceLayer.repository.UsuarioRepository;
import com.eam.proyecto.persistenceLayer.repository.FlujoTrabajoRepository;
import com.eam.proyecto.persistenceLayer.repository.FlujoTrabajoPasoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DocuCloudSeedData implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final RolUsuarioRepository rolUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacionRepository organizacionRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final EstadoDocumentoRepository estadoDocumentoRepository;
    private final FlujoTrabajoRepository flujoTrabajoRepository;
    private final FlujoTrabajoPasoRepository flujoTrabajoPasoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${docucloud.seed.organizacion-nit:900123456}")
    private Long organizacionNit;

    @Value("${docucloud.seed.admin-cedula:999999999}")
    private Long adminCedula;

    @Value("${docucloud.seed.admin-email:admin@docucloud.local}")
    private String adminEmail;

    @Value("${docucloud.seed.admin-password:Admin12345}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        RolEntity adminRole = ensureRole("ADMIN", "Administrador de organizacion");
        RolEntity userRole = ensureRole("USER", "Usuario Estandar");
        ensureRole("SISTEMA", "Sistema");
        RolEntity editorRole = ensureRole("EDITOR", "Editor de documentos");
        RolEntity revisorRole = ensureRole("REVISOR", "Revisor de documentos");
        RolEntity aprobadorRole = ensureRole("APROBADOR", "Aprobador de documentos");

        OrganizacionEntity organizacion = ensureOrganizacion();
        UsuarioEntity admin = ensureAdminUser(organizacion);
        ensureUserRole(admin, adminRole);

        // Seed users for testing
        UsuarioEntity carlos = ensureSeedUser(organizacion, 1001001001L, "Carlos Mendoza", "carlos.mendoza@docucloud.local");
        ensureUserRole(carlos, revisorRole);

        UsuarioEntity laura = ensureSeedUser(organizacion, 1002002002L, "Laura Gutierrez", "laura.gutierrez@docucloud.local");
        ensureUserRole(laura, aprobadorRole);

        UsuarioEntity andres = ensureSeedUser(organizacion, 1003003003L, "Andres Ramirez", "andres.ramirez@docucloud.local");
        ensureUserRole(andres, editorRole);

        UsuarioEntity usuario = ensureSeedUser(organizacion, 1004004004L, "Sofia Torres", "sofia.torres@docucloud.local");
        ensureUserRole(usuario, userRole);

        organizacionRepository.findAll().forEach(this::ensureCatalogosBase);
        organizacionRepository.findAll().forEach(this::ensureWorkflows);
    }

    private RolEntity ensureRole(String nombre, String descripcion) {
        return rolRepository.findByNombre(nombre)
                .map(rol -> {
                    if (rol.getDescripcion() == null || rol.getDescripcion().isBlank()) {
                        rol.setDescripcion(descripcion);
                        return rolRepository.save(rol);
                    }
                    return rol;
                })
                .orElseGet(() -> {
                    RolEntity rol = new RolEntity();
                    rol.setNombre(nombre);
                    rol.setDescripcion(descripcion);
                    return rolRepository.save(rol);
                });
    }

    private OrganizacionEntity ensureOrganizacion() {
        return organizacionRepository.findById(organizacionNit)
                .orElseGet(() -> {
                    OrganizacionEntity organizacion = new OrganizacionEntity();
                    organizacion.setNit(organizacionNit);
                    organizacion.setNombre("DocuCloud Demo");
                    organizacion.setEmail("contacto@docucloud.local");
                    organizacion.setTelefono("606 555 0140");
                    organizacion.setDirCalle("Carrera 15");
                    organizacion.setDirNumero("24-10");
                    organizacion.setDirComuna("Centro");
                    organizacion.setActive(true);
                    organizacion.setCreadoEn(LocalDateTime.now());
                    return organizacionRepository.save(organizacion);
                });
    }

    private UsuarioEntity ensureAdminUser(OrganizacionEntity organizacion) {
        Optional<UsuarioEntity> existingAdmin = usuarioRepository.findById(adminCedula);
        if (existingAdmin.isPresent()) {
            UsuarioEntity admin = existingAdmin.get();
            if (Boolean.FALSE.equals(admin.getActive())) {
                admin.setActive(true);
            }
            return usuarioRepository.save(admin);
        }

        UsuarioEntity admin = new UsuarioEntity();
        admin.setCedula(adminCedula);
        admin.setNombre("Administrador DocuCloud");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setActive(true);
        admin.setOrganizacion(organizacion);
        admin.setCreadoEn(LocalDateTime.now());
        return usuarioRepository.save(admin);
    }

    private void ensureUserRole(UsuarioEntity usuario, RolEntity rol) {
        if (rolUsuarioRepository.existsByUsuarioAndRol(usuario, rol)) {
            return;
        }

        RolUsuarioEntity rolUsuario = new RolUsuarioEntity();
        rolUsuario.setUsuario(usuario);
        rolUsuario.setRol(rol);
        rolUsuarioRepository.save(rolUsuario);
    }

    private UsuarioEntity ensureSeedUser(OrganizacionEntity organizacion, Long cedula, String nombre, String email) {
        return usuarioRepository.findById(cedula)
                .orElseGet(() -> {
                    UsuarioEntity user = new UsuarioEntity();
                    user.setCedula(cedula);
                    user.setNombre(nombre);
                    user.setEmail(email);
                    user.setPasswordHash(passwordEncoder.encode("User1234*"));
                    user.setActive(true);
                    user.setOrganizacion(organizacion);
                    user.setCreadoEn(LocalDateTime.now());
                    return usuarioRepository.save(user);
                });
    }

    private void ensureCatalogosBase(OrganizacionEntity organizacion) {
        ensureTipoDocumento(organizacion, "Contrato", "Documentos contractuales internos y externos");
        ensureTipoDocumento(organizacion, "Factura", "Soportes contables y facturacion");
        ensureTipoDocumento(organizacion, "Acta", "Actas de comite, reunion o aprobacion");
        ensureTipoDocumento(organizacion, "Informe", "Informes operativos y administrativos");

        ensureEstadoDocumento(organizacion, "Creado", "#3A7CA5", true, false);
        ensureEstadoDocumento(organizacion, "En revision", "#C49A3A", false, false);
        ensureEstadoDocumento(organizacion, "Aprobado", "#2D6A4F", false, true);
        ensureEstadoDocumento(organizacion, "Rechazado", "#A63D40", false, true);
    }

    private void ensureTipoDocumento(OrganizacionEntity organizacion, String nombre, String descripcion) {
        TipoDocumentoEntity tipo = tipoDocumentoRepository.findByNombreAndOrganizacion(nombre, organizacion)
                .orElseGet(() -> {
                    TipoDocumentoEntity nuevo = new TipoDocumentoEntity();
                    nuevo.setNombre(nombre);
                    nuevo.setOrganizacion(organizacion);
                    nuevo.setCreadoEn(LocalDateTime.now());
                    return nuevo;
                });

        tipo.setDescripcion(descripcion);
        tipo.setActive(true);
        tipoDocumentoRepository.save(tipo);
    }

    private void ensureEstadoDocumento(
            OrganizacionEntity organizacion,
            String nombre,
            String color,
            boolean esInicial,
            boolean esFinal
    ) {
        boolean hasInitial = estadoDocumentoRepository.findByOrganizacionAndEsInicialTrue(organizacion).isPresent();
        EstadoDocumentoEntity estado = estadoDocumentoRepository.findByNombreAndOrganizacion(nombre, organizacion)
                .orElseGet(() -> {
                    EstadoDocumentoEntity nuevo = new EstadoDocumentoEntity();
                    nuevo.setNombre(nombre);
                    nuevo.setOrganizacion(organizacion);
                    return nuevo;
                });

        estado.setColor(color);
        estado.setEsInicial(esInicial && (!hasInitial || Boolean.TRUE.equals(estado.getEsInicial())));
        estado.setEsFinal(esFinal);
        estadoDocumentoRepository.save(estado);
    }

    private void ensureWorkflows(OrganizacionEntity organizacion) {
        Optional<TipoDocumentoEntity> contratoOpt = tipoDocumentoRepository.findByNombreAndOrganizacion("Contrato", organizacion);
        if (contratoOpt.isEmpty()) {
            return;
        }
        TipoDocumentoEntity contrato = contratoOpt.get();

        boolean workflowExists = flujoTrabajoRepository.existsByOrganizacionAndTipoDocumentoAndActivoTrue(organizacion, contrato);
        if (workflowExists) {
            return;
        }

        FlujoTrabajoEntity flujo = new FlujoTrabajoEntity();
        flujo.setNombre("Flujo de Contratos");
        flujo.setDescripcion("Flujo de aprobación para Contratos");
        flujo.setActivo(true);
        flujo.setOrganizacion(organizacion);
        flujo.setTipoDocumento(contrato);
        final FlujoTrabajoEntity savedFlujo = flujoTrabajoRepository.save(flujo);

        RolEntity revisorRol = rolRepository.findByNombre("REVISOR").orElseThrow();
        RolEntity aprobadorRol = rolRepository.findByNombre("APROBADOR").orElseThrow();

        EstadoDocumentoEntity enRevisionEstado = estadoDocumentoRepository.findByNombreAndOrganizacion("En revision", organizacion).orElseThrow();
        EstadoDocumentoEntity aprobadoEstado = estadoDocumentoRepository.findByNombreAndOrganizacion("Aprobado", organizacion).orElseThrow();

        FlujoTrabajoPasoEntity paso1 = new FlujoTrabajoPasoEntity();
        paso1.setNombre("Revisión de Contrato");
        paso1.setDescripcion("Paso de revisión del contrato por un Revisor");
        paso1.setOrdenPaso(1);
        paso1.setFlujoTrabajo(savedFlujo);
        paso1.setRolRequerido(revisorRol);
        paso1.setObjetivoEstado(enRevisionEstado);
        flujoTrabajoPasoRepository.save(paso1);

        FlujoTrabajoPasoEntity paso2 = new FlujoTrabajoPasoEntity();
        paso2.setNombre("Aprobación de Contrato");
        paso2.setDescripcion("Paso de aprobación final por un Aprobador");
        paso2.setOrdenPaso(2);
        paso2.setFlujoTrabajo(savedFlujo);
        paso2.setRolRequerido(aprobadorRol);
        paso2.setObjetivoEstado(aprobadoEstado);
        flujoTrabajoPasoRepository.save(paso2);
    }
}
