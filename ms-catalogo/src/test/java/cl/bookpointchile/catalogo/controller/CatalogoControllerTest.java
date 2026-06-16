package cl.bookpointchile.catalogo.controller;

import cl.bookpointchile.catalogo.dto.*;
import cl.bookpointchile.catalogo.exception.ProductoNoEncontradoException;
import cl.bookpointchile.catalogo.service.CatalogoService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogoController.class)
@ActiveProfiles("test")
class CatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockitoBean
    private CatalogoService catalogoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buscarProductos_retorna200() throws Exception {
        Page<ProductoResponseDTO> page = new PageImpl<>(List.of(
                ProductoResponseDTO.builder().id(1L).titulo("Cien años de soledad").build()));
        Mockito.when(catalogoService.buscarProductosConFiltros(
                        any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/catalogo/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Cien años de soledad"));
    }

    @Test
    void obtenerProductoPorId_retorna200() throws Exception {
        Mockito.when(catalogoService.obtenerProductoPorId(1L))
                .thenReturn(ProductoResponseDTO.builder().id(1L).titulo("Rayuela").build());

        mockMvc.perform(get("/api/catalogo/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Rayuela"));
    }

    @Test
    void obtenerProductoPorIdInexistente_retorna404() throws Exception {
        Mockito.when(catalogoService.obtenerProductoPorId(99L))
                .thenThrow(new ProductoNoEncontradoException("No existe"));

        mockMvc.perform(get("/api/catalogo/productos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarProducto_retorna201() throws Exception {
        ProductoRegistroRequestDTO request = ProductoRegistroRequestDTO.builder()
                .titulo("Rayuela").autor("Cortázar").editorial("Alfaguara")
                .precio(new BigDecimal("9990")).categoria("Novela").build();
        Mockito.when(catalogoService.registrarProducto(any()))
                .thenReturn(ProductoResponseDTO.builder().id(5L).titulo("Rayuela").build());

        mockMvc.perform(post("/api/catalogo/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5L));
    }

    @Test
    void registrarProductoInvalido_retorna400() throws Exception {
        ProductoRegistroRequestDTO request = ProductoRegistroRequestDTO.builder()
                .titulo("").build();

        mockMvc.perform(post("/api/catalogo/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agregarResena_retorna201() throws Exception {
        ResenaRequestDTO request = ResenaRequestDTO.builder()
                .usuarioId(1L).usuarioNombre("Camila").calificacion(5).comentario("Excelente").build();
        Mockito.when(catalogoService.agregarResena(eq(1L), any()))
                .thenReturn(ResenaResponseDTO.builder().id(10L).calificacion(5).build());

        mockMvc.perform(post("/api/catalogo/productos/1/resenas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calificacion").value(5));
    }
}
