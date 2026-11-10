# Prueba completa de endpoints del backend Huerto Hogar.
# Uso: powershell -ExecutionPolicy Bypass -File .\scripts\endpoints-test.ps1
# Requiere el backend corriendo en http://localhost:8080

$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"
$script:fallos = 0
$script:ok = 0

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
        Uri             = "$base$Ruta"
        Method          = $Metodo
        Headers         = $headers
        UseBasicParsing = $true
        TimeoutSec      = 20
    }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        $respuesta = Invoke-WebRequest @params
        return [pscustomobject]@{ Status = [int]$respuesta.StatusCode; Body = $respuesta.Content }
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
            } catch { }
        }
        if (-not $contenido) { $contenido = $_.Exception.Message }
        return [pscustomobject]@{ Status = $status; Body = $contenido }
    }
}

function Verificar {
    param([string]$Nombre, [bool]$Condicion, [string]$Detalle = "")
    if ($Condicion) {
        Write-Host ("[OK]    " + $Nombre) -ForegroundColor Green
        $script:ok++
    } else {
        Write-Host ("[FALLA] " + $Nombre + " " + $Detalle) -ForegroundColor Red
        $script:fallos++
    }
}

function Login {
    param([string]$Email, [string]$Password)
    $r = Llamar -Metodo POST -Ruta "/api/auth/login" -Body @{ email = $Email; password = $Password }
    if ($r.Status -ne 200) { throw "No se pudo iniciar sesion con $Email ($($r.Status)): $($r.Body)" }
    return ($r.Body | ConvertFrom-Json)
}

Write-Host "Esperando al backend..." -ForegroundColor Cyan
$arriba = $false
for ($i = 0; $i -lt 60; $i++) {
    $ping = Llamar -Metodo GET -Ruta "/api/productos"
    if ($ping.Status -eq 200) {
        $semilla = $ping.Body | ConvertFrom-Json
        if ($semilla.Count -ge 1) { $arriba = $true; break }
    }
    Start-Sleep -Seconds 1
}
if (-not $arriba) { Write-Host "Backend no disponible" -ForegroundColor Red; exit 1 }

$admin = Login "admin@huerto.cl" "Admin123!"
$vendedor = Login "vendedor@huerto.cl" "Vendedor123!"
$cliente = Login "cliente@huerto.cl" "Cliente123!"
$sufijo = Get-Random -Maximum 999999

Write-Host "`n=== AUTENTICACION ===" -ForegroundColor Cyan

# POST /api/auth/register
$emailNuevo = "nuevo$sufijo@huerto.cl"
$registro = Llamar -Metodo POST -Ruta "/api/auth/register" -Body @{
    nombre = "Usuario Nuevo"; email = $emailNuevo; password = "Nuevo123!"
    telefono = "+56955555555"; direccion = "Calle Nueva 123"; rol = "ADMIN"
}
$datosRegistro = $registro.Body | ConvertFrom-Json
Verificar "POST /api/auth/register -> 201" ($registro.Status -eq 201) "status=$($registro.Status)"
Verificar "POST /api/auth/register fuerza CLIENTE" ($datosRegistro.usuario.rol -eq "CLIENTE") "rol=$($datosRegistro.usuario.rol)"

# POST /api/auth/login
Verificar "POST /api/auth/login -> 200" ($null -ne $cliente.accessToken) ""
Verificar "POST /api/auth/login credenciales malas -> 401" ((Llamar -Metodo POST -Ruta "/api/auth/login" -Body @{ email = "cliente@huerto.cl"; password = "mala" }).Status -eq 401) ""

# GET /api/auth/me
$me = Llamar -Metodo GET -Ruta "/api/auth/me" -Token $cliente.accessToken
Verificar "GET /api/auth/me -> 200" ($me.Status -eq 200) "status=$($me.Status)"
Verificar "GET /api/auth/me sin token -> 401" ((Llamar -Metodo GET -Ruta "/api/auth/me").Status -eq 401) ""

