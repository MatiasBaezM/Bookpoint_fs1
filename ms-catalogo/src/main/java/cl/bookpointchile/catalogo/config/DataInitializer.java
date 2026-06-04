package cl.bookpointchile.catalogo.config;

import cl.bookpointchile.catalogo.model.Producto;
import cl.bookpointchile.catalogo.model.Resena;
import cl.bookpointchile.catalogo.repository.ProductoRepository;
import cl.bookpointchile.catalogo.repository.ResenaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductoRepository productoRepository;
    private final ResenaRepository resenaRepository;

    @Override
    public void run(String... args) throws Exception {
        if (productoRepository.count() == 0) {
            log.info("Inicializando catálogo base en ms-catalogo...");

            // 1. Crear Productos (Libros, Papelería y Material Educativo)
            Producto libro1 = Producto.builder()
                    .titulo("Introducción a los Algoritmos en Java")
                    .autor("Thomas H. Cormen")
                    .editorial("Mit Press")
                    .precio(new BigDecimal("25000.00"))
                    .categoria("Libro")
                    .descripcion("El libro de algoritmos definitivo utilizado en las mejores academias de ingeniería del mundo.")
                    .build();

            Producto libro2 = Producto.builder()
                    .titulo("Patrones de Diseño de Software")
                    .autor("Erich Gamma")
                    .editorial("Addison-Wesley")
                    .precio(new BigDecimal("35000.00"))
                    .categoria("Libro")
                    .descripcion("Aprende los 23 patrones de diseño clásicos explicados con diagramas estructurales e implementaciones reales.")
                    .build();

            Producto libro3 = Producto.builder()
                    .titulo("Microservicios Eficientes con Spring Boot")
                    .autor("John Carnell")
                    .editorial("Manning Publications")
                    .precio(new BigDecimal("42000.00"))
                    .categoria("Libro")
                    .descripcion("Guía práctica de diseño y despliegue de microservicios con Spring Cloud, Gateway y Eureka.")
                    .build();

            Producto papel1 = Producto.builder()
                    .titulo("Set de Destacadores Premium pastel")
                    .autor("Stabilo")
                    .editorial("Stabilo Ltda")
                    .precio(new BigDecimal("8500.00"))
                    .categoria("Papelería")
                    .descripcion("Caja de 6 destacadores de colores pastel de larga duración con tecnología antisecado.")
                    .build();

            Producto material1 = Producto.builder()
                    .titulo("Kit Educativo de Robótica Arduino")
                    .autor("Arduino Chile")
                    .editorial("Arduino SRL")
                    .precio(new BigDecimal("75000.00"))
                    .categoria("Material Educativo")
                    .descripcion("Kit completo de iniciación en robótica infantil y juvenil con sensores y placa Arduino Uno.")
                    .build();

            // Guardar catálogo base
            productoRepository.saveAll(List.of(libro1, libro2, libro3, papel1, material1));
            log.info("Catálogo base registrado con éxito.");

            // 2. Agregar Reseñas base para cálculos dinámicos de estrellas
            Resena resena1 = Resena.builder()
                    .usuarioId(10L)
                    .usuarioNombre("Juan Pérez")
                    .calificacion(5)
                    .comentario("Excelente libro de algoritmos. Es denso pero sumamente detallado y explicativo. Totalmente recomendado.")
                    .build();

            Resena resena2 = Resena.builder()
                    .usuarioId(11L)
                    .usuarioNombre("Camila Toro")
                    .calificacion(4)
                    .comentario("Muy completo e instructivo, ideal para repasar antes de entrevistas técnicas.")
                    .build();

            Resena resena3 = Resena.builder()
                    .usuarioId(12L)
                    .usuarioNombre("Andrés Valenzuela")
                    .calificacion(5)
                    .comentario("Un clásico infaltable en la biblioteca de cualquier desarrollador senior de software.")
                    .build();

            Resena resena4 = Resena.builder()
                    .usuarioId(13L)
                    .usuarioNombre("Daniela Muñoz")
                    .calificacion(3)
                    .comentario("El contenido es espectacular, pero los ejemplos en C++ de esta edición están algo obsoletos.")
                    .build();

            Resena resena5 = Resena.builder()
                    .usuarioId(14L)
                    .usuarioNombre("Mauricio Soto")
                    .calificacion(4)
                    .comentario("Muy bonitos colores y trazo suave, no traspasa las hojas comunes. Buen producto.")
                    .build();

            // Vincular bidireccionalmente usando los helpers
            libro1.addResena(resena1);
            libro1.addResena(resena2); // Promedio: 4.5
            
            libro2.addResena(resena3);
            libro2.addResena(resena4); // Promedio: 4.0

            papel1.addResena(resena5); // Promedio: 4.0

            // Persistir reseñas en lote
            resenaRepository.saveAll(List.of(resena1, resena2, resena3, resena4, resena5));
            log.info("Reseñas base asociadas con éxito. Promedios iniciales recalculados de forma dinámica.");
        } else {
            log.info("Catálogo de ms-catalogo ya se encuentra sembrado en la base de datos.");
        }
    }
}
