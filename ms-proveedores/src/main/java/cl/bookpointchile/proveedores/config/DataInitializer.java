package cl.bookpointchile.proveedores.config;

import cl.bookpointchile.proveedores.model.*;
import cl.bookpointchile.proveedores.repository.OrdenCompraRepository;
import cl.bookpointchile.proveedores.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenCompraRepository;

    @Override
    public void run(String... args) throws Exception {
        if (proveedorRepository.count() == 0) {
            log.info("Inicializando proveedores y ODC base en ms-proveedores...");

            // 1. Sembrar Proveedores B2B base
            Proveedor planeta = Proveedor.builder()
                    .rut("76.123.456-K")
                    .razonSocial("Editorial Planeta Chilena S.A.")
                    .emailContacto("adquisiciones@planeta.cl")
                    .telefono("+56223456789")
                    .build();

            Proveedor papeleria = Proveedor.builder()
                    .rut("88.987.654-3")
                    .razonSocial("Distribuidora de Papelería Concepción Ltda")
                    .emailContacto("ventas@papeleriaconcep.cl")
                    .telefono("+56412345678")
                    .build();

            Proveedor randomHouse = Proveedor.builder()
                    .rut("77.345.987-1")
                    .razonSocial("Penguin Random House Grupo Editorial")
                    .emailContacto("contacto@randomhouse.cl")
                    .telefono("+56229876543")
                    .build();

            proveedorRepository.saveAll(List.of(planeta, papeleria, randomHouse));
            log.info("Proveedores base inyectados.");

            // 2. Sembrar Órdenes de Compra (ODC) de prueba
            if (ordenCompraRepository.count() == 0) {
                // ODC 1: PENDIENTE para Editorial Planeta (para probar la transicion a RECIBIDA)
                OrdenCompra odc1 = OrdenCompra.builder()
                        .proveedor(planeta)
                        .fechaEmision(LocalDateTime.now().minusDays(2))
                        .estado(EstadoOrden.PENDIENTE)
                        .build();

                DetalleOrden det1 = DetalleOrden.builder()
                        .productoId(101L) // Introducción a los Algoritmos en Java
                        .cantidadSolicitada(50)
                        .costoUnitario(new BigDecimal("15000.00"))
                        .build();

                DetalleOrden det2 = DetalleOrden.builder()
                        .productoId(102L) // Patrones de Diseño
                        .cantidadSolicitada(30)
                        .costoUnitario(new BigDecimal("20000.00"))
                        .build();

                odc1.addDetalle(det1);
                odc1.addDetalle(det2);

                // ODC 2: RECIBIDA previamente para Distribuidora de Papelería
                OrdenCompra odc2 = OrdenCompra.builder()
                        .proveedor(papeleria)
                        .fechaEmision(LocalDateTime.now().minusDays(5))
                        .estado(EstadoOrden.RECIBIDA)
                        .build();

                DetalleOrden det3 = DetalleOrden.builder()
                        .productoId(104L) // Destacadores Stabilo
                        .cantidadSolicitada(100)
                        .costoUnitario(new BigDecimal("4500.00"))
                        .build();

                odc2.addDetalle(det3);

                // ODC 3: CANCELADA (para probar el rechazo de recepción)
                OrdenCompra odc3 = OrdenCompra.builder()
                        .proveedor(randomHouse)
                        .fechaEmision(LocalDateTime.now().minusDays(10))
                        .estado(EstadoOrden.CANCELADA)
                        .build();

                DetalleOrden det4 = DetalleOrden.builder()
                        .productoId(103L) // Microservicios Spring
                        .cantidadSolicitada(10)
                        .costoUnitario(new BigDecimal("28000.00"))
                        .build();

                odc3.addDetalle(det4);

                ordenCompraRepository.saveAll(List.of(odc1, odc2, odc3));
                log.info("Órdenes de Compra (ODC) de prueba sembradas (PENDIENTE, RECIBIDA, CANCELADA).");
            }
        } else {
            log.info("Proveedores y ODC ya inicializados en la base de datos.");
        }
    }
}
