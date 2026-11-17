package cl.huertohogar.backend.dto.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PedidoRequest(
        @NotEmpty(message = "El pedido debe tener al menos un producto")
        List<@Valid ItemPedidoRequest> items,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccionEntrega,

        @Size(max = 120, message = "El nombre de contacto no puede superar 120 caracteres")
        String nombreContacto,

        @Size(max = 30, message = "El teléfono de contacto no puede superar 30 caracteres")
        String telefonoContacto
) {
}
