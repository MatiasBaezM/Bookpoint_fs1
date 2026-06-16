package cl.bookpointchile.proveedores.service;

import cl.bookpointchile.proveedores.dto.*;
import cl.bookpointchile.proveedores.exception.OrdenCompraInvalidaException;
import cl.bookpointchile.proveedores.exception.ProveedorNoEncontradoException;
import cl.bookpointchile.proveedores.exception.ResourceNotFoundException;
import cl.bookpointchile.proveedores.model.EstadoOrden;
import cl.bookpointchile.proveedores.model.OrdenCompra;
import cl.bookpointchile.proveedores.model.Proveedor;
import cl.bookpointchile.proveedores.repository.OrdenCompraRepository;
import cl.bookpointchile.proveedores.repository.ProveedorRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedoresServiceImplTest {

    @Mock
    private ProveedorRepository proveedorRepository;
    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    @InjectMocks
    private ProveedoresServiceImpl proveedoresService;

    private Proveedor proveedor(Long id) {
        return Proveedor.builder()
                .id(id).rut("96543210-1").razonSocial("Distribuidora Andes")
                .emailContacto("ventas@andes.cl").telefono("+56222345678").build();
    }

    // ---------- registrarProveedor ----------

    @Test
    void registrarProveedor_guardaYRetorna() {
        CrearProveedorRequestDTO request = CrearProveedorRequestDTO.builder()
                .rut("96543210-1").razonSocial("Distribuidora Andes")
                .emailContacto("ventas@andes.cl").telefono("+56222345678").build();
        when(proveedorRepository.existsByRut("96543210-1")).thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(inv -> {
            Proveedor p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProveedorResponseDTO response = proveedoresService.registrarProveedor(request);

        assertEquals(1L, response.getId());
        assertEquals("Distribuidora Andes", response.getRazonSocial());
    }

    @Test
    void registrarProveedorDuplicado_lanzaExcepcion() {
        CrearProveedorRequestDTO request = CrearProveedorRequestDTO.builder()
                .rut("96543210-1").razonSocial("X").emailContacto("a@b.cl").build();
        when(proveedorRepository.existsByRut("96543210-1")).thenReturn(true);

        assertThrows(OrdenCompraInvalidaException.class,
                () -> proveedoresService.registrarProveedor(request));
        verify(proveedorRepository, never()).save(any());
    }

    // ---------- emitirOrdenCompra ----------

    @Test
    void emitirOrdenCompra_calculaTotalYGuarda() {
        OrdenCompraRequestDTO request = OrdenCompraRequestDTO.builder()
                .proveedorId(1L)
                .detalles(List.of(DetalleOrdenRequestDTO.builder()
                        .productoId(1L).cantidadSolicitada(100).costoUnitario(new BigDecimal("6500")).build()))
                .build();
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor(1L)));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(inv -> {
            OrdenCompra o = inv.getArgument(0);
            o.setId(1L);
            return o;
        });

        OrdenCompraResponseDTO response = proveedoresService.emitirOrdenCompra(request);

        assertEquals(EstadoOrden.PENDIENTE, response.getEstado());
        assertEquals(new BigDecimal("650000"), response.getTotal());
        assertEquals(1, response.getDetalles().size());
    }

    @Test
    void emitirOrdenCompraProveedorInexistente_lanzaNoEncontrado() {
        OrdenCompraRequestDTO request = OrdenCompraRequestDTO.builder()
                .proveedorId(99L)
                .detalles(List.of(DetalleOrdenRequestDTO.builder()
                        .productoId(1L).cantidadSolicitada(10).costoUnitario(new BigDecimal("100")).build()))
                .build();
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProveedorNoEncontradoException.class,
                () -> proveedoresService.emitirOrdenCompra(request));
    }

    // ---------- registrarRecepcionMercaderia ----------

    @Test
    void registrarRecepcion_transicionaARecibida() {
        OrdenCompra orden = OrdenCompra.builder()
                .id(1L).proveedor(proveedor(1L)).estado(EstadoOrden.PENDIENTE).build();
        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(inv -> inv.getArgument(0));

        OrdenCompraResponseDTO response = proveedoresService.registrarRecepcionMercaderia(1L);

        assertEquals(EstadoOrden.RECIBIDA, response.getEstado());
    }

    @Test
    void registrarRecepcionOrdenInexistente_lanzaResourceNotFound() {
        when(ordenCompraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> proveedoresService.registrarRecepcionMercaderia(99L));
    }

    @Test
    void registrarRecepcionOrdenCancelada_lanzaOrdenInvalida() {
        OrdenCompra orden = OrdenCompra.builder()
                .id(1L).proveedor(proveedor(1L)).estado(EstadoOrden.CANCELADA).build();
        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(orden));

        assertThrows(OrdenCompraInvalidaException.class,
                () -> proveedoresService.registrarRecepcionMercaderia(1L));
    }

    // ---------- obtenerTodosProveedores ----------

    @Test
    void obtenerTodosProveedores_retornaLista() {
        when(proveedorRepository.findAll()).thenReturn(List.of(proveedor(1L)));

        assertEquals(1, proveedoresService.obtenerTodosProveedores().size());
        verify(proveedorRepository, times(1)).findAll();
    }
}
