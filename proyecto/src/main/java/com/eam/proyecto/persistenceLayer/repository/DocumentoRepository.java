package com.eam.proyecto.persistenceLayer.repository;

import com.eam.proyecto.persistenceLayer.entity.DocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.EstadoDocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.TipoDocumentoEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<DocumentoEntity, Long> {

    List<DocumentoEntity> findByOrganizacion(OrganizacionEntity organizacion);

    List<DocumentoEntity> findByOrganizacionOrderByCreadoEnDesc(OrganizacionEntity organizacion);

    Optional<DocumentoEntity> findByIdAndOrganizacion(Long id, OrganizacionEntity organizacion);

    List<DocumentoEntity> findByOrganizacionAndTipoDocumento(
            OrganizacionEntity organizacion, TipoDocumentoEntity tipoDocumento);

    List<DocumentoEntity> findByOrganizacionAndEstadoDocumento(
            OrganizacionEntity organizacion, EstadoDocumentoEntity estadoDocumento);

    List<DocumentoEntity> findByOrganizacionAndTipoDocumentoAndEstadoDocumento(
            OrganizacionEntity organizacion,
            TipoDocumentoEntity tipoDocumento,
            EstadoDocumentoEntity estadoDocumento);

    List<DocumentoEntity> findByOrganizacionAndCreadoEnBetween(
            OrganizacionEntity organizacion,
            LocalDateTime desde,
            LocalDateTime hasta);

    List<DocumentoEntity> findByOrganizacionAndTituloContainingIgnoreCase(
            OrganizacionEntity organizacion, String titulo);

    List<DocumentoEntity> findByOrganizacionAndCreadoPor(
            OrganizacionEntity organizacion, UsuarioEntity creadoPor);

    @Query("""
            SELECT d FROM DocumentoEntity d
            WHERE d.organizacion = :org
            AND (:tipo IS NULL OR d.tipoDocumento = :tipo)
            AND (:estado IS NULL OR d.estadoDocumento = :estado)
            AND (:desde IS NULL OR d.creadoEn >= :desde)
            AND (:hasta IS NULL OR d.creadoEn <= :hasta)
            ORDER BY d.creadoEn DESC
            """)
    List<DocumentoEntity> filtrar(
            @Param("org") OrganizacionEntity organizacion,
            @Param("tipo") TipoDocumentoEntity tipoDocumento,
            @Param("estado") EstadoDocumentoEntity estadoDocumento,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    Long countByOrganizacionAndEstadoDocumento(
            OrganizacionEntity organizacion, EstadoDocumentoEntity estadoDocumento);

    Optional<DocumentoEntity> findByIdAndArchivoRutaIsNotNull(Long id);

    List<DocumentoEntity> findTop10ByOrganizacionOrderByCreadoEnDesc(OrganizacionEntity organizacion);
}