package cl.bookpointchile.sucursales.service;

import cl.bookpointchile.sucursales.dto.SucursalRequestDTO;
import cl.bookpointchile.sucursales.dto.SucursalResponseDTO;

import java.util.List;

public interface SucursalService {
    SucursalResponseDTO crearSucursal(SucursalRequestDTO request);
    SucursalResponseDTO obtenerPorId(Long id);
    List<SucursalResponseDTO> obtenerTodas();
    SucursalResponseDTO actualizarSucursal(Long id, SucursalRequestDTO request);
}
