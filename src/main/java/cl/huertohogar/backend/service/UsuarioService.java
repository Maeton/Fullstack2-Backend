package cl.huertohogar.backend.service;

import cl.huertohogar.backend.model.Usuario;
import cl.huertohogar.backend.dto.usuario.UsuarioRequest;
import cl.huertohogar.backend.dto.usuario.UsuarioResponse;
import cl.huertohogar.backend.dto.usuario.UsuarioUpdateRequest;
import cl.huertohogar.backend.exception.ConflictoException;
import cl.huertohogar.backend.exception.RecursoNoEncontradoException;
import cl.huertohogar.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAllByOrderByFechaRegistroDesc()
                .stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscarEntidad(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String email = normalizarEmail(request.email());

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("Ya existe un usuario con ese correo");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setTelefono(request.telefono());
        usuario.setDireccion(request.direccion());
        usuario.setRol(request.rol());
        usuario.setActivo(true);

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarEntidad(id);

        if (request.nombre() != null && !request.nombre().isBlank()) {
            usuario.setNombre(request.nombre().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String email = normalizarEmail(request.email());
            if (!email.equals(usuario.getEmail()) && usuarioRepository.existsByEmail(email)) {
                throw new ConflictoException("Ya existe un usuario con ese correo");
            }
            usuario.setEmail(email);
        }

        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.telefono() != null) {
            usuario.setTelefono(request.telefono());
        }

        if (request.direccion() != null) {
            usuario.setDireccion(request.direccion());
        }

        if (request.rol() != null) {
            usuario.setRol(request.rol());
        }

        if (request.activo() != null) {
            usuario.setActivo(request.activo());
        }

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = buscarEntidad(id);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(normalizarEmail(email))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private Usuario buscarEntidad(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
