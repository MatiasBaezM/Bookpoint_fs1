package cl.bookpointchile.inventario.config;

import cl.bookpointchile.inventario.model.Inventario;
import cl.bookpointchile.inventario.repository.InventarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final InventarioRepository inventarioRepository;

    @Override
    public void run(String... args) throws Exception {
        if (inventarioRepository.count() == 0) {
            log.info("Inicializando datos base en la base de datos de ms-inventario...");

            // 1. Cargar Inventario Base asignando sucursalId estáticos (1: Concepción, 2: Temuco, 3: La Serena)
            // Libro 101: Introducción a los Algoritmos en Java
            Inventario inv1 = Inventario.builder()
                    .productoId(101L)
                    .productoNombre("Introducción a los Algoritmos en Java")
                    .sku("SKU-ALG-JAVA-101")
                    .cantidad(100)
                    .stockMinimo(10)
                    .sucursalId(1L) // Concepción
                    .build();

            Inventario inv2 = Inventario.builder()
                    .productoId(101L)
                    .productoNombre("Introducción a los Algoritmos en Java")
                    .sku("SKU-ALG-JAVA-102")
                    .cantidad(15)
                    .stockMinimo(5)
                    .sucursalId(2L) // Temuco
                    .build();

            // Libro 102: Patrones de Diseño de Software
            Inventario inv3 = Inventario.builder()
                    .productoId(102L)
                    .productoNombre("Patrones de Diseño de Software")
                    .sku("SKU-PAT-DSG-201")
                    .cantidad(50)
                    .stockMinimo(8)
                    .sucursalId(1L) // Concepción
                    .build();

            // Libro 103: Microservicios Eficientes con Spring Boot
            Inventario inv4 = Inventario.builder()
                    .productoId(103L)
                    .productoNombre("Microservicios Eficientes con Spring Boot")
                    .sku("SKU-MSV-SPR-301")
                    .cantidad(3) // BAJO STOCK - Debe disparar alerta de reposición
                    .stockMinimo(5)
                    .sucursalId(1L) // Concepción
                    .build();

            Inventario inv5 = Inventario.builder()
                    .productoId(103L)
                    .productoNombre("Microservicios Eficientes con Spring Boot")
                    .sku("SKU-MSV-SPR-302")
                    .cantidad(2) // BAJO STOCK - Debe disparar alerta de reposición
                    .stockMinimo(5)
                    .sucursalId(3L) // La Serena
                    .build();

            // Producto 999: Libro sin Stock para Pruebas del Gateway/Ventas
            Inventario inv6 = Inventario.builder()
                    .productoId(999L)
                    .productoNombre("Libro Agotado de Pruebas")
                    .sku("SKU-OUT-STK-999")
                    .cantidad(0)
                    .stockMinimo(2)
                    .sucursalId(1L) // Concepción
                    .build();

            inventarioRepository.saveAll(List.of(inv1, inv2, inv3, inv4, inv5, inv6));
            log.info("Inventario base cargado con éxito vinculando IDs de sucursales maestras.");
        } else {
            log.info("Base de datos ya inicializada. Omitiendo la carga de datos de prueba.");
        }
    }
}
