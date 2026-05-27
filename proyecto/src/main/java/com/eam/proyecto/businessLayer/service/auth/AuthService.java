package com.eam.proyecto.businessLayer.service.auth;

import com.eam.proyecto.persistenceLayer.entity.OrganizacionEntity;
import com.eam.proyecto.persistenceLayer.entity.RolEntity;
import com.eam.proyecto.persistenceLayer.entity.RolUsuarioEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import com.eam.proyecto.persistenceLayer.repository.OrganizacionRepository;
import com.eam.proyecto.persistenceLayer.repository.RolRepository;
import com.eam.proyecto.persistenceLayer.repository.RolUsuarioRepository;
import com.eam.proyecto.persistenceLayer.repository.UsuarioRepository;
import com.eam.proyecto.securityLayer.CustomUserDetails;
import com.eam.proyecto.securityLayer.dto.AuthRequests.LoginRequest;
import com.eam.proyecto.securityLayer.dto.AuthRequests.RegisterRequest;
import com.eam.proyecto.securityLayer.dto.AuthResponses.JwtResponse;
import com.eam.proyecto.securityLayer.dto.AuthResponses.UserDto;
import com.eam.proyecto.securityLayer.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacionRepository organizacionRepository;
    private final RolRepository rolRepository;
    private final RolUsuarioRepository rolUsuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public JwtResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return generateJwtResponse(userDetails);
    }

    public JwtResponse register(RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("El correo ya esta registrado");
        }
        if (usuarioRepository.findById(request.getCedula()).isPresent()) {
            throw new RuntimeException("La cedula ya esta registrada");
        }

        Long nit = request.getOrganizacionNit() != null ? request.getOrganizacionNit() : 1L;
        OrganizacionEntity organizacion = organizacionRepository.findById(nit)
                .orElseGet(() -> {
                    OrganizacionEntity newOrg = new OrganizacionEntity();
                    newOrg.setNit(nit);
                    newOrg.setNombre(valueOrDefault(request.getOrganizacionNombre(), "Organizacion " + nit));
                    newOrg.setEmail(valueOrDefault(request.getOrganizacionEmail(), request.getEmail()));
                    newOrg.setTelefono(request.getOrganizacionTelefono());
                    newOrg.setDirCalle(request.getOrganizacionDirCalle());
                    newOrg.setDirNumero(request.getOrganizacionDirNumero());
                    newOrg.setDirComuna(request.getOrganizacionDirComuna());
                    newOrg.setActive(true);
                    newOrg.setCreadoEn(LocalDateTime.now());
                    return organizacionRepository.save(newOrg);
                });

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setCedula(request.getCedula());
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setActive(true);
        usuario.setOrganizacion(organizacion);
        usuario.setCreadoEn(LocalDateTime.now());

        usuarioRepository.save(usuario);

        String roleName = "USER";
        RolEntity rolUser = rolRepository.findByNombre(roleName)
                .orElseGet(() -> {
                    RolEntity newRol = new RolEntity();
                    newRol.setNombre(roleName);
                    newRol.setDescripcion("Usuario Estandar");
                    return rolRepository.save(newRol);
                });

        RolUsuarioEntity rolUsuario = new RolUsuarioEntity();
        rolUsuario.setUsuario(usuario);
        rolUsuario.setRol(rolUser);
        rolUsuarioRepository.save(rolUsuario);

        CustomUserDetails userDetails = new CustomUserDetails(usuario, java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + roleName)
        ));

        return generateJwtResponse(userDetails);
    }

    private JwtResponse generateJwtResponse(CustomUserDetails userDetails) {
        String jwt = jwtService.generateToken(userDetails);

        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER")
                .replace("ROLE_", "")
                .toLowerCase();

        UserDto userDto = UserDto.builder()
                .id(String.valueOf(userDetails.getUsuario().getCedula()))
                .name(userDetails.getUsuario().getNombre())
                .email(userDetails.getUsuario().getEmail())
                .role(role)
                .organizacionNit(userDetails.getUsuario().getOrganizacion().getNit())
                .organizacionNombre(userDetails.getUsuario().getOrganizacion().getNombre())
                .build();

        return JwtResponse.builder()
                .token(jwt)
                .user(userDto)
                .build();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value != null && !value.trim().isEmpty() ? value.trim() : defaultValue;
    }
}
