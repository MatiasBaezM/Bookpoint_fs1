package cl.bookpointchile.bodega.service;

import cl.bookpointchile.bodega.dto.CrearOrdenPickingRequestDTO;
import cl.bookpointchile.bodega.dto.OrdenPickingResponseDTO;
import cl.bookpointchile.bodega.dto.UbicacionRequestDTO;
import cl.bookpointchile.bodega.dto.UbicacionResponseDTO;

import java.util.List;

public interface BodegaService {
    UbicacionResponseDTO registrarUbicacion(UbicacionRequestDTO request);
    OrdenPickingResponseDTO crearOrdenPicking(CrearOrdenPickingRequestDTO request);
    OrdenPickingResponseDTO actualizarEstadoPicking(Long id, String nuevoEstado);
    List<UbicacionResponseDTO> obtenerUbicaciones();
    List<OrdenPickingResponseDTO> obtenerOrdenesPicking();
}