# POST /api/auth/refresh
$refresh = Llamar -Metodo POST -Ruta "/api/auth/refresh" -Body @{ refreshToken = $cliente.refreshToken }
Verificar "POST /api/auth/refresh -> 200" ($refresh.Status -eq 200) "status=$($refresh.Status)"
$refreshDatos = $refresh.Body | ConvertFrom-Json
Verificar "POST /api/auth/refresh rota el token" ($refreshDatos.refreshToken -ne $cliente.refreshToken) ""

# POST /api/auth/logout (usuario nuevo, para no invalidar los tokens de prueba)
$nuevo = Login $emailNuevo "Nuevo123!"
$logout = Llamar -Metodo POST -Ruta "/api/auth/logout" -Token $nuevo.accessToken
Verificar "POST /api/auth/logout -> 204" ($logout.Status -eq 204) "status=$($logout.Status)"
$refreshLogout = Llamar -Metodo POST -Ruta "/api/auth/refresh" -Body @{ refreshToken = $nuevo.refreshToken }
Verificar "POST /api/auth/refresh tras logout -> 401" ($refreshLogout.Status -eq 401) "status=$($refreshLogout.Status)"

Write-Host "`n=== USUARIOS ===" -ForegroundColor Cyan

# GET /api/usuarios
$listaUsuarios = Llamar -Metodo GET -Ruta "/api/usuarios" -Token $admin.accessToken
Verificar "GET /api/usuarios (ADMIN) -> 200" ($listaUsuarios.Status -eq 200) "status=$($listaUsuarios.Status)"
Verificar "GET /api/usuarios (CLIENTE) -> 403" ((Llamar -Metodo GET -Ruta "/api/usuarios" -Token $cliente.accessToken).Status -eq 403) ""
Verificar "GET /api/usuarios sin token -> 401" ((Llamar -Metodo GET -Ruta "/api/usuarios").Status -eq 401) ""

# POST /api/usuarios
$emailVendedorNuevo = "vendedor$sufijo@huerto.cl"
$crearUsuario = Llamar -Metodo POST -Ruta "/api/usuarios" -Token $admin.accessToken -Body @{
    nombre = "Vendedor Temporal"; email = $emailVendedorNuevo; password = "Vend123!"
    telefono = "+56966666666"; direccion = "Calle Vendedor 456"; rol = "VENDEDOR"
}
$usuarioCreado = $crearUsuario.Body | ConvertFrom-Json
Verificar "POST /api/usuarios -> 201" ($crearUsuario.Status -eq 201) "status=$($crearUsuario.Status)"
Verificar "POST /api/usuarios devuelve rol VENDEDOR" ($usuarioCreado.rol -eq "VENDEDOR") "rol=$($usuarioCreado.rol)"

# GET /api/usuarios/{id}
$usuarioId = $usuarioCreado.id
$obtenerUsuario = Llamar -Metodo GET -Ruta "/api/usuarios/$usuarioId" -Token $admin.accessToken
Verificar "GET /api/usuarios/{id} -> 200" ($obtenerUsuario.Status -eq 200) "status=$($obtenerUsuario.Status)"
Verificar "GET /api/usuarios/{id inexistente} -> 404" ((Llamar -Metodo GET -Ruta "/api/usuarios/999999" -Token $admin.accessToken).Status -eq 404) ""

# PUT /api/usuarios/{id}
$actualizarUsuario = Llamar -Metodo PUT -Ruta "/api/usuarios/$usuarioId" -Token $admin.accessToken -Body @{
    nombre = "Vendedor Actualizado"; rol = "CLIENTE"; activo = $true
}
Verificar "PUT /api/usuarios/{id} -> 200" ($actualizarUsuario.Status -eq 200) "status=$($actualizarUsuario.Status)"
$usuarioActualizado = $actualizarUsuario.Body | ConvertFrom-Json
Verificar "PUT /api/usuarios/{id} cambia el rol" ($usuarioActualizado.rol -eq "CLIENTE") "rol=$($usuarioActualizado.rol)"

