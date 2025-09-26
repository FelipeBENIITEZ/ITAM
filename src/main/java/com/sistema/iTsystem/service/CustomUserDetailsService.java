package com.sistema.iTsystem.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Constructor con log
    public CustomUserDetailsService() {
        System.err.println("CUSTOM USER DETAILS SERVICE CREADO");
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        System.err.println("================================");
        System.err.println("DEBUGING: BUSCANDO USUARIO: " + login);
        System.err.println("================================");
        
        // Buscar usuario por login
        Usuario usuario = usuarioRepository.findByUsuLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + login));

        System.err.println("Usuario encontrado: " + usuario.getUsuLogin());
        //System.err.println("Password hash: " + usuario.getUsuPassword());
        System.err.println("Rol: " + (usuario.getRol() != null ? usuario.getRol().getRolNom() : "null"));

        // Determinar el rol
        String roleName = "USER"; // por defecto
        if (usuario.getRol() != null && usuario.getRol().getRolNom() != null) {
            roleName = usuario.getRol().getRolNom()
                .replace(" ", "_")
                .replaceAll("[^a-zA-Z0-9_]", "")
                .toUpperCase();
        }

        System.err.println("Rol procesado: ROLE_" + roleName);

        // Retornar UserDetails
        return User.builder()
                .username(usuario.getUsuLogin())          
                .password(usuario.getUsuPassword())       
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + roleName)))
                .build();
    }
}