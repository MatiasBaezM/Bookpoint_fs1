package cl.bookpointchile.usuarios.service;

import cl.bookpointchile.usuarios.dto.ActualizarRolRequestDTO;
import cl.bookpointchile.usuarios.dto.UsuarioRegistroRequestDTO;
import cl.bookpointchile.usuarios.dto.UsuarioResponseDTO;
import cl.bookpointchile.usuarios.exception.EmailYaRegistradoException;
import cl.bookpointchile.usuarios.exception.ResourceNotFoundException;
import cl.bookpointchile.usuarios.exception.UsuarioNoEncontradoException;
import cl.bookpointchile.usuarios.model.Rol;
import cl.bookpointchile.usuarios.model.Usuario;
import cl.bookpointchile.usuarios.repository.RolRepository;
import cl.bookpointchile.usuarios.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Rol rol(Long id, String nombre) {
        return Rol.builder().id(id).nombre(nombre).descripcion("desc").build();
    }

    private Usuario usuario(Long id, Rol rol) {
        return Usuario.builder()
                .id(id).rut("19876543-2").nombre("Camila Soto").email("camila@example.com")
                .password("[SHA256]secret123").estado("ACTIVO").rol(rol).build();
    }

    // ---------- registrarUsuario ----------

    @Test
    void registrarUsuarioConRolPorDefecto_guarda() {
        UsuarioRegistroRequestDTO request = UsuarioRegistroRequestDTO.builder()
                .rut("19876543-2").nombre("Camila Soto").email("camila@example.com")
                .password("Segura1234").build(); // rolId null -> Cliente Web
        when(usuarioRepository.existsByEmail("camila@example.com")).thenReturn(false);
        when(usuarioRepository.existsByRut("19876543-2")).thenReturn(false);
        when(rolRepository.findByNombreIgnoreCase("Cliente Web"))
                .thenReturn(Optional.of(rol(4L, "Cliente Web")));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UsuarioResponseDTO response = usuarioService.registrarUsuario(request);

        assertEquals(1L, response.getId());
        assertEquals("Cliente Web", response.getRolNombre());
        assertEquals("ACTIVO", response.getEstado());
    }

    @Test
    void registrarUsuarioEmailDuplicado_lanzaExcepcion() {
        UsuarioRegistroRequestDTO request = UsuarioRegistroRequestDTO.builder()
                .rut("19876543-2").nombre("Camila").email("camila@example.com")
                .password("Segura1234").build();
        when(usuarioRepository.existsByEmail("camila@example.com")).thenReturn(true);

        assertThrows(EmailYaRegistradoException.class,
                () -> usuarioService.registrarUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarUsuarioConRolInexistente_lanzaResourceNotFound() {
        UsuarioRegistroRequestDTO request = UsuarioRegistroRequestDTO.builder()
                .rut("19876543-2").nombre("Camila").email("camila@example.com")
                .password("Segura1234").rolId(99L).build();
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(usuarioRepository.existsByRut(any())).thenReturn(false);
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.registrarUsuario(request));
    }

    // ---------- obtenerUsuarioPorId ----------

    @Test
    void obtenerUsuarioPorIdExistente_retornaUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, rol(4L, "Cliente Web"))));

        UsuarioResponseDTO response = usuarioService.obtenerUsuarioPorId(1L);

        assertEquals("camila@example.com", response.getEmail());
    }

    @Test
    void obtenerUsuarioPorIdInexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> usuarioService.obtenerUsuarioPorId(99L));
    }

    // ---------- actualizarRol ----------

    @Test
    void actualizarRol_actualizaCorrectamente() {
        Usuario u = usuario(1L, rol(4L, "Cliente Web"));
        ActualizarRolRequestDTO request = ActualizarRolRequestDTO.builder().rolId(1L).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rol(1L, "Administrador")));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponseDTO response = usuarioService.actualizarRol(1L, request);

        assertEquals("Administrador", response.getRolNombre());
    }

    @Test
    void actualizarRolUsuarioInexistente_lanzaNoEncontrado() {
        ActualizarRolRequestDTO request = ActualizarRolRequestDTO.builder().rolId(1L).build();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> usuarioService.actualizarRol(99L, request));
    }

    @Test
    void actualizarRolConRolInexistente_lanzaResourceNotFound() {
        Usuario u = usuario(1L, rol(4L, "Cliente Web"));
        ActualizarRolRequestDTO request = ActualizarRolRequestDTO.builder().rolId(99L).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.actualizarRol(1L, request));
    }
}
