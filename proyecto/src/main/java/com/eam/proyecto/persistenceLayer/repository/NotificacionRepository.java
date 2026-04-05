package com.docucloud.persistence.repository;

import com.docucloud.persistence.entity.DocumentoEntity;
import com.docucloud.persistence.entity.NotificacionEntity;
import com.docucloud.persistence.entity.UsuarioEntity;
import com.docucloud.persistence.enums.CanalNotificacionEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {

    List<NotificacionEntity> findByUsuarioOrderByEnviadaADesc(UsuarioEntity usuario);

    List<NotificacionEntity> findByUsuarioAndEstaLeidaFalse(UsuarioEntity usuario);

    List<NotificacionEntity> findByUsuarioAndEstaLeidaTrue(UsuarioEntity usuario);

    List<NotificacionEntity> findByUsuarioAndCanal(UsuarioEntity usuario, CanalNotificacionEnum canal);

    List<NotificacionEntity> findByDocumento(DocumentoEntity documento);

    List<NotificacionEntity> findByUsuarioAndCanalAndEstaLeidaFalse(
            UsuarioEntity usuario, CanalNotificacionEnum canal);

    Long countByUsuarioAndEstaLeidaFalse(UsuarioEntity usuario);

    @Modifying
    @Transactional
    @Query("UPDATE NotificacionEntity n SET n.estaLeida = true WHERE n.usuario = :usuario AND n.estaLeida = false")
    void marcarTodasComoLeidas(@Param("usuario") UsuarioEntity usuario);

    List<NotificacionEntity> findByUsuarioAndCanalAndEnviadaAIsNull(
            UsuarioEntity usuario, CanalNotificacionEnum canal);
}