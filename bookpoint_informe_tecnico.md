# Informe de Análisis Técnico y de QA — Plataforma BookPoint

---

## 1. Matriz de Degradación y Responsabilidad de Microservicios

El sistema BookPoint está compuesto por un Gateway de entrada y 10 microservicios funcionales autónomos. A continuación se detalla la matriz de asignación de puertos, esquemas de bases de datos y la delimitación exacta de sus responsabilidades de negocio junto a sus flujos de intercomunicación:

| Microservicio | Puerto | Base de Datos MySQL | Dominio Técnico y Responsabilidades Lógicas | Flujos de Intercomunicación |
| :--- | :---: | :--- | :--- | :--- |
| **`ms-gateway`** | `8080` | *Ninguna (N/A)* | Punto de entrada unificado y enrutamiento perimetral basado en rutas declarativas (`Path=/api/X/**`). | Redirección de peticiones entrantes hacia las instancias internas correspondientes (puertos `8081`-`8090`). |
| **`ms-ventas`** | `8081` | `bookpoint_ventas` | Gestión del ciclo de vida de transacciones (ventas online y presenciales). Cálculo de subtotales, aplicación de descuentos y emisión de folios. | **Síncrono (Feign)**: `InventarioClient` (stock), `UsuarioClient` (clientes), `PromocionClient` (descuentos), `FacturacionClient` (boletas). <br>**Asíncrono (RabbitMQ)**: Publica `VentaCreadaEvent`. |
| **`ms-inventario`** | `8082` | `bookpoint_inventario` | Control de stock físico por sucursal, traslados de mercadería inter-sucursales y emisión de alertas de reposición. | **Síncrono (Feign)**: `SucursalesClient` para validar sucursales. <br>**Asíncrono (RabbitMQ)**: Consume `VentaCreadaEvent` y publica `StockReservadoEvent` / `StockRechazadoEvent`. |
| **`ms-usuarios`** | `8083` | `bookpoint_usuarios` | Registro, autenticación, control de perfiles y estados operativos ("ACTIVO"/"INACTIVO") de usuarios. | **Síncrono (Feign)**: Expone endpoints consumidos por `ms-ventas`. |
| **`ms-catalogo`** | `8084` | `bookpoint_catalogo` | Gestión del catálogo de libros (título, autor, editorial, precio), búsquedas filtradas, paginación y reseñas. | Autónomo. Expone endpoints de consulta para el cliente web. |
| **`ms-logistica`** | `8085` | `bookpoint_logistica` | Despacho de productos, asignación de rutas de distribución física y actualización de estados del envío. | **Síncrono (Feign)**: `SucursalesClient` para validar el origen de los despachos. |
| **`ms-proveedores`** | `8086` | `bookpoint_proveedores` | Registro de proveedores, emisión de órdenes de compra B2B y control de recepciones de mercadería física. | Autónomo. Gestiona compras de abastecimiento. |
| **`ms-promociones`** | `8087` | `bookpoint_promociones` | Gestión y reglas de negocio para cupones promocionales y convenios institucionales de descuento. | **Síncrono (Feign)**: Consumido por `ms-ventas` para validar cupones. |
| **`ms-facturacion`** | `8088` | `bookpoint_facturacion` | Generación y almacenamiento de documentos tributarios electrónicos legalmente válidos (Boletas y Facturas). | **Síncrono (Feign)**: Consumido por `ms-ventas` tras confirmar la transacción. |
| **`ms-bodega`** | `8089` | `bookpoint_bodega` | Zonificación física de productos (pasillos, estantes, niveles) y órdenes de picking para operarios. | Autónomo. Ejecuta la preparación física de pedidos. |
| **`ms-sucursales`** | `8090` | `bookpoint_sucursales` | Maestro operativo de locales físicos, direcciones, horarios de atención y estado operativo. | **Síncrono (Feign)**: Consumido por `ms-inventario` y `ms-logistica`. |

---

