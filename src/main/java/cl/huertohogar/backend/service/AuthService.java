package cl.huertohogar.backend.service;

import cl.huertohogar.backend.model.RefreshToken;
import cl.huertohogar.backend.model.Rol;
import cl.huertohogar.backend.model.Usuario;
import cl.huertohogar.backend.dto.auth.AuthResponse;
import cl.huertohogar.backend.dto.auth.LoginRequest;
import cl.huertohogar.backend.dto.auth.RegisterRequest;
import cl.huertohogar.backend.dto.usuario.UsuarioResponse;
import cl.huertohogar.backend.exception.ConflictoException;
import cl.huertohogar.backend.exception.TokenInvalidoException;
import cl.huertohogar.backend.repository.RefreshTokenRepository;
import cl.huertohogar.backend.repository.UsuarioRepository;
import cl.huertohogar.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public AuthService(UsuarioRepository usuarioRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        String email = normalizarEmail(request.email());

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("Ya existe una cuenta registrada con ese correo");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setTelefono(request.telefono());
        usuario.setDireccion(request.direccion());
        // El registro público siempre crea CLIENTE.
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(true);

        usuarioRepository.save(usuario);
        return generarRespuesta(usuario);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizarEmail(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            throw new TokenInvalidoException("Correo o contraseña incorrectos");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new TokenInvalidoException("Correo o contraseña incorrectos"));

        if (!usuario.isActivo()) {
            throw new TokenInvalidoException("La cuenta está desactivada");
        }

        return generarRespuesta(usuario);
    }

    @Transactional
    public AuthResponse refrescar(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenInvalidoException("Refresh token inválido"));

        if (refreshToken.isRevocado()) {
            throw new TokenInvalidoException("El refresh token ya fue utilizado");
        }

        if (refreshToken.getExpiraEn().isBefore(Instant.now())) {
            throw new TokenInvalidoException("El refresh token expiró, inicia sesión nuevamente");
        }

        Usuario usuario = refreshToken.getUsuario();
        if (!usuario.isActivo()) {
            throw new TokenInvalidoException("La cuenta está desactivada");
        }

        // Rotación: el token usado se revoca y se emite uno nuevo.
        refreshToken.setRevocado(true);
        refreshTokenRepository.save(refreshToken);

        return generarRespuesta(usuario);
    }

    @Transactional
    public void cerrarSesion(String email) {
        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new TokenInvalidoException("Sesión no válida"));

        refreshTokenRepository.deleteByUsuarioId(usuario.getId());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuarioActual(String email) {
        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new TokenInvalidoException("Sesión no válida"));
        return UsuarioResponse.from(usuario);
    }

    private AuthResponse generarRespuesta(Usuario usuario) {
        String accessToken = jwtService.generarAccessToken(usuario);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUsuario(usuario);
        refreshToken.setExpiraEn(Instant.now().plusMillis(refreshExpirationMs));
        refreshToken.setRevocado(false);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                jwtService.getExpirationMs() / 1000,
                UsuarioResponse.from(usuario)
        );
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
