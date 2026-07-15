package cl.bookpointchile.inventario.service;

import cl.bookpointchile.inventario.dto.*;

import java.util.List;

public interface InventarioService {
    InventarioResponseDTO registrarAjusteFisico(AjusteStockRequestDTO request);
    InventarioResponseDTO trasladarStock(TrasladoStockRequestDTO request);
    InventarioResponseDTO obtenerStock(Long sucursalId, Long productoId);
    List<InventarioResponseDTO> obtenerStockPorSucursal(Long sucursalId);
    List<InventarioResponseDTO> obtenerAlertasReposicion();
    StockResponseDTO verificarDisponibilidad(Long sucursalId, Long productoId, Integer cantidad);

    // Descuenta, de forma síncrona y atómica, el stock de la sucursal indicada.
    // Lanza StockInsuficienteException si algún detalle no alcanza, dejando el inventario
    // intacto (rollback de la transacción).
    void descontarStockVenta(DescontarStockRequestDTO request);
}
