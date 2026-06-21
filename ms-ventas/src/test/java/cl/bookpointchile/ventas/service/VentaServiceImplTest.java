package cl.bookpointchile.ventas.service;

import cl.bookpointchile.ventas.client.FacturacionClient;
import cl.bookpointchile.ventas.client.InventarioClient;
import cl.bookpointchile.ventas.client.PromocionClient;
import cl.bookpointchile.ventas.client.UsuarioClient;
import cl.bookpointchile.ventas.dto.*;
import cl.bookpointchile.ventas.exception.InsufficientStockException;
import cl.bookpointchile.ventas.exception.InvalidSaleException;
import cl.bookpointchile.ventas.exception.ResourceNotFoundException;
import feign.FeignException;
import cl.bookpointchile.ventas.model.TipoDescuento;
import cl.bookpointchile.ventas.model.TipoVenta;
import cl.bookpointchile.ventas.model.Venta;
import cl.bookpointchile.ventas.repository.VentaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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
    private RabbitTemplate rabbitTemplate;
    @Mock
    private InventarioClient inventarioClient;
    @Mock
    private PromocionClient promocionClient;
    @Mock
    private FacturacionClient facturacionClient;
    @Mock
    private UsuarioClient usuarioClient;

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
        return StockResponseDTO.builder().productoId(1L).disponible(true).stockActual(50).build();
    }

    // ---------- registrarVenta ----------

    @Test
    void registrarVentaPresencialSinDescuento_calculaTotalYGuarda() {
        // Given
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.PRESENCIAL)
                .clienteNombre("Camila Soto")
                .clienteRut("19876543-2")
                .asistenteNombre("Pedro Vega")
                .detalles(List.of(detalle(2, "12990")))
                .build();

        when(inventarioClient.checkStock(1L, 2)).thenReturn(stockDisponible());
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
        verify(ventaRepository, times(1)).save(any(Venta.class));
        // Se emite el evento VentaCreada a RabbitMQ
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void registrarVentaPresencialSinAsistente_lanzaInvalidSale() {
        // Given
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.PRESENCIAL)
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
                .detalles(List.of(detalle(5, "10000")))
                .build();
        StockResponseDTO sinStock = StockResponseDTO.builder()
                .productoId(1L).disponible(false).stockActual(2).build();
        when(inventarioClient.checkStock(1L, 5)).thenReturn(sinStock);

        // When + Then
        assertThrows(InsufficientStockException.class, () -> ventaService.registrarVenta(request));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void registrarVentaConCuponValido_aplicaDescuento() {
        // Given (10% de descuento sobre 20000 = 2000 -> total 18000)
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.ONLINE)
                .codigoDescuento("DESCUENTO10")
                .detalles(List.of(detalle(2, "10000")))
                .build();

        when(inventarioClient.checkStock(1L, 2)).thenReturn(stockDisponible());
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
                .usuarioId(5L)
                .detalles(List.of(detalle(1, "10000")))
                .build();

        when(usuarioClient.obtenerUsuarioPorId(5L)).thenReturn(
                UsuarioResponseDTO.builder().id(5L).nombre("Ana López").rut("12345678-9").estado("ACTIVO").build());
        when(inventarioClient.checkStock(1L, 1)).thenReturn(stockDisponible());
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
                .id(1L).folio("BP-ONL-0001").tipoVenta(TipoVenta.ONLINE).usuarioId(5L)
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
}
