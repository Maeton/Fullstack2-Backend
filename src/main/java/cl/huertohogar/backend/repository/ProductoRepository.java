package cl.huertohogar.backend.repository;

import cl.huertohogar.backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrueOrderByNombreAsc();

    List<Producto> findAllByOrderByNombreAsc();

    List<Producto> findByVendedorIdOrderByNombreAsc(Long vendedorId);
}
