package cl.huertohogar.backend.dto.producto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
        String nombre,

        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
        BigDecimal precio,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @Size(max = 80, message = "La categoría no puede superar 80 caracteres")
        String categoria,

        @Size(max = 500, message = "La URL de imagen no puede superar 500 caracteres")
        String imagenUrl
) {
}
