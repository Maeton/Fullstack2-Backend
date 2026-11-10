package cl.huertohogar.backend.dto.pedido;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad
) {
}
