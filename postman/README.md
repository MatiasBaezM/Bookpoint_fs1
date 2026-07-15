# Pruebas Postman — Bookpoint Chile

Colección de pruebas para los 10 microservicios de Bookpoint Chile.

## Archivos

- `Bookpoint.postman_collection.json` — Colección con todos los endpoints agrupados por microservicio.
- `Bookpoint-Local.postman_environment.json` — Environment con las URLs locales (`localhost`) y el puerto de cada servicio.

## Cómo importar

1. Abre Postman → **Import**.
2. Arrastra ambos archivos `.json`.
3. Arriba a la derecha, selecciona el environment **Bookpoint - Local**.

## Puertos por microservicio

| Microservicio    | Puerto | Variable          | Context path        |
|------------------|--------|-------------------|---------------------|
| ms-ventas        | 8081   | `{{ventasUrl}}`       | `/api/ventas`       |
| ms-inventario    | 8082   | `{{inventarioUrl}}`   | `/api/inventario`   |
| ms-usuarios      | 8083   | `{{usuariosUrl}}`     | `/api/usuarios`     |
| ms-catalogo      | 8084   | `{{catalogoUrl}}`     | `/api/catalogo`     |
| ms-logistica     | 8085   | `{{logisticaUrl}}`    | `/api/logistica`    |
| ms-proveedores   | 8086   | `{{proveedoresUrl}}`  | `/api/proveedores`  |
| ms-promociones   | 8087   | `{{promocionesUrl}}`  | `/api/promociones`  |
| ms-facturacion   | 8088   | `{{facturacionUrl}}`  | `/api/facturacion`  |
| ms-bodega        | 8089   | `{{bodegaUrl}}`       | `/api/bodega`       |
| ms-sucursales    | 8090   | `{{sucursalesUrl}}`   | `/api/sucursales`   |

## Tests incluidos

- **Test global** (nivel colección): valida que el tiempo de respuesta sea < 3000 ms. No valida un status `2xx` genérico porque varias peticiones prueban deliberadamente casos negativos (400/404/409).
- **Tests por petición**: cada petición valida explícitamente su status esperado (éxito o error). Los `POST` de creación además guardan el `id` / `folio` devuelto en variables de colección (`productoId`, `sucursalId`, `folioVenta`, etc.) para encadenar las peticiones siguientes.

## Carpeta "0) Flujo E2E - Stock por Sucursal y Facturación"

Es la carpeta más importante de la colección: encadena 16 peticiones para demostrar, de punta a punta, que:

1. El stock se descuenta **solo de la sucursal donde ocurre la venta** — comprar en la Sucursal B nunca "toma prestado" stock de la Sucursal A, aunque la red en conjunto tenga stock suficiente (paso 9, espera `400`).
2. `ms-ventas` descuenta el stock de forma **síncrona** llamando a `POST /api/inventario/descuento` en `ms-inventario`, y la respuesta de `POST /api/ventas` ya refleja el estado final (`COMPLETADA` o `RECHAZADA`) — no hay que esperar un mensaje asíncrono.
3. Al confirmarse la venta se **emite automáticamente** un documento tributario en `ms-facturacion`, ya ligado a `ventaId`, `usuarioId`, `sucursalId` y el detalle de productos (código = `productoId`).
4. Un segundo intento de emitir el mismo documento devuelve `409` (protección contra doble emisión).
5. El nuevo endpoint síncrono `POST /api/inventario/descuento` también se puede invocar directamente (no solo desde `ms-ventas`).

Usa variables propias (`e2eUsuarioId`, `e2eSucursalAId`, `e2eSucursalBId`, `e2eProductoId`, `e2eFolioVenta`, `e2eVentaId`) para no interferir con las demás carpetas. **Ejecutar sus peticiones en orden** (1 a 16), con el Collection Runner o una por una.

## Orden sugerido para el resto de la colección (encadenado)

1. **ms-usuarios** → registrar usuario.
2. **ms-sucursales** → crear sucursal.
3. **ms-catalogo** → registrar producto.
4. **ms-inventario** → ajuste de stock (deja stock disponible) — incluye el nuevo endpoint `POST /api/inventario/descuento`.
5. **ms-ventas** → registrar venta (genera `folioVenta` y `ventaId`; el documento tributario se emite automáticamente).
6. **ms-facturacion** → consultar el documento auto-emitido para ese folio.
7. **ms-promociones / ms-proveedores / ms-logistica** → flujos independientes.
8. **ms-bodega** → requiere que `ventaId` (de ms-ventas) y `sucursalId`/`productoId` (con stock ya cargado en ms-inventario) existan. La orden de picking sigue una máquina de estados estricta: `PENDIENTE → EN_PROCESO → COMPLETADA` (no se puede saltar directo a `COMPLETADA`).

> Puedes usar el **Collection Runner** para ejecutar toda la colección o una carpeta de una sola vez.
