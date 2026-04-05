package com.docucloud.persistence.repository;

import com.docucloud.persistence.entity.AuditRegistroEntity;
import com.docucloud.persistence.entity.DocumentoEntity;
import com.docucloud.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRegistroRepository extends JpaRepository<AuditRegistroEntity, Long> {

    List<AuditRegistroEntity> findByDocumentoOrderByCreadoEnAsc(DocumentoEntity documento);

    List<AuditRegistroEntity> findByDocumentoOrderByCreadoEnDesc(DocumentoEntity documento);

    List<AuditRegistroEntity> findByUsuario(UsuarioEntity usuario);

    List<AuditRegistroEntity> findByDocumentoAndAccion(DocumentoEntity documento, String accion);

    List<AuditRegistroEntity> findByCreadoEnBetweenOrderByCreadoEnDesc(
            LocalDateTime desde, LocalDateTime hasta);

    List<AuditRegistroEntity> findByUsuarioAndCreadoEnBetweenOrderByCreadoEnDesc(
            UsuarioEntity usuario, LocalDateTime desde, LocalDateTime hasta);

    AuditRegistroEntity findFirstByDocumentoOrderByCreadoEnDesc(DocumentoEntity documento);

    @Query("""
            SELECT a FROM AuditRegistroEntity a
            WHERE (:accion IS NULL OR a.accion = :accion)
            AND (:desde IS NULL OR a.creadoEn >= :desde)
            AND (:hasta IS NULL OR a.creadoEn <= :hasta)
            ORDER BY a.creadoEn DESC
            """)
    List<AuditRegistroEntity> trazabilidadCompleta(
            @Param("accion") String accion,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}