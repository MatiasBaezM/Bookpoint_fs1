package cl.bookpointchile.facturacion.config;

import cl.bookpointchile.facturacion.model.DocumentoTributario;
import cl.bookpointchile.facturacion.repository.DocumentoTributarioRepository;
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

    private final DocumentoTributarioRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            log.info("Inicializando base de datos en ms-facturacion con datos semilla...");

            // 1. Boleta Semilla
            DocumentoTributario boleta1 = DocumentoTributario.builder()
                    .folioVenta("V-2026-0001")
                    .rutCliente("19.876.543-K")
                    .razonSocial(null)
                    .giro(null)
                    .tipoDocumento("BOLETA")
                    .montoNeto(10000.0)
                    .montoIva(1900.0)
                    .montoTotal(11900.0)
                    .fechaEmision(LocalDateTime.now().minusDays(2))
                    .build();

            // 2. Factura Semilla (B2B)
            DocumentoTributario factura1 = DocumentoTributario.builder()
                    .folioVenta("V-2026-0002")
                    .rutCliente("76.543.210-9")
                    .razonSocial("EDITORIAL ALFA S.A.")
                    .giro("VENTA AL POR MAYOR DE LIBROS")
                    .tipoDocumento("FACTURA")
                    .montoNeto(150000.0)
                    .montoIva(28500.0)
                    .montoTotal(178500.0)
                    .fechaEmision(LocalDateTime.now().minusDays(1))
                    .build();

            repository.saveAll(List.of(boleta1, factura1));
            log.info("Sembrado de datos en ms-facturacion finalizado con éxito (Folios: V-2026-0001, V-2026-0002).");
        } else {
            log.info("El catálogo fiscal de ms-facturacion ya contiene datos sembrados.");
        }
    }
}
