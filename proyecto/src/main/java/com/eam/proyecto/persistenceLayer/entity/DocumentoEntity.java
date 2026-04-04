package com.docucloud.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;

    private String descripcion;

    private Integer version;

    @Column(name = "archivo_nombre")
    private String archivoNombre;

    @Column(name = "archivo_ruta")
    private String archivoRuta;

    @Column(name = "tamaño_archivo")
    private Long tamañoArchivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private UsuarioEntity creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizacionEntity organizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_tipo_id")
    private TipoDocumentoEntity tipoDocumento;

    @Enumerated(EnumType.STRING)
    private EstadoDocumentoEnum estado;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;
}
