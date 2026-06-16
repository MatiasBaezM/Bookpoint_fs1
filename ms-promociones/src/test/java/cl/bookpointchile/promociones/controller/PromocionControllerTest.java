package cl.bookpointchile.promociones.controller;

import cl.bookpointchile.promociones.dto.CrearPromocionRequestDTO;
import cl.bookpointchile.promociones.dto.PromocionResponseDTO;
import cl.bookpointchile.promociones.exception.PromocionNoEncontradaException;
import cl.bookpointchile.promociones.service.PromocionesService;

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

@WebMvcTest(PromocionController.class)
@ActiveProfiles("test")
class PromocionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockitoBean
    private PromocionesService promocionesService;

    @Test
    void registrarPromocion_retorna201() throws Exception {
        String json = "{\"codigo\":\"DESCUENTO10\",\"porcentajeDescuento\":10," +
                "\"fechaInicio\":\"2026-06-01\",\"fechaFin\":\"2026-12-31\"}";
        Mockito.when(promocionesService.registrarPromocion(any()))
                .thenReturn(PromocionResponseDTO.builder().id(1L).codigo("DESCUENTO10").build());

        mockMvc.perform(post("/api/promociones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("DESCUENTO10"));
    }

    @Test
    void registrarPromocionInvalida_retorna400() throws Exception {
        // faltan código y fechas obligatorias
        String json = "{\"codigo\":\"\",\"porcentajeDescuento\":10}";

        mockMvc.perform(post("/api/promociones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerTodas_retorna200() throws Exception {
        Mockito.when(promocionesService.obtenerTodas())
                .thenReturn(List.of(PromocionResponseDTO.builder().id(1L).codigo("DESCUENTO10").build()));

        mockMvc.perform(get("/api/promociones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("DESCUENTO10"));
    }

    @Test
    void validarPromocion_retorna200() throws Exception {
        Mockito.when(promocionesService.validarPromocion("DESCUENTO10"))
                .thenReturn(PromocionResponseDTO.builder().id(1L).codigo("DESCUENTO10").vigente(true).build());

        mockMvc.perform(get("/api/promociones/validar/DESCUENTO10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vigente").value(true));
    }

    @Test
    void validarPromocionInexistente_retorna404() throws Exception {
        Mockito.when(promocionesService.validarPromocion("NOPE"))
                .thenThrow(new PromocionNoEncontradaException("No existe"));

        mockMvc.perform(get("/api/promociones/validar/NOPE"))
                .andExpect(status().isNotFound());
    }
}
