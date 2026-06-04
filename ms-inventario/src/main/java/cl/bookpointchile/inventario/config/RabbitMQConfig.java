package cl.bookpointchile.inventario.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_VENTAS = "exchange.ventas";
    public static final String QUEUE_VENTA_CREADA = "queue.inventario.venta.creada";
    
    public static final String ROUTING_KEY_VENTA_CREADA = "routing.venta.creada";
    public static final String ROUTING_KEY_STOCK_RESERVADO = "routing.stock.reservado";
    public static final String ROUTING_KEY_STOCK_RECHAZADO = "routing.stock.rechazado";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_VENTAS);
    }

    @Bean
    public Queue ventaCreadaQueue() {
        return new Queue(QUEUE_VENTA_CREADA, true);
    }

    @Bean
    public Binding ventaCreadaBinding(Queue ventaCreadaQueue, TopicExchange exchange) {
        return BindingBuilder.bind(ventaCreadaQueue).to(exchange).with(ROUTING_KEY_VENTA_CREADA);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