# DELETE /api/usuarios/{id}
$eliminarUsuario = Llamar -Metodo DELETE -Ruta "/api/usuarios/$usuarioId" -Token $admin.accessToken
Verificar "DELETE /api/usuarios/{id} -> 204" ($eliminarUsuario.Status -eq 204) "status=$($eliminarUsuario.Status)"
$loginDesactivado = Llamar -Metodo POST -Ruta "/api/auth/login" -Body @{ email = $emailVendedorNuevo; password = "Vend123!" }
Verificar "Usuario desactivado no puede iniciar sesion -> 401" ($loginDesactivado.Status -eq 401) "status=$($loginDesactivado.Status)"

Write-Host "`n=== PRODUCTOS ===" -ForegroundColor Cyan

# GET /api/productos
$catalogo = Llamar -Metodo GET -Ruta "/api/productos"
$productos = $catalogo.Body | ConvertFrom-Json
Verificar "GET /api/productos (publico) -> 200" ($catalogo.Status -eq 200 -and $productos.Count -ge 1) "status=$($catalogo.Status) count=$($productos.Count)"
$productoBase = $productos[0]

# GET /api/productos/{id}
$obtenerProducto = Llamar -Metodo GET -Ruta "/api/productos/$($productoBase.id)"
Verificar "GET /api/productos/{id} (publico) -> 200" ($obtenerProducto.Status -eq 200) "status=$($obtenerProducto.Status)"
Verificar "GET /api/productos/{id inexistente} -> 404" ((Llamar -Metodo GET -Ruta "/api/productos/999999").Status -eq 404) ""

# POST /api/productos (ADMIN)
$crearProductoAdmin = Llamar -Metodo POST -Ruta "/api/productos" -Token $admin.accessToken -Body @{
    nombre = "Producto Admin $sufijo"; descripcion = "Creado por admin"; precio = 1500
    stock = 5; categoria = "Frutas"; imagenUrl = "mango.jpg"
}
$productoAdmin = $crearProductoAdmin.Body | ConvertFrom-Json
Verificar "POST /api/productos (ADMIN) -> 201" ($crearProductoAdmin.Status -eq 201) "status=$($crearProductoAdmin.Status)"
Verificar "POST /api/productos (CLIENTE) -> 403" ((Llamar -Metodo POST -Ruta "/api/productos" -Token $cliente.accessToken -Body @{ nombre = "X"; precio = 10; stock = 1 }).Status -eq 403) ""

# PUT /api/productos/{id} (ADMIN, dueño)
$editarAdmin = Llamar -Metodo PUT -Ruta "/api/productos/$($productoAdmin.id)" -Token $admin.accessToken -Body @{
    nombre = "Producto Admin Editado"; descripcion = "Editado"; precio = 1700; stock = 8; categoria = "Frutas"
}
Verificar "PUT /api/productos/{id} (ADMIN dueño) -> 200" ($editarAdmin.Status -eq 200) "status=$($editarAdmin.Status)"

# POST /api/productos (VENDEDOR)
$crearProductoVendedor = Llamar -Metodo POST -Ruta "/api/productos" -Token $vendedor.accessToken -Body @{
    nombre = "Producto Vendedor $sufijo"; descripcion = "Creado por vendedor"; precio = 900
    stock = 4; categoria = "Frutas"; imagenUrl = "naranja.jpg"
}
$productoVendedor = $crearProductoVendedor.Body | ConvertFrom-Json
Verificar "POST /api/productos (VENDEDOR) -> 201" ($crearProductoVendedor.Status -eq 201) "status=$($crearProductoVendedor.Status)"

