package com.eam.proyecto.securityLayer;

import com.eam.proyecto.persistenceLayer.entity.RolUsuarioEntity;
import com.eam.proyecto.persistenceLayer.entity.UsuarioEntity;
import com.eam.proyecto.persistenceLayer.repository.RolUsuarioRepository;
import com.eam.proyecto.persistenceLayer.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final RolUsuarioRepository rolUsuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        List<RolUsuarioEntity> rolesUsuario = rolUsuarioRepository.findByUsuario(usuario);
        
        List<GrantedAuthority> authorities = rolesUsuario.stream()
                .map(ru -> new SimpleGrantedAuthority("ROLE_" + ru.getRol().getNombre().toUpperCase()))
                .collect(Collectors.toList());

        return new CustomUserDetails(usuario, authorities);
    }
}
