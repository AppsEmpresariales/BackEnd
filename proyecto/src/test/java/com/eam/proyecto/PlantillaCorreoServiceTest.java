package com.eam.proyecto;

import com.eam.proyecto.businessLayer.dto.PlantillaCorreoCreateDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoDTO;
import com.eam.proyecto.businessLayer.dto.PlantillaCorreoUpdateDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.impl.PlantillaCorreoServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.PlantillaCorreoDAO;
import com.eam.proyecto.persistenceLayer.entity.enums.TipoEventoEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests para PlantillaCorreoServiceImpl
 *
 * OPERACIONES BAJO PRUEBA (RF40 / RF43):
 * - createPlantillaCorreo : crear plantilla con regla de unicidad activa por evento.
 * - getPlantillaCorreoById : búsqueda por ID.
 * - getPlantillaActivaByOrganizacionAndEvento : resolver plantilla para disparar notificación.
 * - getPlantillasActivasByOrganizacion / getAllPlantillasByOrganizacion : listados.
 * - updatePlantillaCorreo : actualización de asunto y cuerpo.
 * - activarPlantilla / desactivarPlantilla : cambio de estado activo.
 * - deletePlantillaCorreo : eliminación física.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlantillaCorreoService - Unit Tests")
class PlantillaCorreoServiceTest {

    // ─── Dependencias mockeadas ───────────────────────────────────────────────

    @Mock
    private PlantillaCorreoDAO plantillaCorreoDAO;

    @Mock
    private OrganizacionService organizacionService;

    // ─── Sistema bajo prueba (SUT) ────────────────────────────────────────────

    @InjectMocks
    private PlantillaCorreoServiceImpl plantillaCorreoService;

    // ─── Datos de prueba reutilizables ────────────────────────────────────────

    private PlantillaCorreoCreateDTO validCreateDTO;
    private PlantillaCorreoUpdateDTO validUpdateDTO;
    private PlantillaCorreoDTO validPlantillaDTO;

    private final Long PLANTILLA_ID     = 1L;
    private final Long ORGANIZACION_NIT = 900111222L;
    private final TipoEventoEnum EVENTO = TipoEventoEnum.DOCUMENTO_CREADO;

    /**
     * Se ejecuta antes de cada test.
     * Construye objetos de entrada y respuesta válidos.
     */
    @BeforeEach
    void setUp() {
        validCreateDTO = new PlantillaCorreoCreateDTO();
        validCreateDTO.setTipoEvento(EVENTO);
        validCreateDTO.setAsunto("Nuevo documento registrado en DocuCloud");
        validCreateDTO.setCuerpo("Estimado usuario, se ha creado un nuevo documento.");
        validCreateDTO.setOrganizacionNit(ORGANIZACION_NIT);
        validCreateDTO.setNombre("Plantilla creación de documento");

        validUpdateDTO = new PlantillaCorreoUpdateDTO();
        validUpdateDTO.setAsunto("Asunto actualizado");
        validUpdateDTO.setCuerpo("Cuerpo actualizado correctamente.");

        validPlantillaDTO = new PlantillaCorreoDTO();
        validPlantillaDTO.setOrganizacionNit(ORGANIZACION_NIT);
        validPlantillaDTO.setTipoEvento(EVENTO);
    }

    // ==================== CREATE PLANTILLA ====================

