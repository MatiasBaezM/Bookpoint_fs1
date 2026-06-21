package cl.bookpointchile.inventario.service;

import cl.bookpointchile.inventario.client.SucursalesClient;
import cl.bookpointchile.inventario.config.RabbitMQConfig;
import cl.bookpointchile.inventario.dto.*;
import cl.bookpointchile.inventario.event.DetalleVentaEvent;
import cl.bookpointchile.inventario.event.StockRechazadoEvent;
import cl.bookpointchile.inventario.event.StockReservadoEvent;
import cl.bookpointchile.inventario.event.VentaCreadaEvent;
import cl.bookpointchile.inventario.exception.ResourceNotFoundException;
import cl.bookpointchile.inventario.exception.StockInsuficienteException;
import cl.bookpointchile.inventario.exception.SucursalNoEncontradaException;
import cl.bookpointchile.inventario.model.Inventario;
import cl.bookpointchile.inventario.model.Sucursal;
import cl.bookpointchile.inventario.repository.InventarioRepository;
import cl.bookpointchile.inventario.repository.SucursalRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    @Mock private InventarioRepository inventarioRepository;
    @Mock private SucursalRepository sucursalRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private SucursalesClient sucursalesClient;

    @InjectMocks
    private InventarioServiceImpl inventarioService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Sucursal sucursal(Long id) {
        return Sucursal.builder().id(id).nombre("Sucursal " + id).direccion("Av. 1").build();
    }

    private Inventario inventario(Long id, int cantidad, int stockMinimo, Sucursal s) {
        return Inventario.builder()
                .id(id).productoId(1L).productoNombre("Libro").sku("SKU-1-" + s.getId())
                .cantidad(cantidad).stockMinimo(stockMinimo).sucursal(s).build();
    }

    private SucursalMaestraResponseDTO sucursalActiva(Long id) {
        return SucursalMaestraResponseDTO.builder()
                .id(id).nombre("Sucursal " + id).estadoOperativo("ACTIVO").build();
    }

    // ── registrarAjusteFisico ────────────────────────────────────────────────

    @Test
    void registrarAjusteCreaNuevoInventario_cuandoNoExiste() {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).sucursalId(1L).cantidadAjuste(50).motivo("Reposición").build();
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(sucursal(1L)));
        when(sucursalesClient.obtenerPorId(1L)).thenReturn(sucursalActiva(1L));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L)).thenReturn(Optional.empty());
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(inv -> {
            Inventario i = inv.getArgument(0);
            i.setId(10L);
            return i;
        });

        InventarioResponseDTO response = inventarioService.registrarAjusteFisico(request);

        assertEquals(50, response.getCantidad());
        assertEquals(1L, response.getSucursalId());
    }

    @Test
    void registrarAjusteNegativoSinInventarioPrevio_lanzaStockInsuficiente() {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).sucursalId(1L).cantidadAjuste(-5).motivo("Merma").build();
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(sucursal(1L)));
        when(sucursalesClient.obtenerPorId(1L)).thenReturn(sucursalActiva(1L));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.registrarAjusteFisico(request));
        verify(inventarioRepository, never()).save(any());
    }

    @Test
    void registrarAjusteSucursalInexistente_lanzaSucursalNoEncontrada() {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).sucursalId(99L).cantidadAjuste(5).motivo("X").build();
        when(sucursalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SucursalNoEncontradaException.class,
                () -> inventarioService.registrarAjusteFisico(request));
    }

    @Test
    void registrarAjusteQueDejaStockNegativo_lanzaStockInsuficiente() {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).sucursalId(1L).cantidadAjuste(-100).motivo("Merma").build();
        Sucursal s = sucursal(1L);
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(s));
        when(sucursalesClient.obtenerPorId(1L)).thenReturn(sucursalActiva(1L));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L))
                .thenReturn(Optional.of(inventario(10L, 10, 5, s)));

        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.registrarAjusteFisico(request));
    }

    @Test
    void registrarAjusteSucursalInactiva_lanzaSucursalNoEncontrada() {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).sucursalId(1L).cantidadAjuste(10).motivo("X").build();
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(sucursal(1L)));
        when(sucursalesClient.obtenerPorId(1L)).thenReturn(
                SucursalMaestraResponseDTO.builder().id(1L).estadoOperativo("INACTIVO").build());

        assertThrows(SucursalNoEncontradaException.class,
                () -> inventarioService.registrarAjusteFisico(request));
    }

    // ── trasladarStock ───────────────────────────────────────────────────────

    @Test
    void trasladarStockOrigenIgualDestino_lanzaStockInsuficiente() {
        TrasladoStockRequestDTO request = TrasladoStockRequestDTO.builder()
                .productoId(1L).sucursalOrigenId(1L).sucursalDestinoId(1L).cantidad(5).build();

        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.trasladarStock(request));
    }

    @Test
    void trasladarStockOrigenNoEncontrado_lanzaSucursalNoEncontrada() {
        TrasladoStockRequestDTO request = TrasladoStockRequestDTO.builder()
                .productoId(1L).sucursalOrigenId(99L).sucursalDestinoId(2L).cantidad(5).build();
        when(sucursalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SucursalNoEncontradaException.class,
                () -> inventarioService.trasladarStock(request));
    }

    @Test
    void trasladarStockProductoSinStockEnOrigen_lanzaStockInsuficiente() {
        TrasladoStockRequestDTO request = TrasladoStockRequestDTO.builder()
                .productoId(1L).sucursalOrigenId(1L).sucursalDestinoId(2L).cantidad(5).build();
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(sucursal(1L)));
        when(sucursalRepository.findById(2L)).thenReturn(Optional.of(sucursal(2L)));
        when(sucursalesClient.obtenerPorId(1L)).thenReturn(sucursalActiva(1L));
        when(sucursalesClient.obtenerPorId(2L)).thenReturn(sucursalActiva(2L));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.trasladarStock(request));
    }

    @Test
    void trasladarStockInsuficienteEnOrigen_lanzaStockInsuficiente() {
        TrasladoStockRequestDTO request = TrasladoStockRequestDTO.builder()
                .productoId(1L).sucursalOrigenId(1L).sucursalDestinoId(2L).cantidad(50).build();
        Sucursal origen = sucursal(1L);
        Sucursal destino = sucursal(2L);
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(origen));
        when(sucursalRepository.findById(2L)).thenReturn(Optional.of(destino));
        when(sucursalesClient.obtenerPorId(1L)).thenReturn(sucursalActiva(1L));
        when(sucursalesClient.obtenerPorId(2L)).thenReturn(sucursalActiva(2L));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L))
                .thenReturn(Optional.of(inventario(10L, 5, 2, origen)));

        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.trasladarStock(request));
    }

    @Test
    void trasladarStock_exitoso_actualizaDestinoExistente() {
        TrasladoStockRequestDTO request = TrasladoStockRequestDTO.builder()
                .productoId(1L).sucursalOrigenId(1L).sucursalDestinoId(2L).cantidad(5).build();
        Sucursal origen = sucursal(1L);
        Sucursal destino = sucursal(2L);
        Inventario invOrigen = inventario(10L, 20, 2, origen);
        Inventario invDestino = inventario(11L, 3, 2, destino);

        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(origen));
        when(sucursalRepository.findById(2L)).thenReturn(Optional.of(destino));
        when(sucursalesClient.obtenerPorId(1L)).thenReturn(sucursalActiva(1L));
        when(sucursalesClient.obtenerPorId(2L)).thenReturn(sucursalActiva(2L));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L)).thenReturn(Optional.of(invOrigen));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 2L)).thenReturn(Optional.of(invDestino));
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(inv -> inv.getArgument(0));

        InventarioResponseDTO response = inventarioService.trasladarStock(request);

        assertEquals(8, response.getCantidad()); // 3 + 5
        assertEquals(15, invOrigen.getCantidad()); // 20 - 5
        verify(inventarioRepository, times(2)).save(any(Inventario.class));
    }

    @Test
    void trasladarStock_exitoso_creaInventarioEnDestino() {
        TrasladoStockRequestDTO request = TrasladoStockRequestDTO.builder()
                .productoId(1L).sucursalOrigenId(1L).sucursalDestinoId(2L).cantidad(5).build();
        Sucursal origen = sucursal(1L);
        Sucursal destino = sucursal(2L);
        Inventario invOrigen = inventario(10L, 20, 2, origen);

        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(origen));
        when(sucursalRepository.findById(2L)).thenReturn(Optional.of(destino));
        when(sucursalesClient.obtenerPorId(1L)).thenReturn(sucursalActiva(1L));
        when(sucursalesClient.obtenerPorId(2L)).thenReturn(sucursalActiva(2L));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L)).thenReturn(Optional.of(invOrigen));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 2L)).thenReturn(Optional.empty());
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(inv -> {
            Inventario i = inv.getArgument(0);
            if (i.getId() == null) i.setId(99L);
            return i;
        });

        InventarioResponseDTO response = inventarioService.trasladarStock(request);

        assertEquals(5, response.getCantidad());
        assertEquals(2L, response.getSucursalId());
        assertEquals(15, invOrigen.getCantidad()); // 20 - 5
    }

    // ── obtenerStock ─────────────────────────────────────────────────────────

    @Test
    void obtenerStockSucursalInexistente_lanzaSucursalNoEncontrada() {
        when(sucursalRepository.existsById(99L)).thenReturn(false);

        assertThrows(SucursalNoEncontradaException.class,
                () -> inventarioService.obtenerStock(99L, 1L));
    }

    @Test
    void obtenerStockProductoNoRegistrado_lanzaResourceNotFound() {
        when(sucursalRepository.existsById(1L)).thenReturn(true);
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> inventarioService.obtenerStock(1L, 1L));
    }

    @Test
    void obtenerStock_exitoso_retornaInventario() {
        Sucursal s = sucursal(1L);
        when(sucursalRepository.existsById(1L)).thenReturn(true);
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L))
                .thenReturn(Optional.of(inventario(10L, 30, 5, s)));

        InventarioResponseDTO response = inventarioService.obtenerStock(1L, 1L);

        assertEquals(30, response.getCantidad());
        assertEquals(1L, response.getSucursalId());
    }

    // ── obtenerStockPorSucursal ───────────────────────────────────────────────

    @Test
    void obtenerStockPorSucursalInexistente_lanzaSucursalNoEncontrada() {
        when(sucursalRepository.existsById(99L)).thenReturn(false);

        assertThrows(SucursalNoEncontradaException.class,
                () -> inventarioService.obtenerStockPorSucursal(99L));
    }

    @Test
    void obtenerStockPorSucursal_exitoso_retornaLista() {
        Sucursal s = sucursal(1L);
        when(sucursalRepository.existsById(1L)).thenReturn(true);
        when(inventarioRepository.findBySucursalId(1L))
                .thenReturn(List.of(inventario(10L, 20, 5, s), inventario(11L, 5, 3, s)));

        List<InventarioResponseDTO> response = inventarioService.obtenerStockPorSucursal(1L);

        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).getSucursalId());
    }

    // ── verificarDisponibilidad ───────────────────────────────────────────────

    @Test
    void verificarDisponibilidadConStockSuficiente_retornaDisponible() {
        Sucursal s = sucursal(1L);
        when(inventarioRepository.findAll()).thenReturn(List.of(inventario(10L, 50, 5, s)));

        StockResponseDTO response = inventarioService.verificarDisponibilidad(1L, 10);

        assertTrue(response.isDisponible());
        assertEquals(50, response.getStockActual());
    }

    @Test
    void verificarDisponibilidadConStockInsuficiente_retornaNoDisponible() {
        Sucursal s = sucursal(1L);
        when(inventarioRepository.findAll()).thenReturn(List.of(inventario(10L, 2, 5, s)));

        StockResponseDTO response = inventarioService.verificarDisponibilidad(1L, 10);

        assertFalse(response.isDisponible());
        assertEquals(2, response.getStockActual());
    }

    // ── obtenerAlertasReposicion ──────────────────────────────────────────────

    @Test
    void obtenerAlertasReposicion_retornaLista() {
        Sucursal s = sucursal(1L);
        when(inventarioRepository.findAlertasStock()).thenReturn(List.of(inventario(10L, 3, 5, s)));

        List<InventarioResponseDTO> response = inventarioService.obtenerAlertasReposicion();

        assertEquals(1, response.size());
        assertTrue(response.get(0).isAlertaReposicion());
    }

    // ── procesarVentaCreada ───────────────────────────────────────────────────

    @Test
    void procesarVentaCreada_stockSuficiente_descontaYPublicaReservado() {
        Sucursal s = sucursal(1L);
        Inventario inv = inventario(10L, 20, 5, s);

        VentaCreadaEvent event = VentaCreadaEvent.builder()
                .ventaId(1L).folio("BP-ONL-0001")
                .detalles(List.of(DetalleVentaEvent.builder().productoId(1L).cantidad(3).build()))
                .build();

        when(inventarioRepository.findAll()).thenReturn(List.of(inv));
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(i -> i.getArgument(0));

        inventarioService.procesarVentaCreada(event);

        assertEquals(17, inv.getCantidad()); // 20 - 3
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_VENTAS),
                eq(RabbitMQConfig.ROUTING_KEY_STOCK_RESERVADO),
                captor.capture());
        StockReservadoEvent reservado = (StockReservadoEvent) captor.getValue();
        assertEquals("BP-ONL-0001", reservado.getFolio());
        assertEquals(1L, reservado.getVentaId());
    }

    @Test
    void procesarVentaCreada_stockInsuficiente_publicaRechazado() {
        Sucursal s = sucursal(1L);
        Inventario inv = inventario(10L, 2, 5, s);

        VentaCreadaEvent event = VentaCreadaEvent.builder()
                .ventaId(2L).folio("BP-ONL-0002")
                .detalles(List.of(DetalleVentaEvent.builder().productoId(1L).cantidad(10).build()))
                .build();

        when(inventarioRepository.findAll()).thenReturn(List.of(inv));

        inventarioService.procesarVentaCreada(event);

        verify(inventarioRepository, never()).save(any());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_VENTAS),
                eq(RabbitMQConfig.ROUTING_KEY_STOCK_RECHAZADO),
                captor.capture());
        StockRechazadoEvent rechazado = (StockRechazadoEvent) captor.getValue();
        assertEquals("BP-ONL-0002", rechazado.getFolio());
        assertEquals(2L, rechazado.getVentaId());
    }
}
