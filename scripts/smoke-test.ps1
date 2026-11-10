# Smoke test del backend Huerto Hogar.
# Uso:  powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
# Requiere el backend corriendo en http://localhost:8080

$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"
$fallos = 0

function Llamar {
    param(
        [string]$Metodo,
        [string]$Ruta,
        $Body = $null,
        [string]$Token = $null
    )

    $headers = @{ "Accept" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }

    $params = @{
        Uri                = "$base$Ruta"
        Method             = $Metodo
        Headers            = $headers
        UseBasicParsing    = $true
        TimeoutSec         = 20
    }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        $respuesta = Invoke-WebRequest @params
        return [pscustomobject]@{
            Status = [int]$respuesta.StatusCode
            Body   = $respuesta.Content
        }
    } catch {
        $status = 0
        $contenido = $_.ErrorDetails.Message

        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $texto = $reader.ReadToEnd()
                if ($texto) { $contenido = $texto }
            } catch {
                # Si no se puede leer el cuerpo se usa el mensaje de la excepcion.
            }
        }

        if (-not $contenido) {
            $contenido = $_.Exception.Message
        }

        return [pscustomobject]@{
            Status = $status
            Body   = $contenido
        }
    }
}

function Verificar {
    param([string]$Nombre, [bool]$Condicion, [string]$Detalle = "")
    if ($Condicion) {
        Write-Host ("[OK]   " + $Nombre) -ForegroundColor Green
    } else {
        Write-Host ("[FALLA] " + $Nombre + " " + $Detalle) -ForegroundColor Red
        $script:fallos++
    }
}

Write-Host "Esperando al backend y a los datos semilla..." -ForegroundColor Cyan
$arriba = $false
for ($i = 0; $i -lt 60; $i++) {
    $ping = Llamar -Metodo GET -Ruta "/api/productos"
    if ($ping.Status -eq 200) {
        $semilla = $ping.Body | ConvertFrom-Json
        if ($semilla.Count -ge 1) { $arriba = $true; break }
    }
    Start-Sleep -Seconds 1
}
if (-not $arriba) {
    Write-Host "El backend no respondio en http://localhost:8080" -ForegroundColor Red
    exit 1
}

# 1. Catalogo publico
$productos = (Llamar -Metodo GET -Ruta "/api/productos").Body | ConvertFrom-Json
Verificar "GET /api/productos publico responde 200" ($productos.Count -ge 6) "count=$($productos.Count)"
$productoId = $productos[0].id

# 2. Sin token -> 401
$sinToken = Llamar -Metodo GET -Ruta "/api/pedidos/mis-pedidos"
Verificar "GET /api/pedidos/mis-pedidos sin token -> 401" ($sinToken.Status -eq 401) "status=$($sinToken.Status)"

# 3. Login CLIENTE
$loginCliente = Llamar -Metodo POST -Ruta "/api/auth/login" -Body @{
    email    = "cliente@huerto.cl"
    password = "Cliente123!"
}
$datosCliente = $loginCliente.Body | ConvertFrom-Json
Verificar "Login CLIENTE -> 200 y token" ($loginCliente.Status -eq 200 -and $datosCliente.accessToken)
Verificar "Login CLIENTE entrega rol CLIENTE" ($datosCliente.usuario.rol -eq "CLIENTE") "rol=$($datosCliente.usuario.rol)"

# 4. /me
$me = Llamar -Metodo GET -Ruta "/api/auth/me" -Token $datosCliente.accessToken
Verificar "GET /api/auth/me con token -> 200" ($me.Status -eq 200) "status=$($me.Status)"

# 5. CLIENTE no puede listar usuarios -> 403
$usuariosCliente = Llamar -Metodo GET -Ruta "/api/usuarios" -Token $datosCliente.accessToken
Verificar "CLIENTE en /api/usuarios -> 403" ($usuariosCliente.Status -eq 403) "status=$($usuariosCliente.Status)"

# 6. Login ADMIN
$loginAdmin = Llamar -Metodo POST -Ruta "/api/auth/login" -Body @{
    email    = "admin@huerto.cl"
    password = "Admin123!"
}
$datosAdmin = $loginAdmin.Body | ConvertFrom-Json
Verificar "Login ADMIN -> 200" ($loginAdmin.Status -eq 200)

$usuarios = (Llamar -Metodo GET -Ruta "/api/usuarios" -Token $datosAdmin.accessToken).Body | ConvertFrom-Json
Verificar "ADMIN lista usuarios (>= 3)" ($usuarios.Count -ge 3) "count=$($usuarios.Count)"

# 7. ADMIN crea producto
$nuevoProducto = Llamar -Metodo POST -Ruta "/api/productos" -Token $datosAdmin.accessToken -Body @{
    nombre      = "Producto Smoke Test"
    descripcion = "Creado por el smoke test"
    precio      = 1990
    stock       = 10
    categoria   = "Frutas"
    imagenUrl   = "mango.jpg"
}
Verificar "ADMIN crea producto -> 201" ($nuevoProducto.Status -eq 201) "status=$($nuevoProducto.Status)"
$productoSmoke = $nuevoProducto.Body | ConvertFrom-Json