# PUT producto propio del VENDEDOR
$editarVendedor = Llamar -Metodo PUT -Ruta "/api/productos/$($productoVendedor.id)" -Token $vendedor.accessToken -Body @{
    nombre = "Producto Vendedor Editado"; descripcion = "Editado"; precio = 950; stock = 6; categoria = "Frutas"
}
Verificar "PUT /api/productos/{id} (VENDEDOR dueño) -> 200" ($editarVendedor.Status -eq 200) "status=$($editarVendedor.Status)"

# PUT producto ajeno (debe fallar)
$editarAjeno = Llamar -Metodo PUT -Ruta "/api/productos/$($productoAdmin.id)" -Token $vendedor.accessToken -Body @{
    nombre = "Intento ajeno"; precio = 1; stock = 1; categoria = "Frutas"
}
Verificar "PUT /api/productos/{id} (VENDEDOR ajeno) -> 403" ($editarAjeno.Status -eq 403) "status=$($editarAjeno.Status)"

# DELETE /api/productos/{id} (VENDEDOR dueño)
$borrarVendedor = Llamar -Metodo DELETE -Ruta "/api/productos/$($productoVendedor.id)" -Token $vendedor.accessToken
Verificar "DELETE /api/productos/{id} (VENDEDOR dueño) -> 204" ($borrarVendedor.Status -eq 204) "status=$($borrarVendedor.Status)"

# DELETE /api/productos/{id} (ADMIN)
$borrarAdmin = Llamar -Metodo DELETE -Ruta "/api/productos/$($productoAdmin.id)" -Token $admin.accessToken
Verificar "DELETE /api/productos/{id} (ADMIN) -> 204" ($borrarAdmin.Status -eq 204) "status=$($borrarAdmin.Status)"

Write-Host "`n=== PEDIDOS ===" -ForegroundColor Cyan

# Producto dedicado para no consumir el stock de los productos semilla
$crearProductoPedido = Llamar -Metodo POST -Ruta "/api/productos" -Token $vendedor.accessToken -Body @{
    nombre = "Producto Pedido $sufijo"; descripcion = "Producto para pruebas de pedidos"; precio = 1000
    stock = 100; categoria = "Frutas"; imagenUrl = "naranja.jpg"
}
$productoPedido = $crearProductoPedido.Body | ConvertFrom-Json
Verificar "POST /api/productos (producto para pedidos) -> 201" ($crearProductoPedido.Status -eq 201) "status=$($crearProductoPedido.Status)"

# POST /api/pedidos
$precioEsperado = [decimal]$productoPedido.precio * 2
$crearPedido = Llamar -Metodo POST -Ruta "/api/pedidos" -Token $cliente.accessToken -Body @{
    items = @(@{ productoId = $productoPedido.id; cantidad = 2 })
    direccionEntrega = "Av. Endpoints 123"
}
$pedido = $crearPedido.Body | ConvertFrom-Json
Verificar "POST /api/pedidos (CLIENTE) -> 201" ($crearPedido.Status -eq 201) "status=$($crearPedido.Status)"
Verificar "POST /api/pedidos calcula el total en servidor" ([decimal]$pedido.total -eq $precioEsperado) "total=$($pedido.total) esperado=$precioEsperado"
Verificar "POST /api/pedidos (ADMIN) -> 403" ((Llamar -Metodo POST -Ruta "/api/pedidos" -Token $admin.accessToken -Body @{ items = @(@{ productoId = $productoPedido.id; cantidad = 1 }) }).Status -eq 403) ""
Verificar "POST /api/pedidos sin token -> 401" ((Llamar -Metodo POST -Ruta "/api/pedidos" -Body @{ items = @(@{ productoId = $productoPedido.id; cantidad = 1 }) }).Status -eq 401) ""
Verificar "POST /api/pedidos sin stock -> 422" ((Llamar -Metodo POST -Ruta "/api/pedidos" -Token $cliente.accessToken -Body @{ items = @(@{ productoId = $productoPedido.id; cantidad = 999999 }) }).Status -eq 422) ""

