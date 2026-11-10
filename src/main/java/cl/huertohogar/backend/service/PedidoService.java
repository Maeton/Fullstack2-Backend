package cl.huertohogar.backend.service;

import cl.huertohogar.backend.model.DetallePedido;
import cl.huertohogar.backend.model.EstadoPedido;
import cl.huertohogar.backend.model.Pedido;
import cl.huertohogar.backend.model.Producto;
import cl.huertohogar.backend.model.Rol;
import cl.huertohogar.backend.model.Usuario;
import cl.huertohogar.backend.dto.pedido.ItemPedidoRequest;
import cl.huertohogar.backend.dto.pedido.PedidoRequest;
import cl.huertohogar.backend.dto.pedido.PedidoResponse;
import cl.huertohogar.backend.exception.AccesoDenegadoException;
import cl.huertohogar.backend.exception.RecursoNoEncontradoException;
import cl.huertohogar.backend.exception.ReglaNegocioException;
import cl.huertohogar.backend.repository.PedidoRepository;
import cl.huertohogar.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioService usuarioService;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductoRepository productoRepository,
                         UsuarioService usuarioService) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public PedidoResponse crear(String emailCliente, PedidoRequest request) {
        Usuario cliente = usuarioService.buscarPorEmail(emailCliente);

        if (cliente.getRol() != Rol.CLIENTE) {
            throw new ReglaNegocioException("Solo los clientes pueden crear pedidos");
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setDireccionEntrega(
                request.direccionEntrega() != null && !request.direccionEntrega().isBlank()
                        ? request.direccionEntrega().trim()
                        : cliente.getDireccion());

        BigDecimal total = BigDecimal.ZERO;

        for (ItemPedidoRequest item : request.items()) {
            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Producto no encontrado con id " + item.productoId()));

            if (!producto.isActivo()) {
                throw new ReglaNegocioException(
                        "El producto '" + producto.getNombre() + "' ya no está disponible");
            }

            if (producto.getStock() < item.cantidad()) {
                throw new ReglaNegocioException(
                        "Stock insuficiente para '" + producto.getNombre()
                                + "'. Disponible: " + producto.getStock());
            }

            BigDecimal precioUnitario = producto.getPrecio();
            BigDecimal subtotal = precioUnitario.multiply(BigDecimal.valueOf(item.cantidad()));

            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(item.cantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subtotal);
            pedido.agregarDetalle(detalle);

            // El stock y el total se calculan en el servidor, nunca desde el navegador.
            producto.setStock(producto.getStock() - item.cantidad());
            productoRepository.save(producto);

            total = total.add(subtotal);
        }

        pedido.setTotal(total);
        Pedido guardado = pedidoRepository.save(pedido);
        return PedidoResponse.from(guardado);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> misPedidos(String emailCliente) {
        Usuario cliente = usuarioService.buscarPorEmail(emailCliente);
        return pedidoRepository.findByClienteIdOrderByFechaDesc(cliente.getId())
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarTodos() {
        return pedidoRepository.findAllByOrderByFechaDesc()
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id, String emailSolicitante, boolean esAdmin) {
        Pedido pedido = buscarEntidad(id);
        validarAcceso(pedido, emailSolicitante, esAdmin);
        return PedidoResponse.from(pedido);
    }

    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = buscarEntidad(id);

        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new ReglaNegocioException("Un pedido cancelado no puede cambiar de estado");
        }

        if (pedido.getEstado() == EstadoPedido.ENTREGADO && nuevoEstado != EstadoPedido.ENTREGADO) {
            throw new ReglaNegocioException("Un pedido entregado no puede volver a otro estado");
        }

        pedido.setEstado(nuevoEstado);
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    private Pedido buscarEntidad(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido", id));
    }

    private void validarAcceso(Pedido pedido, String emailSolicitante, boolean esAdmin) {
        if (esAdmin) {
            return;
        }

        Usuario solicitante = usuarioService.buscarPorEmail(emailSolicitante);

        if (!pedido.getCliente().getId().equals(solicitante.getId())) {
            throw new AccesoDenegadoException("No puedes ver pedidos de otros clientes");
        }
    }
}
