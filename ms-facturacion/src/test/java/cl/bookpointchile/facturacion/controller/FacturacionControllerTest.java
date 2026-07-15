package cl.bookpointchile.facturacion.controller;

import cl.bookpointchile.facturacion.dto.DetalleDocumentoRequestDTO;
import cl.bookpointchile.facturacion.dto.DetalleDocumentoResponseDTO;
import cl.bookpointchile.facturacion.dto.DocumentoResponseDTO;
import cl.bookpointchile.facturacion.dto.EmitirDocumentoRequestDTO;
import cl.bookpointchile.facturacion.exception.DatosFacturacionIncompletosException;
import cl.bookpointchile.facturacion.exception.DocumentoNoEncontradoException;
import cl.bookpointchile.facturacion.service.FacturacionService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FacturacionController.class)
@ActiveProfiles("test")
class FacturacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockitoBean
    private FacturacionService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void emitirDocumento_retorna201() throws Exception {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();
        Mockito.when(service.emitirDocumento(any()))
                .thenReturn(DocumentoResponseDTO.builder().id(1L).folioVenta("BP-PRE-0001")
                        .montoTotal(11900.0).build());

        mockMvc.perform(post("/api/facturacion/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoTotal").value(11900.0));
    }

    @Test
    void emitirDocumentoInvalido_retorna400() throws Exception {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .rutCliente("1-9").build(); // faltan folioVenta, tipoDocumento, montoNeto

        mockMvc.perform(post("/api/facturacion/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emitirDocumento_exponeVentaUsuarioSucursalYDetalle() throws Exception {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();

        Mockito.when(service.emitirDocumento(any()))
                .thenReturn(DocumentoResponseDTO.builder()
                        .id(1L).folioVenta("BP-PRE-0001")
                        .ventaId(50L).usuarioId(7L).sucursalId(2L)
                        .montoTotal(11900.0)
                        .detalles(List.of(DetalleDocumentoResponseDTO.builder()
                                .id(1L).productoId(101L).productoNombre("Libro de Prueba")
                                .cantidad(2).precioUnitario(new BigDecimal("5950.00"))
                                .subtotal(new BigDecimal("11900.00")).build()))
                        .build());

        mockMvc.perform(post("/api/facturacion/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ventaId").value(50))
                .andExpect(jsonPath("$.usuarioId").value(7))
                .andExpect(jsonPath("$.sucursalId").value(2))
                .andExpect(jsonPath("$.detalles[0].productoId").value(101))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2));
    }

    @Test
    void emitirDocumentoConDetalleInvalido_retorna400() throws Exception {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0)
                .detalles(List.of(DetalleDocumentoRequestDTO.builder()
                        .productoId(101L).productoNombre("Libro")
                        .cantidad(0) // @Min(1) falla
                        .precioUnitario(new BigDecimal("5950.00")).build()))
                .build();

        mockMvc.perform(post("/api/facturacion/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emitirDocumentoDeVentaSinSucursal_retorna400() throws Exception {
        EmitirDocumentoRequestDTO request = EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-PRE-0001").rutCliente("19876543-2")
                .tipoDocumento("BOLETA").montoNeto(10000.0).build();

        Mockito.when(service.emitirDocumento(any()))
                .thenThrow(new DatosFacturacionIncompletosException("La venta no tiene una sucursal asociada."));

        mockMvc.perform(post("/api/facturacion/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerPorFolioVenta_retorna200() throws Exception {
        Mockito.when(service.obtenerPorFolioVenta("BP-PRE-0001"))
                .thenReturn(DocumentoResponseDTO.builder().id(1L).folioVenta("BP-PRE-0001").build());

        mockMvc.perform(get("/api/facturacion/venta/BP-PRE-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioVenta").value("BP-PRE-0001"));
    }

    @Test
    void obtenerPorFolioVentaInexistente_retorna404() throws Exception {
        Mockito.when(service.obtenerPorFolioVenta("NO-EXISTE"))
                .thenThrow(new DocumentoNoEncontradoException("No existe"));

        mockMvc.perform(get("/api/facturacion/venta/NO-EXISTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerTodos_retorna200() throws Exception {
        Mockito.when(service.obtenerTodos())
                .thenReturn(List.of(DocumentoResponseDTO.builder().id(1L).folioVenta("BP-PRE-0001").build()));

        mockMvc.perform(get("/api/facturacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].folioVenta").value("BP-PRE-0001"));
    }
}
