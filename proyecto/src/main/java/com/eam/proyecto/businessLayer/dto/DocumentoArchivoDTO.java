package com.eam.proyecto.businessLayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.Resource;

@Data
@AllArgsConstructor
public class DocumentoArchivoDTO {
    private Resource resource;
    private String fileName;
    private String contentType;
    private Long size;
}
