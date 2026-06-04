package cl.bookpointchile.bodega.config;

import cl.bookpointchile.bodega.model.OrdenPicking;
import cl.bookpointchile.bodega.model.UbicacionFisica;
import cl.bookpointchile.bodega.repository.OrdenPickingRepository;
import cl.bookpointchile.bodega.repository.UbicacionFisicaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UbicacionFisicaRepository ubicacionRepository;
    private final OrdenPickingRepository pickingRepository;

    @Override
    public void run(String... args) throws Exception {
        if (ubicacionRepository.count() == 0) {
            log.info("Inicializando ubicaciones físicas base en ms-bodega...");

            UbicacionFisica u1 = UbicacionFisica.builder()
                    .pasillo("Pasillo A")
                    .estante("Estante 1")
                    .nivel("Nivel 2")
                    .codigoBarras("P-A-E1-N2")
                    .build();

            UbicacionFisica u2 = UbicacionFisica.builder()
                    .pasillo("Pasillo B")
                    .estante("Estante 3")
                    .nivel("Nivel 1")
                    .codigoBarras("P-B-E3-N1")
                    .build();

            UbicacionFisica u3 = UbicacionFisica.builder()
                    .pasillo("Pasillo C")
                    .estante("Estante 2")
                    .nivel("Nivel 3")
                    .codigoBarras("P-C-E2-N3")
                    .build();

            ubicacionRepository.saveAll(List.of(u1, u2, u3));
            log.info("Ubicaciones físicas inicializadas (P-A-E1-N2, P-B-E3-N1, P-C-E2-N3).");
        }

        if (pickingRepository.count() == 0) {
            log.info("Inicializando órdenes de picking base en ms-bodega...");

            OrdenPicking op1 = OrdenPicking.builder()
                    .ventaId(5001L) // Venta ficticia ID 5001
                    .operarioAsignado("Juan Pérez (Operario de Bodega)")
                    .estado("PENDIENTE")
                    .fechaAsignacion(LocalDateTime.now().minusHours(2))
                    .build();

            pickingRepository.save(op1);
            log.info("Orden de picking semilla registrada con éxito para la venta ID 5001 (Estado: PENDIENTE).");
        }
    }
}
