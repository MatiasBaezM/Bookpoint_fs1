package cl.bookpointchile.bodega.service;

import cl.bookpointchile.bodega.client.InventarioClient;
import cl.bookpointchile.bodega.client.VentasClient;
import cl.bookpointchile.bodega.dto.*;
import cl.bookpointchile.bodega.exception.EstadoPickingInvalidoException;
import cl.bookpointchile.bodega.exception.OrdenPickingNoEncontradaException;
import cl.bookpointchile.bodega.exception.StockInsuficienteException;
import cl.bookpointchile.bodega.model.OrdenPicking;
import cl.bookpointchile.bodega.model.UbicacionFisica;
import cl.bookpointchile.bodega.repository.OrdenPickingRepository;
import cl.bookpointchile.bodega.repository.UbicacionFisicaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BodegaServiceImplTest {

    @Mock
    private UbicacionFisicaRepository ubicacionRepository;
    @Mock
    private OrdenPickingRepository pickingRepository;
    @Mock
    private InventarioClient inventarioClient;
    @Mock
    private VentasClient ventasClient;

    @InjectMocks
    private BodegaServiceImpl bodegaService;

    private InventarioResponseDTO inventarioDisponible() {
        return InventarioResponseDTO.builder().productoId(1L).sucursalId(1L).cantidad(50).build();
    }

    private VentaResponseDTO ventaMock(Long id) {
        return VentaResponseDTO.builder().id(id).folio("BP-ONL-1010").build();
    }

    // ---------- registrarUbicacion ----------

    @Test
    void registrarUbicacion_guardaYRetorna() {
        UbicacionRequestDTO request = UbicacionRequestDTO.builder()
                .pasillo("A").estante("03").nivel("2").codigoBarras("7801234567890").build();
        when(ubicacionRepository.existsByCodigoBarras("7801234567890")).thenReturn(false);
        when(ubicacionRepository.save(any(UbicacionFisica.class))).thenAnswer(inv -> {
            UbicacionFisica u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UbicacionResponseDTO response = bodegaService.registrarUbicacion(request);

        assertEquals(1L, response.getId());
        assertEquals("7801234567890", response.getCodigoBarras());
    }

    @Test
    void registrarUbicacionDuplicada_lanzaExcepcion() {
        UbicacionRequestDTO request = UbicacionRequestDTO.builder()
                .pasillo("A").estante("03").nivel("2").codigoBarras("DUP123").build();
        when(ubicacionRepository.existsByCodigoBarras("DUP123")).thenReturn(true);

        assertThrows(EstadoPickingInvalidoException.class,
                () -> bodegaService.registrarUbicacion(request));
        verify(ubicacionRepository, never()).save(any());
    }

    // ---------- crearOrdenPicking ----------

    @Test
    void crearOrdenPicking_conStock_guardaOrden() {
        CrearOrdenPickingRequestDTO request = CrearOrdenPickingRequestDTO.builder()
                .ventaId(100L).sucursalId(1L).productoId(1L).cantidad(2).operarioAsignado("Juan Pérez").build();
        when(ventasClient.obtenerVentaPorId(100L)).thenReturn(ventaMock(100L));
        when(pickingRepository.existsByVentaId(100L)).thenReturn(false);
        when(inventarioClient.obtenerStock(1L, 1L)).thenReturn(inventarioDisponible());
        when(pickingRepository.save(any(OrdenPicking.class))).thenAnswer(inv -> {
            OrdenPicking o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        OrdenPickingResponseDTO response = bodegaService.crearOrdenPicking(request);

        assertEquals("PENDIENTE", response.getEstado());
        assertEquals(100L, response.getVentaId());
        assertEquals(1L, response.getSucursalId());
    }

    @Test
    void crearOrdenPickingVentaInexistente_lanzaExcepcion() {
        CrearOrdenPickingRequestDTO request = CrearOrdenPickingRequestDTO.builder()
                .ventaId(100L).sucursalId(1L).productoId(1L).cantidad(2).operarioAsignado("Juan").build();
        when(ventasClient.obtenerVentaPorId(100L)).thenThrow(new RuntimeException("Not found"));

        assertThrows(EstadoPickingInvalidoException.class,
                () -> bodegaService.crearOrdenPicking(request));
        verify(pickingRepository, never()).save(any());
    }

    @Test
    void crearOrdenPickingVentaDuplicada_lanzaExcepcion() {
        CrearOrdenPickingRequestDTO request = CrearOrdenPickingRequestDTO.builder()
                .ventaId(100L).sucursalId(1L).productoId(1L).cantidad(2).operarioAsignado("Juan").build();
        when(ventasClient.obtenerVentaPorId(100L)).thenReturn(ventaMock(100L));
        when(pickingRepository.existsByVentaId(100L)).thenReturn(true);

        assertThrows(EstadoPickingInvalidoException.class,
                () -> bodegaService.crearOrdenPicking(request));
        verify(pickingRepository, never()).save(any());
    }

    @Test
    void crearOrdenPickingSinStock_lanzaStockInsuficiente() {
        CrearOrdenPickingRequestDTO request = CrearOrdenPickingRequestDTO.builder()
                .ventaId(100L).sucursalId(1L).productoId(1L).cantidad(99).operarioAsignado("Juan").build();
        when(ventasClient.obtenerVentaPorId(100L)).thenReturn(ventaMock(100L));
        when(pickingRepository.existsByVentaId(100L)).thenReturn(false);
        when(inventarioClient.obtenerStock(1L, 1L))
                .thenReturn(InventarioResponseDTO.builder().productoId(1L).sucursalId(1L).cantidad(2).build());

        assertThrows(StockInsuficienteException.class,
                () -> bodegaService.crearOrdenPicking(request));
        verify(pickingRepository, never()).save(any());
    }

    // ---------- actualizarEstadoPicking ----------

    @Test
    void actualizarEstadoPickingPendienteAEnProceso_actualiza() {
        OrdenPicking orden = OrdenPicking.builder().id(1L).ventaId(100L).sucursalId(1L).estado("PENDIENTE").build();
        when(pickingRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(pickingRepository.save(any(OrdenPicking.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdenPickingResponseDTO response = bodegaService.actualizarEstadoPicking(1L, "EN_PROCESO");

        assertEquals("EN_PROCESO", response.getEstado());
    }

    @Test
    void actualizarEstadoPickingEnProcesoACompletada_actualizaYDescuenta() {
        OrdenPicking orden = OrdenPicking.builder().id(1L).ventaId(100L).sucursalId(1L).productoId(5L).cantidad(3).estado("EN_PROCESO").build();
        when(pickingRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(pickingRepository.save(any(OrdenPicking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventarioClient.registrarAjuste(any())).thenReturn(InventarioResponseDTO.builder().build());

        OrdenPickingResponseDTO response = bodegaService.actualizarEstadoPicking(1L, "COMPLETADA");

        assertEquals("COMPLETADA", response.getEstado());
        verify(inventarioClient, times(1)).registrarAjuste(any());
    }

    @Test
    void actualizarEstadoPickingTransicionInvalida_lanzaExcepcion() {
        OrdenPicking orden = OrdenPicking.builder().id(1L).estado("PENDIENTE").build();
        when(pickingRepository.findById(1L)).thenReturn(Optional.of(orden));

        // PENDIENTE no puede saltar directo a COMPLETADA
        assertThrows(EstadoPickingInvalidoException.class,
                () -> bodegaService.actualizarEstadoPicking(1L, "COMPLETADA"));
    }

    @Test
    void actualizarEstadoPickingEstadoInexistente_lanzaExcepcion() {
        OrdenPicking orden = OrdenPicking.builder().id(1L).estado("PENDIENTE").build();
        when(pickingRepository.findById(1L)).thenReturn(Optional.of(orden));

        assertThrows(EstadoPickingInvalidoException.class,
                () -> bodegaService.actualizarEstadoPicking(1L, "VOLANDO"));
    }

    @Test
    void actualizarEstadoPickingOrdenInexistente_lanzaNoEncontrada() {
        when(pickingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrdenPickingNoEncontradaException.class,
                () -> bodegaService.actualizarEstadoPicking(99L, "EN_PROCESO"));
    }

    // ---------- listados ----------

    @Test
    void obtenerUbicaciones_retornaLista() {
        when(ubicacionRepository.findAll()).thenReturn(java.util.List.of(
                UbicacionFisica.builder().id(1L).codigoBarras("ABC").build()));

        assertEquals(1, bodegaService.obtenerUbicaciones().size());
        verify(ubicacionRepository, times(1)).findAll();
    }

    @Test
    void obtenerOrdenesPicking_retornaLista() {
        when(pickingRepository.findAll()).thenReturn(java.util.List.of(
                OrdenPicking.builder().id(1L).estado("PENDIENTE").build()));

        assertEquals(1, bodegaService.obtenerOrdenesPicking().size());
        verify(pickingRepository, times(1)).findAll();
    }
}