# 8. CLIENTE crea pedido
$pedido = Llamar -Metodo POST -Ruta "/api/pedidos" -Token $datosCliente.accessToken -Body @{
    items = @(
        @{ productoId = $productoId; cantidad = 2 }
    )
    direccionEntrega = "Av. Siempre Viva 742"
}
$datosPedido = $pedido.Body | ConvertFrom-Json
Verificar "CLIENTE crea pedido -> 201" ($pedido.Status -eq 201) "status=$($pedido.Status)"
Verificar "El total se calcula en el servidor" ($datosPedido.total -gt 0) "total=$($datosPedido.total)"

# 9. Historial del cliente
$misPedidos = (Llamar -Metodo GET -Ruta "/api/pedidos/mis-pedidos" -Token $datosCliente.accessToken).Body | ConvertFrom-Json
Verificar "CLIENTE ve su historial" ($misPedidos.Count -ge 1) "count=$($misPedidos.Count)"

# 9b. Restricciones del VENDEDOR
$loginVendedor = Llamar -Metodo POST -Ruta "/api/auth/login" -Body @{
    email    = "vendedor@huerto.cl"
    password = "Vendedor123!"
}
$datosVendedor = $loginVendedor.Body | ConvertFrom-Json
Verificar "Login VENDEDOR -> 200" ($loginVendedor.Status -eq 200)

$pedidoAjeno = Llamar -Metodo GET -Ruta "/api/pedidos/$($datosPedido.id)" -Token $datosVendedor.accessToken
Verificar "VENDEDOR no ve un pedido ajeno -> 403" ($pedidoAjeno.Status -eq 403) "status=$($pedidoAjeno.Status)"

$todosPedidosVendedor = Llamar -Metodo GET -Ruta "/api/pedidos" -Token $datosVendedor.accessToken
Verificar "VENDEDOR no lista todos los pedidos -> 403" ($todosPedidosVendedor.Status -eq 403) "status=$($todosPedidosVendedor.Status)"

$editarAjeno = Llamar -Metodo PUT -Ruta "/api/productos/$($productoSmoke.id)" -Token $datosVendedor.accessToken -Body @{
    nombre    = "Intento ajeno"
    precio    = 100
    stock     = 1
    categoria = "Frutas"
}
Verificar "VENDEDOR no edita un producto ajeno -> 403" ($editarAjeno.Status -eq 403) "status=$($editarAjeno.Status)"

# 10. Refresh token
$refresh = Llamar -Metodo POST -Ruta "/api/auth/refresh" -Body @{
    refreshToken = $datosCliente.refreshToken
}
$datosRefresh = $refresh.Body | ConvertFrom-Json
Verificar "Refresh token entrega nuevo access" ($refresh.Status -eq 200 -and $datosRefresh.accessToken) "status=$($refresh.Status)"
Verificar "El refresh token rota" ($datosRefresh.refreshToken -ne $datosCliente.refreshToken)

# 11. Swagger/OpenAPI
$openapi = Llamar -Metodo GET -Ruta "/v3/api-docs"
Verificar "OpenAPI responde 200" ($openapi.Status -eq 200) "status=$($openapi.Status)"

# 12. Registro publico fuerza CLIENTE (aunque se envie rol)
$emailNuevo = "smoke" + (Get-Random -Maximum 99999) + "@huerto.cl"
$registro = Llamar -Metodo POST -Ruta "/api/auth/register" -Body @{
    nombre    = "Cliente Smoke"
    email     = $emailNuevo
    password  = "Cliente123!"
    telefono  = "+56944444444"
    direccion = "Calle Smoke 123"
    rol       = "ADMIN"
}
$datosRegistro = $registro.Body | ConvertFrom-Json
Verificar "Registro publico -> 201" ($registro.Status -eq 201) "status=$($registro.Status)"
Verificar "Registro publico NO permite ADMIN" ($datosRegistro.usuario.rol -eq "CLIENTE") "rol=$($datosRegistro.usuario.rol)"

# Limpieza: desactivar los productos creados por el smoke test
$catalogo = (Llamar -Metodo GET -Ruta "/api/productos").Body | ConvertFrom-Json
$catalogo | Where-Object { $_.nombre -eq "Producto Smoke Test" } | ForEach-Object {
    Llamar -Metodo DELETE -Ruta "/api/productos/$($_.id)" -Token $datosAdmin.accessToken | Out-Null
}

Write-Host ""
if ($fallos -eq 0) {
    Write-Host "TODAS LAS PRUEBAS PASARON" -ForegroundColor Green
    exit 0
} else {
    Write-Host "$fallos PRUEBA(S) FALLARON" -ForegroundColor Red
    exit 1
}
