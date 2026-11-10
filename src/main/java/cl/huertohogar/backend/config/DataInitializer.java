package cl.huertohogar.backend.config;

import cl.huertohogar.backend.model.Producto;
import cl.huertohogar.backend.model.Rol;
import cl.huertohogar.backend.model.Usuario;
import cl.huertohogar.backend.repository.ProductoRepository;
import cl.huertohogar.backend.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner cargarDatosIniciales(UsuarioRepository usuarioRepository,
                                           ProductoRepository productoRepository,
                                           PasswordEncoder passwordEncoder) {
        return args -> {
            crearUsuarioSiNoExiste(usuarioRepository, passwordEncoder,
                    "Administrador Huerto", "admin@huerto.cl", "Admin123!", Rol.ADMIN,
                    "+56911111111", "Av. Providencia 1234, Santiago");

            Usuario vendedor = crearUsuarioSiNoExiste(usuarioRepository, passwordEncoder,
                    "Vendedor Huerto", "vendedor@huerto.cl", "Vendedor123!", Rol.VENDEDOR,
                    "+56922222222", "Av. Vicuña Mackenna 5678, Santiago");

            crearUsuarioSiNoExiste(usuarioRepository, passwordEncoder,
                    "Cliente Huerto", "cliente@huerto.cl", "Cliente123!", Rol.CLIENTE,
                    "+56933333333", "Av. Matta 910, Santiago");

            if (productoRepository.count() == 0) {
                List<Producto> productos = List.of(
                        producto("Manzanas Fuji", "Manzanas Fuji crujientes y dulces.",
                                new BigDecimal("1200.00"), 150, "Frutas", "manzanasfuji.jpg", vendedor),
                        producto("Naranjas", "Jugosas y ricas en vitamina C.",
                                new BigDecimal("1000.00"), 200, "Frutas", "naranja.jpg", vendedor),
                        producto("Plátanos", "Plátanos maduros y dulces.",
                                new BigDecimal("800.00"), 250, "Frutas", "platanos.jpg", vendedor),
                        producto("Pomelo", "Pomelo fresco y jugoso.",
                                new BigDecimal("2150.00"), 50, "Frutas", "pomelo.jpg", vendedor),
                        producto("Palta", "Palta fresca y cremosa.",
                                new BigDecimal("6290.00"), 50, "Frutas", "aguacate.jpg", vendedor),
                        producto("Mango", "Mango fresco y jugoso.",
                                new BigDecimal("2990.00"), 50, "Frutas", "mango.jpg", vendedor)
                );
                productoRepository.saveAll(productos);
            }
        };
    }

    private Usuario crearUsuarioSiNoExiste(UsuarioRepository repository,
                                           PasswordEncoder passwordEncoder,
                                           String nombre, String email, String password,
                                           Rol rol, String telefono, String direccion) {
        return repository.findByEmail(email).orElseGet(() -> {
            Usuario usuario = new Usuario();
            usuario.setNombre(nombre);
            usuario.setEmail(email);
            usuario.setPassword(passwordEncoder.encode(password));
            usuario.setRol(rol);
            usuario.setTelefono(telefono);
            usuario.setDireccion(direccion);
            usuario.setActivo(true);
            return repository.save(usuario);
        });
    }

    private Producto producto(String nombre, String descripcion, BigDecimal precio, int stock,
                              String categoria, String imagenUrl, Usuario vendedor) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setPrecio(precio);
        producto.setStock(stock);
        producto.setCategoria(categoria);
        producto.setImagenUrl(imagenUrl);
        producto.setVendedor(vendedor);
        producto.setActivo(true);
        return producto;
    }
}
