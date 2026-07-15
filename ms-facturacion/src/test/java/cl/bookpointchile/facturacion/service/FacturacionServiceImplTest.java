package cl.bookpointchile.facturacion.service;

import cl.bookpointchile.facturacion.client.UsuariosClient;
import cl.bookpointchile.facturacion.client.VentasClient;
import cl.bookpointchile.facturacion.dto.DetalleDocumentoRequestDTO;
import cl.bookpointchile.facturacion.dto.DocumentoResponseDTO;
import cl.bookpointchile.facturacion.dto.EmitirDocumentoRequestDTO;
import cl.bookpointchile.facturacion.dto.UsuarioResponseDTO;
import cl.bookpointchile.facturacion.dto.VentaResponseDTO;
import cl.bookpointchile.facturacion.exception.DatosFacturacionIncompletosException;
import cl.bookpointchile.facturacion.exception.DocumentoDuplicadoException;
import cl.bookpointchile.facturacion.exception.DocumentoNoEncontradoException;
import cl.bookpointchile.facturacion.model.DocumentoTributario;
import cl.bookpointchile.facturacion.repository.DocumentoTributarioRepository;

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
class FacturacionServiceImplTest {

    @Mock
    private DocumentoTributarioRepository repository;

    @Mock
    private VentasClient ventasClient;

    @Mock
    private UsuariosClient usuariosClient;

    @InjectMocks
    private FacturacionServiceImpl facturacionService;

    // ── helpers ──────────────────────────────────────────────────────────────

    // ms-ventas es la fuente de la sucursal, el usuario y el detalle de productos,
    // así que toda venta simulada debe traerlos.
    private VentaResponseDTO venta(String folio, Double total, String rut, String tipoDocumento) {
        return VentaResponseDTO.builder()
                .id(50L).folio(folio).total(total).sucursalId(1L).usuarioId(7L)
                .clienteRut(rut).tipoDocumento(tipoDocumento).estado("COMPLETA")
                .detalles(java.util.List.of(VentaResponseDTO.DetalleVentaResponseDTO.builder()
                        .productoId(101L).productoNombre("Libro de Prueba").cantidad(1)
                        .precioUnitario(new java.math.BigDecimal("11900.00"))
                        .subtotal(new java.math.BigDecimal("11900.00"))
                        .build()))
                .build();
    }

    // ---------- emitirDocumento ----------

    @Test
    void emitirBoleta_calculaIvaYGuarda() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("bp-pre-0001").rutCliente("19876543-2")
                .tipoDocumento("boleta").montoNeto(10000.0).build();

        VentaResponseDTO mockVenta = venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA");

        UsuarioResponseDTO mockUsuario = UsuarioResponseDTO.builder()
                .rut("19876543-2").estado("ACTIVO").build();

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001")).thenReturn(mockVenta);
        when(usuariosClient.obtenerUsuarioPorRut("19876543-2")).thenReturn(mockUsuario);

