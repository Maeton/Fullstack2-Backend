package cl.huertohogar.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Ingresa un correo válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 100, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Usa el formato +56912345678")
        String telefono,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(min = 5, max = 255, message = "La dirección debe tener al menos 5 caracteres")
        String direccion
) {
}
