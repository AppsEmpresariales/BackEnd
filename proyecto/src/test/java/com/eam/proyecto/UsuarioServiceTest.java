package com.eam.proyecto;

import com.eam.proyecto.businessLayer.dto.UsuarioCreateDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioDTO;
import com.eam.proyecto.businessLayer.dto.UsuarioUpdateDTO;
import com.eam.proyecto.businessLayer.service.OrganizacionService;
import com.eam.proyecto.businessLayer.service.impl.UsuarioServiceImpl;
import com.eam.proyecto.persistenceLayer.dao.UsuarioDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests para UsuarioServiceImpl
 *
 * OBJETIVO: Probar la lógica de negocio del servicio de forma aislada
 * - No requiere base de datos
 * - No requiere Spring Context
 * - Usa mocks para UsuarioDAO, OrganizacionService y BCryptPasswordEncoder
 * - Cubre RF09 / RF10 / RF12 / RF13 / RF14 / RF16
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - Unit Tests")
public class UsuarioServiceTest {

    // ─── Dependencias mockeadas ───────────────────────────────────────────────
    @Mock
    private UsuarioDAO usuarioDAO;

    @Mock
    private OrganizacionService organizacionService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    // ─── Clase bajo prueba (SUT) ──────────────────────────────────────────────
    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    // ─── Datos de prueba compartidos ─────────────────────────────────────────
    private UsuarioCreateDTO validCreateDTO;
    private UsuarioDTO        validUsuarioDTO;
    private Long              validCedula;
    private Long              validOrganizacionNit;

    /**
     * Se ejecuta antes de cada test.
     * Inicializa objetos en estado válido para reutilizarlos.
     */
    @BeforeEach
    void setUp() {
        validCedula         = 10203040L;
        validOrganizacionNit = 900123456L;

        validCreateDTO = new UsuarioCreateDTO();
        validCreateDTO.setCedula(validCedula);
        validCreateDTO.setNombre("Ana Torres");
        validCreateDTO.setEmail("ana.torres@empresa.com");
        validCreateDTO.setPassword("Secreta123");
        validCreateDTO.setOrganizacionNit(validOrganizacionNit);

        validUsuarioDTO = new UsuarioDTO();
        validUsuarioDTO.setCedula(validCedula);
        validUsuarioDTO.setNombre("Ana Torres");
        validUsuarioDTO.setEmail("ana.torres@empresa.com");
        validUsuarioDTO.setActive(true);
        validUsuarioDTO.setOrganizacionNit(validOrganizacionNit);
    }

    // ==================== CREATE ====================

    @Test
    @DisplayName("CREATE - Datos válidos deben retornar usuario creado con contraseña encriptada")
    void createUsuario_ValidData_ShouldReturnCreatedUsuario() {
        // Arrange
        String hashedPassword = "$2a$10$hasheado";
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(null); // El service solo llama al método; si no lanza excepción, continúa
        when(usuarioDAO.existsByEmail(validCreateDTO.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validCreateDTO.getPassword())).thenReturn(hashedPassword);
        when(usuarioDAO.save(any(UsuarioCreateDTO.class))).thenReturn(validUsuarioDTO);

        // Act
        UsuarioDTO result = usuarioService.createUsuario(validCreateDTO);

        // Assert - estado
        assertThat(result).isNotNull();
        assertThat(result.getCedula()).isEqualTo(validCedula);
        assertThat(result.getActive()).isTrue();

        // Assert - comportamiento: contraseña encriptada y active=true antes de persistir
        ArgumentCaptor<UsuarioCreateDTO> captor = ArgumentCaptor.forClass(UsuarioCreateDTO.class);
        verify(organizacionService, times(1)).getOrganizacionActivaByNit(validOrganizacionNit);
        verify(usuarioDAO, times(1)).existsByEmail(validCreateDTO.getEmail());
        verify(passwordEncoder, times(1)).encode("Secreta123");
        verify(usuarioDAO, times(1)).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo(hashedPassword);
        assertThat(captor.getValue().getActive()).isTrue();
    }

