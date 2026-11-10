package cl.huertohogar.backend.dto.auth;

import cl.huertohogar.backend.dto.usuario.UsuarioResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UsuarioResponse usuario
) {
}
