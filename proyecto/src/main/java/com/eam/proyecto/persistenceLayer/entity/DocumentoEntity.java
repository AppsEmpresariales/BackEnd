package com.eam.proyecto.persistenceLayer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// DocumentoEntity — estado como FK real
@Entity
@Table(name = "documentos")
@Data @NoArgsConstructor @AllArgsConstructor
public class DocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;
    private String descripcion;
    @Column(nullable = false)
    private Integer version;

    @Column(name = "archivo_nombre")
    private String archivoNombre;

    @Column(name = "archivo_ruta")
    private String archivoRuta;

    @Column(name = "tamanio_archivo")
    private Long tamanioArchivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", nullable = false)
    private UsuarioEntity creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizacionEntity organizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumentoEntity tipoDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_documento_id", nullable = false) // FK real, no Enum
    private EstadoDocumentoEntity estadoDocumento;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}
