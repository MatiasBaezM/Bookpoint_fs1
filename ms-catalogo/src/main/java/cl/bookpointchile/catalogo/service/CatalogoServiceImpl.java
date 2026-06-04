package cl.bookpointchile.catalogo.service;

import cl.bookpointchile.catalogo.dto.*;
import cl.bookpointchile.catalogo.exception.ProductoNoEncontradoException;
import cl.bookpointchile.catalogo.model.Producto;
import cl.bookpointchile.catalogo.model.Resena;
import cl.bookpointchile.catalogo.repository.ProductoRepository;
import cl.bookpointchile.catalogo.repository.ResenaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final ProductoRepository productoRepository;
    private final ResenaRepository resenaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResponseDTO> buscarProductosConFiltros(
            String autor, String editorial, String categoria, 
            BigDecimal precioMin, BigDecimal precioMax, int page, int size) {
            
        log.info("Buscando productos con filtros. Autor: {}, Editorial: {}, Categoría: {}, Rango Precios: [{} - {}], Pág: {}, Cantidad: {}",
                autor, editorial, categoria, precioMin, precioMax, page, size);

        Pageable pageable = PageRequest.of(page, size);
        
        // Ejecutar consulta filtrada en la base de datos con paginación
        Page<Producto> productosPage = productoRepository.buscarConFiltros(
                autor != null && !autor.trim().isEmpty() ? autor.trim() : null,
                editorial != null && !editorial.trim().isEmpty() ? editorial.trim() : null,
                categoria != null && !categoria.trim().isEmpty() ? categoria.trim() : null,
                precioMin,
                precioMax,
                pageable
        );

        log.info("Búsqueda completada. Total de productos encontrados: {}", productosPage.getTotalElements());
        return productosPage.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO obtenerProductoPorId(Long id) {
        log.info("Obteniendo producto por ID: {}", id);
        
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Búsqueda fallida: Producto con ID {} no encontrado.", id);
                    return new ProductoNoEncontradoException("El producto con ID " + id + " no fue encontrado en el catálogo.");
                });

        return mapToResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponseDTO registrarProducto(ProductoRegistroRequestDTO request) {
        log.info("Registrando nuevo producto en el catálogo: '{}' de '{}'", request.getTitulo(), request.getAutor());

        Producto producto = Producto.builder()
                .titulo(request.getTitulo())
                .autor(request.getAutor())
                .editorial(request.getEditorial())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .descripcion(request.getDescripcion())
                .build();

        Producto saved = productoRepository.save(producto);
        log.info("Producto guardado con éxito. ID asignado: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ResenaResponseDTO agregarResena(Long productoId, ResenaRequestDTO request) {
        log.info("Agregando reseña para Producto ID: {}. Calificación: {} estrellas, por Usuario: '{}'",
                productoId, request.getCalificacion(), request.getUsuarioNombre());

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> {
                    log.warn("Intento de reseña fallido: Producto ID {} no existe.", productoId);
                    return new ProductoNoEncontradoException("No se puede agregar reseña. El producto con ID " + productoId + " no existe.");
                });

        // Doble validación de rango de estrellas en capa de servicio (buenas prácticas senior)
        if (request.getCalificacion() < 1 || request.getCalificacion() > 5) {
            log.warn("Intento de reseña fallido: Calificación inválida {} de 5.", request.getCalificacion());
            throw new IllegalArgumentException("La calificación debe estar estrictamente entre 1 y 5 estrellas.");
        }

        Resena resena = Resena.builder()
                .usuarioId(request.getUsuarioId())
                .usuarioNombre(request.getUsuarioNombre())
                .calificacion(request.getCalificacion())
                .comentario(request.getComentario())
                .build();

        // Vincular bidireccionalmente usando el helper del modelo
        producto.addResena(resena);
        
        Resena savedResena = resenaRepository.save(resena);
        log.info("Reseña guardada con éxito con ID: {} para producto '{}'", savedResena.getId(), producto.getTitulo());

        return ResenaResponseDTO.builder()
                .id(savedResena.getId())
                .usuarioId(savedResena.getUsuarioId())
                .usuarioNombre(savedResena.getUsuarioNombre())
                .calificacion(savedResena.getCalificacion())
                .comentario(savedResena.getComentario())
                .build();
    }

    // Helper manual de mapeo y cálculo dinámico de estrellas promedio
    private ProductoResponseDTO mapToResponse(Producto p) {
        // Cálculo del promedio de calificaciones de reseñas
        double promedio = p.getResenas().stream()
                .mapToInt(Resena::getCalificacion)
                .average()
                .orElse(0.0);

        // Redondear a 1 decimal
        BigDecimal bd = BigDecimal.valueOf(promedio).setScale(1, RoundingMode.HALF_UP);
        double calificacionPromedio = bd.doubleValue();

        List<ResenaResponseDTO> resenaDTOs = p.getResenas().stream()
                .map(r -> ResenaResponseDTO.builder()
                        .id(r.getId())
                        .usuarioId(r.getUsuarioId())
                        .usuarioNombre(r.getUsuarioNombre())
                        .calificacion(r.getCalificacion())
                        .comentario(r.getComentario())
                        .build())
                .collect(Collectors.toList());

        return ProductoResponseDTO.builder()
                .id(p.getId())
                .titulo(p.getTitulo())
                .autor(p.getAutor())
                .editorial(p.getEditorial())
                .precio(p.getPrecio())
                .descripcion(p.getDescripcion())
                .categoria(p.getCategoria())
                .calificacionPromedio(calificacionPromedio)
                .totalResenas(p.getResenas().size())
                .resenas(resenaDTOs)
                .build();
    }
}
