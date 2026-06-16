package cl.bookpointchile.promociones.service;

import cl.bookpointchile.promociones.dto.CrearPromocionRequestDTO;
import cl.bookpointchile.promociones.dto.PromocionResponseDTO;
import cl.bookpointchile.promociones.exception.PromocionCaducadaException;
import cl.bookpointchile.promociones.exception.PromocionNoEncontradaException;
import cl.bookpointchile.promociones.model.Promocion;
import cl.bookpointchile.promociones.repository.PromocionRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromocionesServiceImplTest {

    @Mock
    private PromocionRepository promocionRepository;

    @InjectMocks
    private PromocionesServiceImpl promocionesService;

    private Promocion promocionVigente() {
        return Promocion.builder()
                .id(1L).codigo("DESCUENTO10").porcentajeDescuento(10)
                .fechaInicio(LocalDate.now().minusDays(5))
                .fechaFin(LocalDate.now().plusDays(5))
                .estado("ACTIVO").build();
    }

    // ---------- registrarPromocion ----------

    @Test
    void registrarPromocion_guardaYRetorna() {
        CrearPromocionRequestDTO request = CrearPromocionRequestDTO.builder()
                .codigo("descuento10").porcentajeDescuento(10)
                .fechaInicio(LocalDate.now()).fechaFin(LocalDate.now().plusDays(30)).build();
        when(promocionRepository.existsByCodigoIgnoreCase("DESCUENTO10")).thenReturn(false);
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(inv -> {
            Promocion p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PromocionResponseDTO response = promocionesService.registrarPromocion(request);

        assertEquals("DESCUENTO10", response.getCodigo());
        assertEquals("ACTIVO", response.getEstado());
    }

    @Test
    void registrarPromocionDuplicada_lanzaExcepcion() {
        CrearPromocionRequestDTO request = CrearPromocionRequestDTO.builder()
                .codigo("DESCUENTO10").porcentajeDescuento(10)
                .fechaInicio(LocalDate.now()).fechaFin(LocalDate.now().plusDays(30)).build();
        when(promocionRepository.existsByCodigoIgnoreCase("DESCUENTO10")).thenReturn(true);

        assertThrows(PromocionCaducadaException.class,
                () -> promocionesService.registrarPromocion(request));
        verify(promocionRepository, never()).save(any());
    }

    // ---------- validarPromocion ----------

    @Test
    void validarPromocionVigente_retornaPromocion() {
        when(promocionRepository.findByCodigoIgnoreCase("DESCUENTO10"))
                .thenReturn(Optional.of(promocionVigente()));

        PromocionResponseDTO response = promocionesService.validarPromocion("descuento10");

        assertTrue(response.isVigente());
        assertEquals(10, response.getPorcentajeDescuento());
    }

    @Test
    void validarPromocionInexistente_lanzaNoEncontrada() {
        when(promocionRepository.findByCodigoIgnoreCase("NOPE")).thenReturn(Optional.empty());

        assertThrows(PromocionNoEncontradaException.class,
                () -> promocionesService.validarPromocion("nope"));
    }

    @Test
    void validarPromocionInactiva_lanzaCaducada() {
        Promocion inactiva = promocionVigente();
        inactiva.setEstado("INACTIVO");
        when(promocionRepository.findByCodigoIgnoreCase("DESCUENTO10"))
                .thenReturn(Optional.of(inactiva));

        assertThrows(PromocionCaducadaException.class,
                () -> promocionesService.validarPromocion("DESCUENTO10"));
    }

    @Test
    void validarPromocionCaducada_lanzaCaducada() {
        Promocion caducada = Promocion.builder()
                .id(1L).codigo("VIEJA").porcentajeDescuento(10)
                .fechaInicio(LocalDate.now().minusDays(30))
                .fechaFin(LocalDate.now().minusDays(1))
                .estado("ACTIVO").build();
        when(promocionRepository.findByCodigoIgnoreCase("VIEJA"))
                .thenReturn(Optional.of(caducada));

        assertThrows(PromocionCaducadaException.class,
                () -> promocionesService.validarPromocion("VIEJA"));
    }

    // ---------- obtenerTodas ----------

    @Test
    void obtenerTodas_retornaLista() {
        when(promocionRepository.findAll()).thenReturn(List.of(promocionVigente()));

        assertEquals(1, promocionesService.obtenerTodas().size());
        verify(promocionRepository, times(1)).findAll();
    }
}
