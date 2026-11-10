package cl.huertohogar.backend.controller;

import cl.huertohogar.backend.dto.usuario.UsuarioRequest;
import cl.huertohogar.backend.dto.usuario.UsuarioResponse;
import cl.huertohogar.backend.dto.usuario.UsuarioUpdateRequest;
import cl.huertohogar.backend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "CRUD de usuarios. Solo ADMIN")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Listar todos los usuarios")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usuario por id")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un usuario con rol ADMIN, VENDEDOR o CLIENTE")
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos, rol o estado de un usuario")
    public UsuarioResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar un usuario (borrado lógico)")
    public void desactivar(@PathVariable Long id) {
        usuarioService.desactivar(id);
    }
}
