package cl.bookpointchile.promociones.service;

import cl.bookpointchile.promociones.dto.CrearPromocionRequestDTO;
import cl.bookpointchile.promociones.dto.PromocionResponseDTO;

import java.util.List;

public interface PromocionesService {
    PromocionResponseDTO registrarPromocion(CrearPromocionRequestDTO request);
    List<PromocionResponseDTO> obtenerTodas();
    PromocionResponseDTO validarPromocion(String codigo);
}