## 2. Stack Tecnológico y Estándares de Ingeniería Distribuidos

### Clasificación Transversal del Stack
* **Java SDK**: El proyecto está configurado para compilar bajo **Java 25** en el archivo POM principal, con dependencias de ejecución compatibles desde Java 17 y 21.
* **Spring Boot**: Utiliza las versiones **4.0.6** para los microservicios de negocio y **4.1.0** para el Gateway. La API del framework mantiene la compatibilidad de anotaciones con Spring Boot 3.x.
* **Persistencia (JPA/Hibernate)**: Mapeo objeto-relacional mediante Spring Data JPA e Hibernate. Implementa estrategias de DDL automático (`spring.jpa.hibernate.ddl-auto=update`) para sincronizar entidades Java con las tablas MySQL.
* **Validación**: Implementa la especificación **Bean Validation JSR 380** (mediante anotaciones como `@NotNull`, `@Size`, `@Pattern`) a nivel de controladores para interceptar payloads corruptos antes de procesarlos.
* **OpenFeign & Resilience4j**: Declaración de clientes REST dinámicos y tolerancia a fallos mediante disyuntores (*circuit breakers*) y reintentos automáticos para evitar la degradación en cascada del sistema.
* **RabbitMQ (AMQP)**: Desacoplamiento asíncrono para eventos del ciclo de vida de la orden.
* **SLF4J & Lombok**: Abstracción de trazas de ejecución con `@Slf4j` y generación automatizada de código mediante anotaciones (`@Builder`, `@Getter`, `@RequiredArgsConstructor`).

### Cumplimiento del Patrón CSR (Client-Side Rendering)
El backend está estructurado bajo un desacoplamiento de capas estricto para retornar únicamente representaciones de estado en formato JSON, delegando la construcción del HTML al navegador del cliente:
* **Controller**: Expone recursos REST stateless, mapea DTOs mediante Spring Web y responde con códigos HTTP semánticos (200, 201, 400, 404, 500).
* **Service**: Contiene la lógica transaccional de negocio pura, aislando las decisiones operativas de la capa web.
* **Repository**: Interfaces que extienden `JpaRepository` para ejecutar consultas SQL optimizadas.
* **DTO**: Clases inmutables que definen el contrato de datos con el cliente frontend, evitando la exposición de las entidades físicas `@Entity`.
* **Model**: Representación del esquema de datos persistido.
* **Exception**: Excepciones custom de negocio (ej. `InsufficientStockException`, `InvalidSaleException`) lanzadas por la capa de servicio.

### Manejo Global de Excepciones
Cada microservicio cuenta con un componente anotado con `@RestControllerAdvice` (ej. `GlobalExceptionHandler`). Este intercepta excepciones específicas de negocio y errores de validación de Spring (`MethodArgumentNotValidException`), transformándolos en una respuesta estandarizada:
* Retorna un DTO común (`ErrorResponse`) que contiene: timestamp, status (HTTP code), error (HTTP text), message (detalle del error) y path (URI del recurso).
* Esto garantiza que el frontend reciba siempre la misma estructura JSON ante cualquier fallo, permitiendo un manejo de errores robusto en la UI.

### Inicialización Aislada de Datos (Data Seeders)
En el microservicio `ms-bodega`, se implementa una clase `DataInitializer` que hereda de `CommandLineRunner`. Al arrancar el servicio en su contenedor, este inicializador evalúa el volumen de registros en base de datos (`ubicacionRepository.count() == 0`). Si el almacenamiento está vacío, persiste de forma automática ubicaciones de almacenamiento semilla (Pasillos, Estantes, Niveles) y órdenes de picking de prueba en la base de datos local `bookpoint_bodega`.

---

## 3. Cobertura de Pruebas de API con Postman

El diseño de pruebas implementado en `/postman` se orienta a la validación de la integración funcional de los endpoints REST expuestos:

