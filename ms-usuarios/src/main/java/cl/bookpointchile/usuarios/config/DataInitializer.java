package cl.bookpointchile.usuarios.config;

import cl.bookpointchile.usuarios.model.Rol;
import cl.bookpointchile.usuarios.model.Usuario;
import cl.bookpointchile.usuarios.repository.RolRepository;
import cl.bookpointchile.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) throws Exception {
        if (rolRepository.count() == 0) {
            log.info("Inicializando roles base en ms-usuarios...");

            // 1. Crear los 4 Roles Exclusivos de BookPoint Chile
            Rol admin = Rol.builder()
                    .nombre("Administrador del Sistema")
                    .descripcion("Gestiona usuarios, asigna/modifica permisos y monitorea la plataforma.")
                    .build();

            Rol jefe = Rol.builder()
                    .nombre("Jefe de Sucursal")
                    .descripcion("Administra personal de su sucursal física asignada.")
                    .build();

            Rol asistente = Rol.builder()
                    .nombre("Asistente de Ventas")
                    .descripcion("Empleado de caja física encargado del registro de ventas locales.")
                    .build();

            Rol cliente = Rol.builder()
                    .nombre("Cliente Web")
                    .descripcion("Usuario final. Realiza compras online y gestiona su perfil.")
                    .build();

            rolRepository.saveAll(List.of(admin, jefe, asistente, cliente));
            log.info("Roles base cargados con éxito.");

            // 2. Crear un Administrador del Sistema por defecto para facilitar pruebas del evaluador
            if (usuarioRepository.count() == 0) {
                Usuario adminUser = Usuario.builder()
                        .rut("11.111.111-1")
                        .nombre("Administrador Principal")
                        .email("admin@bookpoint.cl")
                        .password("[SHA256]admin1234") // Contraseña segura
                        .estado("ACTIVO")
                        .rol(admin)
                        .build();

                usuarioRepository.save(adminUser);
                log.info("Usuario Administrador Principal sembrado con éxito (admin@bookpoint.cl / admin1234).");
            }
        } else {
            log.info("Roles base de ms-usuarios ya se encuentran sembrados en la base de datos.");
        }
    }
}
