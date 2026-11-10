# Huerto Hogar — Backend

Backend REST de Huerto Hogar construido con **Spring Boot 4**, **Spring Data JPA**, **Spring Security + JWT**, **H2** y **Swagger/OpenAPI**.

## Requisitos

- Java 17
- No hace falta Maven instalado: se usa el wrapper `mvnw.cmd`

## Ejecutar

```powershell
.\mvnw.cmd -DskipTests package
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1
```

- API: <http://localhost:8080/api>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Consola H2: <http://localhost:8080/h2-console> (JDBC: `jdbc:h2:file:./data/huertohogar`, usuario `sa`, sin contraseña)

Detener:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-backend.ps1
```

## Pruebas

```powershell
.\mvnw.cmd test                             # tests de contexto
powershell -File .\scripts\smoke-test.ps1   # 21 verificaciones
powershell -File .\scripts\endpoints-test.ps1 # 50 verificaciones
```

## Usuarios semilla

| Rol | Email | Contraseña |
| --- | --- | --- |
| ADMIN | `admin@huerto.cl` | `Admin123!` |
| VENDEDOR | `vendedor@huerto.cl` | `Vendedor123!` |
| CLIENTE | `cliente@huerto.cl` | `Cliente123!` |

## Endpoints

### Autenticación — `/api/auth`

| Método | Ruta | Acceso |
| --- | --- | --- |
| POST | `/register` | Público (siempre crea CLIENTE) |
| POST | `/login` | Público |
| POST | `/refresh` | Público (rota el refresh token) |
| POST | `/logout` | Autenticado |
| GET | `/me` | Autenticado |

### Productos — `/api/productos`

| Método | Ruta | Acceso |
| --- | --- | --- |
| GET | `/` | Público |
| GET | `/{id}` | Público |
| POST | `/` | VENDEDOR / ADMIN |
| PUT | `/{id}` | VENDEDOR dueño / ADMIN |
| DELETE | `/{id}` | VENDEDOR dueño / ADMIN |

### Pedidos — `/api/pedidos`

| Método | Ruta | Acceso |
| --- | --- | --- |
| POST | `/` | CLIENTE |
| GET | `/mis-pedidos` | CLIENTE |
| GET | `/` | ADMIN |
| GET | `/{id}` | Dueño / ADMIN |
| PATCH | `/{id}/estado` | VENDEDOR / ADMIN |

### Usuarios — `/api/usuarios`

| Método | Ruta | Acceso |
| --- | --- | --- |
| GET | `/` | ADMIN |
| GET | `/{id}` | ADMIN |
| POST | `/` | ADMIN |
| PUT | `/{id}` | ADMIN |
| DELETE | `/{id}` | ADMIN |

## Reglas de negocio

- El registro público **siempre** asigna el rol `CLIENTE`; nunca acepta roles altos desde el navegador.
- El total del pedido y los subtotales se calculan en el servidor.
- El stock se descuenta de forma transaccional; si un producto no tiene stock, la operación completa se revierte con `422`.
- Un vendedor solo puede editar/eliminar sus propios productos y solo ve los pedidos que contienen sus productos.
- Contraseñas con BCrypt; access token de 15 minutos y refresh token de 7 días.
- Errores estandarizados: `400`, `401`, `403`, `404`, `409`, `422`, `500`.

## Base de datos

- Desarrollo/demo: H2 en archivo `./data/huertohogar`.
- Pruebas: H2 en memoria (`application-test.properties`).
- Datos semilla: 3 usuarios y 6 productos creados por `DataInitializer`.
