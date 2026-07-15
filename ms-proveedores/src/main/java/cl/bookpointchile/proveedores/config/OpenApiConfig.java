package cl.bookpointchile.proveedores.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI().info(new Info()
                .title("BookPoint Chile - API de Proveedores")
                .description("Microservicio de gestión de proveedores y órdenes de compra, "
                        + "incluida la recepción de mercadería que actualiza el inventario.")
                .version("1.0.0"));
    }
}
