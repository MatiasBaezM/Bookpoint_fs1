package cl.bookpointchile.logistica.controller;

import cl.bookpointchile.logistica.dto.ActualizarEstadoRequestDTO;
import cl.bookpointchile.logistica.dto.CrearEnvioRequestDTO;
import cl.bookpointchile.logistica.dto.EnvioResponseDTO;
import cl.bookpointchile.logistica.exception.ResourceNotFoundException;
import cl.bookpointchile.logistica.exception.TransicionEstadoInvalidaException;
import cl.bookpointchile.logistica.model.EstadoEnvio;
import cl.bookpointchile.logistica.service.LogisticaService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogisticaController.class)
@ActiveProfiles("test")
class LogisticaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockitoBean
    private LogisticaService logisticaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void crearEnvio_retorna201() throws Exception {
        CrearEnvioRequestDTO request = CrearEnvioRequestDTO.builder()
                .ventaId(100L)
                .direccionDestino("Av. Siempre Viva 123, Hualpén")
                .build();
        EnvioResponseDTO response = EnvioResponseDTO.builder()
                .id(1L).ventaId(100L).estado(EstadoEnvio.PENDIENTE).destino("Hualpén").build();

        Mockito.when(logisticaService.crearEnvio(any(CrearEnvioRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/logistica/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.ventaId").value(100L))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void crearEnvioConDatosInvalidos_retorna400() throws Exception {
        // ventaId nulo y direccion vacía -> falla @Valid
        CrearEnvioRequestDTO request = CrearEnvioRequestDTO.builder()
                .direccionDestino("")
                .build();

        mockMvc.perform(post("/api/logistica/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarEstado_retorna200() throws Exception {
        ActualizarEstadoRequestDTO request = ActualizarEstadoRequestDTO.builder()
                .estado(EstadoEnvio.EN_RUTA).build();
        EnvioResponseDTO response = EnvioResponseDTO.builder()
                .id(1L).ventaId(100L).estado(EstadoEnvio.EN_RUTA).build();

        Mockito.when(logisticaService.actualizarEstado(eq(1L), any(ActualizarEstadoRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/logistica/envios/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_RUTA"));
    }

    @Test
    void actualizarEstadoTransicionInvalida_retorna400() throws Exception {
        ActualizarEstadoRequestDTO request = ActualizarEstadoRequestDTO.builder()
                .estado(EstadoEnvio.ENTREGADO).build();

        Mockito.when(logisticaService.actualizarEstado(eq(1L), any(ActualizarEstadoRequestDTO.class)))
                .thenThrow(new TransicionEstadoInvalidaException("Transición inválida."));

        mockMvc.perform(put("/api/logistica/envios/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerEnvioPorVentaId_retorna200() throws Exception {
        EnvioResponseDTO response = EnvioResponseDTO.builder()
                .id(1L).ventaId(5001L).estado(EstadoEnvio.PENDIENTE).build();

        Mockito.when(logisticaService.obtenerEnvioPorVentaId(5001L)).thenReturn(response);

        mockMvc.perform(get("/api/logistica/envios/venta/5001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventaId").value(5001L));
    }

    @Test
    void obtenerEnvioPorVentaIdInexistente_retorna404() throws Exception {
        Mockito.when(logisticaService.obtenerEnvioPorVentaId(9999L))
                .thenThrow(new ResourceNotFoundException("No se encontró despacho."));

        mockMvc.perform(get("/api/logistica/envios/venta/9999"))
                .andExpect(status().isNotFound());
    }
}
