package cl.bookpointchile.sucursales.controller;

import cl.bookpointchile.sucursales.dto.SucursalRequestDTO;
import cl.bookpointchile.sucursales.dto.SucursalResponseDTO;
import cl.bookpointchile.sucursales.exception.SucursalNoEncontradaException;
import cl.bookpointchile.sucursales.service.SucursalService;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SucursalController.class)
@ActiveProfiles("test")
class SucursalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockitoBean
    private SucursalService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void obtenerTodas_retorna200() throws Exception {
        Mockito.when(service.obtenerTodas())
                .thenReturn(List.of(SucursalResponseDTO.builder().id(1L).nombre("Centro").build()));

        mockMvc.perform(get("/api/sucursales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Centro"));
    }

    @Test
    void obtenerPorId_retorna200() throws Exception {
        Mockito.when(service.obtenerPorId(1L))
                .thenReturn(SucursalResponseDTO.builder().id(1L).nombre("Centro").build());

        mockMvc.perform(get("/api/sucursales/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Centro"));
    }

    @Test
    void obtenerPorIdInexistente_retorna404() throws Exception {
        Mockito.when(service.obtenerPorId(99L))
                .thenThrow(new SucursalNoEncontradaException("No existe"));

        mockMvc.perform(get("/api/sucursales/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearSucursal_retorna201() throws Exception {
        SucursalRequestDTO request = SucursalRequestDTO.builder()
                .nombre("Sucursal Centro").direccion("Av. Libertad 1234").comuna("Concepción").build();
        Mockito.when(service.crearSucursal(any()))
                .thenReturn(SucursalResponseDTO.builder().id(1L).nombre("Sucursal Centro").build());

        mockMvc.perform(post("/api/sucursales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void crearSucursalInvalida_retorna400() throws Exception {
        SucursalRequestDTO request = SucursalRequestDTO.builder().nombre("").build();

        mockMvc.perform(post("/api/sucursales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarSucursal_retorna200() throws Exception {
        SucursalRequestDTO request = SucursalRequestDTO.builder()
                .nombre("Centro").direccion("Nueva Dir").comuna("Talcahuano").build();
        Mockito.when(service.actualizarSucursal(eq(1L), any()))
                .thenReturn(SucursalResponseDTO.builder().id(1L).nombre("Centro").comuna("Talcahuano").build());

        mockMvc.perform(put("/api/sucursales/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comuna").value("Talcahuano"));
    }
}
