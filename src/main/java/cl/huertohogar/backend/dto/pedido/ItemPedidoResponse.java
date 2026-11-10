package cl.huertohogar.backend.dto.pedido;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        Long productoId,
        String nombre,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
