package cl.bookpointchile.inventario.service;

import cl.bookpointchile.inventario.dto.*;

import java.util.List;

public interface InventarioService {
    InventarioResponseDTO registrarAjusteFisico(AjusteStockRequestDTO request);
    InventarioResponseDTO trasladarStock(TrasladoStockRequestDTO request);
    InventarioResponseDTO obtenerStock(Long sucursalId, Long productoId);
    List<InventarioResponseDTO> obtenerStockPorSucursal(Long sucursalId);
    List<InventarioResponseDTO> obtenerAlertasReposicion();
    StockResponseDTO verificarDisponibilidad(Long productoId, Integer cantidad);
    void procesarVentaCreada(cl.bookpointchile.inventario.event.VentaCreadaEvent event);
}
