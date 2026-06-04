package cl.bookpointchile.sucursales.config;

import cl.bookpointchile.sucursales.model.Sucursal;
import cl.bookpointchile.sucursales.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SucursalRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            log.info("Inicializando catálogo de sucursales base en ms-sucursales...");

            // 1. Bodega Central Concepción (ubicada en Hualpén)
            Sucursal bodegaCentral = Sucursal.builder()
                    .nombre("Bodega Central Concepción")
                    .direccion("Av. Las Golondrinas 1820")
                    .comuna("Hualpén")
                    .region("Región del Biobío")
                    .telefono("+56 41 273 4001")
                    .horarioAtencion("Lunes a Viernes 08:30 a 18:00")
                    .estadoOperativo("ACTIVO")
                    .build();

            // 2. Sucursal Temuco
            Sucursal sucursalTemuco = Sucursal.builder()
                    .nombre("Sucursal Temuco")
                    .direccion("Av. Alemania 0890")
                    .comuna("Temuco")
                    .region("Región de La Araucanía")
                    .telefono("+56 45 221 3002")
                    .horarioAtencion("Lunes a Sábado 09:00 a 20:00")
                    .estadoOperativo("ACTIVO")
                    .build();

            // 3. Sucursal La Serena
            Sucursal sucursalLaSerena = Sucursal.builder()
                    .nombre("Sucursal La Serena")
                    .direccion("Alberto Solari 1400 (Mall Plaza)")
                    .comuna("La Serena")
                    .region("Región de Coquimbo")
                    .telefono("+56 51 242 8003")
                    .horarioAtencion("Lunes a Domingo 10:00 a 21:00")
                    .estadoOperativo("ACTIVO")
                    .build();

            repository.saveAll(List.of(bodegaCentral, sucursalTemuco, sucursalLaSerena));
            log.info("Sembrado de sucursales en ms-sucursales finalizado con éxito (3 sedes creadas).");
        } else {
            log.info("El maestro de datos de ms-sucursales ya contiene sedes registradas.");
        }
    }
}
