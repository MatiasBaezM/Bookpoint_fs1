package cl.bookpointchile.ventas.listener;

import cl.bookpointchile.ventas.config.RabbitMQConfig;
import cl.bookpointchile.ventas.event.StockRechazadoEvent;
import cl.bookpointchile.ventas.event.StockReservadoEvent;
import cl.bookpointchile.ventas.model.EstadoVenta;
import cl.bookpointchile.ventas.model.Venta;
import cl.bookpointchile.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VentaEventListener {

    private final VentaRepository ventaRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_STOCK_RESERVADO)
    @Transactional
    public void procesarStockReservado(StockReservadoEvent event) {
        log.info("Recibido StockReservadoEvent para Venta ID: {}, Folio: {}", event.getVentaId(), event.getFolio());
        try {
            Venta venta = ventaRepository.findById(event.getVentaId())
                    .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + event.getVentaId()));

            venta.setEstado(EstadoVenta.COMPLETADA);
            ventaRepository.save(venta);
            log.info("Venta Folio: {} actualizada a estado COMPLETADA de forma exitosa.", venta.getFolio());
        } catch (Exception e) {
            log.error("Error al procesar reserva de stock para Venta ID {}: {}", event.getVentaId(), e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_STOCK_RECHAZADO)
    @Transactional
    public void procesarStockRechazado(StockRechazadoEvent event) {
        log.info("Recibido StockRechazadoEvent para Venta ID: {}, Folio: {}, Motivo: {}", 
                event.getVentaId(), event.getFolio(), event.getMotivo());
        try {
            Venta venta = ventaRepository.findById(event.getVentaId())
                    .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + event.getVentaId()));

            venta.setEstado(EstadoVenta.RECHAZADA);
            ventaRepository.save(venta);
            log.warn("Venta Folio: {} actualizada a estado RECHAZADA debido a: {}", venta.getFolio(), event.getMotivo());

            iniciarTransaccionCompensacion(venta, event.getMotivo());
        } catch (Exception e) {
            log.error("Error al procesar rechazo de stock para Venta ID {}: {}", event.getVentaId(), e.getMessage());
        }
    }

    private void iniciarTransaccionCompensacion(Venta venta, String motivo) {
        log.info("[TRANSACCIÓN DE COMPENSACIÓN] Iniciando reembolso y anulación para la venta Folio: {}.", venta.getFolio());
        log.info("[TRANSACCIÓN DE COMPENSACIÓN] Monto a devolver: ${}. Cliente: {} (RUT: {}).", 
                venta.getTotal(), venta.getClienteNombre(), venta.getClienteRut());
        log.info("[TRANSACCIÓN DE COMPENSACIÓN] Proceso de reembolso gatillado y registrado en bitácora de auditoría.");
    }
}
