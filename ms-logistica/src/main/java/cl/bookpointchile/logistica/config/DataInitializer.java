package cl.bookpointchile.logistica.config;

import cl.bookpointchile.logistica.model.Envio;
import cl.bookpointchile.logistica.model.EstadoEnvio;
import cl.bookpointchile.logistica.model.RutaDistribucion;
import cl.bookpointchile.logistica.repository.EnvioRepository;
import cl.bookpointchile.logistica.repository.RutaDistribucionRepository;
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

    private final RutaDistribucionRepository rutaDistribucionRepository;
    private final EnvioRepository envioRepository;

    @Override
    public void run(String... args) throws Exception {
        if (rutaDistribucionRepository.count() == 0) {
            log.info("Inicializando datos logísticos base en ms-logistica...");

            // 1. Cargar las Rutas de Distribución optimizadas desde la Bodega Central (Concepción)
            RutaDistribucion rutaHualpen = RutaDistribucion.builder()
                    .origen("Bodega Central Concepción")
                    .destino("Hualpén")
                    .transportista("Courier BíoBío - Zona A")
                    .build();

            RutaDistribucion rutaTalcahuano = RutaDistribucion.builder()
                    .origen("Bodega Central Concepción")
                    .destino("Talcahuano")
                    .transportista("Courier BíoBío - Zona B")
                    .build();

            RutaDistribucion rutaGeneral = RutaDistribucion.builder()
                    .origen("Bodega Central Concepción")
                    .destino("Región del Bío Bío")
                    .transportista("Transportes del Sur S.A.")
                    .build();

            rutaDistribucionRepository.saveAll(List.of(rutaHualpen, rutaTalcahuano, rutaGeneral));
            log.info("Rutas de distribución (Hualpén, Talcahuano y Región Bío Bío) creadas.");

            // 2. Cargar envíos iniciales de prueba (asociados a ventas simuladas 5001, 5002, 5003)
            if (envioRepository.count() == 0) {
                // Envio PENDIENTE hacia Hualpén (utilizará rutaHualpen automáticamente)
                Envio envio1 = Envio.builder()
                        .ventaId(5001L)
                        .direccionDestino("Calle Los Copihues 1420, Hualpén")
                        .estado(EstadoEnvio.PENDIENTE)
                        .fechaEstimada(LocalDate.now().plusDays(3))
                        .rutaDistribucion(rutaHualpen)
                        .build();

                // Envio EN_RUTA hacia Talcahuano (utilizará rutaTalcahuano automáticamente)
                Envio envio2 = Envio.builder()
                        .ventaId(5002L)
                        .direccionDestino("Av. Colón 3200, Talcahuano")
                        .estado(EstadoEnvio.EN_RUTA)
                        .fechaEstimada(LocalDate.now().plusDays(2))
                        .rutaDistribucion(rutaTalcahuano)
                        .build();

                // Envio ENTREGADO (Estado final, no modificable)
                Envio envio3 = Envio.builder()
                        .ventaId(5003L)
                        .direccionDestino("O'Higgins 124, Concepción")
                        .estado(EstadoEnvio.ENTREGADO)
                        .fechaEstimada(LocalDate.now().minusDays(1))
                        .rutaDistribucion(rutaGeneral)
                        .build();

                envioRepository.saveAll(List.of(envio1, envio2, envio3));
                log.info("Envíos de prueba inyectados (PENDIENTE en Hualpén, EN_RUTA en Talcahuano, ENTREGADO en Concepción).");
            }
        } else {
            log.info("Datos logísticos ya inicializados en la base de datos.");
        }
    }
}
