package cl.bookpointchile.ventas.event;

import cl.bookpointchile.ventas.client.FacturacionClient;
import cl.bookpointchile.ventas.dto.DocumentoResponseDTO;
import cl.bookpointchile.ventas.dto.EmitirDocumentoRequestDTO;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaConfirmadaListenerTest {

    @Mock
    private FacturacionClient facturacionClient;

    private VentaConfirmadaListener listener;

    private EmitirDocumentoRequestDTO request() {
        return EmitirDocumentoRequestDTO.builder()
                .folioVenta("BP-ONL-ABCD1234")
                .ventaId(1L)
                .sucursalId(1L)
                .rutCliente("66666666-6")
                .tipoDocumento("BOLETA")
                .montoNeto(10000.0)
                .build();
    }

    @Test
    void alConfirmarVenta_llamaAFacturacionClientConLaSolicitudDelEvento() {
        // Given
        listener = new VentaConfirmadaListener(facturacionClient);
        EmitirDocumentoRequestDTO request = request();
        when(facturacionClient.emitirDocumento(request))
                .thenReturn(DocumentoResponseDTO.builder().id(99L).folioVenta(request.getFolioVenta()).build());

        // When
        listener.alConfirmarVenta(new VentaRegistradaInternaEvent(this, request));

        // Then
        verify(facturacionClient, times(1)).emitirDocumento(request);
    }

    @Test
    void alConfirmarVenta_siFacturacionFalla_noPropagaLaExcepcion() {
        // Given: la emisión del documento es best-effort tras confirmar la venta;
        // un fallo de comunicación con ms-facturacion no debe romper el listener.
        listener = new VentaConfirmadaListener(facturacionClient);
        EmitirDocumentoRequestDTO request = request();
        when(facturacionClient.emitirDocumento(request)).thenThrow(mock(FeignException.class));

        // When + Then
        assertDoesNotThrow(() -> listener.alConfirmarVenta(new VentaRegistradaInternaEvent(this, request)));
        verify(facturacionClient, times(1)).emitirDocumento(request);
    }
}
