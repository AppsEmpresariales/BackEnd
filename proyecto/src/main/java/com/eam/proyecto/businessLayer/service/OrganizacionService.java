// OrganizacionService.java
package com.eam.proyecto.businessLayer.service;

import com.eam.proyecto.businessLayer.dto.OrganizacionCreateDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionDTO;
import com.eam.proyecto.businessLayer.dto.OrganizacionUpdateDTO;
import java.util.List;

public interface OrganizacionService {

    OrganizacionDTO createOrganizacion(OrganizacionCreateDTO createDTO);
    OrganizacionDTO getOrganizacionByNit(Long nit);
    OrganizacionDTO getOrganizacionActivaByNit(Long nit);
    List<OrganizacionDTO> getAllOrganizaciones();
    List<OrganizacionDTO> getOrganizacionesActivas();
    OrganizacionDTO updateOrganizacion(Long nit, OrganizacionUpdateDTO updateDTO);
    void deleteOrganizacion(Long nit);
}