package cl.huertohogar.backend.dto.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PedidoRequest(
        @NotEmpty(message = "El pedido debe tener al menos un producto")
        List<@Valid ItemPedidoRequest> items,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccionEntrega
) {
}