# GET /api/pedidos/mis-pedidos
$misPedidos = Llamar -Metodo GET -Ruta "/api/pedidos/mis-pedidos" -Token $cliente.accessToken
Verificar "GET /api/pedidos/mis-pedidos (CLIENTE) -> 200" ($misPedidos.Status -eq 200) "status=$($misPedidos.Status)"
Verificar "GET /api/pedidos/mis-pedidos (ADMIN) -> 403" ((Llamar -Metodo GET -Ruta "/api/pedidos/mis-pedidos" -Token $admin.accessToken).Status -eq 403) ""

# GET /api/pedidos (ADMIN)
$todosPedidos = Llamar -Metodo GET -Ruta "/api/pedidos" -Token $admin.accessToken
Verificar "GET /api/pedidos (ADMIN) -> 200" ($todosPedidos.Status -eq 200) "status=$($todosPedidos.Status)"
Verificar "GET /api/pedidos (CLIENTE) -> 403" ((Llamar -Metodo GET -Ruta "/api/pedidos" -Token $cliente.accessToken).Status -eq 403) ""

# GET /api/pedidos/{id}
$pedidoId = $pedido.id
$obtenerPedido = Llamar -Metodo GET -Ruta "/api/pedidos/$pedidoId" -Token $cliente.accessToken
Verificar "GET /api/pedidos/{id} (dueño) -> 200" ($obtenerPedido.Status -eq 200) "status=$($obtenerPedido.Status)"
Verificar "GET /api/pedidos/{id} (ADMIN) -> 200" ((Llamar -Metodo GET -Ruta "/api/pedidos/$pedidoId" -Token $admin.accessToken).Status -eq 200) ""
Verificar "GET /api/pedidos/{id} (VENDEDOR ajeno) -> 403" ((Llamar -Metodo GET -Ruta "/api/pedidos/$pedidoId" -Token $vendedor.accessToken).Status -eq 403) ""
Verificar "GET /api/pedidos/{id inexistente} -> 404" ((Llamar -Metodo GET -Ruta "/api/pedidos/999999" -Token $admin.accessToken).Status -eq 404) ""

# PATCH /api/pedidos/{id}/estado
$cambiarEstado = Llamar -Metodo PATCH -Ruta "/api/pedidos/$pedidoId/estado" -Token $admin.accessToken -Body @{ estado = "CONFIRMADO" }
$pedidoConfirmado = $cambiarEstado.Body | ConvertFrom-Json
Verificar "PATCH /api/pedidos/{id}/estado (ADMIN) -> 200" ($cambiarEstado.Status -eq 200) "status=$($cambiarEstado.Status)"
Verificar "PATCH /api/pedidos/{id}/estado cambia a CONFIRMADO" ($pedidoConfirmado.estado -eq "CONFIRMADO") "estado=$($pedidoConfirmado.estado)"
Verificar "PATCH /api/pedidos/{id}/estado (VENDEDOR) -> 200" ((Llamar -Metodo PATCH -Ruta "/api/pedidos/$pedidoId/estado" -Token $vendedor.accessToken -Body @{ estado = "ENTREGADO" }).Status -eq 200) ""
Verificar "PATCH /api/pedidos/{id}/estado (CLIENTE) -> 403" ((Llamar -Metodo PATCH -Ruta "/api/pedidos/$pedidoId/estado" -Token $cliente.accessToken -Body @{ estado = "CANCELADO" }).Status -eq 403) ""

# Limpieza del producto de pedidos
Llamar -Metodo DELETE -Ruta "/api/productos/$($productoPedido.id)" -Token $vendedor.accessToken | Out-Null

Write-Host ""
if ($script:fallos -eq 0) {
    Write-Host "TODOS LOS ENDPOINTS FUNCIONAN: $script:ok verificaciones OK" -ForegroundColor Green
    exit 0
} else {
    Write-Host "$script:fallos FALLA(S) de $($script:ok + $script:fallos) verificaciones" -ForegroundColor Red
    exit 1
}
