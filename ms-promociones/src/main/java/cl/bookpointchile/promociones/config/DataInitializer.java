package cl.bookpointchile.promociones.config;

import cl.bookpointchile.promociones.model.Promocion;
import cl.bookpointchile.promociones.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PromocionRepository promocionRepository;

    @Override
    public void run(String... args) throws Exception {
        if (promocionRepository.count() == 0) {
            log.info("Inicializando datos base en ms-promociones...");

            // 1. CONVENIO_ESTUDIANTIL - 15% Descuento vigente
            Promocion convenioEstudiantil = Promocion.builder()
                    .codigo("CONVENIO_ESTUDIANTIL")
                    .porcentajeDescuento(15)
                    .fechaInicio(LocalDate.now().minusMonths(3))
                    .fechaFin(LocalDate.now().plusMonths(9)) // Vigente para todo el año en curso
                    .estado("ACTIVO")
                    .build();

            // 2. DESCUENTO10 - 10% Descuento vigente
            Promocion descuento10 = Promocion.builder()
                    .codigo("DESCUENTO10")
                    .porcentajeDescuento(10)
                    .fechaInicio(LocalDate.now().minusMonths(3))
                    .fechaFin(LocalDate.now().plusMonths(9)) // Vigente para todo el año en curso
                    .estado("ACTIVO")
                    .build();

            // 3. VERANO2025 - 20% Descuento caducado
            Promocion verano2025 = Promocion.builder()
                    .codigo("VERANO2025")
                    .porcentajeDescuento(20)
                    .fechaInicio(LocalDate.of(2024, 12, 1))
                    .fechaFin(LocalDate.of(2025, 3, 31)) // Caducado
                    .estado("ACTIVO")
                    .build();

            promocionRepository.saveAll(List.of(convenioEstudiantil, descuento10, verano2025));
            log.info("Promociones base creadas con éxito (CONVENIO_ESTUDIANTIL, DESCUENTO10, VERANO2025).");
        } else {
            log.info("Catálogo de ms-promociones ya se encuentra sembrado en la base de datos.");
        }
    }
}
