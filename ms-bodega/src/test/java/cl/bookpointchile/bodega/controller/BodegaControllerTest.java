package cl.bookpointchile.bodega.controller;

import cl.bookpointchile.bodega.dto.*;
import cl.bookpointchile.bodega.exception.OrdenPickingNoEncontradaException;
import cl.bookpointchile.bodega.service.BodegaService;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BodegaController.class)
@ActiveProfiles("test")
class BodegaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockitoBean
    private BodegaService bodegaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registrarUbicacion_retorna201() throws Exception {
        UbicacionRequestDTO request = UbicacionRequestDTO.builder()
                .pasillo("A").estante("03").nivel("2").codigoBarras("7801234567890").build();
        Mockito.when(bodegaService.registrarUbicacion(any()))
                .thenReturn(UbicacionResponseDTO.builder().id(1L).codigoBarras("7801234567890").build());

        mockMvc.perform(post("/api/bodega/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void registrarUbicacionInvalida_retorna400() throws Exception {
        UbicacionRequestDTO request = UbicacionRequestDTO.builder().pasillo("").build();

        mockMvc.perform(post("/api/bodega/ubicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearOrdenPicking_retorna201() throws Exception {
        CrearOrdenPickingRequestDTO request = CrearOrdenPickingRequestDTO.builder()
                .ventaId(100L).productoId(1L).cantidad(2).operarioAsignado("Juan").build();
        Mockito.when(bodegaService.crearOrdenPicking(any()))
                .thenReturn(OrdenPickingResponseDTO.builder().id(1L).ventaId(100L).estado("PENDIENTE").build());

        mockMvc.perform(post("/api/bodega/picking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void actualizarEstadoPicking_retorna200() throws Exception {
        Mockito.when(bodegaService.actualizarEstadoPicking(eq(1L), anyString()))
                .thenReturn(OrdenPickingResponseDTO.builder().id(1L).estado("EN_PROCESO").build());

        mockMvc.perform(put("/api/bodega/picking/1/estado").param("estado", "EN_PROCESO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
    }

    @Test
    void actualizarEstadoPickingInexistente_retorna404() throws Exception {
        Mockito.when(bodegaService.actualizarEstadoPicking(eq(99L), anyString()))
                .thenThrow(new OrdenPickingNoEncontradaException("No existe"));

        mockMvc.perform(put("/api/bodega/picking/99/estado").param("estado", "EN_PROCESO"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerUbicaciones_retorna200() throws Exception {
        Mockito.when(bodegaService.obtenerUbicaciones())
                .thenReturn(List.of(UbicacionResponseDTO.builder().id(1L).codigoBarras("ABC").build()));

        mockMvc.perform(get("/api/bodega/ubicaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoBarras").value("ABC"));
    }

    @Test
    void obtenerOrdenesPicking_retorna200() throws Exception {
        Mockito.when(bodegaService.obtenerOrdenesPicking())
                .thenReturn(List.of(OrdenPickingResponseDTO.builder().id(1L).estado("PENDIENTE").build()));

        mockMvc.perform(get("/api/bodega/picking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }
}
