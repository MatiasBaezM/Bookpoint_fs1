package cl.bookpointchile.inventario.controller;

import cl.bookpointchile.inventario.dto.*;
import cl.bookpointchile.inventario.exception.SucursalNoEncontradaException;
import cl.bookpointchile.inventario.service.InventarioService;

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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventarioController.class)
@ActiveProfiles("test")
class InventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockitoBean
    private InventarioService inventarioService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registrarAjuste_retorna200() throws Exception {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).sucursalId(1L).cantidadAjuste(50).motivo("Reposición").build();
        Mockito.when(inventarioService.registrarAjusteFisico(any()))
                .thenReturn(InventarioResponseDTO.builder().id(10L).cantidad(50).build());

        mockMvc.perform(put("/api/inventario/ajuste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(50));
    }

    @Test
    void registrarAjusteInvalido_retorna400() throws Exception {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).build(); // faltan sucursalId, cantidadAjuste, motivo

        mockMvc.perform(put("/api/inventario/ajuste")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void trasladarStock_retorna201() throws Exception {
        TrasladoStockRequestDTO request = TrasladoStockRequestDTO.builder()
                .productoId(1L).sucursalOrigenId(1L).sucursalDestinoId(2L).cantidad(10).build();
        Mockito.when(inventarioService.trasladarStock(any()))
                .thenReturn(InventarioResponseDTO.builder().id(11L).cantidad(10).sucursalId(2L).build());

        mockMvc.perform(post("/api/inventario/traslado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sucursalId").value(2L));
    }

    @Test
    void obtenerStock_retorna200() throws Exception {
        Mockito.when(inventarioService.obtenerStock(1L, 1L))
                .thenReturn(InventarioResponseDTO.builder().id(10L).productoId(1L).cantidad(50).build());

        mockMvc.perform(get("/api/inventario/1/producto/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(50));
    }

    @Test
    void obtenerStockSucursalInexistente_retorna404() throws Exception {
        Mockito.when(inventarioService.obtenerStock(99L, 1L))
                .thenThrow(new SucursalNoEncontradaException("No existe"));

        mockMvc.perform(get("/api/inventario/99/producto/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkStock_retorna200() throws Exception {
        Mockito.when(inventarioService.verificarDisponibilidad(anyLong(), anyInt()))
                .thenReturn(StockResponseDTO.builder().productoId(1L).disponible(true).stockActual(50).build());

        mockMvc.perform(get("/api/inventario/check-stock").param("productoId", "1").param("cantidad", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void obtenerAlertas_retorna200() throws Exception {
        Mockito.when(inventarioService.obtenerAlertasReposicion())
                .thenReturn(List.of(InventarioResponseDTO.builder().id(10L).alertaReposicion(true).build()));

        mockMvc.perform(get("/api/inventario/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alertaReposicion").value(true));
    }

    @Test
    void obtenerStockPorSucursal_retorna200() throws Exception {
        Mockito.when(inventarioService.obtenerStockPorSucursal(1L))
                .thenReturn(List.of(InventarioResponseDTO.builder().id(10L).sucursalId(1L).build()));

        mockMvc.perform(get("/api/inventario/sucursal/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sucursalId").value(1L));
    }
}
