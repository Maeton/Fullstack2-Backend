package cl.huertohogar.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenAPI huertoHogarOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Huerto Hogar API")
                        .version("1.0.0")
                        .description("""
                                API REST del backend de Huerto Hogar.

                                **Roles disponibles:** ADMIN, VENDEDOR y CLIENTE.

                                Para probar los endpoints protegidos:
                                1. Ejecuta `POST /api/auth/login` con un usuario semilla.
                                2. Copia el `accessToken` de la respuesta.
                                3. Pulsa **Authorize** y pega el token.
                                """)
                        .contact(new Contact().name("Equipo Huerto Hogar")))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA,
                                new SecurityScheme()
                                        .name(ESQUEMA)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Escribe el access token JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
    }
}
