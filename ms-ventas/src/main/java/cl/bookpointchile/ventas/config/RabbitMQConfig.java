package cl.bookpointchile.ventas.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_VENTAS = "ventas.exchange";

    public static final String QUEUE_VENTA_CREADA = "venta.creada.queue";
    public static final String QUEUE_STOCK_RESERVADO = "stock.reservado.queue";
    public static final String QUEUE_STOCK_RECHAZADO = "stock.rechazado.queue";

    public static final String ROUTING_KEY_VENTA_CREADA = "venta.creada";
    public static final String ROUTING_KEY_STOCK_RESERVADO = "stock.reservado";
    public static final String ROUTING_KEY_STOCK_RECHAZADO = "stock.rechazado";

    @Bean
    public TopicExchange ventasExchange() {
        return new TopicExchange(EXCHANGE_VENTAS);
    }

    @Bean
    public Queue ventaCreadaQueue() {
        return new Queue(QUEUE_VENTA_CREADA, true);
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
    public Binding bindingVentaCreada(Queue ventaCreadaQueue, TopicExchange ventasExchange) {
        return BindingBuilder.bind(ventaCreadaQueue).to(ventasExchange).with(ROUTING_KEY_VENTA_CREADA);
    }

    @Bean
    public Binding bindingStockReservado(Queue stockReservadoQueue, TopicExchange ventasExchange) {
        return BindingBuilder.bind(stockReservadoQueue).to(ventasExchange).with(ROUTING_KEY_STOCK_RESERVADO);
    }

    @Bean
    public Binding bindingStockRechazado(Queue stockRechazadoQueue, TopicExchange ventasExchange) {
        return BindingBuilder.bind(stockRechazadoQueue).to(ventasExchange).with(ROUTING_KEY_STOCK_RECHAZADO);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
