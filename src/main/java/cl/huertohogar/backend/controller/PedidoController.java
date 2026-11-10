package cl.huertohogar.backend.controller;

import cl.huertohogar.backend.dto.pedido.EstadoPedidoRequest;
import cl.huertohogar.backend.dto.pedido.PedidoRequest;
import cl.huertohogar.backend.dto.pedido.PedidoResponse;
import cl.huertohogar.backend.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pedidos", description = "Carrito, historial y gestión de pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Crear un pedido desde el carrito (CLIENTE)")
    public PedidoResponse crear(Authentication authentication,
                                @Valid @RequestBody PedidoRequest request) {
        return pedidoService.crear(authentication.getName(), request);
    }

    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Listar los pedidos del cliente autenticado")
    public List<PedidoResponse> misPedidos(Authentication authentication) {
        return pedidoService.misPedidos(authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todos los pedidos (ADMIN)")
    public List<PedidoResponse> listarTodos() {
        return pedidoService.listarTodos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un pedido (dueño o ADMIN)")
    public PedidoResponse obtener(@PathVariable Long id, Authentication authentication) {
        return pedidoService.obtener(id, authentication.getName(), esAdmin(authentication));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @Operation(summary = "Cambiar el estado de un pedido (ADMIN o VENDEDOR)")
    public PedidoResponse cambiarEstado(@PathVariable Long id,
                                        @Valid @RequestBody EstadoPedidoRequest request) {
        return pedidoService.cambiarEstado(id, request.estado());
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