        when(repository.save(any(DocumentoTributario.class))).thenAnswer(inv -> {
            DocumentoTributario d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        DocumentoResponseDTO response = facturacionService.emitirDocumento(request);

        assertEquals(1900.0, response.getMontoIva());
        assertEquals(11900.0, response.getMontoTotal());
        assertEquals("BOLETA", response.getTipoDocumento());
        assertEquals("BP-PRE-0001", response.getFolioVenta());
    }

    @Test
    void emitirDocumento_relacionaVentaUsuarioYSucursal_conElDetalleDeProductos() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001"))
                .thenReturn(venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA"));
        when(usuariosClient.obtenerUsuarioPorRut("19876543-2"))
                .thenReturn(UsuarioResponseDTO.builder().rut("19876543-2").estado("ACTIVO").build());
        when(repository.save(any(DocumentoTributario.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoResponseDTO response = facturacionService.emitirDocumento(request);

        assertEquals(50L, response.getVentaId());
        assertEquals(7L, response.getUsuarioId());
        assertEquals(1L, response.getSucursalId());
        assertEquals(1, response.getDetalles().size());
        assertEquals(101L, response.getDetalles().get(0).getProductoId());
        assertEquals(1, response.getDetalles().get(0).getCantidad());
    }

    @Test
    void emitirDocumento_montoNetoNoCoincideConLaVenta_lanzaDatosIncompletos() {
        // El neto real de la venta (11900 / 1.19 = 10000) no coincide con lo que trae la petición.
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(500.0).build();

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001"))
                .thenReturn(venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA"));

        assertThrows(DatosFacturacionIncompletosException.class,
                () -> facturacionService.emitirDocumento(request));
        verify(repository, never()).save(any());
    }

    @Test
    void emitirDocumento_ventaNoEncontradaEnMsVentas_lanzaDatosIncompletos() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-9999").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();

        when(repository.existsByFolioVenta("BP-PRE-9999")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-9999"))
                .thenThrow(mock(feign.FeignException.NotFound.class));

        assertThrows(DatosFacturacionIncompletosException.class,
                () -> facturacionService.emitirDocumento(request));
        verify(repository, never()).save(any());
    }

    @Test
    void emitirDocumento_fallaComunicacionConMsVentas_lanzaDatosIncompletos() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001"))
                .thenThrow(new RuntimeException("Timeout"));

        assertThrows(DatosFacturacionIncompletosException.class,
                () -> facturacionService.emitirDocumento(request));
        verify(repository, never()).save(any());
    }

    @Test
    void emitirDocumento_clienteNoRegistradoEnMsUsuarios_lanzaDatosIncompletos() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001"))
                .thenReturn(venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA"));
        when(usuariosClient.obtenerUsuarioPorRut("19876543-2"))
                .thenThrow(mock(feign.FeignException.NotFound.class));

        assertThrows(DatosFacturacionIncompletosException.class,
                () -> facturacionService.emitirDocumento(request));
        verify(repository, never()).save(any());
    }

    @Test
    void emitirDocumento_clienteInactivo_lanzaDatosIncompletos() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001"))
                .thenReturn(venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA"));
        when(usuariosClient.obtenerUsuarioPorRut("19876543-2"))
                .thenReturn(UsuarioResponseDTO.builder().rut("19876543-2").estado("INACTIVO").build());

        assertThrows(DatosFacturacionIncompletosException.class,
                () -> facturacionService.emitirDocumento(request));
        verify(repository, never()).save(any());
    }

    @Test
    void emitirDocumento_fallaComunicacionConMsUsuarios_continuaEnModoDegradadoYEmite() {
        // Un fallo de comunicación (no un 404) con ms-usuarios no debe bloquear la emisión:
        // el documento se emite igual, en modo degradado.
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001"))
                .thenReturn(venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA"));
        when(usuariosClient.obtenerUsuarioPorRut("19876543-2"))
                .thenThrow(new RuntimeException("Timeout"));
        when(repository.save(any(DocumentoTributario.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoResponseDTO response = facturacionService.emitirDocumento(request);

        assertEquals("BP-PRE-0001", response.getFolioVenta());
        verify(repository, times(1)).save(any(DocumentoTributario.class));
    }

    @Test
    void emitirDocumentoDeVentaSinSucursal_lanzaDatosIncompletos() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();

        VentaResponseDTO sinSucursal = venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA");
        sinSucursal.setSucursalId(null);

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001")).thenReturn(sinSucursal);

        assertThrows(DatosFacturacionIncompletosException.class,
                () -> facturacionService.emitirDocumento(request));
        verify(repository, never()).save(any());
    }

    @Test
    void emitirDocumento_siLaVentaNoTraeDetalle_usaElDeLaPeticion() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0)
                .detalles(java.util.List.of(DetalleDocumentoRequestDTO.builder()
                        .productoId(777L).productoNombre("Producto de Respaldo").cantidad(3)
                        .precioUnitario(new java.math.BigDecimal("1000.00"))
                        .subtotal(new java.math.BigDecimal("3000.00"))
                        .build()))
                .build();

        VentaResponseDTO sinDetalles = venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA");
        sinDetalles.setDetalles(null);

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001")).thenReturn(sinDetalles);
        when(usuariosClient.obtenerUsuarioPorRut("19876543-2"))
                .thenReturn(UsuarioResponseDTO.builder().rut("19876543-2").estado("ACTIVO").build());
        when(repository.save(any(DocumentoTributario.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoResponseDTO response = facturacionService.emitirDocumento(request);

        assertEquals(1, response.getDetalles().size());
        assertEquals(777L, response.getDetalles().get(0).getProductoId());
    }

    @Test
    void emitirDocumento_elDetalleDeLaVentaTienePrioridadSobreElDeLaPeticion() {
        // ms-ventas es la fuente de verdad: si la petición trae otro producto, se ignora.
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0)
                .detalles(java.util.List.of(DetalleDocumentoRequestDTO.builder()
                        .productoId(999L).productoNombre("Producto Falso").cantidad(50)
                        .precioUnitario(new java.math.BigDecimal("1.00"))
                        .build()))
                .build();

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001"))
                .thenReturn(venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA"));
        when(usuariosClient.obtenerUsuarioPorRut("19876543-2"))
                .thenReturn(UsuarioResponseDTO.builder().rut("19876543-2").estado("ACTIVO").build());
        when(repository.save(any(DocumentoTributario.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoResponseDTO response = facturacionService.emitirDocumento(request);

        assertEquals(1, response.getDetalles().size());
        assertEquals(101L, response.getDetalles().get(0).getProductoId()); // el de la venta, no el 999
    }

    @Test
    void emitirDocumento_siLaLineaNoTraeSubtotal_loCalcula() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0)
                .detalles(java.util.List.of(DetalleDocumentoRequestDTO.builder()
                        .productoId(777L).productoNombre("Sin subtotal").cantidad(3)
                        .precioUnitario(new java.math.BigDecimal("1000.00"))
                        .subtotal(null) // debe calcularse como 1000 * 3
                        .build()))
                .build();

        VentaResponseDTO sinDetalles = venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA");
        sinDetalles.setDetalles(null);

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001")).thenReturn(sinDetalles);
        when(usuariosClient.obtenerUsuarioPorRut("19876543-2"))
                .thenReturn(UsuarioResponseDTO.builder().rut("19876543-2").estado("ACTIVO").build());
        when(repository.save(any(DocumentoTributario.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoResponseDTO response = facturacionService.emitirDocumento(request);

        assertEquals(0, new java.math.BigDecimal("3000.00")
                .compareTo(response.getDetalles().get(0).getSubtotal()));
    }

    @Test
    void emitirDocumento_siLaPeticionNoTraeSucursal_usaLaDeLaVenta() {
        // La sucursal de la venta manda aunque la petición traiga otra.
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0)
                .sucursalId(99L)
                .build();

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001"))
                .thenReturn(venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA")); // sucursalId = 1
        when(usuariosClient.obtenerUsuarioPorRut("19876543-2"))
                .thenReturn(UsuarioResponseDTO.builder().rut("19876543-2").estado("ACTIVO").build());
        when(repository.save(any(DocumentoTributario.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentoResponseDTO response = facturacionService.emitirDocumento(request);

        assertEquals(1L, response.getSucursalId());
    }

    @Test
    void emitirDocumentoSinDetalleDeProductos_lanzaDatosIncompletos() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();

        VentaResponseDTO sinDetalles = venta("BP-PRE-0001", 11900.0, "19876543-2", "BOLETA");
        sinDetalles.setDetalles(java.util.List.of());

        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0001")).thenReturn(sinDetalles);

        assertThrows(DatosFacturacionIncompletosException.class,
                () -> facturacionService.emitirDocumento(request));
        verify(repository, never()).save(any());
    }

    @Test
    void emitirFacturaSinRazonSocial_lanzaDatosIncompletos() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-ONL-0002").rutCliente("76543210-K")
                .tipoDocumento("FACTURA").montoNeto(20000.0).build();

        VentaResponseDTO mockVenta = venta("BP-ONL-0002", 23800.0, "76543210-K", "FACTURA");

        UsuarioResponseDTO mockUsuario = UsuarioResponseDTO.builder()
                .rut("76543210-K").estado("ACTIVO").build();

        when(repository.existsByFolioVenta("BP-ONL-0002")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-ONL-0002")).thenReturn(mockVenta);
        when(usuariosClient.obtenerUsuarioPorRut("76543210-K")).thenReturn(mockUsuario);

        assertThrows(DatosFacturacionIncompletosException.class,
                () -> facturacionService.emitirDocumento(request));
        verify(repository, never()).save(any());
    }

    @Test
    void emitirDocumentoDuplicado_lanzaExcepcion() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("1-9")
                .tipoDocumento("BOLETA").montoNeto(5000.0).build();
        when(repository.existsByFolioVenta("BP-PRE-0001")).thenReturn(true);

        assertThrows(DocumentoDuplicadoException.class,
                () -> facturacionService.emitirDocumento(request));
        verify(repository, never()).save(any());
    }

    @Test
    void emitirDocumentoTipoInvalido_lanzaDatosIncompletos() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0003").rutCliente("1-9")
                .tipoDocumento("VALE").montoNeto(5000.0).build();

        VentaResponseDTO mockVenta = venta("BP-PRE-0003", 5950.0, "1-9", "BOLETA");

        UsuarioResponseDTO mockUsuario = UsuarioResponseDTO.builder()
                .rut("1-9").estado("ACTIVO").build();

        when(repository.existsByFolioVenta("BP-PRE-0003")).thenReturn(false);
        when(ventasClient.obtenerVentaPorFolio("BP-PRE-0003")).thenReturn(mockVenta);
        when(usuariosClient.obtenerUsuarioPorRut("1-9")).thenReturn(mockUsuario);

        assertThrows(DatosFacturacionIncompletosException.class,
                () -> facturacionService.emitirDocumento(request));
    }

    // ---------- obtenerPorFolioVenta ----------

    @Test
    void obtenerPorFolioVentaExistente_retornaDocumento() {
        DocumentoTributario doc = DocumentoTributario.builder()
                .id(1L).folioVenta("BP-PRE-0001").tipoDocumento("BOLETA")
                .montoNeto(10000.0).montoIva(1900.0).montoTotal(11900.0).build();
        when(repository.findByFolioVenta("BP-PRE-0001")).thenReturn(Optional.of(doc));

        DocumentoResponseDTO response = facturacionService.obtenerPorFolioVenta("bp-pre-0001");

        assertEquals("BP-PRE-0001", response.getFolioVenta());
    }

    @Test
    void obtenerPorFolioVentaInexistente_lanzaNoEncontrado() {
        when(repository.findByFolioVenta("NO-EXISTE")).thenReturn(Optional.empty());

        assertThrows(DocumentoNoEncontradoException.class,
                () -> facturacionService.obtenerPorFolioVenta("no-existe"));
    }

    @Test
    void obtenerTodos_retornaLista() {
        when(repository.findAll()).thenReturn(java.util.List.of(
                DocumentoTributario.builder().id(1L).folioVenta("BP-PRE-0001").build()));

        assertEquals(1, facturacionService.obtenerTodos().size());
        verify(repository, times(1)).findAll();
    }
}
