package cl.bookpointchile.ventas.service;

import cl.bookpointchile.ventas.client.FacturacionClient;
import cl.bookpointchile.ventas.client.InventarioClient;
import cl.bookpointchile.ventas.client.PromocionClient;
import cl.bookpointchile.ventas.client.UsuarioClient;
import cl.bookpointchile.ventas.dto.*;
import cl.bookpointchile.ventas.exception.InsufficientStockException;
import cl.bookpointchile.ventas.exception.InvalidSaleException;
import cl.bookpointchile.ventas.exception.ResourceNotFoundException;
import cl.bookpointchile.ventas.event.VentaRegistradaInternaEvent;
import feign.FeignException;
import cl.bookpointchile.ventas.model.EstadoVenta;
import cl.bookpointchile.ventas.model.TipoDescuento;
import cl.bookpointchile.ventas.model.TipoVenta;
import cl.bookpointchile.ventas.model.Venta;
import cl.bookpointchile.ventas.repository.VentaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private InventarioClient inventarioClient;
    @Mock
    private PromocionClient promocionClient;
    @Mock
    private FacturacionClient facturacionClient;
    @Mock
    private UsuarioClient usuarioClient;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VentaServiceImpl ventaService;

    // ---------- Helpers ----------
    private DetalleVentaRequestDTO detalle(int cantidad, String precio) {
        return DetalleVentaRequestDTO.builder()
                .productoId(1L)
                .productoNombre("Cien años de soledad")
                .cantidad(cantidad)
                .precioUnitario(new BigDecimal(precio))
                .build();
    }

    private StockResponseDTO stockDisponible() {
        return StockResponseDTO.builder().productoId(1L).sucursalId(1L).disponible(true).stockActual(50).build();
    }

    // ---------- registrarVenta ----------

    @Test
    void registrarVentaPresencialSinDescuento_calculaTotalYGuarda() {
        // Given
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.PRESENCIAL)
                .sucursalId(1L)
                .clienteNombre("Camila Soto")
                .clienteRut("19876543-2")
                .asistenteNombre("Pedro Vega")
                .detalles(List.of(detalle(2, "12990")))
                .build();

        when(inventarioClient.checkStock(1L, 1L, 2)).thenReturn(stockDisponible());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        // When
        VentaResponseDTO response = ventaService.registrarVenta(request);

        // Then
        assertNotNull(response);
        assertEquals(new BigDecimal("25980.00"), response.getTotal());
        assertEquals(TipoDescuento.NINGUNO, response.getTipoDescuento());
        assertTrue(response.getFolio().startsWith("BP-PRE-"));
        assertEquals(EstadoVenta.COMPLETADA, response.getEstado());
        // Se guarda al crear (PENDIENTE) y de nuevo al fijar el estado final (COMPLETADA)
        verify(ventaRepository, times(2)).save(any(Venta.class));
        // El stock se descuenta de forma síncrona en ms-inventario (reemplaza el antiguo mensaje a RabbitMQ)
        verify(inventarioClient, times(1)).descontarStock(any(DescontarStockRequestDTO.class));
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    void registrarVentaPresencialSinAsistente_lanzaInvalidSale() {
        // Given
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.PRESENCIAL)
                .sucursalId(1L)
                .asistenteNombre("   ")
                .detalles(List.of(detalle(1, "10000")))
                .build();

        // When + Then
        assertThrows(InvalidSaleException.class, () -> ventaService.registrarVenta(request));
        verify(ventaRepository, never()).save(any(Venta.class));
        verifyNoInteractions(inventarioClient);
    }

    @Test
    void registrarVentaSinStock_lanzaInsufficientStock() {
        // Given
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.ONLINE)
                .sucursalId(1L)
                .detalles(List.of(detalle(5, "10000")))
                .build();
        StockResponseDTO sinStock = StockResponseDTO.builder()
                .productoId(1L).sucursalId(1L).disponible(false).stockActual(2).build();
        when(inventarioClient.checkStock(1L, 1L, 5)).thenReturn(sinStock);

        // When + Then
        assertThrows(InsufficientStockException.class, () -> ventaService.registrarVenta(request));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void registrarVenta_consultaElStockDeSuSucursalYLoDescuentaDeEsaMisma() {
        // La venta es en la sucursal 3: el stock debe consultarse en la 3 y el descuento
        // síncrono a ms-inventario debe pedirse para esa misma sucursal, no otra.
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.ONLINE)
                .sucursalId(3L)
                .usuarioId(null)
                .detalles(List.of(detalle(2, "10000")))
                .build();

        when(inventarioClient.checkStock(3L, 1L, 2)).thenReturn(
                StockResponseDTO.builder().productoId(1L).sucursalId(3L).disponible(true).stockActual(50).build());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(77L);
            return v;
        });

        VentaResponseDTO response = ventaService.registrarVenta(request);

        assertEquals(3L, response.getSucursalId());
        assertEquals(EstadoVenta.COMPLETADA, response.getEstado());
        verify(inventarioClient, times(1)).checkStock(3L, 1L, 2);

        ArgumentCaptor<DescontarStockRequestDTO> captor = ArgumentCaptor.forClass(DescontarStockRequestDTO.class);
        verify(inventarioClient).descontarStock(captor.capture());
        DescontarStockRequestDTO descuento = captor.getValue();
        assertEquals(3L, descuento.getSucursalId());
        assertEquals(77L, descuento.getVentaId());
        assertEquals(1, descuento.getDetalles().size());
        assertEquals(1L, descuento.getDetalles().get(0).getProductoId());
        assertEquals(2, descuento.getDetalles().get(0).getCantidad());
    }

    @Test
    void registrarVenta_siMsInventarioRechazaElDescuento_quedaRechazadaPeroSeGuarda() {
        // Simula una condición de carrera: el stock alcanzaba en la verificación previa
        // pero se agotó antes del descuento real. La venta debe quedar registrada como RECHAZADA,
        // no perderse ni propagar el error al cliente.
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.ONLINE)
                .sucursalId(1L)
                .detalles(List.of(detalle(2, "10000")))
                .build();

        when(inventarioClient.checkStock(1L, 1L, 2)).thenReturn(stockDisponible());
        doThrow(new RuntimeException("Stock insuficiente en ms-inventario"))
                .when(inventarioClient).descontarStock(any());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        VentaResponseDTO response = ventaService.registrarVenta(request);

        assertEquals(EstadoVenta.RECHAZADA, response.getEstado());
        verify(ventaRepository, times(2)).save(any(Venta.class));
    }

    @Test
    void registrarVenta_enviaAFacturacionLaVentaElUsuarioLaSucursalYElDetalle() {
        // Given
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.ONLINE)
                .sucursalId(2L)
                .usuarioId(5L)
                .detalles(List.of(detalle(2, "10000")))
                .build();

        when(usuarioClient.obtenerUsuarioPorId(5L)).thenReturn(
                UsuarioResponseDTO.builder().id(5L).nombre("Ana López").rut("12345678-9").estado("ACTIVO").build());
        when(inventarioClient.checkStock(2L, 1L, 2)).thenReturn(
                StockResponseDTO.builder().productoId(1L).sucursalId(2L).disponible(true).stockActual(50).build());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(88L);
            return v;
        });

        // When
        ventaService.registrarVenta(request);

        // Then: el documento tributario se pide con la trazabilidad completa
        ArgumentCaptor<VentaRegistradaInternaEvent> captor =
                ArgumentCaptor.forClass(VentaRegistradaInternaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        EmitirDocumentoRequestDTO facturaRequest = captor.getValue().getRequest();

        assertEquals(88L, facturaRequest.getVentaId());
        assertEquals(5L, facturaRequest.getUsuarioId());
        assertEquals(2L, facturaRequest.getSucursalId());
        assertEquals("12345678-9", facturaRequest.getRutCliente());
        assertEquals(1, facturaRequest.getDetalles().size());
        assertEquals(1L, facturaRequest.getDetalles().get(0).getProductoId());
        assertEquals(2, facturaRequest.getDetalles().get(0).getCantidad());
    }

    @Test
    void registrarVentaSinUsuarioRegistrado_facturaConRutGenericoYSinUsuarioId() {
        // Given (venta anónima: no hay usuarioId)
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.PRESENCIAL)
                .sucursalId(1L)
                .asistenteNombre("Pedro Vega")
                .detalles(List.of(detalle(1, "10000")))
                .build();

        when(inventarioClient.checkStock(1L, 1L, 1)).thenReturn(stockDisponible());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(89L);
            return v;
        });

        // When
        ventaService.registrarVenta(request);

        // Then
        ArgumentCaptor<VentaRegistradaInternaEvent> captor =
                ArgumentCaptor.forClass(VentaRegistradaInternaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        EmitirDocumentoRequestDTO facturaRequest = captor.getValue().getRequest();

        assertNull(facturaRequest.getUsuarioId());
        assertEquals("66666666-6", facturaRequest.getRutCliente()); // RUT genérico
        assertEquals(1L, facturaRequest.getSucursalId());
    }

    @Test
    void registrarVentaConCuponValido_aplicaDescuento() {
        // Given (10% de descuento sobre 20000 = 2000 -> total 18000)
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.ONLINE)
                .sucursalId(1L)
                .codigoDescuento("DESCUENTO10")
                .detalles(List.of(detalle(2, "10000")))
                .build();

        when(inventarioClient.checkStock(1L, 1L, 2)).thenReturn(stockDisponible());
        when(promocionClient.validarPromocion("DESCUENTO10"))
                .thenReturn(PromocionResponseDTO.builder()
                        .codigo("DESCUENTO10").porcentajeDescuento(10).vigente(true).build());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(2L);
            return v;
        });

        // When
        VentaResponseDTO response = ventaService.registrarVenta(request);

        // Then
        assertEquals(new BigDecimal("2000.00"), response.getDescuentoAplicado());
        assertEquals(new BigDecimal("18000.00"), response.getTotal());
        assertEquals(TipoDescuento.CUPON, response.getTipoDescuento());
    }

    @Test
    void obtenerVentaPorFolioExistente_retornaVenta() {
        // Given
        Venta venta = Venta.builder()
                .id(1L).folio("BP-PRE-ABCD1234").tipoVenta(TipoVenta.PRESENCIAL)
                .sucursalId(1L)
                .subtotal(new BigDecimal("10000")).total(new BigDecimal("10000"))
                .descuentoAplicado(BigDecimal.ZERO).tipoDescuento(TipoDescuento.NINGUNO)
                .build();
        when(ventaRepository.findByFolio("BP-PRE-ABCD1234")).thenReturn(Optional.of(venta));

        // When
        VentaResponseDTO response = ventaService.obtenerVentaPorFolio("BP-PRE-ABCD1234");

        // Then
        assertEquals("BP-PRE-ABCD1234", response.getFolio());
    }

    @Test
    void obtenerVentaPorFolioInexistente_lanzaResourceNotFound() {
        // Given
        when(ventaRepository.findByFolio("NO-EXISTE")).thenReturn(Optional.empty());

        // When + Then
        assertThrows(ResourceNotFoundException.class,
                () -> ventaService.obtenerVentaPorFolio("NO-EXISTE"));
    }

    @Test
    void obtenerTodas_retornaListado() {
        // Given
        Venta venta = Venta.builder()
                .id(1L).folio("BP-ONL-0001").tipoVenta(TipoVenta.ONLINE)
                .sucursalId(1L)
                .subtotal(new BigDecimal("5000")).total(new BigDecimal("5000"))
                .descuentoAplicado(BigDecimal.ZERO).tipoDescuento(TipoDescuento.NINGUNO)
                .build();
        when(ventaRepository.findAll()).thenReturn(List.of(venta));

        // When
        List<VentaResponseDTO> response = ventaService.obtenerTodas();

        // Then
        assertEquals(1, response.size());
        assertEquals("BP-ONL-0001", response.get(0).getFolio());
        verify(ventaRepository, times(1)).findAll();
    }

    @Test
    void registrarVentaConUsuarioRegistrado_resuelveNombreYRut() {
        // Given
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.ONLINE)
                .sucursalId(1L)
                .usuarioId(5L)
                .detalles(List.of(detalle(1, "10000")))
                .build();

        when(usuarioClient.obtenerUsuarioPorId(5L)).thenReturn(
                UsuarioResponseDTO.builder().id(5L).nombre("Ana López").rut("12345678-9").estado("ACTIVO").build());
        when(inventarioClient.checkStock(1L, 1L, 1)).thenReturn(stockDisponible());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(10L);
            return v;
        });

        // When
        VentaResponseDTO response = ventaService.registrarVenta(request);

        // Then
        assertNotNull(response);
        assertEquals(5L, response.getUsuarioId());
        assertEquals("Ana López", response.getClienteNombre());
        assertEquals("12345678-9", response.getClienteRut());
    }

    @Test
    void registrarVentaConUsuarioInactivo_lanzaInvalidSale() {
        // Given
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.ONLINE)
                .sucursalId(1L)
                .usuarioId(7L)
                .detalles(List.of(detalle(1, "10000")))
                .build();

        when(usuarioClient.obtenerUsuarioPorId(7L)).thenReturn(
                UsuarioResponseDTO.builder().id(7L).nombre("Carlos").rut("99999999-9").estado("INACTIVO").build());

        // When + Then
        assertThrows(InvalidSaleException.class, () -> ventaService.registrarVenta(request));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void registrarVentaConUsuarioInexistente_lanzaInvalidSale() {
        // Given
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.ONLINE)
                .sucursalId(1L)
                .usuarioId(999L)
                .detalles(List.of(detalle(1, "10000")))
                .build();

        when(usuarioClient.obtenerUsuarioPorId(999L))
                .thenThrow(FeignException.NotFound.class);

        // When + Then
        assertThrows(InvalidSaleException.class, () -> ventaService.registrarVenta(request));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void obtenerVentasPorUsuario_retornaHistorial() {
        // Given
        Venta venta = Venta.builder()
                .id(1L).folio("BP-ONL-0001").tipoVenta(TipoVenta.ONLINE)
                .sucursalId(1L).usuarioId(5L)
                .subtotal(new BigDecimal("5000")).total(new BigDecimal("5000"))
                .descuentoAplicado(BigDecimal.ZERO).tipoDescuento(TipoDescuento.NINGUNO)
                .build();
        when(ventaRepository.findByUsuarioId(5L)).thenReturn(List.of(venta));

        // When
        List<VentaResponseDTO> response = ventaService.obtenerVentasPorUsuario(5L);

        // Then
        assertEquals(1, response.size());
        assertEquals(5L, response.get(0).getUsuarioId());
        verify(ventaRepository, times(1)).findByUsuarioId(5L);
    }

    @Test
    void obtenerVentaPorIdExistente_retornaVenta() {
        // Given
        Venta venta = Venta.builder()
                .id(10L).folio("BP-ONL-1010").tipoVenta(TipoVenta.ONLINE)
                .sucursalId(1L).usuarioId(5L)
                .subtotal(new BigDecimal("5000")).total(new BigDecimal("5000"))
                .descuentoAplicado(BigDecimal.ZERO).tipoDescuento(TipoDescuento.NINGUNO)
                .build();
        when(ventaRepository.findById(10L)).thenReturn(Optional.of(venta));

        // When
        VentaResponseDTO response = ventaService.obtenerVentaPorId(10L);

        // Then
        assertEquals(10L, response.getId());
        assertEquals("BP-ONL-1010", response.getFolio());
    }

    @Test
    void obtenerVentaPorIdInexistente_lanzaResourceNotFound() {
        // Given
        when(ventaRepository.findById(999L)).thenReturn(Optional.empty());

        // When + Then
        assertThrows(ResourceNotFoundException.class, () -> ventaService.obtenerVentaPorId(999L));
    }
}
