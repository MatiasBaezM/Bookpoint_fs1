package cl.bookpointchile.proveedores.controller;

import cl.bookpointchile.proveedores.dto.*;
import cl.bookpointchile.proveedores.exception.ProveedorNoEncontradoException;
import cl.bookpointchile.proveedores.model.EstadoOrden;
import cl.bookpointchile.proveedores.service.ProveedoresService;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProveedoresController.class)
@ActiveProfiles("test")
class ProveedoresControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockitoBean
    private ProveedoresService proveedoresService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void obtenerTodos_retorna200() throws Exception {
        Mockito.when(proveedoresService.obtenerTodosProveedores())
                .thenReturn(List.of(ProveedorResponseDTO.builder().id(1L).razonSocial("Andes").build()));

        mockMvc.perform(get("/api/proveedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].razonSocial").value("Andes"));
    }

    @Test
    void registrarProveedor_retorna201() throws Exception {
        CrearProveedorRequestDTO request = CrearProveedorRequestDTO.builder()
                .rut("96543210-1").razonSocial("Andes").emailContacto("ventas@andes.cl").build();
        Mockito.when(proveedoresService.registrarProveedor(any()))
                .thenReturn(ProveedorResponseDTO.builder().id(1L).razonSocial("Andes").build());

        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void registrarProveedorEmailInvalido_retorna400() throws Exception {
        CrearProveedorRequestDTO request = CrearProveedorRequestDTO.builder()
                .rut("96543210-1").razonSocial("Andes").emailContacto("correo-invalido").build();

        mockMvc.perform(post("/api/proveedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emitirOrdenCompra_retorna201() throws Exception {
        OrdenCompraRequestDTO request = OrdenCompraRequestDTO.builder()
                .proveedorId(1L)
                .detalles(List.of(DetalleOrdenRequestDTO.builder()
                        .productoId(1L).cantidadSolicitada(100).costoUnitario(new BigDecimal("6500")).build()))
                .build();
        Mockito.when(proveedoresService.emitirOrdenCompra(any()))
                .thenReturn(OrdenCompraResponseDTO.builder().id(1L).estado(EstadoOrden.PENDIENTE)
                        .total(new BigDecimal("650000")).build());

        mockMvc.perform(post("/api/proveedores/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void registrarRecepcion_retorna200() throws Exception {
        Mockito.when(proveedoresService.registrarRecepcionMercaderia(1L))
                .thenReturn(OrdenCompraResponseDTO.builder().id(1L).estado(EstadoOrden.RECIBIDA).build());

        mockMvc.perform(put("/api/proveedores/ordenes/1/recepcion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECIBIDA"));
    }

    @Test
    void emitirOrdenProveedorInexistente_retorna404() throws Exception {
        OrdenCompraRequestDTO request = OrdenCompraRequestDTO.builder()
                .proveedorId(99L)
                .detalles(List.of(DetalleOrdenRequestDTO.builder()
                        .productoId(1L).cantidadSolicitada(10).costoUnitario(new BigDecimal("100")).build()))
                .build();
        Mockito.when(proveedoresService.emitirOrdenCompra(any()))
                .thenThrow(new ProveedorNoEncontradoException("No existe"));

        mockMvc.perform(post("/api/proveedores/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
