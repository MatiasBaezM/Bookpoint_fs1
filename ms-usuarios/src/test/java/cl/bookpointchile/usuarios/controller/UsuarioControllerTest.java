package cl.bookpointchile.usuarios.controller;

import cl.bookpointchile.usuarios.dto.ActualizarRolRequestDTO;
import cl.bookpointchile.usuarios.dto.UsuarioRegistroRequestDTO;
import cl.bookpointchile.usuarios.dto.UsuarioResponseDTO;
import cl.bookpointchile.usuarios.exception.UsuarioNoEncontradoException;
import cl.bookpointchile.usuarios.service.UsuarioService;

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

@WebMvcTest(UsuarioController.class)
@ActiveProfiles("test")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockitoBean
    private UsuarioService usuarioService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registrarUsuario_retorna201() throws Exception {
        UsuarioRegistroRequestDTO request = UsuarioRegistroRequestDTO.builder()
                .rut("19876543-2").nombre("Camila Soto").email("camila@example.com")
                .password("Segura1234").build();
        Mockito.when(usuarioService.registrarUsuario(any()))
                .thenReturn(UsuarioResponseDTO.builder().id(1L).email("camila@example.com")
                        .rolNombre("Cliente Web").build());

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.rolNombre").value("Cliente Web"));
    }

    @Test
    void registrarUsuarioPasswordCorta_retorna400() throws Exception {
        UsuarioRegistroRequestDTO request = UsuarioRegistroRequestDTO.builder()
                .rut("19876543-2").nombre("Camila").email("camila@example.com")
                .password("123").build(); // < 8 caracteres

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarUsuarioEmailInvalido_retorna400() throws Exception {
        UsuarioRegistroRequestDTO request = UsuarioRegistroRequestDTO.builder()
                .rut("19876543-2").nombre("Camila").email("correo-malo")
                .password("Segura1234").build();

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerUsuarioPorId_retorna200() throws Exception {
        Mockito.when(usuarioService.obtenerUsuarioPorId(1L))
                .thenReturn(UsuarioResponseDTO.builder().id(1L).email("camila@example.com").build());

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("camila@example.com"));
    }

    @Test
    void obtenerUsuarioPorIdInexistente_retorna404() throws Exception {
        Mockito.when(usuarioService.obtenerUsuarioPorId(99L))
                .thenThrow(new UsuarioNoEncontradoException("No existe"));

        mockMvc.perform(get("/api/usuarios/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarRol_retorna200() throws Exception {
        ActualizarRolRequestDTO request = ActualizarRolRequestDTO.builder().rolId(1L).build();
        Mockito.when(usuarioService.actualizarRol(eq(1L), any()))
                .thenReturn(UsuarioResponseDTO.builder().id(1L).rolNombre("Administrador").build());

        mockMvc.perform(put("/api/usuarios/1/rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolNombre").value("Administrador"));
    }
}
