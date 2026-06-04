package cl.bookpointchile.inventario.event;

import cl.bookpointchile.inventario.config.RabbitMQConfig;
import cl.bookpointchile.inventario.service.InventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioEventListener {

    private final InventarioService inventarioService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_VENTA_CREADA)
    public void onVentaCreada(VentaCreadaEvent event) {
        log.info("Evento recibido: VENTA_CREADA para venta ID: {}, Folio: {}", event.getVentaId(), event.getFolio());
        inventarioService.procesarVentaCreada(event);
    }
}
