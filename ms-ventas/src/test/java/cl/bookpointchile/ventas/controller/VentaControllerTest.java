package cl.bookpointchile.ventas.controller;

import cl.bookpointchile.ventas.dto.DetalleVentaRequestDTO;
import cl.bookpointchile.ventas.dto.VentaRequestDTO;
import cl.bookpointchile.ventas.dto.VentaResponseDTO;
import cl.bookpointchile.ventas.exception.InsufficientStockException;
import cl.bookpointchile.ventas.exception.ResourceNotFoundException;
import cl.bookpointchile.ventas.model.TipoVenta;
import cl.bookpointchile.ventas.service.VentaService;

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

@WebMvcTest(VentaController.class)
@ActiveProfiles("test")
class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockitoBean
    private VentaService ventaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private VentaRequestDTO requestValido() {
        return VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.PRESENCIAL)
                .clienteNombre("Camila Soto")
                .asistenteNombre("Pedro Vega")
                .detalles(List.of(DetalleVentaRequestDTO.builder()
                        .productoId(1L).productoNombre("Libro").cantidad(2)
                        .precioUnitario(new BigDecimal("12990")).build()))
                .build();
    }

    @Test
    void registrarVenta_retorna201() throws Exception {
        VentaResponseDTO response = VentaResponseDTO.builder()
                .id(1L).folio("BP-PRE-ABCD1234").tipoVenta(TipoVenta.PRESENCIAL)
                .total(new BigDecimal("25980.00")).build();

        Mockito.when(ventaService.registrarVenta(any(VentaRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.folio").value("BP-PRE-ABCD1234"));
    }

    @Test
    void registrarVentaSinDetalles_retorna400() throws Exception {
        VentaRequestDTO request = VentaRequestDTO.builder()
                .tipoVenta(TipoVenta.ONLINE)
                .detalles(List.of()) // @NotEmpty falla
                .build();

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarVentaSinStock_retorna400() throws Exception {
        Mockito.when(ventaService.registrarVenta(any(VentaRequestDTO.class)))
                .thenThrow(new InsufficientStockException("Stock insuficiente"));

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerVentaPorFolio_retorna200() throws Exception {
        VentaResponseDTO response = VentaResponseDTO.builder()
                .id(1L).folio("BP-PRE-ABCD1234").tipoVenta(TipoVenta.PRESENCIAL).build();

        Mockito.when(ventaService.obtenerVentaPorFolio("BP-PRE-ABCD1234")).thenReturn(response);

        mockMvc.perform(get("/api/ventas/BP-PRE-ABCD1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folio").value("BP-PRE-ABCD1234"));
    }

    @Test
    void obtenerVentaPorFolioInexistente_retorna404() throws Exception {
        Mockito.when(ventaService.obtenerVentaPorFolio("NO-EXISTE"))
                .thenThrow(new ResourceNotFoundException("La venta no existe."));

        mockMvc.perform(get("/api/ventas/NO-EXISTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerTodas_retorna200() throws Exception {
        VentaResponseDTO response = VentaResponseDTO.builder()
                .id(1L).folio("BP-ONL-0001").tipoVenta(TipoVenta.ONLINE).build();

        Mockito.when(ventaService.obtenerTodas()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].folio").value("BP-ONL-0001"));
    }

    @Test
    void obtenerVentasPorUsuario_retorna200() throws Exception {
        VentaResponseDTO response = VentaResponseDTO.builder()
                .id(1L).folio("BP-ONL-0001").tipoVenta(TipoVenta.ONLINE).usuarioId(5L).build();

        Mockito.when(ventaService.obtenerVentasPorUsuario(5L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/ventas/usuario/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(5));
    }
}
