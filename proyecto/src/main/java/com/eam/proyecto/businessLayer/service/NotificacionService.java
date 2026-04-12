// NotificacionService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.NotificacionCreateDTO;
import com.eam.proyecto.businessLayer.dto.NotificacionDTO;
import com.eam.proyecto.persistenceLayer.entity.enums.CanalNotificacionEnum;
import java.util.List;

public interface NotificacionService {
    NotificacionDTO enviarNotificacion(NotificacionCreateDTO createDTO);
    NotificacionDTO getNotificacionById(Long id);
    List<NotificacionDTO> getNotificacionesByUsuario(Long cedula);
    List<NotificacionDTO> getNoLeidasByUsuario(Long cedula);
    long countNoLeidasByUsuario(Long cedula);
    NotificacionDTO marcarLeida(Long id);
    void marcarTodasComoLeidas(Long cedula);
    void deleteNotificacion(Long id);
}