package com.sistema.iTsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.sistema.iTsystem.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        System.err.println("CONFIGURANDO DaoAuthenticationProvider - INICIANDO APP");
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        System.err.println("DaoAuthenticationProvider CONFIGURADO CORRECTAMENTE");
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.err.println("CONFIGURANDO SECURITY FILTER CHAIN");
       
        http
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(authz -> authz
                // Recursos estáticos (CSS, JS, imágenes)
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                
                // Página de login
                .requestMatchers("/login").permitAll()
                
                // ==================== ENDPOINTS DE API PARA PRUEBAS ====================
                // Permitir acceso sin autenticación a la API de catálogos (para Postman)
                .requestMatchers("/api/catalogos/**").permitAll()
                
                // Endpoint de test (opcional, para navegador)
                .requestMatchers("/test").permitAll()
                
                // ==================== RESTO REQUIERE AUTENTICACIÓN ====================
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            // ==================== DESACTIVAR CSRF PARA API ====================
            // Solo para endpoints de API (necesario para Postman)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            );
       
        System.err.println("SECURITY FILTER CHAIN CONFIGURADO");
        return http.build();
    }
}