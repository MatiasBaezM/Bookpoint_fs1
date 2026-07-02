package cl.bookpointchile.inventario.listener;

import cl.bookpointchile.inventario.config.RabbitMQConfig;
import cl.bookpointchile.inventario.event.VentaCreadaEvent;
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
    public void listenVentaCreada(VentaCreadaEvent event) {
        log.info("Recibido VentaCreadaEvent para procesar stock de forma asíncrona. Venta ID: {}, Folio: {}", 
                event.getVentaId(), event.getFolio());
        try {
            inventarioService.procesarVentaCreada(event);
        } catch (Exception e) {
            log.error("Fallo crítico al delegar el procesamiento de VentaCreadaEvent para Venta ID {}: {}", 
                    event.getVentaId(), e.getMessage());
        }
    }
}