    @Test
    @DisplayName("CREATE - datos válidos deben retornar plantilla creada con activo=true")
    void createPlantillaCorreo_DatosValidos_RetornaPlantillaCreada() {
        // ARRANGE
        when(plantillaCorreoDAO.existeActivaByOrganizacionNitAndTipoEvento(
                ORGANIZACION_NIT, EVENTO))
                .thenReturn(false);
        when(plantillaCorreoDAO.save(any(PlantillaCorreoCreateDTO.class)))
                .thenReturn(validPlantillaDTO);

        // ACT
        PlantillaCorreoDTO result = plantillaCorreoService.createPlantillaCorreo(validCreateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        verify(organizacionService, times(1)).getOrganizacionActivaByNit(ORGANIZACION_NIT);
        verify(plantillaCorreoDAO,  times(1))
                .existeActivaByOrganizacionNitAndTipoEvento(ORGANIZACION_NIT, EVENTO);
        verify(plantillaCorreoDAO,  times(1)).save(any(PlantillaCorreoCreateDTO.class));
    }

    @Test
    @DisplayName("CREATE - el service asigna activo=true antes de persistir")
    void createPlantillaCorreo_DatosValidos_AsignaActivoTrue() {
        // ARRANGE
        when(plantillaCorreoDAO.existeActivaByOrganizacionNitAndTipoEvento(
                ORGANIZACION_NIT, EVENTO))
                .thenReturn(false);
        when(plantillaCorreoDAO.save(any(PlantillaCorreoCreateDTO.class)))
                .thenReturn(validPlantillaDTO);

        // ACT
        plantillaCorreoService.createPlantillaCorreo(validCreateDTO);

        // ASSERT — el service debe haber asignado activo=true antes de llamar al DAO
        assertThat(validCreateDTO.getActivo()).isTrue();
    }

    @Test
    @DisplayName("CREATE - ya existe plantilla activa para el mismo evento debe lanzar IllegalStateException")
    void createPlantillaCorreo_PlantillaActivaDuplicada_LanzaIllegalStateException() {
        // ARRANGE — ya hay una plantilla activa para ese evento en la org
        when(plantillaCorreoDAO.existeActivaByOrganizacionNitAndTipoEvento(
                ORGANIZACION_NIT, EVENTO))
                .thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.createPlantillaCorreo(validCreateDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe una plantilla activa para el evento");

        verify(plantillaCorreoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - tipoEvento null debe lanzar IllegalArgumentException")
    void createPlantillaCorreo_TipoEventoNull_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setTipoEvento(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.createPlantillaCorreo(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo de evento es obligatorio");

        verify(organizacionService, never()).getOrganizacionActivaByNit(any());
        verify(plantillaCorreoDAO,  never()).save(any());
    }

    @Test
    @DisplayName("CREATE - asunto null debe lanzar IllegalArgumentException")
    void createPlantillaCorreo_AsuntoNull_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setAsunto(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.createPlantillaCorreo(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asunto de la plantilla es obligatorio");

        verify(plantillaCorreoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - asunto vacío debe lanzar IllegalArgumentException")
    void createPlantillaCorreo_AsuntoVacio_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setAsunto("   ");

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.createPlantillaCorreo(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asunto de la plantilla es obligatorio");

        verify(plantillaCorreoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - cuerpo null debe lanzar IllegalArgumentException")
    void createPlantillaCorreo_CuerpoNull_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setCuerpo(null);

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.createPlantillaCorreo(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cuerpo de la plantilla es obligatorio");

        verify(plantillaCorreoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - cuerpo vacío debe lanzar IllegalArgumentException")
    void createPlantillaCorreo_CuerpoVacio_LanzaIllegalArgumentException() {
        // ARRANGE
        validCreateDTO.setCuerpo("  ");

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.createPlantillaCorreo(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cuerpo de la plantilla es obligatorio");

        verify(plantillaCorreoDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - organización inexistente debe lanzar RuntimeException sin persistir")
    void createPlantillaCorreo_OrganizacionInexistente_LanzaRuntimeExceptionSinPersistir() {
        // ARRANGE
        when(organizacionService.getOrganizacionActivaByNit(ORGANIZACION_NIT))
                .thenThrow(new RuntimeException("Organización no encontrada o inactiva con NIT: " + ORGANIZACION_NIT));

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.createPlantillaCorreo(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Organización no encontrada");

        verify(plantillaCorreoDAO, never()).save(any());
    }

    // ==================== GET PLANTILLA BY ID ====================

    @Test
    @DisplayName("GET BY ID - plantilla existente debe retornar DTO")
    void getPlantillaCorreoById_Existente_RetornaDTO() {
        // ARRANGE
        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(validPlantillaDTO));

        // ACT
        PlantillaCorreoDTO result = plantillaCorreoService.getPlantillaCorreoById(PLANTILLA_ID);

        // ASSERT
        assertThat(result).isNotNull();
        verify(plantillaCorreoDAO, times(1)).findById(PLANTILLA_ID);
    }

    @Test
    @DisplayName("GET BY ID - plantilla inexistente debe lanzar RuntimeException")
    void getPlantillaCorreoById_Inexistente_LanzaRuntimeException() {
        // ARRANGE
        Long idInexistente = 999L;
        when(plantillaCorreoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.getPlantillaCorreoById(idInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Plantilla de correo no encontrada con ID: " + idInexistente);
    }

    // ==================== GET PLANTILLA ACTIVA POR EVENTO ====================

    @Test
    @DisplayName("GET ACTIVA - plantilla activa para evento existente debe retornar DTO")
    void getPlantillaActivaByOrganizacionAndEvento_Existente_RetornaDTO() {
        // ARRANGE
        when(plantillaCorreoDAO.findActivaByOrganizacionNitAndTipoEvento(
                ORGANIZACION_NIT, EVENTO))
                .thenReturn(Optional.of(validPlantillaDTO));

        // ACT
        PlantillaCorreoDTO result =
                plantillaCorreoService.getPlantillaActivaByOrganizacionAndEvento(
                        ORGANIZACION_NIT, EVENTO);

        // ASSERT
        assertThat(result).isNotNull();
        verify(plantillaCorreoDAO, times(1))
                .findActivaByOrganizacionNitAndTipoEvento(ORGANIZACION_NIT, EVENTO);
    }

    @Test
    @DisplayName("GET ACTIVA - sin plantilla configurada debe lanzar IllegalStateException")
    void getPlantillaActivaByOrganizacionAndEvento_SinPlantilla_LanzaIllegalStateException() {
        // ARRANGE
        when(plantillaCorreoDAO.findActivaByOrganizacionNitAndTipoEvento(
                ORGANIZACION_NIT, EVENTO))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() ->
                plantillaCorreoService.getPlantillaActivaByOrganizacionAndEvento(
                        ORGANIZACION_NIT, EVENTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay plantilla de correo activa configurada para el evento");
    }

    // ==================== LISTADOS ====================

    @Test
    @DisplayName("GET ACTIVAS - organización válida debe retornar solo plantillas activas")
    void getPlantillasActivasByOrganizacion_OrgValida_RetornaActivas() {
        // ARRANGE
        List<PlantillaCorreoDTO> activas = Arrays.asList(
                new PlantillaCorreoDTO(), new PlantillaCorreoDTO()
        );
        when(plantillaCorreoDAO.findActivasByOrganizacionNit(ORGANIZACION_NIT))
                .thenReturn(activas);

        // ACT
        List<PlantillaCorreoDTO> result =
                plantillaCorreoService.getPlantillasActivasByOrganizacion(ORGANIZACION_NIT);

        // ASSERT
        assertThat(result).hasSize(2);
        verify(organizacionService, times(1)).getOrganizacionByNit(ORGANIZACION_NIT);
        verify(plantillaCorreoDAO,  times(1)).findActivasByOrganizacionNit(ORGANIZACION_NIT);
    }

    @Test
    @DisplayName("GET ALL - debe retornar plantillas activas e inactivas de la organización")
    void getAllPlantillasByOrganizacion_OrgValida_RetornaTodasLasPlantillas() {
        // ARRANGE
        List<PlantillaCorreoDTO> todas = Arrays.asList(
                new PlantillaCorreoDTO(), new PlantillaCorreoDTO(), new PlantillaCorreoDTO()
        );
        when(plantillaCorreoDAO.findAllByOrganizacionNit(ORGANIZACION_NIT))
                .thenReturn(todas);

        // ACT
        List<PlantillaCorreoDTO> result =
                plantillaCorreoService.getAllPlantillasByOrganizacion(ORGANIZACION_NIT);

        // ASSERT
        assertThat(result).hasSize(3);
        verify(organizacionService, times(1)).getOrganizacionByNit(ORGANIZACION_NIT);
        verify(plantillaCorreoDAO,  times(1)).findAllByOrganizacionNit(ORGANIZACION_NIT);
    }

    // ==================== UPDATE PLANTILLA ====================

    @Test
    @DisplayName("UPDATE - datos válidos deben retornar plantilla actualizada")
    void updatePlantillaCorreo_DatosValidos_RetornaPlantillaActualizada() {
        // ARRANGE
        PlantillaCorreoDTO actualizada = new PlantillaCorreoDTO();
        actualizada.setAsunto("Asunto actualizado");

        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(validPlantillaDTO));
        when(plantillaCorreoDAO.update(eq(PLANTILLA_ID), any(PlantillaCorreoUpdateDTO.class)))
                .thenReturn(Optional.of(actualizada));

        // ACT
        PlantillaCorreoDTO result =
                plantillaCorreoService.updatePlantillaCorreo(PLANTILLA_ID, validUpdateDTO);

        // ASSERT
        assertThat(result).isNotNull();
        verify(plantillaCorreoDAO, times(1))
                .update(eq(PLANTILLA_ID), any(PlantillaCorreoUpdateDTO.class));
    }

    @Test
    @DisplayName("UPDATE - asunto vacío debe lanzar IllegalArgumentException")
    void updatePlantillaCorreo_AsuntoVacio_LanzaIllegalArgumentException() {
        // ARRANGE
        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(validPlantillaDTO));
        validUpdateDTO.setAsunto("   ");

        // ACT & ASSERT
        assertThatThrownBy(() ->
                plantillaCorreoService.updatePlantillaCorreo(PLANTILLA_ID, validUpdateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asunto no puede estar vacío");

        verify(plantillaCorreoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("UPDATE - cuerpo vacío debe lanzar IllegalArgumentException")
    void updatePlantillaCorreo_CuerpoVacio_LanzaIllegalArgumentException() {
        // ARRANGE
        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(validPlantillaDTO));
        validUpdateDTO.setCuerpo("  ");

        // ACT & ASSERT
        assertThatThrownBy(() ->
                plantillaCorreoService.updatePlantillaCorreo(PLANTILLA_ID, validUpdateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cuerpo de la plantilla no puede estar vacío");

        verify(plantillaCorreoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("UPDATE - plantilla inexistente debe lanzar RuntimeException")
    void updatePlantillaCorreo_Inexistente_LanzaRuntimeException() {
        // ARRANGE
        Long idInexistente = 888L;
        when(plantillaCorreoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() ->
                plantillaCorreoService.updatePlantillaCorreo(idInexistente, validUpdateDTO))
                .isInstanceOf(RuntimeException.class);

        verify(plantillaCorreoDAO, never()).update(any(), any());
    }

    // ==================== ACTIVAR PLANTILLA ====================

    @Test
    @DisplayName("ACTIVAR - plantilla inactiva sin conflicto debe activarse correctamente")
    void activarPlantilla_PlantillaInactivaSinConflicto_SeActiva() {
        // ARRANGE — la plantilla está inactiva y no hay otra activa para el mismo evento
        PlantillaCorreoDTO inactiva = new PlantillaCorreoDTO();
        inactiva.setActivo(false);
        inactiva.setOrganizacionNit(ORGANIZACION_NIT);
        inactiva.setTipoEvento(EVENTO);

        PlantillaCorreoDTO activada = new PlantillaCorreoDTO();
        activada.setActivo(true);

        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(inactiva));
        when(plantillaCorreoDAO.existeActivaByOrganizacionNitAndTipoEvento(
                ORGANIZACION_NIT, EVENTO))
                .thenReturn(false);
        when(plantillaCorreoDAO.activar(PLANTILLA_ID))
                .thenReturn(Optional.of(activada));

        // ACT
        PlantillaCorreoDTO result = plantillaCorreoService.activarPlantilla(PLANTILLA_ID);

        // ASSERT
        assertThat(result).isNotNull();
        verify(plantillaCorreoDAO, times(1)).activar(PLANTILLA_ID);
    }

    @Test
    @DisplayName("ACTIVAR - plantilla ya activa debe lanzar IllegalStateException")
    void activarPlantilla_YaActiva_LanzaIllegalStateException() {
        // ARRANGE — la plantilla ya está activa
        PlantillaCorreoDTO yaActiva = new PlantillaCorreoDTO();
        yaActiva.setActivo(true);

        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(yaActiva));

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.activarPlantilla(PLANTILLA_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La plantilla ya se encuentra activa");

        verify(plantillaCorreoDAO, never()).activar(any());
    }

    @Test
    @DisplayName("ACTIVAR - existe otra plantilla activa para el mismo evento debe lanzar IllegalStateException")
    void activarPlantilla_OtraActivaMismoEvento_LanzaIllegalStateException() {
        // ARRANGE — inactiva, pero ya hay otra activa para el mismo evento
        PlantillaCorreoDTO inactiva = new PlantillaCorreoDTO();
        inactiva.setActivo(false);
        inactiva.setOrganizacionNit(ORGANIZACION_NIT);
        inactiva.setTipoEvento(EVENTO);

        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(inactiva));
        when(plantillaCorreoDAO.existeActivaByOrganizacionNitAndTipoEvento(
                ORGANIZACION_NIT, EVENTO))
                .thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.activarPlantilla(PLANTILLA_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe una plantilla activa para el evento");

        verify(plantillaCorreoDAO, never()).activar(any());
    }

    // ==================== DESACTIVAR PLANTILLA ====================

    @Test
    @DisplayName("DESACTIVAR - plantilla activa debe desactivarse correctamente")
    void desactivarPlantilla_PlantillaActiva_SeDesactiva() {
        // ARRANGE
        PlantillaCorreoDTO activa = new PlantillaCorreoDTO();
        activa.setActivo(true);

        PlantillaCorreoDTO desactivada = new PlantillaCorreoDTO();
        desactivada.setActivo(false);

        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(activa));
        when(plantillaCorreoDAO.desactivar(PLANTILLA_ID))
                .thenReturn(Optional.of(desactivada));

        // ACT
        PlantillaCorreoDTO result = plantillaCorreoService.desactivarPlantilla(PLANTILLA_ID);

        // ASSERT
        assertThat(result).isNotNull();
        verify(plantillaCorreoDAO, times(1)).desactivar(PLANTILLA_ID);
    }

    @Test
    @DisplayName("DESACTIVAR - plantilla ya inactiva debe lanzar IllegalStateException")
    void desactivarPlantilla_YaInactiva_LanzaIllegalStateException() {
        // ARRANGE
        PlantillaCorreoDTO yaInactiva = new PlantillaCorreoDTO();
        yaInactiva.setActivo(false);

        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(yaInactiva));

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.desactivarPlantilla(PLANTILLA_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La plantilla ya se encuentra inactiva");

        verify(plantillaCorreoDAO, never()).desactivar(any());
    }

    // ==================== DELETE PLANTILLA ====================

    @Test
    @DisplayName("DELETE - plantilla existente debe eliminarse sin lanzar excepción")
    void deletePlantillaCorreo_Existente_EliminaSinError() {
        // ARRANGE
        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(validPlantillaDTO));
        when(plantillaCorreoDAO.deleteById(PLANTILLA_ID))
                .thenReturn(true);

        // ACT & ASSERT
        assertThatCode(() -> plantillaCorreoService.deletePlantillaCorreo(PLANTILLA_ID))
                .doesNotThrowAnyException();

        verify(plantillaCorreoDAO, times(1)).deleteById(PLANTILLA_ID);
    }

    @Test
    @DisplayName("DELETE - error en DAO debe lanzar RuntimeException")
    void deletePlantillaCorreo_ErrorEnDAO_LanzaRuntimeException() {
        // ARRANGE — el DAO devuelve false (fallo en eliminación)
        when(plantillaCorreoDAO.findById(PLANTILLA_ID))
                .thenReturn(Optional.of(validPlantillaDTO));
        when(plantillaCorreoDAO.deleteById(PLANTILLA_ID))
                .thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.deletePlantillaCorreo(PLANTILLA_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al eliminar plantilla de correo ID: " + PLANTILLA_ID);
    }

    @Test
    @DisplayName("DELETE - plantilla inexistente debe lanzar RuntimeException sin llamar deleteById")
    void deletePlantillaCorreo_Inexistente_LanzaRuntimeExceptionSinLlamarDelete() {
        // ARRANGE
        Long idInexistente = 666L;
        when(plantillaCorreoDAO.findById(idInexistente))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> plantillaCorreoService.deletePlantillaCorreo(idInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Plantilla de correo no encontrada con ID: " + idInexistente);

        verify(plantillaCorreoDAO, never()).deleteById(any());
    }
}