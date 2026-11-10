package cl.huertohogar.backend.repository;

import cl.huertohogar.backend.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByFechaDesc(Long clienteId);

    List<Pedido> findAllByOrderByFechaDesc();
}
