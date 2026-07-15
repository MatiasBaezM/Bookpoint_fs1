package cl.bookpointchile.ventas.event;

import cl.bookpointchile.ventas.client.FacturacionClient;
import cl.bookpointchile.ventas.dto.DocumentoResponseDTO;
import cl.bookpointchile.ventas.dto.EmitirDocumentoRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class VentaConfirmadaListener {

    private final FacturacionClient facturacionClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alConfirmarVenta(VentaRegistradaInternaEvent event) {
        EmitirDocumentoRequestDTO request = event.getRequest();
        log.info("Venta confirmada y guardada en BD. Iniciando emisión de factura/boleta para folio: {}", request.getFolioVenta());
        try {
            DocumentoResponseDTO documento = facturacionClient.emitirDocumento(request);
            log.info("Documento tributario emitido exitosamente para folio {}: ID {}", request.getFolioVenta(), documento.getId());
        } catch (Exception e) {
            log.warn("No fue posible emitir el documento tributario tras confirmar la venta {}: {}", request.getFolioVenta(), e.getMessage());
        }
    }
}
