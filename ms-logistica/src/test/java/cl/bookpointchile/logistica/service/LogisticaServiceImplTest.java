package cl.bookpointchile.logistica.service;

import cl.bookpointchile.logistica.client.SucursalesClient;
import cl.bookpointchile.logistica.dto.ActualizarEstadoRequestDTO;
import cl.bookpointchile.logistica.dto.CrearEnvioRequestDTO;
import cl.bookpointchile.logistica.dto.EnvioResponseDTO;
import cl.bookpointchile.logistica.dto.SucursalResponseDTO;
import cl.bookpointchile.logistica.exception.OrigenNoDisponibleException;
import cl.bookpointchile.logistica.exception.ResourceNotFoundException;
import cl.bookpointchile.logistica.exception.TransicionEstadoInvalidaException;
import cl.bookpointchile.logistica.model.Envio;
import cl.bookpointchile.logistica.model.EstadoEnvio;
import cl.bookpointchile.logistica.model.RutaDistribucion;
import cl.bookpointchile.logistica.repository.EnvioRepository;
import cl.bookpointchile.logistica.repository.RutaDistribucionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogisticaServiceImplTest {

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private RutaDistribucionRepository rutaDistribucionRepository;

    @Mock
    private SucursalesClient sucursalesClient;

    @InjectMocks
    private LogisticaServiceImpl logisticaService;

    // ---------- Helpers ----------
    private RutaDistribucion rutaConcepcion() {
        return RutaDistribucion.builder()
                .id(10L)
                .origen("Bodega Central Concepción")
                .destino("Hualpén")
                .transportista("Courier BíoBío - Zona A")
                .build();
    }

    private SucursalResponseDTO sucursalOrigenActiva() {
        return SucursalResponseDTO.builder()
                .id(1L)
                .nombre("Bodega Central Concepción")
                .estadoOperativo("ACTIVO")
                .build();
    }

    // ---------- crearEnvio ----------

    @Test
    void crearEnvioConRutaIdExplicita_guardaYRetornaDespacho() {
        // Given
        CrearEnvioRequestDTO request = CrearEnvioRequestDTO.builder()
                .ventaId(100L)
                .direccionDestino("Av. Siempre Viva 123, Hualpén")
                .rutaId(10L)
                .build();
        RutaDistribucion ruta = rutaConcepcion();

        when(envioRepository.findByVentaId(100L)).thenReturn(Optional.empty());
        when(rutaDistribucionRepository.findById(10L)).thenReturn(Optional.of(ruta));
        when(sucursalesClient.obtenerTodas()).thenReturn(List.of(sucursalOrigenActiva()));
        when(envioRepository.save(any(Envio.class))).thenAnswer(inv -> {
            Envio e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        // When
        EnvioResponseDTO response = logisticaService.crearEnvio(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getVentaId());
        assertEquals(EstadoEnvio.PENDIENTE, response.getEstado());
        assertEquals("Hualpén", response.getDestino());
        verify(envioRepository, times(1)).save(any(Envio.class));
    }

    @Test
    void crearEnvioConVentaDuplicada_lanzaTransicionEstadoInvalida() {
        // Given
        CrearEnvioRequestDTO request = CrearEnvioRequestDTO.builder()
                .ventaId(100L)
                .direccionDestino("Calle 1")
                .build();
        when(envioRepository.findByVentaId(100L)).thenReturn(Optional.of(new Envio()));

        // When + Then
        TransicionEstadoInvalidaException ex = assertThrows(
                TransicionEstadoInvalidaException.class,
                () -> logisticaService.crearEnvio(request));
        assertTrue(ex.getMessage().contains("Ya existe un registro de despacho"));
        verify(envioRepository, never()).save(any(Envio.class));
    }

    @Test
    void crearEnvioConRutaIdInexistente_lanzaResourceNotFound() {
        // Given
        CrearEnvioRequestDTO request = CrearEnvioRequestDTO.builder()
                .ventaId(100L)
                .direccionDestino("Calle 1")
                .rutaId(999L)
                .build();
        when(envioRepository.findByVentaId(100L)).thenReturn(Optional.empty());
        when(rutaDistribucionRepository.findById(999L)).thenReturn(Optional.empty());

        // When + Then
        assertThrows(ResourceNotFoundException.class,
                () -> logisticaService.crearEnvio(request));
        verify(envioRepository, never()).save(any(Envio.class));
    }

    @Test
    void crearEnvioSinRutaId_asignaRutaPorComunaHualpen() {
        // Given
        CrearEnvioRequestDTO request = CrearEnvioRequestDTO.builder()
                .ventaId(101L)
                .direccionDestino("Pasaje Los Aromos 45, Hualpén")
                .build();
        when(envioRepository.findByVentaId(101L)).thenReturn(Optional.empty());
        when(rutaDistribucionRepository.findByDestinoIgnoreCase("Hualpén"))
                .thenReturn(List.of(rutaConcepcion()));
        when(sucursalesClient.obtenerTodas()).thenReturn(List.of(sucursalOrigenActiva()));
        when(envioRepository.save(any(Envio.class))).thenAnswer(inv -> {
            Envio e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        // When
        EnvioResponseDTO response = logisticaService.crearEnvio(request);

        // Then
        assertEquals("Hualpén", response.getDestino());
        verify(rutaDistribucionRepository, times(1)).findByDestinoIgnoreCase("Hualpén");
    }

    @Test
    void crearEnvioConOrigenNoOperativo_lanzaOrigenNoDisponible() {
        // Given
        CrearEnvioRequestDTO request = CrearEnvioRequestDTO.builder()
                .ventaId(102L)
                .direccionDestino("Av. Colón 3200, Talcahuano")
                .rutaId(10L)
                .build();
        when(envioRepository.findByVentaId(102L)).thenReturn(Optional.empty());
        when(rutaDistribucionRepository.findById(10L)).thenReturn(Optional.of(rutaConcepcion()));
        // Sucursal de origen existe pero está INACTIVA
        SucursalResponseDTO inactiva = SucursalResponseDTO.builder()
                .nombre("Bodega Central Concepción")
                .estadoOperativo("INACTIVO")
                .build();
        when(sucursalesClient.obtenerTodas()).thenReturn(List.of(inactiva));

        // When + Then
        assertThrows(OrigenNoDisponibleException.class,
                () -> logisticaService.crearEnvio(request));
        verify(envioRepository, never()).save(any(Envio.class));
    }

    // ---------- actualizarEstado ----------

    @Test
    void actualizarEstadoDePendienteAEnRuta_actualizaCorrectamente() {
        // Given
        Envio envio = Envio.builder().id(1L).ventaId(100L).estado(EstadoEnvio.PENDIENTE).build();
        ActualizarEstadoRequestDTO request = ActualizarEstadoRequestDTO.builder()
                .estado(EstadoEnvio.EN_RUTA).build();
        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any(Envio.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        EnvioResponseDTO response = logisticaService.actualizarEstado(1L, request);

        // Then
        assertEquals(EstadoEnvio.EN_RUTA, response.getEstado());
        verify(envioRepository, times(1)).save(envio);
    }

    @Test
    void actualizarEstadoTransicionInvalida_pendienteAEntregado_lanzaExcepcion() {
        // Given
        Envio envio = Envio.builder().id(1L).estado(EstadoEnvio.PENDIENTE).build();
        ActualizarEstadoRequestDTO request = ActualizarEstadoRequestDTO.builder()
                .estado(EstadoEnvio.ENTREGADO).build();
        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));

        // When + Then
        assertThrows(TransicionEstadoInvalidaException.class,
                () -> logisticaService.actualizarEstado(1L, request));
        verify(envioRepository, never()).save(any(Envio.class));
    }

    @Test
    void actualizarEstadoDeEnvioEntregado_noPermiteCambios() {
        // Given
        Envio envio = Envio.builder().id(1L).estado(EstadoEnvio.ENTREGADO).build();
        ActualizarEstadoRequestDTO request = ActualizarEstadoRequestDTO.builder()
                .estado(EstadoEnvio.DEVUELTO).build();
        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));

        // When + Then
        assertThrows(TransicionEstadoInvalidaException.class,
                () -> logisticaService.actualizarEstado(1L, request));
    }

    @Test
    void actualizarEstadoMismoEstado_noGuardaCambios() {
        // Given
        Envio envio = Envio.builder().id(1L).estado(EstadoEnvio.PENDIENTE).build();
        ActualizarEstadoRequestDTO request = ActualizarEstadoRequestDTO.builder()
                .estado(EstadoEnvio.PENDIENTE).build();
        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));

        // When
        EnvioResponseDTO response = logisticaService.actualizarEstado(1L, request);

        // Then
        assertEquals(EstadoEnvio.PENDIENTE, response.getEstado());
        verify(envioRepository, never()).save(any(Envio.class));
    }

    @Test
    void actualizarEstadoEnvioInexistente_lanzaResourceNotFound() {
        // Given
        ActualizarEstadoRequestDTO request = ActualizarEstadoRequestDTO.builder()
                .estado(EstadoEnvio.EN_RUTA).build();
        when(envioRepository.findById(99L)).thenReturn(Optional.empty());

        // When + Then
        assertThrows(ResourceNotFoundException.class,
                () -> logisticaService.actualizarEstado(99L, request));
    }

    // ---------- obtenerEnvioPorVentaId ----------

    @Test
    void obtenerEnvioPorVentaIdExistente_retornaDespacho() {
        // Given
        Envio envio = Envio.builder()
                .id(1L).ventaId(5001L).estado(EstadoEnvio.PENDIENTE)
                .rutaDistribucion(rutaConcepcion()).build();
        when(envioRepository.findByVentaId(5001L)).thenReturn(Optional.of(envio));

        // When
        EnvioResponseDTO response = logisticaService.obtenerEnvioPorVentaId(5001L);

        // Then
        assertNotNull(response);
        assertEquals(5001L, response.getVentaId());
        assertEquals("Courier BíoBío - Zona A", response.getTransportista());
    }

    @Test
    void obtenerEnvioPorVentaIdInexistente_lanzaResourceNotFound() {
        // Given
        when(envioRepository.findByVentaId(9999L)).thenReturn(Optional.empty());

        // When + Then
        assertThrows(ResourceNotFoundException.class,
                () -> logisticaService.obtenerEnvioPorVentaId(9999L));
    }
}
