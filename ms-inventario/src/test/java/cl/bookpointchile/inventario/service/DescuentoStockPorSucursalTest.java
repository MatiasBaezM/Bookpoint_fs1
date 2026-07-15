package cl.bookpointchile.inventario.service;

import cl.bookpointchile.inventario.client.SucursalesClient;
import cl.bookpointchile.inventario.dto.DescontarStockRequestDTO;
import cl.bookpointchile.inventario.dto.DetalleStockRequestDTO;
import cl.bookpointchile.inventario.dto.StockResponseDTO;
import cl.bookpointchile.inventario.dto.SucursalMaestraResponseDTO;
import cl.bookpointchile.inventario.exception.StockInsuficienteException;
import cl.bookpointchile.inventario.model.Inventario;
import cl.bookpointchile.inventario.repository.InventarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Verificación end-to-end contra una base de datos real (H2) y el repositorio real:
// comprueba que comprar en una sucursal descuenta únicamente el stock de esa sucursal.
@SpringBootTest
@ActiveProfiles("test")
class DescuentoStockPorSucursalTest {

    @Autowired private InventarioService inventarioService;
    @Autowired private InventarioRepository inventarioRepository;

    @SuppressWarnings("removal")
    @MockitoBean private SucursalesClient sucursalesClient;

    private static final Long PRODUCTO = 500L;
    private static final Long CONCEPCION = 1L;
    private static final Long TEMUCO = 2L;

    @BeforeEach
    void setUp() {
        inventarioRepository.deleteAll();

        Mockito.when(sucursalesClient.obtenerPorId(Mockito.anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return SucursalMaestraResponseDTO.builder()
                    .id(id).nombre("Sucursal " + id).estadoOperativo("ACTIVO").build();
        });

        inventarioRepository.saveAll(List.of(
                Inventario.builder().productoId(PRODUCTO).productoNombre("Libro Demo")
                        .sku("SKU-500-1").cantidad(10).stockMinimo(2).sucursalId(CONCEPCION).build(),
                Inventario.builder().productoId(PRODUCTO).productoNombre("Libro Demo")
                        .sku("SKU-500-2").cantidad(4).stockMinimo(2).sucursalId(TEMUCO).build()));
    }

    private DescontarStockRequestDTO compra(Long sucursalId, int cantidad) {
        return DescontarStockRequestDTO.builder()
                .ventaId(1L).folio("BP-PRE-IT01").sucursalId(sucursalId).usuarioId(7L)
                .detalles(List.of(DetalleStockRequestDTO.builder().productoId(PRODUCTO).cantidad(cantidad).build()))
                .build();
    }

    private int stockDe(Long sucursalId) {
        return inventarioRepository.findByProductoIdAndSucursalId(PRODUCTO, sucursalId)
                .orElseThrow().getCantidad();
    }

    @Test
    void comprarEnConcepcion_descuentaSoloConcepcion() {
        inventarioService.descontarStockVenta(compra(CONCEPCION, 3));

        assertEquals(7, stockDe(CONCEPCION)); // 10 - 3
        assertEquals(4, stockDe(TEMUCO));     // intacto
    }

    @Test
    void comprarEnTemuco_descuentaSoloTemuco() {
        inventarioService.descontarStockVenta(compra(TEMUCO, 3));

        assertEquals(10, stockDe(CONCEPCION)); // intacto
        assertEquals(1, stockDe(TEMUCO));      // 4 - 3
    }

    @Test
    void comprarMasDeLoQueHayEnLaSucursal_falla_aunqueLaRedTengaSuficiente() {
        // Temuco tiene 4 y Concepción 10 (14 en total). Pedir 6 en Temuco debe fallar:
        // no se puede completar con el stock de otra sucursal.
        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.descontarStockVenta(compra(TEMUCO, 6)));

        assertEquals(10, stockDe(CONCEPCION));
        assertEquals(4, stockDe(TEMUCO));
    }

    @Test
    void verificarDisponibilidad_esRelativaALaSucursal() {
        StockResponseDTO enTemuco = inventarioService.verificarDisponibilidad(TEMUCO, PRODUCTO, 6);
        assertFalse(enTemuco.isDisponible());
        assertEquals(4, enTemuco.getStockActual());

        StockResponseDTO enConcepcion = inventarioService.verificarDisponibilidad(CONCEPCION, PRODUCTO, 6);
        assertTrue(enConcepcion.isDisponible());
        assertEquals(10, enConcepcion.getStockActual());
    }
}
