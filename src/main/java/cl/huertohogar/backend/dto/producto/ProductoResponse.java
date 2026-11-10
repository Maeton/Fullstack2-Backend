package cl.huertohogar.backend.dto.producto;

import cl.huertohogar.backend.model.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        String categoria,
        String imagenUrl,
        boolean activo,
        Long vendedorId,
        String vendedorNombre,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {

    public static ProductoResponse from(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getCategoria(),
                producto.getImagenUrl(),
                producto.isActivo(),
                producto.getVendedor() != null ? producto.getVendedor().getId() : null,
                producto.getVendedor() != null ? producto.getVendedor().getNombre() : null,
                producto.getCreadoEn(),
                producto.getActualizadoEn()
        );
    }
}
