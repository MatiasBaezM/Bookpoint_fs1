package cl.bookpointchile.inventario.service;

import cl.bookpointchile.inventario.client.SucursalesClient;
import cl.bookpointchile.inventario.dto.*;
import cl.bookpointchile.inventario.exception.StockInsuficienteException;
import cl.bookpointchile.inventario.exception.SucursalNoEncontradaException;
import cl.bookpointchile.inventario.model.Inventario;
import cl.bookpointchile.inventario.model.Sucursal;
import cl.bookpointchile.inventario.repository.InventarioRepository;
import cl.bookpointchile.inventario.repository.SucursalRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    @Mock
    private InventarioRepository inventarioRepository;
    @Mock
    private SucursalRepository sucursalRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private SucursalesClient sucursalesClient;

    @InjectMocks
    private InventarioServiceImpl inventarioService;

    private Sucursal sucursal(Long id) {
        return Sucursal.builder().id(id).nombre("Sucursal Centro").direccion("Av. 1").build();
    }

    private Inventario inventario(Long id, int cantidad, int stockMinimo, Sucursal s) {
        return Inventario.builder()
                .id(id).productoId(1L).productoNombre("Libro").sku("SKU-1-" + s.getId())
                .cantidad(cantidad).stockMinimo(stockMinimo).sucursal(s).build();
    }

    // ---------- registrarAjusteFisico ----------

    @Test
    void registrarAjusteCreaNuevoInventario_cuandoNoExiste() {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).sucursalId(1L).cantidadAjuste(50).motivo("Reposición").build();
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(sucursal(1L)));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L)).thenReturn(Optional.empty());
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(inv -> {
            Inventario i = inv.getArgument(0);
            i.setId(10L);
            return i;
        });

        InventarioResponseDTO response = inventarioService.registrarAjusteFisico(request);

        assertEquals(50, response.getCantidad());
        assertEquals(1L, response.getSucursalId());
    }

    @Test
    void registrarAjusteNegativoSinInventarioPrevio_lanzaStockInsuficiente() {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).sucursalId(1L).cantidadAjuste(-5).motivo("Merma").build();
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(sucursal(1L)));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.registrarAjusteFisico(request));
        verify(inventarioRepository, never()).save(any());
    }

    @Test
    void registrarAjusteSucursalInexistente_lanzaSucursalNoEncontrada() {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).sucursalId(99L).cantidadAjuste(5).motivo("X").build();
        when(sucursalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SucursalNoEncontradaException.class,
                () -> inventarioService.registrarAjusteFisico(request));
    }

    @Test
    void registrarAjusteQueDejaStockNegativo_lanzaStockInsuficiente() {
        AjusteStockRequestDTO request = AjusteStockRequestDTO.builder()
                .productoId(1L).sucursalId(1L).cantidadAjuste(-100).motivo("Merma").build();
        Sucursal s = sucursal(1L);
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(s));
        when(inventarioRepository.findByProductoIdAndSucursalId(1L, 1L))
                .thenReturn(Optional.of(inventario(10L, 10, 5, s)));

        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.registrarAjusteFisico(request));
    }

    // ---------- trasladarStock ----------

    @Test
    void trasladarStockOrigenIgualDestino_lanzaStockInsuficiente() {
        TrasladoStockRequestDTO request = TrasladoStockRequestDTO.builder()
                .productoId(1L).sucursalOrigenId(1L).sucursalDestinoId(1L).cantidad(5).build();

        assertThrows(StockInsuficienteException.class,
                () -> inventarioService.trasladarStock(request));
    }

    // ---------- obtenerStock ----------

    @Test
    void obtenerStockSucursalInexistente_lanzaSucursalNoEncontrada() {
        when(sucursalRepository.existsById(99L)).thenReturn(false);

        assertThrows(SucursalNoEncontradaException.class,
                () -> inventarioService.obtenerStock(99L, 1L));
    }

    // ---------- verificarDisponibilidad ----------

    @Test
    void verificarDisponibilidadConStockSuficiente_retornaDisponible() {
        Sucursal s = sucursal(1L);
        when(inventarioRepository.findAll()).thenReturn(List.of(inventario(10L, 50, 5, s)));

        StockResponseDTO response = inventarioService.verificarDisponibilidad(1L, 10);

        assertTrue(response.isDisponible());
        assertEquals(50, response.getStockActual());
    }

    @Test
    void verificarDisponibilidadConStockInsuficiente_retornaNoDisponible() {
        Sucursal s = sucursal(1L);
        when(inventarioRepository.findAll()).thenReturn(List.of(inventario(10L, 2, 5, s)));

        StockResponseDTO response = inventarioService.verificarDisponibilidad(1L, 10);

        assertFalse(response.isDisponible());
        assertEquals(2, response.getStockActual());
    }

    // ---------- obtenerAlertasReposicion ----------

    @Test
    void obtenerAlertasReposicion_retornaLista() {
        Sucursal s = sucursal(1L);
        when(inventarioRepository.findAlertasStock()).thenReturn(List.of(inventario(10L, 3, 5, s)));

        List<InventarioResponseDTO> response = inventarioService.obtenerAlertasReposicion();

        assertEquals(1, response.size());
        assertTrue(response.get(0).isAlertaReposicion());
    }
}