    @Test
    @DisplayName("CREATE - Email duplicado debe lanzar IllegalArgumentException sin persistir")
    void createUsuario_DuplicateEmail_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenReturn(null);
        when(usuarioDAO.existsByEmail(validCreateDTO.getEmail())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.createUsuario(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");

        verify(usuarioDAO, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("CREATE - Organización inactiva o inexistente debe propagar RuntimeException")
    void createUsuario_InactiveOrganizacion_ShouldThrowRuntimeException() {
        // Arrange
        when(organizacionService.getOrganizacionActivaByNit(validOrganizacionNit))
                .thenThrow(new RuntimeException("Organización no encontrada o inactiva"));

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.createUsuario(validCreateDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inactiva");

        verify(usuarioDAO, never()).existsByEmail(anyString());
        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Cédula null debe lanzar IllegalArgumentException")
    void createUsuario_NullCedula_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setCedula(null);

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.createUsuario(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula");

        verify(organizacionService, never()).getOrganizacionActivaByNit(any());
        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Cédula con valor cero o negativo debe lanzar IllegalArgumentException")
    void createUsuario_NonPositiveCedula_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setCedula(-50L);

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.createUsuario(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula");

        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Nombre null debe lanzar IllegalArgumentException")
    void createUsuario_NullNombre_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setNombre(null);

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.createUsuario(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Nombre mayor a 150 caracteres debe lanzar IllegalArgumentException")
    void createUsuario_NombreTooLong_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setNombre("B".repeat(151));

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.createUsuario(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("150 caracteres");

        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Email null debe lanzar IllegalArgumentException")
    void createUsuario_NullEmail_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setEmail(null);

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.createUsuario(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");

        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - Contraseña con menos de 8 caracteres debe lanzar IllegalArgumentException")
    void createUsuario_ShortPassword_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setPassword("corta");

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.createUsuario(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 caracteres");

        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("CREATE - NIT de organización null debe lanzar IllegalArgumentException")
    void createUsuario_NullOrganizacionNit_ShouldThrowIllegalArgumentException() {
        // Arrange
        validCreateDTO.setOrganizacionNit(null);

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.createUsuario(validCreateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NIT");

        verify(usuarioDAO, never()).save(any());
    }

    // ==================== READ ====================

    @Test
    @DisplayName("READ - Cédula existente debe retornar UsuarioDTO")
    void getUsuarioByCedula_ExistingCedula_ShouldReturnUsuario() {
        // Arrange
        when(usuarioDAO.findByCedula(validCedula)).thenReturn(Optional.of(validUsuarioDTO));

        // Act
        UsuarioDTO result = usuarioService.getUsuarioByCedula(validCedula);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCedula()).isEqualTo(validCedula);
        assertThat(result.getNombre()).isEqualTo("Ana Torres");
        verify(usuarioDAO, times(1)).findByCedula(validCedula);
    }

    @Test
    @DisplayName("READ - Cédula inexistente debe lanzar RuntimeException")
    void getUsuarioByCedula_NonExistentCedula_ShouldThrowRuntimeException() {
        // Arrange
        when(usuarioDAO.findByCedula(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.getUsuarioByCedula(99999999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");

        verify(usuarioDAO, times(1)).findByCedula(99999999L);
    }

    @Test
    @DisplayName("READ EMAIL - Email existente y activo debe retornar UsuarioDTO")
    void getUsuarioByEmail_ActiveEmail_ShouldReturnUsuario() {
        // Arrange
        when(usuarioDAO.findByEmailAndActive(validUsuarioDTO.getEmail()))
                .thenReturn(Optional.of(validUsuarioDTO));

        // Act
        UsuarioDTO result = usuarioService.getUsuarioByEmail(validUsuarioDTO.getEmail());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(validUsuarioDTO.getEmail());
        verify(usuarioDAO, times(1)).findByEmailAndActive(validUsuarioDTO.getEmail());
    }

    @Test
    @DisplayName("READ EMAIL - Email null debe lanzar IllegalArgumentException")
    void getUsuarioByEmail_NullEmail_ShouldThrowIllegalArgumentException() {
        // Arrange - no stub needed; validation occurs before DAO call

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.getUsuarioByEmail(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");

        verify(usuarioDAO, never()).findByEmailAndActive(any());
    }

    @Test
    @DisplayName("READ EMAIL - Usuario inactivo o inexistente debe lanzar RuntimeException con mensaje genérico")
    void getUsuarioByEmail_InactiveOrNotFound_ShouldThrowRuntimeException() {
        // Arrange
        when(usuarioDAO.findByEmailAndActive(anyString())).thenReturn(Optional.empty());

        // Act & Assert (mensaje genérico por seguridad — RF: no revelar si el email existe)
        assertThatThrownBy(() -> usuarioService.getUsuarioByEmail("noexiste@empresa.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credenciales");
    }

    @Test
    @DisplayName("READ ALL ACTIVOS - Debe retornar solo usuarios activos de la organización")
    void getUsuariosActivosByOrganizacion_ShouldReturnActiveList() {
        // Arrange
        List<UsuarioDTO> activos = Arrays.asList(validUsuarioDTO);
        when(organizacionService.getOrganizacionByNit(validOrganizacionNit)).thenReturn(null);
        when(usuarioDAO.findActivosByOrganizacionNit(validOrganizacionNit)).thenReturn(activos);

        // Act
        List<UsuarioDTO> result = usuarioService.getUsuariosActivosByOrganizacion(validOrganizacionNit);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
        verify(usuarioDAO, times(1)).findActivosByOrganizacionNit(validOrganizacionNit);
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("UPDATE - Nueva contraseña debe encriptarse antes de persistir")
    void updateUsuario_NewPassword_ShouldEncryptBeforePersisting() {
        // Arrange
        UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
        updateDTO.setNombre("Ana Torres Modificada");
        updateDTO.setPassword("NuevaPass99");

        String newHash = "$2a$10$nuevoHash";
        UsuarioDTO updated = new UsuarioDTO();
        updated.setCedula(validCedula);
        updated.setNombre("Ana Torres Modificada");

        when(usuarioDAO.findByCedula(validCedula)).thenReturn(Optional.of(validUsuarioDTO));
        when(passwordEncoder.encode("NuevaPass99")).thenReturn(newHash);
        when(usuarioDAO.update(eq(validCedula), any(UsuarioUpdateDTO.class)))
                .thenReturn(Optional.of(updated));

        // Act
        UsuarioDTO result = usuarioService.updateUsuario(validCedula, updateDTO);

        // Assert - la contraseña fue encriptada y enviada al DAO
        assertThat(result).isNotNull();
        ArgumentCaptor<UsuarioUpdateDTO> captor = ArgumentCaptor.forClass(UsuarioUpdateDTO.class);
        verify(passwordEncoder, times(1)).encode("NuevaPass99");
        verify(usuarioDAO, times(1)).update(eq(validCedula), captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo(newHash);
    }

    @Test
    @DisplayName("UPDATE - Sin nueva contraseña, passwordEncoder NO debe ser invocado")
    void updateUsuario_NoPassword_ShouldNotCallPasswordEncoder() {
        // Arrange
        UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
        updateDTO.setNombre("Ana Torres Modificada");
        // sin setPassword -> null

        UsuarioDTO updated = new UsuarioDTO();
        updated.setCedula(validCedula);

        when(usuarioDAO.findByCedula(validCedula)).thenReturn(Optional.of(validUsuarioDTO));
        when(usuarioDAO.update(eq(validCedula), any(UsuarioUpdateDTO.class)))
                .thenReturn(Optional.of(updated));

        // Act
        usuarioService.updateUsuario(validCedula, updateDTO);

        // Assert - passwordEncoder no debe ser llamado si no hay nueva contraseña
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("UPDATE - Cédula inexistente debe lanzar RuntimeException")
    void updateUsuario_NonExistentCedula_ShouldThrowRuntimeException() {
        // Arrange
        when(usuarioDAO.findByCedula(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.updateUsuario(99999999L, new UsuarioUpdateDTO()))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioDAO, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("UPDATE - Contraseña menor a 8 caracteres debe lanzar IllegalArgumentException")
    void updateUsuario_ShortPassword_ShouldThrowIllegalArgumentException() {
        // Arrange
        UsuarioUpdateDTO updateDTO = new UsuarioUpdateDTO();
        updateDTO.setPassword("corta");
        when(usuarioDAO.findByCedula(validCedula)).thenReturn(Optional.of(validUsuarioDTO));

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.updateUsuario(validCedula, updateDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 caracteres");

        verify(usuarioDAO, never()).update(anyLong(), any());
    }

    // ==================== ACTIVAR / INACTIVAR ====================

    @Test
    @DisplayName("ACTIVAR - Usuario inactivo debe activarse correctamente")
    void activarUsuario_InactiveUser_ShouldActivateSuccessfully() {
        // Arrange
        UsuarioDTO inactiveUser = new UsuarioDTO();
        inactiveUser.setCedula(validCedula);
        inactiveUser.setActive(false);

        UsuarioDTO activatedUser = new UsuarioDTO();
        activatedUser.setCedula(validCedula);
        activatedUser.setActive(true);

        when(usuarioDAO.findByCedula(validCedula)).thenReturn(Optional.of(inactiveUser));
        when(usuarioDAO.update(eq(validCedula), any(UsuarioUpdateDTO.class)))
                .thenReturn(Optional.of(activatedUser));

        // Act
        UsuarioDTO result = usuarioService.activarUsuario(validCedula);

        // Assert
        assertThat(result.getActive()).isTrue();
        ArgumentCaptor<UsuarioUpdateDTO> captor = ArgumentCaptor.forClass(UsuarioUpdateDTO.class);
        verify(usuarioDAO, times(1)).update(eq(validCedula), captor.capture());
        assertThat(captor.getValue().getActive()).isTrue();
    }

    @Test
    @DisplayName("ACTIVAR - Usuario ya activo debe lanzar IllegalStateException")
    void activarUsuario_AlreadyActive_ShouldThrowIllegalStateException() {
        // Arrange - usuario ya tiene active=true
        when(usuarioDAO.findByCedula(validCedula)).thenReturn(Optional.of(validUsuarioDTO));

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.activarUsuario(validCedula))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya se encuentra activo");

        verify(usuarioDAO, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("INACTIVAR - Usuario activo debe inactivarse correctamente")
    void inactivarUsuario_ActiveUser_ShouldInactivateSuccessfully() {
        // Arrange - validUsuarioDTO tiene active=true
        UsuarioDTO inactivatedUser = new UsuarioDTO();
        inactivatedUser.setCedula(validCedula);
        inactivatedUser.setActive(false);

        when(usuarioDAO.findByCedula(validCedula)).thenReturn(Optional.of(validUsuarioDTO));
        when(usuarioDAO.update(eq(validCedula), any(UsuarioUpdateDTO.class)))
                .thenReturn(Optional.of(inactivatedUser));

        // Act
        UsuarioDTO result = usuarioService.inactivarUsuario(validCedula);

        // Assert
        assertThat(result.getActive()).isFalse();
        ArgumentCaptor<UsuarioUpdateDTO> captor = ArgumentCaptor.forClass(UsuarioUpdateDTO.class);
        verify(usuarioDAO, times(1)).update(eq(validCedula), captor.capture());
        assertThat(captor.getValue().getActive()).isFalse();
    }

    @Test
    @DisplayName("INACTIVAR - Usuario ya inactivo debe lanzar IllegalStateException")
    void inactivarUsuario_AlreadyInactive_ShouldThrowIllegalStateException() {
        // Arrange
        UsuarioDTO inactiveUser = new UsuarioDTO();
        inactiveUser.setCedula(validCedula);
        inactiveUser.setActive(false);
        when(usuarioDAO.findByCedula(validCedula)).thenReturn(Optional.of(inactiveUser));

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.inactivarUsuario(validCedula))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya se encuentra inactivo");

        verify(usuarioDAO, never()).update(anyLong(), any());
    }

    // ==================== DELETE ====================

    @Test
    @DisplayName("DELETE - Cédula existente debe completar sin excepción")
    void deleteUsuario_ExistingCedula_ShouldCompleteWithoutException() {
        // Arrange
        when(usuarioDAO.findByCedula(validCedula)).thenReturn(Optional.of(validUsuarioDTO));
        when(usuarioDAO.deleteByCedula(validCedula)).thenReturn(true);

        // Act & Assert
        assertThatCode(() -> usuarioService.deleteUsuario(validCedula))
                .doesNotThrowAnyException();

        verify(usuarioDAO, times(1)).findByCedula(validCedula);
        verify(usuarioDAO, times(1)).deleteByCedula(validCedula);
    }

    @Test
    @DisplayName("DELETE - Cédula inexistente debe lanzar RuntimeException")
    void deleteUsuario_NonExistentCedula_ShouldThrowRuntimeException() {
        // Arrange
        when(usuarioDAO.findByCedula(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.deleteUsuario(99999999L))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioDAO, never()).deleteByCedula(anyLong());
    }

    @Test
    @DisplayName("DELETE - DAO retorna false debe lanzar RuntimeException")
    void deleteUsuario_DaoReturnsFalse_ShouldThrowRuntimeException() {
        // Arrange
        when(usuarioDAO.findByCedula(validCedula)).thenReturn(Optional.of(validUsuarioDTO));
        when(usuarioDAO.deleteByCedula(validCedula)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.deleteUsuario(validCedula))
                .isInstanceOf(RuntimeException.class);

        verify(usuarioDAO, times(1)).deleteByCedula(validCedula);
    }
}
