package cl.bookpointchile.sucursales.service;

import cl.bookpointchile.sucursales.dto.SucursalRequestDTO;
import cl.bookpointchile.sucursales.dto.SucursalResponseDTO;
import cl.bookpointchile.sucursales.exception.NombreSucursalDuplicadoException;
import cl.bookpointchile.sucursales.exception.SucursalNoEncontradaException;
import cl.bookpointchile.sucursales.model.Sucursal;
import cl.bookpointchile.sucursales.repository.SucursalRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SucursalServiceImplTest {

    @Mock
    private SucursalRepository repository;

    @InjectMocks
    private SucursalServiceImpl sucursalService;

    private Sucursal sucursal(Long id, String nombre) {
        return Sucursal.builder()
                .id(id).nombre(nombre).direccion("Av. 1").comuna("Concepción")
                .region("Región del Biobío").telefono("+5641000000")
                .horarioAtencion("L-V 09:00-18:00").estadoOperativo("ACTIVO").build();
    }

    // ---------- crearSucursal ----------

    @Test
    void crearSucursal_guardaYAplicaDefaults() {
        SucursalRequestDTO request = SucursalRequestDTO.builder()
                .nombre("Sucursal Centro").direccion("Av. Libertad 1234").comuna("Concepción").build();
        when(repository.existsByNombreIgnoreCase("Sucursal Centro")).thenReturn(false);
        when(repository.save(any(Sucursal.class))).thenAnswer(inv -> {
            Sucursal s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SucursalResponseDTO response = sucursalService.crearSucursal(request);

        assertEquals(1L, response.getId());
        // Región por defecto cuando no se envía
        assertEquals("Región Metropolitana", response.getRegion());
        assertEquals("ACTIVO", response.getEstadoOperativo());
    }

    @Test
    void crearSucursalNombreDuplicado_lanzaExcepcion() {
        SucursalRequestDTO request = SucursalRequestDTO.builder()
                .nombre("Sucursal Centro").direccion("Av. 1").comuna("Concepción").build();
        when(repository.existsByNombreIgnoreCase("Sucursal Centro")).thenReturn(true);

        assertThrows(NombreSucursalDuplicadoException.class,
                () -> sucursalService.crearSucursal(request));
        verify(repository, never()).save(any());
    }

    // ---------- obtenerPorId ----------

    @Test
    void obtenerPorIdExistente_retornaSucursal() {
        when(repository.findById(1L)).thenReturn(Optional.of(sucursal(1L, "Centro")));

        SucursalResponseDTO response = sucursalService.obtenerPorId(1L);

        assertEquals("Centro", response.getNombre());
    }

    @Test
    void obtenerPorIdInexistente_lanzaNoEncontrada() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SucursalNoEncontradaException.class,
                () -> sucursalService.obtenerPorId(99L));
    }

    // ---------- actualizarSucursal ----------

    @Test
    void actualizarSucursal_actualizaCampos() {
        Sucursal existente = sucursal(1L, "Centro");
        SucursalRequestDTO request = SucursalRequestDTO.builder()
                .nombre("Centro").direccion("Nueva Dir 999").comuna("Talcahuano").build();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Sucursal.class))).thenAnswer(inv -> inv.getArgument(0));

        SucursalResponseDTO response = sucursalService.actualizarSucursal(1L, request);

        assertEquals("Talcahuano", response.getComuna());
        assertEquals("Nueva Dir 999", response.getDireccion());
    }

    @Test
    void actualizarSucursalInexistente_lanzaNoEncontrada() {
        SucursalRequestDTO request = SucursalRequestDTO.builder()
                .nombre("X").direccion("Y").comuna("Z").build();
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SucursalNoEncontradaException.class,
                () -> sucursalService.actualizarSucursal(99L, request));
    }

    @Test
    void actualizarSucursalConNombreYaUsado_lanzaDuplicado() {
        Sucursal existente = sucursal(1L, "Centro");
        SucursalRequestDTO request = SucursalRequestDTO.builder()
                .nombre("Otra Sucursal").direccion("Dir").comuna("Comuna").build();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByNombreIgnoreCase("Otra Sucursal")).thenReturn(true);

        assertThrows(NombreSucursalDuplicadoException.class,
                () -> sucursalService.actualizarSucursal(1L, request));
    }

    // ---------- obtenerTodas ----------

    @Test
    void obtenerTodas_retornaLista() {
        when(repository.findAll()).thenReturn(List.of(sucursal(1L, "Centro")));

        assertEquals(1, sucursalService.obtenerTodas().size());
        verify(repository, times(1)).findAll();
    }
}
