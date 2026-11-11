# Backend Spring Boot

## Pruebas

Levanta el backend y ejecuta las verificaciones desde la raíz del proyecto:

- `.\mvnw.cmd test`: ejecuta los tests de contexto con JUnit.
- `.\scripts\smoke-test.ps1`: ejecuta las 21 verificaciones de humo.
- `.\scripts\endpoints-test.ps1`: ejecuta las 50 verificaciones de endpoints.

```powershell
.\mvnw.cmd -DskipTests package
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1

powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\endpoints-test.ps1
```

Las pruebas usan H2 en memoria (`application-test.properties`) y JWT de prueba, sin tocar la base de desarrollo.

## Ejecutar

```powershell
.\mvnw.cmd -DskipTests package
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
```

- API: <http://localhost:8080/api>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Consola H2: <http://localhost:8080/h2-console> (`jdbc:h2:file:./data/huertohogar`, usuario `sa`, sin contraseña)

Detener:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-backend.ps1
```

## Usuarios semilla

| Rol | Email | Contraseña |
| --- | --- | --- |
| ADMIN | `admin@huerto.cl` | `Admin123!` |
| VENDEDOR | `vendedor@huerto.cl` | `Vendedor123!` |
| CLIENTE | `cliente@huerto.cl` | `Cliente123!` |

## Endpoints

| Método | Ruta | Acceso |
| --- | --- | --- |
| POST | `/api/auth/register` | Público (siempre CLIENTE) |
| POST | `/api/auth/login` | Público |
| POST | `/api/auth/refresh` | Público |
| POST | `/api/auth/logout` | Autenticado |
| GET | `/api/auth/me` | Autenticado |
| GET | `/api/productos` | Público |
| GET | `/api/productos/{id}` | Público |
| POST | `/api/productos` | VENDEDOR / ADMIN |
| PUT | `/api/productos/{id}` | VENDEDOR dueño / ADMIN |
| DELETE | `/api/productos/{id}` | VENDEDOR dueño / ADMIN |
| POST | `/api/pedidos` | CLIENTE |
| GET | `/api/pedidos/mis-pedidos` | CLIENTE |
| GET | `/api/pedidos` | ADMIN |
| GET | `/api/pedidos/{id}` | Dueño / ADMIN |
| PATCH | `/api/pedidos/{id}/estado` | VENDEDOR / ADMIN |
| GET | `/api/usuarios` | ADMIN |
| GET | `/api/usuarios/{id}` | ADMIN |
| POST | `/api/usuarios` | ADMIN |
| PUT | `/api/usuarios/{id}` | ADMIN |
| DELETE | `/api/usuarios/{id}` | ADMIN |

## Reglas de negocio

- El registro público siempre asigna `CLIENTE`; los roles altos los crea un `ADMIN` o vienen en los datos semilla.
- El total y los subtotales del pedido se calculan en el servidor.
- El stock se descuenta de forma transaccional; si falta stock, la operación completa se revierte con `422`.
- Un vendedor solo edita sus propios productos y solo ve los pedidos que los contienen.
- Contraseñas con BCrypt, access token de 15 minutos y refresh token de 7 días con rotación.

## Integración con el frontend

El cliente React de la Evaluación Parcial 3 consume esta API:

- Frontend: <https://github.com/Maeton/Fullstack2>
- Origen permitido por CORS: `http://localhost:5173`
- El frontend adjunta el access token en `Authorization: Bearer ...`, renueva la sesión con `/api/auth/refresh` y controla rutas por rol.
- Las restricciones del frontend se refuerzan en el backend con `@PreAuthorize` (por ejemplo, CLIENTE recibe `403` al intentar administrar usuarios o productos ajenos).

## Estructura

```
src/main/java/cl/huertohogar/backend
├── config        Seguridad, OpenAPI y datos semilla
├── controller    Endpoints REST
├── dto           Request y response
├── exception     Manejo global de errores
├── model         Entidades JPA
├── repository    Repositorios Spring Data
├── security      JWT y UserDetails
└── service       Lógica de negocio
```

## Documentación

- [Spring Boot](https://docs.spring.io/spring-boot/index.html)
- [Spring Security](https://docs.spring.io/spring-security/reference/index.html)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/index.html)
- [Springdoc OpenAPI](https://springdoc.org/)
