package cl.bookpointchile.facturacion.controller;

import cl.bookpointchile.facturacion.dto.DocumentoResponseDTO;
import cl.bookpointchile.facturacion.dto.EmitirDocumentoRequestDTO;
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
