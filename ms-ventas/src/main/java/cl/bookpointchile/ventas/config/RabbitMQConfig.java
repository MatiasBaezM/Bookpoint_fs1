package cl.bookpointchile.ventas.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_VENTAS = "exchange.ventas";
    public static final String QUEUE_STOCK_RESERVADO = "queue.venta.stock.reservado";
    public static final String QUEUE_STOCK_RECHAZADO = "queue.venta.stock.rechazado";
    
    public static final String ROUTING_KEY_VENTA_CREADA = "routing.venta.creada";
    public static final String ROUTING_KEY_STOCK_RESERVADO = "routing.stock.reservado";
    public static final String ROUTING_KEY_STOCK_RECHAZADO = "routing.stock.rechazado";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_VENTAS);
    }

    @Bean
    public Queue stockReservadoQueue() {
        return new Queue(QUEUE_STOCK_RESERVADO, true);
    }

    @Bean
    public Queue stockRechazadoQueue() {
        return new Queue(QUEUE_STOCK_RECHAZADO, true);
    }

    @Bean
    public Binding stockReservadoBinding(Queue stockReservadoQueue, TopicExchange exchange) {
        return BindingBuilder.bind(stockReservadoQueue).to(exchange).with(ROUTING_KEY_STOCK_RESERVADO);
    }

    @Bean
    public Binding stockRechazadoBinding(Queue stockRechazadoQueue, TopicExchange exchange) {
        return BindingBuilder.bind(stockRechazadoQueue).to(exchange).with(ROUTING_KEY_STOCK_RECHAZADO);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
