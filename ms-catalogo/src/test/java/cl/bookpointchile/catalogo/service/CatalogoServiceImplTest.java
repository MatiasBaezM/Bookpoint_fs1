package cl.bookpointchile.catalogo.service;

import cl.bookpointchile.catalogo.dto.*;
import cl.bookpointchile.catalogo.exception.ProductoNoEncontradoException;
import cl.bookpointchile.catalogo.model.Producto;
import cl.bookpointchile.catalogo.model.Resena;
import cl.bookpointchile.catalogo.repository.ProductoRepository;
import cl.bookpointchile.catalogo.repository.ResenaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ResenaRepository resenaRepository;

    @InjectMocks
    private CatalogoServiceImpl catalogoService;

    private Producto producto(Long id) {
        return Producto.builder()
                .id(id).titulo("Cien años de soledad").autor("García Márquez")
                .editorial("Sudamericana").precio(new BigDecimal("12990")).categoria("Novela")
                .build();
    }

    // ---------- buscarProductosConFiltros ----------

    @Test
    void buscarProductosConFiltros_retornaPagina() {
        Page<Producto> page = new PageImpl<>(List.of(producto(1L)));
        when(productoRepository.buscarConFiltros(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        Page<ProductoResponseDTO> response = catalogoService.buscarProductosConFiltros(
                null, null, "Novela", null, null, 0, 10);

        assertEquals(1, response.getTotalElements());
        assertEquals("Cien años de soledad", response.getContent().get(0).getTitulo());
    }

    // ---------- obtenerProductoPorId ----------

    @Test
    void obtenerProductoPorIdExistente_retornaProducto() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L)));

        ProductoResponseDTO response = catalogoService.obtenerProductoPorId(1L);

        assertEquals(1L, response.getId());
        assertEquals(0.0, response.getCalificacionPromedio());
        assertEquals(0, response.getTotalResenas());
    }

    @Test
    void obtenerProductoPorIdInexistente_lanzaNoEncontrado() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductoNoEncontradoException.class,
                () -> catalogoService.obtenerProductoPorId(99L));
    }

    // ---------- registrarProducto ----------

    @Test
    void registrarProducto_guardaYRetorna() {
        ProductoRegistroRequestDTO request = ProductoRegistroRequestDTO.builder()
                .titulo("Rayuela").autor("Cortázar").editorial("Alfaguara")
                .precio(new BigDecimal("9990")).categoria("Novela").build();
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(5L);
            return p;
        });

        ProductoResponseDTO response = catalogoService.registrarProducto(request);

        assertEquals(5L, response.getId());
        assertEquals("Rayuela", response.getTitulo());
    }

    // ---------- agregarResena ----------

    @Test
    void agregarResenaProductoExistente_guardaResena() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L)));
        when(resenaRepository.save(any(Resena.class))).thenAnswer(inv -> {
            Resena r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });
        ResenaRequestDTO request = ResenaRequestDTO.builder()
                .usuarioId(1L).usuarioNombre("Camila").calificacion(5).comentario("Excelente").build();

        ResenaResponseDTO response = catalogoService.agregarResena(1L, request);

        assertEquals(10L, response.getId());
        assertEquals(5, response.getCalificacion());
        verify(resenaRepository, times(1)).save(any(Resena.class));
    }

    @Test
    void agregarResenaProductoInexistente_lanzaNoEncontrado() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());
        ResenaRequestDTO request = ResenaRequestDTO.builder()
                .usuarioId(1L).usuarioNombre("Camila").calificacion(5).comentario("Top").build();

        assertThrows(ProductoNoEncontradoException.class,
                () -> catalogoService.agregarResena(99L, request));
        verify(resenaRepository, never()).save(any());
    }

    @Test
    void agregarResenaConCalificacionInvalida_lanzaIllegalArgument() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto(1L)));
        ResenaRequestDTO request = ResenaRequestDTO.builder()
                .usuarioId(1L).usuarioNombre("Camila").calificacion(9).comentario("Fuera de rango").build();

        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.agregarResena(1L, request));
    }
}
