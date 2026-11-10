package cl.huertohogar.backend.controller;

import cl.huertohogar.backend.dto.auth.AuthResponse;
import cl.huertohogar.backend.dto.auth.LoginRequest;
import cl.huertohogar.backend.dto.auth.RefreshRequest;
import cl.huertohogar.backend.dto.auth.RegisterRequest;
import cl.huertohogar.backend.dto.usuario.UsuarioResponse;
import cl.huertohogar.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro, login, refresh y sesión actual")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Registrar un cliente",
            description = "Crea una cuenta con rol CLIENTE. Los roles ADMIN y VENDEDOR solo los asigna un administrador.")
    public AuthResponse registrar(@Valid @RequestBody RegisterRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión y obtener access + refresh token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar el access token usando el refresh token")
    public AuthResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        return authService.refrescar(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cerrar sesión e invalidar los refresh tokens del usuario")
    public void cerrarSesion(Authentication authentication) {
        authService.cerrarSesion(authentication.getName());
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener el usuario autenticado")
    public ResponseEntity<UsuarioResponse> usuarioActual(Authentication authentication) {
        return ResponseEntity.ok(authService.usuarioActual(authentication.getName()));
    }
}
