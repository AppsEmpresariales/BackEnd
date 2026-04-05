package com.docucloud.persistence.repository;

import com.docucloud.persistence.entity.DocumentoEntity;
import com.docucloud.persistence.entity.FlujoTrabajoPasoEntity;
import com.docucloud.persistence.entity.FlujoTrabajoTareaEntity;
import com.docucloud.persistence.entity.UsuarioEntity;
import com.docucloud.persistence.enums.EstadoTareaEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlujoTrabajoTareaRepository extends JpaRepository<FlujoTrabajoTareaEntity, Long> {

    List<FlujoTrabajoTareaEntity> findByDocumento(DocumentoEntity documento);

    List<FlujoTrabajoTareaEntity> findByAsignadoAAndEstado(UsuarioEntity usuario, EstadoTareaEnum estado);

    Optional<FlujoTrabajoTareaEntity> findFirstByDocumentoAndEstadoOrderByCreadoEnDesc(
            DocumentoEntity documento, EstadoTareaEnum estado);

    List<FlujoTrabajoTareaEntity> findByPasoAndEstado(FlujoTrabajoPasoEntity paso, EstadoTareaEnum estado);

    List<FlujoTrabajoTareaEntity> findByAsignadoAAndEstadoOrderByCreadoEnAsc(
            UsuarioEntity usuario, EstadoTareaEnum estado);

    boolean existsByDocumentoAndEstado(DocumentoEntity documento, EstadoTareaEnum estado);

    @Query("""
            SELECT t FROM FlujoTrabajoTareaEntity t
            WHERE t.asignadoA = :usuario
            AND t.estado = 'PENDIENTE'
            AND t.fechaLimite < :ahora
            """)
    List<FlujoTrabajoTareaEntity> findTareasVencidas(
            @Param("usuario") UsuarioEntity usuario,
            @Param("ahora") LocalDateTime ahora);

    List<FlujoTrabajoTareaEntity> findByDocumentoOrderByCreadoEnAsc(DocumentoEntity documento);

    Long countByAsignadoAAndEstado(UsuarioEntity usuario, EstadoTareaEnum estado);
}