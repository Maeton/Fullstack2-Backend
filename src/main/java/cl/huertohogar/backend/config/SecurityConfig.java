package cl.huertohogar.backend.config;

import cl.huertohogar.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers ->
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/h2-console/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                        .requestMatchers("/api/productos/**").hasAnyRole("ADMIN", "VENDEDOR")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/pedidos/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::escribirNoAutorizado)
                        .accessDeniedHandler(this::escribirProhibido))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:4173"
        ));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracion.setExposedHeaders(List.of("Authorization"));
        configuracion.setAllowCredentials(true);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuracion);
        return source;
    }

    private void escribirNoAutorizado(HttpServletRequest request, HttpServletResponse response,
                                      org.springframework.security.core.AuthenticationException ex)
            throws IOException {
        escribirError(request, response, HttpStatus.UNAUTHORIZED,
                "Debes iniciar sesión para acceder a este recurso");
    }

    private void escribirProhibido(HttpServletRequest request, HttpServletResponse response,
                                   org.springframework.security.access.AccessDeniedException ex)
            throws IOException {
        escribirError(request, response, HttpStatus.FORBIDDEN,
                "No tienes permisos para realizar esta acción");
    }

    private void escribirError(HttpServletRequest request, HttpServletResponse response,
                               HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String body = "{"
                + "\"timestamp\":\"" + java.time.LocalDateTime.now() + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + escaparJson(status.getReasonPhrase()) + "\","
                + "\"message\":\"" + escaparJson(mensaje) + "\","
                + "\"path\":\"" + escaparJson(request.getRequestURI()) + "\""
                + "}";
        response.getWriter().write(body);
    }

    private String escaparJson(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
