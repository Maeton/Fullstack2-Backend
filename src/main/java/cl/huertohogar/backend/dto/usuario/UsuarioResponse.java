package cl.huertohogar.backend.dto.usuario;

import cl.huertohogar.backend.model.Rol;
import cl.huertohogar.backend.model.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        String direccion,
        Rol rol,
        boolean activo,
        LocalDateTime fechaRegistro
) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getDireccion(),
                usuario.getRol(),
                usuario.isActivo(),
                usuario.getFechaRegistro()
        );
    }
}
