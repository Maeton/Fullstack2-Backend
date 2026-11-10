package cl.huertohogar.backend.service;

import cl.huertohogar.backend.model.Producto;
import cl.huertohogar.backend.model.Rol;
import cl.huertohogar.backend.model.Usuario;
import cl.huertohogar.backend.dto.producto.ProductoRequest;
import cl.huertohogar.backend.dto.producto.ProductoResponse;
import cl.huertohogar.backend.exception.AccesoDenegadoException;
import cl.huertohogar.backend.exception.RecursoNoEncontradoException;
import cl.huertohogar.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final UsuarioService usuarioService;

    public ProductoService(ProductoRepository productoRepository, UsuarioService usuarioService) {
        this.productoRepository = productoRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos() {
        return productoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.from(buscarActivo(id));
    }

    @Transactional
    public ProductoResponse crear(String emailVendedor, ProductoRequest request) {
        Usuario vendedor = usuarioService.buscarPorEmail(emailVendedor);

        Producto producto = new Producto();
        aplicarDatos(producto, request);
        producto.setVendedor(vendedor);
        producto.setActivo(true);

        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, String emailSolicitante, boolean esAdmin,
                                       ProductoRequest request) {
        Producto producto = buscarActivo(id);
        validarPropietario(producto, emailSolicitante, esAdmin);

        aplicarDatos(producto, request);
        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Transactional
    public void desactivar(Long id, String emailSolicitante, boolean esAdmin) {
        Producto producto = buscarActivo(id);
        validarPropietario(producto, emailSolicitante, esAdmin);

        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Transactional(readOnly = true)
    public Producto buscarActivo(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));

        if (!producto.isActivo()) {
            throw new RecursoNoEncontradoException("El producto está desactivado");
        }
        return producto;
    }

    private void validarPropietario(Producto producto, String emailSolicitante, boolean esAdmin) {
        if (esAdmin) {
            return;
        }

        Usuario solicitante = usuarioService.buscarPorEmail(emailSolicitante);

        if (solicitante.getRol() != Rol.VENDEDOR) {
            throw new AccesoDenegadoException(
                    "Solo un vendedor o un administrador puede modificar productos");
        }

        if (producto.getVendedor() == null
                || !producto.getVendedor().getId().equals(solicitante.getId())) {
            throw new AccesoDenegadoException(
                    "Solo puedes modificar los productos que tú publicaste");
        }
    }

    private void aplicarDatos(Producto producto, ProductoRequest request) {
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setCategoria(request.categoria());
        producto.setImagenUrl(request.imagenUrl());
    }
}
