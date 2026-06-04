package cl.bookpointchile.catalogo.service;

import cl.bookpointchile.catalogo.dto.ProductoRegistroRequestDTO;
import cl.bookpointchile.catalogo.dto.ProductoResponseDTO;
import cl.bookpointchile.catalogo.dto.ResenaRequestDTO;
import cl.bookpointchile.catalogo.dto.ResenaResponseDTO;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface CatalogoService {
    Page<ProductoResponseDTO> buscarProductosConFiltros(
            String autor, String editorial, String categoria, 
            BigDecimal precioMin, BigDecimal precioMax, int page, int size);
            
    ProductoResponseDTO obtenerProductoPorId(Long id);
    ProductoResponseDTO registrarProducto(ProductoRegistroRequestDTO request);
    ResenaResponseDTO agregarResena(Long productoId, ResenaRequestDTO request);
}
