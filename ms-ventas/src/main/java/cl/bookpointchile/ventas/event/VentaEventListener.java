package cl.bookpointchile.ventas.event;

import cl.bookpointchile.ventas.config.RabbitMQConfig;
import cl.bookpointchile.ventas.model.EstadoVenta;
import cl.bookpointchile.ventas.model.Venta;
import cl.bookpointchile.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VentaEventListener {

    private final VentaRepository ventaRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_STOCK_RESERVADO)
    public void onStockReservado(StockReservadoEvent event) {
        log.info("Evento recibido: STOCK_RESERVADO para venta ID: {}", event.getVentaId());
        
        Optional<Venta> ventaOpt = ventaRepository.findById(event.getVentaId());
        if (ventaOpt.isPresent()) {
            Venta venta = ventaOpt.get();
            venta.setEstado(EstadoVenta.COMPLETADA);
            ventaRepository.save(venta);
            log.info("Venta ID: {} actualizada a estado COMPLETADA.", event.getVentaId());
        } else {
            log.error("No se encontró la venta con ID: {} para actualizar su estado.", event.getVentaId());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_STOCK_RECHAZADO)
    public void onStockRechazado(StockRechazadoEvent event) {
        log.warn("Evento recibido: STOCK_RECHAZADO para venta ID: {}. Motivo: {}", event.getVentaId(), event.getMotivo());
        
        Optional<Venta> ventaOpt = ventaRepository.findById(event.getVentaId());
        if (ventaOpt.isPresent()) {
            Venta venta = ventaOpt.get();
            venta.setEstado(EstadoVenta.RECHAZADA);
            ventaRepository.save(venta);
            log.info("Venta ID: {} actualizada a estado RECHAZADA.", event.getVentaId());
        } else {
            log.error("No se encontró la venta con ID: {} para actualizar su estado.", event.getVentaId());
        }
    }
}
