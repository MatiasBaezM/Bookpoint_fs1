package cl.bookpointchile.proveedores.service;

import cl.bookpointchile.proveedores.dto.CrearProveedorRequestDTO;
import cl.bookpointchile.proveedores.dto.OrdenCompraRequestDTO;
import cl.bookpointchile.proveedores.dto.OrdenCompraResponseDTO;
import cl.bookpointchile.proveedores.dto.ProveedorResponseDTO;

import java.util.List;

public interface ProveedoresService {
    ProveedorResponseDTO registrarProveedor(CrearProveedorRequestDTO request);
    List<ProveedorResponseDTO> obtenerTodosProveedores();
    OrdenCompraResponseDTO emitirOrdenCompra(OrdenCompraRequestDTO request);
    OrdenCompraResponseDTO registrarRecepcionMercaderia(Long id);
}
