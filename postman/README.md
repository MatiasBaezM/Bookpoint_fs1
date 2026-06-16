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

- **Test global** (nivel colección): valida que el status sea `2xx` y que el tiempo de respuesta sea < 3000 ms.
- **Tests por petición**: los `POST` de creación validan `201 Created` y guardan el `id` / `folio` devuelto en variables de colección (`productoId`, `sucursalId`, `folioVenta`, etc.) para encadenar las peticiones siguientes.

## Orden sugerido de ejecución (encadenado)

1. **ms-usuarios** → registrar usuario.
2. **ms-sucursales** → crear sucursal.
3. **ms-catalogo** → registrar producto.
4. **ms-inventario** → ajuste de stock (deja stock disponible).
5. **ms-ventas** → registrar venta (genera `folioVenta`).
6. **ms-facturacion** → emitir documento con ese folio.
7. **ms-promociones / ms-proveedores / ms-logistica / ms-bodega** → flujos independientes.

> Puedes usar el **Collection Runner** para ejecutar toda la colección o una carpeta de una sola vez.
