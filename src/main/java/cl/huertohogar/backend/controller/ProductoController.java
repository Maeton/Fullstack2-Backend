package cl.huertohogar.backend.controller;

import cl.huertohogar.backend.dto.producto.ProductoRequest;
import cl.huertohogar.backend.dto.producto.ProductoResponse;
import cl.huertohogar.backend.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Catálogo público y CRUD para ADMIN/VENDEDOR")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @Operation(summary = "Listar los productos activos (público)")
    public List<ProductoResponse> listar() {
        return productoService.listarActivos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto activo por id (público)")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear un producto")
    public ProductoResponse crear(Authentication authentication,
                                  @Valid @RequestBody ProductoRequest request) {
        return productoService.crear(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar un producto (ADMIN o vendedor dueño)")
    public ProductoResponse actualizar(@PathVariable Long id,
                                       Authentication authentication,
                                       @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(
                id, authentication.getName(), esAdmin(authentication), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Desactivar un producto (ADMIN o vendedor dueño)")
    public void desactivar(@PathVariable Long id, Authentication authentication) {
        productoService.desactivar(id, authentication.getName(), esAdmin(authentication));
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
