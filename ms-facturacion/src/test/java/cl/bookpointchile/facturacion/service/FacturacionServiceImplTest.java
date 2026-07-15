package cl.bookpointchile.facturacion.service;

import cl.bookpointchile.facturacion.client.UsuariosClient;
import cl.bookpointchile.facturacion.client.VentasClient;
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

    // ---------- emitirDocumento ----------

    @Test
    void emitirBoleta_calculaIvaYGuarda() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("bp-pre-0001").rutCliente("19876543-2")
                .tipoDocumento("boleta").montoNeto(10000.0).build();

        VentaResponseDTO mockVenta = VentaResponseDTO.builder()
                .folio("BP-PRE-0001").total(11900.0).clienteRut("19876543-2").tipoDocumento("BOLETA").estado("COMPLETA").build();

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
    void emitirFacturaSinRazonSocial_lanzaDatosIncompletos() {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-ONL-0002").rutCliente("76543210-K")
                .tipoDocumento("FACTURA").montoNeto(20000.0).build();

        VentaResponseDTO mockVenta = VentaResponseDTO.builder()
                .folio("BP-ONL-0002").total(23800.0).clienteRut("76543210-K").tipoDocumento("FACTURA").estado("COMPLETA").build();

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

        VentaResponseDTO mockVenta = VentaResponseDTO.builder()
                .folio("BP-PRE-0003").total(5950.0).clienteRut("1-9").tipoDocumento("BOLETA").estado("COMPLETA").build();

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