### Colección de Pruebas (`Bookpoint.postman_collection.json`)
* **Validación de Esquema y Respuestas**: 
  * A nivel de colección global, se inyecta un script de prueba que valida que todas las respuestas de los microservicios retornen un estado HTTP exitoso `2xx` (`pm.expect(pm.response.code).to.be.within(200, 299)`) y que el tiempo de respuesta sea inferior a **3000 ms**.
  * Las peticiones de creación (peticiones `POST`) validan específicamente el código `201 Created` y analizan la respuesta JSON devuelta para confirmar la existencia de identificadores de recursos persistidos (ej. la presencia de la propiedad `id`).

### Gestión de Variables de Entorno y Encadenamiento (`Bookpoint-Local.postman_environment.json`)
* **Interconectividad**: El archivo de entorno local define variables de host específicas para cada servicio (`ventasUrl`, `inventarioUrl`, `usuariosUrl`, etc.), apuntando a sus respectivos puertos locales (de `8081` a `8090`).
* **Encadenamiento de Flujos**: Para simular escenarios de integración reales de extremo a extremo sin intervención manual, los scripts de prueba interinterceptan los JSON devueltos y actualizan variables a nivel de colección:
  1. Al ejecutar `POST Registrar usuario`, el test extrae `json.id` y lo guarda en `usuarioId`.
  2. Al ejecutar `POST Crear sucursal` y `POST Registrar producto`, guarda `sucursalId` y `productoId`.
  3. `POST Registrar venta` lee estas variables para construir el payload, realiza la venta y almacena la salida en `folioVenta` e `ventaId`.
  4. Los endpoints subsiguientes (como la facturación) consumen de forma inmediata `{{folioVenta}}` para completar el flujo operativo de forma transparente.

---

## 4. Conclusiones Arquitectónicas y Operativas

Basado estrictamente en el análisis de código del repositorio de BookPoint, se deducen las siguientes conclusiones del sistema:

* **Desacoplamiento de Datos e Independencia**: La arquitectura cumple de manera rigurosa con el principio de "base de datos por microservicio". Al no existir consultas cruzadas en base de datos (*cross-database joins*) ni acoplamiento de almacenamiento, los servicios son completamente autónomos en términos de esquema, pudiendo modificarse y escalarse de forma aislada.
* **Compromiso en Compilación (Error Crítico Detectado)**:
  * Los componentes `VentaServiceImpl.java` (en `ms-ventas`) e `InventarioServiceImpl.java` (en `ms-inventario`) realizan importaciones explícitas de una clase de configuración local:
    * `import cl.bookpointchile.ventas.config.RabbitMQConfig;`
    * `import cl.bookpointchile.inventario.config.RabbitMQConfig;`
  * Sin embargo, el paquete `config` y el archivo `RabbitMQConfig.java` **no existen físicamente** en las carpetas de código de ninguno de los dos microservicios. Este hecho generará fallos críticos de compilación ("Cannot find symbol / Package does not exist") al intentar construir el proyecto mediante Maven.
* **Mensajería Asíncrona Inoperativa**:
  * Aunque `ms-ventas` publica eventos `VentaCreadaEvent` y `ms-inventario` tiene programada una firma para procesarlos (`procesarVentaCreada(VentaCreadaEvent event)`), **no existe ningún oyente `@RabbitListener` o componente suscriptor implementado en la plataforma**. 
  * Los eventos generados por las ventas quedarán acumulados en el broker de mensajería (si existe el exchange de RabbitMQ configurado de manera externa) sin ser nunca consumidos, impidiendo la actualización de inventarios y rompiendo el flujo de consistencia eventual.
* **Tolerancia a Fallos y Redundancia**: El uso de OpenFeign con la integración nativa de disyuntores (`Resilience4j`) proporciona una adecuada tolerancia a fallos ante peticiones síncronas bloqueantes, aislando el comportamiento de `ms-ventas` en caso de indisponibilidad temporal del servicio de facturación (`ms-facturacion`).
