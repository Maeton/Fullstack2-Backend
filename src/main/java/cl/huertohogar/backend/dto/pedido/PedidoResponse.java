package cl.huertohogar.backend.dto.pedido;

import cl.huertohogar.backend.model.DetallePedido;
import cl.huertohogar.backend.model.EstadoPedido;
import cl.huertohogar.backend.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        LocalDateTime fecha,
        EstadoPedido estado,
        BigDecimal total,
        String direccionEntrega,
        Long clienteId,
        String clienteNombre,
        String clienteEmail,
        List<ItemPedidoResponse> items
) {

    public static PedidoResponse from(Pedido pedido) {
        List<ItemPedidoResponse> items = pedido.getDetalles().stream()
                .map(PedidoResponse::toItem)
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getFecha(),
                pedido.getEstado(),
                pedido.getTotal(),
                pedido.getDireccionEntrega(),
                pedido.getCliente().getId(),
                pedido.getCliente().getNombre(),
                pedido.getCliente().getEmail(),
                items
        );
    }

    private static ItemPedidoResponse toItem(DetallePedido detalle) {
        return new ItemPedidoResponse(
                detalle.getProducto().getId(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal()
        );
    }
}
